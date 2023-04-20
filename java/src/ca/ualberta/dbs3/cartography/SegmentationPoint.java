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
 * The <code>SegmentationPoint</code> class represents a point at which a
 * street should be segmented. It is unsafe to sort the segmentation points
 * using the implementation of {@link Comparable} in {@link Point}: that
 * implementation just sorts points by their X and Y coordinates. Due to
 * numerical stability concerns, the ordering of these coordinates may not
 * reflect the natural ordering of the segmentation points along a street (that
 * is, the ordering from one end of the street to the other). This class makes
 * a best effort to rectify that situation.
 */
public class SegmentationPoint implements Comparable<SegmentationPoint> {
    /**
     * The point in space where this segmentation point occurs.
     */
    private Point point;

    /**
     * The street for which this segmentation point exists.
     */
    private Street street;

    /**
     * The signed distance from this segmentation point to the start of the
     * street that it segments.
     */
    private double distToStreetStart;

    /**
     * The description of why this segmentation point exists.
     */
    private String description;

    /**
     * Creates a new segmentation point at the given point in space on the
     * given street.
     *
     * @param point the point in space.
     * @param street the street being segmented.
     * @param description a description of why this segmentation point exists.
     */
    public SegmentationPoint(Point point, Street street, String description) {
        this.point = point;
        this.street = street;
        this.description = description;

        /*
         * Compute a signed distance to the start of the street. Throw a
         * negative sign onto the distance if it is further from the end of the
         * street than the start of the street is -- this takes into account
         * that an intersection point can technically be computed beyond the
         * end of a street.
         */
        Point[] mlp = street.getMidline().getPoints();
        this.distToStreetStart = mlp[0].distance(point);
        if (mlp[1].distance(point) > street.getLength())
            this.distToStreetStart = -this.distToStreetStart;
    }

    /**
     * Returns the point in space at which the segmentation occurs.
     *
     * @return the point in space of segmentation.
     */
    public Point getPoint() {
        return this.point;
    }

    /**
     * Returns the street for which this segmentation point exists.
     *
     * @return the street for which this segmentation point exists.
     */
    public Street getStreet() {
        return this.street;
    }

    /**
     * Returns a human-readable description of why this segmentation point
     * exists.
     *
     * @return a description of why this segmentation point exists.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Compares this <code>SegmentationPoint</code> with the specified
     * <code>SegmentationPoint</code> for order. These objects are sorted first
     * by the street on which they are located, and secondly by their natural
     * ordering in progression along the street.
     *
     * @param o the <code>SegmentationPoint</code> to which to compare this
     *        object.
     * @return a negative number if this <code>SegmentationPoint</code> is
     *         smaller than the given one, a positive number if it is larger,
     *         and <code>0</code> if they are equal.
     */
    public int compareTo(SegmentationPoint o) {
        int strCmp = this.street.compareTo(o.street);
        if (strCmp != 0)
            return strCmp;

        if (this.distToStreetStart < o.distToStreetStart)
            return -1;
        else if (this.distToStreetStart > o.distToStreetStart)
            return 1;
        else {
            /*
             * Here, the two points are likely to be physically the same point,
             * but we could have triggered a corner condition, by some trick of
             * floating point stability. This is an arbitrary but consistent
             * return value if the two points are not equal, or 0 if they are.
             */
            return this.point.compareTo(o.point);
        }
    }

    /**
     * Tests is this <code>SegmentationPoint</code> is equal to another
     * <code>SegmentationPoint</code>. Two <code>SegmentationPoint</code>s are
     * equal if they represent the same point in space on the same street.
     *
     * @param o the object for which to test equality.
     * @return <code>true</code> if the two <code>SegmentationPoint</code>s are
     *         the same, otherwise <code>false</code>.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof SegmentationPoint) {
            SegmentationPoint other = (SegmentationPoint) o;
            return ((this.distToStreetStart == other.distToStreetStart)
                    && this.street.equals(other.street)
                    && this.point.equals(other.point));
        }
        return false;
    }

    /**
     * Returns a hash code for this <code>SegmentationPoint</code>.
     *
     * @return a hash code for this <code>SegmentationPoint</code>, equal to
     *         the hash code of its distance to the start of the street.
     */
    public int hashCode() {
        return Double.valueOf(this.distToStreetStart).hashCode();
    }
}
