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

package ca.ualberta.dbs3.cartography;

import java.io.*;

/**
 * The <code>ViewSpecification</code> class represents a the specification of a
 * view, given in the file defining a {@link Map}.
 */
public class ViewSpecification {
    /**
     * The filename of the image file.
     */
    private String filename;

    /**
     * The canonical filename of the image file, or <code>null</code> if the
     * view specification has not been resolved relative to the map filename.
     */
    private String canonical;

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
     * Creates a new <code>ViewSpecification</code> with the given image
     * filename and constant.
     *
     * @param name a human-readable description of the view.
     * @param filename the filename of the image file.
     * @param metresToPixels the number of pixels in the image that it takes to
     *        represent one metre.
     * @throws IllegalArgumentException if <code>metresToPixels</code> is less
     *         than or equal to zero.
     */
    public ViewSpecification(String name, String filename,
            double metresToPixels) {
        if (metresToPixels <= 0.0)
            throw new IllegalArgumentException("Invalid conversion ratio");

        this.name = name;
        this.filename = filename;
        this.metresToPixels = metresToPixels;
        this.canonical = null;
    }

    /**
     * Sets the path in which the image file is located, relative to the given
     * map file name.
     *
     * @param mapFilename the absolute or relative filename of the map file.
     * @throws IOException if a file system error occurs while attempting to
     *         resolve the path name.
     */
    public void resolvePath(String mapFilename) throws IOException {
        File mapfileParent = new File(mapFilename).getParentFile();
        File imageFile = new File(mapfileParent, this.filename);
        this.canonical = imageFile.getCanonicalPath();
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
     * Returns the filename of the image file.
     *
     * @return the filename of the image file.
     */
    public String getFilename() {
        if (this.canonical == null)
            return this.filename;
        else
            return this.canonical;
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
}
