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
import ca.ualberta.dbs3.commandLine.*;
import ca.ualberta.dbs3.math.*;
import ca.ualberta.dbs3.network.UnsignedInteger;

/**
 * The <code>UAMPSimulation</code> class represents a stand-alone DBS3
 * simulation output to standard output. It does not have any manner of network
 * interface.
 */
public class UAMPSimulation extends Application {
    /**
     * The command line options passed to the Java VM.
     */
    private String[] args;

    /**
     * Creates a new <code>UAMPSimulation</code> application.
     *
     * @param args the command line arguments given to the Java VM.
     */
    public UAMPSimulation(String[] args) {
        this.args = args;
    }

    /**
     * Runs the application.
     *
     * @return a return code to send when the application exits.
     * @throws MapFileException if there is an error in the loaded map file.
     */
    public int run() throws MapFileException {
        /* Parse the command line */
        Parser parser = new Parser();
        OptionMapFile opMap = new OptionMapFile();
        OptionPathfinder opPath = new OptionPathfinder();
        OptionDestinationChooser opDest = new OptionDestinationChooser();
        OptionAgents opAgents = new OptionAgents();
        OptionDuration opDuration = new OptionDuration();
        OptionSeed opSeed = new OptionSeed();
        OptionSpeed opSpeed = new OptionSpeed();
        OptionPause opPause = new OptionPause();
        OptionOutput opOutput = new OptionOutput();
        OptionStreetCut opStreetCut = new OptionStreetCut();
        parser.add(opMap);
        parser.add(opPath);
        parser.add(opDest);
        parser.add(opAgents);
        parser.add(opDuration);
        parser.add(opSeed);
        parser.add(opSpeed);
        parser.add(opPause);
        parser.add(opOutput);
        parser.add(opStreetCut);
        if (parser.parse(this.args) == false)
            return -1;
        if (opOutput.noOutput() == false)
            parser.printSummary();

        /* Load the map, pathfinder, etc. */
        ProgressMonitor pm = null;
        if (opOutput.noOutput() == false)
            pm = new ProgressMonitorPrint();
        Map map = new Map(opMap.getMapFile());
        PathfinderOptimal pathfinder =
                (PathfinderOptimal) (opPath.getPathfinder(map));
        pathfinder.setStreetCutEnabled(opStreetCut.enableStreetCut());
        DestinationChooser destChooser = opDest.getChooser(map, pm);

        /* If we need to output metrics, set the pathfinder to gather them */
        if (opOutput.outputMetrics())
            pathfinder.setGatherMetrics(true);

        /* Create the simulation */
        int numAgents = opAgents.getNumAgents();
        UnsignedInteger timeLimit = opDuration.getDuration();
        Simulation innerSim = new Simulation(numAgents, opSpeed.getRange(),
                opPause.getRange(), timeLimit, destChooser, pathfinder,
                opSeed.getSeed(), pm);
        SimulationDiscrete sim = new SimulationDiscrete(innerSim);

        /* Output a progress bar if we're only outputting metrics */
        if (opOutput.outputMetrics() || opOutput.outputTiming())
            pm.start("Simulating agents");
        long startMS = System.currentTimeMillis();

        /* Simulate the moving agents */
        for (int agent = 0; agent < numAgents; agent++) {
            if (opOutput.outputMovement()) {
                System.out.println();
                System.out.println("Agent " + agent);
            }
            while (true) {
                UnsignedInteger timeUI = sim.getCurrentTime(agent);
                if (opOutput.outputMovement()) {
                    UnsignedInteger[] loc = sim.getCurrentLocation(agent);
                    double outSec = ((double) (timeUI.toLong())) / 1000.0;
                    double outX = ((double) (loc[0].toLong())) / 1000.0;
                    double outY = ((double) (loc[1].toLong())) / 1000.0;
                    double outZ = 0.0;
                    System.out.format("Time %.3f: location %.3f, %.3f, %.3f%n",
                            outSec, outX, outY, outZ);
                } else if (opOutput.outputMetrics()
                        || opOutput.outputTiming()) {
                    double progress = ((double) agent) / numAgents;
                    long tl = timeLimit.toLong();
                    if (tl != 0)
                        progress += ((double) (timeUI.toLong()))
                                / (timeLimit.toLong() * numAgents);
                    pm.update(progress);
                }
                if (sim.advance(agent) == false)
                    break;
            }
        }
        if (opOutput.outputMetrics() || opOutput.outputTiming())
            pm.end();

        /* Output final metrics */
        if (opOutput.outputMetrics()) {
            System.out.println();
            pathfinder.printMetrics();
        }
        if (opOutput.outputTiming()) {
            System.out.println();
            System.out.println("Simulation time: "
                    + (System.currentTimeMillis() - startMS) + " ms");
        }
        return 0;
    }

