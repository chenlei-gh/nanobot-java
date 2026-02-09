package com.nanobot.agent;

import java.util.*;
import java.util.concurrent.*;

/**
 * Subagent Manager - Background subagent execution
 * Manages lightweight agent instances for background task processing
 */
public class SubagentManager {
    private final Map<String, Subagent> activeSubagents = new ConcurrentHashMap<>();
    private final SubagentExecutor executor;
    private final String workspace;
    private int subagentCounter = 0;

    public interface SubagentExecutor {
        String execute(String task, String systemPrompt, String model);
    }

    public static class Subagent {
        private final String id;
        private final String task;
        private final String systemPrompt;
        private final String model;
        private volatile SubagentStatus status;
        private final long createdAt;
        private volatile long completedAt;
        private String result;
        private volatile String error;

        public enum SubagentStatus {
            PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
        }

        public Subagent(String task, String systemPrompt, String model) {
            this.id = UUID.randomUUID().toString();
            this.task = task;
            this.systemPrompt = systemPrompt;
            this.model = model;
            this.status = SubagentStatus.PENDING;
            this.createdAt = System.currentTimeMillis();
        }

        // Getters
        public String getId() { return id; }
        public String getTask() { return task; }
        public String getSystemPrompt() { return systemPrompt; }
        public String getModel() { return model; }
        public SubagentStatus getStatus() { return status; }
        public void setStatus(SubagentStatus status) { this.status = status; }
        public long getCreatedAt() { return createdAt; }
        public long getCompletedAt() { return completedAt; }
        public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public long getDurationMs() {
            long endTime = completedAt > 0 ? completedAt : System.currentTimeMillis();
            return endTime - createdAt;
        }
    }

    public SubagentManager(SubagentExecutor executor, String workspace) {
        this.executor = executor;
        this.workspace = workspace;
    }

    /**
     * Create and start a new subagent
     */
    public String createSubagent(String task, String systemPrompt, String model) {
        return createSubagent(task, systemPrompt, model, "none");
    }

    public String createSubagent(String task, String systemPrompt, String model, String isolationLevel) {
        String subagentId = "sub_" + (++subagentCounter);
        Subagent subagent = new Subagent(task, systemPrompt, model);

        activeSubagents.put(subagentId, subagent);

        // Execute subagent in virtual thread
        CompletableFuture.runAsync(() -> {
            try {
                subagent.setStatus(Subagent.SubagentStatus.RUNNING);

                String result = executor.execute(task, systemPrompt, model);

                subagent.setResult(result);
                subagent.setStatus(Subagent.SubagentStatus.COMPLETED);
                subagent.setCompletedAt(System.currentTimeMillis());

            } catch (Exception e) {
                subagent.setError(e.getMessage());
                subagent.setStatus(Subagent.SubagentStatus.FAILED);
                subagent.setCompletedAt(System.currentTimeMillis());
            }
        });

        return subagentId;
    }

    /**
     * Get subagent status
     */
    public Subagent getSubagent(String subagentId) {
        return activeSubagents.get(subagentId);
    }

    /**
     * Cancel a running subagent
     */
    public boolean cancelSubagent(String subagentId) {
        Subagent subagent = activeSubagents.get(subagentId);
        if (subagent != null && subagent.getStatus() == Subagent.SubagentStatus.RUNNING) {
            subagent.setStatus(Subagent.SubagentStatus.CANCELLED);
            subagent.setCompletedAt(System.currentTimeMillis());
            return true;
        }
        return false;
    }

    /**
     * Get result from completed subagent
     */
    public String getSubagentResult(String subagentId) {
        Subagent subagent = activeSubagents.get(subagentId);
        if (subagent == null) {
            throw new IllegalArgumentException("Unknown subagent: " + subagentId);
        }

        if (subagent.getStatus() == Subagent.SubagentStatus.FAILED) {
            throw new RuntimeException("Subagent failed: " + subagent.getError());
        }

        if (subagent.getStatus() == Subagent.SubagentStatus.RUNNING) {
            throw new IllegalStateException("Subagent still running");
        }

        return subagent.getResult();
    }

    /**
     * Wait for subagent to complete
     */
    public String waitForSubagent(String subagentId, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        Subagent subagent;

        while ((subagent = activeSubagents.get(subagentId)) != null) {
            Subagent.SubagentStatus status = subagent.getStatus();

            if (status == Subagent.SubagentStatus.COMPLETED) {
                return subagent.getResult();
            }
            if (status == Subagent.SubagentStatus.FAILED) {
                throw new RuntimeException("Subagent failed: " + subagent.getError());
            }
            if (status == Subagent.SubagentStatus.CANCELLED) {
                throw new IllegalStateException("Subagent was cancelled");
            }

            if (System.currentTimeMillis() - start > timeoutMs) {
                throw new IllegalStateException("Subagent timeout");
            }

            Thread.sleep(100);
        }

        throw new IllegalArgumentException("Unknown subagent: " + subagentId);
    }

    /**
     * Get all active subagents
     */
    public Map<String, Subagent> getActiveSubagents() {
        return new HashMap<>(activeSubagents);
    }

    /**
     * Get subagent statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSubagents", activeSubagents.size());

        long completed = activeSubagents.values().stream()
            .filter(s -> s.getStatus() == Subagent.SubagentStatus.COMPLETED)
            .count();
        long failed = activeSubagents.values().stream()
            .filter(s -> s.getStatus() == Subagent.SubagentStatus.FAILED)
            .count();
        long running = activeSubagents.values().stream()
            .filter(s -> s.getStatus() == Subagent.SubagentStatus.RUNNING)
            .count();

        stats.put("completed", completed);
        stats.put("failed", failed);
        stats.put("running", running);

        return stats;
    }

    /**
     * Cleanup old subagents
     */
    public void cleanup(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        activeSubagents.entrySet().removeIf(entry -> {
            Subagent subagent = entry.getValue();
            return subagent.getCompletedAt() > 0 && subagent.getCompletedAt() < cutoff;
        });
    }
}
