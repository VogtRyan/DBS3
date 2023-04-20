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

/**
 * The <code>GeodesicFinderMinDistance</code> class is responsible for finding
 * {@link Geodesic}s between two segments such that distance travelled is
 * minimized.
 */
public class GeodesicFinderMinDistance extends GeodesicFinder {
    /**
     * Creates a new <code>GeodesicFinderMinDistance</code> that operates on
     * the given map.
     *
     * @param map the map on which this search algorithm operates.
     */
    public GeodesicFinderMinDistance(Map map) {
        super(map, "minimal Euclidean distance");
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
        return new CrumbMinDistance(waypoint, prev, endPoint);
    }
}
