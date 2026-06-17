@echo off
setlocal
cd /d "%~dp0.."

echo === Gemini diagnostic ===
echo.

if "%GOOGLE_API_KEY%"=="" (
    if exist config\gemini.key (
        echo [OK]   config\gemini.key found
        set KEY_OK=1
    ) else (
        echo [FAIL] No GOOGLE_API_KEY and no config\gemini.key
        echo        Copy config\gemini.key.example to config\gemini.key and paste your key
        set KEY_OK=0
    )
) else (
    echo [OK]   GOOGLE_API_KEY is set.
    set KEY_OK=1
)

if exist users.json (
    echo [OK]   users.json exists - registered users:
    for /f "tokens=1 delims=:" %%u in ('findstr /r "^\".*\":" users.json') do (
        set "line=%%u"
        setlocal EnableDelayedExpansion
        echo        - !line:"=!
        endlocal
    )
) else (
    echo [WARN] users.json not found - register a user in the client first.
)

echo.
if "%KEY_OK%"=="1" (
    echo Testing Gemini API...
    javac -encoding UTF-8 -sourcepath src -d bin src\chess\GeminiCheck.java 2>nul
    if errorlevel 1 (
        echo [FAIL] Could not compile GeminiCheck.java
    ) else (
        java -cp bin chess.GeminiCheck
    )
) else (
    echo Skipping API test - set GOOGLE_API_KEY first.
)

echo.
echo === In the game ===
echo 1. Start SERVER in a terminal that has GOOGLE_API_KEY set
echo 2. Start CLIENT, login as your user, click "Play vs Gemini"
echo 3. Make a move - if Black replies in a few seconds, Gemini works
echo 4. If you see "Gemini is not configured on the server" - key missing on server
echo 5. If you see "Gemini unavailable - local AI played" - API failed, fallback used
pause
