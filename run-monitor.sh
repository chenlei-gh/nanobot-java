#!/bin/bash

# 启动监控服务器测试
echo "🧪 启动 Nanobot 监控服务器..."
echo ""

# 编译
echo "📦 编译项目..."
mvn compile -q

# 运行监控服务器
echo ""
echo "🚀 启动监控服务器..."
mvn exec:java -Dexec.mainClass="com.nanobot.monitor.MonitorServerTest" -q
