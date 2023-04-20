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

import ca.ualberta.dbs3.math.*;
import java.util.Iterator;

/**
 * The <code>GeodesicFinderMinTurns</code> class is responsible for finding
 * {@link Geodesic}s between two segments such that the number of turns made is
 * minimized.
 */
public class GeodesicFinderMinTurns extends GeodesicFinder {
    /**
     * Creates a new <code>GeodesicFinderMinTurns</code> that operates on the
     * given map.
     *
     * @param map the map on which this search algorithm operates.
     */
    public GeodesicFinderMinTurns(Map map) {
        super(map, "minimal turn");
    }

    /**
     * Returns a newly constructed <code>Crumb</code> to be used in building a
     * geodesic, representing the given location along the constructed path.
     *
     * @param waypoint the location along the path where the crumb is dropped.
     * @param prev the previous crumb in the path, or <code>null</code> if this
     *        crumb is the first in the path.
     * @param endPoint the target location for the constructed path.
     * @param endStreet the street on which the target destination is located.
     * @return the constructed path piece.
     */
    protected Crumb getCrumb(Waypoint waypoint, Crumb prev, Point endPoint,
            Street endStreet) {
        return new CrumbMinTurns(waypoint, prev, endPoint, endStreet,
                this.map);
    }

    /**
     * Returns an iterations of the intersections on the current street that
     * could produce minimum-turn paths to the end street.
     *
     * @param current the crumb being expanded currently.
     * @return an iteration over a list of <code>Intersection</code>s that can
     *         yield minimum-turn paths to the ending street.
     */
    protected Iterator<Intersection> getIntersections(Crumb current) {
        return new MinTurnsIterator(this.map, (CrumbMinTurns) current);
    }
}
