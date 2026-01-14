@rem
@rem Gradle startup script for Windows
@rem
@echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if defined JAVA_HOME goto findJavaFromJavaHome

set JAVACMD=java
where java >NUL 2>&1
if %ERRORLEVEL% == 0 goto execute

echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
exit /b 1

:findJavaFromJavaHome
set JAVACMD=%JAVA_HOME%\bin\java.exe

:execute
"%JAVACMD%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
endlocal
