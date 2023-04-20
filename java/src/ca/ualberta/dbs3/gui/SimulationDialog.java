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
import ca.ualberta.dbs3.math.*;
import ca.ualberta.dbs3.simulations.*;
import javax.swing.*;

/**
 * The <code>SimulationDialog</code> class represents a window that the user
 * uses to configure a simulation to run.
 */
public class SimulationDialog extends DialogUserInput {
    /**
     * The main frame to which this simulation configuration window belongs.
     */
    private MainFrame parent;

    /**
     * The map on which the simulations are being run.
     */
    private Map map;

    /**
     * The text field containing the number of agents in the simulation.
     */
    private UserIntegerField numAgents;

    /**
     * The text field containing the duration of the simulation.
     */
    private UserDoubleField duration;

    /**
     * The various units (seconds, minutes, etc.) that duration could be in.
     */
    private JComboBox<String> durationUnits;

    /**
     * The text field containing the minimum speed in metres per second.
     */
    private UserDoubleField minSpeed;

    /**
     * The text field containing the maximum speed in metres per second.
     */
    private UserDoubleField maxSpeed;

    /**
     * The distributions of agent speeds.
     */
    private JComboBox<String> speedDistribution;

    /**
     * The text field containing the minimum pause time in seconds.
     */
    private UserDoubleField minPause;

    /**
     * The text field containing the maximum pause time in seconds.
     */
    private UserDoubleField maxPause;

    /**
     * The distributions of agent pause times.
     */
    private JComboBox<String> pauseDistribution;

    /**
     * The text field containing the destination chooser alpha value.
     */
    private UserDoubleField alpha;

    /**
     * The text field containing the destination chooser delta value.
     */
    private UserDoubleField delta;

    /**
     * The text field containing the destination chooser radius value.
     */
    private UserIntegerField radius;

    /**
     * The various pathfinders available.
     */
    private JComboBox<String> pathfinder;

    /**
     * The text field containing the random seed to use.
     */
    private UserIntegerField seed;

    /**
     * The checkbox to enable the MVISP server.
     */
    private UserEnabler portCheck;

    /**
     * The text field containing the port number.
     */
    private UserIntegerField port;

    /**
     * The most recently started simulation builder.
     */
    private SimulationBuilder builder;

    /**
     * Creates a new <code>SimulationDialog</code> that belongs to the given
     * parent frame.
     *
     * @param parent the parent frame to which this dialog belongs.
     * @param map the map used to build simulations.
     * @param spec the simulation specification from which to build the
     *        dialog's initial settings.
     */
    public SimulationDialog(MainFrame parent, Map map,
            SimulationSpecification spec) {
        super(parent, "Simulation Specification", "Start Simulation",
                "Cancel");
        this.parent = parent;
        this.map = map;
        this.builder = null;

        /* Create and layout all of the elements */
        this.layoutElements(spec);
        this.registerComponents();
        DialogFactory.setSize(this);
    }

    /**
     * Terminates all threads that the <code>SimulationDialog</code> may be
     * running.
     */
    public void terminateAllThreads() {
        if (this.builder != null)
            this.builder.terminateAllThreads();
    }

    /**
     * Passes a constructed <code>SimulationRecorded</code> to the parent
     * window, then closes this dialog.
     *
     * @param simulation the constructed simulation.
     * @param spec the specification used to build that simulation.
     */
    public void receive(SimulationRecorded simulation,
            SimulationSpecification spec) {
        this.parent.setSimulation(simulation, spec);
        this.closeDialog();
    }

    /**
     * Ensure that any running threads are killed if the user cancels.
     */
    protected void userCancelled() {
        this.terminateAllThreads();
    }

    /**
     * Called when the user clicks the OK button or presses the enter key while
     * the OK button is enabled. Begins construction of the simulation.
     */
    protected void userOK() {
        /* Don't let anything happen if there is still a thread */
        if (this.builder != null && this.builder.isRunning())
            return;

        /* Parse user input */
        SimulationSpecification spec = this.getSpecification();
        if (spec == null)
            return;

        /* Launch the builder threads */
        this.builder = new SimulationBuilder(this.map, spec, this);
        this.builder.build();
    }

