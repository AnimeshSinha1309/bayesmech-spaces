import app.routes as routes
import scripts.seed_android_mock_data as seed_data


def test_get_main_chat_returns_seeded_thread_payload(client):
    response = client.get(f"/chat/{seed_data.CURRENT_USER_ID}")

    assert response.status_code == 200
    body = response.json()
    assert body["thread"]["thread_id"] == seed_data.CURRENT_USER_THREAD_ID
    assert [item["_id"] for item in body["items"]] == ["msg-1", "msg-2", "msg-3", "msg-4"]
    assert body["items"][2]["message_type"] == "event_card"


def test_create_main_chat_message_appends_to_seeded_thread(client):
    response = client.post(
        f"/chat/{seed_data.CURRENT_USER_ID}/messages",
        json={
            "sender_type": "user",
            "sender_user_id": seed_data.CURRENT_USER_ID,
            "message_type": "text",
            "content_text": "Show me something active for tomorrow morning.",
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["thread_id"] == seed_data.CURRENT_USER_THREAD_ID
    assert body["content_text"] == "Show me something active for tomorrow morning."


def test_main_chat_transcribe_uses_stubbed_transcriber(client, monkeypatch):
    monkeypatch.setattr(
        routes,
        "transcribe_audio_bytes",
        lambda **_: {
            "text": "Find me a run tomorrow morning.",
            "model": "fake-transcriber",
        },
    )

    response = client.post(
        f"/chat/{seed_data.CURRENT_USER_ID}/transcribe",
        files={"file": ("voice.m4a", b"audio-bytes", "audio/mp4")},
    )

    assert response.status_code == 200
    assert response.json()["text"] == "Find me a run tomorrow morning."


def test_get_event_chat_returns_seeded_thread(client):
    response = client.get(f"/events/{seed_data.EVENT_ID}/chat")

    assert response.status_code == 200
    body = response.json()
    assert body["thread"]["thread_id"] == seed_data.EVENT_THREAD_ID
    assert body["thread"]["participant_user_ids"][0] == seed_data.CURRENT_USER_ID
    assert body["items"] == []


def test_create_event_chat_message_writes_to_seeded_event_thread(client):
    response = client.post(
        f"/events/{seed_data.EVENT_ID}/chat/messages",
        json={
            "sender_type": "user",
            "sender_user_id": "usr-attendee-01",
            "message_type": "text",
            "content_text": "Looking forward to this tonight.",
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["thread_id"] == seed_data.EVENT_THREAD_ID
    assert body["event_id"] == seed_data.EVENT_ID


def test_connection_score_uses_stubbed_matcher(client, monkeypatch):
    monkeypatch.setattr(
        routes,
        "score_profiles",
        lambda first_user, second_user: {
            "user_1": first_user["_id"],
            "user_2": second_user["_id"],
            "score": 88,
            "reasoning": "Shared taste in music and low-pressure plans.",
            "what_matches": ["music", "small gatherings"],
            "what_does_not_match": [],
            "model": "test-model",
        },
    )

    response = client.get("/connections/score/user-animesh/usr-attendee-01")

    assert response.status_code == 200
    body = response.json()
    assert body["score"] == 88
    assert body["user_1"] == seed_data.CURRENT_USER_ID


def test_find_new_connections_scores_seeded_attendees(client, monkeypatch):
    monkeypatch.setattr(
        routes,
        "score_profiles",
        lambda current_user, candidate: {
            "user_1": current_user["_id"],
            "user_2": candidate["_id"],
            "score": 95 if candidate["_id"] == "usr-attendee-01" else 60,
            "reasoning": f"Scored against {candidate['display_name']}",
            "what_matches": [candidate["display_name"]],
            "what_does_not_match": [],
            "model": "test-model",
        },
    )

    response = client.get(f"/connections/findnew/{seed_data.CURRENT_USER_ID}?limit=3")

    assert response.status_code == 200
    body = response.json()
    assert body["evaluated_candidates"] == 16
    assert len(body["results"]) == 3
    assert body["results"][0]["user_id"] == "usr-attendee-01"
