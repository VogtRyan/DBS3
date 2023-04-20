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

package ca.ualberta.dbs3.cartography;

import ca.ualberta.dbs3.math.*;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * The <code>MapFileParser</code> class is responsible for parsing saved map
 * files.
 * <p>
 * There are four types of lines in a map file:
 * <ol>
 * <li>Unit to metre conversion defintions:
 * <code>unitsToMetres UNITS_TO_METRES</code>. This command sets the default
 * number of metres per unit of measurement in the map file, until the next
 * <code>unitsToMetres</code> line in the file. For example, if
 * <code>UNITS_TO_METRES</code> is <code>5</code>, then there are five metres
 * per each unit of distance in the map file. The default value if none is
 * specified in the file is <code>1</code>.</li>
 * <li>Default width definitions: <code>defaultWidth DEFAULT_WIDTH</code>. This
 * command sets the default width (in map file units) for all streets with no
 * width specified, until the next <code>defaultWidth</code> line in the file.
 * The default value if none is specified in the file is <code>1</code>.</li>
 * <li>Street definitions: <code>street from FROM_X FROM_Y to TO_X TO_Y [width
 * WIDTH] name NAME [...]</code>. Defines a street between the given pairs of
 * coordinates, with the optional width (otherwise it will use the default
 * width) and the name (consisting of one or more words).</li>
 * <li>Street continuations: <code>onto TO_X TO_Y</code>. Continues the
 * previous <code>street</code> or <code>onto</code> definition, building a new
 * street starting at the previous <code>to</code> coordinates, having the same
 * name and width.</li>
 * <li>View definitions: <code>view scale METRES_TO_PIXES using FILENAME
 * name NAME [...]</code>. Defines an image that can be used to view the map.
 * The scale determines how to convert the metre measurements of agents'
 * movement to pixels on the image.</li>
 * </ol>
 * Anything that occurs after a <code>#</code> on any line is ignored.
 */
public class MapFileParser {
    /**
     * The default value for the default width of streets.
     */
    private static final double DEFAULT_WIDTH = 1.0;

    /**
     * The default value for the multiplicative constant that converts map file
     * units into metres.
     */
    private static final double DEFAULT_UNITS_TO_METRES = 1.0;

    /**
     * The name of the map file to parse.
     */
    private String filename;

    /**
     * The Scanner responsible for splitting apart the lines in the file into
     * separate Strings.
     */
    private Scanner scanner;

    /**
     * The default width of streets, if no width is specified.
     */
    private double defaultWidth;

    /**
     * The multiplicative constant for converting map file units into metres.
     */
    private double unitsToMetres;

    /**
     * The line number currently being parsed in the map file.
     */
    private int line;

    /**
     * The most-recently parsed streets that has not yet been added to the list
     * of streets. May be empty, or could have multiple sections of a street
     * while we are parsing an "onto" command.
     */
    private LinkedList<StreetInformation> parsedStreets;

    /**
     * The streets parsed thus far.
     */
    private List<Street> streets;

    /**
     * The map views parsed thus far.
     */
    private List<ViewSpecification> views;

