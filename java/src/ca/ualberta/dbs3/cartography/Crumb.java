/*
 * Copyright (c) 2010-2023 Ryan Vogt <rvogt@ualberta.ca>
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

/**
 * The <code>Crumb</code> class represents a way of constructing {@link Path}
 * objects, as well as measuring and comparing the cost of paths that will be
 * generated. Conceptually, a <code>Crumb</code> represents part of a trail of
 * breadcrumbs you drop behind you as you are walking a path along the streets.
 * <p>
 * Crumbs are comparable by the lower bounds of the total cost they will incur
 * to produce paths to a final destination. It is a requirement that the cost
 * acquired so far by a crumb be less than or equal to the estimated cost to
 * make it to the destination, which itself must be less than or equal to the
 * actual total cost that will be acquired by the crumb at the destination.
 */
public abstract class Crumb implements Comparable<Crumb> {
    /**
     * An indication that this crumb should be made obsolete.
     */
    protected static final int OBSOLETE_THIS = 1;

    /**
     * An indication that a crumb given to this one as a parameter should be
     * made obsolete.
     */
    protected static final int OBSOLETE_PARAMETER = -1;

    /**
     * The location at which path construction currently terminates.
     */
    protected Waypoint waypoint;

    /**
     * The previous crumb in the construction of a path, or <code>null</code>
     * if this is the first location in the path being constructed.
     */
    protected Crumb previous;

    /**
     * Whether this crumb has been made obsolete by a different route.
     */
    private boolean obsolete;

    /**
     * Creates a new <code>Crumb</code> at the given location, continuing from
     * the given previous crumb.
     *
     * @param location the location for this crumb.
     * @param previous the previous crumb along the path being constructed, or
     *        <code>null</code> if this crumb is the first.
     */
    public Crumb(Waypoint location, Crumb previous) {
        this.waypoint = location;
        this.previous = previous;
        this.obsolete = false;
    }

    /**
     * Returns the location corresponding to this crumb.
     *
     * @return the corresponding location.
     */
    public Waypoint getWaypoint() {
        return this.waypoint;
    }

    /**
     * Returns the previous <code>Crumb</code> in the path being constructed,
     * or <code>null</code> if there is no previous crumb.
     *
     * @return the previous crumb, or <code>null</code>.
     */
    public Crumb getPrevious() {
        return this.previous;
    }

    /**
     * Returns whether this crumb, or any previous crumb, is on the given
     * <code>Street</code>.
     *
     * @param street the street in question.
     * @return <code>true</code> if the given street appears in this or any
     *         previous crumb, otherwise <code>false</code>.
     */
    public boolean tracesBackTo(Street street) {
        Crumb current = this;
        while (current != null) {
            Street str = current.waypoint.getStreet();
            if (str.equals(street))
                return true;
            current = current.previous;
        }
        return false;
    }

    /**
     * Returns whether or not this <code>Crumb</code> has been rendered
     * obsolete.
     *
     * @return <code>true</code> if this crumb is obsolete, otherwise
     *         <code>false</code>.
     */
    public boolean isObsolete() {
        return this.obsolete;
    }

    /**
     * Flags this <code>Crumb</code> as obsolete.
     */
    public void setObsolete() {
        this.obsolete = true;
    }

    /**
     * Checks whether this <code>Crumb</code> renders the given crumb obsolete,
     * or vice versa, or if neither crumb renders the other obsolete. The two
     * crumbs are assumed to be along {@link Path}s making it to the same
     * {@link Street}, but not necessarily the same physical location. If one
     * crumb could reorient and/or move to the other crumb at a lower incurred
     * total cost, even after the move and/or reorientation, return that one of
     * the crumbs should be made obsolete.
     *
     * @param o the crumb to check for obsolescence against this crumb.
     * @return a positive value if this crumb costs so much more that it should
     *         be made obsolete, a negative value if the given crumb costs so
     *         much more that it should be made obsolete, or <code>0</code> if
     *         neither crumb should be made obsolete.
     */
    public int checkObsolete(Crumb o) {
        return this.checkObsolete(o, false);
    }

    /**
     * Checks whether this <code>Crumb</code> renders the given crumb obsolete,
     * or vice versa, or if neither crumb renders the other obsolete. The two
     * crumbs are assumed to be along {@link Path}s to the same {@link Street},
     * but not necessarily the same physical location. If one crumb could
     * reorient and/or move to the other crumb at a lower incurred total cost,
     * even after the move and/or reorientation, return that one of the crumbs
     * should be made obsolete.
     *
     * @param o the crumb to check for obsolescence against this crumb.
     * @param allowPrimaryTies by default, if two crumbs have the same incurred
     *        primary cost metric after reorientation, the obsolescence code
     *        will attempt to obsolete one crumb based on any secondary cost
     *        metrics (if applicable) or obsolete the higher incurred-cost
     *        crumb (if there are no secondary metrics). If
     *        <code>allowPrimaryTies</code> is <code>true</code>, this
     *        behaviour is suppressed and two crumbs with a tied primary
     *        post-reorientation metric are allowed to coexist.
     * @return a positive value if this crumb costs so much more that it should
     *         be made obsolete, a negative value if the given crumb costs so
     *         much more that it should be made obsolete, or <code>0</code> if
     *         neither crumb should be made obsolete.
     */
    public abstract int checkObsolete(Crumb o, boolean allowPrimaryTies);

    /**
     * Compares the lower bound estimate on the cost of a {@link Path} that
     * could result from this <code>Crumb</code> against the lower bound
     * estimate on the cost of the given crumb. Returns a negative integer,
     * zero, or a positive integer respectively if this crumb's lower bound is
     * less than, equal to, or greater than the lower bound of the given crumb.
     *
     * @param o the crumb to which to compare this crumb.
     * @return a negative number if this crumb has a smaller lower bound than
     *         the given crumb, <code>0</code> if they have equal lower bounds,
     *         or a positive number if this crumb has a larger lower bound.
     */
    public abstract int compareTo(Crumb o);

    /**
     * Returns a lower bound estimate for the primary cost metric of the path
     * to be formed by this <code>Crumb</code>. The cost acquired so far by the
     * crumb must be less than or equal to the returned value, which in turn
     * must be less than or equal to the actual total cost that will be
     * acquired to make it to the destination.
     *
     * @return a lower bound estimate for the primary cost metric.
     */
    public abstract double primaryBound();
}
