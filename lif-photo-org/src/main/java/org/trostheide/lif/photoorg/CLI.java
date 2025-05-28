package org.trostheide.lif.photoorg;

import org.trostheide.lif.core.LoggerService;
import org.trostheide.lif.core.LifIndexManager;
import org.trostheide.lif.core.ProgressTracker;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * CLI entrypoint for the lif-photo-org application.
 *
 * Usage example:
 *   java -jar lif-photo-org.jar \
 *     --source /path/to/source \
 *     --target /path/to/target \
 *     --mode raw \
 *     --longside 3000 \
 *     --since 2020-01-01T00:00:00Z \
 *     --extensions jpg,png,cr2 \
 *     --threads 4
 */
public class CLI {
    public static void main(String[] args) {
        int exitCode = new CLI().run(args);
        System.exit(exitCode);
    }

    private int run(String[] args) {
        // 1) Define CLI options
        Options options = new Options();
        options.addOption(Option.builder("s").longOpt("source")
                .hasArg().argName("dir").required()
                .desc("Source directory to scan").build());
        options.addOption(Option.builder("t").longOpt("target")
                .hasArg().argName("dir").required()
                .desc("Target directory for output").build());
        options.addOption(Option.builder("m").longOpt("mode")
                .hasArg().argName("raw|jpeg").required()
                .desc("Processing mode: 'raw' (darktable) or 'jpeg' (ImageIO)").build());
        options.addOption(Option.builder().longOpt("longside")
                .hasArg().argName("pixels")
                .desc("Max length of the longer side (0 = no resize)").build());
        options.addOption(Option.builder().longOpt("since")
                .hasArg().argName("ISO")
                .desc("Only files modified on/after this ISO-8601 timestamp").build());
        options.addOption(Option.builder().longOpt("extensions")
                .hasArg().argName("csv")
                .desc("Comma-separated file extensions to include (default = all common)").build());
        options.addOption(Option.builder().longOpt("threads")
                .hasArg().argName("n")
                .desc("Number of parallel worker threads (default = #cores)").build());
        options.addOption(Option.builder().longOpt("darktable-path")
                .hasArg().argName("dtp")
                .desc("Full Path to darktable-cli)").build());
        options.addOption("h", "help", false, "Show help");

        CommandLine cmd;
        try {
            cmd = new DefaultParser().parse(options, args);
            if (cmd.hasOption("help")) {
                new HelpFormatter().printHelp("lif-photo-org", options);
                return 0;
            }
        } catch (ParseException e) {
            System.err.println("ERROR parsing arguments: " + e.getMessage());
            new HelpFormatter().printHelp("lif-photo-org", options);
            return 1;
        }

        // 2) Extract parameters
        String sourceDir = cmd.getOptionValue("source");
        String targetDir = cmd.getOptionValue("target");
        String mode      = cmd.getOptionValue("mode");
        int longSide     = Integer.parseInt(cmd.getOptionValue("longside", "0"));
        String since     = cmd.getOptionValue("since", null);
        String extsCsv   = cmd.getOptionValue("extensions", null);
        int threads      = Integer.parseInt(
                cmd.getOptionValue("threads",
                        String.valueOf(Runtime.getRuntime().availableProcessors()))
        );
        String darktablePath = cmd.getOptionValue("darktable-path", "darktable-cli");



        System.out.println("Source:     " + sourceDir);
        System.out.println("Target:     " + targetDir);
        System.out.println("Mode:       " + mode);
        System.out.println("Long side:  " + longSide);
        System.out.println("Since:      " + since);
        System.out.println("Extensions: " + extsCsv);
        System.out.println("Threads:    " + threads);
        System.out.println("Darktable CLI: " + darktablePath);

        // 3) Initialize core services
        LoggerService logger   = new LoggerService(CLI.class);
        DirectoryScanner scanner = new DirectoryScanner(logger, since, extsCsv);
        LifIndexManager indexMgr = new LifIndexManager(new File(targetDir, ".lif-index.json"));

        // 4) Choose decoder implementation
        PhotoDecoder decoder;
        try {
            if ("raw".equalsIgnoreCase(mode)) {
                decoder = new DarktableDecoder(darktablePath, longSide);
            } else if ("jpeg".equalsIgnoreCase(mode)) {
                decoder = new JpegDecoder(longSide);
            } else {
                System.err.println("ERROR: Unknown mode '" + mode + "'");
                return 2;
            }
        } catch (Exception e) {
            logger.error("Failed to initialize decoder", e);
            return 3;
        }

        // 5) Build the processor
        PhotoProcessor processor = new PhotoProcessor(
                new File(sourceDir),
                new File(targetDir),
                logger,
                indexMgr,
                decoder
        );

        // 6) Discover files
        List<File> files = scanner.scan(new File(sourceDir));
        System.out.println("Found " + files.size() + " files to process.");

        // 7) Set up progress tracking
        ProgressTracker progress = new ProgressTracker(logger);
        progress.startTask(files.size());

        // 8) Process in parallel
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        for (File f : files) {
            exec.submit(() -> {
                processor.process(f);
                progress.step(1);
            });
        }
        exec.shutdown();
        try {
            exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for tasks to finish", ie);
            return 4;
        }
        progress.onComplete();

        System.out.println("All done.");
        return 0;
    }

}
