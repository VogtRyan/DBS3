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

import ca.ualberta.dbs3.network.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.LinkedList;

/**
 * The <code>Client</code> class represents a UAMP or MVISP client connection
 * to a server.
 */
public abstract class Client {
    /**
     * The version of the UAMP/MVISP protocol supported by this library.
     */
    private static final byte SUPPORTED_VERSION = (byte) (0x80);

    /**
     * A constant value to improve code readability.
     */
    private static final UnsignedInteger ZERO = new UnsignedInteger(0);

    /**
     * The open socket to the server.
     */
    private Socket socket;

    /**
     * An input stream that can be used to read data sent by the server.
     */
    protected DataInputStream dis;

    /**
     * An output stream that can be used to write data to the server.
     */
    protected DataOutputStream dos;

    /**
     * The optional features supported by the client application.
     */
    private ClientFeatures clientFeatures;

    /**
     * The optional features supported by the server.
     */
    private ClientFeatures serverFeatures;

    /**
     * The buffers containing the mobility updates for each agent.
     */
    private UAMPAgent[] agents;

    /**
     * The duration of the simulation in milliseconds.
     */
    private long duration;

    /**
     * The smallest time of any agent's current UAMP update.
     */
    private long smallestCurrentTime;

    /**
     * The largest time of any agent's previous UAMP update.
     */
    private long largestLastTime;

    /**
     * Creates a new <code>Client</code> that will connect to the UAMP or MVISP
     * server at the given location.
     *
     * @param hostname the host on which the server resides.
     * @param port the port on which the server resides.
     * @param features the set of optional features implemented by the client
     *        application.
     * @throws IOException if there is an error creating the socket connection.
     */
    public Client(String hostname, int port, ClientFeatures features)
            throws IOException {
        this.socket = new Socket(hostname, port);
        this.dis = new DataInputStream(this.socket.getInputStream());
        this.dos = new DataOutputStream(this.socket.getOutputStream());
        this.clientFeatures = features;
    }

    /**
     * Performs the initial handshake between client and server.
     *
     * @param openingBytes the opening four bytes of the handshake data,
     *        indicating the type of connection.
     * @throws IllegalArgumentException if the length of
     *         <code>openingBytes</code> is not <code>4</code>.
     * @throws IOException if there is an IO error during the handshake.
     * @throws UAMPException if there is a protocol error during the handshake.
     */
    protected void performHandshake(String openingBytes)
            throws IOException, UAMPException {
        /* Send our handshake */
        if (openingBytes.length() != 4) {
            throw new IllegalArgumentException("Invalid protocol length");
        }
        BufferWriter bw = new BufferWriter(this.dos, 9);
        bw.write(openingBytes.getBytes("US-ASCII"));
        bw.write(Client.SUPPORTED_VERSION);
        bw.write(this.clientFeatures.getBitField());

        /* Receive the server handshake and ensure protocol compatibility */
        BufferReader br = new BufferReader(this.dis, 9);
        byte[] sProto = new byte[4];
        br.read(sProto);
        verifyProtocolBytes(sProto);

        /*
         * Check that the server supports some common version of UAMP/MVISP
         * with us.
         */
        byte sVersions = br.readByte();
        if ((sVersions & Client.SUPPORTED_VERSION) == 0) {
            this.dos.writeByte(0);
            throw new UAMPException("Client and server support no shared "
                    + "protocol version");
        }

        /* Check what additional data the server will send */
        this.serverFeatures = new ClientFeatures(br.readUnsignedInt());
        if (this.serverFeatures.get3DSupport()
                && this.clientFeatures.get3DSupport() == false) {
            this.dos.writeByte(0);
            throw new UAMPException("Server sends 3D data, but client "
                    + "only supports 2D data");
        }
        if (this.serverFeatures.getAppearDisappearSupport()
                && this.clientFeatures.getAppearDisappearSupport() == false) {
            this.dos.writeByte(0);
            throw new UAMPException("Server agents appear and disappear, "
                    + "which client does not support");
        }

        /*
         * Send the VERSION_CHOICE message. Since we only support a single
         * version, the version choice message is identical to the versions
         * supported message.
         */
        this.dos.writeByte(Client.SUPPORTED_VERSION);
        byte sChoice = this.dis.readByte();
        if (sChoice == 0)
            throw new UAMPException("Server rejected handshake");
        else if (sChoice != Client.SUPPORTED_VERSION)
            throw new UAMPException("Client and server disagree on version");
    }

