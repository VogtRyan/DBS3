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

/**
 * Classes that implement the <code>TimeButtonListener</code> interface are
 * able to be notified when a {@link TimeButton} is clicked. That notification
 * will include information about the command displayed on the
 * <code>TimeButton</code> when it was clicked.
 */
public interface TimeButtonListener {
    /**
     * Called when a <code>TimeButton</code> displaying the
     * {@link TimeButton.Command#BEGINNING} command is clicked.
     *
     * @param button the button that was clicked.
     */
    public void beginning(TimeButton button);

    /**
     * Called when a <code>TimeButton</code> displaying the
     * {@link TimeButton.Command#PREV_CHAPTER} command is clicked.
     *
     * @param button the button that was clicked.
     */
    public void prevChapter(TimeButton button);

    /**
     * Called when a <code>TimeButton</code> displaying the
     * {@link TimeButton.Command#PAUSE} command is clicked.
     *
     * @param button the button that was clicked.
     */
    public void pause(TimeButton button);

    /**
     * Called when a <code>TimeButton</code> displaying the
     * {@link TimeButton.Command#PLAY} command is clicked.
     *
     * @param button the button that was clicked.
     */
    public void play(TimeButton button);

    /**
     * Called when a <code>TimeButton</code> displaying the
     * {@link TimeButton.Command#NEXT_CHAPTER} command is clicked.
     *
     * @param button the button that was clicked.
     */
    public void nextChapter(TimeButton button);

    /**
     * Called when a <code>TimeButton</code> displaying the
     * {@link TimeButton.Command#END} command is clicked.
     *
     * @param button the button that was clicked.
     */
    public void end(TimeButton button);
}
