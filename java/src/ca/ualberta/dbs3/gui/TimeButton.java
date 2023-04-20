/*
 * Copyright (c) 2009-2023 Ryan Vogt <rvogt@ualberta.ca>
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
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

/**
 * The <code>TimeButton</code> class represents a button that can be used to
 * alter the current time in a playback. These buttons are the standard buttons
 * found, e.g., on remote controls.
 */
public class TimeButton extends JButton {
    /**
     * The <code>Command</code> enumeration represents the different available
     * types of {@link TimeButton}s.
     */
    public enum Command {
        /**
         * A <code>TimeButton</code> that resets playback to time zero.
         */
        BEGINNING,

        /**
         * A <code>TimeButton</code> that reverses playback by one chapter.
         */
        PREV_CHAPTER,

        /**
         * A <code>TimeButton</code> that pauses a running playback.
         */
        PAUSE,

        /**
         * A <code>TimeButton</code> that starts playback.
         */
        PLAY,

        /**
         * A <code>TimeButton</code> that skips playback ahead to the next
         * chapter.
         */
        NEXT_CHAPTER,

        /**
         * A <code>TimeButton</code> that skips playback ahead to the end.
         */
        END
    }

    /**
     * Which of the six control types this button is currently displaying.
     */
    private Command type;

    /**
     * The height and width of the button in pixels.
     */
    private int size;

    /**
     * The installed <code>TimeButtonListener</code>s.
     */
    private ArrayList<TimeButtonListener> listeners;

    /**
     * Creates a new <code>TimeButton</code> with the given command symbol on
     * it.
     *
     * @param command the type of button to create.
     * @param size the height and width of the button in pixels.
     */
    public TimeButton(Command command, int size) {
        super();
        this.size = size;
        this.listeners = new ArrayList<TimeButtonListener>();
        this.addActionListener(new ClickListener());
        this.setPreferredSize(new Dimension(size, size));
        this.setFocusable(false);
        this.setCommand(command);
    }

    /**
     * Sets the command displayed on this <code>TimeButton</code>.
     *
     * @param command the command displayed on the button.
     */
    public void setCommand(Command command) {
        this.type = command;
        switch (command) {
            case BEGINNING:
                this.setIcon(new PlayBarIcon(false, true));
                break;
            case PREV_CHAPTER:
                this.setIcon(new PlayBarIcon(true, true));
                break;
            case PAUSE:
                this.setIcon(new PauseIcon());
                break;
            case PLAY:
                this.setIcon(new PlayIcon());
                break;
            case NEXT_CHAPTER:
                this.setIcon(new PlayBarIcon(false, false));
                break;
            case END:
                this.setIcon(new PlayBarIcon(true, false));
                break;
            default:
                throw new RuntimeException("Should not reach here");
        }
    }

    /**
     * Adds the time button listener to the set of listeners that will be
     * informed when this button is pressed.
     *
     * @param listener the listener to add. If <code>null</code>, this method
     *        will do nothing.
     */
    public void addTimeButtonListener(TimeButtonListener listener) {
        if (listener != null)
            this.listeners.add(listener);
    }

