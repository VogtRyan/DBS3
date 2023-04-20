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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The <code>PathfinderOptimal</code> class is an implementation of the MEAS
 * (multi-expansion A*) search algorithm with the option to enable StreetCut.
 * It is a pathfinder that attempts to minimize distance travelled, where
 * distance is measured by some metric. For any two points on the same street,
 * the optimal pathfinder will move an agent in a straight line between those
 * two paths. Otherwise, the agent will choose to move to intersections such as
 * to minimize some {@link Path} metric.
 * <p>
 * The cost metric being minimized must be monotonic, in the sense that if a
 * Path <code>p</code> has cost <code>c</code>, then adding an additional
 * {@link Waypoint} at any index within <code>p</code> must not decrease the
 * cost (the new cost must be greater than or equal to <code>c</code>).
 */
public abstract class PathfinderOptimal extends Pathfinder {
    /**
     * The number of decimal places with which to output gathered metrics.
     */
    private static final int DECIMAL_PLACES = 4;

    /**
     * The map on which this pathfinder functions.
     */
    protected Map map;

    /**
     * Whether the street cut algorithm should be enabled.
     */
    private boolean streetCutEnabled;

    /**
     * A metric recording the number of obsoletions of crumbs prior to being
     * added to the queue.
     */
    private long preObsolete;

    /**
     * A metric recording he number of obsoletions of crumbs that were still in
     * the queue.
     */
    private long queueObsolete;

    /**
     * A metric recording the number of obsoletions of crumbs that had already
     * been expanded from the queue.
     */
    private long lateObsolete;

    /**
     * Creates a new <code>PathfinderOptimal</code> that functions on the given
     * map.
     *
     * @param map the map on which this pathfinder functions.
     */
    public PathfinderOptimal(Map map) {
        this.map = map;
        this.preObsolete = this.queueObsolete = this.lateObsolete = 0L;
        this.streetCutEnabled = true;
    }

    /**
     * Sets the StreetCut algorithm to be enabled for this pathfinder. By
     * default, StreetCut is active.
     *
     * @param enabled whether or not the StreetCut algorithm should be enabled.
     */
    public void setStreetCutEnabled(boolean enabled) {
        this.streetCutEnabled = enabled;
    }

