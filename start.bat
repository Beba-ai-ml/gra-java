@echo off
REM Windows: double-click this file.
cd /d "%~dp0"
where py >nul 2>nul
if %errorlevel%==0 (
  py -3 start.py
) else (
  where python >nul 2>nul
  if errorlevel 1 (
    echo Brak Pythona. Zainstaluj go z https://www.python.org/downloads/ i zaznacz "Add python.exe to PATH".
    pause
    exit /b 1
  )
  python start.py
)
if errorlevel 1 pause
