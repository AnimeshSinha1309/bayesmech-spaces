# Android Data Notes

The Android app now reads its profile and chat content from the backend server.

## Current data locations

- `app/src/main/java/spaces/bayesmech/com/data/backend/BackendRepository.kt`
  Fetches the current user and chat transcript from the backend and posts user messages back to it.

- `scripts/seed_android_mock_data.py`
  Preloads the old Android demo content into MongoDB so the frontend keeps the same behavior without shipping local seed data.

## Rule for future work

- Do not place demo content directly inside composables.
- Keep app-facing data behind repository interfaces.
- Add or reshape backend seed data when the UI needs new placeholders or richer demo scenarios.
