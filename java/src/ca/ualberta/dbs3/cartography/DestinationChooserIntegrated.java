/*
 * Copyright (c) 2010-2023 Ryan Vogt <rvogt@ualberta.ca>
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
 * The <code>DestinationChooserIntegrated</code> class is a destination chooser
 * that takes into account two factors in choosing destinations:
 * <ul>
 * <li>The centrality of potential destination streets (that is, how well
 * integrated potential destinations are); and</li>
 * <li>How many turns it takes to get from the current street to the potential
 * destination (that is, distance decay).</li>
 * </ul>
 * <p>
 * The algorithm is controlled by three constants, <code>alpha &ge; 0.0</code>,
 * <code>delta &ge; 0.0</code>, and <code>radius &ge; 0</code>. The first,
 * <code>alpha</code>, is an exponent that controls how much influence
 * centrality has on destination selection, and <code>radius</code> controls
 * how wide (in turns) we consider when measuring centrality. The other
 * exponent, <code>delta &ge; 0</code>, controls how much influence distance
 * decay has on destination selection.
 * <p>
 * Formally, let <code>L(s)</code> be the length of street <code>s</code>, and
 * let <code>D(s,d)</code> be the distance between a source street
 * <code>s</code> and a potential destination street <code>d</code>. If
 * <code>T(s,d)</code> is the minimum number of turns between <code>s</code>
 * and <code>d</code>, then the distance between <code>s</code> and
 * <code>d</code> is <code>D(s,d) = T(s,d) + 1 &ge; 1</code>.
 * <p>
 * The integration value of a potential destination <code>d</code>, within a
 * radius of <code>radius</code>, is
 * <code>I(d) = Sum_{i | T(i,d) &le; radius}[L(i) * D(i,d)] /
 * Sum_{i | T(i,d) &le; radius}[L(i)]</code>. Note that
 * <code>1 &le; I(d) &le; radius + 1</code>, with larger values of
 * <code>I(d)</code> representing a destination that is more poorly integrated.
 * <p>
 * The probability of choosing a destination on street <code>d</code> as a
 * destination, given that you are starting on street <code>s</code>, is
 * proportional to
 * <code>P(s,d) = L(d) / [I(d)^{alpha} * D(s,d)^{delta}]</code>.
 * <p>
 * Note that if either <code>alpha</code> or <code>radius</code> is zero,
 * centrality has no effect on destination selection. If <code>delta</code> is
 * zero, distance decay is no effect on destination selection. If both of these
 * conditions are met, the selection algorithm degenerates into that used by a
 * {@link DestinationChooserUniform}.
 */
public class DestinationChooserIntegrated extends DestinationChooser {
    /**
     * The map on which this destination chooser functions.
     */
    private Map map;

    /**
     * A distribution over all of the streets, for each source street, based on
     * the above formula.
     */
    private Distribution[] streets;

    /**
     * The steady state distribution over all streets that each street was
     * chosen as the most recently arrived-at destination.
     */
    private Distribution steady;

    /**
     * Creates a new <code>DestinationChooserIntegrated</code> that operates on
     * the given map with the given parameters.
     *
     * @param map the map on which this destination chooser will work.
     * @param alpha the integration value exponent.
     * @param delta the distance decay exponent.
     * @param radius the radius for calculating integration values.
     * @throws IllegalArgumentException if any of the constants are negative or
     *         if the resulting destination chooser is not ergodic (per value
     *         overflow).
     */
    public DestinationChooserIntegrated(Map map, double alpha, double delta,
            int radius) {
        this(map, alpha, delta, radius, null);
    }

    /**
     * Creates a new <code>DestinationChooserIntegrated</code> that operates on
     * the given map with the given parameters.
     *
     * @param map the map on which this destination chooser will work.
     * @param alpha the integration value exponent.
     * @param delta the distance decay exponent.
     * @param radius the radius for calculating integration values.
     * @param pm the progress monitor to receive updates on the construction of
     *        this destination chooser, which may be <code>null</code>. Note
     *        that if the cancel flag is raised in the progress monitor, the
     *        destination chooser will be left in an inconsistent state with
     *        almost-certainly erroneous behaviour.
     * @throws IllegalArgumentException if any of the constants are negative or
     *         if the resulting destination chooser is not ergodic (per value
     *         overflow).
     */
    public DestinationChooserIntegrated(Map map, double alpha, double delta,
            int radius, ProgressMonitor pm) {
        if (alpha < 0.0 || delta < 0.0 || radius < 0)
            throw new IllegalArgumentException("Negative constants");
        this.map = map;
        if (pm != null) {
            if (pm.shouldCancel())
                return;
            pm.start(DestinationChooser.INITIALIZATION_DESCRIPTION);
            pm.updateIndeterminate();
        }

        /*
         * Compute the Distribution for each street S, of the probabilities
         * that each street i is the next destination when starting from S.
         */
        int numStreets = this.map.numStreets();
        double[][] probValues = new double[numStreets][numStreets];
        for (int onDst = 0; onDst < numStreets; onDst++) {
            Street dst = this.map.getStreet(onDst);
            double prob = dst.getLength()
                    / Math.pow(this.integration(dst, radius), alpha);
            for (int onSrc = 0; onSrc < numStreets; onSrc++) {
                Street src = this.map.getStreet(onSrc);
                probValues[onSrc][onDst] = prob
                        / Math.pow(this.map.minTurns(src, dst) + 1, delta);
            }
            if (pm != null && pm.shouldCancel())
                return;
        }
        this.streets = new Distribution[numStreets];
        for (int onSrc = 0; onSrc < numStreets; onSrc++)
            this.streets[onSrc] = new Distribution(probValues[onSrc]);

        /*
         * Find the steady state of the Markov model to determine the steady
         * state distribution of destination selection.
         */
        MarkovChain mc = new MarkovChain(this.streets);
        try {
            this.steady = mc.equilibrium(pm);
        } catch (UnsupportedOperationException uoe) {
            throw new IllegalStateException(
                    "Destination chooser not ergodic");
        }

        if (pm != null && pm.shouldCancel() == false)
            pm.end();
    }

    /**
     * Returns a destination to which an agent should travel.
     *
     * @param current the current location of the agent.
     * @param prng the pseudorandom number generator to use.
     * @return the next destination for that agent.
     */
    public Waypoint getDestination(Waypoint current, Random prng) {
        int currentIndex = this.map.indexOf(current.getStreet());
        int dstIndex = this.streets[currentIndex].getIndex(prng);
        Street str = this.map.getStreet(dstIndex);
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
        int dstIndex = this.steady.getIndex(prng);
        Street str = this.map.getStreet(dstIndex);
        Point pnt = str.getRandomPoint(prng);
        return new Waypoint(pnt, str);
    }

    /**
     * Computes the integration value <code>I(street)</code> for a given
     * street, over a given radius.
     *
     * @param street the street for which to compute the integration value.
     * @param radius the radius of the integration computation.
     * @return the integration value, <code>I(street)</code>.
     */
    private double integration(Street street, int radius) {
        double num = 0.0;
        double denom = 0.0;

        Iterator<Street> iter = this.map.getStreets();
        while (iter.hasNext()) {
            Street other = iter.next();
            int turns = this.map.minTurns(street, other);
            if (turns <= radius) {
                double len = other.getLength();
                num += len * (turns + 1);
                denom += len;
            }
        }

        return num / denom;
    }
}
