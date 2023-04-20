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

package ca.ualberta.dbs3.math;

import java.util.LinkedList;

/**
 * The <code>MarkovChain</code> class represents a finite-state environment in
 * which transitions between states rely only on the current state.
 */
public class MarkovChain {
    /**
     * The default computational error bound for estimating the equilibrium
     * distribution of a Markov chain.
     */
    public static final double DEFAULT_EPSILON = 0.0000000000001;

    /**
     * The message to display while computing an equilibrium distribution.
     */
    private static final String EQUILIBRIUM_DESCRIPTION =
            "Computing Markov chain equilibrium distribution";

    /**
     * The transition matrix for this Markov chain.
     */
    private double[][] chain;

    /**
     * Creates a new Markov chain with the transition probabilities between
     * each pair of states determined by the probability density functions of
     * the given <code>Distribution</code>s. That is, the probability of
     * transitioning to state <code>j</code> if you are in state <code>i</code>
     * is <code>states[i].getProbability(j)</code>.
     *
     * @param states the transition probabilities from each of the given
     *        states.
     * @throws IllegalArgumentException if <code>states</code> has a length of
     *         zero, or if the number of elements in each
     *         <code>Distribution</code> is not equal to the length of
     *         <code>states</code>.
     */
    public MarkovChain(Distribution[] states) {
        /* Store the chain: the single-step probability graph */
        if (states.length == 0)
            throw new IllegalArgumentException("Invalid number of states");
        this.chain = new double[states.length][];
        for (int i = 0; i < states.length; i++) {
            this.chain[i] = states[i].getPDF();
            if (this.chain[i].length != states.length)
                throw new IllegalArgumentException("State count mismatch");
        }
    }

    /**
     * Numerically estimates the equilibrium distribution of the Markov chain,
     * to within a bound specified by {@link #DEFAULT_EPSILON}. Note that this
     * class only implements equilibrium distribution computation for ergodic
     * Markov chains; if this Markov chain is not ergodic, an
     * <code>UnsupportedOperationException</code> is thrown.
     *
     * @return the equilibrium probability of being in each state.
     * @throws UnsupportedOperationException if this Markov chain is not
     *         ergodic.
     */
    public Distribution equilibrium() {
        return this.equilibrium(MarkovChain.DEFAULT_EPSILON, null);
    }

    /**
     * Numerically estimates the equilibrium distribution of the Markov chain,
     * to within a bound specified by {@link #DEFAULT_EPSILON}. Note that this
     * class only implements equilibrium distribution computation for ergodic
     * Markov chains; if this Markov chain is not ergodic, an
     * <code>UnsupportedOperationException</code> is thrown.
     *
     * @param pm the progress monitor to watch for a cancel flag.
     * @return the equilibrium probability of being in each state, or
     *         <code>null</code> if the cancel flag is raised in the progress
     *         monitor.
     * @throws UnsupportedOperationException if this Markov chain is not
     *         ergodic.
     */
    public Distribution equilibrium(ProgressMonitor pm) {
        return this.equilibrium(MarkovChain.DEFAULT_EPSILON, pm);
    }

    /**
     * Numerically estimates the equilibrium distribution of the Markov chain,
     * to within a bound specified by <code>epsilon</code>. Note that this
     * class only implements equilibrium distribution computation for ergodic
     * Markov chains; if this Markov chain is not ergodic, an
     * <code>UnsupportedOperationException</code> is thrown.
     *
     * @param epsilon the error bound for the equilibrium computation.
     * @return the equilibrium probability of being in each state.
     * @throws UnsupportedOperationException if this Markov chain is not
     *         ergodic.
     * @throws IllegalArgumentException if <code>epsilon</code> is not greater
     *         than zero.
     */
    public Distribution equilibrium(double epsilon) {
        return this.equilibrium(epsilon, null);
    }

