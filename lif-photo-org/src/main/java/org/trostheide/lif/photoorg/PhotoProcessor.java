package org.trostheide.lif.photoorg;

import org.trostheide.lif.core.LoggerService;
import org.trostheide.lif.core.LifIndexManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Processes a source image file: decode, resize (if requested), write
 * to the mirrored target directory, and update the index.
 * Now skips files whose JPEG already exists in the target.
 */
public class PhotoProcessor {
    private final Path sourceRoot;
    private final Path targetRoot;
    private final String mode;
    private final LoggerService logger;
    private final LifIndexManager indexMgr;
    private final PhotoDecoder decoder;
    private final ImageResizer resizer;
    private final int longSide;

    public PhotoProcessor(
            File sourceRoot,
            File targetRoot,
            String mode,
            LoggerService logger,
            LifIndexManager indexMgr,
            PhotoDecoder decoder,
            ImageResizer resizer,
            int longSide
    ) {
        this.sourceRoot = sourceRoot.toPath();
        this.targetRoot = targetRoot.toPath();
        this.mode       = mode;
        this.logger     = logger;
        this.indexMgr   = indexMgr;
        this.decoder    = decoder;
        this.resizer    = resizer;
        this.longSide   = longSide;
    }

    /**
     * Process a single source file:
     * 1) Determine mirrored output path
     * 2) Skip if already exists
     * 3) Decode→resize→write JPEG
     * 4) Update index
     */
    public void process(File srcFile) throws Exception {
        // 1) Compute relative directory under sourceRoot
        Path srcPath = srcFile.toPath();
        Path relDir  = sourceRoot.relativize(srcPath.getParent());

        // 2) Build target directory and output file path
        Path outDir  = targetRoot.resolve(relDir);
        String baseName = srcFile.getName().replaceAll("\\.[^.]+$", "");
        Path outFilePath = outDir.resolve(baseName + ".jpg");
        File outFile = outFilePath.toFile();

        // 3) Skip if target already exists
        if (outFile.exists()) {
            logger.info("Skipping existing: " + outFile.getAbsolutePath());
            return;
        }

        // 4) Ensure target directory exists
        Files.createDirectories(outDir);

        // 5) Decode (RAW → BufferedImage or JPEG → BufferedImage)
        BufferedImage img = decoder.decode(srcFile);

        // 6) Resize if requested
        if (longSide > 0) {
            img = resizer.resize(img, longSide, longSide);
        }

        // 7) Write output JPEG
        ImageIO.write(img, "jpg", outFile);
        logger.info("Wrote: " + outFile.getAbsolutePath());

        // 8) Update processing index
        indexMgr.readIndex(); // load existing entries
        indexMgr.writeIndexEntry(srcFile, outFile); // append new entry
    }
}
