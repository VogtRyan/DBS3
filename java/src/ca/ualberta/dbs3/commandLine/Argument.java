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

/**
 * The <code>Argument</code> class represents an argument that an option must
 * take on the command line.
 */
public abstract class Argument {
    /**
     * A human-readable description of the argument. This description must not
     * include whitespace, for when it gets printed out in a usage message.
     */
    protected String description;

    /**
     * Constructs a new <code>Argument</code>.
     *
     * @param description the human-readable description of this argument that
     *        must not include any whitespace.
     * @throws IllegalArgumentException if <code>description</code> contains
     *         whitespace.
     */
    public Argument(String description) {
        this.description = description;
        char[] cArray = this.description.toCharArray();
        for (int i = 0; i < cArray.length; i++) {
            if (Character.isWhitespace(cArray[i]))
                throw new IllegalArgumentException(
                        "Whitespace in description");
        }
    }

    /**
     * Returns a human-readable <code>String</code> representation of the
     * argument.
     *
     * @return a <code>String</code> representation of the argument.
     */
    public String toString() {
        return this.description;
    }

    /**
     * Parses the argument off the command line.
     *
     * @param args the command line arguments, as supplied to the Java VM.
     * @param index the index at which to check for the argument.
     * @return the first index after the argument.
     * @throws ParseException if the argument cannot be parsed from the command
     *         line.
     */
    public abstract int parse(String args[], int index) throws ParseException;

    /**
     * Resets the argument back to its default value.
     */
    public abstract void reset();
}
