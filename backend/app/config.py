import os
from typing import List
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    app_env: str = "production"
    app_host: str = "0.0.0.0"
    app_port: int = 8000
    public_api_base_url: str = ""
    cloudflare_tunnel_hostname: str = ""
    api_token: str = ""  # Optional Bearer Token

    # Worker lucidadl configuration
    lucidadl_executable: str = os.getenv("LUCIDADL_EXECUTABLE", "lucidadl")
    xvfb_run_executable: str = os.getenv("XVFB_RUN_EXECUTABLE", "xvfb-run")
    lucidadl_workdir: str = os.getenv("LUCIDADL_WORKDIR", "/tmp/sonora_downloads")
    lucidadl_timeout_seconds: int = int(os.getenv("LUCIDADL_TIMEOUT_SECONDS", "300"))
    worker_concurrency: int = 1

    # Database
    database_path: str = os.getenv("DATABASE_PATH", "sonora_jobs.db")

    # CORS
    cors_origins: List[str] = ["*"]

    class Config:
        env_file = ".env"
        extra = "ignore"

settings = Settings()
