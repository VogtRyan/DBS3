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

import ca.ualberta.dbs3.math.Random;
import ca.ualberta.dbs3.simulations.State;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The <code>Preferences</code> class dictates how different elements in a map
 * and simulation are to be displayed to the user.
 */
public class Preferences {
    /**
     * The default agent size.
     */
    private static final int DEFAULT_SIZE = 8;

    /**
     * The default street colour.
     */
    private static final Color DEFAULT_STREET = new Color(255, 0, 0, 50);

    /**
     * The default intersection colour.
     */
    private static final Color DEFAULT_INTERSECTION = new Color(0, 0, 255, 50);

    /**
     * The default destination colour.
     */
    private static final Color DEFAULT_DESTINATION = new Color(0, 255, 0, 50);

    /**
     * The default empty-state agent colour (that is, a state with no textual
     * description).
     */
    private static final Color DEFAULT_EMPTY_STATE = Color.BLACK;

    /**
     * The diameter in pixels of agents on the display.
     */
    private int agentSize;

    /**
     * The colour of the street overlays.
     */
    private Color streetColour;

    /**
     * The colour of the intersection overlays.
     */
    private Color intersectionColour;

    /**
     * The colour of the destinations.
     */
    private Color destinationColour;

    /**
     * The colours for agents in each state.
     */
    private TreeMap<String, Color> colourMap;

    /**
     * The observers to be informed when the preferences are changed.
     */
    private ArrayList<PreferencesObserver> observers;

    /**
     * Creates a <code>Preferences</code> with the default settings.
     */
    public Preferences() {
        this.agentSize = Preferences.DEFAULT_SIZE;
        this.streetColour = Preferences.DEFAULT_STREET;
        this.intersectionColour = Preferences.DEFAULT_INTERSECTION;
        this.destinationColour = Preferences.DEFAULT_DESTINATION;
        this.colourMap = new TreeMap<String, Color>();
        this.observers = new ArrayList<PreferencesObserver>();
    }

    /**
     * Creates a <code>Preferences</code> that is a copy of all of the
     * preferences in the given <code>Preferences</code> object. Observers of
     * the given <code>Preferences</code> will not, however, become observers
     * of the newly created <code>Preferences</code> object.
     *
     * @param other the preferences to deep-copy.
     */
    public Preferences(Preferences other) {
        this.agentSize = other.agentSize;
        this.streetColour = other.streetColour;
        this.intersectionColour = other.intersectionColour;
        this.destinationColour = other.destinationColour;
        this.colourMap = new TreeMap<String, Color>();
        Set<Map.Entry<String, Color>> set = other.colourMap.entrySet();
        for (Map.Entry<String, Color> entry : set)
            this.colourMap.put(entry.getKey(), entry.getValue());
        this.observers = new ArrayList<PreferencesObserver>();
    }

    /**
     * Tests if the given set of preferences is identical to this set.
     *
     * @param obj the other set of preferences.
     * @return <code>true</code> if they are equal, otherwise
     *         <code>false</code>.
     */
    public boolean equals(Object obj) {
        if ((obj == null) || (!(obj instanceof Preferences)))
            return false;
        Preferences other = (Preferences) obj;
        if (this.agentSize != other.agentSize
                || this.streetColour.equals(other.streetColour) == false
                || this.intersectionColour
                        .equals(other.intersectionColour) == false
                || this.destinationColour
                        .equals(other.destinationColour) == false)
            return false;

        Set<Map.Entry<String, Color>> mySet = this.colourMap.entrySet();
        Set<Map.Entry<String, Color>> otherSet = other.colourMap.entrySet();
        if (mySet.size() != otherSet.size())
            return false;

        Iterator<Map.Entry<String, Color>> me = mySet.iterator();
        Iterator<Map.Entry<String, Color>> you = otherSet.iterator();
        while (me.hasNext()) {
            Map.Entry<String, Color> a = me.next();
            Map.Entry<String, Color> b = you.next();
            if (a.getKey().equals(b.getKey()) == false)
                return false;
            if (a.getValue().equals(b.getValue()) == false)
                return false;
        }
        return true;
    }

    /**
     * Returns a hash code for this <code>Preferences</code> object.
     *
     * @return a hash code for this <code>Preferences</code> object, equal to
     *         its agent size.
     */
    public int hashCode() {
        return this.agentSize;
    }

    /**
     * Adds the given observer to the list of observers to be notified when the
     * preferences are modified.
     *
     * @param observer the observer to add, or <code>null</code> to do nothing.
     */
    public void addObserver(PreferencesObserver observer) {
        if (observer == null)
            return;
        if (this.observers.contains(observer) == false)
            this.observers.add(observer);
    }

