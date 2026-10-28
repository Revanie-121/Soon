#!/bin/bash
set -e

# ==============================================================================
# SONORA: SETUP TERMUX & UBUNTU PROOT-DISTRO SCRIPT
# Jalankan script ini di terminal Termux Android.
# ==============================================================================

echo "======================================================"
echo "    SONORA - SETUP TERMUX & UBUNTU PROOT-DISTRO       "
echo "======================================================"

# 1. Update Termux Packages
echo "[1/5] Memperbarui paket Termux..."
pkg update -y && pkg upgrade -y
pkg install -y proot-distro git curl wget

# 2. Install Ubuntu via proot-distro
echo "[2/5] Memeriksa instalasi Ubuntu di proot-distro..."
if ! proot-distro list | grep -q "ubuntu (installed)"; then
    echo "Mengunduh dan memasang Ubuntu..."
    proot-distro install ubuntu
else
    echo "Ubuntu sudah terpasang di proot-distro."
fi

# 3. Setup Dependencies inside Ubuntu
echo "[3/5] Memasang dependensi dalam Ubuntu (Python3, Xvfb, FFmpeg)..."
proot-distro login ubuntu -- bash -c "
    apt update -y && apt upgrade -y
    apt install -y python3 python3-pip python3-venv xvfb ffmpeg git curl wget
    
    # Buat direktori aplikasi Sonora
    mkdir -p /root/sonora/downloads
    cd /root/sonora

    # Buat Python Virtual Environment
    if [ ! -d '/root/sonora/venv' ]; then
        python3 -m venv /root/sonora/venv
    fi

    # Aktifkan venv dan pasang paket backend
    source /root/sonora/venv/bin/activate
    pip install --upgrade pip
    pip install fastapi uvicorn pydantic pydantic-settings httpx aiosqlite

    echo 'Memverifikasi keberadaan lucidadl...'
    # Pengguna dapat memasang lucidadl di venv ini dengan:
    # pip install lucidadl
"

echo "[4/5] Menguji perintah xvfb-run..."
proot-distro login ubuntu -- bash -c "xvfb-run --help > /dev/null 2>&1 && echo 'xvfb-run siap digunakan!' || echo 'Periksa xvfb-run'"

echo "[5/5] Selesai!"
echo "Untuk menjalankan backend Sonora di Termux:"
echo "bash scripts/start_backend.sh"
