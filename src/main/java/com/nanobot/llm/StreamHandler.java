package com.nanobot.llm;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Stream Handler - LLM streaming response processing
 * Handles SSE (Server-Sent Events) and chunked responses
 */
public class StreamHandler {
    private final BlockingQueue<StreamChunk> chunkQueue = new LinkedBlockingQueue<>();
    private final AtomicReference<StreamStatus> status = new AtomicReference<>(StreamStatus.IDLE);
    private final List<Consumer<StreamChunk>> chunkListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> tokenListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<StreamEvent>> eventListeners = new CopyOnWriteArrayList<>();
    private final StringBuilder currentContent = new StringBuilder();
    private ExecutorService executor;

    public enum StreamStatus {
        IDLE, CONNECTED, STREAMING, COMPLETED, ERROR, CANCELLED
    }

    public static class StreamChunk {
        private final String chunkId;
        private final String content;
        private final boolean isComplete;
        private final boolean isError;
        private final String errorMessage;
        private final Map<String, Object> metadata;
        private final long timestamp;

        public StreamChunk(String content, boolean isComplete) {
            this.chunkId = UUID.randomUUID().toString();
            this.content = content;
            this.isComplete = isComplete;
            this.isError = false;
            this.errorMessage = null;
            this.metadata = new HashMap<>();
            this.timestamp = System.currentTimeMillis();
        }

        public StreamChunk(String errorMessage) {
            this.chunkId = UUID.randomUUID().toString();
            this.content = "";
            this.isComplete = true;
            this.isError = true;
            this.errorMessage = errorMessage;
            this.metadata = new HashMap<>();
            this.timestamp = System.currentTimeMillis();
        }

        public String getChunkId() { return chunkId; }
        public String getContent() { return content; }
        public boolean isComplete() { return isComplete; }
        public boolean isError() { return isError; }
        public String getErrorMessage() { return errorMessage; }
        public Map<String, Object> getMetadata() { return metadata; }
        public long getTimestamp() { return timestamp; }
    }

    public static class StreamEvent {
        private final String eventType;
        private final Map<String, Object> data;
        private final long timestamp;

        public StreamEvent(String eventType, Map<String, Object> data) {
            this.eventType = eventType;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public String getEventType() { return eventType; }
        public Map<String, Object> getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }

    public StreamHandler() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Start streaming
     */
    public void start() {
        status.set(StreamStatus.CONNECTED);
        currentContent.setLength(0);
        chunkQueue.clear();
    }

    /**
     * Stop streaming
     */
    public void stop() {
        status.set(StreamStatus.CANCELLED);
        chunkQueue.clear();
    }

    /**
     * Add chunk to stream
     */
    public void addChunk(String content) {
        if (status.get() != StreamStatus.STREAMING) {
            status.set(StreamStatus.STREAMING);
        }

        StreamChunk chunk = new StreamChunk(content, false);
        chunkQueue.offer(chunk);
        currentContent.append(content);

        // Notify listeners
        for (Consumer<StreamChunk> listener : chunkListeners) {
            listener.accept(chunk);
        }

        for (Consumer<String> listener : tokenListeners) {
            listener.accept(content);
        }
    }

    /**
     * Complete the stream
     */
    public void complete() {
        StreamChunk chunk = new StreamChunk("", true);
        chunkQueue.offer(chunk);
        status.set(StreamStatus.COMPLETED);

        for (Consumer<StreamChunk> listener : chunkListeners) {
            listener.accept(chunk);
        }
    }

    /**
     * Signal error in stream
     */
    public void error(String errorMessage) {
        StreamChunk chunk = new StreamChunk(errorMessage);
        chunkQueue.offer(chunk);
        status.set(StreamStatus.ERROR);

        for (Consumer<StreamChunk> listener : chunkListeners) {
            listener.accept(chunk);
        }
    }

    /**
     * Add stream event
     */
    public void addEvent(String eventType, Map<String, Object> data) {
        StreamEvent event = new StreamEvent(eventType, data);

        for (Consumer<StreamEvent> listener : eventListeners) {
            listener.accept(event);
        }
    }

    /**
     * Get accumulated content
     */
    public String getAccumulatedContent() {
        return currentContent.toString();
    }

