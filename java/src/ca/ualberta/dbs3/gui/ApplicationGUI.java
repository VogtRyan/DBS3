/*
 * Copyright (c) 2009-2023 Ryan Vogt <rvogt@ualberta.ca>
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

import ca.ualberta.dbs3.commandLine.*;
import java.awt.*;
import java.awt.desktop.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * The <code>ApplicationGUI</code> class is responsible for providing
 * functionality shared across all GUI applications, such as displaying errors,
 * and adapting to specific operating system environments.
 */
public abstract class ApplicationGUI extends Application {
    /**
     * Whether or not we are running on OS X / macOS.
     */
    private static final boolean isOSX =
            System.getProperty("os.name").toLowerCase().startsWith("mac os x");

    /**
     * The singleton GUI application instance.
     */
    private static ApplicationGUI singleton = null;

    /**
     * The name of this application.
     */
    private String name;

    /**
     * Whether or not to use anti-aliasing. Disabling anti-aliasing can
     * significantly improve performance on very old systems.
     */
    private boolean useAA = true;

    /**
     * Creates a new <code>ApplicationGUI</code> and initializes any
     * OS-dependent GUI components.
     *
     * @param name the name of this application.
     * @param useAA whether anti-aliasing should be used in this application.
     * @throws RuntimeException if operating system-dependent initializtion
     *         fails.
     * @throws IllegalStateException if more than one
     *         <code>ApplicationGUI</code> object is created.
     */
    protected ApplicationGUI(String name, boolean useAA) {
        /* Duplicate singletons prevented in superclass */
        super();
        ApplicationGUI.singleton = this;
        this.name = name;
        this.useAA = useAA;

        ErrorHandlerGUI ehg = new ErrorHandlerGUI();
        Thread.setDefaultUncaughtExceptionHandler(ehg);

        /* OS X special integration */
        if (ApplicationGUI.isOSX) {
            System.setProperty("Xdock:name", name);
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }

        /*
         * Wipe any system-optional default menus, and handle a system-provided
         * quit
         */
        if (Desktop.isDesktopSupported() == false)
            return;
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
            desktop.setQuitHandler(new DesktopQuitHandler());
        }
        if (desktop.isSupported(Desktop.Action.APP_MENU_BAR))
            desktop.setDefaultMenuBar(new JMenuBar());
    }

    /**
     * Quits the application.
     */
    public abstract void quit();

    /**
     * Returns whether or not this GUI application should attempt to use
     * anti-aliasing.
     *
     * @return <code>true</code> if we should attempt to use AA,
     *         <code>false</code> otherwise.
     */
    public boolean useAA() {
        return this.useAA;
    }

    /**
     * Opens a dialog box displaying the given error.
     *
     * @param error the exception or error from which to display the message.
     */
    public void displayError(Throwable error) {
        String str = Application.throwableToString(error, false);
        this.displayError(str, null);
    }

    /**
     * Opens a dialog box displaying the given error.
     *
     * @param error the error string to display.
     */
    public void displayError(String error) {
        this.displayError(error, null);
    }

    /**
     * Opens a dialog box on the given parent component showing the given
     * error.
     *
     * @param error the exception or error from which to display the message.
     * @param parent the parent component of the dialog.
     */
    public void displayError(Throwable error, Component parent) {
        String str = Application.throwableToString(error, false);
        this.displayError(str, parent);
    }

    /**
     * Opens a dialog box on the given parent component showing the given
     * error.
     *
     * @param error the error string to display.
     * @param parent the parent component of the dialog.
     */
    public void displayError(String error, Component parent) {
        if (error == null)
            error = "Unknown error";
        JOptionPane.showMessageDialog(parent, error, this.name + " Error",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Returns the singleton <code>ApplicationGUI</code> instance, or
     * <code>null</code> if no instance has been created.
     *
     * @return the singleton instance or <code>null</code>.
     */
    public static ApplicationGUI getApplication() {
        return ApplicationGUI.singleton;
    }

    /**
     * Returns whether or not the operating system automatically provides a
     * "Quit" item in an application's menu. If the operating system's
     * functionality is unknown, this method returns <code>false</code>.
     *
     * @return <code>true</code> if a "Quit" item is known to be automatically
     *         provided, <code>false</code> otherwise.
     */
    public static boolean quitProvided() {
        return ApplicationGUI.isOSX;
    }

    /**
     * Returns the accelerator mask (i.e., special key) that should be used for
     * menu items in an application.
     *
     * @return the accelerator mask used on this system.
     */
    public static int getAcceleratorMask() {
        if (ApplicationGUI.isOSX)
            return InputEvent.META_DOWN_MASK;
        else
            return InputEvent.CTRL_DOWN_MASK;
    }

    /**
     * The <code>DesktopQuitHandler</code> listens for invocations of the
     * built-in desktop's Quit command, and acts on such an event to quit the
     * application.
     */
    private class DesktopQuitHandler implements QuitHandler {
        /**
         * Invoked when the desktop's built-in Quit command is called, quitting
         * the application.
         *
         * @param e the event that triggers the built-in Quit command.
         * @param response ignored, as this is the object that would be
         *        notified of the application's intent to continue.
         */
        @Override
        public void handleQuitRequestWith(QuitEvent e, QuitResponse response) {
            quit();
        }
    }

    /**
     * The <code>ErrorHandlerGUI</code> class is responsible for catching
     * uncaught throwables and displaying an error dialog.
     */
    private static class ErrorHandlerGUI
            implements Thread.UncaughtExceptionHandler {
        /**
         * Displays an error when the given thread terminates due to an
         * uncaught exception.
         *
         * @param t the thread that is terminated.
         * @param e the throwable error that caused the termination.
         */
        public void uncaughtException(Thread t, Throwable e) {
            String disp = Application.throwableToString(e, false);
            String pr = Application.throwableToString(e, true);
            System.err.println(pr);
            ApplicationGUI.singleton.displayError(disp);
        }
    }
}
