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

import ca.ualberta.dbs3.commandLine.*;
import ca.ualberta.dbs3.network.*;
import ca.ualberta.dbs3.simulations.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * The <code>ServerThread</code> class contains code shared by server threads
 * of UAMP and MVISP servers.
 */
public abstract class ServerThread extends Thread {
    /**
     * The versions of the UAMP protocol supported. We support only version 2,
     * indicated with the first bit set.
     */
    private static final byte SUPPORTED_VERSION = (byte) 0x80;

    /**
     * The flags sent by the server. We do not send 3D data or data with
     * additions and removals, so no flags are set.
     */
    private static final byte[] UAMP_FLAGS =
            {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};

    /**
     * The maximal number of worker threads that will be launched by a server
     * thread.
     */
    private static int WORKER_THREADS =
            Runtime.getRuntime().availableProcessors();

    /**
     * The data input stream from the socket connection.
     */
    protected DataInputStream dis;

    /**
     * The data output stream for the socket connection.
     */
    protected DataOutputStream dos;

    /**
     * The port on which potentially to open a server.
     */
    private int port;

    /**
     * The server socket potentially opened by this thread, if non-null.
     */
    private ServerSocket server;

    /**
     * The inbound connection for this thread to deal with.
     */
    private Socket connection;

    /**
     * Whether the thread has completed naturally and error-free.
     */
    private boolean completed;

    /**
     * Whether the thread has been terminated by a kill command.
     */
    private boolean killed;

    /**
     * Whether the thread has come to an end by error.
     */
    private Throwable error;

    /**
     * Whether or not this thread has begun executing.
     */
    private boolean started;

    /**
     * The listener registered to this <code>ServerThread</code>.
     */
    private ServerThreadListener listener;

    /**
     * The manager responsible for responding to location requests.
     */
    private Manager manager;

    /**
     * Creates a new <code>ServerThread</code> that will process the
     * preexisting socket connection.
     *
     * @param connection the connection to process.
     */
    public ServerThread(Socket connection) {
        this.port = 0;
        this.server = null;
        this.connection = connection;
        this.dis = null;
        this.dos = null;
        this.completed = false;
        this.killed = false;
        this.error = null;
        this.started = false;
        this.listener = new DefaultServerThreadListener();
        this.manager = null;
    }

    /**
     * Creates a new <code>ServerThread</code> that will open a server on the
     * given port and deal with a single incoming connection before closing the
     * server.
     *
     * @param port the port on which to open the server.
     */
    public ServerThread(int port) {
        this.port = port;
        this.server = null;
        this.connection = null;
        this.dis = null;
        this.dos = null;
        this.completed = false;
        this.killed = false;
        this.error = null;
        this.started = false;
        this.listener = new DefaultServerThreadListener();
        this.manager = null;
    }

    /**
     * Sets the registered listener for this thread to the given listener. By
     * default, each <code>ServerThread</code> uses a
     * <code>ServerThreadListener</code> that prints errors to standard error
     * but does nothing else.
     *
     * @param listener the new listener for this thread.
     * @throws IllegalArgumentException if <code>listener</code> is
     *         <code>null</code>.
     * @throws IllegalStateException if the thread has started running.
     */
    public void setListener(ServerThreadListener listener) {
        if (listener == null)
            throw new IllegalArgumentException("Null listener");

        synchronized (this) {
            if (this.started)
                throw new IllegalStateException("Thread is running");
            this.listener = listener;
        }
    }

