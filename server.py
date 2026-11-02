#!/usr/bin/env python3
"""
=============================================================================
SONORA DOWNLOAD ENGINE - STANDALONE SERVER (FASTAPI + LUCIDADL)
=============================================================================
File ini adalah server mandiri (single-file server) lengkap untuk Sonora.
Dapat dijalankan langsung di Termux / Ubuntu proot-distro dengan:
    python3 server.py
atau
    uvicorn server:app --host 0.0.0.0 --port 8000
=============================================================================
"""

import asyncio
import datetime
import os
import re
import shutil
import uuid
from contextlib import asynccontextmanager
from enum import Enum
from typing import Dict, List, Optional

import aiosqlite
import httpx
import uvicorn
from fastapi import FastAPI, HTTPException, Query, Header, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, field_validator

# =============================================================================
# KONFIGURASI ENVIRONMENT
# =============================================================================
APP_HOST = os.getenv("APP_HOST", "0.0.0.0")
APP_PORT = int(os.getenv("APP_PORT", "8000"))
API_TOKEN = os.getenv("API_TOKEN", "")
DATABASE_PATH = os.getenv("DATABASE_PATH", "sonora_jobs.db")
LUCIDADL_EXECUTABLE = os.getenv("LUCIDADL_EXECUTABLE", "lucidadl")
XVFB_RUN_EXECUTABLE = os.getenv("XVFB_RUN_EXECUTABLE", "xvfb-run")
LUCIDADL_WORKDIR = os.getenv("LUCIDADL_WORKDIR", "/tmp/sonora_downloads")
LUCIDADL_TIMEOUT_SECONDS = int(os.getenv("LUCIDADL_TIMEOUT_SECONDS", "300"))
WORKER_CONCURRENCY = int(os.getenv("WORKER_CONCURRENCY", "1"))

# Regex untuk mencocokkan tautan unduhan langsung (audio stream / archive)
DIRECT_LINK_REGEX = re.compile(
    r'(https?://[^\s"\'<>]+(?:mp3|flac|m4a|aac|wav|zip|download|stream|media|audio|track)[^\s"\'<>]*)',
    re.IGNORECASE
)

# Regex validasi ASIN 10 karakter alfanumerik
ASIN_REGEX = re.compile(r"^[A-Z0-9]{10}$")

# =============================================================================
# VALIDASI ASIN & GENERATOR URL AMAZON MUSIC
# =============================================================================
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

# =============================================================================
# PYDANTIC SCHEMAS
# =============================================================================
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
        asin = info.data.get("asin")
        entity_type = info.data.get("type")
        if asin and entity_type:
            expected = build_amazon_music_url(asin, entity_type)
            if v != expected:
                return expected
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

# =============================================================================
# DATABASE (SQLITE)
# =============================================================================
CREATE_JOBS_TABLE = """
CREATE TABLE IF NOT EXISTS download_jobs (
    job_id TEXT PRIMARY KEY,
    asin TEXT NOT NULL,
    type TEXT NOT NULL,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    year INTEGER,
    status TEXT NOT NULL,
    progress REAL,
    message TEXT,
    direct_url TEXT,
    error TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    logs TEXT
);
"""

CREATE_EVENTS_TABLE = """
CREATE TABLE IF NOT EXISTS job_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    details TEXT,
    timestamp TEXT NOT NULL,
    FOREIGN KEY(job_id) REFERENCES download_jobs(job_id)
);
"""

async def init_db():
    async with aiosqlite.connect(DATABASE_PATH) as db:
        await db.execute(CREATE_JOBS_TABLE)
        await db.execute(CREATE_EVENTS_TABLE)
        await db.commit()

