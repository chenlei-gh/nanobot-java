package com.nanobot.monitor;

import com.nanobot.core.MessageBus;
import com.nanobot.core.ContextManager;
import com.nanobot.bus.EventBus;
import com.nanobot.agent.SubagentManager;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Lightweight monitoring server using Java built-in HTTP server
 * No external dependencies required
 */
public class MonitorServer {
    private final HttpServer server;
    private final MessageBus messageBus;
    private final ContextManager contextManager;
    private final EventBus eventBus;
    private final SubagentManager subagentManager;
    private final int port;

    public MonitorServer(int port, MessageBus messageBus, ContextManager contextManager,
                         EventBus eventBus, SubagentManager subagentManager) throws IOException {
        this.port = port;
        this.messageBus = messageBus;
        this.contextManager = contextManager;
        this.eventBus = eventBus;
        this.subagentManager = subagentManager;

        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        setupEndpoints();
    }

    private void setupEndpoints() {
        server.createContext("/", this::handleRoot);
        server.createContext("/api/stats", this::handleStats);
        server.createContext("/api/health", this::handleHealth);
    }

    public void start() {
        server.start();
        System.out.println("📊 监控服务已启动: http://localhost:" + port);
    }

    public void stop() {
        server.stop(0);
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        String html = generateDashboard();
        sendResponse(exchange, 200, html, "text/html");
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        String json = generateStatsJson();
        sendResponse(exchange, 200, json, "application/json");
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        String json = "{\"status\":\"healthy\",\"timestamp\":" + System.currentTimeMillis() + "}";
        sendResponse(exchange, 200, json, "application/json");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String generateStatsJson() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        Map<String, Object> messageBusStats = messageBus != null ? messageBus.getStats() : Map.of();
        Map<String, Object> eventBusStats = eventBus != null ? eventBus.getStats() : Map.of();
        Map<String, Object> subagentStats = subagentManager != null ? subagentManager.getStats() : Map.of();

        return String.format("""
            {
              "memory": {
                "used": %d,
                "total": %d,
                "max": %d,
                "free": %d,
                "usedMB": %.2f,
                "totalMB": %.2f,
                "maxMB": %.2f,
                "usagePercent": %.2f
              },
              "messageBus": %s,
              "eventBus": %s,
              "subagents": %s,
              "sessions": %d,
              "timestamp": %d
            }
            """,
            usedMemory, totalMemory, maxMemory, freeMemory,
            usedMemory / 1024.0 / 1024.0,
            totalMemory / 1024.0 / 1024.0,
            maxMemory / 1024.0 / 1024.0,
            (usedMemory * 100.0) / maxMemory,
            toJson(messageBusStats),
            toJson(eventBusStats),
            toJson(subagentStats),
            contextManager != null ? contextManager.getSessionKeys().size() : 0,
            System.currentTimeMillis()
        );
    }

