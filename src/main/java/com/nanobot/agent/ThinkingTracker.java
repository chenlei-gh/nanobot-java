package com.nanobot.agent;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Thinking Tracker - Reasoning/Thinking process tracking
 * Captures and manages agent's reasoning steps for transparency
 */
public class ThinkingTracker {
    private final Map<String, ThoughtProcess> activeThoughts = new ConcurrentHashMap<>();
    private final List<ThoughtListener> listeners = new CopyOnWriteArrayList<>();
    private final int maxStepsPerThought;
    private int thoughtCounter = 0;

    public interface ThoughtListener {
        void onThoughtStarted(String thoughtId, String prompt);
        void onThoughtStep(String thoughtId, int step, String content);
        void onThoughtCompleted(String thoughtId, String summary);
        void onThoughtFailed(String thoughtId, String error);
    }

    public static class ThoughtStep {
        private final int stepNumber;
        private final String content;
        private final long timestamp;
        private final Map<String, Object> metadata;

        public ThoughtStep(int stepNumber, String content) {
            this.stepNumber = stepNumber;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
            this.metadata = new HashMap<>();
        }

        public int getStepNumber() { return stepNumber; }
        public String getContent() { return content; }
        public long getTimestamp() { return timestamp; }
        public Map<String, Object> getMetadata() { return metadata; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("step", stepNumber);
            map.put("content", content);
            map.put("timestamp", timestamp);
            map.put("metadata", metadata);
            return map;
        }
    }

    public static class ThoughtProcess {
        private final String id;
        private final String prompt;
        private final String model;
        private volatile ThoughtStatus status;
        private final List<ThoughtStep> steps = new ArrayList<>();
        private volatile String summary;
        private volatile String error;
        private final long createdAt;
        private volatile long completedAt;

        public enum ThoughtStatus {
            ACTIVE, COMPLETED, FAILED, CANCELLED
        }

        public ThoughtProcess(String prompt, String model) {
            this.id = UUID.randomUUID().toString();
            this.prompt = prompt;
            this.model = model;
            this.status = ThoughtStatus.ACTIVE;
            this.createdAt = System.currentTimeMillis();
        }

        public String getId() { return id; }
        public String getPrompt() { return prompt; }
        public String getModel() { return model; }
        public ThoughtStatus getStatus() { return status; }
        public void setStatus(ThoughtStatus status) { this.status = status; }
        public List<ThoughtStep> getSteps() { return steps; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public long getCreatedAt() { return createdAt; }
        public long getCompletedAt() { return completedAt; }
        public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
        public long getDurationMs() {
            long end = completedAt > 0 ? completedAt : System.currentTimeMillis();
            return end - createdAt;
        }

        public void addStep(ThoughtStep step) {
            synchronized (steps) {
                steps.add(step);
            }
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("prompt", prompt);
            map.put("model", model);
            map.put("status", status.name());
            map.put("steps", steps.stream().map(ThoughtStep::toMap).toList());
            map.put("summary", summary);
            map.put("error", error);
            map.put("createdAt", createdAt);
            map.put("completedAt", completedAt);
            map.put("durationMs", getDurationMs());
            return map;
        }
    }

    public ThinkingTracker() {
        this(50);
    }

    public ThinkingTracker(int maxStepsPerThought) {
        this.maxStepsPerThought = maxStepsPerThought;
    }

    /**
     * Start a new thought process
     */
    public String startThinking(String prompt) {
        return startThinking(prompt, null);
    }

    public String startThinking(String prompt, String model) {
        String thoughtId = "thought_" + (++thoughtCounter);
        ThoughtProcess thought = new ThoughtProcess(prompt, model);

        activeThoughts.put(thoughtId, thought);

        // Notify listeners
        notifyThoughtStarted(thoughtId, prompt);

        return thoughtId;
    }

    /**
     * Add a reasoning step
     */
    public void addStep(String thoughtId, String content) {
        ThoughtProcess thought = activeThoughts.get(thoughtId);
        if (thought == null) {
            throw new IllegalArgumentException("Unknown thought: " + thoughtId);
        }

        if (thought.getStatus() != ThoughtProcess.ThoughtStatus.ACTIVE) {
            throw new IllegalStateException("Thought is not active");
        }

        if (thought.getSteps().size() >= maxStepsPerThought) {
            throw new IllegalStateException("Max steps exceeded");
        }

        ThoughtStep step = new ThoughtStep(thought.getSteps().size() + 1, content);
        thought.addStep(step);

        // Notify listeners
        notifyThoughtStep(thoughtId, step.getStepNumber(), content);
    }

    /**
     * Add a step with metadata
     */
    public void addStep(String thoughtId, String content, Map<String, Object> metadata) {
        ThoughtProcess thought = activeThoughts.get(thoughtId);
        if (thought == null) {
            throw new IllegalArgumentException("Unknown thought: " + thoughtId);
        }

        ThoughtStep step = new ThoughtStep(thought.getSteps().size() + 1, content);
        step.getMetadata().putAll(metadata);
        thought.addStep(step);

        notifyThoughtStep(thoughtId, step.getStepNumber(), content);
    }

