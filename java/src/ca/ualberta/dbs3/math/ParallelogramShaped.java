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

package ca.ualberta.dbs3.math;

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>ParallelogramShaped</code> class represents any construct on the
 * two-dimensional plane that is parallelogram-shaped (including those shaped
 * like more specific parallelograms, such as rectangles), about which you can
 * ask a variety of geometric questions.
 */
public abstract class ParallelogramShaped {
    /**
     * The centre point of the parallelogram.
     */
    private Point centre;

    /**
     * A vector from the centre point, running parallel to the edges A and B,
     * reaching edge C.
     */
    private Vector parAB;

    /**
     * A vector from the centre point, running parallel to the edges C and D,
     * reaching edge A.
     */
    private Vector parCD;

    /**
     * Creates a new parallelogram shaped construct centred at the given point,
     * with its four edges defined by the two given vectors.
     *
     * @param centre the centre point of the parallelogram.
     * @param parAB a vector from the centre point, running parallel to the
     *        edges A and B, reaching edge C.
     * @param parCD a vector from the centre point, running parallel to the
     *        edges C and D, reaching edge A.
     * @throws IllegalArgumentException if the two vectors are not linearly
     *         independent.
     */
    public ParallelogramShaped(Point centre, Vector parAB, Vector parCD) {
        /* Ensure the vectors and linearly independent */
        if (parAB.isIndependent(parCD) == false)
            throw new IllegalArgumentException("Linearly dependent vectors");
        this.centre = centre;
        this.parAB = parAB;
        this.parCD = parCD;
    }


    /**
     * Returns the point at the centre of this parallelogram.
     *
     * @return the <code>Point</code> on the centre of this parallelogram.
     */
    public Point getCentrePoint() {
        return this.centre;
    }

    /**
     * Returns a random point chosen uniformly from within this parallelogram.
     *
     * @param prng the psuedo-random number generator to use.
     * @return a uniformly chosen random point.
     */
    public Point getRandomPoint(Random prng) {
        /*
         * The PRNG returns a value in [0.0, 1.0]. We want a value within
         * [-1.0, 1.0] to dictate how far to move along each vector.
         */
        double ab = prng.nextDouble() * 2.0 - 1.0;
        double cd = prng.nextDouble() * 2.0 - 1.0;
        return this.centre.add(this.parAB, ab).add(this.parCD, cd);
    }

    /**
     * Returns a random point chosen uniformly from within this parallelogram,
     * reflected into all four quadrants of the parallelogram.
     *
     * @param prng the pseudo-random number generator to use.
     * @return a uniformly chosen point, reflected into all four quadrants.
     */
    public List<Point> getRandomPoints(Random prng) {
        /*
         * Start at the centre point. Then, move a multiple in [0.0, 1.0] of
         * parAB from there, and move a multiple in [0.0, 1.0] of parCD from
         * there. By flipping the movement vectors, we can mirror the result
         * into all four quadrants
         */
        Vector moveA = this.parAB.multiply(prng.nextDouble());
        Vector moveB = this.parCD.multiply(prng.nextDouble());

        List<Point> ret = new ArrayList<Point>(4);

        Point temp = this.centre.add(moveA);
        ret.add(temp.add(moveB));
        ret.add(temp.subtract(moveB));

        temp = this.centre.subtract(moveA);
        ret.add(temp.subtract(moveB));
        ret.add(temp.add(moveB));

        return ret;
    }

    /**
     * Returns a size-four array with the four corners of this parallelogram.
     * The corners are ordered such that drawing a line between the ordered
     * elements of the array creates a closed polygon.
     * 
     * @return the four corners of the parallelogram.
     */
    public Point[] getCornerPoints() {
        Point[] ret = new Point[4];

        ret[0] = this.centre.add(this.parAB);
        ret[1] = ret[0].subtract(this.parCD);
        ret[0] = ret[0].add(this.parCD);

        ret[3] = this.centre.subtract(this.parAB);
        ret[2] = ret[3].subtract(this.parCD);
        ret[3] = ret[3].add(this.parCD);

        return ret;
    }

