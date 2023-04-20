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
 * The <code>Correlation</code> class can be used to compute the correlation
 * between a set of pairs of real numbers, in which each pair may or may not be
 * weighted the same.
 */
public class Correlation {
    /**
     * The total weight that has been added to the correlation computer.
     */
    private double weight;

    /**
     * The weighted sum of the x values.
     */
    private double sumWX;

    /**
     * The weighted sum of the y values.
     */
    private double sumWY;

    /**
     * The weighted sum of the squares of the x values.
     */
    private double sumWXX;

    /**
     * The weighted sum of the squares of the y values.
     */
    private double sumWYY;

    /**
     * The weighted sum of the products of the (x, y) pairs.
     */
    private double sumWXY;

    /**
     * Creates a new correlation computer with no pairs of numbers yet added.
     */
    public Correlation() {
        this.weight = 0.0;
        this.sumWX = this.sumWY = 0.0;
        this.sumWXX = this.sumWYY = this.sumWXY = 0.0;
    }

    /**
     * Adds the given ordered pair of numbers to the correlation computer with
     * a weight of <code>1.0</code>.
     *
     * @param x the first value.
     * @param y the second value.
     * @throws IllegalArgumentException if either value is not finite.
     */
    public void add(double x, double y) {
        this.add(x, y, 1.0);
    }

    /**
     * Adds the given ordered pair of numbers to the correlation computer with
     * the given weight placed on its computation.
     *
     * @param x the first value.
     * @param y the second value.
     * @param weight the weight to place on this ordered pair during
     *        computation.
     * @throws IllegalArgumentException if either value in the ordered pair is
     *         not finite, or if the weight if not a non-negative finite value.
     */
    public void add(double x, double y, double weight) {
        if (Double.isInfinite(x) || Double.isNaN(x) || Double.isInfinite(y)
                || Double.isNaN(y))
            throw new IllegalArgumentException("Invalid value to add");
        if (Double.isInfinite(weight) || Double.isNaN(weight) || weight < 0.0)
            throw new IllegalArgumentException("Invalid weight");

        this.weight += weight;
        this.sumWX += weight * x;
        this.sumWY += weight * y;
        this.sumWXX += weight * x * x;
        this.sumWYY += weight * y * y;
        this.sumWXY += weight * x * y;
    }

    /**
     * Returns the linear correlation coefficient, also called 'r.' Will return
     * <code>NaN</code> if fewer than two unique data pairs have been added.
     *
     * @return the linear correlation coefficient.
     */
    public double getCorrelation() {
        double num = this.weight * this.sumWXY - this.sumWX * this.sumWY;
        double d1 = this.weight * this.sumWXX - this.sumWX * this.sumWX;
        double d2 = this.weight * this.sumWYY - this.sumWY * this.sumWY;
        return (num / Math.sqrt(d1 * d2));
    }

    /**
     * Returns the coefficient of determination, also called 'r^2.' Will return
     * <code>NaN</code> if fewer than two unique data pairs have been added.
     *
     * @return the coefficient of determination.
     */
    public double getDetermination() {
        double r = this.getCorrelation();
        return r * r;
    }
}
