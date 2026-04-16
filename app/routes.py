from fastapi import APIRouter, File, HTTPException, UploadFile
from pymongo.errors import DuplicateKeyError

from app.community import backfill_profile_embeddings, rank_community_candidates, refresh_profile_embedding_for_user
from app.db import db
from app.matching import build_event_payload, score_profiles, score_user_for_event, select_events_for_query
from app.profile_ai import (
    continue_profile_ai_session,
    end_profile_ai_session,
    start_profile_ai_session,
)
from app.models import (
    AudioTranscriptionResponse,
    ChatMessageCreate,
    DirectMessageCreate,
    EventCreate,
    EventMembershipCreate,
    EventPatch,
    ProfileAiEndResponse,
    ProfileAiTurnRequest,
    ProfileAiTurnResponse,
    UserCreate,
    UserGoogleAuth,
    UserPatch,
    UserSignIn,
)
from app.transcription import transcribe_audio_bytes
from app.utils import dm_thread_id, prefixed_id, utc_now

router = APIRouter()


def _build_thread_payload(thread: dict) -> dict:
    participant_user_ids = thread.get("participant_user_ids", [])
    participant_profiles = list(
        db.users.find(
            {"_id": {"$in": participant_user_ids}},
            {"_id": 1, "display_name": 1, "username": 1, "avatar_url": 1},
        )
    ) if participant_user_ids else []
    event = db.events.find_one(
        {"_id": thread.get("event_id")},
        {"_id": 1, "title": 1, "description": 1, "location.name": 1},
    ) if thread.get("event_id") else None
    messages = list(
        db.chat_messages.find(
            {"thread_id": thread["_id"]},
            {
                "_id": 1,
                "thread_id": 1,
                "thread_type": 1,
                "event_id": 1,
                "sender_type": 1,
                "sender_user_id": 1,
                "message_type": 1,
                "content_text": 1,
                "content_structured": 1,
                "created_at": 1,
                "edited_at": 1,
            },
        ).sort("created_at", 1)
    )
    return {
        "thread": {
            "thread_id": thread["_id"],
            "thread_type": thread["thread_type"],
            "owner_user_id": thread.get("owner_user_id"),
            "event_id": thread.get("event_id"),
            "participant_user_ids": participant_user_ids,
            "participant_profiles": participant_profiles,
            "status": thread.get("status"),
            "created_at": thread.get("created_at"),
            "updated_at": thread.get("updated_at"),
            "last_message_at": thread.get("last_message_at"),
            "title": event.get("title") if event else thread.get("title"),
            "subtitle": event.get("location", {}).get("name") if event else thread.get("subtitle"),
        },
        "items": messages,
    }


def _get_user_or_404(user_id: str) -> dict:
    user = db.users.find_one({"_id": user_id})
    if not user:
        raise HTTPException(status_code=404, detail="user not found")
    return user


def _get_event_or_404(event_id: str) -> dict:
    event = db.events.find_one({"_id": event_id})
    if not event:
        raise HTTPException(status_code=404, detail="event not found")
    return event


def _get_main_thread_or_404(user_id: str) -> dict:
    user = db.users.find_one({"_id": user_id}, {"main_thread_id": 1})
    if not user:
        raise HTTPException(status_code=404, detail="user not found")

    thread = db.chat_threads.find_one({"_id": user["main_thread_id"]})
    if not thread:
        raise HTTPException(status_code=404, detail="main chat thread not found")
    return thread


def _get_thread_or_404(thread_id: str) -> dict:
    thread = db.chat_threads.find_one({"_id": thread_id})
    if not thread:
        raise HTTPException(status_code=404, detail="thread not found")
    return thread


def _assert_thread_participant(thread: dict, user_id: str) -> None:
    if user_id not in thread.get("participant_user_ids", []):
        raise HTTPException(status_code=403, detail="user is not a participant in this thread")


def _get_event_context(event_id: str) -> tuple[dict, dict | None, list[dict], set[str]]:
    event = _get_event_or_404(event_id)
    creator = db.users.find_one({"_id": event.get("creator_user_id")}) if event.get("creator_user_id") else None
    memberships = list(db.event_memberships.find({"event_id": event_id}))
    attendee_ids = {membership["user_id"] for membership in memberships}
    attendees = list(db.users.find({"_id": {"$in": list(attendee_ids)}})) if attendee_ids else []
    return event, creator, attendees, attendee_ids


