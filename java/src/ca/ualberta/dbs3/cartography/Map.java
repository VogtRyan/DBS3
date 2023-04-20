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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.TreeSet;

/**
 * The <code>Map</code> class represents a set of {@link Street}s on which
 * mobile agents can travel. Each <code>Street</code> on the <code>Map</code>
 * has a unique numerical identifier, ranging from <code>0</code> the one less
 * than the number of <code>Street</code>s.
 */
public class Map {
    /**
     * The minimum coordinate value that any location on the map can take.
     */
    public static final double MIN_COORD = 0.0;

    /**
     * The maximum coordinate value that any location on the map can take. Note
     * that this upper bound allows all coordinates, converted from metres to
     * millimetres, to be expressed in the range of a 32-bit unsigned integer
     * (since 4294967.295 metres is 4294967295 millimetres).
     */
    public static final double MAX_COORD = 4294967.295;

    /**
     * All of the streets in the simulation. It is guaranteed that
     * <code>this.streets[i].getID() == i</code>.
     */
    private Street[] streets;

    /**
     * All of the intersections for each street. The array at index
     * <code>i</code> is the set of intersections for the <code>Street</code>
     * with ID <code>i</code>.
     */
    private Intersection[][] intersections;

    /**
     * All of the segments, or portions of streets between intersections and
     * street endpoints, that occur in this map sorted into their natural
     * order.
     */
    private Segment[] segments;

    /**
     * The value of <code>firstSegment[i]</code> is the index into the
     * <code>segments</code> array where the first segement for street
     * <code>i</code> is located.
     */
    private int[] firstSegment;

    /**
     * All of the view specifications defined in the map file.
     */
    private ViewSpecification[] views;

    /**
     * The minimum number of turns required to travel between any two streets.
     * This 2D array is symmetric across the diagonal; however, the full-sized
     * 2D array is stored for simplicity.
     */
    private int[][] numTurns;

    /**
     * A bounding box for this map.
     */
    private BoundingBox boundingBox;

    /**
     * Creates a new <code>Map</code> by parsing the given map definition file.
     *
     * @param filename the name of the map file to parse.
     * @throws MapFileException if there are any errors in the given map file.
     */
    public Map(String filename) throws MapFileException {
        MapFileParser parser = new MapFileParser(filename);
        this.streets = parser.getStreets();
        this.computeIntersections(filename);
        this.computeSegments();
        this.verifyInBounds(filename);
        if (this.buildTurnsCache() == false)
            throw new MapFileException(filename, "map not connected");
        this.views = parser.getViews();
    }

    /**
     * Returns the number of streets on this map.
     *
     * @return the number of streets on this map.
     */
    public int numStreets() {
        return this.streets.length;
    }

    /**
     * Returns the street at the given index in the natural ordering of
     * <code>Street</code> objects.
     *
     * @param index the index of the street to retrieve.
     * @return the street at that index.
     */
    public Street getStreet(int index) {
        return this.streets[index];
    }

    /**
     * Returns the index of the given street in the natural ordering of
     * <code>Street</code> objects on this map.
     *
     * @param street the street for which to return the index.
     * @return the index of the street.
     */
    public int indexOf(Street street) {
        int i = street.getID();
        if (i < 0 || i >= this.streets.length)
            throw new IllegalArgumentException("Invalid street index");
        return i;
    }

    /**
     * Returns an iteration over all the streets on this map, ordered by the
     * natural ordering of the <code>Street</code> objects.
     *
     * @return an iteration over all the streets.
     */
    public Iterator<Street> getStreets() {
        return new ArrayIterator<Street>(this.streets);
    }

    /**
     * Returns the number of intersections that the given street has.
     *
     * @param street the street for which to return the number of
     *        intersections.
     * @return the number of intersections that street has.
     */
    public int numIntersections(Street street) {
        return this.intersections[street.getID()].length;
    }

    /**
     * Returns the intersection belonging to the given street at the given
     * index into the iteration over all the street's intersections.
     *
     * @param street the street for which to return the intersection.
     * @param index the intersection index.
     * @return the intersection for the given street at the given index.
     */
    public Intersection getIntersection(Street street, int index) {
        return this.intersections[street.getID()][index];
    }

