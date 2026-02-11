package com.nanobot.monitor;

import com.nanobot.core.MessageBus;
import com.nanobot.core.ContextManager;

/**
 * Standalone monitoring server for testing
 */
public class MonitorServerTest {
    public static void main(String[] args) {
        try {
            System.out.println("🚀 启动监控服务器测试...");

            // Create minimal components
            MessageBus messageBus = new MessageBus();
            ContextManager contextManager = new ContextManager();

            messageBus.start();

            // Start monitor server
            int port = Integer.parseInt(System.getenv().getOrDefault("MONITOR_PORT", "8080"));
            MonitorServer server = new MonitorServer(port, messageBus, contextManager, null, null);
            server.start();

            System.out.println("✅ 监控服务器已启动");
            System.out.println("📊 访问: http://localhost:" + port);
            System.out.println("🔗 Codespaces URL: 查看 PORTS 标签页获取公开 URL");
            System.out.println("");
            System.out.println("按 Ctrl+C 停止服务器...");

            // Keep running
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("❌ 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
