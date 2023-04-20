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

package ca.ualberta.dbs3.math;

/**
 * The <code>Distribution</code> class represents a set of values, each
 * associated with an index. It is possible to choose an index, such that the
 * probability of an index <code>i</code> being chosen is proportional to its
 * associated value.
 */
public class Distribution {
    /**
     * Each value, divided by the sum of all the values, plus the previous
     * value in the array. In other words, the cumulative distribution function
     * of the values.
     */
    private double[] cdf;

    /**
     * Constructs a new <code>Distribution</code>, from which indices can be
     * chosen with probability proportional to the given values.
     *
     * @param values the value associated with each index.
     * @throws IllegalArgumentException if any provided value is less than
     *         <code>0.0</code>, if the sum of all the values is
     *         <code>0.0</code>, or if the array has length <code>0</code>.
     */
    public Distribution(double[] values) {
        /* Sum up all the values, and find the last non-zero entry */
        double sum = 0.0;
        int lastNonZero = -1;
        if (values.length == 0)
            throw new IllegalArgumentException("Illegal value array length");
        for (int i = 0; i < values.length; i++) {
            if (values[i] < 0.0 || Double.isInfinite(values[i])
                    || Double.isNaN(values[i]))
                throw new IllegalArgumentException(
                        "Illegal value " + values[i]);
            sum += values[i];
            if (values[i] != 0.0)
                lastNonZero = i;
        }
        if (sum == 0.0)
            throw new IllegalArgumentException("All values sum to zero");

        /* Build the CDF */
        this.cdf = new double[values.length];
        double prev = 0.0;
        for (int i = 0; i < lastNonZero; i++) {
            prev += values[i] / sum;
            this.cdf[i] = prev;
        }
        for (int i = lastNonZero; i < values.length; i++)
            this.cdf[i] = 1.0;
    }

    /**
     * Returns the probability distribution function for this distribution.
     *
     * @return the probability distribution function.
     */
    public double[] getPDF() {
        double[] pdf = new double[this.cdf.length];
        pdf[0] = this.cdf[0];
        for (int i = 1; i < pdf.length; i++)
            pdf[i] = this.cdf[i] - this.cdf[i - 1];
        return pdf;
    }

    /**
     * Returns the probability than a given index will be returned by the
     * {@link #getIndex} function. Note that <code>getProbability(i)</code> is
     * equivalent to <code>getPDF()[i]</code>.
     *
     * @param index the index for which to check the probability.
     * @return the probability that index will be returned by the
     *         <code>getIndex</code> function.
     * @throws ArrayIndexOutOfBoundsException if <code>index</code> is less
     *         than zero, or greater than or equal to the length of the array
     *         given to the constructor.
     */
    public double getProbability(int index) {
        if (index == 0)
            return this.cdf[0];
        else
            return (this.cdf[index] - this.cdf[index - 1]);
    }

    /**
     * Returns an index from <code>0</code> to <code>length-1</code>, where
     * <code>length</code> was the length of the values array used to construct
     * this <code>Distribution</code>. The index is chosen proportionally to
     * the given values.
     *
     * @param prng the pseudo-random number generator to use.
     * @return an index chosen proportionally to the given values.
     */
    public int getIndex(Random prng) {
        double r = prng.nextDouble();
        int low = 0;
        int high = this.cdf.length - 1;

        /*
         * Find the index i such that cdf[i-1] < r <= cdf[i]. Pretend cdf[-1] <
         * 0, to handle the border case.
         */
        while (true) {
            int i = low + ((high - low) / 2);
            double iVal = this.cdf[i];
            double iMinusOneVal;
            if (i > 0)
                iMinusOneVal = this.cdf[i - 1];
            else
                iMinusOneVal = -1.0;
            if (iMinusOneVal >= r)
                high = i - 1;
            else if (iVal < r)
                low = i + 1;
            else
                return i;
        }
    }

    /**
     * Tests is this <code>Distribution</code> is equal to another
     * <code>Distribution</code>. Two <code>Distribution</code>s are equal if
     * their cumulative distribution functions are identical.
     *
     * @param o the object for which to test equality.
     * @return <code>true</code> if the two <code>Distribution</code>s are the
     *         same, otherwise <code>false</code>.
     */
    public boolean equals(Object o) {
        if ((o == null) || (!(o instanceof Distribution)))
            return false;
        Distribution other = (Distribution) o;
        if (this.cdf.length != other.cdf.length)
            return false;
        for (int i = 0; i < this.cdf.length; i++) {
            if (this.cdf[i] != other.cdf[i])
                return false;
        }
        return true;
    }

    /**
     * Returns a hash code for this <code>Distribution</code>.
     *
     * @return a hash code for this <code>Distribution</code>, equal to the
     *         hash code of the first value in the cumulative distribution
     *         function.
     */
    public int hashCode() {
        return Double.valueOf(this.cdf[0]).hashCode();
    }
}
