@echo off
echo Stopping Gradle daemons...
call gradlew --stop

echo Attempting normal clean...
call gradlew clean
if %ERRORLEVEL% == 0 (
    echo Clean completed successfully!
    goto :end
)

echo Normal clean failed. Attempting force delete...
powershell -Command "Remove-Item -Recurse -Force 'app\build' -ErrorAction SilentlyContinue"
echo Build directory force deleted.

echo Running clean again...
call gradlew clean

:end
echo Done!
pause