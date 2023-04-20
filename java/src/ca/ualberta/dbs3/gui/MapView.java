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

package ca.ualberta.dbs3.gui;

import ca.ualberta.dbs3.cartography.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

/**
 * The <code>MapView</code> class represents a the specification of a view,
 * given in the file defining a {@link Map}, including a cached copy of the
 * image file that it specifies.
 */
public class MapView {
    /**
     * A cached copy of the image file specified by the view.
     */
    private BufferedImage image;

    /**
     * A human-readable description of the view.
     */
    private String name;

    /**
     * The conversion ratio between metres and the number of pixels on the map
     * it takes to represent a metre.
     */
    private double metresToPixels;

    /**
     * Creates a new <code>MapView</code> from the given
     * <code>ViewSpecification</code>.
     *
     * @param specification the specification of the view.
     * @throws IOException if there is an error loading the image file.
     */
    public MapView(ViewSpecification specification) throws IOException {
        this.name = specification.getName();
        this.metresToPixels = specification.getMetresToPixels();
        File imgFile = new File(specification.getFilename());
        this.image = ImageIO.read(imgFile);
        if (this.image == null)
            throw new IOException(
                    "Cannot read image" + specification.getFilename());
    }

    /**
     * Returns the human-readable description of the view.
     *
     * @return the human-readable description of the view.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the number of pixels it takes in the image file to represent one
     * metre.
     *
     * @return the number of pixels per metre.
     */
    public double getMetresToPixels() {
        return this.metresToPixels;
    }

    /**
     * Returns the cached copy of the image used to display this view.
     *
     * @return the image used by this view.
     */
    public BufferedImage getImage() {
        return this.image;
    }
}
