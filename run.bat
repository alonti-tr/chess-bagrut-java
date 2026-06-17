@echo off
setlocal EnableDelayedExpansion

if "%1"=="" (
    echo.
    echo  ===== Chess Bagrut =====
    echo.
    echo   1 = Server  ^(start this FIRST^)
    echo   2 = Client  ^(GUI - start after server^)
    echo   3 = Exit
    echo.
    set /p CHOICE=Choose 1, 2 or 3:
    if "!CHOICE!"=="1" set MODE=server
    if "!CHOICE!"=="2" set MODE=client
    if "!CHOICE!"=="3" exit /b 0
    if not defined MODE (
        echo Invalid choice.
        pause
        exit /b 1
    )
    goto compile
)
set MODE=%1

where javac >nul 2>&1
if %errorlevel%==0 (
    set JAVAC=javac
    set JAVA=java
    goto compile
)

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

if "%MODE%"=="server" (
    "%JAVA%" -cp bin chess.Main server
) else (
    "%JAVA%" -cp bin chess.Main client
)
