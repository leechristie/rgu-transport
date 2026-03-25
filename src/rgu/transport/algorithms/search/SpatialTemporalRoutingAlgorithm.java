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
import java.util.function.*;

/**
 * An algorithm which can find a route between two vertices on an explorable
 * spatial-temporal graph.
 *
 * @param <E> the edge type
 * @author Lee A. Christie
 */
@SuppressWarnings("java:S119") // use of non-standard 2-letter type <ST>
public interface SpatialTemporalRoutingAlgorithm<E> {

    /**
     * Finds a route from the specified source to the specified target.
     *
     * @param <ST>      the type of the spatial-temporal vertex
     * @param <S>       the type of the spatial component of the vertex
     * @param graph     the graph on which to route, not null
     * @param source    the source vertex, in graph, not null
     * @param target    the target vertex, in graph, not null
     * @param stMapping a spatial-temporal mapping
     * @return a list of vertices describing each step in the path
     * @throws TargetUnreachableException if no route could be found from source to target
     * @throws InterruptedException       if the thread is interrupted, and the implementation
     *                                    supports interruption
     */
    @Deprecated(forRemoval = true)
    <ST, S> List<ST> stPath(Explorable<ST, E> graph,
                            ST source,
                            ST target,
                            SpatialTemporalMapping<S, E, ST> stMapping)
            throws TargetUnreachableException, InterruptedException;

    /**
     * Finds a route from the specified source to the specified target within a given maximum cost.
     *
     * @param <ST>      the type of the spatial-temporal vertex
     * @param <S>       the type of the spatial component of the vertex
     * @param graph     the graph on which to route, not null
     * @param source    the source vertex, in graph, not null
     * @param target    the target vertex, in graph, not null
     * @param maxCost   the maximum cost, considered unlimited if null
     * @param stMapping a spatial-temporal mapping
     * @return a list of vertices describing each step in the path
     * @throws TargetUnreachableException if no route could be found from source to target
     * @throws InterruptedException       if the thread is interrupted, and the implementation
     *                                    supports interruption
     */
    @Deprecated(forRemoval = true)
    <ST, S> List<ST> stPath(Explorable<ST, E> graph,
                            ST source,
                            ST target,
                            E maxCost,
                            SpatialTemporalMapping<S, E, ST> stMapping)
            throws TargetUnreachableException, InterruptedException;

    /**
     * Finds a route from the specified source to the specified target(s) by predicate which returns
     * true if a given vertex is a target.
     *
     * @param <ST>      the type of the spatial-temporal vertex
     * @param <S>       the type of the spatial component of the vertex
     * @param graph     the graph on which to route, not null
     * @param source    the source vertex, in graph, not null
     * @param target    the criteria to identify a target vertex, in graph, not null
     * @param stMapping a spatial-temporal mapping
     * @return a list of vertices describing each step in the path
     * @throws TargetUnreachableException if no route could be found from source to target
     * @throws InterruptedException       if the thread is interrupted, and the implementation
     *                                    supports interruption
     */
    <ST, S> List<ST> stPath(Explorable<ST, E> graph,
                            ST source,
                            Predicate<ST> target,
                            SpatialTemporalMapping<S, E, ST> stMapping)
            throws TargetUnreachableException, InterruptedException;

    /**
     * Finds a route from the specified source to the specified target(s) by predicate which returns
     * true if a given vertex is a target, within a given maximum cost.
     *
     * @param <ST>      the type of the spatial-temporal vertex
     * @param <S>       the type of the spatial component of the vertex
     * @param graph     the graph on which to route, not null
     * @param source    the source vertex, in graph, not null
     * @param target    the target vertex, in graph, not null
     * @param maxCost   the maximum cost, considered unlimited if null
     * @param stMapping a spatial-temporal mapping
     * @return a list of vertices describing each step in the path
     * @throws TargetUnreachableException if no route could be found from source to target
     * @throws InterruptedException       if the thread is interrupted, and the implementation
     *                                    supports interruption
     */
    <ST, S> List<ST> stPath(Explorable<ST, E> graph,
                            ST source,
                            Predicate<ST> target,
                            E maxCost,
                            SpatialTemporalMapping<S, E, ST> stMapping)
            throws TargetUnreachableException, InterruptedException;

}
