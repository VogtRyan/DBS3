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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The <code>MinTurnsIterator</code> class is an iteration over all the
 * {@link Intersection}s on a {@link Street} that could lead to an ending
 * street in a minimum number of turns. Intersections that lead more turns away
 * from all ending streets are skipped.
 */
public class MinTurnsIterator implements Iterator<Intersection> {
    /**
     * The underlying iteration over all intersections on the street.
     */
    private Iterator<Intersection> all;

    /**
     * The next intersection that will be returned by this iteration, or
     * <code>null</code> if there are no more elements to be returned.
     */
    private Intersection next;

    /**
     * The map for which we are iterating over intersections.
     */
    private Map map;

    /**
     * The crumb for which we are returning intersections.
     */
    private CrumbMinTurns expanding;

    /**
     * Creates a new <code>MinTurnsIterator</code> that returns intersections
     * belonging to the given street, that can reach any of the ending streets
     * in a minimum of turns.
     *
     * @param map the map for which we are iterating over intersections.
     * @param current the current crumb to expand to new intersections.
     */
    public MinTurnsIterator(Map map, CrumbMinTurns current) {
        this.map = map;
        this.expanding = current;
        this.all = map.getIntersections(current.getWaypoint().getStreet());
        this.advance();
    }

    /**
     * Returns whether there are remaining elements in the iteration.
     *
     * @return <code>true</code> if there are more elements, otherwise
     *         <code>false</code>.
     */
    public boolean hasNext() {
        return (this.next != null);
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     */
    public Intersection next() {
        if (this.next == null)
            throw new NoSuchElementException("No more elements");
        Intersection ret = this.next;
        this.advance();
        return ret;
    }

    /**
     * Throws an <code>UnsupportedOperationException</code>.
     *
     * @throws UnsupportedOperationException always.
     */
    public void remove() {
        throw new UnsupportedOperationException("Remove not supported");
    }

    /**
     * Sets the next element to be returned by this iteration by advancing
     * through the underlying iteration over all intersections.
     */
    private void advance() {
        int currentTurns = this.expanding.getRemainingTurns();
        while (this.all.hasNext()) {
            Intersection nextAll = this.all.next();
            Iterator<Street> end = this.expanding.getEndStreets();
            while (end.hasNext()) {
                Street endStreet = end.next();
                int mirrorTurns = this.map
                        .minTurns(nextAll.getMirror().getStreet(), endStreet);
                if (mirrorTurns < currentTurns) {
                    this.next = nextAll;
                    return;
                }
            }
        }
        this.next = null;
    }
}
