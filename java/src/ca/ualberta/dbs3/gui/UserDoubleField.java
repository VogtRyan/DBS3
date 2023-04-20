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
 * The <code>UserDoubleField</code> class represents a text field that can only
 * contain non-negative floating point values.
 */
public class UserDoubleField extends UserTextField {
    /**
     * Creates a new <code>UserDoubleField</code> containing the given value.
     *
     * @param initialValue the initial value the field will contain.
     * @throws IllegalArgumentException if <code>initialValue</code> is
     *         negative.
     */
    public UserDoubleField(double initialValue) {
        super("" + initialValue);
        if (initialValue < 0.0)
            throw new IllegalArgumentException("Negative value");
    }

    /**
     * Returns the contents of this text field, or <code>-1.0</code> if there
     * is an error making the conversion.
     *
     * @return the double value of the text field or <code>-1.0</code>.
     */
    public double getDouble() {
        try {
            return Double.valueOf(this.getText());
        } catch (NumberFormatException nfe) {
            return -1.0;
        }
    }

    /**
     * Creates the default implementation of the model to be used.
     *
     * @return the model to be used.
     */
    protected Document createDefaultModel() {
        return new DoubleDocument();
    }

    /**
     * The <code>DoubleDocument</code> class represents a document that can
     * contain only a single non-negative floating point value.
     */
    private class DoubleDocument extends PlainDocument {
        /**
         * Inserts a string of content, if it conforms to the required floating
         * point structure.
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

            String oldContent = this.getText(0, this.getLength());
            String regex = "\\d*";
            if (oldContent.indexOf('.') == -1)
                regex = "\\d*(\\.)?\\d*";

            if (str.matches(regex))
                super.insertString(offs, str, a);
        }
    }
}
