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

import ca.ualberta.dbs3.commandLine.Application;
import ca.ualberta.dbs3.cartography.*;
import ca.ualberta.dbs3.simulations.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import javax.imageio.*;
import javax.swing.*;

/**
 * The <code>MainFrame</code> class represents a single window in the DBS3 GUI
 * that contains a map-and-simulation display with controls.
 */
public class MainFrame extends DialogOwner {
    /**
     * The various simulation speeds available.
     */
    private static final double[] SPEEDS =
            {0.2, 0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0, 200.0, 500.0};

    /**
     * The labels that show the user the available speeds.
     */
    private static final String[] SPEED_LABELS = {"x 1/5", "x 1/2", "x 1",
            "x 2", "x 5", "x 10", "x 20", "x 50", "x 100", "x 200", "x 500"};

    /**
     * The default speed at which to play back simulations.
     */
    private static final int DEFAULT_SPEED = 2; /* Index 2 is 1x speed */

    /**
     * The index of the separator between the overlay colour and the agent
     * colours in the colour menu.
     */
    private static final int COLOUR_SEPARATOR_INDEX = 3;

    /**
     * Unused serialization version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The DBS3 map file opened in this window.
     */
    private String filename;

    /**
     * The display preferences used by this frame.
     */
    private Preferences prefs;

    /**
     * The map that will be displayed, and on which simulations will be run.
     */
    private Map map;

    /**
     * All of the views and associated images defined in the map file.
     */
    private MapView[] views;

    /**
     * The upper panel of the display, containing the image and the overlays.
     */
    private MainFrameImage imagePanel;

    /**
     * The lower panel of the display, containing the simulation playback
     * controls.
     */
    private MainFrameControls controlPanel;

    /**
     * The simulation configuration window.
     */
    private SimulationDialog simConf;

    /**
     * The specification that will be used to create the simulation dialog.
     */
    private SimulationSpecification spec;

    /**
     * The agent size configuration window.
     */
    private SizeDialog sizeConf;

    /**
     * The menu item to end the currently running simulation.
     */
    private JMenuItem endSimulationItem;

    /**
     * The menu containing the playback speeds.
     */
    private MenuDecorated speedMenu;

    /**
     * The menu item group containing all of the speeds.
     */
    private MenuItemGroup speedGroup;

    /**
     * The menu item for whether or not to view the map.
     */
    private JMenuItem viewMap;

    /**
     * The menu item for whether or not to view the streets.
     */
    private JMenuItem viewStreets;

    /**
     * The menu item for whether or not to view the intersections.
     */
    private JMenuItem viewIntersections;

    /**
     * The menu item for whether or not to view the agents.
     */
    private JMenuItem viewAgents;

    /**
     * The menu item for whether or not to view the destinations.
     */
    private JMenuItem viewDestinations;

    /**
     * The menu item for agent size.
     */
    private JMenuItem viewAgentSize;

    /**
     * The colour menu.
     */
    private JMenu colourMenu;

    /**
     * The street overlay colour menu item.
     */
    private ColourMenuItem streetColour;

    /**
     * The intersection overlay colour menu item.
     */
    private ColourMenuItem intersectionColour;

    /**
     * The destination overlay colour menu item.
     */
    private ColourMenuItem destinationColour;

    /**
     * The agent colour items in the colour menu, being either a single colour
     * for UAMP simulations, or different colours for different states in MVISP
     * simulations.
     */
    private ArrayList<ColourMenuItem> agentColours;

    /**
     * Creates a new <code>MainFrame</code> that will display the given map.
     *
     * @param mapFile the filename of the map file.
     * @param prefs the preferences set to use, including items such as agent
     *        colours and sizes.
     * @throws MapFileException if the map file or any of the view images
     *         cannot be read, or if there is a format error.
     */
    public MainFrame(String mapFile, Preferences prefs)
            throws MapFileException {
        /* Load the map file */
        super("DBS3: " + new File(mapFile).getName());
        this.filename = mapFile;
        this.prefs = prefs;
        this.spec = new SimulationSpecification();
        this.loadMapFile(mapFile);

        /* Layout all of the elements and menus and set their listeners */
        this.layoutElements();
        this.addMenusAndListeners();
        this.installKeyBindings();
        this.simConf = null;
        this.sizeConf = null;

        /* Realize all GUI elements and set the screen size */
        this.pack();
        this.setMinimumSize(this.getSize());
    }