    /**
     * Called when the user's input to any component in this dialog changes.
     *
     * @return <code>true</code> if the user's current input is legal, or
     *         <code>false</code> to disable the OK button.
     */
    protected boolean inputChanged() {
        return (this.getSpecification() != null);
    }

    /**
     * Returns a <code>SimulationSpecification</code> based on the user's
     * current input, or <code>null</code> if the user's input is invalid. Also
     * updates the legality of all user input fields.
     *
     * @return the current specification or <code>null</code>.
     */
    private SimulationSpecification getSpecification() {
        boolean inputGood = true;

        /* Check if the number of agents is legal input */
        int numAgents = this.numAgents.getInteger();
        if (numAgents <= 0) {
            this.numAgents.setLegal(false);
            inputGood = false;
        } else
            this.numAgents.setLegal(true);

        /* Verify that the speed and pause ranges are sane */
        if (this.checkRange(this.minSpeed, this.maxSpeed) == false)
            inputGood = false;
        if (this.checkRange(this.minPause, this.maxPause) == false)
            inputGood = false;

        /* Check if the duration is legal input */
        double duration = this.duration.getDouble();
        int durationUnit = this.durationUnits.getSelectedIndex();
        if (duration < 0.0 || OptionDuration.getDuration(duration,
                durationUnit) == null) {
            this.duration.setLegal(false);
            inputGood = false;
        } else
            this.duration.setLegal(true);

        /* Check if the destination selection parameters are legal */
        double alpha = this.alpha.getDouble();
        double delta = this.delta.getDouble();
        int radius = this.radius.getInteger();
        if (alpha < 0.0) {
            this.alpha.setLegal(false);
            inputGood = false;
        } else
            this.alpha.setLegal(true);
        if (delta < 0.0) {
            this.delta.setLegal(false);
            inputGood = false;
        } else
            this.delta.setLegal(true);
        if (radius < 0) {
            this.radius.setLegal(false);
            inputGood = false;
        } else
            this.radius.setLegal(true);

        /* Check if the seed is legal */
        long seed = this.seed.getLong();
        if (seed < 0) {
            this.seed.setLegal(false);
            inputGood = false;
        } else
            this.seed.setLegal(true);

        /* Legal ports are non-zero unsigned integers */
        int port = this.port.getInteger();
        if (port < OptionPort.MIN_PORT || port > OptionPort.MAX_PORT)
            port = OptionPort.MIN_PORT - 1;
        boolean mvisp = this.portCheck.isSelected();
        if (mvisp) {
            if (port == OptionPort.MIN_PORT - 1) {
                this.port.setLegal(false);
                inputGood = false;
            } else
                this.port.setLegal(true);
        }

        if (inputGood == false)
            return null;
        Range speed = OptionRange.getRange(this.minSpeed.getDouble(),
                this.maxSpeed.getDouble(),
                this.speedDistribution.getSelectedIndex());
        Range pause = OptionRange.getRange(this.minPause.getDouble(),
                this.maxPause.getDouble(),
                this.pauseDistribution.getSelectedIndex());
        int pathfinder = this.pathfinder.getSelectedIndex();
        return new SimulationSpecification(numAgents, speed, pause, duration,
                durationUnit, pathfinder, alpha, delta, radius, seed, mvisp,
                port);
    }

    /**
     * Verifies that two fields used to input a range have positive,
     * non-crossing values and update their text colours accordingly.
     *
     * @param minField the lower bound input.
     * @param maxField the upper bound input.
     * @return <code>true</code> if the range is sane, otherwise
     *         <code>false</code>.
     */
    private boolean checkRange(UserDoubleField minField,
            UserDoubleField maxField) {
        /*
         * Range input can be invalid if either input is invalid, or if the
         * bounds cross.
         */
        double min = minField.getDouble();
        double max = maxField.getDouble();
        if (min < 0.0 || max < 0.0 || max < min) {
            if (min < 0.0 || (max >= 0.0 && max < min))
                minField.setLegal(false);
            else
                minField.setLegal(true);
            if (max < 0.0 || (min >= 0.0 && max < min))
                maxField.setLegal(false);
            else
                maxField.setLegal(true);
            return false;
        } else {
            minField.setLegal(true);
            maxField.setLegal(true);
            return true;
        }
    }

