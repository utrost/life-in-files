package org.trostheide.lif.photofaces;

public class LoggerService {
    public static void info(String message) {
        System.out.println("[INFO] " + message);
    }
    public static void error(String message) {
        System.err.println("[ERROR] " + message);
    }
    // Extend as needed (debug, warn, etc.)
}
