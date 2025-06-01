package org.trostheide.lif.phototagging;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.bytesource.ByteSourceFile;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.jpeg.iptc.IptcRecord;
import org.apache.commons.imaging.formats.jpeg.iptc.IptcTypes;
import org.apache.commons.imaging.formats.jpeg.iptc.PhotoshopApp13Data;
import org.apache.commons.imaging.formats.jpeg.iptc.JpegIptcRewriter;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class MetadataEmbedder {

    /**
     * Embeds description and tags into JPEG EXIF and IPTC metadata using Apache Commons Imaging.
     *
     * @param imageFile   the JPEG image file to modify (in-place)
     * @param description the description text to embed
     * @param tags        a list of tags to embed as IPTC keywords and EXIF comment
     * @throws IOException if reading or writing the image fails
     */
    public static void writeDescriptionAndTags(File imageFile, String description, List<String> tags) throws IOException {
        File tempExifFile = File.createTempFile("updated-exif-", ".jpg");

        try {
            // --- Embed EXIF ---
            TiffOutputSet outputSet;
            var metadata = Imaging.getMetadata(imageFile);
            var jpegMetadata = (metadata instanceof JpegImageMetadata) ? (JpegImageMetadata) metadata : null;

            if (jpegMetadata != null && jpegMetadata.getExif() != null) {
                outputSet = jpegMetadata.getExif().getOutputSet();
            } else {
                outputSet = new TiffOutputSet();
            }

            TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();

            exifDirectory.removeField(TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION);
            exifDirectory.removeField(ExifTagConstants.EXIF_TAG_USER_COMMENT);

            if (description != null && !description.isBlank()) {
                exifDirectory.add(TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION, description);
            }

            if (tags != null && !tags.isEmpty()) {
                String keywords = String.join(", ", tags);
                exifDirectory.add(ExifTagConstants.EXIF_TAG_USER_COMMENT, keywords);
            }

            try (
                    OutputStream os = new FileOutputStream(tempExifFile);
                    FileInputStream fis = new FileInputStream(imageFile)
            ) {
                new ExifRewriter().updateExifMetadataLossless(new ByteSourceFile(imageFile), os, outputSet);
            }

            // --- Embed IPTC (Caption and Keywords) ---
            File finalOutput = File.createTempFile("final-", ".jpg");
            PhotoshopApp13Data existingIptc = (jpegMetadata != null &&
                    jpegMetadata.getPhotoshop() != null)
                    ? jpegMetadata.getPhotoshop().photoshopApp13Data
                    : null;

            List<IptcRecord> newRecords = new ArrayList<>();
            if (existingIptc != null) {
                for (IptcRecord rec : existingIptc.getRecords()) {
                    if (!rec.iptcType.equals(IptcTypes.CAPTION_ABSTRACT) &&
                            !rec.iptcType.equals(IptcTypes.KEYWORDS)) {
                        newRecords.add(rec);
                    }
                }
            }

            if (description != null && !description.isBlank()) {
                newRecords.add(new IptcRecord(IptcTypes.CAPTION_ABSTRACT, description));
            }

            if (tags != null && !tags.isEmpty()) {
                for (String tag : tags) {
                    newRecords.add(new IptcRecord(IptcTypes.KEYWORDS, tag));
                }
            }

            PhotoshopApp13Data newIptcData = new PhotoshopApp13Data(
                    newRecords,
                    existingIptc != null ? existingIptc.getNonIptcBlocks() : Collections.emptyList()
            );

            try (
                    InputStream is = new BufferedInputStream(new FileInputStream(tempExifFile));
                    OutputStream os = new BufferedOutputStream(new FileOutputStream(finalOutput))
            ) {
                new JpegIptcRewriter().writeIPTC(is, os, newIptcData);
            }

            // Overwrite original image in-place
            Files.move(finalOutput.toPath(), imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        } catch (ImageReadException | ImageWriteException e) {
            throw new IOException("Failed to embed metadata: " + e.getMessage(), e);
        }
    }
}