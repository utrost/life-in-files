package org.trostheide.lif.photoorg;

import org.trostheide.lif.core.LifIndexManager;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public class PhotoProcessor {
    private final Path sourceRoot;
    private final Path targetRoot;
    private final LifIndexManager indexMgr;
    private final PhotoDecoder decoder;
    private final String orderMode;
    private final EventManager eventManager;
    private final DateExtractor dateExtractor = new DateExtractor();

    public PhotoProcessor(
            File sourceRootDir, File targetRootDir, LifIndexManager indexMgr,
            PhotoDecoder decoder, String orderMode, EventManager eventManager
    ) {
        this.sourceRoot = sourceRootDir.toPath();
        this.targetRoot = targetRootDir.toPath();
        this.indexMgr = indexMgr;
        this.decoder = decoder;
        this.orderMode = orderMode;
        this.eventManager = eventManager;
    }

    public void process(File srcFile) {
        try {
            String threadName = Thread.currentThread().getName();
            System.out.println(String.format("[%s] START Processing: %s", threadName, srcFile.getAbsolutePath()));

            Path outDir = determineOutputDir(srcFile, threadName);
            Files.createDirectories(outDir);

            String baseName = srcFile.getName().replaceAll("\\.[^.]+$", "");
            Path outFilePath = outDir.resolve(baseName + ".jpg");
            File outFile = outFilePath.toFile();

            if (outFile.exists()) {
                System.out.println(String.format("[%s] Skipping existing: %s", threadName, outFile.getAbsolutePath()));
                return;
            }

            if (decoder instanceof DarktableDecoder) {
                ((DarktableDecoder) decoder).convertTo(srcFile, outFilePath);
            } else {
                BufferedImage img = decoder.decode(srcFile);
                ExifPreservingWriter.writeJpegWithExif(srcFile, img, outFilePath);
            }
            System.out.println(String.format("[%s] Wrote to: %s", threadName, outFile.getAbsolutePath()));

            indexMgr.writeIndexEntry(srcFile, outFile);

        } catch (Exception e) {
            System.err.println("Failed processing " + srcFile.getAbsolutePath());
            e.printStackTrace(System.err);
        }
    }

    private Path determineOutputDir(File srcFile, String threadName) {
        if ("event".equalsIgnoreCase(orderMode) && eventManager != null) {
            DateExtractor.PathInfo info = dateExtractor.extractPathInfo(srcFile);
            LocalDate photoDate = info.date();
            System.out.println(String.format("[%s] Date for file '%s' is: %s", threadName, srcFile.getName(), photoDate));

            if (photoDate != null) {
                EventManager.Event event = eventManager.findBestEventForDate(photoDate);
                if (event != null) {
                    System.out.println(String.format("[%s] Matched event '%s' (%s - %s) for date %s", threadName, event.name(), event.startDate(), event.endDate(), photoDate));
                    Path finalPath = targetRoot.resolve(String.valueOf(photoDate.getYear())).resolve(String.format("%02d", photoDate.getMonthValue())).resolve(event.name());
                    System.out.println(String.format("[%s] Final path determined by event: %s", threadName, finalPath));
                    return finalPath;
                } else {
                    System.out.println(String.format("[%s] No matching event found for date %s", threadName, photoDate));
                }
            }
        }

        if ("date".equalsIgnoreCase(orderMode) || "event".equalsIgnoreCase(orderMode)) {
            DateExtractor.PathInfo info = dateExtractor.extractPathInfo(srcFile);
            LocalDate d = info.date();
            Path dir = targetRoot.resolve(String.valueOf(d.getYear())).resolve(String.format("%02d", d.getMonthValue()));
            if (info.qualifier() != null) {
                dir = dir.resolve(info.qualifier());
            }
            System.out.println(String.format("[%s] Final path determined by date/path: %s", threadName, dir));
            return dir;
        }

        Path rel = sourceRoot.relativize(srcFile.toPath().getParent());
        Path finalPath = targetRoot.resolve(rel);
        System.out.println(String.format("[%s] Final path determined by structure: %s", threadName, finalPath));
        return finalPath;
    }
}
