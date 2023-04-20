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
 * The <code>ArgumentLong</code> class represents a {@link Long} argument that
 * an option must take on the command line.
 */
public class ArgumentLong extends Argument {
    /**
     * The value of the argument.
     */
    private long value;

    /**
     * The default value of the argument.
     */
    private long myDefault;

    /**
     * The minimum value of the argument.
     */
    private long min;

    /**
     * The maximum value of the argument.
     */
    private long max;

    /**
     * Constructs a new <code>ArgumentLong</code> with the given description
     * and <code>0L</code> as its default value.
     *
     * @param description the human-readable description of this argument that
     *        must not include any whitespace.
     * @throws IllegalArgumentException if <code>description</code> contains
     *         whitespace.
     */
    public ArgumentLong(String description) {
        this(description, 0L, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    /**
     * Constructs a new <code>ArgumentLong</code> with the given description
     * and default value.
     *
     * @param description the human-readable description of this argument that
     *        must not include any whitespace.
     * @param defaultValue the default value of this argument if the associated
     *        option is not provided on the command line.
     * @throws IllegalArgumentException if <code>description</code> contains
     *         whitespace.
     */
    public ArgumentLong(String description, long defaultValue) {
        this(description, defaultValue, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    /**
     * Constructs a new <code>ArgumentLong</code> with the given description,
     * default value, and minimum value.
     *
     * @param description the human-readable description of this argument that
     *        must not include any whitespace.
     * @param defaultValue the default value of this argument if the associated
     *        option is not provided on the command line.
     * @param min the minimum value of the argument.
     * @throws IllegalArgumentException if <code>description</code> contains
     *         whitespace, or if <code>defaultValue</code> is less than
     *         <code>min</code>.
     */
    public ArgumentLong(String description, long defaultValue, long min) {
        this(description, defaultValue, min, Long.MAX_VALUE);
    }

    /**
     * Constructs a new <code>ArgumentLong</code> with the given description,
     * default value, and minimum and maximum values.
     *
     * @param description the human-readable description of this argument that
     *        must not include any whitespace.
     * @param defaultValue the default value of this argument if the associated
     *        option is not provided on the command line.
     * @param min the minimum value of the argument.
     * @param max the maximum value of the argument.
     * @throws IllegalArgumentException if <code>description</code> contains
     *         whitespace, if <code>defaultValue</code> is less than
     *         <code>min</code> or larger than <code>max</code>, or if
     *         <code>min</code> is larger than <code>max</code>.
     */
    public ArgumentLong(String description, long defaultValue, long min,
            long max) {
        super(description);
        this.value = defaultValue;
        this.myDefault = defaultValue;
        this.min = min;
        this.max = max;
        if (this.min > this.max)
            throw new IllegalArgumentException("Minimum and maximum cross");
        if (this.value < this.min || this.value > this.max)
            throw new IllegalArgumentException("Value out of bounds");
    }

    /**
     * Parses the {@link Long} argument off the command line.
     *
     * @param args the command line arguments, as supplied to the Java VM.
     * @param index the index at which to check for the argument.
     * @return the first index after the argument.
     * @throws ParseException if the argument cannot be parsed from the command
     *         line at the given index.
     */
    public int parse(String args[], int index) throws ParseException {
        long val = this.value;

        if (index < 0 || index >= args.length)
            throw new ParseException("Invalid index into command line");
        try {
            val = Long.valueOf(args[index]);
        } catch (NumberFormatException nfe) {
            throw new ParseException("Invalid long: " + args[index]);
        }
        if (val < this.min || val > this.max)
            throw new ParseException("Illegal value: " + val);

        this.value = val;
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
     * @return the <code>long</code> value of the argument.
     */
    public long getValue() {
        return this.value;
    }
}
