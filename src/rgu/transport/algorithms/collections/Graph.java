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

package rgu.transport.algorithms.collections;

import java.util.*;
import java.util.function.*;

/**
 * A mathematical graph. Graph extends Explorable with more capabilities.
 *
 * @param <V> the vertex type
 * @param <E> the edge type
 * @author Lee A. Christie
 */
public interface Graph<V, E> extends Explorable<V, E> {

    /**
     * The set of vertices in the graph.
     *
     * @return the set of vertices
     * @throws UnsupportedOperationException if the graph implementation does not support this
     *                                       operation
     */
    default Set<V> vertices() {
        throw new UnsupportedOperationException(
                "graph implementation does not support vertices()");
    }

    /**
     * Checks whether the graph contains the specified vertex.
     *
     * @param vertex the vertex, not null
     * @return true if contained, false otherwise
     */
    boolean containsVertex(V vertex);

    /**
     * Checks whether the graph contains an edge from one specified source to another.
     *
     * @param from the vertex for which this is an out-edge, not null
     * @param to   the vertex for which this is an in-edge, not null
     * @return if the edge exists, false otherwise
     */
    boolean containsEdge(V from, V to);

    /**
     * Returns a view of the graph with all edges reversed.
     *
     * @return a view of the graph
     * @throws UnsupportedOperationException if the graph implementation does not support this
     *                                       operation
     */
    default Graph<V, E> reverse() {
        throw new UnsupportedOperationException("graph implementation does not support reverse()");
    }

    /**
     * Finds the connected components of the graph, assuming that the graph is symmetric in
     * structure. This requires that at least that for every edge (A, B) there exists an edge
     * (B, A), even if the value of the edge is different.
     *
     * @return a list of fully-connected sub graphs ordered from largest to smallest
     * @throws UnsupportedOperationException if the graph implementation does not support this
     *                                       operation
     */
    default List<Graph<V, E>> symmetricConnectedComponents() {
        throw new UnsupportedOperationException(
                "graph implementation does not support symmetricConnectedComponents()");
    }

    /**
     * Returns a graph which is equal to this graph but symmetric by removing one-way edges.
     *
     * @return a symmetric graph
     * @throws UnsupportedOperationException if the graph implementation does not support this
     *                                       operation
     */
    default Graph<V, E> breakToSymmetricStructure() {
        return toSymmetricStructure(_ -> null);
    }

    /**
     * Returns a graph which is equal to this graph but symmetric by turning
     * one-way edges into two-way edges of equal value.
     *
     * @return a symmetric graph
     * @throws UnsupportedOperationException if the graph implementation does
     *                                       not support this operation
     */
    default Graph<V, E> mirrorToSymmetricStructure() {
        return toSymmetricStructure(edge -> edge.value);
    }

    /**
     * Returns a graph which is equal to this graph but symmetric. The provided callback function is
     * called for each one-way edge, and must return null to indicate that the edge should be
     * removed, or a non-null value to indicate that the reverse edge should be added with the
     * returned value.
     *
     * @param edgeMap mapping for how to handle each one-way edge, not null
     * @return a symmetric graph
     * @throws UnsupportedOperationException if the graph implementation does not support this
     *                                       operation
     */
    default Graph<V, E> toSymmetricStructure(Function<Edge<V, E>, E> edgeMap) {
        Objects.requireNonNull(edgeMap, "edgeMap");
        throw new UnsupportedOperationException(
                "graph implementation does not support toSymmetricStructure(Function)");
    }

