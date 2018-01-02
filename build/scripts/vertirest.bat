@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  vertirest startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and VERTIREST_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\netty-common-4.1.15.Final.jar;%APP_HOME%\lib\mongodb-driver-async-3.4.1.jar;%APP_HOME%\lib\netty-codec-dns-4.1.15.Final.jar;%APP_HOME%\lib\annotations-13.0.jar;%APP_HOME%\lib\jackson-annotations-2.9.0.jar;%APP_HOME%\lib\vertx-web-common-3.5.0.jar;%APP_HOME%\lib\netty-codec-http2-4.1.15.Final.jar;%APP_HOME%\lib\vertx-bridge-common-3.5.0.jar;%APP_HOME%\lib\netty-codec-http-4.1.15.Final.jar;%APP_HOME%\lib\vertirest-1.0-SNAPSHOT.jar;%APP_HOME%\lib\vertx-mongo-client-3.5.0.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-1.2.0.jar;%APP_HOME%\lib\mongodb-driver-core-3.4.1.jar;%APP_HOME%\lib\vertx-web-client-3.5.0.jar;%APP_HOME%\lib\vertx-unit-3.5.0.jar;%APP_HOME%\lib\vertx-auth-common-3.5.0.jar;%APP_HOME%\lib\netty-handler-4.1.15.Final.jar;%APP_HOME%\lib\kotlin-stdlib-1.2.0.jar;%APP_HOME%\lib\vertx-core-3.5.0.jar;%APP_HOME%\lib\netty-codec-4.1.15.Final.jar;%APP_HOME%\lib\bson-3.4.1.jar;%APP_HOME%\lib\vertx-web-3.5.0.jar;%APP_HOME%\lib\jackson-core-2.9.0.jar;%APP_HOME%\lib\netty-codec-socks-4.1.15.Final.jar;%APP_HOME%\lib\jackson-databind-2.9.0.jar;%APP_HOME%\lib\netty-resolver-dns-4.1.15.Final.jar;%APP_HOME%\lib\netty-handler-proxy-4.1.15.Final.jar;%APP_HOME%\lib\netty-resolver-4.1.15.Final.jar;%APP_HOME%\lib\netty-transport-4.1.15.Final.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-1.2.0.jar;%APP_HOME%\lib\netty-buffer-4.1.15.Final.jar

@rem Execute vertirest
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %VERTIREST_OPTS%  -classpath "%CLASSPATH%" io.vertx.core.Launcher %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable VERTIREST_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%VERTIREST_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
