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
 * The <code>DestinationChooserUniform</code> class is a destination chooser
 * for which every possible destination has the same possibility of being
 * chosen, regardless of an agent's current location. In other words, if a
 * {@link Street} <code>S1</code> is twice as long as another
 * <code>Street</code> <code>S2</code>, then a location on <code>S1</code> is
 * twice as likely to be chosen as a destination as a location on
 * <code>S2</code> is.
 */
public class DestinationChooserUniform extends DestinationChooser {
    /**
     * The map on which this destination chooser functions.
     */
    private Map map;

    /**
     * A distribution over all of the streets, based on their lengths.
     */
    private Distribution streets;

    /**
     * Creates a new <code>DestinationChooserUniform</code> that operates on
     * the given map.
     *
     * @param map the map on which this destination chooser will work.
     */
    public DestinationChooserUniform(Map map) {
        this(map, null);
    }

    /**
     * Creates a new <code>DestinationChooserUniform</code> that operates on
     * the given map.
     *
     * @param map the map on which this destination chooser will work.
     * @param pm the progress monitor to receive updates on the construction of
     *        this destination chooser, which may be <code>null</code>. Note
     *        that if the cancel flag is raised in the progress monitor, the
     *        destination chooser will be left in an inconsistent state with
     *        almost-certainly erroneous behaviour.
     */
    public DestinationChooserUniform(Map map, ProgressMonitor pm) {
        /*
         * We use an indeterminate here for consistent behaviour across all
         * different destination choosers.
         */
        if (pm != null) {
            if (pm.shouldCancel())
                return;
            pm.start(DestinationChooser.INITIALIZATION_DESCRIPTION);
            pm.updateIndeterminate();
        }

        this.map = map;
        double[] streets = new double[map.numStreets()];
        for (int i = 0; i < streets.length; i++)
            streets[i] = map.getStreet(i).getLength();
        this.streets = new Distribution(streets);

        if (pm != null && pm.shouldCancel() == false)
            pm.end();
    }

    /**
     * Returns a new destination to which an agent should travel.
     *
     * @param current the current location of the agent, which is ignored by a
     *        <code>DestinationChooserUniform</code>.
     * @param prng the pseudorandom number generator to use.
     * @return the next destination for that agent.
     */
    public Waypoint getDestination(Waypoint current, Random prng) {
        Street str = this.map.getStreet(this.streets.getIndex(prng));
        Point pnt = str.getRandomPoint(prng);
        return new Waypoint(pnt, str);
    }

    /**
     * Returns a randomly generated previous destination, where an agent most
     * recently arrived, assuming that the agent is in a steady state.
     *
     * @param prng the pseudorandom number generator to use.
     * @return a random destination at which an agent most recently arrived,
     *         assuming the agent is moving in steady state.
     */
    public Waypoint getSteadyDestination(Random prng) {
        return this.getDestination(null, prng);
    }
}
