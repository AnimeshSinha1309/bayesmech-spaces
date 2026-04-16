from app.db import ensure_indexes, db


CURRENT_USER_ID = "user-animesh"
CURRENT_USER_EMAIL = "animesh@example.com"
CURRENT_USER_THREAD_ID = f"thread_main_{CURRENT_USER_ID}"
EVENT_ID = "evt-rooftop-acoustic-session"
EVENT_THREAD_ID = f"thread_event_{EVENT_ID}"


def upsert_user(document: dict) -> None:
    db.users.update_one({"_id": document["_id"]}, {"$set": document}, upsert=True)


def upsert_chat_thread(document: dict) -> None:
    db.chat_threads.update_one({"_id": document["_id"]}, {"$set": document}, upsert=True)


def upsert_chat_message(document: dict) -> None:
    db.chat_messages.update_one({"_id": document["_id"]}, {"$set": document}, upsert=True)


def upsert_event(document: dict) -> None:
    db.events.update_one({"_id": document["_id"]}, {"$set": document}, upsert=True)


def upsert_event_membership(document: dict) -> None:
    db.event_memberships.update_one({"_id": document["_id"]}, {"$set": document}, upsert=True)


def build_attendees() -> list[dict]:
    attendee_names = [
        "Nikhil",
        "Rhea",
        "Sana",
        "Aarav",
        "Meera",
        "Kabir",
        "Ira",
        "Dev",
        "Maya",
        "Arjun",
        "Tara",
        "Neil",
        "Aditi",
        "Rohan",
        "Leah",
        "Zoya",
    ]
    attendees = []
    for index, name in enumerate(attendee_names, start=1):
        user_id = f"usr-attendee-{index:02d}"
        attendees.append(
            {
                "_id": user_id,
                "email": f"{user_id}@example.com",
                "password": "seeded-password",
                "auth_providers": ["seed"],
                "google_id": None,
                "display_name": name,
                "username": name.lower(),
                "avatar_url": None,
                "account_status": "active",
                "persona": {
                    "seeded_for": "android_demo",
                },
                "connections": [],
                "event_refs": {
                    "hosted_event_ids": [],
                    "joined_event_ids": [EVENT_ID],
                    "attended_event_ids": [],
                },
                "main_thread_id": f"thread_main_{user_id}",
                "last_login_at": "2026-04-16T03:30:00Z",
            }
        )
    return attendees


