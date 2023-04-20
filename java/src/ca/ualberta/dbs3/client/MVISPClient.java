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

import ca.ualberta.dbs3.network.BufferReader;
import ca.ualberta.dbs3.network.BufferWriter;
import ca.ualberta.dbs3.network.UAMPException;
import ca.ualberta.dbs3.network.UnsignedInteger;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * The <code>MVISPClient</code> class represents a client connection to an
 * MVISP server.
 */
public class MVISPClient extends Client {
    /**
     * The maximum length of a state name.
     */
    public static final int MAX_STATE_LENGTH = 1024;

    /**
     * The maximal number of state changes that can be buffered before they are
     * sent to an MVISP server.
     */
    private static final int CHANGE_BUFFER = 128;

    /**
     * The ordered queue of state changes to be sent to the server.
     */
    private Queue<MVISPState> stateChanges;

    /**
     * The number of states to which agents may transition, or a negative value
     * to indicate that state changes are disabled.
     */
    private int numStates;

    /**
     * Creates a new <code>MVISPClient</code> connection to the MVISP server at
     * the given host and port, with the given set of possible agent states.
     *
     * @param hostname the host on which the MVISP server resides.
     * @param port the port on which the MVISP server resides.
     * @param states the list of potential states in which agents can exist.
     * @throws IllegalArgumentException if the list of states is empty or if
     *         there is a duplicate, empty, or too-long state name.
     * @throws IOException if there is an IO error establishing the connection.
     * @throws UAMPException if there is a protocol error during the
     *         establishment of the connection.
     */
    public MVISPClient(String hostname, int port, List<String> states)
            throws IOException, UAMPException {
        this(hostname, port, states, new AcceptAll(), new ClientFeatures());
    }

    /**
     * Creates a new <code>MVISPClient</code> connection to the MVISP server at
     * the given host and port, with the given set of possible agent states.
     *
     * @param hostname the host on which the MVISP server resides.
     * @param port the port on which the MVISP server resides.
     * @param states the list of potential states in which agents can exist.
     * @throws IllegalArgumentException if the list of states is empty or if
     *         there is a duplicate, empty, or too-long state name.
     * @throws IOException if there is an IO error establishing the connection.
     * @throws UAMPException if there is a protocol error during the
     *         establishment of the connection.
     */
    public MVISPClient(String hostname, int port, String[] states)
            throws IOException, UAMPException {
        this(hostname, port, states, new AcceptAll(), new ClientFeatures());
    }

    /**
     * Creates a new <code>MVISPClient</code> connection to the MVISP server at
     * the given host and port, with the given set of possible agent states,
     * accepting or rejecting the server's simulation specification as
     * determined by the given callback.
     *
     * @param hostname the host on which the MVISP server resides.
     * @param port the port on which the MVISP server resides.
     * @param states the list of potential states in which agents can exist.
     * @param callback the callback object that determines whether to accept or
     *        reject the server's simulation specification.
     * @throws IllegalArgumentException if the list of states is empty or if
     *         there is a duplicate, empty, or too-long state name.
     * @throws IOException if there is an IO error establishing the connection.
     * @throws UAMPException if there is a protocol error during the
     *         establishment of the connection.
     */
    public MVISPClient(String hostname, int port, List<String> states,
            MVISPCallback callback) throws IOException, UAMPException {
        this(hostname, port, states, callback, new ClientFeatures());
    }

    /**
     * Creates a new <code>MVISPClient</code> connection to the MVISP server at
     * the given host and port, with the given set of possible agent states,
     * accepting or rejecting the server's simulation specification as
     * determined by the given callback.
     *
     * @param hostname the host on which the MVISP server resides.
     * @param port the port on which the MVISP server resides.
     * @param states the list of potential states in which agents can exist.
     * @param callback the callback object that determines whether to accept or
     *        reject the server's simulation specification.
     * @throws IllegalArgumentException if the list of states is empty or if
     *         there is a duplicate, empty, or too-long state name.
     * @throws IOException if there is an IO error establishing the connection.
     * @throws UAMPException if there is a protocol error during the
     *         establishment of the connection.
     */
    public MVISPClient(String hostname, int port, String[] states,
            MVISPCallback callback) throws IOException, UAMPException {
        this(hostname, port, states, callback, new ClientFeatures());
    }

