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

package ca.ualberta.dbs3.client;

/**
 * The <Code>AgentCommand</code> class represents a command from a UAMP or
 * MVISP server denoting that an agent is moving from a given location at a
 * given time, arriving at a destination location at a given time. Note that,
 * in contrast to the underlying UAMP and MVSIP protocols, which fuction on the
 * concept of snapshots of agents at specific times, the
 * <code>AgentCommand</code> class represents a view of an agent's state for a
 * period of time.
 */
public class AgentCommand {
    /**
     * The agent for which this command is intended.
     */
    private int agentID;

    /**
     * The starting x coordinate in metres.
     */
    private double fromX;

    /**
     * The starting y coordinate in metres.
     */
    private double fromY;

    /**
     * The starting z coordinate in metres.
     */
    private double fromZ;

    /**
     * The starting time in seconds.
     */
    private double fromTime;

    /**
     * The destination x coordinate in metres.
     */
    private double toX;

    /**
     * The destination y coordinate in metres.
     */
    private double toY;

    /**
     * The destination z coordinate in metres.
     */
    private double toZ;

    /**
     * The destination time in seconds.
     */
    private double toTime;

    /**
     * Whether the agent is present in the environment during this time period.
     * If <code>false</code>, the coordinates in this <code>AgentCommand</code>
     * have no meaning.
     */
    private boolean present;

    /**
     * Creates a new initial <code>AgentCommand</code> with identical start and
     * destination at time zero, in which the agent is present in the
     * environment.
     *
     * @param agentID the ID of the agent receiving this command.
     * @param x the x coordinate in metres.
     * @param y the y coordinate in metres.
     * @param z the z coordinate in metres.
     * @throws IllegalArgumentException if any value is negative.
     */
    public AgentCommand(int agentID, double x, double y, double z) {
        this(agentID, x, y, z, true);
    }

    /**
     * Creates a new initial <code>AgentCommand</code> with identical start and
     * destination at time zero.
     *
     * @param agentID the ID of the agent receiving this command.
     * @param x the x coordinate in metres.
     * @param y the y coordinate in metres.
     * @param z the z coordinate in metres.
     * @param present whether the agent is present in the environment at the
     *        start of the simulation. If <code>false</code>, the coordinates
     *        have no meaning (arbitrary non-negative values should be used).
     * @throws IllegalArgumentException if any value is negative.
     */
    public AgentCommand(int agentID, double x, double y, double z,
            boolean present) {
        if (agentID < 0 || this.invalid(x) || this.invalid(y)
                || this.invalid(z))
            throw new IllegalArgumentException("Invalid value in command");
        this.agentID = agentID;
        this.fromTime = this.toTime = 0.0;
        this.fromX = this.toX = x;
        this.fromY = this.toY = y;
        this.fromZ = this.toZ = z;
        this.present = present;
    }

    /**
     * Creates a new <code>AgentCommand</code> in which the agent is present in
     * the environment and that proceeds the given <code>AgentCommand</code>.
     *
     * @param previous the previous agent command.
     * @param x the destination x coordinate in metres.
     * @param y the destination y coordinate in metres.
     * @param z the destination z coordinate in metres.
     * @param time the destination time in metres.
     * @throws IllegalArgumentException if the given time is not greater than
     *         the destination time of the previous command, or if any given
     *         parameter is negative.
     */
    public AgentCommand(AgentCommand previous, double x, double y, double z,
            double time) {
        this(previous, x, y, z, time, true);
    }

    /**
     * Creates a new <code>AgentCommand</code> that proceeds the given
     * <code>AgentCommand</code>.
     *
     * @param previous the previous agent command.
     * @param x the destination x coordinate in metres.
     * @param y the destination y coordinate in metres.
     * @param z the destination z coordinate in metres.
     * @param time the destination time in metres.
     * @param present whether the agent is present in the environment during
     *        this time period. If <code>false</code>, the coordinates have no
     *        meaning (arbitrary non-negative values should be used).
     * @throws IllegalArgumentException if the given time is not greater than
     *         the destination time of the previous command, or if any given
     *         parameter is negative.
     */
    public AgentCommand(AgentCommand previous, double x, double y, double z,
            double time, boolean present) {
        if (this.invalid(x) || this.invalid(y) || this.invalid(z)
                || this.invalid(time))
            throw new IllegalArgumentException("Invalid value in command");
        if (previous.toTime >= time)
            throw new IllegalArgumentException("Non-increasing command time");
        this.agentID = previous.agentID;
        this.fromX = previous.toX;
        this.fromY = previous.toY;
        this.fromZ = previous.toZ;
        this.fromTime = previous.toTime;
        this.toX = x;
        this.toY = y;
        this.toZ = z;
        this.toTime = time;
        this.present = present;
    }

