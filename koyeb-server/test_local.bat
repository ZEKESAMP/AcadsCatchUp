@echo off
title AcadsCatchUp - Java Gmail Checker Server (Local Test)
echo ========================================================
echo   Starting AcadsCatchUp Java Gmail Checker Microservice
echo   Developer: F4TAL
echo ========================================================
echo.
if not exist "bin\com\acadscatchup\server\GmailCheckerServer.class" (
    echo Compiling Java server...
    mkdir bin 2>nul
    javac -d bin src/com/acadscatchup/server/GmailCheckerServer.java
)
echo.
echo Server running at: http://localhost:8000/
echo Press Ctrl+C to stop.
echo.
java -cp bin com.acadscatchup.server.GmailCheckerServer
pause