    /**
     * Returns an iterator over all of the intersections for the given street.
     *
     * @param street the street for which to get the intersections.
     * @return an iteration over the street's intersections.
     */
    public Iterator<Intersection> getIntersections(Street street) {
        Intersection[] array = this.intersections[street.getID()];
        return new ArrayIterator<Intersection>(array);
    }

    /**
     * Returns an iterator over all of the intersections for the given street,
     * excluding (if applicable) any intersections leading to the given street.
     *
     * @param street the street for which to get the intersections.
     * @param excluding exclude any intersections leading to
     *        <code>excluding</code>, or <code>null</code> to exclude no
     *        intersections.
     * @return an iteration over the street's intersections.
     */
    public Iterator<Intersection> getIntersectionsExcluding(Street street,
            Street excluding) {
        return new ExcludeIterator(street, excluding);
    }

    /**
     * Returns an iterator over all of the unique intersections on the map
     * (that is, for each mirrored-intersection pair, only one intersection
     * will appear in the iteration).
     *
     * @return an iterator over the map's unique intersections.
     */
    public Iterator<Intersection> getUniqueIntersections() {
        return new UniqueIntersectionIterator();
    }

    /**
     * Returns the total number of segments on this map.
     *
     * @return the number of segments on this map.
     */
    public int numSegments() {
        return this.segments.length;
    }

    /**
     * Returns the number of segments belonging to the given street.
     *
     * @param street the street for which to count the segments.
     * @return the number of segments belonging to that street.
     */
    public int numSegments(Street street) {
        int sid = street.getID();
        int nextFirst;

        if (sid == this.streets.length - 1)
            nextFirst = this.segments.length;
        else
            nextFirst = this.firstSegment[sid + 1];
        return (nextFirst - this.firstSegment[sid]);
    }

    /**
     * Returns the segment at the given index into the ordering of all the
     * segments on the map.
     *
     * @param index the index into the ordering of all the segments.
     * @return the segment at the given index.
     */
    public Segment getSegment(int index) {
        return this.segments[index];
    }

    /**
     * Returns the segment at the given index into the ordering of the segments
     * on the given street.
     *
     * @param street the street from which to fetch a segment.
     * @param index the index into the order of the segments on the given
     *        street.
     * @return the segment at the given index for the given street.
     */
    public Segment getSegment(Street street, int index) {
        int num = this.numSegments(street);
        if (index < 0 || index >= num)
            throw new ArrayIndexOutOfBoundsException("Invalid index " + index);

        int first = this.firstSegment[street.getID()];
        return this.segments[first + index];
    }

    /**
     * Returns the index of the given segment in the natural ordering of all
     * the segments on this map.
     *
     * @param segment the segment for which to return the index.
     * @return the index of the segment.
     */
    public int indexOf(Segment segment) {
        int sid = segment.getStreet().getID();
        int bsr;

        if (sid == this.streets.length - 1)
            bsr = Arrays.binarySearch(this.segments, this.firstSegment[sid],
                    this.segments.length, segment);
        else
            bsr = Arrays.binarySearch(this.segments, this.firstSegment[sid],
                    this.firstSegment[sid + 1], segment);

        if (bsr < 0)
            throw new IllegalArgumentException("Invalid segment index");
        return bsr;
    }

    /**
     * Returns an iteration over all the segments on this map, ordered by the
     * natural ordering of the <code>Segment</code> objects.
     *
     * @return an iteration over all the segments.
     */
    public Iterator<Segment> getSegments() {
        return new ArrayIterator<Segment>(this.segments);
    }

    /**
     * Returns an iteration over all the segments for the given street, ordered
     * by the natural ordering of the <code>Segment</code> objects.
     *
     * @param street the street for which to get the segments.
     * @return an iteration over all the segments on that street.
     */
    public Iterator<Segment> getSegments(Street street) {
        int first = this.firstSegment[street.getID()];
        int last = first + this.numSegments(street) - 1;
        return new ArrayIterator<Segment>(this.segments, first, last);
    }

