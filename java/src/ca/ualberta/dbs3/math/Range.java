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
 * The <code>Range</code> class represents an inclusive range between two
 * numbers on the real number line from which you can draw random values.
 */
public abstract class Range {
    /**
     * The low end of the inclusive range.
     */
    protected double min;

    /**
     * The high end of the inclusive range.
     */
    protected double max;

    /**
     * Creates a new <code>Range</code> between the two given numbers,
     * inclusive.
     *
     * @param min the minimum end of the range.
     * @param max the maximum end of the range.
     * @throws IllegalArgumentException if <code>min</code> is greater than
     *         <code>max</code>.
     */
    public Range(double min, double max) {
        if (min > max)
            throw new IllegalArgumentException("Invalid range specification");
        this.min = min;
        this.max = max;
    }

    /**
     * Returns the lower bound of this range.
     *
     * @return the lower bound of this range.
     */
    public double getMin() {
        return this.min;
    }

    /**
     * Returns the upper bound of this range.
     *
     * @return the upper bound of this range.
     */
    public double getMax() {
        return this.max;
    }

    /**
     * Returns the expected value of random values drawn from this range.
     *
     * @return the expected value of {@link #getRandom}.
     */
    public abstract double expectedValue();

    /**
     * Returns a random value selected from this range.
     *
     * @param prng the pseudorandom number generator to use.
     * @return a value drawn from this range.
     */
    public abstract double getRandom(Random prng);
}
