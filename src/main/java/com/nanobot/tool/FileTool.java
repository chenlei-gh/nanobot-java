package com.nanobot.tool;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * File Tool - File system operations
 */
public class FileTool {
    private static final int MAX_FILE_SIZE = 1024 * 1024; // 1MB

    public static String readFile(Map<String, Object> args, String workspace) {
        String path = getPath(args, workspace);

        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File not found: " + path);
        }

        if (!Files.isReadable(filePath)) {
            throw new IllegalArgumentException("File not readable: " + path);
        }

        try {
            long size = Files.size(filePath);
            if (size > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("File too large: " + size + " bytes (max: " + MAX_FILE_SIZE + ")");
            }

            String content = Files.readString(filePath);
            return "File: " + path + "\n\n" + content;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + e.getMessage());
        }
    }

    public static String writeFile(Map<String, Object> args, String workspace) {
        String path = getPath(args, workspace);
        String content = getStringArg(args, "content", "Content is required");

        try {
            Path filePath = Paths.get(path);

            // Create parent directories if needed
            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.writeString(filePath, content);

            return "Successfully wrote " + content.length() + " bytes to " + path;

        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + e.getMessage());
        }
    }

    public static String editFile(Map<String, Object> args, String workspace) {
        String path = getPath(args, workspace);
        String oldText = getStringArg(args, "old_text", "old_text is required");
        String newText = getStringArg(args, "new_text", "new_text is required");

        try {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                throw new IllegalArgumentException("File not found: " + path);
            }

            String content = Files.readString(filePath);

            if (!content.contains(oldText)) {
                throw new IllegalArgumentException("Text not found in file: " + oldText);
            }

            String newContent = content.replace(oldText, newText);
            Files.writeString(filePath, newContent);

            return "Successfully edited " + path;

        } catch (IOException e) {
            throw new RuntimeException("Failed to edit file: " + e.getMessage());
        }
    }

    public static String listDir(Map<String, Object> args, String workspace) {
        String path = getPath(args, workspace);

        Path dirPath = Paths.get(path);
        if (!Files.exists(dirPath)) {
            throw new IllegalArgumentException("Directory not found: " + path);
        }

        if (!Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("Not a directory: " + path);
        }

        try {
            StringBuilder sb = new StringBuilder();
            Files.list(dirPath)
                .sorted(Comparator.comparing(p -> !Files.isDirectory(p)))
                .forEach(p -> {
                    try {
                        String name = p.getFileName().toString();
                        if (Files.isDirectory(p)) {
                            sb.append("📁 ").append(name).append("\n");
                        } else {
                            sb.append("📄 ").append(name);
                            if (Files.isReadable(p)) {
                                long size = Files.size(p);
                                sb.append(" (").append(formatSize(size)).append(")");
                            }
                            sb.append("\n");
                        }
                    } catch (IOException e) {
                        // Skip unreadable files
                    }
                });

            return sb.toString();

        } catch (IOException e) {
            throw new RuntimeException("Failed to list directory: " + e.getMessage());
        }
    }

    public static String globFiles(Map<String, Object> args, String workspace) {
        String pattern = getStringArg(args, "pattern", "pattern is required");

        Path startPath;
        String patternPath = pattern;

        if (pattern.startsWith("/") || pattern.contains(":")) {
            startPath = Paths.get("/");
            patternPath = pattern.startsWith("/") ? pattern.substring(1) : pattern;
        } else {
            startPath = Paths.get(workspace);
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Pattern: ").append(pattern).append("\n\n");

            String globPattern = patternPath.endsWith("**") ?
                patternPath + "*" :
                patternPath.contains("*") ? patternPath : "**/" + patternPath;

            int maxResults = getIntArg(args, "maxResults", 50);

            try (var stream = Files.walk(startPath)) {
                stream
                    .filter(path -> {
                        try {
                            String relative = startPath.relativize(path).toString();
                            return matchesGlob(relative, globPattern);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .limit(maxResults + 1)
                    .forEach(p -> sb.append(p.toString()).append("\n"));
            }

            return sb.toString();

        } catch (IOException e) {
            throw new RuntimeException("Failed to glob files: " + e.getMessage());
        }
    }

    private static boolean matchesGlob(String path, String pattern) {
        // Simple glob matching
        String regex = pattern
            .replace(".", "\\.")
            .replace("**/", "(.*/)?")
            .replace("**", ".*")
            .replace("*", "[^/]*")
            .replace("?", ".");

        return path.matches(regex);
    }

    private static String getPath(Map<String, Object> args, String workspace) {
        String path = getStringArg(args, "path", "path is required");
        if (path.startsWith("~/")) {
            path = System.getProperty("user.home") + path.substring(1);
        } else if (!path.startsWith("/") && !path.contains(":")) {
            path = workspace + "/" + path;
        }
        return path;
    }

    private static String getStringArg(Map<String, Object> args, String key, String errorMsg) {
        Object value = args.get(key);
        if (value == null) {
            throw new IllegalArgumentException(errorMsg);
        }
        return value.toString();
    }

    private static int getIntArg(Map<String, Object> args, String key, int defaultValue) {
        Object value = args.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    private static String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024));
    }
}
