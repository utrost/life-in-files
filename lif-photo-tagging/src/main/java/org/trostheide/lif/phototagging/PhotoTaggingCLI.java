package org.trostheide.lif.phototagging;

import org.apache.commons.cli.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Command-line entry point for the lif-photo-tagging tool.
 */
public class PhotoTaggingCLI {

    public static void main(String[] args) {
        Options options = new Options();

        options.addOption(Option.builder("i")
                .longOpt("input")
                .hasArg()
                .argName("directory")
                .desc("Root directory to scan for .jpg/.jpeg images (required)")
                .required()
                .build());

        options.addOption(Option.builder("s")
                .longOpt("since")
                .hasArg()
                .argName("yyyy-mm-dd")
                .desc("Only process images modified after the specified date")
                .build());

        options.addOption(Option.builder()
                .longOpt("ollama-endpoint")
                .hasArg()
                .argName("url")
                .desc("LLM endpoint for image tagging (default: http://localhost:11434)")
                .build());

        options.addOption(Option.builder()
                .longOpt("thumbnail-width")
                .hasArg()
                .argName("pixels")
                .desc("Width for resized image before LLM processing (default: 512)")
                .build());

        options.addOption(Option.builder("t")
                .longOpt("tag-vocabulary")
                .hasArg()
                .argName("file")
                .desc("Optional file with predefined tags to guide tagging")
                .build());

        options.addOption(Option.builder()
                .longOpt("update")
                .desc("Append to existing YAML sidecars instead of skipping")
                .build());

        options.addOption(Option.builder()
                .longOpt("rerun")
                .desc("Force full reprocessing and overwrite existing metadata")
                .build());

        options.addOption(Option.builder("l")
                .longOpt("log")
                .hasArg()
                .argName("file")
                .desc("Path to the CSV log file")
                .build());

        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Display usage information")
                .build());

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                formatter.printHelp("java -jar lif-photo-tagging.jar [options]", options);
                return;
            }

            PhotoTaggingConfig config = new PhotoTaggingConfig();

            // Required input directory
            Path inputPath = Paths.get(cmd.getOptionValue("input"));
            if (!Files.exists(inputPath) || !Files.isDirectory(inputPath)) {
                System.err.println("Error: Invalid input directory: " + inputPath);
                System.exit(1);
            }
            config.setInputDir(inputPath);

            // Optional values
            if (cmd.hasOption("since")) {
                try {
                    config.setSinceDate(LocalDate.parse(cmd.getOptionValue("since")));
                } catch (DateTimeParseException e) {
                    System.err.println("Error: Invalid date format for --since (expected yyyy-mm-dd)");
                    System.exit(1);
                }
            }

            if (cmd.hasOption("ollama-endpoint")) {
                config.setApiEndpoint(cmd.getOptionValue("ollama-endpoint"));
            }

            if (cmd.hasOption("thumbnail-width")) {
                try {
                    config.setThumbnailWidth(Integer.parseInt(cmd.getOptionValue("thumbnail-width")));
                } catch (NumberFormatException e) {
                    System.err.println("Error: thumbnail-width must be an integer");
                    System.exit(1);
                }
            }

            if (cmd.hasOption("tag-vocabulary")) {
                config.setTagList(cmd.getOptionValue("tag-vocabulary"));
            }

            config.setDryRun(false); // Set to true if implementing a dry-run flag later
            config.setUpdate(cmd.hasOption("update"));
            config.setRerun(cmd.hasOption("rerun"));

            if (cmd.hasOption("log")) {
                config.setLogFilePath(Paths.get(cmd.getOptionValue("log")));
            }

            // Call the actual processor
            PhotoTaggingProcessor.run(config);

        } catch (MissingOptionException e) {
            System.err.println("Missing required options: " + e.getMessage());
            formatter.printHelp("java -jar lif-photo-tagging.jar [options]", options);
            System.exit(1);
        } catch (ParseException e) {
            System.err.println("Failed to parse command-line arguments: " + e.getMessage());
            formatter.printHelp("java -jar lif-photo-tagging.jar [options]", options);
            System.exit(1);
        }
    }
}
