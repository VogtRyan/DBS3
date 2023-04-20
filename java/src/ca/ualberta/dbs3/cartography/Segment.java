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
 * The <code>Segment</code> class represents a portion of a {@link Street} in
 * between two intersections or endpoints of the street.
 */
public class Segment extends Road implements Comparable<Segment> {
    /**
     * The street that this segment is part of.
     */
    Street street;

    /**
     * The smaller of the two segmentation points, used for sorting segments.
     */
    SegmentationPoint sortPoint;

    /**
     * The larger of the two segmentation points.
     */
    SegmentationPoint otherPoint;

    /**
     * Creates a new <code>Segment</code> between the two given segmentation
     * points on a street.
     *
     * @param pointA one of the segmentation points.
     * @param pointB the other segmentation point.
     * @throws IllegalArgumentException if the two points are for different
     *         streets.
     */
    public Segment(SegmentationPoint pointA, SegmentationPoint pointB) {
        super(new LineSegment(pointA.getPoint(), pointB.getPoint()),
                pointA.getStreet().getWidth());

        this.street = pointA.getStreet();
        if (this.street.equals(pointB.getStreet()) == false)
            throw new IllegalArgumentException("Different streets");

        if (pointA.compareTo(pointB) < 0) {
            this.sortPoint = pointA;
            this.otherPoint = pointB;
        } else {
            this.sortPoint = pointB;
            this.otherPoint = pointA;
        }
    }

    /**
     * Returns the street that contains this segment.
     *
     * @return the street that contains this segment.
     */
    public Street getStreet() {
        return this.street;
    }

    /**
     * Returns a human-readable description of the location of this segment.
     *
     * @return a human-readable description of the segment.
     */
    public String getDescription() {
        return this.street.getName() + ", between "
                + this.sortPoint.getDescription() + " and "
                + this.otherPoint.getDescription();
    }

    /**
     * Returns whether this <code>Segment</code> occurs before the given
     * <code>SegmentationPoint</code> in the natural ordering of all of the
     * segments on all of the streets.
     *
     * @param sp the segmentation point.
     * @return <code>true</code> if this <code>Segment</code> occurs before the
     *         given <code>SegmentationPoint</code>, <code>false</code>
     *         otherwise.
     */
    public boolean occursBefore(SegmentationPoint sp) {
        return (this.otherPoint.compareTo(sp) <= 0);
    }

    /**
     * Compares this <code>Segment</code> with the specified
     * <code>Segment</code> for order. These objects are sorted by their
     * natural ordering in progression along all of the streets.
     *
     * @param o the <code>Segment</code> to which to compare this object.
     * @return a negative number if this <code>Segment</code> is smaller than
     *         the given <code>Segment</code>, a positive number if it is
     *         larger, and <code>0</code> if they are equal.
     */
    public int compareTo(Segment o) {
        int pRet = this.sortPoint.compareTo(o.sortPoint);
        if (pRet != 0)
            return pRet;

        /* Keep compareTo consistent with equals */
        return this.otherPoint.compareTo(o.otherPoint);
    }

    /**
     * Tests is this <code>Segment</code> is equal to another
     * <code>Segment</code>. Two <code>Segment</code>s are equal if they
     * represent the same points on the same street.
     *
     * @param o the object for which to test equality.
     * @return <code>true</code> if the two <code>Segment</code>s are the same,
     *         otherwise <code>false</code>.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof Segment) {
            Segment other = (Segment) o;
            return (this.sortPoint.equals(other.sortPoint)
                    && this.otherPoint.equals(other.otherPoint));
        }
        return false;
    }

    /**
     * Returns a hash code for this <code>Segment</code>.
     *
     * @return a hash code for this <code>Segment</code>, equal to the hash
     *         code of its lower segmentation point.
     */
    public int hashCode() {
        return this.sortPoint.hashCode();
    }
}
