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

import java.util.Arrays;

/**
 * The <code>Parallelogram</code> class represents a parallelogram on the
 * two-dimensional plane.
 */
public class Parallelogram extends ParallelogramShaped {
    /**
     * Creates a new parallelogram centred at the given point, with its four
     * edges defined by the two given vectors.
     *
     * @param centre the centre point of the parallelogram.
     * @param parAB a vector from the centre point, running parallel to the
     *        edges A and B, reaching edge C.
     * @param parCD a vector from the centre point, running parallel to the
     *        edges C and D, reaching edge A.
     * @throws IllegalArgumentException if the two vectors are not linearly
     *         independent.
     */
    public Parallelogram(Point centre, Vector parAB, Vector parCD) {
        super(centre, parAB, parCD);
    }

    /**
     * Tests is this <code>Parallelogram</code> is equal to another
     * <code>Parallelogram</code>. Two <code>Parallelogram</code>s are equal if
     * they define the same set of points on the 2D plane.
     *
     * @param o the object for which to test equality.
     * @return <code>true</code> if the two <code>Parallelograms</code>s are
     *         the same, otherwise <code>false</code>.
     */
    public boolean equals(Object o) {
        if ((o == null) || (!(o instanceof Parallelogram)))
            return false;
        Parallelogram other = (Parallelogram) o;

        /* Fast test: the centre points must be equal */
        if (this.getCentrePoint().equals(other.getCentrePoint()) == false)
            return false;

        /* Ensure that all four corner points are the same */
        Point[] me = this.getCornerPoints();
        Point[] you = other.getCornerPoints();
        Arrays.sort(me);
        Arrays.sort(you);
        for (int i = 0; i < me.length; i++) {
            if (me[i].equals(you[i]) == false)
                return false;
        }
        return true;
    }

    /**
     * Returns a hash code for this <code>Parallelogram</code>.
     *
     * @return a hash code for this <code>Parallelogram</code>, equal to the
     *         hash code of its centre point.
     */
    public int hashCode() {
        return this.getCentrePoint().hashCode();
    }
}
