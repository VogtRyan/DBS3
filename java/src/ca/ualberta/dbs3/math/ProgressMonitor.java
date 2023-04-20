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

import java.util.LinkedList;

/**
 * The <code>ProgressMonitor</code> class is used to inform the user about the
 * progress of long operations.
 */
public abstract class ProgressMonitor {
    /**
     * The currently executing sequence of operations, ordered by when they
     * were started. That is, the head of the list represents the master
     * operation.
     */
    private LinkedList<String> operations;

    /**
     * Whether the operation should be cancelled.
     */
    private boolean cancel;

    /**
     * Creates a new <code>ProgressMonitor</code> for which the cancel flag has
     * not yet been raised.
     */
    public ProgressMonitor() {
        this.operations = new LinkedList<String>();
        this.cancel = false;
    }

    /**
     * Raises a flag in the progress monitor that the operation should be
     * cancelled.
     */
    public void cancel() {
        this.cancel = true;
    }

    /**
     * Returns whether or not the cancel flag has been raised.
     *
     * @return <code>true</code> if the cancel flag has been raised, otherwise
     *         <code>false</code>.
     */
    public boolean shouldCancel() {
        return this.cancel;
    }

    /**
     * Informs the progress monitor that a new operation with the given
     * description has begun.
     *
     * @param description the description of the new operation to start.
     */
    public void start(String description) {
        this.operations.add(description);
        if (this.operations.size() == 1)
            this.startMaster(description);
    }

    /**
     * Informs the progress monitor about the status of the most recently
     * started operation.
     *
     * @param complete the number of units of work that are complete.
     * @param total the total units of work to be done.
     * @throws IllegalArgumentException if <code>complete</code> is negative,
     *         if <code>total</code> if not greater than zero, or if
     *         <code>complete</code> is greater than <code>total</code>.
     * @throws IllegalStateException if there are no operations running.
     */
    public void update(int complete, int total) {
        if (total <= 0 || complete < 0 || complete > total)
            throw new IllegalArgumentException("Invalid update values");
        this.update(((double) complete) / total);
    }

    /**
     * Informs the progress monitor about the status of the most recently
     * started operation.
     *
     * @param complete the fraction of the work complete, as a value between
     *        <code>0.0</code> and <code>1.0</code> inclusive.
     * @throws IllegalArgumentException if <code>complete</code> is not
     *         between, <code>0.0</code> and <code>1.0</code> inclusive.
     * @throws IllegalStateException if there are no operations running.
     */
    public void update(double complete) {
        if (complete < 0.0 || complete > 1.0)
            throw new IllegalArgumentException("Invalid update value");
        int size = this.operations.size();
        if (size == 0)
            throw new IllegalStateException("No operations running");
        else if (size == 1)
            this.updateMaster(this.operations.peek(), complete);
    }

    /**
     * Informs the progress monitor that the most recently started operation
     * has an indeterminate running time.
     *
     * @throws IllegalStateException if there are no operations running.
     */
    public void updateIndeterminate() {
        int size = this.operations.size();
        if (size == 0)
            throw new IllegalStateException("No operations running");
        else if (size == 1)
            this.updateMasterIndeterminate(this.operations.peek());
    }

    /**
     * Informs the progress monitor that the most recently started operation
     * has completed.
     *
     * @throws IllegalStateException if there are no operations running.
     */
    public void end() {
        int size = this.operations.size();
        if (size == 0)
            throw new IllegalStateException("No operations running");
        String master = this.operations.removeLast();
        if (size == 1)
            this.endMaster(master);
    }

    /**
     * Informs the user that a new master operation has started.
     *
     * @param description the description of the master operation.
     */
    public abstract void startMaster(String description);

    /**
     * Informs the user about progress in the master operation.
     *
     * @param description the description of the master operation.
     * @param complete the fraction of the work complete, as a value between
     *        <code>0.0</code> and <code>1.0</code> inclusive.
     */
    public abstract void updateMaster(String description, double complete);

    /**
     * Informs the user that the master operation has indeterminate length.
     *
     * @param description the description of the master operation.
     */
    public abstract void updateMasterIndeterminate(String description);

    /**
     * Informs the user that the master operation has completed.
     *
     * @param description the description of the master operation.
     */
    public abstract void endMaster(String description);
}
