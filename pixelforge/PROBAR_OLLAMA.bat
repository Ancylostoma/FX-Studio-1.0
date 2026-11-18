@echo off
chcp 65001 >nul
title Prueba real de PixelForge con Ollama
cd /d "%~dp0"
rem Ejecuta la prueba de integracion con el modelo indicado (por defecto qwen3.5:4b)
rem y abre la carpeta con el informe y las imagenes. Tarda unos minutos.
set MODELO=%1
if "%MODELO%"=="" set MODELO=qwen3.5:4b
where node >nul 2>nul || (echo [!] Falta Node.js: instalalo desde https://nodejs.org & pause & exit /b 1)
curl -s -o nul --max-time 2 http://127.0.0.1:11434/api/version
if errorlevel 1 (
  echo [!] Ollama no responde. Ejecuta INICIAR.bat o abre la aplicacion Ollama y repite.
  pause
  exit /b 1
)
node tools\ollama-check.mjs %MODELO%
echo.
echo El informe esta en docs\ollama-check\informe.md
start "" "docs\ollama-check"
pause
