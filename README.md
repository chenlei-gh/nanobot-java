# 🤖 Nanobot Java - 智能 AI 助手

<div align="center">

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net)
[![Maven](https://img.shields.io/badge/Maven-3.9-green.svg)](https://maven.apache.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**基于 Java 21 虚拟线程的高性能 AI Agent**

[快速开始](#-快速开始) • [功能特性](#-功能特性) • [使用指南](#-使用指南) • [常见问题](#-常见问题)

</div>

---

## 📖 简介

Nanobot Java 是一个轻量级、高性能的 AI 助手，支持多种 AI 模型（OpenAI、Claude、DeepSeek 等），可以帮你完成文件操作、代码编写、网页搜索等各种任务。

### ✨ 主要特点

- 🚀 **一键启动** - 无需复杂配置，开箱即用
- 🔥 **高性能** - 基于 Java 21 虚拟线程，支持高并发
- 🪶 **超轻量** - 仅 13 MB，启动仅需 1.4 秒
- 💾 **内存优化** - 长期运行内存稳定在 450MB，自动清理机制
- 📊 **实时监控** - 内置 Web 监控面板，实时查看运行状态和内存使用
- 🤖 **多模型支持** - OpenAI、Anthropic、DeepSeek、Qwen、Gemini
- 🛠️ **丰富工具** - 文件操作、Shell 命令、网页搜索
- 💬 **多种交互方式** - CLI、Telegram、WhatsApp
- 🔌 **易于扩展** - 简单的插件系统

---

## 🚀 快速开始

### 方式一：一键安装（推荐）⚡

只需一条命令，自动完成所有配置：

```bash
curl -fsSL https://raw.githubusercontent.com/chenlei-gh/nanobot-java/main/install.sh | bash
```

或者使用 wget：

```bash
wget -qO- https://raw.githubusercontent.com/chenlei-gh/nanobot-java/main/install.sh | bash
```

安装完成后，运行：

```bash
cd ~/nanobot-java && ./start.sh
```

就这么简单！🎉

### 方式二：手动安装

#### 1. 克隆项目
```bash
git clone https://github.com/chenlei-gh/nanobot-java.git
cd nanobot-java
```

#### 2. 设置 API 密钥
```bash
# 设置 OpenAI API Key
export OPENAI_API_KEY=sk-your-key-here

# 或设置 Anthropic API Key
export ANTHROPIC_API_KEY=sk-ant-your-key-here
```

#### 3. 启动 Nanobot
```bash
# Linux/Mac
./start.sh

# Windows
start.bat
```

### 方式三：Docker 部署

```bash
# 1. 设置环境变量
export OPENAI_API_KEY=sk-your-key-here

# 2. 启动容器
docker-compose up -d

# 3. 进入交互模式
docker exec -it nanobot-java java -jar target/nanobot-1.0.0.jar
```

### 方式四：完全手动部署

```bash
# 1. 确保已安装 Java 21+ 和 Maven 3.9+
java -version
mvn -version

# 2. 设置 API 密钥
export OPENAI_API_KEY=sk-your-key-here
# 或
export ANTHROPIC_API_KEY=sk-ant-your-key-here

# 3. 编译项目
mvn clean package -DskipTests

# 4. 运行
java -jar target/nanobot-1.0.0.jar
```

---

## 🎯 功能特性

### 核心功能

| 功能 | 说明 |
|------|------|
| 💬 **智能对话** | 支持多轮对话，理解上下文 |
| 📁 **文件操作** | 读取、写入、编辑文件 |
| 🖥️ **命令执行** | 执行 Shell 命令 |
| 🌐 **网页搜索** | 搜索和抓取网页内容 |
| 🔄 **流式响应** | 实时显示 AI 回复 |
| 📊 **Token 统计** | 实时统计 Token 使用量 |

### 支持的 AI 模型

- **OpenAI**: GPT-4, GPT-4 Turbo, GPT-3.5 Turbo
- **Anthropic**: Claude 3.5 Sonnet, Claude 3 Opus
- **DeepSeek**: DeepSeek Chat
- **Qwen**: 通义千问
- **Gemini**: Google Gemini

---

## 📚 使用指南

### 基本使用

#### 交互模式
```bash
./start.sh
```

进入交互模式后，直接输入你的问题：
```
nanobot> 帮我写一个 Python 脚本，读取 CSV 文件并统计数据
nanobot> 查看当前目录下的所有文件
nanobot> 搜索最新的 AI 新闻
```

#### 单次命令模式
```bash
./start.sh agent "帮我写一个 Hello World 程序"
```

### 内置命令

在交互模式下，你可以使用以下命令：

| 命令 | 说明 |
|------|------|
| `/help` | 显示帮助信息 |
| `/exit` | 退出程序 |
| `/clear` | 清屏 |
| `/stats` | 显示统计信息 |
| `/tools` | 列出可用工具 |
| `/sessions` | 列出会话 |
| `/reset` | 清除会话历史 |

### 配置文件

创建 `nanobot.yaml` 文件来自定义配置：

```yaml
agents:
  defaults:
    model: gpt-4              # 默认模型
    maxIterations: 20         # 最大迭代次数
    temperature: 0.7          # 温度参数

  agents:
    assistant:
      name: "我的助手"
      model: gpt-4
      tools:
        - read_file           # 读取文件
        - write_file          # 写入文件
        - shell               # 执行命令
        - web_fetch           # 网页抓取

workspace: ~/.nanobot/workspace  # 工作目录
data: ~/.nanobot/data            # 数据目录
```

### 环境变量

| 变量名 | 说明 | 必需 |
|--------|------|------|
| `OPENAI_API_KEY` | OpenAI API 密钥 | 二选一 |
| `ANTHROPIC_API_KEY` | Anthropic API 密钥 | 二选一 |
| `BRAVE_SEARCH_API_KEY` | Brave 搜索 API 密钥 | 可选 |

---

## 🌐 Web 界面

Nanobot 提供完整的 Web 界面，包含 AI 对话、参数配置和实时监控功能。

### 启动 Web 服务

```bash
./start-web.sh
```

或者手动启动：

```bash
export OPENAI_API_KEY=sk-your-key-here
java -cp "target/classes:target/nanobot-1.0.0.jar" com.nanobot.web.WebServerTest
```

### 访问 Web 界面

在浏览器中打开：
```
http://localhost:9090
```

**Codespaces 用户**: 在 VS Code 的 "PORTS" 标签页中找到端口 9090，点击地球图标获取公开 URL。

### 功能特性

#### 💬 AI 对话界面
- 实时对话交互
- 流式响应显示
- 多会话管理
- 历史记录查看

#### ⚙️ 参数配置
- 模型选择（GPT-4, Claude, DeepSeek 等）
- 温度参数调节
- 最大迭代次数设置
- 工具启用/禁用

#### 📊 实时监控
- 内存使用情况
- 会话统计
- 消息队列状态
- 系统健康检查

#### 🎨 现代化界面
- 响应式设计
- 渐变色主题
- 流畅动画
- 移动端适配

### 自定义端口

通过环境变量设置 Web 端口：

```bash
export WEB_PORT=8080
./start-web.sh
```

### API 接口

#### 对话接口
- `POST /api/chat` - 发送消息并获取 AI 响应
- `GET /api/stream` - SSE 流式响应

#### 会话管理
- `GET /api/sessions` - 获取所有会话列表
- `DELETE /api/sessions?sessionId=xxx` - 清除指定会话

#### 配置管理
- `GET /api/config` - 获取当前配置
- `GET /api/tools` - 获取可用工具列表

#### 监控接口
- `GET /api/stats` - 获取系统统计数据
- `GET /api/health` - 健康检查

---

## 💡 使用示例

### 示例 1：文件操作
```
nanobot> 读取 README.md 文件的内容
nanobot> 在 output.txt 中写入 "Hello, World!"
```

### 示例 2：代码编写
```
nanobot> 帮我写一个 Python 脚本，实现快速排序算法
nanobot> 优化这段代码的性能
```

### 示例 3：数据分析
```
nanobot> 分析 data.csv 文件，统计各列的平均值
nanobot> 生成一个数据可视化图表
```

### 示例 4：网页搜索
```
nanobot> 搜索最新的 AI 技术趋势
nanobot> 获取 https://example.com 的内容
```

---

## 🔧 高级功能

### 添加自定义工具

```java
// 在 ToolRegistry 中注册新工具
registry.register(
    "my_tool",
    "我的自定义工具",
    Map.of(
        "param1", new ToolParameter("string", "参数说明", true)
    ),
    true,
    (args, workspace) -> {
        // 工具实现
        return "执行结果";
    }
);
```

### 定时任务

```yaml
cron:
  jobs:
    - name: "每日报告"
      schedule: "0 9 * * *"  # 每天 9:00
      message: "生成今日工作报告"
```

---

## ❓ 常见问题

### Q: 如何获取 API 密钥？

**OpenAI:**
1. 访问 [OpenAI Platform](https://platform.openai.com)
2. 注册/登录账号
3. 进入 API Keys 页面创建密钥

**Anthropic:**
1. 访问 [Anthropic Console](https://console.anthropic.com)
2. 注册/登录账号
3. 创建 API 密钥

### Q: 支持哪些操作系统？

支持所有主流操作系统：
- ✅ Linux
- ✅ macOS
- ✅ Windows
- ✅ Docker

### Q: 需要什么配置？

最低配置：
- Java 21+
- 2GB RAM
- 500MB 磁盘空间

推荐配置：
- Java 21+
- 4GB RAM
- 1GB 磁盘空间

### Q: 如何更新到最新版本？

```bash
git pull origin main
./start.sh
```

### Q: 遇到问题怎么办？

1. 查看 [Issues](https://github.com/chenlei-gh/nanobot-java/issues)
2. 提交新的 Issue
3. 加入讨论组

---

## ⚡ 性能优化

Nanobot Java 经过深度优化，在保持完整功能的同时实现了极致的轻量化：

### 📊 性能指标

| 指标 | 数值 | 对比 |
|------|------|------|
| **包体积** | 13 MB | 比平均 Spring Boot 应用小 60% |
| **启动时间** | 1.4 秒 | 比优化前快 44% |
| **内存占用** | ~100 MB | 比优化前省 33% |
| **依赖数量** | 28 个 | 精简 38% |

### 🎯 与其他 AI Agent 对比

| 项目 | 语言 | 大小 | 启动时间 | 内存 |
|------|------|------|----------|------|
| **Nanobot Java** | Java 21 | **13 MB** | **1.4s** | **100 MB** |
| 典型 Node.js Agent | Node.js | 100-200 MB | 2-3s | 150-200 MB |
| 典型 Python Agent | Python | 50-100 MB | 3-5s | 200-300 MB |
| 典型 Spring Boot | Java | 30-50 MB | 3-5s | 200-300 MB |

### 🔧 优化措施

- ✅ 移除不必要的 Web 服务器（节省 8 MB）
- ✅ 精简依赖树（减少 38% 依赖）
- ✅ 启用编译优化
- ✅ JAR 压缩优化
- ✅ 使用 Java 21 虚拟线程（高并发低开销）

详细优化说明请查看 [MEMORY_OPTIMIZATION.md](MEMORY_OPTIMIZATION.md)

---

## 📦 项目结构

```
nanobot-java/
├── src/main/java/com/nanobot/
│   ├── cli/              # 命令行界面
│   ├── core/             # 核心引擎
│   ├── tool/             # 工具系统
│   ├── llm/              # LLM 提供商
│   ├── channel/          # 通信通道
│   ├── agent/            # Agent 管理
│   └── config/           # 配置管理
├── start.sh              # 启动脚本 (Linux/Mac)
├── start.bat             # 启动脚本 (Windows)
├── setup.sh              # 设置向导
├── Dockerfile            # Docker 镜像
├── docker-compose.yml    # Docker Compose 配置
└── README.md             # 本文件
```

---

## 🤝 贡献

欢迎贡献代码！请查看 [贡献指南](CONTRIBUTING.md)。

---

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

---

## 🙏 致谢

基于 [HKUDS/nanobot](https://github.com/HKUDS/nanobot) 项目开发。

---

## 📞 联系方式

- GitHub Issues: [提交问题](https://github.com/chenlei-gh/nanobot-java/issues)
- Email: your-email@example.com

---

<div align="center">

**如果这个项目对你有帮助，请给个 ⭐️ Star！**

Made with ❤️ by Nanobot Team

</div>
