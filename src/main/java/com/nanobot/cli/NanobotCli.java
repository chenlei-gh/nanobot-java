package com.nanobot.cli;

import com.nanobot.config.*;
import com.nanobot.core.*;
import com.nanobot.llm.*;
import com.nanobot.tool.*;
import com.nanobot.cron.*;
import jline.console.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Interactive CLI - Main entry point for command-line usage
 */
public class NanobotCli {
    private static final String VERSION = "1.0.0";

    private static NanobotConfig config;
    private static MessageBus messageBus;
    private static ContextManager contextManager;
    private static ToolRegistry toolRegistry;
    private static AgentLoop agentLoop;
    private static CronService cronService;

    public static void main(String[] args) {
        System.out.println("Nanobot v" + VERSION + " - AI Agent (Java 21)");
        System.out.println("======================================\n");

        if (args.length > 0) {
            handleCommand(args);
            return;
        }

        try {
            initialize();
            runInteractive();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void handleCommand(String[] args) {
        String command = args[0];

        switch (command) {
            case "run" -> {
                String configPath = args.length > 1 ? args[1] : "nanobot.yaml";
                runAgent(configPath);
            }
            case "agent" -> {
                String message = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "";
                runSingleAgent(message);
            }
            case "shell" -> {
                runShell();
            }
            case "version" -> {
                System.out.println("Nanobot v" + VERSION);
            }
            case "help" -> {
                printHelp();
            }
            default -> {
                System.out.println("Unknown command: " + command);
                printHelp();
            }
        }
    }

    private static void initialize() throws Exception {
        config = loadConfig();
        messageBus = new MessageBus();
        contextManager = new ContextManager(50, 8000);
        toolRegistry = createToolRegistry();
        LlmProvider llmProvider = createLlmProvider();
        agentLoop = new AgentLoop(
            messageBus, llmProvider, toolRegistry, contextManager,
            config.getWorkspacePath(),
            config.getAgents().getDefaultsModel(),
            config.getAgents().getMaxIterations()
        );

        cronService = new CronService(
            Paths.get(config.getDataPath(), "cron", "jobs.json"),
            job -> {
                String result = agentLoop.process("cron:" + job.getId(), job.getMessage());
                return new CronJob.ExecutedJob(job.getId(), job.getMessage(), result, 0);
            }
        );

        messageBus.start();
        agentLoop.start();
        cronService.start();
    }

    private static NanobotConfig loadConfig() {
        Path configPath = Paths.get("nanobot.yaml");
        if (!configPath.toFile().exists()) {
            configPath = Paths.get(System.getProperty("user.home"), ".nanobot", "config.yaml");
        }

        if (configPath.toFile().exists()) {
            return YamlConfigLoader.load(configPath.toString());
        }

        NanobotConfig defaultConfig = new NanobotConfig();
        defaultConfig.setWorkspacePath(System.getProperty("user.home") + "/.nanobot/workspace");
        return defaultConfig;
    }

    private static ToolRegistry createToolRegistry() {
        ToolRegistry registry = new ToolRegistry();

        // File tools
        registry.register(
            "read_file",
            "Read contents of a file",
            Map.of(
                "path", new ToolRegistry.ToolParameter("string", "Path to file", true)
            ),
            true,
            FileTool::readFile
        );

        registry.register(
            "write_file",
            "Write content to a file",
            Map.of(
                "path", new ToolRegistry.ToolParameter("string", "Path to file", true),
                "content", new ToolRegistry.ToolParameter("string", "Content to write", true)
            ),
            true,
            FileTool::writeFile
        );

        registry.register(
            "edit_file",
            "Replace text in a file",
            Map.of(
                "path", new ToolRegistry.ToolParameter("string", "Path to file", true),
                "old_text", new ToolRegistry.ToolParameter("string", "Text to replace", true),
                "new_text", new ToolRegistry.ToolParameter("string", "Replacement text", true)
            ),
            true,
            FileTool::editFile
        );

        registry.register(
            "list_dir",
            "List directory contents",
            Map.of(
                "path", new ToolRegistry.ToolParameter("string", "Directory path", false)
            ),
            true,
            FileTool::listDir
        );

        // Shell tool
        registry.register(
            "bash",
            "Execute a shell command",
            Map.of(
                "command", new ToolRegistry.ToolParameter("string", "Command to execute", true)
            ),
            true,
            ShellTool::execute
        );

        // Web tools
        registry.register(
            "web_fetch",
            "Fetch content from URL",
            Map.of(
                "url", new ToolRegistry.ToolParameter("string", "URL to fetch", true),
                "extractMode", new ToolRegistry.ToolParameter("string", "markdown or text", false),
                "maxChars", new ToolRegistry.ToolParameter("integer", "Max characters", false)
            ),
            false,
            WebTool::fetch
        );

        registry.register(
            "web_search",
            "Search the web",
            Map.of(
                "query", new ToolRegistry.ToolParameter("string", "Search query", true),
                "count", new ToolRegistry.ToolParameter("integer", "Max results", false)
            ),
            false,
            WebTool::search
        );

        return registry;
    }

    private static LlmProvider createLlmProvider() {
        String openaiKey = System.getenv("OPENAI_API_KEY");
        String anthropicKey = System.getenv("ANTHROPIC_API_KEY");

        if (openaiKey != null && !openaiKey.isEmpty()) {
            return new OpenAiProvider(openaiKey);
        }

        if (anthropicKey != null && !anthropicKey.isEmpty()) {
            return new AnthropicProvider(anthropicKey);
        }

        throw new IllegalArgumentException("Please set OPENAI_API_KEY or ANTHROPIC_API_KEY environment variable");
    }

    private static void runInteractive() throws Exception {
        ConsoleReader reader = new ConsoleReader();

        reader.setPrompt("nanobot> ");

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.isEmpty()) continue;

            if (line.startsWith("/")) {
                handleSlashCommand(line);
            } else {
                processMessage(line);
            }

            reader.setPrompt("nanobot> ");
        }
    }

