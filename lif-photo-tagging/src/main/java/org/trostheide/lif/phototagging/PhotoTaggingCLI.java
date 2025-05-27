package org.trostheide.lif.phototagging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PhotoTaggingCLI {
    private static final Logger logger = LoggerFactory.getLogger(PhotoTaggingCLI.class);

    public static void main(String[] args) {
        // Parse CLI arguments (simple parser)
        PhotoTaggingConfig config = PhotoTaggingConfig.fromArgs(args);

        // Validate input directory
        Path inputDir = config.getInputDirectory();
        if (!Files.exists(inputDir) || !Files.isDirectory(inputDir)) {
            logger.error("Input directory does not exist: {}", inputDir);
            System.exit(1);
        }

        // Create and announce run-specific temp folder for thumbnails
        try {
            ThumbnailUtils.initializeTempDir();
        } catch (IOException e) {
            logger.error("Failed to initialize temporary directory for thumbnails.", e);
            System.exit(1);
        }

        // Discover all .jpg/.jpeg files (recursive)
        List<Path> imageFiles = listJpegFilesRecursive(inputDir, config.getSinceDate());
        logger.info("Discovered {} JPEG files for tagging.", imageFiles.size());

        for (Path imagePath : imageFiles) {
            try {
                logger.info("Processing: {}", imagePath);

                // a) Create temporary thumbnail in run-specific temp folder
                Path thumbnail = ThumbnailUtils.createThumbnail(imagePath, config.getThumbnailWidth(), config.isDryRun());

                // b) Call LLM API for tags & description (mock for now)
                //LLMResult llmResult = LLMUtils.callLLM(thumbnail, config);
                LLMResult llmResult = LLMUtils.callLLM(thumbnail, config);
                logger.info("LLM returned: {}", llmResult);

                // (future steps go here: metadata, YAML, etc.)

                // c) Delete thumbnail after use (unless dry-run)
                if (!config.isDryRun()) {
                  //  ThumbnailUtils.deleteThumbnail(thumbnail);
                }

                logger.info("Finished: {}", imagePath);

            } catch (Exception ex) {
                logger.error("Failed to process: {}", imagePath, ex);
            }
        }

        // Delete temp dir if empty (optional hygiene)
        if (!config.isDryRun()) {
            ThumbnailUtils.deleteTempDirIfEmpty();
        }

        logger.info("lif-photo-tagging complete. Processed: {} images.", imageFiles.size());
    }


    /**
     * Recursively lists all .jpg/.jpeg files under the given directory, optionally filtering by modification date.
     */
    private static List<Path> listJpegFilesRecursive(Path root, LocalDate sinceDate) {
        List<Path> files = new ArrayList<>();
        try {
            Files.walk(root)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String lower = path.getFileName().toString().toLowerCase();
                        boolean isJpeg = lower.endsWith(".jpg") || lower.endsWith(".jpeg");
                        boolean afterDate = true;
                        if (sinceDate != null) {
                            try {
                                afterDate = Files.getLastModifiedTime(path)
                                        .toInstant()
                                        .atZone(java.time.ZoneId.systemDefault())
                                        .toLocalDate()
                                        .isAfter(sinceDate.minusDays(1)); // inclusive
                            } catch (IOException e) {
                                // If we can't read date, skip file
                                return false;
                            }
                        }
                        return isJpeg && afterDate;
                    })
                    .forEach(files::add);
        } catch (IOException e) {
            logger.error("Failed to traverse input directory: {}", root, e);
        }
        return files;
    }
}
