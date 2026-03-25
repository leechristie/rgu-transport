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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.*;

import java.util.*;
import java.util.function.*;

import static org.junit.jupiter.api.Assertions.*;

final class TestGraph {

    private Graph<String, Integer> defaultGraphType;

    @BeforeEach
    void create_default_type() {
        defaultGraphType = new Graph<String, Integer>() {
            @Override
            public boolean containsVertex(String vertex) {
                return false;
            }
            @Override
            public boolean containsEdge(String from, String to) {
                throw new NoSuchElementException();
            }
            @Override
            public Set<String> neighbours(String vertex) {
                return Set.of();
            }
            @Override
            public Integer edge(String from, String to) {
                throw new NoSuchElementException();
            }
        };
    }

    void assertUnsupported(Executable ex) {
        assertThrows(UnsupportedOperationException.class, ex);
    }

    @Test
    void default_type_does_not_support_optional_operations() {
        assertUnsupported(() -> defaultGraphType.vertices());
        assertUnsupported(() -> defaultGraphType.reverse());
        assertUnsupported(() -> defaultGraphType.symmetricConnectedComponents());
        assertUnsupported(() -> defaultGraphType.breakToSymmetricStructure());
        assertUnsupported(() -> defaultGraphType.mirrorToSymmetricStructure());
        assertThrows(NullPointerException.class,
                () -> defaultGraphType.toSymmetricStructure(null));
        assertUnsupported(
                () -> defaultGraphType.toSymmetricStructure(e -> null));
        assertUnsupported(() -> defaultGraphType.isSymmetricStructure());
        assertUnsupported(() -> defaultGraphType.structuralAsymmetries());
        assertThrows(NullPointerException.class,
                () -> defaultGraphType.applyToEdges(null));
        assertUnsupported(() -> defaultGraphType.applyToEdges(e -> null));
        assertThrows(NullPointerException.class,
                () -> defaultGraphType.forEachEdge(null));
        assertUnsupported(() -> defaultGraphType.forEachEdge(e -> {}));
        assertThrows(NullPointerException.class,
                () -> defaultGraphType.deleteEdges(null));
        assertUnsupported(() -> defaultGraphType.deleteEdges(e -> false));
        assertThrows(NullPointerException.class,
                () -> defaultGraphType.preserveEdges(null));
        assertUnsupported(() -> defaultGraphType.preserveEdges(e -> false));
        assertThrows(NullPointerException.class,
                () -> defaultGraphType.preserveVertices(null));
        assertUnsupported(() -> defaultGraphType.preserveVertices(v -> false));
        assertThrows(NullPointerException.class,
                () -> defaultGraphType.deleteVertices(null));
        assertUnsupported(() -> defaultGraphType.deleteVertices(v -> false));
    }

    @Test
    void symmetric_connected_components_empty_graph() {

        HashGraph.Builder<String, Integer> builder = HashGraph.builder();
        Graph<String, Integer> graph = builder.build();
        assertEquals(List.of(), graph.symmetricConnectedComponents());

    }

    @Test
    void symmetric_connected_components_singleton_graph() {

        HashGraph.Builder<String, Integer> builder = HashGraph.builder();
        builder.addVertex("A");
        Graph<String, Integer> graph = builder.build();

        HashGraph.Builder<String, Integer> builderCopy = HashGraph.builder();
        builderCopy.addVertex("A");
        Graph<String, Integer> graphCopy = builderCopy.build();

        assertEquals(List.of(graphCopy), graph.symmetricConnectedComponents());

    }

    @Test
    void symmetric_connected_components_fully_connected_twin() {

        HashGraph.Builder<String, Integer> builder = HashGraph.builder();
        builder.addVertices("A", "B");
        builder.addEdge("A", "B", 42);
        builder.addEdge("B", "A", 100);
        Graph<String, Integer> graph = builder.build();

        HashGraph.Builder<String, Integer> builderCopy = HashGraph.builder();
        builderCopy.addVertices("A", "B");
        builderCopy.addEdge("A", "B", 42);
        builderCopy.addEdge("B", "A", 100);
        Graph<String, Integer> graphCopy = builderCopy.build();

        assertEquals(List.of(graphCopy), graph.symmetricConnectedComponents());

    }

