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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * The <code>CrumbMinTurns</code> class represents a piece of a {@link Path}
 * whose cost is determined first according to the number of turns made, and
 * secondly by total Euclidean distance travelled.
 */
public class CrumbMinTurns extends Crumb {
    /**
     * The accumulated number of turns so far.
     */
    private int turns;

    /**
     * The lower bound on the total number of turns that will be required to
     * reach the destination.
     */
    private int turnsLB;

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
     * All of the acceptable destination streets for this crumb.
     */
    private List<Street> endStreets;

    /**
     * Creates a new <code>CrumbMinTurns</code> to be used in building a
     * {@link Path}, representing the given location along the constructed
     * path.
     *
     * @param waypoint the location along the path where the crumb is dropped.
     * @param prev the previous crumb in the path, or <code>null</code> if this
     *        crumb is the first in the path.
     * @param endPoint the target location for the constructed path.
     * @param endStreet the street on which the target destination is located.
     * @param map the map on which this crumb functions.
     */
    public CrumbMinTurns(Waypoint waypoint, Crumb prev, Point endPoint,
            Street endStreet, Map map) {
        super(waypoint, prev);
        CrumbMinTurns prevCrumb = (CrumbMinTurns) prev;
        Street myStreet = waypoint.getStreet();
        Point myLoc = waypoint.getLocation();

        /* Compute accumulated turns and distance */
        if (prevCrumb == null) {
            this.turns = 0;
            this.dist = 0.0;
        } else {
            if (prevCrumb.waypoint.getStreet().equals(myStreet))
                this.turns = prevCrumb.turns;
            else
                this.turns = prevCrumb.turns + 1;
            Point prevLoc = prevCrumb.waypoint.getLocation();
            this.dist = prevCrumb.dist + prevLoc.distance(myLoc);
        }

        /* Estimate turns and distance to the end */
        this.distLB = this.dist + myLoc.distance(endPoint);
        if (prevCrumb == null) {
            this.endStreets = new LinkedList<Street>();
            this.endStreets.add(endStreet);
            this.turnsLB = map.minTurns(myStreet, endStreet);
        } else {
            this.endStreets = prevCrumb.endStreets;
            this.turnsLB = prevCrumb.turnsLB;
        }
    }

    /**
     * Creates a new <code>CrumbMinTurns</code> to be used in building a
     * {@link Path}, representing the given location along the constructed
     * path.
     *
     * @param waypoint the location along the path where the crumb is dropped.
     * @param prev the previous crumb in the path, or <code>null</code> if this
     *        crumb is the first in the path.
     * @param endPoint the target location for the constructed path.
     * @param endStreets the list of streets on which an agent could perceive
     *        the target destination.
     * @param map the map on which this crumb functions.
     */
    public CrumbMinTurns(Waypoint waypoint, Crumb prev, Point endPoint,
            List<Street> endStreets, Map map) {
        super(waypoint, prev);
        CrumbMinTurns prevCrumb = (CrumbMinTurns) prev;
        Street myStreet = waypoint.getStreet();
        Point myLoc = waypoint.getLocation();

        /* Compute accumulated turns and distance */
        if (prevCrumb == null) {
            this.turns = 0;
            this.dist = 0.0;
        } else {
            if (prevCrumb.waypoint.getStreet().equals(myStreet))
                this.turns = prevCrumb.turns;
            else
                this.turns = prevCrumb.turns + 1;
            Point prevLoc = prevCrumb.waypoint.getLocation();
            this.dist = prevCrumb.dist + prevLoc.distance(myLoc);
        }

        /* Estimate turns and distance to the end */
        this.distLB = this.dist + myLoc.distance(endPoint);
        if (prevCrumb == null) {
            this.endStreets = new LinkedList<Street>();
            this.turnsLB = Integer.MAX_VALUE;
            for (Street endStreet : endStreets) {
                int t = map.minTurns(myStreet, endStreet);
                if (t < this.turnsLB) {
                    this.turnsLB = t;
                    this.endStreets.clear();
                    this.endStreets.add(endStreet);
                } else if (t == this.turnsLB)
                    this.endStreets.add(endStreet);
            }
        } else {
            this.endStreets = prevCrumb.endStreets;
            this.turnsLB = prevCrumb.turnsLB;
        }
    }