def main() -> None:
    ensure_indexes()

    current_user = {
        "_id": CURRENT_USER_ID,
        "email": CURRENT_USER_EMAIL,
        "password": "seeded-password",
        "auth_providers": ["seed"],
        "google_id": None,
        "display_name": "Animesh",
        "username": "animesh",
        "avatar_url": None,
        "account_status": "active",
        "persona": {
            "mobile_profile": {
                "headline": "Curious builder, runner, and low-pressure social planner",
                "location_label": "Bengaluru",
                "avatar_fallback_prompt": (
                    "A warm Studio Ghibli-inspired portrait of Animesh with a thoughtful expression, "
                    "soft paper-toned palette, subtle city background, and friendly social energy."
                ),
                "interests_summary": (
                    "I'm drawn to things that feel alive, thoughtful, and a little textured. "
                    "The best events for me usually combine movement, strong taste, or conversations "
                    "with people who actually care about what they're doing."
                ),
                "journey": [
                    {
                        "title": "Where I'm from",
                        "body": (
                            "I'm from India, and a lot of how I read people and places comes from "
                            "growing up around density, ambition, and constant motion. I learned early "
                            "how to tell the difference between energy that feels alive and noise that just drains you."
                        ),
                    },
                    {
                        "title": "Where I went to school",
                        "body": (
                            "School made me interested in how ideas connect across disciplines. I was always "
                            "more interested in environments that made me curious and conversations that changed "
                            "how I saw things than in following one narrow track."
                        ),
                    },
                    {
                        "title": "Where I work",
                        "body": (
                            "I work on technology and product problems, which means I spend a lot of time thinking "
                            "about behavior, interfaces, and what makes people show up for something. What I find cool "
                            "is thoughtful ambition, good taste, and people building things with real intent. In my free "
                            "time I run, walk through the city, find small music gatherings, and look for conversations "
                            "that leave me sharper than before."
                        ),
                    },
                ],
                "interests": [
                    {
                        "tag": "#running",
                        "description": (
                            "Running 10Ks keeps me grounded. I ran a 10K in Cubbon Park, I like weekend loops "
                            "around Ulsoor Lake, and I'm usually looking for small group runs that feel social "
                            "without turning into a spectacle."
                        ),
                    },
                    {
                        "tag": "#startups",
                        "description": (
                            "I like conversations about product taste, distribution, and odd early-stage decisions. "
                            "Small founder dinners and operator meetups are much more interesting to me than loud networking events."
                        ),
                    },
                    {
                        "tag": "#music",
                        "description": (
                            "I keep coming back to intimate live sets, especially acoustic sessions and smaller rooms "
                            "where you can actually listen. I like events where the music shapes the mood instead of overpowering it."
                        ),
                    },
                    {
                        "tag": "#design",
                        "description": (
                            "I'm drawn to people who care about details, language, and how something feels in use. "
                            "Design critiques, product conversations, and spaces with strong visual taste almost always pull me in."
                        ),
                    },
                    {
                        "tag": "#citywalks",
                        "description": (
                            "I like exploring neighborhoods on foot, finding coffee spots, and letting conversations unfold while walking. "
                            "Unstructured city time often leads to the best kinds of plans."
                        ),
                    },
                ],
            }
        },
        "connections": [],
        "event_refs": {
            "hosted_event_ids": [EVENT_ID],
            "joined_event_ids": [EVENT_ID],
            "attended_event_ids": [],
        },
        "main_thread_id": CURRENT_USER_THREAD_ID,
        "last_login_at": "2026-04-16T03:35:00Z",
    }
    upsert_user(current_user)

    for attendee in build_attendees():
        upsert_user(attendee)
        upsert_chat_thread(
            {
                "_id": attendee["main_thread_id"],
                "thread_type": "dm_with_system",
                "owner_user_id": attendee["_id"],
                "event_id": None,
                "participant_user_ids": [attendee["_id"]],
                "status": "active",
                "created_at": "2026-04-16T03:20:00Z",
                "updated_at": "2026-04-16T03:20:00Z",
                "last_message_at": "2026-04-16T03:20:00Z",
            }
        )

    upsert_chat_thread(
        {
            "_id": CURRENT_USER_THREAD_ID,
            "thread_type": "dm_with_system",
            "owner_user_id": CURRENT_USER_ID,
            "event_id": None,
            "participant_user_ids": [CURRENT_USER_ID],
            "status": "active",
            "created_at": "2026-04-16T03:32:00Z",
            "updated_at": "2026-04-16T03:35:00Z",
            "last_message_at": "2026-04-16T03:35:00Z",
        }
    )

    upsert_event(
        {
            "_id": EVENT_ID,
            "creator_user_id": CURRENT_USER_ID,
            "title": "Rooftop Acoustic Session",
            "description": (
                "An intimate acoustic evening on a rooftop with stripped-back sets, a small crowd, "
                "and enough room to actually talk between songs."
            ),
            "category_tags": ["music", "acoustic", "social"],
            "status": "published",
            "visibility_type": "targeted",
            "created_via": "chat",
            "timing": {
                "start_time": "2026-04-16T13:30:00Z",
                "timezone": "Asia/Kolkata",
                "label": "Tonight, 7:00 PM",
            },
            "location": {
                "name": "Dialogues Cafe, Koramangala",
                "maps_url": "https://maps.google.com/?q=Dialogues+Cafe+Koramangala+Bengaluru",
            },
            "capacity": {
                "max_attendees": 24,
            },
            "attendance": {
                "attendee_count": 16,
                "confirmed_count": 16,
                "maybe_count": 0,
                "declined_count": 0,
            },
            "routing": {},
            "chat": {
                "thread_id": EVENT_THREAD_ID,
                "status": "active",
                "expires_at": None,
                "participant_user_ids": [CURRENT_USER_ID] + [f"usr-attendee-{index:02d}" for index in range(1, 17)],
            },
            "source": {
                "seeded_from": "android_mock_data",
            },
            "created_at": "2026-04-16T03:33:00Z",
            "updated_at": "2026-04-16T03:35:00Z",
        }
    )

    upsert_chat_thread(
        {
            "_id": EVENT_THREAD_ID,
            "thread_type": "event_chat",
            "owner_user_id": None,
            "event_id": EVENT_ID,
            "participant_user_ids": [CURRENT_USER_ID] + [f"usr-attendee-{index:02d}" for index in range(1, 17)],
            "status": "active",
            "created_at": "2026-04-16T03:33:00Z",
            "updated_at": "2026-04-16T03:35:00Z",
            "last_message_at": "2026-04-16T03:35:00Z",
        }
    )

    upsert_event_membership(
        {
            "_id": "mem-current-user-event",
            "event_id": EVENT_ID,
            "user_id": CURRENT_USER_ID,
            "role": "creator",
            "rsvp_status": "joined",
            "discovery_source": "direct_chat",
            "joined_at": "2026-04-16T03:33:00Z",
            "attended_at": None,
            "left_at": None,
        }
    )

    for index in range(1, 17):
        upsert_event_membership(
            {
                "_id": f"mem-attendee-{index:02d}",
                "event_id": EVENT_ID,
                "user_id": f"usr-attendee-{index:02d}",
                "role": "attendee",
                "rsvp_status": "joined",
                "discovery_source": "broadcast",
                "joined_at": "2026-04-16T03:34:00Z",
                "attended_at": None,
                "left_at": None,
            }
        )

    upsert_chat_message(
        {
            "_id": "msg-1",
            "thread_id": CURRENT_USER_THREAD_ID,
            "thread_type": "dm_with_system",
            "event_id": None,
            "sender_type": "assistant",
            "sender_user_id": None,
            "message_type": "text",
            "content_text": "Good morning. What kind of event are you in the mood for today?",
            "content_structured": None,
            "created_at": "2026-04-16T03:32:00Z",
            "edited_at": None,
        }
    )
    upsert_chat_message(
        {
            "_id": "msg-2",
            "thread_id": CURRENT_USER_THREAD_ID,
            "thread_type": "dm_with_system",
            "event_id": None,
            "sender_type": "user",
            "sender_user_id": CURRENT_USER_ID,
            "message_type": "text",
            "content_text": "Something social but low-pressure. Maybe music, a run, or a talk.",
            "content_structured": None,
            "created_at": "2026-04-16T03:33:00Z",
            "edited_at": None,
        }
    )
    upsert_chat_message(
        {
            "_id": "msg-3",
            "thread_id": CURRENT_USER_THREAD_ID,
            "thread_type": "dm_with_system",
            "event_id": EVENT_ID,
            "sender_type": "assistant",
            "sender_user_id": None,
            "message_type": "event_card",
            "content_text": "I can help with that. Here is one event that matches your vibe tonight.",
            "content_structured": {
                "card_type": "event_card",
                "event": {
                    "event_id": EVENT_ID,
                    "title": "Rooftop Acoustic Session",
                    "location_name": "Dialogues Cafe, Koramangala",
                    "maps_url": "https://maps.google.com/?q=Dialogues+Cafe+Koramangala+Bengaluru",
                    "description": (
                        "An intimate acoustic evening on a rooftop with stripped-back sets, a small crowd, "
                        "and enough room to actually talk between songs."
                    ),
                    "attendees": [
                        {"display_name": "Nikhil", "avatar_url": None},
                        {"display_name": "Rhea", "avatar_url": None},
                        {"display_name": "Sana", "avatar_url": None},
                    ],
                    "attendee_count": 16,
                },
            },
            "created_at": "2026-04-16T03:34:00Z",
            "edited_at": None,
        }
    )
    upsert_chat_message(
        {
            "_id": "msg-4",
            "thread_id": CURRENT_USER_THREAD_ID,
            "thread_type": "dm_with_system",
            "event_id": None,
            "sender_type": "assistant",
            "sender_user_id": None,
            "message_type": "text",
            "content_text": "If you'd rather keep it active, there's also a sunrise run gathering tomorrow.",
            "content_structured": None,
            "created_at": "2026-04-16T03:35:00Z",
            "edited_at": None,
        }
    )

    print(f"Seeded Android demo data for {CURRENT_USER_ID} into {db.name}.")


if __name__ == "__main__":
    main()
