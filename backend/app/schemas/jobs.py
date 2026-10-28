import re
from enum import Enum
from typing import List, Optional
from pydantic import BaseModel, Field, field_validator

class EntityType(str, Enum):
    TRACK = "TRACK"
    ALBUM = "ALBUM"

class JobStatusEnum(str, Enum):
    QUEUED = "queued"
    STARTING = "starting"
    PROCESSING = "processing"
    WAITING_FOR_RESULT = "waiting_for_result"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"
    TIMED_OUT = "timed_out"

ASIN_REGEX = re.compile(r"^[A-Z0-9]{10}$")

def validate_and_normalize_asin(asin: str) -> str:
    if not asin:
        raise ValueError("ASIN cannot be empty")
    cleaned = asin.strip().upper()
    if len(cleaned) != 10 or not ASIN_REGEX.match(cleaned):
        raise ValueError(f"Invalid ASIN '{asin}'. Must be exactly 10 alphanumeric characters.")
    return cleaned

def build_amazon_music_url(asin: str, entity_type: EntityType) -> str:
    clean_asin = validate_and_normalize_asin(asin)
    if entity_type == EntityType.ALBUM:
        return f"https://music.amazon.com/albums/{clean_asin}"
    elif entity_type == EntityType.TRACK:
        return f"https://music.amazon.com/tracks/{clean_asin}"
    raise ValueError(f"Unsupported entity type: {entity_type}")

class JobPayloadItem(BaseModel):
    asin: str
    type: EntityType
    url: str
    title: str = "Unknown Title"
    artist: str = "Unknown Artist"
    year: Optional[int] = None

    @field_validator("asin")
    @classmethod
    def check_asin(cls, v: str) -> str:
        return validate_and_normalize_asin(v)

    @field_validator("url")
    @classmethod
    def verify_url(cls, v: str, info) -> str:
        # Re-build canonical URL to ensure matching rules
        asin = info.data.get("asin")
        entity_type = info.data.get("type")
        if asin and entity_type:
            expected = build_amazon_music_url(asin, entity_type)
            if v != expected:
                return expected  # Normalize to canonical
        return v

class JobPayload(BaseModel):
    schema_version: str = "1.0"
    job_id: Optional[str] = None
    source: str = "amazon_music"
    items: List[JobPayloadItem] = Field(..., min_length=1)

class JobCreateResponse(BaseModel):
    job_id: str
    status: str
    message: str

class JobStatusResponse(BaseModel):
    job_id: str
    status: str
    progress: Optional[float] = None
    message: Optional[str] = None
    direct_url: Optional[str] = None
    error: Optional[str] = None
    created_at: Optional[str] = None
    updated_at: Optional[str] = None
    logs: Optional[str] = None

class MusicMetadata(BaseModel):
    asin: str
    title: str
    artist: str
    year: Optional[int] = None
    type: EntityType = EntityType.TRACK
    artwork_url: Optional[str] = None
    source_url: Optional[str] = None

class HealthResponse(BaseModel):
    status: str
    version: str = "1.0.0"
    worker_status: str = "idle"
    provider_status: str = "ready"
