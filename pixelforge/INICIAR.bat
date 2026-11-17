@echo off
chcp 65001 >nul
title PixelForge Studio
cd /d "%~dp0"

where node >nul 2>nul
if errorlevel 1 (
  echo [!] Falta Node.js. Instala la version LTS desde https://nodejs.org y vuelve a ejecutar este archivo.
  pause
  exit /b 1
)
node -e "process.exit(+process.versions.node.split('.')[0] >= 20 ? 0 : 1)"
if errorlevel 1 (
  echo [!] Se necesita Node.js 20 o superior. Actualizalo desde https://nodejs.org
  pause
  exit /b 1
)

rem Si ya esta en marcha, solo abre el navegador.
curl -s -o nul http://127.0.0.1:8787/api/status
if not errorlevel 1 (
  start "" http://localhost:8787
  exit /b 0
)

echo Iniciando PixelForge Studio en http://localhost:8787 ...
echo (El motor procedural funciona sin Ollama. Para la IA local ejecuta COMPROBAR_OLLAMA.bat)
start "PixelForge Studio - servidor" /min node server\server.mjs
timeout /t 2 /nobreak >nul
start "" http://localhost:8787
echo Listo. Para detenerlo usa DETENER.bat
timeout /t 4 >nul
