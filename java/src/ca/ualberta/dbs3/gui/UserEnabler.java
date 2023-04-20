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

import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

/**
 * The <code>UserEnabler</code> class represents a check box with no caption
 * that, when checked or unchecked, respectively enables or disables other
 * components.
 */
public class UserEnabler extends JCheckBox {
    /**
     * The components that this enabler enables or disables.
     */
    private ArrayList<JComponent> components;

    /**
     * Creates a new <code>UserEnabler</code> in the given initial state.
     *
     * @param selected whether or not the enabler is initially checked.
     */
    public UserEnabler(boolean selected) {
        super("", selected);
        this.components = new ArrayList<JComponent>();
        this.addActionListener(new UserEnablerListener());
    }

    /**
     * Adds a new component that will be controlled by this enabler.
     *
     * @param component the component to be controlled by this enabler.
     */
    public void addControlledComponent(JComponent component) {
        component.setEnabled(this.isSelected());
        this.components.add(component);
    }

    /**
     * Sets whether this enabler is checked or not, and enables or disables all
     * of its controlled components.
     *
     * @param selected whether or not this enabler is checked.
     */
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        this.doEnable();
    }

    /**
     * Enables or disables all of the controlled components based on the
     * current selection of this enabler.
     */
    private void doEnable() {
        boolean enable = this.isSelected();
        for (JComponent component : this.components)
            component.setEnabled(enable);
    }

    /**
     * The <code>UserEnablerListener</code> is responsible for listening for
     * when the user changes the selection value of this enabler.
     */
    private class UserEnablerListener implements ActionListener {
        /**
         * Called when the user clicks the enabler, changing its selection
         * value. This action will enable or disable all of the controlled
         * components.
         *
         * @param e the event that caused this method to be called, which is
         *        ignored.
         */
        public void actionPerformed(ActionEvent e) {
            doEnable();
        }
    }
}
