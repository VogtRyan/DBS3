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
 * whose cost is determined solely by the total Euclidean distance travelled.
 */
public class CrumbMinDistance extends Crumb {
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
     * Creates a new <code>CrumbMinDistance</code> to be used in building a
     * {@link Path}, representing the given location along the constructed
     * path.
     *
     * @param waypoint the location along the path where the crumb is dropped.
     * @param prev the previous crumb in the path, or <code>null</code> if this
     *        crumb is the first in the path.
     * @param endPoint the target location for the constructed path.
     */
    public CrumbMinDistance(Waypoint waypoint, Crumb prev, Point endPoint) {
        super(waypoint, prev);
        CrumbMinDistance prevCrumb = (CrumbMinDistance) prev;

        Point myLoc = waypoint.getLocation();
        if (prevCrumb == null)
            this.dist = 0.0;
        else {
            Point prevLoc = prevCrumb.waypoint.getLocation();
            this.dist = prevCrumb.dist + prevLoc.distance(myLoc);
        }
        this.distLB = this.dist + myLoc.distance(endPoint);
    }

    /**
     * Checks whether this <code>Crumb</code> renders the given crumb obsolete,
     * or vice versa, or if neither crumb renders the other obsolete. The two
     * crumbs are assumed to be along {@link Path}s to the same {@link Street},
     * but not necessarily the same physical location. If one crumb could move
     * to the other crumb at a lower incurred total cost, even after the move,
     * return that one of the crumbs should be made obsolete.
     *
     * @param o the crumb to check for obsolescence against this crumb.
     * @param allowPrimaryTies by default, if two crumbs have the same incurred
     *        Euclidean distance (the primary cost metric) after moving, the
     *        obsolescence code will obsolete the higher incurred-cost crumb.
     *        If <code>allowPrimaryTies</code> is <code>true</code>, this
     *        behaviour is suppressed and two crumbs with a tied post-move
     *        incurred Euclidean distance are allowed to coexist.
     * @return a positive value if this crumb costs so much more that it should
     *         be made obsolete, a negative value if the given crumb costs so
     *         much more that it should be made obsolete, or <code>0</code> if
     *         neither crumb should be made obsolete.
     * @throws ClassCastException if the given crumb is not a
     *         <code>CrumbMinDistance</code>.
     */
    public int checkObsolete(Crumb o, boolean allowPrimaryTies) {
        CrumbMinDistance other = (CrumbMinDistance) o;
        Point myLocation = this.waypoint.getLocation();
        Point otherLocation = other.waypoint.getLocation();
        double delta = myLocation.distance(otherLocation);

        if (allowPrimaryTies) {
            if (this.dist + delta < other.dist)
                return Crumb.OBSOLETE_PARAMETER;
            else if (other.dist + delta < this.dist)
                return Crumb.OBSOLETE_THIS;
            return 0;
        } else {
            if (this.dist + delta <= other.dist)
                return Crumb.OBSOLETE_PARAMETER;
            else if (other.dist + delta <= this.dist)
                return Crumb.OBSOLETE_THIS;
            return 0;
        }
    }

    /**
     * Compares the lower bound estimate on the total Euclidean distance (the
     * primary and only cost metric) of a {@link Path} that could result from
     * this <code>Crumb</code> against the lower bound estimate on the cost
     * metric of the given crumb. Returns a negative integer, zero, or a
     * positive integer as this lower bound is less than, equal to, or greater
     * than the lower bound of the given crumb.
     *
     * @param o the crumb to which to compare this crumb.
     * @return a negative number if this crumb has a smaller lower bound,
     *         <code>0</code> if they have equal lower bounds, or a positive
     *         number if this crumb has a larger lower bound.
     * @throws ClassCastException if the given crumb is not a
     *         <code>CrumbMinDistance</code>.
     */
    public int compareTo(Crumb o) {
        CrumbMinDistance other = (CrumbMinDistance) o;
        if (this.distLB < other.distLB)
            return -1;
        else if (this.distLB == other.distLB)
            return 0;
        else
            return 1;
    }

    /**
     * Returns a lower bound estimate for the total Euclidean distance (the
     * primary and only cost metric) of the {@link Path} to be formed by this
     * crumb. The cost acquired so far by the crumb must be less than or equal
     * to the returned value, which in turn must be less than or equal to the
     * actual total cost that will be acquired to make it to the destination.
     *
     * @return a lower bound estimate for the primary cost metric.
     */
    public double primaryBound() {
        return this.distLB;
    }
}
