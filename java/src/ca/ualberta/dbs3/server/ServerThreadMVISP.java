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

package ca.ualberta.dbs3.server;

import ca.ualberta.dbs3.network.*;
import ca.ualberta.dbs3.simulations.*;
import java.io.*;
import java.util.Arrays;

/**
 * The <code>ServerThreadMVISP</code> class represents a single thread that
 * executes the MVISP protocol.
 */
public class ServerThreadMVISP extends ServerThread {
    /**
     * The four identification bytes for the MVISP protocol.
     */
    public static final byte[] ID_BYTES =
            {(byte) 0x4d, (byte) 0x56, (byte) 0x49, (byte) 0x53};

    /**
     * The simulation to send to the client.
     */
    private SimulationDiscrete simulation;

    /**
     * The factory for the set of state transitions for all agents.
     */
    private StateRecordFactory factory;

    /**
     * Creates a new <code>ServerThreadMVISP</code> that will open a socket and
     * deal with a single connection.
     *
     * @param simulation the simulation to send to the client.
     * @param port the port on which to open the server.
     * @throws IllegalArgumentException if any time has progressed in the
     *         simulation.
     */
    public ServerThreadMVISP(SimulationDiscrete simulation, int port) {
        super(port);

        int numAgents = simulation.getNumAgents();
        for (int i = 0; i < numAgents; i++) {
            if (simulation.getCurrentTimeSeconds(i) != 0.0)
                throw new IllegalArgumentException("Non-fresh simulation");
        }

        this.simulation = simulation;
        this.factory = new StateRecordFactory(simulation.getNumAgents());
    }

    /**
     * Returns the four bytes to send to the client at the beginning of the
     * initialization phase.
     *
     * @return the four bytes to send.
     */
    protected byte[] getIDBytes() {
        return ServerThreadMVISP.ID_BYTES;
    }

    /**
     * Parses the four bytes received from the client during the initialization
     * phase.
     *
     * @param clientID the four identifying bytes sent by the client.
     * @throws UAMPException if this server cannot interact with the connecting
     *         client.
     */
    protected void parseIDBytes(byte[] clientID) throws UAMPException {
        if (Arrays.equals(clientID, ServerThreadUAMP.ID_BYTES))
            throw new UAMPException(
                    "UAMP client attempted to connect to MVISP server");
        else if (Arrays.equals(clientID, ServerThreadMVISP.ID_BYTES) == false)
            throw new UAMPException("Unknown value received during handshake");
    }

    /**
     * Sends the simulation specification to the client and reads the state
     * specification, before returning the specified simulation.
     *
     * @return the specified simulation.
     * @throws IOException on an IO error with the socket.
     * @throws UAMPException if the client does not conform to the protocol or
     *         if the simulation specification is denied.
     */
    protected SimulationDiscrete getSimulation()
            throws IOException, UAMPException {
        /* Write the simulation specification */
        BufferWriter bw = new BufferWriter(this.dos, 8);
        bw.write(this.simulation.getNumAgents());
        bw.write(this.simulation.getDuration());

        /* Read the number of states */
        BufferReader br = new BufferReader(this.dis, 4);
        long nsl = br.readUnsignedInt().toLong();
        if (nsl == 0L)
            throw new UAMPException("Client denied simulation specification");
        if (nsl > Integer.MAX_VALUE)
            throw new UAMPException("Too many states");
        int numStates = (int) nsl;

        /* Read the state lengths */
        br = new BufferReader(this.dis, ((long) numStates) * 4);
        int[] stateLengths = new int[numStates];
        for (int i = 0; i < numStates; i++) {
            long len = br.readUnsignedInt().toLong();
            if (len == 0)
                throw new UAMPException("Invalid state name length");
            if (len > Integer.MAX_VALUE)
                throw new UAMPException("State name too long");
            stateLengths[i] = (int) len;
        }

        /* Read the state names */
        long totalLen = 0;
        for (int i = 0; i < numStates; i++)
            totalLen += stateLengths[i];
        br = new BufferReader(this.dis, totalLen);
        for (int i = 0; i < numStates; i++) {
            String name = br.readString(stateLengths[i]);
            try {
                this.factory.addState(name);
            } catch (IllegalArgumentException iae) {
                throw new UAMPException("Duplicate state name");
            }
        }

        /* Return the constructed simulation */
        return this.simulation;
    }

    /**
     * Adds a parsed state change command to the set of state change commands
     * parsed by this server thread.
     *
     * @param num the number of state change messages to follow.
     * @throws IOException on an IO error with the socket.
     * @throws UAMPException if the client does not conform to the protocol
     */
    protected void parseStateChange(UnsignedInteger num)
            throws IOException, UAMPException {
        long numChanges = num.toLong();
        if (numChanges == 0L)
            throw new UAMPException("Invalid NUM_CHANGES value");

        /* Multiplication is safe: 2^32-1 * 12 is in range of a long */
        BufferReader br = new BufferReader(this.dis, numChanges * 12);

        for (long i = 0; i < numChanges; i++) {
            /* Read the three parameters to the state change */
            long id = br.readUnsignedInt().toLong();
            long time = br.readUnsignedInt().toLong();
            long state = br.readUnsignedInt().toLong();

            /* Ensure the values are sane */
            if (state >= (long) (this.factory.getNumStates()))
                throw new UAMPException("Invalid state number");
            if (time > this.simulation.getDuration().toLong())
                throw new UAMPException(
                        "State change time greater than duration");
            if (id >= (long) (this.simulation.getNumAgents()))
                throw new UAMPException("Invalid agent ID in state change");

            this.factory.addTransition((int) id, (int) state, time);
        }
    }

    /**
     * Returns the <code>StateRecord</code> built from the state change
     * messages sent to this MVISP server.
     *
     * @return the constructed <code>StateRecord</code>.
     * @throws IllegalStateException if the thread did not complete
     *         successfully.
     */
    public StateRecord getStateRecord() {
        if (this.isSuccessfullyCompleted() == false)
            throw new IllegalStateException("Did not complete successfully");
        return this.factory.getRecord();
    }
}
