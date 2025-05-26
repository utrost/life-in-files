package org.trostheide.lif.photoorg;

import java.awt.image.BufferedImage;

// Resizer interface
public interface ImageResizer {
    BufferedImage resize(BufferedImage input, int maxWidth, int maxHeight) throws Exception;
}
