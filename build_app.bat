@echo off
setlocal enabledelayedexpansion

set "JAVA_BIN=C:\Program Files\Java\jdk-26.0.2.1\bin"
set "JAVAC=%JAVA_BIN%\javac.exe"
set "JAR=%JAVA_BIN%\jar.exe"

echo ===================================================
echo [1/4] Preparing clean target/classes directory...
echo ===================================================
if exist "target\classes" rd /s /q "target\classes"
mkdir "target\classes"

set "CP=target\libs\*"

echo ===================================================
echo [2/4] Compiling Java source files...
echo ===================================================
dir /s /b "src\main\java\*.java" > "target\sources.txt"
"%JAVAC%" -J-Xmx384m -J-XX:TieredStopAtLevel=1 --release 21 -cp "%CP%" -d "target\classes" @"target\sources.txt"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)

echo ===================================================
echo [3/4] Copying resources...
echo ===================================================
xcopy /s /y /i "src\main\resources\*" "target\classes\" > nul

echo ===================================================
echo [4/4] Creating standalone cross-platform Fat JAR...
echo ===================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0package_fatjar.ps1"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Fat Jar creation failed!
    exit /b %ERRORLEVEL%
)

copy /y "dist\AcadsCatchUp.jar" "dist\AcadsCatchUp-Portable\app\AcadsCatchUp.jar" > nul
copy /y "dist\AcadsCatchUp.jar" "dist\AcadsCatchUp-Portable\app\acadscatchup-app.jar" > nul
copy /y "dist\AcadsCatchUp.jar" "target\acadscatchup-1.0.0.jar" > nul

if exist "C:\Program Files\WinRAR\WinRAR.exe" (
    echo ===================================================
    echo [5/6] Packaging standalone AcadsCatchUp.exe...
    echo ===================================================
    "C:\Program Files\WinRAR\WinRAR.exe" a -r -sfx -z"dist\sfx_config.txt" -iicon"dist\app_icon.ico" -ep1 "dist\AcadsCatchUp.exe" "dist\AcadsCatchUp-Portable\*" > nul
    copy /y "dist\AcadsCatchUp.exe" "dist\AcadsCatchUp-Setup.exe" > nul
    echo Standalone executables updated!
)

where 7z >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo ===================================================
    echo [6/7] Packaging AcadsCatchUp-v1.0.zip...
    echo ===================================================
    if exist "dist\AcadsCatchUp-v1.0.zip" del /q "dist\AcadsCatchUp-v1.0.zip"
    7z a -tzip "dist\AcadsCatchUp-v1.0.zip" ".\dist\AcadsCatchUp-Portable\*" > nul
    echo Portable distribution zip updated!
)

echo ===================================================
echo [7/7] Packaging AcadsCatchUp-Linux distribution...
echo ===================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0package_linux.ps1"

echo ===================================================
echo [SUCCESS] Build completed successfully!
echo ===================================================


