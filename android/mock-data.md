# Mock Data Notes

This Android v1 app uses local seeded data only.

The seeded data is isolated so it can be replaced by an AWS-backed demo source later without rewriting screen code.

## Current mock data locations

- `app/src/main/java/spaces/bayesmech/com/data/mock/MockSeedData.kt`
  Holds the seeded current user and initial chat transcript.

- `app/src/main/java/spaces/bayesmech/com/data/mock/MockRepositories.kt`
  Exposes the seeded data through repository interfaces used by the UI.

## Rule for future work

- Do not place demo content directly inside composables.
- Keep all placeholder users, messages, events, and profile data behind repository interfaces.
- When the live demo backend is introduced, swap repository implementations first and keep screen APIs stable.
