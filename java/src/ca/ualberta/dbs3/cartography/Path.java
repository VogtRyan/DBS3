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
 * The <code>Path</code> class represents a sequence of <code>Waypoint</code>s
 * that an agent can follow.
 */
public class Path {
    /**
     * The first waypoint in the ordered list, or <code>null</code> if the path
     * is empty.
     */
    private PathElement first;

    /**
     * The last waypoint in the ordered list, or <code>null</code> if the path
     * is empty.
     */
    private PathElement last;

    /**
     * Creates a new <code>Path</code> with just a single waypoint.
     *
     * @param waypoint the waypoint to be added.
     */
    public Path(Waypoint waypoint) {
        this.first = new PathElement(waypoint);
        this.last = this.first;
    }

    /**
     * Creates a new <code>Path</code> that is a deep copy of the given path.
     *
     * @param path the path to deep copy.
     */
    public Path(Path path) {
        this.first = this.last = null;
        PathElement pe = path.first;
        while (pe != null) {
            this.append(pe.getWaypoint());
            pe = pe.getNext();
        }
    }

    /**
     * Returns the first <code>Waypoint</code> in the path.
     *
     * @return the first waypoint in the path, or <code>null</code> if the path
     *         is empty.
     */
    public Waypoint peek() {
        if (this.first == null)
            return null;
        else
            return this.first.getWaypoint();
    }

    /**
     * Returns the last <code>Waypoint</code> in the path.
     *
     * @return the last waypoint in the path, or <code>null</code> if the path
     *         is empty.
     */
    public Waypoint peekEnd() {
        if (this.last == null)
            return null;
        else
            return this.last.getWaypoint();
    }

    /**
     * Adds the given <code>Waypoint</code> to the start of the
     * <code>Path</code>.
     *
     * @param waypoint the waypoint to prepend to the start of the path.
     * @throws IllegalArgumentException if <code>waypoint</code> is
     *         <code>null</code>.
     */
    public void prepend(Waypoint waypoint) {
        if (waypoint == null)
            throw new IllegalArgumentException("Null waypoint");
        this.first = new PathElement(waypoint, this.first);
        if (this.last == null)
            this.last = this.first;
    }

    /**
     * Adds the given <code>Waypoint</code> to the end of the
     * <code>Path</code>.
     *
     * @param waypoint the waypoint to append to the end of the path.
     * @throws IllegalArgumentException if <code>waypoint</code> is
     *         <code>null</code>.
     */
    public void append(Waypoint waypoint) {
        if (waypoint == null)
            throw new IllegalArgumentException("Null waypoint");
        PathElement pe = new PathElement(waypoint);
        if (this.last == null) {
            this.first = pe;
            this.last = pe;
        } else {
            this.last.setNext(pe);
            this.last = pe;
        }
    }

    /**
     * Removes the first <code>Waypoint</code> from the <code>Path</code> and
     * returns it.
     *
     * @return the first waypoint in the path, or <code>null</code> if the path
     *         is empty.
     */
    public Waypoint pop() {
        if (this.first == null)
            return null;
        Waypoint ret = this.first.getWaypoint();
        this.first = this.first.getNext();
        return ret;
    }

    /**
     * Calculates and returns the total Euclidean distance covered by this
     * <code>Path</code>.
     *
     * @return the Euclidean distance covered.
     */
    public double getDistance() {
        double dist = 0.0;
        Point prev = null;
        Point current = null;
        PathElement pe = this.first;
        while (pe != null) {
            prev = current;
            current = pe.getWaypoint().getLocation();
            if (prev != null)
                dist += prev.distance(current);
            pe = pe.getNext();
        }
        return dist;
    }

    /**
     * The <code>PathElement</code> class implements a single-linked list of
     * {@link Waypoint}s, which is (or at least was, historically) slightly
     * faster than the standard <code>LinkedList</code> class, for the heavily
     * used <code>Path</code> class.
     */
    private class PathElement {
        /**
         * The <code>Waypoint</code> at this list element.
         */
        private Waypoint waypoint;

        /**
         * The next item in the list, or <code>null</code> if this is the last
         * item in the list.
         */
        private PathElement next;

        /**
         * Creates a new list element with no next element.
         *
         * @param waypoint the waypoint contained in this list element.
         */
        public PathElement(Waypoint waypoint) {
            this(waypoint, null);
        }

        /**
         * Creates a new list element with the given waypoint and next list
         * item.
         *
         * @param waypoint the waypoint contained in this list element.
         * @param next the next item in the list, or <code>null</code>.
         */
        public PathElement(Waypoint waypoint, PathElement next) {
            this.waypoint = waypoint;
            this.next = next;
        }

        /**
         * Returns the waypoint contained in this list element.
         *
         * @return the waypoint contained in this list element.
         */
        public Waypoint getWaypoint() {
            return this.waypoint;
        }

        /**
         * Returns the next item in the list, or <code>null</code> if this is
         * the last element in the list.
         *
         * @return the next item in the list or <code>null</code>.
         */
        public PathElement getNext() {
            return this.next;
        }

        /**
         * Sets the next element in the list to the given element.
         *
         * @param next the next element in the list or <code>null</code>.
         */
        public void setNext(PathElement next) {
            this.next = next;
        }
    }
}