    /**
     * Returns the <code>Path</code> that an agent should take between the two
     * given points. The path is guaranteed to have at least two
     * <code>Waypoint</code>s: the two given points. Note, though, that those
     * <code>Waypoint</code>s need not be unique.
     *
     * @param start the starting location of the agent.
     * @param end the ending location of the agent.
     * @param prng the pseudorandom number generator to use.
     * @return a path for the agent to follow between the two given points.
     */
    public Path getPath(Waypoint start, Waypoint end, Random prng) {
        PriorityQueue<Crumb> queue = new PriorityQueue<Crumb>();
        HashMap<Street, StreetCache> caches =
                new HashMap<Street, StreetCache>();

        /*
         * Either given location, if in an intersection, could be perceived as
         * being on any number of streets.
         */
        List<Street> startStreets = this.getStreetsContaining(start);
        List<Street> endStreets = this.getStreetsContaining(end);
        Point startPoint = start.getLocation();
        Point endPoint = end.getLocation();
        for (Street ss : startStreets) {
            Waypoint startWaypoint = new Waypoint(startPoint, ss);
            Crumb crumb =
                    this.getCrumb(startWaypoint, null, endPoint, endStreets);
            StreetCache cache = this.getStreetCache(ss, caches);
            cache.addToQueue(crumb, queue);
        }

        /*
         * Perform a modified A* search: account for the fact that even though
         * a path arriving at a point is higher cost than a previous path that
         * arrived at that point, the newly arrived path may turn out to be a
         * better path (think of minimizing angle change; you could arrive at
         * intersection i with higher angle change than a previous incoming
         * path, but your angle of incidence to the intersection could leave
         * you in a more optimal position going forward).
         */
        while (true) {
            /*
             * If the smallest lower-bound cost piece on the queue is a
             * terminal piece, the search is complete. If it is an obsolete
             * piece, rendered useless by something we have queued, we can skip
             * it.
             */
            Crumb current = queue.poll();
            if (current.isObsolete())
                continue;
            Waypoint currentWaypoint = current.getWaypoint();
            if (currentWaypoint.getLocation().equals(endPoint))
                return this.reconstructPath(current, start, end);

            /*
             * Check if we are on a possible destination street, in which case
             * we walk in a straight line to the finish.
             *
             * We do not cache this crumb; we only cache things that may be
             * expanded. Technically, we could cache this crumb (potentially
             * obsoleting things to be expanded on this street), but we would
             * have to change the flagObsolete function for all implementations
             * of PathfinderOptimal Crumbs so that this crumb doesn't get
             * obsoleted; and, there would be no point in doing so anyway.
             */
            Street currentStreet = currentWaypoint.getStreet();
            if (endStreets.contains(currentStreet)) {
                Waypoint endWaypoint = new Waypoint(endPoint, currentStreet);
                Crumb crumb = this.getCrumb(endWaypoint, current, endPoint,
                        endStreets);
                queue.add(crumb);
                continue;
            }

            /*
             * Otherwise, iterate through all the possible intersections we
             * could take.
             */
            Iterator<Intersection> it = this.getIntersections(current);
            while (it.hasNext()) {
                /*
                 * For each of the four points in the intersection, create a
                 * crumb by extending the current path to that point. Check the
                 * new crumbs against the cache for obsolesence, and if they
                 * are not rendered obsolete, add them to the queue.
                 */
                Intersection mirror = it.next().getMirror();
                Street mirrorStreet = mirror.getStreet();
                StreetCache cache = this.getStreetCache(mirrorStreet, caches);
                List<Point> points = cache.getPoints(mirror, prng);
                for (Point nextPoint : points) {

                    /*
                     * In the GeodesicFinder version of the MEAS algorithm, we
                     * need a check here that looks like:
                     *
                     *     if (currentPoint.equals(nextPoint)
                     *             && current.tracesBackTo(mirrorStreet))
                     *         break;
                     *
                     * That check is not necessary in this version of the
                     * algorithm, because primary-metric ties are forbidden by
                     * checkObsolete(). If a new crumb is dropped with the same
                     * metrics at the same location as an existing crumb, the
                     * new crumb will be rendered obsolete (note that the
                     * preference to obsolete the new crumb over the existing
                     * crumb -- i.e., the order of the two crumb parameters --
                     * is important here).
                     */

                    Waypoint nextWaypoint =
                            new Waypoint(nextPoint, mirrorStreet);
                    Crumb nextCrumb = this.getCrumb(nextWaypoint, current,
                            endPoint, endStreets);
                    cache.addToQueue(nextCrumb, queue);
                }
            }
        }
    }

    /**
     * Prints all of the metrics gathered during calls to {@link #getPath}
     * during which {@link #setGatherMetrics} had been set to
     * <code>true</code>.
     */
    public void printMetrics() {
        long total = this.preObsolete + this.queueObsolete + this.lateObsolete;
        System.out.format(
                "Obsoletions of pre-queue crumbs: %"
                        + (PathfinderOptimal.DECIMAL_PLACES + 4) + "."
                        + PathfinderOptimal.DECIMAL_PLACES + "f%% (%d)%n",
                ((double) this.preObsolete) / total * 100, this.preObsolete);
        System.out.format(
                "Obsoletions of crumbs in queue:  %"
                        + (PathfinderOptimal.DECIMAL_PLACES + 4) + "."
                        + PathfinderOptimal.DECIMAL_PLACES + "f%% (%d)%n",
                ((double) this.queueObsolete) / total * 100,
                this.queueObsolete);
        System.out.format(
                "Obsoletions of expanded crumbs:  %"
                        + (PathfinderOptimal.DECIMAL_PLACES + 4) + "."
                        + PathfinderOptimal.DECIMAL_PLACES + "f%% (%d)%n",
                ((double) this.lateObsolete) / total * 100, this.lateObsolete);
    }

