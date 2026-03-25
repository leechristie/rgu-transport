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

package rgu.transport.geospatial.multimodal;

import rgu.transport.algorithms.collections.Graph;
import rgu.transport.algorithms.collections.TwoDTree;
import rgu.transport.algorithms.search.ThreadUtil;
import rgu.transport.algorithms.util.ProgressListener;
import rgu.transport.geospatial.*;
import rgu.transport.geospatial.osm.*;
import rgu.transport.util.*;

import java.time.*;
import java.util.*;
import java.util.function.*;

/**
 * Used to join a transit network to a road network. Artificial edges are added to connect each
 * point on the transit network with a nearby point on the road network as the crow flies.
 * Artificial edges in the given network are discarded.
 *
 * @author Lee A. Christie
 */
public final class NetworkAndRoadJoiner {

    private NetworkAndRoadJoiner() {
        throw new AssertionError("utility class constructor");
    }

    /**
     * Joins a transit network to a road network.
     *
     * @param transitNetwork the transit network, not null
     * @param roadGraph the road graph, not null
     * @param drivingSpeed the driving speed, not null
     * @param walkingSpeed the walking speed, not null
     * @param maxDistanceToNearbyRoad the maximum distance to walk between a location on the transit
     *                                network and a location on the road graph, not null
     * @param consumerRoadEdge callback for road edges, can be null if not needed
     * @param consumerTransitEdge callback for transit edges, can be null if not needed
     * @param consumerArtificialEdge callback for artificial edges, can be null if not needed
     * @param progress the progress listener, can be null if not needed
     */
    public static TransitNetwork join(TransitNetwork transitNetwork,
                                      Graph<GeoLocation, RoadEdge> roadGraph,
                                      Speed drivingSpeed,
                                      Speed walkingSpeed,
                                      Distance maxDistanceToNearbyRoad,
                                      BiConsumer<GeoLocation, GeoLocation> consumerRoadEdge,
                                      BiConsumer<GeoLocation, GeoLocation> consumerTransitEdge,
                                      BiConsumer<GeoLocation, GeoLocation> consumerArtificialEdge,
                                      ProgressListener progress)
            throws InterruptedException {

        // non-null args
        Objects.requireNonNull(transitNetwork, "transitNetwork");
        Objects.requireNonNull(roadGraph, "roadGraph");
        Objects.requireNonNull(drivingSpeed, "drivingSpeed");
        Objects.requireNonNull(walkingSpeed, "walkingSpeed");
        Objects.requireNonNull(maxDistanceToNearbyRoad, "maxDistanceToNearbyRoad");

        // nullable args
        if (consumerRoadEdge == null) { consumerRoadEdge = (a, b) -> {}; }
        if (consumerTransitEdge == null) { consumerTransitEdge = (a, b) -> {}; }
        if (consumerArtificialEdge == null) { consumerArtificialEdge = (a, b) -> {}; }
        if (progress == null) { progress = ProgressListener.none(); }

        // builder for returned network
        TransitNetwork.Builder builder = TransitNetwork.builder(true);
        builder.setCarSpeed(drivingSpeed);
        builder.setWalkSpeed(walkingSpeed);

        // copy roads to graph
        progress.onNewStage("Copying road edges...");
        for (GeoLocation vertex : roadGraph.vertices()) {
            ThreadUtil.checkInterrupt();
            builder.addLocation(vertex);
        }
        for (GeoLocation a : roadGraph.vertices()) {
            ThreadUtil.checkInterrupt();
            for (GeoLocation b : roadGraph.neighbours(a)) {
                builder.addRoad(a, b);
                consumerRoadEdge.accept(a, b);
            }
        }

        // copy network
        progress.onNewStage("Copying transit edges...");
        for (GeoLocation vertex : transitNetwork.locations()) {
            ThreadUtil.checkInterrupt();
            builder.addLocation(vertex);
        }
        for (GeoLocation a : transitNetwork.locations()) {
            ThreadUtil.checkInterrupt();
            for (Trip trip : transitNetwork.transitNeighbours(a)) {
                GeoLocation b = trip.destination();
                LocalTime departureTime = trip.departureTime();
                LocalTime arrivalTime = trip.arrivalTime();
                builder.addTransit(a, b, departureTime, arrivalTime);
                consumerTransitEdge.accept(a, b);
            }
        }

        // set up KD-tree for finding point on the road graph
        progress.onNewStage("Pre-processing road vertices...");
        TwoDTree<GeoLocation> tree = TwoDTree.of(Set.copyOf(roadGraph.vertices()),
                                                 progress, true);

        // finding connections
        progress.onNewStage("Connecting to roads...");
        Map<GeoLocation, GeoLocation> nearestMapping = GeoLocations.findWithTwoDTree(
                tree, transitNetwork.locations(), progress, true);

        // applying limit of walk time (for artificial edges)
        progress.onNewStage("Checking for points without roads...");
        {
            int i = 0;
            int n = nearestMapping.size();
            progress.onUpdateProgress(i / (double) n);
            for (GeoLocation key : new HashSet<>(nearestMapping.keySet())) {
                ThreadUtil.checkInterrupt();
                GeoLocation value = nearestMapping.get(key);
                if (GeoLocation.HAVERSINE.distance(key, value)
                        .compareTo(maxDistanceToNearbyRoad) > 0) {
                    nearestMapping.remove(key);
                }
                i++;
                progress.onUpdateProgress(i / (double) n);
            }
        }
        progress.onUpdateProgress(1.0);

        // created artificial edges
        progress.onNewStage("Creating artificial edges...");
        {
            int i = 0;
            int n = nearestMapping.size();
            for (Map.Entry<GeoLocation, GeoLocation> entry : nearestMapping.entrySet()) {
                ThreadUtil.checkInterrupt();
                GeoLocation a = entry.getKey();
                GeoLocation b = entry.getValue();
                Distance distance = GeoLocation.HAVERSINE.distance(a, b);
                Duration duration = walkingSpeed.timeTruncatedToSeconds(distance);
                builder.addArtificialEdge(a, b, duration);
                consumerArtificialEdge.accept(a, b);
                i++;
                progress.onUpdateProgress(i / (double) n);
            }
        }
        progress.onUpdateProgress(1.0);

        // return network
        progress.onCompletion();
        return builder.build();

    }

}
