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
import rgu.transport.algorithms.search.*;

import java.util.*;
import java.util.function.*;

import static java.lang.Math.*;
import static org.junit.jupiter.api.Assertions.*;
import static rgu.transport.algorithms.collections.SampleGraphs.*;
import static rgu.transport.algorithms.search.RoutingAlgorithm.*;

class TestSearch {

    private static final double INVERSE_DELTA = 1000000.0;
    private static final double DELTA = 1.0 / INVERSE_DELTA;

    private Graph<Point, Double> graph1;
    private Graph<Point, Double> reverse1;
    private RoutingAlgorithm<Double> routing1;
    private ReachabilityAlgorithm<Double> reachability1;

    private Graph<String, Integer> graph2;
    private Graph<String, Integer> reverse2;
    private RoutingAlgorithm<Integer> routing2;
    private ReachabilityAlgorithm<Integer> reachability2;

    @BeforeEach
    void sample_graph() {
        graph1 = SampleGraphs.sampleGraph1();
        reverse1 = graph1.reverse();
        graph2 = SampleGraphs.sampleGraph2();
        reverse2 = graph2.reverse();
    }

    @BeforeEach
    void search_algo() {
        Dijkstra<Double> dijkstra1 = Dijkstra.ofDouble();
        routing1 = dijkstra1;
        reachability1 = dijkstra1;
        Dijkstra<Integer> dijkstra2 = Dijkstra.ofInt();
        routing2 = dijkstra2;
        reachability2 = dijkstra2;
    }

    @Test
    void double_triple_reverse() {
        assertNotSame(graph1, graph1.reverse());
        assertSame(graph1, graph1.reverse().reverse());
        assertNotSame(graph1.reverse(), graph1.reverse().reverse());
        assertSame(graph1.reverse(), graph1.reverse().reverse().reverse());
    }

    @Test
    void path() throws TargetUnreachableException, InterruptedException {
        assertEquals(List.of(a, b), routing1.path(graph1, a, b));
        assertEquals(List.of(a, b, c), routing1.path(graph1, a, c));
        assertEquals(List.of(b, c), routing1.path(graph1, b, c));
        assertEquals(List.of(c, b), routing1.path(graph1, c, b));
        assertEquals(List.of(x, y), routing1.path(graph1, x, y));
        assertEquals(List.of(z, y), routing1.path(graph1, z, y));
        assertEquals(List.of(y, z), routing1.path(graph1, y, z));
        assertEquals(List.of(x, y, z), routing1.path(graph1, x, z));
    }

    @Test
    void can_not_path_backwards() {
        assertUnreachable(() -> routing1.path(graph1, b, a));
        assertUnreachable(() -> routing1.path(graph1, c, a));
        assertUnreachable(() -> routing1.path(graph1, y, x));
        assertUnreachable(() -> routing1.path(graph1, z, x));
    }

    @Test
    void can_not_path_cross_components() {
        assertUnreachable(() -> routing1.path(graph1, a, x));
        assertUnreachable(() -> routing1.path(graph1, a, y));
        assertUnreachable(() -> routing1.path(graph1, a, z));
        assertUnreachable(() -> routing1.path(graph1, b, x));
        assertUnreachable(() -> routing1.path(graph1, b, y));
        assertUnreachable(() -> routing1.path(graph1, b, z));
        assertUnreachable(() -> routing1.path(graph1, c, x));
        assertUnreachable(() -> routing1.path(graph1, c, y));
        assertUnreachable(() -> routing1.path(graph1, c, z));
        assertUnreachable(() -> routing1.path(graph1, x, a));
        assertUnreachable(() -> routing1.path(graph1, x, b));
        assertUnreachable(() -> routing1.path(graph1, x, c));
        assertUnreachable(() -> routing1.path(graph1, y, a));
        assertUnreachable(() -> routing1.path(graph1, y, b));
        assertUnreachable(() -> routing1.path(graph1, y, c));
        assertUnreachable(() -> routing1.path(graph1, z, a));
        assertUnreachable(() -> routing1.path(graph1, z, b));
        assertUnreachable(() -> routing1.path(graph1, z, c));
    }

    private void assertUnreachable(Executable ex) {
        assertThrows(TargetUnreachableException.class, ex);
    }

