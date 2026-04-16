import math
from typing import Any

from fastapi import HTTPException
from openai import APIError, OpenAI, RateLimitError

from app.config import settings
from app.db import db
from app.utils import utc_now


def build_profile_embedding_text(user: dict[str, Any]) -> str:
    persona = user.get("persona") or {}
    profile_dict = persona.get("profile_dict") if isinstance(persona.get("profile_dict"), dict) else {}
    mobile_profile = persona.get("mobile_profile") if isinstance(persona.get("mobile_profile"), dict) else {}

    journey_lines = [
        f"{entry.get('title', '').strip()}: {entry.get('body', '').strip()}"
        for entry in mobile_profile.get("journey", [])
        if isinstance(entry, dict)
    ]
    interest_lines = [
        f"{entry.get('tag', '').strip()}: {entry.get('description', '').strip()}"
        for entry in mobile_profile.get("interests", [])
        if isinstance(entry, dict)
    ]
    profile_lines = [
        f"display_name: {user.get('display_name', '')}",
        f"username: {user.get('username', '')}",
        f"headline: {mobile_profile.get('headline', '')}",
        f"location: {mobile_profile.get('location_label', '')}",
        f"interests_summary: {mobile_profile.get('interests_summary', '')}",
        f"home_base: {profile_dict.get('home_base', '')}",
        f"work_context: {profile_dict.get('work_context', '')}",
        f"social_energy: {profile_dict.get('social_energy', '')}",
        f"ideal_plans: {profile_dict.get('ideal_plans', '')}",
        f"social_goals: {profile_dict.get('social_goals', '')}",
        f"conversation_spark: {profile_dict.get('conversation_spark', '')}",
        f"interest_tags: {', '.join(profile_dict.get('interest_tags', []))}",
        f"profile_notes: {profile_dict.get('profile_notes', '')}",
        f"event_refs: {user.get('event_refs', {})}",
        f"journey: {' | '.join(journey_lines)}",
        f"interests: {' | '.join(interest_lines)}",
    ]
    return "\n".join(line.strip() for line in profile_lines if line.strip())


def ensure_profile_embedding(user: dict[str, Any], *, force: bool = False) -> dict[str, Any] | None:
    if not settings.openai_api_key:
        return None

    embedding_text = build_profile_embedding_text(user)
    stored_embedding = ((user.get("matching") or {}).get("profile_embedding")) or {}
    stored_vector = stored_embedding.get("vector")
    if (
        not force
        and stored_embedding.get("model") == settings.openai_embedding_model
        and stored_embedding.get("text") == embedding_text
        and isinstance(stored_vector, list)
        and stored_vector
    ):
        return stored_embedding

    client = OpenAI(api_key=settings.openai_api_key)
    try:
        response = client.embeddings.create(
            model=settings.openai_embedding_model,
            input=embedding_text,
        )
    except RateLimitError as exc:
        raise HTTPException(status_code=503, detail="OpenAI quota exceeded for community embeddings") from exc
    except APIError as exc:
        raise HTTPException(status_code=502, detail="OpenAI request failed during community embeddings") from exc

    vector = response.data[0].embedding
    embedding_record = {
        "model": settings.openai_embedding_model,
        "text": embedding_text,
        "vector": vector,
        "updated_at": utc_now(),
    }
    db.users.update_one(
        {"_id": user["_id"]},
        {
            "$set": {"matching.profile_embedding": embedding_record},
            "$unset": {"matching.profile_embedding_error": ""},
        },
    )
    user.setdefault("matching", {})["profile_embedding"] = embedding_record
    return embedding_record


def refresh_profile_embedding_for_user(user_id: str) -> None:
    user = db.users.find_one({"_id": user_id})
    if not user:
        return
    try:
        ensure_profile_embedding(user, force=True)
    except HTTPException as exc:
        db.users.update_one(
            {"_id": user_id},
            {
                "$set": {
                    "matching.profile_embedding_error": {
                        "detail": exc.detail,
                        "updated_at": utc_now(),
                    }
                }
            },
        )


def rank_community_candidates(current_user: dict[str, Any], candidate_users: list[dict[str, Any]], limit: int = 10) -> dict[str, Any]:
    if not candidate_users:
        return {
            "user_id": current_user["_id"],
            "evaluated_candidates": 0,
            "results": [],
        }

    shortlist = _embedding_shortlist(current_user, candidate_users, limit=limit)
    reranked = []
    for candidate, embedding_similarity in shortlist[: max(1, min(limit, 20))]:
        shared_event_ids = _shared_event_ids(current_user, candidate)
        shared_event_count = len(shared_event_ids)
        reranked.append(
            {
                "user_id": candidate["_id"],
                "display_name": candidate.get("display_name"),
                "username": candidate.get("username"),
                "avatar_url": candidate.get("avatar_url"),
                "headline": ((candidate.get("persona") or {}).get("mobile_profile") or {}).get("headline"),
                "score": embedding_score(embedding_similarity, shared_event_count),
                "embedding_similarity": embedding_similarity,
                "reasoning": build_embedding_reasoning(current_user, candidate, embedding_similarity, shared_event_count),
                "what_matches": build_embedding_matches(current_user, candidate),
                "what_does_not_match": [],
                "shared_event_count": shared_event_count,
                "shared_event_ids": shared_event_ids,
            }
        )

    reranked.sort(key=lambda item: (item["embedding_similarity"], item["shared_event_count"]), reverse=True)
    return {
        "user_id": current_user["_id"],
        "evaluated_candidates": len(candidate_users),
        "shortlisted_with_embeddings": len(shortlist),
        "reranked_candidates": 0,
        "results": reranked[: max(1, min(limit, 20))],
    }


