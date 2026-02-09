#!/bin/bash
# 🚀 Nanobot Java - One-Click Launch Script for GitHub Codespaces
# 此脚本用于在GitHub Codespaces中一键启动Nanobot

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║         🚀 Nanobot Java - Codespaces Launcher            ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo

# Check if running in Codespaces
if [ -n "$CODESPACES" ]; then
    echo -e "${GREEN}✓ Running in GitHub Codespaces${NC}"
else
    echo -e "${YELLOW}⚠ Not detected as Codespaces, but will continue anyway${NC}"
fi

echo
echo -e "${BLUE}[1/5]${NC} 检查环境..."
echo "  Java: $(java -version 2>&1 | head -n 1)"
echo "  Maven: $(mvn --version 2>&1 | head -n 1)"
echo

echo -e "${BLUE}[2/5]${NC} 清理并构建项目..."
cd /workspaces/nanobot 2>/dev/null || cd $(pwd)
mvn clean package -DskipTests -q
echo -e "  ${GREEN}✓ 构建完成${NC}"
echo

echo -e "${BLUE}[3/5]${NC} 设置API密钥..."
if [ -n "$OPENAI_API_KEY" ]; then
    echo -e "  ${GREEN}✓ OpenAI API密钥已设置${NC}"
else
    echo -e "  ${YELLOW}⚠ 未检测到OPENAI_API_KEY${NC}"
    echo "  请设置: export OPENAI_API_KEY=your-key"
fi

if [ -n "$ANTHROPIC_API_KEY" ]; then
    echo -e "  ${GREEN}✓ Anthropic API密钥已设置${NC}"
else
    echo -e "  ${YELLOW}⚠ 未检测到ANTHROPIC_API_KEY${NC}"
fi
echo

echo -e "${BLUE}[4/5]${NC} 可用命令:"
echo
cat << 'EOF'
  ┌────────────────────────────────────────────────────┐
  │  交互模式:                                         │
  │    java -jar target/nanobot-1.0.0.jar             │
  │                                                    │
  │  单次查询:                                         │
  │    java -jar target/nanobot-1.0.0.jar agent "Hello"│
  │                                                    │
  │  Maven运行:                                        │
  │    mvn exec:java -Dexec.mainClass=...            │
  │                                                    │
  │  查看帮助:                                         │
  │    java -jar target/nanobot-1.0.0.jar help      │
  └────────────────────────────────────────────────────┘
EOF
echo

echo -e "${BLUE}[5/5]${NC} 选择运行模式:"
echo
echo "  1) 交互模式 (Interactive mode)"
echo "  2) 单次查询 (Single query)"
echo "  3) 查看帮助 (Help)"
echo "  4) 仅构建 (Build only)"
echo "  5) 运行测试 (Run tests)"
echo
read -p "请选择 [1-5]: " choice
echo

case $choice in
    1)
        echo -e "${GREEN}启动交互模式...${NC}"
        java -jar target/nanobot-1.0.0.jar
        ;;
    2)
        echo -e "${YELLOW}请输入查询内容:${NC}"
        read -p "> " query
        java -jar target/nanobot-1.0.0.jar agent "$query"
        ;;
    3)
        java -jar target/nanobot-1.0.0.jar help
        ;;
    4)
        echo -e "${GREEN}构建完成！${NC}"
        ls -lh target/nanobot-*.jar
        ;;
    5)
        echo -e "${GREEN}运行测试...${NC}"
        mvn test
        ;;
    *)
        echo -e "${RED}无效选择${NC}"
        exit 1
        ;;
esac

echo
echo -e "${CYAN}════════════════════════════════════════════════════════════${NC}"
echo "感谢使用Nanobot Java!"
echo "文档: https://github.com/你的用户名/nanobot-java"
echo -e "${CYAN}════════════════════════════════════════════════════════════${NC}"
