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

import ca.ualberta.dbs3.cartography.*;
import ca.ualberta.dbs3.commandLine.*;
import java.awt.Window;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.filechooser.*;

/**
 * The <code>DBS3</code> class is a singleton class that represents the entire
 * running DBS3 application, which will automatically terminate once all of its
 * windows have been closed.
 */
public class DBS3 extends ApplicationGUI {
    /**
     * Launches the DBS3 application.
     *
     * @param args the command line arguments given to the Java VM.
     */
    public static void main(String[] args) {
        /* Parse the command line */
        Parser parser = new Parser();
        OptionMapOrDirectory mapLocation = new OptionMapOrDirectory();
        OptionConfig config = new OptionConfig();
        parser.add(mapLocation);
        parser.add(config);
        if (parser.parse(args) == false)
            System.exit(-1);

        /* Create the application */
        final DBS3 application =
                new DBS3(config.getConfigFile(), config.isConfigFileNew());
        final String mapFile = mapLocation.getMapFile();
        final String mapDir = mapLocation.getMapDirectory();

        /* Create the GUI */
        Runnable r = new Runnable() {
            public void run() {
                String toOpen = null;
                if (mapDir != null)
                    toOpen = application.chooseFile(null, mapDir);
                else
                    toOpen = mapFile;

                if (toOpen == null)
                    System.exit(0);
                if (application.createMainFrame(null, toOpen) == false)
                    System.exit(0);
            }
        };
        SwingUtilities.invokeLater(r);
    }

    /**
     * Returns the singleton instance of the running application.
     *
     * @return the running DBS3 application.
     */
    public static DBS3 getApplication() {
        return (DBS3) (ApplicationGUI.getApplication());
    }

    /**
     * The list of running windows for this DBS3 application.
     */
    private ArrayList<MainFrame> myFrames;

    /**
     * The display preferences for all running frames.
     */
    private Preferences prefs;

    /**
     * The original preferences contents, so we can tell whether preferences
     * have changed when we exit. Will be <code>null</code> if the original
     * preferences file failed to load.
     */
    private Preferences prefsOriginal;

    /**
     * The preferences filename.
     */
    private String prefFile;

    /**
     * The hidden constructor for the singleton instance of the DBS3
     * application.
     *
     * @param prefFile the filename of the configuration file.
     * @param newPrefs whether a new configuration file should be created at
     *        the given location.
     */
    private DBS3(String prefFile, boolean newPrefs) {
        /*
         * Turn off anti-aliasing based on an environmental variable. Used to
         * improve performance on machines with very limited graphics
         * capabilities.
         */
        super("DBS3", (Application.getEnvironment("DBS3_NOAA") == null));
        this.myFrames = new ArrayList<MainFrame>();

        /* Initialize preferences */
        this.prefFile = prefFile;
        if (newPrefs) {
            this.prefs = new Preferences();
            this.prefsOriginal = new Preferences(this.prefs);
            this.writePreferencesFile();
        } else {
            try {
                this.prefsOriginal = null;
                this.prefs = Preferences.readFromFile(this.prefFile);
                this.prefsOriginal = new Preferences(this.prefs);
            } catch (Exception excep) {
                String reason = excep.getMessage();
                if (reason == null)
                    reason = "";
                else
                    reason = reason + "\n";
                this.displayError("Failed to load " + this.prefFile + "\n"
                        + reason + "Will use default preferences.", null);
                this.prefs = new Preferences();
                this.prefsOriginal = null;
            }
        }

        /*
         * Add the reread observer to the preferences, which will be notified
         * if the user changes any display preferences.
         */
        this.prefs.addObserver(new RereadObserver());
    }

    /**
     * Opens an <code>SMF</code> file of the user's choosing, and instantiates
     * a {@link MainFrame} to display it. The <code>SMF</code> file format is
     * used to describe a DBS3 map, and is documented in the
     * {@link MapFileParser} class.
     *
     * @param parent the parent <code>MainFrame</code> on which the open-file
     *        dialog is displayed, and from which the dialog's initial location
     *        is derived.
     */
    public void openFile(MainFrame parent) {
        String location = new File(parent.getMapFilename()).getParent();
        String filename = this.chooseFile(parent, location);
        if (filename != null)
            this.createMainFrame(parent, filename);
    }

