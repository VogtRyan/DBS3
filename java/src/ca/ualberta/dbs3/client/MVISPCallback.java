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
 * A class that implements the <code>MVISPCallback</code> interface is capable
 * of receiving the number of agents and length of time of a simulation
 * specified by an MVISP server and determining whether the client should
 * accept or reject the simulation specification.
 */
public interface MVISPCallback {
    /**
     * Determines whether or not an MVISP client should accept a simulation
     * specification provided by an MVISP server.
     *
     * @param numAgents the number of agents in the simulation specification,
     *        guaranteed to be greater than zero.
     * @param duration the duration of the simulation in the simulation
     *        specification, in seconds, guaranteed to be non-negative.
     * @return <code>true</code> if the simulation specification should be
     *         accepted, or <code>false</code> if it should be rejected.
     */
    public boolean processSpecification(int numAgents, double duration);
}
