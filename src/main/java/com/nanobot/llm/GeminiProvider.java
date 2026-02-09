package com.nanobot.llm;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;

/**
 * Gemini Provider - Google Gemini API integration
 * Supports Gemini 1.5 Pro, Flash, and other variants
 */
public class GeminiProvider implements LlmProvider {
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final String GEMINI_STREAM_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:streamGenerateContent";
    private static final Set<String> GEMINI_MODELS = Set.of(
        "gemini-1.5-pro",
        "gemini-1.5-flash",
        "gemini-1.5-pro-exp-0827",
        "gemini-1.5-flash-exp-0827",
        "gemini-1.0-pro",
        "gemini-pro"
    );

    private final String apiKey;
    private final HttpClient client;

    public GeminiProvider(String apiKey) {
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
            String normalizedModel = normalizeModelName(model);
            String apiUrl = String.format(GEMINI_API_URL, normalizedModel);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", buildContents(messages, systemPrompt));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 4096);
            generationConfig.put("topP", 0.95);
            requestBody.put("generationConfig", generationConfig);

            if (tools != null && !tools.isEmpty()) {
                requestBody.put("tools", convertTools(tools));
            }

            String jsonBody = toJson(requestBody);

            String finalUrl = apiUrl + "?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(finalUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Gemini API error: " + response.body());
            }

            return parseResponse(response.body());

        } catch (Exception e) {
            throw new RuntimeException("Gemini request failed: " + e.getMessage());
        }
    }

    @Override
    public boolean supportsModel(String model) {
        return GEMINI_MODELS.stream().anyMatch(m ->
            model.toLowerCase().contains(m.toLowerCase().replace("-", "")));
    }

    @Override
    public String getName() {
        return "Gemini";
    }

    private String normalizeModelName(String model) {
        // Handle model name variations
        if (model.contains(":")) {
            return model;
        }
        return model.replace(".", "-");
    }

    private List<Map<String, Object>> buildContents(List<Map<String, String>> messages, String systemPrompt) {
        List<Map<String, Object>> contents = new ArrayList<>();

        Map<String, Object> content = new HashMap<>();
        List<Map<String, String>> parts = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            parts.add(Map.of("text", systemPrompt));
        }

        for (Map<String, String> msg : messages) {
            String role = msg.getOrDefault("role", "user");
            String normalizedRole = "user".equals(role) ? "user" : "model";
            parts.add(Map.of("text", msg.getOrDefault("content", "")));
        }

        content.put("parts", parts);
        contents.add(content);

        return contents;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> convertTools(List<Map<String, Object>> tools) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> tool : tools) {
            Object functionObj = tool.get("function");
            if (functionObj instanceof Map) {
                Map<String, Object> function = (Map<String, Object>) functionObj;
                Map<String, Object> toolDef = new HashMap<>();
                toolDef.put("functionDeclarations", List.of(function));
                result.add(toolDef);
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private LlmResponse parseResponse(String json) {
        try {
            Map<String, Object> response = parseJson(json);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");

            if (candidates == null || candidates.isEmpty()) {
                return new LlmResponse("", List.of(), 0);
            }

            Map<String, Object> candidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

            StringBuilder contentBuilder = new StringBuilder();
            List<ToolCall> toolCalls = new ArrayList<>();

            for (Map<String, Object> part : parts) {
                if (part.containsKey("text")) {
                    contentBuilder.append(part.get("text"));
                } else if (part.containsKey("functionCall")) {
                    Map<String, Object> functionCall = (Map<String, Object>) part.get("functionCall");
                    String id = (String) functionCall.get("id");
                    String name = (String) functionCall.get("name");
                    Map<String, Object> args = (Map<String, Object>) functionCall.get("args");
                    toolCalls.add(new ToolCall(id, name, args != null ? args : new HashMap<>()));
                }
            }

            int tokens = 0;
            Map<String, Object> usageMetadata = (Map<String, Object>) candidate.get("usageMetadata");
            if (usageMetadata != null) {
                tokens = (Integer) usageMetadata.getOrDefault("totalTokenCount", 0);
            }

            return new LlmResponse(contentBuilder.toString(), toolCalls, tokens);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage());
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
