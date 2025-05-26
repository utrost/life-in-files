package org.trostheide.lif.photoorg;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;

public class ExifPreservingWriter {
    public static void writeJpegWithExif(File sourceRaw,
                                         BufferedImage img,
                                         Path outputFile) throws Exception {
        // 1) Serialize resized image
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        byte[] jpegBytes = baos.toByteArray();

        // 2) Extract EXIF from source
        TiffOutputSet exif = null;
        ImageMetadata md = Imaging.getMetadata(sourceRaw);
        if (md instanceof JpegImageMetadata) {
            var jpegMd = (JpegImageMetadata) md;
            if (jpegMd.getExif() != null) exif = jpegMd.getExif().getOutputSet();
        } else if (md instanceof TiffImageMetadata) {
            exif = ((TiffImageMetadata) md).getOutputSet();
        }

        // 3) Write out, embedding EXIF if present
        Files.createDirectories(outputFile.getParent());
        try (OutputStream os = Files.newOutputStream(outputFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            if (exif != null) {
                new ExifRewriter()
                        .updateExifMetadataLossy(jpegBytes, os, exif);
            } else {
                os.write(jpegBytes);
            }
        }
    }
}
