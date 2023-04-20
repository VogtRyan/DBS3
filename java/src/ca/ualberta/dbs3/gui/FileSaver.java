/*
 * Copyright (c) 2011-2023 Ryan Vogt <rvogt@ualberta.ca>
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
import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.*;

/**
 * The <code>FileSaver</code> class is a dialog box that can be used to choose
 * where to save a file. It remembers the last location where a file was saved
 * by the running application, and it will ask for confirmation if a file
 * already exists.
 */
public class FileSaver {
    /**
     * The last file saved by the running application, or <code>null</code> if
     * no file has yet been saved.
     */
    private static File last = null;

    /**
     * The default filename to use, excluding the extension.
     */
    private String defaultFile;

    /**
     * The type of file to be saved by this file saver.
     */
    private String type;

    /**
     * The description of the file type to be saved by this file saver.
     */
    private String description;

    /**
     * Creates a new <code>FileSaver</code> that will save files of the given
     * type.
     *
     * @param defaultFile the default filename to use, excluding the extension.
     * @param type the file extension.
     * @param description the long description of the type of file to be saved.
     */
    public FileSaver(String defaultFile, String type, String description) {
        this.defaultFile = defaultFile;
        this.type = type;
        this.description = description;
    }

    /**
     * Returns a user-chosen file to save, or <code>null</code> if the user
     * cancels the save operation.
     *
     * @param parent the parent component over which to display the dialog.
     * @return the file to save or <code>null</code>.
     */
    public File getFile(Component parent) {
        /* Set up the save dialog */
        JFileChooser chooser = new ConfirmChooser(FileSaver.last);
        FileNameExtensionFilter filter =
                new FileNameExtensionFilter(this.description, this.type);
        chooser.setFileFilter(filter);

        /* Enter a default filename */
        String directory = "";
        File defFile;
        int i = 0;
        if (FileSaver.last != null) {
            try {
                directory = FileSaver.last.getCanonicalFile().getParent()
                        + System.getProperty("file.separator");
            } catch (Exception excep) {
            }
        }
        while (true) {
            if (i == 0)
                defFile = new File(
                        directory + this.defaultFile + "." + this.type);
            else
                defFile = new File(directory + this.defaultFile + "-" + i + "."
                        + this.type);
            if (defFile.exists() == false)
                break;
            i++;
        }
        chooser.setSelectedFile(defFile);

        /* Display the dialog and return the selection */
        int r = chooser.showSaveDialog(parent);
        if (r != JFileChooser.APPROVE_OPTION)
            return null;
        File ret = chooser.getSelectedFile();
        FileSaver.last = ret;
        return ret;
    }

    /**
     * The <code>ConfirmChooser</code> is a file chooser dialog that will ask
     * for confirmation before overriding a file.
     */
    private class ConfirmChooser extends JFileChooser {
        /**
         * Creates a new <code>ConfirmChooser</code> starting in the given
         * directory.
         *
         * @param currentDirectory the current directory in which to start.
         */
        public ConfirmChooser(File currentDirectory) {
            super(currentDirectory);
        }

        /**
         * Called when the user approves of a selection in the dialog box. Will
         * display a confirmation dialog before overriding a file.
         */
        public void approveSelection() {
            File file = this.getSelectedFile();
            if (file != null && file.exists()) {
                if (this.confirmOverwrite() == false)
                    return;
            }
            super.approveSelection();
        }

        /**
         * Displays a message to the user to confirm that they want to
         * overwrite an existing file.
         *
         * @return <code>true</code> if the user wants to overwrite the
         *         existing file, otherwise <code>false</code>.
         */
        private boolean confirmOverwrite() {
            String file = this.getSelectedFile().getAbsolutePath();
            String title = "Replace existing file";
            String message = "The file \"" + file
                    + "\" already exists.  Do you wish to replace it?";
            String[] options = {"Replace file", "Cancel"};
            int i = JOptionPane.showOptionDialog(this, message, title,
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, options, options[1]);
            return (i == JOptionPane.YES_OPTION);
        }
    }
}
