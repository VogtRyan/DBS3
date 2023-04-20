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
 * The <code>Line</code> class represents an infinite-length line on the 2D
 * plane.
 */
public class Line {
    /**
     * One point on the line.
     */
    private Point p1;

    /**
     * Another point on the line.
     */
    private Point p2;

    /**
     * The vector showing the direction and magnitude from <code>p1</code> to
     * <code>p2</code>.
     */
    private Vector p1p2;

    /**
     * The slope of the line.
     */
    private double slope;

    /**
     * Creates a new <code>Line</code> equivalent to the infinite-length
     * extension of the line segment between two distinct points.
     *
     * @param aPoint the first point.
     * @param bPoint the second point.
     * @throws IllegalArgumentException if the <code>Point</code>s are equal.
     */
    public Line(Point aPoint, Point bPoint) {
        if (aPoint.equals(bPoint))
            throw new IllegalArgumentException("Equal points for line");
        this.p1 = aPoint;
        this.p2 = bPoint;
        this.p1p2 = new Vector(this.p1, this.p2);
        if (this.p1p2.getDeltaX() == 0.0)
            this.slope = Double.POSITIVE_INFINITY;
        else
            this.slope = this.p1p2.getDeltaY() / this.p1p2.getDeltaX();
    }

    /**
     * Computes the unique intersection point between this line and the given
     * line.
     *
     * @param other the line with which to compute the intersection.
     * @return a <code>Point</code> representing the intersection point, or
     *         <code>null</code> if there are no intersection points (i.e.,
     *         parallel non-equal lines) or infinite intersection points (i.e.,
     *         the two lines are equal).
     */
    public Point getIntersection(Line other) {
        /*
         * Fast test before the complex calculations below: lines with equal
         * slope cannot have a unique intersection point.
         */
        if (this.slope == other.slope)
            return null;

        /*
         * Denote this line as L = A + l*(B - A)
         * Denote the other as M = C + m*(D - C)
         * where l and m are scalars, and A and B are two points in this line
         * and C and D are two point in the other line.  Define Ldx as the
         * change in x between the two points in L (i.e., Ldx = Bx - Ax), etc.
         * The lines intersect where L = M, for unknown scalars l and m
         * We need only solve L = M for l and m:
         * 
         * [Ax] + l * [Ldx] = [Cx] + m * [Mdx]
         * [Ay]       [Ldy]   [Cy]       [Mdy]
         *
         * ->
         * 
         * [Ldx    -Mdx] * [l] = [Cx - Ax]
         * [Ldy    -Mdy]   [m]   [Cy - Ay]
         */
        Matrix matrix =
                new Matrix(this.p1p2.getDeltaX(), -other.p1p2.getDeltaX(),
                        this.p1p2.getDeltaY(), -other.p1p2.getDeltaY());
        Vector vector = new Vector(other.p1.getX() - this.p1.getX(),
                other.p1.getY() - this.p1.getY());
        if (matrix.numSolutions(vector) != 1)
            return null;

        /*
         * While the computational intersection algorithm is quite numerically
         * stable, these explicit checks are used because one can reasonably
         * expect, after the following code is run (where a != b and b != c):
         *     Line one = new Line(a, b);
         *     Line two = new Line(b, c);
         *     Point intersection = one.getIntersection(two);
         * that the following expression:
         *     intersection == null || intersection.equals(b)
         * would be guaranteed to return true
         */
        if (this.p1.equals(other.p1) || this.p1.equals(other.p2))
            return this.p1;
        else if (this.p2.equals(other.p1) || this.p2.equals(other.p2))
            return this.p2;

        /*
         * Solve the intersection numerically. First, solve for the scalars l
         * and m. Then, using L = A + l * (B - A), find the intersection point.
         */
        Vector lm = matrix.solve(vector);
        return this.p1.add(this.p1p2, lm.getDeltaX());
    }

    /**
     * Tests is this <code>Line</code> is equal to another <code>Line</code>.
     * Two <code>Line</code>s are equal if they define the same
     * infinite-cardinality set of points on the 2D plane.
     *
     * @param o the object for which to test equality.
     * @return <code>true</code> if the two <code>Line</code>s are the same,
     *         otherwise <code>false</code>.
     */
    public boolean equals(Object o) {
        if ((o == null) || (!(o instanceof Line)))
            return false;
        Line other = (Line) o;

        /*
         * Fast test before complex calculations below: equal lines must have
         * equal slope
         */
        if (this.slope != other.slope)
            return false;

        /* See comments in the getIntersection function */
        Matrix matrix =
                new Matrix(this.p1p2.getDeltaX(), -other.p1p2.getDeltaX(),
                        this.p1p2.getDeltaY(), -other.p1p2.getDeltaY());
        Vector vector = new Vector(other.p1.getX() - this.p1.getX(),
                other.p1.getY() - this.p1.getY());
        return (matrix.numSolutions(vector) > 1);
    }

    /**
     * Returns a hash code for this <code>Line</code>.
     *
     * @return a hash code for this <code>Line</code>, equal to the hash code
     *         of its slope.
     */
    public int hashCode() {
        return Double.valueOf(this.slope).hashCode();
    }
}
