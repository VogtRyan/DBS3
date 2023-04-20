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

import ca.ualberta.dbs3.network.BufferWriter;
import ca.ualberta.dbs3.network.UAMPException;
import ca.ualberta.dbs3.network.UnsignedInteger;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * The <code>UAMPClient</code> class represents a client connection to a UAMP
 * server.
 */
public class UAMPClient extends Client {
    /**
     * The maximum possible simulation duration that can be requested from a
     * UAMP server, in seconds.
     */
    public static final double MAX_TIME = 4294967.295;

    /**
     * Creates a new <code>UAMPClient</code> connection to the UAMP server at
     * the given host and port, requesting mobility data for the given number
     * of agents in a simulation with the given duration initialized with the
     * given random seed.
     *
     * @param hostname the host on which the UAMP server resides.
     * @param port the port on which the UAMP server resides.
     * @param numAgents the number of agents in the simulation.
     * @param timeLimit the duration of the simulation in seconds.
     * @param seed a random seed with which to initialize the simulation.
     * @throws IllegalArgumentException if the number of agents or time limit
     *         is invalid (see {@link #MAX_TIME}).
     * @throws IOException if there is an IO error establishing the connection.
     * @throws UAMPException if there is a protocol error during the
     *         establishment of the connection.
     */
    public UAMPClient(String hostname, int port, int numAgents,
            double timeLimit, long seed) throws IOException, UAMPException {
        this(hostname, port, numAgents, timeLimit, seed, new ClientFeatures());
    }

    /**
     * Creates a new <code>UAMPClient</code> connection to the UAMP server at
     * the given host and port, requesting mobility data for the given number
     * of agents in a simulation with the given duration initialized with the
     * given random seed, where the client application supports the given
     * optional extensions to the UAMP protocol.
     *
     * @param hostname the host on which the UAMP server resides.
     * @param port the port on which the UAMP server resides.
     * @param numAgents the number of agents in the simulation.
     * @param timeLimit the duration of the simulation in seconds.
     * @param seed a random seed with which to initialize the simulation.
     * @param features the optional UAMP features supported by the client
     *        application.
     * @throws IllegalArgumentException if the number of agents or time limit
     *         is invalid (see {@link #MAX_TIME}).
     * @throws IOException if there is an IO error establishing the connection.
     * @throws UAMPException if there is a protocol error during the
     *         establishment of the connection.
     */
    public UAMPClient(String hostname, int port, int numAgents,
            double timeLimit, long seed, ClientFeatures features)
            throws IOException, UAMPException {
        /* Connect to the server and verify parameter sanity */
        super(hostname, port, features);
        if (numAgents <= 0 || numAgents > UnsignedInteger.MAX_VALUE)
            throw new IllegalArgumentException("Invalid number of agents");
        if (timeLimit < 0.0 || timeLimit > UAMPClient.MAX_TIME)
            throw new IllegalArgumentException("Invalid simulation duration");

        /* Perform the initial version-selection handshake */
        this.performHandshake("UAMP");

        /* Send the simulation request */
        UnsignedInteger numAgentsUI = new UnsignedInteger(numAgents);
        UnsignedInteger timeLimitUI = new UnsignedInteger(timeLimit * 1000.0);
        UnsignedInteger seedUI = new UnsignedInteger(seed & 0xFFFFFFFFL);
        BufferWriter bw = new BufferWriter(this.dos, 12);
        bw.write(numAgentsUI);
        bw.write(timeLimitUI);
        bw.write(seedUI);

        /* Read the reply */
        byte response = this.dis.readByte();
        if (response == (byte) 0x01)
            throw new UAMPException("Server denied simulation request");
        else if (response != (byte) 0x00)
            throw new UAMPException("Unknown response to simulation request");
        this.readInitialLocations(numAgentsUI, timeLimitUI);
    }

    /**
     * Verifies that the four protocol bytes sent by the server at the
     * beginning of the handshake are the bytes expected by the UAMP protocol.
     * 
     * @param serverProtocol the four bytes sent by the server.
     * @throws IllegalArgumentException if the length of
     *         <code>serverProtocol</code> is not <code>4</code>.
     * @throws UAMPException if the four bytes do not match the specification
     *         of the UAMP protocol.
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

        if (Arrays.equals(serverProtocol, mBytes))
            throw new UAMPException("UAMP client connecting to MVISP server");
        else if (Arrays.equals(serverProtocol, uBytes) == false)
            throw new UAMPException("Unknown handshake received from server");
    }
}
