/*
 * Copyright (c) 2010-2023 Ryan Vogt <rvogt@ualberta.ca>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION
 * OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 * CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ca.ualberta.dbs3.math;

/**
 * The <code>ProgressMonitorPrint</code> class represents a progress monitor
 * that prints its progress to standard output.
 */
public class ProgressMonitorPrint extends ProgressMonitor {
    /**
     * The minimum number of milliseconds between update outputs.
     */
    private static final long MIN_PERIOD = 200;

    /**
     * The minimum number of milliseconds to wait before computing an ETA.
     */
    private static final long ETA_DELAY = 1500;

    /**
     * The string that erases the current line in a terminal.
     */
    private static final String ERASE = "\033[2K\r";

    /**
     * The time at which the master operation began.
     */
    private long start;

    /**
     * The time at which there was an update output.
     */
    private long lastOutput;

    /**
     * Creates a new <code>ProgressMonitorPrint</code> to monitor tasks.
     */
    public ProgressMonitorPrint() {
        this.start = 0;
        this.lastOutput = 0;
    }

    /**
     * Informs the user that a new master operation has started.
     *
     * @param description the description of the master operation.
     */
    public void startMaster(String description) {
        this.start = System.currentTimeMillis();
        this.lastOutput = 0;
        System.out.print(description + ": 0.00%   ETA: Computing");
        System.out.flush();
    }

    /**
     * Informs the user about progress in the master operation.
     *
     * @param description the description of the master operation.
     * @param complete the fraction of the work complete, as a value between
     *        <code>0.0</code> and <code>1.0</code> inclusive.
     */
    public void updateMaster(String description, double complete) {
        /* Only update if MIN_PERIOD time has passed */
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastOutput < ProgressMonitorPrint.MIN_PERIOD)
            return;
        this.lastOutput = currentTime;
        String formatStr =
                ProgressMonitorPrint.ERASE + description + ": %.2f%%   ETA: ";

        /* Compute the percentage to be output to two decimal places */
        long rVal = Math.round(complete * 10000);
        if (rVal == 10000 && complete < 1.0)
            rVal = 9999;
        double out = ((double) rVal) / 100.0;

        /*
         * Don't try to compute an ETA if nothing is complete, or if we haven't
         * passed some minimum amount of time.
         */
        long elapsed = currentTime - this.start;
        if (complete == 0.0 || elapsed < ProgressMonitorPrint.ETA_DELAY) {
            System.out.format(formatStr + "Computing", out);
            System.out.flush();
            return;
        }

        /* Otherwise, compute an ETA and show to days-level granularity */
        long msRemaining = Math.round(((double) elapsed / complete) - elapsed);
        if (msRemaining < 0)
            msRemaining = 0;
        long days = (msRemaining / 86400000);
        msRemaining -= days * 86400000;
        long hours = (msRemaining / 3600000);
        msRemaining -= hours * 3600000;
        long minutes = (msRemaining / 60000);
        msRemaining -= minutes * 60000;
        long seconds = Math.round(((double) msRemaining) / 1000);
        if (seconds == 60) {
            minutes++;
            seconds = 0L;
        }

        /* Output message, progress, and ETA */
        if (days != 0)
            System.out.format(formatStr + "%dd.%dh.%dm.%ds", out, days, hours,
                    minutes, seconds);
        else if (hours != 0)
            System.out.format(formatStr + "%dh.%dm.%ds", out, hours, minutes,
                    seconds);
        else if (minutes != 0)
            System.out.format(formatStr + "%dm.%ds", out, minutes, seconds);
        else
            System.out.format(formatStr + "%ds", out, seconds);
        System.out.flush();
    }

    /**
     * Informs the user that the master operation has indeterminate length.
     *
     * @param description the description of the master operation.
     */
    public void updateMasterIndeterminate(String description) {
        System.out.print(
                ProgressMonitorPrint.ERASE + description + ": working...");
        System.out.flush();
    }

    /**
     * Informs the user that the master operation has completed.
     *
     * @param description the description of the master operation.
     */
    public void endMaster(String description) {
        System.out.println(
                ProgressMonitorPrint.ERASE + description + ": 100.00%");
    }
}
