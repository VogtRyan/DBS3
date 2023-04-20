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

package ca.ualberta.dbs3.simulations;

import java.util.Arrays;

/**
 * The <code>TimeDivider</code> class divides an amount of time into
 * fixed-length intervals (i.e., chapters).
 */
public class TimeDivider {
    /**
     * The maximum time, i.e., the final marker.
     */
    private long maxTime;

    /**
     * All of the markers that occur between chapters, i.e., all markers except
     * the implicit zero-time and maxTime-time markers.
     */
    private long[] innerMarkers;

    /**
     * Creates a new <code>TimeDivider</code> that will divide the given number
     * of milliseconds into, at most, the given number of chapters.
     *
     * @param milliseconds the number of milliseconds to divide.
     * @param chapters the maximum number of chapters to create, which must be
     *        greater than zero.
     * @throws IllegalArgumentException if the amount of time is negative, or
     *         if the number of chapters is not greater than zero.
     */
    public TimeDivider(long milliseconds, int chapters) {
        if (milliseconds < 0)
            throw new IllegalArgumentException("Invalid amount of time");
        if (chapters <= 0)
            throw new IllegalArgumentException("Invalid number of chapters");
        this.maxTime = milliseconds;

        /*
         * There cannot be more chapters than discrete milliseconds, unless the
         * time being divided is zero milliseconds (in which case, there will
         * be one chapter)
         */
        if (chapters > milliseconds) {
            chapters = (int) milliseconds;
            if (chapters == 0)
                chapters = 1;
        }

        /*
         * There will be one fewer inner markers than chapters, since inner
         * markers represent the point between chapters.
         */
        this.innerMarkers = new long[chapters - 1];
        double chapterLength = ((double) milliseconds) / ((double) chapters);
        double currentChapterEnd = chapterLength;
        for (int i = 0; i < chapters - 1; i++) {
            this.innerMarkers[i] = Math.round(currentChapterEnd);
            currentChapterEnd += chapterLength;
        }
    }

    /**
     * Returns the number of chapters into which time is divided, which is
     * guaranteed to be positive.
     *
     * @return the number of chapters into which time is divided.
     */
    public int numChapters() {
        return this.innerMarkers.length + 1;
    }

    /**
     * Returns the number of markers used in dividing time into chapters. A
     * marker denotes the beginning or end of a chapter, or the beginning of
     * one chapter and the end of another. Typically, there will be one more
     * marker than there are chapters, except when the final time is zero
     * milliseconds.
     *
     * @return the number of discrete markers used to divide time.
     */
    public int numMarkers() {
        if (this.maxTime == 0L)
            return 1;
        else
            return this.innerMarkers.length + 2;
    }

    /**
     * Converts the given marker index, starting from zero, to a time.
     *
     * @param index the index of the marker.
     * @return the marker time.
     * @throws ArrayIndexOutOfBoundsException if the given index is negative or
     *         greater than or equal to {@link #numMarkers}.
     */
    public long getMarker(int index) {
        int num = this.numMarkers();
        if (index < 0 || index >= num)
            throw new ArrayIndexOutOfBoundsException("Invalid index");

        if (index == 0)
            return 0;
        else if (index == num - 1)
            return this.maxTime;
        else
            return this.innerMarkers[index - 1];
    }

    /**
     * Returns the index of the nearest time marker occuring strictly before
     * the given time, or index zero if the given time is zero. Indices run
     * from zero to <code>{@link #numMarkers}-1</code>.
     *
     * @param currentMilliseconds the time from which to search back for a
     *        marker.
     * @return the index of the nearest marker time strictly prior to the given
     *         time, or <code>0</code> if the given time is zero.
     * @throws IllegalArgumentException if the current time is negative or
     *         greater than the final time.
     */
    public int prevIndex(long currentMilliseconds) {
        return this.prevIndex(currentMilliseconds, 0);
    }

    /**
     * Returns the nearest time marker occuring strictly before the given time,
     * or time zero if the given time is zero.
     *
     * @param currentMilliseconds the time from which to search back for a
     *        marker.
     * @return the nearest marker time strictly prior to the given time, or
     *         <code>0</code> if the given time is zero.
     * @throws IllegalArgumentException if the current time is negative or
     *         greater than the final time.
     */
    public long prevMarker(long currentMilliseconds) {
        return this.prevMarker(currentMilliseconds, 0);
    }