    /**
     * Numerically estimates the equilibrium distribution of the Markov chain,
     * to within a bound specified by <code>epsilon</code>. Note that this
     * class only implements equilibrium distribution computation for ergodic
     * Markov chains; if this Markov chain is not ergodic, an
     * <code>UnsupportedOperationException</code> is thrown.
     *
     * @param epsilon the error bound for the equilibrium computation.
     * @param pm the progress monitor to watch for a cancel flag.
     * @return the equilibrium probability of being in each state, or
     *         <code>null</code> if the cancel flag is raised in the progress
     *         monitor.
     * @throws UnsupportedOperationException if this Markov chain is not
     *         ergodic.
     * @throws IllegalArgumentException if <code>epsilon</code> is not greater
     *         than zero.
     */
    public Distribution equilibrium(double epsilon, ProgressMonitor pm) {
        if (epsilon <= 0.0)
            throw new IllegalArgumentException("Invalid error bound");

        /* Set up the progress monitor to report on the computation */
        if (pm != null) {
            if (pm.shouldCancel())
                return null;
            pm.start(MarkovChain.EQUILIBRIUM_DESCRIPTION);
            pm.updateIndeterminate();
        }

        /*
         * An ergodic Markov chain, M, is guaranteed to have an equilibrium
         * distribution that can be computed as M^\infinity. Other Markov
         * chains may have unique equilibrium distributions, which may even be
         * computed in the same manner, so this check is overly conservative.
         * However, it guarantees that we do not go into an infinite loop while
         * estimating M^\infinity.
         */
        if (this.isErgodic() == false)
            throw new UnsupportedOperationException(
                    "Markov chain is not " + "ergodic");

        /* Set up the matrix multiplication */
        double[][] mult;
        double[][] res;
        mult = new double[this.chain.length][];
        res = new double[this.chain.length][];
        for (int i = 0; i < this.chain.length; i++) {
            mult[i] = new double[this.chain.length];
            res[i] = new double[this.chain.length];
            for (int j = 0; j < this.chain.length; j++)
                res[i][j] = this.chain[i][j];
        }

        /* Repeat matrix multiplication until the error bound is reached */
        double[] equilibrium = new double[this.chain.length];
        while (true) {
            if (this.equilibriumDone(res, epsilon, equilibrium)) {
                if (pm != null)
                    pm.end();
                return new Distribution(equilibrium);
            }
            double[][] temp = res;
            res = mult;
            mult = temp;
            if (pm != null && pm.shouldCancel())
                return null;
            this.multiply(mult, res);
        }
    }

    /**
     * Tests is this <code>MarkovChain</code> is equal to another
     * <code>MarkovChain</code>. Two <code>MarkovChain</code>s are equal if
     * they have the same number of states, and identical transition
     * probabilities between the two states at the same index.
     *
     * @param o the object for which to test equality.
     * @return <code>true</code> if the two <code>MarkovChain</code>s are the
     *         same, otherwise <code>false</code>.
     */
    public boolean equals(Object o) {
        if ((o == null) || (!(o instanceof MarkovChain)))
            return false;
        MarkovChain other = (MarkovChain) o;

        if (this.chain.length != other.chain.length)
            return false;
        for (int i = 0; i < this.chain.length; i++) {
            for (int j = 0; j < this.chain.length; j++) {
                if (this.chain[i][j] != other.chain[i][j])
                    return false;
            }
        }
        return true;
    }

    /**
     * Returns a hash code for this <code>MarkovChain</code>.
     *
     * @return a hash code for this <code>MarkovChain</code>, equal to the hash
     *         code of the self-transition probability of state zero.
     */
    public int hashCode() {
        return Double.valueOf(this.chain[0][0]).hashCode();
    }

    /**
     * Tests whether the given multi-step transition matrix has reached within
     * <code>epsilon</code> of its equilibrium distribution, and fills the
     * equilibrium vector if it has. Note that the equilibrium vector may be
     * modified even if the equilibrium matrix has not yet been reached, and
     * that the resulting equilibrium vector may not add up to exactly
     * <code>1.0</code> due to rounding errors.
     *
     * @param res the multi-step transition matrix to test.
     * @param epsilon the error bound of the equilibrium computation.
     * @param equilibrium the equilibrium distribution vector to fill.
     * @return <code>true</code> if equilibrium matrix has been reached,
     *         <code>false</code> otherwise.
     */
    private boolean equilibriumDone(double[][] res, double epsilon,
            double[] equilibrium) {
        double min, max;

        for (int onCol = 0; onCol < res.length; onCol++) {
            max = res[0][onCol];
            min = max;
            for (int onRow = 1; onRow < res.length; onRow++) {
                if (res[onRow][onCol] > max)
                    max = res[onRow][onCol];
                else if (res[onRow][onCol] < min)
                    min = res[onRow][onCol];
                if (max - min > epsilon)
                    return false;
            }
            equilibrium[onCol] = min + (max - min) * 0.5;
        }
        return true;
    }

    /**
     * Compute the matrix multiplication <code>src * src</code> and store the
     * result in <code>dst</code>.
     *
     * @param src the matrix to square.
     * @param dst the location in which to store the result.
     */
    private void multiply(double[][] src, double[][] dst) {
        for (int row = 0; row < src.length; row++) {
            for (int col = 0; col < src.length; col++) {
                dst[row][col] = 0.0;
                for (int i = 0; i < src.length; i++)
                    dst[row][col] += src[row][i] * src[i][col];
            }
        }
    }

