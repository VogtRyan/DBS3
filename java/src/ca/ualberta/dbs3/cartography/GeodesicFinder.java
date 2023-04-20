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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The <code>GeodesicFinder</code> class implements a variation on the MEAS
 * (multi-expansion A*) search algorithm. It returns all paths between two
 * {@link Segment}s that minimize some primary search metric, such as the
 * number of turns. In other words, it is a search algorithm for all possible
 * segment-paths that minimize a primary metric.
 */
public abstract class GeodesicFinder {
    /**
     * The map on which this search algorithm functions.
     */
    protected Map map;

    /**
     * A description of the types of geodesics returned by this algorithm.
     */
    private String description;

    /**
     * Creates a new <code>GeodesicFinder</code> that functions on the given
     * map.
     *
     * @param map the map on which this search algorithm functions.
     * @param description the description of the geodesics found by this
     *        algorithm.
     */
    public GeodesicFinder(Map map, String description) {
        this.map = map;
        this.description = description;
    }

    /**
     * Returns the description of the geodesics found by this algorithm.
     *
     * @return the description of the algorithm.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Returns the set of all geodesics that minimize the primary metric
     * between the two given segments. This function is commutative in that
     * <code>getGeodesics(a,b)</code> returns the same set of geodesics as
     * <code>getGeodesics(b,a)</code>. The two segments need not be unique, but
     * geodesics returned for non-unique segments are guaranteed to have a cost
     * of zero.
     *
     * @param start the starting segment for the geodesic search.
     * @param end the target segment for the geodesic search.
     * @return all possible geodesics between the two segments.
     */
    public GeodesicSet getGeodesics(Segment start, Segment end) {
        PriorityQueue<GeodesicCrumb> queue =
                new PriorityQueue<GeodesicCrumb>();
        HashMap<Street, StreetCache> caches =
                new HashMap<Street, StreetCache>();
        List<GeodesicCrumb> geodesics = new LinkedList<GeodesicCrumb>();
        Crumb aGeodesic = null;

        /* Add the starting segment location */
        Street startStreet = start.getStreet();
        Street endStreet = end.getStreet();
        Point endPoint = end.getCentrePoint();
        Waypoint startWaypoint =
                new Waypoint(start.getCentrePoint(), startStreet);
        Waypoint endWaypoint = new Waypoint(endPoint, endStreet);
        Crumb startCrumb =
                this.getCrumb(startWaypoint, null, endPoint, endStreet);
        StreetCache startCache = this.getStreetCache(startStreet, caches);
        startCache.addToQueue(new SegmentCrumb(startCrumb, null, start),
                queue);

        /*
         * Perform the MEAS search. See the PathfinderOptimal class for more
         * information.
         */
        while (true) {
            /* If we exceed the cost of a current geodesic, we're done */
            GeodesicCrumb geoCurrent = queue.poll();
            if (geoCurrent == null)
                return this.reconstructGeodesics(geodesics);
            Crumb current = geoCurrent.getCrumb();
            if (current.isObsolete())
                continue;
            if (aGeodesic != null
                    && aGeodesic.primaryBound() < current.primaryBound())
                return this.reconstructGeodesics(geodesics);

            /* Check if we've found a new geodesic */
            Waypoint currentWaypoint = current.getWaypoint();
            if ((geoCurrent instanceof SegmentCrumb)
                    && currentWaypoint.equals(endWaypoint)) {
                geodesics.add(geoCurrent);
                aGeodesic = current;
                continue;
            }

            /* If we can walk straight to the destination segment, we do so */
            Street currentStreet = currentWaypoint.getStreet();
            if (endStreet.equals(currentStreet)) {
                Crumb crumb = this.getCrumb(endWaypoint, current, endPoint,
                        endStreet);
                queue.add(new SegmentCrumb(crumb, geoCurrent, end));
                continue;
            }

            /*
             * Otherwise, iterate through all the possible intersections we
             * could take.
             */
            Point currentPoint = currentWaypoint.getLocation();
            Iterator<Intersection> it = this.getIntersections(current);
            while (it.hasNext()) {
                Intersection mirror = it.next().getMirror();
                Street mirrorStreet = mirror.getStreet();
                StreetCache cache = this.getStreetCache(mirrorStreet, caches);
                Point nextPoint = mirror.getCentrePoint();

                /*
                 * If we're not physically moving, make sure we're not stuck in
                 * a loop from, e.g., a three-way intersection.
                 */
                if (currentPoint.equals(nextPoint)
                        && current.tracesBackTo(mirrorStreet))
                    continue;

                Waypoint nextWaypoint = new Waypoint(nextPoint, mirrorStreet);
                Crumb nextCrumb = this.getCrumb(nextWaypoint, current,
                        endPoint, endStreet);
                cache.addToQueue(
                        new IntersectionCrumb(nextCrumb, geoCurrent, mirror),
                        queue);
            }
        }
    }