    /**
     * Runs the server thread and informs the registered
     * {@link ServerThreadListener} of how the thread terminates.
     */
    public void run() {
        /*
         * Initialize the streams and run the server protocol. We do not need
         * to check the value of this.killed inside a lock; checking it here in
         * this fashion just ensures that initializeStreams() wasn't
         * interrupted, leaving the data streams in a partially initialized
         * state.
         */
        synchronized (this) {
            this.started = true;
        }
        try {
            this.initializeStreams();
            if (this.killed == false)
                this.runProtocol();
        } catch (Throwable e) {
            synchronized (this) {
                if (this.killed == false)
                    this.error = e;
            }
        }

        /*
         * Regardless of the outcome of the protocol, close all of the
         * resources associated with it.
         */
        try {
            this.closeStreams();
        } catch (Throwable e) {
            synchronized (this) {
                if (this.killed == false && this.error == null)
                    this.error = e;
            }
        }
        synchronized (this) {
            if (this.killed == false && this.manager != null)
                this.manager.terminate();
        }

        /*
         * With everything complete, set the completed flag to true if it was
         * neither an error nor a kill command that ended the server's run.
         */
        synchronized (this) {
            if (this.killed == false && this.error == null)
                this.completed = true;
        }

        /*
         * One and only one of these three conditions is guaranteed to be true:
         * - The completed flag is set;
         * - The killed flag is set; or,
         * - The error message is set.
         * Inform the listener which way the server thread ended its run.
         */
        if (this.completed)
            this.listener.serverThreadCompleted(this);
        else if (this.killed)
            this.listener.serverThreadKilled(this);
        else
            this.listener.serverThreadError(this, this.error);
    }

    /**
     * Kills the thread, interrupting any operations taking place.
     */
    public void killThread() {
        synchronized (this) {
            if (this.completed || this.killed || this.error != null)
                return;
            this.killed = true;
            if (this.manager != null)
                this.manager.terminate();
        }

        try {
            this.closeStreams();
            this.interrupt();
        } catch (IOException ioe) {
        }
    }

    /**
     * Returns whether or not the thread completed successfully.
     *
     * @return <code>true</code> if the thread has completed, and the execution
     *         came to an error-free, non-killed conclusion. Otherwise, returns
     *         <code>false</code>.
     */
    public synchronized boolean isSuccessfullyCompleted() {
        return this.completed;
    }

    /**
     * Returns the four bytes to send to the client at the beginning of the
     * initialization phase.
     *
     * @return the four bytes to send.
     */
    protected abstract byte[] getIDBytes();

    /**
     * Parses the four bytes received from the client during the initialization
     * phase.
     *
     * @param clientID the four identifying bytes sent by the client.
     * @throws UAMPException if this server cannot interact with the connecting
     *         client.
     */
    protected abstract void parseIDBytes(byte[] clientID) throws UAMPException;

    /**
     * Called by the {@link #run} method to get the simulation that will be
     * executed.
     *
     * @return the simulation that will be executed.
     * @throws IOException if there is an error reading from or writing to the
     *         socket.
     * @throws UAMPException if there is an error executing the UAMP or MVISP
     *         protocol.
     */
    protected abstract SimulationDiscrete getSimulation()
            throws IOException, UAMPException;

    /**
     * Called by the {@link #run} method after reading a
     * <code>CHANGE_STATE</code> command (the byte <code>0x02</code> followed
     * by the number of state changes) off the socket during the update phase
     * of the protocol.
     *
     * @param num the number of state change messages to follow.
     * @throws IOException if there is an error reading from or writing to the
     *         socket.
     * @throws UAMPException if there is an error executing the UAMP or MVISP
     *         protocol.
     */
    protected abstract void parseStateChange(UnsignedInteger num)
            throws IOException, UAMPException;

    /**
     * Returns the number of worker threads used to advance a newly constructed
     * simulation.
     *
     * @return the number of threads used.
     */
    public static int getWorkerThreads() {
        return ServerThread.WORKER_THREADS;
    }

    /**
     * Sets the number of threads used to advance a newly constructed
     * simulation.
     *
     * @param num the number of threads to be used.
     * @throws IllegalArgumentException if the given number of threads is zero
     *         or negative.
     */
    public static void setWorkerThreads(int num) {
        if (num <= 0)
            throw new IllegalArgumentException("Invalid number of threads");
        ServerThread.WORKER_THREADS = num;
    }

