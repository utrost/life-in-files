package org.trostheide.lif.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory; // Use standard SLF4J factory

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple progress reporter that logs "completed/total (percent%)" on each step.
 */
public class ProgressTracker {
    // Use standard LoggerFactory, not the custom LoggerService
    private static final Logger log = LoggerFactory.getLogger(ProgressTracker.class);
    private long totalWork;
    private final AtomicLong completed = new AtomicLong(0);

    /**
     * No-argument constructor.
     */
    public ProgressTracker() {
        // The constructor is now empty as it should be.
    }

    /**
     * Initialize the tracker with the total units of work.
     */
    public void startTask(long totalWork) {
        this.totalWork = totalWork;
        this.completed.set(0);
        String msg = formatMessage(0);
        log.info(msg);
        System.out.println(msg); // Also print to stdout for visibility
    }

    /**
     * Advance the completed count by the given amount.
     */
    public void step(long count) {
        long done = this.completed.addAndGet(count);
        String msg = formatMessage(done);
        log.info(msg);
        System.out.println(msg);
    }

    /**
     * Call when all work is done to log the final 100% message.
     */
    public void onComplete() {
        this.completed.set(this.totalWork);
        String msg = formatMessage(this.totalWork);
        log.info(msg);
        System.out.println(msg);
    }

    private String formatMessage(long done) {
        int percent = totalWork > 0
                ? (int)((done * 100) / totalWork)
                : 100;
        return String.format("[Progress] %d/%d processed (%d%%)", done, totalWork, percent);
    }
}
