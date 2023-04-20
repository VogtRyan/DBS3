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

package ca.ualberta.dbs3.gui;

import java.awt.Graphics;
import ca.ualberta.dbs3.math.*;

/**
 * The <code>Overlay</code> class refers to a fixed polygon that can be drawn
 * on a {@link Graphics} object.
 */
public class Overlay {
    /**
     * The x coordinates of the points, in metres.
     */
    private double[] x;

    /**
     * The y coordinates of the point, in metres.
     */
    private double[] y;

    /**
     * Preallocated space for integer computations on x coordinates, used to
     * speed up drawing by eliminating the need for memory allocation on each
     * redraw.
     */
    private int[] xInt;

    /**
     * Preallocated space for integer computations on y coordinates, used to
     * speed up drawing by eliminating the need for memory allocation on each
     * redraw.
     */
    private int[] yInt;

    /**
     * Creates a new <code>Overlay</code> from the given corner points.
     *
     * @param bounds the corner points for the polygon overlay.
     */
    public Overlay(Point[] bounds) {
        this.x = new double[bounds.length];
        this.y = new double[bounds.length];
        this.xInt = new int[bounds.length];
        this.yInt = new int[bounds.length];

        for (int i = 0; i < bounds.length; i++) {
            this.x[i] = bounds[i].getX();
            this.y[i] = bounds[i].getY();
        }
    }

    /**
     * Draws this overlay on the given <code>Graphics</code> object.
     *
     * @param xScale the factor by which to multiply the x coordinates.
     * @param yScale the factor by which to multiple the y coordinates.
     * @param g the graphics destination.
     */
    public void draw(double xScale, double yScale, Graphics g) {
        for (int i = 0; i < this.x.length; i++) {
            this.xInt[i] = (int) Math.round(this.x[i] * xScale);
            this.yInt[i] = (int) Math.round(this.y[i] * yScale);
        }
        g.fillPolygon(this.xInt, this.yInt, this.xInt.length);
    }
}
