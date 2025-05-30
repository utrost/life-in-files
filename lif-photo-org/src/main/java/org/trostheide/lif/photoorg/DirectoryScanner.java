package org.trostheide.lif.photoorg;

import org.trostheide.lif.core.LoggerService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Recursively scans a directory for files:
 *  - filtered by creation/modification date (since)
 *  - matching one of the given extensions
 *  - optionally including video files
 */
public class DirectoryScanner {
    private static final Set<String> DEFAULT_IMAGE_EXTS = Set.of(
            "jpg","jpeg","png","tif","tiff",
            "cr2","nef","arw","dng","orf","raf","rw2","pef","srw","kdc"
    );
    private static final Set<String> VIDEO_EXTS = Set.of(
            "mp4","mov","avi","wmf","mkv"
    );

    private final LoggerService logger;
    private final Instant sinceInstant;
    private final Set<String> extensions; // lower-case, without dot

    /**
     * @param logger      for info/error output
     * @param since       ISO-8601 timestamp (e.g. "2025-01-01T00:00:00Z"), or null
     * @param extsCsv     comma-separated extensions (e.g. "jpg,png,cr2"), or null
     * @param copyVideo   if true, include common video extensions
     */
    public DirectoryScanner(LoggerService logger,
                            String since,
                            String extsCsv,
                            boolean copyVideo) {
        this.logger = logger;

        // parse 'since' timestamp
        if (since != null && !since.isBlank()) {
            Instant tmp;
            try {
                tmp = Instant.parse(since);
            } catch (DateTimeParseException e) {
                logger.error("Invalid --since timestamp, ignoring filter: " + since,
                        e);
                tmp = null;
            }
            this.sinceInstant = tmp;
        } else {
            this.sinceInstant = null;
        }

        // build extensions set
        Set<String> exts;
        if (extsCsv != null && !extsCsv.isBlank()) {
            exts = Arrays.stream(extsCsv.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toCollection(HashSet::new));
        } else {
            exts = new HashSet<>(DEFAULT_IMAGE_EXTS);
        }
        if (copyVideo) {
            exts.addAll(VIDEO_EXTS);
        }
        this.extensions = Collections.unmodifiableSet(exts);
    }

    /**
     * Walks sourceDir recursively and returns a list of files that:
     *  - are modified on/after 'sinceInstant' (if set)
     *  - have an extension in the 'extensions' set
     */
    public List<File> scan(File sourceDir) {
        if (!sourceDir.isDirectory()) {
            logger.error("Source is not a directory: " + sourceDir,
                    new IllegalArgumentException(sourceDir.toString()));
            return Collections.emptyList();
        }

        List<File> result = new ArrayList<>();
        scanRecursive(sourceDir.toPath(), result);
        logger.info("Queued " + result.size() + " files for processing");
        return result;
    }

    private void scanRecursive(Path dir, List<File> out) {
        try {
            Files.list(dir).forEach(path -> {
                File f = path.toFile();
                if (f.isDirectory()) {
                    scanRecursive(path, out);
                } else if (matches(f)) {
                    out.add(f);
                    logger.info("  + " + f.getAbsolutePath());
                }
            });
        } catch (Exception e) {
            logger.error("Error scanning directory: " + dir, e);
        }
    }

    private boolean matches(File f) {
        // extension check
        String name = f.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = name.substring(dot + 1);
        if (!extensions.contains(ext)) return false;

        // date filter
        if (sinceInstant != null) {
            Instant mod = Instant.ofEpochMilli(f.lastModified());
            if (mod.isBefore(sinceInstant)) {
                return false;
            }
        }
        return true;
    }
}
