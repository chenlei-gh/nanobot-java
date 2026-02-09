package com.nanobot.channel;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * WhatsApp Channel - WhatsApp Business API integration
 * Supports text messages, templates, and media
 */
public class WhatsAppChannel implements Channel {
    private final String phoneNumberId;
    private final String accessToken;
    private final String apiVersion;
    private final String apiUrl;
    private final HttpClient httpClient;
    private ChannelHandler handler;
    private volatile boolean running = false;
    private ExecutorService executor;
    private volatile String webhookVerifyToken;

    public WhatsAppChannel(String phoneNumberId, String accessToken) {
        this(phoneNumberId, accessToken, "v18.0");
    }

    public WhatsAppChannel(String phoneNumberId, String accessToken, String apiVersion) {
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
        this.apiVersion = apiVersion;
        this.apiUrl = "https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId;
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public String getName() {
        return "whatsapp";
    }

    @Override
    public String getId() {
        return "whatsapp:" + phoneNumberId.substring(0, Math.min(5, phoneNumberId.length()));
    }

    @Override
    public void start() {
        if (running) return;
        running = true;
    }

    @Override
    public void stop() {
        running = false;
        executor.shutdown();
    }

    @Override
    public void send(String chatId, String message) {
        try {
            sendTextMessage(chatId, message);
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

    /**
     * Process incoming webhook
     */
    @SuppressWarnings("unchecked")
    public void processWebhook(String body) {
        try {
            Map<String, Object> data = parseJson(body);
            List<Map<String, Object>> entryList = (List<Map<String, Object>>) data.get("entry");

            if (entryList == null || entryList.isEmpty()) {
                return;
            }

            for (Map<String, Object> entry : entryList) {
                List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");

                for (Map<String, Object> change : changes) {
                    Map<String, Object> value = (Map<String, Object>) change.get("value");

                    // Handle messages
                    if (value.containsKey("messages")) {
                        List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");

                        for (Map<String, Object> message : messages) {
                            processIncomingMessage(message, value);
                        }
                    }

                    // Handle status updates
                    if (value.containsKey("statuses")) {
                        List<Map<String, Object>> statuses = (List<Map<String, Object>>) value.get("statuses");

                        for (Map<String, Object> status : statuses) {
                            processStatusUpdate(status);
                        }
                    }
                }
            }

        } catch (Exception e) {
            if (handler != null) {
                handler.onError(getId(), "Failed to process webhook: " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processIncomingMessage(Map<String, Object> message, Map<String, Object> value) {
        String from = (String) message.get("from");
        String msgId = (String) message.get("id");
        String timestamp = (String) message.get("timestamp");

        Map<String, Object> metadata = (Map<String, Object>) value.get("metadata");
        String phoneNumber = metadata != null ? (String) metadata.get("display_phone_number") : "";

        // Determine message type
        if (message.containsKey("text")) {
            String text = (String) ((Map<String, Object>) message.get("text")).get("body");

            if (handler != null) {
                handler.onMessage(getId(), from, from, text);
            }
        } else if (message.containsKey("button")) {
            String text = (String) ((Map<String, Object>) message.get("button")).get("text");

            if (handler != null) {
                handler.onMessage(getId(), from, from, "/" + text);
            }
        } else if (message.containsKey("interactive")) {
            Map<String, Object> interactive = (Map<String, Object>) message.get("interactive");
            String type = (String) interactive.get("type");

            if ("button_reply".equals(type)) {
                String text = (String) ((Map<String, Object>) interactive.get("button_reply")).get("title");

                if (handler != null) {
                    handler.onMessage(getId(), from, from, "/" + text);
                }
            } else if ("list_reply".equals(type)) {
                String id = (String) ((Map<String, Object>) interactive.get("list_reply")).get("id");
                String title = (String) ((Map<String, Object>) interactive.get("list_reply")).get("title");

                if (handler != null) {
                    handler.onMessage(getId(), from, from, "/" + title + ":" + id);
                }
            }
        }

        if (handler != null) {
            handler.onConnect(getId(), from);
        }

        // Mark message as read
        try {
            markMessageRead(msgId);
        } catch (Exception e) {
            // Ignore
        }
    }

    private void processStatusUpdate(Map<String, Object> status) {
        String id = (String) status.get("id");
        String status = (String) status.get("status");

        // Could emit events for message status changes
    }

    private void sendTextMessage(String to, String text) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messaging_product", "whatsapp");
        requestBody.put("to", to);
        requestBody.put("type", "text");

        Map<String, Object> textBody = new HashMap<>();
        textBody.put("preview_url", false);
        textBody.put("body", text);
        requestBody.put("text", textBody);

        sendRequest(requestBody);
    }

    private void sendTemplateMessage(String to, String templateName, Map<String, String> parameters) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messaging_product", "whatsapp");
        requestBody.put("to", to);
        requestBody.put("type", "template");

        Map<String, Object> template = new HashMap<>();
        template.put("name", templateName);

        Map<String, Object> language = new HashMap<>();
        language.put("code", "en_US");
        template.put("language", language);

        List<Map<String, Object>> components = new ArrayList<>();

        if (parameters != null && !parameters.isEmpty()) {
            List<Map<String, Object>> parametersList = new ArrayList<>();

            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                Map<String, Object> param = new HashMap<>();
                param.put("type", "text");
                param.put("text", entry.getValue());
                parametersList.add(param);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("type", "body");
            body.put("parameters", parametersList);
            components.add(body);
        }

        if (!components.isEmpty()) {
            template.put("components", components);
        }

        requestBody.put("template", template);

        sendRequest(requestBody);
    }

    private void sendRequest(Map<String, Object> requestBody) throws Exception {
        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl + "/messages"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + accessToken)
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void markMessageRead(String messageId) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messaging_product", "whatsapp");
        requestBody.put("status", "read");
        requestBody.put("message_id", messageId);

        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl + "/messages"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + accessToken)
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Verify webhook
     */
    public boolean verifyWebhook(String mode, String token, String challenge) {
        if ("subscribe".equals(mode) && token.equals(webhookVerifyToken)) {
            return true;
        }
        return false;
    }

    public void setWebhookVerifyToken(String token) {
        this.webhookVerifyToken = token;
    }

    /**
     * Send interactive buttons
     */
    public void sendButtons(String to, String text, List<Button> buttons) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messaging_product", "whatsapp");
        requestBody.put("to", to);
        requestBody.put("type", "interactive");

        Map<String, Object> interactive = new HashMap<>();
        interactive.put("type", "button");

        Map<String, Object> buttonBody = new HashMap<>();
        buttonBody.put("text", text);
        interactive.put("body", buttonBody);

        List<Map<String, Object>> buttonList = new ArrayList<>();
        int i = 0;
        for (Button button : buttons) {
            Map<String, Object> buttonMap = new HashMap<>();
            buttonMap.put("type", "reply");
            buttonMap.put("reply", Map.of("id", button.id, "title", button.title));
            buttonList.add(buttonMap);

            if (++i >= 3) break; // WhatsApp limit
        }

        interactive.put("action", Map.of("buttons", buttonList));
        requestBody.put("interactive", interactive);

        sendRequest(requestBody);
    }

    /**
     * Send list message
     */
    public void sendList(String to, String text, String buttonText, List<ListItem> sections) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messaging_product", "whatsapp");
        requestBody.put("to", to);
        requestBody.put("type", "interactive");

        Map<String, Object> interactive = new HashMap<>();
        interactive.put("type", "list");

        Map<String, Object> header = new HashMap<>();
        header.put("type", "text");
        header.put("text", text.substring(0, Math.min(20, text.length()));
        interactive.put("header", header);

        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        interactive.put("body", body);

        Map<String, Object> action = new HashMap<>();
        action.put("button", buttonText);

        List<Map<String, Object>> sectionList = new ArrayList<>();
        for (ListItem section : sections) {
            Map<String, Object> sectionMap = new HashMap<>();
            sectionMap.put("title", section.title);

            List<Map<String, Object>> rowList = new ArrayList<>();
            for (ListItem.Item item : section.items) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", item.id);
                row.put("title", item.title);
                if (item.description != null) {
                    row.put("description", item.description);
                }
                rowList.add(row);
            }

            sectionMap.put("rows", rowList);
            sectionList.add(sectionMap);
        }

        action.put("sections", sectionList);
        interactive.put("action", action);
        requestBody.put("interactive", interactive);

        sendRequest(requestBody);
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

    public static class Button {
        public final String id;
        public final String title;

        public Button(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    public static class ListItem {
        public final String title;
        public final List<Item> items;

        public ListItem(String title, List<Item> items) {
            this.title = title;
            this.items = items;
        }

        public static class Item {
            public final String id;
            public final String title;
            public final String description;

            public Item(String id, String title, String description) {
                this.id = id;
                this.title = title;
                this.description = description;
            }
        }
    }
}
