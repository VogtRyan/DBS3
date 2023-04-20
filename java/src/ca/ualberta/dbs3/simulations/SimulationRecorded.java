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

/**
 * The <code>SimulationRecorded</code> class represents a single
 * {@link SimulationDiscrete} that can be replayed. State information about
 * each agent can also be attached to the simulation in the form of a
 * {@link StateRecord}.
 */
public class SimulationRecorded {
    /**
     * The underlying simulation that has been recorded.
     */
    private SimulationDiscrete simulation;

    /**
     * The duration in milliseconds of the simulation.
     */
    private long durationMilli;

    /**
     * The current time elapsed in milliseconds in the playback of the recorded
     * simulation.
     */
    private long currentTime;

    /**
     * The state transition record of all of the agents in the simulation,
     * which keeps track of their state at all points of the recorded
     * simulation.
     */
    private StateRecord stateRecord;

    /**
     * The position interpolaters for each agent.
     */
    private AgentInterpolater[] interpolaters;

    /**
     * A deep copy of the agents at the start of the simulation, to allow for
     * fast rewinding to the beginning with no re-initialization.
     */
    private Agent[] rewind;

    /**
     * Creates a new <code>SimulationRecorded</code> wrapper around the given
     * discrete simulation.
     *
     * @param simulation the discrete simulation from which to construct the
     *        recorded simulation.
     * @param pm the progress monitor to receive updates on the construction of
     *        this simulation, which may be <code>null</code>. Note that if the
     *        cancel flag is raised in the progress monitor, the simulation
     *        will be left in an inconsistent state with almost-certainly
     *        erroneous behaviour.
     * @throws IllegalArgumentException if any time has advanced in the given
     *         discrete simulation.
     */
    public SimulationRecorded(SimulationDiscrete simulation,
            ProgressMonitor pm) {
        this(simulation,
                StateRecordFactory.getDefaultRecord(simulation.getNumAgents()),
                pm);
    }

    /**
     * Creates a new <code>SimulationRecorded</code> wrapper around the given
     * discrete simulation with the given state record.
     *
     * @param simulation the discrete simulation from which to construct the
     *        recorded simulation.
     * @param record the state record for the recorded simulation.
     * @param pm the progress monitor to receive updates on the construction of
     *        this simulation, which may be <code>null</code>. Note that if the
     *        cancel flag is raised in the progress monitor, the simulation
     *        will be left in an inconsistent state with almost-certainly
     *        erroneous behaviour.
     * @throws IllegalArgumentException if any time has advanced in the given
     *         discrete simulation, or if the number of agents in the state
     *         record does not match the number of agents in the simulation.
     */
    public SimulationRecorded(SimulationDiscrete simulation,
            StateRecord record, ProgressMonitor pm) {
        int numAgents = simulation.getNumAgents();
        if (numAgents != record.getNumAgents())
            throw new IllegalArgumentException("Agent count mismatch");
        for (int i = 0; i < numAgents; i++) {
            if (simulation.getCurrentTimeSeconds(i) != 0.0)
                throw new IllegalArgumentException("Non-fresh simulation");
        }

        this.simulation = simulation;

        this.durationMilli = simulation.getDuration().toLong();
        this.currentTime = 0L;
        this.stateRecord = record;
        this.interpolaters = new AgentInterpolater[numAgents];
        this.rewind = new Agent[numAgents];

        if (pm != null) {
            if (pm.shouldCancel())
                return;
            pm.start("Preparing playback");
        }

        for (int i = 0; i < numAgents; i++) {
            if (pm != null) {
                if (pm.shouldCancel())
                    return;
                pm.update(i, numAgents);
            }
            this.rewind[i] = new Agent(simulation.getAgent(i));
            this.interpolaters[i] = new AgentInterpolater(i);
        }

        if (pm != null && pm.shouldCancel() == false)
            pm.end();
    }

    /**
     * Returns the number of agents in the recorded simulation.
     *
     * @return the number of agents in the simulation.
     */
    public int getNumAgents() {
        return this.simulation.getNumAgents();
    }

    /**
     * Returns the duration of the recorded simulation in milliseconds.
     *
     * @return the duration in milliseconds.
     */
    public long getDuration() {
        return this.durationMilli;
    }

    /**
     * Returns the state record used by the recorded simulation to determine
     * agent state at each point in time.
     *
     * @return the state record.
     */
    public StateRecord getStateRecord() {
        return this.stateRecord;
    }

    /**
     * Sets the time elapsed in the simulation to the given number of
     * milliseconds, fast forwarding or rewinding as necessary.
     *
     * @param milliseconds the number of milliseconds since the start of the
     *        simulation.
     * @throws IllegalArgumentException if the number of milliseconds is
     *         negative or if it exceeds the duration of the simulation.
     */
    public void setTime(long milliseconds) {
        if (milliseconds < 0 || milliseconds > this.durationMilli)
            throw new IllegalArgumentException("Invalid time request");

        /*
         * The AgentInterpolater is responsible for actually doing the
         * computations, and those computations are performed on-request. This
         * function can get called many times, but the computations are only
         * performed as necessary.
         */
        this.currentTime = milliseconds;
    }

    /**
     * Returns the location of the given agent, in metres, at the current time.
     *
     * @param agent the agent for which to return the current location.
     * @return the agent's current location.
     */
    public Point getLocation(int agent) {
        this.interpolaters[agent].setTime(this.currentTime);
        return this.interpolaters[agent].getLocation();
    }

