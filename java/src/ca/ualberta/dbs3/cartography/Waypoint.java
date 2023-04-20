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
 * A <code>Waypoint</code> is a location on a {@link Path}. It marks either the
 * beginning or end of the path, or a change in the street on which the agent
 * is walking.
 */
public class Waypoint {
    /**
     * The physical location of the waypoint.
     */
    private Point location;

    /**
     * The <code>Street</code> on which the waypoint is located.
     */
    private Street onStreet;

    /**
     * Creates a new <code>Waypoint</code> at the given location that an agent
     * conceives of as being on the given <code>Street</code>.
     *
     * @param location the physical location of the waypoint.
     * @param onStreet the street on which an agent conceptualizes itself as
     *        being on when at this waypoint.
     */
    public Waypoint(Point location, Street onStreet) {
        this.location = location;
        this.onStreet = onStreet;
    }

    /**
     * Returns the physical location of the waypoint.
     *
     * @return the physical location of the waypoint.
     */
    public Point getLocation() {
        return this.location;
    }

    /**
     * Returns the street on which an agent conceptualizes itself as being on
     * when it arrives at this waypoint.
     *
     * @return the street with which the waypoint is identified.
     */
    public Street getStreet() {
        return this.onStreet;
    }

    /**
     * Returns the Euclidean distance between this waypoint and the given
     * waypoint.
     *
     * @param other the waypoint to which to compute the distance.
     * @return the Euclidean distance between this waypoint and
     *         <code>other</code>.
     */
    public double distance(Waypoint other) {
        return this.location.distance(other.location);
    }

    /**
     * Returns a <code>String</code> representation of this
     * <code>Waypoint</code>.
     *
     * @return a human-readable description of this <code>Waypoint</code>.
     */
    public String toString() {
        return this.location.toString() + " on " + this.onStreet.getName();
    }

    /**
     * Tests is this <code>Waypoint</code> is equal to another
     * <code>Waypoint</code>. Two <code>Waypoint</code>s are equal if they
     * represent the same physical location on the same street.
     *
     * @param o the object for which to test equality.
     * @return <code>true</code> if the two <code>Waypoint</code>s are at the
     *         same location, otherwise <code>false</code>.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof Waypoint) {
            Waypoint other = (Waypoint) o;
            return (this.location.equals(other.location)
                    && this.onStreet.equals(other.onStreet));
        }
        return false;
    }

    /**
     * Returns a hash code for this <code>Waypoint</code>.
     *
     * @return a hash code for this <code>Waypoint</code>, equal to the hash
     *         code of its location.
     */
    public int hashCode() {
        return this.location.hashCode();
    }
}
