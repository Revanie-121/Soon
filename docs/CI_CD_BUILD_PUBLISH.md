# Panduan Otomatisasi CI/CD Build & Publish (GitHub Actions)

Aplikasi Sonora telah dilengkapi dengan workflow otomatisasi **GitHub Actions** (`.github/workflows/build_and_publish.yml`) untuk membangun (*build*), menguji (*test*), menandatangani (*sign*), dan menerbitkan (*publish*) file instalasi **APK** dan **AAB (Android App Bundle)** secara otomatis.

---

## 1. Mekanisme Pemicu (Triggers)

Workflow akan berjalan secara otomatis dalam kondisi berikut:

1. **Push Git Tag Rilis (Direkomendasikan):**
   Setiap kali Anda membuat dan mengunggah tag versi baru, GitHub Actions akan langsung mengompilasi aplikasi dan membuat **GitHub Release**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. **Push ke Branch `main` / `master`:**
   Setiap kali ada commit baru yang di-merge ke branch utama.
3. **Pemicu Manual (`workflow_dispatch`):**
   Dapat dijalankan kapan saja langsung dari tab **Actions** di repositori GitHub Anda:
   - Pilih workflow **Build and Publish Sonora Android App**.
   - Klik tombol **Run workflow**.
   - Pilih opsi: Tipe build (`release`/`debug`), publikasikan ke GitHub Releases (`true`/`false`), atau unggah ke Google Play (`true`/`false`).
4. **Pull Request:**
   Menjalankan pengujian unit otomatis (`:app:testDebugUnitTest`) untuk memastikan tidak ada perubahan kode yang merusak fitur.

---

## 2. File Output Hasil Build

Setiap build yang berhasil akan menghasilkan:
- **`Sonora-release.apk`**: File instalasi Android langsung (bisa diunduh dan dipasang di HP).
- **`Sonora-release.aab`**: Paket Android App Bundle yang siap diunggah ke Google Play Console.
- **`unit-test-report`**: Laporan hasil pengujian unit.

File-file tersebut otomatis diunggah ke:
1. **GitHub Releases:** Tersedia di tab *Releases* repositori GitHub Anda untuk diunduh langsung oleh publik/pengguna.
2. **GitHub Actions Artifacts:** Tersimpan di halaman run workflow selama 30 hari.

---

## 3. Konfigurasi GitHub Repository Secrets (Opsional tapi Direkomendasikan)

Untuk menandatangani APK rilis dengan kunci sertifikat resmi Anda sendiri:

### A. Siapkan File Keystore Android
Jika Anda sudah memiliki file keystore (misal `my-upload-key.jks`), ubah file tersebut menjadi string Base64:
```bash
# Di terminal Linux / macOS / Termux:
base64 -w 0 my-upload-key.jks > keystore_base64.txt

# Di PowerShell Windows:
[Convert]::ToBase64String([IO.File]::ReadAllBytes("my-upload-key.jks")) | Out-File -Encoding ascii keystore_base64.txt
```

### B. Tambahkan Secrets di Repositori GitHub
Masuk ke menu: **Settings** $\rightarrow$ **Secrets and variables** $\rightarrow$ **Actions** $\rightarrow$ **New repository secret**:

| Nama Secret | Deskripsi |
|-------------|-----------|
| `KEYSTORE_BASE64` | Isi teks string Base64 dari file keystore `.jks` Anda |
| `STORE_PASSWORD` | Password keystore Anda |
| `KEY_PASSWORD` | Password alias kunci Anda |
| `PLAY_CONFIG_JSON` | *(Opsional)* Isi JSON Service Account Google Play jika ingin otomatis deploy ke Google Play Console |

> **Catatan:** Jika secret di atas belum dikonfigurasi, script workflow sudah cerdas dan secara otomatis akan membuat *fallback signing key* sementara sehingga proses build APK dan pembuatan GitHub Release tetap berjalan sukses tanpa error!

---

## 4. Lokasi File Workflow

File script workflow tersimpan di:
```
.github/workflows/build_and_publish.yml
```
Anda dapat menyesuaikan versi SDK, nama rilis, atau track Google Play sesuai kebutuhan pengembangan.
