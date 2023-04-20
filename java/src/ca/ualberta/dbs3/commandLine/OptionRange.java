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

import ca.ualberta.dbs3.math.*;

/**
 * The <code>OptionRange</code> class represents a command line option to
 * choose a set of bounds and a distribution for a non-negative variable.
 */
public abstract class OptionRange extends Option {
    /**
     * The setting to make the default distribution uniform, as well as the
     * index into {@link #getDistributions} for a uniform random variable.
     */
    public static final int DISTRIBUTION_UNIFORM = 0;

    /**
     * The setting to make the default distribution normal, as well as the
     * index into {@link #getDistributions} for a normal random variable.
     */
    public static final int DISTRIBUTION_NORMAL = 1;

    /**
     * The setting to make the default distribution log-normal, as well as the
     * index into {@link #getDistributions} for a log-normal random variable.
     */
    public static final int DISTRIBUTION_LOG_NORMAL = 2;

    /**
     * Different available distributions that can be chosen by the user.
     */
    private static final String[] DISTRIBUTIONS =
            {"Uniform random", "Scaled normal", "Scaled log-normal"};

    /**
     * The choice to use the uniform random distribution.
     */
    private Choice uniform;

    /**
     * The choice to use the normal distribution.
     */
    private Choice normal;

    /**
     * The choice to use the log-normal distribution.
     */
    private Choice logNormal;

    /**
     * The lower bound argument, shared by all of the choices.
     */
    private ArgumentDouble min;

    /**
     * The upper bound argument, shared by all of the choices.
     */
    private ArgumentDouble max;

    /**
     * The human-readable unit to use in the printed command-line description.
     */
    private String unit;

    /**
     * Creates a new command-line option for specifying a range and
     * distribution for a variable.
     *
     * @param header a header describing this option.
     * @param choicePrefix the prefix to attach to each command-line choice.
     * @param fullUnit the full-length unit description to be printed as the
     *        post-processing description.
     * @param shortUnit the shorter-length unit description to be included in
     *        the parser output.
     * @param defaultMin the default lower bound of the user-specified range.
     * @param defaultMax the default upper bound of the user-specified range.
     * @param defaultDistribution the distribution type to use, if one is not
     *        specified by the user.
     * @throws IllegalArgumentException if a default value is negative, if the
     *         default values cross, or if default distribution is invalid.
     */
    protected OptionRange(String header, String choicePrefix, String fullUnit,
            String shortUnit, double defaultMin, double defaultMax,
            int defaultDistribution) {
        super(header);

        this.uniform = new Choice(choicePrefix + "Uniform");
        this.normal = new Choice(choicePrefix + "Normal");
        this.logNormal = new Choice(choicePrefix + "LogNormal");
        if (defaultMin < 0.0 || defaultMax < 0.0 || defaultMin > defaultMax)
            throw new IllegalArgumentException("Invalid default range values");
        this.min = new ArgumentDouble("min" + shortUnit, defaultMin, 0.0);
        this.max = new ArgumentDouble("max" + shortUnit, defaultMax, 0.0);
        this.unit = fullUnit;

        this.uniform.add(this.min);
        this.uniform.add(this.max);
        this.normal.add(this.min);
        this.normal.add(this.max);
        this.logNormal.add(this.min);
        this.logNormal.add(this.max);

        switch (defaultDistribution) {
            case OptionRange.DISTRIBUTION_UNIFORM:
                this.addDefault(this.uniform);
                this.add(this.normal);
                this.add(this.logNormal);
                break;
            case OptionRange.DISTRIBUTION_NORMAL:
                this.add(this.uniform);
                this.addDefault(this.normal);
                this.add(this.logNormal);
                break;
            case OptionRange.DISTRIBUTION_LOG_NORMAL:
                this.add(this.uniform);
                this.add(this.normal);
                this.addDefault(this.logNormal);
                break;
            default:
                throw new IllegalArgumentException(
                        "Invalid default distribution" + "type");
        }
    }

    /**
     * Returns the range specified by the user, or the default range if none
     * has been specified.
     *
     * @return the user-chosen range.
     */
    public Range getRange() {
        return OptionRange.getRange(this.min.getValue(), this.max.getValue(),
                this.getCurrentType());
    }

    /**
     * Tests whether the arguments are valid by ensuring that the bounds do not
     * cross.
     *
     * @return <code>true</code> if the values represent a valid range, or
     *         <code>false</code> if they cross.
     */
    public boolean isValid() {
        return (this.min.getValue() <= this.max.getValue());
    }

    /**
     * Returns a string summarizing the currently selected range.
     *
     * @return a string summarizing the currently selected choice.
     */
    public String getDescription() {
        return Double.toString(this.min.getValue()) + " - "
                + this.max.getValue() + " " + this.unit + " ("
                + OptionRange.DISTRIBUTIONS[this.getCurrentType()]
                        .toLowerCase()
                + " distribution)";
    }

    /**
     * Returns an array containing the names of various distributions that can
     * be specified by the user.
     *
     * @return an array of names of distributions.
     */
    public static String[] getDistributions() {
        String[] ret = new String[OptionRange.DISTRIBUTIONS.length];
        System.arraycopy(OptionRange.DISTRIBUTIONS, 0, ret, 0, ret.length);
        return ret;
    }

    /**
     * Returns a range with the given bounds and distribution type.
     *
     * @param min the lower bound of the range.
     * @param max the upper bound of the range.
     * @param distribution the distribution type, as an index into the
     *        {@link #getDistributions} array.
     * @return the requested range.
     * @throws IllegalArgumentException if the given bounds allow negative
     *         values or if they cross, or if the distribution is not a valid
     *         index into the distributions array.
     */
    public static Range getRange(double min, double max, int distribution) {
        if (min < 0.0 || max < 0.0 || min > max)
            throw new IllegalArgumentException("Invalid range values");
        if (distribution < 0
                || distribution >= OptionRange.DISTRIBUTIONS.length)
            throw new IllegalArgumentException("Invalid distribution type");

        switch (distribution) {
            case OptionRange.DISTRIBUTION_UNIFORM:
                return new RangeUniform(min, max);
            case OptionRange.DISTRIBUTION_NORMAL:
                return new RangeNormal(min, max);
            case OptionRange.DISTRIBUTION_LOG_NORMAL:
                return new RangeLogNormal(min, max);
            default:
                throw new IllegalStateException("Cannot reach");
        }
    }

    /**
     * Returns the index into the {@link #getDistributions} array matching the
     * distribution of the given range.
     *
     * @param range the range for which to determine the distribution.
     * @return the distribution of the given range.
     * @throws IllegalArgumentException if the distribution of the given range
     *         is not recognized.
     */
    public static int getDistribution(Range range) {
        if (range instanceof RangeUniform)
            return OptionRange.DISTRIBUTION_UNIFORM;
        else if (range instanceof RangeNormal)
            return OptionRange.DISTRIBUTION_NORMAL;
        else if (range instanceof RangeLogNormal)
            return OptionRange.DISTRIBUTION_LOG_NORMAL;
        else
            throw new IllegalArgumentException("Unknown range distribution");
    }

    /**
     * Returns the index of the currently selected distribution type.
     *
     * @return the index into the {@link #getDistributions} array that is
     *         currently selected.
     */
    private int getCurrentType() {
        if (this.uniform.isActive())
            return OptionRange.DISTRIBUTION_UNIFORM;
        else if (this.normal.isActive())
            return OptionRange.DISTRIBUTION_NORMAL;
        else
            return OptionRange.DISTRIBUTION_LOG_NORMAL;
    }
}
