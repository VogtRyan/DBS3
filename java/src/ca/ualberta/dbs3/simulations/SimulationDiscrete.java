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

package ca.ualberta.dbs3.simulations;

import ca.ualberta.dbs3.math.*;
import ca.ualberta.dbs3.network.UnsignedInteger;

/**
 * The <code>SimulationDiscrete</code> class is a wrapper around a
 * {@link Simulation} that rounds all times to the nearest millisecond and all
 * locations to the nearest millimetre. It also guarantees that when an agent
 * in the simulation is advanced, it advances time by at least one millisecond
 * (by advancing the underlying simulation multiple times, if needed).
 */
public class SimulationDiscrete {
    /**
     * The simulation running underneath the discretizing layer.
     */
    private Simulation simulation;

    /**
     * The duration of the discrete simulation in milliseconds.
     */
    private UnsignedInteger durationMilli;

    /**
     * Creates a new <code>SimulationDiscrete</code> wrapper around the given
     * simulation.
     *
     * @param simulation the simulation around which to place the wrapper.
     * @throws IllegalArgumentException if the duration of the simulation in
     *         milliseconds exceeds the bounds of an unsigned 32-bit integer.
     */
    public SimulationDiscrete(Simulation simulation) {
        this.simulation = simulation;
        this.durationMilli =
                new UnsignedInteger(simulation.getDuration() * 1000);
    }

    /**
     * Creates a new <code>SimulationDiscrete</code> that is a deep copy of the
     * given simulation.
     *
     * @param simulation the simulation to copy.
     */
    public SimulationDiscrete(SimulationDiscrete simulation) {
        this.simulation = new Simulation(simulation.simulation);
        this.durationMilli = simulation.durationMilli;
    }

    /**
     * Returns the number of agents in the simulation.
     *
     * @return the number of agents in the simulation.
     */
    public int getNumAgents() {
        return this.simulation.getNumAgents();
    }

    /**
     * Returns the duration of the simulation in milliseconds.
     *
     * @return the duration of the simulation in milliseconds.
     */
    public UnsignedInteger getDuration() {
        return this.durationMilli;
    }

    /**
     * Returns the current amount of time for which a given agent has moved in
     * the simulation, rounded to the nearest millisecond.
     *
     * @param agent the agent in question.
     * @return the current amount of time for which that agent has moved.
     */
    public UnsignedInteger getCurrentTime(int agent) {
        double timeMoved = this.simulation.getCurrentTime(agent);
        return new UnsignedInteger(timeMoved * 1000.0);
    }

    /**
     * Returns the current location of the given agent, rounded to the nearest
     * millimetre, as a pair of coordinates (the x at index <code>0</code> and
     * the y at index <code>1</code>).
     *
     * @param agent the index of the agent in question.
     * @return the agent's location represented as a pair of coordinates.
     */
    public UnsignedInteger[] getCurrentLocation(int agent) {
        UnsignedInteger[] ret = new UnsignedInteger[2];
        Point p = this.simulation.getCurrentLocation(agent);
        ret[0] = new UnsignedInteger(p.getX() * 1000);
        ret[1] = new UnsignedInteger(p.getY() * 1000);
        return ret;
    }

    /**
     * Returns the current amount of time for which a given agent has moved in
     * the simulation, in seconds.
     *
     * @param agent the agent in question.
     * @return the current amount of time for which the agent has moved.
     */
    public double getCurrentTimeSeconds(int agent) {
        return this.simulation.getCurrentTime(agent);
    }

    /**
     * Returns the current location of the given agent, expressed in metres.
     *
     * @param agent the index of the agent in question.
     * @return the agent's location in metres.
     */
    public Point getCurrentLocationMetres(int agent) {
        return this.simulation.getCurrentLocation(agent);
    }

    /**
     * Returns the ultimate destination to which the agent is currently
     * travelling, expressed in metres.
     *
     * @param agent the index of the agent in question.
     * @return the ultimate destination of the agent.
     */
    public Point getDestinationMetres(int agent) {
        return this.simulation.getDestination(agent);
    }

    /**
     * Advances the given agent's movement to the next point at which it
     * changes direction or to the end of the simulation, such that the amount
     * of time for which that agent has moved (rounded to the nearest
     * millisecond) increases by at least one.
     *
     * @param agent the agent in question.
     * @return <code>true</code> if the agent's movement was successfully
     *         advanced, or <code>false</code> if the simulation has ended for
     *         that agent.
     */
    public boolean advance(int agent) {
        UnsignedInteger currentTime = this.getCurrentTime(agent);
        UnsignedInteger newTime = null;

        if (currentTime.compareTo(this.durationMilli) >= 0)
            return false;

        while (true) {
            /* Advance the underlying simulation */
            if (this.simulation.advance(agent) == false)
                throw new IllegalStateException("Cannot advance simulation");

            /*
             * If we have not advanced ahead at least a millisecond, assume
             * that nothing interesting has happened, and continue advancing
             * the underlying simulation.
             */
            newTime = this.getCurrentTime(agent);
            if (currentTime.equals(newTime) == false)
                break;
        }

        return true;
    }

    /**
     * Sets the agent at the given identification index to the given agent.
     *
     * @param id the identification index of the agent.
     * @param agent a reference to the new agent.
     * @throws ArrayIndexOutOfBoundsException if <code>id</code> is negative or
     *         greater than or equal to the number of agents.
     */
    public void setAgent(int id, Agent agent) {
        this.simulation.setAgent(id, agent);
    }

    /**
     * Returns a reference to the agent at the given identification index.
     *
     * @param id the identification index of the agent.
     * @return a reference to the agent.
     * @throws ArrayIndexOutOfBoundsException if <code>id</code> is negative or
     *         greater than or equal to the number of agents.
     */
    public Agent getAgent(int id) {
        return this.simulation.getAgent(id);
    }
}
