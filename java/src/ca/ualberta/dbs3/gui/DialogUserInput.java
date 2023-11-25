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
import javax.swing.event.*;

/**
 * The <code>DialogUserInput</code> class represents a dialog into which the
 * user can enter information, with subclasses of <code>DialogUserInput</code>
 * being informed whenever the user input changes. The user has the ability to
 * either cancel from the dialog (by clicking a cancel button, closing the
 * window from the windowing system, or pressing escape) or confirm their
 * choices (either by clicking an OK button or pressing enter).
 */
public abstract class DialogUserInput extends DialogCancellable {
    /**
     * Unused serialization version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The OK button.
     */
    private JButton okButton;

    /**
     * The listener to add to all input components, responsible for listening
     * for user input changes to any component and updating if the OK button
     * should be active according to whether the new user input is legal.
     */
    private InputListener listener;

    /**
     * Creates a new <code>DialogUserInput</code> with the given owner, title,
     * and text on the OK and cancel buttons.
     *
     * @param owner the frame that owns this dialog.
     * @param title the title for the title bar of this dialog.
     * @param okButtonText the text to appear on the OK button.
     * @param cancelButtonText the text to appear on the cancel button.
     */
    public DialogUserInput(Frame owner, String title, String okButtonText,
            String cancelButtonText) {
        super(owner, title, cancelButtonText);
        this.finishConstruction(okButtonText);
    }

    /**
     * Creates a new <code>DialogUserInput</code> with the given owner, title,
     * and text on the OK and cancel buttons.
     *
     * @param owner the dialog that owns this dialog.
     * @param title the title for the title bar of this dialog.
     * @param okButtonText the text to appear on the OK button.
     * @param cancelButtonText the text to appear on the cancel button.
     */
    public DialogUserInput(Dialog owner, String title, String okButtonText,
            String cancelButtonText) {
        super(owner, title, cancelButtonText);
        this.finishConstruction(okButtonText);
    }

    /**
     * Runs the code that is shared across all of the constructors, creating
     * listeners for both the OK button and the enter key-press.
     *
     * @param okButtonText the text to appear on the OK button.
     */
    private void finishConstruction(String okButtonText) {
        /* Create the OK button */
        this.okButton = new JButton(okButtonText);
        this.okButton.addActionListener(new OKButtonListener());

        /* Bind an action to the enter key */
        JRootPane rootPane = this.getRootPane();
        InputMap iMap = rootPane
                .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enterAction");
        ActionMap aMap = rootPane.getActionMap();
        aMap.put("enterAction", new EnterKeyAction());

        /* Create the listeners for all registered components */
        this.listener = new InputListener();
    }

    /**
     * Subclasses can override this method to add behaviour that takes place
     * whenever the any registered component is modified.
     *
     * @return <code>true</code> if the user's current input is legal, or
     *         <code>false</code> to disable the OK button.
     */
    protected boolean inputChanged() {
        return true;
    }

    /**
     * Subclasses can override this method to add behaviour that takes place
     * whenever the user clicks the OK button or presses the enter key while
     * the OK button is enabled.
     */
    protected void userOK() {
        this.closeDialog();
    }

    /**
     * Saves the size of this dialog and disposes of it.
     */
    protected void closeDialog() {
        DialogFactory.rememberSize(this);
        this.dispose();
    }

    /**
     * Adds the given component to the list of registered input components that
     * will be monitored for changes.
     *
     * @param component the component to add.
     */
    protected void registerInputComponent(JTextField component) {
        component.getDocument().addDocumentListener(this.listener);
    }

    /**
     * Adds the given component to the list of registered input components that
     * will be monitored for changes.
     *
     * @param component the component to add.
     */
    protected void registerInputComponent(AbstractButton component) {
        component.addActionListener(this.listener);
    }

    /**
     * Adds the given component to the list of registered input components that
     * will be monitored for changes.
     *
     * @param component the component to add.
     */
    protected void registerInputComponent(JComboBox<String> component) {
        component.addActionListener(this.listener);
        component.addKeyListener(new ComboEnterListener(component));
    }

