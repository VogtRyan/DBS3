/*
 * Copyright (c) 2009-2023 Ryan Vogt <rvogt@ualberta.ca>
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
 * The <code>TimeLabel</code> class represents a <code>JLabel</code> with the
 * ability to set its text to a human-readable time format.
 */
public class TimeLabel extends JLabel {
    /**
     * Creates a new <code>TimeLabel</code> displaying time zero.
     */
    public TimeLabel() {
        super(TimeLabel.timeToString(0), SwingConstants.RIGHT);
    }

    /**
     * Sets the preferred size of the label according to the maximum time that
     * it could hold.
     *
     * @param milliseconds the maximum possible time in milliseconds.
     * @throws IllegalArgumentException if <code>milliseconds</code> is
     *         negative.
     */
    public void setMaxTime(long milliseconds) {
        JLabel test = new JLabel(TimeLabel.timeToString(milliseconds),
                SwingConstants.RIGHT);
        this.setPreferredSize(test.getPreferredSize());
        this.revalidate();
    }

    /**
     * Sets the text for this label to be equal to the given time, in the
     * format <code>H:MM:SS.mmm</code>.
     *
     * @param milliseconds the time expressed in milliseconds.
     * @throws IllegalArgumentException if <code>milliseconds</code> is
     *         negative.
     */
    public void setTime(long milliseconds) {
        this.setText(TimeLabel.timeToString(milliseconds));
    }

    /**
     * Converts the given number of milliseconds into a <code>String</code> in
     * the format <code>H:MM:SS.mmm</code>.
     *
     * @param milliseconds the number of milliseconds to convert.
     * @return a <code>String</code> representation of the time.
     * @throws IllegalArgumentException if <code>milliseconds</code> is
     *         negative.
     */
    private static String timeToString(long milliseconds) {
        if (milliseconds < 0)
            throw new IllegalArgumentException("Negative milliseconds");

        long hours = milliseconds / 3600000;
        milliseconds -= hours * 3600000;
        long minutes = milliseconds / 60000;
        milliseconds -= minutes * 60000;
        long seconds = milliseconds / 1000;
        milliseconds -= seconds * 1000;

        return "" + hours + ":" + TimeLabel.zeroPrefixed(minutes, 2) + ":"
                + TimeLabel.zeroPrefixed(seconds, 2) + "."
                + TimeLabel.zeroPrefixed(milliseconds, 3);
    }

    /**
     * Returns a <code>String</code> representation of the given value,
     * prefixed with zeros so that it is guaranteed to be exactly the required
     * number of digits in length.
     *
     * @param value the value to convert to a <code>String</code>.
     * @param requiredDigits the required length of the final
     *        <code>String</code>.
     * @return the value converted to a <code>String</code> prefixed with
     *         zeroes as necessary.
     * @throws IllegalArgumentException if the value, when converted to a
     *         <code>String</code>, already exceeds the required number of
     *         digits.
     */
    private static String zeroPrefixed(long value, int requiredDigits) {
        String str = "" + value;
        int length = str.length();
        if (length > requiredDigits)
            throw new IllegalArgumentException("Value too long");
        while (length < requiredDigits) {
            str = "0" + str;
            length++;
        }
        return str;
    }
}
