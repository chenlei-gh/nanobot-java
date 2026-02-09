package com.nanobot.tool;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;

/**
 * Tool Registry - Manages all available tools
 * Thread-safe registration and execution
 */
public class ToolRegistry {
    private final ConcurrentHashMap<String, ToolDescriptor> tools = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BiFunction<Map<String, Object>, String, Object>> executors = new ConcurrentHashMap<>();

    public static class ToolDescriptor {
        private final String name;
        private final String description;
        private final Map<String, ToolParameter> parameters;
        private final boolean requiresWorkspace;

        public ToolDescriptor(String name, String description, Map<String, ToolParameter> parameters, boolean requiresWorkspace) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
            this.requiresWorkspace = requiresWorkspace;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public Map<String, ToolParameter> getParameters() { return parameters; }
        public boolean isRequiresWorkspace() { return requiresWorkspace; }
    }

    public static class ToolParameter {
        private final String type;
        private final String description;
        private final boolean required;

        public ToolParameter(String type, String description, boolean required) {
            this.type = type;
            this.description = description;
            this.required = required;
        }

        public String getType() { return type; }
        public String getDescription() { return description; }
        public boolean isRequired() { return required; }
    }

    /**
     * Register a tool
     */
    public void register(String name, String description, Map<String, ToolParameter> parameters,
                         boolean requiresWorkspace,
                         BiFunction<Map<String, Object>, String, Object> executor) {
        tools.put(name, new ToolDescriptor(name, description, parameters, requiresWorkspace));
        executors.put(name, executor);
    }

    /**
     * Execute a tool
     */
    public Object execute(String name, Map<String, Object> arguments, String workspace, String sessionId) {
        ToolDescriptor descriptor = tools.get(name);
        if (descriptor == null) {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }

        BiFunction<Map<String, Object>, String, Object> executor = executors.get(name);
        if (executor == null) {
            throw new IllegalArgumentException("No executor for tool: " + name);
        }

        return executor.apply(arguments, workspace);
    }

    /**
     * Get tool descriptor
     */
    public ToolDescriptor getTool(String name) {
        return tools.get(name);
    }

    /**
     * Get all tool names
     */
    public Set<String> getToolNames() {
        return tools.keySet();
    }

    /**
     * Get all tool descriptors
     */
    public Collection<ToolDescriptor> getAllTools() {
        return tools.values();
    }

    /**
     * Check if tool exists
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    /**
     * Get tool names for LLM function calling format
     */
    public List<Map<String, Object>> getToolsForLlm() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (ToolDescriptor tool : tools.values()) {
            Map<String, Object> toolDef = new HashMap<>();
            toolDef.put("type", "function");
            toolDef.put("function", getFunctionDefinition(tool));
            result.add(toolDef);
        }

        return result;
    }

    private Map<String, Object> getFunctionDefinition(ToolDescriptor tool) {
        Map<String, Object> function = new HashMap<>();
        function.put("name", tool.getName());
        function.put("description", tool.getDescription());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", new HashMap<>());
        parameters.put("required", new ArrayList<String>());

        Map<String, ToolParameter> paramDefs = tool.getParameters();
        for (Map.Entry<String, ToolParameter> entry : paramDefs.entrySet()) {
            Map<String, Object> paramDef = new HashMap<>();
            paramDef.put("type", entry.getValue().getType());
            paramDef.put("description", entry.getValue().getDescription());

            ((Map<String, Object>) parameters.get("properties")).put(entry.getKey(), paramDef);

            if (entry.getValue().isRequired()) {
                ((List<String>) parameters.get("required")).add(entry.getKey());
            }
        }

        function.put("parameters", parameters);
        return function;
    }
}
