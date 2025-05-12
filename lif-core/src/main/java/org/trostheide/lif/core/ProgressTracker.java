// lif-core/src/main/java/org/trostheide/lif/core/ProgressTracker.java
package org.trostheide.lif.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class ProgressTracker {
    private final long total;
    private long current;

    public ProgressTracker(long totalWork) {
        this.total = totalWork;
        this.current = 0;
    }

    public void step(long work) {
        current += work;
        double pct = (total > 0) ? (current * 100.0 / total) : 100.0;
        System.out.printf("Progress: %.2f%%%n", pct);
    }

    public void complete() {
        System.out.println("Progress: 100% (complete)");
    }
}

