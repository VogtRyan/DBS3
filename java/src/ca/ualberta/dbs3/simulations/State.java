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

package ca.ualberta.dbs3.simulations;

/**
 * The <code>State</code> class represents a state in which an MVISP agent can
 * be, based on feedback from an MVISP client to the server.
 */
public class State {
    /**
     * The name of the state.
     */
    private String name;

    /**
     * The index of the state in the client's state list.
     */
    private int index;

    /**
     * Creates a new <code>State</code> with the given name at the given client
     * index.
     *
     * @param name the name of the state.
     * @param index the client index of the state.
     * @throws IllegalArgumentException if the index is negative or if the name
     *         is <code>null</code>.
     */
    public State(String name, int index) {
        if (name == null)
            throw new IllegalArgumentException("Null state name");
        if (index < 0)
            throw new IllegalArgumentException("Invalid state index");
        this.name = name;
        this.index = index;
    }

    /**
     * Returns the name of the state.
     *
     * @return the name of the state.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the client index of the state.
     *
     * @return the client index of the state.
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * Tests if this state is equal to the given state. Two states are equal if
     * they have the same name and client index.
     *
     * @param o the object to test for equality.
     * @return <code>true</code> if the two states are equal,
     *         <code>false</code> otherwise.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof State) {
            State s = (State) o;
            return (s.index == this.index && s.name.equals(this.name));
        }
        return false;
    }

    /**
     * Returns a hash code for this <code>State</code>.
     *
     * @return a hash code for this state, equal to the client index.
     */
    public int hashCode() {
        return this.index;
    }
}
