import datetime
import aiosqlite
from typing import Dict, List, Optional
from ..config import settings

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
    async with aiosqlite.connect(settings.database_path) as db:
        await db.execute(CREATE_JOBS_TABLE)
        await db.execute(CREATE_EVENTS_TABLE)
        await db.commit()

async def insert_job(job: Dict):
    now = datetime.datetime.utcnow().isoformat()
    async with aiosqlite.connect(settings.database_path) as db:
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

    async with aiosqlite.connect(settings.database_path) as db:
        query = f"UPDATE download_jobs SET {', '.join(set_clauses)} WHERE job_id = ?"
        await db.execute(query, values)
        await db.commit()

async def get_job(job_id: str) -> Optional[Dict]:
    async with aiosqlite.connect(settings.database_path) as db:
        db.row_factory = aiosqlite.Row
        async with db.execute("SELECT * FROM download_jobs WHERE job_id = ?", (job_id,)) as cursor:
            row = await cursor.fetchone()
            if row:
                return dict(row)
    return None

async def list_jobs(limit: int = 50) -> List[Dict]:
    async with aiosqlite.connect(settings.database_path) as db:
        db.row_factory = aiosqlite.Row
        async with db.execute("SELECT * FROM download_jobs ORDER BY created_at DESC LIMIT ?", (limit,)) as cursor:
            rows = await cursor.fetchall()
            return [dict(r) for r in rows]

async def log_event(job_id: str, event_type: str, details: str = ""):
    now = datetime.datetime.utcnow().isoformat()
    async with aiosqlite.connect(settings.database_path) as db:
        await db.execute(
            "INSERT INTO job_events (job_id, event_type, details, timestamp) VALUES (?, ?, ?, ?)",
            (job_id, event_type, details, now)
        )
        await db.commit()