def _build_event_card_payload(event: dict, creator: dict | None, attendees: list[dict]) -> dict:
    location = event.get("location", {})
    timing = event.get("timing", {})
    attendee_cards = [
        {
            "display_name": attendee.get("display_name", attendee.get("_id")),
            "avatar_url": attendee.get("avatar_url"),
        }
        for attendee in attendees[:3]
    ]
    return {
        "card_type": "event_card",
        "event": {
            "event_id": event["_id"],
            "title": event.get("title"),
            "location_name": location.get("location_name") or location.get("name", ""),
            "maps_url": location.get("maps_url", ""),
            "description": event.get("description", ""),
            "attendees": attendee_cards,
            "attendee_count": event.get("attendance", {}).get("attendee_count", len(attendees)),
            "start_label": timing.get("label", ""),
            "category_tags": event.get("category_tags", []),
            "creator_name": creator.get("display_name") if creator else None,
        }
    }


def _build_event_search_candidates() -> list[dict]:
    candidates = []
    for event in db.events.find({"status": "published"}):
        creator, attendees = None, []
        if event.get("creator_user_id"):
            creator = db.users.find_one({"_id": event["creator_user_id"]})
        memberships = list(db.event_memberships.find({"event_id": event["_id"], "rsvp_status": "joined"}))
        attendee_ids = [membership["user_id"] for membership in memberships]
        if attendee_ids:
            attendees = list(db.users.find({"_id": {"$in": attendee_ids}}))
        candidates.append(build_event_payload(event, creator, attendees))
    return candidates


def _build_chat_search_response(user_id: str, thread: dict, query: str) -> dict:
    user = _get_user_or_404(user_id)
    user_message = _create_chat_message(thread, ChatMessageCreate(
        sender_type="user",
        sender_user_id=user_id,
        message_type="text",
        content_text=query,
        content_structured=None,
    ))
    candidates = _build_event_search_candidates()
    search_result = select_events_for_query(user=user, query=query, events=candidates)
    matched_event_ids = search_result.get("matched_event_ids", [])

    assistant_messages = []
    assistant_text = search_result.get("response_text", "").strip()
    if matched_event_ids:
        if not assistant_text:
            assistant_text = "Here are the events that look most relevant."
        assistant_messages.append(
            _create_chat_message(
                thread,
                ChatMessageCreate(
                    sender_type="assistant",
                    sender_user_id=None,
                    message_type="text",
                    content_text=assistant_text,
                    content_structured=None,
                ),
            )
        )
        for event_id in matched_event_ids[:3]:
            event, creator, attendees, _ = _get_event_context(event_id)
            assistant_messages.append(
                _create_chat_message(
                    thread,
                    ChatMessageCreate(
                        sender_type="assistant",
                        sender_user_id=None,
                        message_type="event_card",
                        content_text="",
                        content_structured=_build_event_card_payload(event, creator, attendees),
                    ),
                )
            )
    else:
        assistant_messages.append(
            _create_chat_message(
                thread,
                ChatMessageCreate(
                    sender_type="assistant",
                    sender_user_id=None,
                    message_type="text",
                    content_text="Nothing seems to match that. Do you want to host something yourself? Use /create to host.",
                    content_structured=None,
                ),
            )
        )

    return {
        "user_message": user_message,
        "messages": assistant_messages,
    }


def _serialize_profile_ai_transcript(messages: list) -> list[dict[str, str]]:
    return [
        {
            "role": message.role,
            "text": message.text,
        }
        for message in messages
    ]


def _get_profile_dict_from_user(user: dict | None) -> dict:
    if not user:
        return {}
    persona = user.get("persona") or {}
    if isinstance(persona.get("profile_dict"), dict):
        return persona["profile_dict"]
    return persona if isinstance(persona, dict) else {}


@router.get("/health")
def healthcheck() -> dict[str, str]:
    return {"status": "ok"}


