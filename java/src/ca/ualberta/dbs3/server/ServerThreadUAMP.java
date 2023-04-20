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

package ca.ualberta.dbs3.server;

import ca.ualberta.dbs3.cartography.*;
import ca.ualberta.dbs3.math.*;
import ca.ualberta.dbs3.network.*;
import ca.ualberta.dbs3.simulations.*;
import java.io.*;
import java.net.*;
import java.util.Arrays;

/**
 * The <code>ServerThreadUAMP</code> class represents a single thread that
 * executes the UAMP protocol.
 */
public class ServerThreadUAMP extends ServerThread {
    /**
     * The four identification bytes for the UAMP protocol.
     */
    public static final byte[] ID_BYTES =
            {(byte) 0x55, (byte) 0x41, (byte) 0x4d, (byte) 0x50};

    /**
     * The minimum number of agents allowed for a request to be accepted.
     */
    private static final long MIN_AGENTS = 1L;

    /**
     * The maximum number of agents allowed for a request to be accepted. This
     * number is arbitrary, and can be changed without issue, so long as it is
     * not increased beyond Integer.MAX_VALUE. This limit is meant to ensure
     * that individual simulations do not use surprisingly large amounts of
     * resources.
     */
    private static final long MAX_AGENTS = 1000000L;

    /**
     * The range of speeds of agents on this server.
     */
    private Range speed;

    /**
     * The range of agent pause times on this server.
     */
    private Range pause;

    /**
     * The destination chooser used by agents on this server.
     */
    private DestinationChooser destChooser;

    /**
     * The pathfinder used by agents on this server.
     */
    private Pathfinder pathfinder;

    /**
     * Creates a new <code>ServerThreadUAMP</code> to deal with a single
     * incoming connection.
     *
     * @param speed the range of speeds at which agents may move, in metres per
     *        second.
     * @param pause the range of pause times that agents may use, in seconds.
     * @param destChooser the destination selection algorithm that agents will
     *        use.
     * @param pathfinder the pathfinding algorithm that agents will use.
     * @param connection the inbound connection for this thread to deal with.
     */
    public ServerThreadUAMP(Range speed, Range pause,
            DestinationChooser destChooser, Pathfinder pathfinder,
            Socket connection) {
        super(connection);
        this.speed = speed;
        this.pause = pause;
        this.destChooser = destChooser;
        this.pathfinder = pathfinder;
    }

    /**
     * Returns the four bytes to send to the client at the beginning of the
     * initialization phase.
     *
     * @return the four bytes to send.
     */
    protected byte[] getIDBytes() {
        return ServerThreadUAMP.ID_BYTES;
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
        if (Arrays.equals(clientID, ServerThreadMVISP.ID_BYTES))
            throw new UAMPException(
                    "MVISP client attempted to connect to UAMP server");
        else if (Arrays.equals(clientID, ServerThreadUAMP.ID_BYTES) == false)
            throw new UAMPException("Unknown value received during handshake");
    }

    /**
     * Reads the simulation request message from the client, and responds with
     * either a REQUEST_OKAY or REQUEST_DENIED message.
     *
     * @return the requested simulation.
     * @throws IOException on an IO error with the socket.
     * @throws UAMPException if the client does not conform to the protocol or
     *         if the simulation request is denied.
     */
    protected SimulationDiscrete getSimulation()
            throws IOException, UAMPException {
        /* Read NUM_AGENTS, TIME_LIMIT, and SEED */
        BufferReader br = new BufferReader(this.dis, 12);
        long numAgents = br.readUnsignedInt().toLong();
        UnsignedInteger timeLimit = br.readUnsignedInt();
        long seed = br.readUnsignedInt().toLong();

        /* If the agent number is not sane, reject the request */
        if (numAgents < ServerThreadUAMP.MIN_AGENTS
                || numAgents > ServerThreadUAMP.MAX_AGENTS) {
            this.dos.writeByte((byte) 0x01);
            throw new UAMPException("Simulation request rejected");
        }

        /* Otherwise, accept the request */
        this.dos.writeByte((byte) 0x00);
        Simulation sim =
                new Simulation((int) numAgents, this.speed, this.pause,
                        timeLimit, this.destChooser, this.pathfinder, seed);
        return new SimulationDiscrete(sim);
    }

    /**
     * Throws a <code>UAMPException</code>, since state change messages should
     * never be sent to a UAMP server.
     *
     * @param num the number of state change messages to follow.
     * @throws IOException never; this <code>throws</code> directive is just
     *         necessary to comply with the {@link ServerThread} interface.
     * @throws UAMPException always.
     */
    protected void parseStateChange(UnsignedInteger num)
            throws IOException, UAMPException {
        throw new UAMPException("CHANGE_STATE message sent to UAMP server");
    }
}
