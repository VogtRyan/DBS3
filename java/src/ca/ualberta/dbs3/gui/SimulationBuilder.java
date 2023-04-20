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

package ca.ualberta.dbs3.gui;

import ca.ualberta.dbs3.cartography.*;
import ca.ualberta.dbs3.commandLine.*;
import ca.ualberta.dbs3.network.*;
import ca.ualberta.dbs3.server.*;
import ca.ualberta.dbs3.simulations.*;
import javax.swing.*;

/**
 * The <code>SimulationBuilder</code> class is responsible for taking a
 * {@link SimulationSpecification} and transforming it into a
 * {@link SimulationRecorded}. The <code>SimulationBuilder</code> will launch
 * and control an MVISP server if necessary.
 */
public class SimulationBuilder {
    /**
     * The message displayed before negotiation is complete.
     */
    private static final String PRE_EXCHANGE =
            "Awaiting connection from MVISP client...";

    /**
     * The message displayed after negotiation is complete.
     */
    private static final String POST_EXCHANGE = "Sending mobility data...";

    /**
     * The map on which the simulation will run.
     */
    private Map map;

    /**
     * The simulation specification from which we are building a simulation.
     */
    private SimulationSpecification spec;

    /**
     * The dialog that will receive the completed simulation.
     */
    private SimulationDialog dialog;

    /**
     * The dialog that gets updated with our progress.
     */
    private SimulationInitDialog init;

    /**
     * The progress monitor that updates the progress bar.
     */
    private BarMonitor pm;

    /**
     * A running simulation construction thread, or <code>null</code> if no
     * such thread is running.
     */
    private ConstructionThread constructionThread;

    /**
     * A running MVISP server, or <code>null</code> if no server thread is
     * running.
     */
    private ServerThreadMVISP mvispThread;

    /**
     * The last queued progress bar percentage update on the EDT, or
     * <code>-1</code> if the last update on the EDT set the bar to not display
     * an active percentage.
     */
    private int lastQueued;

    /**
     * Creates a new <code>SimulationBuilder</code> that will build a
     * simulation according to the given specifications.
     *
     * @param map the map on which the simulation will run.
     * @param spec the simulation specification.
     * @param dialog the dialog that will receive the constructed simulation.
     */
    public SimulationBuilder(Map map, SimulationSpecification spec,
            SimulationDialog dialog) {
        this.map = map;
        this.spec = spec;
        this.dialog = dialog;
        this.init = null;
        this.pm = new BarMonitor();
        this.constructionThread = null;
        this.mvispThread = null;
        this.lastQueued = -1;
    }

    /**
     * Builds the simulation and passes it to the simulation dialog when
     * complete. This method will spawn worker threads to do all of the heavy
     * computation, so it is safe to call this method from the EDT; however,
     * the return call to the simulation dialog will occur on the EDT. Note
     * that the behaviour of this class is undefined if this function is called
     * more than once per <code>SimulationBuilder</code> object.
     */
    public void build() {
        /*
         * This may look like a race condition (another thread could try to set
         * the dialog invisible before it is set visible here), but it there is
         * in fact no race condition. This code is executing on the event
         * dispatch thread, and any setVisible(false) code executed by another
         * thread is placed onto the event dispatch queue. So, the
         * setVisible(true) is guaranteed to execute before any
         * setVisible(false).
         */
        this.init = new SimulationInitDialog(this.dialog, this);
        this.constructionThread = new ConstructionThread();
        this.constructionThread.start();
        this.init.setVisible(true);
    }

    /**
     * Terminates all threads that the <code>SimulationBuilder</code> may be
     * running.
     */
    public void terminateAllThreads() {
        this.pm.cancel();
        this.constructionThread = null;
        if (this.mvispThread != null) {
            this.mvispThread.killThread();
            this.mvispThread = null;
        }
    }

    /**
     * Tests whether any threads are running.
     *
     * @return <code>true</code> if any threads are running, otherwise
     *         <code>false</code>.
     */
    public boolean isRunning() {
        return (this.constructionThread != null);
    }

