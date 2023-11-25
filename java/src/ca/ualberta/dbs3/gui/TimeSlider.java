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

package ca.ualberta.dbs3.gui;

import javax.swing.*;

/**
 * The <code>TimeSlider</code> class represents a slider that can represent a
 * range of time expressed in milliseconds.
 */
public class TimeSlider extends JSlider {
    /**
     * The maximum number of ticks that will appear on any time slider.
     */
    private static final int MAX_SLIDER = 100000000;

    /**
     * Unused serialization version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The maximum number of milliseconds expressible on this slider.
     */
    private long max;

    /**
     * Creates a new <code>TimeSlider</code> capable of displaying a time range
     * of one millisecond, with the current time set to zero.
     */
    public TimeSlider() {
        super();
        this.setFocusable(false);
        this.resetWithMaximum(1L);
    }

    /**
     * Sets the maximum time expressible on this slider to the given number of
     * milliseconds, then resets the displayed time to zero milliseconds.
     *
     * @param maxMilliseconds the new maximum number of milliseconds that can
     *        be displayed on this slider.
     * @throws IllegalArgumentException if the given maximum time is negative.
     */
    public void resetWithMaximum(long maxMilliseconds) {
        if (maxMilliseconds < 0)
            throw new IllegalArgumentException("Invalid maximum time");

        this.max = maxMilliseconds;
        this.setValue(0);

        if (maxMilliseconds <= TimeSlider.MAX_SLIDER)
            this.setMaximum((int) maxMilliseconds);
        else
            this.setMaximum(TimeSlider.MAX_SLIDER);
    }

    /**
     * Sets the time currently displayed on the slider to the given number of
     * milliseconds.
     *
     * @param milliseconds the time to display.
     * @throws IllegalArgumentException if the given time is negative or
     *         greater than the maximum time this slider can display.
     */
    public void setTime(long milliseconds) {
        if (milliseconds < 0 || milliseconds > this.max)
            throw new IllegalArgumentException("Invalid time to set");

        if (this.max <= TimeSlider.MAX_SLIDER)
            this.setValue((int) milliseconds);
        else {
            double frac = ((double) milliseconds) / ((double) (this.max));
            this.setValue((int) (Math.round(frac * TimeSlider.MAX_SLIDER)));
        }
    }

    /**
     * Returns an estimate of the current time in milliseconds, based on the
     * position of the slider.
     *
     * @return an estimate of the time in milliseconds.
     */
    public long getTime() {
        if (this.max <= TimeSlider.MAX_SLIDER)
            return (long) (this.getValue());
        else {
            double frac = ((double) (this.getValue()))
                    / ((double) (TimeSlider.MAX_SLIDER));
            return Math.round(frac * this.max);
        }
    }
}