    /**
     * Returns an iteration of the <code>Intersection</code>s on the current
     * {@link Street} to be attempted by the pathfinder, given the
     * <code>Crumb</code> currently being expanded. Intersections that are
     * guaranteed not to produce best paths need not be returned by the
     * iterator. By default, an iteration over all the intersections on the
     * street except an intersection leading back to the street of the previous
     * crumb is returned, but subclasses can override this method if better
     * optimizations exist.
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
    protected abstract Crumb getCrumb(Waypoint waypoint, Crumb prev,
            Point endPoint, List<Street> endStreets);

    /**
     * Returns a list of all the streets containing the given waypoint,
     * including the street associated with the given waypoint.
     *
     * @param waypoint the waypoint to test for containment on various streets.
     * @return a list of all the streets containing the given waypoint's
     *         physical location.
     */
    private List<Street> getStreetsContaining(Waypoint waypoint) {
        List<Street> ret = new LinkedList<Street>();
        Street str = waypoint.getStreet();
        Point pt = waypoint.getLocation();
        ret.add(str);

        Iterator<Intersection> it = this.map.getIntersections(str);
        while (it.hasNext()) {
            Intersection inter = it.next();
            if (inter.contains(pt))
                ret.add(inter.getMirror().getStreet());
        }

        return ret;
    }

    /**
     * Returns the cache for the given street, generating it on-demand if
     * necessary.
     *
     * @param street the street for which to return or generate the cache.
     * @param map the hashmap from which to draw or store the cache.
     * @return the street cache for that street.
     */
    private StreetCache getStreetCache(Street street,
            HashMap<Street, StreetCache> map) {
        StreetCache ret = map.get(street);
        if (ret == null) {
            if (this.streetCutEnabled)
                ret = new StreetCacheCut();
            else
                ret = new StreetCacheSimple();
            map.put(street, ret);
        }
        return ret;
    }

    /**
     * Reconstructs the optimal path from the terminal path piece.
     *
     * @param terminalPiece the final piece in the path to reconstruct.
     * @param start the expected start waypoint in the path.
     * @param end the expected end waypoint in the path.
     * @return the reconstructed path.
     */
    private Path reconstructPath(Crumb terminalPiece, Waypoint start,
            Waypoint end) {
        /* Walk backwards to reconstruct the path */
        Path path = new Path(terminalPiece.getWaypoint());
        while (true) {
            terminalPiece = terminalPiece.getPrevious();
            if (terminalPiece == null)
                break;
            path.prepend(terminalPiece.getWaypoint());
        }

        /*
         * Meet our guarantee of first, last elements equal to the given
         * arguments.
         */
        if (path.peek().equals(start) == false)
            path.prepend(start);
        if (path.peekEnd().equals(end) == false)
            path.append(end);
        return path;
    }

    /**
     * The <code>StreetCache</code> class is responsible for storing the four
     * random (mirrored) <code>Point</code>s chosen for each
     * {@link Intersection} on a given {@link Street} during each execution of
     * the pathfinder. It is also responsible for maintaining the list of
     * non-obsolete {@link Crumb}s dropped on that street.
     */
    private abstract class StreetCache {
        /**
         * The four mirrored points for each intersection on the street.
         */
        private HashMap<Intersection, List<Point>> points;

        /**
         * Creates a new <code>StreetCache</code> for a {@link Street}.
         */
        public StreetCache() {
            this.points = new HashMap<Intersection, List<Point>>();
        }

        /**
         * Returns the four random <code>Point</code>s stored for the given
         * intersection in this cache, generating them if necessary.
         *
         * @param intersection the intersection for which to return the random
         *        points.
         * @param prng the pseudorandom number generator used to choose the
         *        four random points.
         * @return the four random points for that intersection.
         */
        public List<Point> getPoints(Intersection intersection, Random prng) {
            List<Point> ret = this.points.get(intersection);
            if (ret == null) {
                ret = intersection.getRandomPoints(prng);
                this.points.put(intersection, ret);
            }
            return ret;
        }

