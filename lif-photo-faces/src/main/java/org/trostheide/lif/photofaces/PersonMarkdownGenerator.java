package org.trostheide.lif.photofaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.trostheide.lif.core.LoggerService;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.*;

public class PersonMarkdownGenerator {
    private static final Logger log = LoggerService.getLogger(PersonMarkdownGenerator.class);

    public void generateMarkdownFiles(String imageDir, String personDir) {
        File jsonFile = Paths.get(imageDir, "face-clusters.json").toFile();
        ObjectMapper mapper = new ObjectMapper();
        Map<Integer, List<JsonNode>> clusterMap = new HashMap<>();

        // Step 1: Load face clusters and group by cluster label
        try {
            ArrayNode faces = (ArrayNode) mapper.readTree(jsonFile);
            for (JsonNode face : faces) {
                int cluster = face.get("cluster").asInt();
                if (cluster < 0) continue; // Ignore noise/outliers
                clusterMap.computeIfAbsent(cluster, k -> new ArrayList<>()).add(face);
            }
        } catch (Exception e) {
            log.error("Failed to load or parse face-clusters.json", e);
            return;
        }

        // Step 2: For each cluster, write a markdown file as a table
        for (Map.Entry<Integer, List<JsonNode>> entry : clusterMap.entrySet()) {
            int clusterId = entry.getKey();
            List<JsonNode> faces = entry.getValue();
            String mdName = String.format("Person-Cluster-%d.md", clusterId);
            File mdFile = Paths.get(personDir, mdName).toFile();

            try (FileWriter writer = new FileWriter(mdFile)) {
                writer.write("# Person Cluster " + clusterId + "\n\n");
                writer.write("## Associated Faces\n\n");

                // Table header
                writer.write("| Face Crop | Original Image | Include? |\n");
                writer.write("|-----------|----------------|----------|\n");

                for (JsonNode face : faces) {
                    String faceCrop = face.has("face_crop") ? face.get("face_crop").asText() : null;
                    String imagePath = face.get("image").asText();

                    // Face crop: try to resolve as PNG (if present)
                    String faceCropPath = null;
                    if (faceCrop != null) {
                        File cropFile = new File(faceCrop);
                        if (cropFile.isAbsolute()) {
                            faceCropPath = cropFile.getAbsolutePath();
                        } else {
                            faceCropPath = Paths.get(imageDir).resolve(faceCrop).toAbsolutePath().toString();
                        }
                    }
                    // Original image
                    String origImgPath = Paths.get(imagePath).toAbsolutePath().toString();

                    // Relativize for markdown
                    String relCrop = faceCropPath != null ? relativizePath(new File(mdFile.getParent()), faceCropPath).replace("\\", "/") : "";
                    String relOrig = relativizePath(new File(mdFile.getParent()), origImgPath).replace("\\", "/");

                    // Write the table row
                    writer.write(String.format("| ![](%s) | ![](%s) | [ ] |\n",
                            relCrop, relOrig));
                }

                writer.write("\n- [ ] Manually review and assign name if known.\n");
                log.info("Wrote person file: " + mdFile.getAbsolutePath());
            } catch (Exception e) {
                log.error("Failed to write: " + mdFile.getAbsolutePath(), e);
            }
        }
        log.info("All cluster markdown files written to: " + personDir);
    }

    /**
     * Utility to produce a relative path from Markdown file to image/crop.
     */
    private String relativizePath(File fromDir, String toFile) {
        try {
            return fromDir.toPath().toAbsolutePath().relativize(Paths.get(toFile).toAbsolutePath()).toString();
        } catch (Exception e) {
            return toFile;
        }
    }
}
