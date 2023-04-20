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

package ca.ualberta.dbs3.cartography;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

/**
 * The <code>GeodesicSet</code> class represents a set of {@link Geodesic}s
 * between two locations. The <code>GeodesicSet</code> class allows for
 * duplicate elements to exist in the set.
 */
public class GeodesicSet {
    /**
     * All of the <code>Geodesic</code>s in this set.
     */
    private List<Geodesic> list;

    /**
     * The primary metric cost of the geodesics in this set.
     */
    private double cost;

    /**
     * Creates a new, empty <code>GeodesicSet</code>.
     */
    public GeodesicSet() {
        this.list = new LinkedList<Geodesic>();
        this.cost = Double.POSITIVE_INFINITY;
    }

    /**
     * Adds the given geodesic to this set of geodesics.
     *
     * @param geo the geodesic to add.
     * @throws IllegalArgumentException if the cost of this geodesic is not
     *         equal to the primary cost metric of any geodesics already in
     *         this set.
     */
    public void add(Geodesic geo) {
        if (this.list.isEmpty()) {
            this.cost = geo.getCost();
            this.list.add(geo);
        } else {
            if (this.cost != geo.getCost())
                throw new IllegalArgumentException("Differing costs");
            this.list.add(geo);
        }
    }

    /**
     * Returns the primary metric cost of the geodesics in this set, or
     * <code>Double.POSTIIVE_INFINITY</code> if this set is empty.
     *
     * @return the cost of geodesics in this set.
     */
    public double getCost() {
        return this.cost;
    }

    /**
     * Returns the proportion of geodesics in this set that contain the given
     * segment.
     *
     * @param segment the segment in which we are interested.
     * @return the proportion between <code>0.0</code> and <code>1.0</code> of
     *         the segments in this set that contain the given segment, or
     *         <code>NaN</code> if this set is empty.
     */
    public double proportionContains(Segment segment) {
        int contain = 0;
        int total = 0;
        Iterator<Geodesic> it = this.list.iterator();
        while (it.hasNext()) {
            Geodesic geo = it.next();
            total++;
            if (geo.contains(segment))
                contain++;
        }
        if (total == 0)
            return Double.NaN;
        else
            return ((double) contain) / total;
    }
}
