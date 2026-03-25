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

package rgu.transport.algorithms.search;

import rgu.transport.algorithms.collections.*;

import java.util.*;

/**
 * An algorithm which can determine which vertices on an explorable spatial-temporal graph are
 * reachable from a source.
 *
 * @param <E> the edge type
 * @author Lee A. Christie
 */
@SuppressWarnings("java:S119") // use of non-standard 2-letter type <ST>
public interface SpatialTemporalReachabilityAlgorithm<E> {

    /**
     * Returns the set of reachable vertices and the corresponding cost of each vertex's shortest
     * path.
     *
     * @param <ST>     the type of the spatial-temporal vertex
     * @param <S>      the type of the spatial component of the vertex
     * @param graph    the graph on which to route, not null
     * @param source   the source vertex, in graph, not null
     * @param stMapping a spatial-temporal mapping
     * @return a map of reachable vertices, with associated costs
     * @throws InterruptedException if the thread is interrupted, and the implementation supports
     *                              interruption
     */
    <ST, S> Map<ST, E> stReachable(Explorable<ST, E> graph,
                                   ST source,
                                   SpatialTemporalMapping<S, E, ST> stMapping)
            throws InterruptedException;

    /**
     * Returns the set of vertices reachable within a given maximum cost, and the corresponding cost
     * of each vertex's shortest path.
     *
     * @param <ST>     the type of the spatial-temporal vertex
     * @param <S>      the type of the spatial component of the vertex
     * @param graph    the graph on which to route, not null
     * @param source   the source vertex, in graph, not null
     * @param maxCost  the maximum cost, considered unlimited if null
     * @param stMapping a spatial-temporal mapping
     * @return a map of reachable vertices, with associated costs
     * @throws InterruptedException if the thread is interrupted, and the
     *                              implementation supports interruption
     */
    <ST, S> Map<ST, E> stReachable(Explorable<ST, E> graph,
                                   ST source,
                                   E maxCost,
                                   SpatialTemporalMapping<S, E, ST> stMapping)
            throws InterruptedException;

    /**
     * Streams to a callback function the set of reachable vertices and the corresponding cost of
     * each vertex's shortest path.
     *
     * @param <ST>     the type of the spatial-temporal vertex
     * @param <S>      the type of the spatial component of the vertex
     * @param graph    the graph on which to route, not null
     * @param source   the source vertex, in graph, not null
     * @param callback a callback which acts as a consumer of reachable vertices, with associated
     *                 costs, but returns {@link ReachabilityCallback.Action#BREAK} if the
     *                 algorithm should abandon the search or
     *                 {@link ReachabilityCallback.Action#CONTINUE} otherwise, not null
     * @param stMapping a spatial-temporal mapping
     * @throws InterruptedException if the thread is interrupted, and the implementation supports
     *                              interruption
     */
    <ST, S> void stReachable(Explorable<ST, E> graph,
                             ST source,
                             ReachabilityCallback<ST, E> callback,
                             SpatialTemporalMapping<S, E, ST> stMapping)
            throws InterruptedException;

    /**
     * Streams to a callback function the set of vertices reachable within a given maximum cost,
     * and the corresponding cost of each vertex's shortest path.
     *
     * @param <ST>     the type of the spatial-temporal vertex
     * @param <S>      the type of the spatial component of the vertex
     * @param graph    the graph on which to route, not null
     * @param source   the source vertex, in graph, not null
     * @param maxCost  the maximum cost, considered unlimited if null
     * @param callback a callback which acts as a consumer of reachable vertices, with associated
     *                 costs, but returns {@link ReachabilityCallback.Action#BREAK} if the
     *                 algorithm should abandon the search or
     *                 {@link ReachabilityCallback.Action#CONTINUE} otherwise, not null
     * @param stMapping a spatial-temporal mapping
     * @throws InterruptedException if the thread is interrupted, and the implementation supports
     *                              interruption
     */
    <ST, S> void stReachable(Explorable<ST, E> graph,
                             ST source,
                             E maxCost,
                             ReachabilityCallback<ST, E> callback,
                             SpatialTemporalMapping<S, E, ST> stMapping)
            throws InterruptedException;

}