    /**
     * Returns the filename of the map displayed by this
     * <code>MainFrame</code>.
     *
     * @return the filename of the map displayed.
     */
    public String getMapFilename() {
        return this.filename;
    }

    /**
     * Sets the simulation being displayed on this frame to the given
     * simulation, or to no simulation if <code>null</code> is provided.
     *
     * @param simulation the simulation to display, or <code>null</code>.
     * @param spec the specification used to construct the simulation, or
     *        <code>null</code> if none was used.
     */
    public void setSimulation(SimulationRecorded simulation,
            SimulationSpecification spec) {
        /* Set the simulation in the panels */
        this.imagePanel.setSimulation(simulation);
        this.controlPanel.setSimulation(simulation);

        /*
         * If a specification was used, remember it for future instances of the
         * simulation configuration dialog.
         */
        if (spec != null)
            this.spec = spec;

        /* Enable or disable menu items */
        boolean enable = (simulation != null);
        this.endSimulationItem.setEnabled(enable);
        this.viewAgents.setEnabled(enable);
        this.viewDestinations.setEnabled(enable);
        this.speedMenu.setEnabled(enable);
        this.viewAgentSize.setEnabled(enable);

        /*
         * Finally, update the contents of the colour menu. There should only
         * be a separator present when there is a simulation running.
         */
        if (enable && this.agentColours.isEmpty())
            this.colourMenu.addSeparator();
        else if (enable == false && this.agentColours.isEmpty() == false)
            this.colourMenu.remove(MainFrame.COLOUR_SEPARATOR_INDEX);
        for (ColourMenuItem item : this.agentColours)
            this.colourMenu.remove(item);
        this.agentColours.clear();
        if (enable) {
            StateRecord record = simulation.getStateRecord();
            int numStates = record.getNumStates();
            for (int i = 0; i < numStates; i++) {
                ColourMenuItem item = new StateColourItem(record.getState(i));
                this.colourMenu.add(item);
                this.agentColours.add(item);
            }
        }
    }

    /**
     * Instructs the frame to re-read its display preferences object.
     */
    public void rereadPreferences() {
        this.imagePanel.rereadPreferences();
        this.streetColour.rereadPreferences();
        this.intersectionColour.rereadPreferences();
        this.destinationColour.rereadPreferences();
        for (ColourMenuItem item : this.agentColours)
            item.rereadPreferences();
    }

    /**
     * Terminates all running threads in things owned by this main frame and
     * disposes this frame.
     */
    public void terminateAndDispose() {
        if (this.simConf != null)
            this.simConf.terminateAllThreads();
        this.controlPanel.terminateAllThreads();
        this.dispose();
    }

    /**
     * Loads the given map file and all of its associated views.
     *
     * @param mapFile the map filename to load.
     * @throws MapFileException if the map file or any of the view images
     *         cannot be read, or if there is a format error.
     */
    private void loadMapFile(String mapFile) throws MapFileException {
        /* Load the map file, possibly throwing an exception */
        this.map = new Map(mapFile);

        /* Create all of the map views */
        int numViews = this.map.numViewSpecifications();
        if (numViews == 0)
            throw new MapFileException(mapFile,
                    "Map file does not define any views.");
        this.views = new MapView[numViews];
        for (int i = 0; i < numViews; i++) {
            ViewSpecification vs = this.map.getViewSpecification(i);
            try {
                this.views[i] = new MapView(vs);
            } catch (IOException ioe) {
                throw new MapFileException(mapFile,
                        "Cannot load view image: " + vs.getFilename());
            }

            /* Ensure no duplicate view names */
            String viewName = this.views[i].getName();
            for (int j = 0; j < i; j++) {
                if (this.views[j].getName().equals(viewName))
                    throw new MapFileException(mapFile,
                            "Duplicate view name: " + viewName);
            }
        }
    }

