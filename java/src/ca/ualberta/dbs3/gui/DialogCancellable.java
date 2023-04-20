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
 * The <code>DialogCancellable</code> class represents a dialog that the user
 * can cancel out of, either by closing the window from the windows system, by
 * clicking on a cancel button, or by pressing the escape key. It also
 * reimplements {@link #setVisible}, causing it to be made visible centered
 * overtop its owner dialog or frame with a default component selected.
 */
public abstract class DialogCancellable extends JDialog {
    /**
     * The owner frame or dialog.
     */
    private Window owner;

    /**
     * The button that cancels this dialog.
     */
    private JButton cancelButton;

    /**
     * Creates a new non-modal <code>DialogCancellable</code> with the given
     * owner, title, and text on the cancel button.
     *
     * @param owner the frame that owns this dialog.
     * @param title the title for the title bar of this dialog.
     * @param cancelButtonText the text to appear on the cancel button.
     */
    public DialogCancellable(Frame owner, String title,
            String cancelButtonText) {
        super(owner, title);
        this.finishConstruction(owner, cancelButtonText);
    }

    /**
     * Creates a new <code>DialogCancellable</code> with the given owner,
     * title, modality, and text on the cancel button.
     *
     * @param owner the frame that owns this dialog.
     * @param title the title for the title bar of this dialog.
     * @param modal <code>true</code> if the dialog will be modal,
     *        <code>false</code> if it will be modeless.
     * @param cancelButtonText the text to appear on the cancel button.
     */
    public DialogCancellable(Frame owner, String title, boolean modal,
            String cancelButtonText) {
        super(owner, title, modal);
        this.finishConstruction(owner, cancelButtonText);
    }

    /**
     * Creates a new non-modal <code>DialogCancellable</code> with the given
     * owner, title, and text on the cancel button.
     *
     * @param owner the dialog that owns this dialog.
     * @param title the title for the title bar of this dialog.
     * @param cancelButtonText the text to appear on the cancel button.
     */
    public DialogCancellable(Dialog owner, String title,
            String cancelButtonText) {
        super(owner, title);
        this.finishConstruction(owner, cancelButtonText);
    }

    /**
     * Creates a new <code>DialogCancellable</code> with the given owner,
     * title, modality, and text on the cancel button.
     *
     * @param owner the dialog that owns this dialog.
     * @param title the title for the title bar of this dialog.
     * @param modal <code>true</code> if the dialog will be modal,
     *        <code>false</code> if it will be modeless.
     * @param cancelButtonText the text to appear on the cancel button.
     */
    public DialogCancellable(Dialog owner, String title, boolean modal,
            String cancelButtonText) {
        super(owner, title, modal);
        this.finishConstruction(owner, cancelButtonText);
    }

    /**
     * Runs the code that is shared across all of the constructors, which
     * installs the listeners for all the different ways to cancel this dialog.
     *
     * @param owner the window that owns this dialog.
     * @param cancelButtonText the text to appear on the cancel button.
     */
    private void finishConstruction(Window owner, String cancelButtonText) {
        this.owner = owner;

        /* Install the listener for the cancel button and window closing */
        UserCancelListener myListener = new UserCancelListener();
        this.addWindowListener(myListener);
        this.cancelButton = new JButton(cancelButtonText);
        this.cancelButton.addActionListener(myListener);

        /* Install the action for the escape key */
        JRootPane rootPane = this.getRootPane();
        InputMap iMap = rootPane
                .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                "escapeAction");
        ActionMap aMap = rootPane.getActionMap();
        aMap.put("escapeAction", new EscapeKeyAction());
    }

    /**
     * Sets the visibility of the dialog, causing it to disappear, or appear
     * centered overtop the owner window.
     *
     * @param visible whether or not this dialog should be visible.
     */
    public void setVisible(boolean visible) {
        if (visible && this.isVisible() == false)
            this.setLocationRelativeTo(this.owner);
        super.setVisible(visible);
    }

    /**
     * Subclasses can override this method to add behaviour that takes place
     * immediately before this dialog is made invisible from the user
     * cancelling (either by clicking the cancel button, closing the dialog
     * from the windowing system, or pressing the escape key).
     */
    protected void userCancelled() {}

    /**
     * Returns the cancel button, which can be placed into a dialog that
     * subclasses <code>DialogCancellable</code>.
     *
     * @return the cancel button for this dialog.
     */
    protected JButton getCancelButton() {
        return this.cancelButton;
    }

    /**
     * Called when the user performs a cancelling action (clicks the cancel
     * button or closes the dialog window), and is responsible for calling
     * {@link #userCancelled} before disposing of the dialog.
     */
    private void userCancel() {
        this.userCancelled();
        this.dispose();
    }

    /**
     * The <code>UserCancelListener</code> listens for the cancel button to be
     * clicked or the window to be closed from the window system. In either
     * case, the listener calls {@link #userCancel}, thereby invoking any
     * subclass cancellation actions and disposing of the dialog.
     */
    private class UserCancelListener extends WindowAdapter
            implements ActionListener {
        /**
         * Called when the user clicks the cancel button.
         *
         * @param e the action that occured, specifically the click of the
         *        cancel button.
         */
        public void actionPerformed(ActionEvent e) {
            userCancel();
        }

        /**
         * Called when the user closes the window from the window system's
         * menu.
         *
         * @param e the window event that caused this method to be called.
         */
        public void windowClosing(WindowEvent e) {
            userCancel();
        }
    }

    /**
     * The <code>EscapeKeyAction</code> class represents the action that should
     * be taken when the escape key is pressed in a cancellable dialog.
     * Specifically, it calls {@link #userCancel}, thereby invoking any
     * subclass cancellation actions and disposing of the dialog.
     */
    private class EscapeKeyAction extends AbstractAction {
        /**
         * Called when the user presses the escape key.
         *
         * @param e the action that occured, specifically the escape key being
         *        pressed.
         */
        public void actionPerformed(ActionEvent e) {
            userCancel();
        }
    }
}