    /**
     * Runs the UAMP or MVISP protocol.
     *
     * @throws IOException if there is an error reading or writing from the
     *         socket.
     * @throws UAMPException if there is an error executing the UAMP or MVISP
     *         protocol.
     */
    private void runProtocol() throws IOException, UAMPException {
        /* Perform the exchange and get the simulation */
        this.initializationPhase();
        SimulationDiscrete sim = this.getSimulation();
        int numAgents = sim.getNumAgents();
        this.listener.serverThreadExchange(this, numAgents,
                sim.getDuration().toLong());

        /* Start the manager that will process location requests */
        int numThreads = ServerThread.WORKER_THREADS < numAgents
                ? ServerThread.WORKER_THREADS
                : numAgents;
        synchronized (this) {
            if (this.killed == false) {
                if (numThreads <= 1)
                    this.manager = new ManagerSimple(sim);
                else
                    this.manager = new ManagerMulti(sim, numThreads);
                this.manager.startManager();
            }
        }

        while (this.killed == false) {
            /* Read in the client's next command */
            BufferReader br = new BufferReader(this.dis, 5);
            byte cmd = br.readByte();
            UnsignedInteger cmdNum = br.readUnsignedInt();
            if (cmd == (byte) 0x00 && cmdNum.toLong() == 0L)
                return; /* TERMINATE_SIMULATION command */

            /*
             * The last four bytes of the command represent the size of the
             * command, i.e., the number of location requests or state changes.
             */
            if (cmd == (byte) 0x01)
                this.parseLocationRequest(cmdNum, numAgents);
            else if (cmd == (byte) 0x02)
                this.parseStateChange(cmdNum);
            else
                throw new UAMPException("Unknown command in update phase");
        }
    }

    /**
     * Runs the handshake defined by the initialization phase.
     *
     * @throws IOException if there is an error reading or writing from the
     *         socket.
     * @throws UAMPException if there is an error executing the UAMP or MVISP
     *         protocol.
     */
    private void initializationPhase() throws IOException, UAMPException {
        /* Allocated space for client replies */
        byte[] cID = new byte[4];
        byte cVer;

        /* Write server initialization */
        BufferWriter bw = new BufferWriter(this.dos, 9);
        bw.write(this.getIDBytes());
        bw.write(ServerThread.SUPPORTED_VERSION);
        bw.write(ServerThread.UAMP_FLAGS);

        /* Read the client initialization (note flags are ignored) */
        BufferReader br = new BufferReader(this.dis, 9);
        br.read(cID);
        cVer = br.readByte();
        br.read(new byte[4]);

        /* If there is a problem, send an INITIALIZATION_FAILED message */
        try {
            this.parseIDBytes(cID);
            cVer = (byte) (cVer & ServerThread.SUPPORTED_VERSION);
            if (cVer == (byte) 0x00)
                throw new UAMPException(
                        "Client and server support no shared version");
        } catch (UAMPException uae) {
            try {
                this.dos.writeByte((byte) 0x00);
            } catch (Exception e) {
            }
            throw uae;
        }

        /*
         * Otherwise, send VERSION_CHOICE. Note that only a single bit is set
         * in SUPPORTED_VERSION, making it a legal VERSION_CHOICE.
         */
        this.dos.writeByte(ServerThread.SUPPORTED_VERSION);
        cVer = this.dis.readByte();
        if (cVer == (byte) 0x00)
            throw new UAMPException(
                    "Client rejected handshake for unknown reason");
        else if (cVer != ServerThread.SUPPORTED_VERSION)
            throw new UAMPException(
                    "Client and server do not agree on version");
    }

    /**
     * Parses and responds to a location request command.
     *
     * @param num the number of agent IDs to follow in the location request.
     * @param numAgents the total number of agents in the simulation.
     * @throws IOException if there is an error reading or writing from the
     *         socket.
     * @throws UAMPException if there is an error executing the UAMP or MVISP
     *         protocol.
     */
    private void parseLocationRequest(UnsignedInteger num, int numAgents)
            throws IOException, UAMPException {
        long numRequests = num.toLong();
        if (numRequests == 0L)
            throw new UAMPException("Invalid NUM_REQUESTS value");

        /*
         * Multiplication is safe: both 2^32-1 * 4 and 2^32-1 * 12 are in the
         * range of a long.
         */
        BufferReader br = new BufferReader(this.dis, numRequests * 4);
        BufferWriter bw = new BufferWriter(this.dos, numRequests * 12);
        this.manager.startOrder(bw);

        /*
         * Pass all requests to the manager and wait on its completion.
         */
        for (long i = 0; i < numRequests; i++) {
            long agentIDLong = br.readUnsignedInt().toLong();
            if (agentIDLong >= (long) numAgents)
                throw new UAMPException(
                        "Invalid agent ID in location request");
            this.manager.addToOrder((int) agentIDLong);
        }
        this.manager.endOrder();
    }

