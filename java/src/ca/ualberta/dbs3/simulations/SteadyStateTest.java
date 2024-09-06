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

package ca.ualberta.dbs3.simulations;

import ca.ualberta.dbs3.cartography.*;
import ca.ualberta.dbs3.commandLine.*;
import ca.ualberta.dbs3.math.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * The <code>SteadyStateTest</code> class is capable of reading a map file and
 * running a single simulation on it, comparing the steady state of people on a
 * given street or segment to how the street or segment is initialized.
 */
public class SteadyStateTest extends Application {
    /**
     * The number of decimal places to output.
     */
    private static final int DECIMAL_PLACES = 5;

    /**
     * The command line options passed to the Java VM.
     */
    private String[] args;

    /**
     * Creates a new <code>SteadyStateTest</code> application.
     *
     * @param args the command line arguments given to the Java VM.
     */
    public SteadyStateTest(String[] args) {
        this.args = args;
    }

    /**
     * Runs the application.
     *
     * @return a return code to send when the application exits.
     * @throws MapFileException if there is an error in the loaded map file.
     * @throws SteadyStateFileException if there is an error loading an
     *         observation file.
     */
    public int run() throws MapFileException, SteadyStateFileException {
        /* Parse the command line */
        Parser parser = new Parser();
        OptionMapFile opMap = new OptionMapFile();
        OptionPathfinder opPath = new OptionPathfinder();
        OptionDestinationChooser opDest = new OptionDestinationChooser();
        OptionDestinations opNumDests = new OptionDestinations();
        OptionSeed opSeed = new OptionSeed();
        OptionOutput opOutput = new OptionOutput();
        OptionNames opNames = new OptionNames();
        OptionSort opSort = new OptionSort();
        OptionBias opBias = new OptionBias();
        parser.add(opMap);
        parser.add(opPath);
        parser.add(opDest);
        parser.add(opNumDests);
        parser.add(opSeed);
        parser.add(opOutput);
        parser.add(opNames);
        parser.add(opSort);
        parser.add(opBias);
        if (parser.parse(this.args) == false)
            return -1;
        parser.printSummary();

        /* Load the map, pathfinder, etc. */
        ProgressMonitor pm = new ProgressMonitorPrint();
        Map map = new Map(opMap.getMapFile());
        Pathfinder pathfinder = opPath.getPathfinder(map);
        DestinationChooser destChooser = opDest.getChooser(map, pm);

        /* Run the steady state test */
        DistanceMap dm = this.runTest(map, destChooser, pathfinder,
                opNumDests.getNumDestinations(), opSeed.getSeed());

        /* Output whatever results are desired */
        if (opOutput.outputSegments())
            this.outputResult(dm, map, false, opNames.outputNames(),
                    opSort.sortOutput());
        if (opOutput.outputStreets())
            this.outputResult(dm, map, true, opNames.outputNames(),
                    opSort.sortOutput());
        if (opOutput.outputCorrelations())
            this.outputCorrelations(dm, map, opBias.doBias());
        String corrFile = opOutput.outputCorrelationsFromFile();
        if (corrFile != null)
            this.outputCorrelations(dm, map, opBias.doBias(), corrFile);
        return 0;
    }

    /**
     * Runs a single steady state test, using the given movement rules and
     * random seed, moving to the given number of destinations.
     *
     * @param map the map on which to run the test.
     * @param destChooser the destination chooser to use.
     * @param pathfinder the pathfinder to use.
     * @param destinations the number of destinations to which to travel.
     * @param seed the random seed to use.
     * @return a distance map, containing how much time was spent on each
     *         segment of the map.
     */
    private DistanceMap runTest(Map map, DestinationChooser destChooser,
            Pathfinder pathfinder, int destinations, long seed) {
        SeedGenerator seedGen = new SeedGenerator(seed);
        Random prng = new Random(seedGen.nextSeed());
        DistanceMap dm = new DistanceMap();
        ProgressMonitor pm = new ProgressMonitorPrint();

        Waypoint current = destChooser.getSteadyDestination(prng);
        pm.start("Simulating agent");
        for (int i = 0; i < destinations; i++) {
            pm.update(i, destinations);
            Waypoint next = destChooser.getDestination(current, prng);
            Path path = pathfinder.getPath(current, next, prng);
            this.consume(path, dm, map);
            current = next;
        }
        pm.end();
        return dm;
    }

