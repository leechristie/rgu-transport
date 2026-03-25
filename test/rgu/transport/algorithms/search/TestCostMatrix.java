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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.*;
import rgu.transport.algorithms.collections.*;
import rgu.transport.algorithms.util.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TestCostMatrix {

    private <T> void assertThrowsWithExceptionText(
            Class<T> expectedType, String substring, Executable executable) {
        assertThrows(NoSuchElementException.class, executable);
        try {
            executable.execute();
            fail("expected exception");
        } catch (Throwable ex) {
            if (expectedType.isInstance(ex)) {
                if (!ex.getMessage().contains(substring)) {
                    fail("missing substring: \"" + substring
                            + "\"\nexception message: \"" + ex + "\"");
                }
            } else {
                assertEquals(expectedType, ex.getClass());
            }
        }
    }

    @Test
    void empty_matrix_from_empty_graph() throws InterruptedException {
        HashGraph.Builder<String, Integer> builder = HashGraph.builder();
        Graph<String, Integer> graph = builder.build();
        Set<String> targets = Set.of();
        Dijkstra<Integer> dijkstra = Dijkstra.ofInt();
        CostMatrix<String, Integer> matrix
                = CostMatrix.construct(graph, targets, dijkstra);
        assertEquals(Set.of(), matrix.vertices());
        assertFalse(matrix.contains("missing_a"));
        assertFalse(matrix.contains("missing_b"));
        assertThrowsWithExceptionText(NoSuchElementException.class,
                "missing_",
                () -> matrix.cost("missing_a", "missing_b"));
        assertThrowsWithExceptionText(NoSuchElementException.class,
                "missing_",
                () -> matrix.cost("missing_a",
                                  "missing_b",
                                  Integer.MAX_VALUE));
        assertThrowsWithExceptionText(NoSuchElementException.class,
                "missing_",
                () -> matrix.isReachable("missing_a", "missing_b"));
    }

    @Test
    void empty_graph_can_not_construct() {
        HashGraph.Builder<String, Integer> builder = HashGraph.builder();
        Graph<String, Integer> graph = builder.build();
        Set<String> targets = Set.of("missing_a", "missing_b");
        Dijkstra<Integer> dijkstra = Dijkstra.ofInt();
        assertThrowsWithExceptionText(NoSuchElementException.class,
                "missing_",
                () -> CostMatrix.construct(graph, targets, dijkstra));
        assertThrowsWithExceptionText(NoSuchElementException.class,
                "missing_",
                () -> CostMatrix.construct(graph, targets, dijkstra,
                        ProgressListener.none()));
        assertThrowsWithExceptionText(NoSuchElementException.class,
                "missing_",
                () -> CostMatrix.construct(graph, targets, dijkstra,
                        ProgressListener.none(), true));
    }

    private final Set<String> vertices = Set.of("A", "B", "C", "D", "E");
    private final ReachabilityAlgorithm<Integer> algorithm = Dijkstra.ofInt();

    private Graph<String, Integer> test;
    private CostMatrix<String, Integer> matrix;

    void createGraph() {
        test = SampleGraphs.sampleGraph2();
    }

    @Test
    void null_element_can_not_construct() {
        createGraph();
        Set<String> withNull = new HashSet<>();
        withNull.add("A");
        withNull.add(null);
        withNull.add("B");
        assertThrows(NullPointerException.class,
                () -> CostMatrix.construct(test, withNull, algorithm));
    }

    void createMatrix() {
        createGraph();
        try {
            matrix = CostMatrix.construct(test, vertices, algorithm);
            // check called with progress listener return
            assertNotNull(CostMatrix.construct(test, vertices, algorithm,
                                               ProgressListener.none()));
            assertNotNull(CostMatrix.construct(test, vertices, algorithm,
                                               ProgressListener.none(), true));
        } catch (InterruptedException e) {
            fail(e);
        }
    }

    @Test
    void has_given_vertices() {
        createMatrix();
        assertEquals(vertices, matrix.vertices());
    }

    @Test
    void individual_vertices_contained() {
        createMatrix();
        assertTrue(matrix.contains("A"));
        assertTrue(matrix.contains("B"));
        assertTrue(matrix.contains("C"));
        assertTrue(matrix.contains("D"));
        assertTrue(matrix.contains("E"));
        assertFalse(matrix.contains("x"));
        assertFalse(matrix.contains("y"));
        assertFalse(matrix.contains("z"));
        assertFalse(matrix.contains("missing"));
        assertThrows(NullPointerException.class, () -> matrix.contains(null));
    }

    @Test
    void boolean_reachable_preserved() {
        createMatrix();
        // scc {D, E} can reach scc {A, B, C}
        for (String from : List.of("A", "B", "C")) {
            for (String to : List.of("A", "B", "C")) {
                assertTrue(matrix.isReachable(from, to));
            }
            for (String to : List.of("D", "E")) {
                assertFalse(matrix.isReachable(from, to));
            }
        }
        for (String from : List.of("D", "E")) {
            for (String to : vertices) {
                assertTrue(matrix.isReachable(from, to));
            }
        }
    }

    @Test
    void throws_if_unreachable() {
        createMatrix();
        for (String from : List.of("A", "B", "C")) {
            for (String to : List.of("D", "E")) {
                assertThrows(TargetUnreachableException.class,
                        () -> matrix.cost(from, to));
            }
        }
    }

    @Test
    void returns_infinity_if_unreachable() {
        createMatrix();
        Integer infinity1 = 123456;
        Integer infinity2 = -1;
        Integer infinity3 = Integer.MAX_VALUE;
        for (String from : List.of("A", "B", "C")) {
            for (String to : List.of("D", "E")) {
                assertSame(infinity1, matrix.cost(from, to, infinity1));
                assertSame(infinity2, matrix.cost(from, to, infinity2));
                assertSame(infinity3, matrix.cost(from, to, infinity3));
            }
        }
    }

    @Test
    void returns_zero_on_diagonal()
            throws TargetUnreachableException {
        createMatrix();
        for (String vertex : vertices) {
            assertEquals(0, matrix.cost(vertex, vertex));
        }
    }

    @Test
    void returns_shortest_path_costs()
            throws TargetUnreachableException {
        createMatrix();
        assertEquals(3, matrix.cost("A", "B"));
        assertEquals(5, matrix.cost("A", "C"));
        assertEquals(3, matrix.cost("B", "A"));
        assertEquals(2, matrix.cost("B", "C"));
        assertEquals(7, matrix.cost("C", "A"));
        assertEquals(4, matrix.cost("C", "B"));
        assertEquals(8, matrix.cost("D", "A"));
        assertEquals(5, matrix.cost("D", "B"));
        assertEquals(1, matrix.cost("D", "C"));
        assertEquals(2, matrix.cost("D", "E"));
        assertEquals(10, matrix.cost("E", "A"));
        assertEquals(7, matrix.cost("E", "B"));
        assertEquals(3, matrix.cost("E", "C"));
        assertEquals(2, matrix.cost("E", "D"));
    }

    @Test
    void construct_calls_listener() throws InterruptedException {
        createGraph();
        MockProgressListener listener = new MockProgressListener();
        CostMatrix.construct(test, vertices, algorithm, listener);
        assertEquals("Constructing cost matrix...", listener.description);
        assertEquals(1.0, listener.progress);
        assertNull(listener.action);
        assertTrue(listener.done);
        assertEquals(1, listener.onNewStageCalls);
        assertEquals(6, listener.onUpdateProgressCalls);
        assertEquals(1, listener.onCompletionCalls);
        assertEquals(0, listener.setCancelActionCalls);
        assertEquals(List.of(0.0, 0.2, 0.4, 0.6, 0.8, 1.0),
                listener.progressHistory);
    }

    @Test
    void construct_calls_listener_false_substage()
            throws InterruptedException {
        createGraph();
        MockProgressListener listener = new MockProgressListener();
        CostMatrix.construct(test, vertices, algorithm, listener, false);
        assertEquals("Constructing cost matrix...", listener.description);
        assertEquals(1.0, listener.progress);
        assertNull(listener.action);
        assertTrue(listener.done);
        assertEquals(1, listener.onNewStageCalls);
        assertEquals(6, listener.onUpdateProgressCalls);
        assertEquals(1, listener.onCompletionCalls);
        assertEquals(0, listener.setCancelActionCalls);
        assertEquals(List.of(0.0, 0.2, 0.4, 0.6, 0.8, 1.0),
                listener.progressHistory);
    }

    @Test
    void construct_calls_listener_true_substage()
            throws InterruptedException {
        createGraph();
        MockProgressListener listener = new MockProgressListener();
        CostMatrix.construct(test, vertices, algorithm, listener, true);
        assertNull(null, listener.description);
        assertEquals(1.0, listener.progress);
        assertNull(listener.action);
        assertFalse(listener.done);
        assertEquals(0, listener.onNewStageCalls);
        assertEquals(6, listener.onUpdateProgressCalls);
        assertEquals(0, listener.onCompletionCalls);
        assertEquals(0, listener.setCancelActionCalls);
        assertEquals(List.of(0.0, 0.2, 0.4, 0.6, 0.8, 1.0),
                listener.progressHistory);
    }

}
