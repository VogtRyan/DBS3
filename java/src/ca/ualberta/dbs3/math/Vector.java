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
 * The <code>Vector</code> class represents a direction and magnitude in 2D
 * space. It is essentially an arrow from the origin to a point on the plane.
 */
public class Vector {
    /**
     * The change in X exhibited by this <code>Vector</code>.
     */
    private double deltaX;

    /**
     * The change in Y exhibited by this <code>Vector</code>.
     */
    private double deltaY;

    /**
     * Creates a new <code>Vector</code> with the given change in X and Y.
     *
     * @param deltaX the change in X.
     * @param deltaY the change in Y.
     */
    public Vector(double deltaX, double deltaY) {
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    /**
     * Creates a new <code>Vector</code> that represents the direction and
     * distance between two points on the plane.
     *
     * @param begin the origin of the vector.
     * @param end the end of the vector.
     */
    public Vector(Point begin, Point end) {
        this.deltaX = end.getX() - begin.getX();
        this.deltaY = end.getY() - begin.getY();
    }

    /**
     * Returns the change in X exhibited by this <code>Vector</code>.
     *
     * @return the change in X exhibited by this <code>Vector</code>.
     */
    public double getDeltaX() {
        return this.deltaX;
    }

    /**
     * Returns the change in Y exhibited by this <code>Vector</code>.
     *
     * @return the change in Y exhibited by this <code>Vector</code>.
     */
    public double getDeltaY() {
        return this.deltaY;
    }

    /**
     * Returns a <code>Vector</code> with the same direction as this
     * <code>Vector</code>, but with length <code>1.0</code>. If this
     * <code>Vector</code> is the zero-vector, <code>null</code> is returned.
     *
     * @return an equivalent-direction <code>Vector</code> with length
     *         <code>1.0</code>, or <code>null</code> if this is the
     *         zero-vector.
     */
    public Vector normalize() {
        double len = this.length();
        if (len == 0.0)
            return null;
        return new Vector(this.deltaX / len, this.deltaY / len);
    }

    /**
     * Returns a <code>Vector</code> with the same direction as this
     * <code>Vector</code>, but with the given length. If this
     * <code>Vector</code> is the zero-vector, <code>null</code> is returned.
     *
     * @param length the length of the returned vector.
     * @return an equivalent-direction <code>Vector</code> with the given
     *         length, or <code>null</code> if this is the zero-vector.
     */
    public Vector normalize(double length) {
        double len = this.length();
        if (len == 0.0)
            return null;
        return new Vector(this.deltaX / len * length,
                this.deltaY / len * length);
    }

    /**
     * Returns a <code>Vector</code> equal to this one, except with the given
     * scalar multiplied into each component.
     *
     * @param factor the scalar by which to multiply each component.
     * @return a new <code>Vector</code> equal to this one multiplied by the
     *         given scalar.
     */
    public Vector multiply(double factor) {
        return new Vector(this.deltaX * factor, this.deltaY * factor);
    }

    /**
     * Returns an orthogonal <code>Vector</code> to this <code>Vector</code>,
     * with the same length as this <code>Vector</code>. If this
     * <code>Vector</code> is the zero-vector, <code>null</code> is returned.
     *
     * @return an equivalent-length <code>Vector</code> with orthogonal
     *         direction, or <code>null</code> if this is the zero-vector.
     */
    public Vector orthogonal() {
        if (this.deltaX == 0.0 && this.deltaY == 0.0)
            return null;
        return new Vector(this.deltaY, -this.deltaX);
    }

    /**
     * Returns the dot product between this vector and the given vector.
     *
     * @param other the vector with which to compute the dot product.
     * @return the dot product between this vector and <code>other</code>.
     */
    public double dotProduct(Vector other) {
        return this.deltaX * other.deltaX + this.deltaY * other.deltaY;
    }

    /**
     * Returns the cross product between this vector and the given vector.
     *
     * @param other the vector with which to compute the cross product.
     * @return the cross product between this vector and <code>other</code>.
     */
    public double crossProduct(Vector other) {
        return this.deltaX * other.deltaY - other.deltaX * this.deltaY;
    }

    /**
     * Returns the angle between this vector and the given vector, in the range
     * <i>0.0</i> through <i>pi</i>.
     *
     * @param other the vector with which to compute the angle.
     * @return the angle between this vector and <code>other</code>.
     * @throws IllegalArgumentException if there is no angle between the two
     *         vectors (because one or both are the zero vector).
     */
    public double angle(Vector other) {
        if ((this.deltaX == 0.0 && this.deltaY == 0.0)
                || (other.deltaX == 0.0 && other.deltaY == 0.0))
            throw new IllegalArgumentException("No angle between vectors");
        return Math.atan2(Math.abs(this.crossProduct(other)),
                this.dotProduct(other));
    }

    /**
     * Returns whether the given vector is linearly independent with this
     * vector.
     *
     * @param other the vector to test for linear independence with this one.
     * @return <code>true</code> if the two vectors are linearly independent,
     *         <code>false</code> otherwise.
     */
    public boolean isIndependent(Vector other) {
        return (this.crossProduct(other) != 0.0);
    }

    /**
     * Returns the length of this <code>Vector</code>.
     *
     * @return the length of this <code>Vector</code>.
     */
    public double length() {
        return Math
                .sqrt(this.deltaX * this.deltaX + this.deltaY * this.deltaY);
    }

    /**
     * Returns a <code>String</code> representation of this
     * <code>Vector</code>.
     *
     * @return a <code>String</code> representation of this
     *         <code>Vector</code>.
     */
    public String toString() {
        return "<" + this.deltaX + ", " + this.deltaY + ">";
    }

    /**
     * Tests is this <code>Vector</code> is equal to another
     * <code>Vector</code>. Two <code>Vector</code>s are equal if both of their
     * values are the same.
     *
     * @param o the object for which to test equality.
     * @return <code>true</code> if the two <code>Vector</code>s are the same,
     *         otherwise <code>false</code>.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof Vector) {
            Vector other = (Vector) o;
            return ((this.deltaX == other.deltaX)
                    && (this.deltaY == other.deltaY));
        }
        return false;
    }

    /**
     * Returns a hash code for this <code>Vector</code>.
     *
     * @return a hash code for this <code>Vector</code>, formed by combining
     *         the hash codes of its two components.
     */
    public int hashCode() {
        Double x = Double.valueOf(this.deltaX);
        Double y = Double.valueOf(this.deltaY);
        return ((x.hashCode() & 0xF0F0F0F0) | (y.hashCode() & 0x0F0F0F0F));
    }
}
