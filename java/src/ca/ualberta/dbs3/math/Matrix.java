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

/**
 * The <code>Matrix</code> class represents a 2x2 matrix.
 */
public class Matrix {
    /**
     * The top-left value in the matrix.
     */
    private double a;

    /**
     * The top-right value in the matrix.
     */
    private double b;

    /**
     * The lower-left value in the matrix.
     */
    private double c;

    /**
     * The lower-right value in the matrix.
     */
    private double d;

    /**
     * The determinant of the matrix.
     */
    private double det;

    /**
     * Creates a new <code>Matrix</code> with the given four elements.
     *
     * @param a11 the top-left value in the matrix.
     * @param a12 the top-right value in the matrix.
     * @param a21 the bottom-left value in the matrix.
     * @param a22 the bottom-right value in the matrix.
     */
    public Matrix(double a11, double a12, double a21, double a22) {
        this.a = a11;
        this.b = a12;
        this.c = a21;
        this.d = a22;
        this.det = this.a * this.d - this.b * this.c;
    }

    /**
     * Creates a new <code>Matrix</code> using the given two
     * <code>Vector</code>s as its columns.
     *
     * @param column1 the left column.
     * @param column2 the right column.
     */
    public Matrix(Vector column1, Vector column2) {
        this(column1.getDeltaX(), column2.getDeltaX(), column1.getDeltaY(),
                column2.getDeltaY());
    }

    /**
     * Returns the determinant of this <code>Matrix</code>.
     *
     * @return the determinant of the matrix.
     */
    public double getDeterminant() {
        return this.det;
    }

    /**
     * Solves the matrix equation <code>Ax=r</code>, where <code>A</code> is
     * this <code>Matrix</code> and <code>r</code> is the given
     * <code>Vector</code>. Returns <code>x</code>.
     *
     * @param r the right-hand side of the matrix equation.
     * @return <code>x</code>, the solution to the matrix equation, or
     *         <code>null</code> if there is no solution.
     */
    public Vector solve(Vector r) {
        if (this.det == 0.0)
            return null;
        double e = r.getDeltaX();
        double f = r.getDeltaY();
        double xNum = e * this.d - this.b * f;
        double yNum = this.a * f - e * this.c;
        return new Vector(xNum / this.det, yNum / this.det);
    }

    /**
     * Determines the number of solutions to the matrix equation
     * <code>Ax=r</code>, where <code>A</code> is this <code>Matrix</code> and
     * <code>r</code> is the given <code>Vector</code>.
     *
     * @param r the right-hand side of the matrix equation.
     * @return <code>0</code> if there are no solutions, <code>1</code> if
     *         there is a single unique solution, or an arbitrary value greater
     *         than <code>1</code> there are an infinite number of solutions.
     */
    public int numSolutions(Vector r) {
        /* From Cramer's rule */
        if (this.det != 0.0)
            return 1;
        double xNum = r.getDeltaX() * this.d - this.b * r.getDeltaY();
        if (xNum == 0.0)
            return Integer.MAX_VALUE;
        else
            return 0;
    }

    /**
     * Returns a <code>String</code> representation of this
     * <code>Matrix</code>.
     *
     * @return a <code>String</code> representation of this
     *         <code>Matrix</code>.
     */
    public String toString() {
        return "[" + this.a + " " + this.b + " \\ " + this.c + " " + this.d
                + "]";
    }

    /**
     * Tests is this <code>Matrix</code> is equal to another
     * <code>Matrix</code>. Two <code>Matrix</code>s are equal if all four of
     * their values are the same.
     *
     * @param o the object for which to test equality.
     * @return <code>true</code> if the two <code>Matrix</code>s are the same,
     *         otherwise <code>false</code>.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof Matrix) {
            Matrix other = (Matrix) o;
            return ((this.a == other.a) && (this.b == other.b)
                    && (this.c == other.c) && (this.d == other.d));
        }
        return false;
    }

    /**
     * Returns a hash code for this <code>Matrix</code>.
     *
     * @return a hash code for this <code>Matrix</code>, formed by combining
     *         the hash codes of its four elements.
     */
    public int hashCode() {
        Double a = Double.valueOf(this.a);
        Double b = Double.valueOf(this.b);
        Double c = Double.valueOf(this.c);
        Double d = Double.valueOf(this.d);
        return ((a.hashCode() & 0x0F0F0000) | (b.hashCode() & 0xF0F00000)
                | (c.hashCode() & 0x00000F0F) | (d.hashCode() & 0x0000F0F0));
    }
}