    /**
     * Get current status
     */
    public StreamStatus getStatus() {
        return status.get();
    }

    /**
     * Get next chunk (blocking)
     */
    public StreamChunk getNextChunk() throws InterruptedException {
        return chunkQueue.take();
    }

    /**
     * Get next chunk with timeout
     */
    public StreamChunk getNextChunk(long timeoutMs) throws InterruptedException {
        StreamChunk chunk = chunkQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (chunk == null) {
            throw new InterruptedException("Timeout waiting for chunk");
        }
        return chunk;
    }

    /**
     * Consume stream until complete
     */
    public String consumeStream() throws InterruptedException {
        StringBuilder sb = new StringBuilder();
        start();

        while (true) {
            StreamChunk chunk = getNextChunk();

            if (chunk.isError()) {
                throw new RuntimeException("Stream error: " + chunk.getErrorMessage());
            }

            sb.append(chunk.getContent());

            if (chunk.isComplete()) {
                break;
            }
        }

        return sb.toString();
    }

    /**
     * Add chunk listener
     */
    public void addChunkListener(Consumer<StreamChunk> listener) {
        chunkListeners.add(listener);
    }

    /**
     * Add token listener
     */
    public void addTokenListener(Consumer<String> listener) {
        tokenListeners.add(listener);
    }

    /**
     * Add event listener
     */
    public void addEventListener(Consumer<StreamEvent> listener) {
        eventListeners.add(listener);
    }

    /**
     * Remove listener
     */
    public void removeChunkListener(Consumer<StreamChunk> listener) {
        chunkListeners.remove(listener);
    }

    public void removeTokenListener(Consumer<String> listener) {
        tokenListeners.remove(listener);
    }

    public void removeEventListener(Consumer<StreamEvent> listener) {
        eventListeners.remove(listener);
    }

    /**
     * Parse SSE (Server-Sent Events) format
     */
    public static List<StreamChunk> parseSSE(String sseData) {
        List<StreamChunk> chunks = new ArrayList<>();

        String[] lines = sseData.split("\n");
        StringBuilder currentChunk = new StringBuilder();
        boolean inChunk = false;

        for (String line : lines) {
            if (line.startsWith("data:")) {
                String data = line.substring(5).trim();

                if ("[DONE]".equals(data)) {
                    if (currentChunk.length() > 0) {
                        chunks.add(new StreamChunk(currentChunk.toString(), false));
                        currentChunk.setLength(0);
                    }
                    chunks.add(new StreamChunk("", true));
                    break;
                }

                if (!data.isEmpty()) {
                    try {
                        currentChunk.append(parseJsonChunk(data));
                    } catch (Exception e) {
                        currentChunk.append(data);
                    }
                }

                inChunk = true;
            } else if (line.isEmpty() && inChunk) {
                if (currentChunk.length() > 0) {
                    chunks.add(new StreamChunk(currentChunk.toString(), false));
                    currentChunk.setLength(0);
                }
                inChunk = false;
            }
        }

        return chunks;
    }

    private static String parseJsonChunk(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();

            Map<String, Object> data = mapper.readValue(json, Map.class);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) data.get("choices");
            if (choices == null || choices.isEmpty()) {
                return "";
            }

            Map<String, Object> choice = choices.get(0);
            Map<String, Object> delta = (Map<String, Object>) choice.get("delta");

            if (delta == null) {
                return "";
            }

            Object contentObj = delta.get("content");
            if (contentObj == null) {
                return "";
            }

            return contentObj.toString();

        } catch (Exception e) {
            return json;
        }
    }

    /**
     * Format content as SSE
     */
    public static String formatSSE(String event, String data) {
        StringBuilder sb = new StringBuilder();
        sb.append("event: ").append(event).append("\n");
        sb.append("data: ").append(data).append("\n\n");
        return sb.toString();
    }

    /**
     * Get stream statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("status", status.get());
        stats.put("accumulatedLength", currentContent.length());
        stats.put("queuedChunks", chunkQueue.size());
        stats.put("chunkListeners", chunkListeners.size());
        stats.put("tokenListeners", tokenListeners.size());
        stats.put("eventListeners", eventListeners.size());
        return stats;
    }
}
