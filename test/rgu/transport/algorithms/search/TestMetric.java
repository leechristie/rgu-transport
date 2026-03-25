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

import static org.junit.jupiter.api.Assertions.*;

final class TestMetric {

    @Test
    void metric_heuristic_throws_on_null() {
        Metric<Integer, Integer> metric = (a, b) -> Math.abs(a - b);
        assertThrows(NullPointerException.class, () -> metric.asHeuristic(null));
    }

    @Test
    void metric_heuristic_matches_impl() {
        Metric<Integer, Integer> metric = (a, b) -> Math.abs(a - b);
        assertEquals(10, metric.distance(1234, 1244));
        assertEquals(10, metric.distance(1244, 1234));
        assertEquals(10, metric.asHeuristic(1234).apply(1244));
        assertEquals(10, metric.asHeuristic(1244).apply(1234));
    }

}
