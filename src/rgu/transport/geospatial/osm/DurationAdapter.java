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

package rgu.transport.geospatial.osm;

import rgu.transport.algorithms.collections.Graph;
import rgu.transport.geospatial.*;

import java.time.*;
import java.util.*;

/**
 * Adapts an instance of Graph&lt;GeoLocation, RoadEdge&gt; to Graph&lt;GeoLocation, Duration&gt;
 * to use with an instance of RoutingAlgorithm&lt;Duration&gt; or
 * ReachabilityAlgorithm&lt;Duration&gt;.
 *
 * @author Lee A. Christie
 */
public final class DurationAdapter {

    private DurationAdapter() {
        throw new AssertionError("utility class constructor");
    }

    /**
     * Adapts the specified graph using the specified fixed speed.
     *
     * @param graph the graph to adapt, not null
     * @param speed the fixed speed, not null, > 0
     * @return an adapted graph
     */
    public static Graph<GeoLocation, Duration> adapt(
            Graph<GeoLocation, RoadEdge> graph, Speed speed) {
        Objects.requireNonNull(speed, "speed");
        if (speed.isZero()) {
            throw new IllegalArgumentException("speed = " + speed + ", expected > 0");
        }
        return new AdapterImpl(graph) {
            @Override
            public Duration edge(GeoLocation from, GeoLocation to) {
                // ignoring speed limit - using given walking speed
                return speed.timeTruncatedToSeconds(graph.edge(from, to).distance());
            }
        };
    }

    /**
     * Adapts the specified graph using the speed defined by the road edges.
     *
     * @param graph the graph to adapt, not null
     * @return an adapted graph
     */
    public static Graph<GeoLocation, Duration> adapt(Graph<GeoLocation, RoadEdge> graph) {
        return new AdapterImpl(graph) {
            @Override
            public Duration edge(GeoLocation from, GeoLocation to) {
                RoadEdge re = graph.edge(from, to);
                return re.speed().timeTruncatedToSeconds(re.distance());
            }
        };
    }

    // implementation of the methods which are common between both versions of the adapt method
    private static abstract class AdapterImpl
            implements Graph<GeoLocation, Duration> {

        Graph<GeoLocation, RoadEdge> graph;

        AdapterImpl(Graph<GeoLocation, RoadEdge> graph) {
            Objects.requireNonNull(graph);
            this.graph = graph;
        }

        @Override
        public Set<GeoLocation> vertices() {
            return graph.vertices();
        }
        @Override
        public boolean containsVertex(GeoLocation vertex) {
            return graph.containsVertex(vertex);
        }
        @Override
        public Set<GeoLocation> neighbours(GeoLocation vertex) {
            return graph.neighbours(vertex);
        }
        @Override
        public boolean containsEdge(GeoLocation from, GeoLocation to) {
            return graph.containsEdge(from, to);
        }

    }

}