    /**
     * Returns an iteration over the segments that occur in the natural
     * ordering between, but not including, the two given segments.
     *
     * @param segA one segment.
     * @param segB the other segment.
     * @return an iteration over all of the segments between the two given
     *         segments.
     * @throws IllegalArgumentException if the two segments are on different
     *         streets.
     */
    public Iterator<Segment> getSegmentsBetween(Segment segA, Segment segB) {
        /* Get all the segments on the street */
        Street str = segA.getStreet();
        if (str.equals(segB.getStreet()) == false)
            throw new IllegalArgumentException(
                    "Segments on different streets");

        /* Find the indices of the given segments */
        int first = this.firstSegment[str.getID()];
        int num = this.numSegments(str);
        int a = Arrays.binarySearch(this.segments, first, first + num, segA);
        int b = Arrays.binarySearch(this.segments, first, first + num, segB);
        if (a < 0 || b < 0)
            throw new IllegalArgumentException("Segments not on this map");

        /* Return the iteration */
        if (a < b)
            return new ArrayIterator<Segment>(this.segments, a + 1, b - 1);
        else
            return new ArrayIterator<Segment>(this.segments, b + 1, a - 1);
    }

    /**
     * Returns an iteration over the segments that occur in the natural
     * ordering between the given segment and intersection (not including the
     * given segment).
     *
     * @param inter the given intersection.
     * @param seg the given segment.
     * @return an iteration over all of the segments between the intersection
     *         and segment, not including the given segment.
     * @throws IllegalArgumentException if the segment and intersection are on
     *         different streets.
     */
    public Iterator<Segment> getSegmentsBetween(Intersection inter,
            Segment seg) {
        /* Get all the segments on the street */
        Street str = seg.getStreet();
        if (str.equals(inter.getStreet()) == false)
            throw new IllegalArgumentException(
                    "Segment and intersection on different streets");

        /*
         * Find the index of the segment and of the closest segment to the
         * intersection that occurs before the intersection.
         */
        int first = this.firstSegment[str.getID()];
        int num = this.numSegments(str);
        int segIndex =
                Arrays.binarySearch(this.segments, first, first + num, seg);
        int before = this.closestSegmentBefore(inter, first, first + num);
        if (segIndex < 0)
            throw new IllegalArgumentException("Segment not on map");

        /* Return the iteration */
        if (segIndex < before)
            return new ArrayIterator<Segment>(this.segments, segIndex + 1,
                    before);
        else
            return new ArrayIterator<Segment>(this.segments, before + 1,
                    segIndex - 1);
    }

    /**
     * Returns an iteration over the segments that occur in the natural
     * ordering of segments between the two given intersections.
     *
     * @param interA one intersection.
     * @param interB the other intersection.
     * @return an iteration over all of the segments between the two given
     *         intersections.
     * @throws IllegalArgumentException if the two intersections are on
     *         different streets.
     */
    public Iterator<Segment> getSegmentsBetween(Intersection interA,
            Intersection interB) {
        /* Get all the segments on the street */
        Street str = interA.getStreet();
        if (str.equals(interB.getStreet()) == false)
            throw new IllegalArgumentException(
                    "Intersections on different streets");

        /*
         * Find the indices of the closest segments that occur before each
         * intersection.
         */
        int first = this.firstSegment[str.getID()];
        int num = this.numSegments(str);
        int a = this.closestSegmentBefore(interA, first, first + num);
        int b = this.closestSegmentBefore(interB, first, first + num);

        /* Return the iteration */
        if (a < b)
            return new ArrayIterator<Segment>(this.segments, a + 1, b);
        else
            return new ArrayIterator<Segment>(this.segments, b + 1, a);
    }

    /**
     * Returns the number of view specifications in the map file.
     *
     * @return the number of view specifications in the map file.
     */
    public int numViewSpecifications() {
        return this.views.length;
    }

