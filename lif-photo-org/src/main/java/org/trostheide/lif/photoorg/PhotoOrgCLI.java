package org.trostheide.lif.photoorg;

import org.apache.commons.cli.*;
import org.trostheide.lif.core.LifIndexManager;
import org.trostheide.lif.core.ProgressTracker;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PhotoOrgCLI {

    public static void main(String[] args) {
        int exitCode = new PhotoOrgCLI().run(args);
        System.exit(exitCode);
    }

    private int run(String[] args) {
        Options options = new Options();
        // ... (options definition is unchanged)
        options.addOption(Option.builder("s").longOpt("source").hasArg().argName("dir").required().desc("Source directory to scan").build());
        options.addOption(Option.builder("t").longOpt("target").hasArg().argName("dir").required().desc("Target directory for output").build());
        options.addOption(Option.builder("m").longOpt("mode").hasArg().argName("raw|jpeg").required().desc("Processing mode: raw or jpeg").build());
        options.addOption(Option.builder("o").longOpt("order").hasArg().argName("structure|date|event").desc("Output folder layout (default: structure)").build());
        options.addOption(null, "event-rescan", false, "Force a full rescan of all events, ignoring the saved cache.");
        options.addOption(Option.builder().longOpt("longside").hasArg().argName("pixels").desc("Max length of the longer side (0 = no resize)").build());
        options.addOption(Option.builder().longOpt("since").hasArg().argName("ISO").desc("Only include files modified on/after this ISO-8601 timestamp").build());
        options.addOption(Option.builder().longOpt("extensions").hasArg().argName("csv").desc("Comma-separated file extensions to include").build());
        options.addOption(Option.builder().longOpt("threads").hasArg().argName("n").desc("Number of parallel worker threads").build());
        options.addOption(Option.builder().longOpt("darktable-path").hasArg().argName("path").desc("Full path to darktable-cli binary").build());
        options.addOption(Option.builder().longOpt("quality").hasArg().argName("1-100").desc("JPEG quality percentage (default: 95)").build());
        options.addOption(Option.builder().longOpt("video").hasArg().argName("true|false").desc("Copy video files instead of skipping (default: false)").build());
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

        String sourceDir = cmd.getOptionValue("source");
        String targetDir = cmd.getOptionValue("target");
        String mode = cmd.getOptionValue("mode");
        String order = cmd.getOptionValue("order", "structure");
        boolean eventRescan = cmd.hasOption("event-rescan");
        int longSide = Integer.parseInt(cmd.getOptionValue("longside", "0"));
        String since = cmd.getOptionValue("since", null);
        String extsCsv = cmd.getOptionValue("extensions", null);
        int threads = Integer.parseInt(cmd.getOptionValue("threads", String.valueOf(Runtime.getRuntime().availableProcessors())));
        String dtPath = cmd.getOptionValue("darktable-path", "darktable-cli");
        int quality = Integer.parseInt(cmd.getOptionValue("quality", "95"));
        boolean copyVideo = Boolean.parseBoolean(cmd.getOptionValue("video", "false"));

        System.out.println("Source:          " + sourceDir);
        System.out.println("Target:          " + targetDir);
        System.out.println("Mode:            " + mode);
        System.out.println("Ordering:        " + order);
        System.out.println("Event Rescan:    " + eventRescan);
        System.out.println("Long side:       " + longSide);
        System.out.println("Since:           " + (since != null ? since : "not set"));
        System.out.println("Threads:         " + threads);

        EventManager eventManager = null;
        if ("event".equalsIgnoreCase(order)) {
            eventManager = new EventManager(new File(targetDir));
            if (!eventRescan) {
                eventManager.loadEvents();
            }
            eventManager.discoverNewEvents(new File(sourceDir));
        }

        DirectoryScanner scanner = new DirectoryScanner(since, extsCsv, copyVideo);
        LifIndexManager indexMgr = new LifIndexManager(new File(targetDir, ".lif-index.json"));
        PhotoDecoder decoder = createDecoder(mode, dtPath, longSide, quality);
        if (decoder == null) return 2;

        PhotoProcessor processor = new PhotoProcessor(
                new File(sourceDir), new File(targetDir),
                indexMgr, decoder, order, eventManager
        );

        long startTime = System.currentTimeMillis();
        System.out.println("\n--- Starting File Processing Phase ---");
        List<File> files = scanner.scan(new File(sourceDir));
        System.out.println("Found " + files.size() + " files to process.");

        ProgressTracker progress = new ProgressTracker();
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
            System.err.println("Processing was interrupted.");
            return 4;
        }
        progress.onComplete();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("\nProcessing finished. Total time: " + TimeUnit.MILLISECONDS.toSeconds(duration) + " seconds.");

        if (eventManager != null) {
            eventManager.saveEvents();
        }

        return 0;
    }

    private PhotoDecoder createDecoder(String mode, String dtPath, int longSide, int quality) {
        try {
            if ("raw".equalsIgnoreCase(mode)) {
                return new DarktableDecoder(dtPath, longSide, quality);
            } else if ("jpeg".equalsIgnoreCase(mode)) {
                return new JpegDecoder(longSide, quality);
            } else {
                System.err.println("ERROR: Unknown mode '" + mode + "'");
                return null;
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize decoder: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }
}
