package com.nanobot.web;

import com.nanobot.core.*;
import com.nanobot.llm.LlmProvider;
import com.nanobot.tool.ToolRegistry;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Comprehensive Web Server for Nanobot
 * Provides chat interface, configuration, and monitoring
 */
public class WebServer {
    private final HttpServer server;
    private final MessageBus messageBus;
    private final ContextManager contextManager;
    private final AgentLoop agentLoop;
    private final ToolRegistry toolRegistry;
    private final int port;

    // SSE connections for streaming
    private final Map<String, HttpExchange> sseConnections = new ConcurrentHashMap<>();

    public WebServer(
        int port,
        MessageBus messageBus,
        ContextManager contextManager,
        AgentLoop agentLoop,
        ToolRegistry toolRegistry
    ) throws IOException {
        this.port = port;
        this.messageBus = messageBus;
        this.contextManager = contextManager;
        this.agentLoop = agentLoop;
        this.toolRegistry = toolRegistry;

        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        setupRoutes();
    }

    private void setupRoutes() {
        // UI Routes
        server.createContext("/", this::handleIndex);

        // API Routes
        server.createContext("/api/chat", this::handleChat);
        server.createContext("/api/sessions", this::handleSessions);
        server.createContext("/api/config", this::handleConfig);
        server.createContext("/api/tools", this::handleTools);
        server.createContext("/api/stats", this::handleStats);
        server.createContext("/api/health", this::handleHealth);

        // SSE for streaming
        server.createContext("/api/stream", this::handleStream);
    }

    public void start() {
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("✅ Web服务器已启动");
        System.out.println("📊 访问: http://localhost:" + port);
        System.out.println("🔗 Codespaces URL: 查看 PORTS 标签页获取公开 URL");
    }

    public void stop() {
        server.stop(0);
    }

    // ==================== HTTP Handlers ====================

    private void handleIndex(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        String html = generateIndexHtml();
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        sendResponse(exchange, 200, html);
    }

    private void handleChat(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> request = parseJson(body);

            String sessionId = (String) request.getOrDefault("sessionId", "default");
            String message = (String) request.get("message");

            if (message == null || message.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, Map.of("error", "Message is required"));
                return;
            }

            // Process message asynchronously
            CompletableFuture<String> future = agentLoop.processAsync(sessionId, message);
            String response = future.get(60, TimeUnit.SECONDS);

            Map<String, Object> result = new HashMap<>();
            result.put("response", response);
            result.put("sessionId", sessionId);
            result.put("timestamp", System.currentTimeMillis());

