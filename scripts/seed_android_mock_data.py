from app.db import ensure_indexes, db


CURRENT_USER_ID = "user-animesh"
CURRENT_USER_EMAIL = "animesh@example.com"
CURRENT_USER_THREAD_ID = f"thread_main_{CURRENT_USER_ID}"
EVENT_ID = "evt-rooftop-acoustic-session"
EVENT_THREAD_ID = f"thread_event_{EVENT_ID}"
RUN_EVENT_ID_1 = "evt-cubbon-sunrise-run"
RUN_EVENT_ID_2 = "evt-ulsoor-social-run"
SNOOKER_EVENT_ID = "evt-sjr-vogue-snooker-night"
CHESS_EVENT_ID_1 = "evt-chess-cafe-opening"
CHESS_EVENT_ID_2 = "evt-endgame-study-circle"
CHESS_EVENT_ID_3 = "evt-sunday-speed-chess"
WRITERS_EVENT_ID = "evt-brick-over-bangalore-writers-room"
CURRENT_USER_HOSTED_EVENT_IDS = [
    RUN_EVENT_ID_2,
    SNOOKER_EVENT_ID,
]
ALL_JOINED_EVENT_IDS = [
    EVENT_ID,
    RUN_EVENT_ID_1,
    RUN_EVENT_ID_2,
    SNOOKER_EVENT_ID,
    CHESS_EVENT_ID_1,
    CHESS_EVENT_ID_2,
    CHESS_EVENT_ID_3,
    WRITERS_EVENT_ID,
]
EVENT_CREATOR_BY_ID = {
    EVENT_ID: "usr-attendee-13",
    RUN_EVENT_ID_1: "usr-attendee-01",
    RUN_EVENT_ID_2: CURRENT_USER_ID,
    SNOOKER_EVENT_ID: CURRENT_USER_ID,
    CHESS_EVENT_ID_1: "usr-attendee-02",
    CHESS_EVENT_ID_2: "usr-attendee-11",
    CHESS_EVENT_ID_3: "usr-attendee-06",
    WRITERS_EVENT_ID: "usr-attendee-15",
}
ATTENDEE_HOSTED_EVENT_IDS = {
    "usr-attendee-01": [RUN_EVENT_ID_1],
    "usr-attendee-02": [CHESS_EVENT_ID_1],
    "usr-attendee-06": [CHESS_EVENT_ID_3],
    "usr-attendee-11": [CHESS_EVENT_ID_2],
    "usr-attendee-13": [EVENT_ID],
    "usr-attendee-15": [WRITERS_EVENT_ID],
}
ATTENDEE_SPECS = [
    {
        "name": "Nikhil",
        "avatar_url": "avatar_nikhil",
        "headline": "Early-morning runner who likes coffee after movement",
        "summary": "Most of my good plans begin outdoors and end in a cafe with an easy conversation.",
        "tags": ["running", "coffee", "citywalks"],
        "ideal_plans": "Park runs, breakfast, and low-pressure weekend catchups.",
        "conversation_spark": "Training routines, neighborhoods, and design details in everyday places.",
        "joined_event_ids": [EVENT_ID, RUN_EVENT_ID_1, CHESS_EVENT_ID_2],
    },
    {
        "name": "Rhea",
        "avatar_url": "avatar_rhea",
        "headline": "Chess regular with a bias for smart, quiet rooms",
        "summary": "I like small groups, strong questions, and games that reward patience and pattern recognition.",
        "tags": ["chess", "books", "strategy"],
        "ideal_plans": "Cafe chess, bookstores, and long conversations with a point of view.",
        "conversation_spark": "Openings, memoirs, and how people learn complicated things.",
        "joined_event_ids": [EVENT_ID, CHESS_EVENT_ID_1, CHESS_EVENT_ID_2, WRITERS_EVENT_ID],
    },
    {
        "name": "Sana",
        "avatar_url": "avatar_sana",
        "headline": "Thoughtful generalist who likes running and good writing",
        "summary": "I usually say yes to plans that combine movement, ideas, and people who are easy to talk to.",
        "tags": ["running", "writing", "design"],
        "ideal_plans": "A short run, a calm breakfast, or a writing circle with real feedback.",
        "conversation_spark": "Essays, product taste, and routines that make life feel cleaner.",
        "joined_event_ids": [EVENT_ID, RUN_EVENT_ID_1, WRITERS_EVENT_ID],
    },
    {
        "name": "Aarav",
        "headline": "Builder type who likes startup talk without the noise",
        "summary": "I want conversations with substance, clear thinking, and people making interesting bets.",
        "tags": ["startups", "product", "design"],
        "ideal_plans": "Founder dinners, product critiques, and walks after work.",
        "conversation_spark": "Distribution, interfaces, and weird early-stage decisions.",
        "joined_event_ids": [EVENT_ID, RUN_EVENT_ID_1, RUN_EVENT_ID_2],
    },
    {
        "name": "Meera",
        "headline": "Reader and writer who prefers small rooms over crowded scenes",
        "summary": "I like thoughtful prompts, annotated books, and people who notice language.",
        "tags": ["writing", "books", "creative"],
        "ideal_plans": "Writers groups, independent bookstores, and coffee with notebooks.",
        "conversation_spark": "Essays, voice, and how taste gets formed.",
        "joined_event_ids": [CHESS_EVENT_ID_1, WRITERS_EVENT_ID],
    },
    {
        "name": "Kabir",
        "headline": "Curious operator who alternates between chess boards and product docs",
        "summary": "My favorite social settings are strategic but warm: something to think about and people to laugh with.",
        "tags": ["chess", "startups", "games"],
        "ideal_plans": "Rapid chess, dinner after work, and conversations about systems.",
        "conversation_spark": "Strategy, incentives, and product operations.",
        "joined_event_ids": [RUN_EVENT_ID_2, CHESS_EVENT_ID_3],
    },
    {
        "name": "Ira",
        "headline": "Design-minded walker who likes intimate music spaces",
        "summary": "I respond to places with mood, visual care, and conversations that unfold slowly.",
        "tags": ["design", "music", "citywalks"],
        "ideal_plans": "Gallery walks, acoustic sessions, and coffee in beautiful rooms.",
        "conversation_spark": "Typography, set design, and cities at night.",
        "joined_event_ids": [EVENT_ID, CHESS_EVENT_ID_2],
    },
    {
        "name": "Dev",
        "headline": "Consistent runner who likes meeting people through activity first",
        "summary": "Movement makes conversation easier for me, especially when the pace stays relaxed.",
        "tags": ["running", "fitness", "social"],
        "ideal_plans": "Morning runs, mobility sessions, and breakfast with a group.",
        "conversation_spark": "Training, recovery, and discipline that doesn't feel rigid.",
        "joined_event_ids": [EVENT_ID, RUN_EVENT_ID_1, CHESS_EVENT_ID_3],
    },
    {
        "name": "Maya",
        "headline": "Creative planner drawn to essays, films, and better questions",
        "summary": "I care about narrative, taste, and people who can make a room feel more alive.",
        "tags": ["writing", "culture", "music"],
        "ideal_plans": "Film clubs, live music, and long writing conversations.",
        "conversation_spark": "Story structure, criticism, and the emotional texture of cities.",
        "joined_event_ids": [SNOOKER_EVENT_ID, WRITERS_EVENT_ID],
    },
    {
        "name": "Arjun",
        "headline": "Neighborhood athlete with an eye for simple recurring rituals",
        "summary": "I like plans that are easy to repeat and improve over time.",
        "tags": ["running", "fitness", "habits"],
        "ideal_plans": "Evening runs, snack stops, and practical conversations.",
        "conversation_spark": "Habit loops, race prep, and low-friction routines.",
        "joined_event_ids": [RUN_EVENT_ID_2],
    },
    {
        "name": "Tara",
        "headline": "Bookish strategist who enjoys learning communities",
        "summary": "I like collaborative spaces where people come to get sharper together.",
        "tags": ["chess", "learning", "books"],
        "ideal_plans": "Study circles, workshops, and quiet cafes.",
        "conversation_spark": "Pedagogy, books, and long-form thinking.",
        "joined_event_ids": [EVENT_ID, CHESS_EVENT_ID_1],
    },
    {
        "name": "Neil",
        "headline": "Walker, note-taker, and fan of creative routines",
        "summary": "I am interested in the link between movement, clarity, and sustained creative work.",
        "tags": ["citywalks", "writing", "creativity"],
        "ideal_plans": "Walk-and-talks, notebook sessions, and small meetups with structure.",
        "conversation_spark": "Idea generation, routines, and making creative work less dramatic.",
        "joined_event_ids": [RUN_EVENT_ID_1, CHESS_EVENT_ID_3],
    },
    {
        "name": "Aditi",
        "headline": "Soft-spoken host who likes making social plans feel easy",
        "summary": "I notice group energy quickly and prefer gatherings with a clear, gentle center.",
        "tags": ["music", "social", "hospitality"],
        "ideal_plans": "House concerts, board games, and dinners with a strong host.",
        "conversation_spark": "Hosting, playlists, and how space changes behavior.",
        "joined_event_ids": [EVENT_ID, CHESS_EVENT_ID_2],
    },
    {
        "name": "Rohan",
        "headline": "Competitive enough for snooker, calm enough for a long chat after",
        "summary": "I like games that create a rhythm for conversation instead of interrupting it.",
        "tags": ["snooker", "games", "social"],
        "ideal_plans": "Clubhouse games, tea, and unhurried evenings.",
        "conversation_spark": "Cue sports, group dynamics, and neighborhood life.",
        "joined_event_ids": [SNOOKER_EVENT_ID],
    },
    {
        "name": "Leah",
        "headline": "Essay reader who likes writers rooms with sharp but kind feedback",
        "summary": "I am drawn to language, revision, and people who can be precise without being cold.",
        "tags": ["writing", "books", "creative"],
        "ideal_plans": "Writers meetups, literary events, and coffee with marked-up drafts.",
        "conversation_spark": "Revision, criticism, and why certain sentences stay with you.",
        "joined_event_ids": [EVENT_ID, RUN_EVENT_ID_2, WRITERS_EVENT_ID],
    },
    {
        "name": "Zoya",
        "headline": "Culture-heavy social planner who likes mixing art and conversation",
        "summary": "The best rooms for me combine a point of view, aesthetic care, and people who listen well.",
        "tags": ["writing", "design", "music"],
        "ideal_plans": "Talks, live sets, and creative meetups with a strong host.",
        "conversation_spark": "Creative process, curation, and how scenes get built.",
        "joined_event_ids": [CHESS_EVENT_ID_1, WRITERS_EVENT_ID],
    },
]


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
    attendees = []
    for index, spec in enumerate(ATTENDEE_SPECS, start=1):
        name = spec["name"]
        user_id = f"usr-attendee-{index:02d}"
        tag_list = spec["tags"]
        attendees.append(
            {
                "_id": user_id,
                "email": f"{user_id}@example.com",
                "password": "seeded-password",
                "auth_providers": ["seed"],
                "google_id": None,
                "display_name": name,
                "username": name.lower(),
                "avatar_url": spec.get("avatar_url"),
                "account_status": "active",
                "persona": {
                    "seeded_for": "android_demo",
                    "mobile_profile": {
                        "headline": spec["headline"],
                        "location_label": "Bengaluru",
                        "interests_summary": spec["summary"],
                        "journey": [],
                        "interests": [
                            {
                                "tag": f"#{tag}",
                                "description": f"{name} keeps coming back to {tag.replace('_', ' ')} in social plans and conversations.",
                            }
                            for tag in tag_list
                        ],
                    },
                    "profile_dict": {
                        "home_base": "Bengaluru",
                        "work_context": spec["headline"],
                        "social_energy": spec["summary"],
                        "ideal_plans": spec["ideal_plans"],
                        "social_goals": "Meet thoughtful people through recurring local plans.",
                        "conversation_spark": spec["conversation_spark"],
                        "interest_tags": tag_list,
                        "profile_notes": spec["summary"],
                    },
                },
                "connections": [],
                "event_refs": {
                    "hosted_event_ids": ATTENDEE_HOSTED_EVENT_IDS.get(user_id, []),
                    "joined_event_ids": spec["joined_event_ids"],
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
        "avatar_url": "avatar_animesh",
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
            "hosted_event_ids": CURRENT_USER_HOSTED_EVENT_IDS,
            "joined_event_ids": ALL_JOINED_EVENT_IDS,
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
            "creator_user_id": EVENT_CREATOR_BY_ID[EVENT_ID],
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
            "updated_at": "2026-04-16T10:48:00Z",
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
            "updated_at": "2026-04-16T10:48:00Z",
            "last_message_at": "2026-04-16T10:48:00Z",
        }
    )

    additional_events = [
        {
            "_id": RUN_EVENT_ID_1,
            "thread_id": f"thread_event_{RUN_EVENT_ID_1}",
            "title": "Cubbon Sunrise Run",
            "description": (
                "A steady 6K loop through Cubbon Park with a social pace, post-run coffee, "
                "and enough room for people who want movement without race-day intensity."
            ),
            "category_tags": ["running", "fitness", "outdoors", "coffee"],
            "timing": {
                "start_time": "2026-04-17T00:30:00Z",
                "timezone": "Asia/Kolkata",
                "label": "Tomorrow, 6:00 AM",
            },
            "location": {
                "name": "Cubbon Park Main Gate",
                "maps_url": "https://maps.google.com/?q=Cubbon+Park+Main+Gate+Bengaluru",
            },
            "capacity": {"max_attendees": 18},
            "attendance": {
                "attendee_count": 5,
                "confirmed_count": 5,
                "maybe_count": 0,
                "declined_count": 0,
            },
            "participant_user_ids": [CURRENT_USER_ID, "usr-attendee-01", "usr-attendee-04", "usr-attendee-08", "usr-attendee-12"],
            "created_at": "2026-04-16T03:40:00Z",
            "updated_at": "2026-04-16T03:50:00Z",
        },
        {
            "_id": RUN_EVENT_ID_2,
            "thread_id": f"thread_event_{RUN_EVENT_ID_2}",
            "title": "Ulsoor Lake Tempo Run",
            "description": (
                "An 8K evening run around Ulsoor Lake for people who want a bit more pace, "
                "followed by a cool-down walk and quick catch-up."
            ),
            "category_tags": ["running", "fitness", "outdoors", "social"],
            "timing": {
                "start_time": "2026-04-18T12:45:00Z",
                "timezone": "Asia/Kolkata",
                "label": "Saturday, 6:15 PM",
            },
            "location": {
                "name": "Ulsoor Lake Boat Club Entrance",
                "maps_url": "https://maps.google.com/?q=Ulsoor+Lake+Boat+Club+Entrance+Bengaluru",
            },
            "capacity": {"max_attendees": 14},
            "attendance": {
                "attendee_count": 4,
                "confirmed_count": 4,
                "maybe_count": 0,
                "declined_count": 0,
            },
            "participant_user_ids": [CURRENT_USER_ID, "usr-attendee-06", "usr-attendee-10", "usr-attendee-15"],
            "created_at": "2026-04-16T03:42:00Z",
            "updated_at": "2026-04-16T03:52:00Z",
        },
        {
            "_id": SNOOKER_EVENT_ID,
            "thread_id": f"thread_event_{SNOOKER_EVENT_ID}",
            "title": "Snooker Night at SJR Vogue Apartments",
            "description": (
                "A low-key snooker session in the clubhouse at SJR Vogue Apartments. "
                "Good for people who want a few frames, light conversation, and a close-to-home plan."
            ),
            "category_tags": ["snooker", "games", "social", "indoor"],
            "timing": {
                "start_time": "2026-04-16T14:00:00Z",
                "timezone": "Asia/Kolkata",
                "label": "Today, 7:30 PM",
            },
            "location": {
                "name": "SJR Vogue Apartments Clubhouse",
                "maps_url": "https://maps.google.com/?q=SJR+Vogue+Apartments+Bengaluru",
            },
            "capacity": {"max_attendees": 8},
            "attendance": {
                "attendee_count": 4,
                "confirmed_count": 4,
                "maybe_count": 0,
                "declined_count": 0,
            },
            "participant_user_ids": [CURRENT_USER_ID, "usr-attendee-02", "usr-attendee-09", "usr-attendee-14"],
            "created_at": "2026-04-16T06:10:00Z",
            "updated_at": "2026-04-16T06:25:00Z",
        },
        {
            "_id": CHESS_EVENT_ID_1,
            "thread_id": f"thread_event_{CHESS_EVENT_ID_1}",
            "title": "Chess and Coffee Opening Prep",
            "description": (
                "Bring a board or borrow one for casual rapid games, opening experiments, "
                "and coffee-fueled discussion without tournament pressure."
            ),
            "category_tags": ["chess", "games", "coffee", "strategy"],
            "timing": {
                "start_time": "2026-04-17T13:00:00Z",
                "timezone": "Asia/Kolkata",
                "label": "Tomorrow, 6:30 PM",
            },
            "location": {
                "name": "Third Wave Coffee, Indiranagar",
                "maps_url": "https://maps.google.com/?q=Third+Wave+Coffee+Indiranagar+Bengaluru",
            },
            "capacity": {"max_attendees": 12},
            "attendance": {
                "attendee_count": 5,
                "confirmed_count": 5,
                "maybe_count": 0,
                "declined_count": 0,
            },
            "participant_user_ids": [CURRENT_USER_ID, "usr-attendee-03", "usr-attendee-05", "usr-attendee-11", "usr-attendee-16"],
            "created_at": "2026-04-16T03:45:00Z",
            "updated_at": "2026-04-16T03:55:00Z",
        },
        {
            "_id": CHESS_EVENT_ID_2,
            "thread_id": f"thread_event_{CHESS_EVENT_ID_2}",
            "title": "Endgame Study Circle",
            "description": (
                "A small chess meetup focused on practical endgames, annotated positions, "
                "and slow discussion for people who enjoy learning as much as playing."
            ),
            "category_tags": ["chess", "learning", "strategy", "indoor"],
            "timing": {
                "start_time": "2026-04-19T05:30:00Z",
                "timezone": "Asia/Kolkata",
                "label": "Sunday, 11:00 AM",
            },
            "location": {
                "name": "Atta Galatta, Indiranagar",
                "maps_url": "https://maps.google.com/?q=Atta+Galatta+Indiranagar+Bengaluru",
            },
            "capacity": {"max_attendees": 10},
            "attendance": {
                "attendee_count": 4,
                "confirmed_count": 4,
                "maybe_count": 0,
                "declined_count": 0,
            },
            "participant_user_ids": [CURRENT_USER_ID, "usr-attendee-01", "usr-attendee-07", "usr-attendee-13"],
            "created_at": "2026-04-16T03:47:00Z",
            "updated_at": "2026-04-16T03:57:00Z",
        },
        {
            "_id": CHESS_EVENT_ID_3,
            "thread_id": f"thread_event_{CHESS_EVENT_ID_3}",
            "title": "Sunday Speed Chess Boards",
            "description": (
                "Fast 10-minute games, quick pairings, and a friendly ladder for anyone who wants "
                "a sharper, more playful chess evening."
            ),
            "category_tags": ["chess", "games", "social", "competitive"],
            "timing": {
                "start_time": "2026-04-20T13:30:00Z",
                "timezone": "Asia/Kolkata",
                "label": "Monday, 7:00 PM",
            },
            "location": {
                "name": "Champaca Bookstore Courtyard",
                "maps_url": "https://maps.google.com/?q=Champaca+Bookstore+Bengaluru",
            },
            "capacity": {"max_attendees": 16},
            "attendance": {
                "attendee_count": 6,
                "confirmed_count": 6,
                "maybe_count": 0,
                "declined_count": 0,
            },
            "participant_user_ids": [CURRENT_USER_ID, "usr-attendee-04", "usr-attendee-06", "usr-attendee-08", "usr-attendee-12", "usr-attendee-15"],
            "created_at": "2026-04-16T03:49:00Z",
            "updated_at": "2026-04-16T03:59:00Z",
        },
        {
            "_id": WRITERS_EVENT_ID,
            "thread_id": f"thread_event_{WRITERS_EVENT_ID}",
            "title": "Writers Meetup at Brick Over Bangalore",
            "description": (
                "A writers room for essays, notes, fiction fragments, and works-in-progress. "
                "Read a page, listen well, and trade thoughtful feedback over coffee."
            ),
            "category_tags": ["writing", "books", "creative", "meetup"],
            "timing": {
                "start_time": "2026-04-19T10:30:00Z",
                "timezone": "Asia/Kolkata",
                "label": "Sunday, 4:00 PM",
            },
            "location": {
                "name": "Brick Over Bangalore",
                "maps_url": "https://maps.google.com/?q=Brick+Over+Bangalore",
            },
            "capacity": {"max_attendees": 14},
            "attendance": {
                "attendee_count": 5,
                "confirmed_count": 5,
                "maybe_count": 0,
                "declined_count": 0,
            },
            "participant_user_ids": [CURRENT_USER_ID, "usr-attendee-02", "usr-attendee-05", "usr-attendee-11", "usr-attendee-13"],
            "created_at": "2026-04-16T03:51:00Z",
            "updated_at": "2026-04-16T04:01:00Z",
        },
    ]

    for event in additional_events:
        upsert_event(
            {
                "_id": event["_id"],
                "creator_user_id": EVENT_CREATOR_BY_ID[event["_id"]],
                "title": event["title"],
                "description": event["description"],
                "category_tags": event["category_tags"],
                "status": "published",
                "visibility_type": "targeted",
                "created_via": "chat",
                "timing": event["timing"],
                "location": event["location"],
                "capacity": event["capacity"],
                "attendance": event["attendance"],
                "routing": {},
                "chat": {
                    "thread_id": event["thread_id"],
                    "status": "active",
                    "expires_at": None,
                    "participant_user_ids": event["participant_user_ids"],
                },
                "source": {
                    "seeded_from": "android_mock_data",
                },
                "created_at": event["created_at"],
                "updated_at": event["updated_at"],
            }
        )
        upsert_chat_thread(
            {
                "_id": event["thread_id"],
                "thread_type": "event_chat",
                "owner_user_id": None,
                "event_id": event["_id"],
                "participant_user_ids": event["participant_user_ids"],
                "status": "active",
                "created_at": event["created_at"],
                "updated_at": event["updated_at"],
                "last_message_at": event["updated_at"],
            }
        )

    upsert_event_membership(
        {
            "_id": "mem-current-user-event",
            "event_id": EVENT_ID,
            "user_id": CURRENT_USER_ID,
            "role": "attendee",
            "rsvp_status": "joined",
            "discovery_source": "direct_chat",
            "joined_at": "2026-04-16T03:33:00Z",
            "attended_at": None,
            "left_at": None,
        }
    )

    for index in range(1, 17):
        attendee_user_id = f"usr-attendee-{index:02d}"
        upsert_event_membership(
            {
                "_id": f"mem-attendee-{index:02d}",
                "event_id": EVENT_ID,
                "user_id": attendee_user_id,
                "role": "creator" if attendee_user_id == EVENT_CREATOR_BY_ID[EVENT_ID] else "attendee",
                "rsvp_status": "joined",
                "discovery_source": "direct_chat" if attendee_user_id == EVENT_CREATOR_BY_ID[EVENT_ID] else "broadcast",
                "joined_at": "2026-04-16T03:34:00Z",
                "attended_at": None,
                "left_at": None,
            }
        )

    for event in additional_events:
        for participant_user_id in event["participant_user_ids"]:
            upsert_event_membership(
                {
                    "_id": f"mem-{event['_id']}-{participant_user_id}",
                    "event_id": event["_id"],
                    "user_id": participant_user_id,
                    "role": "creator" if participant_user_id == EVENT_CREATOR_BY_ID[event["_id"]] else "attendee",
                    "rsvp_status": "joined",
                    "discovery_source": "direct_chat" if participant_user_id == EVENT_CREATOR_BY_ID[event["_id"]] else "broadcast",
                    "joined_at": event["created_at"],
                    "attended_at": None,
                    "left_at": None,
                }
            )

    upsert_chat_message(
        {
            "_id": "msg-event-1",
            "thread_id": EVENT_THREAD_ID,
            "thread_type": "event_chat",
            "event_id": EVENT_ID,
            "sender_type": "user",
            "sender_user_id": "usr-attendee-02",
            "message_type": "text",
            "content_text": "I can reach Dialogues by 6:50. Happy to save a corner table near the rail.",
            "content_structured": None,
            "created_at": "2026-04-16T10:40:00Z",
            "edited_at": None,
        }
    )
    upsert_chat_message(
        {
            "_id": "msg-event-2",
            "thread_id": EVENT_THREAD_ID,
            "thread_type": "event_chat",
            "event_id": EVENT_ID,
            "sender_type": "user",
            "sender_user_id": CURRENT_USER_ID,
            "message_type": "text",
            "content_text": "Perfect. I booked the rooftop section, so let's use that as the meetup spot.",
            "content_structured": None,
            "created_at": "2026-04-16T10:44:00Z",
            "edited_at": None,
        }
    )
    upsert_chat_message(
        {
            "_id": "msg-event-3",
            "thread_id": EVENT_THREAD_ID,
            "thread_type": "event_chat",
            "event_id": EVENT_ID,
            "sender_type": "user",
            "sender_user_id": "usr-attendee-03",
            "message_type": "text",
            "content_text": "Nice. I'll be there a little early if anyone wants to grab chai before the set starts.",
            "content_structured": None,
            "created_at": "2026-04-16T10:48:00Z",
            "edited_at": None,
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
                        {"display_name": "Nikhil", "avatar_url": "avatar_nikhil"},
                        {"display_name": "Rhea", "avatar_url": "avatar_rhea"},
                        {"display_name": "Sana", "avatar_url": "avatar_sana"},
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
