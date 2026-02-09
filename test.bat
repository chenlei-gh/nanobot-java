@echo off
REM Nanobot Java Test Script
REM Requires Java 21+

echo ========================================
echo Testing Nanobot Java Project
echo ========================================
echo.

REM Check Java version
echo [1/4] Checking Java version...
java -version 2>&1

REM Check for Maven
echo.
echo [2/4] Checking Maven...
where mvn >nul 2>&1
if %errorlevel% equ 0 (
    echo Maven found
    mvn --version
) else (
    echo Maven not found
)

REM Check project structure
echo.
echo [3/4] Verifying project structure...
dir /s /b src\main\java\*.java 2>nul | find /c ".java"
echo Java files found

echo.
echo [4/4] Project ready for build
echo.

REM Check if pom.xml exists
if exist pom.xml (
    echo ✓ pom.xml found
) else (
    echo ✗ pom.xml missing
)

REM Check source directories
if exist src\main\java (
    echo ✓ Source directory found
) else (
    echo ✗ Source directory missing
)

echo.
echo ========================================
echo Build Instructions:
echo ========================================
echo.
echo 1. Install Java 21:
echo    - Download from https://adoptium.net
echo    - Or: winget install EclipseAdoptium.Temurin.21.JDK
echo.
echo 2. Install Maven:
echo    - Download from https://maven.apache.org
echo    - Or: winget install Apache.Maven
echo.
echo 3. Build the project:
echo    mvn clean package
echo.
echo 4. Run Nanobot:
echo    java -jar target\nanobot-1.0.0.jar agent "Hello"
echo.
echo ========================================
