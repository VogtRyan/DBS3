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

package ca.ualberta.dbs3.commandLine;

import ca.ualberta.dbs3.network.UnsignedInteger;

/**
 * The <code>OptionDuration</code> class represents a command line option to
 * set the duration of the simulation.
 */
public class OptionDuration extends Option {
    /**
     * The default duration to use if none is specified, in minutes.
     */
    public static final double DEFAULT_DURATION = 15.0;

    /**
     * The default unit of measure to use, as an index into {@link #getUnits}.
     */
    public static final int DEFAULT_UNIT = 2;

    /**
     * Different units for expressing duration.
     */
    private static final String[] UNITS =
            {"milliseconds", "seconds", "minutes", "hours", "days"};

    /**
     * The choice to use milliseconds.
     */
    private Choice milliseconds;

    /**
     * The choice to use seconds.
     */
    private Choice seconds;

    /**
     * The choice to use minutes.
     */
    private Choice minutes;

    /**
     * The choice to use hours.
     */
    private Choice hours;

    /**
     * The choice to use days.
     */
    private Choice days;

    /**
     * The double argument given to the milliseconds option.
     */
    private ArgumentDouble argMilliseconds;

    /**
     * The double argument given to the seconds option.
     */
    private ArgumentDouble argSeconds;

    /**
     * The double argument given to the minutes option.
     */
    private ArgumentDouble argMinutes;

    /**
     * The double argument given to the hours option.
     */
    private ArgumentDouble argHours;

    /**
     * The double argument given to the days option.
     */
    private ArgumentDouble argDays;

    /**
     * Creates a new <code>OptionDuration</code> to be added to a parser.
     */
    public OptionDuration() {
        super("Duration");

        this.milliseconds = new Choice("milliseconds");
        this.seconds = new Choice("seconds");
        this.minutes = new Choice("minutes");
        this.hours = new Choice("hours");
        this.days = new Choice("days");

        double max = (double) UnsignedInteger.MAX_VALUE;
        this.argMilliseconds = new ArgumentDouble("ms", 0.0, 0.0, max);
        this.argSeconds = new ArgumentDouble("s", 0.0, 0.0, max / 1000.0);
        this.argMinutes = new ArgumentDouble("m",
                OptionDuration.DEFAULT_DURATION, 0.0, max / 60000.0);
        this.argHours = new ArgumentDouble("h", 0.0, 0.0, max / 3600000.0);
        this.argDays = new ArgumentDouble("d", 0.0, 0.0, max / 86400000.0);

        this.milliseconds.add(this.argMilliseconds);
        this.seconds.add(this.argSeconds);
        this.minutes.add(this.argMinutes);
        this.hours.add(this.argHours);
        this.days.add(this.argDays);

        this.add(this.milliseconds);
        this.add(this.seconds);
        this.addDefault(this.minutes);
        this.add(this.hours);
        this.add(this.days);
    }

    /**
     * Returns the duration in milliseconds parsed off the command line.
     *
     * @return the duration specified by the user.
     */
    public UnsignedInteger getDuration() {
        double duration;
        int index;
        if (this.milliseconds.isActive()) {
            duration = this.argMilliseconds.getValue();
            index = 0;
        } else if (this.seconds.isActive()) {
            duration = this.argSeconds.getValue();
            index = 1;
        } else if (this.minutes.isActive()) {
            duration = this.argMinutes.getValue();
            index = 2;
        } else if (this.hours.isActive()) {
            duration = this.argHours.getValue();
            index = 3;
        } else {
            duration = this.argDays.getValue();
            index = 4;
        }
        return OptionDuration.getDuration(duration, index);
    }

    /**
     * Returns a string with a description of the current choice for this
     * option.
     *
     * @return a description of the current choice.
     */
    public String getDescription() {
        long ms = this.getDuration().toLong();
        double d = ((double) ms) / 1000.0;
        return String.format("%.3f seconds", d);
    }

    /**
     * Returns an array containing the names of various units that can be used
     * to express a duration.
     *
     * @return an array of names of units.
     */
    public static String[] getUnits() {
        String[] ret = new String[OptionDuration.UNITS.length];
        System.arraycopy(OptionDuration.UNITS, 0, ret, 0, ret.length);
        return ret;
    }

    /**
     * Returns a duration in milliseconds, converted from the given duration in
     * the given unit.
     *
     * @param duration the duration in any unit of time in {@link #getUnits}.
     * @param unit the index into the {@link #getUnits} array.
     * @return the duration in milliseconds, or <code>null</code> if the
     *         converted value is outside the bounds of an
     *         <code>UnsignedInteger</code>.
     * @throws IllegalArgumentException if the given duration is negative or if
     *         the unit is not a valid index into the units array.
     */
    public static UnsignedInteger getDuration(double duration, int unit) {
        if (duration < 0.0)
            throw new IllegalArgumentException("Negative time");
        if (unit < 0 || unit >= OptionDuration.UNITS.length)
            throw new IllegalArgumentException("Invalid unit");

        if (unit == 1)
            duration *= 1000.0;
        else if (unit == 2)
            duration *= 60000.0;
        else if (unit == 3)
            duration *= 3600000.0;
        else if (unit == 4)
            duration *= 86400000.0;

        try {
            return new UnsignedInteger(duration);
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }
}
