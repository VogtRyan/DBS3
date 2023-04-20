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
import java.util.*;
import javax.swing.*;

/**
 * The <code>DialogFactory</code> class is responsible for constructing user
 * input dialogs in which users must enter information into a series of rows.
 * This class also provides variables and methods for standardizing the layout
 * and size of all such dialogs.
 */
public class DialogFactory {
    /**
     * The size of the border, in pixels, that dialogs should place around the
     * edge of the dialog, and between major dialog elements.
     */
    public static final int MAJOR_SPACE = 20;

    /**
     * The size of the border, in pixels, that dialogs should place between
     * rows in the dialog.
     */
    public static final int ROW_SPACE = 15;

    /**
     * The size of the space, in pixels, that dialogs should place between
     * elements of the same row.
     */
    public static final int SUB_SPACE = 5;

    /**
     * The number of pixels wider than the minimum possible (packed) width that
     * user input dialogs should use as their minimum (user-settable) width.
     */
    private static final int EXTRA_WIDTH = 50;

    /**
     * The mapping from subclasses of <code>DialogUserInput</code> to the
     * previously saved sizes for those dialogs.
     */
    private static HashMap<Class<? extends DialogUserInput>, DialogSize> sizes =
            new HashMap<Class<? extends DialogUserInput>, DialogSize>();

    /**
     * The rows that have been added to an instance of
     * <code>DialogFactory</code>.
     */
    private ArrayList<PanelRow> rows;

    /**
     * Sets the size of the dialog based on previously saved sizes for this
     * class of dialog.
     *
     * @param dialog the dialog whose size and minimum size will be set.
     */
    public static void setSize(DialogUserInput dialog) {
        /* Determine our class, and find the stored size */
        Class<? extends DialogUserInput> myClass = dialog.getClass();
        DialogSize ds = DialogFactory.sizes.get(myClass);

        /*
         * If we have no stored size, we have to compute a default one. Use the
         * size of the dialog with well-packed components as the default.
         */
        if (ds == null) {
            dialog.pack();
            Dimension d = dialog.getSize();
            d.width += DialogFactory.EXTRA_WIDTH;
            ds = new DialogSize(d);
            DialogFactory.sizes.put(myClass, ds);
        }

        /* Set the dialog size */
        dialog.setMinimumSize(ds.getMinimumSize());
        dialog.setSize(ds.getInitialSize());
    }

    /**
     * Remembers the current size of this dialog, causing any newly created
     * dialogs of that class to be the same size.
     *
     * @param dialog the dialog whose size is to be remembered.
     * @throws IllegalStateException if the {@link #setSize} method has never
     *         been invoked on a dialog of this class.
     */
    public static void rememberSize(DialogUserInput dialog) {
        Class<? extends DialogUserInput> myClass = dialog.getClass();
        DialogSize ds = DialogFactory.sizes.get(myClass);
        if (ds == null)
            throw new IllegalStateException("No stored size for " + myClass);
        ds.setInitialSize(dialog.getSize());
    }

    /**
     * Creates a new <code>DialogFactory</code> that will build a panel with
     * one row.
     *
     * @param header the header for the first row.
     */
    public DialogFactory(String header) {
        this.rows = new ArrayList<PanelRow>();
        this.addRow(header);
    }

    /**
     * Adds a new row with the given header to the panel that will be built by
     * this dialog factory.
     *
     * @param header the header of the new row.
     */
    public void addRow(String header) {
        this.rows.add(new PanelRow(header));
    }

    /**
     * Adds the given component to the current row in this factory.
     *
     * @param component the component to add to the current row.
     * @throws IllegalArgumentException if the component has already been added
     *         to this factory.
     */
    public void add(Component component) {
        /* Check the legality of the addition */
        if (this.contains(component))
            throw new IllegalArgumentException("Duplicate component");

        /* Add the component to the current row */
        PanelRow row = this.rows.get(this.rows.size() - 1);
        row.add(component);
    }

