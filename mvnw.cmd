@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __ MVNW_CMD__=
@SETLOCAL

@SET MAVEN_WRAPPER_JAR="%~dp0.mvn\wrapper\maven-wrapper.jar"
@SET JAVA_EXE=%JAVA_HOME%/bin/java.exe
@IF NOT EXIST "%JAVA_EXE%" SET JAVA_EXE=java

"%JAVA_EXE%" -jar %MAVEN_WRAPPER_JAR% %*

@ENDLOCAL
