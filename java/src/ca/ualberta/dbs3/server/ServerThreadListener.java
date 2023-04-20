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

/**
 * Classes conforming to the <code>ServerThreadListener</code> interface are
 * able to respond to the completion or termination of a {@link ServerThread}.
 */
public interface ServerThreadListener {
    /**
     * A <code>ServerThread</code> that successfully passes the negotiation
     * phase will call this method of its registered
     * <code>ServerThreadListener</code>.
     *
     * @param thread the thread that completed negotiation.
     * @param numAgents the number of agents in the simulation.
     * @param durationMilli the number of milliseconds in duration the
     *        simulation will be.
     */
    public void serverThreadExchange(ServerThread thread, int numAgents,
            long durationMilli);

    /**
     * A <code>ServerThread</code> that sends mobility data with a given
     * timestamp will call this method of its registered
     * <code>ServerThreadListener</code>.
     *
     * @param thread the thread that sent mobility data.
     * @param agentID the agent for which mobility data was sent.
     * @param timestamp the timestamp of the mobility data sent in
     *        milliseconds.
     */
    public void serverThreadProgress(ServerThread thread, int agentID,
            long timestamp);

    /**
     * A <code>ServerThread</code> that comes to a natural, error-free
     * completion will call this method of its registered
     * <code>ServerThreadListener</code>.
     *
     * @param thread the thread that completes.
     */
    public void serverThreadCompleted(ServerThread thread);

    /**
     * A <code>ServerThread</code> that is killed via its
     * {@link ServerThread#killThread} method will call this method of its
     * registered <code>ServerThreadListener</code>.
     *
     * @param thread the thread that is killed.
     */
    public void serverThreadKilled(ServerThread thread);

    /**
     * A <code>ServerThread</code> that terminates due to an error will call
     * this method of its registered <code>ServerThreadListener</code>.
     *
     * @param thread the thread that experiences the error.
     * @param error the error that occurred.
     */
    public void serverThreadError(ServerThread thread, Throwable error);
}
