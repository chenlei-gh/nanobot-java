#!/bin/bash

# Nanobot Java - 快速设置脚本
# Quick setup script

echo "╔════════════════════════════════════════════════════════════╗"
echo "║           Nanobot Java - 快速设置向导                     ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# 创建配置文件
echo "📝 创建配置文件..."

cat > nanobot.yaml << 'EOF'
# Nanobot 配置文件

agents:
  defaults:
    model: gpt-4
    maxIterations: 20
    temperature: 0.7

  agents:
    assistant:
      name: "Nanobot Assistant"
      model: gpt-4
      tools:
        - read_file
        - write_file
        - shell
        - web_fetch

# 工作目录
workspace: ~/.nanobot/workspace
data: ~/.nanobot/data

# 工具配置
tools:
  webTools:
    enabled: true
    timeout: 30000
EOF

echo "✅ 配置文件已创建: nanobot.yaml"
echo ""

# 设置 API 密钥
echo "🔑 设置 API 密钥"
echo ""
echo "请选择你要使用的 AI 服务:"
echo "  1) OpenAI (GPT-4, GPT-3.5)"
echo "  2) Anthropic (Claude)"
echo "  3) 两者都设置"
echo "  4) 跳过"
echo ""
read -p "请选择 (1-4): " choice

case $choice in
    1)
        read -p "请输入 OpenAI API Key: " openai_key
        echo "export OPENAI_API_KEY=$openai_key" >> ~/.bashrc
        echo "export OPENAI_API_KEY=$openai_key" >> ~/.zshrc 2>/dev/null || true
        export OPENAI_API_KEY=$openai_key
        echo "✅ OpenAI API Key 已设置"
        ;;
    2)
        read -p "请输入 Anthropic API Key: " anthropic_key
        echo "export ANTHROPIC_API_KEY=$anthropic_key" >> ~/.bashrc
        echo "export ANTHROPIC_API_KEY=$anthropic_key" >> ~/.zshrc 2>/dev/null || true
        export ANTHROPIC_API_KEY=$anthropic_key
        echo "✅ Anthropic API Key 已设置"
        ;;
    3)
        read -p "请输入 OpenAI API Key: " openai_key
        read -p "请输入 Anthropic API Key: " anthropic_key
        echo "export OPENAI_API_KEY=$openai_key" >> ~/.bashrc
        echo "export ANTHROPIC_API_KEY=$anthropic_key" >> ~/.bashrc
        echo "export OPENAI_API_KEY=$openai_key" >> ~/.zshrc 2>/dev/null || true
        echo "export ANTHROPIC_API_KEY=$anthropic_key" >> ~/.zshrc 2>/dev/null || true
        export OPENAI_API_KEY=$openai_key
        export ANTHROPIC_API_KEY=$anthropic_key
        echo "✅ API Keys 已设置"
        ;;
    4)
        echo "⏭️  跳过 API Key 设置"
        ;;
    *)
        echo "❌ 无效选择"
        exit 1
        ;;
esac

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ 设置完成！"
echo ""
echo "现在你可以运行:"
echo "  ./start.sh          # 启动交互模式"
echo "  ./start.sh help     # 查看帮助"
echo ""
echo "注意: 如果你设置了 API Key，请重新加载 shell 配置:"
echo "  source ~/.bashrc    # 或 source ~/.zshrc"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