    /**
     * Verifies that the four protocol bytes sent by the server at the
     * beginning of the handshake match the four bytes expected by the client.
     *
     * @param serverProtocol the four bytes sent by the server.
     * @throws IllegalArgumentException if the length of
     *         <code>serverProtocol</code> is not <code>4</code>.
     * @throws UAMPException if the four bytes do not match the four bytes
     *         expected by the client.
     * @throws UnsupportedEncodingException if the known byte formats cannot be
     *         encoded.
     */
    protected abstract void verifyProtocolBytes(byte[] serverProtocol)
            throws UAMPException, UnsupportedEncodingException;

    /**
     * Reads and buffers the first stream of location information for each
     * agent.
     *
     * @param numAgents the number of agents in the simulation.
     * @param timeLimit the duration of the simulation in milliseconds.
     * @throws IOException if there is an IO error during the location read.
     * @throws UAMPException if there is a protocol error during the procedure.
     */
    protected void readInitialLocations(UnsignedInteger numAgents,
            UnsignedInteger timeLimit) throws IOException, UAMPException {
        this.agents = new UAMPAgent[(int) (numAgents.toLong())];
        for (int i = 0; i < this.agents.length; i++)
            this.agents[i] = new UAMPAgent(i);
        this.duration = timeLimit.toLong();
        this.smallestCurrentTime = this.largestLastTime = 0L;
        this.fillQueues();
    }

    /**
     * Returns the number of agents in the simulation.
     *
     * @return the number of agents in the simulation.
     */
    public int getNumAgents() {
        return this.agents.length;
    }

    /**
     * Returns the duration of the simulation in seconds.
     *
     * @return the duration of the simulation in seconds.
     */
    public double getDuration() {
        return ((double) this.duration) / 1000.0;
    }

    /**
     * Disconnects from the UAMP or MVISP server, flushing all of the
     * communication buffers and sending the command to terminate the
     * simulation.
     *
     * @throws IOException if there is an IO error during the termination
     *         phase.
     */
    public void terminate() throws IOException {
        try {
            this.flushStateChanges();
            BufferWriter bw = new BufferWriter(this.dos, 5);
            bw.write(0L);
            bw.write((byte) 0x00);
        } finally {
            this.socket.close();
        }
    }

    /**
     * Returns the current command for the given agent. The first command
     * available to each agent is its initial location command, which will have
     * <code>fromTime = toTime = 0.0</code>, and <code>fromX = toX</code> and
     * <code>fromY = toY</code>. All subsequent commands (see the
     * {@link #advance} method) will have <code>toTime &gt; fromTime</code>,
     * with the starting time and location of each command guaranteed to be the
     * ending time and location of the previous command.
     *
     * @param agentID the agent for which to return the current command.
     * @return the current command of the given agent.
     * @throws IllegalArgumentException if the given agent ID is invalid.
     */
    public AgentCommand currentCommand(int agentID) {
        if (agentID < 0 || agentID >= this.agents.length)
            throw new IllegalArgumentException("Invalid agent ID");
        return this.agents[agentID].currentCommand();
    }

