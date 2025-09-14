@echo off
echo Cleaning Android build directory...

REM Kill any Java/Gradle processes
taskkill /f /im java.exe 2>nul
taskkill /f /im gradle.exe 2>nul

REM Wait a moment for processes to terminate
timeout /t 2 /nobreak >nul

REM Remove build directories
rmdir /s /q "app\build" 2>nul
rmdir /s /q ".gradle" 2>nul

REM Clean and rebuild
call gradlew clean
call gradlew assembleDebug

echo Build completed!
pause
