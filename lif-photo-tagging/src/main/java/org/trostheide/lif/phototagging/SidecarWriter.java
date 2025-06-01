package org.trostheide.lif.phototagging;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class SidecarWriter {

    /**
     * Writes the YAML sidecar file containing LLM result and optional image metadata.
     *
     * @param photoPath original image file
     * @param result    LLM result (description and tags)
     * @param config    configuration containing model info and flags
     * @throws IOException if writing fails
     */
    public static void writeYaml(Path photoPath, LLMResult result, PhotoTaggingConfig config) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();

        root.put("filename", photoPath.getFileName().toString());
        root.put("path", photoPath.toAbsolutePath().toString());
        root.put("description", result.getDescription());
        root.put("tags", result.getTags());

        Map<String, Object> llmInfo = new LinkedHashMap<>();
        llmInfo.put("model", config.getModel());
        llmInfo.put("endpoint", config.getApiEndpoint());
        llmInfo.put("processed", ZonedDateTime.now().toString());
        root.put("llm", llmInfo);

        // Optionally embed metadata block
        if (config.isEmbedMetadata()) {
            try {
                Map<String, Map<String, String>> extracted = MetadataExtractor.extractMetadata(photoPath.toFile());
                if (!extracted.isEmpty()) {
                    root.put("metadata", extracted);
                }
            } catch (IOException e) {
                System.err.println("⚠Could not extract EXIF metadata: " + e.getMessage());
            }
        }

        File yamlFile = new File(photoPath.toAbsolutePath().toString().replaceAll("\\.jpe?g$", "") + ".yaml");

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        Representer representer = new Representer(options);
        representer.addClassTag(LinkedHashMap.class, Tag.MAP);

        try (FileWriter writer = new FileWriter(yamlFile)) {
            new Yaml(representer, options).dump(root, writer);
        }

        // Optional: embed tags + description into the photo metadata
        if (config.isEmbedMetadata()) {
            MetadataEmbedder.writeDescriptionAndTags(photoPath.toFile(), result.getDescription(), result.getTags());
        }
    }
}