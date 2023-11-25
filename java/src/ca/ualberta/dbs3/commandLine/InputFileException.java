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

package ca.ualberta.dbs3.commandLine;

import java.io.*;

/**
 * The <code>InputFileException</code> class is used to handle errors that
 * occur while parsing an input file.
 */
public class InputFileException extends Exception {
    /**
     * Unused serialization version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new <code>InputFileException</code> with the given error
     * having happened in the given file.
     *
     * @param filename the file in which the error occurred.
     * @param err a human-readable description of the error.
     */
    public InputFileException(String filename, String err) {
        super("Error with " + InputFileException.parseName(filename) + ": "
                + err);
    }

    /**
     * Creates a new <code>InputFileException</code> with the given error
     * having happened on the given line of the given file.
     *
     * @param filename the file in which the error occurred.
     * @param line the line on which the error occurred, which must be greater
     *        than zero.
     * @param err a human-readable description of the error.
     * @throws IllegalArgumentException if <code>line</code> is less than or
     *         equal to zero.
     */
    public InputFileException(String filename, int line, String err) {
        super("Error with " + InputFileException.parseName(filename)
                + ", line " + line + ": " + err);
        if (line <= 0)
            throw new IllegalArgumentException("Invalid line number " + line);
    }

    /**
     * Attempts to choose the best filename to display for an erroneous file.
     *
     * @param filename a known filename for the file, or <code>null</code> or
     *        the empty string if no filename is known.
     * @return the best attempt filename to display.
     */
    private static String parseName(String filename) {
        if (filename == null || filename.isEmpty())
            return "<unknown file>";
        File f = new File(filename);
        return f.getName();
    }
}