    private static void runAgent(String configPath) {
        System.out.println("Running agent with config: " + configPath);
        initialize();
        runInteractive();
    }

    private static void runSingleAgent(String message) {
        try {
            initialize();
            String response = agentLoop.process("cli:" + System.currentTimeMillis(), message);
            System.out.println("\n" + response);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void runShell() {
        System.out.println("Entering shell mode. Type /exit to quit.");
        runInteractive();
    }

    private static void processMessage(String message) {
        try {
            String sessionId = "cli:" + System.currentTimeMillis();
            String response = agentLoop.process(sessionId, message);
            System.out.println("\n" + response);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void handleSlashCommand(String command) {
        String[] parts = command.split("\\s+");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "/help" -> printHelp();
            case "/exit", "/quit" -> {
                System.out.println("Goodbye!");
                System.exit(0);
            }
            case "/clear" -> {
                System.out.print("\033[H\033[2J");
            }
            case "/stats" -> {
                Map<String, Object> stats = new HashMap<>();
                stats.put("messageBus", messageBus.getStats());
                stats.put("cron", cronService.getStats());
                stats.put("sessions", contextManager.getSessionKeys().size());
                System.out.println(stats);
            }
            case "/sessions" -> {
                for (String session : contextManager.getSessionKeys()) {
                    Map<String, Object> info = contextManager.getSessionInfo(session);
                    System.out.println(session + ": " + info.get("messageCount") + " messages");
                }
            }
            case "/tools" -> {
                for (String tool : toolRegistry.getToolNames()) {
                    ToolRegistry.ToolDescriptor desc = toolRegistry.getTool(tool);
                    System.out.println("- " + desc.getName() + ": " + desc.getDescription());
                }
            }
            case "/cron" -> {
                for (CronJob job : cronService.getJobs()) {
                    System.out.println("- " + job.getName() + ": " + job.getSchedule().kind);
                }
            }
            case "/reset" -> {
                for (String session : contextManager.getSessionKeys()) {
                    contextManager.clearSession(session);
                }
                System.out.println("Sessions cleared.");
            }
            default -> System.out.println("Unknown command: " + cmd);
        }
    }

    private static void printHelp() {
        System.out.println("""
            Nanobot Commands:
              /help     - Show this help
              /exit     - Exit nanobot
              /clear    - Clear screen
              /stats    - Show statistics
              /sessions - List active sessions
              /tools    - List available tools
              /cron     - List scheduled jobs
              /reset    - Clear all sessions

            Environment Variables:
              OPENAI_API_KEY     - OpenAI API key
              ANTHROPIC_API_KEY  - Anthropic API key

            Usage:
              nanobot run [config]  - Run with config file
              nanobot agent [msg]  - Send single message
              nanobot shell        - Interactive shell mode
            """);
    }
}
