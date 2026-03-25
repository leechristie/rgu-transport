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

import java.math.*;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

final class TestMinHeap {

    private BinaryMinHeap<String, Integer> siHeap;

    @BeforeEach
    void set_up() {
        siHeap = new BinaryMinHeap<>((a, b) -> a < b);
    }

    @Test
    void generic_construction_supported() {
        MinHeap<String, Integer> one = new BinaryMinHeap<>((a, b) -> a < b);
        MinHeap<BigInteger, LocalTime> two = new BinaryMinHeap<>(
                (a, b) -> a.isBefore(b));
        assertNotNull(one);
        assertNotNull(two);
    }

    @Test
    void new_throws_on_null() {
        assertThrows(NullPointerException.class,
                    () -> new BinaryMinHeap<>(null));
    }

    @Test
    void new_heap_is_empty() {
        assertTrue(siHeap.isEmpty());
    }

    @Test
    void after_add_heap_is_not_empty() {
        siHeap.addElement("hello", 0);
        assertFalse(siHeap.isEmpty());
    }

    @Test
    void after_add_and_remove_heap_is_empty() {
        siHeap.addElement("hello", 0);
        siHeap.deleteMinimum();
        assertTrue(siHeap.isEmpty());
    }

    @Test
    void remove_throws_on_empty_heap() {
        assertThrows(NoSuchElementException.class,
                () -> siHeap.deleteMinimum());
    }

    @Test
    void remove_returns_element_on_singleton_heap() {
        siHeap.addElement("hello", 0);
        assertEquals("hello", siHeap.deleteMinimum());
    }

    @Test
    void add_throws_on_null() {
        assertThrows(NullPointerException.class,
                     () -> siHeap.addElement(null, 0));
        assertThrows(NullPointerException.class,
                     () -> siHeap.addElement("", null));
        assertThrows(NullPointerException.class,
                     () -> siHeap.addElement(null, null));
    }

    @Test
    void add_throws_on_duplicate() {
        siHeap.addElement("hi", 0);
        assertThrows(IllegalStateException.class,
                     () -> siHeap.addElement("hi", 0));
        assertThrows(IllegalStateException.class,
                     () -> siHeap.addElement("hi", 42));
    }

    @Test
    void elements_added_in_order_remove_in_order() {
        siHeap.addElement("foo", 42);
        siHeap.addElement("bar", 100);
        siHeap.addElement("baz", 1000);
        assertEquals("foo", siHeap.deleteMinimum());
        assertEquals("bar", siHeap.deleteMinimum());
        assertEquals("baz", siHeap.deleteMinimum());
    }

    @Test
    void elements_added_in_reverse_order_remove_in_order() {
        siHeap.addElement("baz", 1000);
        siHeap.addElement("bar", 100);
        siHeap.addElement("foo", 42);
        assertEquals("foo", siHeap.deleteMinimum());
        assertEquals("bar", siHeap.deleteMinimum());
        assertEquals("baz", siHeap.deleteMinimum());
    }

    @Test
    void decrease_key_throws_on_null() {
        assertThrows(NullPointerException.class,
                     () -> siHeap.decreaseKey(null, 0));
        assertThrows(NullPointerException.class,
                     () -> siHeap.decreaseKey("", null));
        assertThrows(NullPointerException.class,
                     () -> siHeap.decreaseKey(null, null));
    }

    @Test
    void decrease_key_throws_with_non_existent_element() {
        assertThrows(NoSuchElementException.class,
                     () -> siHeap.decreaseKey("bar", 101));
    }

    @Test
    void decrease_key_throws_with_non_smaller_key() {
        siHeap.addElement("baz", 1000);
        siHeap.addElement("bar", 100);
        siHeap.addElement("foo", 42);
        assertThrows(IllegalStateException.class,
                     () -> siHeap.decreaseKey("bar", 101));
        assertThrows(IllegalStateException.class,
                     () -> siHeap.decreaseKey("foo", 42));
    }

    @Test
    void decrease_key_in_same_order_does_not_affect_remove() {
        siHeap.addElement("baz", 1000);
        siHeap.addElement("bar", 100);
        siHeap.addElement("foo", 42);
        siHeap.decreaseKey("bar", 43);
        assertEquals("foo", siHeap.deleteMinimum());
        assertEquals("bar", siHeap.deleteMinimum());
        assertEquals("baz", siHeap.deleteMinimum());
    }

    @Test
    void decrease_key_can_switch_order() {
        siHeap.addElement("baz", 1000);
        siHeap.addElement("bar", 100);
        siHeap.addElement("foo", 42);
        siHeap.decreaseKey("bar", 41);
        assertEquals("bar", siHeap.deleteMinimum());
        assertEquals("foo", siHeap.deleteMinimum());
        assertEquals("baz", siHeap.deleteMinimum());
    }

    @Test
    void decrease_key_and_remove_can_be_interleaved() {
        siHeap.addElement("a", 10);
        siHeap.addElement("b", 20);
        siHeap.addElement("c", 30);
        siHeap.addElement("d", 40);
        siHeap.addElement("e", 50);
        siHeap.addElement("f", 60);
        assertEquals("a", siHeap.deleteMinimum());
        siHeap.decreaseKey("d", 15);
        siHeap.decreaseKey("f", 55);
        siHeap.decreaseKey("c", 19);
        assertEquals("d", siHeap.deleteMinimum());
        siHeap.decreaseKey("e", 5);
        siHeap.decreaseKey("c", 4);
        assertEquals("c", siHeap.deleteMinimum());
        assertEquals("e", siHeap.deleteMinimum());
        siHeap.decreaseKey("f", 10);
        assertEquals("f", siHeap.deleteMinimum());
        assertEquals("b", siHeap.deleteMinimum());
        assertTrue(siHeap.isEmpty());
    }

    @Test
    void decrease_key_and_remove_add_with_re_add_can_be_interleaved() {
        siHeap.addElement("a", 10);
        siHeap.addElement("b", 20);
        siHeap.addElement("c", 30);
        siHeap.addElement("d", 40);
        siHeap.addElement("e", 50);
        assertEquals("a", siHeap.deleteMinimum());
        siHeap.addElement("f", 60);
        siHeap.decreaseKey("c", 10);
        siHeap.addElement("g", 70);
        siHeap.addElement("h", 80);
        assertEquals("c", siHeap.deleteMinimum());
        siHeap.addElement("a", 90);
        assertEquals("b", siHeap.deleteMinimum());
        siHeap.decreaseKey("a", 5);
        assertEquals("a", siHeap.deleteMinimum());
        assertEquals("d", siHeap.deleteMinimum());
        assertEquals("e", siHeap.deleteMinimum());
        assertEquals("f", siHeap.deleteMinimum());
        assertEquals("g", siHeap.deleteMinimum());
        assertEquals("h", siHeap.deleteMinimum());
        assertTrue(siHeap.isEmpty());
    }

    @Test
    void to_string_on_empty_heap_gives_empty_braces() {
        assertEquals("{}", siHeap.toString());
    }

    @Test
    void to_string_on_heap_gives_csv() {
        siHeap.addElement("baz", 1000);
        siHeap.addElement("bar", 100);
        siHeap.addElement("foo", 42);
        siHeap.decreaseKey("bar", 41);
        Set<String> expected = Set.of("41:bar", "42:foo", "1000:baz");
        String str = siHeap.toString();
        assertEquals('{', str.charAt(0));
        assertEquals('}', str.charAt(str.length()-1));
        str = str.substring(1, str.length()-1);
        Set<String> actual = Set.of(str.split(", "));
        assertEquals(expected, actual);
    }

}
