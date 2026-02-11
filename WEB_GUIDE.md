# Nanobot Web 界面使用指南

## 快速开始

### 1. 设置 API 密钥

```bash
# 使用 OpenAI
export OPENAI_API_KEY=sk-your-key-here

# 或使用 Anthropic
export ANTHROPIC_API_KEY=sk-ant-your-key-here
```

### 2. 启动 Web 服务器

```bash
./start-web.sh
```

### 3. 访问界面

- **本地**: http://localhost:9090
- **Codespaces**: 在 VS Code 的 "PORTS" 标签页中找到端口 9090，点击地球图标

## 界面功能

### 💬 对话界面

#### 基本使用
1. 在输入框中输入问题
2. 按 Enter 或点击"发送"按钮
3. 等待 AI 响应

#### 示例对话
```
你: 帮我读取 README.md 文件的前 10 行
AI: 我来帮你读取文件...
    [使用 read_file 工具]
    文件内容如下：...

你: 创建一个 hello.txt 文件，内容是 "Hello, World!"
AI: 我来创建文件...
    [使用 write_file 工具]
    文件已创建成功！

你: 搜索最新的 AI 新闻
AI: 我来搜索...
    [使用 web_fetch 工具]
    以下是最新的 AI 新闻：...
```

### 📋 会话管理

#### 创建新会话
1. 点击侧边栏的"➕ 新建会话"按钮
2. 新会话自动激活
3. 在新会话中开始对话

#### 切换会话
1. 点击会话列表中的会话名称
2. 对话区域切换到该会话
3. 历史消息自动加载

#### 删除会话
1. 点击会话右侧的"删除"按钮
2. 会话立即删除
3. 如果删除的是当前会话，自动切换到默认会话

**注意**: 默认会话不能删除

### 📊 系统监控

#### 实时指标
- **内存使用**: 显示当前内存占用（MB）
- **活跃会话**: 显示当前会话数量
- **状态**: 显示服务器运行状态

#### 自动刷新
- 每 3 秒自动更新
- 无需手动刷新
- 实时反映系统状态

### 🛠️ 工具列表

显示所有可用的工具：
- **read_file**: 读取文件内容
- **write_file**: 写入文件内容
- **shell**: 执行 Shell 命令
- **web_fetch**: 抓取网页内容

## 高级功能

### API 接口

#### 对话接口
```bash
curl -X POST http://localhost:9090/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "my-session",
    "message": "你好，请介绍一下你自己"
  }'
```

响应：
```json
{
  "response": "你好！我是 Nanobot...",
  "sessionId": "my-session",
  "timestamp": 1234567890
}
```

#### 会话列表
```bash
curl http://localhost:9090/api/sessions
```

响应：
```json
{
  "sessions": [
    {
      "sessionId": "default",
      "messageCount": 10,
      "estimatedTokens": 2500,
      "oldestMessage": 1234567890,
      "latestMessage": 1234567900
    }
  ]
}
```

#### 删除会话
```bash
curl -X DELETE "http://localhost:9090/api/sessions?sessionId=my-session"
```

#### 系统统计
```bash
curl http://localhost:9090/api/stats
```

响应：
```json
{
  "memory": {
    "used": 100,
    "total": 512,
    "max": 2048,
    "free": 412
  },
  "sessions": 3,
  "timestamp": 1234567890
}
```

#### 健康检查
```bash
curl http://localhost:9090/api/health
```

响应：
```json
{
  "status": "ok",
  "timestamp": 1234567890
}
```

### 自定义端口

```bash
export WEB_PORT=8080
./start-web.sh
```

### 后台运行

```bash
nohup ./start-web.sh > /tmp/nanobot-web.log 2>&1 &
```

查看日志：
```bash
tail -f /tmp/nanobot-web.log
```

停止服务：
```bash
pkill -f WebServerTest
```

## 使用技巧

### 1. 高效对话

**明确指令**
```
❌ 不好: "帮我看看文件"
✅ 好: "读取 src/main/java/Main.java 文件的内容"
```

**分步骤**
```
1. 先读取文件
2. 分析内容
3. 提出建议
```

**使用上下文**
```
你: 读取 config.yaml 文件
AI: [显示内容]
你: 把 port 改成 8080
AI: [修改文件]
```

