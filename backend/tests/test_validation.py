import pytest
from app.schemas.jobs import (
    validate_and_normalize_asin,
    build_amazon_music_url,
    EntityType,
    JobPayload,
    JobPayloadItem
)

def test_valid_asin_normalization():
    assert validate_and_normalize_asin("B00C069H6M") == "B00C069H6M"
    assert validate_and_normalize_asin("  b083z9374m  ") == "B083Z9374M"

def test_invalid_asin():
    with pytest.raises(ValueError):
        validate_and_normalize_asin("B08123")  # Too short
    with pytest.raises(ValueError):
        validate_and_normalize_asin("B08123456789")  # Too long
    with pytest.raises(ValueError):
        validate_and_normalize_asin("B08123456#")  # Special chars

def test_url_generator():
    track_url = build_amazon_music_url("B083Z9374M", EntityType.TRACK)
    assert track_url == "https://music.amazon.com/tracks/B083Z9374M"

    album_url = build_amazon_music_url("B00C069H6M", EntityType.ALBUM)
    assert album_url == "https://music.amazon.com/albums/B00C069H6M"

def test_job_payload_validation():
    item = JobPayloadItem(
        asin="B00C069H6M",
        type=EntityType.ALBUM,
        url="https://music.amazon.com/albums/B00C069H6M",
        title="Random Access Memories",
        artist="Daft Punk"
    )
    payload = JobPayload(items=[item])
    assert payload.schema_version == "1.0"
    assert payload.source == "amazon_music"
    assert payload.items[0].asin == "B00C069H6M"
