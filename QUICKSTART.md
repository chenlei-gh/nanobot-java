# Nanobot Java - 完整使用指南

## 📋 快速索引

| 场景 | 推荐方案 | 链接 |
|------|---------|------|
| **无安装权限** | GitHub Codespaces | https://github.com/features/codespaces |
| **在线运行** | Replit | https://replit.com |
| **Docker环境** | 已有Docker | `docker build` |
| **便携Java** | 下载ZIP包 | 无需安装 |

---

## 🔧 方案1: GitHub Codespaces (推荐 - 免费)

### 步骤1: 准备仓库
```bash
# 1. 在GitHub创建新仓库
# 2. 上传所有文件
# 或使用GitHub Desktop同步
```

### 步骤2: 启动Codespaces
1. 打开GitHub仓库页面
2. 点击绿色 "Code" 按钮
3. 选择 "Codespaces" → "Create codespace"

### 步骤3: 在Codespaces中执行
```bash
# 检查环境
java -version
mvn --version

# 构建项目
mvn clean package -DskipTests

# 运行CLI模式
java -jar target/nanobot-1.0.0.jar agent "你好，帮我写个Python脚本"

# 或交互模式
java -jar target/nanobot-1.0.0.jar
```

---

## 🔧 方案2: Replit (在线IDE)

### 步骤
1. 访问: https://replit.com
2. 点击 "Create Replit"
3. 选择 "Java" 模板
4. 导入项目文件

### 运行
```bash
# Replit会自动检测pom.xml
mvn clean package
java -jar target/nanobot-1.0.0.jar agent "Hello"
```

---

## 🔧 方案3: Gitpod (免费在线)

### 步骤
1. 访问: https://gitpod.io
2. 粘贴GitHub仓库URL
3. 自动创建开发环境

### 快捷链接
```
https://gitpod.io/#https://github.com/your-username/nanobot-java
```

---

## 🔧 方案4: 便携Java环境 (无需安装)

### 下载便携版Java (ZIP格式)
```bash
# Windows (SikuliX便携版，包含Java)
# 下载地址: https://github.com/merveilles/The-Turning-Value/tree/master/tools

# 提取后设置JAVA_HOME
set JAVA_HOME=C:\path\to\portable-java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%

# 验证
java -version
```

---

## 🔧 方案5: 使用Docker Hub镜像

### 已有Docker环境
```bash
# 构建
docker build -t nanobot-java .

# 运行
docker run -it nanobot-java agent "Hello World"
```

### 使用现成镜像
```bash
# 使用OpenJDK 21镜像
docker run -it --rm \
  -v $(pwd):/app \
  -w /app \
  eclipse-temurin:21-jdk-alpine \
  mvn clean package && \
  java -jar target/nanobot-1.0.0.jar agent "Test"
```

---

## 📦 快速验证脚本

### 方案A: 完整构建测试
```bash
#!/bin/bash
echo "=== Nanobot Java 构建测试 ==="

# 1. 检查环境
echo "[1/5] 检查构建环境..."
command -v java >/dev/null 2>&1 && java -version || echo "需要Java环境"
command -v mvn >/dev/null 2>&1 && mvn --version || echo "需要Maven"

# 2. 清理
echo "[2/5] 清理项目..."
mvn clean

# 3. 编译
echo "[3/5] 编译代码..."
mvn compile

# 4. 测试
echo "[4/5] 运行测试..."
mvn test

# 5. 打包
echo "[5/5] 打包..."
mvn package -DskipTests

echo "完成！产物: target/nanobot-1.0.0.jar"
```

### 方案B: Docker一键测试
```bash
#!/bin/bash
# Docker环境快速测试
docker run --rm \
  -v "$(pwd)":/project \
  -w /project \
  maven:3.9-eclipse-temurin-21 \
  bash -c "mvn clean package -DskipTests && java -jar target/nanobot-1.0.0.jar agent 'Test'"
```

---

## 🎯 预期测试结果

运行后应该看到：

```
╔════════════════════════════════════════════════════╗
║           Nanobot Java - Verification Tests      ║
╚════════════════════════════════════════════════════╝

Testing ContextManager...
  ✓ ContextManager basic operations
Testing MessageBus...
  ✓ MessageBus core functionality
Testing ToolRegistry...
  ✓ ToolRegistry registration
Testing TokenCounter...
  ✓ TokenCounter estimation
Testing EventBus...
  ✓ EventBus publish/subscribe
Testing StreamHandler...
  ✓ StreamHandler chunk processing
Testing ThinkingTracker...
  ✓ ThinkingTracker reasoning
Testing SubagentManager...
  ✓ SubagentManager background tasks

────────────────────────────────────────────────────
Total: 8 tests
Passed: 8
Failed: 0
────────────────────────────────────────────────────

╔════════════════════════════════════════════════════╗
║              All Tests Passed!                   ║
╚════════════════════════════════════════════════════╝
```

---

## 📞 获取帮助

1. **GitHub Issues**: 报告构建问题
2. **文档**: 查看 README.md
3. **示例**: 查看 examples/ 目录

---

## ⚡ 下一步

设置好环境后：
1. ✅ 运行测试: `mvn test`
2. ⚙️ 配置API密钥: `export OPENAI_API_KEY=your-key`
3. 🚀 启动应用: `java -jar target/nanobot-1.0.0.jar`
4. 💬 开始对话: `/help` 查看命令
