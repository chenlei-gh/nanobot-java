package com.nanobot.llm;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;

/**
 * OpenAI Provider - OpenAI GPT models integration
 */
public class OpenAiProvider implements LlmProvider {
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final Set<String> OPENAI_MODELS = Set.of(
        "gpt-4", "gpt-4-turbo", "gpt-4o", "gpt-4o-mini",
        "gpt-3.5-turbo", "gpt-3.5-turbo-16k"
    );

    private final String apiKey;
    private final HttpClient client;

    public OpenAiProvider(String apiKey) {
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
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.7);

            // Build messages
            List<Map<String, String>> allMessages = new ArrayList<>();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                allMessages.add(Map.of("role", "system", "content", systemPrompt));
            }

            allMessages.addAll(messages);

            requestBody.put("messages", allMessages);

            // Add tools if provided
            if (tools != null && !tools.isEmpty()) {
                List<Map<String, Object>> functions = new ArrayList<>();
                for (Map<String, Object> tool : tools) {
                    Object functionObj = tool.get("function");
                    if (functionObj instanceof Map) {
                        functions.add((Map<String, Object>) functionObj);
                    }
                }
                if (!functions.isEmpty()) {
                    requestBody.put("tools", functions);
                    requestBody.put("tool_choice", "auto");
                }
            }

            String jsonBody = toJson(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("OpenAI API error: " + response.body());
            }

            return parseResponse(response.body());

        } catch (Exception e) {
            throw new RuntimeException("OpenAI request failed: " + e.getMessage());
        }
    }

    @Override
    public boolean supportsModel(String model) {
        return OPENAI_MODELS.stream().anyMatch(m ->
            model.toLowerCase().startsWith(m.toLowerCase()));
    }

    @Override
    public String getName() {
        return "OpenAI";
    }

    @SuppressWarnings("unchecked")
    private LlmResponse parseResponse(String json) {
        try {
            Map<String, Object> response = parseJson(json);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");

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
                            // Parse JSON arguments
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
            throw new RuntimeException("Failed to parse OpenAI response: " + e.getMessage());
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
