package com.nanobot.llm;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;

/**
 * Qwen Provider - Alibaba Qwen (通义千问) API integration
 * Supports Qwen and Qwen-Coder models
 */
public class QwenProvider implements LlmProvider {
    private static final String QWEN_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
    private static final Set<String> QWEN_MODELS = Set.of(
        "qwen-turbo",
        "qwen-plus",
        "qwen-max",
        "qwen-max-longcontext",
        "qwen-coder-turbo",
        "qwen-coder"
    );

    private final String apiKey;
    private final HttpClient client;

    public QwenProvider(String apiKey) {
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

            Map<String, Object> input = new HashMap<>();
            input.put("messages", buildMessages(messages, systemPrompt));
            requestBody.put("input", input);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("temperature", 0.7);
            parameters.put("max_tokens", 4096);
            parameters.put("top_p", 0.95);

            if (tools != null && !tools.isEmpty()) {
                parameters.put("tools", convertTools(tools));
                parameters.put("tool_choice", "auto");
            }

            requestBody.put("parameters", parameters);

            String jsonBody = toJson(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(QWEN_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-DashScope-Session-Id", UUID.randomUUID().toString())
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Qwen API error: " + response.body());
            }

            return parseResponse(response.body());

        } catch (Exception e) {
            throw new RuntimeException("Qwen request failed: " + e.getMessage());
        }
    }

    @Override
    public boolean supportsModel(String model) {
        return QWEN_MODELS.stream().anyMatch(m ->
            model.toLowerCase().contains(m.toLowerCase()));
    }

    @Override
    public String getName() {
        return "Qwen";
    }

    private List<Map<String, String>> buildMessages(List<Map<String, String>> messages, String systemPrompt) {
        List<Map<String, String>> allMessages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            allMessages.add(Map.of("role", "system", "content", systemPrompt));
        }

        allMessages.addAll(messages);
        return allMessages;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> convertTools(List<Map<String, Object>> tools) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> tool : tools) {
            Object functionObj = tool.get("function");
            if (functionObj instanceof Map) {
                Map<String, Object> function = (Map<String, Object>) functionObj;
                Map<String, Object> toolDef = new HashMap<>();
                toolDef.put("type", "function");
                toolDef.put("function", function);
                result.add(toolDef);
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private LlmResponse parseResponse(String json) {
        try {
            Map<String, Object> response = parseJson(json);
            Map<String, Object> output = (Map<String, Object>) response.get("output");
            List<Map<String, Object>> choices = (List<Map<String, Object>>) output.get("choices");

            if (choices == null || choices.isEmpty()) {
                return new LlmResponse("", List.of(), 0);
            }

            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");

            String content = "";
            if (message.containsKey("content")) {
                Object contentObj = message.get("content");
                content = contentObj != null ? contentObj.toString() : "";
            }

            List<ToolCall> toolCalls = new ArrayList<>();
            Object toolCallsObj = message.get("tool_calls");
            if (toolCallsObj instanceof List) {
                for (Object tc : (List<?>) toolCallsObj) {
                    if (tc instanceof Map) {
                        Map<String, Object> tcMap = (Map<String, Object>) tc;
                        String id = (String) tcMap.get("id");
                        Map<String, Object> function = (Map<String, Object>) tcMap.get("function");
                        String name = function != null ? (String) function.get("name") : "";
                        Object argsObj = function != null ? function.get("arguments") : "";
                        Map<String, Object> args = new HashMap<>();

                        if (argsObj instanceof Map) {
                            args = (Map<String, Object>) argsObj;
                        } else if (argsObj instanceof String) {
                            args = parseJson((String) argsObj);
                        }

                        toolCalls.add(new ToolCall(id, name, args));
                    }
                }
            }

            Map<String, Object> usage = (Map<String, Object>) response.get("usage");
            int tokens = usage != null ? (Integer) usage.getOrDefault("total_tokens", 0) : 0;

            return new LlmResponse(content, toolCalls, tokens);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Qwen response: " + e.getMessage());
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