    /**
     * Complete thought with summary
     */
    public void completeThinking(String thoughtId, String summary) {
        ThoughtProcess thought = activeThoughts.get(thoughtId);
        if (thought == null) {
            throw new IllegalArgumentException("Unknown thought: " + thoughtId);
        }

        thought.setSummary(summary);
        thought.setStatus(ThoughtProcess.ThoughtStatus.COMPLETED);
        thought.setCompletedAt(System.currentTimeMillis());

        notifyThoughtCompleted(thoughtId, summary);
    }

    /**
     * Mark thought as failed
     */
    public void failThinking(String thoughtId, String error) {
        ThoughtProcess thought = activeThoughts.get(thoughtId);
        if (thought == null) {
            throw new IllegalArgumentException("Unknown thought: " + thoughtId);
        }

        thought.setError(error);
        thought.setStatus(ThoughtProcess.ThoughtStatus.FAILED);
        thought.setCompletedAt(System.currentTimeMillis());

        notifyThoughtFailed(thoughtId, error);
    }

    /**
     * Cancel thought
     */
    public void cancelThinking(String thoughtId) {
        ThoughtProcess thought = activeThoughts.get(thoughtId);
        if (thought == null) {
            throw new IllegalArgumentException("Unknown thought: " + thoughtId);
        }

        thought.setStatus(ThoughtProcess.ThoughtStatus.CANCELLED);
        thought.setCompletedAt(System.currentTimeMillis());
    }

    /**
     * Get thought process
     */
    public ThoughtProcess getThought(String thoughtId) {
        return activeThoughts.get(thoughtId);
    }

    /**
     * Format thinking for LLM context
     */
    public String formatThinkingForContext(String thoughtId) {
        ThoughtProcess thought = activeThoughts.get(thoughtId);
        if (thought == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<thinking>\n");

        for (ThoughtStep step : thought.getSteps()) {
            sb.append(String.format("[Step %d] %s\n", step.getStepNumber(), step.getContent()));
        }

        if (thought.getSummary() != null) {
            sb.append(String.format("Conclusion: %s\n", thought.getSummary()));
        }

        sb.append("</thinking>");

        return sb.toString();
    }

    /**
     * Get formatted thinking trace
     */
    public String formatThinkingTrace(String thoughtId) {
        ThoughtProcess thought = activeThoughts.get(thoughtId);
        if (thought == null) {
            return "Unknown thought: " + thoughtId;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Thought %s (%s)\n", thoughtId, thought.getModel()));
        sb.append(String.format("Status: %s | Duration: %dms\n\n", thought.getStatus(), thought.getDurationMs()));

        for (ThoughtStep step : thought.getSteps()) {
            sb.append(String.format("%d. %s\n", step.getStepNumber(), step.getContent()));
        }

        if (thought.getSummary() != null) {
            sb.append(String.format("\n→ %s\n", thought.getSummary()));
        }

        if (thought.getError() != null) {
            sb.append(String.format("\n✗ Error: %s\n", thought.getError()));
        }

        return sb.toString();
    }

    /**
     * Add listener
     */
    public void addListener(ThoughtListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener
     */
    public void removeListener(ThoughtListener listener) {
        listeners.remove(listener);
    }

    private void notifyThoughtStarted(String thoughtId, String prompt) {
        for (ThoughtListener listener : listeners) {
            try {
                listener.onThoughtStarted(thoughtId, prompt);
            } catch (Exception e) {
                // Log but don't crash
            }
        }
    }

    private void notifyThoughtStep(String thoughtId, int step, String content) {
        for (ThoughtListener listener : listeners) {
            try {
                listener.onThoughtStep(thoughtId, step, content);
            } catch (Exception e) {
                // Log but don't crash
            }
        }
    }

    private void notifyThoughtCompleted(String thoughtId, String summary) {
        for (ThoughtListener listener : listeners) {
            try {
                listener.onThoughtCompleted(thoughtId, summary);
            } catch (Exception e) {
                // Log but don't crash
            }
        }
    }

    private void notifyThoughtFailed(String thoughtId, String error) {
        for (ThoughtListener listener : listeners) {
            try {
                listener.onThoughtFailed(thoughtId, error);
            } catch (Exception e) {
                // Log but don't crash
            }
        }
    }

    /**
     * Get statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeThoughts", activeThoughts.size());

        long completed = activeThoughts.values().stream()
            .filter(t -> t.getStatus() == ThoughtProcess.ThoughtStatus.COMPLETED)
            .count();
        long failed = activeThoughts.values().stream()
            .filter(t -> t.getStatus() == ThoughtProcess.ThoughtStatus.FAILED)
            .count();

        stats.put("completed", completed);
        stats.put("failed", failed);

        return stats;
    }
}