async def insert_job(job: Dict):
    now = datetime.datetime.utcnow().isoformat()
    async with aiosqlite.connect(DATABASE_PATH) as db:
        await db.execute(
            """
            INSERT INTO download_jobs (
                job_id, asin, type, url, title, artist, year,
                status, progress, message, direct_url, error,
                created_at, updated_at, logs
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                job["job_id"], job["asin"], job["type"], job["url"],
                job.get("title", ""), job.get("artist", ""), job.get("year"),
                job.get("status", "queued"), job.get("progress"), job.get("message"),
                job.get("direct_url"), job.get("error"), now, now, job.get("logs")
            )
        )
        await db.commit()

async def update_job(job_id: str, updates: Dict):
    now = datetime.datetime.utcnow().isoformat()
    updates["updated_at"] = now
    set_clauses = [f"{k} = ?" for k in updates.keys()]
    values = list(updates.values())
    values.append(job_id)

    async with aiosqlite.connect(DATABASE_PATH) as db:
        query = f"UPDATE download_jobs SET {', '.join(set_clauses)} WHERE job_id = ?"
        await db.execute(query, values)
        await db.commit()

async def get_job(job_id: str) -> Optional[Dict]:
    async with aiosqlite.connect(DATABASE_PATH) as db:
        db.row_factory = aiosqlite.Row
        async with db.execute("SELECT * FROM download_jobs WHERE job_id = ?", (job_id,)) as cursor:
            row = await cursor.fetchone()
            if row:
                return dict(row)
    return None

async def list_jobs(limit: int = 50) -> List[Dict]:
    async with aiosqlite.connect(DATABASE_PATH) as db:
        db.row_factory = aiosqlite.Row
        async with db.execute("SELECT * FROM download_jobs ORDER BY created_at DESC LIMIT ?", (limit,)) as cursor:
            rows = await cursor.fetchall()
            return [dict(r) for r in rows]

async def log_event(job_id: str, event_type: str, details: str = ""):
    now = datetime.datetime.utcnow().isoformat()
    async with aiosqlite.connect(DATABASE_PATH) as db:
        await db.execute(
            "INSERT INTO job_events (job_id, event_type, details, timestamp) VALUES (?, ?, ?, ?)",
            (job_id, event_type, details, now)
        )
        await db.commit()

# =============================================================================
# WORKER LUCIDADL DENGAN DISPLAY VIRTUAL XVFB
# =============================================================================
class LucidaDLWorker:
    def __init__(self):
        self.queue: asyncio.Queue = asyncio.Queue()
        self.active_processes: Dict[str, asyncio.subprocess.Process] = {}
        self.is_running = False
        self._worker_task: Optional[asyncio.Task] = None
        self.semaphore = asyncio.Semaphore(WORKER_CONCURRENCY)

    async def start(self):
        if not self.is_running:
            self.is_running = True
            os.makedirs(LUCIDADL_WORKDIR, exist_ok=True)
            self._worker_task = asyncio.create_task(self._process_queue())

    async def stop(self):
        self.is_running = False
        if self._worker_task:
            self._worker_task.cancel()
        for job_id, proc in list(self.active_processes.items()):
            try:
                proc.terminate()
            except Exception:
                pass

    async def enqueue_job(self, job_id: str):
        await self.queue.put(job_id)

    async def cancel_job(self, job_id: str) -> bool:
        if job_id in self.active_processes:
            proc = self.active_processes[job_id]
            try:
                proc.terminate()
                await asyncio.sleep(0.5)
                if proc.returncode is None:
                    proc.kill()
            except Exception:
                pass
            await update_job(job_id, {
                "status": "cancelled",
                "message": "Job dibatalkan oleh pengguna"
            })
            await log_event(job_id, "CANCELLED", "Proses dihentikan pengguna")
            return True
        return False

    async def _process_queue(self):
        while self.is_running:
            try:
                job_id = await self.queue.get()
                async with self.semaphore:
                    await self._execute_download_job(job_id)
                self.queue.task_done()
            except asyncio.CancelledError:
                break
            except Exception:
                await asyncio.sleep(1)

    async def _execute_download_job(self, job_id: str):
        job = await get_job(job_id)
        if not job or job.get("status") == "cancelled":
            return

        target_url = job.get("url")
        log_buffer = []

        await update_job(job_id, {
            "status": "starting",
            "message": "Menginisialisasi xvfb-run dan lucidadl...",
            "progress": 0.1
        })
        await log_event(job_id, "STARTING", f"Memulai worker untuk {target_url}")

        xvfb_cmd = shutil.which(XVFB_RUN_EXECUTABLE) or XVFB_RUN_EXECUTABLE
        lucidadl_cmd = shutil.which(LUCIDADL_EXECUTABLE) or LUCIDADL_EXECUTABLE

        # Periksa ketersediaan executable
        if not shutil.which(lucidadl_cmd) and not os.path.exists(lucidadl_cmd):
            error_msg = (
                f"Binary '{LUCIDADL_EXECUTABLE}' tidak ditemukan di PATH. "
                "Pastikan Anda menjalankan: pip install lucidadl di virtual environment Anda."
            )
            await update_job(job_id, {
                "status": "failed",
                "error": error_msg,
                "message": "lucidadl executable tidak ditemukan"
            })
            await log_event(job_id, "FAILED", error_msg)
            return

        cmd = [xvfb_cmd, "-a", lucidadl_cmd, target_url]

        try:
            await update_job(job_id, {
                "status": "processing",
                "message": "Memproses ekstraksi audio dengan lucidadl...",
                "progress": 0.4
            })

            process = await asyncio.create_subprocess_exec(
                *cmd,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
                cwd=LUCIDADL_WORKDIR
            )
            self.active_processes[job_id] = process

            direct_link = None
            try:
                stdout_bytes, stderr_bytes = await asyncio.wait_for(
                    process.communicate(),
                    timeout=LUCIDADL_TIMEOUT_SECONDS
                )
                stdout_text = stdout_bytes.decode("utf-8", errors="replace")
                stderr_text = stderr_bytes.decode("utf-8", errors="replace")
                combined_output = f"{stdout_text}\n{stderr_text}"
                log_buffer.append(combined_output)

                match = DIRECT_LINK_REGEX.search(combined_output)
                if match:
                    direct_link = match.group(1)

            except asyncio.TimeoutError:
                process.terminate()
                await asyncio.sleep(0.5)
                if process.returncode is None:
                    process.kill()
                await update_job(job_id, {
                    "status": "timed_out",
                    "error": f"Pekerjaan melebihi batas waktu {LUCIDADL_TIMEOUT_SECONDS}s",
                    "message": "Timeout menunggu output lucidadl"
                })
                await log_event(job_id, "TIMED_OUT", "Subprocess timeout")
                return
            finally:
                self.active_processes.pop(job_id, None)

            if process.returncode == 0 and direct_link:
                await update_job(job_id, {
                    "status": "completed",
                    "direct_url": direct_link,
                    "progress": 1.0,
                    "message": "Unduhan selesai. Direct link berhasil didapatkan.",
                    "logs": "\n".join(log_buffer)[:4000]
                })
                await log_event(job_id, "COMPLETED", f"Direct link: {direct_link}")
            elif process.returncode == 0 and not direct_link:
                await update_job(job_id, {
                    "status": "waiting_for_result",
                    "message": "Proses selesai, sedang memverifikasi index file...",
                    "logs": "\n".join(log_buffer)[:4000]
                })
            else:
                await update_job(job_id, {
                    "status": "failed",
                    "error": f"lucidadl keluar dengan status kode {process.returncode}",
                    "message": "Proses gagal saat ekstraksi audio",
                    "logs": "\n".join(log_buffer)[:4000]
                })
                await log_event(job_id, "FAILED", f"Exit code {process.returncode}")

        except Exception as e:
            self.active_processes.pop(job_id, None)
            await update_job(job_id, {
                "status": "failed",
                "error": str(e),
                "message": "Kesalahan worker tidak terduga"
            })
            await log_event(job_id, "FAILED", str(e))

worker = LucidaDLWorker()

# =============================================================================
# METADATA SAMPLE & SEARCH PROVIDER
# =============================================================================
SAMPLE_CATALOG = [
    MusicMetadata(
        asin="B00C069H6M",
        title="Random Access Memories",
        artist="Daft Punk",
        year=2013,
        type=EntityType.ALBUM,
        artwork_url="https://images-na.ssl-images-amazon.com/images/I/61U7T985z8L._SX500_.jpg",
        source_url="https://music.amazon.com/albums/B00C069H6M"
    ),
    MusicMetadata(
        asin="B083Z9374M",
        title="Blinding Lights",
        artist="The Weeknd",
        year=2020,
        type=EntityType.TRACK,
        artwork_url="https://images-na.ssl-images-amazon.com/images/I/71u%2B4jU57rL._SX500_.jpg",
        source_url="https://music.amazon.com/tracks/B083Z9374M"
    ),
    MusicMetadata(
        asin="B00138F12A",
        title="Bohemian Rhapsody",
        artist="Queen",
        year=1975,
        type=EntityType.TRACK,
        artwork_url="https://images-na.ssl-images-amazon.com/images/I/71B%2B1T7eNmL._SX500_.jpg",
        source_url="https://music.amazon.com/tracks/B00138F12A"
    ),
    MusicMetadata(
        asin="B07PB2341M",
        title="When We All Fall Asleep, Where Do We Go?",
        artist="Billie Eilish",
        year=2019,
        type=EntityType.ALBUM,
        artwork_url="https://images-na.ssl-images-amazon.com/images/I/61kC8H6zM0L._SX500_.jpg",
        source_url="https://music.amazon.com/albums/B07PB2341M"
    ),
    MusicMetadata(
        asin="B07V2QJ4PL",
        title="Bad Guy",
        artist="Billie Eilish",
        year=2019,
        type=EntityType.TRACK,
        artwork_url="https://images-na.ssl-images-amazon.com/images/I/61kC8H6zM0L._SX500_.jpg",
        source_url="https://music.amazon.com/tracks/B07V2QJ4PL"
    ),
    MusicMetadata(
        asin="B08R92L9K8",
        title="Discovery",
        artist="Daft Punk",
        year=2001,
        type=EntityType.ALBUM,
        artwork_url="https://images-na.ssl-images-amazon.com/images/I/71P7u1F4H7L._SX500_.jpg",
        source_url="https://music.amazon.com/albums/B08R92L9K8"
    ),
    MusicMetadata(
        asin="B007XU06KM",
        title="Get Lucky (feat. Pharrell Williams)",
        artist="Daft Punk",
        year=2013,
        type=EntityType.TRACK,
        artwork_url="https://images-na.ssl-images-amazon.com/images/I/61U7T985z8L._SX500_.jpg",
        source_url="https://music.amazon.com/tracks/B007XU06KM"
    ),
    MusicMetadata(
        asin="B09V7N88W2",
        title="Harry's House",
        artist="Harry Styles",
        year=2022,
        type=EntityType.ALBUM,
        artwork_url="https://images-na.ssl-images-amazon.com/images/I/81M6bLgJzKL._SX500_.jpg",
        source_url="https://music.amazon.com/albums/B09V7N88W2"
    ),
    MusicMetadata(
        asin="B09V8G3JMK",
        title="As It Was",
        artist="Harry Styles",
        year=2022,
        type=EntityType.TRACK,
        artwork_url="https://images-na.ssl-images-amazon.com/images/I/81M6bLgJzKL._SX500_.jpg",
        source_url="https://music.amazon.com/tracks/B09V8G3JMK"
    )
]

# =============================================================================
# FASTAPI APP & ROUTES
# =============================================================================
@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    await worker.start()
    yield
    await worker.stop()

app = FastAPI(
    title="Sonora Download Engine API",
    description="Backend API powering Sonora music metadata extraction & lucidadl processing on Termux / Ubuntu",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def verify_token(authorization: Optional[str] = Header(None)):
    if API_TOKEN:
        if not authorization or not authorization.startswith("Bearer "):
            raise HTTPException(status_code=401, detail="Missing or invalid Bearer token")
        token = authorization.split("Bearer ")[1].strip()
        if token != API_TOKEN:
            raise HTTPException(status_code=403, detail="Forbidden: Invalid token")

@app.get("/")
async def root():
    return {
        "name": "Sonora Download Engine API",
        "status": "online",
        "worker": "ready",
        "version": "1.0.0",
        "docs_url": "/docs"
    }

@app.get("/api/health", response_model=HealthResponse)
async def check_health():
    worker_status = "busy" if len(worker.active_processes) > 0 else "idle"
    return HealthResponse(
        status="ok",
        version="1.0.0",
        worker_status=worker_status,
        provider_status="ready"
    )

@app.get("/api/search", response_model=List[MusicMetadata])
async def search_music(
    q: str = Query(..., min_length=1, description="Query string"),
    type: str = Query("all", description="all, track, or album"),
    limit: int = Query(20, ge=1, le=50)
):
    query_clean = q.strip().lower()
    type_clean = type.strip().upper()

    results = []
    for item in SAMPLE_CATALOG:
        match_query = (
            query_clean in item.title.lower() or
            query_clean in item.artist.lower() or
            query_clean in item.asin.lower()
        )
        match_type = (
            type_clean in ("ALL", "") or
            (type_clean in ("TRACK", "TRACKS") and item.type == EntityType.TRACK) or
            (type_clean in ("ALBUM", "ALBUMS") and item.type == EntityType.ALBUM)
        )
        if match_query and match_type:
            results.append(item)

    # Jika pencarian adalah ASIN 10 karakter yang valid, otomatis sertakan item kanonis
    if len(query_clean) == 10 and ASIN_REGEX.match(query_clean.upper()):
        asin_upper = query_clean.upper()
        if not any(r.asin == asin_upper for r in results):
            entity = EntityType.ALBUM if type_clean in ("ALBUM", "ALBUMS") else EntityType.TRACK
            results.append(
                MusicMetadata(
                    asin=asin_upper,
                    title=f"Amazon Music Item {asin_upper}",
                    artist="Amazon Music Artist",
                    type=entity,
                    source_url=build_amazon_music_url(asin_upper, entity)
                )
            )

    return results[:limit]

@app.post("/api/jobs", response_model=JobCreateResponse, status_code=status.HTTP_201_CREATED)
async def create_job(payload: JobPayload, authorization: Optional[str] = Header(None)):
    verify_token(authorization)

    first_item = payload.items[0]
    job_id = payload.job_id or str(uuid.uuid4())

    existing = await get_job(job_id)
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
        "message": "Pekerjaan ditambahkan ke antrean unduhan"
    }

    await insert_job(job_data)
    await log_event(job_id, "QUEUED", f"Enqueued URL: {first_item.url}")
    await worker.enqueue_job(job_id)

    return JobCreateResponse(
        job_id=job_id,
        status="queued",
        message="Pekerjaan berhasil masuk ke antrean unduhan"
    )

@app.get("/api/jobs", response_model=List[JobStatusResponse])
async def list_all_jobs(limit: int = Query(50, ge=1, le=100)):
    raw_jobs = await list_jobs(limit=limit)
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

@app.get("/api/jobs/{job_id}", response_model=JobStatusResponse)
async def get_job_status(job_id: str):
    job = await get_job(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Pekerjaan tidak ditemukan")

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

@app.post("/api/jobs/{job_id}/cancel")
async def cancel_job_endpoint(job_id: str, authorization: Optional[str] = Header(None)):
    verify_token(authorization)
    job = await get_job(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Pekerjaan tidak ditemukan")

    success = await worker.cancel_job(job_id)
    if not success:
        await update_job(job_id, {"status": "cancelled", "message": "Pekerjaan ditandai dibatalkan"})

    return {"status": "ok", "message": f"Pekerjaan {job_id} berhasil dibatalkan"}

@app.post("/api/jobs/{job_id}/retry")
async def retry_job_endpoint(job_id: str, authorization: Optional[str] = Header(None)):
    verify_token(authorization)
    job = await get_job(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Pekerjaan tidak ditemukan")

    await update_job(job_id, {
        "status": "queued",
        "progress": 0.0,
        "direct_url": None,
        "error": None,
        "message": "Menjadwalkan ulang unduhan..."
    })
    await worker.enqueue_job(job_id)
    return {"status": "ok", "message": f"Pekerjaan {job_id} dijadwalkan ulang"}

@app.get("/api/jobs/{job_id}/result", response_model=JobStatusResponse)
async def get_job_result(job_id: str):
    job = await get_job(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Pekerjaan tidak ditemukan")

    if job["status"] != "completed" or not job.get("direct_url"):
        raise HTTPException(
            status_code=400,
            detail=f"Pekerjaan belum selesai (status saat ini: {job['status']})"
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

# =============================================================================
# ENTRYPOINT JIKA DIJALANKAN LANGSUNG: python3 server.py
# =============================================================================
if __name__ == "__main__":
    print("=" * 60)
    print(" 🎵 SONORA DOWNLOAD ENGINE SERVER")
    print(f" 🌐 Host: {APP_HOST}:{APP_PORT}")
    print(f" 📂 Workdir: {LUCIDADL_WORKDIR}")
    print("=" * 60)
    uvicorn.run(app, host=APP_HOST, port=APP_PORT)