    /**
     * Creates a new <code>MapFileParser</code> to parse the file with the
     * given name.
     *
     * @param filename the name of the file to parse.
     * @throws MapFileException if the map file does not conform to the map
     *         file specification, or is otherwise not able to be parsed.
     */
    public MapFileParser(String filename) throws MapFileException {
        this.filename = filename;
        this.defaultWidth = MapFileParser.DEFAULT_WIDTH;
        this.unitsToMetres = MapFileParser.DEFAULT_UNITS_TO_METRES;
        this.line = 0;
        this.parsedStreets = new LinkedList<StreetInformation>();
        this.streets = new ArrayList<Street>();
        this.views = new ArrayList<ViewSpecification>();

        /* Open the map definition file */
        try {
            this.scanner = new Scanner(new File(this.filename));
        } catch (FileNotFoundException e) {
            throw new MapFileException(this.filename, "file not found");
        }

        /* Parse each line in the map file */
        try {
            while (true) {
                if (this.scanner.hasNextLine() == false)
                    break;
                this.line++;
                this.parseLine(this.scanner.nextLine());
            }
            this.processParsedStreets();
        } finally {
            this.scanner.close();
        }

        /* Ensure that the map file contains some streets */
        if (this.streets.isEmpty())
            throw new MapFileException(this.filename, "no streets in map");

        /* Resolve view image file names */
        int numViews = this.views.size();
        for (int i = 0; i < numViews; i++) {
            ViewSpecification vs = this.views.get(i);
            try {
                vs.resolvePath(this.filename);
            } catch (IOException ioe) {
                throw new MapFileException(this.filename,
                        "cannot resolve filename of view: "
                                + vs.getFilename());
            }
        }
    }

    /**
     * Returns an array of all the <code>Street</code>s defined in the map
     * file, guaranteed to be larger than length zero, with the <code>ID</code>
     * of each <code>Street</code> guaranteed to be equal to its index in the
     * array.
     *
     * @return the streets defined in the map file.
     */
    public Street[] getStreets() {
        return this.streets.toArray(new Street[this.streets.size()]);
    }

    /**
     * Returns an array of all the <code>ViewSpecification</code>s defined in
     * the map file.
     *
     * @return the view specifications defined in the map file.
     */
    public ViewSpecification[] getViews() {
        return this.views.toArray(new ViewSpecification[this.views.size()]);
    }

    /**
     * Parses a single line from the map file.
     *
     * @param line the line to parse.
     * @throws MapFileException if the line does not conform to the map file
     *         specification.
     */
    private void parseLine(String line) throws MapFileException {
        /* Trim off whitespace and comments */
        line = line.trim();
        int commentIndex = line.indexOf('#');
        if (commentIndex != -1)
            line = line.substring(0, commentIndex);
        if (line.length() == 0)
            return;
        String[] tokens = line.split(" ");

        /* Ensure that "onto" is attached to a "street" command */
        if (tokens[0].equalsIgnoreCase("onto")) {
            if (this.parsedStreets.isEmpty())
                throw new MapFileException(this.filename, this.line,
                        "\"onto\" without \"street\"");
        } else
            this.processParsedStreets();

        /* Process the command */
        if (tokens[0].equalsIgnoreCase("street"))
            this.parseStreet(tokens);
        else if (tokens[0].equalsIgnoreCase("onto"))
            this.parseOnto(tokens);
        else if (tokens[0].equalsIgnoreCase("view"))
            this.views.add(this.parseView(tokens));
        else if (tokens[0].equalsIgnoreCase("unitsToMetres"))
            this.unitsToMetres = this.parseDoubleCommand(tokens);
        else if (tokens[0].equalsIgnoreCase("defaultWidth"))
            this.defaultWidth = this.parseDoubleCommand(tokens);
        else
            throw new MapFileException(this.filename, this.line,
                    "illegal command \"" + tokens[0] + "\"");
    }

