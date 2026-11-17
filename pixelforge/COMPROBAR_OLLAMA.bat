@echo off
chcp 65001 >nul
title Comprobar Ollama
echo === Comprobacion de la IA local (Ollama) ===
echo.

for /f %%m in ('powershell -NoProfile -Command "[math]::Round((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory/1GB)"') do set RAM=%%m
echo Memoria RAM del equipo: %RAM% GB
where nvidia-smi >nul 2>nul && nvidia-smi --query-gpu=name,memory.total --format=csv,noheader
echo.

where ollama >nul 2>nul
if errorlevel 1 (
  echo [ ] Ollama NO esta instalado.
  echo     1. Descargalo de https://ollama.com/download e instalalo.
  echo     2. Vuelve a ejecutar este archivo.
  echo     PixelForge funciona igualmente con el motor procedural.
  goto sugerencia
)
for /f "delims=" %%v in ('ollama --version 2^>nul') do echo [x] %%v

curl -s -o nul http://127.0.0.1:11434/api/version
if errorlevel 1 (
  echo [ ] Ollama esta instalado pero no esta en marcha.
  echo     Abre la aplicacion Ollama desde el menu Inicio, o ejecuta: ollama serve
  goto sugerencia
)
echo [x] Ollama responde en http://127.0.0.1:11434
echo.
echo Modelos instalados:
ollama list
echo.

:sugerencia
echo Modelo sugerido para %RAM% GB de RAM (la descarga NO se hace sola):
if %RAM% GEQ 16 (
  echo     ollama pull qwen2.5:7b      ^(aprox. 4,7 GB^)
) else if %RAM% GEQ 8 (
  echo     ollama pull qwen2.5:3b      ^(aprox. 1,9 GB^)
) else (
  echo     ollama pull qwen2.5:1.5b    ^(aprox. 1 GB^)
)
echo Copia la linea en una terminal solo si quieres descargarlo.
echo Mas detalles en GUIA_MODELOS.md
echo.
pause
