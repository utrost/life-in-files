package org.trostheide.lif.phototagging;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;

public class ThumbnailUtils {
    private static final Logger logger = LoggerFactory.getLogger(ThumbnailUtils.class);
    private static Path runTempDir;

    /** Initializes (once per run) the run-specific temp dir for thumbnails. */
    public static void initializeTempDir() throws IOException {
        if (runTempDir == null) {
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            runTempDir = Files.createTempDirectory("lif-photo-tagging-" + timestamp + "-");
            logger.info("Temporary thumbnail directory: {}", runTempDir);
        }
    }

    /** Creates a thumbnail file for imagePath in the run temp directory. */
    public static Path createThumbnail(Path imagePath, int width, boolean dryRun) throws IOException {
        initializeTempDir();
        String baseName = imagePath.getFileName().toString();
        String thumbName = baseName.replaceAll("\\.(?=[^\\.]+$)", "_thumb.");
        Path tempFile = runTempDir.resolve(thumbName);

        logger.debug("Thumbnail path: {}", tempFile);

        if (dryRun) {
            logger.info("[Dry-Run] Would create thumbnail for: {} at {}", imagePath, tempFile);
            return tempFile;
        }

        try {
            Thumbnails.of(imagePath.toFile())
                    .width(width)
                    .outputQuality(0.8f)
                    .toFile(tempFile.toFile());
            logger.info("Thumbnail created: {}", tempFile);
        } catch (Exception e) {
            logger.error("Failed to create thumbnail for: " + imagePath, e);
            throw new IOException("Thumbnail creation failed", e);
        }

        return tempFile;
    }

    /** Deletes the thumbnail file (unless in dry-run mode). */
    public static void deleteThumbnail(Path thumbnail) {
        try {
            Files.deleteIfExists(thumbnail);
            logger.debug("Deleted thumbnail: {}", thumbnail);
        } catch (IOException e) {
            logger.warn("Could not delete thumbnail: {}", thumbnail, e);
        }
    }

    /** Optionally deletes the temp dir if it is empty. */
    public static void deleteTempDirIfEmpty() {
        try {
            if (runTempDir != null && Files.isDirectory(runTempDir) && Files.list(runTempDir).count() == 0) {
                Files.delete(runTempDir);
                logger.info("Deleted temporary directory: {}", runTempDir);
            }
        } catch (IOException e) {
            logger.warn("Could not delete temporary directory: {}", runTempDir, e);
        }
    }
}