    /**
     * Consumes the given path, adding all of the distances spent on each
     * segment to the given distance map.
     *
     * @param path the path to consume.
     * @param dm the distance map to which to add the distances spent.
     * @param map the map on which the movement occurred.
     */
    private void consume(Path path, DistanceMap dm, Map map) {
        Waypoint current = path.pop();
        Waypoint next = path.pop();
        while (next != null) {

            /*
             * Iterate through all ordered pairs of unique points in the path
             */
            Point currentLocation = current.getLocation();
            Point nextLocation = next.getLocation();
            if (currentLocation.equals(nextLocation) == false) {
                /*
                 * Add the distance spent on the indicated street and on all
                 * cross streets
                 */
                LineSegment lineSeg =
                        new LineSegment(currentLocation, nextLocation);
                Street currentStreet = current.getStreet();
                this.addDistances(dm, lineSeg, currentStreet, map);
                Iterator<Intersection> it =
                        map.getIntersections(currentStreet);
                while (it.hasNext()) {
                    Street cross = it.next().getMirror().getStreet();
                    this.addDistances(dm, lineSeg, cross, map);
                }
            }

            current = next;
            next = path.pop();
        }
    }

    /**
     * For the given street, add the amount of the line segment that is
     * contained in each of that street's segments to the given distance map.
     *
     * @param dm the distance map to which to add the values.
     * @param lineSeg the line segment to overlay on the street.
     * @param street the street for which to test its segments for containment
     *        of the line segment.
     * @param map the map on which the movement occurred.
     */
    private void addDistances(DistanceMap dm, LineSegment lineSeg,
            Street street, Map map) {
        Iterator<Segment> it = map.getSegments(street);
        while (it.hasNext()) {
            Segment segment = it.next();
            double distance = segment.lengthContained(lineSeg);
            if (distance > 0.0)
                dm.add(segment, distance);
        }
    }

    /**
     * Outputs the results of a steady state test at the street or segment
     * level.
     *
     * @param dm the recorded distance map of distance spent on each segment.
     * @param map the map on which the test was performed.
     * @param aggregate whether to aggregate results to street level, as
     *        opposed to outputting segment-based statistics.
     * @param outputNames whether street or segment names should be output.
     * @param sortByDistance whether the results should be resorted according
     *        to the amount of distance spent on each street/segment.
     */
    private void outputResult(DistanceMap dm, Map map, boolean aggregate,
            boolean outputNames, boolean sortByDistance) {
        /*
         * Convert the DistanceMap to a Distribution, thereby expressing the
         * amount of distance spent on each street/segment as a value between
         * 0.0 and 1.0, with a sum of 1.0.
         */
        double[] pdf = aggregate ? new double[map.numStreets()]
                : new double[map.numSegments()];
        for (int i = 0; i < pdf.length; i++) {
            if (aggregate)
                pdf[i] = dm.getDistance(map.getStreet(i));
            else
                pdf[i] = dm.getDistance(map.getSegment(i));
        }
        pdf = (new Distribution(pdf)).getPDF();

        /* Build the set of results that will be output. */
        SteadyResult[] results = new SteadyResult[pdf.length];
        for (int i = 0; i < results.length; i++) {
            String name = null;
            if (outputNames) {
                name = (aggregate ? "Street " : "Segment ");
                name = name + Integer.toString(i + 1);
                if (aggregate)
                    name = name + " (" + map.getStreet(i).getName() + ")";
                else
                    name = name + " (" + map.getSegment(i).getDescription()
                            + ")";
            }
            results[i] = new SteadyResult(pdf[i], name);
        }

        /* If requested, sort the results by distance instead of number */
        if (sortByDistance)
            Arrays.sort(results);

        /* Output the data */
        System.out.println("\nPercentage of movement on each "
                + (aggregate ? "street" : "segment") + ":");
        for (int i = 0; i < results.length; i++)
            results[i].output();
    }

