#!/bin/bash

echo "🚀 启动 Nanobot 监控服务器..."
echo ""

# 停止旧进程
pkill -f MonitorServerTest 2>/dev/null

# 启动监控服务器
cd /workspaces/nanobot-java
java -cp "target/classes:target/nanobot-1.0.0.jar" com.nanobot.monitor.MonitorServerTest
