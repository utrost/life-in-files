// src/main/java/org/trostheide/lif/photoorg/JpegDecoder.java
package org.trostheide.lif.photoorg;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Reads a JPEG file and optionally resizes it to a specified maximum long side length,
 * using the given JPEG quality.
 */
public class JpegDecoder implements PhotoDecoder {
    private final int longSide;
    private final int quality;

    /**
     * @param longSide max length of the longer side in pixels (<=0 = no resize)
     * @param quality  JPEG quality percentage (1-100)
     */
    public JpegDecoder(int longSide, int quality) {
        this.longSide = longSide;
        this.quality  = quality;
    }

    @Override
    public BufferedImage decode(File srcFile) throws Exception {
        BufferedImage img = ImageIO.read(srcFile);
        if (img == null) {
            throw new IllegalStateException("Failed to read JPEG: " + srcFile);
        }

        if (longSide > 0) {
            // resize & set output quality
            img = Thumbnails.of(img)
                    .size(longSide, longSide)
                    .outputQuality(quality / 100.0)
                    .asBufferedImage();
        }
        return img;
    }
}
