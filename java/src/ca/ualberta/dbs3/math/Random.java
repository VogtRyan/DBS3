/*
 * Copyright (c) 2011-2023 Ryan Vogt <rvogt@ualberta.ca>
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
 * The <code>Random</code> class is a partial reimplementation of the standard
 * Java 6 {@link java.util.Random} class, with the ability to copy the state of
 * the pseudorandom number generator. The code in this class is largely based
 * off the descriptions and examples in the Java API, version 6.
 */
public class Random {
    /**
     * The seed used to generate the next random number.
     */
    private long seed;

    /**
     * A buffered Gaussian value to be returned by {@link #nextGaussian}.
     */
    private double nextGaussian;

    /**
     * Whether <code>nextGaussian</code> contains a buffered value.
     */
    private boolean hasNextGaussian;

    /**
     * Creates a new pseudorandom number generator with the given seed.
     *
     * @param seed the initial seed to use.
     */
    public Random(long seed) {
        this.seed = (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1);
        this.hasNextGaussian = false;
    }

    /**
     * Creates a new pseudorandom number generator that is an exact copy of the
     * given random number generator.
     *
     * @param prng the random number generator to copy.
     */
    public Random(Random prng) {
        this.seed = prng.seed;
        this.nextGaussian = prng.nextGaussian;
        this.hasNextGaussian = prng.hasNextGaussian;
    }

    /**
     * Returns a uniformly distributed value between zero (inclusive) and the
     * given value (exclusive).
     *
     * @param n the exclusive upper bound of the value returned.
     * @return a uniformly distributed value in the given bounds.
     * @throws IllegalArgumentException if the bound is less than or equal to
     *         zero.
     */
    public int nextInt(int n) {
        if (n <= 0)
            throw new IllegalArgumentException("Invalid value");

        /* Fast approach if n is a power of 2 */
        if ((n & -n) == n)
            return (int) ((n * (long) (this.next(31))) >> 31);

        int bits, val;
        do {
            bits = this.next(31);
            val = bits % n;
        } while (bits - val + (n - 1) < 0);
        return val;
    }

    /**
     * Returns a uniformly distributed value between <code>0.0</code> and
     * <code>1.0</code>.
     *
     * @return a uniformly distributed unit value.
     */
    public double nextDouble() {
        return (((long) (this.next(26)) << 27) + this.next(27))
                / ((double) (1L << 53));
    }

    /**
     * Returns a normally distributed value with a mean of <code>0.0</code> and
     * a standard deviation of <code>1.0</code>.
     *
     * @return a Guassian value.
     */
    public double nextGaussian() {
        if (this.hasNextGaussian) {
            this.hasNextGaussian = false;
            return this.nextGaussian;
        }

        double v1, v2, s;
        do {
            v1 = 2 * this.nextDouble() - 1;
            v2 = 2 * this.nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
        } while (s >= 1.0 || s == 0.0);
        double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
        this.nextGaussian = v2 * multiplier;
        this.hasNextGaussian = true;
        return v1 * multiplier;
    }

    /**
     * Returns a random integer value in which the low-order given number of
     * bits may be set.
     *
     * @param bits the number of bits which may be set.
     * @return a random integer value.
     */
    private int next(int bits) {
        this.seed = (this.seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        return (int) (this.seed >>> (48 - bits));
    }
}
