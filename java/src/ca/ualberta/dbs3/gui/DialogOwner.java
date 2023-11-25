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
import java.util.*;
import javax.swing.*;

/**
 * The <code>DialogOwner</code> class represents a frame that can intelligently
 * refocus its dialogs as individual dialogs are disposed.
 */
public class DialogOwner extends JFrame {
    /**
     * Unused serialization version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The linked list of all dialogs owned by this frame, with elements at the
     * front of the list having had focus more recently.
     */
    private LinkedList<JDialog> dialogs;

    /**
     * The window listener that listens to the focus change on the registered
     * dialogs.
     */
    private DialogListener listener;

    /**
     * Creates a new <code>DialogOwner</code> with the given title.
     *
     * @param title the title of the new frame.
     */
    public DialogOwner(String title) {
        super(title);
        this.listener = new DialogListener();
        this.dialogs = new LinkedList<JDialog>();
    }

    /**
     * Displays the given dialog, or gives the dialog focus if it is already
     * visible.
     *
     * @param dialog the dialog to display.
     */
    protected void displayDialog(JDialog dialog) {
        if (this.dialogs.contains(dialog))
            dialog.toFront();
        else {
            dialog.addWindowListener(this.listener);
            this.dialogs.add(dialog);
            dialog.setVisible(true);
        }
    }

    /**
     * The <code>DialogListener</code> class is responsible for listening for
     * when registered dialogs receive focus (updating the linked list of
     * registered dialogs) and for when they are disposed (potentially
     * refocusing another dialog in its place).
     */
    private class DialogListener extends WindowAdapter {
        /**
         * Updates the focus linked list when a dialog becomes active.
         *
         * @param e the window event that caused this method to be called.
         */
        public void windowActivated(WindowEvent e) {
            JDialog dialog = (JDialog) (e.getWindow());
            dialogs.remove(dialog);
            dialogs.addFirst(dialog);
        }

        /**
         * Resets the focused dialog when a dialog has been disposed, handing
         * the focus off to the most-recently focused remaining dialog.
         *
         * @param e the window event that caused this method to be called.
         */
        public void windowClosed(WindowEvent e) {
            /* Remove the registered dialog */
            JDialog dialog = (JDialog) (e.getWindow());
            dialogs.remove(dialog);

            /* Chooses a new dialog to focus */
            Iterator<JDialog> it = dialogs.iterator();
            while (it.hasNext()) {
                JDialog nextDialog = it.next();
                if (nextDialog.isVisible() == false
                        || nextDialog.isFocusableWindow() == false)
                    continue;
                if (nextDialog.isFocused())
                    return;
                nextDialog.toFront();
                return;
            }

            /* Focus the owner frame if there is no such dialog */
            toFront();
        }
    }
}
