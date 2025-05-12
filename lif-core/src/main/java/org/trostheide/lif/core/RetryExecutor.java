
// lif-core/src/main/java/org/trostheide/lif/core/RetryExecutor.java
package org.trostheide.lif.core;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;

public class RetryExecutor {
    private final int maxAttempts;
    private final long backoffMillis;

    public RetryExecutor(int maxAttempts, long backoffMillis) {
        this.maxAttempts = maxAttempts;
        this.backoffMillis = backoffMillis;
    }

    public <T> T execute(Callable<T> callable) throws Exception {
        int attempts = 0;
        while (true) {
            try {
                return callable.call();
            } catch (Exception e) {
                attempts++;
                if (attempts >= maxAttempts) {
                    throw e;
                }
                Thread.sleep(backoffMillis * attempts);
            }
        }
    }
}

