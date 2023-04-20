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

package ca.ualberta.dbs3.network;

import java.io.*;
import java.nio.charset.Charset;

/**
 * The <code>BufferReader</code> class is a wrapper around a
 * {@link DataInputStream} that reads a fixed number of bytes from the data
 * input stream. If that number of bytes is large, the buffer reader will read
 * the data in chunks of some maximal size.
 */
public class BufferReader {
    /**
     * The maximum number of bytes to read off the underlying stream at once.
     */
    private static final int MAX_AT_ONCE = 2048;

    /**
     * The total amount of data that this reader will read off the underlying
     * stream.
     */
    private long total;

    /**
     * The total amount of data that this reader has returned from its
     * underlying buffer.
     */
    private long amntReturned;

    /**
     * The amount of data in the buffer that hasn't been returned to the owner
     * of this buffer reader yet.
     */
    private int inBuffer;

    /**
     * The buffer of data read off the underlying stream.
     */
    private byte[] buffer;

    /**
     * The encapsulated data input stream.
     */
    private DataInputStream dis;

    /**
     * Creates a new <code>BufferReader</code> around the given data input
     * stream, that will read (potentially immediately) the given amount of
     * data in total from the stream.
     *
     * @param dis the data input stream from which to read data.
     * @param amount the total amount of data for this reader to read.
     * @throws IllegalArgumentException if the amount of data is not greater
     *         than zero.
     * @throws IOException if there is an error reading data from the given
     *         input stream.
     */
    public BufferReader(DataInputStream dis, long amount) throws IOException {
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount of data");
        this.total = amount;
        this.amntReturned = 0;
        this.dis = dis;

        if (amount < (long) BufferReader.MAX_AT_ONCE)
            this.buffer = new byte[(int) amount];
        else
            this.buffer = new byte[BufferReader.MAX_AT_ONCE];

        this.dis.readFully(this.buffer);
        this.inBuffer = this.buffer.length;
    }

    /**
     * Returns a byte from the underlying input stream.
     *
     * @return a byte from the input stream.
     * @throws IllegalStateException if there are not enough bytes remaining.
     * @throws IOException if there is an error with the underlying data input
     *         stream.
     */
    public byte readByte() throws IOException {
        byte[] ba = new byte[1];
        this.read(ba);
        return ba[0];
    }

    /**
     * Returns an unsigned integer from the underlying input stream.
     *
     * @return an unsigned integer from the input stream.
     * @throws IllegalStateException if there are not enough bytes remaining.
     * @throws IOException if there is an error with the underlying data input
     *         stream.
     */
    public UnsignedInteger readUnsignedInt() throws IOException {
        byte[] ba = new byte[4];
        this.read(ba);
        return new UnsignedInteger(ba);
    }

    /**
     * Returns an ASCII string of the given length from the underlying input
     * stream.
     *
     * @param length the length of the string to return.
     * @return the ASCII string that is read.
     * @throws IllegalArgumentException if the given length is not greater than
     *         zero.
     * @throws IllegalStateException if there are not enough bytes remaining.
     * @throws IOException if there is an error with the underlying data input
     *         stream.
     */
    public String readString(int length) throws IOException {
        if (length <= 0)
            throw new IllegalArgumentException("Invalid string length");
        byte[] ba = new byte[length];
        this.read(ba);
        return new String(ba, Charset.forName("US-ASCII"));
    }

    /**
     * Fills the given array with bytes from the underlying input stream.
     *
     * @param b the array to fill with data.
     * @throws IllegalStateException if there are not enough bytes remaining.
     * @throws IOException if there is an error with the underlying data input
     *         stream.
     */
    public void read(byte[] b) throws IOException {
        /* Check if we've exceeded the amount given to the constructor */
        long totalReturned = b.length + this.amntReturned;
        if (totalReturned < b.length || totalReturned < this.amntReturned
                || totalReturned > this.total)
            throw new IOException(
                    "Requesting too much data from buffer reader");

        /*
         * Copy from the internal buffer (of data read off the input stream) to
         * the provided array. As necessary, read more from the underlying
         * input stream.
         *
         * We read data into the high index of the internal buffer (rather than
         * starting at index 0), so there will always be data at: buffer.length
         * - inBuffer.
         */
        int toGo = b.length;
        int onByte = 0;
        while (toGo > 0) {
            int thisTime = toGo < inBuffer ? toGo : inBuffer;
            System.arraycopy(this.buffer, this.buffer.length - inBuffer, b,
                    onByte, thisTime);
            toGo -= thisTime;
            onByte += thisTime;
            this.inBuffer -= thisTime;
            this.amntReturned += thisTime;
            if (this.inBuffer == 0 && this.amntReturned < this.total) {
                long dataRemaining = this.total - this.amntReturned;
                int refillWith =
                        (dataRemaining < (long) BufferReader.MAX_AT_ONCE)
                                ? ((int) dataRemaining)
                                : BufferReader.MAX_AT_ONCE;
                this.dis.readFully(this.buffer,
                        this.buffer.length - refillWith, refillWith);
                this.inBuffer = refillWith;
            }
        }
    }
}