    /**
     * Initializes the data input and data output streams for this thread.
     *
     * @throws IOException if there is an IO error opening the server (if
     *         necessary) or any of the data streams.
     * 
     */
    private void initializeStreams() throws IOException {
        Socket myConnection = null;

        /* If there is no preexisting connection, we have to open a server */
        if (this.connection == null) {
            synchronized (this) {
                if (this.killed)
                    return;
                this.server = new ServerSocket(this.port);
            }

            /*
             * We use the idiom of accepting the connection outside the lock
             * then setting the instance variable inside the lock to prevent a
             * race condition against the killThread method. If killThread is
             * called while we are waiting on an accept, everything still gets
             * closed properly.
             */
            myConnection = this.server.accept();
            synchronized (this) {
                if (this.killed) {
                    myConnection.close();
                    return;
                }
                this.connection = myConnection;
            }
        }

        /* Once a connection is open, get the streams from it */
        synchronized (this) {
            if (this.killed)
                return;
            InputStream is = this.connection.getInputStream();
            this.dis = new DataInputStream(is);
            OutputStream os = this.connection.getOutputStream();
            this.dos = new DataOutputStream(os);
        }
    }

    /**
     * Closes any non-null instance servers, sockets, or data streams.
     *
     * @throws IOException if there is an IO error closing any of the
     *         resources.
     */
    private void closeStreams() throws IOException {
        if (this.dos != null)
            this.dos.close();
        if (this.dis != null)
            this.dis.close();
        if (this.connection != null)
            this.connection.close();
        if (this.server != null)
            this.server.close();
    }

    /**
     * The <code>Manager</code> class is responsible for processing and
     * responding to location requests.
     */
    private abstract class Manager {
        /**
         * The simulation being run by the manager.
         */
        protected SimulationDiscrete simulation;

        /**
         * The writer to which computed results are output.
         */
        private BufferWriter bw;

        /**
         * Creates a new manager to run the given simulation.
         *
         * @param simulation the simulation to be run by the manager.
         */
        public Manager(SimulationDiscrete simulation) {
            this.simulation = simulation;
        }

        /**
         * Starts the background resources for this manager. By default, this
         * function does nothing.
         */
        public void startManager() {}

        /**
         * Sets the output buffer writer to the given buffer writer.
         *
         * @param bw the output buffer writer.
         */
        public void startOrder(BufferWriter bw) {
            this.bw = bw;
        }

        /**
         * Adds the given agent to the queue of agent updates that need to be
         * output.
         *
         * @param agent the agent to output.
         * @throws IOException if there is an error writing to the buffer
         *         writer.
         */
        public abstract void addToOrder(int agent) throws IOException;

        /**
         * Wait until all of the agent updates added to the queue have been
         * output. By default, this function does nothing.
         *
         * @throws IOException if there is an error writing to the buffer
         *         writer.
         */
        public void endOrder() throws IOException {}

        /**
         * Terminate all resources associated with this manager. By default,
         * this function does nothing.
         */
        public void terminate() {}

        /**
         * Writes the given agent update to the buffered writer.
         *
         * @param agentID the agent ID.
         * @param x the x coordinate.
         * @param y the y coordinate.
         * @param time the time of the update.
         * @throws IOException if there is an error writing to the buffered
         *         writer.
         */
        protected void writeUpdate(int agentID, UnsignedInteger x,
                UnsignedInteger y, UnsignedInteger time) throws IOException {
            /* Place the update in the write buffer (2D server skips Z) */
            this.bw.write(time);
            this.bw.write(x);
            this.bw.write(y);
            listener.serverThreadProgress(ServerThread.this, agentID,
                    time.toLong());
        }
    }