    /**
     * Returns the OK button, which can be placed into a dialog that subclasses
     * <code>DialogUserInput</code>.
     *
     * @return the cancel button for this dialog.
     */
    protected JButton getOKButton() {
        return this.okButton;
    }

    /**
     * The <code>InputListener</code> class is responsible for listening for
     * changes to any registered user input component. When changes occur, we
     * test to see if the OK button should still be enabled.
     */
    private class InputListener implements ActionListener, DocumentListener {
        /**
         * Tests the validity of user input when a button is clicked or a combo
         * box changed, and enables or disables the OK button accordingly.
         *
         * @param e the event that occurred, which is ignored.
         */
        public void actionPerformed(ActionEvent e) {
            okButton.setEnabled(inputChanged());
        }

        /**
         * Tests the validity of user input when an attribute change is made to
         * a text field, and enables or disables the OK button accordingly.
         *
         * @param e the event that caused this method to be called, which is
         *        ignored.
         */
        public void changedUpdate(DocumentEvent e) {
            okButton.setEnabled(inputChanged());
        }

        /**
         * Tests the validity of user input when an insertion is made to a text
         * field, and enables or disables the OK button accordingly.
         *
         * @param e the event that caused this method to be called, which is
         *        ignored.
         */
        public void insertUpdate(DocumentEvent e) {
            okButton.setEnabled(inputChanged());
        }

        /**
         * Tests the validity of user input when a deletion change is made to a
         * text field, and enables or disables the OK button accordingly.
         *
         * @param e the event that caused this method to be called, which is
         *        ignored.
         */
        public void removeUpdate(DocumentEvent e) {
            okButton.setEnabled(inputChanged());
        }
    }

    /**
     * The <code>OKButtonListener</code> class is responsible for listening for
     * when the user clicks the OK button.
     */
    private class OKButtonListener implements ActionListener {
        /**
         * Called when the user clicks the OK button.
         *
         * @param e the event that caused this method to be called.
         */
        public void actionPerformed(ActionEvent e) {
            userOK();
        }
    }

    /**
     * The <code>EnterKeyAction</code> class is responsible for translating a
     * press of the enter key into a click of the OK button (which may or may
     * not be enabled).
     */
    private class EnterKeyAction extends AbstractAction {
        /**
         * Unused serialization version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Translates a press of the enter key into a click of the OK button.
         *
         * @param e the action that occured.
         */
        public void actionPerformed(ActionEvent e) {
            if (okButton.isEnabled())
                userOK();
        }
    }

    /**
     * The <code>ComboEnterListener</code> is responsible for listening for
     * when the user presses the enter key while a <code>JComboBox</code> has
     * the focus. Without the presence of this listener, the default behaviour
     * for this key press when a <code>JComboBox</code> has focus is to select
     * the currently highlighted item in the popup if it is open, and to do
     * nothing if it is closed. By adding this listener, we can keep the
     * default behaviour when the popup is open, but perform the OK button
     * action if the popup is closed.
     */
    private class ComboEnterListener implements KeyListener {
        /**
         * The box on which this listener is listening for the enter key.
         */
        private JComboBox<String> box;

        /**
         * Creates a new <code>ComboEnterListener</code> that listens for the
         * enter key on the given combo box.
         *
         * @param box the box on which to listen for the enter key.
         */
        public ComboEnterListener(JComboBox<String> box) {
            this.box = box;
        }

        /**
         * Performs the dialog's OK button action if the OK button is active
         * and the popup is not visible, at the moment the enter key is
         * pressed.
         *
         * @param e the event that caused this method to be invoked.
         */
        public void keyPressed(KeyEvent e) {
            if ((e.getKeyCode() == KeyEvent.VK_ENTER)
                    && (this.box.isPopupVisible() == false)
                    && okButton.isEnabled())
                userOK();
        }

        /**
         * Does nothing when a key is released on the combo box.
         *
         * @param e the event that caused this method to be invoked.
         */
        public void keyReleased(KeyEvent e) {}

        /**
         * Does nothing when a key is typed on the combo box.
         *
         * @param e the event that caused this method to be invoked.
         */
        public void keyTyped(KeyEvent e) {}
    }
}
