@echo off
REM Nanobot Java - 一键启动脚本 (Windows)
REM One-click startup script for Nanobot Java

setlocal enabledelayedexpansion

echo ╔════════════════════════════════════════════════════════════╗
echo ║           Nanobot Java - AI Agent 启动器                  ║
echo ╚════════════════════════════════════════════════════════════╝
echo.

REM 检查 Java
echo 🔍 检查 Java 环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未找到 Java。请安装 Java 21 或更高版本。
    echo    下载地址: https://adoptium.net
    pause
    exit /b 1
)
echo ✅ Java 已安装

REM 检查 Maven
echo 🔍 检查 Maven...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未找到 Maven。请安装 Maven 3.9 或更高版本。
    echo    下载地址: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)
echo ✅ Maven 已安装

REM 检查 API 密钥
echo 🔍 检查 API 密钥...
if "%OPENAI_API_KEY%"=="" if "%ANTHROPIC_API_KEY%"=="" (
    echo ⚠️  警告: 未设置 API 密钥
    echo.
    echo 请设置以下环境变量之一:
    echo   set OPENAI_API_KEY=sk-your-key-here
    echo   set ANTHROPIC_API_KEY=sk-ant-your-key-here
    echo.
    set /p continue="是否继续? (y/n): "
    if /i not "!continue!"=="y" exit /b 1
) else (
    echo ✅ API 密钥已设置
)

REM 检查是否需要编译
if not exist "target\nanobot-1.0.0.jar" (
    echo.
    echo 📦 首次运行，正在编译项目...
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo ❌ 编译失败
        pause
        exit /b 1
    )
    echo ✅ 编译完成
) else (
    echo ✅ 项目已编译
)

REM 启动应用
echo.
echo 🚀 启动 Nanobot...
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

REM 检查是否有参数
if "%~1"=="" (
    REM 交互模式
    java -jar target\nanobot-1.0.0.jar
) else (
    REM 命令模式
    java -jar target\nanobot-1.0.0.jar %*
)

pause
