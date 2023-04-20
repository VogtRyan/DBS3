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
 * The <code>RangeUniform</code> class is a range of real numbers from which
 * you can draw values at uniform random.
 */
public class RangeUniform extends Range {
    /**
     * Creates a new <code>RangeUniform</code> between the two given numbers,
     * inclusive.
     *
     * @param min the minimum end of the range.
     * @param max the maximum end of the range.
     * @throws IllegalArgumentException if <code>min</code> is greater than
     *         <code>max</code>.
     */
    public RangeUniform(double min, double max) {
        super(min, max);
    }

    /**
     * Returns the expected value of random values drawn from this range.
     *
     * @return the expected value of {@link #getRandom}.
     */
    public double expectedValue() {
        return (this.min + (this.max - this.min) * 0.5);
    }

    /**
     * Returns a random number selected uniformly from this range.
     *
     * @param prng the pseudorandom number generator to use.
     * @return a uniformly selected random number from this range.
     */
    public double getRandom(Random prng) {
        double d = prng.nextDouble();
        return (this.min + (this.max - this.min) * d);
    }
}