    /**
     * Returns an iteration of the intersections on the current street to be
     * attempted by the pathfinder, given the <code>Crumb</code> currently
     * being expanded. Intersections that are guaranteed not to produce best
     * paths need not be returned by the iterator. By default, an iteration
     * over all the intersections on the street except an intersection leading
     * back to the street of the previous crumb is returned, but subclasses can
     * override this method if better optimizations exist.
     *
     * @param current the crumb currently being expanded.
     * @return an iteration of intersections on the current street for the
     *         pathfinder to try to use.
     */
    protected Iterator<Intersection> getIntersections(Crumb current) {
        Crumb prev = current.getPrevious();
        Street exclude = null;
        if (prev != null)
            exclude = prev.getWaypoint().getStreet();
        Street cStreet = current.getWaypoint().getStreet();
        return this.map.getIntersectionsExcluding(cStreet, exclude);
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
    protected abstract Crumb getCrumb(Waypoint waypoint, Crumb prev,
            Point endPoint, Street endStreet);

    /**
     * Returns the cache for the given street, generating it on-demand if
     * necessary. See the <code>StreetCache</code> class, below, for more
     * information.
     *
     * @param street the street for which to return or generate the cache.
     * @param map the hashmap from which to draw or store the cache.
     * @return the street cache for that street.
     */
    private StreetCache getStreetCache(Street street,
            HashMap<Street, StreetCache> map) {
        StreetCache ret = map.get(street);
        if (ret == null) {
            ret = new StreetCache();
            map.put(street, ret);
        }
        return ret;
    }

    /**
     * Reconstructs the set of {@link Geodesic}s based on the terminal path
     * pieces.
     *
     * @param crumbList the list of terminal crumbs.
     * @return the set of Geodesics, one per terminal crumb.
     */
    private GeodesicSet reconstructGeodesics(List<GeodesicCrumb> crumbList) {
        if (crumbList.isEmpty())
            throw new IllegalArgumentException("No geodesics found");
        GeodesicSet ret = new GeodesicSet();

        for (GeodesicCrumb terminalCrumb : crumbList) {
            double cost = terminalCrumb.getCrumb().primaryBound();
            Geodesic geo = new Geodesic(cost);
            GeodesicCrumb next = null;
            GeodesicCrumb current = terminalCrumb;
            while (current != null) {
                current.addToGeodesic(geo, next);
                next = current;
                current = next.getPrevious();
            }
            ret.add(geo);
        }
        return ret;
    }

    /**
     * The <code>StreetCache</code> class is responsible for storing the list
     * of non-obsolete {@link Crumb}s dropped on a given street.
     */
    private class StreetCache {
        /**
         * The non-obsolete crumbs for this street.
         */
        private List<Crumb> cache;

        /**
         * Creates a new <code>StreetCache</code> in which all of the crumbs on
         * the street may obsolete each other (that is, StreetCut is enabled).
         */
        public StreetCache() {
            this.cache = new LinkedList<Crumb>();
        }

        /**
         * Attempts to add the given crumb to the crumb cache for this street,
         * updating the cache by removing obsolete entries as necessary. If the
         * given crumb is not rendered obsolete, it is added to the given
         * queue.
         *
         * @param geoCrumb the crumb to add to the cache.
         * @param queue the queue to which to add the crumb if it is not
         *        obsolete.
         */
        public void addToQueue(GeodesicCrumb geoCrumb,
                PriorityQueue<GeodesicCrumb> queue) {
            Crumb crumb = geoCrumb.getCrumb();
            Iterator<Crumb> it = this.cache.iterator();
            while (it.hasNext()) {
                Crumb old = it.next();

                /*
                 * For geodesic searches, we have to allow primary-metric ties
                 * on obsolesence checks.
                 */
                int obs = old.checkObsolete(crumb, true);
                if (obs > 0) {
                    old.setObsolete();
                    it.remove();
                } else if (obs < 0) {
                    crumb.setObsolete();
                    break;
                }
            }

            if (crumb.isObsolete() == false) {
                this.cache.add(crumb);
                queue.add(geoCrumb);
            }
        }
    }

    /**
     * The <code>GeodesicCrumb</code> class is a wrapper around a {@link Crumb}
     * that stores additional information for reconstructing geodesics.
     */
    private abstract class GeodesicCrumb implements Comparable<GeodesicCrumb> {
        /**
         * The crumb inside the wrapper.
         */
        private Crumb crumb;

        /**
         * The wrapper around the previous crumb on the path, or
         * <code>null</code> if this is the first loation in the path.
         */
        private GeodesicCrumb previous;

        /**
         * Creates a new <code>GeodesicCrumb</code> wrapped around the given
         * crumb.
         *
         * @param crumb the inner crumb.
         * @param previous the previous crumb on the path, or <code>null</code>
         *        if this is the first crumb on the path.
         */
        public GeodesicCrumb(Crumb crumb, GeodesicCrumb previous) {
            this.crumb = crumb;
            this.previous = previous;
        }

        /**
         * Returns the inner crumb around which this geodesic crumb is wrapped.
         *
         * @return the inner crumb.
         */
        public Crumb getCrumb() {
            return this.crumb;
        }

        /**
         * Returns the previous geodesic crumb on the path.
         *
         * @return the previous geodesic crumb, or <code>null</code> if this is
         *         the first crumb in the path.
         */
        public GeodesicCrumb getPrevious() {
            return this.previous;
        }

        /**
         * Compares the two inner crumbs for order.
         *
         * @param o the other crumb wrapper to compare.
         * @return a value identical to the comparison return of the two inner
         *         crumbs.
         */
        public int compareTo(GeodesicCrumb o) {
            return this.crumb.compareTo(o.crumb);
        }

        /**
         * Adds the segment at the point of this crumb, and all segments
         * leading up to (but not including) the segment at the point of the
         * given crumb, to the given geodesic.
         *
         * @param geodesic the geodesic to which to add the segments.
         * @param next the next crumb in the path, or <code>null</code> if this
         *        crumb is the final crumb in the path.
         */
        public abstract void addToGeodesic(Geodesic geodesic,
                GeodesicCrumb next);
    }

    /**
     * The <code>IntersectionCrumb</code> class is a wrapper around a standard
     * {@link Crumb} that includes the annotation that an intersection exists
     * at the point of the crumb.
     */
    private class IntersectionCrumb extends GeodesicCrumb {
        /**
         * The intersection at the point of the crumb.
         */
        private Intersection intersection;

        /**
         * Creates a new crumb wrapper with the given intersection information.
         *
         * @param crumb the crumb around which to wrap this wrapper.
         * @param previous the previous crumb on the path, or <code>null</code>
         *        if this is the first crumb on the path.
         * @param intersection the intersection where the given crumb is
         *        located.
         */
        public IntersectionCrumb(Crumb crumb, GeodesicCrumb previous,
                Intersection intersection) {
            super(crumb, previous);
            this.intersection = intersection;
        }

        /**
         * Returns the intersection annotated onto this crumb.
         *
         * @return the associated intersection.
         */
        public Intersection getIntersection() {
            return this.intersection;
        }

        /**
         * Adds the segment at the point of this crumb, and all segments
         * leading up to (but not including) the segment at the point of the
         * given crumb, to the given geodesic.
         *
         * @param geodesic the geodesic to which to add the segments.
         * @param next the next crumb in the path, or <code>null</code> if this
         *        crumb is the final crumb in the path.
         */
        public void addToGeodesic(Geodesic geodesic, GeodesicCrumb next) {
            if (next == null)
                return;

            Iterator<Segment> it;
            if (next instanceof IntersectionCrumb) {
                IntersectionCrumb ic = (IntersectionCrumb) next;
                it = map.getSegmentsBetween(this.intersection,
                        ic.intersection.getMirror());
            } else {
                SegmentCrumb sc = (SegmentCrumb) next;
                it = map.getSegmentsBetween(this.intersection,
                        sc.getSegment());
            }
            while (it.hasNext()) {
                Segment seg = it.next();
                geodesic.addSegment(seg);
            }
        }
    }

    /**
     * The <code>SegmentCrumb</code> class is a wrapper around a standard
     * {@link Crumb} that includes the annotation that a segment exists at the
     * point of the crumb.
     */
    private class SegmentCrumb extends GeodesicCrumb {
        /**
         * The segment at the point of the crumb.
         */
        private Segment segment;

        /**
         * Creates a new crumb wrapper with the given intersection information.
         *
         * @param crumb the crumb around which to wrap this wrapper.
         * @param previous the previous crumb on the path, or <code>null</code>
         *        if this is the first crumb on the path.
         * @param segment the segment where the given crumb is located.
         */
        public SegmentCrumb(Crumb crumb, GeodesicCrumb previous,
                Segment segment) {
            super(crumb, previous);
            this.segment = segment;
        }

        /**
         * Returns the segment annotated onto this crumb.
         *
         * @return the associated segment.
         */
        public Segment getSegment() {
            return this.segment;
        }

        /**
         * Adds the segment at the point of this crumb, and all segments
         * leading up to (but not including) the segment at the point of the
         * given crumb, to the given geodesic.
         *
         * @param geodesic the geodesic to which to add the segments.
         * @param next the next crumb in the path, or <code>null</code> if this
         *        crumb is the final crumb in the path.
         */
        public void addToGeodesic(Geodesic geodesic, GeodesicCrumb next) {
            geodesic.addSegment(this.segment);
            if (next == null)
                return;

            Iterator<Segment> it;
            if (next instanceof IntersectionCrumb) {
                IntersectionCrumb ic = (IntersectionCrumb) next;
                it = map.getSegmentsBetween(ic.getIntersection().getMirror(),
                        this.segment);
            } else {
                SegmentCrumb sc = (SegmentCrumb) next;
                it = map.getSegmentsBetween(this.segment, sc.segment);
            }
            while (it.hasNext()) {
                Segment seg = it.next();
                geodesic.addSegment(seg);
            }
        }
    }
}
