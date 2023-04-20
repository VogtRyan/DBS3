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

/**
 * The <code>BufferWriter</code> class is a wrapper around a
 * {@link DataOutputStream} that writes a fixed number of bytes to the data
 * output stream. If that number of bytes is large, the buffer writer will send
 * the data in chunks of some maximal size.
 */
public class BufferWriter {
    /**
     * The maximum number of bytes to buffer before writing them to the data
     * output stream.
     */
    private static final int MAX_AT_ONCE = 2048;

    /**
     * The total amount of data for this buffer writer to send.
     */
    private long total;

    /**
     * The amount of data given to this buffer writer so far.
     */
    private long amntGiven;

    /**
     * The amount of unsent data in the buffer right now.
     */
    private int inBuffer;

    /**
     * The buffer of unsent data.
     */
    private byte[] buffer;

    /**
     * The encapsulated data output stream.
     */
    private DataOutputStream dos;

    /**
     * Creates a new <code>BufferWriter</code> around the given data output
     * stream, designed to send the given predetermined amount of data.
     *
     * @param dos the data output stream on which to send the data.
     * @param amount the total amount of data for this writer to send.
     * @throws IllegalArgumentException if the amount of data is not greater
     *         than zero.
     */
    public BufferWriter(DataOutputStream dos, long amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount of data");
        this.total = amount;
        this.amntGiven = 0;
        this.inBuffer = 0;
        this.dos = dos;

        if (amount < (long) BufferWriter.MAX_AT_ONCE)
            this.buffer = new byte[(int) amount];
        else
            this.buffer = new byte[BufferWriter.MAX_AT_ONCE];
    }

    /**
     * Places the given byte into the buffer, to be written to the data output
     * stream.
     *
     * @param b the byte to write.
     * @throws IOException if there is an error writing to the data output
     *         stream, or if the total amount of data given to the constructor
     *         has been exceeded.
     */
    public void write(byte b) throws IOException {
        byte[] ba = new byte[1];
        ba[0] = b;
        this.write(ba);
    }

    /**
     * Places the given unsigned integer into the buffer, to be written to the
     * data output stream.
     *
     * @param ui the unsigned integer to write.
     * @throws IOException if there is an error writing to the data output
     *         stream, or if the total amount of data given to the constructor
     *         has been exceeded.
     */
    public void write(UnsignedInteger ui) throws IOException {
        byte[] ba = ui.toBytes();
        this.write(ba);
    }

    /**
     * Places the given unsigned integer into the buffer, to be written to the
     * data output stream.
     *
     * @param l the unsigned integer to write.
     * @throws IllegalArgumentException if the given long value is outside the
     *         range of an unsigned integer.
     * @throws IOException if there is an error writing to the data output
     *         stream, or if the total amount of data given to the constructor
     *         has been exceeded.
     */
    public void write(long l) throws IOException {
        byte[] ba = UnsignedInteger.toBytes(l);
        this.write(ba);
    }

    /**
     * Places the given bytes into the buffer, to be written to the data output
     * stream.
     *
     * @param b the bytes to write.
     * @throws IOException if there is an error writing to the data output
     *         stream, or if the total amount of data given to the constructor
     *         has been exceeded.
     */
    public void write(byte[] b) throws IOException {
        /* Check if we've exceeded the amount given to the constructor */
        long totalGiven = b.length + this.amntGiven;
        if (totalGiven < b.length || totalGiven < this.amntGiven
                || totalGiven > this.total)
            throw new IOException("Too much data for buffer writer");

        /*
         * As the buffer fills, or if we hit our total amount of data, write to
         * the underlying output stream.
         */
        int toGo = b.length;
        int onByte = 0;
        while (toGo > 0) {
            int bufferLeft = this.buffer.length - inBuffer;
            int thisTime = toGo < bufferLeft ? toGo : bufferLeft;
            System.arraycopy(b, onByte, this.buffer, inBuffer, thisTime);
            toGo -= thisTime;
            onByte += thisTime;
            this.inBuffer += thisTime;
            this.amntGiven += thisTime;
            if (this.inBuffer == this.buffer.length
                    || this.amntGiven == this.total) {
                this.dos.write(this.buffer, 0, this.inBuffer);
                this.inBuffer = 0;
            }
        }
    }
}