    /**
     * Returns whether this parallelogram falls entirely within the given
     * bounds.
     *
     * @param minX the lower X coordinate bound.
     * @param minY the lower Y coordinate bound.
     * @param maxX the upper X coordinate bound.
     * @param maxY the upper Y coordinate bound.
     * @return <code>true</code> if this parallelogram's X coordinates are
     *         within the two X bounds and its Y coordinates are within the two
     *         Y bounds, <code>false</code> otherwise.
     * @throws IllegalArgumentException if <code>minX</code> is greater than
     *         <code>maxX</code> or <code>minY</code> is greater than
     *         <code>maxY</code>.
     */
    public boolean fallsWithin(double minX, double minY, double maxX,
            double maxY) {
        Point[] corners = this.getCornerPoints();
        for (int i = 0; i < corners.length; i++) {
            if (corners[i].fallsWithin(minX, minY, maxX, maxY) == false)
                return false;
        }
        return true;
    }

    /**
     * Returns a size-four array with the four line segments that enclose the
     * area of this parallelogram. The line segments are ordered such the
     * segments at indices <code>0</code> and <code>1</code> are parallel, as
     * are the segments at indices <code>2</code> and <code>3</code>.
     *
     * @return the bounding lines of the parallelogram.
     */
    public LineSegment[] getBoundingLines() {
        Point[] corners = this.getCornerPoints();

        LineSegment[] ret = new LineSegment[4];
        ret[0] = new LineSegment(corners[0], corners[1]);
        ret[1] = new LineSegment(corners[3], corners[2]);
        ret[2] = new LineSegment(corners[0], corners[3]);
        ret[3] = new LineSegment(corners[1], corners[2]);
        return ret;
    }

    /**
     * Tests whether any of the bounding line segments of this parallelogram
     * intersect any of the bounding line segments of the given parallelogram.
     *
     * @param other the parallelogram to test for bounding-segment
     *        intersection.
     * @return <code>true</code> if the bounding line segments of the given
     *         parallelogram intersect the bounding line segments of this
     *         parallelogram, <code>false</code> otherwise.
     */
    public boolean boundingLinesIntersect(ParallelogramShaped other) {
        LineSegment[] me = this.getBoundingLines();
        LineSegment[] you = other.getBoundingLines();

        for (int i = 0; i < me.length; i++) {
            for (int j = 0; j < you.length; j++) {
                if (me[i].intersects(you[j]))
                    return true;
            }
        }
        return false;
    }

    /**
     * Tests whether the given point falls within the parallelogram.
     *
     * @param point the point to test.
     * @return <code>true</code> if the point lies within the parallelogram,
     *         <code>false</code> otherwise.
     */
    public boolean contains(Point point) {
        /*
         * Express the direction and distance from the centre of the
         * parallelogram to the given point as a vector "dist". Since we are
         * guaranteed that parAB and parCD are linearly independent, express
         * "dist" as a linear combination of the these two vectors.
         */
        Vector dist = new Vector(this.centre, point);
        Matrix matrix = new Matrix(this.parAB, this.parCD);
        Vector soln = matrix.solve(dist);

        /*
         * We can move a multiple of parAB within [-1.0, 1.0] away from the
         * centre and a multiple of parCD within [-1.0, 1.0] away from the
         * centre without leaving the parallelogram.
         */
        double coefAB = soln.getDeltaX();
        double coefCD = soln.getDeltaY();
        return (-1.0 <= coefAB && coefAB <= 1.0 && -1.0 <= coefCD
                && coefCD <= 1.0);
    }

    /**
     * Tests whether the given parallelogram, in its entirety, falls within
     * this parallelogram.
     *
     * @param other the parallelogram to test.
     * @return <code>true</code> if the other parallelogram lies, in its
     *         entirety, within this parallelogram, <code>false</code>
     *         otherwise.
     */
    public boolean contains(ParallelogramShaped other) {
        Point[] ocp = other.getCornerPoints();
        for (int i = 0; i < ocp.length; i++) {
            if (this.contains(ocp[i]) == false)
                return false;
        }
        return true;
    }

