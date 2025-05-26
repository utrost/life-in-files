package org.trostheide.lif.photoorg;

import java.awt.image.BufferedImage;
import java.io.File;

// Decoder interface
public interface PhotoDecoder {
    BufferedImage decode(File rawFile) throws Exception;
}