    /**
     * Outputs the results of a steady state test as a correlation between time
     * spent on each segment and standard Space Syntax predictors of agents on
     * each segment.
     *
     * @param dm the recorded distance map of distance spent on each segment.
     * @param map the map on which the test was performed.
     * @param bias whether to bias the correlation computation based on the
     *        length of the segments.
     */
    private void outputCorrelations(DistanceMap dm, Map map, boolean bias) {
        int[] segs = new int[map.numSegments()];
        double[] cAngle = new double[segs.length];
        double[] cDist = new double[segs.length];
        double[] cTurn = new double[segs.length];
        double[] bAngle = new double[segs.length];
        double[] bDist = new double[segs.length];
        double[] bTurn = new double[segs.length];
        double[] experimental = new double[segs.length];
        for (int i = 0; i < segs.length; i++) {
            segs[i] = i;
            experimental[i] = dm.getDistance(map.getSegment(i));
        }
        GeodesicFinder gf;

        ProgressMonitor pm = new ProgressMonitorPrint();
        SyntaxComputer sc = new SyntaxComputer();
        gf = new GeodesicFinderMinAngle(map);
        sc.compute(map, gf, cAngle, bAngle, pm);
        gf = new GeodesicFinderMinDistance(map);
        sc.compute(map, gf, cDist, bDist, pm);
        gf = new GeodesicFinderMinTurns(map);
        sc.compute(map, gf, cTurn, bTurn, pm);

        System.out.println();
        this.outputCorrelation(map, experimental, cAngle, segs,
                "minimal angle closeness", bias);
        this.outputCorrelation(map, experimental, bAngle, segs,
                "minimal angle betweenness", bias);
        this.outputCorrelation(map, experimental, cDist, segs,
                "minimal Euclidean distance closeness", bias);
        this.outputCorrelation(map, experimental, bDist, segs,
                "minimal Euclidean distance betweenness", bias);
        this.outputCorrelation(map, experimental, cTurn, segs,
                "minimal turn closeness", bias);
        this.outputCorrelation(map, experimental, bTurn, segs,
                "minimal turn betweenness", bias);
    }

    /**
     * Outputs the results of a steady state test as a correlation between time
     * spent on some segments and a file with recorded numbers of real people
     * on those segments. The input file must consists of lines each containing
     * a pair of integers: the first integer represents the segment number
     * (from 1 to the number of segments, seen using
     * <code>-outputSegments</code>) and the second number represents the
     * number of people on that segment.
     *
     * @param dm the recorded distance map of distance spent on each segment.
     * @param map the map on which the test was performed.
     * @param bias whether to bias the correlation computation based on the
     *        length of the segments.
     * @param experimentFile the file containing the actual human results.
     * @throws SteadyStateFileException if there is an error reading the input
     *         file.
     */
    private void outputCorrelations(DistanceMap dm, Map map, boolean bias,
            String experimentFile) throws SteadyStateFileException {
        Set<Integer> segmentsInFile = new HashSet<Integer>();
        List<Integer> segmentNumber = new ArrayList<Integer>();
        List<Integer> peopleOnSegment = new ArrayList<Integer>();
        int numSegs = map.numSegments();

        /* Read the input file line-by-line */
        int line = 0;
        try (Scanner scanner = new Scanner(new File(experimentFile))) {
            while (scanner.hasNextLine()) {
                line++;
                String lineStr = scanner.nextLine().trim();
                int commentIndex = lineStr.indexOf('#');
                if (commentIndex != -1)
                    lineStr = lineStr.substring(0, commentIndex);
                if (lineStr.length() == 0)
                    continue;
                String[] tokens = lineStr.split(" ");
                if (tokens.length != 2)
                    throw new SteadyStateFileException(experimentFile, line,
                            "not two integers");
                int segNum = Integer.parseInt(tokens[0]);
                int numPeople = Integer.parseInt(tokens[1]);
                if (segNum <= 0 || segNum > numSegs)
                    throw new SteadyStateFileException(experimentFile, line,
                            "invalid segment number");
                if (numPeople < 0)
                    throw new SteadyStateFileException(experimentFile, line,
                            "invalid number of people");
                if (segmentsInFile.add(segNum) == false)
                    throw new SteadyStateFileException(experimentFile, line,
                            "repeat segment");
                segmentNumber.add(segNum);
                peopleOnSegment.add(numPeople);
            }
        } catch (NumberFormatException nfe) {
            throw new SteadyStateFileException(experimentFile, line,
                    "invalid integer");
        } catch (FileNotFoundException fnfe) {
            throw new SteadyStateFileException(experimentFile,
                    "file not found");
        }
        if (segmentsInFile.isEmpty())
            throw new SteadyStateFileException(experimentFile, "empty file");

        /* Grab the simulated steady-state results for those segments */
        double[] sim = new double[peopleOnSegment.size()];
        double[] observed = new double[sim.length];
        int[] seg = new int[sim.length];
        for (int i = 0; i < sim.length; i++) {
            seg[i] = segmentNumber.get(i) - 1;
            sim[i] = dm.getDistance(map.getSegment(seg[i]));
            observed[i] = (double) (peopleOnSegment.get(i));
        }

        /* Output the result */
        this.outputCorrelation(map, sim, observed, seg,
                "observed pedestrians on " + sim.length + " segments", bias);
    }

