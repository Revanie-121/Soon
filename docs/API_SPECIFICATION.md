# Sonora Backend REST API Specification

## Base URL
Default Local: `http://127.0.0.1:8000`
Emulator: `http://10.0.2.2:8000`
Cloudflare Tunnel: `https://<tunnel-subdomain>.trycloudflare.com`

---

## Endpoints

### 1. Health Check
- **Method:** `GET`
- **Route:** `/api/health`
- **Response:**
  ```json
  {
    "status": "ok",
    "version": "1.0.0",
    "worker_status": "idle",
    "provider_status": "ready"
  }
  ```

### 2. Search Music
- **Method:** `GET`
- **Route:** `/api/search`
- **Parameters:**
  - `q` (string, required): Query pencarian judul atau artis
  - `type` (string, optional): `all`, `track`, atau `album`
  - `limit` (integer, optional): Maksimum jumlah hasil (default: 20)
- **Response:**
  ```json
  [
    {
      "asin": "B00C069H6M",
      "title": "Random Access Memories",
      "artist": "Daft Punk",
      "year": 2013,
      "type": "ALBUM",
      "artwork_url": "https://...",
      "source_url": "https://music.amazon.com/albums/B00C069H6M"
    }
  ]
  ```

### 3. Submit Download Job
- **Method:** `POST`
- **Route:** `/api/jobs`
- **Request Body:**
  ```json
  {
    "schema_version": "1.0",
    "job_id": null,
    "source": "amazon_music",
    "items": [
      {
        "asin": "B00C069H6M",
        "type": "ALBUM",
        "url": "https://music.amazon.com/albums/B00C069H6M",
        "title": "Random Access Memories",
        "artist": "Daft Punk",
        "year": 2013
      }
    ]
  }
  ```
- **Response (201 Created):**
  ```json
  {
    "job_id": "8487f311-667d-419b-a31d-04ff53c07e2c",
    "status": "queued",
    "message": "Job added to download queue"
  }
  ```

### 4. Get Job Status (Polling)
- **Method:** `GET`
- **Route:** `/api/jobs/{job_id}`
- **Response:**
  ```json
  {
    "job_id": "8487f311-667d-419b-a31d-04ff53c07e2c",
    "status": "completed",
    "progress": 1.0,
    "message": "Download completed. Direct link acquired.",
    "direct_url": "https://cdn.example.com/audio/B00C069H6M.flac",
    "error": null,
    "created_at": "2026-10-27T17:00:00Z",
    "updated_at": "2026-10-27T17:01:30Z",
    "logs": "[Worker] xvfb-run -a lucidadl ... Exit code 0"
  }
  ```

### 5. Cancel Job
- **Method:** `POST`
- **Route:** `/api/jobs/{job_id}/cancel`
- **Response:**
  ```json
  {
    "status": "ok",
    "message": "Job 8487f311-667d-419b-a31d-04ff53c07e2c cancelled"
  }
  ```

### 6. Retry Job
- **Method:** `POST`
- **Route:** `/api/jobs/{job_id}/retry`
- **Response:**
  ```json
  {
    "status": "ok",
    "message": "Job 8487f311-667d-419b-a31d-04ff53c07e2c requeued"
  }
  ```

### 7. Get Direct Download Result
- **Method:** `GET`
- **Route:** `/api/jobs/{job_id}/result`
- **Response (200 OK):**
  ```json
  {
    "job_id": "8487f311-667d-419b-a31d-04ff53c07e2c",
    "status": "completed",
    "direct_url": "https://cdn.example.com/audio/B00C069H6M.flac"
  }
  ```
