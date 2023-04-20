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
 * The <code>Point</code> class represents a single point in 2D space.
 */
public class Point implements Comparable<Point> {
    /**
     * The x coordinate.
     */
    private double x;

    /**
     * The y coordinate.
     */
    private double y;

    /**
     * Creates a new <code>Point</code> with the given x and y coordinates.
     *
     * @param x the x coordinate.
     * @param y the y coordinate.
     */
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the x coordinate of the <code>Point</code>.
     *
     * @return the x coordinate.
     */
    public double getX() {
        return this.x;
    }

    /**
     * Returns the y coordinate of the <code>Point</code>.
     *
     * @return the y coordinate.
     */
    public double getY() {
        return this.y;
    }

    /**
     * Returns a new <code>Point</code> moved by the given <code>Vector</code>.
     *
     * @param vector the amount by which to move this <code>Point</code>.
     * @return a new <code>Point</code> at the new location.
     */
    public Point add(Vector vector) {
        return new Point(this.x + vector.getDeltaX(),
                this.y + vector.getDeltaY());
    }

    /**
     * Returns a new <code>Point</code> moved by the given multiple of the
     * given <code>Vector</code>.
     *
     * @param vector the vector specifying the direction to move this
     *        <code>Point</code>.
     * @param multiple the multiple of the vector by which to move this
     *        <code>Point</code>.
     * @return a new <code>Point</code> at the new location.
     */
    public Point add(Vector vector, double multiple) {
        return new Point(this.x + vector.getDeltaX() * multiple,
                this.y + vector.getDeltaY() * multiple);
    }

    /**
     * Returns a new <code>Point</code> moved by the additive inverse of the
     * given <code>Vector</code>.
     *
     * @param vector the amount by which to move this <code>Point</code>.
     * @return a new <code>Point</code> at the new location.
     */
    public Point subtract(Vector vector) {
        return new Point(this.x - vector.getDeltaX(),
                this.y - vector.getDeltaY());
    }