@router.post("/auth/signup")
def signup(payload: UserCreate) -> dict:
    user_id = prefixed_id("usr")
    now = utc_now()
    document = {
        "_id": user_id,
        "email": payload.email,
        "password": payload.password,
        "auth_providers": payload.auth_providers,
        "google_id": payload.google_id,
        "display_name": payload.display_name,
        "username": payload.username,
        "avatar_url": payload.avatar_url,
        "account_status": "active",
        "persona": {},
        "connections": [],
        "event_refs": {
            "hosted_event_ids": [],
            "joined_event_ids": [],
            "attended_event_ids": [],
        },
        "main_thread_id": f"thread_main_{user_id}",
        "last_login_at": now,
    }

    try:
        db.users.insert_one(document)
    except DuplicateKeyError as exc:
        raise HTTPException(status_code=409, detail="email already exists") from exc

    db.chat_threads.insert_one(
        {
            "_id": document["main_thread_id"],
            "thread_type": "dm_with_system",
            "owner_user_id": user_id,
            "event_id": None,
            "participant_user_ids": [user_id],
            "status": "active",
            "created_at": now,
            "updated_at": now,
            "last_message_at": now,
        }
    )
    return document


@router.post("/auth/signin")
def signin(payload: UserSignIn) -> dict:
    user = db.users.find_one({"email": payload.email})
    if not user or user.get("password") != payload.password:
        raise HTTPException(status_code=401, detail="invalid credentials")

    now = utc_now()
    db.users.update_one({"_id": user["_id"]}, {"$set": {"last_login_at": now}})
    user["last_login_at"] = now
    return user


@router.post("/auth/google")
def google_auth(payload: UserGoogleAuth) -> dict:
    user = db.users.find_one({"google_id": payload.google_id})
    now = utc_now()
    if user:
        db.users.update_one(
            {"_id": user["_id"]},
            {
                "$set": {
                    "email": payload.email,
                    "display_name": payload.display_name,
                    "username": payload.username,
                    "avatar_url": payload.avatar_url,
                    "last_login_at": now,
                }
            },
        )
        user.update(
            {
                "email": payload.email,
                "display_name": payload.display_name,
                "username": payload.username,
                "avatar_url": payload.avatar_url,
                "last_login_at": now,
            }
        )
        return user

    user_id = prefixed_id("usr")
    document = {
        "_id": user_id,
        "email": payload.email,
        "password": None,
        "auth_providers": ["google"],
        "google_id": payload.google_id,
        "display_name": payload.display_name,
        "username": payload.username,
        "avatar_url": payload.avatar_url,
        "account_status": "active",
        "persona": {},
        "connections": [],
        "event_refs": {
            "hosted_event_ids": [],
            "joined_event_ids": [],
            "attended_event_ids": [],
        },
        "main_thread_id": f"thread_main_{user_id}",
        "last_login_at": now,
    }
    try:
        db.users.insert_one(document)
    except DuplicateKeyError as exc:
        raise HTTPException(status_code=409, detail="email already exists") from exc

    db.chat_threads.insert_one(
        {
            "_id": document["main_thread_id"],
            "thread_type": "dm_with_system",
            "owner_user_id": user_id,
            "event_id": None,
            "participant_user_ids": [user_id],
            "status": "active",
            "created_at": now,
            "updated_at": now,
            "last_message_at": now,
        }
    )
    return document


@router.get("/users/{user_id}")
def get_user(user_id: str) -> dict:
    return _get_user_or_404(user_id)


@router.get("/mobile/bootstrap/{user_id}")
def get_mobile_bootstrap(user_id: str) -> dict:
    user = _get_user_or_404(user_id)
    thread = _get_main_thread_or_404(user_id)
    return {
        "current_user": user,
        "main_chat": _build_thread_payload(thread),
    }


@router.patch("/users/{user_id}")
def update_user(user_id: str, payload: UserPatch) -> dict:
    _get_user_or_404(user_id)

    updates = payload.model_dump(exclude_none=True)
    if updates:
        db.users.update_one({"_id": user_id}, {"$set": updates})
        if "persona" in updates or "display_name" in updates or "username" in updates:
            refresh_profile_embedding_for_user(user_id)
    return db.users.find_one({"_id": user_id})


