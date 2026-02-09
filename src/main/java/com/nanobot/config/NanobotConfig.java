package com.nanobot.config;

import java.util.*;

/**
 * Nanobot Configuration
 */
public class NanobotConfig {
    private AgentConfig agents = new AgentConfig();
    private McpServersConfig mcpServers = new McpServersConfig();
    private ToolsConfig tools = new ToolsConfig();
    private String workspacePath = System.getProperty("user.home") + "/.nanobot/workspace";
    private String dataPath = System.getProperty("user.home") + "/.nanobot/data";

    public static class AgentConfig {
        private String defaultsModel = "gpt-4";
        private int maxIterations = 20;
        private int maxToolIterations = 10;
        private Map<String, AgentDefinition> agents = new HashMap<>();

        public String getDefaultsModel() { return defaultsModel; }
        public void setDefaultsModel(String model) { this.defaultsModel = model; }
        public int getMaxIterations() { return maxIterations; }
        public void setMaxIterations(int iterations) { this.maxIterations = iterations; }
        public int getMaxToolIterations() { return maxToolIterations; }
        public Map<String, AgentDefinition> getAgents() { return agents; }
    }

    public static class AgentDefinition {
        private String name;
        private String model;
        private List<String> mcpServers = new ArrayList<>();
        private List<String> tools = new ArrayList<>();
        private double temperature = 0.7;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<String> getMcpServers() { return mcpServers; }
        public List<String> getTools() { return tools; }
        public double getTemperature() { return temperature; }
    }

    public static class McpServersConfig {
        private Map<String, McpServerDefinition> servers = new HashMap<>();

        public Map<String, McpServerDefinition> getServers() { return servers; }
    }

    public static class McpServerDefinition {
        private String url;
        private Map<String, String> headers = new HashMap<>();

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public Map<String, String> getHeaders() { return headers; }
    }

    public static class ToolsConfig {
        private WebToolsConfig web = new WebToolsConfig();

        public WebToolsConfig getWeb() { return web; }
    }

    public static class WebToolsConfig {
        private String searchApiKey;
        private String searchApiBase = "https://api.search.brave.com";

        public String getSearchApiKey() { return searchApiKey; }
        public void setSearchApiKey(String key) { this.searchApiKey = key; }
        public String getSearchApiBase() { return searchApiBase; }
    }

    // Getters and Setters
    public AgentConfig getAgents() { return agents; }
    public McpServersConfig getMcpServers() { return mcpServers; }
    public ToolsConfig getTools() { return tools; }
    public String getWorkspacePath() { return workspacePath; }
    public void setWorkspacePath(String path) { this.workspacePath = path; }
    public String getDataPath() { return dataPath; }
    public void setDataPath(String dataPath) { this.dataPath = dataPath; }
}
