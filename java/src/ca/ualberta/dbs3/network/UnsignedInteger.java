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

package ca.ualberta.dbs3.network;

/**
 * The <code>UnsignedInteger</code> class represents a 32-bit unsigned integer
 * value.
 */
public class UnsignedInteger implements Comparable<UnsignedInteger> {
    /**
     * The minimal value that can be assigned to an
     * <code>UnsignedInteger</code>, 0, in <code>long</code> format.
     */
    public static final long MIN_VALUE = 0L;

    /**
     * The maximal value that can be assigned to an
     * <code>UnsignedInteger</code>, 4294967295, in <code>long</code> format.
     */
    public static final long MAX_VALUE = 0xFFFFFFFFL;

    /**
     * The value of the unsigned integer, in <code>long</code> format.
     */
    private long value;

    /**
     * Creates a new <code>UnsignedInteger</code> from the given
     * <code>long</code> value.
     *
     * @param value the value of the unsigned integer.
     * @throws IllegalArgumentException if <code>value</code> is less than
     *         {@link #MIN_VALUE} or greater than {@link #MAX_VALUE}.
     */
    public UnsignedInteger(long value) {
        if (value < UnsignedInteger.MIN_VALUE
                || value > UnsignedInteger.MAX_VALUE)
            throw new IllegalArgumentException("Value out of range");
        this.value = value;
    }

    /**
     * Creates a new <code>UnsignedInteger</code> by rounding the given
     * <code>double</code> value to the nearest whole number.
     *
     * @param value the value to be rounded to the nearest whole number.
     * @throws IllegalArgumentException if when rounded to the nearest whole
     *         number, <code>value</code> becomes less than {@link #MIN_VALUE}
     *         or greater than {@link #MAX_VALUE}.
     */
    public UnsignedInteger(double value) {
        if (Double.isNaN(value))
            throw new IllegalArgumentException("Value out of range");
        this.value = Math.round(value);
        if (this.value < UnsignedInteger.MIN_VALUE
                || this.value > UnsignedInteger.MAX_VALUE)
            throw new IllegalArgumentException("Value out of range");
    }

    /**
     * Creates a new <code>UnsignedInteger</code> from the given four bytes in
     * network order.
     *
     * @param value the value of the unsigned integer, in network order.
     * @throws IllegalArgumentException if value is not 32 bits in length
     *         (i.e., a <code>byte</code> array of length 4).
     */
    public UnsignedInteger(byte[] value) {
        this.value = UnsignedInteger.toLong(value);
    }

    /**
     * Creates a new <code>UnsignedInteger</code> from the given four bytes in
     * network order.
     *
     * @param array the array containing, at some offset, the value of the
     *        unsigned integer in network order.
     * @param offset the offset into the array at which to start reading the
     *        unsigned integer.
     * @throws IllegalArgumentException if there are not 32 bits worth of data
     *         in the array after the given offset.
     */
    public UnsignedInteger(byte[] array, int offset) {
        this.value = UnsignedInteger.toLong(array, offset);
    }

    /**
     * Returns the value of this <code>UnsignedInteger</code> expressed as a
     * <code>long</code>.
     *
     * @return the value of the <code>UnsignedInteger</code>.
     */
    public long toLong() {
        return this.value;
    }

    /**
     * Converts the given four bytes in network order, representing an unsigned
     * integer, into a long value.
     *
     * @param value the value of the unsigned integer, in network order.
     * @return the long equivalent of the given four network-order bytes.
     * @throws IllegalArgumentException if value is not 32 bits in length
     *         (i.e., a <code>byte</code> array of length 4).
     */
    public static long toLong(byte[] value) {
        if (value.length != 4)
            throw new IllegalArgumentException("Invalid array length");
        return UnsignedInteger.toLong(value, 0);
    }

    /**
     * Converts the given four bytes in network order, representing an unsigned
     * integer, into a long value.
     *
     * @param array the array containing, at some offset, the value of the
     *        unsigned integer in network order.
     * @param offset the offset into the array at which to start reading the
     *        unsigned integer.
     * @return the long equivalent of the given four network-order bytes.
     * @throws IllegalArgumentException if there are not 32 bits worth of data
     *         in the array after the given offset.
     */
    public static long toLong(byte[] array, int offset) {
        if (offset < 0 || offset > array.length - 4)
            throw new IllegalArgumentException("Invalid array offset");
        long value = 0;
        value |= (0xFFL & ((long) array[offset])) << 24;
        value |= (0xFFL & ((long) array[offset + 1])) << 16;
        value |= (0xFFL & ((long) array[offset + 2])) << 8;
        value |= (0xFFL & ((long) array[offset + 3]));
        return value;
    }

