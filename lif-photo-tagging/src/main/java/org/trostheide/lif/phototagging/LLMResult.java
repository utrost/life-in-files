package org.trostheide.lif.phototagging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of LLM-based image tagging.
 */
public class LLMResult {

    private String description;
    private List<String> tags = new ArrayList<>();
    private double confidence;

    public LLMResult() {
    }

    public LLMResult(String description, List<String> tags, double confidence) {
        this.description = description;
        this.tags = tags;
        this.confidence = confidence;
    }

    public static LLMResult fromJson(JsonNode root) {
        try {
            if (root.has("message") && root.get("message").has("content")) {
                String content = root.get("message").get("content").asText();

                // Parse the content string as JSON
                ObjectMapper mapper = new ObjectMapper();
                JsonNode contentNode = mapper.readTree(content);

                String description = contentNode.has("description") ? contentNode.get("description").asText() : "";
                List<String> tags = new ArrayList<>();

                if (contentNode.has("tags") && contentNode.get("tags").isArray()) {
                    for (JsonNode tagNode : contentNode.get("tags")) {
                        tags.add(tagNode.asText());
                    }
                }

                return new LLMResult(description, tags, 1.0);
            } else {
                System.err.println("⚠Unexpected response format (no 'message.content')");
            }
        } catch (Exception e) {
            System.err.println("Failed to parse LLM JSON response: " + e.getMessage());
        }

        return new LLMResult("", List.of(), 0.0);
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return "LLMResult{" +
                "description='" + description + '\'' +
                ", tags=" + tags +
                ", confidence=" + confidence +
                '}';
    }

    public static LLMResult fromRawText(String text) {
        String[] parts = text.split("(?i)Tags?:", 2);  // case-insensitive split
        String description = parts[0].replaceAll("\\*\\*", "").trim(); // remove bold markers
        List<String> tags = new ArrayList<>();

        if (parts.length > 1) {
            String tagBlock = parts[1];
            // Split by lines or commas
            String[] lines = tagBlock.split("[\\r\\n,]");
            for (String line : lines) {
                String tag = line.replaceAll("[\\*\\-]", "").trim(); // remove * and - bullets
                if (!tag.isEmpty()) {
                    tags.add(tag);
                }
            }
        }

        return new LLMResult(description, tags, 1.0);
    }


}
