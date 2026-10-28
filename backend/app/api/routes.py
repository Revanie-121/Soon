import uuid
from typing import List, Optional
from fastapi import APIRouter, HTTPException, Query, Header, status
from ..config import settings
from ..schemas.jobs import (
    JobPayload,
    JobCreateResponse,
    JobStatusResponse,
    MusicMetadata,
    HealthResponse
)
from ..database import db
from ..providers.amazon_music import amazon_provider
from ..workers.lucidadl_worker import worker

router = APIRouter(prefix="/api")

def verify_token(authorization: Optional[str] = Header(None)):
    if settings.api_token:
        if not authorization or not authorization.startswith("Bearer "):
            raise HTTPException(status_code=401, detail="Missing or invalid Bearer token")
        token = authorization.split("Bearer ")[1].strip()
        if token != settings.api_token:
            raise HTTPException(status_code=403, detail="Forbidden: Invalid token")

@router.get("/health", response_model=HealthResponse)
async def check_health():
    worker_status = "busy" if len(worker.active_processes) > 0 else "idle"
    return HealthResponse(
        status="ok",
        version="1.0.0",
        worker_status=worker_status,
        provider_status="ready"
    )

@router.get("/search", response_model=List[MusicMetadata])
async def search_music(
    q: str = Query(..., min_length=1, description="Query string"),
    type: str = Query("all", description="all, track, or album"),
    limit: int = Query(20, ge=1, le=50)
):
    return await amazon_provider.search_music(query=q, filter_type=type, limit=limit)

@router.post("/jobs", response_model=JobCreateResponse, status_code=status.HTTP_201_CREATED)
async def create_job(payload: JobPayload, authorization: Optional[str] = Header(None)):
    verify_token(authorization)

    first_item = payload.items[0]
    job_id = payload.job_id or str(uuid.uuid4())

    # Check if job already exists
    existing = await db.get_job(job_id)
    if existing:
        return JobCreateResponse(
            job_id=job_id,
            status=existing["status"],
            message="Existing job returned (idempotent)"
        )

    job_data = {
        "job_id": job_id,
        "asin": first_item.asin,
        "type": first_item.type.value,
        "url": first_item.url,
        "title": first_item.title,
        "artist": first_item.artist,
        "year": first_item.year,
        "status": "queued",
        "progress": 0.0,
        "message": "Job queued for download worker"
    }

    await db.insert_job(job_data)
    await db.log_event(job_id, "QUEUED", f"Enqueued URL: {first_item.url}")
    await worker.enqueue_job(job_id)

    return JobCreateResponse(
        job_id=job_id,
        status="queued",
        message="Job added to download queue"
    )

@router.get("/jobs", response_model=List[JobStatusResponse])
async def list_all_jobs(limit: int = Query(50, ge=1, le=100)):
    raw_jobs = await db.list_jobs(limit=limit)
    return [
        JobStatusResponse(
            job_id=j["job_id"],
            status=j["status"],
            progress=j.get("progress"),
            message=j.get("message"),
            direct_url=j.get("direct_url"),
            error=j.get("error"),
            created_at=j.get("created_at"),
            updated_at=j.get("updated_at"),
            logs=j.get("logs")
        ) for j in raw_jobs
    ]

@router.get("/jobs/{job_id}", response_model=JobStatusResponse)
async def get_job_status(job_id: str):
    job = await db.get_job(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    return JobStatusResponse(
        job_id=job["job_id"],
        status=job["status"],
        progress=job.get("progress"),
        message=job.get("message"),
        direct_url=job.get("direct_url"),
        error=job.get("error"),
        created_at=job.get("created_at"),
        updated_at=job.get("updated_at"),
        logs=job.get("logs")
    )

@router.post("/jobs/{job_id}/cancel")
async def cancel_job_endpoint(job_id: str, authorization: Optional[str] = Header(None)):
    verify_token(authorization)
    job = await db.get_job(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    success = await worker.cancel_job(job_id)
    if not success:
        # If not active in worker, update directly
        await db.update_job(job_id, {"status": "cancelled", "message": "Job marked as cancelled"})

    return {"status": "ok", "message": f"Job {job_id} cancelled"}

@router.post("/jobs/{job_id}/retry")
async def retry_job_endpoint(job_id: str, authorization: Optional[str] = Header(None)):
    verify_token(authorization)
    job = await db.get_job(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    await db.update_job(job_id, {
        "status": "queued",
        "progress": 0.0,
        "direct_url": None,
        "error": None,
        "message": "Requeued for download"
    })
    await worker.enqueue_job(job_id)
    return {"status": "ok", "message": f"Job {job_id} requeued"}

@router.get("/jobs/{job_id}/result", response_model=JobStatusResponse)
async def get_job_result(job_id: str):
    job = await db.get_job(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    if job["status"] != "completed" or not job.get("direct_url"):
        raise HTTPException(
            status_code=400,
            detail=f"Job is not completed yet (current status: {job['status']})"
        )

    return JobStatusResponse(
        job_id=job["job_id"],
        status=job["status"],
        progress=job.get("progress"),
        message=job.get("message"),
        direct_url=job.get("direct_url"),
        error=job.get("error"),
        created_at=job.get("created_at"),
        updated_at=job.get("updated_at")
    )