    /**
     * Removes the given observer from the list of observers to be notified
     * when the preferences are modified, if it is in the list.
     *
     * @param observer to observer to remove, or <code>null</code> to do
     *        nothing.
     */
    public void removeObserver(PreferencesObserver observer) {
        if (observer != null)
            this.observers.remove(observer);
    }

    /**
     * Returns the diameter in pixels of agents on the display in pixels.
     *
     * @return the diameter of agents on the display in pixels.
     */
    public int getAgentSize() {
        return this.agentSize;
    }

    /**
     * Sets the diameter in pixels of agents on the display, and notifies all
     * of the observers.
     *
     * @param diameter the diameter in pixels.
     * @throws IllegalArgumentException if <code>diameter</code> is not
     *         positive.
     */
    public void setAgentSize(int diameter) {
        if (diameter <= 0)
            throw new IllegalArgumentException("Invalid diameter");
        this.agentSize = diameter;

        for (PreferencesObserver observer : this.observers)
            observer.preferencesChanged(this);
    }

    /**
     * Returns the colour of the street overlays to display on a map.
     *
     * @return the colour of the streets.
     */
    public Color getStreetColour() {
        return this.streetColour;
    }

    /**
     * Sets the colour of the streets overlays on the display, and notifies all
     * the observers.
     *
     * @param colour the new colour for the overlay.
     * @throws IllegalArgumentException if <code>colour</code> is
     *         <code>null</code>.
     */
    public void setStreetColour(Color colour) {
        if (colour == null)
            throw new IllegalArgumentException("Null colour");
        this.streetColour = colour;

        for (PreferencesObserver observer : this.observers)
            observer.preferencesChanged(this);
    }

    /**
     * Returns the colour of the intersection overlays to display on a map.
     *
     * @return the colour of the intersections.
     */
    public Color getIntersectionColour() {
        return this.intersectionColour;
    }

    /**
     * Sets the colour of the intersection overlays on the display, and
     * notifies all the observers.
     *
     * @param colour the new colour for the overlay.
     * @throws IllegalArgumentException if <code>colour</code> is
     *         <code>null</code>.
     */
    public void setIntersectionColour(Color colour) {
        if (colour == null)
            throw new IllegalArgumentException("Null colour");
        this.intersectionColour = colour;

        for (PreferencesObserver observer : this.observers)
            observer.preferencesChanged(this);
    }

    /**
     * Returns the colour of destinations on the display.
     *
     * @return the colour of destinations.
     */
    public Color getDestinationColour() {
        return this.destinationColour;
    }

    /**
     * Sets the colour of destinations on the display, and notifies all the
     * observers.
     *
     * @param colour the colour to make destinations.
     * @throws IllegalArgumentException if <code>colour</code> is
     *         <code>null</code>.
     */
    public void setDestinationColour(Color colour) {
        if (colour == null)
            throw new IllegalArgumentException("Null colour");
        this.destinationColour = colour;

        for (PreferencesObserver observer : this.observers)
            observer.preferencesChanged(this);
    }

    /**
     * Returns the colour of agents in the given state, generating a consistent
     * random colour (based on the textual description of the state) if there
     * is no specified colour for that state.
     *
     * @param state the agent state.
     * @return the colour of agents in that state.
     * @throws IllegalArgumentException if <code>state</code> is
     *         <code>null</code>
     */
    public Color getAgentColour(State state) {
        if (state == null)
            throw new IllegalArgumentException("Null state");
        String name = state.getName();
        Color ret = this.colourMap.get(name);
        if (ret != null)
            return ret;
        else
            return this.getDefaultColour(name);
    }

    /**
     * Sets the colour of agents in the empty-named state.
     *
     * @param colour the colour to make the agents.
     * @throws IllegalArgumentException if <code>colour</code> is
     *         <code>null</code>.
     */
    public void setAgentColour(Color colour) {
        this.setAgentColour(new State("", 0), colour);
    }

    /**
     * Sets the colour of agents in a given state.
     *
     * @param state the state for which to set the colour.
     * @param colour the colour to make the agents in that state.
     * @throws IllegalArgumentException if <code>state</code> is
     *         <code>null</code> or if <code>colour</code> is
     *         <code>null</code>.
     */
    public void setAgentColour(State state, Color colour) {
        if (state == null)
            throw new IllegalArgumentException("Null state");
        if (colour == null)
            throw new IllegalArgumentException("Null colour");

        /*
         * Default colours should not be saved into the colour map. This design
         * makes it possible to tell if the user has truly modified the colour
         * map, versus just adding an entry that sets a state to a default
         * colour.
         */
        String name = state.getName();
        if (colour.equals(this.getDefaultColour(name)))
            this.colourMap.remove(name);
        else
            this.colourMap.put(name, colour);

        for (PreferencesObserver observer : this.observers)
            observer.preferencesChanged(this);
    }

