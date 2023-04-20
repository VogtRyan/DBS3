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

import ca.ualberta.dbs3.cartography.*;
import ca.ualberta.dbs3.math.*;

/**
 * A <code>OptionDestinationChooser</code> represents a command line option for
 * choosing which {@link DestinationChooser} to use.
 */
public class OptionDestinationChooser extends Option {
    /**
     * The default alpha value to use with an integrated destination chooser.
     */
    public static final double DEFAULT_ALPHA = 1.0;

    /**
     * The default delta value to use with an integrated destination chooser.
     */
    public static final double DEFAULT_DELTA = 1.0;

    /**
     * The default radius to use with an integrated destination chooser.
     */
    public static final int DEFAULT_RADIUS = 1000;

    /**
     * The choice to use the uniform destination chooser.
     */
    private Choice uniform;

    /**
     * The choice to use the integrated destination chooser.
     */
    private Choice integrated;

    /**
     * The alpha value to use with the integrated chooser.
     */
    private ArgumentDouble alpha;

    /**
     * The delta value to use with the integrated chooser.
     */
    private ArgumentDouble delta;

    /**
     * The radius to use with the integrated chooser.
     */
    private ArgumentInt radius;

    /**
     * Creates a new <code>OptionDestinationChooser</code> to be added to a
     * parser.
     */
    public OptionDestinationChooser() {
        super("Destinations");
        this.uniform = new Choice("uniform");
        this.integrated = new Choice("integrated");
        this.alpha = new ArgumentDouble("alpha",
                OptionDestinationChooser.DEFAULT_ALPHA, 0.0);
        this.delta = new ArgumentDouble("delta",
                OptionDestinationChooser.DEFAULT_DELTA, 0.0);
        this.radius = new ArgumentInt("radius",
                OptionDestinationChooser.DEFAULT_RADIUS, 0);
        this.integrated.add(this.alpha);
        this.integrated.add(this.delta);
        this.integrated.add(this.radius);

        this.add(this.uniform);
        this.addDefault(this.integrated);
    }

    /**
     * Returns the destination chooser currently specified by this option.
     *
     * @param map the map on which the chooser will function.
     * @return the destination chooser specified by the user, or the default.
     */
    public DestinationChooser getChooser(Map map) {
        return this.getChooser(map, null);
    }

    /**
     * Returns the destination chooser currently specified by this option.
     *
     * @param map the map on which the chooser will function.
     * @param pm the progress monitor to update with the construction of the
     *        destination chooser.
     * @return the destination chooser specified by the user, or the default.
     */
    public DestinationChooser getChooser(Map map, ProgressMonitor pm) {
        if (this.uniform.isActive())
            return new DestinationChooserUniform(map, pm);
        else
            return new DestinationChooserIntegrated(map, this.alpha.getValue(),
                    this.delta.getValue(), this.radius.getValue(), pm);
    }

    /**
     * Returns a string with a description of the current choice for this
     * option.
     *
     * @return a description of the current choice.
     */
    public String getDescription() {
        if (this.uniform.isActive())
            return "Uniform";
        else
            return "Integrated (alpha = " + this.alpha.getValue()
                    + ", delta = " + this.delta.getValue() + ", radius = "
                    + this.radius.getValue() + ")";
    }
}
