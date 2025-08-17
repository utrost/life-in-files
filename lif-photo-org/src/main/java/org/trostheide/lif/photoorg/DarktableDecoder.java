package org.trostheide.lif.photoorg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trostheide.lif.core.LoggerService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Uses darktable-cli to convert & resize images in parallel, preserving metadata.
 * Each process runs with a temporary, isolated config directory to avoid database locks.
 */
public class DarktableDecoder implements PhotoDecoder {
    private static final Logger log = LoggerFactory.getLogger(DarktableDecoder.class);


    private final String dtBinary;
    private final int longSide;
    private final int quality;

    public DarktableDecoder(String dtBinary, int longSide, int quality) {
        this.dtBinary = dtBinary;
        this.longSide  = longSide;
        this.quality   = quality;
    }

    /**
     * Raw mode entrypoint: runs darktable-cli to convert the source file
     * into a JPEG at the given outputPath.
     *
     * @param srcFile    source RAW or image file
     * @param outputPath desired .jpg file path
     */
    public void convertTo(File srcFile, Path outputPath) throws Exception {
        // Create a unique temporary directory for this process's config
        Path tempConfigDir = Files.createTempDirectory("lif-darktable-config-" + UUID.randomUUID());

        try {
            // Ensure output directory exists
            Path outDir = outputPath.getParent();
            Files.createDirectories(outDir);

            // Build command with isolated config directory
            List<String> cmd = new ArrayList<>();
            cmd.add(dtBinary);
            cmd.add(srcFile.getAbsolutePath());
            cmd.add(outputPath.toString()); // Directly specify the output file
            if (longSide > 0) {
                cmd.add("--width");  cmd.add(String.valueOf(longSide));
                cmd.add("--height"); cmd.add(String.valueOf(longSide));
            }
            cmd.add("--core");
            cmd.add("--configdir");
            cmd.add(tempConfigDir.toString());
            cmd.add("--cachedir");
            cmd.add(tempConfigDir.toString());

            if (quality >= 1 && quality <= 100) {
                cmd.add("--conf");
                cmd.add("plugins/imageio/format/jpeg/quality=" + quality);
            }

            // Execute the process
            Process proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();

            // It's good practice to consume the output stream to prevent the process buffer from filling up
            String output = new BufferedReader(new InputStreamReader(proc.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));

            int exit = proc.waitFor();
            if (exit != 0) {
                log.error("darktable-cli failed for {} with exit code {}. Output:\n{}", srcFile.getName(), exit, output);
                throw new IOException("darktable-cli failed (exit=" + exit + ")");
            }

            if (!Files.exists(outputPath)) {
                log.error("Expected output not found: {}. Output:\n{}", outputPath, output);
                throw new IOException("Expected output not found: " + outputPath);
            }

        } finally {
            // Cleanup: Recursively delete the temporary config directory
            Files.walk(tempConfigDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
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