    @Test
    void path_reverse() throws TargetUnreachableException, InterruptedException {
        assertUnreachable(() -> routing1.path(reverse1, a, b));
        assertUnreachable(() -> routing1.path(reverse1, a, c));
        assertEquals(List.of(b, c), routing1.path(reverse1, b, c));
        assertEquals(List.of(c, b), routing1.path(reverse1, c, b));
        assertUnreachable(() -> routing1.path(reverse1, x, y));
        assertEquals(List.of(z, y), routing1.path(reverse1, z, y));
        assertEquals(List.of(y, z), routing1.path(reverse1, y, z));
        assertUnreachable(() -> routing1.path(reverse1, x, z));
        assertEquals(List.of(b, a), routing1.path(reverse1, b, a));
        assertEquals(List.of(c, b, a), routing1.path(reverse1, c, a));
        assertEquals(List.of(y, x), routing1.path(reverse1, y, x));
        assertEquals(List.of(z, y, x), routing1.path(reverse1, z, x));
        assertUnreachable(() -> routing1.path(reverse1, a, x));
        assertUnreachable(() -> routing1.path(reverse1, a, y));
        assertUnreachable(() -> routing1.path(reverse1, a, z));
        assertUnreachable(() -> routing1.path(reverse1, b, x));
        assertUnreachable(() -> routing1.path(reverse1, b, y));
        assertUnreachable(() -> routing1.path(reverse1, b, z));
        assertUnreachable(() -> routing1.path(reverse1, c, x));
        assertUnreachable(() -> routing1.path(reverse1, c, y));
        assertUnreachable(() -> routing1.path(reverse1, c, z));
        assertUnreachable(() -> routing1.path(reverse1, x, a));
        assertUnreachable(() -> routing1.path(reverse1, x, b));
        assertUnreachable(() -> routing1.path(reverse1, x, c));
        assertUnreachable(() -> routing1.path(reverse1, y, a));
        assertUnreachable(() -> routing1.path(reverse1, y, b));
        assertUnreachable(() -> routing1.path(reverse1, y, c));
        assertUnreachable(() -> routing1.path(reverse1, z, a));
        assertUnreachable(() -> routing1.path(reverse1, z, b));
        assertUnreachable(() -> routing1.path(reverse1, z, c));
    }

    @Test
    void path_length()
            throws TargetUnreachableException, InterruptedException {
        assertEquals(1.0,
                     routing1.cost(graph1, routing1.path(graph1, a, b)), DELTA);
        assertEquals(1.0 + sqrt(2.0),
                     routing1.cost(graph1, routing1.path(graph1, a, c)),  DELTA);
        assertEquals(sqrt(2.0),
                     routing1.cost(graph1, routing1.path(graph1, b, c)), DELTA);
        assertEquals(sqrt(2.0),
                     routing1.cost(graph1, routing1.path(graph1, c, b)), DELTA);
        assertEquals(1.0,
                     routing1.cost(graph1, routing1.path(graph1, x, y)), DELTA);
        assertEquals(1.0,
                     routing1.cost(graph1, routing1.path(graph1, z, y)), DELTA);
        assertEquals(1.0,
                     routing1.cost(graph1, routing1.path(graph1, y, z)), DELTA);
        assertEquals(2.0,
                     routing1.cost(graph1, routing1.path(graph1, x, z)), DELTA);
    }

    @Test
    void reachable() throws InterruptedException {
        assertEqualsRounded(Map.of(a, 0.0, b, 1.0, c, 1.0 + sqrt(2.0)),
                            reachability1.reachable(graph1, a));
        assertEqualsRounded(Map.of(b, 0.0, c, sqrt(2.0)),
                            reachability1.reachable(graph1, b));
        assertEqualsRounded(Map.of(b, sqrt(2.0), c, 0.0),
                            reachability1.reachable(graph1, c));
        assertEqualsRounded(Map.of(x, 0.0, y, 1.0, z, 2.0),
                            reachability1.reachable(graph1, x));
        assertEqualsRounded(Map.of(y, 0.0, z, 1.0),
                            reachability1.reachable(graph1, y));
        assertEqualsRounded(Map.of(y, 1.0, z, 0.0),
                            reachability1.reachable(graph1, z));
    }

