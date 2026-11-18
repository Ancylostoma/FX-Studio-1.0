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

rem --- Ollama (IA local, opcional) -------------------------------------------
rem Solo se inicia "ollama serve" si Ollama no responde y no hay ya un
rem servidor ollama.exe en marcha: nunca se crea una segunda instancia.
curl -s -o nul --max-time 2 http://127.0.0.1:11434/api/version
if not errorlevel 1 (
  echo [x] Ollama ya esta en marcha.
  goto pixelforge
)
where ollama >nul 2>nul
if errorlevel 1 (
  echo [ ] Ollama no esta instalado: se usara el motor procedural.
  goto pixelforge
)
tasklist /FI "IMAGENAME eq ollama.exe" 2>nul | findstr /I "ollama.exe" >nul
if not errorlevel 1 (
  echo [.] Ollama se esta iniciando, esperando...
) else (
  echo [.] Iniciando Ollama en segundo plano ^(ollama serve^)...
  start "Ollama" /min ollama serve
)
set /a TRIES=0
:esperar_ollama
timeout /t 1 /nobreak >nul
curl -s -o nul --max-time 2 http://127.0.0.1:11434/api/version
if not errorlevel 1 (
  echo [x] Ollama listo.
  goto pixelforge
)
set /a TRIES+=1
if %TRIES% LSS 20 goto esperar_ollama
echo [!] Ollama no respondio en 20 s. PixelForge funcionara con el motor procedural.
echo     Puedes abrir la aplicacion Ollama y pulsar "Comprobar conexion" en PixelForge.

:pixelforge
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
