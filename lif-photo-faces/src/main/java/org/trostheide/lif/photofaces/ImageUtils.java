package org.trostheide.lif.photofaces;

import org.slf4j.Logger;
import org.trostheide.lif.core.LoggerService;

/**
 * Helper methods for image IO, cropping, resizing, etc.
 */
public class ImageUtils {
    private static final Logger log = LoggerService.getLogger(ImageUtils.class);
    /**
     * Loads an image file and returns it as a suitable object (e.g., Mat or BufferedImage).
     */
    public static Object loadImage(String path) {
        log.info("Loading image: " + path);
        // TODO: Implement image loading
        return null;
    }

    // Add other static helpers as needed (crop, resize, save, etc.)
}
