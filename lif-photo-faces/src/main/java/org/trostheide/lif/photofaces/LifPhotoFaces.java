package org.trostheide.lif.photofaces;

import org.apache.commons.cli.*;
import org.trostheide.lif.photofaces.config.PhotoFacesConfig;

public class LifPhotoFaces {

    public static void main(String[] args) {
        Options options = new Options();

        Option modeOption = Option.builder("m")
                .longOpt("mode")
                .desc("Mode: detect or sync")
                .hasArg()
                .argName("MODE")
                .required()
                .build();
        options.addOption(modeOption);

        options.addOption(Option.builder()
                .longOpt("image-dir")
                .desc("Directory containing images (jpg)")
                .hasArg()
                .argName("DIR")
                .required()
                .build());

        options.addOption(Option.builder()
                .longOpt("person-dir")
                .desc("Directory for person markdown files")
                .hasArg()
                .argName("DIR")
                .required()
                .build());

        options.addOption(Option.builder()
                .longOpt("dry-run")
                .desc("Preview actions, do not write changes")
                .build());

        options.addOption(Option.builder()
                .longOpt("since-date")
                .desc("Only process images created after this date (YYYY-MM-DD)")
                .hasArg()
                .argName("DATE")
                .build());

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            CommandLine cmd = parser.parse(options, args);

            String mode = cmd.getOptionValue("mode");
            String imageDir = cmd.getOptionValue("image-dir");
            String personDir = cmd.getOptionValue("person-dir");
            boolean dryRun = cmd.hasOption("dry-run");
            String sinceDate = cmd.getOptionValue("since-date");

            // Config object (expand as needed)
            PhotoFacesConfig config = new PhotoFacesConfig(imageDir, personDir, dryRun, sinceDate);

            if ("detect".equalsIgnoreCase(mode)) {
                FaceDetectionService detection = new FaceDetectionService(config);
                detection.runDetection();
            } else if ("sync".equalsIgnoreCase(mode)) {
                MetadataSyncService sync = new MetadataSyncService(config);
                sync.runSync();
            } else {
                System.err.println("Unknown mode: " + mode);
                formatter.printHelp("lif-photo-faces", options);
            }

        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            formatter.printHelp("lif-photo-faces", options);
            System.exit(1);
        }
    }
}
