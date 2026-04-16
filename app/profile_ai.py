import json
from typing import Any

from fastapi import HTTPException
from openai import APIError, OpenAI, RateLimitError

from app.config import settings

PROFILE_AI_SYSTEM_PROMPT = """
You are a conversational AI that helps complete and refine a user's profile dictionary for a social app.

You will receive:
- the user's display name
- the current profile dictionary
- the conversation transcript so far
- the latest user message when applicable
- a mode: either start, continue, or end

Your job:
- maintain and improve the profile dictionary as the conversation progresses
- ask exactly one short, natural next question when the mode is start or continue
- if the profile is already complete, ask whether the user wants to add or change anything
- when the mode is end, do not ask another question; return a brief closing line and the final updated profile dictionary

Use this profile dictionary shape exactly:
{
  "home_base": "<string>",
  "work_context": "<string>",
  "social_energy": "<string>",
  "ideal_plans": "<string>",
  "social_goals": "<string>",
  "conversation_spark": "<string>",
  "interest_tags": ["<string>", "..."],
  "profile_notes": "<string>"
}

Completeness rules:
- the profile is complete when all string fields are non-empty and interest_tags has at least 3 items
- keep existing details unless the conversation clearly updates them
- normalize concise answers into polished but faithful profile language

Return JSON only with this exact shape:
{
  "assistant_text": "<next question or closing line>",
  "updated_profile_dict": {
    "home_base": "<string>",
    "work_context": "<string>",
    "social_energy": "<string>",
    "ideal_plans": "<string>",
    "social_goals": "<string>",
    "conversation_spark": "<string>",
    "interest_tags": ["<string>", "..."],
    "profile_notes": "<string>"
  },
  "is_complete": <true or false>
}
""".strip()

PROFILE_AI_FIELDS = (
    "home_base",
    "work_context",
    "social_energy",
    "ideal_plans",
    "social_goals",
    "conversation_spark",
    "profile_notes",
)


def _extract_json(text: str) -> dict[str, Any]:
    normalized = text.strip()
    if normalized.startswith("```"):
        lines = normalized.splitlines()
        if lines and lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].startswith("```"):
            lines = lines[:-1]
        normalized = "\n".join(lines).strip()
    return json.loads(normalized)


def _normalize_profile_dict(profile: dict[str, Any]) -> dict[str, Any]:
    normalized = {
        key: str(profile.get(key, "") or "").strip()
        for key in PROFILE_AI_FIELDS
    }
    tags = profile.get("interest_tags", [])
    if not isinstance(tags, list):
        tags = []
    normalized["interest_tags"] = [
        str(tag).strip()
        for tag in tags
        if str(tag).strip()
    ]
    return normalized


def is_profile_complete(profile: dict[str, Any]) -> bool:
    normalized = _normalize_profile_dict(profile)
    return all(normalized[key] for key in PROFILE_AI_FIELDS) and len(normalized["interest_tags"]) >= 3


def _build_payload(
    *,
    mode: str,
    display_name: str | None,
    current_profile_dict: dict[str, Any],
    transcript: list[dict[str, str]],
    user_message: str | None,
) -> dict[str, Any]:
    return {
        "mode": mode,
        "display_name": display_name or "User",
        "current_profile_dict": _normalize_profile_dict(current_profile_dict),
        "is_profile_complete": is_profile_complete(current_profile_dict),
        "transcript": transcript,
        "latest_user_message": (user_message or "").strip() or None,
    }


def _call_profile_model(payload: dict[str, Any], detail_label: str) -> dict[str, Any]:
    if not settings.openai_api_key:
        raise HTTPException(status_code=500, detail="OPENAI_KEY is not configured")

    client = OpenAI(api_key=settings.openai_api_key)
    try:
        response = client.responses.create(
            model=settings.openai_model,
            input=[
                {
                    "role": "system",
                    "content": [{"type": "input_text", "text": PROFILE_AI_SYSTEM_PROMPT}],
                },
                {
                    "role": "user",
                    "content": [{"type": "input_text", "text": json.dumps(payload, ensure_ascii=True)}],
                },
            ],
        )
    except RateLimitError as exc:
        raise HTTPException(status_code=503, detail=f"OpenAI quota exceeded for {detail_label}") from exc
    except APIError as exc:
        raise HTTPException(status_code=502, detail=f"OpenAI request failed during {detail_label}") from exc

    try:
        result = _extract_json(response.output_text)
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=502, detail=f"OpenAI returned an invalid payload for {detail_label}") from exc

    updated_profile_dict = _normalize_profile_dict(result.get("updated_profile_dict", {}))
    assistant_text = str(result.get("assistant_text", "")).strip()
    if not assistant_text:
        raise HTTPException(status_code=502, detail=f"OpenAI response missing assistant_text for {detail_label}")

    return {
        "assistant_text": assistant_text,
        "updated_profile_dict": updated_profile_dict,
        "is_complete": is_profile_complete(updated_profile_dict),
        "model": settings.openai_model,
    }


def start_profile_ai_session(
    *,
    display_name: str | None,
    current_profile_dict: dict[str, Any],
    transcript: list[dict[str, str]],
) -> dict[str, Any]:
    payload = _build_payload(
        mode="start",
        display_name=display_name,
        current_profile_dict=current_profile_dict,
        transcript=transcript,
        user_message=None,
    )
    return _call_profile_model(payload, "profile AI start")


def continue_profile_ai_session(
    *,
    display_name: str | None,
    current_profile_dict: dict[str, Any],
    transcript: list[dict[str, str]],
    user_message: str,
) -> dict[str, Any]:
    payload = _build_payload(
        mode="continue",
        display_name=display_name,
        current_profile_dict=current_profile_dict,
        transcript=transcript,
        user_message=user_message,
    )
    return _call_profile_model(payload, "profile AI reply")


def end_profile_ai_session(
    *,
    display_name: str | None,
    current_profile_dict: dict[str, Any],
    transcript: list[dict[str, str]],
) -> dict[str, Any]:
    payload = _build_payload(
        mode="end",
        display_name=display_name,
        current_profile_dict=current_profile_dict,
        transcript=transcript,
        user_message=None,
    )
    result = _call_profile_model(payload, "profile AI end")
    return {
        "final_profile_dict": result["updated_profile_dict"],
        "closing_text": result["assistant_text"],
        "is_complete": result["is_complete"],
        "model": result["model"],
    }
