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
 * The <code>Street</code> class represents a named road on which a mobile
 * agent can travel.
 */
public class Street extends Road implements Comparable<Street> {
    /**
     * The human-readable name of this street.
     */
    private String name;

    /**
     * A numerical identifier for this street.
     */
    private int id;

    /**
     * Creates a new <code>Street</code> with the given name, location, and
     * size.
     *
     * @param name the human-readable name for this street.
     * @param id a numerical identifier for the street.
     * @param midline a line segment defining the median of the street.
     * @param width the width of the street, in metres.
     * @throws IllegalArgumentException if the width is less than or equal to
     *         zero.
     */
    public Street(String name, int id, LineSegment midline, double width) {
        super(midline, width);
        this.name = name;
        this.id = id;
    }

    /**
     * Returns the numerical identifier of this street.
     *
     * @return the numerical identifier.
     */
    public int getID() {
        return this.id;
    }

    /**
     * Returns the human-readable name of this street.
     *
     * @return the human-readable name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the centre point of the intersection of this <code>Street</code>
     * with the given <code>Street</code>, or <code>null</code> if they do not
     * intersect.
     *
     * @param other the other street.
     * @return the centre point of the intersection, or <code>null</code> if
     *         there is no intersection.
     * @throws IllegalArgumentException if the street midlines intersect at
     *         more than one point, or if one street is contained within the
     *         other.
     */
    public Point intersectionPoint(Street other) {
        /*
         * First, check for the pathological case of one street being contained
         * within the other.
         */
        if (this.contains(other) || other.contains(this))
            throw new IllegalArgumentException("Street contained in another");

        /*
         * Consider the two streets to intersect if there is any intersection
         * of the bounding lines. If such an intersection exists, extend both
         * of the midlines to infinity, and check where they intersect. If they
         * are parallel, that function will return null. Parallel streets
         * should not intersect.
         */
        if (this.boundingLinesIntersect(other)) {
            Line extA = this.midline.getExtension();
            Line extB = other.midline.getExtension();
            Point intPt = extA.getIntersection(extB);
            if (intPt == null)
                throw new IllegalArgumentException(
                        "Parallel streets intersecting");
            return intPt;
        } else
            return null;
    }

    /**
     * Compares this <code>Street</code> with the specified <code>Street</code>
     * for order. These objects are sorted according to their numerical IDs.
     *
     * @param o the <code>Street</code> to which to compare this object.
     * @return a negative number if this <code>Street</code> has a smaller ID
     *         than the given one, a positive number if it is larger, and
     *         <code>0</code> if they are equal.
     */
    public int compareTo(Street o) {
        return (this.id - o.id);
    }

    /**
     * Tests is this <code>Street</code> is equal to another
     * <code>Street</code>. Two <code>Streets</code>s are considered equal if
     * their numerical identifiers are the same.
     *
     * @param o the object for which to test equality.
     * @return <code>true</code> if the two <code>Streets</code>s are the same,
     *         otherwise <code>false</code>.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof Street) {
            Street other = (Street) o;
            return (this.id == other.id);
        }
        return false;
    }

    /**
     * Returns a hash code for this <code>Street</code>.
     *
     * @return a hash code for this <code>Street</code>, equal to the hash code
     *         of its numerical identifier.
     */
    public int hashCode() {
        return Integer.valueOf(this.id).hashCode();
    }
}