### 2. 会话管理

**按项目分会话**
- 项目 A 会话
- 项目 B 会话
- 测试会话

**按任务分会话**
- 代码审查会话
- 文档编写会话
- 问题调试会话

### 3. 监控优化

**内存监控**
- 定期查看内存使用
- 超过 80% 时考虑重启
- 删除不需要的会话

**性能优化**
- 避免过长的对话历史
- 定期清空会话
- 使用多个短会话而不是一个长会话

## 常见问题

### Q: 为什么对话没有响应？

A: 检查以下几点：
1. API 密钥是否正确设置
2. 网络连接是否正常
3. 查看浏览器控制台错误
4. 检查服务器日志

### Q: 如何清空对话历史？

A: 点击对话界面右上角的"清空对话"按钮

### Q: 会话数据会保存吗？

A: 会话数据存储在内存中，重启服务器后会清空。持久化存储功能正在开发中。

### Q: 支持多用户吗？

A: 目前是单用户模式，每个浏览器标签页共享同一个服务器实例。

### Q: 如何添加自定义工具？

A: 在 `ToolRegistry.java` 中注册新工具，Web 界面会自动显示。

### Q: 支持文件上传吗？

A: 目前不支持，可以通过对话让 AI 创建文件。文件上传功能在未来计划中。

### Q: 如何使用不同的 AI 模型？

A: 目前模型在启动时配置，动态切换功能正在开发中。

## 故障排除

### 服务器无法启动

```bash
# 检查端口是否被占用
lsof -i :9090

# 杀死占用端口的进程
kill -9 <PID>

# 或使用其他端口
export WEB_PORT=8080
./start-web.sh
```

### 对话超时

```bash
# 增加超时时间（修改 WebServer.java）
String response = future.get(120, TimeUnit.SECONDS); // 改为 120 秒
```

### 内存不足

```bash
# 增加 JVM 内存（修改 start-web.sh）
java -Xmx1024m -cp "target/classes:target/nanobot-1.0.0.jar" com.nanobot.web.WebServerTest
```

### 查看详细日志

```bash
# 启动时输出详细日志
java -cp "target/classes:target/nanobot-1.0.0.jar" com.nanobot.web.WebServerTest 2>&1 | tee web.log
```

## 性能指标

### 启动时间
- 冷启动: 1-2 秒
- 热启动: < 1 秒

### 内存占用
- 初始: ~100 MB
- 运行中: ~200-300 MB
- 峰值: < 512 MB

### 响应时间
- 健康检查: < 10ms
- 统计接口: < 50ms
- 对话接口: 取决于 AI 模型（通常 2-10 秒）

### 并发能力
- 支持虚拟线程
- 理论上可处理数千并发连接
- 实际受限于 AI API 速率限制

## 安全建议

### 1. 不要暴露到公网

Web 界面没有身份验证，仅用于本地开发。

### 2. 保护 API 密钥

```bash
# 不要在代码中硬编码
# 使用环境变量
export OPENAI_API_KEY=sk-xxx

# 或使用配置文件（添加到 .gitignore）
echo "OPENAI_API_KEY=sk-xxx" > .env
source .env
```

### 3. 限制工具权限

修改 `ToolRegistry.java` 限制危险操作：
- 禁止删除文件
- 限制 Shell 命令
- 沙箱化文件访问

### 4. 使用反向代理

生产环境建议使用 Nginx + HTTPS：
```nginx
server {
    listen 443 ssl;
    server_name nanobot.example.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://localhost:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 更新日志

### v1.0.0 (2026-02-11)
- ✅ 初始版本发布
- ✅ AI 对话界面
- ✅ 会话管理
- ✅ 系统监控
- ✅ 工具列表
- ✅ REST API
- ✅ 响应式设计

### 未来计划
- [ ] 深色主题
- [ ] 文件上传
- [ ] Markdown 渲染
- [ ] 代码高亮
- [ ] 语音输入
- [ ] 多用户支持
- [ ] 持久化存储

## 反馈与贡献

- GitHub Issues: https://github.com/chenlei-gh/nanobot-java/issues
- 贡献指南: CONTRIBUTING.md
- 许可证: Apache License 2.0

---

**享受使用 Nanobot Web 界面！** 🚀