@router.post("/users/{user_id}/profile-ai/start", response_model=ProfileAiTurnResponse)
def start_profile_ai(user_id: str, payload: ProfileAiTurnRequest) -> dict:
    user = db.users.find_one({"_id": user_id}, {"display_name": 1, "persona": 1})
    current_profile_dict = payload.current_profile_dict or _get_profile_dict_from_user(user)
    display_name = payload.display_name or (user.get("display_name") if user else None)
    return start_profile_ai_session(
        display_name=display_name,
        current_profile_dict=current_profile_dict,
        transcript=_serialize_profile_ai_transcript(payload.transcript),
    )


@router.post("/users/{user_id}/profile-ai/reply", response_model=ProfileAiTurnResponse)
def reply_profile_ai(user_id: str, payload: ProfileAiTurnRequest) -> dict:
    user = db.users.find_one({"_id": user_id}, {"display_name": 1})
    user_message = (payload.user_message or "").strip()
    if not user_message:
        raise HTTPException(status_code=400, detail="user_message is required")
    return continue_profile_ai_session(
        display_name=payload.display_name or (user.get("display_name") if user else None),
        current_profile_dict=payload.current_profile_dict,
        transcript=_serialize_profile_ai_transcript(payload.transcript),
        user_message=user_message,
    )


@router.post("/users/{user_id}/profile-ai/transcribe", response_model=AudioTranscriptionResponse)
async def transcribe_profile_ai_audio(user_id: str, file: UploadFile = File(...)) -> dict[str, str]:
    _get_user_or_404(user_id)
    audio_bytes = await file.read()
    return transcribe_audio_bytes(
        filename=file.filename or "profile-ai.m4a",
        content_type=file.content_type,
        audio_bytes=audio_bytes,
    )


@router.post("/users/{user_id}/profile-ai/end", response_model=ProfileAiEndResponse)
def end_profile_ai(user_id: str, payload: ProfileAiTurnRequest) -> dict:
    user = db.users.find_one({"_id": user_id}, {"display_name": 1})
    result = end_profile_ai_session(
        display_name=payload.display_name or (user.get("display_name") if user else None),
        current_profile_dict=payload.current_profile_dict,
        transcript=_serialize_profile_ai_transcript(payload.transcript),
    )
    if user:
        db.users.update_one(
            {"_id": user_id},
            {"$set": {"persona.profile_dict": result["final_profile_dict"]}},
        )
        refresh_profile_embedding_for_user(user_id)
    return result


@router.post("/users/direct-message")
def create_or_get_direct_message(payload: DirectMessageCreate) -> dict:
    user = _get_user_or_404(payload.user_id)
    other_user = _get_user_or_404(payload.other_user_id)

    thread_id = dm_thread_id(payload.user_id, payload.other_user_id)
    existing_thread = db.chat_threads.find_one({"_id": thread_id})
    now = utc_now()
    if not existing_thread:
        db.chat_threads.insert_one(
            {
                "_id": thread_id,
                "thread_type": "direct_message",
                "owner_user_id": None,
                "event_id": None,
                "participant_user_ids": [payload.user_id, payload.other_user_id],
                "title": other_user.get("display_name"),
                "subtitle": other_user.get("username"),
                "status": "active",
                "created_at": now,
                "updated_at": now,
                "last_message_at": now,
            }
        )

    for current_user_id, peer_user_id in (
        (payload.user_id, payload.other_user_id),
        (payload.other_user_id, payload.user_id),
    ):
        db.users.update_one(
            {"_id": current_user_id, "connections.user_id": {"$ne": peer_user_id}},
            {
                "$push": {
                    "connections": {
                        "user_id": peer_user_id,
                        "dm_thread_id": thread_id,
                        "connection_type": "explicit_chat",
                        "last_interaction_at": now,
                    }
                }
            },
        )

    return _build_thread_payload(db.chat_threads.find_one({"_id": thread_id}))