    /**
     * The <code>ManagerSimple</code> class has the calling thread do all of
     * the work of computing and sending agent updates.
     */
    private class ManagerSimple extends Manager {
        /**
         * Whether the initial location update for each agent has been sent.
         */
        private boolean[] initSent;

        /**
         * Creates a new simple manager to run the given simulation.
         *
         * @param simulation the simulation to run.
         */
        public ManagerSimple(SimulationDiscrete simulation) {
            super(simulation);
            int numAgents = simulation.getNumAgents();
            this.initSent = new boolean[numAgents];
            for (int i = 0; i < numAgents; i++)
                this.initSent[i] = false;
        }

        /**
         * Adds the given agent to the queue of agent updates that need to be
         * output.
         *
         * @param agent the agent to output.
         * @throws IOException if there is an error writing to the buffer
         *         writer.
         */
        public void addToOrder(int agent) throws IOException {
            if (this.initSent[agent])
                this.simulation.advance(agent);
            else
                this.initSent[agent] = true;
            UnsignedInteger[] loc = this.simulation.getCurrentLocation(agent);
            UnsignedInteger time = this.simulation.getCurrentTime(agent);
            this.writeUpdate(agent, loc[0], loc[1], time);
        }
    }

    /**
     * The <code>ManagerMulti</code> class is responsible for computing agent
     * updates in a multi-threaded fashion.
     */
    private class ManagerMulti extends Manager {
        /**
         * All of the agent updates requested by the calling thread.
         */
        private Queue<Integer> order;

        /**
         * Those agents that need updates computed and do not yet have a worker
         * thread computing updates.
         */
        private Queue<Integer> workToDo;

        /**
         * The number of updates required for each agent to meet the current
         * order. An entry may be negative if there are agent updates in the
         * update queue, but no call for that agent in the current order.
         */
        private int[] numRequired;

        /**
         * The sum of the non-negative entries in <code>numRequired</code>, or
         * in other words the number of agent simulation steps that have to be
         * completed before the order can be delivered.
         */
        private TotalWorkCounter totalWork;

        /**
         * All of the updates computed so far for each agent.
         */
        private List<Queue<AgentUpdate>> updateQueues;

        /**
         * The number of worker threads that will be launched.
         */
        private int numWorkers;

        /**
         * A flag to signal that worker threads should terminate.
         */
        private boolean killWorkers;

        /**
         * Creates a new multi-threaded manager that will operate on the given
         * simulation with the given number of threads.
         *
         * @param simulation the simulation for which agent updates will be
         *        computed.
         * @param threads the number of worker threads to use to compute
         *        updates.
         */
        public ManagerMulti(SimulationDiscrete simulation, int threads) {
            super(simulation);
            int numAgents = simulation.getNumAgents();
            this.order = new LinkedList<Integer>();
            this.workToDo = new LinkedList<Integer>();
            this.numRequired = new int[numAgents];
            this.totalWork = new TotalWorkCounter();
            this.updateQueues = new ArrayList<Queue<AgentUpdate>>(numAgents);
            this.numWorkers = threads;
            this.killWorkers = false;

            /* Add the initial location of each agent to the queue */
            for (int i = 0; i < numAgents; i++) {
                this.numRequired[i] = -1;
                Queue<AgentUpdate> resQueue = new LinkedList<AgentUpdate>();
                UnsignedInteger[] loc = simulation.getCurrentLocation(i);
                UnsignedInteger time = simulation.getCurrentTime(i);
                resQueue.add(new AgentUpdate(loc[0], loc[1], time));
                this.updateQueues.add(resQueue);
            }
        }

        /**
         * Starts the worker threads used by this manager.
         */
        public void startManager() {
            for (int i = 0; i < this.numWorkers; i++) {
                WorkerThread wt = new WorkerThread(this, this.simulation);
                wt.start();
            }
        }