    private String toJson(Map<String, Object> map) {
        if (map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapValue = (Map<String, Object>) value;
                sb.append(toJson(mapValue));
            } else {
                sb.append(value);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String generateDashboard() {
        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nanobot 监控面板</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #333;
            padding: 20px;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
        }
        .header {
            text-align: center;
            color: white;
            margin-bottom: 30px;
        }
        .header h1 {
            font-size: 2.5em;
            margin-bottom: 10px;
        }
        .header p {
            font-size: 1.1em;
            opacity: 0.9;
        }
        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 20px;
            margin-bottom: 20px;
        }
        .card {
            background: white;
            border-radius: 12px;
            padding: 20px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
        .card h2 {
            font-size: 1.3em;
            margin-bottom: 15px;
            color: #667eea;
            display: flex;
            align-items: center;
        }
        .card h2::before {
            content: '📊';
            margin-right: 10px;
        }
        .stat {
            display: flex;
            justify-content: space-between;
            padding: 10px 0;
            border-bottom: 1px solid #eee;
        }
        .stat:last-child {
            border-bottom: none;
        }
        .stat-label {
            color: #666;
        }
        .stat-value {
            font-weight: bold;
            color: #333;
        }
        .progress-bar {
            width: 100%;
            height: 30px;
            background: #f0f0f0;
            border-radius: 15px;
            overflow: hidden;
            margin: 10px 0;
        }
        .progress-fill {
            height: 100%;
            background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
            transition: width 0.3s ease;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-weight: bold;
        }
        .status {
            display: inline-block;
            padding: 5px 15px;
            border-radius: 20px;
            font-size: 0.9em;
            font-weight: bold;
        }
        .status.healthy {
            background: #10b981;
            color: white;
        }
        .refresh-info {
            text-align: center;
            color: white;
            margin-top: 20px;
            opacity: 0.8;
        }
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
        }
        .updating {
            animation: pulse 1s infinite;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🤖 Nanobot 监控面板</h1>
            <p>实时监控系统运行状态和内存使用情况</p>
        </div>

        <div class="grid">
            <div class="card">
                <h2>内存使用</h2>
                <div class="stat">
                    <span class="stat-label">已使用</span>
                    <span class="stat-value" id="usedMemory">-</span>
                </div>
                <div class="progress-bar">
                    <div class="progress-fill" id="memoryProgress" style="width: 0%">0%</div>
                </div>
                <div class="stat">
                    <span class="stat-label">总内存</span>
                    <span class="stat-value" id="totalMemory">-</span>
                </div>
                <div class="stat">
                    <span class="stat-label">最大内存</span>
                    <span class="stat-value" id="maxMemory">-</span>
                </div>
            </div>

            <div class="card">
                <h2>系统状态</h2>
                <div class="stat">
                    <span class="stat-label">运行状态</span>
                    <span class="status healthy">运行中</span>
                </div>
                <div class="stat">
                    <span class="stat-label">活跃会话</span>
                    <span class="stat-value" id="sessions">-</span>
                </div>
                <div class="stat">
                    <span class="stat-label">消息队列</span>
                    <span class="stat-value" id="queueSize">-</span>
                </div>
                <div class="stat">
                    <span class="stat-label">子代理</span>
                    <span class="stat-value" id="subagents">-</span>
                </div>
            </div>

            <div class="card">
                <h2>消息总线</h2>
                <div class="stat">
                    <span class="stat-label">入站队列</span>
                    <span class="stat-value" id="inboundQueue">-</span>
                </div>
                <div class="stat">
                    <span class="stat-label">出站队列</span>
                    <span class="stat-value" id="outboundQueue">-</span>
                </div>
                <div class="stat">
                    <span class="stat-label">活跃通道</span>
                    <span class="stat-value" id="activeChannels">-</span>
                </div>
                <div class="stat">
                    <span class="stat-label">总消息数</span>
                    <span class="stat-value" id="totalMessages">-</span>
                </div>
            </div>

            <div class="card">
                <h2>事件总线</h2>
                <div class="stat">
                    <span class="stat-label">事件日志</span>
                    <span class="stat-value" id="eventLog">-</span>
                </div>
                <div class="stat">
                    <span class="stat-label">事件类型</span>
                    <span class="stat-value" id="eventTypes">-</span>
                </div>
                <div class="stat">
                    <span class="stat-label">处理器数量</span>
                    <span class="stat-value" id="handlers">-</span>
                </div>
            </div>
        </div>

        <div class="refresh-info">
            <p>⏱️ 每 2 秒自动刷新 | 最后更新: <span id="lastUpdate">-</span></p>
        </div>
    </div>

    <script>
        function updateStats() {
            fetch('/api/stats')
                .then(response => response.json())
                .then(data => {
                    // Memory
                    document.getElementById('usedMemory').textContent = data.memory.usedMB.toFixed(2) + ' MB';
                    document.getElementById('totalMemory').textContent = data.memory.totalMB.toFixed(2) + ' MB';
                    document.getElementById('maxMemory').textContent = data.memory.maxMB.toFixed(2) + ' MB';

                    const usagePercent = data.memory.usagePercent.toFixed(1);
                    const progressBar = document.getElementById('memoryProgress');
                    progressBar.style.width = usagePercent + '%';
                    progressBar.textContent = usagePercent + '%';

                    // System
                    document.getElementById('sessions').textContent = data.sessions;
                    document.getElementById('queueSize').textContent =
                        (data.messageBus.inboundQueueSize || 0) + (data.messageBus.outboundQueueSize || 0);
                    document.getElementById('subagents').textContent = data.subagents.totalSubagents || 0;

                    // Message Bus
                    document.getElementById('inboundQueue').textContent = data.messageBus.inboundQueueSize || 0;
                    document.getElementById('outboundQueue').textContent = data.messageBus.outboundQueueSize || 0;
                    document.getElementById('activeChannels').textContent = data.messageBus.activeChannels || 0;
                    document.getElementById('totalMessages').textContent = data.messageBus.totalMessages || 0;

                    // Event Bus
                    document.getElementById('eventLog').textContent = data.eventBus.logSize || 0;
                    document.getElementById('eventTypes').textContent = data.eventBus.totalEventTypes || 0;
                    document.getElementById('handlers').textContent = data.eventBus.totalHandlers || 0;

                    // Last update
                    document.getElementById('lastUpdate').textContent = new Date().toLocaleTimeString('zh-CN');
                })
                .catch(error => console.error('Error fetching stats:', error));
        }

        // Initial update
        updateStats();

        // Auto refresh every 2 seconds
        setInterval(updateStats, 2000);
    </script>
</body>
</html>
            """;
    }
}
