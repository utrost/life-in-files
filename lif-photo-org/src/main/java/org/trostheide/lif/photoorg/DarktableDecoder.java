package org.trostheide.lif.photoorg;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Uses darktable-cli to convert a RAW file to JPEG,
 * but ensures only one conversion runs at a time.
 */
public class DarktableDecoder implements PhotoDecoder {
    // single lock across all threads
    private static final Object DT_LOCK = new Object();
    private final Path tmpDir;

    public DarktableDecoder() throws IOException {
        tmpDir = Files.createTempDirectory("lif_darktable_");
        tmpDir.toFile().deleteOnExit();
    }

    @Override
    public BufferedImage decode(File rawFile) throws Exception {
        // everything inside this sync block runs one-at-a-time
        synchronized (DT_LOCK) {
            // prepare output file
            String base = rawFile.getName().replaceAll("\\.[^.]+$", "");
            Path outFile = tmpDir.resolve(base + ".jpg");
            Files.deleteIfExists(outFile);

            List<String> command = List.of(
                    "darktable-cli",
                    rawFile.getAbsolutePath(),
                    outFile.toString()
            );
            // debug print
            System.out.println("DEBUG: Executing: " +
                    command.stream().map(arg -> "\"" + arg + "\"")
                            .collect(Collectors.joining(" ")));

            ProcessBuilder pb = new ProcessBuilder(command)
                    .redirectErrorStream(true);
            Process proc = pb.start();
            String output = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
            int exit = proc.waitFor();
            if (exit != 0) {
                throw new IllegalStateException(
                        "darktable-cli failed (exit=" + exit + "):\n" + output);
            }

            // load the JPEG
            BufferedImage img = ImageIO.read(outFile.toFile());
            if (img == null) {
                throw new IllegalStateException(
                        "Failed to read JPEG from " + outFile);
            }
            return img;
        }
    }
}