        /**
         * Attempts to add the given <code>Crumb</code> to the crumb cache for
         * this street, updating the cache by removing obsolete entries as
         * necessary. If the given crumb is not rendered obsolete, it is added
         * to the given queue.
         *
         * @param crumb the crumb to add to the cache.
         * @param queue the queue to which to add the crumb if it is not
         *        obsolete.
         */
        public void addToQueue(Crumb crumb, PriorityQueue<Crumb> queue) {
            /*
             * Mathematically, we should not be able to obsolete something in
             * the cache and our crumb at the same time (triangle inequality).
             * However, as a matter of numerical stability, it can happen. If
             * it does, there is no issue of correctness (plus or minus some
             * tiny bound of numerical stability), so just let it happen.
             */
            Iterator<Crumb> it = this.getCacheIterator(crumb);
            while (it.hasNext()) {
                Crumb old = it.next();
                int obs = old.checkObsolete(crumb);
                if (obs > 0) {
                    if (gatherMetrics) {
                        if (queue.contains(old))
                            queueObsolete++;
                        else
                            lateObsolete++;
                    }
                    old.setObsolete();
                    it.remove();
                } else if (obs < 0) {
                    /*
                     * Faster in practice to break, rather than to keep trying
                     * to kill other things in the cache, since with almost
                     * certain probability (numerical stability), nothing in
                     * the cache will be obsoleted.
                     */
                    if (gatherMetrics)
                        preObsolete++;
                    crumb.setObsolete();
                    break;
                }
            }

            if (crumb.isObsolete() == false) {
                this.addToCache(crumb);
                queue.add(crumb);
            }
        }

        /**
         * Returns an iterator over the cache elements against which to compare
         * the given new crumb.
         *
         * @param newCrumb the new crumb against which to compare cache
         *        elements.
         * @return an iterator over the cache elements.
         */
        protected abstract Iterator<Crumb> getCacheIterator(Crumb newCrumb);

        /**
         * Adds the given crumb to the cache.
         *
         * @param newCrumb the new crumb to add to the cache.
         */
        protected abstract void addToCache(Crumb newCrumb);
    }

    /**
     * The <code>StreetCacheSimple</code> class is a street cache that does not
     * implement the StreetCut algorithm.
     */
    private class StreetCacheSimple extends StreetCache {
        /**
         * The non-obsolete crumbs for each point in an intersection on this
         * street.
         */
        private HashMap<Point, List<Crumb>> cache;

        /**
         * Creates a new <code>StreetCache</code> in which crumbs at a certain
         * physical location may only obsolete other crumbs at that same
         * location (that is, StreetCut is disabled).
         */
        public StreetCacheSimple() {
            this.cache = new HashMap<Point, List<Crumb>>();
        }

        /**
         * Returns an iterator over the cache elements against which to compare
         * the given new crumb.
         *
         * @param newCrumb the new crumb against which to compare cache
         *        elements.
         */
        protected Iterator<Crumb> getCacheIterator(Crumb newCrumb) {
            Point p = newCrumb.getWaypoint().getLocation();
            List<Crumb> l = this.cache.get(p);
            if (l == null) {
                l = new LinkedList<Crumb>();
                this.cache.put(p, l);
            }
            return l.iterator();
        }

        /**
         * Adds the given crumb to the cache.
         *
         * @param newCrumb the new crumb to add to the cache.
         */
        protected void addToCache(Crumb newCrumb) {
            Point p = newCrumb.getWaypoint().getLocation();
            List<Crumb> l = this.cache.get(p);
            if (l == null) {
                l = new LinkedList<Crumb>();
                this.cache.put(p, l);
            }
            l.add(newCrumb);
        }
    }

    /**
     * The <code>StreetCacheCut</code> class is a street cache that implements
     * the StreetCut algorithm.
     */
    private class StreetCacheCut extends StreetCache {
        /**
         * The non-obsolete crumbs for this street.
         */
        private List<Crumb> cache;

        /**
         * Creates a new <code>StreetCache</code> in which all of the crumbs on
         * the street may obsolete each other (that is, StreetCut is enabled).
         */
        public StreetCacheCut() {
            this.cache = new LinkedList<Crumb>();
        }

        /**
         * Returns an iterator over the cache elements against which to compare
         * the given new crumb.
         *
         * @param newCrumb the new crumb against which to compare cache
         *        elements.
         */
        protected Iterator<Crumb> getCacheIterator(Crumb newCrumb) {
            return this.cache.iterator();
        }

        /**
         * Adds the given crumb to the cache.
         *
         * @param newCrumb the new crumb to add to the cache.
         */
        protected void addToCache(Crumb newCrumb) {
            this.cache.add(newCrumb);
        }
    }
}
