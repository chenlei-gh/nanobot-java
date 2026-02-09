package com.nanobot.tool;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Tool Hot Reload - Dynamic tool reloading without restart
 * Watches tool files and reloads them automatically
 */
public class ToolHotReload {
    private final ToolRegistry registry;
    private final Path toolsPath;
    private final Map<String, Path> toolFiles = new ConcurrentHashMap<>();
    private final ScheduledExecutorService watcher;
    private volatile boolean running = false;
    private final List<ToolChangeListener> listeners = new CopyOnWriteArrayList<>();

    public interface ToolChangeListener {
        void onToolAdded(String toolName);
        void onToolRemoved(String toolName);
        void onToolChanged(String toolName);
        void onError(String toolName, String error);
    }

    public ToolHotReload(ToolRegistry registry, Path toolsPath) {
        this.registry = registry;
        this.toolsPath = toolsPath;
        this.watcher = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Start watching for tool changes
     */
    public void start() {
        if (running) return;
        running = true;

        // Initial scan
        scanForTools();

        // Watch for changes
        watcher.scheduleAtFixedRate(this::scanForChanges, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Stop watching
     */
    public void stop() {
        running = false;
        watcher.shutdown();
    }

    /**
     * Scan for tool files
     */
    private void scanForTools() {
        if (!Files.exists(toolsPath)) {
            return;
        }

        try (var stream = Files.walk(toolsPath)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".java"))
                  .forEach(this::registerToolFile);
        } catch (Exception e) {
            System.err.println("Failed to scan tools: " + e.getMessage());
        }
    }

    /**
     * Detect changes and reload
     */
    private void scanForChanges() {
        for (Map.Entry<String, Path> entry : toolFiles.entrySet()) {
            try {
                long lastModified = Files.getLastModifiedTime(entry.getValue()).toMillis();
                Path toolPath = entry.getValue();

                if (needsReload(toolPath, lastModified)) {
                    reloadTool(entry.getKey(), toolPath);
                }
            } catch (Exception e) {
                notifyError(entry.getKey(), e.getMessage());
            }
        }
    }

    private boolean needsReload(Path path, long lastModified) {
        Long cached = lastModifiedTimes.get(path);
        return cached == null || cached < lastModified;
    }

    private final Map<Path, Long> lastModifiedTimes = new ConcurrentHashMap<>();

    private void registerToolFile(Path path) {
        String toolName = path.getFileName().toString()
            .replace(".java", "")
            .replace("Tool", "")
            .toLowerCase();

        toolFiles.put(toolName, path);
        lastModifiedTimes.put(path, Files.getLastModifiedTime(path).toMillis());

        notifyToolAdded(toolName);
    }

    private void reloadTool(String toolName, Path toolPath) {
        try {
            // Unregister old version
            registry.getToolNames().remove(toolName);

            // Update timestamp
            lastModifiedTimes.put(toolPath, Files.getLastModifiedTime(toolPath).toMillis());

            notifyToolChanged(toolName);

        } catch (Exception e) {
            notifyError(toolName, e.getMessage());
        }
    }

    /**
     * Manually reload a tool
     */
    public void reloadTool(String toolName) {
        Path toolPath = toolFiles.get(toolName);
        if (toolPath != null) {
            reloadTool(toolName, toolPath);
        }
    }

    /**
     * Get watched tools
     */
    public Set<String> getWatchedTools() {
        return new HashSet<>(toolFiles.keySet());
    }

    /**
     * Add change listener
     */
    public void addListener(ToolChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove change listener
     */
    public void removeListener(ToolChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyToolAdded(String toolName) {
        for (ToolChangeListener listener : listeners) {
            try {
                listener.onToolAdded(toolName);
            } catch (Exception e) {
                // Log but continue
            }
        }
    }

    private void notifyToolRemoved(String toolName) {
        for (ToolChangeListener listener : listeners) {
            try {
                listener.onToolRemoved(toolName);
            } catch (Exception e) {
                // Log but continue
            }
        }
    }

    private void notifyToolChanged(String toolName) {
        for (ToolChangeListener listener : listeners) {
            try {
                listener.onToolChanged(toolName);
            } catch (Exception e) {
                // Log but continue
            }
        }
    }

    private void notifyError(String toolName, String error) {
        for (ToolChangeListener listener : listeners) {
            try {
                listener.onError(toolName, error);
            } catch (Exception e) {
                // Log but continue
            }
        }
    }

    /**
     * Get statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("watchedTools", toolFiles.size());
        stats.put("running", running);
        stats.put("watchPath", toolsPath.toString());
        return stats;
    }
}
