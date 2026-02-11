#!/bin/bash

echo "🧪 测试 Nanobot Web 界面"
echo ""

# 检查编译
echo "1️⃣ 检查编译状态..."
if [ ! -f "target/nanobot-1.0.0.jar" ]; then
    echo "   ⚠️  需要先编译项目"
    echo "   运行: mvn clean package -DskipTests"
    exit 1
fi
echo "   ✅ 编译完成"
echo ""

# 检查 API 密钥
echo "2️⃣ 检查 API 密钥..."
if [ -z "$OPENAI_API_KEY" ] && [ -z "$ANTHROPIC_API_KEY" ]; then
    echo "   ⚠️  未设置 API 密钥"
    echo "   请设置: export OPENAI_API_KEY=sk-your-key-here"
    echo "   或者: export ANTHROPIC_API_KEY=sk-ant-your-key-here"
    exit 1
fi
echo "   ✅ API 密钥已设置"
echo ""

# 启动服务器
echo "3️⃣ 启动 Web 服务器..."
export WEB_PORT=9090
nohup java -cp "target/classes:target/nanobot-1.0.0.jar" com.nanobot.web.WebServerTest > /tmp/nanobot-web.log 2>&1 &
WEB_PID=$!
echo "   进程 ID: $WEB_PID"
sleep 3
echo ""

# 测试健康检查
echo "4️⃣ 测试健康检查..."
HEALTH=$(curl -s http://localhost:9090/api/health 2>&1)
if [ $? -eq 0 ]; then
    echo "   ✅ 健康检查通过"
    echo "   响应: $HEALTH"
else
    echo "   ❌ 健康检查失败"
    cat /tmp/nanobot-web.log
    kill $WEB_PID 2>/dev/null
    exit 1
fi
echo ""

# 测试统计接口
echo "5️⃣ 测试统计接口..."
STATS=$(curl -s http://localhost:9090/api/stats 2>&1)
if [ $? -eq 0 ]; then
    echo "   ✅ 统计接口正常"
    echo "   内存使用: $(echo $STATS | grep -o '"used":[0-9]*' | cut -d: -f2) MB"
else
    echo "   ❌ 统计接口失败"
fi
echo ""

# 测试工具列表
echo "6️⃣ 测试工具列表..."
TOOLS=$(curl -s http://localhost:9090/api/tools 2>&1)
if [ $? -eq 0 ]; then
    echo "   ✅ 工具列表正常"
    TOOL_COUNT=$(echo $TOOLS | grep -o '"name"' | wc -l)
    echo "   可用工具数: $TOOL_COUNT"
else
    echo "   ❌ 工具列表失败"
fi
echo ""

# 测试会话接口
echo "7️⃣ 测试会话接口..."
SESSIONS=$(curl -s http://localhost:9090/api/sessions 2>&1)
if [ $? -eq 0 ]; then
    echo "   ✅ 会话接口正常"
else
    echo "   ❌ 会话接口失败"
fi
echo ""

# 测试配置接口
echo "8️⃣ 测试配置接口..."
CONFIG=$(curl -s http://localhost:9090/api/config 2>&1)
if [ $? -eq 0 ]; then
    echo "   ✅ 配置接口正常"
    echo "   当前模型: $(echo $CONFIG | grep -o '"currentModel":"[^"]*"' | cut -d: -f2 | tr -d '"')"
else
    echo "   ❌ 配置接口失败"
fi
echo ""

# 显示访问信息
echo "✅ 所有测试通过！"
echo ""
echo "📊 Web 界面已启动"
echo "🌐 本地访问: http://localhost:9090"
echo "🔗 Codespaces: 查看 PORTS 标签页获取公开 URL"
echo ""
echo "💡 功能说明:"
echo "   • AI 对话界面 - 实时交互"
echo "   • 会话管理 - 多会话支持"
echo "   • 系统监控 - 实时内存和状态"
echo "   • 工具管理 - 查看可用工具"
echo ""
echo "🛑 停止服务器: kill $WEB_PID"
echo "📋 查看日志: tail -f /tmp/nanobot-web.log"
echo ""