            sendJsonResponse(exchange, 200, result);

        } catch (Exception e) {
            sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleSessions(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equals(method)) {
            // List all sessions
            Set<String> sessionKeys = contextManager.getSessionKeys();
            List<Map<String, Object>> sessions = new ArrayList<>();

            for (String key : sessionKeys) {
                Map<String, Object> sessionInfo = contextManager.getSessionInfo(key);
                sessionInfo.put("sessionId", key);
                sessions.add(sessionInfo);
            }

            sendJsonResponse(exchange, 200, Map.of("sessions", sessions));

        } else if ("DELETE".equals(method)) {
            // Clear a session
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.startsWith("sessionId=")) {
                String sessionId = query.substring(10);
                contextManager.clearSession(sessionId);
                sendJsonResponse(exchange, 200, Map.of("message", "Session cleared"));
            } else {
                sendJsonResponse(exchange, 400, Map.of("error", "sessionId required"));
            }

        } else {
            sendResponse(exchange, 405, "Method Not Allowed");
        }
    }

    private void handleConfig(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            Map<String, Object> config = new HashMap<>();
            config.put("models", List.of(
                "gpt-4", "gpt-4-turbo", "gpt-3.5-turbo",
                "claude-3-5-sonnet-20241022", "claude-3-opus-20240229",
                "deepseek-chat", "qwen-max", "gemini-pro"
            ));
            config.put("currentModel", "gpt-4");
            config.put("temperature", 0.7);
            config.put("maxIterations", 20);

            sendJsonResponse(exchange, 200, config);
        } else {
            sendResponse(exchange, 405, "Method Not Allowed");
        }
    }

    private void handleTools(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            List<Map<String, Object>> tools = new ArrayList<>();
            for (ToolRegistry.ToolDescriptor tool : toolRegistry.getAllTools()) {
                Map<String, Object> toolInfo = new HashMap<>();
                toolInfo.put("name", tool.getName());
                toolInfo.put("description", tool.getDescription());
                tools.add(toolInfo);
            }
            sendJsonResponse(exchange, 200, Map.of("tools", tools));
        } else {
            sendResponse(exchange, 405, "Method Not Allowed");
        }
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        Map<String, Object> stats = new HashMap<>();
        stats.put("memory", Map.of(
            "used", usedMemory / (1024 * 1024),
            "total", totalMemory / (1024 * 1024),
            "max", maxMemory / (1024 * 1024),
            "free", freeMemory / (1024 * 1024)
        ));
        stats.put("sessions", contextManager.getSessionKeys().size());
        stats.put("timestamp", System.currentTimeMillis());

        sendJsonResponse(exchange, 200, stats);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        sendJsonResponse(exchange, 200, Map.of("status", "ok", "timestamp", System.currentTimeMillis()));
    }

    private void handleStream(HttpExchange exchange) throws IOException {
        // SSE endpoint for real-time updates
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.sendResponseHeaders(200, 0);

        String sessionId = UUID.randomUUID().toString();
        sseConnections.put(sessionId, exchange);

        // Keep connection alive
        try {
            while (sseConnections.containsKey(sessionId)) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            sseConnections.remove(sessionId);
        }
    }

    // ==================== Helper Methods ====================

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Map<String, Object> data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        String json = toJson(data);
        sendResponse(exchange, statusCode, json);
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(toJsonValue(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    private String toJsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "\"" + escapeJson((String) value) + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map) return toJson((Map<String, Object>) value);
        if (value instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJsonValue(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escapeJson(value.toString()) + "\"";
    }

    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private Map<String, Object> parseJson(String json) {
        // Simple JSON parser for basic objects
        Map<String, Object> result = new HashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("^\"|\"$", "");
                String value = kv[1].trim().replaceAll("^\"|\"$", "");
                result.put(key, value);
            }
        }
        return result;
    }

    private String generateIndexHtml() {
        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nanobot - AI 助手</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }

        .container {
            width: 100%;
            max-width: 1400px;
            display: grid;
            grid-template-columns: 300px 1fr;
            gap: 20px;
            height: 90vh;
        }

        .sidebar {
            background: rgba(255, 255, 255, 0.95);
            border-radius: 20px;
            padding: 25px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            display: flex;
            flex-direction: column;
            gap: 20px;
        }

        .main-panel {
            background: rgba(255, 255, 255, 0.95);
            border-radius: 20px;
            padding: 30px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            display: flex;
            flex-direction: column;
        }

        .logo {
            text-align: center;
            padding: 20px 0;
            border-bottom: 2px solid #f0f0f0;
        }

        .logo h1 {
            font-size: 28px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            margin-bottom: 5px;
        }

        .logo p {
            color: #666;
            font-size: 14px;
        }

        .section {
            background: #f8f9fa;
            border-radius: 12px;
            padding: 15px;
        }

        .section-title {
            font-size: 14px;
            font-weight: 600;
            color: #333;
            margin-bottom: 12px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .stat-item {
            display: flex;
            justify-content: space-between;
            padding: 8px 0;
            border-bottom: 1px solid #e0e0e0;
        }

        .stat-item:last-child {
            border-bottom: none;
        }

        .stat-label {
            color: #666;
            font-size: 13px;
        }

        .stat-value {
            color: #333;
            font-weight: 600;
            font-size: 13px;
        }

        .btn {
            padding: 10px 20px;
            border: none;
            border-radius: 8px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s;
        }

        .btn-primary {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
        }

        .btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
        }

        .btn-secondary {
            background: #f0f0f0;
            color: #333;
        }

        .btn-secondary:hover {
            background: #e0e0e0;
        }

        .chat-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
            padding-bottom: 15px;
            border-bottom: 2px solid #f0f0f0;
        }

        .chat-header h2 {
            font-size: 24px;
            color: #333;
        }

        .chat-messages {
            flex: 1;
            overflow-y: auto;
            padding: 20px;
            background: #f8f9fa;
            border-radius: 12px;
            margin-bottom: 20px;
        }

        .message {
            margin-bottom: 20px;
            animation: fadeIn 0.3s;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .message-user {
            display: flex;
            justify-content: flex-end;
        }

        .message-assistant {
            display: flex;
            justify-content: flex-start;
        }

        .message-content {
            max-width: 70%;
            padding: 15px 20px;
            border-radius: 18px;
            line-height: 1.6;
            word-wrap: break-word;
        }

        .message-user .message-content {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
        }

        .message-assistant .message-content {
            background: white;
            color: #333;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        }

        .chat-input-area {
            display: flex;
            gap: 10px;
        }

        .chat-input {
            flex: 1;
            padding: 15px 20px;
            border: 2px solid #e0e0e0;
            border-radius: 12px;
            font-size: 15px;
            font-family: inherit;
            transition: border-color 0.3s;
        }

        .chat-input:focus {
            outline: none;
            border-color: #667eea;
        }

        .send-btn {
            padding: 15px 30px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 12px;
            font-size: 15px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s;
        }

        .send-btn:hover:not(:disabled) {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
        }

        .send-btn:disabled {
            opacity: 0.5;
            cursor: not-allowed;
        }

        .loading {
            display: inline-block;
            width: 20px;
            height: 20px;
            border: 3px solid rgba(255, 255, 255, 0.3);
            border-radius: 50%;
            border-top-color: white;
            animation: spin 1s linear infinite;
        }

        @keyframes spin {
            to { transform: rotate(360deg); }
        }

        .session-list {
            max-height: 200px;
            overflow-y: auto;
        }

        .session-item {
            padding: 10px;
            margin-bottom: 8px;
            background: white;
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.3s;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .session-item:hover {
            background: #f0f0f0;
            transform: translateX(5px);
        }

        .session-item.active {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
        }

        .delete-session {
            background: #ff4444;
            color: white;
            border: none;
            border-radius: 4px;
            padding: 4px 8px;
            font-size: 12px;
            cursor: pointer;
        }

        @media (max-width: 1024px) {
            .container {
                grid-template-columns: 1fr;
                height: auto;
            }

            .sidebar {
                order: 2;
            }

            .main-panel {
                order: 1;
                height: 70vh;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <!-- Sidebar -->
        <div class="sidebar">
            <div class="logo">
                <h1>🤖 Nanobot</h1>
                <p>智能 AI 助手</p>
            </div>

            <!-- Stats -->
            <div class="section">
                <div class="section-title">系统状态</div>
                <div class="stat-item">
                    <span class="stat-label">内存使用</span>
                    <span class="stat-value" id="memory-usage">-</span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">活跃会话</span>
                    <span class="stat-value" id="session-count">-</span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">状态</span>
                    <span class="stat-value" style="color: #4caf50;">运行中</span>
                </div>
            </div>

            <!-- Sessions -->
            <div class="section">
                <div class="section-title">会话管理</div>
                <button class="btn btn-primary" style="width: 100%; margin-bottom: 10px;" onclick="createNewSession()">
                    ➕ 新建会话
                </button>
                <div class="session-list" id="session-list">
                    <div class="session-item active" data-session="default">
                        <span>默认会话</span>
                    </div>
                </div>
            </div>

            <!-- Tools -->
            <div class="section">
                <div class="section-title">可用工具</div>
                <div id="tools-list" style="font-size: 12px; color: #666;">
                    加载中...
                </div>
            </div>
        </div>

        <!-- Main Chat Panel -->
        <div class="main-panel">
            <div class="chat-header">
                <h2>💬 对话</h2>
                <button class="btn btn-secondary" onclick="clearChat()">清空对话</button>
            </div>

            <div class="chat-messages" id="chat-messages">
                <div class="message message-assistant">
                    <div class="message-content">
                        你好！我是 Nanobot，一个智能 AI 助手。我可以帮你完成文件操作、代码编写、网页搜索等各种任务。有什么我可以帮你的吗？
                    </div>
                </div>
            </div>

            <div class="chat-input-area">
                <input
                    type="text"
                    class="chat-input"
                    id="chat-input"
                    placeholder="输入你的问题..."
                    onkeypress="handleKeyPress(event)"
                />
                <button class="send-btn" id="send-btn" onclick="sendMessage()">
                    发送
                </button>
            </div>
        </div>
    </div>

    <script>
        let currentSession = 'default';
        let isProcessing = false;

        // Initialize
        document.addEventListener('DOMContentLoaded', () => {
            loadStats();
            loadTools();
            setInterval(loadStats, 3000);
        });

        function handleKeyPress(event) {
            if (event.key === 'Enter' && !isProcessing) {
                sendMessage();
            }
        }

        async function sendMessage() {
            if (isProcessing) return;

            const input = document.getElementById('chat-input');
            const message = input.value.trim();

            if (!message) return;

            // Add user message to chat
            addMessage('user', message);
            input.value = '';

            // Disable input
            isProcessing = true;
            updateSendButton(true);

            try {
                const response = await fetch('/api/chat', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        sessionId: currentSession,
                        message: message
                    })
                });

                const data = await response.json();

                if (data.error) {
                    addMessage('assistant', '错误: ' + data.error);
                } else {
                    addMessage('assistant', data.response);
                }
            } catch (error) {
                addMessage('assistant', '错误: ' + error.message);
            } finally {
                isProcessing = false;
                updateSendButton(false);
            }
        }

        function addMessage(role, content) {
            const messagesDiv = document.getElementById('chat-messages');
            const messageDiv = document.createElement('div');
            messageDiv.className = `message message-${role}`;

            const contentDiv = document.createElement('div');
            contentDiv.className = 'message-content';
            contentDiv.textContent = content;

            messageDiv.appendChild(contentDiv);
            messagesDiv.appendChild(messageDiv);
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        }

        function updateSendButton(loading) {
            const btn = document.getElementById('send-btn');
            if (loading) {
                btn.innerHTML = '<div class="loading"></div>';
                btn.disabled = true;
            } else {
                btn.innerHTML = '发送';
                btn.disabled = false;
            }
        }

        async function loadStats() {
            try {
                const response = await fetch('/api/stats');
                const data = await response.json();

                document.getElementById('memory-usage').textContent =
                    data.memory.used + ' MB';
                document.getElementById('session-count').textContent =
                    data.sessions;
            } catch (error) {
                console.error('Failed to load stats:', error);
            }
        }

        async function loadTools() {
            try {
                const response = await fetch('/api/tools');
                const data = await response.json();

                const toolsList = document.getElementById('tools-list');
                if (data.tools && data.tools.length > 0) {
                    toolsList.innerHTML = data.tools.map(tool =>
                        `<div style="padding: 5px 0;">• ${tool.name}</div>`
                    ).join('');
                } else {
                    toolsList.textContent = '无可用工具';
                }
            } catch (error) {
                document.getElementById('tools-list').textContent = '加载失败';
            }
        }

        function clearChat() {
            const messagesDiv = document.getElementById('chat-messages');
            messagesDiv.innerHTML = '';
            addMessage('assistant', '对话已清空。有什么我可以帮你的吗？');
        }

        function createNewSession() {
            const sessionId = 'session-' + Date.now();
            currentSession = sessionId;

            const sessionList = document.getElementById('session-list');
            const sessionItem = document.createElement('div');
            sessionItem.className = 'session-item active';
            sessionItem.dataset.session = sessionId;
            sessionItem.innerHTML = `
                <span>${sessionId}</span>
                <button class="delete-session" onclick="deleteSession('${sessionId}', event)">删除</button>
            `;
            sessionItem.onclick = (e) => {
                if (e.target.classList.contains('delete-session')) return;
                switchSession(sessionId);
            };

            // Remove active from others
            document.querySelectorAll('.session-item').forEach(item => {
                item.classList.remove('active');
            });

            sessionList.appendChild(sessionItem);
            clearChat();
        }

        function switchSession(sessionId) {
            currentSession = sessionId;
            document.querySelectorAll('.session-item').forEach(item => {
                item.classList.remove('active');
                if (item.dataset.session === sessionId) {
                    item.classList.add('active');
                }
            });
            clearChat();
        }

        async function deleteSession(sessionId, event) {
            event.stopPropagation();

            try {
                await fetch(`/api/sessions?sessionId=${sessionId}`, {
                    method: 'DELETE'
                });

                const item = document.querySelector(`[data-session="${sessionId}"]`);
                if (item) item.remove();

                if (currentSession === sessionId) {
                    currentSession = 'default';
                    switchSession('default');
                }
            } catch (error) {
                alert('删除会话失败: ' + error.message);
            }
        }
    </script>
</body>
</html>
        """;
    }
}