@router.post("/events")
def create_event(payload: EventCreate) -> dict:
    _get_user_or_404(payload.creator_user_id)

    event_id = prefixed_id("evt")
    thread_id = f"thread_event_{event_id}"
    now = utc_now()
    document = {
        "_id": event_id,
        "creator_user_id": payload.creator_user_id,
        "title": payload.title,
        "description": payload.description,
        "category_tags": payload.category_tags,
        "status": "published",
        "visibility_type": payload.visibility_type,
        "created_via": payload.created_via,
        "timing": payload.timing,
        "location": payload.location,
        "capacity": payload.capacity or {},
        "attendance": {
            "attendee_count": 1,
            "confirmed_count": 1,
            "maybe_count": 0,
            "declined_count": 0,
        },
        "routing": payload.routing or {},
        "chat": {
            "thread_id": thread_id,
            "status": "active",
            "expires_at": None,
            "participant_user_ids": [payload.creator_user_id],
        },
        "source": payload.source or {},
        "created_at": now,
        "updated_at": now,
    }
    db.events.insert_one(document)

    db.chat_threads.insert_one(
        {
            "_id": thread_id,
            "thread_type": "event_chat",
            "owner_user_id": None,
            "event_id": event_id,
            "participant_user_ids": [payload.creator_user_id],
            "status": "active",
            "created_at": now,
            "updated_at": now,
            "last_message_at": now,
        }
    )

    db.event_memberships.insert_one(
        {
            "_id": prefixed_id("mem"),
            "event_id": event_id,
            "user_id": payload.creator_user_id,
            "role": "creator",
            "rsvp_status": "joined",
            "discovery_source": "direct_chat",
            "joined_at": now,
            "attended_at": None,
            "left_at": None,
        }
    )

    db.users.update_one(
        {"_id": payload.creator_user_id},
        {
            "$addToSet": {
                "event_refs.hosted_event_ids": event_id,
                "event_refs.joined_event_ids": event_id,
            }
        },
    )

    return document


@router.get("/events/{event_id}")
def get_event(event_id: str) -> dict:
    return _get_event_or_404(event_id)


@router.patch("/events/{event_id}")
def update_event(event_id: str, payload: EventPatch) -> dict:
    _get_event_or_404(event_id)

    updates = payload.model_dump(exclude_none=True)
    updates["updated_at"] = utc_now()
    db.events.update_one({"_id": event_id}, {"$set": updates})
    return db.events.find_one({"_id": event_id})


@router.post("/events/{event_id}/join")
def join_event(event_id: str, payload: EventMembershipCreate) -> dict:
    event = db.events.find_one({"_id": event_id})
    user = db.users.find_one({"_id": payload.user_id})
    if not event or not user:
        raise HTTPException(status_code=404, detail="event or user not found")

    now = utc_now()
    existing_membership = db.event_memberships.find_one({"event_id": event_id, "user_id": payload.user_id})
    was_joined = existing_membership and existing_membership.get("rsvp_status") == "joined"
    db.event_memberships.update_one(
        {"event_id": event_id, "user_id": payload.user_id},
        {
            "$setOnInsert": {
                "_id": prefixed_id("mem"),
                "event_id": event_id,
                "user_id": payload.user_id,
                "joined_at": now,
                "attended_at": None,
                "left_at": None,
            },
            "$set": {
                "role": payload.role,
                "rsvp_status": payload.rsvp_status,
                "discovery_source": payload.discovery_source,
            },
        },
        upsert=True,
    )
    db.users.update_one(
        {"_id": payload.user_id},
        {"$addToSet": {"event_refs.joined_event_ids": event_id}},
    )
    db.events.update_one(
        {"_id": event_id},
        {
            "$addToSet": {"chat.participant_user_ids": payload.user_id},
            "$set": {"updated_at": now},
        },
    )
    if not was_joined and payload.rsvp_status == "joined":
        db.events.update_one(
            {"_id": event_id},
            {"$inc": {"attendance.attendee_count": 1, "attendance.confirmed_count": 1}},
        )
    db.chat_threads.update_one(
        {"_id": event["chat"]["thread_id"]},
        {
            "$addToSet": {"participant_user_ids": payload.user_id},
            "$set": {"updated_at": now},
        },
    )
    return db.event_memberships.find_one({"event_id": event_id, "user_id": payload.user_id})


@router.get("/users/{user_id}/events")
def get_user_events(user_id: str) -> dict:
    user = db.users.find_one({"_id": user_id}, {"event_refs": 1})
    if not user:
        raise HTTPException(status_code=404, detail="user not found")
    return user.get("event_refs", {})


