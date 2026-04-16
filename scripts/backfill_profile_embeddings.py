from app.community import backfill_profile_embeddings
from app.db import ensure_indexes


def main() -> None:
    ensure_indexes()
    result = backfill_profile_embeddings()
    print(result)


if __name__ == "__main__":
    main()
