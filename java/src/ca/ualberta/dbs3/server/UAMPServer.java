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

import ca.ualberta.dbs3.cartography.*;
import ca.ualberta.dbs3.commandLine.*;
import ca.ualberta.dbs3.math.*;
import ca.ualberta.dbs3.simulations.*;
import java.io.*;
import java.net.*;

/**
 * The <code>UAMPServer</code> runs the Destination-Based Space Syntax
 * Simulator (DBS3) as a UAMP server, conforming the to RFC specifications.
 */
public class UAMPServer extends Application {
    /**
     * The delay between retries when binding a socket.
     */
    private static final long TIMEOUT_MS = 100;

    /**
     * The maximum amount of time to wait before giving up on binding a socket.
     */
    private static final long MAX_TIMEOUT_MS = 2000;

    /**
     * The command line options passed to the Java VM.
     */
    private String[] args;

    /**
     * Creates a new <code>UAMPServer</code> application.
     *
     * @param args the command line arguments given to the Java VM.
     */
    public UAMPServer(String[] args) {
        this.args = args;
    }

    /**
     * Runs the application.
     *
     * @return a return code to send when the application exits.
     * @throws MapFileException if there is an error in the loaded map file.
     * @throws IOException if there is an error starting the server.
     */
    public int run() throws MapFileException, IOException {
        /* Parse the command line */
        Parser parser = new Parser();
        OptionMapFile opMap = new OptionMapFile();
        OptionPathfinder opPath = new OptionPathfinder();
        OptionDestinationChooser opDest = new OptionDestinationChooser();
        OptionSpeed opSpeed = new OptionSpeed();
        OptionPause opPause = new OptionPause();
        OptionPort opPort = new OptionPort();
        OptionDaemonize opDaemonize = new OptionDaemonize();
        OptionThreads opThreads = new OptionThreads();
        parser.add(opMap);
        parser.add(opPath);
        parser.add(opDest);
        parser.add(opSpeed);
        parser.add(opPause);
        parser.add(opPort);
        parser.add(opDaemonize);
        parser.add(opThreads);
        if (parser.parse(this.args) == false)
            return -1;

        /*
         * Only print the summary and the progress bar if we are not
         * daemonizing
         */
        ProgressMonitor pm = null;
        if (opDaemonize.daemonize() == false) {
            parser.printSummary();
            pm = new ProgressMonitorPrint();
        }

        /* Set the number of threads to be used */
        Simulation.setInitializationThreads(opThreads.getThreads());
        ServerThread.setWorkerThreads(opThreads.getThreads());

        /* Load the map, pathfinder, etc. */
        Map map = new Map(opMap.getMapFile());
        Pathfinder pathfinder = opPath.getPathfinder(map);
        DestinationChooser destChooser = opDest.getChooser(map, pm);
        Range speed = opSpeed.getRange();
        Range pause = opPause.getRange();

        /*
         * Open the socket. If UAMPServer is part of a large experiment, in
         * which servers are started and killed by scripts, it often happens
         * that a socket isn't ready for binding right as a new UAMPServer
         * starts. Wait for a while to see if a socket clears up.
         */
        ServerSocket socket = null;
        long timeout = 0;
        while (true) {
            try {
                socket = new ServerSocket(opPort.getPort());
            } catch (BindException be) {
                if (timeout >= UAMPServer.MAX_TIMEOUT_MS)
                    throw be;
            }
            if (socket != null)
                break;
            try {
                Thread.sleep(UAMPServer.TIMEOUT_MS);
            } catch (InterruptedException ie) {
            }
            timeout += UAMPServer.TIMEOUT_MS;
        }

        /* Inform the user we are ready for connections, or daemonize */
        if (opDaemonize.daemonize())
            this.daemonizeMsgAndClose(opPort.getPort());
        else
            System.out.println(
                    "Now accepting connections on port " + opPort.getPort());

        /* Wait for incoming connections */
        while (true) {
            try {
                Socket connection = socket.accept();
                Thread thread = new ServerThreadUAMP(speed, pause, destChooser,
                        pathfinder, connection);
                thread.start();
            } catch (IOException ioe) {
                String str = Application.throwableToString(ioe);
                System.err.println(str);
            }
        }
    }

    /**
     * Outputs a brief message with the daemon's PID and port, then closes all
     * three of the standard file descriptors.
     * 
     * @param port the port on which DBS3 is running.
     * @throws IOException if any of the standard file descriptors fail to
     *         close.
     */
    private void daemonizeMsgAndClose(int port) throws IOException {
        long pid = ProcessHandle.current().pid();
        System.out.println("DBS3 ready on port " + port + " with PID " + pid);
        System.in.close();
        System.out.close();
        System.err.close();
    }

    /**
     * Runs a DBS3 UAMP server.
     *
     * @param args the command line arguments given to the Java VM.
     * @throws MapFileException if there is an error in the loaded map file.
     * @throws IOException if there is an error starting the server.
     */
    public static void main(String[] args)
            throws MapFileException, IOException {
        UAMPServer application = new UAMPServer(args);
        System.exit(application.run());
    }

    /**
     * The <code>OptionDaemonize</code> class represents a command line option
     * for specifying that the server should be run in the background, as a
     * daemon.
     */
    private class OptionDaemonize extends Option {
        /**
         * The choice to daemonize.
         */
        private Choice daemonize;

        /**
         * Creates a new <code>OptionDaemonize</code> to be added to a parser.
         */
        public OptionDaemonize() {
            super("Daemonize");
            this.daemonize = new Choice("daemonize");
            this.add(this.daemonize);
        }

        /**
         * Returns whether or not this server should run as a daemon.
         *
         * @return <code>true</code> if it should run as a daemon,
         *         <code>false</code> otherwise.
         */
        public boolean daemonize() {
            return this.daemonize.isActive();
        }
    }

    /**
     * The <code>OptionThreads</code> class represents a command line option
     * for specifying the number of threads used per connection.
     */
    private class OptionThreads extends Option {
        /**
         * The number of threads to use.
         */
        private ArgumentInt arg;

        /**
         * Creates a new <code>OptionThreads</code> to be added to a parser.
         */
        public OptionThreads() {
            super("Threads");
            Choice choice = new Choice("threads");
            this.arg = new ArgumentInt("threadsPerConnection",
                    Runtime.getRuntime().availableProcessors(), 1);
            choice.add(this.arg);
            this.addDefault(choice);
        }

        /**
         * Returns the number of threads parsed off the command line.
         *
         * @return the number of threads per connection.
         */
        public int getThreads() {
            return this.arg.getValue();
        }

        /**
         * Returns a string with a description of the current choice for this
         * option.
         *
         * @return a description of the current choice.
         */
        public String getDescription() {
            return Integer.toString(this.getThreads()) + " per connection";
        }
    }
}
