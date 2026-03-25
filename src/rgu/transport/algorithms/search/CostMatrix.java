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
import rgu.transport.algorithms.util.*;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * A precomputed cost matrix based on graph reachability search.
 *
 * @param <T> the vertex type
 * @param <C> the cost (edge) type
 * @author Lee A. Christie
 */
public final class CostMatrix<T, C> {
    // not a record, must protect against mutation of private collections

    private final Map<T, Integer> lookup;
    private final Object[][] matrix;

    private CostMatrix(final Map<T, Integer> lookup,
                       final Object[][] matrix) {
        this.lookup = lookup;
        this.matrix = matrix;
    }

    /**
     * Constructs a cost matrix by searching the specified graph.
     *
     * @param graph     the graph to search, not null
     * @param vertices  the vertices to include in the matrix, not null
     * @param algorithm the reachability algorithm, not null
     * @param <T>       the vertex type
     * @param <C>       the cost (edge) type
     * @return the cost matrix
     * @throws InterruptedException   if the current thread is interrupted while
     *                                creating the cost matrix
     * @throws NoSuchElementException if any of the specified vertices are not
     *                                found in the graph
     */
    public static <T, C> CostMatrix<T, C> construct(
            final Explorable<T, C> graph,
            final Set<T> vertices,
            final ReachabilityAlgorithm<C> algorithm)
            throws InterruptedException {
        return construct(graph,
                vertices,
                algorithm,
                ProgressListener.none(),
                false);
    }

    /**
     * Constructs a cost matrix by searching the specified graph.
     *
     * @param graph     the graph to search, not null
     * @param vertices  the vertices to include in the matrix, not null
     * @param algorithm the reachability algorithm, not null
     * @param progress  listener for the progress between 0.0 and 1.0, not null
     * @param <T>       the vertex type
     * @param <C>       the cost (edge) type
     * @return the cost matrix
     * @throws InterruptedException   if the current thread is interrupted while
     *                                creating the cost matrix
     * @throws NoSuchElementException if any of the specified vertices are not
     *                                found in the graph
     */
    public static <T, C> CostMatrix<T, C> construct(
            final Explorable<T, C> graph,
            final Set<T> vertices,
            final ReachabilityAlgorithm<C> algorithm,
            final ProgressListener progress)
            throws InterruptedException {
        return construct(graph, vertices, algorithm, progress, false);
    }

    /**
     * Constructs a cost matrix by searching the specified graph.
     *
     * @param graph        the graph to search, not null
     * @param vertices     the vertices to include in the matrix, not null
     * @param algorithm    the reachability algorithm, not null
     * @param progress     listener for the progress between 0.0 and 1.0, not null
     * @param subStageOnly if ture, will not call onNewStage or onCompletion on the progress
     *                     listener
     * @param <T>          the vertex type
     * @param <C>          the cost (edge) type
     * @return the cost matrix
     * @throws InterruptedException   if the current thread is interrupted while creating the cost
     *                                matrix
     * @throws NoSuchElementException if any of the specified vertices are not found in the graph
     */
    public static <T, C> CostMatrix<T, C> construct(
            final Explorable<T, C> graph,
            final Set<T> vertices,
            final ReachabilityAlgorithm<C> algorithm,
            final ProgressListener progress,
            final boolean subStageOnly)
            throws InterruptedException {

        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(vertices, "vertices");
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(progress, "progress");

        // start progress listener
        if (!subStageOnly) {
            progress.onNewStage("Constructing cost matrix...");
        }
        progress.onUpdateProgress(0.0);

        // create the lookup map for indices
        int index = 0;
        Map<T, Integer> lookup = new HashMap<>();
        for (T vertex : vertices) {
            Objects.requireNonNull(vertex, "vertices[" + index + "]");
            lookup.put(vertex, index);
            index++;
        }

        // create the resulting matrix as a square array of nulls,
        // which is written to below
        final Object[][] matrix = new Object[index][index];

        // construct each row of the matrix
        int numCompleted = 0;
        for (Map.Entry<T, Integer> from : lookup.entrySet()) {

            // get the current source vertex
            T fromElement = from.getKey();
            int fromID = from.getValue();

            // construct the current row
            constructFromSource(graph,
                    lookup.keySet(),
                    algorithm,
                    lookup,
                    fromElement,
                    matrix[fromID]);

            // update the progress listener
            numCompleted++;
            progress.onUpdateProgress(numCompleted / (double) lookup.size());

        }

        // stop the progress listener
        if (!subStageOnly) {
            progress.onCompletion();
        }

        // return the cost matrix
        return new CostMatrix<>(lookup, matrix);

    }

