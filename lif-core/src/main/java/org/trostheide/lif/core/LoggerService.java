
// lif-core/src/main/java/org/trostheide/lif/core/LoggerService.java
package org.trostheide.lif.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class LoggerService {
    private final Logger logger;

    public LoggerService(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public void info(String msg) {
        logger.info(msg);
    }

    public void error(String msg, Throwable t) {
        logger.error(msg, t);
    }
}