    /**
     * Outputs a single correlation result.
     *
     * @param map the map on which the experiment took place.
     * @param experimental the experimental results.
     * @param expected the Space Syntax theoretical results or observed human
     *        results.
     * @param segmentIndex the index of the segment on the map, at each offset
     *        in the experimental and expected arrays.
     * @param description the description of the expected results.
     * @param bias whether or not to bias the correlation computation based on
     *        the length of each segment.
     */
    private void outputCorrelation(Map map, double[] experimental,
            double[] expected, int[] segmentIndex, String description,
            boolean bias) {
        Correlation cor = new Correlation();
        for (int i = 0; i < experimental.length; i++) {
            if (bias)
                cor.add(experimental[i], expected[i],
                        map.getSegment(segmentIndex[i]).getLength());
            else
                cor.add(experimental[i], expected[i]);
        }
        if (bias)
            System.out.println("Length-biased correlation to " + description);
        else
            System.out.println("Correlation to " + description);
        System.out.format(
                "     r   = %." + SteadyStateTest.DECIMAL_PLACES + "f%n",
                cor.getCorrelation());
        System.out.format(
                "     r^2 = %." + SteadyStateTest.DECIMAL_PLACES + "f%n",
                cor.getDetermination());
    }

    /**
     * Runs a single simulation on the map file specified on the command line.
     *
     * @param args the command-line arguments given to the Java VM.
     * @throws MapFileException if there is an error in the loaded map.
     * @throws SteadyStateFileException if there is an error loading an
     *         observation file.
     */
    public static void main(String[] args)
            throws MapFileException, SteadyStateFileException {
        SteadyStateTest application = new SteadyStateTest(args);
        System.exit(application.run());
    }

    /**
     * The <code>SteadyResult</code> class stores the amount of distance spent
     * on a single street or segment.
     */
    private class SteadyResult implements Comparable<SteadyResult> {
        /**
         * The proportion of distance spent on the street or segment.
         */
        private double distance;

        /**
         * The description of this street or segment, or <code>null</code> if
         * no street or segment description should be output.
         */
        private String description;

        /**
         * Creates a new <code>SteadyResult</code> associating the given
         * distance to the given description.
         *
         * @param distance the proportion of distance spent on the given street
         *        or segment.
         * @param description a description of the street or segment, or
         *        <code>null</code> if no description should be output.
         * @throws IllegalArgumentException if <code>distance</code> is
         *         negative or greater than <code>1.0</code>
         */
        public SteadyResult(double distance, String description) {
            if (distance < 0.0)
                throw new IllegalArgumentException("Negative distance");
            if (distance > 1.0)
                throw new IllegalArgumentException(
                        "Distance greater than 100%");
            this.distance = distance;
            this.description = description;
        }

        /**
         * Prints this result to standard output.
         */
        public void output() {
            System.out.format(
                    "%" + (SteadyStateTest.DECIMAL_PLACES + 4) + "."
                            + SteadyStateTest.DECIMAL_PLACES + "f",
                    this.distance * 100);
            if (this.description != null)
                System.out.println("   " + this.description);
            else
                System.out.println();
        }

