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
 * A <code>Option</code> represents a set of choices, where only zero or one of
 * choices may be specified at once on the command line. This class can
 * represent, for example, mutually exclusive choices (with a default choice if
 * none of them are specified). Subclasses of <code>Option</code> are expected
 * to call the {@link #add} and {@link #addDefault} methods in, and only in,
 * their constructors.
 */
public abstract class Option {
    /**
     * The number of characters per line to which to attempt to limit output.
     */
    private static final int CHARS_PER_LINE = 79;

    /**
     * The number of characters to indent the output.
     */
    private static final int INDENT = 4;

    /**
     * A header describing this option.
     */
    private String header;

    /**
     * The set of choices in this option.
     */
    private ArrayList<Choice> choices;

    /**
     * The default choice to activate if no choice in this option is explicitly
     * specified, or <code>null</code> if there is no default choice.
     */
    private Choice defaultChoice;

    /**
     * Creates a new, empty option with no default choice.
     *
     * @param header a header describing this option.
     */
    public Option(String header) {
        this.header = header;
        this.choices = new ArrayList<Choice>();
        this.defaultChoice = null;
    }

    /**
     * Adds a new choice to this option, not as the default choice.
     *
     * @param choice the choice to add to the option.
     * @throws IllegalArgumentException if a choice with the same denoter
     *         already exists in this option.
     */
    protected void add(Choice choice) {
        /* Sort choices lexicographically */
        int addAt = 0;
        int size = this.choices.size();
        while (addAt < size) {
            int comp = choice.compareTo(this.choices.get(addAt));
            if (comp < 0)
                break;
            else if (comp == 0)
                throw new IllegalArgumentException("Duplicate choice");
            addAt++;
        }
        this.choices.add(addAt, choice);
    }

    /**
     * Adds a new choice to this option as the default choice and activates
     * that choice.
     *
     * @param choice the choice to add to the option.
     * @throws IllegalArgumentException if a choice with the same denoter
     *         already exists in this option, or if there already exists a
     *         default choice for this option.
     */
    protected void addDefault(Choice choice) {
        this.add(choice);
        if (this.defaultChoice != null)
            throw new IllegalArgumentException("Multiple default choices");
        this.defaultChoice = choice;
        choice.activate();
    }

    /**
     * Tests whether the currently active option and its arguments are valid.
     * By default, this function just returns <code>true</code>, but subclasses
     * of <code>Option</code> can override this method to allow a
     * {@link Parser} to detect, for example, invalid combinations of arguments
     * given to a {@link Choice}. It is assumed that the default argument
     * values (see {@link #reset}) are valid, and the behaviour of parsers if
     * they are not is undefined.
     *
     * @return whether or not the currently active choice is valid, which is
     *         <code>true</code> by default.
     */
    public boolean isValid() {
        return true;
    }

    /**
     * Returns a string summarizing the currently selected choice, or
     * <code>null</code> if no information about the given choice should be
     * printed for the user. By default, no information is printed, and
     * subclasses of <code>Option</code> should override this method if a
     * {@link Parser} should print information about those options.
     *
     * @return a string summarizing the currently selected choice or
     *         <code>null</code>.
     */
    public String getDescription() {
        return null;
    }

    /**
     * Deactivates all of the choices in this option, including the default
     * choice (if any), and sets all their arguments back to their default
     * value.
     */
    public void reset() {
        for (Choice choice : this.choices)
            choice.reset();
    }

    /**
     * Attempts to parse a given place in the command line against all of the
     * possible choices in this option.
     *
     * @param args the command line arguments, as passed to the Java VM.
     * @param index the index into the command line at which a match should be
     *        attempted.
     * @return the new index at which to begin parsing the command line if a
     *         match is made, or <code>index</code> if no match is made.
     * @throws ParseException if a choice in this option matches the command
     *         line at the given index but has already been activated, or if
     *         there is an error parsing a matching choice's arguments.
     */
    public int parse(String args[], int index) throws ParseException {
        int newIndex = index;
        for (Choice choice : this.choices) {
            newIndex = choice.parse(args, index);
            if (newIndex != index)
                break;
        }
        return newIndex;
    }

    /**
     * Tests whether multiple choices in this option are active. If no choice
     * is active, activate the default choice (if there is one).
     *
     * @return <code>true</code> if multiple choices are active, otherwise
     *         <code>false</code>.
     */
    public boolean setDefaultAndTest() {
        boolean active = false;
        for (Choice choice : this.choices) {
            if (choice.isActive()) {
                if (active)
                    return true;
                else
                    active = true;
            }
        }

        if (active == false && this.defaultChoice != null)
            this.defaultChoice.activate();
        return false;
    }

    /**
     * Returns a string with a header describing what this option is.
     *
     * @return a string header describing this option.
     */
    public String getHeader() {
        return this.header;
    }

    /**
     * Tests if two <code>Option</code>s are distinct from each other. Two
     * <code>Option</code>s are considered distinct if they do not share any
     * {@link Choice}s with the same denoter.
     *
     * @param other the option to which to compare this option.
     * @return <code>true</code> if the two <code>Option</code>s are distinct,
     *         otherwise <code>false</code>.
     */
    public boolean isDistinct(Option other) {
        for (Choice me : this.choices) {
            for (Choice you : other.choices) {
                if (me.equals(you))
                    return false;
            }
        }
        return true;
    }

    /**
     * Returns a <code>String</code> representation of all of the potential
     * choices in this option.
     *
     * @return a <code>String</code> representation of all of the potential
     *         choices in this option.
     */
    public String toString() {
        String str = "";
        String line = "";
        int onLine = 0;
        String item;

        for (int i = 0; i < Option.INDENT; i++)
            line = line + " ";
        line = line + " [";

        int numChoices = this.choices.size();
        if (numChoices == 0)
            return line + " ]";

        for (int i = 0; i < numChoices; i++) {
            item = " " + this.choices.get(i).toString();
            if (i == numChoices - 1)
                item = item + " ]";
            else
                item = item + " |";

            int newLen = line.length() + item.length();
            if (newLen > Option.CHARS_PER_LINE && onLine > 0) {
                str = str + line + "\n";
                line = "";
                for (int j = 0; j < Option.INDENT; j++)
                    line = line + " ";
                line = line + "  ";
                onLine = 0;
            }

            line = line + item;
            onLine++;
        }
        return str + line;
    }
}