    @Test
    void reachableLimited() throws InterruptedException {

        // 1.0 + sqrt(2.0) == 2.414214 (6.d.p)
        // - rounding set to 6.d.p. on this test suite via assertEqualsRounded

        assertEqualsRounded(Map.of(a, 0.0, b, 1.0, c, 2.414214),
                reachability1.reachable(graph1, a));

        assertEqualsRounded(Map.of(a, 0.0, b, 1.0, c, 2.414214),
                reachability1.reachable(graph1, a, 2.414215));
        assertEqualsRounded(Map.of(a, 0.0, b, 1.0, c, 2.414214),
                reachability1.reachable(graph1, a, 2.414214)); // just right
        assertEqualsRounded(Map.of(a, 0.0, b, 1.0),
                reachability1.reachable(graph1, a, 2.414213));

        assertEqualsRounded(Map.of(a, 0.0, b, 1.0),
                reachability1.reachable(graph1, a, 1.000001));
        assertEqualsRounded(Map.of(a, 0.0, b, 1.0),
                reachability1.reachable(graph1, a, 1.0)); // just right
        assertEqualsRounded(Map.of(a, 0.0),
                reachability1.reachable(graph1, a, 0.999999));

    }

    private static void assertEqualsRounded(Map<Point, Double> expected,
                                            Map<Point, Double> actual) {
        assertEquals(roundMap(expected), roundMap(actual));
    }

    private static Map<Point, Double> roundMap(Map<Point, Double> map) {
        Map<Point, Double> rv = new HashMap<>();
        for (var entry : map.entrySet()) {
            rv.put(entry.getKey(),
                   round(entry.getValue() * INVERSE_DELTA) / INVERSE_DELTA);
        }
        return rv;
    }

    @Test
    void path_method_source_target()
            throws TargetUnreachableException, InterruptedException {

        assertEquals(List.of("D", "C", "z", "B", "x", "A"),
                routing2.path(graph2, "D", "A"));

        assertEquals(List.of("A", "x", "B", "y", "C"),
                routing2.path(graph2, "A", "C"));

        assertUnreachable(() -> routing2.path(graph2, "A", "D"));

    }

    @Test
    void path_method_source_target_maxcost()
            throws TargetUnreachableException, InterruptedException {

        assertEquals(List.of("D", "C", "z", "B", "x", "A"),
                routing2.path(graph2, "D", "A", 9));
        assertEquals(List.of("D", "C", "z", "B", "x", "A"),
                routing2.path(graph2, "D", "A", 8));
        assertUnreachable(() -> routing2.path(graph2, "D", "A", 7));

        assertEquals(List.of("A", "x", "B", "y", "C"),
                routing2.path(graph2, "A", "C", 6));
        assertEquals(List.of("A", "x", "B", "y", "C"),
                routing2.path(graph2, "A", "C", 5));
        assertUnreachable(() -> routing2.path(graph2, "A", "C", 4));

        assertUnreachable(() -> routing2.path(graph2, "A", "D", 100));

    }

    @Test
    void is_one_of() {
        assertThrows(NullPointerException.class,
                () -> isOneOf((Collection<? extends Object>) null));
        assertThrows(NullPointerException.class,
                () -> isOneOf((Object[]) null));
        assertThrows(NullPointerException.class,
                () -> isOneOf(null, null));
        Predicate<String> ABarr = isOneOf("A", "B");
        assertTrue(ABarr.test("A"));
        assertTrue(ABarr.test("B"));
        assertFalse(ABarr.test("C"));
        Predicate<String> ABlist = isOneOf("A", "B");
        assertTrue(ABlist.test("A"));
        assertTrue(ABlist.test("B"));
        assertFalse(ABlist.test("C"));
    }

    @Test
    void path_method_source_predicate()
            throws TargetUnreachableException, InterruptedException {

        assertUnreachable(() -> routing2.path(graph2, "D", isOneOf()));
        assertUnreachable(() -> routing2.path(graph2, "A", isOneOf()));
        assertUnreachable(() -> routing2.path(graph2, "A", isOneOf()));

        assertEquals(List.of("D", "C", "z", "B", "x", "A"),
                routing2.path(graph2, "D", isOneOf("A")));
        assertEquals(List.of("A", "x", "B", "y", "C"),
                routing2.path(graph2, "A", isOneOf("C")));
        assertUnreachable(() -> routing2.path(graph2, "A", isOneOf("D")));

        assertEquals(List.of("B", "y", "C"),
                routing2.path(graph2, "B", isOneOf("A", "E", "C")));

        assertEquals(List.of("B", "x", "A"),
                routing2.path(graph2, "B", isOneOf("A", "E", "D")));

    }

