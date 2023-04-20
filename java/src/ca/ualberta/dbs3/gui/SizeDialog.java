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

/**
 * The <code>SizeDialog</code> dialog allows the user to change the size of
 * agents in the display.
 */
public class SizeDialog extends DialogUserInput {
    /**
     * The text field containing the size of agents.
     */
    private UserIntegerField size;

    /**
     * The preference set that this dialog can modify.
     */
    private Preferences prefs;

    /**
     * Creates a new <code>SizeDialog</code> belonging to the given main frame.
     *
     * @param parent the parent frame of this dialog.
     * @param preferences the preference set that this dialog can modify.
     */
    public SizeDialog(MainFrame parent, Preferences preferences) {
        super(parent, "Set Agent Size", "Set Size", "Cancel");

        /* Create and register the text field */
        this.prefs = preferences;
        this.size = new UserIntegerField(this.prefs.getAgentSize());
        this.registerInputComponent(this.size);

        /* Layout the elements */
        DialogFactory factory = new DialogFactory("Agent diameter:");
        factory.add(this.size);
        this.add(factory.build(this.getOKButton(), this.getCancelButton()));
        DialogFactory.setSize(this);
    }

    /**
     * Checks if the new input is legal, meaning if it is non-zero.
     *
     * @return <code>true</code> if the input is legal, <code>false</code>
     *         otherwise.
     */
    protected boolean inputChanged() {
        int v = this.getInputSize();
        boolean legal = (v != 0);
        this.size.setLegal(legal);
        return legal;
    }

    /**
     * Sets the application-wide size of agents in simulation displays.
     */
    protected void userOK() {
        int v = this.getInputSize();
        if (v != 0)
            this.prefs.setAgentSize(v);
        this.closeDialog();
    }

    /**
     * Returns the current size the user has input into the text field, or code
     * <code>0</code> on invalid input.
     *
     * @return the current size in the input or <code>0</code>.
     */
    private int getInputSize() {
        int v = this.size.getInteger();
        if (v < 0)
            return 0;
        else
            return v;
    }
}
