from pathlib import Path
import sys

import mongomock
import pytest
from fastapi.testclient import TestClient

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

import app.db as app_db
import app.routes as routes
import scripts.seed_android_mock_data as seed_data
from app.main import app


@pytest.fixture()
def fake_db(monkeypatch):
    client = mongomock.MongoClient()
    db = client["thirdspace_test"]

    monkeypatch.setattr(app_db, "db", db)
    monkeypatch.setattr(routes, "db", db)
    monkeypatch.setattr(seed_data, "db", db)

    return db


@pytest.fixture()
def seeded_db(fake_db):
    seed_data.main()
    return fake_db


@pytest.fixture()
def client(seeded_db):
    with TestClient(app) as test_client:
        yield test_client
