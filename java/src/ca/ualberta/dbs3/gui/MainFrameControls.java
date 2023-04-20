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

import ca.ualberta.dbs3.simulations.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * The <code>MainFrameControls</code> class represents the lower portion of a
 * {@link MainFrame}, containing the controls for running a simulation.
 */
public class MainFrameControls extends JPanel {
    /**
     * The size of the buttons in pixels.
     */
    private static final int BUTTON_SIZE = 40;

    /**
     * The size of the border, in pixels, around the control panel area.
     */
    private static final int CONTROL_BORDER = 20;

    /**
     * The size, in pixels, of the space between the text displaying the
     * simulation time and the slider.
     */
    private static final int TEXT_SLIDER_SPACE = 15;

    /**
     * The size, in pixels, of the space between the top half of the control
     * panel containing the slider and text, and the bottom half containing the
     * buttons.
     */
    private static final int SLIDER_BUTTON_SPACE = 10;

    /**
     * The size, in pixels, of the space between two adjacent buttons in the
     * lower half of the control panel.
     */
    private static final int BUTTON_BUTTON_SPACE = 15;

    /**
     * The size, in pixels, of the space between the current time and the speed
     * display.
     */
    private static final int TIME_SPEED_SPACE = 15;

    /**
     * The number of chapters into which to divide playback.
     */
    private static final int NUM_CHAPTERS = 20;

    /**
     * The grace time, in milliseconds, for clicking back to the previous
     * chapter.
     */
    private static final long GRACE_MILLISECONDS = 1500;

    /**
     * The button that returns to the beginning of the simulation.
     */
    private TimeButton beginningButton;

    /**
     * The button that rewinds.
     */
    private TimeButton prevChapterButton;

    /**
     * The button which toggles between the pause and play states.
     */
    private TimeButton playPauseButton;

    /**
     * The button that fast forwards.
     */
    private TimeButton nextChapterButton;

    /**
     * The buttons that skips to the end of the simulation.
     */
    private TimeButton endButton;

    /**
     * The label that displays the current time in the simulation.
     */
    private TimeLabel timeLabel;

    /**
     * The slider that displays the progress of the simulation.
     */
    private TimeSlider slider;

    /**
     * The simulation that is being controlled.
     */
    private SimulationRecorded simulation;

    /**
     * The current time in the playback.
     */
    private long currentTime;

    /**
     * The maximum time in the simulation.
     */
    private long maxTime;

    /**
     * The chapter markers for playback.
     */
    private TimeDivider timeDivider;

    /**
     * The thread responsible for playback.
     */
    private PlayThread playThread;

    /**
     * The thread responsible for updating the time when the user is modifying
     * the time slider.
     */
    private SliderThread sliderThread;

    /**
     * The multiplicative modifier on how fast the simulation should advance.
     */
    private double playbackSpeed;

    /**
     * The image panel that needs to be redrawn as time progresses.
     */
    private MainFrameImage imagePanel;

    /**
     * The speed string to display, or <code>null</code> if there is no string
     * to display.
     */
    private String speedString;

    /**
     * The thread that removes the speed string from the display.
     */
    private StringThread stringThread;

    /**
     * Creates a new <code>MainFrameControls</code> with a playback speed of
     * <code>1.0</code>, and lays out the components.
     *
     * @param mainFrame the main frame to which this control panel belongs,
     *        into which to install the keyboard control bindings.
     * @param imagePanel the image panel that needs to be redrawn as time
     *        changes.
     */
    public MainFrameControls(MainFrame mainFrame, MainFrameImage imagePanel) {
        super();
        this.stringThread = null;
        this.speedString = null;
        this.imagePanel = imagePanel;
        this.layoutElements();
        this.setSimulation(null);
        this.playbackSpeed = 1.0;
        this.installKeyBindings(mainFrame);
    }