    /**
     * Parses a {@link Street} definition line from the map file and adds it to
     * the <code>parsedStreets</code> list.
     *
     * @param tokens the map file line broken into component tokens.
     * @throws MapFileException if the line does not conform to the map file
     *         specification.
     */
    private void parseStreet(String[] tokens) throws MapFileException {
        double fromX, fromY, toX, toY;
        double width = this.defaultWidth;
        String name = "";
        int onToken = 1;

        /* Parse the "from" */
        if (tokens.length <= onToken
                || tokens[onToken].equalsIgnoreCase("from") == false)
            throw new MapFileException(this.filename, this.line,
                    "expected \"from\"");
        onToken++;
        fromX = this.parseDouble(tokens, onToken++);
        fromY = this.parseDouble(tokens, onToken++);

        /* Parse the "to" */
        if (tokens.length <= onToken
                || tokens[onToken].equalsIgnoreCase("to") == false)
            throw new MapFileException(this.filename, this.line,
                    "expected \"to\"");
        onToken++;
        toX = this.parseDouble(tokens, onToken++);
        toY = this.parseDouble(tokens, onToken++);

        /* Parse the optional width */
        if (tokens.length <= onToken
                || (tokens[onToken].equalsIgnoreCase("width") == false
                        && tokens[onToken].equalsIgnoreCase("name") == false))
            throw new MapFileException(this.filename, this.line,
                    "expected \"width\" or \"name\"");
        if (tokens[onToken].equalsIgnoreCase("width")) {
            onToken++;
            width = this.parseDouble(tokens, onToken++);
        }

        /* Parse the name */
        if (tokens.length <= onToken
                || tokens[onToken].equalsIgnoreCase("name") == false)
            throw new MapFileException(this.filename, this.line,
                    "expected \"name\"");
        onToken++;
        if (tokens.length <= onToken)
            throw new MapFileException(this.filename, this.line,
                    "empty street name");
        while (onToken < tokens.length) {
            name = name + tokens[onToken++];
            if (onToken < tokens.length)
                name = name + " ";
        }

        /* Save the information for "onto" commands */
        Point a = new Point(fromX * this.unitsToMetres,
                fromY * this.unitsToMetres);
        Point b =
                new Point(toX * this.unitsToMetres, toY * this.unitsToMetres);
        StreetInformation si =
                new StreetInformation(name, a, b, width * this.unitsToMetres);
        this.parsedStreets.addLast(si);
    }

    /**
     * Parses a {@link Street} continuation line from the map file and adds it
     * to the <code>parsedStreets</code> list.
     *
     * @param tokens the map file line broken into component tokens.
     * @throws MapFileException if the line does not conform to the map file
     *         specification.
     */
    private void parseOnto(String[] tokens) throws MapFileException {
        double toX, toY;
        int onToken = 1;

        toX = this.parseDouble(tokens, onToken++);
        toY = this.parseDouble(tokens, onToken++);

        Point b =
                new Point(toX * this.unitsToMetres, toY * this.unitsToMetres);
        StreetInformation prev = this.parsedStreets.getLast();
        StreetInformation si = new StreetInformation(prev.getName(),
                prev.getTo(), b, prev.getWidth());
        this.parsedStreets.addLast(si);
    }

    /**
     * Processes any street data that has been placed into the
     * <code>parsedStreets</code> list and not yet added to the final list of
     * streets.
     */
    private void processParsedStreets() {
        int nParts = this.parsedStreets.size();
        int onPart = 0;
        if (nParts == 0)
            return;

        while (onPart < nParts) {
            StreetInformation part = this.parsedStreets.poll();
            String name = part.getName();
            if (nParts > 1)
                name += " Part " + (onPart + 1) + "/" + nParts;
            LineSegment ls = new LineSegment(part.getFrom(), part.getTo());
            Street str =
                    new Street(name, this.streets.size(), ls, part.getWidth());
            this.streets.add(str);
            onPart++;
        }
    }

