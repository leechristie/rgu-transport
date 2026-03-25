/*
 * Copyright © 2021-2026 Robert Gordon University
 *
 * This library is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this library. If
 * not, see <https://www.gnu.org/licenses/>.
 */

package rgu.transport.util;

import rgu.transport.algorithms.collections.*;
import rgu.transport.geospatial.*;
import rgu.transport.geospatial.osm.*;

import java.util.*;

public class TestPercentSpeedCap {

    private static Map<String, GeoLocation> locs = new HashMap<>();

    static {
        locs.put("N1", GeoLocation.parseLocation("0.1", "0.1"));
        locs.put("N2", GeoLocation.parseLocation("0.2", "0.2"));
        locs.put("N3", GeoLocation.parseLocation("0.3", "0.3"));
        locs.put("N4", GeoLocation.parseLocation("0.4", "0.4"));
        locs.put("S1", GeoLocation.parseLocation("0.5", "0.5"));
        locs.put("S2", GeoLocation.parseLocation("0.6", "0.6"));
        locs.put("S3", GeoLocation.parseLocation("0.7", "0.7"));
        locs.put("S4", GeoLocation.parseLocation("0.8", "0.8"));
        locs.put("S5", GeoLocation.parseLocation("0.9", "0.9"));
        locs.put("O1", GeoLocation.parseLocation("1.0", "1.0"));
        locs.put("O2", GeoLocation.parseLocation("1.1", "1.1"));
        locs.put("O3", GeoLocation.parseLocation("1.2", "1.2"));
        locs.put("O4", GeoLocation.parseLocation("1.3", "1.3"));
    }

    private static String name(GeoLocation loc) {
        if (locs.size() < 2) {
            throw new AssertionError("unfinished locs");
        }
        for (var e : locs.entrySet()) {
            if (e.getValue().equals(loc)) {
                if (e.getKey() == null) {
                    throw new NoSuchElementException("unknown element: " + loc);
                }
                return e.getKey();
            }
        }
        throw new NoSuchElementException("unknown element: " + loc);
    }

    private static GeoLocation loc(String key) {
        if (locs.size() < 2) {
            throw new AssertionError("unfinished locs");
        }
        if (!locs.containsKey(key)) {
            throw new NoSuchElementException("unknown element: " + key);
        }
        GeoLocation rv = locs.get(key);
        if (rv == null) {
            throw new NoSuchElementException("unknown element: " + key);
        }
        return rv;
    }

    private static <V, E> void addSymetricEdge(HashGraph.Builder<V, E> builder, V from, V to, E edge) {
        try { builder.addVertex(from); } catch (IllegalStateException ignored) { }
        try { builder.addVertex(to); } catch (IllegalStateException ignored) { }
        builder.addEdge(from, to, edge);
        builder.addEdge(to, from, edge);
    }

    public static Graph<GeoLocation, RoadEdge> createTestGraph() {

        HashGraph.Builder<GeoLocation, RoadEdge> builder = HashGraph.builder();

        // north
        addSymetricEdge(builder, loc("N1"), loc("N2"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(100.0)));
        addSymetricEdge(builder, loc("N2"), loc("N3"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(100.0)));
        addSymetricEdge(builder, loc("N1"), loc("N4"), new RoadEdge(Distance.ofMeters(1), null));

        // north-south crossing
        addSymetricEdge(builder, loc("N3"), loc("S1"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(100.0)));
        addSymetricEdge(builder, loc("N4"), loc("S5"), new RoadEdge(Distance.ofMeters(1), null));

        // south
        addSymetricEdge(builder, loc("S1"), loc("S2"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(100.0)));
        addSymetricEdge(builder, loc("S2"), loc("S3"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(200.0)));
        addSymetricEdge(builder, loc("S3"), loc("S4"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(200.0)));
        addSymetricEdge(builder, loc("S2"), loc("S5"), new RoadEdge(Distance.ofMeters(1), null));

        // outside
        addSymetricEdge(builder, loc("O1"), loc("O2"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(100.0)));
        addSymetricEdge(builder, loc("O2"), loc("O3"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(200.0)));
        addSymetricEdge(builder, loc("O3"), loc("O4"), new RoadEdge(Distance.ofMeters(1), null));

        return builder.build();

    }

    public static Graph<GeoLocation, RoadEdge> createExpectedGraph() {

        HashGraph.Builder<GeoLocation, RoadEdge> builder = HashGraph.builder();

        // north
        addSymetricEdge(builder, loc("N1"), loc("N2"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));
        addSymetricEdge(builder, loc("N2"), loc("N3"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));
        addSymetricEdge(builder, loc("N1"), loc("N4"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));

        // north-south crossing
        addSymetricEdge(builder, loc("N3"), loc("S1"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));
        addSymetricEdge(builder, loc("N4"), loc("S5"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));

        // south
        addSymetricEdge(builder, loc("S1"), loc("S2"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(25.0)));
        addSymetricEdge(builder, loc("S2"), loc("S3"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));
        addSymetricEdge(builder, loc("S3"), loc("S4"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));
        addSymetricEdge(builder, loc("S2"), loc("S5"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(25.0)));

        // outside
        addSymetricEdge(builder, loc("O1"), loc("O2"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(12.5)));
        addSymetricEdge(builder, loc("O2"), loc("O3"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(25.0)));
        addSymetricEdge(builder, loc("O3"), loc("O4"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(12.5)));

        return builder.build();

    }

