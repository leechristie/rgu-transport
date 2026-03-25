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

package profiling;

import rgu.transport.algorithms.collections.*;
import rgu.transport.algorithms.search.*;
import rgu.transport.geospatial.*;
import rgu.transport.geospatial.osm.*;
import rgu.transport.util.*;

import java.io.*;
import java.lang.management.*;
import java.time.*;
import java.util.*;

public class ProfileGeoLocation {

    private GeoLocation aberdeen = GeoLocation.parseLocation("57.1524396", "-2.0943897");
    private Graph<GeoLocation, Duration> graph;

    static void main(String[] args) throws InterruptedException, IOException {

        GeoLocation aberdeen = GeoLocation.parseLocation("57.1524396", "-2.0943897");

        System.out.print("loading data... ");
        File file = new File("../../Local Data/highway-maxspeed.roads");
        Speed speed = Speed.ofKilometersPerHour(30);
        Graph<GeoLocation, Duration> graph = DurationAdapter.adapt(DataIO.readRoads(file), speed);
        List<GeoLocation> allLocations = new ArrayList<>(graph.vertices());
        System.out.println("done");

        System.out.print("searching... ");
        ReachabilityAlgorithm<Duration> reachability = Dijkstra.ofDuration();
        int iterations = 100;
        long start = System.nanoTime();
        Map<GeoLocation, Duration> reachable = profile(aberdeen, graph, reachability, iterations);
        long end = System.nanoTime();
        long time = (end - start) / (long) iterations;
        System.out.println("done");

        System.out.println("total vertices: " + graph.vertices().size());
        System.out.println("vertices reachable: " + reachable.size());
        System.out.println("Iterations: " + iterations);
        System.out.println("Average iteration time: " + Duration.ofNanos(time));
        System.out.println();

        /////////////////
        // GeoLocation //
        /////////////////

        // standard implementation
        // 0.0277 sec
        // 0.0266 sec
        // 0.0262 sec
        // ------
        // 0.0268 sec (21,049,512 GeoLocation bytes; 14,033,008 Latitude bytes; 14,033,008 Longitude bytes)

        // change to int/int (without constructor call eliminators on anything except hashcode)
        // 0.0248 sec
        // 0.0245 sec
        // 0.0249 sec
        // ------
        // 0.0247 sec (21,049,512 GeoLocation bytes)

        //////////////
        // RoadEdge //
        //////////////

        // TODO

        if (reachable.size() != 33430) {
            throw new AssertionError("Expected 33430 vertices in reachability search!");
        }

        memoryHistogram();

    }

    private static Map<GeoLocation, Duration> profile(
            GeoLocation aberdeen,
            Graph<GeoLocation, Duration> graph,
            ReachabilityAlgorithm<Duration> reachability,
            int iterations) throws InterruptedException {
        Map<GeoLocation, Duration> reachable = null;
        for (int i = 0; i < iterations; i++) {
            reachable = reachability.reachable(graph, aberdeen, Duration.ofHours(1));
        }
        return reachable;
    }

    public static void memoryHistogram() throws IOException {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String PID = name.substring(0, name.indexOf("@"));
        Process p = Runtime.getRuntime().exec("jcmd " + PID + " GC.class_histogram");
        try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            input.lines().filter((string) -> string.contains("rgu.")).forEach(System.out::println);
        }
    }

}
