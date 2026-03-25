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
import rgu.transport.geospatial.*;

import java.time.*;
import java.util.*;
import java.util.function.*;

/**
 * A thread-safe and generic implementation of Dijkstra's shortest path algorithm. Instances are
 * typically obtained by calling {@link #ofInt()}, {@link #ofLong()}, {@link #ofDouble()},
 * {@link #ofDuration()}, etc. depending on the desired edge type. The generic constructor
 * {@link #Dijkstra(Object, BinaryOperator, BiPredicate)} method enables use of {@code Dijkstra}
 * on other types not listed. {@code Dijkstra} uses the {@link BinaryMinHeap} class.
 *
 * @author Lee A. Christie
 */
@SuppressWarnings("java:S119") // use of non-standard 2-letter type <ST>
public final class Dijkstra<E>
        implements RoutingAlgorithm<E>,
                   ReachabilityAlgorithm<E>,
                   SpatialTemporalRoutingAlgorithm<E>,
                   SpatialTemporalReachabilityAlgorithm<E> {

    private final E zero;
    private final BinaryOperator<E> add;
    private final BiPredicate<E, E> lessThan;

    /**
     * Creates an instance of Dijkstra which works on the specified generic type of edges. Operators
     * must be pure and the zero immutable for a valid thread-safe instance to be returned.
     *
     * @param zero     an immutable instance of E which represents the constant zero, not null
     * @param add      a pure binary operator which returns a + b, not null
     * @param lessThan a pure predicate which tests whether a &lt; b, not null
     */
    public Dijkstra(E zero, BinaryOperator<E> add, BiPredicate<E, E> lessThan) {
        Objects.requireNonNull(zero, "zero");
        Objects.requireNonNull(add, "add");
        Objects.requireNonNull(lessThan, "lessThan");
        this.zero = zero;
        this.add = add;
        this.lessThan = lessThan;
    }

    /**
     * Returns an instance of Dijkstra which works on long edges.
     *
     * @return a Dijkstra instance
     */
    public static Dijkstra<Long> ofLong() {
        return new Dijkstra<>(0L, Long::sum, (a, b) -> a < b);
    }

    /**
     * Returns an instance of Dijkstra which works on double edges.
     *
     * @return a Dijkstra instance
     */
    public static Dijkstra<Double> ofDouble() {
        return new Dijkstra<>(0.0, Double::sum, (a, b) -> a < b);
    }

    /**
     * Returns an instance of Dijkstra which works on integer edges.
     *
     * @return a Dijkstra instance
     */
    public static Dijkstra<Integer> ofInt() {
        return new Dijkstra<>(0, Integer::sum, (a, b) -> a < b);
    }

    /**
     * Returns an instance of Dijkstra which works on {@linkplain Duration duration} edges.
     *
     * @return a Dijkstra instance
     */
    public static Dijkstra<Duration> ofDuration() {
        return new Dijkstra<>(Duration.ZERO, Duration::plus,
                              (a, b) -> a.compareTo(b) < 0);
    }

    /**
     * Returns an instance of Dijkstra which works on {@linkplain Duration duration} edges where
     * edges are all negative but treated as positive.
     *
     * @return a Dijkstra instance
     */
    public static Dijkstra<Duration> ofNegativeDuration() {
        return new Dijkstra<>(Duration.ZERO, Duration::plus,
                              (a, b) -> a.compareTo(b) > 0);
    }

    /**
     * Returns an instance of Dijkstra which works on {@linkplain Distance distance} edges.
     *
     * @return a Dijkstra instance
     */
    public static Dijkstra<Distance> ofDistance() {
        return new Dijkstra<>(Distance.ZERO, Distance::plus,
                              (a, b) -> a.compareTo(b) > 0);
    }

    /**
     * Reconstructs the total cost of a given path.
     *
     * @param graph the graph on which to route, not null
     * @param path  a list of vertices describing each step in the path, no null
     * @param <V>   the vertex type
     * @return the total cost of the path
     */
    @Override
    public <V> E cost(Explorable<V, E> graph, List<V> path) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(path, "path");
        V previous = null;
        E total = this.zero;
        int i = 0;
        for (V current : path) {
            Objects.requireNonNull(current, "path[" + i++ + "]");
            if (previous != null) {
                total = this.add.apply(total, graph.edge(previous, current));
            }
            previous = current;
        }
        return total;
    }

    /**
     * Finds a route from the specified source to the specified target.
     *
     * @param <V>    the vertex type
     * @param graph  the graph on which to route, not null
     * @param source the source vertex, in graph, not null
     * @param target the target vertex, in graph, not null
     * @return a list of vertices describing each step in the path
     * @throws TargetUnreachableException if no route could be found from source to target
     * @throws InterruptedException       if the thread is interrupted
     */
    @Override
    public <V> List<V> path(Explorable<V, E> graph,
                            V source,
                            V target)
            throws TargetUnreachableException, InterruptedException {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        return stPath(graph,
                      source,
                      Predicate.isEqual(target),
                      null,
                      SpatialTemporalMapping.identity());
    }

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
     * @throws InterruptedException       if the thread is interrupted
     */
    @Override
    public <V> List<V> path(Explorable<V, E> graph,
                            V source,
                            V target,
                            E maxCost)
            throws TargetUnreachableException, InterruptedException {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(maxCost, "maxCost");
        return stPath(graph,
                      source,
                      Predicate.isEqual(target),
                      maxCost,
                      SpatialTemporalMapping.identity());
    }

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
     * @throws InterruptedException       if the thread is interrupted
     */
    @Override
    public <V> List<V> path(Explorable<V, E> graph,
                            V source,
                            Predicate<V> target)
            throws TargetUnreachableException, InterruptedException {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        return stPath(graph,
                      source,
                      target,
                      null,
                      SpatialTemporalMapping.identity());
    }

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
     * @throws InterruptedException       if the thread is interrupted
     */
    @Override
    public <V> List<V> path(Explorable<V, E> graph,
                            V source,
                            Predicate<V> target,
                            E maxCost)
            throws TargetUnreachableException, InterruptedException {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(maxCost, "maxCost");
        return stPath(graph,
                      source,
                      target,
                      maxCost,
                      SpatialTemporalMapping.identity());
    }

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
     * @throws InterruptedException       if the thread is interrupted
     */
    @Override
    public <ST, S> List<ST> stPath(Explorable<ST, E> graph,
                                   ST source,
                                   ST target,
                                   SpatialTemporalMapping<S, E, ST> stMapping)
            throws TargetUnreachableException, InterruptedException {
        return stPath(graph, source, target, null, stMapping);
    }

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
     * @throws InterruptedException       if the thread is interrupted
     */
    @Override
    public <ST, S> List<ST> stPath(Explorable<ST, E> graph,
                                   ST source,
                                   ST target,
                                   E maxCost,
                                   SpatialTemporalMapping<S, E, ST> stMapping)
            throws TargetUnreachableException, InterruptedException {
        return stPath(graph, source, Predicate.isEqual(target),
                maxCost, stMapping);
    }

    /**
     * Finds a route from the specified source to the specified target(s) by
     * predicate which returns true if a given vertex is a target.
     *
     * @param <ST>      the type of the spatial-temporal vertex
     * @param <S>       the type of the spatial component of the vertex
     * @param graph     the graph on which to route, not null
     * @param source    the source vertex, in graph, not null
     * @param target    the criteria to identify a target vertex, in graph, not
     *                  null
     * @param stMapping a spatial-temporal mapping
     * @return a list of vertices describing each step in the path
     * @throws TargetUnreachableException if no route could be found from source
     *                                    to target
     * @throws InterruptedException       if the thread is interrupted
     */
    @Override
    public <ST, S> List<ST> stPath(Explorable<ST, E> graph,
                                   ST source,
                                   Predicate<ST> target,
                                   SpatialTemporalMapping<S, E, ST> stMapping)
            throws TargetUnreachableException, InterruptedException {
        return stPath(graph, source, target, null, stMapping);
    }

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
     * @throws InterruptedException       if the thread is interrupted
     */
    @Override
    public <ST, S> List<ST> stPath(Explorable<ST, E> graph,
                                   ST source,
                                   Predicate<ST> target,
                                   E maxCost,
                                   SpatialTemporalMapping<S, E, ST> stMapping)
            throws TargetUnreachableException, InterruptedException {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(stMapping, "stMapping");
        Map<S, E> dist = new HashMap<>();
        Map<S, S> prev = new HashMap<>();
        ST found = this.dijkstra(graph, source, dist, prev, target, maxCost,
                null, stMapping.toSpatial(), stMapping.toSpatialTemporal());
        return reconstructPath(source, prev, dist, found,
                stMapping.toSpatial(), stMapping.toSpatialTemporal());
    }

    /**
     * Returns the set of reachable vertices and the corresponding cost of each vertex's shortest
     * path.
     *
     * @param <V>    the vertex type
     * @param graph  the graph on which to route, not null
     * @param source the source vertex, in graph, not null
     * @return a map of reachable vertices, with associated costs
     * @throws InterruptedException if the thread is interrupted
     */
    @Override
    public <V> Map<V, E> reachable(Explorable<V, E> graph,
                                   V source)
            throws InterruptedException {
        return reachable(graph, source, (E) null);
    }

    /**
     * Returns the set of vertices reachable within a given maximum cost, and the corresponding cost
     * of each vertex's shortest path.
     *
     * @param <V>     the vertex type
     * @param graph   the graph on which to route, not null
     * @param source  the source vertex, in graph, not null
     * @param maxCost the maximum cost, considered unlimited if null
     * @return a map of reachable vertices, with associated costs
     * @throws InterruptedException if the thread is interrupted
     */
    @Override
    public <V> Map<V, E> reachable(Explorable<V, E> graph,
                                   V source,
                                   E maxCost)
            throws InterruptedException {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(source, "source");
        return stReachable(graph, source, maxCost,
                SpatialTemporalMapping.identity());
    }

    /**
     * Streams to a callback function the set of reachable vertices and the corresponding cost of
     * each vertex's shortest path.
     *
     * @param <V>      the vertex type
     * @param graph    the graph on which to route, not null
     * @param source   the source vertex, in graph, not null
     * @param callback a callback which acts as a consumer of reachable vertices, with associated
     *                 costs, but returns {@link ReachabilityCallback.Action#BREAK} if the
     *                 algorithm should abandon the search or
     *                 {@link ReachabilityCallback.Action#CONTINUE} otherwise, not null
     * @throws InterruptedException if the thread is interrupted
     */
    @Override
    public <V> void reachable(Explorable<V, E> graph,
                              V source,
                              ReachabilityCallback<V, E> callback)
            throws InterruptedException {
        reachable(graph, source, null, callback);
    }

    /**
     * Streams to a callback function the set of vertices reachable within a given maximum cost, and
     * the corresponding cost of each vertex's shortest path.
     *
     * @param <V>      the vertex type
     * @param graph    the graph on which to route, not null
     * @param source   the source vertex, in graph, not null
     * @param maxCost  the maximum cost, considered unlimited if null
     * @param callback a callback which acts as a consumer of reachable vertices, with associated
     *                 costs, but returns {@link ReachabilityCallback.Action#BREAK} if the algorithm
     *                 should abandon the search or {@link ReachabilityCallback.Action#CONTINUE}
     *                 otherwise, not null
     * @throws InterruptedException if the thread is interrupted
     */
    @Override
    public <V> void reachable(Explorable<V, E> graph,
                              V source,
                              E maxCost,
                              ReachabilityCallback<V, E> callback)
            throws InterruptedException {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(callback, "callback");
        stReachable(graph, source, maxCost, callback,
                SpatialTemporalMapping.identity());
    }

    /**
     * Returns the set of reachable vertices and the corresponding cost of each vertex's shortest
     * path.
     *
     * @param <ST>      the type of the spatial-temporal vertex
     * @param <S>       the type of the spatial component of the vertex
     * @param graph     the graph on which to route, not null
     * @param source    the source vertex, in graph, not null
     * @param stMapping a spatial-temporal mapping
     * @return a map of reachable vertices, with associated costs
     * @throws InterruptedException if the thread is interrupted
     */
    @Override
    public <ST, S> Map<ST, E> stReachable(
            Explorable<ST, E> graph,
            ST source,
            SpatialTemporalMapping<S, E, ST> stMapping)
            throws InterruptedException {
        return stReachable(graph, source, (E) null, stMapping);
    }

    /**
     * Returns the set of vertices reachable within a given maximum cost, and the corresponding cost
     * of each vertex's shortest path.
     *
     * @param <ST>      the type of the spatial-temporal vertex
     * @param <S>       the type of the spatial component of the vertex
     * @param graph     the graph on which to route, not null
     * @param source    the source vertex, in graph, not null
     * @param maxCost   the maximum cost, considered unlimited if null
     * @param stMapping a spatial-temporal mapping
     * @return a map of reachable vertices, with associated costs
     * @throws InterruptedException if the thread is interrupted
     */
    @Override
    public <ST, S> Map<ST, E> stReachable(
            Explorable<ST, E> graph,
            ST source,
            E maxCost,
            SpatialTemporalMapping<S, E, ST> stMapping)
            throws InterruptedException {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(source, "source");
        Map<S, E> dist = new HashMap<>();
        this.dijkstra(graph, source, dist, null, null, maxCost, null,
                stMapping.toSpatial(), stMapping.toSpatialTemporal());
        Map<ST, E> rv = new HashMap<>();
        for (Map.Entry<S, E> e : dist.entrySet()) {
            rv.put(stMapping.toSpatialTemporal().apply(
                    e.getKey(), e.getValue()), e.getValue());
        }
        return rv;
    }

    /**
     * Streams to a callback function the set of reachable vertices and the corresponding cost of
     * each vertex's shortest path.
     *
     * @param <ST>      the type of the spatial-temporal vertex
     * @param <S>       the type of the spatial component of the vertex
     * @param graph     the graph on which to route, not null
     * @param source    the source vertex, in graph, not null
     * @param callback a callback which acts as a consumer of reachable
     *                 vertices, with associated costs, but returns
     *                 {@link ReachabilityCallback.Action#BREAK} if the
     *                 algorithm should abandon the search or
     *                 {@link ReachabilityCallback.Action#CONTINUE} otherwise,
     *                 not null
     * @param stMapping a spatial-temporal mapping
     * @throws InterruptedException if the thread is interrupted
     */
    @Override
    public <ST, S> void stReachable(Explorable<ST, E> graph,
                                    ST source,
                                    ReachabilityCallback<ST, E> callback,
                                    SpatialTemporalMapping<S, E, ST> stMapping)
            throws InterruptedException {
        stReachable(graph, source, null, callback, stMapping);
    }

    /**
     * Streams to a callback function the set of vertices reachable within a given maximum cost,
     * and the corresponding cost of each vertex's shortest path.
     *
     * @param <ST>      the type of the spatial-temporal vertex
     * @param <S>       the type of the spatial component of the vertex
     * @param graph     the graph on which to route, not null
     * @param source    the source vertex, in graph, not null
     * @param maxCost   the maximum cost, considered unlimited if null
     * @param callback a callback which acts as a consumer of reachable
     *                 vertices, with associated costs, but returns
     *                 {@link ReachabilityCallback.Action#BREAK} if the
     *                 algorithm should abandon the search or
     *                 {@link ReachabilityCallback.Action#CONTINUE} otherwise,
     *                 not null
     * @param stMapping a spatial-temporal mapping
     * @throws InterruptedException if the thread is interrupted
     */
    @Override
    public <ST, S> void stReachable(Explorable<ST, E> graph,
                                    ST source,
                                    E maxCost,
                                    ReachabilityCallback<ST, E> callback,
                                    SpatialTemporalMapping<S, E, ST> stMapping)
            throws InterruptedException {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(source, "source");
        Map<S, E> dist = new HashMap<>();
        this.dijkstra(graph, source, dist, null, null, maxCost, callback,
                stMapping.toSpatial(), stMapping.toSpatialTemporal());
    }

    // actual implementation of Dijkstra's shortest path algorithm,
    // used by public methods
    private <ST, S> ST dijkstra(Explorable<ST, E> graph,
                                ST source,
                                Map<S, E> dist,
                                Map<S, S> prev,
                                Predicate<ST> target,
                                E maxCost,
                                ReachabilityCallback<ST, E> callback,
                                Function<ST, S> toSpatial,
                                BiFunction<S, E, ST> toSpatialTemporal)
            throws InterruptedException {

        // vertices which have been discovered, but whose out-edges have not
        // been fully explored
        MinHeap<S, E> frontier = createMinHeapInstance();

        // begin at the start vertex
        S sourceSpatial = toSpatial.apply(source);
        dist.put(sourceSpatial, this.zero);
        frontier.addElement(sourceSpatial, this.zero);

        // loop while there are still vertices to explore
        while (!frontier.isEmpty()) {

            // current node to explore from is the minimum at the frontier
            S current = frontier.deleteMinimum();

            // Distance to current vertex
            E currentDist = dist.get(current);
            ST currentSpatialTemporal = toSpatialTemporal.apply(current, currentDist);

            // for streaming, if needed
            if (callback != null && callback.consume(currentSpatialTemporal, currentDist)
                    .equals(ReachabilityCallback.Action.BREAK)) {
                // halt algorithm if consumer requests
                return null;
            }

            // check the interrupt flag so that the thread supports interruption
            ThreadUtil.checkInterrupt();

            // we can terminate the algorithm if the target is the minimum at
            // the frontier
            if (target != null && target.test(currentSpatialTemporal)) {
                return currentSpatialTemporal;
            }

            // loop over all out-edges of the current vertex
            for (ST neighbour : graph.neighbours(currentSpatialTemporal)) {

                // get neighbour without temporal component
                S neighbourSpatial = toSpatial.apply(neighbour);

                // the cost of the out-edge
                E edgeCost = graph.edge(currentSpatialTemporal, neighbour);

                // if the graph contains negative edges, then it is not a valid graph for Dijkstra's
                // algorithm
                if (this.lessThan.test(edgeCost, this.zero)) {
                    throw new IllegalArgumentException("graph contains negative edge cost from "
                                                       + current + " to " + neighbour);
                }

                // the distance to the neighbour via the current vertex
                E alt = this.add.apply(currentDist, edgeCost);

                // if the alt distance is larger than the maximum, then ignore the existence of this
                // out-edge and continue to the next neighbour
                if (maxCost != null && this.lessThan.test(maxCost, alt)) {
                    continue;
                }

                // the current known best distance to the neighbour
                E known = dist.get(neighbourSpatial);

                // if this is the first time we have seen this neighbour, add it to the frontier and
                // continue to the next neighbour
                if (known == null) {

                    // set cost to neighbour
                    dist.put(neighbourSpatial, alt);
                    frontier.addElement(neighbourSpatial, alt);

                    // save path for route-finding, only if needed
                    if (prev != null) {
                        prev.put(neighbourSpatial, current);
                    }

                    // if we have seen this neighbour before but the newly-discovered path is
                    // shorter than the previously-know, then update the frontier
                } else if (this.lessThan.test(alt, known)) {

                    // update cost to neighbour
                    dist.put(neighbourSpatial, alt);
                    frontier.decreaseKey(neighbourSpatial, alt);

                    // save path for route-finding, only if needed
                    if (prev != null) {
                        prev.put(neighbourSpatial, current);
                    }

                }

            }

        }

        // there was no target specified or there no target found
        return null;

    }

    // supplier for MinHeap
    private <V> MinHeap<V, E> createMinHeapInstance() {
        return new BinaryMinHeap<>((a, b) -> b == null || this.lessThan.test(a, b));
    }

    // reconstructs a path from the result of path finding
    private <S, ST> List<ST> reconstructPath(
            ST source,
            Map<S, S> prev,
            Map<S, E> dist,
            ST found,
            Function<ST, S> toSpatial,
            BiFunction<S, E, ST> toSpatialTemporal)
            throws TargetUnreachableException {

        // source is target
        if (source == found) {
            return List.of(source);
        }

        // no target found
        if (found == null) {
            throw new TargetUnreachableException("no path from source to target");
        }

        // reconstruct the path (backwards)
        List<ST> path = new ArrayList<>();
        path.add(found);
        S current = toSpatial.apply(found);
        S previous = prev.get(current);
        while (previous != null) {
            E prevDist = dist.get(previous);
            ST prevSpatialTemporal = toSpatialTemporal.apply(previous, prevDist);
            path.add(prevSpatialTemporal);
            current = previous;
            previous = prev.get(current);
        }

        // reverse the constructed path and return
        Collections.reverse(path);
        return path;

    }

}
