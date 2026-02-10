package com.nanobot.channel;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Telegram Channel - Telegram Bot integration
 * Supports text messages, commands, and callbacks
 */
public class TelegramChannel implements Channel {
    private final String botToken;
    private final String apiUrl;
    private final HttpClient httpClient;
    private ChannelHandler handler;
    private volatile boolean running = false;
    private ExecutorService executor;
    private ScheduledExecutorService poller;

    public TelegramChannel(String botToken) {
        this.botToken = botToken;
        this.apiUrl = "https://api.telegram.org/bot" + botToken;
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public String getName() {
        return "telegram";
    }

    @Override
    public String getId() {
        return "telegram:" + botToken.substring(0, Math.min(5, botToken.length()));
    }

    @Override
    public void start() {
        if (running) return;

        running = true;

        // Set webhook or start polling
        try {
            setWebhook(null); // Use polling instead of webhook
            startPolling();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Telegram channel: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        running = false;
        if (poller != null) {
            poller.shutdown();
        }
        executor.shutdown();
    }

    @Override
    public void send(String chatId, String message) {
        try {
            sendMessage(chatId, message, null);
        } catch (Exception e) {
            if (handler != null) {
                handler.onError(getId(), "Failed to send message: " + e.getMessage());
            }
        }
    }

    @Override
    public void setHandler(ChannelHandler handler) {
        this.handler = handler;
    }

    private void startPolling() {
        poller = Executors.newSingleThreadScheduledExecutor();
        AtomicLong offset = new AtomicLong(0);

        poller.scheduleAtFixedRate(() -> {
            try {
                List<Map<String, Object>> updates = getUpdates(offset.get());

                for (Map<String, Object> update : updates) {
                    processUpdate(update);
                    long updateId = ((Number) update.get("update_id")).longValue();
                    if (updateId >= offset.get()) {
                        offset.set(updateId + 1);
                    }
                }
            } catch (Exception e) {
                // Log error but continue polling
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getUpdates(long offset) throws Exception {
        String url = apiUrl + "/getUpdates?timeout=1";
        if (offset > 0) {
            url += "&offset=" + offset;
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString());

        Map<String, Object> data = parseJson(response.body());
        boolean ok = (Boolean) data.getOrDefault("ok", false);

        if (!ok) {
            throw new RuntimeException("Telegram API error: " + data.get("description"));
        }

        return (List<Map<String, Object>>) data.getOrDefault("result", List.of());
    }

    @SuppressWarnings("unchecked")
    private void processUpdate(Map<String, Object> update) {
        try {
            Map<String, Object> message = (Map<String, Object>) update.get("message");

            if (message != null) {
                String chatId = String.valueOf(((Map<String, Object>) message.get("chat")).get("id"));
                String text = (String) message.get("text");
                String firstName = (String) ((Map<String, Object>) message.get("chat")).get("first_name");

                // Handle commands
                if (text != null && text.startsWith("/")) {
                    if (handler != null) {
                        handler.onMessage(getId(), chatId, "telegram:" + firstName, text);
                    }
                } else if (text != null) {
                    if (handler != null) {
                        handler.onMessage(getId(), chatId, "telegram:" + firstName, text);
                    }
                }

                if (handler != null) {
                    handler.onConnect(getId(), chatId);
                }
            }

            // Handle callback queries
            Map<String, Object> callbackQuery = (Map<String, Object>) update.get("callback_query");
            if (callbackQuery != null) {
                String chatId = String.valueOf(((Map<String, Object>) callbackQuery.get("message")).get("chat_id"));
                String data = (String) callbackQuery.get("data");

                if (handler != null) {
                    handler.onMessage(getId(), chatId, "callback", "/" + data);
                }

                // Answer callback
                answerCallback((String) callbackQuery.get("id"));
            }

        } catch (Exception e) {
            if (handler != null) {
                handler.onError(getId(), "Failed to process update: " + e.getMessage());
            }
        }
    }

    private void sendMessage(String chatId, String text, Map<String, Object> options) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", chatId);
        requestBody.put("text", text);

        if (options != null) {
            requestBody.putAll(options);
        }

        // Add default options
        requestBody.put("parse_mode", "Markdown");

        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl + "/sendMessage"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void answerCallback(String callbackId) throws Exception {
        String url = apiUrl + "/answerCallbackQuery?callback_query_id=" + callbackId;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(5))
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void setWebhook(String webhookUrl) throws Exception {
        String url;
        if (webhookUrl == null) {
            url = apiUrl + "/deleteWebhook";
        } else {
            url = apiUrl + "/setWebhook?url=" + URLEncoder.encode(webhookUrl, StandardCharsets.UTF_8);
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public void sendPhoto(String chatId, String photoUrl, String caption) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", chatId);
        requestBody.put("photo", photoUrl);
        if (caption != null) {
            requestBody.put("caption", caption);
            requestBody.put("parse_mode", "Markdown");
        }

        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl + "/sendPhoto"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public void sendKeyboard(String chatId, String text, List<List<String>> keyboard) throws Exception {
        Map<String, Object> replyMarkup = new HashMap<>();
        replyMarkup.put("keyboard", keyboard);
        replyMarkup.put("resize_keyboard", true);
        replyMarkup.put("one_time_keyboard", true);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", chatId);
        requestBody.put("text", text);
        requestBody.put("reply_markup", replyMarkup);

        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl + "/sendMessage"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String toJson(Object obj) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("JSON parsing failed", e);
        }
    }
}
