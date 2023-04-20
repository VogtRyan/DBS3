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

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * The <code>StateRecordFactory</code> is used to construct {@link StateRecord}
 * objects that record the transition times of agents into different states.
 */
public class StateRecordFactory {
    /**
     * The default name of state zero, until a first state name is added using
     * {@link #addState}. We use the empty string since that is not a valid
     * state name in the MVISP protocol, so the default state name is distinct
     * from any state that could ever be received.
     */
    private static final String DEFAULT_STATE_NAME = "";

    /**
     * The number of agents in the state transition record.
     */
    private int numAgents;

    /**
     * The states in the state record.
     */
    private ArrayList<State> states;

    /**
     * A list of the transitions made by all agents. For each agent, there is a
     * map from times in milliseconds to state numbers.
     */
    private ArrayList<TreeMap<Long, State>> transitions;

    /**
     * Creates a new <code>StateRecordFactory</code> for the given number of
     * agents.
     *
     * @param numAgents the number of agents in the state transition record.
     * @throws IllegalArgumentException if <code>numAgents</code> is less than
     *         or equal to zero.
     */
    public StateRecordFactory(int numAgents) {
        if (numAgents <= 0)
            throw new IllegalArgumentException("Invalid number of agents");
        this.numAgents = numAgents;
        this.states = new ArrayList<State>();

        this.transitions = new ArrayList<TreeMap<Long, State>>();
        for (int i = 0; i < numAgents; i++)
            this.transitions.add(new TreeMap<Long, State>());
    }

    /**
     * Adds a new state to the state record; and, if this is the first call to
     * this method, automatically adds a transition at time zero for all agents
     * into the new state.
     *
     * @param stateName the new state name.
     * @throws IllegalArgumentException if the given state name is already in
     *         this factory.
     */
    public void addState(String stateName) {
        for (State state : this.states) {
            if (stateName.equals(state.getName()))
                throw new IllegalArgumentException("Duplicate state name");
        }

        int index = this.states.size();
        State state = new State(stateName, index);
        this.states.add(state);
        if (index != 0)
            return;
        for (int i = 0; i < this.numAgents; i++)
            this.addTransition(i, 0, 0);
    }

    /**
     * Returns the number of states that are currently in the factory.
     *
     * @return the number of states in the factory.
     */
    public int getNumStates() {
        return this.states.size();
    }

    /**
     * Adds a transition for the given agent into the given state number at the
     * given time. If a transition already exists for the given agent at the
     * given time, it is replaced with a new transition.
     *
     * @param agent the agent number, indexed from <code>0</code>.
     * @param stateNumber the state number, indexed from <code>0</code>.
     * @param milliseconds the time in milliseconds at which the transition
     *        occurs.
     * @throws IllegalArgumentException if <code>agent</code> is negative or
     *         greater than the largest agent index; if
     *         <code>stateNumber</code> is negative or greater than the largest
     *         state index currently added to the factory; or, if
     *         <code>milliseconds</code> is negative.
     */
    public void addTransition(int agent, int stateNumber, long milliseconds) {
        if (agent < 0 || agent >= this.numAgents)
            throw new IllegalArgumentException("Invalid agent index");
        if (stateNumber < 0 || stateNumber >= this.states.size())
            throw new IllegalArgumentException("Invalid state index");
        if (milliseconds < 0)
            throw new IllegalArgumentException("Invalid time");

        TreeMap<Long, State> map = this.transitions.get(agent);
        map.put(milliseconds, this.states.get(stateNumber));
    }

    /**
     * Returns the <code>StateRecord</code> built from the transitions added to
     * this factory.
     *
     * @return the constructed <code>StateRecord</code>, or <code>null</code>
     *         if no states have been added to this factory.
     */
    public StateRecord getRecord() {
        if (this.states.isEmpty())
            return null;
        ArrayList<StateRecordSingle> records =
                new ArrayList<StateRecordSingle>();
        for (int i = 0; i < this.numAgents; i++)
            records.add(new StateRecordSingle(this.transitions.get(i)));
        return new StateRecord(records, this.states);
    }

    /**
     * Returns a default <code>StateRecord</code> in which every agent begins
     * in an empty-named state at a time of <code>0</code> milliseconds.
     *
     * @param numAgents the number of agents in the state transition record.
     * @return an essentially empty <code>StateRecord</code>.
     * @throws IllegalArgumentException if <code>numAgents</code> is less than
     *         or equal to zero.
     */
    public static StateRecord getDefaultRecord(int numAgents) {
        StateRecordFactory factory = new StateRecordFactory(numAgents);
        factory.addState(StateRecordFactory.DEFAULT_STATE_NAME);
        return factory.getRecord();
    }
}
