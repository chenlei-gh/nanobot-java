package com.nanobot.tool;

import java.io.*;
import java.util.*;

/**
 * Shell Tool - Execute shell commands
 */
public class ShellTool {
    private static final int TIMEOUT_SECONDS = 60;
    private static final int MAX_OUTPUT = 100000; // 100KB

    public static String execute(Map<String, Object> args, String workspace) {
        String command = getStringArg(args, "command", "command is required");

        // Security: Block dangerous commands
        validateCommand(command);

        try {
            // Change to workspace directory if specified
            ProcessBuilder pb;
            if (command.contains("cd ")) {
                // Handle cd command specially
                String newDir = extractCdPath(command);
                if (newDir != null && !newDir.startsWith("/") && !newDir.contains(":")) {
                    newDir = workspace + "/" + newDir;
                }
                pb = new ProcessBuilder("/bin/sh", "-c", "cd " + quote(newDir) + " && " + command);
            } else {
                pb = new ProcessBuilder("/bin/sh", "-c", command);
            }

            pb.directory(new File(workspace));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read output with timeout
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > MAX_OUTPUT) {
                        output.append("\n\n[Output truncated - too large]");
                        break;
                    }
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0 && output.length() == 0) {
                output.append("[Command failed with exit code ").append(exitCode).append("]");
            }

            return output.toString();

        } catch (Exception e) {
            throw new RuntimeException("Command failed: " + e.getMessage());
        }
    }

    private static void validateCommand(String command) {
        String lower = command.toLowerCase();

        // Block dangerous patterns
        String[] dangerous = {
            "rm -rf", "rm /", "mkfs", "dd if=", "cat /dev/urandom",
            "> /dev/", "2>&1", "$(",
            "&& rm", "; rm", "| rm",
            "chmod 777", "chmod -R",
            "wget", "curl", "nc ", "netcat",
            "ssh", "scp",
            "sudo", "su ",
            "passwd", "shadow", "/etc/passwd"
        };

        for (String pattern : dangerous) {
            if (lower.contains(pattern)) {
                throw new IllegalArgumentException("Dangerous command pattern blocked: " + pattern);
            }
        }
    }

    private static String extractCdPath(String command) {
        int cdIndex = command.indexOf("cd ");
        if (cdIndex == -1) return null;

        String afterCd = command.substring(cdIndex + 3).trim();

        // Extract path (handle quotes)
        if (afterCd.startsWith("\"")) {
            int endQuote = afterCd.indexOf("\"", 1);
            return endQuote > 0 ? afterCd.substring(1, endQuote) : afterCd.substring(1);
        }
        if (afterCd.startsWith("'")) {
            int endQuote = afterCd.indexOf("'", 1);
            return endQuote > 0 ? afterCd.substring(1, endQuote) : afterCd.substring(1);
        }

        // Extract first word
        String[] parts = afterCd.split("\\s+");
        return parts[0];
    }

    private static String quote(String str) {
        if (str == null) return "''";
        return "'" + str.replace("'", "'\\''") + "'";
    }

    private static String getStringArg(Map<String, Object> args, String key, String errorMsg) {
        Object value = args.get(key);
        if (value == null) {
            throw new IllegalArgumentException(errorMsg);
        }
        return value.toString();
    }
}