    @Test
    void path_method_source_predicate_maxcost()
            throws TargetUnreachableException, InterruptedException {

        assertUnreachable(() -> routing2.path(graph2, "D", isOneOf(), 10));
        assertUnreachable(() -> routing2.path(graph2, "A", isOneOf(), 10));
        assertUnreachable(() -> routing2.path(graph2, "A", isOneOf(), 10));

        assertEquals(List.of("D", "C", "z", "B", "x", "A"),
                routing2.path(graph2, "D", isOneOf("A"), 9));
        assertEquals(List.of("D", "C", "z", "B", "x", "A"),
                routing2.path(graph2, "D", isOneOf("A"), 8));
        assertUnreachable(() -> routing2.path(graph2, "D", isOneOf("A"), 7));

        assertEquals(List.of("A", "x", "B", "y", "C"),
                routing2.path(graph2, "A", isOneOf("C"), 6));
        assertEquals(List.of("A", "x", "B", "y", "C"),
                routing2.path(graph2, "A", isOneOf("C"), 5));
        assertUnreachable(() -> routing2.path(graph2, "A", isOneOf("C"), 4));

        assertUnreachable(() -> routing2.path(graph2, "A", isOneOf("D"), 10));

        assertEquals(List.of("B", "y", "C"),
                routing2.path(graph2, "B", isOneOf("A", "E", "C"), 3));
        assertEquals(List.of("B", "y", "C"),
                routing2.path(graph2, "B", isOneOf("A", "E", "C"), 2));
        assertUnreachable(
                () -> routing2.path(graph2, "B", isOneOf("A", "E", "C"), 1));

        assertEquals(List.of("B", "x", "A"),
                routing2.path(graph2, "B", isOneOf("A", "E", "D"), 4));
        assertEquals(List.of("B", "x", "A"),
                routing2.path(graph2, "B", isOneOf("A", "E", "D"), 3));
        assertUnreachable(
                () -> routing2.path(graph2, "B", isOneOf("A", "E", "D"), 2));

    }

    @Test
    void null_check_graph_source_target()
            throws TargetUnreachableException, InterruptedException {

        assertEquals(List.of("D", "C", "z", "B", "x", "A"),
                routing2.path(graph2, "D", "A"));
        assertThrows(NullPointerException.class,
                () -> routing2.path(null, "D", "A"));
        assertThrows(NullPointerException.class,
                () -> routing2.path(graph2, null, "A"));
        assertThrows(NullPointerException.class,
                () -> routing2.path(graph2, "D", (String) null));

    }

    @Test
    void null_check_graph_source_target_maxcost()
            throws TargetUnreachableException, InterruptedException {

        assertEquals(List.of("D", "C", "z", "B", "x", "A"),
                routing2.path(graph2, "D", "A", 100));
        assertThrows(NullPointerException.class,
                () -> routing2.path(null, "D", "A", 100));
        assertThrows(NullPointerException.class,
                () -> routing2.path(graph2, null, "A", 100));
        assertThrows(NullPointerException.class,
                () -> routing2.path(graph2, "D", (String) null, 100));
        assertThrows(NullPointerException.class,
                () -> routing2.path(graph2, "D", "A", null));

    }

    @Test
    void null_check_graph_source_predicate()
            throws TargetUnreachableException, InterruptedException {

        assertEquals(List.of("D", "C", "z", "B", "x", "A"),
                routing2.path(graph2, "D", isOneOf("A")));
        assertThrows(NullPointerException.class,
                () -> routing2.path((Explorable<String, Integer>) null, "D",
                        isOneOf("A")));
        assertThrows(NullPointerException.class,
                () -> routing2.path(graph2, null, isOneOf("A")));
        assertThrows(NullPointerException.class,
                () -> routing2.path(graph2, "D", (Predicate<String>) null));

    }

    @Test
    void null_check_graph_source_predicate_maxcost()
            throws TargetUnreachableException, InterruptedException {

        assertEquals(List.of("D", "C", "z", "B", "x", "A"),
                routing2.path(graph2, "D", isOneOf("A"), 100));
        assertThrows(NullPointerException.class,
                () -> routing2.path((Explorable<String, Integer>) null, "D",
                        isOneOf("A"), 100));
        assertThrows(NullPointerException.class,
                () -> routing2.path(graph2, null, isOneOf("A"), 100));
        assertThrows(NullPointerException.class,
                () -> routing2.path(graph2, "D", (Predicate<String>) null,
                        100));
        assertThrows(NullPointerException.class,
                () -> routing2.path(graph2, "D", isOneOf("A"), null));

    }

}
