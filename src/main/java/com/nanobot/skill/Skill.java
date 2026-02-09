package com.nanobot.skill;

import java.util.*;

/**
 * Skill Definition
 */
public class Skill {
    private String name;
    private String description;
    private String content;
    private List<String> tools = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getTools() { return tools; }
    public Map<String, Object> getMetadata() { return metadata; }

    public String getPrompt() {
        return content != null ? content : description;
    }
}
