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
 * The <code>Pathfinder</code> class is responsible for choosing path that an
 * agent will take from one destination to another.
 */
public abstract class Pathfinder {
    /**
     * Whether or not performance metrics should be gathered on calls to
     * {@link #getPath}.
     */
    protected boolean gatherMetrics;

    /**
     * Creates a new <code>Pathfinder</code>.
     */
    public Pathfinder() {
        this.gatherMetrics = false;
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
    public abstract Path getPath(Waypoint start, Waypoint end, Random prng);

    /**
     * Sets whether or not metrics should be gathered on subsequent calls to
     * {@link #getPath}. Gathering metrics could significantly slow down
     * pathfinding performance, and should only be used when necessary. It is
     * disabled by default.
     *
     * @param gather whether or not to gather metrics.
     */
    public void setGatherMetrics(boolean gather) {
        this.gatherMetrics = gather;
    }

    /**
     * Prints all of the metrics gathered during calls to {@link #getPath}
     * during which {@link #setGatherMetrics} had been set to
     * <code>true</code>.
     */
    public abstract void printMetrics();
}
