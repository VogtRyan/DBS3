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
 * The <code>SyntaxComputer</code> class is responsible for computing the
 * standard closeness and betweenness scores for all the segments on a map.
 */
public class SyntaxComputer {
    /**
     * The radius to use for closeness computations.
     */
    private int radius;

    /**
     * Creates a new <code>SyntaxComputer</code> that uses the maximal radius
     * for closeness computations.
     */
    public SyntaxComputer() {
        this(Integer.MAX_VALUE);
    }

    /**
     * Creates a new <code>SyntaxComputer</code> that uses the given radius for
     * closeness computations.
     *
     * @param radius the radius for closeness computations, in turns.
     * @throws IllegalArgumentException if <code>radius</code> is negative.
     */
    public SyntaxComputer(int radius) {
        if (radius < 0)
            throw new IllegalArgumentException("Invalid radius");
        this.radius = radius;
    }

    /**
     * Computes the closeness and betweenness scores for each segment on the
     * given map, using the given geodesic-finding algorithm, and fills the
     * result arrays.
     *
     * @param map the map on which to perform the computation.
     * @param geoFinder the geodesic finding algorithm to use.
     * @param closeness the closeness result array, which must have a length
     *        equal to the number of segments on the map.
     * @param betweenness the betweenness result array, which must have a
     *        length equal to the number of segments on the map.
     * @throws IllegalArgumentException if either of the arrays do not have the
     *         proper length.
     */
    public void compute(Map map, GeodesicFinder geoFinder, double[] closeness,
            double[] betweenness) {
        this.compute(map, geoFinder, closeness, betweenness, null);
    }

    /**
     * Computes the closeness and betweenness scores for each segment on the
     * given map, using the given geodesic-finding algorithm, and fills the
     * result arrays.
     *
     * @param map the map on which to perform the computation.
     * @param geoFinder the geodesic finding algorithm to use.
     * @param closeness the closeness result array, which must have a length
     *        equal to the number of segments on the map.
     * @param betweenness the betweenness result array, which must have a
     *        length equal to the number of segments on the map.
     * @param pm the progress monitor to inform of the computation progress and
     *        watch for a cancel operation flag, or <code>null</code>. If the
     *        cancel flag is raised, the results in the result arrays will be
     *        undefined.
     * @throws IllegalArgumentException if either of the arrays do not have the
     *         proper length.
     */
    public void compute(Map map, GeodesicFinder geoFinder, double[] closeness,
            double[] betweenness, ProgressMonitor pm) {
        int numSegs = map.numSegments();
        if (closeness.length != numSegs || betweenness.length != numSegs)
            throw new IllegalArgumentException("Invalid result array length");

        int done = 0;
        int toDo = (numSegs * numSegs - numSegs) / 2;
        if (pm != null) {
            if (pm.shouldCancel())
                return;
            pm.start("Computing " + geoFinder.getDescription() + " geodesics");
        }

        for (int i = 0; i < numSegs; i++)
            closeness[i] = betweenness[i] = 0.0;

        for (int onStart = 0; onStart < numSegs; onStart++) {
            Segment start = map.getSegment(onStart);
            for (int onEnd = onStart + 1; onEnd < numSegs; onEnd++) {

                if (pm != null) {
                    if (pm.shouldCancel())
                        return;
                    pm.update(done, toDo);
                }
                done++;

                Segment end = map.getSegment(onEnd);
                GeodesicSet geoSet = geoFinder.getGeodesics(start, end);

                /*
                 * Add the closeness measure only if the segments fall within
                 * the given radius of each other.
                 */
                if (map.minTurns(start.getStreet(),
                        end.getStreet()) <= this.radius) {
                    closeness[onStart] += geoSet.getCost();
                    closeness[onEnd] += geoSet.getCost();
                }

                /* Add betweenness for all segments */
                for (int onBetween = 0; onBetween < numSegs; onBetween++) {
                    Segment between = map.getSegment(onBetween);
                    betweenness[onBetween] +=
                            geoSet.proportionContains(between);
                }
            }
        }

        /* Multiplicative inverse finishes the closeness computation */
        for (int i = 0; i < numSegs; i++)
            closeness[i] = 1.0 / closeness[i];

        if (pm != null && pm.shouldCancel() == false)
            pm.end();
    }
}