    @Test
    void symmetric_connected_components_fully_connected_twin_a_loop() {

        HashGraph.Builder<String, Integer> builder = HashGraph.builder();
        builder.addVertices("A", "B");
        builder.addEdge("A", "B", 42);
        builder.addEdge("A", "A", 1);
        builder.addEdge("B", "A", 100);
        Graph<String, Integer> graph = builder.build();

        HashGraph.Builder<String, Integer> builderCopy = HashGraph.builder();
        builderCopy.addVertices("A", "B");
        builderCopy.addEdge("A", "B", 42);
        builderCopy.addEdge("A", "A", 1);
        builderCopy.addEdge("B", "A", 100);
        Graph<String, Integer> graphCopy = builderCopy.build();

        assertEquals(List.of(graphCopy), graph.symmetricConnectedComponents());

    }

    @Test
    void symmetric_connected_components_not_symmetric() {

        HashGraph.Builder<String, Integer> builder = HashGraph.builder();
        builder.addVertices("A", "B");
        builder.addEdge("A", "B", 42);
        Graph<String, Integer> graph = builder.build();

        assertThrows(IllegalStateException.class,
                     graph::symmetricConnectedComponents);

    }

    @Test
    void symmetric_connected_components_disconnected_twin() {

        HashGraph.Builder<String, Integer> builder = HashGraph.builder();
        builder.addVertices("A", "B");
        Graph<String, Integer> graph = builder.build();

        HashGraph.Builder<String, Integer> builderA = HashGraph.builder();
        builderA.addVertex("A");
        Graph<String, Integer> graphA = builderA.build();

        HashGraph.Builder<String, Integer> builderB = HashGraph.builder();
        builderB.addVertex("B");
        Graph<String, Integer> graphB = builderB.build();

        assertEquals(Set.of(graphA, graphB),
                     new HashSet<>(graph.symmetricConnectedComponents()));

    }

    @Test
    void symmetric_connected_components_disconnected_twin_self_loop() {

        HashGraph.Builder<String, Integer> builder = HashGraph.builder();
        builder.addVertex("A");
        builder.addEdge("A", "A", 42);
        builder.addVertex("B");
        builder.addEdge("B", "B", 42);
        Graph<String, Integer> graph = builder.build();

        HashGraph.Builder<String, Integer> builderA = HashGraph.builder();
        builderA.addVertex("A");
        builderA.addEdge("A", "A", 42);
        Graph<String, Integer> graphA = builderA.build();

        HashGraph.Builder<String, Integer> builderB = HashGraph.builder();
        builderB.addVertex("B");
        builderB.addEdge("B", "B", 42);
        Graph<String, Integer> graphB = builderB.build();

        assertEquals(Set.of(graphA, graphB),
                     new HashSet<>(graph.symmetricConnectedComponents()));

    }

    @Test
    void two_one() {

        HashGraph.Builder<String, Integer> builder = HashGraph.builder();
        builder.addVertices("A", "B", "C");
        builder.addEdge("A", "B", 1);
        builder.addEdge("B", "A", 1);
        Graph<String, Integer> graph = builder.build();

        HashGraph.Builder<String, Integer> builderAB = HashGraph.builder();
        builderAB.addVertices("A", "B");
        builderAB.addEdge("A", "B", 1);
        builderAB.addEdge("B", "A", 1);
        Graph<String, Integer> graphAB = builderAB.build();

        HashGraph.Builder<String, Integer> builderC = HashGraph.builder();
        builderC.addVertex("C");
        Graph<String, Integer> graphC = builderC.build();

        assertEquals(List.of(graphAB, graphC),
                     graph.symmetricConnectedComponents());

    }

