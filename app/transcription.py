from fastapi import HTTPException
from openai import APIError, OpenAI, RateLimitError

from app.config import settings


def transcribe_audio_bytes(
    *,
    filename: str,
    content_type: str | None,
    audio_bytes: bytes,
) -> dict[str, str]:
    if not settings.openai_api_key:
        raise HTTPException(status_code=500, detail="OPENAI_KEY is not configured")
    if not audio_bytes:
        raise HTTPException(status_code=400, detail="audio file is empty")

    client = OpenAI(api_key=settings.openai_api_key)
    try:
        response = client.audio.transcriptions.create(
            model="gpt-4o-mini-transcribe",
            file=(filename or "voice-note.m4a", audio_bytes, content_type or "audio/mp4"),
        )
    except RateLimitError as exc:
        raise HTTPException(status_code=503, detail="OpenAI quota exceeded for audio transcription") from exc
    except APIError as exc:
        raise HTTPException(status_code=502, detail="OpenAI request failed during audio transcription") from exc

    transcript = (getattr(response, "text", None) or "").strip()
    if not transcript:
        raise HTTPException(status_code=502, detail="OpenAI transcription response was empty")

    return {
        "text": transcript,
        "model": "gpt-4o-mini-transcribe",
    }
