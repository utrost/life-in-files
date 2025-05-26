package org.trostheide.lif.photoorg;

import net.coobird.thumbnailator.Thumbnails;
import java.awt.image.BufferedImage;

/**
 * Resizes a BufferedImage using Thumbnailator's high-quality scaling.
 */
public class ThumbnailatorResizer implements ImageResizer {
    @Override
    public BufferedImage resize(BufferedImage input, int maxWidth, int maxHeight) throws Exception {
        // Maintains aspect ratio and uses multi-step scaling under the hood
        return Thumbnails.of(input)
                .size(maxWidth, maxHeight)
                .asBufferedImage();
    }
}