    @Test
    void four_five_one() {

        HashGraph.Builder<String, Integer> builder = HashGraph.builder();
        builder.addVertices("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
        builder.addEdge("A", "B", 7);
        builder.addEdge("B", "A", 9);
        builder.addEdge("B", "C", 8);
        builder.addEdge("C", "B", 2);
        builder.addEdge("D", "C", 2);
        builder.addEdge("C", "D", 1);
        builder.addEdge("E", "F", 3);
        builder.addEdge("F", "E", 2);
        builder.addEdge("F", "G", 2);
        builder.addEdge("G", "F", 7);
        builder.addEdge("H", "G", 4);
        builder.addEdge("G", "H", 2);
        builder.addEdge("H", "F", 1);
        builder.addEdge("F", "H", 2);
        builder.addEdge("H", "J", 7);
        builder.addEdge("J", "H", 3);
        Graph<String, Integer> graph = builder.build();

        HashGraph.Builder<String, Integer> builder4 = HashGraph.builder();
        builder4.addVertices("A", "B", "C", "D");
        builder4.addEdge("A", "B", 7);
        builder4.addEdge("B", "A", 9);
        builder4.addEdge("B", "C", 8);
        builder4.addEdge("C", "B", 2);
        builder4.addEdge("D", "C", 2);
        builder4.addEdge("C", "D", 1);
        Graph<String, Integer> graph4 = builder4.build();

        HashGraph.Builder<String, Integer> builder5 = HashGraph.builder();
        builder5.addVertices("E", "F", "G", "H", "J");
        builder5.addEdge("E", "F", 3);
        builder5.addEdge("F", "E", 2);
        builder5.addEdge("F", "G", 2);
        builder5.addEdge("G", "F", 7);
        builder5.addEdge("H", "G", 4);
        builder5.addEdge("G", "H", 2);
        builder5.addEdge("H", "F", 1);
        builder5.addEdge("F", "H", 2);
        builder5.addEdge("H", "J", 7);
        builder5.addEdge("J", "H", 3);
        Graph<String, Integer> graph5 = builder5.build();

        HashGraph.Builder<String, Integer> builder1 = HashGraph.builder();
        builder1.addVertex("I");
        Graph<String, Integer> graph1 = builder1.build();

        assertEquals(List.of(graph5, graph4, graph1),
                     graph.symmetricConnectedComponents());

    }

    @Test
    void to_symmetric_structure() {

        HashGraph.Builder<String, Integer> builder = HashGraph.builder();
        builder.addVertices("A", "B", "C", "D", "E", "F");
        builder.addEdge("A", "B", 5);
        builder.addEdge("B", "A", 4);
        builder.addEdge("B", "E", 42);
        builder.addEdge("B", "C", -1);
        builder.addEdge("C", "C", -1);
        builder.addEdge("C", "D", 6);
        builder.addEdge("E", "D", -1);
        builder.addEdge("E", "F", -1);
        Graph<String, Integer> graph = builder.build();

        // check list of structural asymmetries
        Set<Graph.Edge<String, Integer>> expected
                = Set.of(new Graph.Edge<>("B", "E", 42),
                         new Graph.Edge<>("B", "C", -1),
                         new Graph.Edge<>("C", "D", 6),
                         new Graph.Edge<>("E", "D", -1),
                         new Graph.Edge<>("E", "F", -1));
        Set<Graph.Edge<String, Integer>> actual
                = new HashSet<>(graph.structuralAsymmetries());
        assertEquals(expected, actual);

        HashGraph.Builder<String, Integer> builder2 = HashGraph.builder();
        builder2.addVertices("A", "B", "C", "D", "E", "F");
        builder2.addEdge("A", "B", 5);
        builder2.addEdge("B", "A", 4);
        builder2.addEdge("B", "E", 42);
        builder2.addEdge("E", "B", 41);
        builder2.addEdge("C", "C", -1);
        builder2.addEdge("C", "D", 6);
        builder2.addEdge("D", "C", 5);
        Graph<String, Integer> graph2 = builder2.build();

        // no structural asymmetries
        assertEquals(List.of(), graph2.structuralAsymmetries());

        // subtract 1 from all mirror edges, or delete original if -1
        assertEquals(graph2,
                graph.toSymmetricStructure(
                        e -> (e.value() == -1) ? null : (e.value() - 1)));

        HashGraph.Builder<String, Integer> builder3 = HashGraph.builder();
        builder3.addVertices("A", "B", "C", "D", "E", "F");
        builder3.addEdge("A", "B", 5);
        builder3.addEdge("B", "A", 4);
        builder3.addEdge("B", "E", 42);
        builder3.addEdge("E", "B", 42);
        builder3.addEdge("B", "C", -1);
        builder3.addEdge("C", "B", -1);
        builder3.addEdge("C", "C", -1);
        builder3.addEdge("C", "D", 6);
        builder3.addEdge("D", "C", 6);
        builder3.addEdge("E", "D", -1);
        builder3.addEdge("D", "E", -1);
        builder3.addEdge("E", "F", -1);
        builder3.addEdge("F", "E", -1);

        Graph<String, Integer> graph3 = builder3.build();

        // mirror edges
        assertEquals(graph3, graph.mirrorToSymmetricStructure());

        // no structural asymmetries
        assertEquals(List.of(), graph3.structuralAsymmetries());

        HashGraph.Builder<String, Integer> builder4 = HashGraph.builder();
        builder4.addVertices("A", "B", "C", "D", "E", "F");
        builder4.addEdge("A", "B", 5);
        builder4.addEdge("B", "A", 4);
        builder4.addEdge("C", "C", -1);
        Graph<String, Integer> graph4 = builder4.build();

        // no structural asymmetries
        assertEquals(List.of(), graph4.structuralAsymmetries());

        // break edges
        assertEquals(graph4, graph.breakToSymmetricStructure());

    }

    @Test
    void same_graph_if_symmetric() {

        HashGraph.Builder<String, Integer> builder = HashGraph.builder();
        builder.addVertices("A", "B", "C", "D", "E", "F");
        builder.addEdge("A", "B", 5);
        builder.addEdge("B", "A", 4);
        builder.addEdge("B", "E", 42);
        builder.addEdge("E", "B", 41);
        builder.addEdge("C", "C", -1);
        builder.addEdge("C", "D", 6);
        builder.addEdge("D", "C", 6);
        Graph<String, Integer> graph = builder.build();

        assertSame(graph, graph.breakToSymmetricStructure());
        assertSame(graph, graph.mirrorToSymmetricStructure());
        assertSame(graph, graph.toSymmetricStructure(e -> e.value() - 1));

    }

    @Test
    void identity_apply_to_edges() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        Function<Graph.Edge<Integer, Double>, Double> map = Graph.Edge::value;

        assertEquals(graph, graph.applyToEdges(map));

    }

