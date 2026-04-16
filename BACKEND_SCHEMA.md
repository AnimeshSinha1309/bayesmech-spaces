# Third Space Backend Schema

This is the simplified hackathon design.

Use:

- one backend service
- one MongoDB database
- OpenAI API for extraction and summarization

Do not use extra infra unless it becomes necessary.

## Why MongoDB

MongoDB is the easiest fit for this app because:

- users, events, profiles, and chat payloads are JSON-shaped
- some fields are structured, some are AI-derived and flexible
- you do not need to model DynamoDB access patterns up front
- it is faster to change documents during a hackathon
- this product does not need complex relational integrity yet

## Minimal Architecture

Keep it simple:

- `backend app`: Express, FastAPI, or similar
- `database`: MongoDB
- `auth`: handled in backend
- `OpenAI API`: parse chat, extract event fields, summarize profiles

You can host all of this anywhere. AWS is not required for v1.

## Collections

Use these collections:

1. `users`
2. `events`
3. `event_memberships`
4. `event_broadcasts`
5. `chat_threads`
6. `chat_messages`
7. `community_edges`

That is enough to support:

- sign up / sign in
- Google OAuth
- profile storage
- OpenAI-based profile matching
- event creation
- targeted event suggestions
- event joining
- event chat
- community visibility

## Auth

For hackathon scope, auth can be backend-managed.

Allow:

- email + password sign up
- email + password sign in
- Google OAuth sign in

Store email/password in the user document if you want the fewest moving parts.
Since you said not to worry about security, that is acceptable for this design pass.
If you later want to clean this up, move auth to a dedicated provider.

## User Schema

Store one main user document in `users`.

```json
{
  "_id": "usr_123",
  "email": "alice@example.com",
  "password": "plain-or-hashed-password",
  "auth_providers": ["email", "google"],
  "google_id": "google-oauth-sub-if-any",
  "display_name": "Alice",
  "username": "alice",
  "avatar_url": "https://...",
  "account_status": "active",
  "persona": {
    "summary": "Prefers casual fitness and small social events nearby.",
    "chat_summary": "Often asks about running, coffee meetups, and live music.",
    "bio": "Early morning runner, likes board games and jazz gigs.",
    "interests": ["running", "board games", "jazz"],
    "social_intent": "open_to_meeting_new_people",
    "preferences": {
      "preferred_times": ["weekday_evenings", "weekend_mornings"],
      "max_distance_km": 10,
      "vibe_tags": ["casual", "small-group", "outdoors"]
    },
    "availability_patterns": {
      "weekday_evenings": true,
      "weekend_mornings": true
    },
    "location_context": {
      "current_area": "Indiranagar",
      "home_city": "Bangalore",
      "hometown": "Pune"
    },
    "work_context": {
      "workplace": "Product company in Bangalore",
      "work_mode": "hybrid"
    },
    "eligibility_signals": {
      "prefers_nearby_events": true,
      "comfortable_with_new_people": true,
      "prefers_small_groups": true
    }
  },
  "connections": [
    {
      "user_id": "usr_456",
      "dm_thread_id": "thread_dm_usr_123_usr_456",
      "connection_type": "explicit_chat",
      "last_interaction_at": "2026-04-15T18:30:00Z"
    },
    {
      "user_id": "usr_789",
      "dm_thread_id": "thread_dm_usr_123_usr_789",
      "connection_type": "explicit_chat",
      "last_interaction_at": "2026-04-14T09:10:00Z"
    }
  ],
  "event_refs": {
    "hosted_event_ids": ["evt_100", "evt_123"],
    "joined_event_ids": ["evt_090", "evt_091", "evt_123"],
    "attended_event_ids": ["evt_090", "evt_123"]
  },
  "main_thread_id": "thread_main_usr_123",
  "last_login_at": "2026-04-16T10:00:00Z"
}
```

### What each user must store

Minimum required fields:

- `_id`
- `email`
- `password`
- `auth_providers`
- `google_id`
- `display_name`
- `avatar_url`
- `account_status`
- `persona`
- `connections`
- `event_refs`
- `main_thread_id`

This single document covers:

- auth identity
- profile and inferred persona
- explicit user-to-user chat links
- event history references
- personalization inputs for matching
- chat linkage

Put all inferred profile data in `persona`. This blob is what the backend should use to decide whether a person is a good fit for an event. It should contain anything learned from the long-form onboarding/profile chat, including interests, social comfort, workplace context, hometown, current area, schedule preferences, and any other useful matching signals.

Store direct user relationships in `connections`. This is specifically the set of people the user has explicitly chatted with one-to-one, along with the DM thread id needed to reopen that conversation.

Store actual event references in `event_refs`, not just counts. You need those ids to render profile history and to explain why someone is in a user's community.

## Connection Matching

Use the OpenAI API to score compatibility between two users from their stored profile data.

Endpoints:

- `/connections/score/{user_1}/{user_2}`
- `/connections/findnew/{user_1}`