    /**
     * Returns the Euclidean distance between this point and the given point.
     *
     * @param other the point to which to compute the distance.
     * @return the distance between this point and <code>other</code>.
     */
    public double distance(Point other) {
        if (this.x == other.x && this.y == other.y)
            return 0.0;
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Returns the point from the given array of points that is closest to this
     * point.
     *
     * @param possibles the set of possible points.
     * @return the point that is closest to this one.
     * @throws IllegalArgumentException if the given array of points has length
     *         <code>0</code>.
     */
    public Point closest(Point[] possibles) {
        if (possibles.length == 0)
            throw new IllegalArgumentException("No possible points");

        Point minPoint = possibles[0];
        double minDist = this.distance(possibles[0]);
        for (int i = 1; i < possibles.length; i++) {
            double dist = this.distance(possibles[i]);
            if (dist < minDist) {
                minDist = dist;
                minPoint = possibles[i];
            }
        }
        return minPoint;
    }

    /**
     * Returns whether this point falls within the given bounds.
     *
     * @param minX the lower X coordinate bound.
     * @param minY the lower Y coordinate bound.
     * @param maxX the upper X coordinate bound.
     * @param maxY the upper Y coordinate bound.
     * @return <code>true</code> if this point's X coordinate is within the two
     *         X bounds and its Y coordinate is within the two Y bounds,
     *         <code>false</code> otherwise.
     * @throws IllegalArgumentException if <code>minX</code> is greater than
     *         <code>maxX</code> or <code>minY</code> is greater than
     *         <code>maxY</code>.
     */
    public boolean fallsWithin(double minX, double minY, double maxX,
            double maxY) {
        if (minX > maxX || minY > maxY)
            throw new IllegalArgumentException("Min and max cross");
        return (minX <= this.x && this.x <= maxX && minY <= this.y
                && this.y <= maxY);
    }

    /**
     * Computes the direction in which you must turn (clockwise or
     * counterclockwise) if you are travelling from this point to
     * <code>dest</code>, and turn in the direction of the point
     * <code>turn</code>.
     *
     * @param dest the destination, moving from this point.
     * @param turn the point to which you are turning.
     * @return <code>1</code> if the turn is counterclockwise, <code>-1</code>
     *         if the turn is clockwise, or <code>0</code> if the three points
     *         are colinear.
     */
    public int turn(Point dest, Point turn) {
        /*
         * Consider vectors v1 = <this to dest>, and v2 = <this to turn> (i.e.,
         * v1 = <dest - this>, and v2 = <turn - this>). For now, assume both
         * deltaX and deltaY for both v1 and v2 are greater than zero.
         *
         * If the slope of v2 is greater than the slope of v1, we turned
         * counterclockwise. If it is less, we turned clockwise; and, if equal,
         * the three points are colinear.
         * 
         * As an example of a counterclockwise turn:
         *     slope(v1) < slope(v2) <=>
         *     deltaY_v1 / deltaX_v1 < deltaY_v2 / deltaX_v2 <=>
         *     deltaY_v1 * deltaX_v2 < deltaY_v2 * deltaX_v1
         * 
         * If we let
         *     lhs = deltaY_v1 * deltaX_v2
         *     rhs = deltaY_v2 * deltaX_v1
         * then we have that the turn was counterclockwise iff lhs < rhs,
         * clockwise iff lhs > rhs, and that the point are colinear iff
         * lhs = rhs.
         *
         * The more general proof that:
         *     lhs < rhs <=> the turn is counterclockwise,
         *     lhs > rhs <=> the turn is clockwise, and
         *     lhs = rhs <=> the points are colinear,
         * even if the deltaX and deltaY may be zero or negative follows from a
         * brute-force approach of looking at all possible quadrants / axes on
         * which the vectors could lie, and deciding whether slope(v1) need be
         * greater or less than slope(v2) in those cases to result in a
         * counterclockwise turn (the inequality sign will flip appropriately).
         */
        double lhs = (dest.y - this.y) * (turn.x - this.x);
        double rhs = (turn.y - this.y) * (dest.x - this.x);
        if (lhs < rhs)
            return 1;
        else if (lhs > rhs)
            return -1;
        else
            return 0;
    }

    /**
     * Returns a <code>String</code> representation of this <code>Point</code>.
     *
     * @return a <code>String</code> representation of this <code>Point</code>.
     */
    public String toString() {
        double outX = Math.round(this.x * 100.0) / 100.0;
        double outY = Math.round(this.y * 100.0) / 100.0;
        return String.format("(%.2f, %.2f)", outX, outY);
    }

    /**
     * Tests is this <code>Point</code> is equal to another <code>Point</code>.
     * Two <code>Point</code>s are equal if their x and y coordinates are the
     * same.
     *
     * @param o the object for which to test equality.
     * @return <code>true</code> if the two <code>Point</code>s are the same,
     *         otherwise <code>false</code>.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof Point) {
            Point other = (Point) o;
            return ((this.x == other.x) && (this.y == other.y));
        }
        return false;
    }

    /**
     * Returns a hash code for this <code>Point</code>.
     *
     * @return a hash code for this <code>Point</code>, constructed by
     *         combining the hash codes of the two coordinates.
     */
    public int hashCode() {
        Double x = Double.valueOf(this.x);
        Double y = Double.valueOf(this.y);
        return ((x.hashCode() & 0xF0F0F0F0) | (y.hashCode() & 0x0F0F0F0F));
    }

    /**
     * Compares this <code>Point</code> with the specified <code>Point</code>
     * for order. These objects are sorted first by their x coordinate, and
     * sub-sorted by their y coordinate.
     *
     * @param o the <code>Point</code> to which to compare this object.
     * @return a negative number if this <code>Point</code> is smaller than the
     *         given <code>Point</code>, a positive number if it is larger, and
     *         <code>0</code> if they are equal.
     */
    public int compareTo(Point o) {
        double diff = this.x - o.x;
        if (diff == 0.0)
            diff = this.y - o.y;
        if (diff < 0.0)
            return -1;
        else if (diff > 0.0)
            return 1;
        else
            return 0;
    }
}