    @Test
    void replace_with_from_vertex() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        HashGraph.Builder<Integer, Double> builder2 = HashGraph.builder();
        builder2.addVertices(1, 2, 3, 4, 5);
        builder2.addEdge(1, 2, 1.0);
        builder2.addEdge(2, 3, 2.0);
        builder2.addEdge(3, 2, 3.0);
        builder2.addEdge(4, 3, 4.0);
        builder2.addEdge(4, 5, 4.0);
        builder2.addEdge(5, 4, 5.0);
        Graph<Integer, Double> graph2 = builder2.build();

        Function<Graph.Edge<Integer, Double>, Double> map
                = e -> (double) e.from();

        assertEquals(graph2, graph.applyToEdges(map));

    }

    @Test
    void replace_with_to_vertex() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        HashGraph.Builder<Integer, Double> builder2 = HashGraph.builder();
        builder2.addVertices(1, 2, 3, 4, 5);
        builder2.addEdge(1, 2, 2.0);
        builder2.addEdge(2, 3, 3.0);
        builder2.addEdge(3, 2, 2.0);
        builder2.addEdge(4, 3, 3.0);
        builder2.addEdge(4, 5, 5.0);
        builder2.addEdge(5, 4, 4.0);
        Graph<Integer, Double> graph2 = builder2.build();

        Function<Graph.Edge<Integer, Double>, Double> map
                = e -> (double) e.to();

        assertEquals(graph2, graph.applyToEdges(map));

    }

    @Test
    void replace_with_avg_of_vertices() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        HashGraph.Builder<Integer, Double> builder2 = HashGraph.builder();
        builder2.addVertices(1, 2, 3, 4, 5);
        builder2.addEdge(1, 2, 1.5);
        builder2.addEdge(2, 3, 2.5);
        builder2.addEdge(3, 2, 2.5);
        builder2.addEdge(4, 3, 3.5);
        builder2.addEdge(4, 5, 4.5);
        builder2.addEdge(5, 4, 4.5);
        Graph<Integer, Double> graph2 = builder2.build();

        Function<Graph.Edge<Integer, Double>, Double> map
                = e -> (e.from() + (double) e.to()) / 2.0;

        assertEquals(graph2, graph.applyToEdges(map));

    }

    @Test
    void replace_with_negative() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        HashGraph.Builder<Integer, Double> builder2 = HashGraph.builder();
        builder2.addVertices(1, 2, 3, 4, 5);
        builder2.addEdge(1, 2, -3.0);
        builder2.addEdge(2, 3, -6.0);
        builder2.addEdge(3, 2, -1.0);
        builder2.addEdge(4, 3, -1.0);
        builder2.addEdge(4, 5, -2.0);
        builder2.addEdge(5, 4, -3.0);
        Graph<Integer, Double> graph2 = builder2.build();

        Function<Graph.Edge<Integer, Double>, Double> map = e -> -e.value();

        assertEquals(graph2, graph.applyToEdges(map));

    }

    @Test
    void replace_with_42_if_vertex_sum_gt_5() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        HashGraph.Builder<Integer, Double> builder2 = HashGraph.builder();
        builder2.addVertices(1, 2, 3, 4, 5);
        builder2.addEdge(1, 2, 3.0);
        builder2.addEdge(2, 3, 6.0);
        builder2.addEdge(3, 2, 1.0);
        builder2.addEdge(4, 3, 42.0);
        builder2.addEdge(4, 5, 42.0);
        builder2.addEdge(5, 4, 42.0);
        Graph<Integer, Double> graph2 = builder2.build();

        Function<Graph.Edge<Integer, Double>, Double> map
                = e -> (e.from() + e.to() > 5) ? 42.0 :  e.value();

        assertEquals(graph2, graph.applyToEdges(map));

    }

    @Test
    void delete_if_odd() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        HashGraph.Builder<Integer, Double> builder2 = HashGraph.builder();
        builder2.addVertices(1, 2, 3, 4, 5);
        builder2.addEdge(2, 3, 6.0);
        builder2.addEdge(4, 5, 2.0);
        Graph<Integer, Double> graph2 = builder2.build();

        Function<Graph.Edge<Integer, Double>, Double> map
                = e -> (e.value() % 2 == 0) ? e.value() : null;

        assertEquals(graph2, graph.applyToEdges(map));

    }

    @Test
    void delete_if_odd_as_delete() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        HashGraph.Builder<Integer, Double> builder2 = HashGraph.builder();
        builder2.addVertices(1, 2, 3, 4, 5);
        builder2.addEdge(2, 3, 6.0);
        builder2.addEdge(4, 5, 2.0);
        Graph<Integer, Double> graph2 = builder2.build();

        Predicate<Graph.Edge<Integer, Double>> odd = e -> e.value() % 2 == 1;

        assertEquals(graph2, graph.deleteEdges(odd));

    }

    @Test
    void delete_if_odd_as_preserve() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        HashGraph.Builder<Integer, Double> builder2 = HashGraph.builder();
        builder2.addVertices(1, 2, 3, 4, 5);
        builder2.addEdge(2, 3, 6.0);
        builder2.addEdge(4, 5, 2.0);
        Graph<Integer, Double> graph2 = builder2.build();

        Predicate<Graph.Edge<Integer, Double>> even = e -> e.value() % 2 == 0;

        assertEquals(graph2, graph.preserveEdges(even));

    }

    @Test
    void delete_if_from_is_prime_else_negate() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        HashGraph.Builder<Integer, Double> builder2 = HashGraph.builder();
        builder2.addVertices(1, 2, 3, 4, 5);
        builder2.addEdge(1, 2, -3.0);
        builder2.addEdge(4, 3, -1.0);
        builder2.addEdge(4, 5, -2.0);
        Graph<Integer, Double> graph2 = builder2.build();

        Function<Graph.Edge<Integer, Double>, Double> map
                = e -> (e.from() == 2 || e.from() == 3 || e.from() == 5)
                ? null : -e.value();

        assertEquals(graph2, graph.applyToEdges(map));

    }

    @Test
    void preserve_vertex_234() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        HashGraph.Builder<Integer, Double> builder2 = HashGraph.builder();
        builder2.addVertices(2, 3, 4);
        builder2.addEdge(2, 3, 6.0);
        builder2.addEdge(3, 2, 1.0);
        builder2.addEdge(4, 3, 1.0);
        Graph<Integer, Double> graph2 = builder2.build();

        Predicate<Integer> map = v -> v == 2 || v == 3 || v == 4;

        assertEquals(graph2, graph.preserveVertices(map));

    }

    @Test
    void preserve_vertex_all() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        Predicate<Integer> map = v -> true;

        assertEquals(graph, graph.preserveVertices(map));

    }

    @Test
    void preserve_vertex_none() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        Predicate<Integer> map = v -> false;

        assertEquals(HashGraph.builder().build(), graph.preserveVertices(map));

    }

    @Test
    void preserve_vertex_145() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        HashGraph.Builder<Integer, Double> builder2 = HashGraph.builder();
        builder2.addVertices(1, 4, 5);
        builder2.addEdge(4, 5, 2.0);
        builder2.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph2 = builder2.build();

        Predicate<Integer> map = v -> v == 1 || v == 4 || v == 5;

        assertEquals(graph2, graph.preserveVertices(map));

    }

    @Test
    void delete_vertex_234() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        HashGraph.Builder<Integer, Double> builder2 = HashGraph.builder();
        builder2.addVertices(1, 5);
        Graph<Integer, Double> graph2 = builder2.build();

        Predicate<Integer> map = v -> v == 2 || v == 3 || v == 4;

        assertEquals(graph2, graph.deleteVertices(map));

    }

    @Test
    void delete_vertex_none() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        Predicate<Integer> map = v -> false;

        assertEquals(graph, graph.deleteVertices(map));

    }

    @Test
    void delete_vertex_all() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        Predicate<Integer> map = v -> true;

        assertEquals(HashGraph.builder().build(), graph.deleteVertices(map));

    }

    @Test
    void delete_vertex_145() {

        HashGraph.Builder<Integer, Double> builder = HashGraph.builder();
        builder.addVertices(1, 2, 3, 4, 5);
        builder.addEdge(1, 2, 3.0);
        builder.addEdge(2, 3, 6.0);
        builder.addEdge(3, 2, 1.0);
        builder.addEdge(4, 3, 1.0);
        builder.addEdge(4, 5, 2.0);
        builder.addEdge(5, 4, 3.0);
        Graph<Integer, Double> graph = builder.build();

        HashGraph.Builder<Integer, Double> builder2 = HashGraph.builder();
        builder2.addVertices(2, 3);
        builder2.addEdge(2, 3, 6.0);
        builder2.addEdge(3, 2, 1.0);
        Graph<Integer, Double> graph2 = builder2.build();

        Predicate<Integer> map = v -> v == 1 || v == 4 || v == 5;

        assertEquals(graph2, graph.deleteVertices(map));

    }

}
