#!/bin/bash
# ==============================================================================
# SONORA: START BACKEND FASTAPI DI UBUNTU PROOT-DISTRO
# ==============================================================================

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Memulai Sonora FastAPI Backend di Ubuntu proot-distro..."

proot-distro login ubuntu -- bash -c "
    cd /root/sonora/backend || cd '$DIR/backend'
    if [ -f '/root/sonora/venv/bin/activate' ]; then
        source /root/sonora/venv/bin/activate
    fi

    export APP_HOST=0.0.0.0
    export APP_PORT=8000
    export LUCIDADL_EXECUTABLE=\$(which lucidadl || echo '/root/sonora/venv/bin/lucidadl')
    export XVFB_RUN_EXECUTABLE=\$(which xvfb-run || echo 'xvfb-run')
    export LUCIDADL_WORKDIR='/root/sonora/downloads'

    echo 'Host: 0.0.0.0:8000'
    echo 'Executable: '\$LUCIDADL_EXECUTABLE
    echo 'Xvfb: '\$XVFB_RUN_EXECUTABLE

    if [ -f '/root/sonora/server.py' ]; then
        echo 'Menjalankan /root/sonora/server.py...'
        python3 /root/sonora/server.py
    elif [ -f '/root/sonora/backend/server.py' ]; then
        echo 'Menjalankan /root/sonora/backend/server.py...'
        python3 /root/sonora/backend/server.py
    elif [ -f '$DIR/server.py' ]; then
        echo 'Menjalankan server.py dari direktori proyek...'
        python3 '$DIR/server.py'
    else
        echo 'Menjalankan app.main:app...'
        python3 -m uvicorn app.main:app --host 0.0.0.0 --port 8000
    fi
"
