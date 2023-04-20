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

import java.util.List;

/**
 * The <code>StateRecord</code> represents a record of all state transitions
 * that agents perform during a simulation.
 */
public class StateRecord {
    /**
     * The transition records for each individual agent.
     */
    private StateRecordSingle[] records;

    /**
     * All of the states in this record.
     */
    private State[] states;

    /**
     * Creates a new <code>StateRecord</code> consisting of all the transition
     * records for individual agents, along with a cross-reference of all of
     * the available states in the simulation.
     *
     * @param records the transition records for each individual agent.
     * @param states all of the available states in the simulation, where each
     *        state's client index must match its index in the list.
     * @throws IllegalArgumentException if any transition record contains a
     *         state not contained in <code>states</code>, if there are any
     *         duplicate state names, if the client index of a state does not
     *         match its index in <code>states</code>, or if either list is
     *         empty.
     */
    public StateRecord(List<StateRecordSingle> records, List<State> states) {
        /* Ensure indices match */
        int numRecords = records.size();
        int numStates = states.size();
        if (numRecords == 0 || numStates == 0)
            throw new IllegalArgumentException("Empty list");
        for (int i = 0; i < numStates; i++) {
            if (states.get(i).getIndex() != i)
                throw new IllegalArgumentException("Client index mismatch");
        }

        /* Check for duplicate state names */
        for (int i = 0; i < numStates; i++) {
            State si = states.get(i);
            for (int j = i + 1; j < numStates; j++) {
                State sj = states.get(j);
                if (si.getName().equals(sj.getName()))
                    throw new IllegalArgumentException("Duplicate state name");
            }
        }

        /* Ensure all states in the records appear in the states list */
        for (int i = 0; i < numRecords; i++) {
            StateRecordSingle srs = records.get(i);
            int numTrans = srs.getNumTransitions();
            for (int j = 0; j <= numTrans && j >= 0; j++) {
                State s = srs.getStateAfter(j);
                if (states.contains(s) == false)
                    throw new IllegalArgumentException(
                            "Unknown state in record");
            }
        }

        /* Convert to arrays to store */
        this.records = records.toArray(new StateRecordSingle[numRecords]);
        this.states = states.toArray(new State[numStates]);
    }

    /**
     * Returns the number of states in this record.
     *
     * @return the number of states in this record.
     */
    public int getNumStates() {
        return this.states.length;
    }

    /**
     * Returns the state with the given client index.
     *
     * @param index the client index of the state to return.
     * @return the state at that client index.
     */
    public State getState(int index) {
        return this.states[index];
    }

    /**
     * Returns the number of agents in this state record.
     *
     * @return the number of agents in the state record.
     */
    public int getNumAgents() {
        return this.records.length;
    }

    /**
     * Returns the state that the given agent would be in, under this state
     * record, at the given time.
     *
     * @param milliseconds the time at which to check the agent state.
     * @param agent the agent for which to check the state.
     * @return the agent state at the given time.
     * @throws IllegalArgumentException if the number of milliseconds is
     *         negative.
     */
    public State getStateAt(long milliseconds, int agent) {
        return this.records[agent].getStateAt(milliseconds);
    }
}
