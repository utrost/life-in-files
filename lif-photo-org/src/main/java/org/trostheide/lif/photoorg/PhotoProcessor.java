package org.trostheide.lif.photoorg;

import org.trostheide.lif.core.LoggerService;
import org.trostheide.lif.core.LifIndexManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Processes a source image file by mirroring the source tree,
 * skipping already‐existing outputs, and then either:
 *
 *  - RAW mode: delegating conversion+resize+metadata to darktable-cli directly
 *  - JPEG mode: decoding & resizing in-JVM + EXIF re-injection
 *
 * Finally it appends an entry to .lif-index.json.
 */
public class PhotoProcessor {
    private final Path sourceRoot;
    private final Path targetRoot;
    private final LoggerService logger;
    private final LifIndexManager indexMgr;
    private final PhotoDecoder decoder;

    public PhotoProcessor(
            File sourceRootDir,
            File targetRootDir,
            LoggerService logger,
            LifIndexManager indexMgr,
            PhotoDecoder decoder
    ) {
        this.sourceRoot = sourceRootDir.toPath();
        this.targetRoot = targetRootDir.toPath();
        this.logger     = logger;
        this.indexMgr   = indexMgr;
        this.decoder    = decoder;
    }

    /**
     * Process a single file.
     *
     * @param srcFile the input RAW or JPEG file
     */
    public void process(File srcFile) {
        try {
            // 1) Mirror directory structure
            Path srcPath = srcFile.toPath();
            Path relDir  = sourceRoot.relativize(srcPath.getParent());
            Path outDir  = targetRoot.resolve(relDir);
            Files.createDirectories(outDir);

            // 2) Compute final output path
            String baseName      = srcFile.getName().replaceAll("\\.[^.]+$", "");
            Path outFilePath     = outDir.resolve(baseName + ".jpg");
            File outFile         = outFilePath.toFile();

            // 3) Skip if already exists
            if (outFile.exists()) {
                logger.info("Skipping existing: " + outFile.getAbsolutePath());
                return;
            }

            logger.info("Processing: " + srcFile.getAbsolutePath());

            // 4) RAW mode? DarktableDecoder knows how to write straight to outFilePath
            if (decoder instanceof DarktableDecoder) {
                DarktableDecoder dt = (DarktableDecoder) decoder;
                dt.convertTo(srcFile, outFilePath);
                logger.info("Wrote (raw): " + outFile.getAbsolutePath());

            } else {
                // 5) JPEG mode: load, resize, then re‐inject EXIF
                BufferedImage img = decoder.decode(srcFile);
                ExifPreservingWriter.writeJpegWithExif(srcFile, img, outFilePath);
                logger.info("Wrote (jpeg): " + outFile.getAbsolutePath());
            }

            // 6) Update the index
            indexMgr.writeIndexEntry(srcFile, outFile);

        } catch (Exception e) {
            logger.error("Failed processing " + srcFile.getAbsolutePath(), e);
        }
    }
}
