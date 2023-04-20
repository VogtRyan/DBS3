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
import java.awt.event.*;
import javax.swing.*;

/**
 * The <code>ColourMenuItem</code> class represents a menu item with a colour
 * next to it, corresponding to the current colour of the preference. When
 * clicked, this menu item will open a dialog box to change the colour (that
 * is, there is no need to install an additional <code>ActionListener</code> on
 * this menu item to perform that task).
 */
public abstract class ColourMenuItem extends JMenuItem {
    /**
     * The size in pixels of the colour swatch.
     */
    private static final int ICON_SIZE = 16;

    /**
     * The parent window over which we should display the dialog box.
     */
    private MainFrame parent;

    /**
     * The last dialog that clicking on this menu item opened, or
     * <code>null</code> if none has yet been opened.
     */
    private ColourDialog dialog;

    /**
     * The title that a newly created dialog will have.
     */
    private String dialogTitle;

    /**
     * Creates a new menu item that will create a dialog over the given parent
     * frame.
     *
     * @param parent the parent frame of the dialog that will appear.
     * @param menuTitle the caption of the menu entry.
     * @param dialogTitle the title of the dialog that will appear.
     * @param initial the initial colour to display.
     */
    public ColourMenuItem(MainFrame parent, String menuTitle,
            String dialogTitle, Color initial) {
        super(menuTitle);
        this.parent = parent;
        this.dialog = null;
        this.dialogTitle = dialogTitle;
        this.setIcon(new ColourIcon(initial));
        this.addActionListener(new ClickListener());
    }

    /**
     * Instructs the menu item to re-read the display preferences given to its
     * constructor, and change the icon colour it is displaying appropriately.
     */
    public void rereadPreferences() {
        Color colour = this.getCurrentColour();
        this.setIcon(new ColourIcon(colour));
    }

    /**
     * Returns the current colour of the preference being controlled.
     *
     * @return the current colour.
     */
    protected abstract Color getCurrentColour();

    /**
     * Called when the user sets a new colour for the preference via the
     * dialog.
     *
     * @param colour the new colour.
     */
    protected abstract void userSetColour(Color colour);

    /**
     * The <code>ColourIcon</code> class represents a square of colour that
     * appears next to the text of a menu item.
     */
    private class ColourIcon implements Icon {
        /**
         * Paints the desired colour.
         */
        private ColourPainter painter;

        /**
         * Creates a new <code>ColourIcon</code> in the given colour.
         *
         * @param colour the colour to draw.
         */
        public ColourIcon(Color colour) {
            this.painter = new ColourPainter(colour, ColourMenuItem.ICON_SIZE);
        }

        /**
         * Returns the height of the icon in pixels.
         *
         * @return the height of the icon in pixels.
         */
        public int getIconHeight() {
            return ColourMenuItem.ICON_SIZE;
        }

        /**
         * Returns the width of the icon in pixels.
         *
         * @return the width of the icon in pixels.
         */
        public int getIconWidth() {
            return ColourMenuItem.ICON_SIZE;
        }

        /**
         * Draws the icon on the provided graphics object at the given
         * location.
         *
         * @param c the component on which the icon is being drawn.
         * @param g the graphics object on which to draw.
         */
        public void paintIcon(Component c, Graphics g, int x, int y) {
            this.painter.draw(g, x, y, getBackground());
        }
    }

    /**
     * The <code>ClickListener</code> class is responsible for listening for
     * when this menu item is clicked, and then opening the dialog to modify
     * the colour.
     */
    private class ClickListener implements ActionListener {
        /**
         * Opens this menu item's dialog.
         *
         * @param e the event that caused this method to be called.
         */
        public void actionPerformed(ActionEvent e) {
            if (dialog == null || dialog.isVisible() == false)
                dialog = new ColourDialog(parent, dialogTitle,
                        getCurrentColour(), ColourMenuItem.this);
            parent.displayDialog(dialog);
        }
    }
}
