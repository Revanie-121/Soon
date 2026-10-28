#!/bin/bash
# ==============================================================================
# SONORA: CLOUDFLARE TUNNEL SETUP & LAUNCHER SCRIPT
# Menghubungkan Backend Sonora (port 8000) ke internet secara aman melalui HTTPS.
# ==============================================================================

echo "======================================================"
echo "    SONORA - SETUP CLOUDFLARE TUNNEL (cloudflared)   "
echo "======================================================"

# Periksa apakah cloudflared terpasang
if ! command -v cloudflared &> /dev/null; then
    echo "cloudflared tidak ditemukan di PATH."
    echo "Mencoba mengunduh binary cloudflared untuk arsitektur CPU Android (arm64)..."
    ARCH=$(uname -m)
    if [ "$ARCH" = "aarch64" ]; then
        curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64 -o /data/data/com.termux/files/usr/bin/cloudflared
        chmod +x /data/data/com.termux/files/usr/bin/cloudflared
        echo "cloudflared arm64 berhasil dipasang."
    else
        echo "Silakan pasang cloudflared sesuai arsitektur CPU Anda."
        exit 1
    fi
fi

echo ""
echo "Pilihan mode Tunnel:"
echo "1. Quick Tunnel (Gratis, URL acak *.trycloudflare.com, tanpa login)"
echo "2. Named Tunnel (Domain kustom, butuh login Cloudflare)"
echo ""

read -p "Pilih mode [1/2] (default: 1): " MODE
MODE=${MODE:-1}

if [ "$MODE" = "1" ]; then
    echo "Menjalankan Quick Tunnel untuk http://localhost:8000..."
    echo "Salin URL *.trycloudflare.com yang muncul di bawah ke halaman Pengaturan Sonora Android:"
    cloudflared tunnel --url http://127.0.0.1:8000
else
    read -p "Masukkan nama tunnel atau token: " TUNNEL_NAME
    cloudflared tunnel run "$TUNNEL_NAME"
fi
