# Third Space

Third Space is an AI-native event discovery app.

Instead of browsing listings or joining noisy groups, users interact through a chat interface. The system understands their interests and context, and routes relevant events to them.

The goal is simple:
> Make it effortless to discover and attend the right events.

---

## Core Idea

Events today are fragmented:
- WhatsApp groups are noisy and hard to search
- Platforms like BookMyShow or Meetup lack strong personalization

Third Space fixes this by:
- Understanding users through conversation
- Matching events to people intelligently
- Delivering events directly through a chat interface

---

## Product Overview

### Chat (Main Interface)

This is the primary surface of the app.

Users can:
- Discover events ("anything happening tonight?")
- Create events ("planning a run tomorrow morning")
- Receive suggestions from the system

All interactions—search, creation, and notifications—happen inside chat.

---

### Profile

A lightweight, AI-generated view of the user:
- Interests (e.g. running, chess, music)
- Preferences (time, vibe, social intent)
- Event history (joined / attended)

This helps the system personalize event recommendations.

---

### Community

A simple view of people you’ve interacted with through events.

Shows:
- People you’ve attended events with
- Shared context ("you both went to 2 running events")

This exists primarily for transparency:
> Users can understand why they are being matched with certain people or events.

---

### Events

Events are the core unit of the system.

Each event includes:
- Basic details (title, time, location)
- A list of attendees
- A temporary chat for coordination

Users can:
- Join events
- See who else is going
- Interact with attendees via event chat

---

### Your Signups (Planned)

A dedicated view for:
- Events you’ve RSVP’d to
- Upcoming plans in one place

This will be added after the core flows are stable.

---

## How It Works

1. Users express interests through chat
2. Events are created by users
3. The system matches events to relevant users
4. Users discover and join events
5. Over time, the system improves relevance

---

## Tech (Hackathon Setup)

- Simple username/password authentication (minimal security)
- Chat-driven interface
- OpenAI API for:
  - Understanding user intent
  - Extracting event details
  - Generating responses

---

## Scope (Hackathon)

We are focusing on:

- Chat-based event creation
- Chat-based event discovery
- Basic user profiles
- Lightweight community visibility

We are intentionally **not** building:
- Complex social features
- Advanced matching systems
- Full notification infrastructure

---

## Vision

Third Space aims to become:
> A personalized event routing layer for the real world

Where the right people naturally find the right events—without searching.

## Related Docs

- [Backend schema](./BACKEND_SCHEMA.md)

## Backend

A minimal backend scaffold now exists in `app/` using `FastAPI + MongoDB`.

Local setup:

```bash
cp .env.example .env
uv sync
uv run uvicorn app.main:app --reload
```

Important env vars:

- `MONGODB_URI`
- `MONGODB_DB_NAME`
- `OPENAI_KEY`
- `OPENAI_MODEL` (optional)

Core API surfaces currently scaffolded:

- `POST /auth/signup`
- `POST /auth/signin`
- `POST /auth/google`
- `GET /users/{user_id}`
- `GET /mobile/bootstrap/{user_id}`
- `PATCH /users/{user_id}`
- `POST /users/direct-message`
- `GET /connections/score/{user_1}/{user_2}`
- `GET /connections/findnew/{user_1}`
- `GET /chat/{user_id}`
- `POST /chat/{user_id}/messages`
- `POST /events`
- `GET /events/{event_id}`
- `PATCH /events/{event_id}`
- `POST /events/{event_id}/join`
- `GET /events/{event_id}/score/{user_id}`
- `POST /events/{event_id}/broadcast`
- `GET /users/{user_id}/events`
- `GET /events/{event_id}/attendees`
- `GET /events/{event_id}/chat`
- `POST /events/{event_id}/chat/messages`

Chat response shape:

- `GET /chat/{user_id}` and `GET /events/{event_id}/chat` both return a dict with:
  - `thread`: thread metadata
  - `items`: ordered chat items

Each entry in `items` can represent either a plain text message or a structured card:

```json
{
  "_id": "msg_123",
  "thread_id": "thread_main_usr_123",
  "thread_type": "dm_with_system",
  "event_id": null,
  "sender_type": "assistant",
  "sender_user_id": null,
  "message_type": "event_card",
  "content_text": "Here are two running events you may like.",
  "content_structured": {
    "card_type": "event_card",
    "events": [
      {
        "event_id": "evt_123",
        "title": "Saturday Morning Cubbon Park Run",
        "start_time": "2026-04-18T01:30:00Z",
        "location_name": "Cubbon Park"
      }
    ]
  },
  "created_at": "2026-04-16T12:00:00Z",
  "edited_at": null
}
```

Use `message_type` to decide how to render the item:

- `text`: render `content_text`
- `event_card`: render `content_structured`
- `system_notice`: render as system metadata or status text

Android demo seed:

```bash
uv run python scripts/seed_android_mock_data.py
```

This creates the seeded mobile demo user `user-animesh`, its main chat thread, a demo event, and matching attendees directly in MongoDB.

Android app to local backend:

```bash
ANDROID_SDK_ROOT=/home/animesh/Android/Sdk ./android/gradlew -p android installDebug
```

The Android app now expects the backend at `http://192.168.252.189:8000` on your local network.

AWS note:

- the current AWS IAM user can authenticate with STS
- it does not have `rds:DescribeDBClusters`, so provisioning DocumentDB/RDS-style databases will require extra permissions

Connection matching:

- `/connections/score/{user_1}/{user_2}` sends both users' stored profile data to the OpenAI API and returns:
  - `score`
  - `reasoning`
  - `what_matches`
  - `what_does_not_match`
- `/connections/findnew/{user_1}` compares `user_1` against users they are not already connected to and returns the highest-scoring candidates

Event matching:

- `/events/{event_id}/score/{user_id}` sends:
  - the user's stored profile data
  - the event data
  - the creator's user data
  - user data for attendees already going
  to the OpenAI API and returns:
  - `score`
  - `reasoning`
  - `what_matches`
  - `what_does_not_match`
- `/events/{event_id}/broadcast?limit=10` scores eligible users for the event, excludes the creator/current attendees/already-broadcasted users, writes top candidates to `event_broadcasts`, and returns the ranked candidates
