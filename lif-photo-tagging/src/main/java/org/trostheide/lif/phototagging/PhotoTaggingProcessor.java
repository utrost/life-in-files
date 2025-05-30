package org.trostheide.lif.phototagging;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main processor for photo tagging workflow.
 * Traverses directories, resizes images, calls LLM, and writes metadata.
 */
public class PhotoTaggingProcessor {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".jpg", ".jpeg");

    public static void run(PhotoTaggingConfig config) {
        System.out.println("Starting lif-photo-tagging...");
        System.out.println("Input: " + config.getInputDir());

        try {
            List<Path> imagesToProcess = collectEligibleImages(config);
            System.out.println("Found " + imagesToProcess.size() + " image(s) to process.");

            AtomicInteger processed = new AtomicInteger();
            AtomicInteger skipped = new AtomicInteger();
            AtomicInteger errors = new AtomicInteger();

            for (Path image : imagesToProcess) {
                try {
                    System.out.println("Processing: " + image);

                    // Resize original image
                    Path thumbnail = ThumbnailUtils.resizeToTemp(image, config.getThumbnailWidth());

                    // Query LLM with thumbnail image
                    LLMResult result = LLMUtils.queryLLM(thumbnail, config);

                    // Show result for now
                    System.out.println("Description: " + result.getDescription());
                    System.out.println("Tags: " + result.getTags());

                    // TODO: Write result to YAML/IPTC/CSV

                    Files.deleteIfExists(thumbnail);
                    processed.incrementAndGet();

                } catch (Exception e) {
                    System.err.println("Error processing " + image + ": " + e.getMessage());
                    errors.incrementAndGet();
                }
            }

            // Final summary
            System.out.println("\n--- Processing Summary ---");
            System.out.println("Total images:   " + imagesToProcess.size());
            System.out.println("Processed:      " + processed.get());
            System.out.println("Skipped/errors: " + errors.get());

        } catch (IOException e) {
            System.err.println("Failed to scan input directory: " + e.getMessage());
        }
    }

    private static List<Path> collectEligibleImages(PhotoTaggingConfig config) throws IOException {
        try (Stream<Path> stream = Files.walk(config.getInputDir())) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> hasSupportedExtension(path.getFileName().toString()))
                    .filter(path -> isAfterSinceDate(path, config.getSinceDate()))
                    .collect(Collectors.toList());
        }
    }

    private static boolean hasSupportedExtension(String filename) {
        String lower = filename.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private static boolean isAfterSinceDate(Path file, LocalDate sinceDate) {
        if (sinceDate == null) return true;
        try {
            Instant fileInstant = Files.getLastModifiedTime(file).toInstant();
            LocalDate fileDate = fileInstant.atZone(ZoneId.systemDefault()).toLocalDate();
            return fileDate.isAfter(sinceDate);
        } catch (IOException e) {
            return false; // conservatively skip if date cannot be read
        }
    }
}
