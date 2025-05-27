package org.trostheide.lif.phototagging;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class PhotoTaggingConfig {

    private Path inputDirectory;
    private LocalDate sinceDate; // optional, can be null
    private String ollamaEndpoint = "http://localhost:11434";
    private int thumbnailWidth = 512;
    private Path tagVocabularyFile; // optional
    private boolean updateMode = false;
    private boolean rerunMode = false;
    private Path logCsvFile; // optional
    private boolean dryRun = false;

    // --- Getters and Setters ---

    public Path getInputDirectory() { return inputDirectory; }
    public void setInputDirectory(Path inputDirectory) { this.inputDirectory = inputDirectory; }

    public LocalDate getSinceDate() { return sinceDate; }
    public void setSinceDate(LocalDate sinceDate) { this.sinceDate = sinceDate; }

    public String getOllamaEndpoint() { return ollamaEndpoint; }
    public void setOllamaEndpoint(String ollamaEndpoint) { this.ollamaEndpoint = ollamaEndpoint; }

    public int getThumbnailWidth() { return thumbnailWidth; }
    public void setThumbnailWidth(int thumbnailWidth) { this.thumbnailWidth = thumbnailWidth; }

    public Path getTagVocabularyFile() { return tagVocabularyFile; }
    public void setTagVocabularyFile(Path tagVocabularyFile) { this.tagVocabularyFile = tagVocabularyFile; }

    public boolean isUpdateMode() { return updateMode; }
    public void setUpdateMode(boolean updateMode) { this.updateMode = updateMode; }

    public boolean isRerunMode() { return rerunMode; }
    public void setRerunMode(boolean rerunMode) { this.rerunMode = rerunMode; }

    public Path getLogCsvFile() { return logCsvFile; }
    public void setLogCsvFile(Path logCsvFile) { this.logCsvFile = logCsvFile; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    // --- CLI Parsing Utility ---

    /**
     * Simple CLI arg parser for demonstration.
     * Supports:
     *   --input <dir>
     *   --since <YYYY-MM-DD>
     *   --ollama-endpoint <url>
     *   --thumbnail-width <n>
     *   --tag-vocabulary <file>
     *   --update
     *   --rerun
     *   --log <csv-file>
     *   --dry-run
     */
    public static PhotoTaggingConfig fromArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length; ++i) {
            String key = args[i];
            if (key.startsWith("--")) {
                if ((i + 1) < args.length && !args[i + 1].startsWith("--")) {
                    params.put(key, args[++i]);
                } else {
                    params.put(key, "true");
                }
            }
        }

        PhotoTaggingConfig config = new PhotoTaggingConfig();
        if (params.containsKey("--input")) {
            config.setInputDirectory(Paths.get(params.get("--input")));
        } else {
            System.err.println("Missing required --input <dir> argument.");
            System.exit(1);
        }
        if (params.containsKey("--since")) {
            config.setSinceDate(LocalDate.parse(params.get("--since")));
        }
        if (params.containsKey("--ollama-endpoint")) {
            config.setOllamaEndpoint(params.get("--ollama-endpoint"));
        }
        if (params.containsKey("--thumbnail-width")) {
            config.setThumbnailWidth(Integer.parseInt(params.get("--thumbnail-width")));
        }
        if (params.containsKey("--tag-vocabulary")) {
            config.setTagVocabularyFile(Paths.get(params.get("--tag-vocabulary")));
        }
        if (params.containsKey("--update")) {
            config.setUpdateMode(true);
        }
        if (params.containsKey("--rerun")) {
            config.setRerunMode(true);
        }
        if (params.containsKey("--log")) {
            config.setLogCsvFile(Paths.get(params.get("--log")));
        }
        if (params.containsKey("--dry-run")) {
            config.setDryRun(true);
        }
        return config;
    }
}
