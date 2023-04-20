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

import ca.ualberta.dbs3.cartography.*;
import ca.ualberta.dbs3.math.*;
import ca.ualberta.dbs3.network.UnsignedInteger;

/**
 * The <code>Simulation</code> class represents a single simulation run of
 * agents moving around on a {@link Map}.
 */
public class Simulation {
    /**
     * The number of threads to use for initializing a simulation.
     */
    private static int INIT_THREADS =
            Runtime.getRuntime().availableProcessors();

    /**
     * The mobile agents involved in the simulation.
     */
    private Agent[] agents;

    /**
     * The total duration of the simulation in seconds.
     */
    private double duration;

    /**
     * The range of speeds at which agents can move.
     */
    private Range speed;

    /**
     * The range of pause times the agents may use, in seconds.
     */
    private Range pause;

    /**
     * The destination selection algorithm used by agents to choose new
     * destinations.
     */
    private DestinationChooser destChooser;

    /**
     * The pathfinder used by agents to move to new destinations.
     */
    private Pathfinder pathfinder;

    /**
     * Creates a new <code>Simulation</code> according to the given parameters.
     *
     * @param numAgents the number of agents with which to populate the map.
     * @param speed the range of speeds at which agents may move, in metres per
     *        second.
     * @param pause the range of pause times the agents may use, in seconds.
     * @param duration the duration of the simulation in seconds.
     * @param destChooser the destination selection algorithm that agents will
     *        use.
     * @param pathfinder the pathfinding algorithm that agents will use.
     * @param seed the psuedorandom number generator seed to use for this
     *        simulation.
     * @throws IllegalArgumentException if the given speeds can be negative, or
     *         if the duration of the simulation is less than zero, or if the
     *         number of agents is not greater than zero.
     */
    public Simulation(int numAgents, Range speed, Range pause, double duration,
            DestinationChooser destChooser, Pathfinder pathfinder, long seed) {
        this(numAgents, speed, pause, duration, destChooser, pathfinder, seed,
                null);
    }

    /**
     * Creates a new <code>Simulation</code> according to the given parameters.
     *
     * @param numAgents the number of agents with which to populate the map.
     * @param speed the range of speeds at which agents may move, in metres per
     *        second.
     * @param pause the range of pause times the agents may use, in seconds.
     * @param durationMilli the duration of the simulation in milliseconds.
     * @param destChooser the destination selection algorithm that agents will
     *        use.
     * @param pathfinder the pathfinding algorithm that agents will use.
     * @param seed the psuedorandom number generator seed to use for this
     *        simulation.
     * @throws IllegalArgumentException if the given speeds can be negative, or
     *         if the number of agents is not greater than zero.
     */
    public Simulation(int numAgents, Range speed, Range pause,
            UnsignedInteger durationMilli, DestinationChooser destChooser,
            Pathfinder pathfinder, long seed) {
        this(numAgents, speed, pause, durationMilli, destChooser, pathfinder,
                seed, null);
    }

