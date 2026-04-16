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