    /**
     * Creates a new <code>MVISPClient</code> connection to the MVISP server at
     * the given host and port, with the given set of possible agent states,
     * where the client application supports the given optional extensions to
     * the MVISP protocol.
     *
     * @param hostname the host on which the MVISP server resides.
     * @param port the port on which the MVISP server resides.
     * @param states the list of potential states in which agents can exist.
     * @param features the optional extensions to the MVISP protocol that are
     *        supported by the client application.
     * @throws IllegalArgumentException if the list of states is empty or if
     *         there is a duplicate, empty, or too-long state name.
     * @throws IOException if there is an IO error establishing the connection.
     * @throws UAMPException if there is a protocol error during the
     *         establishment of the connection.
     */
    public MVISPClient(String hostname, int port, List<String> states,
            ClientFeatures features) throws IOException, UAMPException {
        this(hostname, port, states, new AcceptAll(), features);
    }

    /**
     * Creates a new <code>MVISPClient</code> connection to the MVISP server at
     * the given host and port, with the given set of possible agent states,
     * where the client application supports the given optional extensions to
     * the MVISP protocol.
     *
     * @param hostname the host on which the MVISP server resides.
     * @param port the port on which the MVISP server resides.
     * @param states the list of potential states in which agents can exist.
     * @param features the optional extensions to the MVISP protocol that are
     *        supported by the client application.
     * @throws IllegalArgumentException if the list of states is empty or if
     *         there is a duplicate, empty, or too-long state name.
     * @throws IOException if there is an IO error establishing the connection.
     * @throws UAMPException if there is a protocol error during the
     *         establishment of the connection.
     */
    public MVISPClient(String hostname, int port, String[] states,
            ClientFeatures features) throws IOException, UAMPException {
        this(hostname, port, states, new AcceptAll(), features);
    }

    /**
     * Creates a new <code>MVISPClient</code> connection to the MVISP server at
     * the given host and port, with the given set of possible agent states,
     * where the client application supports the given optional extensions to
     * the MVISP protocol, accepting or rejecting the server's simulation
     * specification as determined by the given callback.
     *
     * @param hostname the host on which the MVISP server resides.
     * @param port the port on which the MVISP server resides.
     * @param states the list of potential states in which agents can exist.
     * @param callback the callback object that determines whether to accept or
     *        reject the server's simulation specification.
     * @param features the optional extensions to the MVISP protocol that are
     *        supported by the client application.
     * @throws IllegalArgumentException if the list of states is empty or if
     *         there is a duplicate, empty, or too-long state name.
     * @throws IOException if there is an IO error establishing the connection.
     * @throws UAMPException if there is a protocol error during the
     *         establishment of the connection.
     */
    public MVISPClient(String hostname, int port, List<String> states,
            MVISPCallback callback, ClientFeatures features)
            throws IOException, UAMPException {
        this(hostname, port, states.toArray(new String[0]), callback,
                features);
    }