@router.get("/events/{event_id}/attendees")
def get_event_attendees(event_id: str) -> list[dict]:
    memberships = list(db.event_memberships.find({"event_id": event_id}, {"_id": 0}))
    user_ids = [membership["user_id"] for membership in memberships]
    users = {
        user["_id"]: user
        for user in db.users.find({"_id": {"$in": user_ids}}, {"display_name": 1, "avatar_url": 1})
    } if user_ids else {}
    return [
        {
            **membership,
            "display_name": users.get(membership["user_id"], {}).get("display_name", membership["user_id"]),
            "avatar_url": users.get(membership["user_id"], {}).get("avatar_url"),
        }
        for membership in memberships
    ]


def _create_chat_message(thread: dict, payload: ChatMessageCreate) -> dict:
    now = utc_now()
    if payload.sender_user_id and payload.sender_user_id not in thread.get("participant_user_ids", []):
        raise HTTPException(status_code=403, detail="sender is not a participant in this thread")
    document = {
        "_id": prefixed_id("msg"),
        "thread_id": thread["_id"],
        "thread_type": thread["thread_type"],
        "event_id": thread.get("event_id"),
        "sender_type": payload.sender_type,
        "sender_user_id": payload.sender_user_id,
        "message_type": payload.message_type,
        "content_text": payload.content_text,
        "content_structured": payload.content_structured,
        "created_at": now,
        "edited_at": None,
    }
    db.chat_messages.insert_one(document)
    db.chat_threads.update_one(
        {"_id": thread["_id"]},
        {"$set": {"updated_at": now, "last_message_at": now}},
    )
    return document


@router.get("/community/{user_id}")
def get_ranked_community(user_id: str, limit: int = 10) -> dict:
    current_user = _get_user_or_404(user_id)
    candidate_users = list(
        db.users.find(
            {
                "_id": {"$ne": user_id},
            }
        )
    )
    return rank_community_candidates(current_user, candidate_users, limit=max(1, min(limit, 20)))


@router.post("/users/backfill-profile-embeddings")
def backfill_profile_embeddings_route(batch_size: int = 16) -> dict:
    return backfill_profile_embeddings(batch_size=batch_size)


@router.get("/chat/{user_id}")
def get_main_chat(user_id: str) -> dict:
    thread = _get_main_thread_or_404(user_id)
    return _build_thread_payload(thread)


@router.post("/chat/{user_id}/messages")
def create_main_chat_message(user_id: str, payload: ChatMessageCreate) -> dict:
    thread = _get_main_thread_or_404(user_id)
    if payload.sender_type == "user" and payload.message_type == "text":
        content_text = (payload.content_text or "").strip()
        if content_text and content_text != "/create":
            return _build_chat_search_response(user_id, thread, content_text)
    return _create_chat_message(thread, payload)


@router.post("/chat/{user_id}/transcribe", response_model=AudioTranscriptionResponse)
async def transcribe_main_chat_audio(user_id: str, file: UploadFile = File(...)) -> dict[str, str]:
    _get_user_or_404(user_id)
    audio_bytes = await file.read()
    return transcribe_audio_bytes(
        filename=file.filename or "voice-note.m4a",
        content_type=file.content_type,
        audio_bytes=audio_bytes,
    )


@router.get("/threads/{thread_id}")
def get_thread(thread_id: str, viewer_user_id: str) -> dict:
    thread = _get_thread_or_404(thread_id)
    _assert_thread_participant(thread, viewer_user_id)
    return _build_thread_payload(thread)


@router.post("/threads/{thread_id}/messages")
def create_thread_message(thread_id: str, payload: ChatMessageCreate) -> dict:
    thread = _get_thread_or_404(thread_id)
    return _create_chat_message(thread, payload)


@router.get("/events/{event_id}/chat")
def get_event_chat(event_id: str) -> dict:
    event = db.events.find_one({"_id": event_id}, {"chat.thread_id": 1})
    if not event:
        raise HTTPException(status_code=404, detail="event not found")

    thread = db.chat_threads.find_one({"_id": event["chat"]["thread_id"]})
    if not thread:
        raise HTTPException(status_code=404, detail="event chat thread not found")
    return _build_thread_payload(thread)


