package com.nanobot.bus;

import java.util.*;
import java.io.*;

/**
 * Event Types - All event types for Nanobot event system
 */
public class NanobotEvent {
    public enum EventType {
        // Message events
        MESSAGE_RECEIVED,
        MESSAGE_SENT,
        MESSAGE_ERROR,

        // Agent events
        AGENT_STARTED,
        AGENT_STOPPED,
        AGENT_THINKING,
        AGENT_RESPONSE,

        // Tool events
        TOOL_CALLED,
        TOOL_STARTED,
        TOOL_COMPLETED,
        TOOL_FAILED,

        // Lifecycle events
        BOT_STARTED,
        BOT_STOPPED,
        BOT_READY,

        // Session events
        SESSION_CREATED,
        SESSION_CLOSED,
        SESSION_TIMEOUT,

        // Error events
        ERROR_OCCURRED,
        RATE_LIMITED,

        // Custom events
        CUSTOM
    }

    public static class Event {
        private final String eventId;
        private final EventType eventType;
        private final String source;
        private final Map<String, Object> data;
        private final long timestamp;
        private final String sessionId;

        public Event(EventType eventType, String source, Map<String, Object> data) {
            this.eventId = UUID.randomUUID().toString();
            this.eventType = eventType;
            this.source = source;
            this.data = data != null ? new HashMap<>(data) : new HashMap<>();
            this.timestamp = System.currentTimeMillis();
            this.sessionId = (String) data.getOrDefault("sessionId", "");
        }

        public String getEventId() { return eventId; }
        public EventType getEventType() { return eventType; }
        public String getSource() { return source; }
        public Map<String, Object> getData() { return data; }
        public long getTimestamp() { return timestamp; }
        public String getSessionId() { return sessionId; }

        @SuppressWarnings("unchecked")
        public <T> T getData(String key) {
            return (T) data.get(key);
        }

        public void setData(String key, Object value) {
            data.put(key, value);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("eventId", eventId);
            map.put("eventType", eventType.name());
            map.put("source", source);
            map.put("data", data);
            map.put("timestamp", timestamp);
            map.put("sessionId", sessionId);
            return map;
        }
    }

    // Convenience factory methods
    public static Event messageReceived(String channel, String chatId, String senderId, String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("channel", channel);
        data.put("chatId", chatId);
        data.put("senderId", senderId);
        data.put("content", content);
        return new Event(EventType.MESSAGE_RECEIVED, channel, data);
    }

    public static Event messageSent(String channel, String chatId, String content) {
        Map<String, Object> data = new HashMap<>();
        data.put("channel", channel);
        data.put("chatId", chatId);
        data.put("content", content);
        return new Event(EventType.MESSAGE_SENT, channel, data);
    }

    public static Event agentThinking(String sessionId, String thought) {
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("thought", thought);
        return new Event(EventType.AGENT_THINKING, "agent", data);
    }

    public static Event agentResponse(String sessionId, String response) {
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("response", response);
        return new Event(EventType.AGENT_RESPONSE, "agent", data);
    }

    public static Event toolCalled(String toolName, Map<String, Object> arguments) {
        Map<String, Object> data = new HashMap<>();
        data.put("toolName", toolName);
        data.put("arguments", arguments);
        return new Event(EventType.TOOL_CALLED, "tool", data);
    }

    public static Event toolCompleted(String toolName, Object result) {
        Map<String, Object> data = new HashMap<>();
        data.put("toolName", toolName);
        data.put("result", result);
        return new Event(EventType.TOOL_COMPLETED, "tool", data);
    }

    public static Event toolFailed(String toolName, String error) {
        Map<String, Object> data = new HashMap<>();
        data.put("toolName", toolName);
        data.put("error", error);
        return new Event(EventType.TOOL_FAILED, "tool", data);
    }

    public static Event errorOccurred(String source, String error, Throwable exception) {
        Map<String, Object> data = new HashMap<>();
        data.put("error", error);
        data.put("exception", exception != null ? exception.toString() : null);
        data.put("stackTrace", exception != null ? getStackTrace(exception) : null);
        return new Event(EventType.ERROR_OCCURRED, source, data);
    }

    private static String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
