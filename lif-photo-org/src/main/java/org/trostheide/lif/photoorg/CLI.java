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

/**
 * CLI entrypoint for the lif-photo-org application.
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
                .desc("Processing mode: raw or jpeg").build());
        options.addOption(Option.builder().longOpt("longside")
                .hasArg().argName("pixels")
                .desc("Max length of the longer side (0 = no resize)").build());
        options.addOption(Option.builder().longOpt("since")
                .hasArg().argName("ISO")
                .desc("Only include files modified on/after this ISO-8601 timestamp").build());
        options.addOption(Option.builder().longOpt("extensions")
                .hasArg().argName("csv")
                .desc("Comma-separated file extensions to include").build());
        options.addOption(Option.builder().longOpt("threads")
                .hasArg().argName("n")
                .desc("Number of parallel worker threads").build());
        options.addOption(Option.builder().longOpt("darktable-path")
                .hasArg().argName("path")
                .desc("Full path to darktable-cli binary").build());
        options.addOption(Option.builder().longOpt("quality")
                .hasArg().argName("1-100")
                .desc("JPEG quality percentage (default: 95)").build());
        options.addOption(Option.builder().longOpt("video")
                .hasArg().argName("true|false")
                .desc("Copy video files instead of skipping (default: false)").build());
        options.addOption(Option.builder().longOpt("order")
                .hasArg().argName("structure|date")
                .desc("Output folder layout: mirror structure or group by date (default: structure)").build());
        options.addOption("h", "help", false, "Show help");

        // 2) Parse
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

        // 3) Extract parameters
        String sourceDir      = cmd.getOptionValue("source");
        String targetDir      = cmd.getOptionValue("target");
        String mode           = cmd.getOptionValue("mode");
        int longSide          = Integer.parseInt(cmd.getOptionValue("longside", "0"));
        String since          = cmd.getOptionValue("since", null);
        String extsCsv        = cmd.getOptionValue("extensions", null);
        int threads           = Integer.parseInt(cmd.getOptionValue("threads",
                String.valueOf(Runtime.getRuntime().availableProcessors())));
        String dtPath         = cmd.getOptionValue("darktable-path", "darktable-cli");
        int quality           = Integer.parseInt(cmd.getOptionValue("quality", "95"));
        boolean copyVideo     = Boolean.parseBoolean(cmd.getOptionValue("video", "false"));
        String order          = cmd.getOptionValue("order", "structure");

        System.out.println("Source:          " + sourceDir);
        System.out.println("Target:          " + targetDir);
        System.out.println("Mode:            " + mode);
        System.out.println("Long side:       " + longSide);
        System.out.println("Since:           " + since);
        System.out.println("Extensions:      " + extsCsv);
        System.out.println("Threads:         " + threads);
        System.out.println("Darktable path:  " + dtPath);
        System.out.println("JPEG quality:    " + quality);
        System.out.println("Copy video:      " + copyVideo);
        System.out.println("Output ordering: " + order);

        // 4) Core services
        LoggerService    logger   = new LoggerService(CLI.class);
        DirectoryScanner scanner  = new DirectoryScanner(logger, since, extsCsv, copyVideo);
        LifIndexManager  indexMgr = new LifIndexManager(new File(targetDir, ".lif-index.json"));

        // 5) Decoder
        PhotoDecoder decoder;
        try {
            if ("raw".equalsIgnoreCase(mode)) {
                decoder = new DarktableDecoder(dtPath, longSide, quality);
            } else if ("jpeg".equalsIgnoreCase(mode)) {
                decoder = new JpegDecoder(longSide, quality);
            } else {
                System.err.println("ERROR: Unknown mode '" + mode + "'");
                return 2;
            }
        } catch (Exception e) {
            logger.error("Failed to initialize decoder", e);
            return 3;
        }

        // 6) Processor
        PhotoProcessor processor = new PhotoProcessor(
                new File(sourceDir),
                new File(targetDir),
                logger,
                indexMgr,
                decoder,
                order.equalsIgnoreCase("date")
        );

        // 7) Scan and process
        List<File> files = scanner.scan(new File(sourceDir));
        System.out.println("Found " + files.size() + " files to process.");

        ProgressTracker progress = new ProgressTracker(logger);
        progress.startTask(files.size());

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
            logger.error("Interrupted", ie);
            return 4;
        }
        progress.onComplete();

        return 0;
    }
}