    /**
     * Returns the intersected command for the given agent. Each agent has a
     * current command, which runs from time <code>fromTime</code> to time
     * <code>toTime</code>. The intersection time is defined as the time period
     * <code>[lateFrom, earlyTo]</code>, where <code>lateFrom</code> is the
     * latest <code>fromTime</code> of any agent, and <code>earlyTo</code> is
     * the earliest <code>toTime</code> of any agent. This method returns an
     * interpolated command for the given agent ID, covering the period
     * <code>[lateFrom, earlyTo]</code>. This method can be used in conjunction
     * with the {@link #advanceOldest} method to present a synchronous view of
     * all the agents' movements.
     *
     * @param agentID the agent for which to return the current command.
     * @return the intersected command for the given agent.
     * @throws IllegalArgumentException if the given agent ID is invalid.
     * @throws IllegalStateException if the {@link #advance} method has been
     *         used to create a situation in which
     *         <code>lateFrom &gt; earlyTo</code>.
     */
    public AgentCommand intersectCommand(int agentID) {
        if (agentID < 0 || agentID >= this.agents.length)
            throw new IllegalArgumentException("Invalid agent ID");
        if (this.largestLastTime > this.smallestCurrentTime)
            throw new IllegalStateException(
                    "No intersection command available");

        /*
         * currentTime[agentID] > lastTime[agentID], unless
         * currentTime[agentID] == 0 (which happens if we have never advanced
         * that agent). In this case, smallestCurrentTime == 0; and, by the
         * above assertion, largestLastTime <= smallestCurrentTime == 0, so
         * largestLastTime == 0.
         */
        UAMPUpdate current = this.agents[agentID].currentUpdate();
        if (current.getTime().toLong() == 0L)
            return this.agents[agentID].currentCommand();
        UAMPUpdate previous = this.agents[agentID].previousUpdate();

        /*
         * If we reach here, we are guaranteed that currentTime[agentID] >
         * lastTime[agentID], so we can interpolate between these times.
         */
        long deltaX = current.getX().toLong() - previous.getX().toLong();
        long deltaY = current.getY().toLong() - previous.getY().toLong();
        long deltaZ = current.getZ().toLong() - previous.getZ().toLong();
        long deltaT = current.getTime().toLong() - previous.getTime().toLong();

        /* Compute the "from" interpolation */
        double fromTime = ((double) this.largestLastTime) / 1000.0;
        double frac =
                ((double) (this.largestLastTime - previous.getTime().toLong()))
                        / deltaT;
        double fromX = (previous.getX().toLong() + frac * deltaX) / 1000.0;
        double fromY = (previous.getY().toLong() + frac * deltaY) / 1000.0;
        double fromZ = (previous.getZ().toLong() + frac * deltaZ) / 1000.0;

        /* Compute the "to" interpolation */
        double toTime = ((double) this.smallestCurrentTime) / 1000.0;
        frac = ((double) (this.smallestCurrentTime
                - previous.getTime().toLong())) / deltaT;
        double toX = (previous.getX().toLong() + frac * deltaX) / 1000.0;
        double toY = (previous.getY().toLong() + frac * deltaY) / 1000.0;
        double toZ = (previous.getZ().toLong() + frac * deltaZ) / 1000.0;

        return new AgentCommand(agentID, fromX, fromY, fromZ, fromTime, toX,
                toY, toZ, toTime, previous.getPresent());
    }

    /**
     * Returns whether there is more mobility data to request for the given
     * agent ID, or if the given agent ID has reached the end of the
     * simulation.
     *
     * @param agentID the agent for which to test if there is more data.
     * @return <code>true</code> if there is more mobility data to request, or
     *         <code>false</code> if that agent ID has reached the end of the
     *         simulation.
     * @throws IllegalArgumentException if the given agent ID is invalid.
     */
    public boolean isMore(int agentID) {
        if (agentID < 0 || agentID >= this.agents.length)
            throw new IllegalArgumentException("Invalid agent ID");
        UAMPUpdate current = this.agents[agentID].currentUpdate();
        return (current.getTime().toLong() < this.duration);
    }

    /**
     * Fetches the next command from the UAMP or MVISP server for the given
     * agent ID.
     *
     * @param agentID the agent for which to advance the simulation.
     * @return <code>true</code> if there was more mobility data to request, or
     *         <code>false</code> if that agent ID had already reached the end
     *         of the simulation.
     * @throws IllegalArgumentException if the given agent ID is invalid.
     * @throws IOException if there is an IO error communicating with the
     *         server.
     * @throws UAMPException if there is a protocol error communicating with
     *         the server.
     */
    public boolean advance(int agentID) throws IOException, UAMPException {
        if (agentID < 0 || agentID >= this.agents.length)
            throw new IllegalArgumentException("Invalid agent ID");
        return this.agents[agentID].advance();
    }

    /**
     * Returns whether there is more mobility data to request for any agent ID,
     * or if all agent IDs have reached the end of the simulation.
     *
     * @return <code>true</code> if there is more mobility data to request, or
     *         <code>false</code> if every agent ID has reached the end of the
     *         simulation.
     */
    public boolean isAnyMore() {
        return (this.smallestCurrentTime < this.duration);
    }

    /**
     * Fetches the next command for all agents with the earliest
     * <code>toTime</code>. Each agent has a current command, which runs from
     * time <code>fromTime</code> to time <code>toTime</code>. This method
     * calls {@link #advance} on the agents whose <code>toTime</code> is the
     * smallest out of all <code>toTime</code>s. This method can be used in
     * conjunction with the {@link #intersectCommand} method to present a
     * synchronous view of all the agents' movements.
     *
     * @return <code>true</code> if there was more mobility data to request, or
     *         <code>false</code> if every agent ID had already reached the end
     *         of the simulation.
     * @throws IOException if there is an IO error communicating with the
     *         server.
     * @throws UAMPException if there is a protocol error communicating with
     *         the server.
     */
    public boolean advanceOldest() throws IOException, UAMPException {
        if (this.smallestCurrentTime == this.duration)
            return false;
        long toAdvance = this.smallestCurrentTime;
        for (int i = 0; i < this.agents.length; i++) {
            if (this.agents[i].currentUpdate().getTime().toLong() == toAdvance)
                this.agents[i].advance();
        }
        return true;
    }