    /**
     * Sets the drawing colour of the given graphics object to match the text
     * colour in this button (were there any text), based on whether or not the
     * button is enabled; then, enables antialiasing (if it is enabled at the
     * application level) for drawing in the button.
     *
     * @param g the <code>Graphics</code> object to set up.
     */
    private void setupGraphics(Graphics g) {
        Color c;
        if (this.isEnabled())
            c = this.getForeground();
        else {
            c = UIManager.getColor("Button.disabledText");
            if (c == null)
                throw new RuntimeException(
                        "Cannot get disabled button colour");
        }
        g.setColor(c);

        if (g instanceof Graphics2D
                && ApplicationGUI.getApplication().useAA()) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

    /**
     * The <code>ClickListener</code> class is responsible for listening for
     * when this <code>TimeButton</code> is clicked, and passing that event on
     * to any listening <code>TimeButtonListener</code>s.
     */
    private class ClickListener implements ActionListener {
        /**
         * Called when this <code>TimeButton</code> is clicked.
         *
         * @param e the event that caused this function to be called, which is
         *        ignored.
         */
        public void actionPerformed(ActionEvent e) {
            /*
             * We record the current command before informing any listeners, in
             * case any of the listeners execute setCommand on the TimeButton.
             */
            Command cmd = type;
            for (TimeButtonListener l : listeners) {
                switch (cmd) {
                    case BEGINNING:
                        l.beginning(TimeButton.this);
                        break;
                    case PREV_CHAPTER:
                        l.prevChapter(TimeButton.this);
                        break;
                    case PAUSE:
                        l.pause(TimeButton.this);
                        break;
                    case PLAY:
                        l.play(TimeButton.this);
                        break;
                    case NEXT_CHAPTER:
                        l.nextChapter(TimeButton.this);
                        break;
                    case END:
                        l.end(TimeButton.this);
                        break;
                    default:
                        throw new RuntimeException("Should not reach here");
                }
            }
        }
    }

    /**
     * The <code>PauseIcon</code> class represents the two vertical bars drawn
     * inside a pause button.
     */
    private class PauseIcon implements Icon {
        /**
         * The height, in pixels, of the two bars.
         */
        private int height;

        /**
         * The total width, in pixels, of the two vertical bars and the space
         * between them.
         */
        private int width;

        /**
         * The width, in pixels, of a single vertical bar.
         */
        private int rectWidth;

        /**
         * Creates a new <code>PauseIcon</code> based on the size of this
         * <code>TimeButton</code>.
         */
        public PauseIcon() {
            this.height = (int) (TimeButton.this.size * 0.3);
            this.width = (int) (TimeButton.this.size * 0.25);
            this.rectWidth = 2 * this.width / 5;
        }

        /**
         * Returns the total height of the icon in pixels.
         *
         * @return the total height of the icon in pixels.
         */
        public int getIconHeight() {
            return this.height;
        }

        /**
         * Returns the total width of the icon in pixels.
         *
         * @return the total width of the icon in pixels.
         */
        public int getIconWidth() {
            return this.width;
        }

        /**
         * Paints the icon onto the given graphics object at the given x and y
         * offset.
         *
         * @param c the component onto which we are drawing the icon.
         * @param g the graphics object on which to draw the icon.
         * @param x the x offset into <code>g</code> at which to draw the icon.
         * @param y the y offset into <code>g</code> at which to draw the icon.
         */
        public void paintIcon(Component c, Graphics g, int x, int y) {
            int space = this.width - this.rectWidth * 2;
            if (this.height <= 0 || this.width <= 0 || this.rectWidth <= 0
                    || space <= 0)
                return;

            setupGraphics(g);
            g.fillRect(x, y, rectWidth, this.height);
            g.fillRect(x + rectWidth + space, y, rectWidth, this.height);
        }
    }

    /**
     * The <code>PlayIcon</code> class represents the triangle drawn onto a
     * play button.
     */
    private class PlayIcon implements Icon {
        /**
         * Buffered space to hold the three x coordinates of the triangle
         * points.
         */
        private int[] xPoints;

        /**
         * Buffered space to hold the three y coordinates of the triangle
         * points.
         */
        private int[] yPoints;

        /**
         * The height, in pixels, of the triangle.
         */
        private int height;

        /**
         * The width, in pixels, of the triangle.
         */
        private int width;

        /**
         * Creates a new <code>PlayIcon</code> based on the size of this
         * <code>TimeButton</code>.
         */
        public PlayIcon() {
            this.xPoints = new int[3];
            this.yPoints = new int[3];
            this.height = (int) (TimeButton.this.size * 0.3);
            this.width = (int) (TimeButton.this.size * 0.25);
        }

        /**
         * Returns the total height of the icon in pixels.
         *
         * @return the total height of the icon in pixels.
         */
        public int getIconHeight() {
            return this.height;
        }

        /**
         * Returns the total width of the icon in pixels.
         *
         * @return the total width of the icon in pixels.
         */
        public int getIconWidth() {
            return this.width;
        }

        /**
         * Paints the icon onto the given graphics object at the given x and y
         * offset.
         *
         * @param c the component onto which we are drawing the icon.
         * @param g the graphics object on which to draw the icon.
         * @param x the x offset into <code>g</code> at which to draw the icon.
         * @param y the y offset into <code>g</code> at which to draw the icon.
         */
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (this.height <= 0 || this.width <= 0 || this.height / 2 <= 0)
                return;
            this.xPoints[0] = this.xPoints[1] = x;
            this.xPoints[2] = x + this.width;
            this.yPoints[0] = y;
            this.yPoints[1] = y + this.height;
            this.yPoints[2] = y + this.height / 2;

            setupGraphics(g);
            g.fillPolygon(this.xPoints, this.yPoints, 3);
        }
    }