    /**
     * Creates a new <code>MVISPClient</code> connection to the MVISP server at
     * the given host and port, with the given set of possible agent states,
     * where the client application supports the given optional extensions to
     * the MVISP protocol, accepting or rejecting the server's simulation
     * specification as determined by the given callback.
     *
     * @param hostname the host on which the MVISP server resides.
     * @param port the port on which the MVISP server resides.
     * @param states the list of potential states in which agents can exist.
     * @param callback the callback object that determines whether to accept or
     *        reject the server's simulation specification.
     * @param features the optional extensions to the MVISP protocol that are
     *        supported by the client application.
     * @throws IllegalArgumentException if the list of states is empty or if
     *         there is a duplicate, empty, or too-long state name.
     * @throws IOException if there is an IO error establishing the connection.
     * @throws UAMPException if there is a protocol error during the
     *         establishment of the connection.
     */
    public MVISPClient(String hostname, int port, String[] states,
            MVISPCallback callback, ClientFeatures features)
            throws IOException, UAMPException {
        /* Connect to the server and verify parameter sanity */
        super(hostname, port, features);
        if (states.length == 0)
            throw new IllegalArgumentException("Empty state list");
        byte[][] stateBytes = new byte[states.length][];
        for (int onState = 0; onState < states.length; onState++) {
            stateBytes[onState] = states[onState].getBytes("US-ASCII");
            if (stateBytes[onState].length == 0
                    || stateBytes[onState].length > MVISPClient.MAX_STATE_LENGTH)
                throw new IllegalArgumentException(
                        "Invalid state name length");
            for (int i = 0; i < onState; i++) {
                if (Arrays.equals(stateBytes[i], stateBytes[onState]))
                    throw new IllegalArgumentException("Duplicate state name");
            }
        }

        /* Perform the initial version-selection handshake */
        this.performHandshake("MVIS");

        /* Read the simulation specification */
        BufferReader br = new BufferReader(this.dis, 8);
        UnsignedInteger uiNumAgents = br.readUnsignedInt();
        UnsignedInteger uiTimeLimit = br.readUnsignedInt();

        /*
         * Test if we're okay with the specification; if not, send a
         * SPECIFICATION_DENIED message (32-bit zero) and disconnect.
         */
        if (uiNumAgents.toLong() > Integer.MAX_VALUE) {
            this.dos.writeInt(0);
            throw new UAMPException(
                    "Too many agents in simulation specification");
        }
        int numAgents = (int) (uiNumAgents.toLong());
        if (numAgents == 0)
            throw new UAMPException("Zero agents in simulation specification");
        double duration = ((double) uiTimeLimit.toLong()) / 1000.0;
        if (callback.processSpecification(numAgents, duration) == false) {
            this.dos.writeInt(0);
            throw new UAMPException(
                    "Simulation specification rejected by callback");
        }

        /* Send the state specification message */
        this.numStates = states.length;
        long specLen = 4L + 4L * this.numStates;
        for (int onState = 0; onState < this.numStates; onState++)
            specLen += stateBytes[onState].length;
        BufferWriter bw = new BufferWriter(this.dos, specLen);
        bw.write(states.length);
        for (int onState = 0; onState < this.numStates; onState++)
            bw.write(stateBytes[onState].length);
        for (int onState = 0; onState < this.numStates; onState++)
            bw.write(stateBytes[onState]);

        /* Read the initial locations */
        this.readInitialLocations(uiNumAgents, uiTimeLimit);
        this.stateChanges = new LinkedList<MVISPState>();
    }

    /**
     * Sends a notification of state change to an MVISP server, changing the
     * given agent at the given time in seconds to the given state. Note that
     * the state change message may not be sent right away; it can be buffered
     * arbitrarily by this library, up until the connection to the server is
     * closed.
     *
     * @param agentID the agent ID of the agent that is changing states.
     * @param atTime the time at which the agent changes states.
     * @param newState the state into which the agent changes, indexed from
     *        zero.
     * @throws IllegalArgumentException if the agent ID, time, or state falls
     *         outside the bounds of the number of agents, duration of the
     *         simulation, or number of states respectively.
     * @throws IOException if there is an IO error sending the state changes to
     *         the server.
     */
    public void changeState(int agentID, double atTime, int newState)
            throws IOException {
        if (agentID < 0 || agentID >= this.getNumAgents())
            throw new IllegalArgumentException("Invalid agent ID");
        if (atTime < 0.0)
            throw new IllegalArgumentException("Negative time");
        UnsignedInteger timeMilli;
        try {
            timeMilli = new UnsignedInteger(atTime * 1000.0);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException(
                    "Invalid time for state change");
        }
        if (timeMilli.toLong() > this.getDurationMillis())
            throw new IllegalArgumentException(
                    "Invalid time for state change");
        if (newState < 0 || newState >= this.numStates)
            throw new IllegalArgumentException("Invalid state");

        MVISPState mvs = new MVISPState(new UnsignedInteger(agentID),
                timeMilli, new UnsignedInteger(newState));
        this.stateChanges.add(mvs);
        if (this.stateChanges.size() == MVISPClient.CHANGE_BUFFER)
            this.flushStateChanges();
    }

