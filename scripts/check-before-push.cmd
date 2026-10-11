@echo off
setlocal

PowerShell -NoProfile -ExecutionPolicy Bypass -File "%~dp0check-before-push.ps1" %*
set "exitCode=%ERRORLEVEL%"

if not "%exitCode%"=="0" (
    echo.
    echo Verification avant push echouee.
) else (
    echo.
    echo Verification avant push reussie.
)

exit /b %exitCode%
