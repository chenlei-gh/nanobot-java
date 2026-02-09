package com.nanobot.llm;

import java.util.*;
import java.util.concurrent.*;

/**
 * Token Counter - Accurate token estimation and counting
 * Supports multiple encoding models (GPT-4, Claude, etc.)
 */
public class TokenCounter {
    private static final Map<String, Tokenizer> TOKENIZERS = new ConcurrentHashMap<>();
    private static final int AVG_CHARS_PER_TOKEN = 4;

    static {
        // Register default tokenizers
        TOKENIZERS.put("claude", new ClaudeTokenizer());
        TOKENIZERS.put("gpt-4", new Gpt4Tokenizer());
        TOKENIZERS.put("gpt-3.5", new Gpt35Tokenizer());
        TOKENIZERS.put("default", new DefaultTokenizer());
    }

    public interface Tokenizer {
        int countTokens(String text);
        int countTokens(List<String> texts);
    }

    // Claude tokenizer (approximate)
    static class ClaudeTokenizer implements Tokenizer {
        private static final int CLAUDE_CHARS_PER_TOKEN = 3;

        @Override
        public int countTokens(String text) {
            if (text == null || text.isEmpty()) return 0;
            return text.length() / CLAUDE_CHARS_PER_TOKEN + countSpecialTokens(text);
        }

        @Override
        public int countTokens(List<String> texts) {
            return texts.stream().mapToInt(this::countTokens).sum();
        }
    }

    // GPT-4 tokenizer (approximate)
    static class Gpt4Tokenizer implements Tokenizer {
        private static final int GPT4_CHARS_PER_TOKEN = 4;

        @Override
        public int countTokens(String text) {
            if (text == null || text.isEmpty()) return 0;
            return text.length() / GPT4_CHARS_PER_TOKEN + countSpecialTokens(text);
        }

        @Override
        public int countTokens(List<String> texts) {
            return texts.stream().mapToInt(this::countTokens).sum();
        }
    }

    // GPT-3.5 tokenizer (approximate)
    static class Gpt35Tokenizer implements Tokenizer {
        private static final int GPT35_CHARS_PER_TOKEN = 4;

        @Override
        public int countTokens(String text) {
            if (text == null || text.isEmpty()) return 0;
            return text.length() / GPT35_CHARS_PER_TOKEN + countSpecialTokens(text);
        }

        @Override
        public int countTokens(List<String> texts) {
            return texts.stream().mapToInt(this::countTokens).sum();
        }
    }

    // Default tokenizer
    static class DefaultTokenizer implements Tokenizer {
        @Override
        public int countTokens(String text) {
            if (text == null || text.isEmpty()) return 0;
            return text.length() / AVG_CHARS_PER_TOKEN;
        }

        @Override
        public int countTokens(List<String> texts) {
            return texts.stream().mapToInt(this::countTokens).sum();
        }
    }

    /**
     * Count tokens for a single text
     */
    public static int countTokens(String text) {
        return countTokens(text, "default");
    }

    public static int countTokens(String text, String model) {
        String key = getModelKey(model);
        Tokenizer tokenizer = TOKENIZERS.getOrDefault(key, TOKENIZERS.get("default"));
        return tokenizer.countTokens(text);
    }

    /**
     * Count tokens for multiple texts
     */
    public static int countTokens(List<String> texts) {
        return countTokens(texts, "default");
    }

    public static int countTokens(List<String> texts, String model) {
        String key = getModelKey(model);
        Tokenizer tokenizer = TOKENIZERS.getOrDefault(key, TOKENIZERS.get("default"));
        return tokenizer.countTokens(texts);
    }

    /**
     * Count tokens for messages (including role markers)
     */
    public static int countMessageTokens(List<Map<String, String>> messages, String model) {
        int total = 0;

        for (Map<String, String> message : messages) {
            String role = message.getOrDefault("role", "user");
            String content = message.getOrDefault("content", "");

            // Role token overhead
            total += countRoleTokens(role, model);

            // Content tokens
            total += countTokens(content, model);
        }

        return total;
    }

    /**
     * Estimate tokens for system prompt + messages
     */
    public static int estimateTotalTokens(
            String systemPrompt,
            List<Map<String, String>> messages,
            String model
    ) {
        int total = 0;

        // System prompt
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            total += countTokens(systemPrompt, model) + 5; // Overhead
        }

        // Messages
        total += countMessageTokens(messages, model);

        // Additional overhead per message
        total += messages.size() * 3;

        return total;
    }

    /**
     * Estimate max tokens that can fit in context
     */
    public static int estimateMaxTokens(
            int contextLimit,
            String systemPrompt,
            List<Map<String, String>> messages,
            String model
    ) {
        int usedTokens = estimateTotalTokens(systemPrompt, messages, model);
        return Math.max(0, contextLimit - usedTokens - 100); // Reserve 100 tokens buffer
    }

    /**
     * Register a custom tokenizer
     */
    public static void registerTokenizer(String name, Tokenizer tokenizer) {
        TOKENIZERS.put(name.toLowerCase(), tokenizer);
    }

    /**
     * Get model key for tokenizer lookup
     */
    private static String getModelKey(String model) {
        if (model == null) return "default";

        String lower = model.toLowerCase();

        if (lower.contains("claude")) return "claude";
        if (lower.contains("gpt-4")) return "gpt-4";
        if (lower.contains("gpt-3.5")) return "gpt-3.5";
        if (lower.contains("gemini")) return "gpt-4"; // Use GPT-4 as approximation
        if (lower.contains("deepseek")) return "gpt-4";
        if (lower.contains("qwen")) return "gpt-4";

        return "default";
    }

    /**
     * Count special tokens (approximate)
     */
    private static int countSpecialTokens(String text) {
        int count = 0;
        // Count common special patterns
        if (text.contains("<|")) count += 2;
        if (text.contains("|")) count += text.length() / 100;
        if (text.contains("{")) count += text.length() / 50;
        if (text.contains("}")) count += text.length() / 50;
        return count;
    }

    /**
     * Count role token overhead
     */
    private static int countRoleTokens(String role, String model) {
        return switch (role.toLowerCase()) {
            case "system" -> 5;
            case "user" -> 3;
            case "assistant", "model" -> 3;
            case "tool", "function" -> 5;
            default -> 3;
        };
    }

    /**
     * Calculate approximate cost
     */
    public static double calculateCost(int inputTokens, int outputTokens,
                                        double inputCostPer1K, double outputCostPer1K) {
        double inputCost = (inputTokens / 1000.0) * inputCostPer1K;
        double outputCost = (outputTokens / 1000.0) * outputCostPer1K;
        return inputCost + outputCost;
    }

    /**
     * Estimate cost for common models
     */
    public static double estimateCost(String model, int inputTokens, int outputTokens) {
        return switch (model.toLowerCase()) {
            case "gpt-4" -> calculateCost(inputTokens, outputTokens, 0.03, 0.06);
            case "gpt-4-turbo" -> calculateCost(inputTokens, outputTokens, 0.01, 0.03);
            case "gpt-4o" -> calculateCost(inputTokens, outputTokens, 0.005, 0.015);
            case "gpt-3.5-turbo" -> calculateCost(inputTokens, outputTokens, 0.0005, 0.0015);
            case "claude-3-5-sonnet" -> calculateCost(inputTokens, outputTokens, 0.003, 0.015);
            case "claude-3-opus" -> calculateCost(inputTokens, outputTokens, 0.015, 0.075);
            case "deepseek-chat" -> calculateCost(inputTokens, outputTokens, 0.00014, 0.00028);
            default -> 0.0;
        };
    }
}
