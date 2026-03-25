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
import rgu.transport.algorithms.search.*;
import rgu.transport.algorithms.util.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TestFinder {

    private static class IntFinder
            implements NearestFinder<Integer> {
        private final Set<Integer> ints = new HashSet<>();
        private int calls = 0;
        int maxDistance = -1;
        IntFinder(int... ints) {
            for (int i : ints) {
                this.ints.add(i);
            }
        }
        @Override
        public Integer nearest(Integer target) {
            this.calls++;
            int bestDistance = -1;
            int best = 0;
            for (int other : ints) {
                int distance = Math.abs(target - other);
                if (maxDistance == -1 || distance <= maxDistance) {
                    if (bestDistance == -1 || distance < bestDistance) {
                        best = other;
                        bestDistance = distance;
                    }
                }
            }
            if (bestDistance == -1) {
                throw new NoSuchElementException("no nearest found for " + target);
            }
            return best;
        }
    }

    private static class DummyListener
            implements ProgressListener {
        int onNewStageCount = 0;
        int onUpdateProgressCount = 0;
        int onCompletionCount = 0;
        int setCancelActionCount = 0;
        Set<Double> progressNumbers = new HashSet<>();
        String stage = null;
        public void onNewStage(String description) {
            stage = description;
            onNewStageCount++;
        }
        public void onUpdateProgress(double progress) {
            progressNumbers.add(progress);
            onUpdateProgressCount++;
        }
        public void onCompletion() {
            onCompletionCount++;
        }
        public void setCancelAction(Runnable action) {
            setCancelActionCount++;
        }
    }

    @Test
    void int_finder_calls_nearest_each_time() {
        IntFinder finder = new IntFinder(10, 15, 30);
        for (int i = 0; i < 2; i++) {
            assertEquals(10, finder.nearest(9));
            assertEquals(10, finder.nearest(10));
            assertEquals(10, finder.nearest(11));
            assertEquals(15, finder.nearest(14));
            assertEquals(15, finder.nearest(15));
            assertEquals(15, finder.nearest(16));
            assertEquals(30, finder.nearest(26));
            assertEquals(30, finder.nearest(29));
            assertEquals(30, finder.nearest(30));
            assertEquals(30, finder.nearest(31));
        }
        assertEquals(20, finder.calls);
    }

    @Test
    void int_finder_multiple_find() throws InterruptedException {
        IntFinder finder = new IntFinder(10, 15, 30);
        Map<Integer, Integer> expected
                = Map.of(9, 10, 15, 15, 16, 15, 30, 30, 31, 30);
        assertEquals(expected, finder.nearest(List.of(9, 16, 31, 30, 15)));
        assertEquals(expected, finder.nearest(List.of(9, 16, 31, 30, 15), ProgressListener.none(), false));
        assertEquals(expected, finder.nearest(List.of(9, 16, 31, 30, 15), ProgressListener.none(), true));
    }

    @Test
    void listener_called() throws InterruptedException {
        IntFinder finder = new IntFinder(10, 15, 30);
        DummyListener listener = new DummyListener();
        finder.nearest(List.of(9, 16, 31, 30, 15), listener, false);
        assertEquals("Finding nearest points...", listener.stage);
        assertEquals(Set.of(0.0, 0.2, 0.4, 0.6, 0.8, 1.0), listener.progressNumbers);
        assertEquals(1, listener.onNewStageCount);
        assertEquals(6, listener.onUpdateProgressCount);
        assertEquals(1, listener.onCompletionCount);
        assertEquals(0, listener.setCancelActionCount);
    }

    @Test
    void listener_called_substage() throws InterruptedException {
        IntFinder finder = new IntFinder(10, 15, 30);
        DummyListener listener = new DummyListener();
        finder.nearest(List.of(9, 16, 31, 30, 15), listener, true);
        assertEquals(Set.of(0.0, 0.2, 0.4, 0.6, 0.8, 1.0), listener.progressNumbers);
        assertEquals(0, listener.onNewStageCount);
        assertEquals(6, listener.onUpdateProgressCount);
        assertEquals(0, listener.onCompletionCount);
        assertEquals(0, listener.setCancelActionCount);
    }

    @Test
    void cache_reduces_calls() {
        IntFinder finder = new IntFinder(10, 15, 30);
        NearestFinder<Integer> cache = finder.cache();
        for (int i = 0; i < 2; i++) {
            assertEquals(10, cache.nearest(9));
            assertEquals(10, cache.nearest(10));
            assertEquals(10, cache.nearest(11));
            assertEquals(15, cache.nearest(14));
            assertEquals(15, cache.nearest(15));
            assertEquals(15, cache.nearest(16));
        }
        assertEquals(6, finder.calls);
    }

    @Test
    void cache_reduces_calls_plus_precomp() {
        IntFinder finder = new IntFinder(10, 15, 30);
        NearestFinder<Integer> cache = finder.cache(Map.of(11, 42, 15, 42, 100, 42));
        for (int i = 0; i < 2; i++) {
            assertEquals(10, cache.nearest(9));
            assertEquals(10, cache.nearest(10));
            assertEquals(42, cache.nearest(11)); // in map
            assertEquals(15, cache.nearest(14));
            assertEquals(42, cache.nearest(15)); // in map
            assertEquals(15, cache.nearest(16));
        }
        assertEquals(4, finder.calls); // 2 of 3 in map used
    }

    @Test
    void concurrent_cache_reduces_calls() {
        IntFinder finder = new IntFinder(10, 15, 30);
        NearestFinder<Integer> cache = finder.concurrentCache();
        for (int i = 0; i < 2; i++) {
            assertEquals(10, cache.nearest(9));
            assertEquals(10, cache.nearest(10));
            assertEquals(10, cache.nearest(11));
            assertEquals(15, cache.nearest(14));
            assertEquals(15, cache.nearest(15));
            assertEquals(15, cache.nearest(16));
        }
        assertEquals(6, finder.calls);
    }

    @Test
    void concurrent_cache_reduces_calls_plus_precomp() {
        IntFinder finder = new IntFinder(10, 15, 30);
        NearestFinder<Integer> cache = finder.concurrentCache(Map.of(11, 42, 15, 42, 100, 42));
        for (int i = 0; i < 2; i++) {
            assertEquals(10, cache.nearest(9));
            assertEquals(10, cache.nearest(10));
            assertEquals(42, cache.nearest(11)); // in map
            assertEquals(15, cache.nearest(14));
            assertEquals(42, cache.nearest(15)); // in map
            assertEquals(15, cache.nearest(16));
        }
        assertEquals(4, finder.calls); // 2 of 3 in map used
    }

    @Test
    void cache_wrap_to_self() {
        IntFinder finder = new IntFinder(10, 15, 30);
        NearestFinder<Integer> cache = finder.cache();
        assertSame(cache, cache.cache());
        NearestFinder<Integer> concurrent = finder.concurrentCache();
        assertSame(concurrent, concurrent.cache());
        assertSame(concurrent, concurrent.concurrentCache());
    }

    @Test
    void cache_upgrade_to_concurrent_cache() {
        IntFinder finder = new IntFinder(10, 15, 30);
        NearestFinder<Integer> cache = finder.cache(Map.of(11, 42, 15, 42, 100, 42));
        assertEquals(10, cache.nearest(9));
        assertEquals(10, cache.nearest(10)); // in map
        assertEquals(42, cache.nearest(11));
        assertEquals(2, finder.calls);
        NearestFinder<Integer> concurrent = cache.concurrentCache();
        assertEquals(10, concurrent.nearest(9));
        assertEquals(10, concurrent.nearest(10)); // in map
        assertEquals(42, concurrent.nearest(11));
        assertEquals(15, concurrent.nearest(14));
        assertEquals(42, concurrent.nearest(15)); // in map
        assertEquals(15, concurrent.nearest(16));
        assertEquals(4, finder.calls);
    }

    @Test
    void cache_factory_null_check() {
        IntFinder finder = new IntFinder(10, 15, 30);
        assertThrows(NullPointerException.class, () -> finder.cache(null));
        assertThrows(NullPointerException.class, () -> finder.concurrentCache(null));
    }

    @Test
    void limit_causes_throw_or_empty() {
        IntFinder finder = new IntFinder(10, 15, 30);
        finder.maxDistance = 10;
        assertEquals(30, finder.nearest(39));
        assertEquals(30, finder.nearest(40));
        assertThrows(NoSuchElementException.class, () -> finder.nearest(41));
        assertEquals(Optional.empty(), finder.nearestOrEmpty(41));
    }

    @Test
    void limit_excluded_from_map() throws InterruptedException {
        IntFinder finder = new IntFinder(10, 15, 30);
        finder.maxDistance = 10;
        assertEquals(Map.of(39, 30, 40, 30), finder.nearest(List.of(39, 41, 40)));
    }

    @Test
    void on_limit_cache_does_not_redo_no_such_element() {
        IntFinder finder = new IntFinder(10, 15, 30);
        finder.maxDistance = 10;
        NearestFinder<Integer> cache = finder.cache();
        assertEquals(Optional.of(30), cache.nearestOrEmpty(39));
        assertEquals(Optional.of(30), cache.nearestOrEmpty(40));
        assertThrows(NoSuchElementException.class, () -> cache.nearest(41));
        assertEquals(Optional.of(30), cache.nearestOrEmpty(39));
        assertEquals(Optional.of(30), cache.nearestOrEmpty(40));
        assertThrows(NoSuchElementException.class, () -> cache.nearest(41));
        assertEquals(3, finder.calls);
    }

    @Test
    void on_limit_concurrent_cache_does_not_redo_no_such_element() {
        IntFinder finder = new IntFinder(10, 15, 30);
        finder.maxDistance = 10;
        NearestFinder<Integer> cache = finder.concurrentCache();
        assertEquals(Optional.of(30), cache.nearestOrEmpty(39));
        assertEquals(Optional.of(30), cache.nearestOrEmpty(40));
        assertThrows(NoSuchElementException.class, () -> cache.nearest(41));
        assertEquals(Optional.of(30), cache.nearestOrEmpty(39));
        assertEquals(Optional.of(30), cache.nearestOrEmpty(40));
        assertThrows(NoSuchElementException.class, () -> cache.nearest(41));
        assertEquals(3, finder.calls);
    }

    @Test
    void on_limit_cache_does_not_redo_no_such_element_collection() throws InterruptedException {
        IntFinder finder = new IntFinder(10, 15, 30);
        finder.maxDistance = 10;
        NearestFinder<Integer> cache = finder.cache();
        cache.nearest(List.of(39, 40, 41));
        cache.nearest(List.of(39, 40, 41));
        assertEquals(3, finder.calls);
    }

    @Test
    void on_limit_concurrent_cache_does_not_redo_no_such_element_collection() throws InterruptedException {
        IntFinder finder = new IntFinder(10, 15, 30);
        finder.maxDistance = 10;
        NearestFinder<Integer> cache = finder.concurrentCache();
        cache.nearest(List.of(39, 40, 41));
        cache.nearest(List.of(39, 40, 41));
        assertEquals(3, finder.calls);
    }

    @Test
    void check_bug_before_v4_double_call_in_collection() throws InterruptedException {
        IntFinder finder = new IntFinder(10, 15, 30);
        finder.nearest(List.of(1, 2, 3, 4, 5));
        assertEquals(5, finder.calls);
    }

}