    /**
     * By default this method does nothing, but see
     * {@link MVISPClient#changeState}.
     *
     * @param agentID the agent ID of the agent that is changing states.
     * @param atTime the time at which the agent changes states.
     * @param newState the state into which the agent changes, indexed from
     *        zero.
     * @throws IOException if there is an IO error sending the state changes to
     *         the server, which by default there will not be.
     */
    public void changeState(int agentID, double atTime, int newState)
            throws IOException {}

    /**
     * By default this method does nothing, but see
     * {@link MVISPClient#flushStateChanges}.
     *
     * @throws IOException if there is an IO error communicating with the
     *         server, which by default there will not be.
     */
    protected void flushStateChanges() throws IOException {}

    /**
     * Returns the duration of the simulation in milliseconds.
     *
     * @return the duration of the simulation in milliseconds.
     */
    protected long getDurationMillis() {
        return this.duration;
    }

    /**
     * Completely fills the update buffers for all agents.
     *
     * @throws IOException if there is an IO error communicating with the
     *         server.
     * @throws UAMPException if there is a protocol error communicating with
     *         the server.
     */
    private void fillQueues() throws IOException, UAMPException {
        /*
         * Determine the number of updates required to fill the queues. Due to
         * the limitations of 32-bit unsigned integers, we may have to break
         * this into multiple distinct requests.
         */
        long toRequest = 0;
        for (int i = 0; i < this.agents.length; i++)
            toRequest += this.agents[i].numUpdatesWanted();
        int startAtAgent = 0;

        /* Repeat until we need not make any more requests */
        while (toRequest > 0) {
            long requestThisTime =
                    toRequest < UnsignedInteger.MAX_VALUE ? toRequest
                            : UnsignedInteger.MAX_VALUE;
            toRequest -= requestThisTime;
            long requested = 0L;

            BufferWriter bw =
                    new BufferWriter(this.dos, 5L + 4L * requestThisTime);
            bw.write((byte) 0x01);
            bw.write(requestThisTime);

            /* Compose this request */
            boolean needToWait = false;
            int startedAtAgent = startAtAgent;
            for (int onAgent = startAtAgent; onAgent < this.agents.length
                    && needToWait == false; onAgent++) {
                int forAgent = this.agents[onAgent].numUpdatesWanted();
                if (requestThisTime - requested >= forAgent)
                    startAtAgent++;
                else {
                    forAgent = (int) (requestThisTime - requested);
                    needToWait = true;
                }
                requested += forAgent;
                for (int i = 0; i < forAgent; i++)
                    bw.write(onAgent);
            }

            /* Process the reply to this request */
            long totalInbound;
            if (this.serverFeatures.get3DSupport())
                totalInbound = 16L * requestThisTime;
            else
                totalInbound = 12L * requestThisTime;
            if (this.serverFeatures.getAppearDisappearSupport())
                totalInbound += requestThisTime;
            BufferReader br = new BufferReader(this.dis, totalInbound);
            for (int onAgent = startedAtAgent; requested > 0; onAgent++) {
                int forAgent = this.agents[onAgent].numUpdatesWanted();
                if (forAgent > requested)
                    forAgent = (int) requested;
                requested -= forAgent;
                for (int i = 0; i < forAgent; i++) {
                    UnsignedInteger time = br.readUnsignedInt();
                    UnsignedInteger x = br.readUnsignedInt();
                    UnsignedInteger y = br.readUnsignedInt();
                    UnsignedInteger z;
                    if (this.serverFeatures.get3DSupport())
                        z = br.readUnsignedInt();
                    else
                        z = Client.ZERO;
                    boolean present = true;
                    if (this.serverFeatures.getAppearDisappearSupport()) {
                        byte presFlag = br.readByte();
                        if (presFlag == (byte) 0x00)
                            present = false;
                        else if (presFlag != (byte) 0x01)
                            throw new UAMPException(
                                    "Invalid present flag value");
                    }
                    UAMPUpdate up = new UAMPUpdate(x, y, z, time, present);
                    this.agents[onAgent].addUpdate(up);
                }
            }
        }
    }

