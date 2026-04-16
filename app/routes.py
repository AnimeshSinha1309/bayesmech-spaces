from fastapi import APIRouter, HTTPException
from pymongo.errors import DuplicateKeyError

from app.db import db
from app.models import (
    ChatMessageCreate,
    DirectMessageCreate,
    EventCreate,
    EventMembershipCreate,
    EventPatch,
    UserCreate,
    UserGoogleAuth,
    UserPatch,
    UserSignIn,
)
from app.utils import dm_thread_id, prefixed_id, utc_now

router = APIRouter()


def _build_thread_payload(thread: dict) -> dict:
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
            "participant_user_ids": thread.get("participant_user_ids", []),
            "status": thread.get("status"),
            "created_at": thread.get("created_at"),
            "updated_at": thread.get("updated_at"),
            "last_message_at": thread.get("last_message_at"),
        },
        "items": messages,
    }


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
    user = db.users.find_one({"_id": user_id})
    if not user:
        raise HTTPException(status_code=404, detail="user not found")
    return user


@router.patch("/users/{user_id}")
def update_user(user_id: str, payload: UserPatch) -> dict:
    existing = db.users.find_one({"_id": user_id})
    if not existing:
        raise HTTPException(status_code=404, detail="user not found")

    updates = payload.model_dump(exclude_none=True)
    if updates:
        db.users.update_one({"_id": user_id}, {"$set": updates})
    return db.users.find_one({"_id": user_id})


@router.post("/users/direct-message")
def create_or_get_direct_message(payload: DirectMessageCreate) -> dict:
    user = db.users.find_one({"_id": payload.user_id})
    other_user = db.users.find_one({"_id": payload.other_user_id})
    if not user or not other_user:
        raise HTTPException(status_code=404, detail="user not found")

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

    return db.chat_threads.find_one({"_id": thread_id})


@router.post("/events")
def create_event(payload: EventCreate) -> dict:
    creator = db.users.find_one({"_id": payload.creator_user_id})
    if not creator:
        raise HTTPException(status_code=404, detail="creator not found")

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
    event = db.events.find_one({"_id": event_id})
    if not event:
        raise HTTPException(status_code=404, detail="event not found")
    return event


@router.patch("/events/{event_id}")
def update_event(event_id: str, payload: EventPatch) -> dict:
    event = db.events.find_one({"_id": event_id})
    if not event:
        raise HTTPException(status_code=404, detail="event not found")

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
            "$inc": {"attendance.attendee_count": 1, "attendance.confirmed_count": 1},
            "$set": {"updated_at": now},
        },
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
    return list(db.event_memberships.find({"event_id": event_id}, {"_id": 0}))


def _create_chat_message(thread: dict, payload: ChatMessageCreate) -> dict:
    now = utc_now()
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


@router.get("/chat/{user_id}")
def get_main_chat(user_id: str) -> dict:
    user = db.users.find_one({"_id": user_id}, {"main_thread_id": 1})
    if not user:
        raise HTTPException(status_code=404, detail="user not found")

    thread = db.chat_threads.find_one({"_id": user["main_thread_id"]})
    if not thread:
        raise HTTPException(status_code=404, detail="main chat thread not found")
    return _build_thread_payload(thread)


@router.post("/chat/{user_id}/messages")
def create_main_chat_message(user_id: str, payload: ChatMessageCreate) -> dict:
    user = db.users.find_one({"_id": user_id}, {"main_thread_id": 1})
    if not user:
        raise HTTPException(status_code=404, detail="user not found")

    thread = db.chat_threads.find_one({"_id": user["main_thread_id"]})
    if not thread:
        raise HTTPException(status_code=404, detail="main chat thread not found")
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
