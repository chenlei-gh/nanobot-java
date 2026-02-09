# 🎯 GitHub Codespaces 启动指南 - Nanobot Java

## 📋 快速索引

### 第一步：上传代码到GitHub
```bash
# 1. 创建GitHub仓库
# 访问 https://github.com/new

# 2. 本地初始化并推送
cd F:\nanobot
git init
git add .
git commit -m "Initial commit: Nanobot Java implementation"
git branch -M main
git remote add origin https://github.com/你的用户名/nanobot-java.git
git push -u origin main
```

### 第二步：启动Codespaces

**方案A: GitHub网页**
1. 打开: https://github.com/你的用户名/nanobot-java
2. 点击 **"Code"** 绿色按钮
3. 选择 **"Codespaces"** 标签
4. 点击 **"Create codespace on main"**

**方案B: 直接链接**
```
https://github.com/codespaces/new?repo=你的用户名/nanobot-java&ref=main
```

### 第三步：在Codespaces中运行

在Terminal终端中执行：

```bash
# 1. 构建项目
mvn clean package -DskipTests

# 2. 设置API密钥
export OPENAI_API_KEY=sk-你的OpenAI密钥
# 或
export ANTHROPIC_API_KEY=sk-ant-你的Anthropic密钥

# 3. 运行
java -jar target/nanobot-1.0.0.jar agent "你好，帮我写个Python脚本"
```

## 🎮 使用场景

### 场景1: 交互对话
```bash
java -jar target/nanobot-1.0.0.jar
```
进入交互模式，输入问题。

### 场景2: 单次查询
```bash
java -jar target/nanobot-1.0.0.jar agent "什么是机器学习？"
```

### 场景3: 运行测试
```bash
mvn test
```

## 🔧 Codespaces配置

项目已包含以下配置：

- `.devcontainer/devcontainer.json` - 容器配置
- `Dockerfile` - Docker镜像配置
- `.github/workflows/build.yml` - CI/CD配置

## 📦 资源使用

- **CPU**: 2核心
- **内存**: 4GB
- **存储**: 32GB
- **免费额度**: 每月120 core hours

## ⚡ 快捷命令

```bash
# 一键构建并运行
mvn clean package -DskipTests && java -jar target/nanobot-1.0.0.jar agent "Hello"

# 查看帮助
java -jar target/nanobot-1.0.0.jar help

# 查看版本
java -jar target/nanobot-1.0.0.jar version

# 进入交互shell
java -jar target/nanobot-1.0.0.jar shell
```

## 🐛 常见问题

### Q: 构建失败？
```bash
# 清理并重新构建
mvn clean
mvn compile
```

### Q: API密钥错误？
```bash
# 检查密钥设置
echo $OPENAI_API_KEY

# 重新设置
export OPENAI_API_KEY=sk-新密钥
```

### Q: 内存不足？
```bash
# 使用较小模型
java -jar target/nanobot-1.0.0.jar agent --model gpt-3.5-turbo "问题"
```

## 📚 相关链接

- **项目文档**: [README.md](README.md)
- **快速开始**: [QUICKSTART.md](QUICKSTART.md)
- **Codespaces**: [https://github.com/features/codespaces](https://github.com/features/codespaces)
- **Java 21**: [https://adoptium.net](https://adoptium.net)
- **Maven**: [https://maven.apache.org](https://maven.apache.org)

## 🎉 开始使用

1. ✅ 上传代码到GitHub
2. ✅ 启动Codespaces
3. ✅ 执行 `mvn clean package -DskipTests`
4. ✅ 设置API密钥
5. ✅ 运行 `java -jar target/nanobot-1.0.0.jar agent "你好！"`

**祝你使用愉快！** 🎉
