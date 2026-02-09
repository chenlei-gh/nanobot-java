package com.nanobot.llm;

import java.util.*;

/**
 * LLM Provider Interface
 */
public interface LlmProvider {
    record LlmResponse(
        String content,
        List<ToolCall> toolCalls,
        int usageTokens
    ) {}

    record ToolCall(
        String id,
        String name,
        Map<String, Object> arguments
    ) {}

    /**
     * Complete a conversation
     */
    LlmResponse complete(String model, List<Map<String, String>> messages, String systemPrompt);

    /**
     * Complete with tools enabled
     */
    LlmResponse completeWithTools(String model, List<Map<String, String>> messages,
                                   String systemPrompt, List<Map<String, Object>> tools);

    /**
     * Check if model is supported
     */
    boolean supportsModel(String model);

    /**
     * Get provider name
     */
    String getName();
}
