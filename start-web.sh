#!/bin/bash

echo "🚀 启动 Nanobot Web 服务器..."
echo ""

# Stop old processes
pkill -f WebServerTest 2>/dev/null

# Check for API keys
if [ -z "$OPENAI_API_KEY" ] && [ -z "$ANTHROPIC_API_KEY" ]; then
    echo "❌ 错误: 未找到 API 密钥"
    echo "请设置以下环境变量之一:"
    echo "  export OPENAI_API_KEY=sk-your-key-here"
    echo "  export ANTHROPIC_API_KEY=sk-ant-your-key-here"
    exit 1
fi

# Start web server
cd /workspaces/nanobot-java
java -cp "target/classes:target/nanobot-1.0.0.jar" com.nanobot.web.WebServerTest
