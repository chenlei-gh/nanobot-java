package com.nanobot.web;

import com.nanobot.core.*;
import com.nanobot.llm.*;
import com.nanobot.tool.ToolRegistry;

/**
 * Standalone web server for testing
 */
public class WebServerTest {
    public static void main(String[] args) {
        try {
            System.out.println("🚀 启动 Nanobot Web 服务器...");
            System.out.println("");

            // Create components
            MessageBus messageBus = new MessageBus();
            ContextManager contextManager = new ContextManager();
            ToolRegistry toolRegistry = new ToolRegistry();

            // Initialize LLM provider
            LlmProvider llmProvider = createLlmProvider();
            if (llmProvider == null) {
                System.err.println("❌ 错误: 未找到有效的 API 密钥");
                System.err.println("请设置以下环境变量之一:");
                System.err.println("  - OPENAI_API_KEY");
                System.err.println("  - ANTHROPIC_API_KEY");
                System.exit(1);
            }

            // Create agent loop
            AgentLoop agentLoop = new AgentLoop(
                messageBus,
                llmProvider,
                toolRegistry,
                contextManager,
                System.getProperty("user.home") + "/.nanobot/workspace",
                "gpt-4",
                20
            );

            // Start components
            messageBus.start();
            agentLoop.start();

            // Start web server
            int port = Integer.parseInt(System.getenv().getOrDefault("WEB_PORT", "9090"));
            WebServer server = new WebServer(port, messageBus, contextManager, agentLoop, toolRegistry);
            server.start();

            System.out.println("");
            System.out.println("✅ Web 服务器已启动");
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

    private static LlmProvider createLlmProvider() {
        String openaiKey = System.getenv("OPENAI_API_KEY");
        String anthropicKey = System.getenv("ANTHROPIC_API_KEY");

        if (openaiKey != null && !openaiKey.isEmpty()) {
            System.out.println("✓ 使用 OpenAI Provider");
            return new OpenAiProvider(openaiKey);
        } else if (anthropicKey != null && !anthropicKey.isEmpty()) {
            System.out.println("✓ 使用 Anthropic Provider");
            return new AnthropicProvider(anthropicKey);
        }

        return null;
    }
}
