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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TestReachabilityCallback {

    @Test
    void throws_on_nulls() {
        assertThrows(NullPointerException.class,
                () -> ReachabilityCallback.from(null));
        assertThrows(NullPointerException.class,
                () -> ReachabilityCallback.from(null, null));
        assertThrows(NullPointerException.class,
                () -> ReachabilityCallback.from((x, y) -> {}, null));
        assertThrows(NullPointerException.class,
                () -> ReachabilityCallback.from(null, (x, y) -> true));
    }

    @Test
    void no_predicate_version_returns_continue() {
        ReachabilityCallback<String, Integer> callback
                = ReachabilityCallback.from((s, i) -> {});
        assertEquals(ReachabilityCallback.Action.CONTINUE,
                callback.consume("x", 1));
        assertEquals(ReachabilityCallback.Action.CONTINUE,
                callback.consume("y", 2));
        assertEquals(ReachabilityCallback.Action.CONTINUE,
                callback.consume("z", 0));
    }

    @Test
    void predicate_version_returns_predicate_result() {
        ReachabilityCallback<String, Integer> callback
                = ReachabilityCallback.from((s, i) -> {}, (s, i) -> i % 3 == 0);
        assertEquals(ReachabilityCallback.Action.CONTINUE,
                callback.consume("x", 1));
        assertEquals(ReachabilityCallback.Action.CONTINUE,
                callback.consume("y", 2));
        assertEquals(ReachabilityCallback.Action.BREAK,
                callback.consume("z", 0));
        assertEquals(ReachabilityCallback.Action.BREAK,
                callback.consume("x", 3));
        assertEquals(ReachabilityCallback.Action.CONTINUE,
                callback.consume("y", 4));
        assertEquals(ReachabilityCallback.Action.CONTINUE,
                callback.consume("z", -1));
        assertEquals(ReachabilityCallback.Action.BREAK,
                callback.consume("x", 33));
        assertEquals(ReachabilityCallback.Action.BREAK,
                callback.consume("y", 900));
        assertEquals(ReachabilityCallback.Action.BREAK,
                callback.consume("z", -6));
    }

    @Test
    void no_predicate_version_calls_consumer() {
        List<String> calls = new ArrayList<>();
        ReachabilityCallback<String, Integer> callback
                = ReachabilityCallback.from((s, i) ->
                calls.add(s + "," + i));
        assertEquals(List.of(), calls);
        callback.consume("x", 1);
        assertEquals(List.of("x,1"), calls);
        callback.consume("y", 2);
        assertEquals(List.of("x,1", "y,2"), calls);
        callback.consume("z", 0);
        assertEquals(List.of("x,1", "y,2", "z,0"), calls);
    }

    @Test
    void predicate_version_calls_both() {
        List<String> calls = new ArrayList<>();
        List<String> calls2 = new ArrayList<>();
        ReachabilityCallback<String, Integer> callback
                = ReachabilityCallback.from((s, i) ->
                calls.add(s + "," + i), (s, i) -> {
                    calls2.add(s + "," + i);
                    return true;
                });
        assertEquals(List.of(), calls);
        assertEquals(List.of(), calls2);
        callback.consume("x", 1);
        assertEquals(List.of("x,1"), calls);
        assertEquals(List.of("x,1"), calls2);
        callback.consume("y", 2);
        assertEquals(List.of("x,1", "y,2"), calls);
        assertEquals(List.of("x,1", "y,2"), calls2);
        callback.consume("z", 0);
        assertEquals(List.of("x,1", "y,2", "z,0"), calls);
        assertEquals(List.of("x,1", "y,2", "z,0"), calls2);
    }

}
