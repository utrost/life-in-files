package org.trostheide.lif.phototagging;

import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class PhotoTaggingCLI {

    public static void main(String[] args) {
        Options options = new Options();

        options.addOption(Option.builder("i")
                .longOpt("input")
                .desc("Input directory with photos to tag")
                .hasArg()
                .argName("dir")
                .required()
                .build());

        options.addOption(Option.builder()
                .longOpt("ollama-endpoint")
                .hasArg()
                .argName("url")
                .desc("Specify the Ollama or LLM endpoint for image analysis (default: http://localhost:11434/api/generate)")
                .build());

        options.addOption(Option.builder("s")
                .longOpt("since")
                .desc("Only process photos created since this date (YYYY-MM-DD)")
                .hasArg()
                .argName("date")
                .build());

        options.addOption(Option.builder()
                .longOpt("dry-run")
                .desc("Run without writing sidecar files")
                .build());

        options.addOption(Option.builder()
                .longOpt("rerun")
                .desc("Process all files regardless of previous YAML tagging")
                .build());

        options.addOption(Option.builder()
                .longOpt("update")
                .desc("Update .yaml files if they already exist")
                .build());

        options.addOption(Option.builder()
                .longOpt("embed")
                .desc("Write description and tags into JPEG metadata")
                .build());

        options.addOption(Option.builder("w")
                .longOpt("width")
                .desc("Thumbnail width in pixels (default: 512)")
                .hasArg()
                .argName("pixels")
                .build());

        options.addOption(Option.builder("m")
                .longOpt("model")
                .desc("Model to use (default: gemma3:4b)")
                .hasArg()
                .argName("model")
                .build());

        options.addOption(Option.builder("p")
                .longOpt("prompt")
                .desc("Prompt to send to the LLM")
                .hasArg()
                .argName("text")
                .build());

        options.addOption(Option.builder("t")
                .longOpt("tags")
                .desc("Optional list of predefined tags (comma-separated)")
                .hasArg()
                .argName("tags")
                .build());

        options.addOption("h", "help", false, "Show this help message");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                formatter.printHelp("lif-photo-tagging", options);
                return;
            }

            PhotoTaggingConfig config = new PhotoTaggingConfig();

            config.setInputDir(Paths.get(cmd.getOptionValue("input")));

            if (cmd.hasOption("since")) {
                try {
                    config.setSinceDate(LocalDate.parse(cmd.getOptionValue("since")));
                } catch (DateTimeParseException e) {
                    System.err.println("Invalid date format for --since (expected YYYY-MM-DD)");
                    return;
                }
            }

            if (cmd.hasOption("ollama-endpoint")) {
                config.setApiEndpoint(cmd.getOptionValue("ollama-endpoint"));
            }

            config.setDryRun(cmd.hasOption("dry-run"));
            config.setRerun(cmd.hasOption("rerun"));
            config.setUpdate(cmd.hasOption("update"));
            config.setEmbedMetadata(cmd.hasOption("embed"));

            if (cmd.hasOption("width")) {
                try {
                    config.setThumbnailWidth(Integer.parseInt(cmd.getOptionValue("width")));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number for --width");
                    return;
                }
            }

            if (cmd.hasOption("model")) {
                config.setModel(cmd.getOptionValue("model"));
            }

            if (cmd.hasOption("prompt")) {
                config.setPrompt(cmd.getOptionValue("prompt"));
            }

            if (cmd.hasOption("tags")) {
                config.setTagList(cmd.getOptionValue("tags"));
            }

            // ✅ Launch processing
            PhotoTaggingProcessor.run(config);

        } catch (ParseException e) {
            System.err.println(e.getMessage());
            formatter.printHelp("lif-photo-tagging", options);
        }
    }
}