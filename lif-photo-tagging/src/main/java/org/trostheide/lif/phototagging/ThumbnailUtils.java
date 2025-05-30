package org.trostheide.lif.phototagging;

import net.coobird.thumbnailator.Thumbnails;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for resizing images to temporary thumbnails
 * for LLM processing. Uses Thumbnailator for efficient JPEG resizing.
 *
 * Note: The caller is responsible for deleting the returned temporary file.
 */
public class ThumbnailUtils {

    /**
     * Resizes a JPEG image to the specified width, preserving aspect ratio,
     * and stores it in the system temp folder as a temporary file.
     *
     * The returned file will not be automatically deleted. Caller must clean up.
     *
     * @param originalImage Path to the original JPEG image
     * @param width Target width in pixels (height is auto-scaled)
     * @return Path to the resized temporary image
     * @throws IOException if resizing fails
     */
    public static Path resizeToTemp(Path originalImage, int width) throws IOException {
        Path tempFile = Files.createTempFile("lif-thumb-", ".jpg");

        Thumbnails.of(originalImage.toFile())
                .width(width)
                .outputFormat("jpg")
                .outputQuality(1.0f)
                .toFile(tempFile.toFile());

        return tempFile;
    }
}
