package org.trostheide.lif.phototagging;

import java.util.List;

public class LLMResult {
    private String description;
    private List<String> tags;
    private double confidence; // Optional, can be used for model output quality

    // Constructor
    public LLMResult(String description, List<String> tags, double confidence) {
        this.description = description;
        this.tags = tags;
        this.confidence = confidence;
    }

    // Getters and setters
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    @Override
    public String toString() {
        return "LLMResult{" +
                "description='" + description + '\'' +
                ", tags=" + tags +
                ", confidence=" + confidence +
                '}';
    }
}
