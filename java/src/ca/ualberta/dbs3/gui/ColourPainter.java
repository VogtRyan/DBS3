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

import java.awt.*;

/**
 * The <code>ColourPainter</code> class is responsible for painting preview
 * rectangles of a colour onto a given graphics object, or painting an error
 * symbol if there is a problem with the colour.
 */
public class ColourPainter {
    /**
     * The width in pixels from the edge of the error symbol to the corner of
     * the painting area.
     */
    private static final int ERROR_SIZE = 2;

    /**
     * The colour to paint.
     */
    private Color colour;

    /**
     * The size of the square in pixels.
     */
    private int size;

    /**
     * Constructs a new <code>ColourPainter</code> to paint a square of the
     * given colour with the given size.
     *
     * @param colour the colour to paint, or <code>null</code> to paint an
     *        error symbol.
     * @param size the size of the square to draw.
     */
    public ColourPainter(Color colour, int size) {
        this.colour = colour;
        this.size = size;
    }

    /**
     * Changes the colour being drawn to the given colour.
     *
     * @param colour the colour to paint, or <code>null</code> to paint an
     *        error symbol.
     */
    public void setColour(Color colour) {
        this.colour = colour;
    }

    /**
     * Paints the colour onto the given graphics object at the given x and y
     * offset, or an error symbol overtop the background colour if the current
     * colour has been set to <code>null</code>.
     *
     * @param g the graphics object on which to draw the colour.
     * @param x the x offset at which to draw the colour.
     * @param y the y offset at which to draw the colour.
     * @param background the background colour.
     */
    public void draw(Graphics g, int x, int y, Color background) {
        /* If the colour is null, paint an error symbol instead */
        if (this.colour == null) {
            this.drawError(g, x, y, background);
            return;
        }

        /* First, paint the background checkerboard */
        int sizeOne = this.size / 2;
        int sizeTwo = this.size - sizeOne;
        g.setColor(Color.WHITE);
        g.fillRect(x, y, sizeOne, sizeOne);
        g.fillRect(x + sizeOne, y + sizeOne, sizeTwo, sizeTwo);
        g.setColor(Color.BLACK);
        g.fillRect(x + sizeOne, y, sizeTwo, sizeOne);
        g.fillRect(x, y + sizeOne, sizeOne, sizeTwo);

        /* Then paint the colour overtop the board */
        g.setColor(this.colour);
        g.fillRect(x, y, this.size, this.size);
    }

    /**
     * Paints an error symbol onto the given graphics object at the given x and
     * y offset.
     *
     * @param g the graphics object on which to draw the error symbol.
     * @param x the x offset at which to draw the error symbol.
     * @param y the y offset at which to draw the error symbol.
     * @param background the background colour.
     */
    private void drawError(Graphics g, int x, int y, Color background) {
        /* First, paint the background colour */
        g.setColor(background);
        g.fillRect(x, y, this.size, this.size);

        /* Invert that color */
        g.setColor(new Color(255 - background.getRed(),
                255 - background.getGreen(), 255 - background.getBlue()));

        /* Paint a big X */
        g.drawLine(x, y, x + this.size - 1, y + this.size - 1);
        g.drawLine(x + this.size - 1, y, x, y + this.size - 1);
        for (int i = 1; i < this.size && i <= ColourPainter.ERROR_SIZE; i++) {
            g.drawLine(x + i, y, x + this.size - 1, y + this.size - i - 1);
            g.drawLine(x, y + i, x + this.size - i - 1, y + this.size - 1);
            g.drawLine(x + this.size - i - 1, y, x, y + this.size - i - 1);
            g.drawLine(x + this.size - 1, y + i, x + i, y + this.size - 1);
        }
    }
}
