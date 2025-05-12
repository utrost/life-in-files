// lif-photo-org/src/main/java/org/trostheide/lif/photoorg/CLI.java
package org.trostheide.lif.photoorg;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.trostheide.lif.core.LoggerService;

import java.io.File;
import java.time.Instant;
import java.util.List;

public class CLI {

    public static void main(String[] args) {
        LoggerService logger = new LoggerService(CLI.class);
        Options options = new Options();

        Option sourceOpt = Option.builder("s")
                .longOpt("source")
                .hasArg()
                .argName("path")
                .desc("Source directory (required)")
                .required()
                .build();

        Option targetOpt = Option.builder("t")
                .longOpt("target")
                .hasArg()
                .argName("path")
                .desc("Target directory (required)")
                .required()
                .build();

        Option modeOpt = Option.builder("m")
                .longOpt("mode")
                .hasArg()
                .argName("raw|jpeg")
                .desc("Processing mode (raw|jpeg), default=jpeg")
                .build();

        options.addOption(sourceOpt);
        options.addOption(targetOpt);
        options.addOption(modeOpt);

        CommandLine cmd;
        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            logger.error("Failed to parse command line arguments", e);
            new HelpFormatter().printHelp("lif-photo-org", options);
            System.exit(1);
            return;
        }

        File source = new File(cmd.getOptionValue("source"));
        File target = new File(cmd.getOptionValue("target"));
        String mode = cmd.getOptionValue("mode", "jpeg");

        logger.info("Starting lif-photo-org");
        logger.info("Source: " + source.getAbsolutePath());
        logger.info("Target: " + target.getAbsolutePath());
        logger.info("Mode: " + mode);

        DirectoryScanner scanner = new DirectoryScanner();
        List<File> files = scanner.scan(source, null);
        files.forEach(f -> logger.info("Found: " + f.getAbsolutePath()));
    }
}