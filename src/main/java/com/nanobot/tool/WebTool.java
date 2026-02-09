package com.nanobot.tool;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;

/**
 * Web Tool - HTTP operations for web search and fetch
 */
public class WebTool {
    private static final HttpClient client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public static String fetch(Map<String, Object> args, String workspace) {
        String url = getStringArg(args, "url", "url is required");
        String extractMode = getStringArg(args, "extractMode", "markdown");
        int maxChars = getIntArg(args, "maxChars", 50000);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Nanobot/1.0")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status >= 400) {
                return "HTTP Error " + status + " for URL: " + url;
            }

            String content = response.body();
            if (content == null || content.isEmpty()) {
                return "No content received from: " + url;
            }

            // Simple extraction (in production, use readability-java)
            String extracted = extractContent(content, extractMode);

            if (extracted.length() > maxChars) {
                extracted = extracted.substring(0, maxChars) + "\n\n[Content truncated]";
            }

            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            result.put("finalUrl", url);
            result.put("status", status);
            result.put("extractor", extractMode);
            result.put("length", extracted.length());
            result.put("truncated", extracted.length() >= maxChars);
            result.put("text", extracted);

            return formatResult(result);

        } catch (Exception e) {
            return "Error fetching URL: " + e.getMessage();
        }
    }

    public static String search(Map<String, Object> args, String workspace) {
        String query = getStringArg(args, "query", "query is required");
        int count = getIntArg(args, "count", 5);

        // This would integrate with Brave Search API or similar
        // For now, return placeholder
        return """
            Web search requires API configuration.
            Set tools.web.search.api_key in config.
            Query: %s
            """.formatted(query);
    }

    private static String extractContent(String html, String mode) {
        if ("text".equals(mode)) {
            return html.replaceAll("(?s)<script[^>]*>.*?</script>", "")
                       .replaceAll("(?s)<style[^>]*>.*?</style>", "")
                       .replaceAll("<[^>]+>", "")
                       .replaceAll("\\s+", " ")
                       .trim();
        }

        // Markdown extraction (simplified)
        String markdown = html.replaceAll("(?s)<script[^>]*>.*?</script>", "")
                              .replaceAll("(?s)<style[^>]*>.*?</style>", "")
                              .replaceAll("#", "\\#")
                              .replaceAll("\\*\\*", "**")
                              .replaceAll("<h1[^>]*>(.*?)</h1>", "\\n# $1\\n")
                              .replaceAll("<h2[^>]*>(.*?)</h2>", "\\n## $1\\n")
                              .replaceAll("<h3[^>]*>(.*?)</h3>", "\\n### $1\\n")
                              .replaceAll("<p[^>]*>(.*?)</p>", "$1\\n\\n")
                              .replaceAll("<br\\s*/?>", "\\n")
                              .replaceAll("<[^>]+>", "")
                              .replaceAll("\\n{3,}", "\\n\\n")
                              .replaceAll("[ \t]+", " ")
                              .trim();

        return markdown;
    }

    private static String formatResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("URL: ").append(result.get("url")).append("\n");
        sb.append("Status: ").append(result.get("status")).append("\n");
        sb.append("Length: ").append(result.get("length")).append(" chars\n");

        if (Boolean.TRUE.equals(result.get("truncated"))) {
            sb.append("(Truncated)\n");
        }

        sb.append("\n---\n\n");
        sb.append(result.get("text"));

        return sb.toString();
    }

    private static String getStringArg(Map<String, Object> args, String key, String errorMsg) {
        Object value = args.get(key);
        if (value == null) {
            throw new IllegalArgumentException(errorMsg);
        }
        return value.toString();
    }

    private static int getIntArg(Map<String, Object> args, String key, int defaultValue) {
        Object value = args.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }
}