    /**
     * Returns true if the graph is symmetric in structure. This requires that at least that for
     * every edge (A, B) there exists an edge (B, A), even if the value of the edge is different.
     *
     * @return true if structurally symmetric, false otherwise
     * @throws UnsupportedOperationException if the graph implementation does not support this
     *                                       operation
     */
    default boolean isSymmetricStructure() {
        try {
            for (V a : vertices()) {
                for (V b : neighbours(a)) {
                    if (!containsEdge(b, a)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (UnsupportedOperationException cause) {
            throw new UnsupportedOperationException(
                    "graph implementation does not support isSymmetricStructure()", cause);
        }
    }

    /**
     * Returns a list of edges which do no make corresponding reverse edges. i.e. each edge (A, B)
     * such that edge (B, A) does not exist in the graph.
     *
     * @return a list of asymmetries
     * @throws UnsupportedOperationException if the graph implementation does not support this
     *                                       operation
     */
    default List<Edge<V, E>> structuralAsymmetries() {
        try {
            List<Edge<V, E>> rv = new ArrayList<>();
            for (V a : vertices()) {
                for (V b : neighbours(a)) {
                    if (!containsEdge(b, a)) {
                        rv.add(new Edge<>(a, b, edge(a, b)));
                    }
                }
            }
            return Collections.unmodifiableList(rv);
        } catch (UnsupportedOperationException cause) {
            throw new UnsupportedOperationException(
                    "graph implementation does not support structuralAsymmetries()", cause);
        }
    }

    /**
     * Returns a graph which is equal to this graph but with the specified transformation applied to
     * all edges. The provided callback function is called for each edge, and must return null to
     * indicate that the edge should be removed, or a non-null value to indicate that the edge
     * should become equal to the given edge value. Identity mapping will preserve the same edge.
     *
     * @param edgeMap mapping a transformation of edges, not null
     * @return a graph
     * @throws UnsupportedOperationException if the graph implementation does not support this
     *                                       operation
     */
    default Graph<V, E> applyToEdges(Function<Edge<V, E>, E> edgeMap) {
        Objects.requireNonNull(edgeMap, "edgeMap");
        throw new UnsupportedOperationException(
                "graph implementation does not support applyToEdges(Function)");
    }

    /**
     * Iterates over edges, calling the given callback function for each edge in the graph.
     *
     * @param consumer a consumer of edges, not null
     * @throws UnsupportedOperationException if the graph implementation does not support this
     *                                       operation
     */
    default void forEachEdge(Consumer<Edge<V, E>> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        throw new UnsupportedOperationException(
                "graph implementation does not support forEachEdge(Consumer)");
    }

    /**
     * Returns a graph which is equal to this graph but without the edges matching the specified
     * predicate. The provided callback function is called for each edge, and must return true to
     * indicate that the edge should be removed, false to indicate that the edge should be
     * preserved. Implementing classes can support this operation by default by implementing
     * {@link #applyToEdges(Function)}.
     *
     * @param delete predicate indicating which edges to delete, not null
     * @return a graph
     * @throws UnsupportedOperationException if the graph implementation does not support this
     *                                       operation
     */
    default Graph<V, E> deleteEdges(Predicate<Edge<V, E>> delete) {
        Objects.requireNonNull(delete, "delete");
        try {
            return applyToEdges(edge -> delete.test(edge) ? null : edge.value());
        } catch (UnsupportedOperationException cause) {
            throw new UnsupportedOperationException(
                    "graph implementation does not support deleteEdges(Predicate)", cause);
        }
    }

    /**
     * Returns a graph which is equal to this graph but with only the edges matching the specified
     * predicate. The provided callback function is called for each edge, and must return false to
     * indicate that the edge should be removed, true to indicate that the edge should be preserved.
     * Implementing classes can support this operation by default by implementing
     * {@link #applyToEdges(Function)}.
     *
     * @param preserve predicate indicating which edges to preserve, not null
     * @return a graph
     * @throws UnsupportedOperationException if the graph implementation does not support this
     *                                       operation
     */
    default Graph<V, E> preserveEdges(Predicate<Edge<V, E>> preserve) {
        Objects.requireNonNull(preserve, "preserve");
        try {
            return applyToEdges(
                    edge -> preserve.test(edge) ? edge.value() : null);
        } catch (UnsupportedOperationException cause) {
            throw new UnsupportedOperationException(
                    "graph implementation does not support" +
                            " preserveEdges(Predicate)", cause);
        }
    }

    /**
     * Returns a graph which is equal to this graph but with only the vertices matching the
     * specified predicate. The provided callback function is called for each vertex, and must
     * return false to indicate that the edge should be removed, true to indicate that the edge
     * should be preserved. If a vertex is deleted, connected in or out edges for that vertex are
     * also deleted.
     *
     * @param preserve predicate indicating which vertices to preserve, not null
     * @return a graph
     * @throws UnsupportedOperationException if the graph implementation does not support this
     *                                       operation
     */
    default Graph<V, E> preserveVertices(Predicate<V> preserve) {
        Objects.requireNonNull(preserve, "preserve");
        throw new UnsupportedOperationException(
                "graph implementation does not support preserveVertices(Predicate)");
    }

    /**
     * Returns a graph which is equal to this graph but without the vertices matching the specified
     * predicate. The provided callback function is called for each vertex, and must return true to
     * indicate that the edge should be removed, false to indicate that the edge should be preserved.
     * If a vertex is deleted, connected in or out edges for that vertex are also deleted.
     * Implementing classes can support this operation by default by implementing
     * {@link #preserveVertices(Predicate)}.
     *
     * @param delete predicate indicating which vertices to delete, not null
     * @return a graph
     * @throws UnsupportedOperationException if the graph implementation does not support this
     *                                       operation
     */
    default Graph<V, E> deleteVertices(Predicate<V> delete) {
        Objects.requireNonNull(delete, "delete");
        try {
            return preserveVertices(delete.negate());
        } catch (UnsupportedOperationException cause) {
            throw new UnsupportedOperationException(
                    "graph implementation does not support" +
                            " deleteVertices(Predicate)", cause);
        }
    }

    /**
     * Record type representing an edge with its end points.
     *
     * @param from  the vertex the edge comes from, not null
     * @param to    the vertex the edge goes to, not null
     * @param value the value of the edge, not null
     * @param <V>   the vertex type
     * @param <E>   the edge type
     */
    record Edge<V, E>(V from, V to, E value) {
        /**
         * Canonical constructor with null-checks.
         *
         * @param from  the vertex the edge comes from, not null
         * @param to    the vertex the edge goes to, not null
         * @param value the value of the edge, not null
         */
        public Edge {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            Objects.requireNonNull(value, "value");
        }
    }

}