    /**
     * Resets the progress bar to indeterminate or disabled in the
     * initialization dialog.
     *
     * @param text the new text to display in the dialog.
     * @param enabled whether or not the progress bar should be enabled.
     */
    private void progressReset(final String text, final boolean enabled) {
        this.lastQueued = -1;
        Runnable r = new Runnable() {
            public void run() {
                if (constructionThread == null)
                    return;
                init.setText(text);
                if (enabled)
                    init.progressIndeterminate();
                else
                    init.progressDisable();
            }
        };
        SwingUtilities.invokeLater(r);
    }

    /**
     * Sets the progress bar value in the initialization dialog.
     *
     * @param value the value to assign to the progress bar.
     */
    public void progressUpdate(final int value) {
        if (value == this.lastQueued)
            return;
        this.lastQueued = value;
        Runnable r = new Runnable() {
            public void run() {
                if (constructionThread != null)
                    init.progressUpdate(value);
            }
        };
        SwingUtilities.invokeLater(r);
    }

    /**
     * Launches an MVISP server thread that will provide a State Record for the
     * construction of the simulation.
     *
     * @param simulation the simulation to run on the MVISP server.
     */
    private void launchMVISP(final SimulationDiscrete simulation) {
        Runnable r = new Runnable() {
            public void run() {
                if (constructionThread == null)
                    return;
                init.setText(SimulationBuilder.PRE_EXCHANGE);
                init.progressDisable();
                SimulationDiscrete copy = new SimulationDiscrete(simulation);
                mvispThread = new ServerThreadMVISP(copy, spec.getPort());
                MVISPServerListener listener =
                        new MVISPServerListener(simulation);
                mvispThread.setListener(listener);
                mvispThread.start();
            }
        };
        SwingUtilities.invokeLater(r);
    }

    /**
     * Places an event on the EDT to pass the completed simulation to the
     * simulation configuration dialog.
     *
     * @param sim the completed simulation to pass.
     */
    private void complete(final SimulationRecorded sim) {
        Runnable r = new Runnable() {
            public void run() {
                /* Check if the threads have been killed */
                if (constructionThread == null)
                    return;
                constructionThread = null;
                init.progressUpdate(100);
                init.dispose();
                dialog.receive(sim, spec);
            }
        };
        SwingUtilities.invokeLater(r);
    }

    /**
     * The <code>ConstructionThread</code> class is responsible for performing
     * the steady state analysis. In the absence of the need for an MVISP
     * server, it will also complete the construction of the simulation.
     */
    private class ConstructionThread extends Thread {
        /**
         * Performs the destination analysis, then either launches an MVISP
         * thread or completes construction of the simulation.
         */
        public void run() {
            /* Initialize the simulation */
            DestinationChooser destChooser =
                    new DestinationChooserIntegrated(map, spec.getAlpha(),
                            spec.getDelta(), spec.getRadius(), pm);
            if (pm.shouldCancel())
                return;
            Pathfinder pathfinder =
                    OptionPathfinder.getPathfinder(map, spec.getPathfinder());
            UnsignedInteger dur = OptionDuration
                    .getDuration(spec.getDuration(), spec.getDurationUnit());
            Simulation sim = new Simulation(spec.getNumAgents(),
                    spec.getSpeed(), spec.getPause(), dur, destChooser,
                    pathfinder, spec.getSeed(), pm);
            SimulationDiscrete simDis = new SimulationDiscrete(sim);
            if (pm.shouldCancel())
                return;

            /* If we're launching an MVISP server, this thread is done */
            if (spec.getMVISP()) {
                launchMVISP(simDis);
                return;
            }

            /* Otherwise, we build the SimulationRecorded */
            SimulationRecorded simRec = new SimulationRecorded(simDis, pm);
            if (pm.shouldCancel())
                return;
            complete(simRec);
        }
    }

    /**
     * The <code>BarMonitor</code> class is responsible for monitoring the
     * progress of an operation and updating the progress bar on a
     * <code>SimulationInitDialog</code> as progress is made.
     */
    private class BarMonitor extends ca.ualberta.dbs3.math.ProgressMonitor {
        /**
         * Informs the user that a new master operation has started.
         *
         * @param description the description of the master operation.
         */
        public void startMaster(String description) {
            progressReset(description, true);
        }

