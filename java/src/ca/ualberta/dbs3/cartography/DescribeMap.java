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

import ca.ualberta.dbs3.commandLine.*;
import ca.ualberta.dbs3.math.*;
import java.util.Iterator;

/**
 * The <code>DescribeMap</code> class is capable of reading a map file and
 * printing out all of the streets and intersections contained in that file, or
 * statistics relating to the map.
 */
public class DescribeMap extends Application {
    /**
     * The number of decimal places to output for correlation results.
     */
    private static final int CORRELATION_DECIMAL_PLACES = 5;

    /**
     * The command line options passed to the Java VM.
     */
    private String[] args;

    /**
     * Creates a new <code>DescribeMap</code> application.
     *
     * @param args the command line arguments given to the Java VM.
     */
    public DescribeMap(String[] args) {
        this.args = args;
    }

    /**
     * Runs the application.
     *
     * @return a return code to send when the application exits.
     * @throws MapFileException if there is an error in the loaded map file.
     */
    public int run() throws MapFileException {
        /* Parse the command line */
        Parser parser = new Parser();
        OptionMapFile opMap = new OptionMapFile();
        OptionOutput opOutput = new OptionOutput();
        OptionBias opBias = new OptionBias();
        parser.add(opMap);
        parser.add(opOutput);
        parser.add(opBias);
        if (parser.parse(this.args) == false)
            return -1;
        parser.printSummary();

        /* Output the desired information */
        Map map = new Map(opMap.getMapFile());
        if (opOutput.outputStreets())
            this.printStreets(map);
        if (opOutput.outputStatistics())
            this.printStatistics(map, opBias.doBias());
        return 0;
    }

    /**
     * Prints a description of all the streets and intersections on the map to
     * standard output.
     *
     * @param map the map to describe.
     */
    private void printStreets(Map map) {
        Iterator<Street> streets = map.getStreets();
        while (streets.hasNext()) {
            Street str = streets.next();;
            String outWidth = String.format("%.2f",
                    Math.round(str.getWidth() * 100) / 100.0);
            System.out.println(str.getName() + ": " + str.getMidline()
                    + ", width " + outWidth);
            Iterator<Intersection> inters = map.getIntersections(str);
            while (inters.hasNext()) {
                Intersection inter = inters.next();
                Street cross = inter.getMirror().getStreet();
                System.out.println("    Intersects " + cross.getName() + ": "
                        + inter.getCentrePoint());
            }
        }

        System.out.println();
        BoundingBox box = map.getBoundingBox();
        System.out.format("Minimal X value: %.2f%n", box.minX());
        System.out.format("Maximal X value: %.2f%n", box.maxX());
        System.out.format("Minimal Y value: %.2f%n", box.minY());
        System.out.format("Maximal Y value: %.2f%n", box.maxY());
    }

    /**
     * Prints statistics about the map to standard output.
     *
     * @param map the map to describe.
     * @param bias whether correlation statistics should be length-biased.
     */
    private void printStatistics(Map map, boolean bias) {
        double[] cAngle = new double[map.numSegments()];
        double[] cDist = new double[cAngle.length];
        double[] cTurn = new double[cAngle.length];
        double[] bAngle = new double[cAngle.length];
        double[] bDist = new double[cAngle.length];
        double[] bTurn = new double[cAngle.length];

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
        this.printDiameter(map);
        System.out.println("\nCorrelation between betweenness and closeness:");
        this.printCorrelation(map, bAngle, cAngle, "minimal angle", bias);
        this.printCorrelation(map, bDist, cDist, "minimal Euclidean distance",
                bias);
        this.printCorrelation(map, bTurn, cTurn, "minimal turn", bias);
    }

    /**
     * Prints the diameter of the given map to standard output.
     *
     * @param map the map to describe.
     */
    private void printDiameter(Map map) {
        int r = 0;
        int n = map.numStreets();
        for (int i = 0; i < n - 1; i++) {
            Street strI = map.getStreet(i);
            for (int j = i + 1; j < n; j++) {
                int t = map.minTurns(strI, map.getStreet(j));
                if (t > r)
                    r = t;
            }
        }
        System.out.println("Map diameter: " + r + " turns");
    }

    /**
     * Outputs a single correlation result.
     *
     * @param map the map on which the computations took place.
     * @param betweenness the computed betweenness for each segment.
     * @param closeness the computed closeness for each segment.
     * @param description the description of the computation method.
     * @param bias whether to bias the correlation computation based on the
     *        length of the segments.
     */
    private void printCorrelation(Map map, double[] betweenness,
            double[] closeness, String description, boolean bias) {
        Correlation cor = new Correlation();
        for (int i = 0; i < betweenness.length; i++) {
            if (bias)
                cor.add(betweenness[i], closeness[i],
                        map.getSegment(i).getLength());
            else
                cor.add(betweenness[i], closeness[i]);
        }
        if (bias)
            System.out.println("Length-biased correlation for " + description);
        else
            System.out.println("Correlation for " + description);
        System.out.format("     r   = %."
                + DescribeMap.CORRELATION_DECIMAL_PLACES + "f%n",
                cor.getCorrelation());
        System.out.format("     r^2 = %."
                + DescribeMap.CORRELATION_DECIMAL_PLACES + "f%n",
                cor.getDetermination());
    }

    /**
     * Reads and describes the map file specified on the command line.
     *
     * @param args the command-line arguments given to the Java VM.
     * @throws MapFileException if there is an error in the loaded map.
     */
    public static void main(String[] args) throws MapFileException {
        DescribeMap application = new DescribeMap(args);
        System.exit(application.run());
    }

    /**
     * A command line option for specifying whether to output a description of
     * the map or statistics about the map.
     */
    private class OptionOutput extends Option {

        /**
         * The choice to output a description of the streets.
         */
        private Choice streets;

        /**
         * The choice to output statistics about the map.
         */
        private Choice statistics;

        /**
         * Creates a new <code>OptionOutput</code> to be added to a parser.
         */
        public OptionOutput() {
            super("Output");
            this.streets = new Choice("outputStreets");
            this.statistics = new Choice("outputStats");
            this.addDefault(this.streets);
            this.add(this.statistics);
        }

        /**
         * Returns whether or not to output a description of the streets.
         *
         * @return <code>true</code> if street descriptions should be output,
         *         otherwise <code>false</code>.
         */
        public boolean outputStreets() {
            return this.streets.isActive();
        }

        /**
         * Returns whether or not to output statistics about the map.
         *
         * @return <code>true</code> if statistics should be output, otherwise
         *         <code>false</code>.
         */
        public boolean outputStatistics() {
            return this.statistics.isActive();
        }

        /**
         * Returns a string with a description of the current choice for this
         * option.
         *
         * @return a description of the current choice.
         */
        public String getDescription() {
            if (this.streets.isActive())
                return "Street descriptions";
            else
                return "Map statistics";
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
}