    /**
     * Lays out all of the elements to display in this dialog.
     *
     * @param spec the specification containing the initial values of the
     *        fields.
     */
    private void layoutElements(SimulationSpecification spec) {
        DialogFactory factory = new DialogFactory("Number of agents:");
        this.numAgents = new UserIntegerField(spec.getNumAgents());
        factory.add(this.numAgents);

        factory.addRow("Simulation duration:");
        this.duration = new UserDoubleField(spec.getDuration());
        this.durationUnits = new JComboBox<String>(OptionDuration.getUnits());
        this.durationUnits.setSelectedIndex(spec.getDurationUnit());
        factory.add(this.duration);
        factory.add(this.durationUnits);

        factory.addRow("Agent speed:");
        Range speed = spec.getSpeed();
        this.minSpeed = new UserDoubleField(speed.getMin());
        this.maxSpeed = new UserDoubleField(speed.getMax());
        factory.add(this.minSpeed);
        factory.add(new JLabel("-"));
        factory.add(this.maxSpeed);
        factory.add(new JLabel("m/s"));

        factory.addRow("Speed distribution:");
        this.speedDistribution =
                new JComboBox<String>(OptionRange.getDistributions());
        int speedIndex = OptionRange.getDistribution(speed);
        this.speedDistribution.setSelectedIndex(speedIndex);
        factory.add(this.speedDistribution);

        factory.addRow("Agent pause time:");
        Range pause = spec.getPause();
        this.minPause = new UserDoubleField(pause.getMin());
        this.maxPause = new UserDoubleField(pause.getMax());
        factory.add(this.minPause);
        factory.add(new JLabel("-"));
        factory.add(this.maxPause);
        factory.add(new JLabel("s"));

        factory.addRow("Pause time distribution:");
        this.pauseDistribution =
                new JComboBox<String>(OptionRange.getDistributions());
        int pauseIndex = OptionRange.getDistribution(pause);
        this.pauseDistribution.setSelectedIndex(pauseIndex);
        factory.add(this.pauseDistribution);

        factory.addRow("Centrality bias exponent:");
        this.alpha = new UserDoubleField(spec.getAlpha());
        factory.add(this.alpha);

        factory.addRow("Distance decay exponent:");
        this.delta = new UserDoubleField(spec.getDelta());
        factory.add(this.delta);

        factory.addRow("Centrality computation radius:");
        this.radius = new UserIntegerField(spec.getRadius());
        factory.add(this.radius);

        factory.addRow("Pathfinding:");
        this.pathfinder =
                new JComboBox<String>(OptionPathfinder.getPathfinders());
        this.pathfinder.setSelectedIndex(spec.getPathfinder());
        factory.add(this.pathfinder);

        factory.addRow("PRNG seed:");
        this.seed = new UserIntegerField(spec.getSeed());
        factory.add(this.seed);

        factory.addRow("MVISP server:");
        this.portCheck = new UserEnabler(spec.getMVISP());
        JLabel portLabel = new JLabel("Port");
        this.portCheck.addControlledComponent(portLabel);
        this.port = new UserIntegerField(spec.getPort());
        this.portCheck.addControlledComponent(this.port);
        factory.add(this.portCheck);
        factory.add(portLabel);
        factory.add(this.port);

        this.add(factory.build(this.getOKButton(), this.getCancelButton()));
    }

    /**
     * Registers all of the input components that will be monitored for
     * changes.
     */
    private void registerComponents() {
        this.registerInputComponent(this.numAgents);
        this.registerInputComponent(this.duration);
        this.registerInputComponent(this.durationUnits);
        this.registerInputComponent(this.minSpeed);
        this.registerInputComponent(this.maxSpeed);
        this.registerInputComponent(this.minPause);
        this.registerInputComponent(this.maxPause);
        this.registerInputComponent(this.alpha);
        this.registerInputComponent(this.delta);
        this.registerInputComponent(this.radius);
        this.registerInputComponent(this.pathfinder);
        this.registerInputComponent(this.seed);
        this.registerInputComponent(this.portCheck);
        this.registerInputComponent(this.port);
    }
}
