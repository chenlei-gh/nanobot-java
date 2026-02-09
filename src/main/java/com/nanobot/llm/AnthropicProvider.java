package com.nanobot.llm;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;

/**
 * Anthropic Provider - Claude models integration
 */
public class AnthropicProvider implements LlmProvider {
    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final Set<String> ANTHROPIC_MODELS = Set.of(
        "claude-3-5-sonnet-latest", "claude-3-5-sonnet-20241022",
        "claude-3-opus-latest", "claude-3-opus-20240229",
        "claude-3-haiku-latest", "claude-3-haiku-20240307",
        "claude-3-sonnet-latest", "claude-3-sonnet-20240229"
    );

    private final String apiKey;
    private final HttpClient client;

    public AnthropicProvider(String apiKey) {
        this.apiKey = apiKey;
        this.client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    @Override
    public LlmResponse complete(String model, List<Map<String, String>> messages,
                                 String systemPrompt) {
        return completeWithTools(model, messages, systemPrompt, null);
    }

    @Override
    public LlmResponse completeWithTools(String model, List<Map<String, String>> messages,
                                          String systemPrompt,
                                          List<Map<String, Object>> tools) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 4096);
            requestBody.put("temperature", 0.7);

            // Build messages (Anthropic format)
            List<Map<String, String>> allMessages = new ArrayList<>();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                allMessages.add(Map.of("role", "user", "content", systemPrompt));
            }

            // Convert to Anthropic format
            for (Map<String, String> msg : messages) {
                String role = msg.getOrDefault("role", "user");
                // Anthropic only supports user/assistant
                if ("system".equals(role)) {
                    role = "user";
                }
                allMessages.add(Map.of("role", role, "content", msg.getOrDefault("content", "")));
            }

            requestBody.put("messages", allMessages);

            // Add tools if provided
            if (tools != null && !tools.isEmpty()) {
                requestBody.put("tools", convertTools(tools));
            }

            String jsonBody = toJson(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANTHROPIC_API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Anthropic API error: " + response.body());
            }

            return parseResponse(response.body());

        } catch (Exception e) {
            throw new RuntimeException("Anthropic request failed: " + e.getMessage());
        }
    }

    @Override
    public boolean supportsModel(String model) {
        return ANTHROPIC_MODELS.stream().anyMatch(m ->
            model.toLowerCase().contains(m.toLowerCase().split("-")[1]));
    }

    @Override
    public String getName() {
        return "Anthropic";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> convertTools(List<Map<String, Object>> tools) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> tool : tools) {
            Object functionObj = tool.get("function");
            if (functionObj instanceof Map) {
                Map<String, Object> function = (Map<String, Object>) functionObj;
                Map<String, Object> toolDef = new HashMap<>();
                toolDef.put("name", function.get("name"));
                toolDef.put("description", function.get("description"));
                toolDef.put("input_schema", function.get("parameters"));
                result.add(toolDef);
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private LlmResponse parseResponse(String json) {
        try {
            Map<String, Object> response = parseJson(json);
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");

            if (content == null || content.isEmpty()) {
                return new LlmResponse("", List.of(), 0);
            }

            StringBuilder contentBuilder = new StringBuilder();
            List<ToolCall> toolCalls = new ArrayList<>();

            for (Map<String, Object> block : content) {
                String type = (String) block.get("type");

                if ("text".equals(type)) {
                    contentBuilder.append(block.get("text"));
                } else if ("tool_use".equals(type)) {
                    String id = (String) block.get("id");
                    String name = (String) ((Map<String, Object>) block.get("input")).get("name");
                    Map<String, Object> input = (Map<String, Object>) ((Map<String, Object>) block.get("input")).get("input");
                    toolCalls.add(new ToolCall(id, name, input != null ? input : new HashMap<>()));
                }
            }

            Map<String, Object> usage = (Map<String, Object>) response.get("usage");
            int tokens = usage != null ? (Integer) usage.getOrDefault("output_tokens", 0) : 0;

            return new LlmResponse(contentBuilder.toString(), toolCalls, tokens);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Anthropic response: " + e.getMessage());
        }
    }

    private String toJson(Object obj) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("JSON parsing failed: " + e.getMessage());
        }
    }
}
