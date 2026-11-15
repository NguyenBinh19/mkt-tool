#!/usr/bin/env bash
set -euo pipefail

Xvfb :99 -screen 0 1366x900x24 -nolisten tcp > /tmp/xvfb.log 2>&1 &
sleep 1
x11vnc -display :99 -localhost -nopw -forever -shared -rfbport 5900 -quiet > /tmp/x11vnc.log 2>&1 &
websockify --web=/usr/share/novnc 0.0.0.0:6080 127.0.0.1:5900 > /tmp/websockify.log 2>&1 &

exec java -jar /app/app.jar
