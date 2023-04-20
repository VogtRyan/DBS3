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

package ca.ualberta.dbs3.math;

import java.security.*;

/**
 * The <code>SeedGenerator</code> class is responsible for taking a single,
 * potentially low-quality seed, and transforming that seed into an arbitrary
 * number of seemingly unrelated seeds that can be given to instances of
 * pseudorandom number generators, which can be used for statistical (not
 * cryptographically secure) experiments. Given the same initial seed, a
 * <code>SeedGenerator</code> will generate the same sequence of seeds for
 * other pseudorandom number generators.
 */
public class SeedGenerator {
    /**
     * The message digest to use to generate the returned seeds.
     */
    private static final String ALGORITHM = "SHA1";

    /**
     * The provider of the algorithm, that should exist in all Java
     * implementations.
     */
    private static final String PROVIDER = "SUN";

    /**
     * The message digest object used to generate seeds.
     */
    private MessageDigest md;

    /**
     * The bytes of the user-provided seed.
     */
    private byte[] seedBytes;

    /**
     * The bytes of the last digest, or all 0 bytes if none has been performed.
     */
    private byte[] digestBytes;

    /**
     * Creates a new <code>SeedGenerator</code> that will generate an arbitrary
     * number of seeds given a single seed.
     *
     * @param seed the single seed from which to generate the other seeds.
     */
    public SeedGenerator(long seed) {
        /* Attempt to load the message digest */
        try {
            this.md = MessageDigest.getInstance(SeedGenerator.ALGORITHM,
                    SeedGenerator.PROVIDER);
        } catch (Exception e) {
            throw new RuntimeException("Cannot get seed generator");
        }

        /* Ensure that it outputs at least the length of a long */
        int len = this.md.getDigestLength();
        if (len < 8)
            throw new RuntimeException("Invalid seed generator length");

        /* Initialize the seed bytes to the given seed */
        this.seedBytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            this.seedBytes[i] = (byte) (seed & 0xFFL);
            seed >>= 8;
        }

        /* Initialize the first digest bytes to zero */
        this.digestBytes = new byte[len];
        for (int i = 0; i < len; i++)
            this.digestBytes[i] = (byte) 0;
    }

    /**
     * Returns the next seed, which can be passed to an instance of any other
     * pseudorandom number generator.
     *
     * @return the next seed generated by this <code>SeedGenerator</code>.
     */
    public long nextSeed() {
        this.md.update(this.seedBytes);
        this.md.update(this.digestBytes);
        this.digestBytes = this.md.digest();

        long ret = 0;
        for (int i = 0; i < 8; i++) {
            ret <<= 8;
            ret |= (long) (0xFF & (int) this.digestBytes[i]);
        }
        return ret;
    }
}