    /**
     * Creates the panel containing the two given buttons, plus all of the
     * contents that have been added to this factory.
     *
     * @param okButton the OK button to put in the panel.
     * @param cancelButton the cancel button to put in the panel.
     * @return the constructed panel.
     * @throws IllegalArgumentException if the OK button or cancel button have
     *         already been added to the factory, or if they are not unique.
     */
    public JPanel build(JButton okButton, JButton cancelButton) {
        if (this.contains(okButton) || this.contains(cancelButton)
                || okButton == cancelButton)
            throw new IllegalArgumentException("Invalid buttons");

        /* Constant constraints */
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout gridBag = new GridBagLayout();
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;

        /* Get the component panels */
        JPanel top = this.buildContentPanel();
        JPanel bottom = this.buildOKCancelPanel(okButton, cancelButton);

        /* Lay the two panels out top-to-bottom */
        gbc.weighty = 1.0;
        gbc.insets = new Insets(DialogFactory.MAJOR_SPACE,
                DialogFactory.MAJOR_SPACE, DialogFactory.MAJOR_SPACE,
                DialogFactory.MAJOR_SPACE);
        gbc.fill = GridBagConstraints.BOTH;
        gridBag.addLayoutComponent(top, gbc);
        gbc.insets = new Insets(0, DialogFactory.MAJOR_SPACE,
                DialogFactory.MAJOR_SPACE, DialogFactory.MAJOR_SPACE);
        gbc.weighty = 1.0 / this.rows.size();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gridBag.addLayoutComponent(bottom, gbc);

        /* Add the elements */
        JPanel panel = new JPanel(gridBag);
        panel.add(top);
        panel.add(bottom);
        return panel;
    }

    /**
     * Builds the sub-panel consisting of the rows of user input.
     *
     * @return the constructed panel.
     */
    private JPanel buildContentPanel() {
        /* Constant constraints */
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout gridBag = new GridBagLayout();
        JPanel panel = new JPanel(gridBag);
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        /* Add each row to the panel */
        gbc.gridy = 0;
        for (PanelRow row : this.rows) {
            int spaceAbove = (gbc.gridy == 0 ? 0 : DialogFactory.ROW_SPACE);

            /* First, add the left-side header */
            gbc.weightx = 0.0;
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets =
                    new Insets(spaceAbove, 0, 0, DialogFactory.MAJOR_SPACE);
            JLabel header = new JLabel(row.getHeader());
            gridBag.addLayoutComponent(header, gbc);
            panel.add(header);

            /* Now add the right side */
            gbc.weightx = 1.0;
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.insets = new Insets(spaceAbove, 0, 0, 0);
            JPanel right = this.buildRightSide(row);
            gridBag.addLayoutComponent(right, gbc);
            panel.add(right);

            gbc.gridy++;
        }

        return panel;
    }

    /**
     * Builds the sub-sub-panel consisting of the input area (not including the
     * header label) for each row.
     *
     * @param row the row for which to build the input area.
     * @return the constructed panel.
     */
    private JPanel buildRightSide(PanelRow row) {
        /* Constant constraints */
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout gridBag = new GridBagLayout();
        JPanel panel = new JPanel(gridBag);
        gbc.weighty = 1.0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        /* Insert the elements into the panel horizontally */
        gbc.insets = new Insets(0, 0, 0, 0);
        int numComponents = row.getNumComponents();
        boolean containsText = false;
        for (int i = 0; i < numComponents; i++) {
            Component component = row.getComponent(i);
            if (component instanceof JTextField) {
                containsText = true;
                break;
            }
        }
        for (gbc.gridx = 0; gbc.gridx < numComponents; gbc.gridx++) {
            Component component = row.getComponent(gbc.gridx);
            if ((component instanceof JTextField) || (containsText == false))
                gbc.weightx = 1.0;
            else
                gbc.weightx = 0.0;
            gridBag.addLayoutComponent(component, gbc);
            panel.add(component);
            gbc.insets = new Insets(0, DialogFactory.SUB_SPACE, 0, 0);
        }

        return panel;
    }