    /**
     * Returns the index of the nearest time marker occuring strictly before
     * the given time subject to the grace period, or index zero if the given
     * time is zero. If the nearest marker time occuring strictly before the
     * given time is both greater than zero and is the largest marker time
     * occuring in the grace period range of
     * <code>[currentMilliseconds-gracePeriod, currentMilliseconds]</code>, the
     * marker index before that one will be returned. Indices run from zero to
     * <code>{@link #numMarkers}-1</code>.
     *
     * @param currentMilliseconds the time from which to search back for a
     *        marker.
     * @param gracePeriod the size of the grace period range.
     * @return the index of the nearest marker time prior to the given time,
     *         considering the given grace period, or <code>0</code> if we jump
     *         all the way back to the zero-time marker.
     * @throws IllegalArgumentException if the current time is negative or
     *         greater than the final time, or if the grace period size is
     *         negative.
     */
    public int prevIndex(long currentMilliseconds, long gracePeriod) {
        if (currentMilliseconds < 0 || currentMilliseconds > this.maxTime)
            throw new IllegalArgumentException("Invalid current time");
        if (gracePeriod < 0L)
            throw new IllegalArgumentException("Invalid grace period");

        /*
         * Binary search to find which two markers the current time sits
         * between. If the result is >= 0, that means the current time is
         * exactly on an inner marker, so we just go back to the previous
         * marker.
         */
        if (currentMilliseconds == this.maxTime)
            return this.innerMarkers.length;
        int res = Arrays.binarySearch(this.innerMarkers, currentMilliseconds);
        if (res >= 0)
            return res;

        /*
         * At this point, we're between markers; find the previous marker.
         * Compute prev in [-1, length-1], where prev == -1 if the previous
         * marker is the start (zero-time) marker.
         */
        int prev = (-res) - 2;
        if (prev < 0)
            return 0;
        long prevMarkerTime = this.innerMarkers[prev];

        /*
         * If the difference between the current time and the time of the
         * previous marker is less than the grace period, we'll actually jump
         * to the marker *before* the previous marker.
         */
        if (currentMilliseconds - prevMarkerTime > gracePeriod)
            return prev + 1;
        else
            return prev;
    }

    /**
     * Returns the nearest time marker occuring strictly before the given time
     * subject to the grace period, or index zero if the given time is zero. If
     * the nearest marker time occuring strictly before the given time is both
     * greater than zero and is the largest marker time occuring in the grace
     * period range of
     * <code>[currentMilliseconds-gracePeriod, currentMilliseconds]</code>, the
     * marker before that one will be returned.
     *
     * @param currentMilliseconds the time from which to search back for a
     *        marker.
     * @param gracePeriod the size of the grace period range.
     * @return the nearest marker time prior to the given time, considering the
     *         given grace period, or <code>0</code> if we jump all the way
     *         back to the zero-time marker.
     * @throws IllegalArgumentException if the current time is negative or
     *         greater than the final time, or if the grace period size is
     *         negative.
     */
    public long prevMarker(long currentMilliseconds, long gracePeriod) {
        return this
                .getMarker(this.prevIndex(currentMilliseconds, gracePeriod));
    }

    /**
     * Returns the index of the nearest time marker occuring on or before the
     * given time. Indices run from zero to <code>{@link #numMarkers}-1</code>.
     *
     * @param currentMilliseconds the time from which to search back for a
     *        marker.
     * @return the index of the nearest marker time on or before the given
     *         time.
     * @throws IllegalArgumentException if the current time is negative or
     *         greater than the final time.
     */
    public int recentIndex(long currentMilliseconds) {
        if (currentMilliseconds < 0 || currentMilliseconds > this.maxTime)
            throw new IllegalArgumentException("Invalid current time");

        /*
         * Binary search to find which two markers the current time sits
         * between. If the result is >= 0, that means the current time is
         * exactly on an inner marker. Otherwise, return the previous marker.
         */
        if (currentMilliseconds == this.maxTime)
            return this.numMarkers() - 1;
        int res = Arrays.binarySearch(this.innerMarkers, currentMilliseconds);
        if (res >= 0)
            return res + 1;
        else
            return (-res) - 1;
    }

    /**
     * Returns the nearest time marker occuring on or before the given time.
     *
     * @param currentMilliseconds the time from which to search back for a
     *        marker.
     * @return the nearest marker time on or before the given time.
     * @throws IllegalArgumentException if the current time is negative or
     *         greater than the final time.
     */
    public long recentMarker(long currentMilliseconds) {
        return this.getMarker(this.recentIndex(currentMilliseconds));
    }

    /**
     * Returns the index of the nearest time marker occuring strictly after the
     * given time, or the maximal index if the given time is the final time.
     * Indices run from zero to <code>{@link #numMarkers}-1</code>.
     *
     * @param currentMilliseconds the time from which to search forward for a
     *        marker.
     * @return the index of the nearest marker time after the given time, or
     *         the index of the final time if the given time is equal to the
     *         final time.
     * @throws IllegalArgumentException if the current time is negative or
     *         greater than the final time.
     */
    public int nextIndex(long currentMilliseconds) {
        if (currentMilliseconds < 0 || currentMilliseconds > this.maxTime)
            throw new IllegalArgumentException("Invalid current time");
        if (this.maxTime == 0L)
            return 0;

        int res = Arrays.binarySearch(this.innerMarkers, currentMilliseconds);
        if (res >= 0)
            return res + 2;
        else
            return (-res);
    }

    /**
     * Returns the nearest time marker occuring after the given time, or the
     * final time if the given time is the final time.
     *
     * @param currentMilliseconds the time from which to search forward for a
     *        marker.
     * @return the nearest marker time after the given time, or the final time
     *         if the given time is equal to the final time.
     * @throws IllegalArgumentException if the current time is negative or
     *         greater than the final time.
     */
    public long nextMarker(long currentMilliseconds) {
        return this.getMarker(this.nextIndex(currentMilliseconds));
    }
}
