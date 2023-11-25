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

import java.awt.*;
import javax.swing.*;

/**
 * The <code>SimulationInitDialog</code> represents a small dialog with a
 * cancel button that the user can click to cancel the construction of a
 * simulation (potentially including the execution of an MVISP server).
 */
public class SimulationInitDialog extends DialogCancellable {
    /**
     * The default text that appears in the status area.
     */
    private static final String DEFAULT_TEXT =
            "Waiting for simulation initialization to begin...";

    /**
     * Unused serialization version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The simulation builder to inform if the action is cancelled.
     */
    private SimulationBuilder builder;

    /**
     * The label displaying the current status of the initialization to the
     * user.
     */
    private JLabel status;

    /**
     * A progress bar to display progress of some operation.
     */
    private JProgressBar progress;

    /**
     * Creates a new <code>SimulationInitDialog</code> belonging to the given
     * <code>SimulationDialog</code>, with an indeterminate progress bar.
     *
     * @param parent the parent dialog that was used to configure the
     *        simulation.
     * @param builder the builder to inform in case of a user cancel action.
     */
    public SimulationInitDialog(SimulationDialog parent,
            SimulationBuilder builder) {
        super(parent, "Initializing Simulation", true, "Cancel");
        this.builder = builder;
        this.layoutElements();
        this.setText(SimulationInitDialog.DEFAULT_TEXT);
        this.progressIndeterminate();
        this.pack();
        this.setMinimumSize(this.getSize());
        this.setResizable(false);
    }

    /**
     * Sets the descriptive text above the progress bar to the given string.
     *
     * @param description the description to display.
     */
    public void setText(String description) {
        this.status.setText(description);
    }

    /**
     * Sets the progress bar to be disabled.
     */
    public void progressDisable() {
        this.progress.setStringPainted(false);
        this.progress.setValue(0);
        this.progress.setIndeterminate(false);
    }

    /**
     * Sets the progress bar to be in an enabled, in indeterminate mode.
     */
    public void progressIndeterminate() {
        this.progress.setStringPainted(false);
        this.progress.setValue(0);
        this.progress.setIndeterminate(true);
    }

    /**
     * Sets the percentage indicated on the progress bar, which is activated
     * and set to a determinate mode.
     *
     * @param percent the percentage to show.
     * @throws IllegalArgumentException if <code>percent</code> is less than
     *         zero or greater than 100.
     */
    public void progressUpdate(int percent) {
        if (percent < 0 || percent > 100)
            throw new IllegalArgumentException("Invalid percentage");
        this.progress.setStringPainted(true);
        this.progress.setIndeterminate(false);
        this.progress.setValue(percent);
    }

    /**
     * Kills all the construction and MVISP threads if the user cancels.
     */
    protected void userCancelled() {
        this.builder.terminateAllThreads();
    }

    /**
     * Places all of the elements in their correct places.
     */
    private void layoutElements() {
        /* Constant constraints */
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout gridBag = new GridBagLayout();
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;

        /* Lay out the message, progress bar, and the cancel button */
        this.status = new JLabel(SimulationInitDialog.DEFAULT_TEXT);
        this.progress = new JProgressBar();
        JButton cancelButton = this.getCancelButton();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(DialogFactory.MAJOR_SPACE,
                DialogFactory.MAJOR_SPACE, DialogFactory.ROW_SPACE,
                DialogFactory.MAJOR_SPACE);
        gridBag.addLayoutComponent(this.status, gbc);
        gbc.insets = new Insets(0, DialogFactory.MAJOR_SPACE,
                DialogFactory.MAJOR_SPACE, DialogFactory.MAJOR_SPACE);
        gridBag.addLayoutComponent(this.progress, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gridBag.addLayoutComponent(cancelButton, gbc);

        /* Add the elements */
        this.setLayout(gridBag);
        this.add(this.status);
        this.add(this.progress);
        this.add(cancelButton);
    }
}
