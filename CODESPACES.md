# Nanobot Java - GitHub Codespaces Quick Start

## 一键启动

1. 访问: https://github.com/new/codespaces
2. 选择: "Public repository"
3. 粘贴仓库URL

或者直接在GitHub仓库页面:
- 点击 "Code" 按钮
- 选择 "Codespaces"
- 点击 "Create codespace"

## 手动创建 (如果已有仓库)

```bash
# 在Codespaces终端中执行
# 1. 克隆项目 (如果需要)
git clone <your-nanobot-repo>
cd nanobot

# 2. 安装依赖并构建
mvn clean package -DskipTests

# 3. 运行
java -jar target/nanobot-1.0.0.jar agent "Hello World"

# 4. 或进入交互模式
java -jar target/nanobot-1.0.0.jar
```

## Docker方式 (如果已安装Docker)

```bash
# 构建镜像
docker build -t nanobot-java .

# 运行
docker run -it nanobot-java agent "Hello"
```

## 包含的功能测试

```bash
# 运行单元测试
mvn test

# 查看测试报告
cat target/surefire-reports/*.txt
```

## 快速验证

```bash
# 1. 检查Java版本
java -version

# 2. 检查Maven版本
mvn --version

# 3. 编译项目
mvn compile

# 4. 运行所有测试
mvn test

# 5. 打包
mvn package -DskipTests

# 6. 查看产物
ls -la target/*.jar
```

## 已实现的组件测试

```bash
# 单独测试各模块
mvn test -Dtest=ContextManagerTest
mvn test -Dtest=MessageBusTest
mvn test -Dtest=ToolRegistryTest
mvn test -Dtest=LlmProviderTest
```

## GitHub Codespaces 优势

✅ 完全免费（每月120 core hours）
✅ 预装Java 21 + Maven
✅ 完整的Linux环境
✅ 可以安装额外工具
✅ 支持Docker
✅ VS Code集成

## 替代在线IDE

- **GitHub Codespaces**: github.com/features/codespaces
- **Gitpod**: gitpod.io
- **CodeSandbox**: codesandbox.io
- **Replit**: replit.com
