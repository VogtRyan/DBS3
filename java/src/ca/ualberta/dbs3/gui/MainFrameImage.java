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

import ca.ualberta.dbs3.cartography.*;
import ca.ualberta.dbs3.simulations.*;
import java.awt.*;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.*;

/**
 * The <code>MainFrameImage</code> class represents a panel that contains the
 * map view image and any overlays drawn on it, as part of the
 * {@link MainFrame}.
 */
public class MainFrameImage extends JPanel {
    /**
     * The map being displayed.
     */
    private Map map;

    /**
     * The map image being displayed.
     */
    private BufferedImage image;

    /**
     * The scaling factor for this image view.
     */
    private double metresToPixels;

    /**
     * The simulation overlayed on the map.
     */
    private SimulationRecorded simulation;

    /**
     * The colour for street overlays.
     */
    private Color streetColour;

    /**
     * The colour for intersection overlays.
     */
    private Color intersectionColour;

    /**
     * The colour for destination dots.
     */
    private Color destinationColour;

    /**
     * The color for each agent state in the simulation.
     */
    private Color[] agentColours;

    /**
     * The width and height of agents, to be passed to
     * {@link Graphics#drawOval}.
     */
    private int ovalSize;

    /**
     * The overlays for showing the streets.
     */
    private Overlay[] streetOverlays;

    /**
     * The overlays for showing the intersections.
     */
    private Overlay[] intersectionOverlays;

    /**
     * Whether or not the underlying map should be rendered.
     */
    private boolean drawMap;

    /**
     * Whether or not the streets should be rendered.
     */
    private boolean drawStreets;

    /**
     * Whether or not the intersections should be rendered.
     */
    private boolean drawIntersections;

    /**
     * Whether or not the agents should be rendered.
     */
    private boolean drawAgents;

    /**
     * Whether or not agent destinations should be rendered.
     */
    private boolean drawDestinations;

    /**
     * The display preferences used by this image frame to determine colours
     * and sizes.
     */
    private Preferences prefs;

    /**
     * Creates a new <code>MainFrameImage</code> using the given
     * <code>MapView</code> as the first view it displays.
     *
     * @param map the map being displayed
     * @param defaultView the initial <code>MapView</code> to display in this
     *        panel.
     * @param preferences the display preferences that determine colours and
     *        sizes.
     */
    public MainFrameImage(Map map, MapView defaultView,
            Preferences preferences) {
        super();
        this.map = map;
        this.simulation = null;
        this.drawMap = this.drawAgents = true;
        this.drawStreets = this.drawIntersections = false;
        this.prefs = preferences;
        this.computeOverlays();
        this.setOpaque(true);
        this.setDoubleBuffered(true);
        this.setMapView(defaultView);
        this.rereadPreferences();
    }

    /**
     * Sets this panel to display the given <code>MapView</code> and sets the
     * preferred size of this panel equal to the size of the image.
     *
     * @param view the view to display.
     */
    public void setMapView(MapView view) {
        /* Cache the image */
        this.image = view.getImage();
        this.metresToPixels = view.getMetresToPixels();

        /* Set the new minimum size for the panel and repaint */
        Dimension min =
                new Dimension(this.image.getWidth(), this.image.getHeight());
        this.setPreferredSize(min);
        this.setMinimumSize(min);
        this.repaint();
    }

    /**
     * Sets the simulation being displayed on this panel to the given
     * simulation.
     *
     * @param simulation the simulation to display.
     */
    public void setSimulation(SimulationRecorded simulation) {
        this.simulation = simulation;
        this.rereadPreferences();
        this.repaint();
    }

    /**
     * Instructs the image display to re-read the display preferences given to
     * its constructor.
     */
    public void rereadPreferences() {
        this.streetColour = this.prefs.getStreetColour();
        this.intersectionColour = this.prefs.getIntersectionColour();
        this.destinationColour = this.prefs.getDestinationColour();

        /* Read in the state colours */
        if (this.simulation == null)
            this.agentColours = new Color[0];
        else {
            StateRecord sr = this.simulation.getStateRecord();
            this.agentColours = new Color[sr.getNumStates()];
            for (int i = 0; i < this.agentColours.length; i++) {
                State si = sr.getState(i);
                this.agentColours[i] = this.prefs.getAgentColour(si);
            }
        }

        /*
         * Preferences returns the desired diameter in pixels of the agents.
         * Graphics.drawOval draws an oval that covers width+1 and height+1
         * pixels, where width and height are its parameters. Hence the need to
         * subtract 1.
         */
        this.ovalSize = this.prefs.getAgentSize() - 1;
        this.repaint();
    }

    /**
     * Sets whether or not the underlying map should be drawn.
     *
     * @param draw whether the underlying map should be drawn.
     */
    public void setDrawMap(boolean draw) {
        this.drawMap = draw;
        this.repaint();
    }

    /**
     * Returns whether or not the underlying map should be drawn.
     *
     * @return <code>true</code> if the underlying map should be drawn,
     *         otherwise <code>false</code>.
     */
    public boolean getDrawMap() {
        return this.drawMap;
    }

    /**
     * Sets whether or not the street overlays should be drawn.
     *
     * @param draw whether the street overlays should be drawn.
     */
    public void setDrawStreets(boolean draw) {
        this.drawStreets = draw;
        this.repaint();
    }

