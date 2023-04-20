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
 * The <code>OptionAgents</code> class represents a command line option for
 * specifying the number of agents in the simulation.
 */
public class OptionAgents extends Option {
    /**
     * The default number of agents to use if none is specified.
     */
    public static final int DEFAULT_AGENTS = 100;

    /**
     * The integer argument given to the only choice in this option.
     */
    private ArgumentInt arg;

    /**
     * Creates a new <code>OptionAgents</code> to be added to a parser.
     */
    public OptionAgents() {
        super("Agents");
        Choice choice = new Choice("agents");
        this.arg =
                new ArgumentInt("numAgents", OptionAgents.DEFAULT_AGENTS, 1);
        choice.add(this.arg);
        this.addDefault(choice);
    }

    /**
     * Returns the number of agents parsed off the command line.
     *
     * @return the number of agents specified by the user.
     */
    public int getNumAgents() {
        return this.arg.getValue();
    }

    /**
     * Returns a string with a description of the current choice for this
     * option.
     *
     * @return a description of the current choice.
     */
    public String getDescription() {
        return Integer.toString(this.getNumAgents());
    }
}
