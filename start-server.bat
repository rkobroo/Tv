@echo off
REM Live Sports TV - Simple Server Runner
REM This batch file attempts to start a local development server

setlocal enabledelayedexpansion

set PORT=3000
set "CD_PATH=%~dp0"

echo.
echo ========================================
echo   Live Sports TV - Development Server
echo ========================================
echo.
echo Directory: %CD_PATH%
echo Port: %PORT%
echo.

REM Check for Python
where python >nul 2>&1
if !errorlevel!==0 (
    echo Found Python! Starting server...
    echo.
    echo 🎬 Server running at: http://localhost:%PORT%
    echo Press Ctrl+C to stop
    echo.
    cd /d "%CD_PATH%"
    python -m http.server %PORT%
    exit /b
)

REM Check for py (Python launcher)
where py >nul 2>&1
if !errorlevel!==0 (
    echo Found Python launcher! Starting server...
    echo.
    echo 🎬 Server running at: http://localhost:%PORT%
    echo Press Ctrl+C to stop
    echo.
    cd /d "%CD_PATH%"
    py -m http.server %PORT%
    exit /b
)

REM Check for Node.js
where node >nul 2>&1
if !errorlevel!==0 (
    echo Found Node.js! Starting server...
    echo.
    echo 🎬 Server running at: http://localhost:%PORT%
    echo Press Ctrl+C to stop
    echo.
    cd /d "%CD_PATH%"
    node server.js
    exit /b
)

echo.
echo ❌ ERROR: No suitable runtime found!
echo.
echo Please install one of the following:
echo   - Python: https://www.python.org/downloads/
echo   - Node.js: https://nodejs.org/
echo.
echo After installation, run this file again.
echo.
pause
exit /b 1
