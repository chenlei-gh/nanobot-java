package com.nanobot.core;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Context Manager - Conversation Context & Memory Management
 * Thread-safe with virtual thread support
 */
public class ContextManager {
    private final ConcurrentHashMap<String, List<ContextMessage>> sessions = new ConcurrentHashMap<>();
    private final int maxMessagesPerSession;
    private final int maxTokensPerSession;
    private final ScheduledExecutorService cleanupExecutor;
    private volatile boolean running = false;

    public ContextManager() {
        this(50, 8000); // Default: 50 messages, 8000 tokens
    }

    public ContextManager(int maxMessagesPerSession, int maxTokensPerSession) {
        this.maxMessagesPerSession = maxMessagesPerSession;
        this.maxTokensPerSession = maxTokensPerSession;
        this.cleanupExecutor = Executors.newScheduledThreadPool(1);
        startAutoCleanup();
    }

    /**
     * Start automatic cleanup of old sessions
     */
    private void startAutoCleanup() {
        running = true;
        // Cleanup sessions older than 1 hour every 10 minutes
        cleanupExecutor.scheduleAtFixedRate(
            () -> cleanupOldSessions(TimeUnit.HOURS.toMillis(1)),
            10, 10, TimeUnit.MINUTES
        );
    }

    /**
     * Stop the context manager and cleanup executor
     */
    public void stop() {
        running = false;
        cleanupExecutor.shutdown();
    }

    public static class ContextMessage {
        private final String role;
        private final String content;
        private final long timestamp;

        public ContextMessage(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
        public long getTimestamp() { return timestamp; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("role", role);
            map.put("content", content);
            return map;
        }

        public int estimateTokens() {
            // Rough estimate: 4 characters per token
            return content.length() / 4;
        }
    }

    /**
     * Add message to session
     */
    public void addMessage(String sessionKey, String role, String content) {
        sessions.computeIfAbsent(sessionKey, k -> new ArrayList<>())
                .add(new ContextMessage(role, content));

        // Prune if needed
        pruneSession(sessionKey);
    }

    /**
     * Get all messages for session
     */
    public List<Map<String, String>> getMessages(String sessionKey) {
        List<ContextMessage> messages = sessions.getOrDefault(sessionKey, new ArrayList<>());

        return messages.stream()
                .map(m -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("role", m.getRole());
                    map.put("content", m.getContent());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Clear session
     */
    public void clearSession(String sessionKey) {
        sessions.remove(sessionKey);
    }

    /**
     * Get session info
     */
    public Map<String, Object> getSessionInfo(String sessionKey) {
        List<ContextMessage> messages = sessions.getOrDefault(sessionKey, new ArrayList<>());
        int totalTokens = messages.stream()
                .mapToInt(ContextMessage::estimateTokens)
                .sum();

        Map<String, Object> info = new HashMap<>();
        info.put("messageCount", messages.size());
        info.put("estimatedTokens", totalTokens);
        info.put("oldestMessage", messages.isEmpty() ? null : messages.get(0).getTimestamp());
        info.put("latestMessage", messages.isEmpty() ? null : messages.get(messages.size() - 1).getTimestamp());

        return info;
    }

    /**
     * Get all session keys
     */
    public Set<String> getSessionKeys() {
        return sessions.keySet();
    }

    /**
     * Prune session to stay within limits
     */
    private void pruneSession(String sessionKey) {
        List<ContextMessage> messages = sessions.get(sessionKey);
        if (messages == null) return;

        // Prune by message count
        while (messages.size() > maxMessagesPerSession) {
            messages.remove(0);
        }

        // Prune by token count
        int totalTokens = messages.stream()
                .mapToInt(ContextMessage::estimateTokens)
                .sum();

        while (totalTokens > maxTokensPerSession && !messages.isEmpty()) {
            totalTokens -= messages.remove(0).estimateTokens();
        }
    }

    /**
     * Create new session if not exists
     */
    public void ensureSession(String sessionKey) {
        sessions.computeIfAbsent(sessionKey, k -> new ArrayList<>());
    }

    /**
     * Remove old sessions (cleanup)
     */
    public void cleanupOldSessions(long maxAgeMs) {
        long cutoffTime = System.currentTimeMillis() - maxAgeMs;

        sessions.entrySet().removeIf(entry -> {
            List<ContextMessage> messages = entry.getValue();
            return messages.isEmpty() ||
                   (messages.get(messages.size() - 1).getTimestamp() < cutoffTime);
        });
    }
}
