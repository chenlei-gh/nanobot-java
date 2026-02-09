package com.nanobot.cron;

import java.util.*;
import java.util.concurrent.*;

/**
 * Cron Job Definition
 */
public class CronJob {
    private String id;
    private String name;
    private CronService.CronSchedule schedule;
    private String message;
    private boolean deliver;
    private long createdAt;
    private long lastRunAt;
    private boolean enabled;

    public CronJob() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.enabled = true;
    }

    public CronJob(String name, CronService.CronSchedule schedule, String message, boolean deliver) {
        this();
        this.name = name;
        this.schedule = schedule;
        this.message = message;
        this.deliver = deliver;
    }

    @FunctionalInterface
    public interface CronExecutor {
        ExecutedJob execute(CronJob job) throws Exception;
    }

    public static class ExecutedJob {
        private final String jobId;
        private final String message;
        private final Object result;
        private final long durationMs;

        public ExecutedJob(String jobId, String message, Object result, long durationMs) {
            this.jobId = jobId;
            this.message = message;
            this.result = result;
            this.durationMs = durationMs;
        }

        public String getJobId() { return jobId; }
        public String getMessage() { return message; }
        public Object getResult() { return result; }
        public long getDurationMs() { return durationMs; }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CronService.CronSchedule getSchedule() { return schedule; }
    public String getMessage() { return message; }
    public boolean isDeliver() { return deliver; }
    public long getCreatedAt() { return createdAt; }
    public long getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(long lastRunAt) { this.lastRunAt = lastRunAt; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("message", message);
        map.put("deliver", deliver);
        map.put("createdAt", createdAt);
        map.put("lastRunAt", lastRunAt);
        map.put("enabled", enabled);

        Map<String, Object> scheduleMap = new HashMap<>();
        scheduleMap.put("kind", schedule.kind);
        if (schedule.everyMs > 0) scheduleMap.put("everyMs", schedule.everyMs);
        if (schedule.cron != null) scheduleMap.put("cron", schedule.cron);
        map.put("schedule", scheduleMap);

        return map;
    }

    @SuppressWarnings("unchecked")
    public static CronJob fromMap(Map<String, Object> data) {
        CronJob job = new CronJob();
        job.setId((String) data.get("id"));
        job.setName((String) data.get("name"));
        job.setMessage((String) data.get("message"));
        job.setDeliver((Boolean) data.getOrDefault("deliver", false));
        job.setCreatedAt(((Number) data.getOrDefault("createdAt", System.currentTimeMillis())).longValue());
        job.setLastRunAt(((Number) data.getOrDefault("lastRunAt", 0)).longValue());
        job.setEnabled((Boolean) data.getOrDefault("enabled", true));

        Map<String, Object> scheduleData = (Map<String, Object>) data.get("schedule");
        if (scheduleData != null) {
            job.schedule = new CronService.CronSchedule();
            job.schedule.kind = (String) scheduleData.get("kind");
            job.schedule.everyMs = ((Number) scheduleData.getOrDefault("everyMs", 0L)).longValue();
            job.schedule.cron = (String) scheduleData.get("cron");
        }

        return job;
    }
}