    /**
     * Opens a new <code>MainFrame</code> with the given file.
     *
     * @param parent the parent window on which to display error messages, or
     *        <code>null</code> to display error messages in some central
     *        location not relative to any window.
     * @param filename the map file to open.
     * @return <code>true</code> if a window was created, or <code>false</code>
     *         if no window was created.
     */
    private boolean createMainFrame(MainFrame parent, String filename) {
        /*
         * Make best effort to find canonical path, so that it is more reliable
         * when we attempt to determine what directory that file is located in.
         */
        String canonical = filename;
        try {
            canonical = new File(filename).getCanonicalPath();
        } catch (Exception excep) {
        }

        MainFrame mf;
        try {
            mf = new MainFrame(canonical, this.prefs);
        } catch (Exception excep) {
            String reason = excep.getMessage();
            if (reason == null)
                reason = "";
            else
                reason = "\n" + reason;
            this.displayError("Cannot open map file " + canonical + reason,
                    parent);
            return false;
        }

        this.myFrames.add(mf);
        mf.addWindowListener(new DBS3TerminationListener());
        mf.setLocationByPlatform(true);
        try {
            mf.setVisible(true);
        } catch (Exception excep) {
            this.displayError(excep, parent);
        }
        return true;
    }

    /**
     * Displays a dialog to open a map file and returns the filename of the map
     * file chosen by the user.
     *
     * @param parent the parent <code>MainFrame</code> over which to display
     *        the open dialog, or <code>null</code> if there is no parent
     *        frame.
     * @param location the location in the file system at which to open the
     *        search dialog.
     * @return a filename to open, or <code>null</code> if the user cancels.
     */
    private String chooseFile(MainFrame parent, String location) {
        /*
         * Make best effort to find canonical path, so that the open dialog is
         * better behaved.
         */
        String canonical = location;
        try {
            canonical = new File(location).getCanonicalPath();
        } catch (Exception excep) {
        }

        JFileChooser chooser = new JFileChooser(canonical);
        FileNameExtensionFilter filter =
                new FileNameExtensionFilter("DBS3 Maps", "smf");
        chooser.setFileFilter(filter);
        int ret = chooser.showOpenDialog(parent);
        if (ret == JFileChooser.APPROVE_OPTION)
            return chooser.getSelectedFile().getAbsolutePath();
        else
            return null;
    }

    /**
     * Writes the current preferences to the preferences file.
     */
    private void writePreferencesFile() {
        try {
            this.prefs.writeToFile(this.prefFile);
        } catch (IOException ioe) {
            String reason = ioe.getMessage();
            if (reason == null)
                reason = "";
            else
                reason = "\n" + reason;
            this.displayError("Cannot write preferences file: " + this.prefFile
                    + reason);
        }
    }

    /**
     * Closes all of the open windows and quits the application.
     */
    public void quit() {
        /*
         * This code executes on the Event Dispatch Thread, so the myFrames
         * list won't be modified until after this function is complete and the
         * windowClosed method of DBS3TerminationListener is called.
         */
        for (MainFrame mf : this.myFrames)
            mf.terminateAndDispose();
    }

    /**
     * Runs once all of the frames have been disposed, asking if preferences
     * should be saved.
     */
    private void applicationShutdown() {
        if (this.prefsOriginal == null
                || this.prefs.equals(this.prefsOriginal) == false) {
            String msg = "Your display options have been modified since you "
                    + "started.\nDo you wish to save your new options?";
            String title = "Display Options Changed";
            String[] options = {"Save options", "Do not save"};
            int ret = JOptionPane.showOptionDialog(null, msg, title,
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);
            if (ret == 0)
                this.writePreferencesFile();
        }
        System.exit(0);
    }

