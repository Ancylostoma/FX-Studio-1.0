@echo off
chcp 65001 >nul
title LinguaBridge AI
cd /d "%~dp0"
where node >nul 2>nul
if errorlevel 1 (
  echo [!] Falta Node.js. Instala la version LTS desde https://nodejs.org
  pause
  exit /b 1
)
curl -s -o nul --max-time 2 http://127.0.0.1:11434/api/version
if errorlevel 1 (
  echo [ ] Ollama no responde: se usara solo el motor de reglas.
) else (
  echo [x] Ollama en marcha: se usara como mejora opcional.
)
start "" http://127.0.0.1:8788
node server\server.mjs
pause
