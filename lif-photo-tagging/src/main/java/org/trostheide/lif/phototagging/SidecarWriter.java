package org.trostheide.lif.phototagging;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SidecarWriter {

    public static void writeYaml(Path imagePath, LLMResult result, PhotoTaggingConfig config) throws IOException {
        Path yamlPath = imagePath.resolveSibling(
                imagePath.getFileName().toString().replaceFirst("\\.[^.]+$", "") + ".yaml"
        );

        try (Writer writer = Files.newBufferedWriter(yamlPath)) {
            writer.write("description: " + quote(result.getDescription()) + "\n");
            writer.write("tags:\n");
            for (String tag : result.getTags()) {
                writer.write("  - " + quote(tag) + "\n");
            }
            writer.write("timestamp: " + ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "\n");
            writer.write("source_image: " + quote(imagePath.getFileName().toString()) + "\n");
            writer.write("full_path: " + quote(imagePath.toAbsolutePath().toString()) + "\n");
            writer.write("model: " + quote(config.getModel()) + "\n");
        }
    }

    private static String quote(String text) {
        if (text == null) return "\"\"";
        return "\"" + text.replace("\"", "'") + "\"";
    }
}