    /**
     * Returns the view specification at the given index, ordered by their
     * appearance in the map file.
     *
     * @param index the index of the view specification to return.
     * @return the view specification at the given index.
     */
    public ViewSpecification getViewSpecification(int index) {
        return this.views[index];
    }

    /**
     * Returns an iteration over all of the view specifications defined in the
     * map file.
     *
     * @return an iteration over all the view specifications.
     */
    public Iterator<ViewSpecification> getViewSpecifications() {
        return new ArrayIterator<ViewSpecification>(this.views);
    }

    /**
     * Returns a bounding box around this map.
     *
     * @return a bounding box containing all of the points on this map.
     */
    public BoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    /**
     * Returns the minimum number of turns required to travel from the given
     * street to the other given street.
     *
     * @param fromStreet the starting street.
     * @param toStreet the ending street.
     * @return the minimum number of turns to get from the starting street to
     *         the ending street.
     */
    public int minTurns(Street fromStreet, Street toStreet) {
        return this.numTurns[fromStreet.getID()][toStreet.getID()];
    }

    /**
     * Initializes the set of intersections in this map.
     *
     * @param filename the name of the map file.
     * @throws MapFileException if the computed intersections are not
     *         well-defined single points in space (for example, if one street
     *         contains another).
     */
    private void computeIntersections(String filename)
            throws MapFileException {
        List<List<Intersection>> ints = new ArrayList<List<Intersection>>();
        for (int i = 0; i < this.streets.length; i++)
            ints.add(new ArrayList<Intersection>());

        for (int i = 0; i < this.streets.length - 1; i++) {
            for (int j = i + 1; j < this.streets.length; j++) {

                /* Compute the intersection for the first pair */
                Intersection inter = null;
                try {
                    inter = Intersection.compute(this.streets[i],
                            this.streets[j]);
                } catch (IllegalArgumentException iae) {
                    throw new MapFileException(filename, iae.getMessage());
                }
                if (inter == null)
                    continue;

                /*
                 * If the intersection exists, mirror it for the other street
                 */
                ints.get(i).add(inter);
                ints.get(j).add(inter.getMirror());
            }
        }

        this.intersections = new Intersection[this.streets.length][];
        for (int i = 0; i < this.streets.length; i++) {
            List<Intersection> list = ints.get(i);
            this.intersections[i] =
                    list.toArray(new Intersection[list.size()]);
        }
    }

    /**
     * Initializes and computes the set of segments in this map.
     */
    private void computeSegments() {
        List<Segment> segs = new ArrayList<Segment>();
        this.firstSegment = new int[this.streets.length];

        for (int onStr = 0; onStr < this.streets.length; onStr++) {
            this.firstSegment[onStr] = segs.size();

            /*
             * Get all of the unique segmentation points of the street defined
             * by intersections.
             */
            TreeSet<SegmentationPoint> sps = new TreeSet<SegmentationPoint>();
            int numInters = this.intersections[onStr].length;
            for (int onInter = 0; onInter < numInters; onInter++) {
                Intersection i = this.intersections[onStr][onInter];
                sps.add(i.getSegmentationPoint());
            }

            /*
             * If the start of the street occurs before any intersection-based
             * segmentation point, add a segmentation point for the start of
             * the street. Same idea for the end of the street, too.
             */
            Point[] ends = this.streets[onStr].getMidline().getPoints();
            String[] desc = this.describeEnds(ends);
            SegmentationPoint start = new SegmentationPoint(ends[0],
                    this.streets[onStr], desc[0]);
            SegmentationPoint end = new SegmentationPoint(ends[1],
                    this.streets[onStr], desc[1]);
            if (sps.isEmpty() || sps.first().compareTo(start) > 0)
                sps.add(start);
            if (sps.last().compareTo(end) < 0)
                sps.add(end);

            /*
             * For each consecutive pair of SegmentationPoints, create a
             * Segment.
             */
            Iterator<SegmentationPoint> it = sps.iterator();
            SegmentationPoint prev = null;
            while (it.hasNext()) {
                SegmentationPoint next = it.next();
                if (prev != null)
                    segs.add(new Segment(prev, next));
                prev = next;
            }
        }

        /* Sort the segments into natural ordering */
        Collections.sort(segs);
        this.segments = segs.toArray(new Segment[segs.size()]);
    }

