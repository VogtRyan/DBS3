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

import java.util.HashSet;

/**
 * A <code>Geodesic</code> is a set of segments, representing a path between
 * two locations on a map, that minimizes some primary-cost metric.
 */
public class Geodesic {
    /**
     * The minimal value of the primary cost metric.
     */
    private double cost;

    /**
     * All of the segments in this geodesic.
     */
    private HashSet<Segment> segments;

    /**
     * Creates a new <code>Geodesic</code> with the given primary cost metric.
     *
     * @param cost the value of the primary cost metric.
     * @throws IllegalArgumentException if <code>cost</code> is negative.
     */
    public Geodesic(double cost) {
        if (cost < 0.0)
            throw new IllegalArgumentException("Invalid metric value");
        this.cost = cost;
        this.segments = new HashSet<Segment>();
    }

    /**
     * Adds the given segment to this geodesic, if it is not already a part of
     * this geodesic.
     *
     * @param segment the segment to add.
     */
    public void addSegment(Segment segment) {
        this.segments.add(segment);
    }

    /**
     * Returns whether or not the given segment is part of this geodesic.
     *
     * @param segment the segment to test for inclusion.
     * @return <code>true</code> if the given segment has been added to this
     *         geodesic, otherwise <code>false</code>.
     */
    public boolean contains(Segment segment) {
        return this.segments.contains(segment);
    }

    /**
     * Returns the primary-metric cost of this geodesic.
     *
     * @return the primary-metric cost.
     */
    public double getCost() {
        return this.cost;
    }
}
