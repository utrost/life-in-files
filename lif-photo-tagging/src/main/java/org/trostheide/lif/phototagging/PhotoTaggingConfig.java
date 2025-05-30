package org.trostheide.lif.phototagging;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Configuration class for lif-photo-tagging.
 * Populated from CLI arguments or optional YAML config file.
 */
public class PhotoTaggingConfig {

    private Path inputDir;
    private LocalDate sinceDate;

    private String apiEndpoint = "http://localhost:11434/api/generate";
    private String model = "gemma3:4b";
    private String prompt = "Describe this image briefly and provide a list of relevant tags.";
    ;

    private int thumbnailWidth = 512;
    private String tagList;

    private boolean dryRun = false;
    private boolean update = false;
    private boolean rerun = false;

    private Path logFilePath;

    // --- Getters and Setters ---

    public Path getInputDir() {
        return inputDir;
    }

    public void setInputDir(Path inputDir) {
        this.inputDir = inputDir;
    }

    public LocalDate getSinceDate() {
        return sinceDate;
    }

    public void setSinceDate(LocalDate sinceDate) {
        this.sinceDate = sinceDate;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public int getThumbnailWidth() {
        return thumbnailWidth;
    }

    public void setThumbnailWidth(int thumbnailWidth) {
        if (thumbnailWidth > 0) {
            this.thumbnailWidth = thumbnailWidth;
        }
    }

    public String getTagList() {
        return tagList;
    }

    public void setTagList(String tagList) {
        this.tagList = tagList;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public boolean isRerun() {
        return rerun;
    }

    public void setRerun(boolean rerun) {
        this.rerun = rerun;
    }

    public Path getLogFilePath() {
        return logFilePath;
    }

    public void setLogFilePath(Path logFilePath) {
        this.logFilePath = logFilePath;
    }

    @Override
    public String toString() {
        return "PhotoTaggingConfig{" +
                "inputDir=" + inputDir +
                ", sinceDate=" + sinceDate +
                ", apiEndpoint='" + apiEndpoint + '\'' +
                ", model='" + model + '\'' +
                ", prompt='" + prompt + '\'' +
                ", thumbnailWidth=" + thumbnailWidth +
                ", tagList='" + tagList + '\'' +
                ", dryRun=" + dryRun +
                ", update=" + update +
                ", rerun=" + rerun +
                ", logFilePath=" + logFilePath +
                '}';
    }
}