    /**
     * Builds the sub-panel consisting of the OK and cancel buttons.
     *
     * @param okButton the OK button.
     * @param cancelButton the cancel button.
     * @return the constructed panel.
     */
    private JPanel buildOKCancelPanel(JButton okButton, JButton cancelButton) {
        /* Force the button sizes equal */
        this.equalizeButtonSize(okButton, cancelButton);

        /* Constant constraints */
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout gridBag = new GridBagLayout();
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;

        /* The OK button on the left */
        gbc.insets = new Insets(0, 0, 0, DialogFactory.MAJOR_SPACE);
        gridBag.addLayoutComponent(okButton, gbc);

        /* The cancel button on the right */
        gbc.insets = new Insets(0, 0, 0, 0);
        gridBag.addLayoutComponent(cancelButton, gbc);

        /* Make the panel */
        JPanel panel = new JPanel(gridBag);
        panel.add(okButton);
        panel.add(cancelButton);
        return panel;
    }

    /**
     * Sets the size of the two given buttons to be equal.
     *
     * @param b1 the first button.
     * @param b2 the second button.
     */
    private void equalizeButtonSize(JButton b1, JButton b2) {
        Dimension d1 = b1.getPreferredSize();
        Dimension d2 = b2.getPreferredSize();
        d1.width = d1.width > d2.width ? d1.width : d2.width;
        d1.height = d1.height > d2.height ? d1.height : d2.height;
        b1.setPreferredSize(d1);
        b2.setPreferredSize(d1);
    }

    /**
     * Returns whether or not the given component already exists in this
     * factory.
     *
     * @param component the component for which to test.
     * @return <code>true</code> if it already exists in this factory,
     *         <code>false</code> otherwise.
     */
    private boolean contains(Component component) {
        for (PanelRow row : this.rows) {
            int n = row.getNumComponents();
            for (int i = 0; i < n; i++) {
                Component c = row.getComponent(i);
                if (c == component)
                    return true;
            }
        }
        return false;
    }

    /**
     * The <code>PanelRow</code> class represents a single row in a panel being
     * constructed by a <code>DialogFactory</code>.
     */
    private class PanelRow {
        /**
         * The heading for this row.
         */
        private String header;

        /**
         * All of the components that should appear to the right of the
         * heading.
         */
        private ArrayList<Component> components;

        /**
         * Creates a new <code>PanelRow</code> with the given heading.
         *
         * @param header the heading for this row.
         */
        public PanelRow(String header) {
            this.header = header;
            this.components = new ArrayList<Component>();
        }

        /**
         * Returns the heading for this row.
         *
         * @return the heading for this row.
         */
        public String getHeader() {
            return this.header;
        }

        /**
         * Adds the given component as the next component to appear in this
         * row.
         *
         * @param component the component to add.
         */
        public void add(Component component) {
            this.components.add(component);
        }

        /**
         * Returns the number of components that should appear to the right of
         * the heading in this row.
         *
         * @return the number of components.
         */
        public int getNumComponents() {
            return this.components.size();
        }

        /**
         * Gets the component, indexed from zero, that should appear after the
         * heading.
         *
         * @param index the index of the component.
         * @return the component at the given index.
         */
        public Component getComponent(int index) {
            return this.components.get(index);
        }
    }

    /**
     * The <code>DialogSize</code> class is just a set of two variables,
     * representing the initial and minimum sizes of a dialog.
     */
    private static class DialogSize {
        /**
         * The initial size of a dialog.
         */
        private Dimension initialSize;

        /**
         * The minimum size of a dialog.
         */
        private Dimension minimumSize;

        /**
         * Creates a new <code>DialogSize</code> with the given size as both
         * its initial and minimum size.
         *
         * @param size the initial and minimum size.
         */
        public DialogSize(Dimension size) {
            this.initialSize = new Dimension(size);
            this.minimumSize = new Dimension(size);
        }

        /**
         * Returns the initial size of a dialog.
         *
         * @return the initial size.
         */
        public Dimension getInitialSize() {
            return new Dimension(this.initialSize);
        }

        /**
         * Returns the minimum size of a dialog.
         *
         * @return the minimum size.
         */
        public Dimension getMinimumSize() {
            return new Dimension(this.minimumSize);
        }

        /**
         * Changes the initial size of a dialog to the given size.
         *
         * @param d the new initial size.
         */
        public void setInitialSize(Dimension d) {
            this.initialSize = new Dimension(d);
        }
    }
}
