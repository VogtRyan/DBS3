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
 * The <code>ArgumentString</code> class represents a {@link String} argument
 * that an option must take on the command line.
 */
public class ArgumentString extends Argument {
    /**
     * The value of the argument.
     */
    private String value;

    /**
     * The default value of the argument.
     */
    private String myDefault;

    /**
     * Constructs a new <code>ArgumentString</code> with the given description
     * and the empty <code>String</code> as its default value.
     *
     * @param description the human-readable description of this argument that
     *        must not include any whitespace.
     * @throws IllegalArgumentException if <code>description</code> contains
     *         whitespace.
     */
    public ArgumentString(String description) {
        this(description, "");
    }

    /**
     * Constructs a new <code>ArgumentString</code> with the given description
     * and default value.
     *
     * @param description the human-readable description of this argument that
     *        must not include any whitespace.
     * @param defaultValue the default value of this argument if the associated
     *        option is not provided on the command line.
     * @throws IllegalArgumentException if <code>description</code> contains
     *         whitespace.
     */
    public ArgumentString(String description, String defaultValue) {
        super(description);
        this.value = defaultValue;
        this.myDefault = defaultValue;
    }

    /**
     * Parses the {@link String} argument off the command line.
     *
     * @param args the command line arguments, as supplied to the Java VM.
     * @param index the index at which to check for the argument.
     * @return the first index after the argument.
     * @throws ParseException if the argument cannot be parsed from the command
     *         line at the given index.
     */
    public int parse(String args[], int index) throws ParseException {
        if (index < 0 || index >= args.length)
            throw new ParseException("Invalid index into command line");
        this.value = args[index];
        return index + 1;
    }

    /**
     * Resets the argument back to its default value.
     */
    public void reset() {
        this.value = this.myDefault;
    }

    /**
     * Gets the value of the argument.
     *
     * @return the <code>String</code> value of the argument.
     */
    public String getValue() {
        return this.value;
    }
}
