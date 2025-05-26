package org.trostheide.lif.photoorg;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Reads a JPEG file and optionally resizes it to a specified maximum long side length.
 * EXIF metadata will be preserved separately by the PhotoProcessor via ExifPreservingWriter.
 */
public class JpegDecoder implements PhotoDecoder {
    private final int longSide;

    /**
     * @param longSide maximum length of the longer side in pixels; non-positive means no resize
     */
    public JpegDecoder(int longSide) {
        this.longSide = longSide;
    }

    @Override
    public BufferedImage decode(File srcFile) throws Exception {
        // 1) Load the JPEG into memory
        BufferedImage img = ImageIO.read(srcFile);
        if (img == null) {
            throw new IllegalStateException("Failed to read JPEG file: " + srcFile.getAbsolutePath());
        }

        // 2) Resize if a longSide limit is specified
        if (longSide > 0) {
            ThumbnailatorResizer resizer = new ThumbnailatorResizer();
            img = resizer.resize(img, longSide, longSide);
        }

        return img;
    }
}