        /**
         * Informs the user about progress in the master operation.
         *
         * @param description the description of the master operation.
         * @param complete the fraction of the work complete, as a value
         *        between <code>0.0</code> and <code>1.0</code> inclusive.
         */
        public void updateMaster(String description, double complete) {
            /* Compute the percentage to be output to the nearest integer */
            int out = (int) Math.round(complete * 100);
            if (out == 100 && complete < 1.0)
                out = 99;
            progressUpdate(out);
        }

        /**
         * Informs the user that the master operation has indeterminate length.
         *
         * @param description the description of the master operation.
         */
        public void updateMasterIndeterminate(String description) {
            progressReset(description, true);
        }

        /**
         * Informs the user that the master operation has completed.
         *
         * @param description the description of the master operation.
         */
        public void endMaster(String description) {
            progressUpdate(100);
        }
    }

    /**
     * An <code>MVISPServerListener</code> is responsible for listening to the
     * outcome of a running <code>MVISPServerThread</code>. If the outcome is
     * successful, this class is responsible for building the simulation.
     */
    private class MVISPServerListener implements ServerThreadListener {
        /**
         * The discrete simulation around which we will build the recorded
         * simulation wrapper.
         */
        private SimulationDiscrete simulation;

        /**
         * The duration of the simulation chosen during the exchange.
         */
        private long duration;

        /**
         * The amount of movement data sent for each agent; negative if nothing
         * at all has been sent, and 0L if the zero-time data has been sent.
         */
        private long[] sent;

        /**
         * Creates a new <code>MVISPServerListener</code> that will build a
         * recorded simulation around the given simulation.
         *
         * @param simulation the discrete simulation around which to build a
         *        recorded simulation.
         */
        public MVISPServerListener(SimulationDiscrete simulation) {
            this.simulation = simulation;
        }

        /**
         * Called if the <code>ServerThread</code> passes the negotiation
         * phase. Enables the progress bar in the cancel dialog.
         *
         * @param thread the thread that completed negotiation.
         * @param numAgents the number of agents in the simulation.
         * @param durationMilli the number of milliseconds in duration the
         *        simulation will be.
         */
        public void serverThreadExchange(ServerThread thread, int numAgents,
                long durationMilli) {
            this.duration = durationMilli;
            this.sent = new long[numAgents];
            for (int i = 0; i < numAgents; i++)
                this.sent[i] = -1L;
            progressReset(SimulationBuilder.POST_EXCHANGE, true);
        }

        /**
         * Called if the <code>ServerThread</code> sends mobility data. Updates
         * the progress bar.
         *
         * @param thread the thread that sent mobility data.
         * @param agentID the agent for which data was sent.
         * @param timestamp the timestamp of the mobility data sent in
         *        milliseconds.
         */
        public void serverThreadProgress(ServerThread thread, int agentID,
                long timestamp) {
            this.sent[agentID] = timestamp;
            double sumOfFrac = 0.0;
            for (int i = 0; i < this.sent.length; i++) {
                double frac;
                if (this.sent[i] < 0L)
                    frac = 0.0;
                else if (this.duration == 0L)
                    frac = 1.0;
                else
                    frac = ((double) this.sent[i]) / ((double) this.duration);
                sumOfFrac += frac;
            }
            int percent =
                    (int) Math.round((sumOfFrac / this.sent.length) * 100);
            progressUpdate(percent);
        }

        /**
         * Called if the MVISP thread comes to an error-free completion.
         * Completes the construction of the simulation.
         *
         * @param thread the thread that completes.
         */
        public void serverThreadCompleted(ServerThread thread) {
            StateRecord record = mvispThread.getStateRecord();
            SimulationRecorded sim =
                    new SimulationRecorded(this.simulation, record, pm);
            complete(sim);
        }

        /**
         * Called if the MVISP server is killed by the user clicking the cancel
         * button. Does nothing.
         *
         * @param thread the thread that is killed.
         */
        public void serverThreadKilled(ServerThread thread) {}

        /**
         * Called if the MVISP server encounters an error. Displays the error.
         *
         * @param thread the thread that experiences the error.
         * @param error the error that occurred.
         */
        public void serverThreadError(ServerThread thread,
                final Throwable error) {
            Runnable r = new Runnable() {
                public void run() {
                    ApplicationGUI.getApplication().displayError(error, init);
                    constructionThread = null;
                    init.dispose();
                }
            };
            SwingUtilities.invokeLater(r);
        }
    }
}
