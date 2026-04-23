@echo off
setlocal enabledelayedexpansion

echo ============================================================
echo   🛡️  SOC Analysis Tool - Launcher
echo ============================================================
echo.

:: 1. Check if Maven exists
set MVN_PATH=%~dp0apache-maven-3.9.15\bin\mvn.cmd
if not exist "!MVN_PATH!" (
    echo [ERROR] Maven not found at: !MVN_PATH!
    echo Please run setup.bat again or make sure the folder exists.
    pause
    exit /b 1
)

:: 2. Check Java
echo [INFO] Checking Java version...
java -version
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java is not installed or not in PATH.
    pause
    exit /b 1
)

:: 3. Launching
cd /d "%~dp0frontend"
echo.
echo [INFO] Starting Application...
echo [INFO] (The first run might take 10-20 seconds to initialize)
echo.

:: We use 'mvn javafx:run' without 'clean' to avoid file-lock errors.
call "!MVN_PATH!" javafx:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ============================================================
    echo   ❌ CRASH DETECTED
    echo ============================================================
    echo The application closed with an error code. 
    echo Please scroll up to see the red error messages.
    echo.
    pause
)
