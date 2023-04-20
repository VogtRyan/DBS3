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

import ca.ualberta.dbs3.math.*;

/**
 * The <code>CrumbMinAngle</code> class represents a piece of a {@link Path}
 * whose cost is determined first according to the sum of the total angle
 * change along the path, and secondly by total Euclidean distance travelled.
 */
public class CrumbMinAngle extends Crumb {
    /**
     * The accumulated angle change so far.
     */
    private double angle;

    /**
     * The lower bound on the total angle change that will be required to reach
     * the destination.
     */
    private double angleLB;

    /**
     * The accumulated Euclidean distance travelled so far.
     */
    private double dist;

    /**
     * The lower bound on the total Euclidean distance that will be required to
     * reach the destination.
     */
    private double distLB;

    /**
     * The vector from the pervious waypoint in the chain of
     * <code>Crumb</code>s that is not equal to this physical location, or
     * <code>null</code> if this is the first physical location in the chain.
     * Note that crumbs other than the first can have a <code>null</code>
     * inbound vector if all of them are at the same physical location.
     */
    private Vector inbound;

    /**
     * Creates a new <code>MinTurnsCrumb</code> to be used in building a
     * {@link Path}, representing the given location along the constructed
     * path.
     *
     * @param waypoint the location along the path where the crumb is dropped.
     * @param prev the previous crumb in the path, or <code>null</code> if this
     *        crumb is the first in the path.
     * @param endPoint the target location for the constructed path.
     */
    public CrumbMinAngle(Waypoint waypoint, Crumb prev, Point endPoint) {
        super(waypoint, prev);
        CrumbMinAngle prevCrumb = (CrumbMinAngle) prev;
        Point myLoc = waypoint.getLocation();

        /*
         * Compute inbound angle, accumulated angle change, and accumulated
         * distance
         */
        if (prevCrumb == null) {
            this.inbound = null;
            this.angle = 0.0;
            this.dist = 0.0;
        } else if (prevCrumb.waypoint.getLocation().equals(myLoc)) {
            this.inbound = prevCrumb.inbound;
            this.angle = prevCrumb.angle;
            this.dist = prevCrumb.dist;
        } else {
            Point prevLoc = prevCrumb.waypoint.getLocation();
            this.inbound = new Vector(prevLoc, myLoc);
            if (prevCrumb.inbound == null)
                this.angle = 0.0;
            else
                this.angle = prevCrumb.angle
                        + prevCrumb.inbound.angle(this.inbound);
            this.dist = prevCrumb.dist + prevLoc.distance(myLoc);
        }

        /* The lower bounds come from a direct path to the destination */
        if (myLoc.equals(endPoint)) {
            this.angleLB = this.angle;
            this.distLB = this.dist;
        } else if (this.inbound == null) {
            this.angleLB = 0.0;
            this.distLB = myLoc.distance(endPoint);
        } else {
            Vector toDest = new Vector(myLoc, endPoint);
            this.angleLB = this.angle + this.inbound.angle(toDest);
            this.distLB = this.dist + myLoc.distance(endPoint);
        }
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
     *        total angle change (the primary cost metric) after reorientation,
     *        the obsolescence code will attempt to obsolete one crumb based on
     *        total Euclidean distance incurred (the secondary cost metric). If
     *        <code>allowPrimaryTies</code> is <code>true</code>, this
     *        behaviour is suppressed and two crumbs with the same incurred
     *        angle change (a tied primary cost metric), post-reorientation,
     *        are allowed to coexist.
     * @return a positive value if this crumb costs so much more that it should
     *         be made obsolete, a negative value if the given crumb costs so
     *         much more that it should be made obsolete, or <code>0</code> if
     *         neither crumb should be made obsolete.
     * @throws ClassCastException if the given crumb is not a
     *         <code>CrumbMinAngle</code>.
     */
    public int checkObsolete(Crumb o, boolean allowPrimaryTies) {
        CrumbMinAngle other = (CrumbMinAngle) o;

        /* Something that has incurred no cost obsoletes anything else. */
        if (this.inbound == null
                && (allowPrimaryTies == false || other.inbound != null))
            return Crumb.OBSOLETE_PARAMETER;
        else if (other.inbound == null)
            return Crumb.OBSOLETE_THIS;

        /*
         * It's not necessarily the case that a crumb with lower accumulated
         * angle will obsolete a crumb with larger accumulated angle, because
         * the two crumbs' inbound angles to the crumb location may be
         * different (and hence their cost for getting to the next location
         * would be different).
         *
         * The smaller accumulated angle only obsoletes the larger one if the
         * smaller accumulated angle can reorient (rotate) itself to face the
         * same way as the larger accumulated angle, and still have less
         * accumulated angle at that point.
         */
        double delta = this.inbound.angle(other.inbound);
        if (this.angle + delta < other.angle)
            return Crumb.OBSOLETE_PARAMETER;
        else if (other.angle + delta < this.angle)
            return Crumb.OBSOLETE_THIS;
        else if (this.angle + delta == other.angle && allowPrimaryTies == false
                && this.dist + this.getDistance(other) <= other.dist)
            return Crumb.OBSOLETE_PARAMETER;
        else if (other.angle + delta == this.angle && allowPrimaryTies == false
                && other.dist + this.getDistance(other) <= this.dist)
            return Crumb.OBSOLETE_THIS;
        return 0;
    }

    /**
     * Compares the lower bound estimate on the total angle change (the primary
     * cost metric) of a {@link Path} that could result from this
     * <code>Crumb</code> against the lower bound estimate on the primary cost
     * metric of the given crumb. Returns a negative integer, zero, or a
     * positive integer as this lower bound is less than, equal to, or greater
     * than the lower bound of the given crumb.
     *
     * @param o the crumb to which to compare this crumb.
     * @return a negative number if this crumb has a smaller lower bound,
     *         <code>0</code> if they have equal lower bounds, or a positive
     *         number if this crumb has a larger lower bound.
     * @throws ClassCastException if the given crumb is not a
     *         <code>CrumbMinAngle</code>.
     */
    public int compareTo(Crumb o) {
        CrumbMinAngle other = (CrumbMinAngle) o;
        if (this.angleLB < other.angleLB)
            return -1;
        else if (this.angleLB > other.angleLB)
            return 1;
        else {
            if (this.distLB < other.distLB)
                return -1;
            else if (this.distLB > other.distLB)
                return 1;
            else
                return 0;
        }
    }

    /**
     * Returns a lower bound estimate for the total angle change (the primary
     * cost metric) of the {@link Path} to be formed by this crumb. The cost
     * acquired so far by the crumb must be less than or equal to the returned
     * value, which in turn must be less than or equal to the actual total cost
     * that will be acquired to make it to the destination.
     *
     * @return a lower bound estimate for the total angle change (the primary
     *         cost metric).
     */
    public double primaryBound() {
        return this.angleLB;
    }

    /**
     * Returns the distance between the physical locations of this crumb and
     * the given crumb.
     *
     * @param other the other crumb.
     * @return the physical distance between this crumb and the other crumb.
     */
    private double getDistance(CrumbMinAngle other) {
        Point lOne = this.waypoint.getLocation();
        Point lTwo = other.waypoint.getLocation();
        return lOne.distance(lTwo);
    }
}
