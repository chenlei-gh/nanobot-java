package com.nanobot.bus;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Event Bus - Pub/Sub event system for Nanobot
 * Supports synchronous and asynchronous event handling
 */
public class EventBus {
    private final ConcurrentHashMap<NanobotEvent.EventType, Set<EventHandler>> handlers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<EventHandler>> taggedHandlers = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<NanobotEvent.Event> eventLog = new ConcurrentLinkedQueue<>();
    private final int maxLogSize;
    private volatile boolean running = false;
    private ExecutorService asyncExecutor;
    private ScheduledExecutorService scheduledExecutor;
    private ScheduledExecutorService cleanupExecutor;

    @FunctionalInterface
    public interface EventHandler {
        void handle(NanobotEvent.Event event);
    }

    public EventBus() {
        this(1000);
    }

    public EventBus(int maxLogSize) {
        this.maxLogSize = maxLogSize;
        this.asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.scheduledExecutor = Executors.newScheduledThreadPool(1);
        this.cleanupExecutor = Executors.newScheduledThreadPool(1);
    }

    /**
     * Start the event bus
     */
    public void start() {
        running = true;
        // Schedule periodic cleanup of old events (every 5 minutes)
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupOldEvents,
            5, 5, TimeUnit.MINUTES
        );
    }

    /**
     * Stop the event bus
     */
    public void stop() {
        running = false;
        asyncExecutor.shutdown();
        scheduledExecutor.shutdown();
        cleanupExecutor.shutdown();
    }

    /**
     * Subscribe to event type
     */
    public void subscribe(NanobotEvent.EventType eventType, EventHandler handler) {
        handlers.computeIfAbsent(eventType, k -> ConcurrentHashMap.newKeySet()).add(handler);
    }

    /**
     * Subscribe to event type with tag
     */
    public void subscribe(NanobotEvent.EventType eventType, String tag, EventHandler handler) {
        subscribe(eventType, handler);
        String tagKey = eventType.name() + ":" + tag;
        taggedHandlers.computeIfAbsent(tagKey, k -> ConcurrentHashMap.newKeySet()).add(handler);
    }

    /**
     * Unsubscribe from event type
     */
    public void unsubscribe(NanobotEvent.EventType eventType, EventHandler handler) {
        Set<EventHandler> handlerSet = handlers.get(eventType);
        if (handlerSet != null) {
            handlerSet.remove(handler);
        }
    }

    /**
     * Unsubscribe by tag
     */
    public void unsubscribe(NanobotEvent.EventType eventType, String tag, EventHandler handler) {
        String tagKey = eventType.name() + ":" + tag;
        Set<EventHandler> handlerSet = taggedHandlers.get(tagKey);
        if (handlerSet != null) {
            handlerSet.remove(handler);
        }
    }

    /**
     * Publish event synchronously
     */
    public void publish(NanobotEvent.Event event) {
        if (!running) return;

        // Log event
        logEvent(event);

        // Get handlers for this event type
        Set<EventHandler> handlerSet = handlers.get(event.getEventType());
        if (handlerSet == null || handlerSet.isEmpty()) {
            return;
        }

        // Execute handlers
        for (EventHandler handler : handlerSet) {
            try {
                handler.handle(event);
            } catch (Exception e) {
                // Log handler error but continue
                System.err.println("Event handler error: " + e.getMessage());
            }
        }
    }

    /**
     * Publish event asynchronously
     */
    public void publishAsync(NanobotEvent.Event event) {
        if (!running) return;

        asyncExecutor.submit(() -> publish(event));
    }

    /**
     * Publish with delay
     */
    public void publishDelayed(NanobotEvent.Event event, long delayMs) {
        if (!running) return;

        scheduledExecutor.schedule(() -> publish(event), delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Convenience methods for common events
     */
    public void publishMessageReceived(String channel, String chatId, String senderId, String content) {
        publish(NanobotEvent.messageReceived(channel, chatId, senderId, content));
    }

    public void publishMessageSent(String channel, String chatId, String content) {
        publish(NanobotEvent.messageSent(channel, chatId, content));
    }

    public void publishAgentThinking(String sessionId, String thought) {
        publish(NanobotEvent.agentThinking(sessionId, thought));
    }

    public void publishAgentResponse(String sessionId, String response) {
        publish(NanobotEvent.agentResponse(sessionId, response));
    }

    public void publishToolCalled(String toolName, Map<String, Object> arguments) {
        publish(NanobotEvent.toolCalled(toolName, arguments));
    }

    public void publishToolCompleted(String toolName, Object result) {
        publish(NanobotEvent.toolCompleted(toolName, result));
    }

    public void publishToolFailed(String toolName, String error) {
        publish(NanobotEvent.toolFailed(toolName, error));
    }

    public void publishError(String source, String error, Throwable exception) {
        publish(NanobotEvent.errorOccurred(source, error, exception));
    }

    /**
     * Request-Reply pattern
     */
    public <T> T request(NanobotEvent.Event request, long timeoutMs) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();

        EventHandler replyHandler = event -> {
            @SuppressWarnings("unchecked")
            T reply = (T) event.getData("reply");
            result.set(reply);
            latch.countDown();
        };

        subscribe(request.getEventType(), "request-" + request.getEventId(), replyHandler);

        publish(request);

        latch.await(timeoutMs, TimeUnit.MILLISECONDS);

        unsubscribe(request.getEventType(), "request-" + request.getEventId(), replyHandler);

        return result.get();
    }

    /**
     * Send reply to request
     */
    public void reply(NanobotEvent.Event request, Object reply) {
        NanobotEvent.Event replyEvent = new NanobotEvent.Event(
            request.getEventType(),
            "system",
            Map.of("reply", reply, "originalEventId", request.getEventId())
        );

        String tagKey = request.getEventType().name() + ":request-" + request.getEventId();
        Set<EventHandler> handlers = taggedHandlers.get(tagKey);

        if (handlers != null) {
            for (EventHandler handler : handlers) {
                try {
                    handler.handle(replyEvent);
                } catch (Exception e) {
                    System.err.println("Reply handler error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Get recent events
     */
    public List<NanobotEvent.Event> getRecentEvents() {
        List<NanobotEvent.Event> events = new ArrayList<>();
        for (NanobotEvent.Event event : eventLog) {
            events.add(event);
        }
        return events;
    }

    /**
     * Get events by type
     */
    public List<NanobotEvent.Event> getEventsByType(NanobotEvent.EventType type) {
        return eventLog.stream()
            .filter(e -> e.getEventType() == type)
            .toList();
    }

    /**
     * Get events by session
     */
    public List<NanobotEvent.Event> getEventsBySession(String sessionId) {
        return eventLog.stream()
            .filter(e -> e.getSessionId().equals(sessionId))
            .toList();
    }

    /**
     * Clear event log
     */
    public void clearLog() {
        eventLog.clear();
    }

    /**
     * Get statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEventTypes", handlers.size());
        stats.put("totalHandlers", handlers.values().stream().mapToInt(Set::size).sum());
        stats.put("logSize", eventLog.size());
        stats.put("running", running);

        Map<String, Integer> eventsByType = new HashMap<>();
        for (NanobotEvent.EventType type : NanobotEvent.EventType.values()) {
            long count = eventLog.stream()
                .filter(e -> e.getEventType() == type)
                .count();
            if (count > 0) {
                eventsByType.put(type.name(), (int) count);
            }
        }
        stats.put("eventsByType", eventsByType);

        return stats;
    }

    private void logEvent(NanobotEvent.Event event) {
        eventLog.offer(event);

        while (eventLog.size() > maxLogSize) {
            eventLog.poll();
        }
    }

    /**
     * Cleanup old events (older than 1 hour)
     */
    private void cleanupOldEvents() {
        long cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        eventLog.removeIf(event -> event.getTimestamp() < cutoffTime);
    }
}
