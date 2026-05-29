@echo off
setlocal

if "%1"=="" (
    echo Usage: run.bat [server^|client]
    echo   run.bat server   - start the chess server
    echo   run.bat client   - start a chess client
    pause
    exit /b 1
)

:: Try PATH first
where javac >nul 2>&1
if %errorlevel%==0 (
    set JAVAC=javac
    set JAVA=java
    goto compile
)

:: Find from registry
for /f "tokens=2*" %%A in ('reg query "HKLM\SOFTWARE\JavaSoft\JDK" /v CurrentVersion 2^>nul') do set JDK_VER=%%B
if defined JDK_VER (
    for /f "tokens=2*" %%A in ('reg query "HKLM\SOFTWARE\JavaSoft\JDK\%JDK_VER%" /v JavaHome 2^>nul') do set JDK_HOME=%%B
)
if defined JDK_HOME (
    set JAVAC=%JDK_HOME%\bin\javac.exe
    set JAVA=%JDK_HOME%\bin\java.exe
    goto compile
)

echo ERROR: Java JDK not found. Please install JDK 17 or later.
pause
exit /b 1

:compile
mkdir bin 2>nul
echo Compiling...
"%JAVAC%" -encoding UTF-8 -sourcepath src -d bin src\chess\Main.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)
echo OK.

if "%1"=="server" (
    "%JAVA%" -cp bin chess.Main server
) else (
    "%JAVA%" -cp bin chess.Main client
)
