@echo off
setlocal

rem Check if mvn is available in PATH
where mvn >nul 2>nul
if %ERRORLEVEL% equ 0 (
    mvn -f backend/pom.xml %*
    exit /b %ERRORLEVEL%
)

rem Check if .tools/apache-maven is available in workspace root
set "SCRIPT_DIR=%~dp0"
if exist "%SCRIPT_DIR%.tools\apache-maven-3.9.9\bin\mvn.cmd" (
    "%SCRIPT_DIR%.tools\apache-maven-3.9.9\bin\mvn.cmd" -f "%SCRIPT_DIR%backend\pom.xml" %*
    exit /b %ERRORLEVEL%
)

echo Error: Maven executable not found in PATH or .tools directory.
exit /b 1
