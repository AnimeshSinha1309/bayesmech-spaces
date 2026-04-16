import app.routes as routes
import scripts.seed_android_mock_data as seed_data


def test_create_event_creates_event_membership_and_chat_thread(client, seeded_db):
    response = client.post(
        "/events",
        json={
            "creator_user_id": seed_data.CURRENT_USER_ID,
            "title": "Sunrise Run Club",
            "description": "Easy-paced social run around Ulsoor Lake.",
            "category_tags": ["running", "social"],
            "timing": {
                "start_time": "2026-04-17T01:30:00Z",
                "timezone": "Asia/Kolkata",
            },
            "location": {
                "name": "Ulsoor Lake",
            },
            "capacity": {"max_attendees": 12},
            "routing": {"mode": "seeded-test"},
            "source": {"created_by": "pytest"},
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["title"] == "Sunrise Run Club"
    assert body["creator_user_id"] == seed_data.CURRENT_USER_ID

    event_thread = seeded_db.chat_threads.find_one({"_id": body["chat"]["thread_id"]})
    membership = seeded_db.event_memberships.find_one(
        {"event_id": body["_id"], "user_id": seed_data.CURRENT_USER_ID}
    )
    assert event_thread is not None
    assert membership is not None
    assert membership["role"] == "creator"


def test_get_event_returns_seeded_rooftop_event(client):
    response = client.get(f"/events/{seed_data.EVENT_ID}")

    assert response.status_code == 200
    body = response.json()
    assert body["_id"] == seed_data.EVENT_ID
    assert body["title"] == "Rooftop Acoustic Session"
    assert body["attendance"]["attendee_count"] == 16


def test_update_event_persists_changes(client):
    response = client.patch(
        f"/events/{seed_data.EVENT_ID}",
        json={
            "title": "Rooftop Acoustic Session Late Edition",
            "status": "sold_out",
            "capacity": {"max_attendees": 24},
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["title"] == "Rooftop Acoustic Session Late Edition"
    assert body["status"] == "sold_out"


def test_join_event_adds_new_attendee_and_updates_attendance(client, seeded_db):
    signup_response = client.post(
        "/auth/signup",
        json={
            "email": "joiner@example.com",
            "password": "secret123",
            "display_name": "Joiner",
            "username": "joiner",
        },
    )
    joiner_id = signup_response.json()["_id"]

    response = client.post(
        f"/events/{seed_data.EVENT_ID}/join",
        json={
            "user_id": joiner_id,
            "role": "attendee",
            "rsvp_status": "joined",
            "discovery_source": "search",
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["user_id"] == joiner_id
    assert body["rsvp_status"] == "joined"

    event = seeded_db.events.find_one({"_id": seed_data.EVENT_ID})
    assert joiner_id in event["chat"]["participant_user_ids"]
    assert event["attendance"]["attendee_count"] == 17


def test_get_user_events_returns_seeded_event_refs(client):
    response = client.get(f"/users/{seed_data.CURRENT_USER_ID}/events")

    assert response.status_code == 200
    assert response.json()["hosted_event_ids"] == [seed_data.EVENT_ID]


def test_get_event_attendees_returns_seeded_users_with_display_names(client):
    response = client.get(f"/events/{seed_data.EVENT_ID}/attendees")

    assert response.status_code == 200
    attendees = response.json()
    assert len(attendees) == 17
    assert any(item["display_name"] == "Animesh" for item in attendees)
    assert any(item["display_name"] == "Nikhil" for item in attendees)


def test_get_event_score_uses_stubbed_matcher_with_seeded_entities(client, monkeypatch):
    monkeypatch.setattr(
        routes,
        "score_user_for_event",
        lambda user, event, creator, attendees: {
            "user_id": user["_id"],
            "event_id": event["_id"],
            "score": 91,
            "reasoning": f"{user['display_name']} matches {event['title']}",
            "what_matches": ["music", "small crowd"],
            "what_does_not_match": [],
            "model": "test-model",
        },
    )

    response = client.get(f"/events/{seed_data.EVENT_ID}/score/usr-attendee-01")

    assert response.status_code == 200
    body = response.json()
    assert body["score"] == 91
    assert body["event_id"] == seed_data.EVENT_ID


def test_broadcast_uses_seeded_data_and_returns_no_candidates_when_everyone_is_already_in_event(
    client, monkeypatch
):
    monkeypatch.setattr(
        routes,
        "score_user_for_event",
        lambda user, event, creator, attendees: {
            "user_id": user["_id"],
            "event_id": event["_id"],
            "score": 75,
            "reasoning": "stub",
            "what_matches": ["music"],
            "what_does_not_match": [],
            "model": "test-model",
        },
    )

    response = client.post(f"/events/{seed_data.EVENT_ID}/broadcast?limit=5")

    assert response.status_code == 200
    body = response.json()
    assert body["event_id"] == seed_data.EVENT_ID
    assert body["evaluated_candidates"] == 0
    assert body["broadcasted_candidates"] == 0
    assert body["results"] == []
