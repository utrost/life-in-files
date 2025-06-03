package org.trostheide.lif.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LoggerService provides a standard way to obtain a class-aware SLF4J Logger.
 * Usage in each class:
 *   private static final Logger log = LoggerService.getLogger(MyClass.class);
 *   log.info("message");
 */
public class LoggerService {
    private LoggerService() {
        // Prevent instantiation
    }

    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
