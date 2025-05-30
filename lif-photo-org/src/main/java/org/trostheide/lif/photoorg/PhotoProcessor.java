package org.trostheide.lif.photoorg;

import org.trostheide.lif.core.LoggerService;
import org.trostheide.lif.core.LifIndexManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Processes a source image or video file by:
 *  1) Determining output directory (mirror structure or date-based)
 *  2) Skipping if the output already exists
 *  3) Delegating to DarktableDecoder.convertTo(...) for raw mode (preserves EXIF)
 *     or to decoder.decode(...) + ImageIO/Exif for jpeg mode
 *  4) Appending an entry to .lif-index.json
 */
public class PhotoProcessor {
    private final Path sourceRoot;
    private final Path targetRoot;
    private final LoggerService logger;
    private final LifIndexManager indexMgr;
    private final PhotoDecoder decoder;
    private final boolean dateOrder;

    public PhotoProcessor(
            File sourceRootDir,
            File targetRootDir,
            LoggerService logger,
            LifIndexManager indexMgr,
            PhotoDecoder decoder,
            boolean dateOrder
    ) {
        this.sourceRoot = sourceRootDir.toPath();
        this.targetRoot = targetRootDir.toPath();
        this.logger     = logger;
        this.indexMgr   = indexMgr;
        this.decoder    = decoder;
        this.dateOrder  = dateOrder;
    }

    public void process(File srcFile) {
        try {
            // 1) Determine output directory
            Path outDir;
            if (dateOrder) {
                // use creation date
                BasicFileAttributes attrs = Files.readAttributes(
                        srcFile.toPath(), BasicFileAttributes.class);
                Instant created = attrs.creationTime().toInstant();
                LocalDate d = created.atZone(ZoneId.systemDefault()).toLocalDate();
                outDir = targetRoot
                        .resolve(String.valueOf(d.getYear()))
                        .resolve(String.format("%02d", d.getMonthValue()))
                        .resolve(String.format("%02d", d.getDayOfMonth()));
            } else {
                Path rel = sourceRoot.relativize(srcFile.toPath().getParent());
                outDir = targetRoot.resolve(rel);
            }
            Files.createDirectories(outDir);

            // 2) Compute output file path
            String baseName = srcFile.getName().replaceAll("\\.[^.]+$", "");
            Path outFilePath = outDir.resolve(baseName + ".jpg");
            File outFile = outFilePath.toFile();

            // 3) Skip if exists
            if (outFile.exists()) {
                logger.info("Skipping existing: " + outFile.getAbsolutePath());
                return;
            }

            logger.info("Processing: " + srcFile.getAbsolutePath());

            // 4) Raw mode? let Darktable write the JPEG directly (preserving EXIF)
            if (decoder instanceof DarktableDecoder) {
                ((DarktableDecoder) decoder).convertTo(srcFile, outFilePath);
                logger.info("Wrote (raw): " + outFile.getAbsolutePath());

            } else {
                // JPEG mode: decode/resizer/Exif writer
                BufferedImage img = decoder.decode(srcFile);
                ImageIO.write(img, "jpg", outFile);
                logger.info("Wrote (jpeg): " + outFile.getAbsolutePath());
            }

            // 5) Record in index
            indexMgr.writeIndexEntry(srcFile, outFile);

        } catch (Exception e) {
            logger.error("Failed processing " + srcFile.getAbsolutePath(), e);
        }
    }
}