    /**
     * Given two points representing the endpoints of a street, return
     * human-readable descriptions (in terms of compass directions) that
     * differentiate the two endpoints of the street.
     *
     * @param ends the two ends to describe.
     * @return a description of the two ends.
     */
    private String[] describeEnds(Point[] ends) {
        String[] ret = new String[2];
        ret[0] = ret[1] = "";

        double xDiff = ends[0].getX() - ends[1].getX();
        double yDiff = ends[0].getY() - ends[1].getY();

        if (xDiff > 0) {
            ret[0] = "east";
            ret[1] = "west";
        } else if (xDiff < 0) {
            ret[0] = "west";
            ret[1] = "east";
        }

        if (yDiff > 0) {
            ret[0] = "south" + ret[0];
            ret[1] = "north" + ret[1];
        } else if (yDiff < 0) {
            ret[0] = "north" + ret[0];
            ret[1] = "south" + ret[1];
        }

        for (int i = 0; i < 2; i++)
            ret[i] = "The " + Character.toUpperCase(ret[i].charAt(0))
                    + ret[i].substring(1) + " End";
        return ret;
    }

    /**
     * Verifies that all of the <code>Street</code>s,
     * <code>Intersection</code>s, and <code>Segment</code>s are inside the
     * coordinate bounds of the <code>Map</code>, throwing a
     * <code>MapFileException</code> otherwise.
     *
     * @param filename the name of the map file.
     * @throws MapFileException if anything is outside the coordinate bounds.
     */
    private void verifyInBounds(String filename) throws MapFileException {
        this.boundingBox = new BoundingBox(this.streets[0]);
        for (int i = 0; i < this.streets.length; i++) {

            /* Verify that each street is in bounds */
            Street str = this.streets[i];
            this.boundingBox = this.boundingBox.expand(str);
            if (str.fallsWithin(Map.MIN_COORD, Map.MIN_COORD, Map.MAX_COORD,
                    Map.MAX_COORD) == false)
                throw new MapFileException(filename,
                        "street out of bounds: " + str.getName());

            /* Check that every intersection of that street is in bounds */
            for (int j = 0; j < this.intersections[i].length; j++) {
                Intersection inter = this.intersections[i][j];
                this.boundingBox = this.boundingBox.expand(inter);
                if (inter.fallsWithin(Map.MIN_COORD, Map.MIN_COORD,
                        Map.MAX_COORD, Map.MAX_COORD) == false) {
                    String nameA = str.getName();
                    String nameB = inter.getMirror().getStreet().getName();
                    throw new MapFileException(filename,
                            "intersection out of bounds: " + nameA + " + "
                                    + nameB);
                }
            }
        }

        /*
         * Segmentation may also push streets out of bounds, despite both the
         * street and its intersections being in bounds.
         */
        for (int i = 0; i < this.segments.length; i++) {
            Segment seg = this.segments[i];
            this.boundingBox = this.boundingBox.expand(seg);
            if (seg.fallsWithin(Map.MIN_COORD, Map.MIN_COORD, Map.MAX_COORD,
                    Map.MAX_COORD) == false) {
                Street str = seg.getStreet();
                throw new MapFileException(filename,
                        "segment of street out of bounds: "
                                + str.getName());
            }
        }
    }

    /**
     * Computes the minimum number of turns to get between every pair of
     * streets on this map, and caches the results.
     *
     * @return <code>true</code> if the cache was successfully built, or
     *         <code>false</code> if the map is not connected.
     */
    private boolean buildTurnsCache() {
        this.numTurns = new int[this.streets.length][];
        for (int i = 0; i < this.streets.length; i++) {
            /*
             * Perform a BFS from each street to determine the number of turns.
             * If this is the first BFS performed, check connectivity.
             */
            this.numTurns[i] = this.bfs(i);
            if (i == 0) {
                for (int j = 0; j < this.numTurns[i].length; j++) {
                    if (this.numTurns[i][j] == -1)
                        return false;
                }
            }
        }
        return true;
    }