def backfill_profile_embeddings(batch_size: int = 16) -> dict[str, Any]:
    if not settings.openai_api_key:
        raise HTTPException(status_code=500, detail="OPENAI_KEY is not configured")

    users = list(db.users.find())
    total = len(users)
    updated = 0
    skipped = 0
    errors = []
    client = OpenAI(api_key=settings.openai_api_key)

    for start in range(0, total, max(1, batch_size)):
        batch = users[start : start + batch_size]
        texts = [build_profile_embedding_text(user) for user in batch]
        needs_refresh = []
        for user, text in zip(batch, texts, strict=True):
            stored_embedding = ((user.get("matching") or {}).get("profile_embedding")) or {}
            stored_vector = stored_embedding.get("vector")
            if (
                stored_embedding.get("model") == settings.openai_embedding_model
                and stored_embedding.get("text") == text
                and isinstance(stored_vector, list)
                and stored_vector
            ):
                skipped += 1
            else:
                needs_refresh.append((user, text))

        if not needs_refresh:
            continue

        try:
            response = client.embeddings.create(
                model=settings.openai_embedding_model,
                input=[text for _, text in needs_refresh],
            )
        except RateLimitError as exc:
            raise HTTPException(status_code=503, detail="OpenAI quota exceeded for embedding backfill") from exc
        except APIError as exc:
            raise HTTPException(status_code=502, detail="OpenAI request failed during embedding backfill") from exc

        now = utc_now()
        for (user, text), embedding_data in zip(needs_refresh, response.data, strict=True):
            db.users.update_one(
                {"_id": user["_id"]},
                {
                    "$set": {
                        "matching.profile_embedding": {
                            "model": settings.openai_embedding_model,
                            "text": text,
                            "vector": embedding_data.embedding,
                            "updated_at": now,
                        }
                    },
                    "$unset": {"matching.profile_embedding_error": ""},
                },
            )
            updated += 1

    return {
        "total_users": total,
        "updated_users": updated,
        "skipped_users": skipped,
        "errors": errors,
    }


def _embedding_shortlist(
    current_user: dict[str, Any],
    candidate_users: list[dict[str, Any]],
    *,
    limit: int,
) -> list[tuple[dict[str, Any], float]]:
    current_embedding = ensure_profile_embedding(current_user)
    if not current_embedding:
        return [(candidate, 0.0) for candidate in candidate_users[: max(limit, 10)]]

    scored = []
    for candidate in candidate_users:
        candidate_embedding = ensure_profile_embedding(candidate)
        if not candidate_embedding:
            continue
        similarity = cosine_similarity(
            current_embedding["vector"],
            candidate_embedding["vector"],
        )
        scored.append((candidate, similarity))

    if not scored:
        return [(candidate, 0.0) for candidate in candidate_users[: max(limit, 10)]]

    scored.sort(key=lambda item: item[1], reverse=True)
    return scored[: max(limit * 3, 10)]


def cosine_similarity(vector_a: list[float], vector_b: list[float]) -> float:
    numerator = sum(left * right for left, right in zip(vector_a, vector_b, strict=False))
    magnitude_a = math.sqrt(sum(value * value for value in vector_a))
    magnitude_b = math.sqrt(sum(value * value for value in vector_b))
    if not magnitude_a or not magnitude_b:
        return 0.0
    return numerator / (magnitude_a * magnitude_b)


def embedding_score(similarity: float, shared_event_count: int) -> int:
    base_score = int(round(max(0.0, min(1.0, similarity)) * 100))
    bonus = min(shared_event_count * 3, 9)
    return max(0, min(100, base_score + bonus))


def build_embedding_reasoning(
    current_user: dict[str, Any],
    candidate: dict[str, Any],
    similarity: float,
    shared_event_count: int,
) -> str:
    current_tags = set(((current_user.get("persona") or {}).get("profile_dict") or {}).get("interest_tags", []))
    candidate_tags = set(((candidate.get("persona") or {}).get("profile_dict") or {}).get("interest_tags", []))
    overlap = sorted(current_tags & candidate_tags)
    overlap_text = ", ".join(overlap[:3]) if overlap else "nearby interests and profile language"
    shared_events_text = (
        f" You also overlap on {shared_event_count} joined events."
        if shared_event_count > 0
        else ""
    )
    return (
        f"Ranked from profile embedding similarity ({similarity:.2f}) with overlap around {overlap_text}."
        f"{shared_events_text}"
    )


def build_embedding_matches(current_user: dict[str, Any], candidate: dict[str, Any]) -> list[str]:
    current_tags = set(((current_user.get("persona") or {}).get("profile_dict") or {}).get("interest_tags", []))
    candidate_tags = set(((candidate.get("persona") or {}).get("profile_dict") or {}).get("interest_tags", []))
    shared_tags = sorted(current_tags & candidate_tags)
    matches = [f"Shared interest: {tag}" for tag in shared_tags[:3]]
    if not matches:
        candidate_headline = ((candidate.get("persona") or {}).get("mobile_profile") or {}).get("headline")
        if candidate_headline:
            matches.append(candidate_headline)
    return matches


def _shared_event_ids(user_one: dict[str, Any], user_two: dict[str, Any]) -> list[str]:
    first_joined = set((user_one.get("event_refs") or {}).get("joined_event_ids", []))
    second_joined = set((user_two.get("event_refs") or {}).get("joined_event_ids", []))
    return sorted(first_joined & second_joined)


def _shared_event_count(user_one: dict[str, Any], user_two: dict[str, Any]) -> int:
    return len(_shared_event_ids(user_one, user_two))
