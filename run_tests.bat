@echo off
setlocal enabledelayedexpansion

set "JAVA_BIN=C:\Program Files\Java\jdk-26.0.2.1\bin"
set "JAVAC=%JAVA_BIN%\javac.exe"
set "JAVA=%JAVA_BIN%\java.exe"

if not exist "%JAVAC%" (
    set "JAVAC=javac"
    set "JAVA=java"
)

echo ================================================================================
echo  [SQA 1/2] Compiling AcadsCatchUp Unit Test Suite...
echo ================================================================================

if not exist "target\test-classes" mkdir "target\test-classes"

set "CP=target\classes;target\libs\*"
dir /s /b "src\test\java\*.java" > "target\test_sources.txt"

"%JAVAC%" -J-Xmx384m --release 21 -cp "%CP%" -d "target\test-classes" @"target\test_sources.txt"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Test Compilation failed!
    exit /b %ERRORLEVEL%
)

echo ================================================================================
echo  [SQA 2/2] Running Automated Capstone Tests with Assertions Enabled (-ea)...
echo ================================================================================

set "RUN_CP=target\test-classes;target\classes;target\libs\*"
"%JAVA%" -ea -cp "%RUN_CP%" com.acadscatchup.test.CapstoneTestRunner

exit /b %ERRORLEVEL%