    /**
     * Creates a new <code>Simulation</code> according to the given parameters.
     *
     * @param numAgents the number of agents with which to populate the map.
     * @param speed the range of speeds at which agents may move, in metres per
     *        second.
     * @param pause the range of pause times the agents may use, in seconds.
     * @param duration the duration of the simulation in seconds.
     * @param destChooser the destination selection algorithm that agents will
     *        use.
     * @param pathfinder the pathfinding algorithm that agents will use.
     * @param seed the psuedorandom number generator seed to use for this
     *        simulation.
     * @param pm the progress monitor to receive updates on the construction of
     *        this simulation, which may be <code>null</code>. Note that if the
     *        cancel flag is raised in the progress monitor, the simulation
     *        will be left in an inconsistent state with almost-certainly
     *        erroneous behaviour.
     * @throws IllegalArgumentException if the given speeds can be negative, or
     *         if the duration of the simulation is less than zero, or if the
     *         number of agents is not greater than zero.
     */
    public Simulation(int numAgents, Range speed, Range pause, double duration,
            DestinationChooser destChooser, Pathfinder pathfinder, long seed,
            ProgressMonitor pm) {
        /* Check and initialize simulation parameters */
        if (speed.getMin() < 0.0)
            throw new IllegalArgumentException("Invalid speeds");
        if (pause.getMin() < 0.0)
            throw new IllegalArgumentException("Invalid pause times");
        if (duration < 0.0)
            throw new IllegalArgumentException("Negative duration");
        if (numAgents <= 0)
            throw new IllegalArgumentException("Invalid number of agents");
        this.agents = new Agent[numAgents];
        this.duration = duration;
        this.speed = speed;
        this.pause = pause;
        this.destChooser = destChooser;
        this.pathfinder = pathfinder;
        SeedGenerator sGen = new SeedGenerator(seed);

        /* Initialize simulation in parallel */
        int numThreads = numAgents < Simulation.INIT_THREADS ? numAgents
                : Simulation.INIT_THREADS;
        TaskCounter counter = new TaskCounter(numAgents);
        if (pm != null) {
            if (pm.shouldCancel())
                return;
            pm.start("Initializing simulation");
            pm.update(0, numAgents);
        }
        synchronized (counter) {
            for (int i = 0; i < numThreads; i++) {
                Thread initThread = new AgentInit(sGen, counter, pm);
                initThread.start();
            }
            while (counter.isComplete() == false) {
                try {
                    counter.wait();
                } catch (InterruptedException iex) {
                }
                if (pm != null && pm.shouldCancel())
                    return;
            }
        }
        if (pm != null && pm.shouldCancel() == false)
            pm.end();
    }

    /**
     * Creates a new <code>Simulation</code> according to the given parameters.
     *
     * @param numAgents the number of agents with which to populate the map.
     * @param speed the range of speeds at which agents may move, in metres per
     *        second.
     * @param pause the range of pause times the agents may use, in seconds.
     * @param durationMilli the duration of the simulation in milliseconds.
     * @param destChooser the destination selection algorithm that agents will
     *        use.
     * @param pathfinder the pathfinding algorithm that agents will use.
     * @param seed the psuedorandom number generator seed to use for this
     *        simulation.
     * @param pm the progress monitor to receive updates on the construction of
     *        this simulation, which may be <code>null</code>. Note that if the
     *        cancel flag is raised in the progress monitor, the simulation
     *        will be left in an inconsistent state with almost-certainly
     *        erroneous behaviour.
     * @throws IllegalArgumentException if the given speeds can be negative, or
     *         if the number of agents is not greater than zero.
     */
    public Simulation(int numAgents, Range speed, Range pause,
            UnsignedInteger durationMilli, DestinationChooser destChooser,
            Pathfinder pathfinder, long seed, ProgressMonitor pm) {
        this(numAgents, speed, pause,
                ((double) (durationMilli.toLong())) / 1000.0, destChooser,
                pathfinder, seed, pm);
    }

    /**
     * Creates a new <code>Simulation</code> that is a deep copy of the given
     * simulation.
     *
     * @param simulation the simulation to copy.
     */
    public Simulation(Simulation simulation) {
        this.agents = new Agent[simulation.agents.length];
        for (int i = 0; i < this.agents.length; i++)
            this.agents[i] = new Agent(simulation.agents[i]);
        this.duration = simulation.duration;
        this.speed = simulation.speed;
        this.pause = simulation.pause;
        this.destChooser = simulation.destChooser;
        this.pathfinder = simulation.pathfinder;
    }

    /**
     * Returns the number of agents in the simulation.
     *
     * @return the number of agents in the simulation.
     */
    public int getNumAgents() {
        return this.agents.length;
    }

    /**
     * Returns the total duration of the simulation, in seconds.
     *
     * @return the duration of the simulation in seconds.
     */
    public double getDuration() {
        return this.duration;
    }

