package org.trostheide.lif.core;

import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple progress reporter that logs "completed/total (percent%)" on each step.
 * Also prints directly to stdout so you can see progress even if Logback isn’t
 * configured correctly.
 */
public class ProgressTracker {
    private static final Logger log = LoggerService.getLogger(ProgressTracker.class);
    private long totalWork;
    private final AtomicLong completed = new AtomicLong(0);


    public ProgressTracker() {
        ;
    }

    /**
     * Initialize the tracker with the total units of work.
     * Resets internal counters.
     *
     * @param totalWork total number of steps/tasks
     */
    public void startTask(long totalWork) {
        this.totalWork = totalWork;
        this.completed.set(0);
        String msg = formatMessage(0);
        log.info(msg);
        System.out.println(msg);
    }

    /**
     * Advance the completed count by the given amount and log progress.
     *
     * @param count number of tasks just completed (often 1)
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

    /**
     * Formats the progress message.
     */
    private String formatMessage(long done) {
        int percent = totalWork > 0
                ? (int)((done * 100) / totalWork)
                : 100;
        return String.format("[Progress] %d/%d processed (%d%%)", done, totalWork, percent);
    }
}