    /**
     * Checks whether this <code>Crumb</code> renders the given crumb obsolete,
     * or vice versa, or if neither crumb renders the other obsolete. The two
     * crumbs are assumed to be along {@link Path}s to the same {@link Street},
     * but not necessarily the same physical location. If one crumb could move
     * to the other crumb without making any additional turns, and at a lower
     * incurred total cost even after the move, return that one of the crumbs
     * should be made obsolete.
     *
     * @param o the crumb to check for obsolescence against this crumb.
     * @param allowPrimaryTies by default, if two crumbs have the same incurred
     *        total number of turns (the primary cost metric) the obsolescence
     *        code will attempt to obsolete one crumb based on total Euclidean
     *        distance incurred (the secondary cost metric). If
     *        <code>allowPrimaryTies</code> is <code>true</code>, this
     *        behaviour is suppressed and two crumbs with the same incurred
     *        number of turns (a tied primary cost metric) are allowed to
     *        coexist.
     * @return a positive value if this crumb costs so much more that it should
     *         be made obsolete, a negative value if the given crumb costs so
     *         much more that it should be made obsolete, or <code>0</code> if
     *         neither crumb should be made obsolete.
     * @throws ClassCastException if the given crumb is not a
     *         <code>CrumbMinTurns</code>.
     */
    public int checkObsolete(Crumb o, boolean allowPrimaryTies) {
        CrumbMinTurns other = (CrumbMinTurns) o;
        if (this.turns < other.turns)
            return Crumb.OBSOLETE_PARAMETER;
        else if (other.turns < this.turns)
            return Crumb.OBSOLETE_THIS;
        if (allowPrimaryTies)
            return 0;

        Point myLocation = this.waypoint.getLocation();
        Point otherLocation = other.waypoint.getLocation();
        double delta = myLocation.distance(otherLocation);
        if (this.dist + delta <= other.dist)
            return Crumb.OBSOLETE_PARAMETER;
        else if (other.dist + delta <= this.dist)
            return Crumb.OBSOLETE_THIS;
        return 0;
    }

    /**
     * Compares the lower bound estimate on the total number of turns (the
     * primary cost metric) of a {@link Path} that could result from this
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
     *         <code>CrumbMinTurns</code>.
     */
    public int compareTo(Crumb o) {
        CrumbMinTurns other = (CrumbMinTurns) o;
        if (this.turnsLB < other.turnsLB)
            return -1;
        else if (this.turnsLB > other.turnsLB)
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
     * Returns a lower bound estimate for the total number of turns (the
     * primary cost metric) of the {@link Path} to be formed by this crumb. The
     * cost acquired so far by the crumb must be less than or equal to the
     * returned value, which must be less than or equal in turn to the actual
     * total cost that will be acquired to make it to the destination.
     *
     * @return a lower bound estimate for the primary cost metric.
     */
    public double primaryBound() {
        return (double) (this.turnsLB);
    }

    /**
     * Returns an iteration over the list of all the acceptable ending streets.
     *
     * @return all the possible ending streets.
     */
    public Iterator<Street> getEndStreets() {
        Iterator<Street> it = this.endStreets.iterator();
        return new NoRemoveIterator(it);
    }

    /**
     * Returns the number of turns necessary to get from this crumb's current
     * street to one of the acceptable destination streets.
     *
     * @return the minimum number of remaining turns.
     */
    public int getRemainingTurns() {
        return this.turnsLB - this.turns;
    }

    /**
     * The <code>NoRemoveIterator</code> class is a wrapper around a linked
     * list iterator that prevents removal of elements.
     */
    private class NoRemoveIterator implements Iterator<Street> {
        /**
         * The inner iterator around which this iterator is wrapped.
         */
        private Iterator<Street> inner;

        /**
         * Creates a new <code>NoRemoveIterator</code> around the given
         * iterator.
         *
         * @param inner the iterator around which to wrap the new object.
         */
        public NoRemoveIterator(Iterator<Street> inner) {
            this.inner = inner;
        }

        /**
         * Returns <code>true</code> if the iteration has more elements.
         *
         * @return <code>true</code> if there are more elements, otherwise
         *         <code>false</code>.
         */
        public boolean hasNext() {
            return this.inner.hasNext();
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration.
         * @throws NoSuchElementException if the iteration has no more
         *         elements.
         */
        public Street next() {
            return this.inner.next();
        }

        /**
         * Throws an <code>UnsupportedOperationException</code>.
         *
         * @throws UnsupportedOperationException always.
         */
        public void remove() {
            throw new UnsupportedOperationException("Cannot remove elements");
        }
    }
}