`/connections/score/{user_1}/{user_2}` should:

- load both users
- pass their profile payloads to OpenAI
- return a dict like:

```json
{
  "user_1": "usr_123",
  "user_2": "usr_456",
  "score": 82,
  "reasoning": "Both users prefer small-group, outdoor social activity and have similar social openness.",
  "what_matches": [
    "Shared interest in running and casual weekend plans",
    "Both seem open to meeting new people"
  ],
  "what_does_not_match": [
    "One user prefers very local events while the other may travel further"
  ]
}
```

`/connections/findnew/{user_1}` should:

- exclude `user_1`
- exclude users already present in `user_1.connections`
- score the remaining candidates
- return the top matches

## Event Matching And Broadcast

Use the OpenAI API to score whether a user should be shown an event.

Endpoints:

- `/events/{event_id}/score/{user_id}`
- `/events/{event_id}/broadcast`

`/events/{event_id}/score/{user_id}` should send:

- the target user's profile
- the event document
- the event creator's user/profile data
- profile data for users already attending

This matters because event fit is not just about event tags. It also depends on:

- whether the creator feels relevant to the target user
- whether the current attendee mix feels compatible
- whether the user's social intent fits the event vibe

Suggested response shape:

```json
{
  "user_id": "usr_789",
  "event_id": "evt_123",
  "score": 84,
  "reasoning": "The user likes casual outdoor weekend plans and the current attendee mix suggests a small, social event.",
  "what_matches": [
    "Strong overlap with running and coffee interests",
    "Weekend morning timing fits user preference"
  ],
  "what_does_not_match": [
    "Travel distance may be slightly above the user's usual preference"
  ]
}
```

`/events/{event_id}/broadcast` should:

- exclude the event creator
- exclude current attendees
- exclude users already in `event_broadcasts` for this event
- score the remaining candidates
- store top candidates in `event_broadcasts`
- return the ranked list

## Event Schema

Store one main event document in `events`.

```json
{
  "_id": "evt_123",
  "creator_user_id": "usr_123",
  "title": "Saturday Morning Cubbon Park Run",
  "description": "Easy 5k run followed by coffee.",
  "category_tags": ["running", "fitness", "outdoors", "coffee"],
  "status": "published",
  "visibility_type": "targeted",
  "created_via": "chat",
  "timing": {
    "start_time": "2026-04-18T01:30:00Z",
    "end_time": "2026-04-18T03:30:00Z",
    "timezone": "Asia/Kolkata",
    "is_all_day": false
  },
  "location": {
    "name": "Cubbon Park",
    "address": "Cubbon Park, Bengaluru",
    "lat": 12.9763,
    "lng": 77.5929,
    "location_type": "physical"
  },
  "capacity": {
    "limit": 12,
    "waitlist_enabled": true
  },
  "attendance": {
    "attendee_count": 6,
    "confirmed_count": 6,
    "maybe_count": 1,
    "declined_count": 3
  },
  "routing": {
    "target_audience_tags": ["running", "weekend_mornings", "outdoors"],
    "broadcast_status": "sent",
    "broadcasted_user_ids": ["usr_200", "usr_201", "usr_202"],
    "routing_summary": "Sent to users interested in nearby casual running events."
  },
  "chat": {
    "thread_id": "thread_event_evt_123",
    "status": "active",
    "expires_at": "2026-04-19T03:30:00Z"
  },
  "source": {
    "source_message_id": "msg_789",
    "extraction_confidence": 0.93
  },
  "created_at": "2026-04-16T10:30:00Z",
  "updated_at": "2026-04-16T10:30:00Z"
}
```

### What each event must store

Minimum required fields:

- `_id`
- `creator_user_id`
- `title`
- `description`
- `category_tags`
- `status`
- `visibility_type`
- `timing`
- `location`
- `chat.thread_id`
- `created_at`
- `updated_at`

This single document covers:

- event identity
- creator
- timing and location
- targeting metadata
- event chat linkage

## Why memberships should be separate

Do not store full attendee state only inside the event document.

Use `event_memberships` for the user-event relationship:

```json
{
  "_id": "mem_123",
  "event_id": "evt_123",
  "user_id": "usr_456",
  "role": "attendee",
  "rsvp_status": "joined",
  "discovery_source": "broadcast",
  "joined_at": "2026-04-16T11:00:00Z",
  "attended_at": null,
  "left_at": null
}
```

This is necessary because you need:

- all events a user joined
- all attendees for an event
- RSVP state changes
- event history for profile updates

## Why broadcasts should be separate

Events are shown to some users but not all users. That needs its own mapping.

Use `event_broadcasts`:

```json
{
  "_id": "brd_123",
  "event_id": "evt_123",
  "user_id": "usr_456",
  "score": 0.88,
  "broadcast_reason": "Matches running + weekend morning preferences.",
  "broadcasted_at": "2026-04-16T10:35:00Z",
  "seen_at": null,
  "clicked_at": null,
  "dismissed_at": null,
  "conversion_status": "unseen"
}
```

