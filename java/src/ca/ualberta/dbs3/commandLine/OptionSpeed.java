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

package ca.ualberta.dbs3.commandLine;

/**
 * A <code>OptionSpeed</code> represents a command line option for choosing a
 * range of possible speeds for agents.
 */
public class OptionSpeed extends OptionRange {
    /**
     * The default minimum speed in metres per second to use, if none is
     * specified.
     */
    public static final double DEFAULT_MIN_SPEED = 0.83;

    /**
     * The default maximum speed in metres per second to use, if none is
     * specified.
     */
    public static final double DEFAULT_MAX_SPEED = 2.21;

    /**
     * The default distribution from which to draw speed values, and also the
     * index into {@link OptionRange#getDistributions} to use.
     */
    public static final int DEFAULT_DISTRIBUTION =
            OptionRange.DISTRIBUTION_NORMAL;

    /**
     * Creates a new <code>OptionSpeed</code> to be added to a parser.
     */
    public OptionSpeed() {
        super("Speed", "speed", "m/s", "MPS", OptionSpeed.DEFAULT_MIN_SPEED,
                OptionSpeed.DEFAULT_MAX_SPEED,
                OptionSpeed.DEFAULT_DISTRIBUTION);
    }
}
