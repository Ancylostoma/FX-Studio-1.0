@echo off
chcp 65001 >nul
rem Detiene solo el proceso de Node que escucha en el puerto 8787.
set FOUND=0
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8787" ^| findstr "LISTENING"') do (
  tasklist /FI "PID eq %%p" | findstr /I "node.exe" >nul && (
    taskkill /PID %%p /F >nul && echo PixelForge Studio detenido (proceso %%p).
    set FOUND=1
  )
)
if "%FOUND%"=="0" echo PixelForge Studio no estaba en marcha.
timeout /t 3 >nul
