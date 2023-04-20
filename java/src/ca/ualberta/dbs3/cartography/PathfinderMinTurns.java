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
import java.util.Iterator;
import java.util.List;

/**
 * The <code>PathfinderMinTurns</code> class is responsible for finding paths
 * between given points on a map that minimize the number of turns taken,
 * breaking ties by minimizing the Euclidean distance travelled.
 */
public class PathfinderMinTurns extends PathfinderOptimal {
    /**
     * Creates a new <code>PathfinderMinTurns</code> that operates on the given
     * map.
     *
     * @param map the map on which this pathfinder operates.
     */
    public PathfinderMinTurns(Map map) {
        super(map);
    }

    /**
     * Returns a newly constructed <code>Crumb</code> to be used in building a
     * {@link Path}, representing the given location along the constructed
     * path.
     *
     * @param waypoint the location along the path where the crumb is dropped.
     * @param prev the previous crumb in the path, or <code>null</code> if this
     *        crumb is the first in the path.
     * @param endPoint the target location for the constructed path.
     * @param endStreets the list of streets on which an agent could perceive
     *        the target destination.
     * @return the constructed path piece.
     */
    protected Crumb getCrumb(Waypoint waypoint, Crumb prev, Point endPoint,
            List<Street> endStreets) {
        return new CrumbMinTurns(waypoint, prev, endPoint, endStreets,
                this.map);
    }

    /**
     * Returns an iterations of the intersections on the current street that
     * could produce minimum-turn paths to a fewest-turns-away end street.
     *
     * @param current the crumb being expanded currently.
     * @return an iteration over a list of <code>Intersection</code>s that can
     *         yield minimum-turn paths to minimum-turns-away ending street.
     */
    protected Iterator<Intersection> getIntersections(Crumb current) {
        return new MinTurnsIterator(this.map, (CrumbMinTurns) current);
    }
}