    /**
     * Returns the current amount of time for which the given agent has moved,
     * in seconds.
     *
     * @param agent the index of the agent in question.
     * @return the amount of time for which that agent has moved.
     * @throws ArrayIndexOutOfBoundsException if <code>id</code> is negative or
     *         greater than or equal to the number of agents.
     */
    public double getCurrentTime(int agent) {
        double t = this.agents[agent].getTimeSimulated();

        /* Numerical precision fix */
        if (t > this.duration)
            return this.duration;
        else
            return t;
    }

    /**
     * Returns the current location of the given agent, in metres.
     *
     * @param agent the index of the agent in question.
     * @return the agent's location.
     * @throws ArrayIndexOutOfBoundsException if <code>id</code> is negative or
     *         greater than or equal to the number of agents.
     */
    public Point getCurrentLocation(int agent) {
        return this.agents[agent].getCurrentLocation();
    }

    /**
     * Returns the ultimate destination to which the agent is currently
     * travelling, in metres.
     *
     * @param agent the index of the agent in question.
     * @return the ultimate destination of the agent.
     * @throws ArrayIndexOutOfBoundsException if <code>id</code> is negative or
     *         greater than or equal to the number of agents.
     */
    public Point getDestination(int agent) {
        return this.agents[agent].getDestination();
    }

    /**
     * Advances the given agent's movement to the next point at which it
     * changes speed or direction or to the end of the simulation.
     *
     * @param agent the index of the agent in question.
     * @return <code>true</code> if the agent's movement was successfully
     *         advanced, or <code>false</code> if the simulation has ended for
     *         that agent.
     * @throws ArrayIndexOutOfBoundsException if <code>id</code> is negative or
     *         greater than or equal to the number of agents.
     */
    public boolean advance(int agent) {
        double currentTime = this.agents[agent].getTimeSimulated();
        if (currentTime >= this.duration)
            return false;

        double maxMove = this.duration - currentTime;
        double eta = this.agents[agent].maxAdvanceTime();

        if (eta < maxMove)
            this.agents[agent].advance(eta);
        else
            this.agents[agent].advance(maxMove);
        return true;
    }

    /**
     * Sets the agent at the given identification index to be the given agent.
     *
     * @param id the identification index of the agent.
     * @param agent a reference to the new agent.
     * @throws ArrayIndexOutOfBoundsException if <code>id</code> is negative or
     *         greater than or equal to the number of agents.
     */
    public void setAgent(int id, Agent agent) {
        this.agents[id] = agent;
    }

    /**
     * Returns a reference to the agent at the given identification index.
     *
     * @param id the identification index of the agent.
     * @return a reference to the agent.
     * @throws ArrayIndexOutOfBoundsException if <code>id</code> is negative or
     *         greater than or equal to the number of agents.
     */
    public Agent getAgent(int id) {
        return this.agents[id];
    }

    /**
     * Returns the range of speeds at which agents may move.
     *
     * @return the speed range.
     */
    public Range getSpeedRange() {
        return this.speed;
    }

    /**
     * Returns the range of seconds for which agents may pause.
     *
     * @return the pause time range.
     */
    public Range getPauseRange() {
        return this.pause;
    }

    /**
     * Returns the destination chooser that agents in this simulation should
     * use.
     *
     * @return the destination chooser.
     */
    public DestinationChooser getDestinationChooser() {
        return this.destChooser;
    }

    /**
     * Returns the pathfinder that agents in this simulation should use.
     *
     * @return the pathfinder.
     */
    public Pathfinder getPathfinder() {
        return this.pathfinder;
    }

    /**
     * Returns the number of threads used to initialize a simulation.
     *
     * @return the number of threads used.
     */
    public static int getInitializationThreads() {
        return Simulation.INIT_THREADS;
    }

    /**
     * Sets the number of threads used to initialize a simulation.
     *
     * @param num the number of threads to be used.
     * @throws IllegalArgumentException if the given number of threads is zero
     *         or negative.
     */
    public static void setInitializationThreads(int num) {
        if (num <= 0)
            throw new IllegalArgumentException("Invalid number of threads");
        Simulation.INIT_THREADS = num;
    }