    /**
     * Performs a breadth-first search starting at the given street.
     *
     * @param startIndex the ID of the street at which to start the BFS.
     * @return an array containing the minimum number of turns required to
     *         reach every street in the map starting at the given street.
     *         Streets that cannot be reached from the given street will have a
     *         value of <code>-1</code>.
     */
    private int[] bfs(int startIndex) {
        LinkedList<Integer> queue = new LinkedList<Integer>();
        int[] distances = new int[this.streets.length];

        /* Queue the first street and set up the distance result list */
        for (int i = 0; i < this.streets.length; i++)
            distances[i] = -1;
        distances[startIndex] = 0;
        queue.offer(startIndex);

        /* Dequeue elements */
        while (true) {
            Integer deq = queue.poll();
            if (deq == null)
                break;
            int deqDist = distances[deq];
            for (int i = 0; i < this.intersections[deq].length; i++) {
                Intersection inter = this.intersections[deq][i];
                int otherID = inter.getMirror().getStreet().getID();
                if (distances[otherID] == -1) {
                    distances[otherID] = deqDist + 1;
                    queue.offer(otherID);
                }
            }
        }
        return distances;
    }

    /**
     * Returns the index of the closest segment to the given intersection, in
     * the given portion of the <code>segments</code> array, that occurs before
     * the given intersection.
     *
     * @param inter the given intersection.
     * @param fromIndex the index of the first segment (inclusive) to be
     *        searched.
     * @param toIndex the index of the last segment (exclusive) to be searched.
     * @return the index of the closest segment in the given portion of the
     *         array that occurs before the given intersection, or
     *         <code>fromIndex-1</code> if the given intersection occurs before
     *         all of the segments in that part of the array.
     * @throws IllegalArgumentException if <code>fromIndex &gt; toIndex</code>.
     */
    private int closestSegmentBefore(Intersection inter, int fromIndex,
            int toIndex) {
        if (fromIndex > toIndex || fromIndex < 0
                || toIndex > this.segments.length)
            throw new IllegalArgumentException("Invalid segment indices");

        /*
         * We want to find the index i (fromIndex-1 <= i <= toIndex-1) such
         * that:
         * - segments[i].occursBefore(inter) == true; and,
         * - segments[i+1].occursBefore(inter) == false.
         * To handle the border cases, assume:
         * - segments[fromIndex-1].occursBefore(inter) == true; and,
         * - segments[toIndex].occursBefore(inter) == false.
         */
        int low = fromIndex - 1;
        int high = toIndex - 1;
        SegmentationPoint sp = inter.getSegmentationPoint();

        while (true) {
            int i = low + ((high - low) / 2);

            /* Compute array[i].occursBefore(inter) */
            boolean occursBefore = true;
            if (i >= fromIndex)
                occursBefore = this.segments[i].occursBefore(sp);
            if (occursBefore == false) {
                high = i - 1;
                continue;
            }

            /* Compute array[i+1].occursBefore(inter) */
            occursBefore = false;
            if (i + 1 < toIndex)
                occursBefore = this.segments[i + 1].occursBefore(sp);
            if (occursBefore == true) {
                low = i + 1;
                continue;
            }

            return i;
        }
    }

    /**
     * The <code>ArrayIterator</code> class is used for iterating through the
     * elements in a single-dimensional array.
     */
    private class ArrayIterator<T> implements Iterator<T> {
        /**
         * The array through which this iterator moves.
         */
        private T[] array;

        /**
         * The next index in the array to be returned.
         */
        private int current;

        /**
         * The last index in the array to be returned.
         */
        private int last;

        /**
         * Creates a new <code>ArrayIterator</code> that functions on the given
         * array.
         *
         * @param array the array through which to iterate.
         */
        public ArrayIterator(T[] array) {
            this.array = array;
            this.current = 0;
            this.last = array.length - 1;
        }