        /**
         * Adds the given agent to the queue of agent updates that need to be
         * output.
         *
         * @param agent the agent to output.
         * @throws IOException never.
         */
        public synchronized void addToOrder(int agent) throws IOException {
            this.order.add(agent);
            this.numRequired[agent]++;
            if (this.numRequired[agent] == 1) {
                this.workToDo.add(agent);
                this.notify();
            }

            /*
             * No need to lock totalWork here, since only calling thread can be
             * running here, and only the calling thread or other threads with
             * the lock on the "this" object ever check its value.
             */
            if (this.numRequired[agent] > 0)
                this.totalWork.increment();
        }

        /**
         * Wait until all of the agent updates added to the queue have been
         * computed then write them to the buffer writer.
         *
         * @throws IOException if there is an error writing to the buffer
         *         writer.
         */
        public void endOrder() throws IOException {
            /* Wait until all processing is complete */
            synchronized (this.totalWork) {
                while (this.totalWork.isDone() == false
                        && this.killWorkers == false) {
                    try {
                        this.totalWork.wait();
                    } catch (InterruptedException iex) {
                    }
                }
            }
            if (this.killWorkers)
                return;

            /*
             * Output to the buffered writer. Note that there is no need to
             * lock anything here, since the worker threads are strictly
             * reactive.
             */
            while (this.order.isEmpty() == false) {
                int agent = this.order.poll();
                Queue<AgentUpdate> resQueue = this.updateQueues.get(agent);
                AgentUpdate au = resQueue.poll();
                this.writeUpdate(agent, au.getX(), au.getY(), au.getTime());
            }
        }

        /**
         * Terminate all the running worker threads, and notify the main thread
         * if it is blocked on the end of computation.
         */
        public synchronized void terminate() {
            this.killWorkers = true;
            this.notifyAll();
            synchronized (this.totalWork) {
                this.totalWork.notify();
            }
        }

        /**
         * Returns an agent that a worker thread can simulate, or
         * <code>-1</code> to indicate that the worker thread should terminate.
         *
         * @return an agent to simulate or <code>-1</code>.
         */
        public synchronized int getAgentToSimulate() {
            while (this.workToDo.isEmpty() && this.killWorkers == false) {
                try {
                    this.wait();
                } catch (InterruptedException iex) {
                }
            }

            if (this.killWorkers)
                return -1;
            return this.workToDo.poll();
        }

        /**
         * Called by worker threads to indicate that they have advanced the
         * simulation for the specified agent.
         *
         * @param agent the agent that was advance.
         * @param au the update for that agent at the current time.
         * @return <code>true</code> if the worker thread should advance the
         *         agent again, or <code>false</code> if the worker thread
         *         should request a new agent to simulate.
         */
        public synchronized boolean deliverUpdate(int agent, AgentUpdate au) {
            if (this.killWorkers)
                return false;

            this.numRequired[agent]--;
            Queue<AgentUpdate> resQueue = this.updateQueues.get(agent);
            resQueue.add(au);

            synchronized (this.totalWork) {
                this.totalWork.decrement();
                if (this.totalWork.isDone())
                    this.totalWork.notify();
            }

            return (this.numRequired[agent] > 0);
        }
    }

    /**
     * The <code>WorkerThread</code> class is responsible for advancing a
     * simulation as dictated by a {@link ManagerMulti}.
     */
    private class WorkerThread extends Thread {
        /**
         * The manager controlling this thread.
         */
        private ManagerMulti manager;

        /**
         * The simulation being advanced by this thread.
         */
        private SimulationDiscrete simulation;

        /**
         * Creates a new <code>WorkerThread</code> that will advance the given
         * simulation at the command of the given manager.
         *
         * @param manager the manager controlling the worker thread.
         * @param simulation the simulation that this thread will advance.
         */
        public WorkerThread(ManagerMulti manager,
                SimulationDiscrete simulation) {
            this.manager = manager;
            this.simulation = simulation;
        }

        /**
         * Executes commands supplied by the manager until told to terminate.
         */
        public void run() {
            while (true) {
                int agent = this.manager.getAgentToSimulate();
                if (agent == -1)
                    break;
                while (true) {
                    this.simulation.advance(agent);
                    UnsignedInteger[] loc =
                            this.simulation.getCurrentLocation(agent);
                    UnsignedInteger time =
                            this.simulation.getCurrentTime(agent);
                    AgentUpdate au = new AgentUpdate(loc[0], loc[1], time);
                    if (this.manager.deliverUpdate(agent, au) == false)
                        break;
                }
            }
        }
    }

