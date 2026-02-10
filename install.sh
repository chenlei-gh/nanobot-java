#!/bin/bash

# Nanobot Java - 一键安装脚本
# One-click installation script

set -e

echo "╔════════════════════════════════════════════════════════════╗"
echo "║        Nanobot Java - 一键安装脚本                        ║"
echo "║        One-Click Installation Script                       ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查命令是否存在
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# 错误处理
error_exit() {
    echo -e "${RED}❌ 错误: $1${NC}" >&2
    exit 1
}

# 成功消息
success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# 警告消息
warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# 1. 检查 Git
echo "🔍 检查依赖..."
if ! command_exists git; then
    error_exit "未找到 Git。请先安装 Git: https://git-scm.com"
fi
success "Git 已安装"

# 2. 检查 Java
if ! command_exists java; then
    error_exit "未找到 Java。请安装 Java 21+: https://adoptium.net"
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    error_exit "Java 版本过低 (当前: $JAVA_VERSION, 需要: 21+)"
fi
success "Java $JAVA_VERSION 已安装"

# 3. 检查 Maven
if ! command_exists mvn; then
    error_exit "未找到 Maven。请安装 Maven 3.9+: https://maven.apache.org"
fi
success "Maven 已安装"

# 4. 设置安装目录
INSTALL_DIR="${NANOBOT_INSTALL_DIR:-$HOME/nanobot-java}"
echo ""
echo "📁 安装目录: $INSTALL_DIR"

# 5. 克隆或更新仓库
if [ -d "$INSTALL_DIR" ]; then
    warning "目录已存在，正在更新..."
    cd "$INSTALL_DIR"
    git pull origin main || error_exit "更新失败"
else
    echo "📦 正在克隆仓库..."
    git clone https://github.com/chenlei-gh/nanobot-java.git "$INSTALL_DIR" || error_exit "克隆失败"
    cd "$INSTALL_DIR"
fi
success "代码已准备就绪"

# 6. 编译项目
echo ""
echo "🔨 正在编译项目..."
mvn clean package -DskipTests -q || error_exit "编译失败"
success "编译完成"

# 7. 创建启动命令
echo ""
echo "🔗 创建全局命令..."

# 创建 nanobot 命令脚本
cat > "$INSTALL_DIR/nanobot" << 'SCRIPT_EOF'
#!/bin/bash
cd "$(dirname "$0")"
exec java -jar target/nanobot-1.0.0.jar "$@"
SCRIPT_EOF

chmod +x "$INSTALL_DIR/nanobot"

# 添加到 PATH（如果还没有）
SHELL_RC=""
if [ -n "$BASH_VERSION" ]; then
    SHELL_RC="$HOME/.bashrc"
elif [ -n "$ZSH_VERSION" ]; then
    SHELL_RC="$HOME/.zshrc"
fi

if [ -n "$SHELL_RC" ]; then
    if ! grep -q "nanobot-java" "$SHELL_RC" 2>/dev/null; then
        echo "" >> "$SHELL_RC"
        echo "# Nanobot Java" >> "$SHELL_RC"
        echo "export PATH=\"\$PATH:$INSTALL_DIR\"" >> "$SHELL_RC"
        success "已添加到 PATH"
        warning "请运行: source $SHELL_RC"
    fi
fi

# 8. 设置 API 密钥
echo ""
echo "🔑 设置 API 密钥"
echo ""
echo "请选择你要使用的 AI 服务:"
echo "  1) OpenAI (GPT-4)"
echo "  2) Anthropic (Claude)"
echo "  3) 稍后手动设置"
echo ""
read -p "请选择 [1-3]: " choice

case $choice in
    1)
        read -p "请输入 OpenAI API Key: " api_key
        if [ -n "$api_key" ]; then
            echo "export OPENAI_API_KEY=$api_key" >> "$SHELL_RC"
            export OPENAI_API_KEY="$api_key"
            success "OpenAI API Key 已设置"
        fi
        ;;
    2)
        read -p "请输入 Anthropic API Key: " api_key
        if [ -n "$api_key" ]; then
            echo "export ANTHROPIC_API_KEY=$api_key" >> "$SHELL_RC"
            export ANTHROPIC_API_KEY="$api_key"
            success "Anthropic API Key 已设置"
        fi
        ;;
    3)
        warning "请稍后手动设置 API Key"
        ;;
esac

# 9. 完成
echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║              🎉 安装完成！                                 ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "📚 快速开始:"
echo ""
echo "  1. 重新加载 shell 配置:"
echo "     source $SHELL_RC"
echo ""
echo "  2. 启动 Nanobot:"
echo "     cd $INSTALL_DIR && ./start.sh"
echo ""
echo "  或者直接运行:"
echo "     $INSTALL_DIR/nanobot"
echo ""
echo "📖 更多信息: https://github.com/chenlei-gh/nanobot-java"
echo ""