    /**
     * Computes the length of the portion of the given line segment that falls
     * inside this parallelogram.
     *
     * @param segment the line segment.
     * @return the length of the line segment that lies inside this
     *         parallelogram.
     */
    public double lengthContained(LineSegment segment) {
        /*
         * For this function, we use the following shorthand:
         * - Let c be the centre point of the parallelogram
         * - Let p and q be the two basis vectors for the parallelogram
         */
        double px = this.parAB.getDeltaX();
        double py = this.parAB.getDeltaY();
        double qx = this.parCD.getDeltaX();
        double qy = this.parCD.getDeltaY();

        /*
         * - Let j, k be the two endpoints of the line segment
         * - Let dx = kx - jx, dy = ky - jy
         */
        Point[] ends = segment.getPoints();
        double dx = ends[1].getX() - ends[0].getX();
        double dy = ends[1].getY() - ends[0].getY();

        /*
         * Consider the line segment s with endpoints m, n, where m = j - c,
         * and n = k - c (essentially, reposition the (0, 0) position to the
         * centre of the parallelogram).
         */
        double mx = ends[0].getX() - this.centre.getX();
        double my = ends[0].getY() - this.centre.getY();

        /*
         * Every point on the infinite-length extension of s can be written as
         *     m + t * (n - m)
         *   = m + t * (k - j),
         * where scalar t in [0, 1] defines the finite-length segment s.
         *
         * Since p and q are linearly independent, we can find scalars a and b
         * for any value of t, such that
         *     m + t * (n - m) = a*p + b*q,
         * which can also be written as
         *     [px qx] * [a] = [mx + t * dx]
         *     [py qy]   [b]   [my + t * dy]
         *
         * We use Cramer's rule to solve for a and b. First, denote the
         * determinant of the matrix as det.
         */
        double det = px * qy - qx * py;

        /*
         * Cramer's rule yields:
         * a = [ t * (dx*qy - dy*qx) + (mx*qy - my*qx) ] / det
         * b = [ t * (dy*px - dx*py) + (my*px - mx*py) ] / det
         *
         * In order for any potion of the infinite-length extension of s to
         * fall within the parallelogram (recentred about (0, 0)), it must be
         * that -1 <= a <= 1, and -1 <= b <= 1. We can solve the two equations
         * above for t.
         *
         * Plus, recall that s is finite length, defined only in the range
         * t \in [0, 1].
         *
         * The intersection of all three of these constraints is the subset of
         * [0, 1] for which the line segment is inside the parallelogram.
         */
        double[] range = new double[2];
        range[0] = 0.0;
        range[1] = 1.0;
        this.intersectUnitInequality(dx * qy - dy * qx, mx * qy - my * qx, det,
                range);
        this.intersectUnitInequality(dy * px - dx * py, my * px - mx * py, det,
                range);

        if (range[0] > range[1])
            return 0.0;
        else
            return ((range[1] - range[0]) * segment.getLength());
    }

    /**
     * Solves the inequality <code>-1 &lt;= (at+b)/c &lt;= 1</code> for
     * <code>t</code>, then intersects that range of <code>t</code> values with
     * the range currently in the <code>res</code> array before saving the
     * intersected result to <code>res</code>. A range in <code>res</code> with
     * <code>res[0] &gt; res[1]</code> is considered to be an empty range.
     *
     * @param a the multiplicative constant.
     * @param b the additive constant.
     * @param c the denominator constant.
     * @param res the results array to intersect and refill with the
     *        intersection.
     * @throws IllegalArgumentException if <code>c == 0.0</code>.
     */
    private void intersectUnitInequality(double a, double b, double c,
            double[] res) {
        /* If the range is already empty, we can skip solving the inequality */
        if (c == 0.0)
            throw new IllegalArgumentException("Denominator cannot be zero");
        if (res[0] > res[1])
            return;

        /* Determine low <= t <= high, such that -1 <= (at+b)/c <= 1 */
        double low, high;
        if (a == 0.0) {
            double quotient = b / c;
            if (-1 <= quotient && quotient <= 1) {
                /* Inequality true for every t */
                low = Double.NEGATIVE_INFINITY;
                high = Double.POSITIVE_INFINITY;
            } else {
                /* Inequality false for every t */
                low = 1.0;
                high = 0.0;
            }
        } else if ((c > 0.0 && a > 0.0) || (c < 0.0 && a < 0.0)) {
            low = (-c - b) / a;
            high = (c - b) / a;
        } else {
            low = (c - b) / a;
            high = (-c - b) / a;
        }

        /*
         * Intersect [low, high] with [res[0], res[1]], and store the result to
         * res.
         */
        if (low > high) {
            res[0] = 1.0;
            res[1] = 0.0;
            return;
        }
        if (low > res[0])
            res[0] = low;
        if (high < res[1])
            res[1] = high;
    }
}
