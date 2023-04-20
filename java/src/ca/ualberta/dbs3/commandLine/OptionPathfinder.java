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

package ca.ualberta.dbs3.commandLine;

import ca.ualberta.dbs3.cartography.*;

/**
 * A <code>OptionPathfinder</code> represents a command line option for
 * choosing which {@link Pathfinder} to use.
 */
public class OptionPathfinder extends Option {
    /**
     * The default pathfinder to use, as an index into {@link #getPathfinders}.
     */
    public static final int DEFAULT_PATHFINDER = 2;

    /**
     * The maximal index into the pathfinder list.
     */
    public static final int MAX_PATHFINDER = 2;

    /**
     * Descriptions of the available pathfinders.
     */
    private static final String[] PATHFINDERS = {"Minimize angle change",
            "Minimize Euclidean distance", "Minimize turns"};

    /**
     * The choice to use the minimum angle change pathfinder.
     */
    private Choice minAngle;

    /**
     * The choice to use the minimum Euclidean distance pathfinder.
     */
    private Choice minDist;

    /**
     * The choice to use the minimum number of turns pathfinder.
     */
    private Choice minTurns;

    /**
     * Creates a new <code>OptionPathfinder</code> to be added to a parser.
     */
    public OptionPathfinder() {
        super("Pathfinder");
        this.minAngle = new Choice("minAngle");
        this.minDist = new Choice("minDist");
        this.minTurns = new Choice("minTurns");
        this.add(this.minAngle);
        this.add(this.minDist);
        this.addDefault(this.minTurns);
    }

    /**
     * Returns the pathfinder currently specified by this option.
     *
     * @param map the map on which the pathfinder will function.
     * @return the pathfinder specified by the user, or the default.
     */
    public Pathfinder getPathfinder(Map map) {
        if (this.minAngle.isActive())
            return new PathfinderMinAngle(map);
        else if (this.minDist.isActive())
            return new PathfinderMinDistance(map);
        else
            return new PathfinderMinTurns(map);
    }

    /**
     * Returns a string with a description of the current choice for this
     * option.
     *
     * @return a description of the current choice.
     */
    public String getDescription() {
        if (this.minAngle.isActive())
            return OptionPathfinder.PATHFINDERS[0];
        else if (this.minDist.isActive())
            return OptionPathfinder.PATHFINDERS[1];
        else
            return OptionPathfinder.PATHFINDERS[2];
    }

    /**
     * Returns an array containing the names of all the pathfinders that are
     * available.
     *
     * @return an array of names of pathfinders.
     */
    public static String[] getPathfinders() {
        String[] ret = new String[OptionPathfinder.PATHFINDERS.length];
        System.arraycopy(OptionPathfinder.PATHFINDERS, 0, ret, 0, ret.length);
        return ret;
    }

    /**
     * Returns a new pathfinder for the given map, where the pathfinding
     * algorithm is determined by the given index into {@link #getPathfinders}.
     *
     * @param map the map on which to build the pathfinder.
     * @param index the index into {@link #getPathfinders}.
     * @return a pathfinder using the chosen algorithm on the given map.
     * @throws IllegalArgumentException if the given index does not reflect a
     *         valid index into the list of pathfinders.
     */
    public static Pathfinder getPathfinder(Map map, int index) {
        if (index == 0)
            return new PathfinderMinAngle(map);
        else if (index == 1)
            return new PathfinderMinDistance(map);
        else if (index == 2)
            return new PathfinderMinTurns(map);
        else
            throw new IllegalArgumentException("Invalid index");
    }
}
