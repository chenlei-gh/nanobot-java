package com.nanobot.tool;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;

/**
 * Enhanced Web Tool - HTTP operations with Brave Search and content extraction
 */
public class EnhancedWebTool {
    private static final HttpClient client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private String braveApiKey;

    public void setBraveApiKey(String apiKey) {
        this.braveApiKey = apiKey;
    }

    public Object fetch(Map<String, Object> args, String workspace) {
        String url = getStringArg(args, "url", "url is required");
        String extractMode = getStringArg(args, "extractMode", "markdown");
        int maxChars = getIntArg(args, "maxChars", 50000);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Nanobot/1.0 (Java)")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status >= 400) {
                return createResult(url, url, status, null, "HTTP Error " + status);
            }

            String content = response.body();
            if (content == null || content.isEmpty()) {
                return createResult(url, url, status, null, "No content received");
            }

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

            return result;

        } catch (Exception e) {
            return createResult(url, url, 0, null, "Error: " + e.getMessage());
        }
    }

    public Object search(Map<String, Object> args, String workspace) {
        String query = getStringArg(args, "query", "query is required");
        int count = getIntArg(args, "count", 10);

        if (braveApiKey == null || braveApiKey.isEmpty()) {
            // Return mock results if no API key
            return createMockSearchResults(query, count);
        }

        try {
            String searchUrl = String.format(
                "https://api.search.brave.com/v1/search?q=%s&count=%d",
                URLEncoder.encode(query, StandardCharsets.UTF_8),
                Math.min(count, 20)
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(searchUrl))
                .header("Accept", "application/json")
                .header("X-Subscription-Token", braveApiKey)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return createMockSearchResults(query, count);
            }

            return parseBraveSearchResults(response.body(), query);

        } catch (Exception e) {
            return createMockSearchResults(query, count);
        }
    }

    @SuppressWarnings("unchecked")
    private Object parseBraveSearchResults(String json, String query) {
        try {
            Map<String, Object> data = parseJson(json);
            List<Map<String, Object>> webResults = (List<Map<String, Object>>) data.get("web");
            List<Map<String, Object>> mixedResults = (List<Map<String, Object>>) data.get("mixed");

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Results for: %s\n\n", query));

            int resultNum = 1;
            List<Object> allResults = new ArrayList<>();

            if (webResults != null) {
                for (Map<String, Object> result : webResults) {
                    String title = (String) result.get("title");
                    String url = (String) result.get("url");
                    String desc = (String) result.get("description");

                    sb.append(String.format("%d. %s\n", resultNum++, title));
                    sb.append(String.format("   URL: %s\n", url));
                    if (desc != null && !desc.isEmpty()) {
                        sb.append(String.format("   %s\n", desc.substring(0, Math.min(200, desc.length()))));
                    }
                    sb.append("\n");

                    Map<String, Object> resultInfo = new HashMap<>();
                    resultInfo.put("title", title);
                    resultInfo.put("url", url);
                    resultInfo.put("description", desc);
                    allResults.add(resultInfo);
                }
            }

            Map<String, Object> searchResult = new HashMap<>();
            searchResult.put("query", query);
            searchResult.put("totalResults", allResults.size());
            searchResult.put("results", allResults);
            searchResult.put("formatted", sb.toString());

            return searchResult;

        } catch (Exception e) {
            return createMockSearchResults(query, 10);
        }
    }

    private Object createMockSearchResults(String query, int count) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Results for: %s\n\n", query));
        sb.append("(Web search requires Brave API key - set tools.web.search.api_key)\n\n");

        for (int i = 1; i <= Math.min(count, 3); i++) {
            sb.append(String.format("%d. Search Result %d\n", i, i));
            sb.append(String.format("   URL: https://example%d.com\n", i));
            sb.append(String.format("   Description: This is a placeholder result for '%s'\n\n", query));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("query", query);
        result.put("totalResults", 0);
        result.put("results", List.of());
        result.put("formatted", sb.toString());

        return result;
    }

    private String extractContent(String html, String mode) {
        if ("text".equals(mode)) {
            return html.replaceAll("(?s)<script[^>]*>.*?</script>", "")
                       .replaceAll("(?s)<style[^>]*>.*?</style>", "")
                       .replaceAll("<[^>]+>", "")
                       .replaceAll("\\s+", " ")
                       .trim();
        }

        // Markdown extraction with basic readability
        String markdown = html;

        // Extract title
        String title = extractTitle(html);

        // Remove scripts and styles
        markdown = markdown.replaceAll("(?s)<script[^>]*>.*?</script>", "");
        markdown = markdown.replaceAll("(?s)<style[^>]*>.*?</style>", "");

        // Convert basic HTML to markdown
        markdown = markdown.replaceAll("<h1[^>]*>(.*?)</h1>", "\n# $1\n\n");
        markdown = markdown.replaceAll("<h2[^>]*>(.*?)</h2>", "\n## $1\n\n");
        markdown = markdown.replaceAll("<h3[^>]*>(.*?)</h3>", "\n### $1\n\n");
        markdown = markdown.replaceAll("<p[^>]*>(.*?)</p>", "$1\n\n");
        markdown = markdown.replaceAll("<br\\s*/?>", "\n");
        markdown = markdown.replaceAll("<a[^>]*href=\"([^\"]*)\"[^>]*>(.*?)</a>", "[$2]($1)");
        markdown = markdown.replaceAll("<strong[^>]*>(.*?)</strong>", "**$1**");
        markdown = markdown.replaceAll("<b[^>]*>(.*?)</b>", "**$1**");
        markdown = markdown.replaceAll("<em[^>]*>(.*?)</em>", "*$1*");
        markdown = markdown.replaceAll("<i[^>]*>(.*?)</i>", "*$1*");
        markdown = markdown.replaceAll("<li[^>]*>(.*?)</li>", "- $1\n");
        markdown = markdown.replaceAll("<code[^>]*>(.*?)</code>", "`$1`");
        markdown = markdown.replaceAll("<pre[^>]*>(.*?)</pre>", "\n```\n$1\n```\n");

        // Clean up
        markdown = markdown.replaceAll("<[^>]+>", "");
        markdown = markdown.replaceAll("\\n{3,}", "\n\n");
        markdown = markdown.replaceAll("[ \t]+", " ");

        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            sb.append("# ").append(title).append("\n\n");
        }
        sb.append(markdown.trim());

        return sb.toString();
    }

    private String extractTitle(String html) {
        // Try Open Graph title first
        String ogTitle = extractMetaContent(html, "og:title");
        if (ogTitle != null) return ogTitle;

        // Try regular title tag
        String titleMatch = html.replaceAll("(?s).*<title[^>]*>(.*?)</title>.*", "$1");
        if (!titleMatch.equals(html)) {
            return titleMatch.trim();
        }

        return null;
    }

    private String extractMetaContent(String html, String property) {
        String pattern = String.format("(?s).*<meta[^>]*(?:property|name)=[\"']%s[\"'][^>]*content=[\"']([^\"']*)[\"'][^>]*>.*", property);
        String match = html.replaceAll(pattern, "$1");
        if (!match.equals(html)) {
            return match.trim();
        }
        return null;
    }

    private Map<String, Object> createResult(String url, String finalUrl, int status,
                                             String content, String error) {
        Map<String, Object> result = new HashMap<>();
        result.put("url", url);
        result.put("finalUrl", finalUrl != null ? finalUrl : url);
        result.put("status", status);
        result.put("length", content != null ? content.length() : 0);
        result.put("truncated", false);
        result.put("text", error != null ? error : (content != null ? content : ""));
        result.put("error", error);

        return result;
    }

    private String getStringArg(Map<String, Object> args, String key, String errorMsg) {
        Object value = args.get(key);
        if (value == null) {
            throw new IllegalArgumentException(errorMsg);
        }
        return value.toString();
    }

    private int getIntArg(Map<String, Object> args, String key, int defaultValue) {
        Object value = args.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
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