    /**
     * Lays out the image and control panels to appear in the main window.
     */
    private void layoutElements() {
        /* Grid bag constants */
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;

        /* The image panel consumes all extra room */
        this.imagePanel =
                new MainFrameImage(this.map, this.views[0], this.prefs);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gridBag.addLayoutComponent(this.imagePanel, gbc);

        /* The control panel is anchored at the bottom and does not grow up */
        this.controlPanel = new MainFrameControls(this, this.imagePanel);
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTH;
        gridBag.addLayoutComponent(this.controlPanel, gbc);

        /* Add the elements */
        this.setLayout(gridBag);
        this.add(this.imagePanel);
        this.add(this.controlPanel);
    }

    /**
     * Adds the menus to the main window.
     */
    private void addMenusAndListeners() {
        JMenuBar menuBar = new JMenuBar();
        MenuDecorated menu;
        CloseListener closeListener = new CloseListener();
        this.addWindowListener(closeListener);

        /* File menu */
        menu = new MenuDecorated("File", KeyEvent.VK_F);
        menu.add("Open...", KeyEvent.VK_O, KeyEvent.VK_O, new OpenListener());
        menu.add("Save image...", KeyEvent.VK_S, KeyEvent.VK_S,
                new CaptureListener());
        menu.add("Close", KeyEvent.VK_C, KeyEvent.VK_W, closeListener);
        if (ApplicationGUI.quitProvided() == false)
            menu.add("Quit", KeyEvent.VK_Q, KeyEvent.VK_Q, new QuitListener());
        menuBar.add(menu);

        /* Simulation menu */
        menu = new MenuDecorated("Simulation", KeyEvent.VK_S);
        menu.add("New simulation...", KeyEvent.VK_N, KeyEvent.VK_N,
                new BeginSimulationListener());
        this.endSimulationItem = menu.add("End simulation", KeyEvent.VK_E,
                new EndSimulationListener());
        this.endSimulationItem.setEnabled(false);
        menu.addSeparator();
        this.speedMenu = new MenuDecorated("Playback speed");
        this.speedGroup = this.speedMenu.add(MainFrame.SPEED_LABELS,
                MainFrame.DEFAULT_SPEED, new SpeedListener());
        menu.add(this.speedMenu);
        this.speedMenu.setEnabled(false);
        menuBar.add(menu);

        /* View menu: map view images */
        menu = new MenuDecorated("View", KeyEvent.VK_V);
        String[] viewNames = new String[this.views.length];
        for (int i = 0; i < this.views.length; i++)
            viewNames[i] = this.views[i].getName();
        menu.add(viewNames, 0, new MapViewListener());

        /* View menu: overlay and size options */
        menu.addSeparator();
        DisplayListener dl = new DisplayListener();
        this.viewMap = menu.add("Show map", KeyEvent.VK_M,
                this.imagePanel.getDrawMap(), dl);
        this.viewStreets = menu.add("Show street overlays", KeyEvent.VK_T,
                this.imagePanel.getDrawStreets(), dl);
        this.viewIntersections = menu.add("Show intersection overlays",
                KeyEvent.VK_I, this.imagePanel.getDrawIntersections(), dl);
        this.viewAgents = menu.add("Show agents", KeyEvent.VK_A,
                this.imagePanel.getDrawAgents(), dl);
        this.viewAgents.setEnabled(false);
        this.viewDestinations = menu.add("Show destinations", KeyEvent.VK_D,
                this.imagePanel.getDrawDestinations(), dl);
        this.viewDestinations.setEnabled(false);
        menu.addSeparator();
        this.viewAgentSize = menu.add("Agent size...", KeyEvent.VK_A,
                new AgentSizeListener());
        this.viewAgentSize.setEnabled(false);
        menuBar.add(menu);

        /* Colour menu */
        this.colourMenu = new MenuDecorated("Colour", KeyEvent.VK_C);
        this.streetColour = new StreetColourItem();
        this.intersectionColour = new IntersectionColourItem();
        this.destinationColour = new DestinationColourItem();
        this.agentColours = new ArrayList<ColourMenuItem>();
        this.colourMenu.add(this.streetColour);
        this.colourMenu.add(this.intersectionColour);
        this.colourMenu.add(this.destinationColour);
        menuBar.add(this.colourMenu);

        /* Set the menu bar for the window */
        this.setJMenuBar(menuBar);
    }

