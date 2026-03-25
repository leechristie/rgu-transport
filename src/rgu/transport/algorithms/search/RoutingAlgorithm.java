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
 * An algorithm which can find a route between two vertices on an explorable graph. Support for
 * predicate-based search is optional and not expected to be supported by directed search
 * algorithms. Instances cam be converted to a typed routing algorithm using the
 * toTypedRoutingAlgorithm method.
 *
 * @param <E> the edge type
 * @author Lee A. Christie
 */
public interface RoutingAlgorithm<E> {

    /**
     * Creates a predicate which checked whether an element is one of a specific given collection of
     * non-null elements.
     *
     * @param <V>     the vertex type
     * @param targets the targets, not null, no null elements
     * @return the predicate
     */
    static <V> Predicate<V> isOneOf(Collection<V> targets) {
        return Set.copyOf(targets)::contains;
    }

    /**
     * Creates a predicate which checked whether an element is one of a specific given collection of
     * non-null elements.
     *
     * @param <V>     the vertex type
     * @param targets the targets, not null, no null elements
     * @return the predicate
     */
    @SafeVarargs // generic var-arg passed to Set.of()
    @SuppressWarnings("varargs")
    static <V> Predicate<V> isOneOf(V... targets) {
        return Set.of(targets)::contains;
    }

    /**
     * Reconstructs the total cost of a given path.
     *
     * @param graph the graph on which to route, not null
     * @param path  a list of vertices describing each step in the path, not null
     * @param <V>   the vertex type
     * @return the total cost of the path
     */
    <V> E cost(Explorable<V, E> graph, List<V> path);

    /**
     * Finds a route from the specified source to the specified target.
     *
     * @param <V>    the vertex type
     * @param graph  the graph on which to route, not null
     * @param source the source vertex, in graph, not null
     * @param target the target vertex, in graph, not null
     * @return a list of vertices describing each step in the path
     * @throws TargetUnreachableException if no route could be found from source to target
     * @throws InterruptedException       if the thread is interrupted, and the implementation
     *                                    supports interruption
     */
    <V> List<V> path(Explorable<V, E> graph, V source, V target)
            throws TargetUnreachableException, InterruptedException;

    /**
     * Finds a route from the specified source to the specified target within a given maximum cost.
     *
     * @param <V>     the vertex type
     * @param graph   the graph on which to route, not null
     * @param source  the source vertex, in graph, not null
     * @param target  the target vertex, in graph, not null
     * @param maxCost the maximum cost, considered unlimited if null
     * @return a list of vertices describing each step in the path
     * @throws TargetUnreachableException if no route could be found from source to target
     * @throws InterruptedException       if the thread is interrupted, and the implementation
     *                                    supports interruption
     */
    <V> List<V> path(Explorable<V, E> graph, V source, V target, E maxCost)
            throws TargetUnreachableException, InterruptedException;

    /**
     * Finds a route from the specified source to the specified target(s) by predicate which returns
     * true if a given vertex is a target.
     *
     * @param <V>    the vertex type
     * @param graph  the graph on which to route, not null
     * @param source the source vertex, in graph, not null
     * @param target the criteria to identify a target vertex, in graph, not null
     * @return a list of vertices describing each step in the path
     * @throws TargetUnreachableException if no route could be found from source to target
     * @throws InterruptedException       if the thread is interrupted, and the implementation
     *                                    supports interruption
     */
    <V> List<V> path(Explorable<V, E> graph, V source, Predicate<V> target)
            throws TargetUnreachableException, InterruptedException;

    /**
     * Finds a route from the specified source to the specified target(s) by predicate which returns
     * true if a given vertex is a target, within a given maximum cost.
     *
     * @param <V>     the vertex type
     * @param graph   the graph on which to route, not null
     * @param source  the source vertex, in graph, not null
     * @param target  the target vertex, in graph, not null
     * @param maxCost the maximum cost, considered unlimited if null
     * @return a list of vertices describing each step in the path
     * @throws TargetUnreachableException if no route could be found from source to target
     * @throws InterruptedException       if the thread is interrupted, and the implementation
     *                                    supports interruption
     */
    <V> List<V> path(Explorable<V, E> graph, V source, Predicate<V> target, E maxCost)
            throws TargetUnreachableException, InterruptedException;

}