@router.post("/events/{event_id}/chat/messages")
def create_event_chat_message(event_id: str, payload: ChatMessageCreate) -> dict:
    event = db.events.find_one({"_id": event_id}, {"chat.thread_id": 1})
    if not event:
        raise HTTPException(status_code=404, detail="event not found")

    thread = db.chat_threads.find_one({"_id": event["chat"]["thread_id"]})
    if not thread:
        raise HTTPException(status_code=404, detail="event chat thread not found")
    return _create_chat_message(thread, payload)


@router.get("/events/{event_id}/score/{user_id}")
def score_event_for_user_route(event_id: str, user_id: str) -> dict:
    user = _get_user_or_404(user_id)
    event, creator, attendees, _ = _get_event_context(event_id)
    return score_user_for_event(user, event, creator, attendees)


@router.post("/events/{event_id}/broadcast")
def broadcast_event_to_best_matches(event_id: str, limit: int = 5) -> dict:
    event, creator, attendees, attendee_ids = _get_event_context(event_id)
    already_broadcast_ids = {
        broadcast["user_id"]
        for broadcast in db.event_broadcasts.find({"event_id": event_id}, {"user_id": 1})
    }
    excluded_user_ids = set(attendee_ids) | already_broadcast_ids
    if creator:
        excluded_user_ids.add(creator["_id"])

    candidate_users = list(
        db.users.find(
            {
                "_id": {
                    "$nin": list(excluded_user_ids),
                }
            }
        )
    )

    scored_candidates = []
    top_n = max(1, min(limit, 50))
    now = utc_now()
    for candidate in candidate_users:
        score = score_user_for_event(candidate, event, creator, attendees)
        scored_candidates.append(
            {
                "user_id": candidate["_id"],
                "display_name": candidate.get("display_name"),
                "username": candidate.get("username"),
                "avatar_url": candidate.get("avatar_url"),
                "score": score["score"],
                "reasoning": score["reasoning"],
                "what_matches": score["what_matches"],
                "what_does_not_match": score["what_does_not_match"],
            }
        )

    scored_candidates.sort(key=lambda item: item["score"], reverse=True)
    selected_candidates = scored_candidates[:top_n]

    for candidate in selected_candidates:
        db.event_broadcasts.update_one(
            {"event_id": event_id, "user_id": candidate["user_id"]},
            {
                "$set": {
                    "score": candidate["score"],
                    "broadcast_reason": candidate["reasoning"],
                    "broadcasted_at": now,
                    "conversion_status": "unseen",
                },
                "$setOnInsert": {
                    "_id": prefixed_id("brd"),
                    "seen_at": None,
                    "clicked_at": None,
                    "dismissed_at": None,
                },
            },
            upsert=True,
        )

    return {
        "event_id": event_id,
        "evaluated_candidates": len(scored_candidates),
        "broadcasted_candidates": len(selected_candidates),
        "results": selected_candidates,
    }


@router.get("/connections/score/{user_1}/{user_2}")
def get_connection_score(user_1: str, user_2: str) -> dict:
    first_user = _get_user_or_404(user_1)
    second_user = _get_user_or_404(user_2)
    return score_profiles(first_user, second_user)


@router.get("/connections/findnew/{user_1}")
def find_new_connections(user_1: str, limit: int = 5) -> dict:
    current_user = _get_user_or_404(user_1)
    existing_connection_ids = {connection["user_id"] for connection in current_user.get("connections", [])}
    candidate_users = list(
        db.users.find(
            {
                "_id": {
                    "$ne": user_1,
                    "$nin": list(existing_connection_ids),
                }
            }
        )
    )

    scored_candidates = []
    for candidate in candidate_users:
        score = score_profiles(current_user, candidate)
        scored_candidates.append(
            {
                "user_id": candidate["_id"],
                "display_name": candidate.get("display_name"),
                "username": candidate.get("username"),
                "avatar_url": candidate.get("avatar_url"),
                "score": score["score"],
                "reasoning": score["reasoning"],
                "what_matches": score["what_matches"],
                "what_does_not_match": score["what_does_not_match"],
            }
        )

    scored_candidates.sort(key=lambda item: item["score"], reverse=True)
    return {
        "user_id": user_1,
        "evaluated_candidates": len(scored_candidates),
        "results": scored_candidates[: max(1, min(limit, 20))],
    }
