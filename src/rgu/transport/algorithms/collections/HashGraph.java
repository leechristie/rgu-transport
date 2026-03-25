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
 * A mathematical graph which uses an underling hashed representation of vertices.
 * {@code HashGraph} is immutable and safe for concurrent use, however, {@link Builder} returned by
 * {@link #builder()} is not safe for concurrent use.
 *
 * @param <V> the vertex type
 * @param <E> the edge type
 * @author Lee A. Christie
 */
public final class HashGraph<V, E> implements Graph<V, E> {

    private final Set<V> vertices;
    private final Map<V, Map<V, E>> neighbours;
    private final Map<V, Map<V, E>> reverseNeighbours;
    private final Graph<V, E> reverse;

    private HashGraph(Set<V> vertices,
                      Map<V, Map<V, E>> neighbours,
                      Map<V, Map<V, E>> reverseNeighbours) {

        Objects.requireNonNull(vertices, "vertices");
        Objects.requireNonNull(neighbours, "neighbours");
        Objects.requireNonNull(reverseNeighbours, "reverseNeighbours");

        this.vertices = vertices;
        this.neighbours = neighbours;
        this.reverseNeighbours = reverseNeighbours;
        this.reverse = new Graph<>() {
            @Override
            public boolean containsVertex(V vertex) {
                return HashGraph.this.containsVertex(vertex);
            }

            // argument order intentionally reversed
            @SuppressWarnings("java:S2234")
            @Override
            public boolean containsEdge(V from, V to) {
                return HashGraph.this.containsEdge(to, from);
            }

            @Override
            public Set<V> neighbours(V vertex) {
                return HashGraph.this.reverseNeighbours(vertex);
            }

            // argument order intentionally reversed
            @SuppressWarnings("java:S2234")
            @Override
            public E edge(V from, V to) {
                return HashGraph.this.edge(to, from);
            }

            @Override
            public Graph<V, E> reverse() {
                return HashGraph.this;
            }
        };

    }

    /**
     * Returns a new builder for HashGraph. Builder is not safe for concurrent use.
     *
     * @param <V> the vertex type
     * @param <E> the edge type
     * @return a builder
     */
    public static <V, E> Builder<V, E> builder() {
        return new Builder<>();
    }

    /**
     * The set of vertices in the graph.
     *
     * @return the set of vertices
     */
    @Override
    public Set<V> vertices() {
        return this.vertices;
    }

    /**
     * Checks whether the graph contains the specified vertex.
     *
     * @param vertex the vertex, not null
     * @return true if contained, false otherwise
     */
    @Override
    public boolean containsVertex(V vertex) {
        Objects.requireNonNull(vertex, "vertex");
        return this.vertices.contains(vertex);
    }

    /**
     * returns the edge between the specified pair of vertices.
     *
     * @param from the vertex for which this will be an out-edge, not null
     * @param to   the vertex for which this will be an in-edge, not null
     * @return the edge
     * @throws NoSuchElementException if the edge does not exist between from and to
     */
    @Override
    public E edge(V from, V to) {

        if (!containsEdge(from, to)) {
            throw new NoSuchElementException(
                    "no edge from " + from + " to " + to);
        }

        return this.neighbours.get(from).get(to);

    }

    /**
     * Checks whether the graph contains an edge from one specified source to another.
     *
     * @param from the vertex for which this is an out-edge, not null
     * @param to   the vertex for which this is an in-edge, not null
     * @return if the edge exists, false otherwise
     * @throws NoSuchElementException if either vertex does not exist
     */
    @Override
    public boolean containsEdge(V from, V to) {

        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        requireVertexExists(from);
        requireVertexExists(to);

        Map<V, E> neighbourMap = this.neighbours.get(from);
        if (neighbourMap != null) {
            return neighbourMap.containsKey(to);
        }
        return false;

    }

    /**
     * The vertices for which there is an out-edge from the specified vertex to that vertex.
     *
     * @param vertex the specified vertex, not null
     * @return the set of neighbours
     */
    @Override
    public Set<V> neighbours(V vertex) {
        return neighboursImpl(this.neighbours, vertex);
    }

    @Override
    public Graph<V, E> reverse() {
        return reverse;
    }

    /**
     * The vertices for which there is an in-edge to the specified vertex from that vertex.
     *
     * @param vertex the specified vertex, not null
     * @return the set of reverse neighbours
     */
    public Set<V> reverseNeighbours(V vertex) {
        return neighboursImpl(this.reverseNeighbours, vertex);
    }

    /**
     * Finds the connected components of the graph, assuming that the graph is symmetric in
     * structure. This requires that at least that for every edge (A, B) there exists an edge
     * (B, A), even if the value of the edge is different.
     *
     * @return a list of fully-connected sub graphs ordered from largest to smallest
     */
    @Override
    public List<Graph<V, E>> symmetricConnectedComponents() {

        // trivial cases optimization, no need to explore the graph, just return
        // immediately
        if (vertices.isEmpty()) {
            return List.of();
        }
        if (vertices.size() == 1) {
            return List.of(this);
        }

        // explore the graph to find connected components
        Set<V> unexplored = new HashSet<>(this.vertices);
        List<Graph<V, E>> rv = new ArrayList<>();
        while (!unexplored.isEmpty()) {
            V start = unexplored.iterator().next();
            Graph<V, E> sub = subGraph(start);
            unexplored.removeAll(sub.vertices());
            rv.add(sub);
        }

        // sort largest to smallest
        rv.sort((left, right) ->
                // note minus sign
                -Integer.compare(left.vertices().size(),
                        right.vertices().size()));
        return Collections.unmodifiableList(rv);

    }

    /**
     * Returns a graph which is equal to this graph but symmetric. The provided callback function is
     * called for each one-way edge, and must return null to indicate that the edge should be
     * removed, or a non-null value to indicate that the reverse edge should be added with the
     * returned value.
     *
     * @param edgeMap mapping for how to handle each one-way edge, not null
     * @return a symmetric graph
     */
    @Override
    public Graph<V, E> toSymmetricStructure(Function<Edge<V, E>, E> edgeMap) {
        Objects.requireNonNull(edgeMap, "edgeMap");
        if (isSymmetricStructure()) {
            return this;
        }
        Builder<V, E> builder = HashGraph.builder();
        for (V v : vertices()) {
            builder.addVertex(v);
        }
        for (V a : vertices()) {
            for (V b : neighbours(a)) {
                // if it has a corresponding reverse edge (or self-loop)
                if (containsEdge(b, a)) {
                    // then add the original forward-edge as-is
                    builder.addEdge(a, b, edge(a, b));
                } else { // asymmetry
                    Edge<V, E> edge = new Edge<>(a, b, edge(a, b));
                    // query the callback for what to do
                    E callbackResult = edgeMap.apply(edge);
                    // otherwise break edge
                    if (callbackResult != null) {
                        // add the original forward-edge as-is
                        builder.addEdge(a, b, edge(a, b));
                        // add the callback result as the reverse-edge
                        builder.addEdge(b, a, callbackResult);
                    }
                }
            }
        }
        return builder.build();
    }

    /**
     * Returns a graph which is equal to this graph but with the specified transformation applied to
     * all edges. The provided callback function is called for each edge, and must return null to
     * indicate that the edge should be removed, or a non-null value to indicate that the edge
     * should become equal to the given edge value. Identity mapping will preserve the same edge.
     *
     * @param edgeMap mapping a transformation of edges, not null
     * @return a graph
     * @throws UnsupportedOperationException if the graph implementation does not support
     *                                       applyToEdges
     */
    @Override
    public Graph<V, E> applyToEdges(Function<Edge<V, E>, E> edgeMap) {
        Objects.requireNonNull(edgeMap, "edgeMap");
        Builder<V, E> builder = HashGraph.builder();
        for (V v : vertices()) {
            builder.addVertex(v);
        }
        for (V a : vertices()) {
            for (V b : neighbours(a)) {
                Edge<V, E> edge = new Edge<>(a, b, edge(a, b));
                // query the callback for what to do
                E callbackResult = edgeMap.apply(edge);
                // add the edge as-is, otherwise break edge
                if (callbackResult != null) {
                    builder.addEdge(a, b, callbackResult);
                }
            }
        }
        return builder.build();
    }

    /**
     * Iterates over edges, calling the given callback function for each edge in the graph.
     *
     * @param consumer a consumer of edges, not null
     * @throws UnsupportedOperationException if the graph implementation does not support this
     *                                       operation
     */
    @Override
    public void forEachEdge(Consumer<Edge<V, E>> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (V a : vertices()) {
            for (V b : neighbours(a)) {
                consumer.accept(new Edge<>(a, b, edge(a, b)));
            }
        }
    }

    /**
     * Returns a graph which is equal to this graph but with only the vertices matching the
     * specified predicate. The provided callback function is called for each vertex, and must
     * return false to indicate that the edge should be removed, true value to indicate that the
     * edge should be preserved. If a vertex is deleted, connected in or out edges for that vertex
     * are also deleted.
     *
     * @param preserve predicate indicating which vertices to preserve, not null
     * @return a graph
     * @throws UnsupportedOperationException if the graph implementation does not support
     *                                       preserveVertices
     */
    @Override
    public Graph<V, E> preserveVertices(Predicate<V> preserve) {
        Objects.requireNonNull(preserve, "preserve");
        Builder<V, E> builder = HashGraph.builder();
        for (V v : vertices()) {
            if (preserve.test(v)) {
                builder.addVertex(v);
            }
        }
        for (V a : vertices()) {
            for (V b : neighbours(a)) {
                // only add edge if both vertices still exist
                if (builder.vertices.contains(a)
                        && builder.vertices.contains(b)) {
                    builder.addEdge(a, b, edge(a, b));
                }
            }
        }
        return builder.build();
    }

    private Graph<V, E> subGraph(V start) {
        Builder<V, E> builder = HashGraph.builder();
        Deque<V> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            V current = queue.pop();
            if (!builder.vertices.contains(current)) {
                builder.addVertex(current);
            }
            for (V neighbour : neighbours(current)) {
                if (!containsEdge(neighbour, current)) {
                    throw new IllegalStateException(
                            "Graph is not symmetric. Found one-way edge from "
                                    + current + " to " + neighbour + ".");
                }
                if (!builder.vertices.contains(neighbour)) {
                    builder.addVertex(neighbour);
                    queue.addLast(neighbour);
                }
                builder.addEdge(current, neighbour, edge(current, neighbour));
            }
        }
        return builder.build();
    }

    private Set<V> neighboursImpl(Map<V, Map<V, E>> maps, V vertex) {
        Objects.requireNonNull(vertex, "vertex");
        requireVertexExists(vertex);
        if (maps.containsKey(vertex)) {
            return Collections.unmodifiableSet(maps.get(vertex).keySet());
        }
        return Set.of();
    }

    private void requireVertexExists(V vertex) {
        if (!containsVertex(vertex)) {
            throw new NoSuchElementException(
                    "vertex " + vertex + " does not exist");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.neighbours,
                this.reverseNeighbours,
                this.vertices);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HashGraph<?, ?> other)) {
            return false;
        }
        return Objects.equals(this.neighbours, other.neighbours)
                && Objects.equals(this.reverseNeighbours,
                other.reverseNeighbours)
                && Objects.equals(this.vertices, other.vertices);
    }

    /**
     * An incomplete summary string representation of the graph.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return super.toString() + (vertices.size() == 1 ? " (1 vertex)"
                : " (" + vertices.size() + " vertices)");
    }

    /**
     * Builder class for HashGraph. Builder is not safe for concurrent use.
     */
    public static final class Builder<V, E> {

        private boolean built = false;
        private Set<V> vertices = new HashSet<>();
        private Map<V, Map<V, E>> neighbours = new HashMap<>();
        private Map<V, Map<V, E>> reverseNeighbours = new HashMap<>();
        private Builder() {
        }

        /**
         * Add a new vertex to the graph.
         *
         * @param vertex the new vertex, not null
         * @return this, for chaining
         * @throws IllegalStateException if already built, or the vertex already exists
         */
        public Builder<V, E> addVertex(V vertex) {

            Objects.requireNonNull(vertex, "vertex");
            requireNotBuilt("addVertex()");

            if (this.vertices.contains(vertex)) {
                throw new IllegalStateException(
                        "vertex " + vertex + " already exists");
            }

            this.vertices.add(vertex);

            return this;

        }

        /**
         * Add new vertices to the graph. If there are any duplicate vertices, the builder is
         * unchanged.
         *
         * @param vertices the new vertices, not null
         * @return this, for chaining
         * @throws IllegalStateException if already built, or any of the vertices already exist
         */
        @SafeVarargs // var args are only passed to List.of, then discarded
        @SuppressWarnings("varargs")
        public final Builder<V, E> addVertices(V... vertices) {
            return addVertices(List.of(vertices));
        }

        /**
         * Add new vertices to the graph. If there are any duplicate vertices, the builder is
         * unchanged.
         *
         * @param vertices the new vertices, not null
         * @return this, for chaining
         * @throws IllegalStateException if already built, or any of the vertices already exist
         */
        public Builder<V, E> addVertices(Collection<V> vertices) {

            Objects.requireNonNull(vertices, "vertices");
            requireNotBuilt("addVertex()");
            List<V> copy = new ArrayList<>(vertices);
            for (int i = 0; i < copy.size(); i++) {
                Objects.requireNonNull(copy.get(i), "vertices[" + i + "]");
            }

            Set<V> added = new HashSet<>();
            for (V v : copy) {
                if (this.vertices.add(v)) {
                    added.add(v);
                } else {
                    this.vertices.removeAll(added);
                    throw new IllegalStateException(
                            "vertex " + v + " already exists");
                }
            }

            return this;

        }

        /**
         * Adds a new edge to the graph.
         *
         * @param from the source vertex, not null
         * @param to   the destination vertex, not null
         * @param edge the edge, not null
         * @return this, for chaining
         * @throws IllegalStateException  if already built, or the edge already exists
         * @throws NoSuchElementException if either vertex does not exist
         */
        public Builder<V, E> addEdge(V from, V to, E edge) {

            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            Objects.requireNonNull(edge, "edge");
            requireNotBuilt("addEdge()");

            addToNeighboursMap(this.neighbours, from, to, edge);
            addToNeighboursMap(this.reverseNeighbours, to, from, edge);

            return this;

        }

        /**
         * Tries to add a new edge to the graph, and returns a boolean indicating whether it was
         * successful.
         *
         * @param from the source vertex, not null
         * @param to   the destination vertex, not null
         * @param edge the edge, not null
         * @return true if successful, false otherwise
         */
        public boolean tryAddEdge(V from, V to, E edge) {
            try {
                addEdge(from, to, edge);
                return true;
            } catch (IllegalStateException | NoSuchElementException ex) {
                return false;
            }
        }

        private void addToNeighboursMap(Map<V, Map<V, E>> n, V f, V t, E e) {
            Map<V, E> map = n.get(f);
            if (map == null) {
                map = new HashMap<>();
            } else if (map.containsKey(t)) {
                throw new IllegalStateException(
                        "edge from " + f + " to " + t + " already exists");
            }
            map.put(t, e);
            n.put(f, map);
        }

        /**
         * Builds and returns the graph.
         *
         * @return the graph
         * @throws IllegalStateException if already built
         */
        public HashGraph<V, E> build() {

            requireNotBuilt("build()");

            HashGraph<V, E> graph = new HashGraph<>(
                    Collections.unmodifiableSet(this.vertices),
                    this.neighbours,
                    this.reverseNeighbours);

            this.vertices = null;
            this.neighbours = null;
            this.reverseNeighbours = null;
            this.built = true;
            return graph;

        }

        private void requireNotBuilt(String method) {
            if (this.built) {
                throw new IllegalStateException(
                        "call to " + method + " after build");
            }
        }

    }

}
