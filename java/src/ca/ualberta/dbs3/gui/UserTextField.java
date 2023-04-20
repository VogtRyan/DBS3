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

import java.awt.*;
import javax.swing.*;

/**
 * The <code>UserTextField</code> class represents a text field with the
 * ability to change the colour of its text based on whether or not its
 * contents are legal.
 */
public class UserTextField extends JTextField {
    /**
     * The colour for legal user input.
     */
    private static final Color GOOD_INPUT = Color.BLACK;

    /**
     * The colour for non-legal user input.
     */
    private static final Color BAD_INPUT = Color.RED;

    /**
     * Whether the text is currently legal.
     */
    private boolean isLegal;

    /**
     * Creates a new <code>UserTextField</code> containing the given contents.
     *
     * @param initialContents the initial contents the field will contain.
     */
    public UserTextField(String initialContents) {
        super(initialContents);

        /*
         * Forces setLegal to execute completely, rather than use a cache hit
         */
        this.isLegal = false;
        this.setLegal(true);
    }

    /**
     * Turns the text to its default colour if the input is legal, or turns it
     * a colour to indicate an error otherwise.
     *
     * @param isLegal whether the current text is legal.
     */
    public void setLegal(boolean isLegal) {
        if (isLegal == this.isLegal)
            return;
        this.isLegal = isLegal;
        if (this.isLegal) {
            this.setForeground(UserTextField.GOOD_INPUT);
            this.setSelectedTextColor(UserTextField.GOOD_INPUT);
            this.setCaretColor(UserTextField.GOOD_INPUT);
        } else {
            this.setForeground(UserTextField.BAD_INPUT);
            this.setSelectedTextColor(UserTextField.BAD_INPUT);
            this.setCaretColor(UserTextField.BAD_INPUT);
        }
    }
}