    /**
     * Returns whether or not the street overlays should be drawn.
     *
     * @return <code>true</code> if the street overlays should be drawn,
     *         otherwise <code>false</code>.
     */
    public boolean getDrawStreets() {
        return this.drawStreets;
    }

    /**
     * Sets whether or not the intersection overlays should be drawn.
     *
     * @param draw whether the intersection overlays should be drawn.
     */
    public void setDrawIntersections(boolean draw) {
        this.drawIntersections = draw;
        this.repaint();
    }

    /**
     * Returns whether or not the intersection overlays should be drawn.
     *
     * @return <code>true</code> if the intersection overlays should be drawn,
     *         otherwise <code>false</code>.
     */
    public boolean getDrawIntersections() {
        return this.drawIntersections;
    }

    /**
     * Sets whether or not the destination dots should be drawn.
     *
     * @param draw whether the destination dots should be drawn.
     */
    public void setDrawDestinations(boolean draw) {
        this.drawDestinations = draw;
        this.repaint();
    }

    /**
     * Returns whether or not the destination dots should be drawn.
     *
     * @return <code>true</code> if the destination dots should be drawn,
     *         otherwise <code>false</code>.
     */
    public boolean getDrawDestinations() {
        return this.drawDestinations;
    }

    /**
     * Sets whether or not the agents should be drawn.
     *
     * @param draw whether the agents should be drawn.
     */
    public void setDrawAgents(boolean draw) {
        this.drawAgents = draw;
        this.repaint();
    }

    /**
     * Returns whether or not the agents should be drawn.
     *
     * @return <code>true</code> if the agents should be drawn, otherwise
     *         <code>false</code>.
     */
    public boolean getDrawAgents() {
        return this.drawAgents;
    }

    /**
     * Invoked by Swing to draw components. Applications should not invoke
     * <code>paint</code> directly, but should instead use the
     * <code>repaint</code> method to schedule the component for redrawing.
     *
     * @param g the <code>Graphics</code> context in which to paint.
     */
    public void paint(Graphics g) {
        /* Antialias the image */
        if (g instanceof Graphics2D
                && ApplicationGUI.getApplication().useAA()) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }

        /*
         * Compute the scale of the image. The ovalOffset is necessary to make
         * the centre of the oval reflect the agent position. The fillOval
         * method draws the top-left corner of the oval at the given
         * coordinates.
         */
        Dimension d = this.getSize();
        double xScale = ((double) (d.width)) / this.image.getWidth()
                * this.metresToPixels;
        double yScale = ((double) (d.height)) / this.image.getHeight()
                * this.metresToPixels;
        double ovalOffset = ((double) this.ovalSize) / 2.0;

        /* Draw the image view first */
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, d.width, d.height);
        if (this.drawMap)
            g.drawImage(image, 0, 0, d.width, d.height, null);

        /* Draw the overlays */
        if (this.drawStreets) {
            g.setColor(this.streetColour);
            for (int i = 0; i < this.streetOverlays.length; i++)
                this.streetOverlays[i].draw(xScale, yScale, g);
        }
        if (this.drawIntersections) {
            g.setColor(this.intersectionColour);
            for (int i = 0; i < this.intersectionOverlays.length; i++)
                this.intersectionOverlays[i].draw(xScale, yScale, g);
        }

        /* Draw the agent destinations */
        if ((this.simulation != null) && this.drawDestinations) {
            int numAgents = this.simulation.getNumAgents();
            g.setColor(this.destinationColour);
            for (int onAgent = 0; onAgent < numAgents; onAgent++) {
                ca.ualberta.dbs3.math.Point p =
                        this.simulation.getDestination(onAgent);
                int x = (int) Math.round(p.getX() * xScale - ovalOffset);
                int y = (int) Math.round(p.getY() * yScale - ovalOffset);
                g.fillOval(x, y, this.ovalSize, this.ovalSize);
            }
        }

        /* Finally, draw the agents */
        if ((this.simulation != null) && this.drawAgents) {
            int numAgents = this.simulation.getNumAgents();
            for (int onAgent = 0; onAgent < numAgents; onAgent++) {
                ca.ualberta.dbs3.math.Point p =
                        this.simulation.getLocation(onAgent);
                int x = (int) Math.round(p.getX() * xScale - ovalOffset);
                int y = (int) Math.round(p.getY() * yScale - ovalOffset);
                int stateIndex = this.simulation.getState(onAgent).getIndex();
                g.setColor(this.agentColours[stateIndex]);
                g.fillOval(x, y, this.ovalSize, this.ovalSize);
            }
        }
    }

    /**
     * Precomputes data for drawing the street and intersection overlays.
     */
    private void computeOverlays() {
        this.streetOverlays = new Overlay[this.map.numStreets()];
        for (int i = 0; i < this.streetOverlays.length; i++) {
            Street str = this.map.getStreet(i);
            this.streetOverlays[i] = new Overlay(str.getCornerPoints());
        }

        List<Intersection> list = new ArrayList<Intersection>();
        Iterator<Intersection> it = this.map.getUniqueIntersections();
        while (it.hasNext())
            list.add(it.next());
        this.intersectionOverlays = new Overlay[list.size()];
        for (int i = 0; i < this.intersectionOverlays.length; i++)
            this.intersectionOverlays[i] =
                    new Overlay(list.get(i).getCornerPoints());
    }
}
