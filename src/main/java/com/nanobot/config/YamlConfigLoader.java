package com.nanobot.config;

import org.yaml.snakeyaml.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * YAML Configuration Loader
 */
public class YamlConfigLoader {
    private static final String CONFIG_FILE = "nanobot.yaml";

    public static NanobotConfig load() {
        return load(CONFIG_FILE);
    }

    public static NanobotConfig load(String configPath) {
        Path path = Paths.get(configPath);

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Config file not found: " + configPath);
        }

        try {
            Yaml yaml = new Yaml();
            try (InputStream is = Files.newInputStream(path)) {
                Map<String, Object> data = yaml.load(is);
                return parseConfig(data);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static NanobotConfig parseConfig(Map<String, Object> data) {
        NanobotConfig config = new NanobotConfig();

        if (data.containsKey("agents")) {
            parseAgents(config, (Map<String, Object>) data.get("agents"));
        }

        if (data.containsKey("mcpServers")) {
            parseMcpServers(config, (Map<String, Object>) data.get("mcpServers"));
        }

        if (data.containsKey("tools")) {
            parseTools(config, (Map<String, Object>) data.get("tools"));
        }

        if (data.containsKey("workspace")) {
            config.setWorkspacePath(String.valueOf(data.get("workspace")));
        }

        if (data.containsKey("data")) {
            config.setDataPath(String.valueOf(data.get("data")));
        }

        return config;
    }

    @SuppressWarnings("unchecked")
    private static void parseAgents(NanobotConfig config, Map<String, Object> agentsData) {
        NanobotConfig.AgentConfig agentConfig = config.getAgents();

        if (agentsData.containsKey("defaults")) {
            Map<String, Object> defaults = (Map<String, Object>) agentsData.get("defaults");
            if (defaults.containsKey("model")) {
                agentConfig.setDefaultsModel(String.valueOf(defaults.get("model")));
            }
            if (defaults.containsKey("maxIterations")) {
                agentConfig.setMaxIterations(((Number) defaults.get("maxIterations")).intValue());
            }
        }

        if (agentsData.containsKey("agents")) {
            Map<String, Object> agents = (Map<String, Object>) agentsData.get("agents");
            for (Map.Entry<String, Object> entry : agents.entrySet()) {
                Map<String, Object> agentData = (Map<String, Object>) entry.getValue();
                NanobotConfig.AgentDefinition agentDef = new NanobotConfig.AgentDefinition();
                agentDef.setName(entry.getKey());
                agentDef.setModel(String.valueOf(agentData.getOrDefault("model", agentConfig.getDefaultsModel())));

                if (agentData.containsKey("mcpServers")) {
                    List<String> mcpServers = (List<String>) agentData.get("mcpServers");
                    agentDef.getMcpServers().addAll(mcpServers);
                }
                if (agentData.containsKey("tools")) {
                    List<String> tools = (List<String>) agentData.get("tools");
                    agentDef.getTools().addAll(tools);
                }
                agentConfig.getAgents().put(entry.getKey(), agentDef);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void parseMcpServers(NanobotConfig config, Map<String, Object> mcpData) {
        NanobotConfig.McpServersConfig mcpConfig = config.getMcpServers();

        for (Map.Entry<String, Object> entry : mcpData.entrySet()) {
            Map<String, Object> serverData = (Map<String, Object>) entry.getValue();
            NanobotConfig.McpServerDefinition serverDef = new NanobotConfig.McpServerDefinition();

            if (serverData.containsKey("url")) {
                serverDef.setUrl(String.valueOf(serverData.get("url")));
            }
            if (serverData.containsKey("headers")) {
                serverDef.getHeaders().putAll((Map<String, String>) serverData.get("headers"));
            }

            mcpConfig.getServers().put(entry.getKey(), serverDef);
        }
    }

    @SuppressWarnings("unchecked")
    private static void parseTools(NanobotConfig config, Map<String, Object> toolsData) {
        NanobotConfig.ToolsConfig toolsConfig = config.getTools();

        if (toolsData.containsKey("web")) {
            Map<String, Object> webData = (Map<String, Object>) toolsData.get("web");
            NanobotConfig.WebToolsConfig webTools = toolsConfig.getWeb();

            if (webData.containsKey("search")) {
                Map<String, Object> searchData = (Map<String, Object>) webData.get("search");
                if (searchData.containsKey("api_key")) {
                    webTools.setSearchApiKey(String.valueOf(searchData.get("api_key")));
                }
            }
        }
    }

    public static void save(NanobotConfig config, String path) {
        throw new UnsupportedOperationException("Saving config not yet implemented");
    }
}