This solves:

- who received an event
- who saw it
- who joined from it

## Chat Schema

Use separate collections for threads and messages.

### `chat_threads`

```json
{
  "_id": "thread_event_evt_123",
  "thread_type": "event_chat",
  "owner_user_id": null,
  "event_id": "evt_123",
  "participant_user_ids": ["usr_123", "usr_456"],
  "status": "active",
  "created_at": "2026-04-16T10:30:00Z",
  "updated_at": "2026-04-16T11:10:00Z",
  "last_message_at": "2026-04-16T11:10:00Z"
}
```

For the main user chat:

```json
{
  "_id": "thread_main_usr_123",
  "thread_type": "dm_with_system",
  "owner_user_id": "usr_123",
  "event_id": null,
  "participant_user_ids": ["usr_123"],
  "status": "active",
  "created_at": "2026-04-16T10:00:00Z",
  "updated_at": "2026-04-16T10:05:00Z",
  "last_message_at": "2026-04-16T10:05:00Z"
}
```

### `chat_messages`

```json
{
  "_id": "msg_123",
  "thread_id": "thread_event_evt_123",
  "thread_type": "event_chat",
  "event_id": "evt_123",
  "sender_type": "user",
  "sender_user_id": "usr_456",
  "message_type": "text",
  "content_text": "I can be there by 7:15.",
  "content_structured": null,
  "created_at": "2026-04-16T11:10:00Z",
  "edited_at": null
}
```

## Chat API Shape

There are two distinct chat surfaces:

- primary user chat: `/chat/{user_id}`
- event chat: `/events/{event_id}/chat`

Both should return a dict, not a raw list.

Suggested response shape:

```json
{
  "thread": {
    "thread_id": "thread_main_usr_123",
    "thread_type": "dm_with_system",
    "owner_user_id": "usr_123",
    "event_id": null,
    "participant_user_ids": ["usr_123"],
    "status": "active",
    "created_at": "2026-04-16T10:00:00Z",
    "updated_at": "2026-04-16T10:05:00Z",
    "last_message_at": "2026-04-16T10:05:00Z"
  },
  "items": [
    {
      "_id": "msg_1",
      "thread_id": "thread_main_usr_123",
      "thread_type": "dm_with_system",
      "event_id": null,
      "sender_type": "assistant",
      "sender_user_id": null,
      "message_type": "text",
      "content_text": "What kind of event are you looking for?",
      "content_structured": null,
      "created_at": "2026-04-16T10:01:00Z",
      "edited_at": null
    },
    {
      "_id": "msg_2",
      "thread_id": "thread_main_usr_123",
      "thread_type": "dm_with_system",
      "event_id": null,
      "sender_type": "assistant",
      "sender_user_id": null,
      "message_type": "event_card",
      "content_text": "Here are two events that match your profile.",
      "content_structured": {
        "card_type": "event_card",
        "events": [
          {
            "event_id": "evt_123",
            "title": "Saturday Morning Cubbon Park Run"
          }
        ]
      },
      "created_at": "2026-04-16T10:02:00Z",
      "edited_at": null
    }
  ]
}
```

This is the right shape because:

- the frontend needs thread-level metadata and chat items together
- `items` can contain plain text messages and richer structured cards
- the backend can add more card types later without breaking the response envelope

## Community Schema

Community is derived from shared events.

Use `community_edges`:

```json
{
  "_id": "edge_usr_123_usr_456",
  "user_id": "usr_123",
  "other_user_id": "usr_456",
  "shared_event_count": 2,
  "shared_event_ids": ["evt_100", "evt_123"],
  "last_shared_event_id": "evt_123",
  "relationship_summary": "You both attended two running events.",
  "last_interaction_at": "2026-04-16T11:00:00Z",
  "status": "active"
}
```

This matches the README idea of community as transparency, not a full social network.

## Core Queries You Need

This design supports these directly:

- find user by email
- find user by google id
- get profile for user
- get main chat thread for user
- get messages for a thread
- create event from chat
- get event by id
- get all attendees for an event
- get all upcoming events for a user
- get all suggested events for a user
- get community for a user

## Suggested Indexes

Keep indexing light.

### `users`

- unique index on `email`
- index on `google_id`
- index on `username` if used

### `events`

- index on `creator_user_id`
- index on `timing.start_time`
- index on `status`
- index on `category_tags`

### `event_memberships`

- unique compound index on `event_id + user_id`
- index on `user_id + rsvp_status`
- index on `event_id + rsvp_status`

### `event_broadcasts`

- unique compound index on `event_id + user_id`
- index on `user_id + broadcasted_at`

### `chat_messages`

- index on `thread_id + created_at`

### `community_edges`

- index on `user_id + last_interaction_at`

## Practical Recommendation

If you want the smallest workable system, build exactly this:

- one backend app
- one MongoDB database
- one `users` document per user
- one `events` document per event
- separate mapping collections for memberships, broadcasts, chat messages, and community

That is enough for the README product without overengineering it.
