package com.nanobot.skill;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Skill Loader - Loads skills from markdown files
 */
public class SkillLoader {
    private final Path skillsPath;
    private final Map<String, Skill> skills = new HashMap<>();

    public SkillLoader(Path skillsPath) {
        this.skillsPath = skillsPath;
    }

    public void load() throws IOException {
        if (!Files.exists(skillsPath)) {
            return;
        }

        try (var stream = Files.walk(skillsPath)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".md"))
                  .forEach(this::loadSkill);
        }
    }

    private void loadSkill(Path path) {
        try {
            String content = Files.readString(path);
            Skill skill = parseSkill(path.getFileName().toString(), content);
            skills.put(skill.getName(), skill);
        } catch (Exception e) {
            System.err.println("Failed to load skill: " + path + " - " + e.getMessage());
        }
    }

    private Skill parseSkill(String filename, String content) {
        Skill skill = new Skill();
        skill.setName(filename.replace(".md", ""));

        // Parse YAML front matter
        if (content.startsWith("---")) {
            int end = content.indexOf("---", 3);
            if (end > 0) {
                String yaml = content.substring(3, end);
                parseYamlFrontMatter(skill, yaml);
                skill.setContent(content.substring(end + 3).trim());
            }
        } else {
            skill.setContent(content);
        }

        return skill;
    }

    private void parseYamlFrontMatter(Skill skill, String yaml) {
        String[] lines = yaml.split("\n");
        for (String line : lines) {
            if (line.startsWith("name:")) {
                skill.setName(line.substring(5).trim());
            } else if (line.startsWith("description:")) {
                skill.setDescription(line.substring(12).trim());
            } else if (line.startsWith("tools:")) {
                // Parse tools list
            }
        }
    }

    public Skill getSkill(String name) {
        return skills.get(name);
    }

    public Collection<Skill> getAllSkills() {
        return skills.values();
    }

    public List<String> getSkillNames() {
        return new ArrayList<>(skills.keySet());
    }
}