    /**
     * Parses a <code>ViewSpecification</code> definition line from the map
     * file and returns a representation of the view.
     *
     * @param tokens the map file line broken into component tokens.
     * @return the <code>ViewSpecification</code> represented by the given line
     *         in the map file.
     * @throws MapFileException if the line does not conform to the map file
     *         specification.
     */
    private ViewSpecification parseView(String[] tokens)
            throws MapFileException {
        double scale;
        String filename;
        String name = "";
        int onToken = 1;

        /* Parse the "scale" */
        if (tokens.length <= onToken
                || tokens[onToken].equalsIgnoreCase("scale") == false)
            throw new MapFileException(this.filename, this.line,
                    "expected \"scale\"");
        onToken++;
        scale = this.parseDouble(tokens, onToken++);

        /* Parse the filename */
        if (tokens.length <= onToken
                || tokens[onToken].equalsIgnoreCase("using") == false)
            throw new MapFileException(this.filename, this.line,
                    "expected \"using\"");
        onToken++;
        if (tokens.length <= onToken)
            throw new MapFileException(this.filename, this.line,
                    "empty view image filename");
        filename = tokens[onToken++];

        /* Parse the name */
        if (tokens.length <= onToken
                || tokens[onToken].equalsIgnoreCase("name") == false)
            throw new MapFileException(this.filename, this.line,
                    "expected \"name\"");
        onToken++;
        if (tokens.length <= onToken)
            throw new MapFileException(this.filename, this.line,
                    "empty view description");
        while (onToken < tokens.length) {
            name = name + tokens[onToken++];
            if (onToken < tokens.length)
                name = name + " ";
        }

        ViewSpecification ret = null;
        try {
            ret = new ViewSpecification(name, filename, scale);
        } catch (IllegalArgumentException excep) {
            throw new MapFileException(this.filename, this.line,
                    "invalid map view description");
        }
        return ret;
    }

    /**
     * Parses a command from the map file that takes a single
     * <code>double</code> argument, and returns the argument.
     *
     * @param tokens the map file line broken into component tokens.
     * @return the argument to the map file line.
     * @throws MapFileException if the line does not conform to the map file
     *         specification.
     */
    private double parseDoubleCommand(String[] tokens)
            throws MapFileException {
        double newValue = this.parseDouble(tokens, 1);
        if (tokens.length > 2)
            throw new MapFileException(this.filename, this.line,
                    "unexpected token \"" + tokens[2] + "\"");
        return newValue;
    }

    /**
     * Parses a <code>double</code> argument to a line in the map file at the
     * given token index.
     *
     * @param tokens the map file line broken into component tokens.
     * @param index the index at which to extract the <code>double</code>.
     * @return the value of the argument at the given index.
     * @throws MapFileException if the line or index is invalid.
     */
    private double parseDouble(String[] tokens, int index)
            throws MapFileException {
        double val = 0.0;

        if (index >= tokens.length)
            throw new MapFileException(this.filename, this.line,
                    "missing value");
        try {
            val = Double.parseDouble(tokens[index]);
        } catch (NumberFormatException e) {
            throw new MapFileException(this.filename, this.line,
                    "illegal number format");
        }
        if (val < 0.0)
            throw new MapFileException(this.filename, this.line,
                    "negative values prohibited");
        return val;
    }

    /**
     * The <code>StreetInformation</code> class represents information parsed
     * from a map file that will be used to construct a {@link Street} object,
     * after any "onto" commands have been parsed.
     */
    private class StreetInformation {
        /**
         * The overall name of the street (excluding any "part" information
         * added because of the "onto" command).
         */
        private String name;

        /**
         * The starting point of the street.
         */
        private Point from;

        /**
         * The end point of the street.
         */
        private Point to;

        /**
         * The width of the street.
         */
        private double width;

        /**
         * Constructs a new <code>StreetInformation</code> structure.
         *
         * @param name the overall name of the street.
         * @param from the starting point of the street.
         * @param to the ending point of the street.
         * @param width the width of the street.
         */
        public StreetInformation(String name, Point from, Point to,
                double width) {
            this.name = name;
            this.from = from;
            this.to = to;
            this.width = width;
        }

        /**
         * Returns the overall name of the street.
         *
         * @return the overall name of the street.
         */
        public String getName() {
            return this.name;
        }

        /**
         * Returns the starting point of the street.
         *
         * @return the starting point of the street.
         */
        public Point getFrom() {
            return this.from;
        }

        /**
         * Returns the ending point of the street.
         *
         * @return the ending point of the street.
         */
        public Point getTo() {
            return this.to;
        }

        /**
         * Returns the width of the street.
         *
         * @return the width of the street.
         */
        public double getWidth() {
            return this.width;
        }
    }
}