    public static Graph<GeoLocation, RoadEdge> createExpectedSouthOnlyGraph() {

        HashGraph.Builder<GeoLocation, RoadEdge> builder = HashGraph.builder();

        // north
        addSymetricEdge(builder, loc("N1"), loc("N2"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(12.5)));
        addSymetricEdge(builder, loc("N2"), loc("N3"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(12.5)));
        addSymetricEdge(builder, loc("N1"), loc("N4"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(12.5)));

        // north-south crossing
        addSymetricEdge(builder, loc("N3"), loc("S1"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(25.0)));
        addSymetricEdge(builder, loc("N4"), loc("S5"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(25.0)));

        // south
        addSymetricEdge(builder, loc("S1"), loc("S2"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(25.0)));
        addSymetricEdge(builder, loc("S2"), loc("S3"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));
        addSymetricEdge(builder, loc("S3"), loc("S4"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));
        addSymetricEdge(builder, loc("S2"), loc("S5"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(25.0)));

        // outside
        addSymetricEdge(builder, loc("O1"), loc("O2"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(12.5)));
        addSymetricEdge(builder, loc("O2"), loc("O3"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(25.0)));
        addSymetricEdge(builder, loc("O3"), loc("O4"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(12.5)));

        return builder.build();

    }

    public static Graph<GeoLocation, RoadEdge> createExpectedNorthOnlyGraph() {

        HashGraph.Builder<GeoLocation, RoadEdge> builder = HashGraph.builder();

        // north
        addSymetricEdge(builder, loc("N1"), loc("N2"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));
        addSymetricEdge(builder, loc("N2"), loc("N3"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));
        addSymetricEdge(builder, loc("N1"), loc("N4"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));

        // north-south crossing
        addSymetricEdge(builder, loc("N3"), loc("S1"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));
        addSymetricEdge(builder, loc("N4"), loc("S5"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));

        // south
        addSymetricEdge(builder, loc("S1"), loc("S2"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(25.0)));
        addSymetricEdge(builder, loc("S2"), loc("S3"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));
        addSymetricEdge(builder, loc("S3"), loc("S4"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));
        addSymetricEdge(builder, loc("S2"), loc("S5"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(25.0)));

        // outside
        addSymetricEdge(builder, loc("O1"), loc("O2"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(25.0)));
        addSymetricEdge(builder, loc("O2"), loc("O3"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(50.0)));
        addSymetricEdge(builder, loc("O3"), loc("O4"), new RoadEdge(Distance.ofMeters(1), Speed.ofMetersPerSecond(25.0)));

        return builder.build();

    }

    public static void main(String[] args) {

        Graph<GeoLocation, RoadEdge> graph = createTestGraph();
        PercentSpeedCap psc = new PercentSpeedCap(graph);

        try {
            psc.apply();
            throw new AssertionError("no exception thrown with no predicates");
        } catch (NoSuchElementException ex) {
            System.out.println("good error: " + ex);
        }

        psc.add("NORTH", x -> name(x).startsWith("N"), Speed.ofMetersPerSecond(50.0));
        Graph<GeoLocation, RoadEdge> actualNorth = psc.apply();
        print(graph, createExpectedNorthOnlyGraph(), actualNorth);

        psc.add("SOUTH", x -> name(x).startsWith("S"), Speed.ofMetersPerSecond(50));
        Graph<GeoLocation, RoadEdge> actualBoth = psc.apply(true);

        print(graph, createExpectedGraph(), actualBoth);


        psc.add("EMPTY", x -> false, Speed.ofMetersPerSecond(50));
        try {
            psc.apply();
            throw new AssertionError("no exception thrown when adding empty predicate");
        } catch (NoSuchElementException ex) {
            System.out.println("good error: " + ex);
        }

        System.out.println("\n\n");

        System.out.println("\n\n");

        psc = new PercentSpeedCap(graph);

        psc.add("SOUTH", x -> name(x).startsWith("S"), Speed.ofMetersPerSecond(50));
        Graph<GeoLocation, RoadEdge> actualSouth = psc.apply();
        print(graph, createExpectedSouthOnlyGraph(), actualSouth);

        System.out.println();
        System.out.println("S=N: " + actualSouth.equals(actualNorth));

    }

    private static void print(Graph<GeoLocation, RoadEdge> graph,
                              Graph<GeoLocation, RoadEdge> expected,
                              Graph<GeoLocation, RoadEdge> actual) {

        System.out.println("\t\t\tORI\t\t\tACT\t\t\tEXP");
        for (GeoLocation from : graph.vertices()) {
            for (GeoLocation to : graph.neighbours(from)) {
                String t = get(graph, from, to);
                String a = get(actual, from, to);
                String e = get(expected, from, to);
                String same = a.equals(e) ? "yes" : "no";
                System.out.println(name(from) + "->" + name(to) + "\t" + t + "\t" + a + "\t" + e + "\t" + same);
            }
        }
        System.out.println(actual);
        System.out.println("same : " + actual.equals(expected));

        System.out.println("\n\n");

    }

    private static String get(Graph<GeoLocation, RoadEdge> graph, GeoLocation from, GeoLocation to) {
        try {
            if (graph.edge(from, to).speed() == null) {
                return "null              ";
            }
            return graph.edge(from, to).distance() + "/" + graph.edge(from, to).speed().toString() + "  ";
        } catch (NoSuchElementException ex) {
            return "-                 ";
        }
    }

}
