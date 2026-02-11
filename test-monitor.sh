#!/bin/bash

# 测试监控服务器
echo "🧪 测试 Nanobot 监控服务器..."
echo ""

# 设置测试 API Key
export OPENAI_API_KEY=sk-test-key-for-monitoring

# 设置监控端口
export MONITOR_PORT=8080

# 启动 Nanobot (后台运行)
echo "启动 Nanobot..."
java -jar target/nanobot-1.0.0.jar shell > /tmp/nanobot.log 2>&1 &
NANOBOT_PID=$!

echo "Nanobot PID: $NANOBOT_PID"
echo "等待服务启动..."
sleep 5

# 检查监控服务是否启动
echo ""
echo "检查监控服务..."
if curl -s http://localhost:8080/api/health > /dev/null; then
    echo "✅ 监控服务运行正常"
    echo ""
    echo "📊 访问监控面板: http://localhost:8080"
    echo ""
    echo "API 测试:"
    echo "- 健康检查:"
    curl -s http://localhost:8080/api/health | jq '.' 2>/dev/null || curl -s http://localhost:8080/api/health
    echo ""
    echo "- 统计信息:"
    curl -s http://localhost:8080/api/stats | jq '.memory' 2>/dev/null || curl -s http://localhost:8080/api/stats | head -20
else
    echo "❌ 监控服务未启动"
    echo ""
    echo "日志:"
    cat /tmp/nanobot.log
fi

echo ""
echo "按 Ctrl+C 停止测试..."
echo "或运行: kill $NANOBOT_PID"

# 等待用户中断
wait $NANOBOT_PID 2>/dev/null || true
