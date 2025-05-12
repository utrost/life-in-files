// lif-photo-org/src/main/java/org/trostheide/lif/photoorg/DirectoryScanner.java
package org.trostheide.lif.photoorg;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DirectoryScanner {

    /**
     * Recursively scans a directory and returns all files.
     * @param directory root to scan
     * @param since optional Instant to filter by modification (null = all)
     * @return list of files modified after 'since'
     */
    public List<File> scan(File directory, Instant since) {
        List<File> result = new ArrayList<>();
        scanRecursively(directory, result, since);
        return result;
    }

    private void scanRecursively(File dir, List<File> result, Instant since) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scanRecursively(f, result, since);
            } else {
                if (since == null || Instant.ofEpochMilli(f.lastModified()).isAfter(since)) {
                    result.add(f);
                }
            }
        }
    }
}