    /**
     * Returns the default colour used for a given state, for use if the user
     * has not specified a value.
     *
     * @param state the state for which to find the default colour.
     * @return the default colour for the given state.
     */
    private Color getDefaultColour(String state) {
        if (state.isEmpty())
            return Preferences.DEFAULT_EMPTY_STATE;
        Random prng = new Random((long) (state.hashCode()));
        return new Color(prng.nextInt(256), prng.nextInt(256),
                prng.nextInt(256));
    }

    /**
     * Writes the contents of this <code>Preferences</code> to the given file.
     *
     * @param filename the file to which to write this preference set.
     * @throws IOException if there is an error writing to the file.
     */
    public void writeToFile(String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        DataOutputStream dos = new DataOutputStream(fos);

        /* Write the agent size */
        dos.writeInt(this.agentSize);

        /* Write the street, intersection, and default agent colours */
        this.writeColour(this.streetColour, dos);
        this.writeColour(this.intersectionColour, dos);
        this.writeColour(this.destinationColour, dos);

        /* Write the number of special colours, followed by each definition */
        dos.writeInt(this.colourMap.size());
        Set<Map.Entry<String, Color>> set = this.colourMap.entrySet();
        for (Map.Entry<String, Color> entry : set) {
            this.writeString(entry.getKey(), dos);
            this.writeColour(entry.getValue(), dos);
        }

        dos.close();
    }

    /**
     * Reads the given filename to construct a <code>Preferences</code>.
     *
     * @param filename the file from which to read.
     * @return the constructed <code>Preferences</code>.
     * @throws IOException if there is an error reading the file.
     * @throws PreferencesFileException if there is a file format error.
     */
    public static Preferences readFromFile(String filename)
            throws IOException, PreferencesFileException {
        Preferences dpref = new Preferences();
        FileInputStream fis = new FileInputStream(filename);
        DataInputStream dis = new DataInputStream(fis);

        /* Read the agent size */
        int size = dis.readInt();
        if (size <= 0) {
            dis.close();
            throw new PreferencesFileException("Invalid agent size");
        }
        dpref.setAgentSize(size);

        /* Read the street, intersection, and destination colours */
        dpref.setStreetColour(dpref.readColour(dis));
        dpref.setIntersectionColour(dpref.readColour(dis));
        dpref.setDestinationColour(dpref.readColour(dis));

        /* Read the number of special colours */
        int numColours = dis.readInt();
        if (numColours < 0)
            throw new PreferencesFileException("Invalid number of states");
        for (int i = 0; i < numColours; i++) {
            String name = dpref.readString(dis);
            if (dpref.colourMap.containsKey(name))
                throw new PreferencesFileException("Duplicate state in file");
            Color colour = dpref.readColour(dis);
            dpref.setAgentColour(new State(name, 0), colour);
        }

        dis.close();
        return dpref;
    }

    /**
     * Writes the given string to the data output stream.
     *
     * @param string the string to write.
     * @param dos the output stream to which to write.
     * @throws IOException if a write error occurs.
     */
    private void writeString(String string, DataOutputStream dos)
            throws IOException {
        int len = string.length();
        dos.writeInt(len);
        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);
            dos.writeChar(c);
        }
    }

    /**
     * Reads a string from the data input stream.
     *
     * @param dis the input stream from which to read.
     * @return the string read from the input stream.
     * @throws IOException if a read error occurs.
     * @throws PreferencesFileException if the string length is not positive.
     */
    private String readString(DataInputStream dis)
            throws IOException, PreferencesFileException {
        int len = dis.readInt();
        if (len < 0)
            throw new PreferencesFileException("Invalid string length");
        String str = "";
        for (int i = 0; i < len; i++)
            str = str + dis.readChar();
        return str;
    }

    /**
     * Writes the given colour to the data output stream.
     *
     * @param colour the colour to write.
     * @param dos the output stream to which to write.
     * @throws IOException if a write error occurs.
     */
    private void writeColour(Color colour, DataOutputStream dos)
            throws IOException {
        dos.writeInt(colour.getRed());
        dos.writeInt(colour.getGreen());
        dos.writeInt(colour.getBlue());
        dos.writeInt(colour.getAlpha());
    }

    /**
     * Reads a colour from the data input stream.
     *
     * @param dis the input stream from which to read.
     * @return the colour read from the input stream.
     * @throws IOException if a read error occurs.
     * @throws PreferencesFileException if the values do not constitute a valid
     *         RGB colour.
     */
    private Color readColour(DataInputStream dis)
            throws IOException, PreferencesFileException {
        int r = dis.readInt();
        int g = dis.readInt();
        int b = dis.readInt();
        int a = dis.readInt();
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255 || a < 0
                || a > 255)
            throw new PreferencesFileException("Invalid colour specification");
        return new Color(r, g, b, a);
    }
}