        /**
         * Creates a new <code>ArrayIterator</code> that functions over the
         * given portion of the given array. If the given indices cross, the
         * iterator will have no elements.
         *
         * @param array the array through which to iterate.
         * @param first the first element to return.
         * @param last the last element to return.
         * @throws IllegalArgumentException if the indices do not cross and the
         *         iteration would run afoul of the array bounds.
         */
        public ArrayIterator(T[] array, int first, int last) {
            if (first < last && (first < 0 || last >= array.length))
                throw new IllegalArgumentException("Iterator out of bounds");
            this.array = array;
            this.current = first;
            this.last = last;
        }

        /**
         * Returns whether the iteration has more elements.
         *
         * @return <code>true</code> if the iteration has more elements,
         *         otherwise <code>false</code>.
         */
        public boolean hasNext() {
            return this.current <= this.last;
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration.
         * @throws NoSuchElementException if the iteration has no more
         *         elements.
         */
        public T next() {
            if (this.current > this.last)
                throw new NoSuchElementException("No more elements in array");
            return this.array[this.current++];
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

    /**
     * The <code>UniqueIntersectionIterator</code> class is responsible for
     * iterating through all of the unique (that is, non-mirrored)
     * intersections on a map.
     */
    private class UniqueIntersectionIterator
            implements Iterator<Intersection> {
        /**
         * The street for which we will return the next intersection.
         */
        private int onStreet;

        /**
         * The index of the next intersection to be returned.
         */
        private int onIntersection;

        /**
         * Creates a new <code>UniqueIntersectionIterator</code>.
         */
        public UniqueIntersectionIterator() {
            this.onStreet = 0;
            this.onIntersection = -1;
            this.advance();
        }

        /**
         * Returns whether the iteration has more elements.
         *
         * @return <code>true</code> if the iteration has more elements,
         *         otherwise <code>false</code>.
         */
        public boolean hasNext() {
            return this.onStreet < streets.length;
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration.
         * @throws NoSuchElementException if the iteration has no more
         *         elements.
         */
        public Intersection next() {
            if (this.onStreet == streets.length)
                throw new NoSuchElementException(
                        "No more unique intersections");
            Intersection i = intersections[this.onStreet][this.onIntersection];
            this.advance();
            return i;
        }

        /**
         * Throws an <code>UnsupportedOperationException</code>.
         *
         * @throws UnsupportedOperationException always.
         */
        public void remove() {
            throw new UnsupportedOperationException("Cannot remove elements");
        }

        /**
         * Advances the iterator the the next unique intersection.
         */
        private void advance() {
            while (this.onStreet < streets.length) {
                this.onIntersection++;
                if (intersections[this.onStreet].length == this.onIntersection) {
                    this.onIntersection = -1;
                    this.onStreet++;
                    continue;
                }
                Intersection inter =
                        intersections[this.onStreet][this.onIntersection];
                Street mirror = inter.getMirror().getStreet();
                if (mirror.getID() > this.onStreet)
                    break;
            }
        }
    }

    /**
     * The <code>ExcludeIterator</code> class is an iteration over all the
     * {@link Intersection}s on a {@link Street}, excluding a given
     * intersecting street from the iteration.
     */
    private class ExcludeIterator implements Iterator<Intersection> {
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
         * The street to exclude from the iteration.
         */
        private Street exclude;

        /**
         * Creates a new <code>ExcludeIterator</code> that returns
         * intersections belonging to the given street, excluding the given
         * street.
         *
         * @param current the street for which to return intersections.
         * @param exclude the cross street to exclude from the iteration, or
         *        <code>null</code> if no street should be excluded.
         */
        public ExcludeIterator(Street current, Street exclude) {
            this.all = getIntersections(current);
            this.exclude = exclude;
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
            while (this.all.hasNext()) {
                Intersection nextAll = this.all.next();
                Street cross = nextAll.getMirror().getStreet();
                if (this.exclude == null
                        || cross.equals(this.exclude) == false) {
                    this.next = nextAll;
                    return;
                }
            }
            this.next = null;
        }
    }
}