    /**
     * The <code>TotalWorkCounter</code> class keeps track of how many
     * simulation advances need to be computed before an order can be filled.
     */
    private class TotalWorkCounter {
        /**
         * The total number of simulation advances before an order is filled.
         */
        private int work;

        /**
         * Creates a new counter with a value of zero.
         */
        public TotalWorkCounter() {
            this.work = 0;
        }

        /**
         * Increments the number of advances until the order is complete.
         */
        public void increment() {
            this.work++;
        }

        /**
         * Decrements the number of advances until the order is complete.
         */
        public void decrement() {
            this.work--;
        }

        /**
         * Returns whether the counter is zero.
         *
         * @return <code>true</code> if the counter is or has reached zero,
         *         otherwise <code>false</code>.
         */
        public boolean isDone() {
            return (this.work == 0);
        }
    }

    /**
     * The <code>AgentUpdate</code> class represents that data that will be
     * sent to a client about an agent's location at a certain time.
     */
    private class AgentUpdate {
        /**
         * The x coordinate of the agent for this update.
         */
        private UnsignedInteger x;

        /**
         * The y coordinate of the agent for this update.
         */
        private UnsignedInteger y;

        /**
         * The time at which the agent is at this location.
         */
        private UnsignedInteger time;

        /**
         * Creates a new <code>AgentUpdate</code> with the given position at
         * the given time.
         *
         * @param x the x coordinate of the update.
         * @param y the y coordinate of the update.
         * @param time the time at which the agent is at those coordinates.
         */
        public AgentUpdate(UnsignedInteger x, UnsignedInteger y,
                UnsignedInteger time) {
            this.x = x;
            this.y = y;
            this.time = time;
        }

        /**
         * Returns the x coordinate of this update.
         *
         * @return the x coordinate of the update.
         */
        public UnsignedInteger getX() {
            return this.x;
        }

        /**
         * Returns the y coordinate of this update.
         *
         * @return the y coordinate of the update.
         */
        public UnsignedInteger getY() {
            return this.y;
        }

        /**
         * Returns the time corresponding to this update.
         *
         * @return the time at which the agent is at these coordinates.
         */
        public UnsignedInteger getTime() {
            return this.time;
        }
    }

    /**
     * The <code>DefaultServerThreadListener</code> class implements the
     * default actions to take on completion of a <code>ServerThread</code>.
     * Specifically, it prints error messages to standard error and does
     * nothing else.
     */
    private class DefaultServerThreadListener implements ServerThreadListener {
        /**
         * Takes no action when a <code>ServerThread</code> successfully passes
         * the negotiation phase.
         *
         * @param thread the thread that completed negotiation.
         * @param numAgents the number of agents in the simulation.
         * @param durationMilli the number of milliseconds in duration the
         *        simulation will be.
         */
        public void serverThreadExchange(ServerThread thread, int numAgents,
                long durationMilli) {}

        /**
         * Takes no action when a <code>ServerThread</code> sends mobility data
         * to a client.
         *
         * @param thread the thread that sent mobility data.
         * @param agentID the agent for which mobility data was sent.
         * @param timestamp the timestamp of the mobility data sent in
         *        milliseconds.
         */
        public void serverThreadProgress(ServerThread thread, int agentID,
                long timestamp) {}

        /**
         * Takes no action when a <code>ServerThread</code> completes cleanly.
         *
         * @param thread the thread that completes.
         */
        public void serverThreadCompleted(ServerThread thread) {}

        /**
         * Takes no action when a <code>ServerThread</code> is killed.
         *
         * @param thread the thread that is killed.
         */
        public void serverThreadKilled(ServerThread thread) {}

        /**
         * Prints a message to standard error when a <code>ServerThread</code>
         * experiences an error.
         *
         * @param thread the thread that experiences an error.
         * @param error the error to print.
         */
        public void serverThreadError(ServerThread thread, Throwable error) {
            String str = Application.throwableToString(error);
            System.err.println(str);
        }
    }
}
