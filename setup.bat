@echo off
setlocal enabledelayedexpansion
echo ============================================================
echo   SOC Analysis Tool - Setup
echo   Installing Python backend dependencies...
echo ============================================================
echo.

cd /d "%~dp0backend"

echo [1/3] Checking Python installation...
python --version
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Python not found. Please install Python 3.11+
    echo Download from: https://www.python.org/downloads/
    pause
    exit /b 1
)

echo.
echo [2/3] Installing Python packages...
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to install Python packages.
    pause
    exit /b 1
)

echo.
echo [3/3] Checking Java and Maven...
java --version

set MVN_PATH=%~dp0apache-maven-3.9.15\bin\mvn.cmd
if exist "!MVN_PATH!" (
    echo [INFO] Using local Maven at !MVN_PATH!
    call "!MVN_PATH!" --version
) else (
    call mvn --version
    if %ERRORLEVEL% NEQ 0 (
        echo ERROR: Maven not found. Please install Maven.
        echo Download from: https://maven.apache.org/download.cgi
        pause
        exit /b 1
    )
)

echo.
echo ============================================================
echo   Setup complete! You can now run: run.bat
echo ============================================================
pause