    /**
     * The <code>DBS3TerminationListener</code> listens for all of the
     * {@link MainFrame}s to close. When the final one closes, the termination
     * listener kills the program.
     */
    private class DBS3TerminationListener extends WindowAdapter {
        /**
         * Records when an application window is disposed, terminating the
         * application if no more registered windows remain.
         *
         * @param e the window event that caused this method to be called.
         */
        public void windowClosed(WindowEvent e) {
            Window w = e.getWindow();
            myFrames.remove(w);
            if (myFrames.isEmpty())
                applicationShutdown();
        }
    }

    /**
     * The <code>RereadObserver</code> class is responsible for observing the
     * application-wide preferences, and informing all of the active windows to
     * reread the preferences set if any changes are made to it. That is, if
     * the user modifies their preferences in one window, all of the open
     * windows will be notified of the change.
     */
    private class RereadObserver implements PreferencesObserver {
        /**
         * Called when a <code>Preferences</code> object that is being observed
         * is modified, and informs all of the active windows to update their
         * display to the new preference values.
         *
         * @param preferences the preferences that were modified.
         */
        public void preferencesChanged(Preferences preferences) {
            for (MainFrame frame : myFrames)
                frame.rereadPreferences();
        }
    }

    /**
     * The <code>OptionMapOrDirectory</code> class represents the choice
     * between either: opening a map, or viewing the contents of a directory in
     * the file system to choose a map.
     */
    private static class OptionMapOrDirectory extends OptionMapFile {
        /**
         * The choice to view a directory.
         */
        private Choice directory;

        /**
         * The argument passed to the directory choice.
         */
        private ArgumentString directoryArg;

        /**
         * Creates a new <code>OptionMapOrDirectory</code> to add to a parser.
         */
        public OptionMapOrDirectory() {
            this.directory = new Choice("directory");
            this.directoryArg = new ArgumentString("mapDirectory");
            this.directory.add(this.directoryArg);
            this.add(this.directory);
        }

        /**
         * Returns the directory to view, or <code>null</code> if a single map
         * file was specified instead.
         *
         * @return the directory to open, or <code>null</code>.
         */
        public String getMapDirectory() {
            if (this.directory.isActive())
                return this.directoryArg.getValue();
            else
                return null;
        }

        /**
         * Returns <code>null</code>, so that this option is omitted from the
         * summary output of options chosen by the user when the application is
         * run.
         *
         * @return <code>null</code> always.
         */
        public String getDescription() {
            return null;
        }
    }

    /**
     * The <code>OptionConfig</code> class represents a choice between reading
     * an existing preferences file or starting a new preferences file.
     */
    private static class OptionConfig extends Option {
        /**
         * The default configuration file to use if none is specified.
         */
        public static final String DEFAULT_CONFIG = "../conf/dbs3GUI.conf";

        /**
         * The choice to start a new configuration file.
         */
        private Choice newConf;

        /**
         * The filename argument to the choice to open an existing
         * configuration file.
         */
        private ArgumentString existingArg;

        /**
         * The filename argument to choice to create a new configuration file.
         */
        private ArgumentString newArg;

        /**
         * Creates a new <code>OptionConfig</code> to be added to a parser.
         */
        public OptionConfig() {
            super("Configuration file");
            Choice existing = new Choice("config");
            this.newConf = new Choice("newConfig");
            this.existingArg = new ArgumentString("configFile",
                    OptionConfig.DEFAULT_CONFIG);
            this.newArg = new ArgumentString("configFile");
            existing.add(this.existingArg);
            this.newConf.add(this.newArg);
            this.addDefault(existing);
            this.add(this.newConf);
        }

        /**
         * Returns the name of the configuration file to use, either existing
         * or new.
         *
         * @return the configuration file to use.
         */
        public String getConfigFile() {
            if (this.newConf.isActive())
                return this.newArg.getValue();
            else
                return this.existingArg.getValue();
        }

        /**
         * Returns whether or not a new configuration file should be created.
         *
         * @return <code>true</code> if the configuration filename specifies
         *         where a new file should be written, or <code>false</code> if
         *         it specifies a file that should already exist.
         */
        public boolean isConfigFileNew() {
            return this.newConf.isActive();
        }
    }
}
