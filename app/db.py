from pymongo import ASCENDING, MongoClient

from app.config import settings


client = MongoClient(settings.mongodb_uri)
db = client[settings.mongodb_db_name]


def ensure_indexes() -> None:
    db.users.create_index("email", unique=True)
    db.users.create_index("google_id", sparse=True)
    db.users.create_index("username", sparse=True)

    db.events.create_index("creator_user_id")
    db.events.create_index("timing.start_time")
    db.events.create_index("status")
    db.events.create_index("category_tags")

    db.event_memberships.create_index(
        [("event_id", ASCENDING), ("user_id", ASCENDING)],
        unique=True,
    )
    db.event_memberships.create_index([("user_id", ASCENDING), ("rsvp_status", ASCENDING)])
    db.event_memberships.create_index([("event_id", ASCENDING), ("rsvp_status", ASCENDING)])

    db.event_broadcasts.create_index(
        [("event_id", ASCENDING), ("user_id", ASCENDING)],
        unique=True,
    )
    db.event_broadcasts.create_index([("user_id", ASCENDING), ("broadcasted_at", ASCENDING)])

    db.chat_messages.create_index([("thread_id", ASCENDING), ("created_at", ASCENDING)])
    db.community_edges.create_index([("user_id", ASCENDING), ("last_interaction_at", ASCENDING)])
