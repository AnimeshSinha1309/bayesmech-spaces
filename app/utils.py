from datetime import datetime, timezone
from uuid import uuid4


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def prefixed_id(prefix: str) -> str:
    return f"{prefix}_{uuid4().hex[:12]}"


def dm_thread_id(user_a: str, user_b: str) -> str:
    ordered = sorted([user_a, user_b])
    return f"thread_dm_{ordered[0]}_{ordered[1]}"
