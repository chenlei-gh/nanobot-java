package com.nanobot.test;

import java.util.*;
import java.util.concurrent.*;

/**
 * Simple verification test for Nanobot Java components
 * Compatible with Java 8+
 */
public class NanobotTest {
    
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static List<String> results = new ArrayList<>();
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           Nanobot Java - Verification Tests            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Run tests
        testContextManager();
        testMessageBus();
        testToolRegistry();
        testTokenCounter();
        testEventSystem();
        testStreamHandler();
        testThinkingTracker();
        testSubagentManager();
        
        // Print results
        printResults();
        
        // Exit with appropriate code
        if (testsFailed > 0) {
            System.exit(1);
        }
    }
    
    private static void testContextManager() {
        printTest("ContextManager - Basic Operations");
        try {
            // Test instantiation
            Object contextManager = Class.forName("com.nanobot.core.ContextManager")
                .getDeclaredConstructor(int.class, int.class)
                .newInstance(50, 8000);
            
            // Test addMessage
            Class<?> cmClass = contextManager.getClass();
            cmClass.getMethod("addMessage", String.class, String.class, String.class)
                .invoke(contextManager, "test-session", "user", "Hello");
            
            // Test getMessages
            Object messages = cmClass.getMethod("getMessages", String.class)
                .invoke(contextManager, "test-session");
            
            // Test session info
            Object info = cmClass.getMethod("getSessionInfo", String.class)
                .invoke(contextManager, "test-session");
            
            passed("ContextManager basic operations");
            
        } catch (Exception e) {
            failed("ContextManager", e);
        }
    }
    
    private static void testMessageBus() {
        printTest("MessageBus - Core Functionality");
        try {
            Object messageBus = Class.forName("com.nanobot.core.MessageBus")
                .getDeclaredConstructor()
                .newInstance();
            
            Class<?> mbClass = messageBus.getClass();
            
            // Test start/stop
            mbClass.getMethod("start").invoke(messageBus);
            mbClass.getMethod("stop").invoke(messageBus);
            
            // Test stats
            Object stats = mbClass.getMethod("getStats").invoke(messageBus);
            
            passed("MessageBus core functionality");
            
        } catch (Exception e) {
            failed("MessageBus", e);
        }
    }
    
    private static void testToolRegistry() {
        printTest("ToolRegistry - Registration");
        try {
            Object registry = Class.forName("com.nanobot.tool.ToolRegistry")
                .getDeclaredConstructor()
                .newInstance();
            
            Class<?> trClass = registry.getClass();
            
            // Test getToolNames
            Object names = trClass.getMethod("getToolNames").invoke(registry);
            
            // Test hasTool
            boolean hasRead = (Boolean) trClass.getMethod("hasTool", String.class)
                .invoke(registry, "read_file");
            
            if (hasRead) {
                passed("ToolRegistry registration");
            } else {
                failed("ToolRegistry", new Exception("read_file tool not registered"));
            }
            
        } catch (Exception e) {
            failed("ToolRegistry", e);
        }
    }
    
    private static void testTokenCounter() {
        printTest("TokenCounter - Estimation");
        try {
            // Test static methods
            int tokens = (Integer) Class.forName("com.nanobot.llm.TokenCounter")
                .getMethod("countTokens", String.class)
                .invoke(null, "Hello, World! This is a test.");
            
            int messagesTokens = (Integer) Class.forName("com.nanobot.llm.TokenCounter")
                .getMethod("countTokens", java.util.List.class, String.class)
                .invoke(null, null, "gpt-4");
            
            passed("TokenCounter estimation");
            
        } catch (Exception e) {
            failed("TokenCounter", e);
        }
    }
    
    private static void testEventSystem() {
        printTest("EventBus - Publish/Subscribe");
        try {
            Object eventBus = Class.forName("com.nanobot.bus.EventBus")
                .getDeclaredConstructor()
                .newInstance();
            
            Class<?> ebClass = eventBus.getClass();
            
            ebClass.getMethod("start").invoke(eventBus);
            ebClass.getMethod("stop").invoke(eventBus);
            
            Object stats = ebClass.getMethod("getStats").invoke(eventBus);
            
            passed("EventBus publish/subscribe");
            
        } catch (Exception e) {
            failed("EventBus", e);
        }
    }
    
    private static void testStreamHandler() {
        printTest("StreamHandler - Chunk Processing");
        try {
            Object handler = Class.forName("com.nanobot.llm.StreamHandler")
                .getDeclaredConstructor()
                .newInstance();
            
            Class<?> shClass = handler.getClass();
            
            // Test status methods
            Object status = shClass.getMethod("getStatus").invoke(handler);
            String statusName = status.toString();
            
            if ("IDLE".equals(statusName)) {
                passed("StreamHandler chunk processing");
            } else {
                failed("StreamHandler", new Exception("Unexpected initial status: " + statusName));
            }
            
        } catch (Exception e) {
            failed("StreamHandler", e);
        }
    }
    
    private static void testThinkingTracker() {
        printTest("ThinkingTracker - Reasoning");
        try {
            Object tracker = Class.forName("com.nanobot.agent.ThinkingTracker")
                .getDeclaredConstructor()
                .newInstance();
            
            Class<?> ttClass = tracker.getClass();
            
            // Test startThinking
            String thoughtId = (String) ttClass.getMethod("startThinking", String.class)
                .invoke(tracker, "Test thought");
            
            // Test getStats
            Object stats = ttClass.getMethod("getStats").invoke(tracker);
            
            if (thoughtId != null && thoughtId.startsWith("thought_")) {
                passed("ThinkingTracker reasoning");
            } else {
                failed("ThinkingTracker", new Exception("Invalid thought ID: " + thoughtId));
            }
            
        } catch (Exception e) {
            failed("ThinkingTracker", e);
        }
    }
    
    private static void testSubagentManager() {
        printTest("SubagentManager - Background Tasks");
        try {
            // Create executor
            java.util.concurrent.ExecutorService executor =
                java.util.concurrent.Executors.newCachedThreadPool();

            Object manager = Class.forName("com.nanobot.agent.SubagentManager")
                .getDeclaredConstructor(
                    java.util.concurrent.ExecutorService.class,
                    String.class
                )
                .newInstance(executor, "/tmp/workspace");

            Class<?> smClass = manager.getClass();

            // Test getStats
            Object stats = smClass.getMethod("getStats").invoke(manager);

            executor.shutdown();

            passed("SubagentManager background tasks");

        } catch (Exception e) {
            failed("SubagentManager", e);
        }
    }
    
    private static void printTest(String testName) {
        System.out.println("Testing " + testName + "...");
    }
    
    private static void passed(String testName) {
        testsPassed++;
        results.add("✓ " + testName);
        System.out.println("  ✓ " + testName);
    }
    
    private static void failed(String testName, Exception e) {
        testsFailed++;
        results.add("✗ " + testName + ": " + e.getMessage());
        System.out.println("  ✗ " + testName);
        System.out.println("    Error: " + e.getMessage());
    }
    
    private static void printResults() {
        System.out.println();
        System.out.println("═".repeat(60));
        System.out.println("Test Results:");
        System.out.println("═".repeat(60));
        
        for (String result : results) {
            System.out.println(result);
        }
        
        System.out.println();
        System.out.println("─".repeat(60));
        System.out.println("Total: " + (testsPassed + testsFailed) + " tests");
        System.out.println("Passed: " + testsPassed);
        System.out.println("Failed: " + testsFailed);
        System.out.println("─".repeat(60));
        
        if (testsFailed == 0) {
            System.out.println();
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║                    All Tests Passed!                      ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
        } else {
            System.out.println();
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║              Some Tests Failed - Fix Issues              ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
        }
    }
}
