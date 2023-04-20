/*
 * Copyright (c) 2010-2023 Ryan Vogt <rvogt@ualberta.ca>
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
 * A <code>OptionMapFile</code> represents a command line option for choosing a
 * DBS3 map file to load.
 */
public class OptionMapFile extends Option {
    /**
     * The default map file to use if none is specified.
     */
    public static final String DEFAULT_MAP = "../maps/fira.smf";

    /**
     * The string argument given to the only choice in this option.
     */
    private ArgumentString arg;

    /**
     * Creates a new <code>OptionMapFile</code> to be added to a parser.
     */
    public OptionMapFile() {
        super("Map file");
        Choice choice = new Choice("map");
        this.arg = new ArgumentString("mapFile", OptionMapFile.DEFAULT_MAP);
        choice.add(this.arg);
        this.addDefault(choice);
    }

    /**
     * Returns the name of the most recently parsed map file.
     *
     * @return the map file specified by the user, or the default.
     */
    public String getMapFile() {
        return this.arg.getValue();
    }

    /**
     * Returns a string with a description of the current choice for this
     * option.
     *
     * @return a description of the current choice.
     */
    public String getDescription() {
        return this.getMapFile();
    }
}
