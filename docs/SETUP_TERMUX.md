# Panduan Instalasi & Eksekusi Sonora di Termux Android & Ubuntu Proot-Distro

Dokumen ini menjelaskan langkah demi langkah untuk menjalankan backend engine Sonora dan tools pemroses unduhan `lucidadl` di Android menggunakan Termux dan Ubuntu `proot-distro`.

---

## 1. Persiapan Awal di Termux

1. Pastikan Anda menggunakan **Termux dari F-Droid** (bukan Play Store yang sudah usang).
2. Buka Termux dan perbarui repositori:
   ```bash
   pkg update -y && pkg upgrade -y
   ```
3. Pasang paket inti:
   ```bash
   pkg install -y proot-distro git curl wget
   ```

---

## 2. Pemasangan Ubuntu Proot-Distro

1. Pasang distro Ubuntu:
   ```bash
   proot-distro install ubuntu
   ```
2. Masuk ke lingkungan Ubuntu:
   ```bash
   proot-distro login ubuntu
   ```

---

## 3. Konfigurasi Lingkungan di Dalam Ubuntu

Jalankan perintah berikut di dalam sesi Ubuntu:

```bash
# Perbarui sistem Ubuntu
apt update -y && apt upgrade -y

# Pasang Python3, venv, Xvfb, dan dependensi media
apt install -y python3 python3-pip python3-venv xvfb ffmpeg git curl

# Buat direktori kerja Sonora
mkdir -p /root/sonora/downloads
cd /root/sonora

# Buat Python Virtual Environment khusus
python3 -m venv /root/sonora/venv
source /root/sonora/venv/bin/activate

# Pasang FastAPI, Uvicorn, dan dependensi Sonora
pip install --upgrade pip
pip install fastapi uvicorn pydantic pydantic-settings httpx aiosqlite

# Pasang lucidadl di dalam virtual environment ini
pip install lucidadl
```

---

## 4. Verifikasi Alat Inti

Periksa apakah binary dapat diakses dan berjalan:

1. **Periksa executable lucidadl:**
   ```bash
   which lucidadl
   # Output ideal: /root/sonora/venv/bin/lucidadl
   ```
2. **Periksa executable xvfb-run:**
   ```bash
   which xvfb-run
   # Output ideal: /usr/bin/xvfb-run
   ```
3. **Uji perintah virtual display:**
   ```bash
   xvfb-run -a lucidadl --help
   ```

---

## 5. Menjalankan Backend Sonora

Di dalam direktori proyek backend Sonora (di Ubuntu):
```bash
source /root/sonora/venv/bin/activate
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Backend akan mendengarkan di: `http://0.0.0.0:8000`

---

## 6. Tips Keandalan Proses di Android

- Agar Android tidak menghentikan Termux di latar belakang (background kill):
  1. Matikan Battery Optimization untuk aplikasi Termux di Pengaturan Android.
  2. Aktifkan **Acquire Wakelock** di Termux melalui menu notifikasi atau perintah `termux-wake-lock`.