    /**
     * The <code>UAMPAgent</code> class represents a buffer holding the
     * received location updates for each agent.
     */
    private class UAMPAgent {
        /**
         * The number of UAMP updates to buffer for an agent, in addition to
         * the previous update. Must be at least one for this class to function
         * properly.
         */
        private static final int QUEUE_SIZE = 5;

        /**
         * The agent ID of this agent.
         */
        private int agentID;

        /**
         * The previous update, representing the starting point of the current
         * command.
         */
        private UAMPUpdate previous;

        /**
         * The ordered updates for this agent, with the head of the queue
         * representing the update that is the endpoint of the current command.
         */
        private LinkedList<UAMPUpdate> updates;

        /**
         * The current command for this agent, which is the interpolation
         * between the previous update and the current update.
         */
        private AgentCommand command;

        /**
         * Whether or not an update with the final time has been received for
         * this agent.
         */
        private boolean receivedFinal;

        /**
         * Creates a new empty <code>UAMPAgent</code> buffer.
         *
         * @param agentID the ID of the agent for which we are buffering
         *        commands.
         */
        public UAMPAgent(int agentID) {
            this.agentID = agentID;
            this.previous = null;
            this.updates = new LinkedList<UAMPUpdate>();
            this.command = null;
            this.receivedFinal = false;
        }

        /**
         * Returns the number of <code>UAMPUpdate</code>s that this agent would
         * like to fill its buffers.
         *
         * @return the number of updates requested.
         */
        public int numUpdatesWanted() {
            if (this.receivedFinal)
                return 0;
            else if (this.previous == null)
                /*
                 * To match behaviour of C library's circular queue
                 * implementation, so that behaviour of client applications is
                 * identical when using either library.
                 */
                return UAMPAgent.QUEUE_SIZE + 1;

            int current = this.updates.size();
            if (current < UAMPAgent.QUEUE_SIZE)
                return UAMPAgent.QUEUE_SIZE - current;
            else
                return 0;
        }

        /**
         * Adds the given update to the queue of updates to process for this
         * agent.
         *
         * @param update the update to add.
         * @throws UAMPException if the update timestamp is not valid or if the
         *         update represents a non-equal repeated final update.
         */
        public void addUpdate(UAMPUpdate update) throws UAMPException {
            if (this.previous == null) {
                if (update.getTime().toLong() != 0L)
                    throw new UAMPException(
                            "First update for agent has non-zero time");
                double x = ((double) (update.getX().toLong())) / 1000.0;
                double y = ((double) (update.getY().toLong())) / 1000.0;
                double z = ((double) (update.getZ().toLong())) / 1000.0;
                this.command = new AgentCommand(this.agentID, x, y, z,
                        update.getPresent());
                this.previous = update;
                this.updates.add(update);
                if (duration == 0L)
                    this.receivedFinal = true;
            } else if (this.receivedFinal) {
                if (this.updates.getLast().equals(update) == false)
                    throw new UAMPException(
                            "Non-equal final updates for agent");
            } else {
                long prevUpTime;
                if (this.updates.isEmpty())
                    prevUpTime = this.previous.getTime().toLong();
                else
                    prevUpTime = this.updates.getLast().getTime().toLong();
                long thisUpTime = update.getTime().toLong();
                if (thisUpTime <= prevUpTime)
                    throw new UAMPException("Non-incrementing timestamps");
                else if (thisUpTime > duration)
                    throw new UAMPException("Timestamp is too large");
                else if (thisUpTime == duration)
                    this.receivedFinal = true;
                this.updates.add(update);
            }
        }

        /**
         * Returns the current command to be executed by this agent.
         *
         * @return the current command, or <code>null</code> if
         *         {@link #addUpdate} has never been called for this agent
         *         buffer.
         */
        public AgentCommand currentCommand() {
            return this.command;
        }

        /**
         * Returns the previous update for this agent, representing the update
         * at the starting point of the current comand.
         *
         * @return the previous update, or <code>null</code> if
         *         {@link #addUpdate} has never been called for this agent
         *         buffer.
         */
        public UAMPUpdate previousUpdate() {
            return this.previous;
        }

        /**
         * Returns the current update for this agent, representing the update
         * at the endpoint of the current command.
         *
         * @return the current update, or <code>null</code> if
         *         {@link #addUpdate} has never been called for this agent
         *         buffer.
         */
        public UAMPUpdate currentUpdate() {
            return this.updates.peek();
        }