    /**
     * Adds keyboard bindings to change the speed of playback.
     */
    private void installKeyBindings() {
        JRootPane rootPane = this.getRootPane();
        InputMap iMap = rootPane
                .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap aMap = rootPane.getActionMap();

        /* Minus = decrement speed */
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "minusAction");
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
                InputEvent.SHIFT_DOWN_MASK), "minusAction");
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0),
                "minusAction");
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT,
                InputEvent.SHIFT_DOWN_MASK), "minusAction");
        aMap.put("minusAction", new GroupIndexAction(this.speedGroup, -1));

        /* Plus = increment speed */
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), "plusAction");
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,
                InputEvent.SHIFT_DOWN_MASK), "plusAction");
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0), "plusAction");
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS,
                InputEvent.SHIFT_DOWN_MASK), "plusAction");
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), "plusAction");
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD,
                InputEvent.SHIFT_DOWN_MASK), "plusAction");
        aMap.put("plusAction", new GroupIndexAction(this.speedGroup, 1));
    }

    /**
     * The <code>OpenListener</code> class is responsible for showing a dialog
     * that allows the user to open a map file, when the user selects that menu
     * item.
     */
    private class OpenListener implements ActionListener {
        /**
         * Shows the open dialog box.
         *
         * @param e the event that caused this method to be called, which is
         *        ignored.
         */
        public void actionPerformed(ActionEvent e) {
            DBS3.getApplication().openFile(MainFrame.this);
        }
    }

    /**
     * The <code>CloseListener</code> class is responsible for closing this
     * frame and terminating all running threads when the user selects that
     * menu item or when the user closes this window from the windowing system.
     */
    private class CloseListener extends WindowAdapter
            implements ActionListener {
        /**
         * Closes this frame and terminates all running threads.
         *
         * @param e the event that caused this method to be called, which is
         *        ignored.
         */
        public void actionPerformed(ActionEvent e) {
            terminateAndDispose();
        }

        /**
         * Closes this frame and terminates all running threads.
         *
         * @param e the window event that caused this method to be called,
         *        which is ignored.
         */
        public void windowClosing(WindowEvent e) {
            terminateAndDispose();
        }
    }

    /**
     * The <code>CaptureListener</code> class is responsible for listening for
     * a command to capture a screenshot from DBS3. It will open a dialog box
     * asking for the location to which to save the screenshot.
     */
    private class CaptureListener implements ActionListener {
        /**
         * Takes the screenshot and opens a dialog box to determine where to
         * save it.
         *
         * @param e the event that caused this method to be called.
         */
        public void actionPerformed(ActionEvent e) {
            /* Take the screenshot */
            Component target = imagePanel;
            BufferedImage bi = new BufferedImage(target.getWidth(),
                    target.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
            target.paint(bi.getGraphics());

            /* Show the save-file dialog */
            FileSaver fs = new FileSaver("DBS3", "png", "PNG Images");
            File saveFile = fs.getFile(MainFrame.this);
            if (saveFile == null)
                return;

            /* Save the file */
            try {
                if (ImageIO.write(bi, "png", saveFile) == false)
                    throw new IOException("No image writer available");
            } catch (IOException ioe) {
                String error = "Cannot save captured image: "
                        + Application.throwableToString(ioe, false);
                ApplicationGUI.getApplication().displayError(error,
                        MainFrame.this);
            }
        }
    }

    /**
     * The <code>QuitListener</code> class is responsible for quitting the
     * application when the quit menu item is chosen, on operating systems that
     * do not automatically provide a "Quit" menu item.
     */
    private class QuitListener implements ActionListener {
        /**
         * Quits the application.
         *
         * @param e the event that caused this method to be called, which is
         *        ignored.
         */
        public void actionPerformed(ActionEvent e) {
            ApplicationGUI.getApplication().quit();
        }
    }

    /**
     * The <code>BeginSimulationListener</code> class is responsible for
     * showing the simulation configuration dialog when the user selects that
     * menu item.
     */
    private class BeginSimulationListener implements ActionListener {
        /**
         * Makes the configuration dialog appear.
         *
         * @param e the event that caused this method to be called, which is
         *        ignored.
         */
        public void actionPerformed(ActionEvent e) {
            if (simConf == null || simConf.isVisible() == false)
                simConf = new SimulationDialog(MainFrame.this, map, spec);
            displayDialog(simConf);
        }
    }

    /**
     * The <code>EndSimulationListener</code> class is responsible for ending
     * the display of a simulation when the user selects that menu item.
     */
    private class EndSimulationListener implements ActionListener {
        /**
         * Sets the current simulation to <code>null</code>.
         *
         * @param e the event that caused this method to be called, which is
         *        ignored.
         */
        public void actionPerformed(ActionEvent e) {
            setSimulation(null, null);
        }
    }

    /**
     * The <code>SpeedListener</code> class is responsible for listening to
     * when the user changes their choice of playback speed.
     */
    private class SpeedListener implements MenuItemGroupListener {
        /**
         * Sets the current playback speed to the chosen speed.
         *
         * @param text the text of the menu item that was selected.
         * @param index the index of the element that was selected.
         */
        public void groupItemSelected(String text, int index) {
            controlPanel.setPlaybackSpeed(MainFrame.SPEEDS[index], text);
        }
    }

    /**
     * The <code>MapViewListener</code> class is responsible for listening to
     * when the user changes which {@link MapView} they would like displayed.
     */
    private class MapViewListener implements MenuItemGroupListener {
        /**
         * Sets the currently displayed map view to the one at the given index,
         * possibly resizing this frame.
         *
         * @param text the text of the menu item that was selected.
         * @param index the index of the element that was selected.
         */
        public void groupItemSelected(String text, int index) {
            /* Set the map view, and record the change in size */
            Dimension oldSize = imagePanel.getPreferredSize();
            imagePanel.setMapView(views[index]);
            Dimension newSize = imagePanel.getPreferredSize();
            double deltaHeight = newSize.getHeight() - oldSize.getHeight();
            double deltaWidth = newSize.getWidth() - oldSize.getWidth();

            /* Possibly resize the main window */
            Dimension minSize = getMinimumSize();
            minSize.setSize(minSize.getWidth() + deltaWidth,
                    minSize.getHeight() + deltaHeight);
            setMinimumSize(minSize);
        }
    }

    /**
     * The <code>DisplayListener</code> class is responsible for listening to
     * when the user selects options for what elements to display in the image
     * panel.
     */
    private class DisplayListener implements ActionListener {
        /**
         * Called when the user changes a display option.
         *
         * @param e the event that caused this thread to be called.
         */
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source == viewMap)
                imagePanel.setDrawMap(viewMap.isSelected());
            else if (source == viewStreets)
                imagePanel.setDrawStreets(viewStreets.isSelected());
            else if (source == viewIntersections)
                imagePanel
                        .setDrawIntersections(viewIntersections.isSelected());
            else if (source == viewAgents)
                imagePanel.setDrawAgents(viewAgents.isSelected());
            else if (source == viewDestinations)
                imagePanel.setDrawDestinations(viewDestinations.isSelected());
        }
    }

    /**
     * The <code>StreetColourItem</code> class is responsible for displaying a
     * menu item and dialog for changing the street colour.
     */
    private class StreetColourItem extends ColourMenuItem {
        /**
         * Unused serialization version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Creates a new <code>StreetColourItem</code>.
         */
        public StreetColourItem() {
            super(MainFrame.this, "Street overlays",
                    "Set Street Overlay Colour", prefs.getStreetColour());
        }

        /**
         * Returns the current colour of the preference being controlled.
         *
         * @return the current colour.
         */
        protected Color getCurrentColour() {
            return prefs.getStreetColour();
        }

        /**
         * Called when the user sets a new colour for the preference via the
         * dialog.
         *
         * @param colour the new colour.
         */
        protected void userSetColour(Color colour) {
            prefs.setStreetColour(colour);
        }
    }

    /**
     * The <code>IntersectionColourItem</code> class is responsible for
     * displaying a menu item and dialog for changing the intersection colour.
     */
    private class IntersectionColourItem extends ColourMenuItem {
        /**
         * Unused serialization version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Creates a new <code>IntersectionColourItem</code>.
         */
        public IntersectionColourItem() {
            super(MainFrame.this, "Intersection overlays",
                    "Set Intersection Overlay Colour",
                    prefs.getIntersectionColour());
        }

        /**
         * Returns the current colour of the preference being controlled.
         *
         * @return the current colour.
         */
        protected Color getCurrentColour() {
            return prefs.getIntersectionColour();
        }

        /**
         * Called when the user sets a new colour for the preference via the
         * dialog.
         *
         * @param colour the new colour.
         */
        protected void userSetColour(Color colour) {
            prefs.setIntersectionColour(colour);
        }
    }

    /**
     * The <code>DestinationColourItem</code> class is responsible for
     * displaying a menu item and dialog for changing the destination colour.
     */
    private class DestinationColourItem extends ColourMenuItem {
        /**
         * Unused serialization version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Creates a new <code>DestinationColourItem</code>.
         */
        public DestinationColourItem() {
            super(MainFrame.this, "Destination overlays",
                    "Set Destination Overlay Colour",
                    prefs.getDestinationColour());
        }

        /**
         * Returns the current colour of the preference being controlled.
         *
         * @return the current colour.
         */
        protected Color getCurrentColour() {
            return prefs.getDestinationColour();
        }

        /**
         * Called when the user sets a new colour for the preference via the
         * dialog.
         *
         * @param colour the new colour.
         */
        protected void userSetColour(Color colour) {
            prefs.setDestinationColour(colour);
        }
    }

    /**
     * The <code>StateColourItem</code> class is responsible for displaying a
     * menu item and dialog for changing the colour of agents in a given state.
     */
    private class StateColourItem extends ColourMenuItem {
        /**
         * Unused serialization version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * The state being controlled by this menu item.
         */
        private State state;

        /**
         * Creates a new <code>StateColourItem</code> for the given state.
         *
         * @param state the state for which to control the colour.
         */
        public StateColourItem(State state) {
            super(MainFrame.this,
                    (state.getName().isEmpty() ? "Agent" : state.getName()),
                    (state.getName().isEmpty() ? "Set Agent Colour"
                            : "Set Agent Colour: " + state.getName()),
                    prefs.getAgentColour(state));
            this.state = state;
        }

        /**
         * Returns the current colour of the preference being controlled.
         *
         * @return the current colour.
         */
        protected Color getCurrentColour() {
            return prefs.getAgentColour(this.state);
        }

        /**
         * Called when the user sets a new colour for the preference via the
         * dialog.
         *
         * @param colour the new colour.
         */
        protected void userSetColour(Color colour) {
            prefs.setAgentColour(this.state, colour);
        }
    }

    /**
     * The <code>GroupIndexAction</code> class represents the action that
     * changes the selected index in a <code>MenuItemGroup</code> based on
     * keyboard input.
     */
    private class GroupIndexAction extends AbstractAction {
        /**
         * Unused serialization version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * The menu item group to modify.
         */
        private MenuItemGroup group;

        /**
         * The amount by which to change the selected index when the action is
         * performed.
         */
        private int amount;

        /**
         * Creates a new <code>GroupIndexAction</code> that affects the
         * selected index of the given menu item group.
         *
         * @param group the menu item group to modify.
         * @param amount the increment of the selected index.
         */
        public GroupIndexAction(MenuItemGroup group, int amount) {
            this.group = group;
            this.amount = amount;
        }

        /**
         * Modify the selected index by the set amount.
         *
         * @param e the action that occured, which is ignored.
         */
        public void actionPerformed(ActionEvent e) {
            this.group.incrementSelectedIndex(this.amount);
        }
    }

    /**
     * The <code>AgentSizeListener</code> class is responsible for showing the
     * size configuration dialog when the user selects that menu item.
     */
    private class AgentSizeListener implements ActionListener {
        /**
         * Makes the configuration dialog appear.
         *
         * @param e the event that caused this method to be called, which is
         *        ignored.
         */
        public void actionPerformed(ActionEvent e) {
            if (sizeConf == null || sizeConf.isVisible() == false)
                sizeConf = new SizeDialog(MainFrame.this, prefs);
            displayDialog(sizeConf);
        }
    }
}
