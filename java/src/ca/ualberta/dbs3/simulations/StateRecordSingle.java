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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * The <code>StateRecordSingle</code> class records the times at which state
 * transitions happen for a single agent.
 */
public class StateRecordSingle {
    /**
     * All of the times, in milliseconds, at which state transitions occur.
     * This array is sorted, guaranteed to have length at least 1, and the
     * first entry is guaranteed to be 0L.
     */
    private long[] recordedTimes;

    /**
     * The states into which to transition at the corresponding times in the
     * recorded times array. Guaranteed to have length at least 1.
     */
    private State[] recordedStates;

    /**
     * The index into the recorded times array, representing the time at which
     * the most recently queried state transition occured.
     */
    private int cachedIndex;

    /**
     * Creates a new <code>StateRecordSingle</code> from the given map of times
     * in milliseconds to states. The map must have at least one entry for the
     * time of zero milliseconds.
     *
     * @param map the map from times in milliseconds to states.
     * @throws IllegalArgumentException if the map does not contain an entry
     *         for the time of zero milliseconds, or if any of the times are
     *         negative.
     */
    public StateRecordSingle(SortedMap<Long, State> map) {
        if (map.size() == 0)
            throw new IllegalArgumentException("No zero-time entry");
        this.cachedIndex = 0;

        /*
         * Find the number of meaningful transitions in the sorted map (i.e.,
         * where the old state and new state are unique).
         */
        Set<Map.Entry<Long, State>> entrySet = map.entrySet();
        int numEntries = 0;
        State prevState = null;
        for (Map.Entry<Long, State> entry : entrySet) {
            State value = entry.getValue();
            if (entry.getKey() < 0)
                throw new IllegalArgumentException("Negative time");
            if (prevState == null || value.equals(prevState) == false) {
                numEntries++;
                prevState = value;
            }
        }

        /*
         * Record all of the meaningful transitions.
         */
        this.recordedTimes = new long[numEntries];
        this.recordedStates = new State[numEntries];
        int i = 0;
        prevState = null;
        for (Map.Entry<Long, State> entry : entrySet) {
            State value = entry.getValue();
            if (prevState == null || value.equals(prevState) == false) {
                this.recordedTimes[i] = entry.getKey();
                this.recordedStates[i] = value;
                i++;
                prevState = value;
            }
        }

        /*
         * Verify that a zero-time entry exists.
         */
        if (this.recordedTimes[0] != 0L)
            throw new IllegalArgumentException("No zero-time entry");
    }

    /**
     * Returns the number of state transitions performed in this record.
     *
     * @return the number of state transitions.
     */
    public int getNumTransitions() {
        return this.recordedStates.length - 1;
    }

    /**
     * Returns the state that the agent will be in after the given number of
     * state transitions.
     *
     * @param transitions the number of state transitions performed.
     * @return the state after the given number of transitions.
     * @throws ArrayIndexOutOfBoundsException if the given number is negative
     *         or greater than the number of transitions performed, as per
     *         {@link #getNumTransitions}.
     */
    public State getStateAfter(int transitions) {
        return this.recordedStates[transitions];
    }

    /**
     * Returns the state that an agent would be in, under this transition
     * record, at the given time.
     *
     * @param milliseconds the time at which to check the agent state.
     * @return the agent state at the given time.
     * @throws IllegalArgumentException if the number of milliseconds is
     *         negative.
     */
    public State getStateAt(long milliseconds) {
        if (milliseconds < 0)
            throw new IllegalArgumentException("Negative time");

        int minSearch, maxSearch;
        int maxIndex = this.recordedTimes.length - 1;

        if (this.recordedTimes[this.cachedIndex] <= milliseconds) {
            /*
             * First, check if the requested time is at the same index into our
             * array of recorded times as the previous query (which will be the
             * case the majority of the time if you are playing back a
             * simulation).
             */
            if (this.cachedIndex == maxIndex
                    || milliseconds < this.recordedTimes[this.cachedIndex + 1])
                return this.recordedStates[this.cachedIndex];

            /*
             * Another common case will be the simulation advancing by one
             * index during playback.
             */
            if (this.cachedIndex + 1 == maxIndex
                    || milliseconds < this.recordedTimes[this.cachedIndex
                            + 2]) {
                this.cachedIndex++;
                return this.recordedStates[this.cachedIndex];
            }

            /*
             * Otherwise, we know the new index is somewhere in
             * [cachedIndex+2, maxIndex].
             */
            minSearch = this.cachedIndex + 2;
            maxSearch = maxIndex;
        } else {
            /*
             * Here, the new index is somewhere in [0, cachedIndex-1].
             */
            minSearch = 0;
            maxSearch = this.cachedIndex - 1;
        }

        /*
         * Perform a binary search in the range [minSearch, maxSearch] for the
         * given time. Return will be >= 0 iff the time exists in our recorded
         * time array, or will return (-p - 1), where p is the insertion point
         * (i.e., the point after our new time index).
         */
        int sRes = Arrays.binarySearch(this.recordedTimes, minSearch,
                maxSearch + 1, milliseconds);
        if (sRes >= 0)
            this.cachedIndex = sRes;
        else
            this.cachedIndex = (-sRes) - 2;
        return this.recordedStates[this.cachedIndex];
    }
}
