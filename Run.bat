@echo off
echo Starting Image to PDF Converter...

set "JAVA_CMD="

rem Check if java is in PATH
where java >nul 2>nul
if %errorlevel% == 0 (
    set "JAVA_CMD=java"
    goto :run
)

rem Check common Java installation locations
if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-21\bin\java.exe"
    goto :run
)

if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-17\bin\java.exe"
    goto :run
)

if exist "C:\Program Files\Java\jdk-11\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-11\bin\java.exe"
    goto :run
)

for /d %%D in ("C:\Program Files\Java\jdk*") do (
    if exist "%%D\bin\java.exe" (
        set "JAVA_CMD=%%D\bin\java.exe"
        goto :run
    )
)

:run
if not "%JAVA_CMD%"=="" (
    echo Using Java: %JAVA_CMD%
    "%JAVA_CMD%" -jar "%~dp0target\jpg-to-pdf-gui.jar"
    if errorlevel 1 (
        echo.
        echo Program exited with error.
        pause
    )
) else (
    echo ERROR: Java is not installed or not found.
    echo.
    echo Please install Java from: https://www.oracle.com/java/technologies/downloads/
    echo Or make sure Java is added to your system PATH.
    echo.
    pause
)