    /**
     * Runs a stand-alone DBS3 simulation.
     *
     * @param args the command line arguments given to the Java VM.
     * @throws MapFileException if there is an error in the loaded map file.
     */
    public static void main(String[] args) throws MapFileException {
        UAMPSimulation application = new UAMPSimulation(args);
        System.exit(application.run());
    }

    /**
     * The <code>OptionOutput</code> class represents a choice between
     * outputting the simulation data, outputting metrics of the simulation, or
     * silencing all output (useful for timing a simulation).
     */
    private static class OptionOutput extends Option {
        /**
         * The choice to output the simulation results.
         */
        private Choice outputSim;

        /**
         * The choice to output metrics.
         */
        private Choice outputMetrics;

        /**
         * The choice to output timing information.
         */
        private Choice outputTime;

        /**
         * Creates a new <code>OptionOutput</code> to be added to the parser.
         */
        public OptionOutput() {
            super("Output");
            this.outputSim = new Choice("outputSimulation");
            this.outputMetrics = new Choice("outputMetrics");
            this.outputTime = new Choice("outputTime");
            Choice silent = new Choice("silent");
            this.addDefault(this.outputSim);
            this.add(this.outputMetrics);
            this.add(this.outputTime);
            this.add(silent);
        }

        /**
         * Returns whether or not to output simulation movement data.
         *
         * @return <code>true</code> if movement data should be output,
         *         otherwise <code>false</code>.
         */
        public boolean outputMovement() {
            return this.outputSim.isActive();
        }

        /**
         * Returns whether or not to output metrics gathered during the
         * simulation.
         *
         * @return <code>true</code> if metrics should be output, otherwise
         *         <code>false</code>.
         */
        public boolean outputMetrics() {
            return this.outputMetrics.isActive();
        }

        /**
         * Returns whether or not to output the time it took the simulation to
         * run.
         *
         * @return <code>true</code> if timing should be output, otherwise
         *         <code>false</code>.
         */
        public boolean outputTiming() {
            return this.outputTime.isActive();
        }

        /**
         * Returns whether there should be no output about the simulation.
         *
         * @return <code>true</code> if there should be no output, otherwise
         *         <code>false</code>.
         */
        public boolean noOutput() {
            return (this.outputMetrics() == false
                    && this.outputMovement() == false
                    && this.outputTiming() == false);
        }
    }

    /**
     * The <code>OptionStreetCut</code> class represents an option to disable
     * the StreetCut algorithm, for timing purposes.
     */
    private class OptionStreetCut extends Option {
        /**
         * The choice to disable StreetCut.
         */
        private Choice disable;

        /**
         * Creates a new <code>OptionStreetCut</code> to be added to the
         * parser.
         */
        public OptionStreetCut() {
            super("StreetCut");
            this.disable = new Choice("disableStreetCut");
            this.add(this.disable);
        }

        /**
         * Returns a string summarizing the user choice.
         *
         * @return a string summary of whether or not to enable StreetCut.
         */
        public String getDescription() {
            if (this.enableStreetCut())
                return "Enabled";
            else
                return "Disabled";
        }

        /**
         * Returns whether or not StreetCut should be enabled.
         *
         * @return <code>true</code> if StreetCut should be enabled, otherwise
         *         <code>false</code>.
         */
        public boolean enableStreetCut() {
            return !(this.disable.isActive());
        }
    }
}
