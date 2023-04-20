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

package ca.ualberta.dbs3.cartography;

import ca.ualberta.dbs3.math.*;

/**
 * The <code>Road</code> class represents any portion of straight roadway,
 * either a street or portion thereof.
 */
public abstract class Road extends ParallelogramShaped {
    /**
     * The line segment that defines the median of the road.
     */
    protected LineSegment midline;

    /**
     * The width of the road.
     */
    protected double width;

    /**
     * Creates a new <code>Road</code> with the given median line and width.
     *
     * @param midline the median line of the <code>Road</code>.
     * @param width the width of the <code>Road</code>.
     * @throws IllegalArgumentException if the width is less than or equal to
     *         zero.
     */
    public Road(LineSegment midline, double width) {
        super(midline.getCentrePoint(), midline.getVector().multiply(0.5),
                midline.getVector().orthogonal().normalize(width / 2));

        if (width <= 0.0)
            throw new IllegalArgumentException("Invalid road width");
        this.midline = midline;
        this.width = width;
    }

    /**
     * Returns the line segment defining the median of the road.
     *
     * @return the line segment defining the median of the road.
     */
    public LineSegment getMidline() {
        return this.midline;
    }

    /**
     * Returns the width of the road.
     *
     * @return the width of the road.
     */
    public double getWidth() {
        return this.width;
    }

    /**
     * Returns the length of the road.
     *
     * @return the length of the road.
     */
    public double getLength() {
        return this.midline.getLength();
    }
}
