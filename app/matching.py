import json
from typing import Any

from fastapi import HTTPException
from openai import APIError, OpenAI, RateLimitError

from app.config import settings

MATCHING_SYSTEM_PROMPT = """
You are evaluating whether two people are a good social match for friendship or community connection.

You will receive two full profile dictionaries. Use the profile data as provided. Compare interests, lifestyle,
social intent, work/home context, event history, and any other useful signals in the profiles.

Return JSON only with this exact shape:
{
  "score": <integer from 0 to 100>,
  "reasoning": "<short paragraph explaining the match overall>",
  "what_matches": ["..."],
  "what_does_not_match": ["..."]
}

Scoring rules:
- 90-100: unusually strong fit
- 70-89: clearly good fit
- 50-69: moderate fit
- 30-49: weak fit
- 0-29: poor fit

Be concrete and rely on the given profile fields. Keep reasoning concise.
""".strip()

EVENT_MATCHING_SYSTEM_PROMPT = """
You are evaluating whether a person is a good fit for an event.

You will receive:
- one user profile dictionary
- one event dictionary
- creator profile data for the event
- attendee profile data for people already going

Judge whether the user should be shown or recommended this event.
Use interests, timing, vibe, social intent, location fit, event format, creator profile,
and attendee mix when helpful.

Return JSON only with this exact shape:
{
  "score": <integer from 0 to 100>,
  "reasoning": "<short paragraph explaining the fit overall>",
  "what_matches": ["..."],
  "what_does_not_match": ["..."]
}

Scoring rules:
- 90-100: extremely strong event fit
- 70-89: good event fit
- 50-69: possible fit
- 30-49: weak fit
- 0-29: poor fit

Be concrete and concise.
""".strip()


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


def build_profile_payload(user: dict[str, Any]) -> dict[str, Any]:
    return {
        "user_id": user["_id"],
        "display_name": user.get("display_name"),
        "username": user.get("username"),
        "persona": user.get("persona", {}),
        "event_refs": user.get("event_refs", {}),
        "connections": user.get("connections", []),
    }


def build_event_payload(
    event: dict[str, Any],
    creator: dict[str, Any] | None,
    attendees: list[dict[str, Any]],
) -> dict[str, Any]:
    return {
        "event_id": event["_id"],
        "title": event.get("title"),
        "description": event.get("description"),
        "category_tags": event.get("category_tags", []),
        "status": event.get("status"),
        "visibility_type": event.get("visibility_type"),
        "timing": event.get("timing", {}),
        "location": event.get("location", {}),
        "capacity": event.get("capacity", {}),
        "attendance": event.get("attendance", {}),
        "routing": event.get("routing", {}),
        "creator": build_profile_payload(creator) if creator else None,
        "attendees": [build_profile_payload(attendee) for attendee in attendees],
    }


def _call_matching_model(prompt: str, payload: dict[str, Any], detail_label: str) -> dict[str, Any]:
    if not settings.openai_api_key:
        raise HTTPException(status_code=500, detail="OPENAI_KEY is not configured")

    client = OpenAI(api_key=settings.openai_api_key)
    try:
        response = client.responses.create(
            model=settings.openai_model,
            input=[
                {
                    "role": "system",
                    "content": [{"type": "input_text", "text": prompt}],
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

    score = result.get("score")
    if not isinstance(score, int):
        raise HTTPException(status_code=502, detail=f"OpenAI response missing integer score for {detail_label}")

    return {
        "score": max(0, min(100, score)),
        "reasoning": result.get("reasoning", ""),
        "what_matches": result.get("what_matches", []),
        "what_does_not_match": result.get("what_does_not_match", []),
        "model": settings.openai_model,
    }


def score_profiles(user_one: dict[str, Any], user_two: dict[str, Any]) -> dict[str, Any]:
    payload = {
        "user_1": build_profile_payload(user_one),
        "user_2": build_profile_payload(user_two),
    }
    result = _call_matching_model(MATCHING_SYSTEM_PROMPT, payload, "connection matching")

    return {
        "user_1": user_one["_id"],
        "user_2": user_two["_id"],
        **result,
    }


def score_user_for_event(
    user: dict[str, Any],
    event: dict[str, Any],
    creator: dict[str, Any] | None,
    attendees: list[dict[str, Any]],
) -> dict[str, Any]:
    payload = {
        "user": build_profile_payload(user),
        "event": build_event_payload(event, creator, attendees),
    }
    result = _call_matching_model(EVENT_MATCHING_SYSTEM_PROMPT, payload, "event matching")

    return {
        "user_id": user["_id"],
        "event_id": event["_id"],
        **result,
    }
