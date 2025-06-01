package org.trostheide.lif.phototagging;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Extracts EXIF metadata from JPEG image files using Apache Commons Imaging.
 */
public class MetadataExtractor {

    /**
     * Extracts available EXIF fields from the given JPEG image.
     *
     * @param imageFile the image file to inspect
     * @return a nested Map with key "exif", mapping to a sub-map of tags and values
     * @throws IOException if reading metadata fails
     */
    public static Map<String, Map<String, String>> extractMetadata(File imageFile) throws IOException {
        Map<String, Map<String, String>> result = new HashMap<>();
        Map<String, String> exifData = new HashMap<>();

        try {
            Object metadata = Imaging.getMetadata(imageFile);
            if (metadata instanceof JpegImageMetadata jpegMetadata) {
                TiffImageMetadata exif = jpegMetadata.getExif();
                if (exif != null) {
                    for (TiffField field : exif.getAllFields()) {
                        String name = field.getTagName();
                        String value = field.getValueDescription();
                        if (value != null && !value.isBlank()) {
                            // Remove double quotes from value string
                            exifData.put(name, value.replace("\"", ""));
                        }
                    }
                }
            }
        } catch (ImageReadException e) {
            throw new IOException("Failed to extract metadata from " + imageFile.getName(), e);
        }

        if (!exifData.isEmpty()) {
            result.put("exif", exifData);
        }

        // IPTC extraction could be added here later

        return result;
    }
}