    /**
     * Returns the value of this <code>UnsignedInteger</code> as four bytes in
     * network order.
     *
     * @return the value of the <code>UnsignedInteger</code> in network order.
     */
    public byte[] toBytes() {
        byte[] ret = new byte[4];
        this.toBytes(ret, 0);
        return ret;
    }

    /**
     * Converts the value of this <code>UnsignedInteger</code> to four
     * network-order bytes, and places them into the given array at the given
     * offset.
     *
     * @param array the array into which to place the four bytes.
     * @param offset the starting offset into the array into which to place the
     *        four bytes.
     * @throws IllegalArgumentException if there are not four bytes after the
     *         given offset into which to place the result.
     */
    public void toBytes(byte[] array, int offset) {
        UnsignedInteger.toBytes(this.value, array, offset);
    }

    /**
     * Converts the given value, which must be in the unsigned integer range,
     * to four network-order bytes, and returns them as an array.
     *
     * @param value the value to convert.
     * @return the bytes in network-order.
     * @throws IllegalArgumentException if <code>value</code> is less than
     *         {@link #MIN_VALUE} or greater than {@link #MAX_VALUE}.
     */
    public static byte[] toBytes(long value) {
        byte[] ret = new byte[4];
        UnsignedInteger.toBytes(value, ret, 0);
        return ret;
    }

    /**
     * Converts the given value, which must be in the unsigned integer range,
     * to four network-order bytes, and places them in the given array at the
     * given offset.
     *
     * @param value the value to convert.
     * @param array the array into which to place the four bytes.
     * @param offset the starting offset into the array into which to place the
     *        four bytes.
     * @throws IllegalArgumentException if there are not four bytes after the
     *         given offset into which to place the result, or if
     *         <code>value</code> is less than {@link #MIN_VALUE} or greater
     *         than {@link #MAX_VALUE}.
     */
    public static void toBytes(long value, byte[] array, int offset) {
        if (value < UnsignedInteger.MIN_VALUE
                || value > UnsignedInteger.MAX_VALUE)
            throw new IllegalArgumentException("Value out of range");
        if (offset < 0 || offset > array.length - 4)
            throw new IllegalArgumentException("Invalid array offset");
        array[offset] = (byte) ((value & 0xFF000000L) >> 24);
        array[offset + 1] = (byte) ((value & 0xFF0000L) >> 16);
        array[offset + 2] = (byte) ((value & 0xFF00L) >> 8);
        array[offset + 3] = (byte) ((value & 0xFFL));
    }

    /**
     * Tests is this <code>UnsignedInteger</code> is equal to another
     * <code>UnsignedInteger</code>. Two <code>UnsignedInteger</code>s are
     * equal if they represent the same 32-bit value.
     *
     * @param o the object for which to test equality.
     * @return <code>true</code> if the two <code>UnsignedInteger</code>s are
     *         the same, otherwise <code>false</code>.
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof UnsignedInteger) {
            UnsignedInteger other = (UnsignedInteger) o;
            return (this.value == other.value);
        }
        return false;
    }

    /**
     * Returns a hash code for this <code>UnsignedInteger</code>.
     *
     * @return a hash code for this <code>UnsignedInteger</code>, equal to the
     *         hash code of its value expressed as a {@link Long}.
     */
    public int hashCode() {
        return Long.valueOf(this.value).hashCode();
    }

    /**
     * Returns a <code>String</code> representation of this
     * <code>UnsignedInteger</code>.
     *
     * @return a <code>String</code> representation of this
     *         <code>UnsignedInteger</code>.
     */
    public String toString() {
        return Long.toString(this.value);
    }

    /**
     * Compares this <code>UnsignedInteger</code> with the specified
     * <code>UnsignedInteger</code> for order.
     *
     * @param o the <code>UnsignedInteger</code> to which to compare this
     *        object.
     * @return a negative number if this <code>UnsignedInteger</code> is
     *         smaller than the given <code>UnsignedInteger</code>, a positive
     *         number if it is larger, and <code>0</code> if they are equal.
     */
    public int compareTo(UnsignedInteger o) {
        if (this.value > o.value)
            return 1;
        else if (this.value < o.value)
            return -1;
        else
            return 0;
    }
}