        /**
         * Compares this <code>SteadyResult</code> with the specified
         * <code>SteadyResult</code> for order. These objects are sorted in
         * reverse order by their distance values (that is, larger distances
         * come first in the natural ordering).
         *
         * @param other the <code>SteadyResult</code> to which to compare this
         *        object.
         * @return a negative number if this <code>SteadyResult</code> is
         *         smaller than the given <code>SteadyResult</code>, a positive
         *         number if it is larger, and <code>0</code> if they are
         *         equal.
         */
        public int compareTo(SteadyResult other) {
            if (this.distance < other.distance)
                return 1;
            else if (this.distance > other.distance)
                return -1;
            else {
                if (this.description == other.description)
                    return 0;
                else
                    return this.description.compareTo(other.description);
            }
        }

        /**
         * Tests if this result is equal to the given state. Two results are
         * equal if they have the same distance value and description.
         *
         * @param o the object to test for equality.
         * @return <code>true</code> if the two results are equal,
         *         <code>false</code> otherwise.
         */
        public boolean equals(Object o) {
            if (o == null || !(o instanceof SteadyResult))
                return false;
            SteadyResult other = (SteadyResult) o;
            if (this.distance != other.distance)
                return false;
            if (this.description == other.description)
                return true;
            return this.description.equals(other.description);
        }

        /**
         * Returns a hash code for this <code>SteadyResult</code>.
         *
         * @return a hash code for this state, equal to the hash code of the
         *         distance.
         */
        public int hashCode() {
            return Double.valueOf(this.distance).hashCode();
        }
    }

    /**
     * A command line option for specifying the number of destinations to which
     * to travel in a steady state test.
     */
    private class OptionDestinations extends Option {
        /**
         * The default number of destinations to use if none is specified.
         */
        public static final int DEFAULT_DESTINATIONS = 1000000;

        /**
         * The integer argument given to the only choice in this option.
         */
        private ArgumentInt arg;

        /**
         * Creates a new <code>OptionDestinations</code> to be added to a
         * parser.
         */
        public OptionDestinations() {
            super("Duration");
            Choice choice = new Choice("duration");
            this.arg = new ArgumentInt("numDestinations",
                    OptionDestinations.DEFAULT_DESTINATIONS, 1);
            choice.add(this.arg);
            this.addDefault(choice);
        }

        /**
         * Returns the number of destinations parsed off the command line.
         *
         * @return the number of destinations specified by the user.
         */
        public int getNumDestinations() {
            return this.arg.getValue();
        }

        /**
         * Returns a string with a description of the current choice for this
         * option.
         *
         * @return a description of the current choice.
         */
        public String getDescription() {
            return Integer.toString(this.getNumDestinations())
                    + " destinations";
        }
    }

    /**
     * A command line option for specifying whether to output segment-based
     * statistics, aggregate statistics to the street level, or output
     * correlations to the standard Space Syntax measures.
     */
    private class OptionOutput extends Option {
        /**
         * The choice to output segment statistics.
         */
        private Choice segment;

        /**
         * The choice to output street statistics.
         */
        private Choice street;

        /**
         * The choice to output correlations to theoretical measures.
         */
        private Choice correlation;

        /**
         * The choice to output correlations to an experimental file measuring
         * the actual number of humans observed on different segments.
         */
        private Choice corrFile;

        /**
         * The argument to the correlation file choice.
         */
        private ArgumentString argCorrFile;

        /**
         * Creates a new <code>OptionOutput</code> to be added to a parser.
         */
        public OptionOutput() {
            super("Output");
            this.segment = new Choice("outputSegments");
            this.street = new Choice("outputStreets");
            this.correlation = new Choice("outputCorrelation");
            this.corrFile = new Choice("outputCorrelationFile");
            this.argCorrFile = new ArgumentString("observationFile");
            this.corrFile.add(this.argCorrFile);
            this.add(this.segment);
            this.addDefault(this.street);
            this.add(this.correlation);
            this.add(this.corrFile);
        }

        /**
         * Returns whether or not to output segment usage statistics.
         *
         * @return <code>true</code> if segment statistics should be output,
         *         otherwise <code>false</code>.
         */
        public boolean outputSegments() {
            return this.segment.isActive();
        }