    // construct a single line of the cost matrix, Object[] row is an array of
    // nulls uses as the out arg
    private static <T, C> void constructFromSource(
            final Explorable<T, C> graph,
            final Set<T> vertices,
            final ReachabilityAlgorithm<C> algorithm,
            final Map<T, Integer> lookup,
            final T from,
            final Object[] row)
            throws InterruptedException {

        // counts how many targets we have found
        final AtomicInteger hits = new AtomicInteger();

        // routes from the specified source to all points
        algorithm.reachable(graph, from, (to, cost) -> {

            // only process if this is a target vertex, otherwise ignore
            if (vertices.contains(to)) {

                // set the cost in the matrix
                row[lookup.get(to)] = cost;

                // if we have found all target vertices, the break early
                hits.incrementAndGet();
                if (hits.get() == vertices.size()) {
                    return ReachabilityCallback.Action.BREAK;
                }

            }

            return ReachabilityCallback.Action.CONTINUE;

        });

    }

    /**
     * Returns the cost from the specified source to the specified target or the given infinity
     * value if there was no route found.
     *
     * @param source   the source vertex, not null
     * @param target   the target vertex, not null
     * @param infinity the value which should be returned if there is no source from source to
     *                 target, for example <code>null</code> or
     *                 <code>Double.POSITIVE_INFINITY</code> as appropriate
     * @return the cost, or the value of <code>infinity</code> if there was no route found
     * @throws NoSuchElementException if either the source or target does not exist in the cost
     *                                matrix
     */
    public C cost(final T source, final T target, final C infinity)
            throws NoSuchElementException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        final int fromID = id(source);
        final int toID = id(target);
        return get(fromID, toID, infinity);
    }

    /**
     * Returns the cost from the specified source to the specified target.
     *
     * @param source the source vertex, not null
     * @param target the target vertex, not null
     * @return the cost, or the value of <code>infinity</code>
     * @throws NoSuchElementException     if either the source or target does not exist in the cost
     *                                    matrix
     * @throws TargetUnreachableException if there was no route found
     */
    public C cost(final T source, final T target)
            throws TargetUnreachableException {
        final C rv = cost(source, target, null);
        if (rv == null) {
            throw new TargetUnreachableException(
                    "no path from source to target");
        }
        return rv;
    }

    /**
     * Check if the specified vertex was contained in the cost matrix.
     *
     * @param vertex the vertex, not null
     * @return true if the vertex exists, false otherwise
     */
    public boolean contains(final T vertex) {
        Objects.requireNonNull(vertex, "vertex");
        return lookup.containsKey(vertex);
    }

    /**
     * Check if there is a route from the specified source to the specified target.
     *
     * @param source the source vertex, not null
     * @param target the target vertex, not null
     * @return true if there is a route, false otherwise
     * @throws NoSuchElementException if either the source or target does not exist in the cost
     *                                matrix
     */
    public boolean isReachable(final T source, final T target) {
        return cost(source, target, null) != null;
    }

    /**
     * Returns the set of vertices as an unmodifiable set.
     *
     * @return the set of vertices
     */
    public Set<T> vertices() {
        return Collections.unmodifiableSet(lookup.keySet());
    }

    // gets the specified vertex from the matrix, suppressed generic cast from
    // underlying array
    @SuppressWarnings("unchecked")
    private C get(final int fromID, final int toID, final C infinity) {
        final Object rv = matrix[fromID][toID];
        if (rv == null) {
            return infinity;
        }
        return (C) rv;
    }

    // gets the id as a primitive int, with check for non-existent vertices
    private int id(final T vertex) {
        final Integer rv = lookup.get(vertex);
        if (rv == null) {
            throw new NoSuchElementException(vertex + " does not exist in cost matrix");
        }
        return rv;
    }

}
