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
 * The <code>ColourDialog</code> dialog allows the user to change the colour of
 * some element in the display.
 */
public class ColourDialog extends DialogUserInput {
    /**
     * The size of the preview swatch.
     */
    private static final int PREVIEW_SIZE = 50;

    /**
     * Unused serialization version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The red text field.
     */
    private UserIntegerField red;

    /**
     * The green text field.
     */
    private UserIntegerField green;

    /**
     * The blue text field.
     */
    private UserIntegerField blue;

    /**
     * The alpha text field.
     */
    private UserIntegerField alpha;

    /**
     * The preview display of what the user has entered.
     */
    private PreviewSwatch swatch;

    /**
     * The controller of this dialog, which is the menu item (with a colour
     * displayed next to it) that triggered the dialog.
     */
    private ColourMenuItem controller;

    /**
     * Creates a new <code>ColourDialog</code> with the given title and initial
     * colour.
     *
     * @param parent the parent frame.
     * @param title the title of the dialog.
     * @param initial the initial colour to be displayed in the dialog.
     * @param controller the menu item to be informed when the colour is
     *        selected.
     */
    public ColourDialog(MainFrame parent, String title, Color initial,
            ColourMenuItem controller) {
        super(parent, title, "Set Colour", "Cancel");
        this.controller = controller;
        this.layoutElements(initial);
        DialogFactory.setSize(this);
    }

    /**
     * Initializes all of the elements and lays them out in the dialog.
     *
     * @param toShow the initial colour to display.
     */
    private void layoutElements(Color toShow) {
        /* Create the text fields and preview with the current colour */
        this.red = new UserIntegerField(toShow.getRed());
        this.green = new UserIntegerField(toShow.getGreen());
        this.blue = new UserIntegerField(toShow.getBlue());
        this.alpha = new UserIntegerField(toShow.getAlpha());
        this.swatch = new PreviewSwatch(toShow);

        /* Register all of the input components */
        this.registerInputComponent(this.red);
        this.registerInputComponent(this.green);
        this.registerInputComponent(this.blue);
        this.registerInputComponent(this.alpha);

        /* Layout the elements */
        DialogFactory factory = new DialogFactory("Red:");
        factory.add(this.red);
        factory.addRow("Green:");
        factory.add(this.green);
        factory.addRow("Blue:");
        factory.add(this.blue);
        factory.addRow("Alpha:");
        factory.add(this.alpha);
        factory.addRow("Preview:");
        factory.add(this.swatch);
        this.add(factory.build(this.getOKButton(), this.getCancelButton()));
    }

    /**
     * Updates the colour of the preview swatch and tests the legality of the
     * text fields.
     *
     * @return <code>true</code> if the input is legal, <code>false</code>
     *         otherwise.
     */
    protected boolean inputChanged() {
        int r = this.getValue(this.red);
        int g = this.getValue(this.green);
        int b = this.getValue(this.blue);
        int a = this.getValue(this.alpha);
        if (r < 0 || g < 0 || b < 0 || a < 0) {
            this.swatch.setColour(null);
            return false;
        } else {
            this.swatch.setColour(new Color(r, g, b, a));
            return true;
        }
    }

    /**
     * Sets the colour of the menu item that triggered this dialog to the
     * current colour.
     */
    protected void userOK() {
        Color myColour =
                new Color(this.getValue(this.red), this.getValue(this.green),
                        this.getValue(this.blue), this.getValue(this.alpha));
        this.controller.userSetColour(myColour);
        this.closeDialog();
    }

    /**
     * Tests if the value in the given field is in the legal range of 0-255. If
     * so, return the value; otherwise, return -1. In either case, update the
     * legality of the colour field.
     *
     * @param field the field to test.
     * @return the field value or <code>-1</code>.
     */
    private int getValue(UserIntegerField field) {
        int v = field.getInteger();
        if (v < 0 || v > 255) {
            field.setLegal(false);
            return -1;
        } else {
            field.setLegal(true);
            return v;
        }
    }

    /**
     * The <code>PreviewSwatch</code> class is responsible for displaying a
     * preview of the input colour to the user.
     */
    private class PreviewSwatch extends JPanel {
        /**
         * Unused serialization version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Paints the desired colour.
         */
        private ColourPainter painter;

        /**
         * Creates a new <code>PreviewSwatch</code> displaying the given
         * colour.
         *
         * @param colour the initial colour to display.
         */
        public PreviewSwatch(Color colour) {
            super();
            Dimension d = new Dimension(ColourDialog.PREVIEW_SIZE,
                    ColourDialog.PREVIEW_SIZE);
            this.setPreferredSize(d);
            this.setMinimumSize(d);
            this.painter =
                    new ColourPainter(colour, ColourDialog.PREVIEW_SIZE);
        }

        /**
         * Sets the colour being displayed to the given colour.
         *
         * @param colour the colour to display.
         */
        public void setColour(Color colour) {
            this.painter.setColour(colour);
            this.repaint();
        }

        /**
         * Invoked by Swing to draw components. Do not invoke
         * <code>paint</code> directly.
         *
         * @param g the <code>Graphics</code> context in which to paint.
         */
        public void paint(Graphics g) {
            this.painter.draw(g, 0, 0, getBackground());
        }
    }
}