    /**
     * Verifies that the four protocol bytes sent by the server at the
     * beginning of the handshake are the bytes expected by the MVISP protocol.
     * 
     * @param serverProtocol the four bytes sent by the server.
     * @throws IllegalArgumentException if the length of
     *         <code>serverProtocol</code> is not <code>4</code>.
     * @throws UAMPException if the four bytes do not match the specification
     *         of the MVISP protocol.
     * @throws UnsupportedEncodingException if the known byte formats cannot be
     *         encoded.
     */
    protected void verifyProtocolBytes(byte[] serverProtocol)
            throws UAMPException, UnsupportedEncodingException {
        if (serverProtocol.length != 4) {
            throw new IllegalArgumentException(
                    "Invalid server protocol byte length");
        }

        byte[] mBytes = "MVIS".getBytes("US-ASCII");
        byte[] uBytes = "UAMP".getBytes("US-ASCII");

        if (Arrays.equals(serverProtocol, uBytes))
            throw new UAMPException("MVISP client connecting to UAMP server");
        else if (Arrays.equals(serverProtocol, mBytes) == false)
            throw new UAMPException("Unknown handshake received from server");
    }

    /**
     * Transmits all buffered state changes to the server.
     *
     * @throws IOException if there is an IO error communicating with the
     *         server.
     */
    protected void flushStateChanges() throws IOException {
        if (this.stateChanges.isEmpty())
            return;

        int num = this.stateChanges.size();
        BufferWriter bw = new BufferWriter(this.dos, 5L + 12L * num);
        bw.write((byte) 0x02);
        bw.write(num);
        while (true) {
            MVISPState mvs = this.stateChanges.poll();
            if (mvs == null)
                break;
            bw.write(mvs.getAgentID());
            bw.write(mvs.getTime());
            bw.write(mvs.getNewState());
        }
    }

    /**
     * The <code>MVISPState</code> class represents a state change message that
     * needs to be sent to an MVISP server.
     */
    private class MVISPState {
        /**
         * The agent that is changing state.
         */
        private UnsignedInteger agentID;

        /**
         * The time of the state change in milliseconds.
         */
        private UnsignedInteger time;

        /**
         * The target state into which to change.
         */
        private UnsignedInteger newState;

        /**
         * Creates a new record of a state change in an MVISP client.
         *
         * @param agentID the agent changing state.
         * @param time the time of the state change in milliseconds.
         * @param newState the state into which the agent is changing.
         */
        public MVISPState(UnsignedInteger agentID, UnsignedInteger time,
                UnsignedInteger newState) {
            this.agentID = agentID;
            this.time = time;
            this.newState = newState;
        }

        /**
         * Returns the ID of the agent changing state.
         *
         * @return the ID of the agent.
         */
        public UnsignedInteger getAgentID() {
            return this.agentID;
        }

        /**
         * Returns the time in milliseconds of the state change.
         *
         * @return the time in milliseconds.
         */
        public UnsignedInteger getTime() {
            return this.time;
        }

        /**
         * Returns the new state of the agent after the state change.
         *
         * @return the new agent state.
         */
        public UnsignedInteger getNewState() {
            return this.newState;
        }
    }

    /**
     * The <code>AcceptAll</code> class is an MVISP callback procedure that
     * accepts all simulation specifications.
     */
    private static class AcceptAll implements MVISPCallback {
        /**
         * Determines whether or not an MVISP client should accept a simulation
         * specification provided by an MVISP server, which the
         * <code>AcceptAll</code> class always does.
         *
         * @param numAgents the number of agents in the simulation
         *        specification, guaranteed to be greater than zero.
         * @param duration the duration of the simulation in the simulation
         *        specification, in seconds, guaranteed to be non-negative.
         * @return <code>true</code> always.
         */
        public boolean processSpecification(int numAgents, double duration) {
            return true;
        }
    }
}
