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
 * The <code>Parser</code> class is used to parse command-line options.
 */
public class Parser {
    /**
     * The set of command-line options.
     */
    private ArrayList<Option> options;

    /**
     * Creates a new, empty command-line parser.
     */
    public Parser() {
        this.options = new ArrayList<Option>();
    }

    /**
     * Adds the given option to the parser. Only zero or one of the choices
     * therein may be activated when the command line is parsed.
     *
     * @param option the option to add.
     * @throws IllegalArgumentException if any denoter in the given option
     *         already exists in this parser, or if another option with the
     *         same header already exists in this parser.
     */
    public void add(Option option) {
        for (Option already : this.options)
            if (option.isDistinct(already) == false)
                throw new IllegalArgumentException("Non-distinct option");

        int addAt = 0;
        int size = this.options.size();
        String oHeader = option.getHeader();
        while (addAt < size) {
            int comp = oHeader.compareTo(this.options.get(addAt).getHeader());
            if (comp < 0)
                break;
            else if (comp == 0)
                throw new IllegalArgumentException("Duplicate header");
            addAt++;
        }
        this.options.add(addAt, option);
    }

    /**
     * Parses the command line, activating choices and recording argument
     * values, or printing a usage message if an error occurs.
     *
     * @param args the command line, as received by the Java VM, or
     *        <code>null</code> to activate all the default choices.
     * @return <code>true</code> if the parse was successful,
     *         <code>false</code> if there was an error.
     */
    public boolean parse(String args[]) {
        for (Option option : this.options)
            option.reset();

        /* Match command line arguments against all possible choices */
        if (args != null) {
            int onArg = 0;
            while (onArg < args.length) {
                int curIndex = onArg;
                boolean matched = false;
                for (Option option : this.options) {
                    try {
                        curIndex = option.parse(args, onArg);
                    } catch (ParseException e) {
                        return this.usage();
                    }
                    if (curIndex != onArg) {
                        onArg = curIndex;
                        matched = true;
                        break;
                    }
                }
                if (matched == false)
                    return this.usage();
            }
        }

        /*
         * Ensure no option had more than one choice selected. For any option
         * that did not have a choice selected, activate its default choice.
         */
        for (Option option : this.options) {
            if (option.setDefaultAndTest() == true
                    || option.isValid() == false)
                return this.usage();
        }

        return true;
    }

    /**
     * Prints a summary of all the currently chosen options to standard output.
     */
    public void printSummary() {
        /* Determine the maximum header length */
        ArrayList<String> headers = new ArrayList<String>();
        ArrayList<String> descs = new ArrayList<String>();
        int maxLen = 0;
        for (Option option : this.options) {
            String header = option.getHeader();
            String desc = option.getDescription();
            if (desc == null)
                continue;
            if (header.length() > maxLen)
                maxLen = header.length();
            headers.add(header);
            descs.add(desc);
        }

        /* Pad all headers with spaces to align descriptions */
        if (headers.isEmpty() == false) {
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);
                System.out.print(header + ": ");
                for (int j = header.length(); j < maxLen; j++)
                    System.out.print(" ");
                System.out.println(descs.get(i));
            }
            System.out.println();
        }
    }

    /**
     * Prints out all possible choices and options.
     *
     * @return <code>false</code> always, which can be interpreted as an error
     *         return value for the parser.
     */
    private boolean usage() {
        for (Option option : this.options) {
            option.reset();
            option.setDefaultAndTest();
        }

        String str = "Options:";
        for (Option option : this.options) {
            str = str + "\n" + option;
        }
        System.err.println(str);
        return false;
    }
}
