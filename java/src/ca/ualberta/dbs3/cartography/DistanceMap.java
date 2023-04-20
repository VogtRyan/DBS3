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

import java.util.HashMap;
import java.util.Set;

/**
 * The <code>DistanceMap</code> class represents a mapping from
 * {@link Segment}s to distance values. It can be used, for example, to record
 * the amount of distance travelled on different segments.
 */
public class DistanceMap {
    /**
     * A mapping from the segments to the stored distances.
     */
    private HashMap<Segment, Double> segments;

    /**
     * Creates a new <code>DistanceMap</code> in which all segments are
     * initially mapped to the distance <code>0.0</code>.
     */
    public DistanceMap() {
        this.segments = new HashMap<Segment, Double>();
    }

    /**
     * Sets the mapping from the given segment to the given distance value.
     *
     * @param segment the segment for which to change the mapping.
     * @param distance the new distance to which to map the given segment.
     * @throws IllegalArgumentException if the given segment is
     *         <code>null</code>.
     */
    public void setDistance(Segment segment, double distance) {
        if (segment == null)
            throw new IllegalArgumentException("Invalid segment parameter");
        this.segments.put(segment, distance);
    }

    /**
     * Adds the given amount to the current value for the given segment.
     *
     * @param segment the segment to which to add the given value.
     * @param amount the amount to add to the value mapped to the given
     *        segment.
     * @throws IllegalArgumentException if the given segment is
     *         <code>null</code>.
     */
    public void add(Segment segment, double amount) {
        if (segment == null)
            throw new IllegalArgumentException("Invalid segment parameter");
        double old = this.getDistance(segment);
        this.setDistance(segment, old + amount);
    }

    /**
     * Returns the distance currently associated with the given segment.
     *
     * @param segment the segment for which to look up the associated distance.
     * @return the associated distance.
     * @throws IllegalArgumentException if the given segment is
     *         <code>null</code>.
     */
    public double getDistance(Segment segment) {
        if (segment == null)
            throw new IllegalArgumentException("Invalid segment parameter");
        Double d = this.segments.get(segment);
        return (d == null ? 0.0 : d);
    }

    /**
     * Returns the sum of all the distances associated with segments of the
     * given street.
     *
     * @param street the street for which to add up all the associated
     *        distances.
     * @return the sum over all segments of the given street of the associated
     *         distances.
     * @throws IllegalArgumentException if the given street is
     *         <code>null</code>.
     */
    public double getDistance(Street street) {
        /*
         * Have to explicitly declare type using java.util.Map.Entry, since
         * there is a Map class in the cartography package.
         */
        if (street == null)
            throw new IllegalArgumentException("Invalid street");
        Set<java.util.Map.Entry<Segment, Double>> entrySet =
                this.segments.entrySet();
        double ret = 0.0;
        for (java.util.Map.Entry<Segment, Double> entry : entrySet) {
            Segment segment = entry.getKey();
            if (segment.getStreet().equals(street))
                ret += entry.getValue();
        }
        return ret;
    }
}
