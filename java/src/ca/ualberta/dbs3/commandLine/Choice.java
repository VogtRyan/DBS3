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

import java.util.*;

/**
 * The <code>Choice</code> class represents a single choice among many that can
 * be chosen for an {@link Option} on the command line.
 */
public class Choice implements Comparable<Choice> {
    /**
     * The prefix that is expected to deliniate the start of every new choice
     * on the command line.
     */
    private static final String PREFIX = "-";

    /**
     * The flag on the command line that represents this choice.
     */
    private String denoter;

    /**
     * Whether the choice has been activated.
     */
    private boolean active;

    /**
     * The arguments that this command-line choice takes.
     */
    private ArrayList<Argument> arguments;

    /**
     * Creates a new, inactive command-line choice.
     *
     * @param denoter the text, excluding the universal prefix, that
     *        corresponds to this choice on the command line.
     * @throws IllegalArgumentException if the given denoter string contains
     *         any whitespace.
     */
    public Choice(String denoter) {
        this.denoter = Choice.PREFIX + denoter;
        this.active = false;
        this.arguments = new ArrayList<Argument>();

        char[] cArray = this.denoter.toCharArray();
        for (int i = 0; i < cArray.length; i++) {
            if (Character.isWhitespace(cArray[i]))
                throw new IllegalArgumentException("Whitespace in denoter");
        }
    }

    /**
     * Adds an argument to the command-line choice.
     *
     * @param arg the argument to add.
     */
    public void add(Argument arg) {
        this.arguments.add(arg);
    }

    /**
     * Tests is this <code>Choice</code> matches the command line at the given
     * index. If so, activate and parse this choice and any subsequent
     * arguments.
     *
     * @param args the command line arguments, as supplied to the Java VM.
     * @param index the index at which to check for the <code>denoter</code>
     *        for this choice.
     * @return the first index after all of the information that was parsed if
     *         a match is made, or <code>index</code> if no match is made.
     * @throws ParseException if this choice matches the command line at the
     *         given index but has already been activated, or if there is an
     *         error parsing this choice's arguments.
     */
    public int parse(String args[], int index) throws ParseException {
        if (this.denoter.equals(args[index])) {
            if (this.active)
                throw new ParseException(
                        "Choice " + this.denoter + " already active");
            this.activate();
            index++;
            for (Argument clArg : this.arguments)
                index = clArg.parse(args, index);
        }
        return index;
    }

    /**
     * Activates this choice.
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Returns whether this command-line choice is active.
     *
     * @return <code>true</code> if the choice has been activated,
     *         <code>false</code> otherwise.
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Deactivates the choice and sets all its parameters back to their default
     * value.
     */
    public void reset() {
        this.active = false;
        for (Argument clArg : this.arguments)
            clArg.reset();
    }

    /**
     * Tests is this <code>Choice</code> is equal to another
     * <code>Choice</code>. Two choices are equal if their denoter strings are
     * the same.
     *
     * @param o the object for which to test equality.
     * @return <code>true</code> if the two <code>Choice</code>s are the same,
     *         otherwise <code>false</code>.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof Choice) {
            Choice c = (Choice) o;
            return this.denoter.equals(c.denoter);
        }
        return false;
    }

    /**
     * Returns a hash code for this <code>Choice</code>.
     *
     * @return a hash code for this <code>Choice</code>, equal to the hash code
     *         of the denoter <code>String</code>.
     */
    public int hashCode() {
        return this.denoter.hashCode();
    }

    /**
     * Compares two <code>Choice</code>s for order. Choices are ordered by the
     * lexicographic order of their denoter strings.
     *
     * @param o the other choice to which to compare this choice.
     * @return <code>-1</code> if this choice is less than the given choice,
     *         <code>1</code> if this choice is greater, or <code>0</code> if
     *         they are equal.
     */
    public int compareTo(Choice o) {
        return this.denoter.compareTo(o.denoter);
    }

    /**
     * Returns a <code>String</code> representation of the choice and all the
     * arguments that proceed it.
     *
     * @return a <code>String</code> representation of the choice and all the
     *         arguments that proceed it.
     */
    public String toString() {
        String str = this.denoter;
        for (Argument clArg : this.arguments)
            str = str + " " + clArg;
        return str;
    }
}
