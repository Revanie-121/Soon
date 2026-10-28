# Panduan Konfigurasi Cloudflare Tunnel untuk Sonora

Cloudflare Tunnel (`cloudflared`) memungkinkan aplikasi Android Sonora berkomunikasi dengan backend FastAPI yang berjalan di Termux secara aman melalui HTTPS publik tanpa perlu port-forwarding pada router.

---

## 1. Pemasangan cloudflared di Termux

Jalankan langsung di lingkungan host Termux (di luar proot-distro):

```bash
# Untuk perangkat Android berarsitektur ARM64
curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64 -o $PREFIX/bin/cloudflared
chmod +x $PREFIX/bin/cloudflared

# Verifikasi pemasangan
cloudflared --version
```

---

## 2. Menjalankan Quick Tunnel (Otomatis & Cepat)

Cara termudah untuk pengujian tanpa perlu domain kustom:

```bash
cloudflared tunnel --url http://127.0.0.1:8000
```

Cloudflare akan memberikan output berupa URL HTTPS publik, contoh:
`https://random-subdomain.trycloudflare.com`

---

## 3. Menghubungkan ke Aplikasi Android Sonora

1. Buka aplikasi Sonora di Android.
2. Masuk ke tab **Settings** (Pengaturan).
3. Pada kolom **Base URL API**, masukkan URL HTTPS tunnel yang Anda peroleh (misal: `https://random-subdomain.trycloudflare.com`).
4. Tekan tombol **Simpan URL**, lalu tekan **Uji Ping**.
5. Indikator status akan berubah menjadi **ONLINE** hijau beserta latensi koneksi.

---

## 4. Keamanan & Troubleshooting

- **Autentikasi Token:** Jika mengaktifkan `API_TOKEN` di file `.env` backend, isi token yang sama di input Bearer Token pada halaman Settings aplikasi Android.
- **Tunnel Unreachable:** Pastikan backend FastAPI sedang berjalan di port 8000 sebelum meluncurkan tunnel.