        /**
         * Returns whether or not to output street usage statistics.
         *
         * @return <code>true</code> if street statistics should be output,
         *         otherwise <code>false</code>.
         */
        public boolean outputStreets() {
            return this.street.isActive();
        }

        /**
         * Returns whether or not to output Space Syntax correlations.
         *
         * @return <code>true</code> if correlations should be output,
         *         otherwise <code>false</code>.
         */
        public boolean outputCorrelations() {
            return this.correlation.isActive();
        }

        /**
         * Returns whether or not to output correlations against an observation
         * file.
         *
         * @return the filename of the observation file, or <code>null</code>
         *         if no such output is desired.
         */
        public String outputCorrelationsFromFile() {
            if (this.corrFile.isActive() == false)
                return null;
            return this.argCorrFile.getValue();
        }

        /**
         * Returns a string with a description of the current choice for this
         * option.
         *
         * @return a description of the current choice.
         */
        public String getDescription() {
            if (this.street.isActive())
                return "Street statistics";
            else if (this.segment.isActive())
                return "Segment statistics";
            else if (this.correlation.isActive())
                return "Space Syntax correlations";
            else
                return "Observational correlations";
        }
    }

    /**
     * A command line option for outputting names alongside the statistics.
     */
    private class OptionNames extends Option {
        /**
         * The choice to output names.
         */
        private Choice names;

        /**
         * Creates a new <code>OptionNames</code> to be added to a parser.
         */
        public OptionNames() {
            super("Names");
            this.names = new Choice("outputNames");
            this.add(this.names);
        }

        /**
         * Returns whether names should be output alongside statistics.
         *
         * @return <code>true</code> if names should be output, otherwise
         *         <code>false</code>.
         */
        public boolean outputNames() {
            return this.names.isActive();
        }
    }

    /**
     * A command line option for sorting the results by distance instead of the
     * default index-based ordering.
     */
    private class OptionSort extends Option {
        /**
         * The choice to sort the output.
         */
        private Choice sort;

        /**
         * Creates a new <code>OptionSort</code> to be added to a parser.
         */
        public OptionSort() {
            super("Sort");
            this.sort = new Choice("sortByDistance");
            this.add(this.sort);
        }

        /**
         * Returns whether output should be sorted by distances.
         *
         * @return <code>true</code> if the program output should be sorted by
         *         distance, or <code>false</code> if it should be sorted by
         *         index.
         */
        public boolean sortOutput() {
            return this.sort.isActive();
        }
    }

    /**
     * A command line option for biasing the correlation output based on the
     * length of segments.
     */
    private class OptionBias extends Option {
        /**
         * The choice to bias the correlation.
         */
        private Choice bias;

        /**
         * Creates a new <code>OptionBias</code> to be added to a parser.
         */
        public OptionBias() {
            super("Bias");
            this.bias = new Choice("biasCorrelation");
            this.add(this.bias);
        }

        /**
         * Returns whether correlations should be biased.
         *
         * @return <code>true</code> if correlations should be biased by
         *         segment length, otherwise <code>false</code>.
         */
        public boolean doBias() {
            return this.bias.isActive();
        }
    }

    /**
     * The <code>SteadyStateFileException</code> class represents an error in a
     * reading or parsing human steady-state experiment result file.
     */
    public class SteadyStateFileException extends InputFileException {
        /**
         * Unused serialization version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Creates a new <code>SteadyStateFileException</code> with the given
         * error having happened in the given file.
         *
         * @param filename the file in which the error occurred.
         * @param err a human-readable description of the error.
         */
        public SteadyStateFileException(String filename, String err) {
            super(filename, err);
        }

        /**
         * Creates a new <code>SteadyStateFileException</code> with the given
         * error having happened on the given line of the given file.
         *
         * @param filename the file in which the error occurred.
         * @param line the line on which the error occurred, which must be
         *        greater than zero.
         * @param err a human-readable description of the error.
         * @throws IllegalArgumentException if <code>line</code> is less than
         *         or equal to zero.
         */
        public SteadyStateFileException(String filename, int line,
                String err) {
            super(filename, line, err);
        }
    }
}
