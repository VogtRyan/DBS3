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
 * The <code>OptionPort</code> class represents a command line option for
 * specifying the port on which a server will run.
 */
public class OptionPort extends Option {
    /**
     * The default port to run on if none is specified.
     */
    public static final int DEFAULT_PORT = 40000;

    /**
     * The minimum legal port value.
     */
    public static final int MIN_PORT = 1;

    /**
     * The maximum legal port value.
     */
    public static final int MAX_PORT = 65535;

    /**
     * The integer argument given to the only choice in this option.
     */
    private ArgumentInt arg;

    /**
     * Creates a new <code>OptionPort</code> to be added to a parser.
     */
    public OptionPort() {
        super("Port");
        Choice choice = new Choice("port");
        this.arg = new ArgumentInt("portNumber", OptionPort.DEFAULT_PORT,
                OptionPort.MIN_PORT, OptionPort.MAX_PORT);
        choice.add(this.arg);
        this.addDefault(choice);
    }

    /**
     * Returns the port number parsed off the command line.
     *
     * @return the port number specified by the user.
     */
    public int getPort() {
        return this.arg.getValue();
    }

    /**
     * Returns a string with a description of the current choice for this
     * option.
     *
     * @return a description of the current choice.
     */
    public String getDescription() {
        return Integer.toString(this.getPort());
    }
}
