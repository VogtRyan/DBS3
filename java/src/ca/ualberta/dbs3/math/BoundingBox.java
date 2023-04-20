/*
 * Copyright (c) 2012-2023 Ryan Vogt <rvogt@ualberta.ca>
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

package ca.ualberta.dbs3.math;

/**
 * The <code>BoundingBox</code> class represents a minimal bounding rectangle,
 * not rotated on the plane, that contains any number of
 * {@link ParallelogramShaped} objects.
 */
public class BoundingBox {
    /**
     * The minimal x value.
     */
    private double minX;

    /**
     * The maximal x value.
     */
    private double maxX;

    /**
     * The minimal y value.
     */
    private double minY;

    /**
     * The maximal y value.
     */
    private double maxY;

    /**
     * Creates a new <code>BoundingBox</code> around the given shape.
     *
     * @param shape the shape for the box to contain.
     */
    public BoundingBox(ParallelogramShaped shape) {
        Point[] corners = shape.getCornerPoints();
        this.minX = this.maxX = corners[0].getX();
        this.minY = this.maxY = corners[0].getY();
        for (int i = 1; i < corners.length; i++) {
            double x = corners[i].getX();
            double y = corners[i].getY();
            if (x < this.minX)
                this.minX = x;
            if (x > this.maxX)
                this.maxX = x;
            if (y < this.minY)
                this.minY = y;
            if (y > this.maxY)
                this.maxY = y;
        }
    }

    /**
     * Possibly expands the bounding box so that it contains the new given
     * shape.
     *
     * @param shape the shape to add to the bounding box.
     * @return a new bounding box that also contains the given shape.
     */
    public BoundingBox expand(ParallelogramShaped shape) {
        BoundingBox ret = new BoundingBox(shape);
        if (this.minX < ret.minX)
            ret.minX = this.minX;
        if (this.maxX > ret.maxX)
            ret.maxX = this.maxX;
        if (this.minY < ret.minY)
            ret.minY = this.minY;
        if (this.maxY > ret.maxY)
            ret.maxY = this.maxY;
        return ret;
    }

    /**
     * Returns the minimal X coordinate within the bounding box.
     *
     * @return the minimal X coordinate.
     */
    public double minX() {
        return this.minX;
    }

    /**
     * Returns the maximal X coordinate within the bounding box.
     *
     * @return the maximal X coordinate.
     */
    public double maxX() {
        return this.maxX;
    }

    /**
     * Returns the minimal Y coordinate within the bounding box.
     *
     * @return the minimal Y coordinate.
     */
    public double minY() {
        return this.minY;
    }

    /**
     * Returns the maximal Y coordinate within the bounding box.
     *
     * @return the maximal Y coordinate.
     */
    public double maxY() {
        return this.maxY;
    }
}
