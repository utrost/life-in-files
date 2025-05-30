package org.trostheide.lif.photoorg;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Uses darktable-cli to convert & resize images, preserving metadata.
 * Provides a convertTo(...) method for raw mode to write directly to the final output file.
 */
public class DarktableDecoder implements PhotoDecoder {
    private static final Object DT_LOCK = new Object();
    private final String dtBinary;
    private final int longSide;
    private final int quality;

    /**
     * @param dtBinary full path or command name for darktable-cli
     * @param longSide max length of the longer side in pixels (<=0 = no resize)
     * @param quality  JPEG quality percentage (1-100)
     * @throws IOException if temp directory cannot be created
     */
    public DarktableDecoder(String dtBinary, int longSide, int quality) throws IOException {
        this.dtBinary = dtBinary;
        this.longSide  = longSide;
        this.quality   = quality;
    }

    /**
     * Raw mode entrypoint: runs darktable-cli to convert the source file
     * into a JPEG at the given outputPath (parent directory must exist).
     *
     * @param srcFile    source RAW or image file
     * @param outputPath desired .jpg file path (name must end with .jpg)
     */
    public void convertTo(File srcFile, Path outputPath) throws Exception {
        synchronized (DT_LOCK) {
            // Ensure output directory exists
            Path outDir = outputPath.getParent();
            Files.createDirectories(outDir);

            // Build command: dtBinary <input> <outDir> [--width W --height H] [--core --conf plugins/imageio/format/jpeg/quality=Q]
            List<String> cmd = new ArrayList<>();
            cmd.add(dtBinary);
            cmd.add(srcFile.getAbsolutePath());
            cmd.add(outDir.toString());
            if (longSide > 0) {
                cmd.add("--width");  cmd.add(String.valueOf(longSide));
                cmd.add("--height"); cmd.add(String.valueOf(longSide));
            }
            if (quality >= 1 && quality <= 100) {
                cmd.add("--core");
                cmd.add("--conf");
                cmd.add("plugins/imageio/format/jpeg/quality=" + quality);
            }

            // Debug print
            String debug = cmd.stream()
                    .map(arg -> "\"" + arg + "\"")
                    .collect(Collectors.joining(" "));
            System.out.println("DEBUG [DarktableDecoder] Executing: " + debug);

            // Execute
            Process proc = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
            String output = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
            int exit = proc.waitFor();

            System.out.println("DEBUG [DarktableDecoder] exit code: " + exit);
            System.out.println(output);

            if (exit != 0) {
                throw new IllegalStateException("darktable-cli failed (exit=" + exit + ")");
            }

            // Note: darktable-cli writes <basename>.jpg into outDir;
            // outputPath should match outDir/<basename>.jpg
            Path expected = outDir.resolve(
                    srcFile.getName().replaceAll("\\.[^.]+$", "") + ".jpg"
            );
            if (!Files.exists(expected)) {
                throw new IllegalStateException("Expected output not found: " + expected);
            }
            // If outputPath differs (e.g. different extension or name), rename
            if (!expected.equals(outputPath)) {
                Files.move(expected, outputPath);
            }
        }
    }

    /**
     * Not used in raw mode. PhotoProcessor invokes convertTo(...) directly.
     */
    @Override
    public BufferedImage decode(File rawFile) {
        throw new UnsupportedOperationException(
                "decode(...) is not supported in raw mode; use convertTo(...) instead"
        );
    }
}
