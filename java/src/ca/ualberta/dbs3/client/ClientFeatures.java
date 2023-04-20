/*
 * Copyright (c) 2011-2023 Ryan Vogt <rvogt@ualberta.ca>
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

package ca.ualberta.dbs3.client;

import ca.ualberta.dbs3.network.UnsignedInteger;

/**
 * The <code>ClientFeatures</code> class represents a set of optional features
 * that UAMP and MVISP clients may choose to implement.
 */
public class ClientFeatures {
    /**
     * The flag for indicating that a client supports 3D data.
     */
    private static final long SUPPORT_3D = 0x80000000L;

    /**
     * The flag for indicating that a client supports agents that appears and
     * disappear.
     */
    private static final long SUPPORT_APPEAR_DISAPPEAR = 0x40000000L;

    /**
     * The set of features supported by the client in UAMP format.
     */
    private long features;

    /**
     * Creates a new empty set of client features, in which no optional
     * features are supported.
     */
    public ClientFeatures() {
        this.features = 0L;
    }

    /**
     * Creates a new set of client features in which the features denoted by
     * the 32-bit bit field are supported.
     *
     * @param features the supported features.
     */
    public ClientFeatures(UnsignedInteger features) {
        this.features = features.toLong();
    }

    /**
     * Sets whether the client supports receiving 3D data.
     *
     * @param support <code>true</code> if the client does support receiving
     *        mobility data from the server where the z coordinates may not be
     *        zero, or <code>false</code> if the client assumes that the z
     *        coordinate of all mobility data is always zero.
     */
    public void set3DSupport(boolean support) {
        if (support)
            this.features |= ClientFeatures.SUPPORT_3D;
        else
            this.features &= ~(ClientFeatures.SUPPORT_3D);
    }

    /**
     * Returns whether or not the client supports receiving 3D data.
     *
     * @return <code>true</code> if the client supports receiving mobility data
     *         from the server where the z coordinates may not be zero, or
     *         <code>false</code> if the client assumes that the z coordinate
     *         of all mobility data is always zero.
     */
    public boolean get3DSupport() {
        return ((this.features & ClientFeatures.SUPPORT_3D) != 0L);
    }

    /**
     * Sets whether the client supports agents appears and disappearing within
     * the simulation environment.
     *
     * @param support <code>true</code> if the client supports mobility data
     *        where the present flag may not be set, or <code>false</code> if
     *        the client assumes that all agents are present at all times.
     */
    public void setAppearDisappearSupport(boolean support) {
        if (support)
            this.features |= ClientFeatures.SUPPORT_APPEAR_DISAPPEAR;
        else
            this.features &= ~(ClientFeatures.SUPPORT_APPEAR_DISAPPEAR);
    }

    /**
     * Returns whether or not the client supports agents appearing and
     * disappearing within the simulation environment.
     *
     * @return <code>true</code> if the client supports mobility data where the
     *         present flag may not be set, or <code>false</code> if the client
     *         assumes that all agents are present at all times.
     */
    public boolean getAppearDisappearSupport() {
        return ((this.features
                & ClientFeatures.SUPPORT_APPEAR_DISAPPEAR) != 0L);
    }

    /**
     * Returns an unsigned integer that, when written in network order, will
     * form a 32-bit bit field in UAMP format indicating supported features.
     *
     * @return the UAMP supported features bit field.
     */
    public UnsignedInteger getBitField() {
        return new UnsignedInteger(this.features);
    }
}
