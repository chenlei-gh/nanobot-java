package com.nanobot.core;

import com.nanobot.llm.LlmProvider;
import com.nanobot.tool.ToolRegistry;
import com.nanobot.tool.ToolExecutor;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Agent Loop - Core Agent Processing Engine
 * Handles LLM calls, tool invocations, and conversation flow
 */
public class AgentLoop {
    private final MessageBus messageBus;
    private final LlmProvider llmProvider;
    private final ToolRegistry toolRegistry;
    private final ContextManager contextManager;
    private final String workspacePath;
    private final String model;
    private final int maxIterations;
    private volatile boolean running = false;
    private ExecutorService virtualThreadPool;

    public AgentLoop(
            MessageBus messageBus,
            LlmProvider llmProvider,
            ToolRegistry toolRegistry,
            ContextManager contextManager,
            String workspacePath,
            String model,
            int maxIterations
    ) {
        this.messageBus = messageBus;
        this.llmProvider = llmProvider;
        this.toolRegistry = toolRegistry;
        this.contextManager = contextManager;
        this.workspacePath = workspacePath;
        this.model = model;
        this.maxIterations = maxIterations;
        this.virtualThreadPool = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void start() {
        this.running = true;
        // Subscribe to agent channel
        messageBus.subscribe("agent", this::processMessage);
        messageBus.subscribe("direct", this::processDirect);
    }

    public void stop() {
        this.running = false;
        virtualThreadPool.shutdown();
    }

    /**
     * Process message from message bus
     */
    private void processMessage(MessageBus.Message message) {
        if (!running) return;

        String sessionKey = message.getChatId();
        String userMessage = message.getContent();

        try {
            String response = process(sessionKey, userMessage);
            messageBus.publishOutbound(
                message.getChannel(),
                "nanobot",
                sessionKey,
                response
            );
        } catch (Exception e) {
            messageBus.publishOutbound(
                message.getChannel(),
                "nanobot",
                sessionKey,
                "Error: " + e.getMessage()
            );
        }
    }

    /**
     * Process direct message (CLI mode)
     */
    private void processDirect(MessageBus.Message message) {
        processMessage(message);
    }

    /**
     * Main agent processing loop
     */
    public String process(String sessionKey, String userMessage) {
        // Add user message to context
        contextManager.addMessage(sessionKey, "user", userMessage);

        // Build conversation history
        List<Map<String, String>> messages = contextManager.getMessages(sessionKey);

        int iteration = 0;
        String assistantResponse = null;

        while (iteration < maxIterations) {
            iteration++;

            try {
                // Call LLM
                LlmProvider.LlmResponse response = llmProvider.complete(
                    model,
                    messages,
                    getSystemPrompt()
                );

                assistantResponse = response.content();

                // Check if assistant wants to use tools
                if (response.toolCalls() != null && !response.toolCalls().isEmpty()) {
                    // Process tool calls
                    List<Map<String, Object>> toolResults = processToolCalls(
                        sessionKey,
                        response.toolCalls()
                    );

                    // Add tool results to conversation
                    contextManager.addMessage(sessionKey, "assistant", assistantResponse);

                    for (Map<String, Object> toolResult : toolResults) {
                        contextManager.addMessage(
                            sessionKey,
                            "tool",
                            String.valueOf(toolResult.get("result"))
                        );
                    }

                    // Continue loop with updated context
                    messages = contextManager.getMessages(sessionKey);
                } else {
                    // No tools needed, add response and break
                    contextManager.addMessage(sessionKey, "assistant", assistantResponse);
                    break;
                }

            } catch (Exception e) {
                return "Error processing request: " + e.getMessage();
            }
        }

        if (assistantResponse == null) {
            assistantResponse = "Max iterations reached without completion";
        }

        return assistantResponse;
    }

    /**
     * Process tool calls from LLM
     */
    private List<Map<String, Object>> processToolCalls(
        String sessionKey,
        List<LlmProvider.ToolCall> toolCalls
    ) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (LlmProvider.ToolCall toolCall : toolCalls) {
            try {
                Map<String, Object> arguments = toolCall.arguments();
                Object result = toolRegistry.execute(
                    toolCall.name(),
                    arguments,
                    workspacePath,
                    sessionKey
                );

                Map<String, Object> toolResult = new HashMap<>();
                toolResult.put("tool", toolCall.name());
                toolResult.put("result", result);
                toolResult.put("success", true);
                results.add(toolResult);

            } catch (Exception e) {
                Map<String, Object> toolResult = new HashMap<>();
                toolResult.put("tool", toolCall.name());
                toolResult.put("error", e.getMessage());
                toolResult.put("success", false);
                results.add(toolResult);
            }
        }

        return results;
    }

    /**
     * Get system prompt
     */
    private String getSystemPrompt() {
        return """
            You are Nanobot, a helpful AI assistant.
            You have access to various tools to help answer user questions.
            Use tools when appropriate, but explain your thinking clearly.
            Be concise and direct in your responses.
            """;
    }

    /**
     * Process message directly (for CLI use)
     */
    public CompletableFuture<String> processAsync(String sessionKey, String message) {
        return CompletableFuture.supplyAsync(
            () -> process(sessionKey, message),
            virtualThreadPool
        );
    }
}
