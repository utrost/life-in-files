package org.trostheide.lif.phototagging;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class PhotoTaggingProcessor {

    public static void run(PhotoTaggingConfig config) {
        List<Path> images = new ArrayList<>();
        Path root = config.getInputDir();

        System.out.println("Scanning directory: " + root);

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    String fileName = path.getFileName().toString().toLowerCase();
                    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
                        if (shouldIncludeFile(path, attrs, config)) {
                            images.add(path);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Failed to walk directory tree: " + e.getMessage());
            return;
        }

        System.out.println("Found " + images.size() + " images to process.\n");

        for (Path photo : images) {
            System.out.println("➡Processing: " + photo);

            try {
                Path thumb = ThumbnailUtils.resizeToTemp(photo, config.getThumbnailWidth());

                LLMResult result = LLMUtils.queryLLM(thumb, config);

                System.out.println("Description: " + result.getDescription());
                System.out.println("Tags: " + result.getTags());

                if (!config.isDryRun()) {
                    SidecarWriter.writeYaml(photo, result, config);
                }

            } catch (Exception e) {
                System.err.println("Error processing " + photo + ": " + e.getMessage());
            }

            System.out.println(); // spacer
        }

        System.out.println("Processing complete.");
    }

    private static boolean shouldIncludeFile(Path path, BasicFileAttributes attrs, PhotoTaggingConfig config) {
        if (config.getSinceDate() != null) {
            LocalDate fileDate = attrs.creationTime()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            if (fileDate.isBefore(config.getSinceDate())) {
                return false;
            }
        }

        Path yamlSidecar = path.resolveSibling(
                path.getFileName().toString().replaceFirst("\\.[^.]+$", "") + ".yaml"
        );

        if (Files.exists(yamlSidecar)) {
            if (config.isUpdate()) {
                return true; // update existing sidecar
            }
            if (!config.isRerun()) {
                return false; // skip if no update or rerun
            }
        }

        return true;
    }
}