        /**
         * Advances to the next update and next command for this agent.
         *
         * @return <code>true</code> if we advanced, or <code>false</code> if
         *         there was no further advancement possible.
         * @throws IOException if there is an IO error refilling the queues.
         * @throws UAMPException if there is a protocol error refilling the
         *         queues.
         */
        public boolean advance() throws IOException, UAMPException {
            /* Pop an update off the queue */
            long currentTime = this.updates.peek().getTime().toLong();
            if (currentTime == duration)
                return false;
            this.previous = this.updates.pop();
            if (this.updates.isEmpty())
                fillQueues();

            /* Update the global time bounds */
            if (currentTime > largestLastTime)
                largestLastTime = currentTime;
            if (currentTime == smallestCurrentTime) {
                long newSmallest = duration;
                for (int i = 0; i < agents.length; i++) {
                    long poss = agents[i].updates.peek().getTime().toLong();
                    if (poss < newSmallest)
                        newSmallest = poss;
                }
                smallestCurrentTime = newSmallest;
            }

            /* Construct the new command */
            UAMPUpdate curUp = this.updates.peek();
            double x = ((double) (curUp.getX().toLong())) / 1000.0;
            double y = ((double) (curUp.getY().toLong())) / 1000.0;
            double z = ((double) (curUp.getZ().toLong())) / 1000.0;
            double time = ((double) (curUp.getTime().toLong())) / 1000.0;
            this.command = new AgentCommand(this.command, x, y, z, time,
                    this.previous.getPresent());
            return true;
        }
    }

    /**
     * The <code>UAMPUpdate</code> class represents a single location update
     * received from a server.
     */
    private class UAMPUpdate {
        /**
         * The x coordinate in millimetres.
         */
        private UnsignedInteger x;

        /**
         * The y coordinate in millimetres.
         */
        private UnsignedInteger y;

        /**
         * The z coordinate in millimetres.
         */
        private UnsignedInteger z;

        /**
         * The time of the update in milliseconds.
         */
        private UnsignedInteger time;

        /**
         * Whether or not the agent is present in the environment at the time
         * of the update.
         */
        private boolean present;

        /**
         * Creates a new <code>UAMPUpdate</code> at the given location and
         * time.
         *
         * @param x the x coordinate of the update in millimetres.
         * @param y the y coordinate of the update in millimetres.
         * @param z the z coordinate of the update in millimetres.
         * @param time the time of the update in milliseconds.
         * @param present whether or not the agent is present at the time of
         *        the update.
         */
        public UAMPUpdate(UnsignedInteger x, UnsignedInteger y,
                UnsignedInteger z, UnsignedInteger time, boolean present) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
            this.present = present;
        }

        /**
         * Returns the x coordinate of the update in millimetres.
         *
         * @return the x coordinate of the update.
         */
        public UnsignedInteger getX() {
            return this.x;
        }

        /**
         * Returns the y coordinate of the update in millimetres.
         *
         * @return the y coordinate of the update.
         */
        public UnsignedInteger getY() {
            return this.y;
        }

        /**
         * Returns the z coordinate of the update in millimetres.
         *
         * @return the z coordinate of the update.
         */
        public UnsignedInteger getZ() {
            return this.z;
        }

        /**
         * Returns the time of the update in milliseconds.
         *
         * @return the time of the update.
         */
        public UnsignedInteger getTime() {
            return this.time;
        }

        /**
         * Returns whether or not the agent is present at the time of the
         * update.
         *
         * @return <code>true</code> if the agent is present, otherwise
         *         <code>false</code>.
         */
        public boolean getPresent() {
            return this.present;
        }

        /**
         * Compares this update with another update for equality.
         *
         * @param o the other update to test for equality.
         * @return <code>true</code> if all parameters are identical, otherwise
         *         <code>false</code>.
         */
        public boolean equals(Object o) {
            if (o == null)
                return false;
            if (o instanceof UAMPUpdate) {
                UAMPUpdate other = (UAMPUpdate) o;
                return (this.x.equals(other.x) && this.y.equals(other.y)
                        && this.z.equals(other.z)
                        && this.time.equals(other.time)
                        && this.present == other.present);
            }
            return false;
        }

        /**
         * Returns a hash code for this <code>UAMPUpdate</code>.
         *
         * @return a hash code for this <code>UAMPUpdate</code>, equal to the
         *         hash code of its x coordinate.
         */
        public int hashCode() {
            return this.x.hashCode();
        }
    }
}
