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

import ca.ualberta.dbs3.cartography.*;
import ca.ualberta.dbs3.math.*;

/**
 * The <code>Agent</code> class represents a single mobile agent moving around
 * a {@link Map}.
 */
public class Agent {
    /**
     * The amount of time to burn off when initializing an agent, in seconds.
     */
    private static final double BURNOFF = 86400.0;

    /**
     * The simulation that defines the rules by which this agent moves.
     */
    private Simulation simulation;

    /**
     * The pseudorandom number generator used to randomize this agent's
     * movement.
     */
    private Random prng;

    /**
     * The speed, in metres per second, at which this agent is moving.
     */
    private double speed;

    /**
     * The current physical location of the agent.
     */
    private Waypoint current;

    /**
     * Ordered locations for the agent to visit. This path may be empty, and
     * its elements need not be unique nor need they be distinct from the
     * <code>current</code> location.
     */
    private Path targets;

    /**
     * Whether the agent is currently paused.
     */
    private boolean paused;

    /**
     * The time until the next target is reached or until the current pause
     * ends.
     */
    private double legTimeLeft;

    /**
     * The amount of time for which this agent has been simulated.
     */
    private double timeSimulated;

    /**
     * Creates a new <code>Agent</code> that will move according to the rules
     * of the given simulation.
     *
     * @param simulation the simulation rules that define this agent.
     * @param prng the pseudorandom number generator used to randomize this
     *        agent's movement.
     */
    public Agent(Simulation simulation, Random prng) {
        /* Choose a permanent speed for the agent */
        this.simulation = simulation;
        this.prng = prng;
        this.speed = simulation.getSpeedRange().getRandom(prng);

        /*
         * Start the agent at a location drawn from the distribution of
         * expected destinations of epoch n, as n goes to infinity.
         */
        DestinationChooser destChooser = simulation.getDestinationChooser();
        Pathfinder pathfinder = simulation.getPathfinder();
        Waypoint prevDest = destChooser.getSteadyDestination(prng);
        Waypoint newDest = destChooser.getDestination(prevDest, prng);
        this.targets = pathfinder.getPath(prevDest, newDest, prng);
        this.current = this.targets.peek();
        this.legTimeLeft = 0.0;
        this.paused = false;

        /*
         * Walk for an amount of time equivalent to the predefined burnoff,
         * moving the agent into some notion of steady state.
         */
        double toWalk = Agent.BURNOFF;
        while (toWalk > 0.0) {
            double walk =
                    toWalk < this.legTimeLeft ? toWalk : this.legTimeLeft;
            this.advance(walk);
            toWalk -= walk;
        }

        /* Reset the simulation time after our steady-state-walk */
        this.timeSimulated = 0.0;
    }

    /**
     * Creates a new <code>Agent</code> that is a deep copy of the given agent,
     * including the state of its psuedorandom number generator.
     *
     * @param agent the agent to copy.
     */
    public Agent(Agent agent) {
        this.simulation = agent.simulation;
        this.prng = new Random(agent.prng);
        this.speed = agent.speed;
        this.current = agent.current;
        this.targets = new Path(agent.targets);
        this.paused = agent.paused;
        this.legTimeLeft = agent.legTimeLeft;
        this.timeSimulated = agent.timeSimulated;
    }

    /**
     * Returns the current physical location of the agent.
     *
     * @return the physical location of the agent.
     */
    public Point getCurrentLocation() {
        return this.current.getLocation();
    }

    /**
     * Returns the ultimate destination to which the agent is currently
     * travelling.
     *
     * @return the ultimate destination of the agent.
     */
    public Point getDestination() {
        Waypoint wp = this.targets.peekEnd();
        if (wp == null)
            return this.current.getLocation();
        else
            return wp.getLocation();
    }

    /**
     * Returns the amount of time for which this agent has been simulated,
     * including both mobile and pause times.
     *
     * @return the amount of time this agent has been simulated.
     */
    public double getTimeSimulated() {
        return this.timeSimulated;
    }

    /**
     * Returns the time required for the agent to reach its next waypoint or
     * the end of its current pause, in seconds, which is greater than or equal
     * to zero.
     *
     * @return the time to reach the next waypoint or the end of the pause.
     */
    public double maxAdvanceTime() {
        return this.legTimeLeft;
    }

    /**
     * Simulates the agent for the given number of seconds, updating its
     * current location as necessary.
     *
     * @param seconds the number of seconds for which the agent should move.
     * @throws IllegalArgumentException if the given number of seconds is
     *         larger than the time required for the agent to reach its next
     *         waypoint or complete its current pause, or if the number of
     *         seconds is less than zero.
     */
    public void advance(double seconds) {
        if (seconds > this.legTimeLeft || seconds < 0.0)
            throw new IllegalArgumentException("Invalid advance time");
        this.timeSimulated += seconds;

        if (this.legTimeLeft == seconds)
            this.endLeg();
        else if (seconds > 0.0) {
            if (this.paused == false) {
                Point cPhysLoc = this.current.getLocation();
                Vector move = new Vector(cPhysLoc,
                        this.targets.peek().getLocation());
                move = move.normalize(seconds * this.speed);
                this.current = new Waypoint(cPhysLoc.add(move),
                        this.current.getStreet());
            }
            this.legTimeLeft -= seconds;
        }
    }

    /**
     * Ends the current leg of travel, either causing the agent to arrive at
     * the next target or causing it to begin or end a pause period.
     */
    private void endLeg() {
        /*
         * If we're paused, choose a new destination and prepare to go there.
         * If we're moving, arrive at the next target location.
         */
        if (this.paused) {
            this.paused = false;
            DestinationChooser destChooser =
                    this.simulation.getDestinationChooser();
            Pathfinder pathfinder = this.simulation.getPathfinder();
            Waypoint newDest =
                    destChooser.getDestination(this.current, this.prng);
            this.targets =
                    pathfinder.getPath(this.current, newDest, this.prng);
            this.targets.pop(); /* First element guaranteed equal to current */
        } else
            this.current = this.targets.pop();

        /*
         * Compute the time it will take to proceed to the next target, or how
         * long we will rest at the currently arrived-at target.
         */
        Waypoint next = this.targets.peek();
        if (next == null) {
            this.paused = true;
            this.legTimeLeft = this.simulation.getPauseRange().getRandom(prng);
        } else {
            Point cPhysLoc = this.current.getLocation();
            double dist = next.getLocation().distance(cPhysLoc);
            if (this.speed == 0.0 && dist > 0.0)
                this.legTimeLeft = Double.POSITIVE_INFINITY;
            else if (dist == 0.0)
                this.legTimeLeft = 0.0;
            else
                this.legTimeLeft = dist / this.speed;
        }
    }
}
