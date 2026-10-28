import asyncio
import os
import re
import signal
import shutil
from typing import Dict, Optional
from ..config import settings
from ..database import db

# Pattern to capture direct download links (e.g., http/https links pointing to audio streams, CDN, or file archives)
DIRECT_LINK_REGEX = re.compile(
    r'(https?://[^\s"\'<>]+(?:mp3|flac|m4a|aac|wav|zip|download|stream|media|audio|track)[^\s"\'<>]*)',
    re.IGNORECASE
)

class LucidaDLWorker:
    def __init__(self):
        self.queue: asyncio.Queue = asyncio.Queue()
        self.active_processes: Dict[str, asyncio.subprocess.Process] = {}
        self.is_running = False
        self._worker_task: Optional[asyncio.Task] = None
        self.semaphore = asyncio.Semaphore(settings.worker_concurrency)

    async def start(self):
        if not self.is_running:
            self.is_running = True
            os.makedirs(settings.lucidadl_workdir, exist_ok=True)
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
            await db.update_job(job_id, {
                "status": "cancelled",
                "message": "Job cancelled by user request"
            })
            await db.log_event(job_id, "CANCELLED", "Process terminated by user")
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
            except Exception as e:
                await asyncio.sleep(1)

    async def _execute_download_job(self, job_id: str):
        job = await db.get_job(job_id)
        if not job or job.get("status") == "cancelled":
            return

        target_url = job.get("url")
        log_buffer = []

        await db.update_job(job_id, {
            "status": "starting",
            "message": "Initializing xvfb-run virtual display and lucidadl...",
            "progress": 0.1
        })
        await db.log_event(job_id, "STARTING", f"Starting worker for {target_url}")

        # Locate binaries
        xvfb_cmd = shutil.which(settings.xvfb_run_executable) or settings.xvfb_run_executable
        lucidadl_cmd = shutil.which(settings.lucidadl_executable) or settings.lucidadl_executable

        # Verify command availability
        if not shutil.which(lucidadl_cmd) and not os.path.exists(lucidadl_cmd):
            error_msg = (
                f"Executable '{settings.lucidadl_executable}' not found in PATH or environment. "
                "Ensure you run backend inside Ubuntu proot-distro virtualenv."
            )
            await db.update_job(job_id, {
                "status": "failed",
                "error": error_msg,
                "message": "lucidadl executable not found"
            })
            await db.log_event(job_id, "FAILED", error_msg)
            return

        # Prepare command args safely without shell=True
        # Command syntax: xvfb-run -a lucidadl [args]
        cmd = [xvfb_cmd, "-a", lucidadl_cmd, target_url]

        try:
            await db.update_job(job_id, {
                "status": "processing",
                "message": "Processing audio extraction with lucidadl...",
                "progress": 0.4
            })

            process = await asyncio.create_subprocess_exec(
                *cmd,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
                cwd=settings.lucidadl_workdir
            )
            self.active_processes[job_id] = process

            direct_link = None
            try:
                stdout_bytes, stderr_bytes = await asyncio.wait_for(
                    process.communicate(),
                    timeout=settings.lucidadl_timeout_seconds
                )
                stdout_text = stdout_bytes.decode("utf-8", errors="replace")
                stderr_text = stderr_bytes.decode("utf-8", errors="replace")
                combined_output = f"{stdout_text}\n{stderr_text}"
                log_buffer.append(combined_output)

                # Search for direct link in output
                match = DIRECT_LINK_REGEX.search(combined_output)
                if match:
                    direct_link = match.group(1)

            except asyncio.TimeoutError:
                process.terminate()
                await asyncio.sleep(0.5)
                if process.returncode is None:
                    process.kill()
                await db.update_job(job_id, {
                    "status": "timed_out",
                    "error": f"Job exceeded execution limit of {settings.lucidadl_timeout_seconds}s",
                    "message": "Timed out waiting for lucidadl output"
                })
                await db.log_event(job_id, "TIMED_OUT", "Subprocess timeout")
                return

            finally:
                self.active_processes.pop(job_id, None)

            if process.returncode == 0 and direct_link:
                await db.update_job(job_id, {
                    "status": "completed",
                    "direct_url": direct_link,
                    "progress": 1.0,
                    "message": "Download completed. Direct link acquired.",
                    "logs": "\n".join(log_buffer)[:4000]
                })
                await db.log_event(job_id, "COMPLETED", f"Acquired direct link: {direct_link}")
            elif process.returncode == 0 and not direct_link:
                # Tools succeeded but direct link requires file check
                await db.update_job(job_id, {
                    "status": "waiting_for_result",
                    "message": "Process succeeded, parsing file index...",
                    "logs": "\n".join(log_buffer)[:4000]
                })
            else:
                await db.update_job(job_id, {
                    "status": "failed",
                    "error": f"lucidadl exited with code {process.returncode}",
                    "message": "Download failed during audio extraction",
                    "logs": "\n".join(log_buffer)[:4000]
                })
                await db.log_event(job_id, "FAILED", f"Exit code {process.returncode}")

        except Exception as e:
            self.active_processes.pop(job_id, None)
            await db.update_job(job_id, {
                "status": "failed",
                "error": str(e),
                "message": "Unexpected worker exception"
            })
            await db.log_event(job_id, "FAILED", str(e))

worker = LucidaDLWorker()
