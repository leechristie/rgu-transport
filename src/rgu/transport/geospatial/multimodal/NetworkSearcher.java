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

import rgu.transport.algorithms.collections.*;
import rgu.transport.algorithms.search.*;
import rgu.transport.geospatial.*;

import java.time.*;
import java.util.*;
import java.util.function.*;

/**
 * Convenience wrapper for reachability searches using Dijkstra. Instance should be obtained from
 * the searcher() method on TransitNetwork.
 *
 * @author Lee A. Christie
 */
public final class NetworkSearcher {

    private static final Dijkstra<Duration> DIJKSTRA = Dijkstra.ofDuration();
    private static final Dijkstra<Duration> NEGATIVE_DIJKSTRA = Dijkstra.ofNegativeDuration();
    private static final Function<JourneyState, JourneyPosition> TO_SPATIAL
        = JourneyState::position;
    private static final LocalDate ANY_DATE = LocalDate.of(2021, 1, 1);

    private final TransitNetwork network;

    NetworkSearcher(TransitNetwork network) {
        this.network = network;
    }

    /**
     * Returns a map of locations reachable within the given time windows,
     * and the corresponding cost of each location's shortest path duration.
     *
     * @param location the location, not null
     * @param earliestDeparture the earliest departure time, not null
     * @param latestArrival the latest arrival time, not null
     * @param direction the direction of travel, not null
     * @return an ordered map of reachable locations, with associated costs
     * @throws InterruptedException if the thread is interrupted
     */
    public Map<GeoLocation, Duration> reachable(GeoLocation location,
                                                LocalTime earliestDeparture,
                                                LocalTime latestArrival,
                                                TravelDirection direction)
            throws InterruptedException {
        return reachable(location, earliestDeparture, latestArrival, direction,
                         (Map<GeoLocation, List<Trip>>) null);
    }

    /**
     * Returns a map of locations reachable within the given time windows,
     * and the corresponding cost of each location's shortest path duration.
     *
     * @param location the location, not null
     * @param earliestDeparture the earliest departure time, not null
     * @param latestArrival the latest arrival time, not null
     * @param direction the direction of travel, not null
     * @param extra additional trips to be included, ignored if null or empty
     * @return an ordered map of reachable locations, with associated costs
     * @throws InterruptedException if the thread is interrupted
     */
    public Map<GeoLocation, Duration> reachable(GeoLocation location,
                                                LocalTime earliestDeparture,
                                                LocalTime latestArrival,
                                                TravelDirection direction,
                                                Map<GeoLocation, List<Trip>> extra)
            throws InterruptedException {
        Map<GeoLocation, Duration> rv = new HashMap<>();
        reachable(location, earliestDeparture, latestArrival, direction, extra, rv::put);
        return rv;
    }

    /**
     * Streams to a callback function the set of locations reachable within the given time windows,
     * and the corresponding cost of each location's shortest path duration.
     *
     * @param location the location, not null
     * @param earliestDeparture the earliest departure time, not null
     * @param latestArrival the latest arrival time, not null
     * @param direction the direction of travel, not null
     * @param consumer a consumer of reachable locations, with associated costs
     * @throws InterruptedException if the thread is interrupted
     */
    public void reachable(GeoLocation location,
                          LocalTime earliestDeparture,
                          LocalTime latestArrival,
                          TravelDirection direction,
                          BiConsumer<GeoLocation, Duration> consumer)
            throws InterruptedException {
        reachable(location, earliestDeparture, latestArrival, direction, null, consumer);
    }

    /**
     * Streams to a callback function the set of locations reachable within the given time windows,
     * and the corresponding cost of each location's shortest path duration.
     *
     * @param location the location, not null
     * @param earliestDeparture the earliest departure time, not null
     * @param latestArrival the latest arrival time, not null
     * @param direction the direction of travel, not null
     * @param extra additional trips to be included, ignored if null or empty
     * @param consumer a consumer of reachable locations, with associated costs
     * @throws InterruptedException if the thread is interrupted
     */
    public void reachable(GeoLocation location,
                          LocalTime earliestDeparture,
                          LocalTime latestArrival,
                          TravelDirection direction,
                          Map<GeoLocation, List<Trip>> extra,
                          BiConsumer<GeoLocation, Duration> consumer)
            throws InterruptedException {

        // argument validation
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(earliestDeparture, "earliestDeparture");
        Objects.requireNonNull(latestArrival, "latestArrival");
        if (!earliestDeparture.isBefore(latestArrival)) {
            throw new IllegalArgumentException("earliestDeparture is not before latestArrival");
        }
        Objects.requireNonNull(direction, "direction");
        if (consumer == null) {
            consumer = (g, d) -> {};
        }
        final BiConsumer<GeoLocation, Duration> finalConsumer = consumer;

        // calculate duration
        LocalDateTime fromDT = LocalDateTime.of(ANY_DATE, earliestDeparture);
        LocalDateTime toDT = LocalDateTime.of(ANY_DATE, latestArrival);
        Duration maxDuration = Duration.between(fromDT, toDT);

        // get the graph (with trips if needed)
        if (extra != null && extra.isEmpty()) {
            extra = Map.of();
        }
        Graph<JourneyState, Duration> graph = extra == null ? this.network.spatialTemporalGraph()
                : this.network.spatialTemporalGraph(extra);

        // assign parameters for positive or negative search direction
        LocalDateTime startDateTime;
        Dijkstra<Duration> dijkstra;
        if (direction == TravelDirection.INBOUND) {
            maxDuration = maxDuration.negated();
            startDateTime = toDT;
            dijkstra = NEGATIVE_DIJKSTRA;
            graph = graph.reverse();
        } else {
            startDateTime = fromDT;
            dijkstra = DIJKSTRA;
        }
        JourneyState source = JourneyState.of(location, startDateTime, false, true);

        // set up ST mapping
        BiFunction<JourneyPosition, Duration, JourneyState> toSpatialTemporal
                = (position, cost) -> JourneyState.of(
                position, startDateTime, cost);

        // consumer wrapper, removes state and negation
        BiConsumer<JourneyState, Duration> consumerWrapper;
        if (direction == TravelDirection.INBOUND) {
            consumerWrapper = (g, d) -> {
                finalConsumer.accept(g.location(), d.negated());
            };
        } else {
            consumerWrapper = (g, d) -> {
                finalConsumer.accept(g.location(), d);
            };
        }

        // run reachable search
        dijkstra.stReachable(graph,
                             source,
                             maxDuration,
                             ReachabilityCallback.from(consumerWrapper),
                             new SpatialTemporalMapping<>(TO_SPATIAL, toSpatialTemporal));

    }

}
