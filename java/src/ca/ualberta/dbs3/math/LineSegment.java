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
 * The <code>LineSegment</code> class represents a line segment between two
 * points in 2D space.
 */
public class LineSegment {
    /**
     * One endpoint of the line. The endpoints are sorted, so this point has an
     * x coordinate guaranteed to be less than or equal to the x coordinate of
     * the other point. If the x coordinates are equal, this point has a y
     * coordinate less than the y coordinate of the other point.
     */
    private Point p1;

    /**
     * The other endpoint of the line segment.
     */
    private Point p2;

    /**
     * Creates a new line segment between the two given endpoints.
     *
     * @param a one endpoint for the line segment.
     * @param b the other endpoint, distinct from <code>a</code>.
     * @throws IllegalArgumentException if the <code>Point</code>s are equal.
     */
    public LineSegment(Point a, Point b) {
        /* Assign the further left (or further down, if tied) point as p1 */
        int cmp = a.compareTo(b);
        if (cmp < 0) {
            this.p1 = a;
            this.p2 = b;
        } else if (cmp > 0) {
            this.p1 = b;
            this.p2 = a;
        } else
            throw new IllegalArgumentException("Equal points");
    }

    /**
     * Returns a new <code>LineSegment</code> moved by the given
     * <code>Vector</code>.
     *
     * @param vector the amount by which to move this <code>LineSegment</code>.
     * @return a new <code>LineSegment</code> at the new location.
     */
    public LineSegment add(Vector vector) {
        return new LineSegment(this.p1.add(vector), this.p2.add(vector));
    }

    /**
     * Returns an array containing the two <code>Point</code>s that constitute
     * this line segment. The endpoints are sorted, so the point at index
     * <code>0</code> has an x coordinate guaranteed to be less than or equal
     * to the x coordinate of the other point. If the x coordinates are equal,
     * the point at index <code>0</code> has a y coordinate less than the y
     * coordinate of the other point.
     *
     * @return a length <code>2</code> array.
     */
    public Point[] getPoints() {
        Point[] p = new Point[2];
        p[0] = this.p1;
        p[1] = this.p2;
        return p;
    }

    /**
     * Returns the Euclidean length of the line segment.
     *
     * @return the Euclidean length of the line segment.
     */
    public double getLength() {
        return this.p1.distance(this.p2);
    }

    /**
     * Returns a random point on this line segment.
     *
     * @param prng the pseudo-random number generator to use.
     * @return a random <code>Point</code> on this line segment.
     */
    public Point getRandomPoint(Random prng) {
        Vector p1p2 = new Vector(this.p1, this.p2);
        return this.p1.add(p1p2.multiply(prng.nextDouble()));
    }

    /**
     * Returns the point at the centre of this line segment.
     *
     * @return the <code>Point</code> on the centre of this segment.
     */
    public Point getCentrePoint() {
        Vector p1p2 = new Vector(this.p1, this.p2);
        return this.p1.add(p1p2.multiply(0.5));
    }

    /**
     * Returns a new <code>Vector</code> representing the direction and length
     * of this line segment.
     *
     * @return a <code>Vector</code> representation of this line segment.
     */
    public Vector getVector() {
        return new Vector(this.p1, this.p2);
    }

    /**
     * Returns a line representing an infinite-length version of this line
     * segment.
     *
     * @return an infinite-length line version of the line segment.
     */
    public Line getExtension() {
        return new Line(this.p1, this.p2);
    }

    /**
     * Returns whether or not there is any intersection between this line
     * segment and the given line segment.
     *
     * @param other the other line segment.
     * @return <code>true</code> if the two line segments intersect at one or
     *         more points, <code>false</code> otherwise.
     */
    public boolean intersects(LineSegment other) {
        /*
         * Consider this segment to be an infinite-length line; ensure that the
         * two points of the other segment lie on opposite sides of the line by
         * checking the direction you turn while travelling from this segment's
         * p1 to this segment's p2, then turning towards the other segment's
         * endpoints. Repeat the procedure by extending the other segment to an
         * infinite-length line, and checking the two points of this segment.
         */
        return LineSegment.orientation(this.p1, this.p2, other.p1)
                * LineSegment.orientation(this.p1, this.p2, other.p2) <= 0
                && LineSegment.orientation(other.p1, other.p2, this.p1)
                        * LineSegment.orientation(other.p1, other.p2,
                                this.p2) <= 0;
    }

    /**
     * Returns a <code>String</code> representation of this line segment.
     *
     * @return a <code>String</code> representation of this line segment.
     */
    public String toString() {
        return "" + this.p1 + " to " + this.p2;
    }

    /**
     * Tests if this <code>LineSegment</code> is equal to another
     * <code>LineSegment</code>. Two <code>LineSegment</code>s are equal if
     * their two endpoints are equal.
     *
     * @param o the object for which to test equality.
     * @return <code>true</code> if the two <code>LineSegment</code>s are the
     *         same, otherwise <code>false</code>.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof LineSegment) {
            LineSegment other = (LineSegment) o;
            return (this.p1.equals(other.p1) && this.p2.equals(other.p2));
        }
        return false;
    }

    /**
     * Returns a hash code for this <code>LineSegment</code>.
     *
     * @return a hash code for this <code>LineSegment</code>, formed by
     *         combining the hash codes of its two endpoints.
     */
    public int hashCode() {
        return ((this.p1.hashCode() & 0xF0F0F0F0)
                | (this.p2.hashCode() & 0x0F0F0F0F));

    }

    /**
     * Computes the direction in which you must turn (clockwise or
     * counterclockwise) if you are travelling from <code>p0</code> to
     * <code>p1</code>, and turn in the direction of the point <code>p2</code>.
     * This function expects that <code>p0</code> compares strictly less than
     * <code>p1</code>.
     *
     * @param p0 the origin.
     * @param p1 the original destination.
     * @param p2 the point to which you are turning.
     * @return <code>1</code> if the turn is counterclockwise or
     *         <code>-1</code> if the turn is clockwise. If the three points
     *         are colinear, return: <code>0</code> if <code>p2</code> is
     *         between <code>p0</code> and <code>p1</code>, or if
     *         <code>p2</code> is equal to either of those points;
     *         <code>1</code> if <code>p1</code> is in the middle; or
     *         <code>-1</code> if <code>p0</code> is in the middle.
     * @throws IllegalArgumentException if <code>p1</code> is not strictly
     *         greater than <code>p0</code>.
     */
    private static int orientation(Point p0, Point p1, Point p2) {
        if (p0.compareTo(p1) >= 0)
            throw new IllegalArgumentException("Invalid orientation points");

        int ret = p0.turn(p1, p2);
        if (ret != 0)
            return ret;

        /*
         * The points are colinear. Arbitrary returns for when points are
         * colinear correct for degenerate intersection cases, as per Dr.
         * Donald Simon's notes titled "Computational Geometry"
         * http://www.mathcs.duq.edu/simon/Fall06/cs300notes3.html Retrieved 3
         * May 2010.
         */
        int p2p0 = p2.compareTo(p0);
        if (p2p0 >= 0 && p2.compareTo(p1) <= 0)
            return 0;
        else if (p2p0 < 0)
            return -1;
        else
            return 1;
    }
}
