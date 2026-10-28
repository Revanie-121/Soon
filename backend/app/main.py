from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from .config import settings
from .database.db import init_db
from .workers.lucidadl_worker import worker
from .api.routes import router

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: initialize SQLite and start background worker
    await init_db()
    await worker.start()
    yield
    # Shutdown: cleanly terminate workers and processes
    await worker.stop()

app = FastAPI(
    title="Sonora Download Engine API",
    description="Backend API powering Sonora music metadata extraction & lucidadl processing on Termux / Ubuntu",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.app_host,
        port=settings.app_port,
        reload=settings.app_env == "development"
    )
