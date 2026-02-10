#!/bin/bash

# Nanobot Java - 一键启动脚本
# One-click startup script for Nanobot Java

set -e

echo "╔════════════════════════════════════════════════════════════╗"
echo "║           Nanobot Java - AI Agent 启动器                  ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# 检查 Java 版本
echo "🔍 检查 Java 环境..."
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到 Java。请安装 Java 21 或更高版本。"
    echo "   下载地址: https://adoptium.net"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ 错误: Java 版本过低 (当前: $JAVA_VERSION, 需要: 21+)"
    echo "   请升级到 Java 21 或更高版本"
    exit 1
fi
echo "✅ Java 版本: $JAVA_VERSION"

# 检查 Maven
echo "🔍 检查 Maven..."
if ! command -v mvn &> /dev/null; then
    echo "❌ 错误: 未找到 Maven。请安装 Maven 3.9 或更高版本。"
    echo "   下载地址: https://maven.apache.org/download.cgi"
    exit 1
fi
echo "✅ Maven 已安装"

# 检查 API 密钥
echo "🔍 检查 API 密钥..."
if [ -z "$OPENAI_API_KEY" ] && [ -z "$ANTHROPIC_API_KEY" ]; then
    echo "⚠️  警告: 未设置 API 密钥"
    echo ""
    echo "请设置以下环境变量之一:"
    echo "  export OPENAI_API_KEY=sk-your-key-here"
    echo "  export ANTHROPIC_API_KEY=sk-ant-your-key-here"
    echo ""
    read -p "是否继续? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo "✅ API 密钥已设置"
fi

# 检查是否需要编译
if [ ! -f "target/nanobot-1.0.0.jar" ]; then
    echo ""
    echo "📦 首次运行，正在编译项目..."
    mvn clean package -DskipTests
    echo "✅ 编译完成"
else
    echo "✅ 项目已编译"
fi

# 启动应用
echo ""
echo "🚀 启动 Nanobot..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 检查是否有参数
if [ $# -eq 0 ]; then
    # 交互模式
    java -jar target/nanobot-1.0.0.jar
else
    # 命令模式
    java -jar target/nanobot-1.0.0.jar "$@"
fi