    /**
     * Sets the simulation being controlled by the control panel to the given
     * simulation.
     *
     * @param simulation the simulation to be controlled by this panel, or
     *        <code>null</code> to indicate no simulation.
     */
    public void setSimulation(SimulationRecorded simulation) {
        this.simulation = simulation;

        /* Destroy any running threads except the speed string thread */
        if (this.playThread != null) {
            this.playThread.stopPlayThread();
            this.playThread = null;
        }
        if (this.sliderThread != null) {
            this.sliderThread.stopSliderThread();
            this.sliderThread = null;
        }

        /*
         * Remove the speed string from the display if there is no simulation
         */
        if (simulation == null)
            this.speedString = null;

        /* Enable or disable the buttons */
        boolean enable = (simulation != null);
        this.beginningButton.setEnabled(enable);
        this.prevChapterButton.setEnabled(enable);
        this.playPauseButton.setEnabled(enable);
        this.nextChapterButton.setEnabled(enable);
        this.endButton.setEnabled(enable);
        this.timeLabel.setEnabled(enable);
        this.slider.setEnabled(enable);
        this.playPauseButton.setCommand(TimeButton.Command.PLAY);

        /* Reset to time zero */
        this.currentTime = 0;
        if (simulation == null)
            this.maxTime = 1;
        else {
            this.maxTime = simulation.getDuration();
            simulation.setTime(0);
        }
        this.timeLabel.setMaxTime(this.maxTime);
        this.timeLabel.setTime(0);
        this.slider.resetWithMaximum(this.maxTime);
        this.timeDivider =
                new TimeDivider(this.maxTime, MainFrameControls.NUM_CHAPTERS);
        this.repaint();
    }

    /**
     * Terminates all threads that the <code>MainFrameControls</code> may be
     * running.
     */
    public void terminateAllThreads() {
        if (this.playThread != null) {
            this.playThread.stopPlayThread();
            this.playThread = null;
        }
        if (this.sliderThread != null) {
            this.sliderThread.stopSliderThread();
            this.sliderThread = null;
        }
        if (this.stringThread != null) {
            this.stringThread.interrupt();
            this.stringThread = null;
            this.speedString = null;
        }
    }

    /**
     * Sets the number of seconds that progress in the simulation for every
     * real second that progresses to the given value.
     *
     * @param playbackSpeed the multiplicative playback speed.
     * @param description a description of the speed to briefly display, or
     *        <code>null</code> for no display.
     */
    public void setPlaybackSpeed(double playbackSpeed, String description) {
        this.playbackSpeed = playbackSpeed;
        this.speedString = description;
        this.repaint();
        if (this.stringThread != null)
            this.stringThread.interrupt();
        this.stringThread = new StringThread();
        this.stringThread.start();
    }

    /**
     * Paints this panel, along with a speed description overlay.
     *
     * @param g the graphics object on which to paint.
     */
    public void paint(Graphics g) {
        super.paint(g);
        if (this.speedString != null) {
            g.setColor(Color.BLACK);
            g.setFont(this.timeLabel.getFont());
            FontMetrics fm = g.getFontMetrics();
            int ascent = fm.getMaxAscent();
            g.drawString(this.speedString, MainFrameControls.CONTROL_BORDER,
                    MainFrameControls.CONTROL_BORDER
                            + this.timeLabel.getHeight()
                            + MainFrameControls.TIME_SPEED_SPACE + ascent);
        }
    }

    /**
     * A convenience method to set the time of all the elements to the given
     * time.
     *
     * @param milliseconds the new time in milliseconds.
     */
    private void setTime(long milliseconds) {
        this.currentTime = milliseconds;
        this.timeLabel.setTime(milliseconds);
        this.slider.setTime(milliseconds);
        if (this.simulation != null)
            this.simulation.setTime(milliseconds);

        if (milliseconds == this.maxTime && this.sliderThread == null) {
            this.playPauseButton.setCommand(TimeButton.Command.PLAY);
            if (this.playThread != null) {
                this.playThread.stopPlayThread();
                this.playThread = null;
            }
        }

        this.imagePanel.repaint();
    }