    /**
     * Returns the state of the given agent at the current time.
     *
     * @param agent the agent for which to return the current state.
     * @return the agent's current state.
     */
    public State getState(int agent) {
        return this.stateRecord.getStateAt(this.currentTime, agent);
    }

    /**
     * Returns the destination of the given agent, in metres, at the current
     * time.
     *
     * @param agent the agent for which to return the current destination.
     * @return the agent's current destination.
     */
    public Point getDestination(int agent) {
        this.interpolaters[agent].setTime(this.currentTime);
        return this.interpolaters[agent].getDestination();
    }

    /**
     * The <code>AgentInterpolater</code> class is used to interpolate the
     * position of an agent at any point in time. This class assumes that no
     * other advances or resets on any agent are done, other than by this
     * class.
     */
    private class AgentInterpolater {
        /**
         * The agent for which this interpolater functions.
         */
        private int agent;

        /**
         * A time for which we received a definitive location for this agent
         * from the underlying simulation.
         */
        private long indexTime;

        /**
         * Another time for which we received a definitive location for this
         * agent from the underlying simulation, which is guaranteed to be less
         * than or equal to <code>indexTime</code>.
         */
        private long prevIndexTime;

        /**
         * The current time of the interpolator, guaranteed to be greater than
         * or equal to <code>prevIndexTime</code> and less than or equal to
         * <code>indexTime</code>.
         */
        private long milliseconds;

        /**
         * The position of the agent at <code>indexTime</code>.
         */
        private Point indexPosition;

        /**
         * The position of the agent at <code>prevIndexTime</code>.
         */
        private Point prevIndexPosition;

        /**
         * The ultimate destination of the agent when it is at
         * <code>indexTime</code>.
         */
        private Point indexDest;

        /**
         * The ultimate destination of the agent when it is at
         * <code>prevIndexTime</code>.
         */
        private Point prevIndexDest;

        /**
         * The vector from the previous index position to the current.
         */
        private Vector delta;

        /**
         * Creates a new <code>AgentInterpolater</code> capable of
         * interpolating positions for the given agent.
         *
         * @param agent the agent for which this interpolater functions.
         */
        public AgentInterpolater(int agent) {
            this.agent = agent;
            this.milliseconds = 0L;
            this.computeZeroIndices(true);
        }

        /**
         * Sets the current time for the interpolator.
         *
         * @param milliseconds the time in milliseconds.
         */
        public void setTime(long milliseconds) {
            this.milliseconds = milliseconds;
            if (milliseconds < this.prevIndexTime
                    || milliseconds > this.indexTime)
                this.updateIndices(milliseconds);
        }

        /**
         * Returns the current location of the agent.
         *
         * @return the location in metres of the agent.
         */
        public Point getLocation() {
            /* Interpolate, if necessary, to determine the current location */
            if (this.milliseconds == this.prevIndexTime)
                return this.prevIndexPosition;
            else if (this.milliseconds == this.indexTime)
                return this.indexPosition;
            else {
                /*
                 * Guaranteed that indexTime != prevIndexTime, since
                 * milliseconds is equal to neither of them, and
                 * prevIndexTime <= milliseconds <= indexTime.
                 */
                double frac = ((double) (this.milliseconds
                        - this.prevIndexTime))
                        / ((double) (this.indexTime - this.prevIndexTime));
                return this.prevIndexPosition.add(this.delta, frac);
            }
        }

        /**
         * Returns the current destination of the agent.
         *
         * @return the destination in metres of the agent.
         */
        public Point getDestination() {
            if (this.milliseconds == this.indexTime)
                return this.indexDest;
            else
                return this.prevIndexDest;
        }

        /**
         * Sets the index times and positions such that
         * <code>prevIndexTime &lt;= milliseconds &lt;= indexTime</code>.
         *
         * @param milliseconds the time about which to focus the index times.
         */
        private void updateIndices(long milliseconds) {
            if (milliseconds < this.prevIndexTime) {
                simulation.setAgent(this.agent, new Agent(rewind[this.agent]));
                this.computeZeroIndices(false);
            }

            while (milliseconds > this.indexTime) {
                this.prevIndexTime = this.indexTime;
                this.prevIndexPosition = this.indexPosition;
                this.prevIndexDest = this.indexDest;
                simulation.advance(this.agent);
                this.indexTime =
                        simulation.getCurrentTime(this.agent).toLong();
                this.indexPosition =
                        simulation.getCurrentLocationMetres(this.agent);
                this.indexDest = simulation.getDestinationMetres(this.agent);
            }

            this.delta =
                    new Vector(this.prevIndexPosition, this.indexPosition);
        }

        /**
         * Provided that the agent is currently at time zero, compute the first
         * index times and locations.
         *
         * @param computeVector whether to compute the delta vector between the
         *        indices.
         */
        private void computeZeroIndices(boolean computeVector) {
            this.prevIndexTime = 0L;
            this.prevIndexPosition =
                    simulation.getCurrentLocationMetres(agent);
            this.prevIndexDest = simulation.getDestinationMetres(agent);

            simulation.advance(this.agent);
            this.indexTime = simulation.getCurrentTime(this.agent).toLong();
            this.indexPosition =
                    simulation.getCurrentLocationMetres(this.agent);
            this.indexDest = simulation.getDestinationMetres(this.agent);

            if (computeVector)
                this.delta =
                        new Vector(this.prevIndexPosition, this.indexPosition);
        }
    }
}
