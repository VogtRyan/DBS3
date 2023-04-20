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
 * The <code>Intersection</code> class represents an intersection between one
 * {@link Street} and a crossing street.
 */
public class Intersection extends ParallelogramShaped {
    /**
     * The segmentation point at which this intersection occurs.
     */
    private SegmentationPoint segPt;

    /**
     * The mirror intersection, which is physically identical to this
     * intersection but "belongs" to the crossing street.
     */
    private Intersection mirror;

    /**
     * Creates a new intersection at the point and street defined by the given
     * <code>SegmentationPoint</code>, intersecting with the given crossing
     * street.
     *
     * @param sp the segmentation point that defines the centre and street of
     *        this intersection.
     * @param parAB a vector from the centre point, running parallel to the
     *        edges A and B, reaching edge C.
     * @param parCD a vector from the centre point, running parallel to the
     *        edges C and D, reaching edge A.
     * @param crossStreet the crossing street of the intersection.
     */
    private Intersection(SegmentationPoint sp, Vector parAB, Vector parCD,
            Street crossStreet) {
        super(sp.getPoint(), parAB, parCD);
        this.segPt = sp;

        SegmentationPoint mirrorSp = new SegmentationPoint(sp.getPoint(),
                crossStreet, sp.getStreet().getName());
        this.mirror = new Intersection(mirrorSp, parAB, parCD, this);
    }

    /**
     * Creates a new intersection at the point and street defined by the given
     * <code>SegmentationPoint</code>, mirrored with the given pre-constructed
     * <code>Intersection</code>.
     *
     * @param sp the segmentation point that defines the centre and street of
     *        this intersection.
     * @param parAB a vector from the centre point, running parallel to the
     *        edges A and B, reaching edge C.
     * @param parCD a vector from the centre point, running parallel to the
     *        edges C and D, reaching edge A.
     * @param mirror the pre-constructed mirror intersection.
     */
    private Intersection(SegmentationPoint sp, Vector parAB, Vector parCD,
            Intersection mirror) {
        super(sp.getPoint(), parAB, parCD);
        this.segPt = sp;
        this.mirror = mirror;
    }

    /**
     * Returns the intersection on the given street with the given cross
     * street, or <code>null</code> if the two streets do not intersect. To get
     * the mirror intersection, in which the street and crossing street are
     * reversed, it is more efficient (computationally and in terms of memory)
     * to call {@link #getMirror} on the returned <code>Intersection</code>,
     * rather than calling <code>compute</code> again.
     *
     * @param theStreet the street on which to compute the intersection.
     * @param crossStreet the crossing street with which to compute the
     *        intersection.
     * @return the computed intersection on <code>theStreet</code>, or
     *         <code>null</code> if the two streets do not intersect.
     * @throws IllegalArgumentException if the two streets do intersect in the
     *         broad geometric meaning of the word, but there is not a
     *         well-defined, single intersection point between the two streets
     *         (for example, if one street fully contains the other, as is
     *         geometrically possible but does not create a well-defined
     *         intersection point).
     */
    public static Intersection compute(Street theStreet, Street crossStreet) {
        Point p = theStreet.intersectionPoint(crossStreet);
        if (p == null)
            return null;
        SegmentationPoint sp =
                new SegmentationPoint(p, theStreet, crossStreet.getName());

        /*
         * The centre line of theStreet, the width of theStreet (perpendicular
         * to its centre line), and the centre line of crossStreet form a
         * right-angle triangle. The hypotenuse represents how long you would
         * have to move from the centre of the intersection along crossStreet's
         * centre line until you reach the edge of theStreet (and vice versa).
         */
        Vector vecA = theStreet.getMidline().getVector();
        Vector vecB = crossStreet.getMidline().getVector();
        double sinTheta = Math.sin(vecA.angle(vecB));
        double distAlongA = (crossStreet.getWidth() / 2) / sinTheta;
        double distAlongB = (theStreet.getWidth() / 2) / sinTheta;
        return new Intersection(sp, vecA.normalize(distAlongA),
                vecB.normalize(distAlongB), crossStreet);
    }

    /**
     * Returns the mirror to this intersection, in which the street and cross
     * street designations have been inverted.
     *
     * @return the mirror intersection.
     */
    public Intersection getMirror() {
        return this.mirror;
    }

    /**
     * Returns the street on which this intersection occurs.
     *
     * @return the street to which this intersection belongs.
     */
    public Street getStreet() {
        return this.segPt.getStreet();
    }

    /**
     * Returns the segmentation point on this intersection's street, created by
     * the presence of this intersection.
     *
     * @return the segmentation point on this intersection's street.
     */
    public SegmentationPoint getSegmentationPoint() {
        return this.segPt;
    }

    /**
     * Tests is this <code>Intersection</code> is equal to another
     * <code>Intersection</code>. Two <code>Intersection</code>s are equal if
     * they represent the same point in space on the same street.
     *
     * @param o the object for which to test equality.
     * @return <code>true</code> if the two <code>Intersection</code>s have
     *         identical {@link SegmentationPoint}s, otherwise
     *         <code>false</code>.
     */
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Intersection))
            return false;
        return this.segPt.equals(((Intersection) o).segPt);
    }

    /**
     * Returns a hash code for this <code>Intersection</code>.
     *
     * @return a hash code for this <code>Intersection</code>, equal to the
     *         hash code of its {@link SegmentationPoint}.
     */
    public int hashCode() {
        return this.segPt.hashCode();
    }
}