    /**
     * Tests whether this Markov chain is ergodic. A Markov chain is ergodic if
     * it is irreducible (also called strongly connected) and aperiodic.
     *
     * @return <code>true</code> if the Markov chain is ergodic, otherwise
     *         <code>false</code>.
     */
    private boolean isErgodic() {
        /*
         * Check if the transition function represents a strongly connected
         * graph (i.e., a directed graph in which there is a path from u to v
         * and a path from v to u for any pair of states u and v) using the
         * following algorithm:
         *
         * 1. Start a BFS at state 0, and ensure all states are visited (i.e.,
         * from state 0, you can reach every state other than state 0); and,
         * 
         * 2. Start a BFS at state 0 on the graph G^T (i.e., the graph in which
         * all the directed edges are reversed) and ensure that all states are
         * visited (i.e., from any state other than state 0, you can reach
         * state 0).
         *
         * We piggyback the aperiodicity check onto the first BFS.
         */
        int period = this.computePeriod();
        if (period != 1)
            return false;
        return this.reverseBFSReachesAll();
    }

    /**
     * Performs a breadth-first search starting at state zero, and uses that
     * search to compute the period of the Markov chain (assuming that the
     * chain is irreducible).
     *
     * @return the period of the Markov chain, assuming irreducibility, or
     *         <code>-1</code> if the BFS does not reach all of the states.
     */
    private int computePeriod() {
        int[] level = new int[this.chain.length];
        LinkedList<Integer> queue = new LinkedList<Integer>();
        int period = -1;

        /* Queue the start state */
        int toFind = level.length - 1;
        level[0] = 0;
        for (int i = 1; i < level.length; i++)
            level[i] = -1;
        queue.offer(0);

        /*
         * The period of the Markov chain (assuming irreducibility) is computed
         * as gcd{val(e) > 0 | e \in E, e \notin T}, where E is the set of all
         * edges found in the BFS, and T is the set of all tree edges found in
         * the BFS. The value of an edge e from state i to state j, val(e), is
         * level(i) - level(j) + 1.
         *
         * See "Graph-Theoretic Analysis of Finite Markov Chains", Jarvis and
         * Shier, Chapter 17, p. 12.
         */
        while (period != 1 || toFind != 0) {
            Integer src = queue.poll();
            if (src == null)
                break;

            for (int dst = 0; dst < level.length; dst++) {
                if (this.chain[src][dst] <= 0.0)
                    continue;
                if (level[dst] == -1) {
                    level[dst] = level[src] + 1;
                    toFind--;
                    queue.offer(dst);
                } else if (period != 1) {
                    int val = level[src] - level[dst] + 1;
                    if (period == -1)
                        period = val;
                    else
                        period = this.gcd(period, val);
                }
            }
        }

        /*
         * If the BFS reached all states, return the period (computed under the
         * assumption that the Markov chain is irreducible, i.e., that a
         * reverse BFS would reach all states). Otherwise, return a failure
         * code.
         */
        if (toFind != 0)
            return -1;
        else
            return period;
    }

    /**
     * Performs a breadth-first search starting at state zero, on a transition
     * graph in which inbound edges are treated as outbound edges (and vice
     * versa), and tests if every state is reached.
     *
     * @return <code>true</code> if all states were reached, <code>false</code>
     *         otherwise.
     */
    private boolean reverseBFSReachesAll() {
        boolean[] reached = new boolean[this.chain.length];
        LinkedList<Integer> queue = new LinkedList<Integer>();
        int toFind;

        /* Queue the start state */
        toFind = reached.length - 1;
        reached[0] = true;
        for (int i = 1; i < reached.length; i++)
            reached[i] = false;
        queue.offer(0);

        /* Dequeue elements to reach new states */
        while (toFind > 0) {
            Integer src = queue.poll();
            if (src == null)
                return false;

            for (int dst = 0; dst < reached.length; dst++) {
                if (reached[dst])
                    continue;
                if (this.chain[dst][src] > 0.0) {
                    reached[dst] = true;
                    toFind--;
                    queue.offer(dst);
                }
            }
        }

        return true;
    }

    /**
     * Returns the greatest common divisor of two positive numbers.
     *
     * @param a the first number.
     * @param b the second number.
     * @return the GCD of the first and second number.
     * @throws IllegalArgumentException if either number is less than or equal
     *         to zero.
     */
    private int gcd(int a, int b) {
        if (a <= 0 || b <= 0)
            throw new IllegalArgumentException("Invalid numbers to find GCD");
        while (b > 0) {
            int newA = b;
            b = a % b;
            a = newA;
        }
        return a;
    }
}
