import scripts.seed_android_mock_data as seed_data


def test_healthcheck(client):
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_signup_creates_user_and_main_thread(client, seeded_db):
    payload = {
        "email": "new-user@example.com",
        "password": "secret123",
        "display_name": "New User",
        "username": "newuser",
        "avatar_url": "avatar_new",
    }

    response = client.post("/auth/signup", json=payload)

    assert response.status_code == 200
    body = response.json()
    assert body["email"] == payload["email"]
    assert body["display_name"] == payload["display_name"]
    assert body["main_thread_id"] == f"thread_main_{body['_id']}"

    thread = seeded_db.chat_threads.find_one({"_id": body["main_thread_id"]})
    assert thread is not None
    assert thread["participant_user_ids"] == [body["_id"]]


def test_signup_rejects_duplicate_email_from_seeded_user(client):
    response = client.post(
        "/auth/signup",
        json={
            "email": seed_data.CURRENT_USER_EMAIL,
            "password": "irrelevant",
            "display_name": "Duplicate",
        },
    )

    assert response.status_code == 409
    assert response.json()["detail"] == "email already exists"


def test_signin_returns_seeded_user(client):
    response = client.post(
        "/auth/signin",
        json={
            "email": seed_data.CURRENT_USER_EMAIL,
            "password": "seeded-password",
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["_id"] == seed_data.CURRENT_USER_ID
    assert body["display_name"] == "Animesh"


def test_signin_rejects_invalid_credentials(client):
    response = client.post(
        "/auth/signin",
        json={
            "email": seed_data.CURRENT_USER_EMAIL,
            "password": "wrong-password",
        },
    )

    assert response.status_code == 401
    assert response.json()["detail"] == "invalid credentials"


def test_google_auth_updates_existing_user(client):
    response = client.post(
        "/auth/google",
        json={
            "email": "animesh+google@example.com",
            "google_id": "google-animesh",
            "display_name": "Animesh Google",
            "username": "animesh-google",
            "avatar_url": "avatar_google",
        },
    )
    assert response.status_code == 200
    created = response.json()

    update_response = client.post(
        "/auth/google",
        json={
            "email": "animesh+google-updated@example.com",
            "google_id": "google-animesh",
            "display_name": "Animesh Updated",
            "username": "animesh-updated",
            "avatar_url": "avatar_updated",
        },
    )

    assert update_response.status_code == 200
    updated = update_response.json()
    assert updated["_id"] == created["_id"]
    assert updated["email"] == "animesh+google-updated@example.com"
    assert updated["display_name"] == "Animesh Updated"


def test_google_auth_creates_new_user_and_thread(client, seeded_db):
    response = client.post(
        "/auth/google",
        json={
            "email": "fresh-google@example.com",
            "google_id": "google-fresh",
            "display_name": "Fresh Google User",
            "username": "fresh-google-user",
            "avatar_url": "avatar_fresh",
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["auth_providers"] == ["google"]

    thread = seeded_db.chat_threads.find_one({"_id": body["main_thread_id"]})
    assert thread is not None
    assert thread["participant_user_ids"] == [body["_id"]]