    /**
     * The <code>TaskCounter</code> class is responsible for handing out tasks
     * labelled from <code>0</code> to <code>n-1</code>.
     */
    private class TaskCounter {
        /**
         * The next task to hand out.
         */
        private int nextHandout;

        /**
         * The number of tasks reported back as completed.
         */
        private int numCompleted;

        /**
         * The total number of tasks.
         */
        private int total;

        /**
         * Creates a new <code>TaskCounter</code> that will hand out the given
         * number of tasks.
         *
         * @param total the total number of tasks to hand out.
         * @throws IllegalArgumentException if the number of tasks is less than
         *         or equal to zero.
         */
        public TaskCounter(int total) {
            if (total <= 0)
                throw new IllegalArgumentException("Invalid number of tasks");
            this.nextHandout = 0;
            this.numCompleted = 0;
            this.total = total;
        }

        /**
         * Returns whether all the tasks have been reported as completed.
         *
         * @return <code>true</code> if all tasks have been reported as
         *         completed, otherwise <code>false</code>.
         */
        public boolean isComplete() {
            return (this.numCompleted == this.total);
        }

        /**
         * Returns the next task to be handed out, or <code>-1</code> if there
         * are no more tasks to hand out.
         *
         * @return the next task, or <code>-1</code>.
         */
        public int getTask() {
            if (this.nextHandout == this.total)
                return -1;
            int ret = this.nextHandout;
            this.nextHandout++;
            return ret;
        }

        /**
         * Informs the task counter that one of the tasks handed out has been
         * completed.
         *
         * @return the number of tasks reported as completed in total.
         */
        public int taskComplete() {
            if (this.numCompleted == this.total)
                throw new IllegalStateException("Too many tasks complete");
            this.numCompleted++;
            return this.numCompleted;
        }

        /**
         * Returns the total number of tasks that will be handed out.
         *
         * @return the total number of tasks.
         */
        public int getTotal() {
            return this.total;
        }
    }

    /**
     * The <code>AgentInit</code> thread is a thread responsible for
     * initializing agents in a simulation.
     */
    private class AgentInit extends Thread {
        /**
         * The seed generator that produces the random seed for each agent.
         */
        private SeedGenerator sGen;

        /**
         * The counter that keeps track of all the agents left to initialize.
         */
        private TaskCounter counter;

        /**
         * The progress monitor that allows the user to cancel, or
         * <code>null</code>.
         */
        private ProgressMonitor pm;

        /**
         * Creates a new agent initialization thread.
         *
         * @param sGen the seed generator that produces agent seeds.
         * @param counter the counter of all initialization tasks.
         * @param pm the progress monitor or <code>null</code>.
         */
        public AgentInit(SeedGenerator sGen, TaskCounter counter,
                ProgressMonitor pm) {
            this.sGen = sGen;
            this.counter = counter;
            this.pm = pm;
        }

        /**
         * Runs the thread until all agents are initialized.
         */
        public void run() {
            int id;
            long seed;
            int total = this.counter.getTotal();

            while (true) {
                /*
                 * Get the next task, and produce the seed before anyone gets
                 * any other tasks, ensuring consistency in generated seeds.
                 */
                synchronized (this.counter) {
                    if (this.pm != null && this.pm.shouldCancel()) {
                        this.counter.notify(); /* Unlock the main thread */
                        return;
                    }

                    id = this.counter.getTask();
                    if (id == -1)
                        return;
                    seed = sGen.nextSeed();
                }

                Random prng = new Random(seed);
                agents[id] = new Agent(Simulation.this, prng);

                synchronized (this.counter) {
                    int done = this.counter.taskComplete();
                    if (this.pm != null)
                        this.pm.update(done, total);
                    if (done == total) {
                        this.counter.notify(); /* Unlock the main thread */
                        return;
                    }
                }
            }
        }
    }
}
