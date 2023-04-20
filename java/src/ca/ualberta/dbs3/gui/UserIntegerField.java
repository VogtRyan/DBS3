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

package ca.ualberta.dbs3.gui;

import javax.swing.text.*;

/**
 * The <code>UserIntegerField</code> class represents a text field that can
 * only contain non-negative integer values.
 */
public class UserIntegerField extends UserTextField {
    /**
     * Creates a new <code>UserIntegerField</code> containing the given value.
     *
     * @param initialValue the initial value the field will contain.
     * @throws IllegalArgumentException if <code>initialValue</code> is
     *         negative.
     */
    public UserIntegerField(int initialValue) {
        super("" + initialValue);
        if (initialValue < 0)
            throw new IllegalArgumentException("Negative value");
    }

    /**
     * Creates a new <code>UserIntegerField</code> containing the given value.
     *
     * @param initialValue the initial value the field will contain.
     * @throws IllegalArgumentException if <code>initialValue</code> is
     *         negative.
     */
    public UserIntegerField(long initialValue) {
        super("" + initialValue);
        if (initialValue < 0)
            throw new IllegalArgumentException("Negative value");
    }

    /**
     * Returns the contents of this text field, or <code>-1</code> if there is
     * an error making the conversion to <code>int</code>.
     *
     * @return the integer value of the text field or <code>-1</code>.
     */
    public int getInteger() {
        try {
            return Integer.valueOf(this.getText());
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    /**
     * Returns the contents of this text field, or <code>-1</code> if there is
     * an error making the conversion to <code>long</code>.
     *
     * @return the long value of the text field or <code>-1</code>.
     */
    public long getLong() {
        try {
            return Long.valueOf(this.getText());
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    /**
     * Creates the default implementation of the model to be used.
     *
     * @return the model to be used.
     */
    protected Document createDefaultModel() {
        return new IntegerDocument();
    }

    /**
     * The <code>IntegerDocument</code> class represents a document that can
     * contain only a single non-negative integer.
     */
    private class IntegerDocument extends PlainDocument {
        /**
         * Inserts a string of content, if it conforms to the required integer
         * structure.
         *
         * @param offs the offset into the document to insert the content.
         * @param str the string to insert.
         * @param a the attributes to associate with the inserted content.
         * @throws BadLocationException if the given insert position is not a
         *         valid position within the document.
         */
        public void insertString(int offs, String str, AttributeSet a)
                throws BadLocationException {
            if (str == null)
                return;
            if (str.matches("\\d*"))
                super.insertString(offs, str, a);
        }
    }
}