    /**
     * The <code>PlayBarIcon</code> class represents the triangle with a
     * vertical bar beside it, drawn onto a playback control button.
     */
    private class PlayBarIcon implements Icon {
        /**
         * Buffered space to hold the three x coordinates of the triangle
         * points.
         */
        private int[] xPoints;

        /**
         * Buffered space to hold the three y coordinates of the triangle
         * points.
         */
        private int[] yPoints;

        /**
         * The height, in pixels, of the triangle and the bar.
         */
        private int height;

        /**
         * The width, in pixels, of the entire icon.
         */
        private int width;

        /**
         * The width, in pixels, of the play triangle.
         */
        private int playWidth;

        /**
         * The width, in pixels, of the vertical bar.
         */
        private int barWidth;

        /**
         * Whether the play triangle should appear to the left of the vertical
         * bar.
         */
        private boolean playOnLeft;

        /**
         * Whether the play triangle should be flipped so that it appears to
         * point left instead of right.
         */
        private boolean playFlipped;

        /**
         * Creates a new <code>PlayBarIcon</code> based on the size of this
         * <code>PlayButton</code>.
         *
         * @param playOnLeft whether the play triangle should appear to the
         *        left of the vertical bar.
         * @param playFlipped whether the play triangle should be flipped so
         *        that it appears to point left instead of right.
         */
        public PlayBarIcon(boolean playOnLeft, boolean playFlipped) {
            this.xPoints = new int[3];
            this.yPoints = new int[3];
            this.height = (int) (TimeButton.this.size * 0.3);
            this.width = (int) (TimeButton.this.size * 0.35);
            this.playWidth = (int) (TimeButton.this.size * 0.25);
            this.barWidth = (int) ((this.width - this.playWidth) * 0.25);
            this.playOnLeft = playOnLeft;
            this.playFlipped = playFlipped;
        }

        /**
         * Returns the total height of the icon in pixels.
         *
         * @return the total height of the icon in pixels.
         */
        public int getIconHeight() {
            return this.height;
        }

        /**
         * Returns the total width of the icon in pixels.
         *
         * @return the total width of the icon in pixels.
         */
        public int getIconWidth() {
            return this.width;
        }

        /**
         * Paints the icon onto the given graphics object at the given x and y
         * offset.
         *
         * @param c the component onto which we are drawing the icon.
         * @param g the graphics object on which to draw the icon.
         * @param x the x offset into <code>g</code> at which to draw the icon.
         * @param y the y offset into <code>g</code> at which to draw the icon.
         */
        public void paintIcon(Component c, Graphics g, int x, int y) {
            /* Determine starting locations */
            int gap = this.width - this.playWidth - this.barWidth;
            int playOffset, barOffset;
            if (this.playOnLeft) {
                playOffset = 0;
                barOffset = this.playWidth + gap;
            } else {
                playOffset = this.barWidth + gap;
                barOffset = 0;
            }

            /* See if the button is big enough */
            if (this.height <= 0 || this.width <= 0 || this.playWidth <= 0
                    || this.barWidth <= 0 || this.height / 2 <= 0)
                return;

            /* Place the play icon polygon points */
            if (this.playFlipped) {
                this.xPoints[0] =
                        this.xPoints[1] = x + playOffset + this.playWidth;
                this.xPoints[2] = x + playOffset;
            } else {
                this.xPoints[0] = this.xPoints[1] = x + playOffset;
                this.xPoints[2] = x + playOffset + this.playWidth;
            }
            this.yPoints[0] = y;
            this.yPoints[1] = y + this.height;
            this.yPoints[2] = y + this.height / 2;

            /* Paint the bar and play icon */
            setupGraphics(g);
            g.fillRect(x + barOffset, y, this.barWidth, this.height);
            g.fillPolygon(this.xPoints, this.yPoints, 3);
        }
    }
}
