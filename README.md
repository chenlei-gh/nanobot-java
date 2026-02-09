# 🚀 Nanobot Java - High-Performance AI Agent

**Java 21 + Virtual Threads Implementation**

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net)
[![Maven](https://img.shields.io/badge/Maven-3.9-green.svg)](https://maven.apache.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

## ✨ 一键启动 (GitHub Codespaces)

### 方式1: 直接访问 🔥
```
https://github.com/codespaces/new?repo=你的用户名/nanobot-java&ref=main&machine=standardLinux
```

### 方式2: GitHub页面启动
1. 访问你的GitHub仓库页面
2. 点击 **"Code"** 按钮
3. 选择 **"Codespaces"** → **"Create codespace"**

### 方式3: 使用这个一键链接
访问: https://github.com/features/codespaces

## 📋 在Codespaces中运行

打开终端(Terminal)执行：

```bash
# 1. 构建项目
mvn clean package -DskipTests

# 2. 设置API密钥
export OPENAI_API_KEY=sk-your-key-here
# 或
export ANTHROPIC_API_KEY=sk-ant-your-key-here

# 3. 交互模式运行
java -jar target/nanobot-1.0.0.jar

# 4. 或发送单条消息
java -jar target/nanobot-1.0.0.jar agent "你好，帮我写个Python脚本"

# 5. 查看帮助
java -jar target/nanobot-1.0.0.jar help
```

## 🎯 功能特性

| 类别 | 功能 |
|------|------|
| **核心引擎** | AgentLoop, MessageBus, ContextManager |
| **LLM提供商** | OpenAI, Anthropic, DeepSeek, Qwen, Gemini |
| **工具系统** | 文件操作, Shell命令, Web搜索/抓取 |
| **通信通道** | CLI, Telegram, WhatsApp |
| **高级特性** | 子Agent管理, 推理跟踪, 流式响应, Token计算 |
| **系统功能** | Cron调度, 技能系统, 热加载, 事件总线 |

## Configuration

Create a `nanobot.yaml` file:

```yaml
agents:
  defaults:
    model: gpt-4
    maxIterations: 20

  agents:
    assistant:
      name: "My Assistant"
      model: gpt-4
      tools:
        - read_file
        - write_file
        - bash
        - web_fetch

workspace: ~/.nanobot/workspace
data: ~/.nanobot/data
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `OPENAI_API_KEY` | OpenAI API key |
| `ANTHROPIC_API_KEY` | Anthropic API key |
| `BRAVE_SEARCH_API_KEY` | Brave Search API key (for web search) |

## Available Tools

| Tool | Description |
|------|-------------|
| `read_file` | Read file contents |
| `write_file` | Write content to file |
| `edit_file` | Replace text in file |
| `list_dir` | List directory contents |
| `bash` | Execute shell command |
| `web_fetch` | Fetch URL content |
| `web_search` | Search the web |

## CLI Commands

| Command | Description |
|---------|-------------|
| `/help` | Show help |
| `/exit` | Exit nanobot |
| `/clear` | Clear screen |
| `/stats` | Show statistics |
| `/sessions` | List sessions |
| `/tools` | List tools |
| `/cron` | List cron jobs |
| `/reset` | Clear sessions |

## Architecture

```
┌─────────────────────────────────────────┐
│            Nanobot Application           │
├─────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌───────┐ │
│  │  CLI     │  │   API    │  │ Web   │ │
│  └────┬─────┘  └────┬─────┘  └───┬───┘ │
│       │             │            │      │
│       └──────────────┴────────────┘      │
│                    │                    │
│              ┌─────▼─────┐               │
│              │ AgentLoop │               │
│              └─────┬─────┘               │
│                    │                     │
│  ┌────────────────┼──────────────────┐  │
│  │                │                  │  │
│  ▼                ▼                  ▼  │
│┌───────┐    ┌─────────┐    ┌──────────┐│
││ Tools │    │ LLM Prov│    │ Channels ││
│└───────┘    └─────────┘    └──────────┘│
│                                             
└─────────────────────────────────────────┘
```

## Virtual Threads Performance

Nanobot leverages Java 21's virtual threads for high-concurrency processing:

```java
// Message processing in virtual threads
ExecutorService vtp = Executors.newVirtualThreadPerTaskExecutor();
```

This allows thousands of concurrent message processing operations with minimal memory overhead.

## Extending Nanobot

### Adding Tools

```java
registry.register(
    "my_tool",
    "Description of my tool",
    Map.of(
        "param1", new ToolParameter("string", "Parameter description", true)
    ),
    true,
    (args, workspace) -> {
        // Tool implementation
        return "Result";
    }
);
```

### Adding Channels

```java
public class MyChannel implements Channel {
    // Implement Channel interface
}
```

## Project Structure

```
src/main/java/com/nanobot/
├── NanobotApplication.java    # Spring Boot entry
├── cli/
│   └── NanobotCli.java        # Interactive CLI
├── core/
│   ├── AgentLoop.java        # Core agent logic
│   ├── MessageBus.java      # Pub/sub messaging
│   └── ContextManager.java  # Conversation context
├── tool/
│   ├── ToolRegistry.java    # Tool management
│   ├── FileTool.java        # File operations
│   ├── ShellTool.java       # Shell commands
│   └── WebTool.java         # Web operations
├── llm/
│   ├── LlmProvider.java     # Provider interface
│   ├── OpenAiProvider.java  # OpenAI implementation
│   └── AnthropicProvider.java # Claude implementation
├── config/
│   ├── NanobotConfig.java   # Config model
│   └── YamlConfigLoader.java # YAML parser
├── cron/
│   ├── CronService.java     # Job scheduling
│   └── CronJob.java         # Job definition
├── channel/
│   └── Channel.java         # Channel interface
└── skill/
    ├── Skill.java           # Skill model
    └── SkillLoader.java     # Skill loader
```

## License

Apache License 2.0

## Credits

Based on [HKUDS/nanobot](https://github.com/HKUDS/nanobot) - Ultra-lightweight AI Assistant
