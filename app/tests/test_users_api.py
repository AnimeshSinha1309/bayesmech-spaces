import app.routes as routes
import scripts.seed_android_mock_data as seed_data


def test_get_user_returns_seeded_mock_user(client):
    response = client.get(f"/users/{seed_data.CURRENT_USER_ID}")

    assert response.status_code == 200
    body = response.json()
    assert body["_id"] == seed_data.CURRENT_USER_ID
    assert body["persona"]["mobile_profile"]["headline"].startswith("Curious builder")


def test_mobile_bootstrap_returns_current_user_and_main_chat(client):
    response = client.get(f"/mobile/bootstrap/{seed_data.CURRENT_USER_ID}")

    assert response.status_code == 200
    body = response.json()
    assert body["current_user"]["_id"] == seed_data.CURRENT_USER_ID
    assert body["main_chat"]["thread"]["thread_id"] == seed_data.CURRENT_USER_THREAD_ID
    assert len(body["main_chat"]["items"]) == 4


def test_update_user_persists_patch(client):
    response = client.patch(
        f"/users/{seed_data.CURRENT_USER_ID}",
        json={
            "display_name": "Animesh Sharma",
            "persona": {
                "profile_dict": {
                    "home_base": "Bengaluru",
                    "interest_tags": ["running", "music", "design"],
                }
            },
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["display_name"] == "Animesh Sharma"
    assert body["persona"]["profile_dict"]["home_base"] == "Bengaluru"


def test_profile_ai_start_uses_stubbed_session(client, monkeypatch):
    monkeypatch.setattr(
        routes,
        "start_profile_ai_session",
        lambda **_: {
            "assistant_text": "What kind of plans do you like most?",
            "updated_profile_dict": {
                "home_base": "Bengaluru",
                "work_context": "",
                "social_energy": "",
                "ideal_plans": "",
                "social_goals": "",
                "conversation_spark": "",
                "interest_tags": ["running", "music", "design"],
                "profile_notes": "",
            },
            "is_complete": False,
            "model": "test-model",
        },
    )

    response = client.post(
        f"/users/{seed_data.CURRENT_USER_ID}/profile-ai/start",
        json={"transcript": []},
    )

    assert response.status_code == 200
    assert response.json()["assistant_text"] == "What kind of plans do you like most?"


def test_profile_ai_reply_requires_user_message(client):
    response = client.post(
        f"/users/{seed_data.CURRENT_USER_ID}/profile-ai/reply",
        json={"current_profile_dict": {}, "transcript": []},
    )

    assert response.status_code == 400
    assert response.json()["detail"] == "user_message is required"


def test_profile_ai_reply_uses_stubbed_session(client, monkeypatch):
    monkeypatch.setattr(
        routes,
        "continue_profile_ai_session",
        lambda **_: {
            "assistant_text": "What sort of crowd feels right for you?",
            "updated_profile_dict": {
                "home_base": "Bengaluru",
                "work_context": "Tech and product",
                "social_energy": "",
                "ideal_plans": "",
                "social_goals": "",
                "conversation_spark": "",
                "interest_tags": ["running", "music", "design"],
                "profile_notes": "",
            },
            "is_complete": False,
            "model": "test-model",
        },
    )

    response = client.post(
        f"/users/{seed_data.CURRENT_USER_ID}/profile-ai/reply",
        json={
            "current_profile_dict": {},
            "transcript": [],
            "user_message": "I work in product and like small groups.",
        },
    )

    assert response.status_code == 200
    assert response.json()["updated_profile_dict"]["work_context"] == "Tech and product"


def test_profile_ai_transcribe_uses_stubbed_transcriber(client, monkeypatch):
    monkeypatch.setattr(
        routes,
        "transcribe_audio_bytes",
        lambda **_: {
            "text": "I like music and city walks.",
            "model": "fake-transcriber",
        },
    )

    response = client.post(
        f"/users/{seed_data.CURRENT_USER_ID}/profile-ai/transcribe",
        files={"file": ("profile.m4a", b"audio-bytes", "audio/mp4")},
    )

    assert response.status_code == 200
    assert response.json()["text"] == "I like music and city walks."


def test_profile_ai_end_stores_final_profile_dict(client, seeded_db, monkeypatch):
    final_profile = {
        "home_base": "Bengaluru",
        "work_context": "Tech and product",
        "social_energy": "Warm in small groups",
        "ideal_plans": "Runs, music nights, founder dinners",
        "social_goals": "Meet thoughtful builders",
        "conversation_spark": "Design, cities, behavior",
        "interest_tags": ["running", "music", "design"],
        "profile_notes": "Low-pressure but intentional",
    }
    monkeypatch.setattr(
        routes,
        "end_profile_ai_session",
        lambda **_: {
            "final_profile_dict": final_profile,
            "closing_text": "Your profile is ready.",
            "is_complete": True,
            "model": "test-model",
        },
    )

    response = client.post(
        f"/users/{seed_data.CURRENT_USER_ID}/profile-ai/end",
        json={"current_profile_dict": {}, "transcript": []},
    )

    assert response.status_code == 200
    assert response.json()["final_profile_dict"] == final_profile

    stored_user = seeded_db.users.find_one({"_id": seed_data.CURRENT_USER_ID})
    assert stored_user["persona"]["profile_dict"] == final_profile


def test_create_or_get_direct_message_creates_thread_and_connections(client, seeded_db):
    response = client.post(
        "/users/direct-message",
        json={
            "user_id": seed_data.CURRENT_USER_ID,
            "other_user_id": "usr-attendee-01",
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["_id"] == "thread_dm_user-animesh_usr-attendee-01"
    assert sorted(body["participant_user_ids"]) == ["user-animesh", "usr-attendee-01"]

    current_user = seeded_db.users.find_one({"_id": seed_data.CURRENT_USER_ID})
    attendee = seeded_db.users.find_one({"_id": "usr-attendee-01"})
    assert current_user["connections"][0]["user_id"] == "usr-attendee-01"
    assert attendee["connections"][0]["user_id"] == seed_data.CURRENT_USER_ID