    /**
     * Creates a new agent command based on the given coordinates and times.
     *
     * @param agentID the ID of the agent receiving this command.
     * @param fromX the starting x coordinate in metres.
     * @param fromY the starting y coordinate in metres.
     * @param fromZ the starting z coordinate in metres.
     * @param fromTime the starting time in seconds.
     * @param toX the destination x coordinate in metres.
     * @param toY the destination y coordinate in metres.
     * @param toZ the destination z coordinate in metres.
     * @param toTime the destination time in seconds.
     * @param present whether the agent is present in the environment at the
     *        during this command. If <code>false</code>, the coordinates have
     *        no meaning (arbitrary non-negative values should be used).
     * @throws IllegalArgumentException if any value is negative, or if the
     *         time bounds cross.
     */
    public AgentCommand(int agentID, double fromX, double fromY, double fromZ,
            double fromTime, double toX, double toY, double toZ, double toTime,
            boolean present) {
        if (agentID < 0 || this.invalid(fromX) || this.invalid(fromY)
                || this.invalid(fromZ) || this.invalid(fromTime)
                || this.invalid(toX) || this.invalid(toY) || this.invalid(toZ)
                || this.invalid(toTime))
            throw new IllegalArgumentException("Invalid value in command");
        if (fromTime > toTime)
            throw new IllegalArgumentException("Time bounds cross");
        this.agentID = agentID;
        this.fromX = fromX;
        this.fromY = fromY;
        this.fromZ = fromZ;
        this.fromTime = fromTime;
        this.toX = toX;
        this.toY = toY;
        this.toZ = toZ;
        this.toTime = toTime;
        this.present = present;
    }

    /**
     * Returns the agent to which this command applies.
     *
     * @return the agent index of the agent receiving this command.
     */
    public int agentID() {
        return this.agentID;
    }

    /**
     * Returns the starting x coordinate, in metres.
     *
     * @return the starting x coordinate.
     */
    public double fromX() {
        return this.fromX;
    }

    /**
     * Returns the starting y coordinate, in metres.
     *
     * @return the starting y coordinate.
     */
    public double fromY() {
        return this.fromY;
    }

    /**
     * Returns the starting z coordinate, in metres.
     *
     * @return the starting z coordinate.
     */
    public double fromZ() {
        return this.fromZ;
    }

    /**
     * Returns the starting time, in seconds.
     *
     * @return the starting time.
     */
    public double fromTime() {
        return this.fromTime;
    }

    /**
     * Returns the destination x coordinate, in metres.
     *
     * @return the destination x coordinate.
     */
    public double toX() {
        return this.toX;
    }

    /**
     * Returns the destination y coordinate, in metres.
     *
     * @return the destination y coordinate.
     */
    public double toY() {
        return this.toY;
    }

    /**
     * Returns the destination z coordinate, in metres.
     *
     * @return the destination z coordinate.
     */
    public double toZ() {
        return this.toZ;
    }

    /**
     * Returns the destination time, in seconds.
     *
     * @return the destination time.
     */
    public double toTime() {
        return this.toTime;
    }

    /**
     * Returns whether or not the agent is present in the environment during
     * the time period specified by this command. If <code>false</code>, the
     * starting and destination coordinates should not be interpreted to have
     * any meaning and can be ignored.
     *
     * @return <code>true</code> if the agent is present in the environment,
     *         otherwise <code>false</code>.
     */
    public boolean present() {
        return this.present;
    }

    /**
     * Validates the given double value, ensuring that it is neither negative
     * nor non-finite.
     *
     * @param value the value to validate.
     * @return <code>true</code> if the value is invalid, otherwise
     *         <code>false</code>.
     */
    private boolean invalid(double value) {
        return (value < 0.0 || Double.isNaN(value)
                || Double.isInfinite(value));
    }
}
