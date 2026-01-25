@echo off
REM Simple HTTP Server using Python or Node.js
REM Check for Python 3
python --version >nul 2>&1
if %errorlevel%==0 (
    echo Starting server with Python 3...
    python -m http.server 3000
    exit /b
)

REM Check for Node.js
node --version >nul 2>&1
if %errorlevel%==0 (
    echo Starting server with Node.js...
    node server.js
    exit /b
)

REM Fallback: Use Python 2 or show error
python -m SimpleHTTPServer 3000 2>nul
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Neither Python nor Node.js is installed!
    echo Please install one of the following:
    echo   - Python: https://www.python.org/downloads/
    echo   - Node.js: https://nodejs.org/
    echo.
    pause
    exit /b 1
)
