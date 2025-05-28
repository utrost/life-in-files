package org.trostheide.lif.photoorg;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Uses darktable-cli to convert & resize images, preserving metadata.
 * Adds extensive debug output to diagnose 2-byte outputs.
 */
public class DarktableDecoder implements PhotoDecoder {
    private static final Object DT_LOCK = new Object();
    private final Path tmpDir;
    private final int longSide;
    private final String dtBinary;

    /**
     * @param dtBinary full path or command name for darktable-cli
     * @param longSide max pixels on the longer side
     */
    public DarktableDecoder(String dtBinary, int longSide) throws IOException {
        this.dtBinary  = dtBinary;
        this.longSide  = longSide;
        this.tmpDir    = Files.createTempDirectory("lif_darktable_");
        tmpDir.toFile().deleteOnExit();
    }

    /**
     * Converts the given source file (RAW or JPEG) into a JPEG at exactly the given
     * outputPath, applying the same width/height flags you have for resizing.
     */
    public void convertTo(File rawFile, Path outputPath) throws Exception {
        synchronized (DT_LOCK) {
            // Build the command
            List<String> cmd = new ArrayList<>();
            cmd.add(dtBinary);
            cmd.add("darktable-cli");
            cmd.add(rawFile.getAbsolutePath());
            cmd.add(outputPath.toString());
            if (longSide > 0) {
                cmd.add("--width");  cmd.add(String.valueOf(longSide));
                cmd.add("--height"); cmd.add(String.valueOf(longSide));
            }

            // Debug print
            System.out.println("DEBUG [DarktableDecoder] raw→final cmd: " +
                    cmd.stream().map(arg -> "\"" + arg + "\"")
                            .collect(Collectors.joining(" ")));

            // Ensure parent exists
            Files.createDirectories(outputPath.getParent());

            // Run
            Process proc = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
            String output = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
            int exit = proc.waitFor();
            if (exit != 0) {
                throw new IllegalStateException(
                        "darktable-cli failed (exit=" + exit + "):\n" + output);
            }
        }
    }

    @Override
    public BufferedImage decode(File rawFile) throws Exception {
        // No longer used for raw-mode; JPEG-mode still uses this
        throw new UnsupportedOperationException("Use convertTo(...) for raw mode");
    }
}
