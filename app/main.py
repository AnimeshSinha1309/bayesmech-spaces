from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.db import ensure_indexes
from app.routes import router


@asynccontextmanager
async def lifespan(_: FastAPI):
    ensure_indexes()
    yield


app = FastAPI(title="Third Space Backend", version="0.1.0", lifespan=lifespan)
app.include_router(router)