    /**
     * Sets the location for all of the elements in the
     * <code>MainFrameControls</code>.
     */
    private void layoutElements() {
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout gridBag = new GridBagLayout();
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        /* The slider and time label */
        JPanel upper = this.layoutUpperElements();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(MainFrameControls.CONTROL_BORDER,
                MainFrameControls.CONTROL_BORDER,
                MainFrameControls.SLIDER_BUTTON_SPACE,
                MainFrameControls.CONTROL_BORDER);
        gridBag.addLayoutComponent(upper, gbc);

        /* The buttons */
        JPanel lower = this.layoutLowerElements();
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, MainFrameControls.CONTROL_BORDER,
                MainFrameControls.CONTROL_BORDER,
                MainFrameControls.CONTROL_BORDER);
        gridBag.addLayoutComponent(lower, gbc);

        this.setLayout(gridBag);
        this.add(upper);
        this.add(lower);
    }

    /**
     * Returns a panel containing all of the upper elements of the control
     * panel: the text and the slider.
     *
     * @return a panel containing the upper half of the control panel.
     */
    private JPanel layoutUpperElements() {
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout gridBag = new GridBagLayout();

        /* The text */
        this.timeLabel = new TimeLabel();
        gbc.fill = GridBagConstraints.NONE;
        gridBag.addLayoutComponent(this.timeLabel, gbc);

        /* The slider */
        this.slider = new TimeSlider();
        this.slider.addMouseListener(new SliderListener());
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, MainFrameControls.TEXT_SLIDER_SPACE, 0, 0);
        gridBag.addLayoutComponent(this.slider, gbc);

        /* Complete the upper panel */
        JPanel topPanel = new JPanel(gridBag);
        topPanel.add(this.timeLabel);
        topPanel.add(this.slider);
        return topPanel;
    }

    /**
     * Returns a panel containing all of the lower elements of the control
     * panel: the five buttons.
     *
     * @return a panel containing the lower half of the control panel.
     */
    private JPanel layoutLowerElements() {
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout gridBag = new GridBagLayout();

        /* Construct the buttons */
        ClickListener cl = new ClickListener();
        this.beginningButton = new TimeButton(TimeButton.Command.BEGINNING,
                MainFrameControls.BUTTON_SIZE);
        this.beginningButton.addTimeButtonListener(cl);
        this.prevChapterButton =
                new TimeButton(TimeButton.Command.PREV_CHAPTER,
                        MainFrameControls.BUTTON_SIZE);
        this.prevChapterButton.addTimeButtonListener(cl);
        this.playPauseButton = new TimeButton(TimeButton.Command.PLAY,
                MainFrameControls.BUTTON_SIZE);
        this.playPauseButton.addTimeButtonListener(cl);
        this.nextChapterButton =
                new TimeButton(TimeButton.Command.NEXT_CHAPTER,
                        MainFrameControls.BUTTON_SIZE);
        this.nextChapterButton.addTimeButtonListener(cl);
        this.endButton = new TimeButton(TimeButton.Command.END,
                MainFrameControls.BUTTON_SIZE);
        this.endButton.addTimeButtonListener(cl);

        /* Shared parameters for all buttons */
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets =
                new Insets(0, 0, 0, MainFrameControls.BUTTON_BUTTON_SPACE);

        /* Add the buttons in order */
        gridBag.addLayoutComponent(this.beginningButton, gbc);
        gbc.gridx = GridBagConstraints.RELATIVE;
        gridBag.addLayoutComponent(this.prevChapterButton, gbc);
        gridBag.addLayoutComponent(this.playPauseButton, gbc);
        gridBag.addLayoutComponent(this.nextChapterButton, gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        gridBag.addLayoutComponent(this.endButton, gbc);

        /* Complete the lower panel */
        JPanel bottom = new JPanel(gridBag);
        bottom.add(this.beginningButton);
        bottom.add(this.prevChapterButton);
        bottom.add(this.playPauseButton);
        bottom.add(this.nextChapterButton);
        bottom.add(this.endButton);
        return bottom;
    }

    /**
     * Installs the keyboard bindings for modifying the time.
     *
     * @param mainFrame the frame into which to install the key bindings.
     */
    private void installKeyBindings(MainFrame mainFrame) {
        JRootPane rootPane = mainFrame.getRootPane();
        InputMap iMap = rootPane.getInputMap();
        ActionMap aMap = rootPane.getActionMap();

        /* Time bindings: 1ms / 10ms / 100ms / 1s */
        for (int time = 1; time <= 1000; time *= 10) {
            int mods;
            switch (time) {
                case 10:
                    mods = InputEvent.SHIFT_DOWN_MASK;
                    break;
                case 100:
                    mods = InputEvent.CTRL_DOWN_MASK;
                    break;
                case 1000:
                    mods = InputEvent.SHIFT_DOWN_MASK
                            | InputEvent.CTRL_DOWN_MASK;
                    break;
                default:
                    mods = 0;
                    break;
            }
            String name = "left-" + time + "-" + mods + "-action";
            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, mods), name);
            aMap.put(name, new TimeAction(-time));
            name = "right-" + time + "-" + mods + "-action";
            iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, mods), name);
            aMap.put(name, new TimeAction(time));
        }


        /* Alt+Left = previous chapter */
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
                InputEvent.ALT_DOWN_MASK), "altLeftAction");
        aMap.put("altLeftAction", new ClickAction(this.prevChapterButton));

        /* Alt+Right = next chapter */
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
                InputEvent.ALT_DOWN_MASK), "altRightAction");
        aMap.put("altRightAction", new ClickAction(this.nextChapterButton));

        /* Meta+Left = beginning */
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
                InputEvent.META_DOWN_MASK), "metaLeftAction");
        aMap.put("metaLeftAction", new ClickAction(this.beginningButton));

        /* Meta+Right = end */
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
                InputEvent.META_DOWN_MASK), "metaRightAction");
        aMap.put("metaRightAction", new ClickAction(this.endButton));

        /* Space = pause / play */
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "spaceAction");
        aMap.put("spaceAction", new ClickAction(this.playPauseButton));
    }

    /**
     * The <code>ClickListener</code> class is responsible for listening for
     * when any of the <code>TimeButton</code>s is clicked and setting the time
     * in the simulation, or starting any play threads.
     */
    private class ClickListener implements TimeButtonListener {
        /**
         * Jump to time zero when the beginning button is clicked.
         *
         * @param button the button that was clicked.
         */
        public void beginning(TimeButton button) {
            if (simulation == null || sliderThread != null)
                return;
            setTime(0);
        }

        /**
         * Jump to the previous chapter when the previous chapter button is
         * clicked.
         *
         * @param button the button that was clicked.
         */
        public void prevChapter(TimeButton button) {
            if (simulation == null || sliderThread != null)
                return;
            long grace = 0L;
            if (playThread != null)
                grace = (long) (MainFrameControls.GRACE_MILLISECONDS
                        * playbackSpeed);
            setTime(timeDivider.prevMarker(currentTime, grace));
        }


        /**
         * Stops any running <code>PlayThread</code>s, so that the simulation
         * no longer automatically advances, when the pause button is clicked.
         *
         * @param button the button that was clicked.
         */
        public void pause(TimeButton button) {
            if (simulation == null || sliderThread != null)
                return;
            if (playThread != null) {
                playThread.stopPlayThread();
                playThread = null;
            }
            button.setCommand(TimeButton.Command.PLAY);
        }

        /**
         * Starts a <code>PlayThread</code> to make the simulation
         * automatically advance, and jumps back to the beginning of the
         * simulation if we were at the very end.
         *
         * @param button the button that was clicked.
         */
        public void play(TimeButton button) {
            if (simulation == null || sliderThread != null)
                return;
            if (playThread != null)
                playThread.stopPlayThread();
            if (currentTime == maxTime)
                setTime(0);
            button.setCommand(TimeButton.Command.PAUSE);
            playThread = new PlayThread();
            playThread.start();
        }

        /**
         * Jump to the next chapter when the next chapter button is clicked.
         *
         * @param button the button that was clicked.
         */
        public void nextChapter(TimeButton button) {
            if (simulation == null || sliderThread != null)
                return;
            setTime(timeDivider.nextMarker(currentTime));
        }

        /**
         * Set the time to the maximal time when the end button is clicked.
         *
         * @param button the button that was clicked.
         */
        public void end(TimeButton button) {
            if (simulation == null || sliderThread != null)
                return;
            setTime(maxTime);
        }
    }

    /**
     * The <code>SliderListener</code> class is responsible for listening to
     * user input on the time slider.
     */
    private class SliderListener extends MouseAdapter {
        /**
         * Starts a new <code>SliderThread</code> to monitor the value of the
         * time slider.
         *
         * @param e the event that caused this method to be called.
         */
        public void mousePressed(MouseEvent e) {
            if (sliderThread != null)
                sliderThread.stopSliderThread();
            sliderThread = new SliderThread();
            sliderThread.start();
        }

        /**
         * Ends a running <code>SliderThread</code> when the mouse is released
         * off the slider.
         *
         * @param e the event that caused this method to be called.
         */
        public void mouseReleased(MouseEvent e) {
            if (sliderThread != null) {
                sliderThread.stopSliderThread();
                sliderThread = null;
            }
            setTime(slider.getTime());
        }
    }

    /**
     * The <code>PlayThread</code> class is responsible for advancing the
     * simulation when the play button is clicked.
     */
    private class PlayThread extends Thread {
        /**
         * The number of milliseconds between placing new events on the event
         * dispatch thread to update the time.
         */
        private static final int PLAY_TIME = 33;

        /**
         * Set when the thread should stop playing.
         */
        private boolean shouldStop;

        /**
         * The last time at which a play thread event triggered a time update.
         */
        private long lastTime;

        /**
         * Creates a new <code>PlayThread</code>.
         */
        public PlayThread() {
            this.shouldStop = false;
        }

        /**
         * Runs the <code>PlayThread</code> until it is told to stop.
         */
        public void run() {
            this.lastTime = System.currentTimeMillis();
            while (true) {
                if (this.shouldStop)
                    return;
                try {
                    Thread.sleep(PlayThread.PLAY_TIME);
                } catch (InterruptedException e) {
                    return;
                }
                Runnable r = new Runnable() {
                    public void run() {
                        playThreadAdvance();
                    }
                };
                SwingUtilities.invokeLater(r);
            }
        }

        /**
         * Instructs the play thread to stop playing.
         */
        public void stopPlayThread() {
            this.shouldStop = true;
            this.interrupt();
        }

        /**
         * Advances the simulation time by the number of milliseconds slept,
         * multiplied by the multiplicative playback speed.
         */
        private void playThreadAdvance() {
            /*
             * Make sure we should still be advancing time. We check if
             * playThread == this to prevent a race condition. If not for this
             * check, a playThreadAdvance() command could have been put on the
             * EDT; but, an earlier command on the EDT could kill the
             * PlayThread and set playThread to null.
             */
            if (this.shouldStop == true || playThread != this)
                return;

            /* Calculate the new time */
            long sysCurr = System.currentTimeMillis();
            long advance =
                    Math.round((sysCurr - this.lastTime) * playbackSpeed);
            this.lastTime = sysCurr;

            /* Don't run if the slider is active */
            if (sliderThread != null)
                return;

            /* Set the new time */
            long newTime = currentTime + advance;
            if (newTime > maxTime)
                setTime(maxTime);
            else
                setTime(newTime);
        }
    }

    /**
     * The <code>SliderThread</code> class is responsible for informing the
     * control panel to read time from the time slider, while the user is
     * manipulating the slider.
     */
    private class SliderThread extends Thread {
        /**
         * The number of milliseconds between placing new events on the event
         * dispatch thread to update the time.
         */
        private static final int SLIDER_TIME = 40;

        /**
         * Set when the thread should stop reading the slider value.
         */
        private boolean shouldStop;

        /**
         * Creates a new <code>SliderThread</code>.
         */
        public SliderThread() {
            this.shouldStop = false;
        }

        /**
         * Runs the <code>SliderThread</code> until it is told to stop,
         * updating the slider value every <code>SLIDER_TIME</code>
         * milliseconds.
         */
        public void run() {
            while (true) {
                if (this.shouldStop)
                    return;
                try {
                    Thread.sleep(SliderThread.SLIDER_TIME);
                } catch (InterruptedException e) {
                    return;
                }
                Runnable r = new Runnable() {
                    public void run() {
                        sliderThreadRead();
                    }
                };
                SwingUtilities.invokeLater(r);
            }
        }

        /**
         * Instructs the slider thread to stop running.
         */
        public void stopSliderThread() {
            this.shouldStop = true;
            this.interrupt();
        }

        /**
         * Sets the current time to the time indicated by the slider.
         */
        public void sliderThreadRead() {
            /*
             * Make sure we should still be setting time to the slider value.
             * See the PlayThread comments about the "!= this" idiom, which is
             * used to prevent a race condition.
             */
            if (this.shouldStop == true || sliderThread != this)
                return;
            long sliderTime = slider.getTime();
            if (sliderTime != currentTime)
                setTime(sliderTime);
        }
    }

    /**
     * The <code>TimeAction</code> class represents the action that changes the
     * current playback time by a set amount based on keyboard input.
     */
    private class TimeAction extends AbstractAction {
        /**
         * The amount by which to modify the time.
         */
        private long amount;

        /**
         * Creates a new <code>TimeAction</code> that affects the playback time
         * by the given amount.
         *
         * @param amount the number of milliseconds to add to the current time.
         */
        public TimeAction(long amount) {
            this.amount = amount;
        }

        /**
         * Modify the time by the set amount.
         *
         * @param e the action that occured, which is ignored.
         */
        public void actionPerformed(ActionEvent e) {
            if (simulation == null || sliderThread != null)
                return;
            long newTime = currentTime + this.amount;
            if (newTime < 0)
                setTime(0);
            else if (newTime > maxTime)
                setTime(maxTime);
            else
                setTime(newTime);
        }
    }

    /**
     * The <code>ClickAction</code> class represents the action that clicks a
     * button when a certain keyboard input is performed.
     */
    private class ClickAction extends AbstractAction {
        /**
         * The button to click.
         */
        private JButton button;

        /**
         * Creates a new <code>ClickAction</code> that clicks the given button.
         *
         * @param button the button to click.
         */
        public ClickAction(JButton button) {
            this.button = button;
        }

        /**
         * Clicks the button.
         *
         * @param e the action that occured, which is ignored.
         */
        public void actionPerformed(ActionEvent e) {
            if (simulation == null || sliderThread != null)
                return;
            if (this.button.isEnabled())
                this.button.doClick();
        }
    }

    /**
     * The <code>StringThread</code> class is responsible for removing the
     * speed description string from the display after a period of time.
     */
    private class StringThread extends Thread {
        /**
         * How long the speed string should be displayed, in milliseconds.
         */
        private static final int STRING_DISPLAY_TIME = 1500;

        /**
         * Waits for the string display time, then removes the string from the
         * display.
         */
        public void run() {
            try {
                Thread.sleep(StringThread.STRING_DISPLAY_TIME);
            } catch (InterruptedException e) {
                return;
            }
            Runnable r = new Runnable() {
                public void run() {
                    removeString();
                }
            };
            SwingUtilities.invokeLater(r);
        }

        /**
         * Removes the string from the display.
         */
        private void removeString() {
            /*
             * See the PlayThread comments about the "!= this" idiom, used to
             * prevent a race condition.
             */
            if (stringThread != this)
                return;
            speedString = null;
            stringThread = null;
            repaint();
        }
    }
}
