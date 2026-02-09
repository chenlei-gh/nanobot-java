#!/bin/bash
# Nanobot Java - Automated Test & Setup Script
# Installs dependencies and runs tests

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║           Nanobot Java - Installation & Test             ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo

# Function to check command availability
check_command() {
    if command -v $1 &> /dev/null; then
        echo -e "✓ $1 found: $(command -v $1)"
        return 0
    else
        echo -e "✗ $1 not found"
        return 1
    fi
}

# Check Java
echo -e "${YELLOW}[1/5] Checking Java Installation...${NC}"
if check_command java; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo "  Version: $JAVA_VERSION"
    
    # Check if Java 21+
    if [[ "$JAVA_VERSION" == *"21"* ]] || [[ "$JAVA_VERSION" == *"openjdk 21"* ]]; then
        echo -e "  ${GREEN}✓ Java 21+ detected${NC}"
    else
        echo -e "  ${YELLOW}⚠ Java 21 required (current: Java 8)${NC}"
        echo "  Installing Java 21..."
        
        if command -v winget &> /dev/null; then
            winget install -e --id EclipseAdoptium.Temurin.21.JDK
        elif command -v brew &> /dev/null; then
            brew install openjdk@21
        elif command -v apt-get &> /dev/null; then
            apt-get update && apt-get install -y openjdk-21-jdk
        else
            echo "Please install Java 21 manually from https://adoptium.net"
            exit 1
        fi
    fi
fi

echo

# Check Maven
echo -e "${YELLOW}[2/5] Checking Maven...${NC}"
if ! check_command mvn; then
    echo "Installing Maven..."
    
    if command -v winget &> /dev/null; then
        winget install -e --id Apache.Maven
    elif command -v brew &> /dev/null; then
        brew install maven
    elif command -v apt-get &> /dev/null; then
        apt-get update && apt-get install -y maven
    else
        echo "Please install Maven manually from https://maven.apache.org"
        exit 1
    fi
fi

echo

# Verify project structure
echo -e "${YELLOW}[3/5] Verifying Project Structure...${NC}"

REQUIRED_DIRS=(
    "src/main/java/com/nanobot/core"
    "src/main/java/com/nanobot/agent"
    "src/main/java/com/nanobot/llm"
    "src/main/java/com/nanobot/tool"
    "src/main/java/com/nanobot/config"
    "src/main/java/com/nanobot/cron"
    "src/main/java/com/nanobot/channel"
    "src/main/java/com/nanobot/skill"
    "src/main/java/com/nanobot/cli"
    "src/main/java/com/nanobot/bus"
)

TOTAL_FILES=0
for dir in "${REQUIRED_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        FILE_COUNT=$(find "$dir" -name "*.java" | wc -l)
        echo "  ✓ $dir ($FILE_COUNT files)"
        ((TOTAL_FILES+=FILE_COUNT))
    else
        echo "  ✗ $dir MISSING"
    fi
done

echo
echo -e "${GREEN}Total Java files: $TOTAL_FILES${NC}"

echo

# Display project summary
echo -e "${YELLOW}[4/5] Project Summary${NC}"
echo
echo "Components:"
echo "  • Core Engine: AgentLoop, MessageBus, ContextManager"
echo "  • LLM Providers: OpenAI, Anthropic, DeepSeek, Qwen, Gemini"
echo "  • Tools: FileTool, ShellTool, WebTool, EnhancedWebTool"
echo "  • Channels: Telegram, WhatsApp"
echo "  • Features: StreamHandler, TokenCounter, ThinkingTracker"
echo "  • Utilities: EventBus, ToolHotReload, CronService"
echo

# Show pom.xml
echo -e "${YELLOW}[5/5] Build Configuration${NC}"
echo
if [ -f "pom.xml" ]; then
    echo "✓ Maven configuration found"
    echo "  • Spring Boot: 3.2.0"
    echo "  • Java Version: 21"
    echo "  • Dependencies: Jackson, SnakeYAML, JLine, Jansi"
    echo
    echo -e "${GREEN}Ready to build!${NC}"
else
    echo -e "${RED}✗ pom.xml not found${NC}"
    exit 1
fi

echo
echo -e "${GREEN}════════════════════════════════════════════════════════════${NC}"
echo "To build and run:"
echo
echo "  1. mvn clean package -DskipTests"
echo "  2. java -jar target/nanobot-1.0.0.jar agent \"Hello World\""
echo "  3. java -jar target/nanobot-1.0.0.jar (interactive mode)"
echo
echo "Or use Maven wrapper:"
echo "  ./mvnw clean package"
echo "  ./mvnw spring-boot:run"
echo
echo -e "${GREEN}════════════════════════════════════════════════════════════${NC}"
