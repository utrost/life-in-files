package org.trostheide.lif.photoorg;

import org.trostheide.lif.core.LoggerService;
import org.trostheide.lif.core.LifIndexManager;
import org.trostheide.lif.core.ProgressTracker;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.List;
import java.util.concurrent.*;

public class CLI implements Callable<Integer> {
    public static void main(String[] args) {
        int exitCode = new CLI().run(args);
        System.exit(exitCode);
    }

    private int run(String[] args) {
        // --- 1) Define CLI options ---
        Options options = new Options();
        options.addOption(Option.builder("s").longOpt("source")
                .hasArg().argName("dir").required()
                .desc("Source directory").build());
        options.addOption(Option.builder("t").longOpt("target")
                .hasArg().argName("dir").required()
                .desc("Target directory").build());
        options.addOption(Option.builder("m").longOpt("mode")
                .hasArg().argName("raw|jpeg").required()
                .desc("Processing mode").build());
        options.addOption(Option.builder().longOpt("longside")
                .hasArg().argName("pixels").build());
        options.addOption(Option.builder().longOpt("since")
                .hasArg().argName("ISO").build());
        options.addOption(Option.builder().longOpt("extensions")
                .hasArg().argName("csv").build());
        options.addOption(Option.builder().longOpt("threads")
                .hasArg().argName("n")
                .desc("Parallel threads (default = # cores)").build());
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

        // --- 2) Extract params ---
        File source = new File(cmd.getOptionValue("source"));
        File target = new File(cmd.getOptionValue("target"));
        String mode = cmd.getOptionValue("mode");
        int longSide = Integer.parseInt(cmd.getOptionValue("longside", "0"));
        String since = cmd.getOptionValue("since", null);
        String exts = cmd.getOptionValue("extensions", null);
        int threads = Integer.parseInt(cmd.getOptionValue(
                "threads",
                String.valueOf(Runtime.getRuntime().availableProcessors())
        ));

        System.out.println("Source: " + source);
        System.out.println("Target: " + target);
        System.out.println("Mode: "   + mode);
        System.out.println("Long side: " + longSide);
        System.out.println("Since: "  + since);
        System.out.println("Extensions: " + exts);
        System.out.println("Threads: " + threads);

        // --- 3) Instantiate core components ---
        LoggerService logger = new LoggerService(CLI.class);
        DirectoryScanner scanner = new DirectoryScanner(logger, since, exts);
        LifIndexManager indexMgr = new LifIndexManager(new File(target, ".lif-index.json"));

        PhotoDecoder decoder;
        try {
            decoder = new DarktableDecoder();
        } catch (Exception e) {
            logger.error("Failed to init DarktableDecoder", e);
            return 2;
        }
        ImageResizer resizer = new ThumbnailatorResizer();
        PhotoProcessor processor = new PhotoProcessor(
                source, target, mode, logger, indexMgr, decoder, resizer, longSide
        );

        // --- 4) Scan for files ---
        List<File> files = scanner.scan(source);
        System.out.println("Found " + files.size() + " files to process.");

        // --- 5) Setup ProgressTracker ---
        ProgressTracker progress = new ProgressTracker(logger);
        progress.startTask(files.size());

        // --- 6) Process in parallel ---
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        for (File f : files) {
            exec.submit(() -> {
                try {
                    processor.process(f);
                } catch (Exception ex) {
                    logger.error("Failed processing " + f.getAbsolutePath(), ex);
                } finally {
                    progress.step(1);
                }
            });
        }

        // --- 7) Shutdown & await completion ---
        exec.shutdown();
        try {
            exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for tasks to finish", ie);
        }
        progress.onComplete();

        System.out.println("All done.");
        return 0;
    }

    @Override
    public Integer call() throws Exception {
        // Unused; we use run() directly
        return 0;
    }
}
