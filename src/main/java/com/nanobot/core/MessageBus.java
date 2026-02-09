package com.nanobot.core;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Message Bus - Thread-safe publish/subscribe pattern
 * Optimized for high-concurrency with virtual threads
 */
public class MessageBus {
    private final ConcurrentHashMap<String, Set<MessageHandler>> subscriptions = new ConcurrentHashMap<>();
    private final BlockingQueue<Message> inboundQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> outboundQueue = new LinkedBlockingQueue<>();
    private final AtomicLong messageCounter = new AtomicLong(0);
    private volatile boolean running = false;
    private ExecutorService virtualThreadPool;

    @FunctionalInterface
    public interface MessageHandler {
        void handle(Message message);
    }

    public static class Message {
        private final String id;
        private final String channel;
        private final String senderId;
        private final String chatId;
        private final String content;
        private final MessageType type;
        private final Map<String, Object> metadata;
        private final long timestamp;

        public Message(String channel, String senderId, String chatId, String content, MessageType type) {
            this.id = UUID.randomUUID().toString();
            this.channel = channel;
            this.senderId = senderId;
            this.chatId = chatId;
            this.content = content;
            this.type = type;
            this.metadata = new ConcurrentHashMap<>();
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public String getId() { return id; }
        public String getChannel() { return channel; }
        public String getSenderId() { return senderId; }
        public String getChatId() { return chatId; }
        public String getContent() { return content; }
        public MessageType getType() { return type; }
        public Map<String, Object> getMetadata() { return metadata; }
        public long getTimestamp() { return timestamp; }

        public void addMetadata(String key, Object value) { metadata.put(key, value); }
    }

    public enum MessageType {
        INBOUND, OUTBOUND, SYSTEM, COMMAND, RESPONSE
    }

    public MessageBus() {
        // Virtual thread pool for handling messages
        this.virtualThreadPool = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void start() {
        this.running = true;
        // Process messages in virtual threads for high concurrency
        virtualThreadPool.submit(this::processInbound);
        virtualThreadPool.submit(this::processOutbound);
    }

    public void stop() {
        this.running = false;
        virtualThreadPool.shutdown();
    }

    /**
     * Subscribe to a channel
     */
    public void subscribe(String channel, MessageHandler handler) {
        subscriptions.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet())
                    .add(handler);
    }

    /**
     * Unsubscribe from a channel
     */
    public void unsubscribe(String channel, MessageHandler handler) {
        Set<MessageHandler> handlers = subscriptions.get(channel);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    /**
     * Publish a message to inbound queue
     */
    public void publishInbound(String channel, String senderId, String chatId, String content) {
        Message msg = new Message(channel, senderId, chatId, content, MessageType.INBOUND);
        inboundQueue.offer(msg);
    }

    /**
     * Publish directly to outbound queue
     */
    public void publishOutbound(String channel, String senderId, String chatId, String content) {
        Message msg = new Message(channel, senderId, chatId, content, MessageType.OUTBOUND);
        outboundQueue.offer(msg);
    }

    /**
     * Publish message to specific handlers (direct routing)
     */
    public void publish(Message message) {
        String channel = message.getChannel();
        Set<MessageHandler> handlers = subscriptions.get(channel);

        if (handlers != null && !handlers.isEmpty()) {
            // Deliver to all handlers in virtual threads for concurrency
            handlers.forEach(handler ->
                virtualThreadPool.submit(() -> handler.handle(message))
            );
        }
    }

    /**
     * Process inbound messages
     */
    private void processInbound() {
        while (running) {
            try {
                Message msg = inboundQueue.poll(100, TimeUnit.MILLISECONDS);
                if (msg != null) {
                    deliverMessage(msg, "inbound");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Process outbound messages
     */
    private void processOutbound() {
        while (running) {
            try {
                Message msg = outboundQueue.poll(100, TimeUnit.MILLISECONDS);
                if (msg != null) {
                    deliverMessage(msg, "outbound");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void deliverMessage(Message msg, String queueType) {
        Set<MessageHandler> handlers = subscriptions.get(msg.getChannel());
        if (handlers != null && !handlers.isEmpty()) {
            handlers.forEach(handler ->
                virtualThreadPool.submit(() -> handler.handle(msg))
            );
        }
    }

    /**
     * Get statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("inboundQueueSize", inboundQueue.size());
        stats.put("outboundQueueSize", outboundQueue.size());
        stats.put("totalMessages", messageCounter.getAndIncrement());
        stats.put("activeChannels", subscriptions.size());
        return stats;
    }
}
