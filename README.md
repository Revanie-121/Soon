# Sonora — Music Search & Download Manager

Sonora adalah aplikasi pencarian metadata musik dan pengunduhan berarsitektur modern yang terhubung ke sumber metadata Amazon Music, serta ditenagai oleh mesin pemroses unduhan Python `lucidadl` yang berjalan di Termux dengan Ubuntu `proot-distro` melalui Xvfb virtual display dan Cloudflare Tunnel.

---

## Fitur Utama

1. **Pencarian Musik:**
   - Judul: *"Discover your music."*
   - Pencarian real-time dengan debounce dan filter kategori (Semua, Tracks, Albums).
   - Tampilan metadata lengkap: Artwork, Judul, Artis, Tahun Rilis, Tipe Entitas (`TRACK` / `ALBUM`), dan ASIN Amazon Music.
2. **Validasi ASIN & Generator URL Terpusat:**
   - Validasi ketat tepat 10 karakter alfanumerik.
   - Pembentukan URL kanonis:
     - `TRACK` -> `https://music.amazon.com/tracks/{ASIN}`
     - `ALBUM` -> `https://music.amazon.com/albums/{ASIN}`
3. **Download Queue & Real-Time Polling:**
   - Antrean pekerjaan dengan pembatasan concurrency (FIFO safe queue).
   - Polling status adaptif (interval awal 2s, gradual backoff hingga 10s dengan jitter acak).
   - Indikator status: `queued`, `starting`, `processing`, `waiting_for_result`, `completed`, `failed`, `cancelled`, `timed_out`.
4. **Deteksi Direct Download Link:**
   - Membaca output actual worker untuk mengekstrak direct link asli.
   - Dialog hasil lengkap dengan tombol **Salin Link**, **Buka Link**, dan **Download**.
5. **Penyimpanan Persisten (Room SQLite & Backend SQLite):**
   - Riwayat pekerjaan tetap tersimpan saat aplikasi ditutup atau dimuat ulang.
   - Filter riwayat berdasarkan status dan pencarian judul/ASIN.
6. **Konektivitas Cloudflare Tunnel:**
   - Akses aman melalui HTTPS publik tanpa port-forwarding router.
   - Pengecekan status koneksi dan latensi (Ping Health Check).
7. **Mode Pengujian (Demo / Test Mode):**
   - Tersedia tombol toggle di Settings untuk menguji alur kerja end-to-end dengan ASIN terverifikasi bahkan saat Termux belum aktif.

---

## Struktur Proyek

```
sonora/
├── app/                             # Native Android App (Kotlin, Jetpack Compose, M3, Room, Retrofit)
│   ├── src/main/java/com/example/
│   │   ├── data/
│   │   │   ├── model/               # MusicItem, DownloadJob, JobStatus, JobPayload
│   │   │   ├── local/               # Room SQLite: JobEntity, JobDao, SonoraDatabase
│   │   │   ├── remote/              # Retrofit: SonoraApiService, ApiClient
│   │   │   ├── provider/            # Metadata Provider Adapters
│   │   │   └── repository/          # AsinValidator, SonoraRepository
│   │   ├── ui/
│   │   │   ├── theme/               # Charcoal, Dark Blue, Violet & Cyan Palette
│   │   │   ├── components/          # Cards, Badges, Modals, Status Indicators
│   │   │   ├── screens/             # SearchScreen, QueueScreen, HistoryScreen, SettingsScreen
│   │   │   └── viewmodel/           # SonoraViewModel
│   │   └── MainActivity.kt
│   └── build.gradle.kts
├── backend/                         # Python FastAPI Backend
│   ├── app/
│   │   ├── api/routes.py            # Endpoints: /api/health, /api/search, /api/jobs
│   │   ├── database/db.py           # SQLite persistence for jobs and events
│   │   ├── providers/amazon_music.py# Amazon Music metadata provider adapter
│   │   ├── workers/lucidadl_worker.py # Subprocess worker: xvfb-run -a lucidadl
│   │   ├── schemas/jobs.py          # Pydantic models with ASIN validation
│   │   ├── config.py                # Environment configuration
│   │   └── main.py                  # FastAPI app entrypoint
│   ├── tests/                       # Unit & integration test suite
│   └── requirements.txt
├── scripts/
│   ├── setup_termux_ubuntu.sh       # Script instalasi otomatis Termux + Ubuntu proot-distro
│   ├── start_backend.sh             # Script peluncur backend di Ubuntu
│   └── setup_cloudflare_tunnel.sh   # Script setup Cloudflare Tunnel
├── docs/
│   ├── SETUP_TERMUX.md              # Panduan lengkap Termux & Xvfb
│   ├── CLOUDFLARE_TUNNEL.md         # Panduan Cloudflare Tunnel
│   └── API_SPECIFICATION.md         # Spesifikasi REST API
└── README.md
```

---

## Panduan Memulai Cepat (Quickstart)

### Menjalankan Backend di Termux & Ubuntu
1. Jalankan script setup:
   ```bash
   bash scripts/setup_termux_ubuntu.sh
   ```
2. Jalankan backend:
   ```bash
   bash scripts/start_backend.sh
   ```
3. Buka tunnel di tab Termux lain:
   ```bash
   bash scripts/setup_cloudflare_tunnel.sh
   ```

### Menjalankan Aplikasi Android
1. Buka aplikasi Sonora di perangkat atau emulator Android.
2. Buka tab **Settings**, masukkan URL backend (misal: `http://10.0.2.2:8000` di emulator atau URL Cloudflare Tunnel).
3. Klik **Uji Ping** untuk memastikan status berubah menjadi **ONLINE**.
4. Kembali ke tab **Search**, cari musik atau masukkan ASIN manual (misal: `B00C069H6M`), lalu tekan **Unduh**.

---

## Otomatisasi CI/CD (GitHub Actions)

Script workflow otomatis tersimpan di `.github/workflows/build_and_publish.yml`.
- **Pemicu Otomatis:** Setiap kali Anda membuat tag rilis (misal `git tag v1.0.0 && git push origin v1.0.0`) atau melakukan push ke branch `main`.
- **Hasil Build:** Otomatis menghasilkan dan mempublikasikan file **`Sonora-release.apk`** dan **`Sonora-release.aab`** ke tab **GitHub Releases** repositori Anda.
- Panduan lengkap konfigurasi penandatanganan keystore tersedia di `docs/CI_CD_BUILD_PUBLISH.md`.
