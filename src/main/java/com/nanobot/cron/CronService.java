package com.nanobot.cron;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Cron Service - Scheduled task execution
 */
public class CronService {
    private final Path storePath;
    private final CronJob.CronExecutor executor;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> jobs = new HashMap<>();
    private volatile boolean running = false;

    public CronService(Path storePath, CronJob.CronExecutor executor) {
        this.storePath = storePath;
        this.executor = executor;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        this.running = true;
        loadJobs();
    }

    public void stop() {
        this.running = false;
        jobs.values().forEach(f -> f.cancel(false));
        scheduler.shutdown();
    }

    public void addJob(String name, CronSchedule schedule, String message, boolean deliver) {
        CronJob job = new CronJob(name, schedule, message, deliver);
        saveJob(job);
        scheduleJob(job);
    }

    public void removeJob(String name) {
        cancelJob(name);
        deleteJob(name);
    }

    public List<CronJob> getJobs() {
        return loadAllJobs();
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobs", jobs.size());
        stats.put("running", running);
        stats.put("storePath", storePath.toString());
        return stats;
    }

    private void scheduleJob(CronJob job) {
        if (!running) return;

        Runnable task = () -> {
            try {
                CronJob.ExecutedJob result = executor.execute(job);
                System.out.println("Cron job executed: " + job.getName());
            } catch (Exception e) {
                System.err.println("Cron job failed: " + job.getName() + " - " + e.getMessage());
            }
        };

        long initialDelay = calculateInitialDelay(job.getSchedule());
        long period = calculatePeriod(job.getSchedule());

        if (period > 0) {
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.MILLISECONDS);
            jobs.put(job.getName(), future);
        }
    }

    private long calculateInitialDelay(CronSchedule schedule) {
        long now = System.currentTimeMillis();

        if ("every".equals(schedule.kind)) {
            return schedule.everyMs;
        }

        if ("cron".equals(schedule.kind)) {
            // Simple cron implementation (minutes only for now)
            String cronExpr = schedule.cron;
            String[] parts = cronExpr.split("\\s+");
            if (parts.length >= 1) {
                int minute = Integer.parseInt(parts[0]);
                long nextMinute = (minute * 60 * 1000L);
                return Math.max(0, nextMinute - now);
            }
        }

        return 0;
    }

    private long calculatePeriod(CronSchedule schedule) {
        if ("every".equals(schedule.kind)) {
            return schedule.everyMs;
        }

        if ("cron".equals(schedule.kind)) {
            String cronExpr = schedule.cron;
            String[] parts = cronExpr.split("\\s+");
            if (parts.length == 1) {
                return Integer.parseInt(parts[0]) * 60 * 1000L;
            }
        }

        return 0;
    }

    private void cancelJob(String name) {
        ScheduledFuture<?> future = jobs.remove(name);
        if (future != null) {
            future.cancel(false);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadJobs() {
        if (!Files.exists(storePath)) return;

        try {
            String content = Files.readString(storePath);
            if (content.trim().isEmpty()) return;

            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();

            List<Map<String, Object>> jobData = mapper.readValue(content, List.class);

            for (Map<String, Object> data : jobData) {
                CronJob job = CronJob.fromMap(data);
                scheduleJob(job);
            }

        } catch (Exception e) {
            System.err.println("Failed to load cron jobs: " + e.getMessage());
        }
    }

    private void saveJob(CronJob job) {
        List<CronJob> jobs = loadAllJobs();
        jobs.removeIf(j -> j.getName().equals(job.getName()));
        jobs.add(job);
        saveAllJobs(jobs);
    }

    private void deleteJob(String name) {
        List<CronJob> jobs = loadAllJobs();
        jobs.removeIf(j -> j.getName().equals(name));
        saveAllJobs(jobs);
    }

    @SuppressWarnings("unchecked")
    private List<CronJob> loadAllJobs() {
        if (!Files.exists(storePath)) return new ArrayList<>();

        try {
            String content = Files.readString(storePath);
            if (content.trim().isEmpty()) return new ArrayList<>();

            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();

            List<Map<String, Object>> jobData = mapper.readValue(content, List.class);

            List<CronJob> jobs = new ArrayList<>();
            for (Map<String, Object> data : jobData) {
                jobs.add(CronJob.fromMap(data));
            }

            return jobs;

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveAllJobs(List<CronJob> jobs) {
        try {
            Files.createDirectories(storePath.getParent());
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(jobs);
            Files.writeString(storePath, json);
        } catch (Exception e) {
            System.err.println("Failed to save cron jobs: " + e.getMessage());
        }
    }

    public static class CronSchedule {
        public String kind;
        public long everyMs;
        public String cron;

        public CronSchedule() {}

        public CronSchedule(String kind, long everyMs) {
            this.kind = kind;
            this.everyMs = everyMs;
        }

        public static CronSchedule every(long ms) {
            return new CronSchedule("every", ms);
        }

        public static CronSchedule cron(String expression) {
            CronSchedule schedule = new CronSchedule();
            schedule.kind = "cron";
            schedule.cron = expression;
            return schedule;
        }
    }
}
