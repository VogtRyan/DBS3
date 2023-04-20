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
 * The <code>DestinationChooser</code> class is responsible for choosing the
 * next destination for an agent after it reaches its current destination.
 */
public abstract class DestinationChooser {
    /**
     * A message to display while initializing a destination chooser.
     */
    protected static final String INITIALIZATION_DESCRIPTION =
            "Computing destination distribution";

    /**
     * No-operation default constructor.
     */
    protected DestinationChooser() {}

    /**
     * Returns a new destination to which an agent should travel.
     *
     * @param current the current location of the agent.
     * @param prng the pseudorandom number generator to use.
     * @return the next destination for that agent.
     */
    public abstract Waypoint getDestination(Waypoint current, Random prng);

    /**
     * Returns a randomly generated previous destination, where an agent most
     * recently arrived, assuming that the agent is in a steady state. That is,
     * the destination is chosen without any knowledge of the prior
     * destinations before that one.
     * <p>
     * For greater clarity, note that the distribution of destinations
     * associated with this function is not the same as the steady state
     * distribution of current locations of agents.
     *
     * @param prng the pseudorandom number generator to use.
     * @return a random destination at which an agent most recently arrived,
     *         assuming the agent is moving in steady state.
     */
    public abstract Waypoint getSteadyDestination(Random prng);
}
