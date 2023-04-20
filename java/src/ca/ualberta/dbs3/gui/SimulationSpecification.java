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

package ca.ualberta.dbs3.gui;

import ca.ualberta.dbs3.commandLine.*;
import ca.ualberta.dbs3.math.*;

/**
 * The <code>SimulationSpecification</code> class represents instructions for
 * how the DBS3 GUI is to create a new simulation, possibly involving the use
 * of an MVISP server.
 */
public class SimulationSpecification {
    /**
     * The number of agents in the simulation.
     */
    private int numAgents;

    /**
     * The range of speeds at which agents may travel in m/s.
     */
    private Range speed;

    /**
     * The range of pause times the agents may use, in seconds.
     */
    private Range pause;

    /**
     * The duration of the simulation in the chosen units.
     */
    private double duration;

    /**
     * The index into the duration units array, specifying the unit of
     * duration.
     */
    private int durationUnit;

    /**
     * The index into the list of pathfinders, specifying the pathfinder
     * algorithm.
     */
    private int pathfinder;

    /**
     * The alpha value for the integrated destination chooser.
     */
    private double alpha;

    /**
     * The delta value for the integrated destination chooser.
     */
    private double delta;

    /**
     * The radius for the integrated destination chooser.
     */
    private int radius;

    /**
     * The random seed to use.
     */
    private long seed;

    /**
     * Whether or not to use an MVISP server.
     */
    private boolean mvisp;

    /**
     * If an MVISP server is used, on what port is it opened.
     */
    private int mvispPort;

    /**
     * Creates a new <code>SimulationSpecification</code> with the default
     * values.
     */
    public SimulationSpecification() {
        this.numAgents = OptionAgents.DEFAULT_AGENTS;
        this.speed = OptionRange.getRange(OptionSpeed.DEFAULT_MIN_SPEED,
                OptionSpeed.DEFAULT_MAX_SPEED,
                OptionSpeed.DEFAULT_DISTRIBUTION);
        this.pause = OptionRange.getRange(OptionPause.DEFAULT_MIN_PAUSE,
                OptionPause.DEFAULT_MAX_PAUSE,
                OptionPause.DEFAULT_DISTRIBUTION);
        this.duration = OptionDuration.DEFAULT_DURATION;
        this.durationUnit = OptionDuration.DEFAULT_UNIT;
        this.pathfinder = OptionPathfinder.DEFAULT_PATHFINDER;
        this.alpha = OptionDestinationChooser.DEFAULT_ALPHA;
        this.delta = OptionDestinationChooser.DEFAULT_DELTA;
        this.radius = OptionDestinationChooser.DEFAULT_RADIUS;
        this.seed = OptionSeed.DEFAULT_SEED;
        this.mvisp = false;
        this.mvispPort = OptionPort.DEFAULT_PORT;
    }

    /**
     * Creates a <code>SimulationSpecification</code> with the given values.
     *
     * @param numAgents the number of agents in the simulation.
     * @param speed the range of potential agent speeds in metres per second.
     * @param pause the range of pause times the agents may use, in seconds.
     * @param duration the duration in some unit, specified by
     *        <code>durationUnit</code>.
     * @param durationUnit the index into the list of available units of time,
     *        used to describe the duration of the simulation.
     * @param pathfinder the index into the list of pathfinding algorithms.
     * @param alpha the alpha value for the integrated destination chooser.
     * @param delta the delta value for the integrated destination chooser.
     * @param radius the radius value for the integrated destination chooser.
     * @param seed the pseudorandom number generator seed.
     * @param mvisp whether an MVISP server should be launched.
     * @param mvispPort the port on which an MVISP server would be launched, if
     *        one is launched.
     * @throws IllegalArgumentException if any of the parameters are outside
     *         their legal bounds.
     */
    public SimulationSpecification(int numAgents, Range speed, Range pause,
            double duration, int durationUnit, int pathfinder, double alpha,
            double delta, int radius, long seed, boolean mvisp,
            int mvispPort) {
        if (numAgents <= 0)
            throw new IllegalArgumentException("Invalid number of agents");
        if (speed.getMin() < 0.0)
            throw new IllegalArgumentException("Possible negative speed");
        if (pause.getMin() < 0.0)
            throw new IllegalArgumentException("Possible negative pause");
        if (OptionDuration.getDuration(duration, durationUnit) == null)
            throw new IllegalArgumentException("Duration too large");
        if (pathfinder < 0 || pathfinder > OptionPathfinder.MAX_PATHFINDER)
            throw new IllegalArgumentException("Invalid pathfinder");
        if (alpha < 0.0 || delta < 0.0 || radius < 0)
            throw new IllegalArgumentException("Invalid destination chooser");
        if (mvispPort < OptionPort.MIN_PORT || mvispPort > OptionPort.MAX_PORT)
            throw new IllegalArgumentException("Invalid port");

        this.numAgents = numAgents;
        this.speed = speed;
        this.pause = pause;
        this.duration = duration;
        this.durationUnit = durationUnit;
        this.pathfinder = pathfinder;
        this.alpha = alpha;
        this.delta = delta;
        this.radius = radius;
        this.seed = seed;
        this.mvisp = mvisp;
        this.mvispPort = mvispPort;
    }

    /**
     * Returns the number of agents in the simulation.
     *
     * @return the number of agents in the simulation.
     */
    public int getNumAgents() {
        return this.numAgents;
    }

    /**
     * Returns the range of speeds at which agents may travel in metres per
     * second.
     *
     * @return the range of speeds at which agents may travel.
     */
    public Range getSpeed() {
        return this.speed;
    }

    /**
     * Returns the range of times for which agents may pause in seconds.
     *
     * @return the range of times for which agents may pause.
     */
    public Range getPause() {
        return this.pause;
    }

    /**
     * Returns the duration of the simulation in the chosen units.
     *
     * @return the duration of the simulation in the chosen units.
     */
    public double getDuration() {
        return this.duration;
    }

    /**
     * Returns the index into the duration units array, specifying the unit of
     * duration.
     *
     * @return the duration unit index.
     */
    public int getDurationUnit() {
        return this.durationUnit;
    }

    /**
     * Returns the index into the list of pathfinders, specifying the
     * pathfinding algorithm.
     *
     * @return the pathfinder algorithm index.
     */
    public int getPathfinder() {
        return this.pathfinder;
    }

    /**
     * Returns the alpha value for the integrated destination chooser.
     *
     * @return the alpha value for the integrated destination chooser.
     */
    public double getAlpha() {
        return this.alpha;
    }

    /**
     * Returns the delta value for the integrated destination chooser.
     *
     * @return the delta value for the integrated destination chooser.
     */
    public double getDelta() {
        return this.delta;
    }

    /**
     * Returns the radius for the integrated destination chooser.
     *
     * @return the radius for the integrated destination chooser.
     */
    public int getRadius() {
        return this.radius;
    }

    /**
     * Returns the random seed to use.
     *
     * @return the random seed to use.
     */
    public long getSeed() {
        return this.seed;
    }

    /**
     * Returns whether or not to use an MVISP server.
     *
     * @return <code>true</code> if an MVISP server should be activated,
     *         <code>false</code> otherwise.
     */
    public boolean getMVISP() {
        return this.mvisp;
    }

    /**
     * Returns the port on which an MVISP server should be started.
     *
     * @return the MVISP server port to use.
     */
    public int getPort() {
        return this.mvispPort;
    }
}
