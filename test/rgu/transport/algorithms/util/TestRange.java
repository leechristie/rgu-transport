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

package rgu.transport.algorithms.util;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class TestRange {

    @Test
    void null_checks() {
        Range.closed(5, 10);
        assertThrows(NullPointerException.class,
                () -> Range.closed(5, null));
        assertThrows(NullPointerException.class,
                () -> Range.closed(null, 10));
        assertThrows(NullPointerException.class,
                () -> Range.closed(null, null));
        Range.halfOpen(5, 10);
        assertThrows(NullPointerException.class,
                () -> Range.halfOpen(5, null));
        assertThrows(NullPointerException.class,
                () -> Range.halfOpen(null, 11));
        assertThrows(NullPointerException.class,
                () -> Range.halfOpen(null, null));
    }

    @Test
    void bad_range_checks() {
        Range.closed(5, 7);
        Range.closed(5, 6);
        Range.closed(5, 5);
        assertThrows(IllegalArgumentException.class,
                () -> Range.closed(5, 4));
        assertThrows(IllegalArgumentException.class,
                () -> Range.closed(5, 3));
        Range.halfOpen(5, 8);
        Range.halfOpen(5, 7);
        Range.halfOpen(5, 6);
        assertThrows(IllegalArgumentException.class,
                () -> Range.halfOpen(5, 5));
        assertThrows(IllegalArgumentException.class,
                () -> Range.halfOpen(5, 4));
    }

    @Test
    void closed_range() {
        Range<Integer> fiveToNine = Range.closed(5, 10);
        for (int i = 5; i <= 10; i++) {
            assertTrue(fiveToNine.test(i));
        }
        assertFalse(fiveToNine.test(4));
        assertFalse(fiveToNine.test(11));
        assertFalse(fiveToNine.test(Integer.MIN_VALUE));
        assertFalse(fiveToNine.test(Integer.MAX_VALUE));
        assertFalse(fiveToNine.isAny());
    }

    @Test
    void open_range() {
        Range<Integer> fiveToNine = Range.halfOpen(5, 11);
        for (int i = 5; i <= 10; i++) {
            assertTrue(fiveToNine.test(i));
        }
        assertFalse(fiveToNine.test(4));
        assertFalse(fiveToNine.test(11));
        assertFalse(fiveToNine.test(Integer.MIN_VALUE));
        assertFalse(fiveToNine.test(Integer.MAX_VALUE));
        assertFalse(fiveToNine.isAny());
    }

    @Test
    void any_range() {
        Range<Integer> any = Range.any();
        assertTrue(any.test(Integer.MIN_VALUE));
        assertTrue(any.test(-1));
        assertTrue(any.test(0));
        assertTrue(any.test(1));
        assertTrue(any.test(Integer.MAX_VALUE));
        assertTrue(any.isAny());
    }

    @Test
    void any_intersects() {
        assertTrue(Range.<Integer>any().intersects(Range.any()));
        assertTrue(Range.closed(15, 60).intersects(Range.any()));
        assertTrue(Range.halfOpen(15, 60).intersects(Range.any()));
        assertTrue(Range.<Integer>any().intersects(Range.closed(60, 200)));
        assertTrue(Range.<Integer>any().intersects(Range.halfOpen(60, 200)));
    }

    @Test
    void closed_intersects_closed() {
        assertTrue(Range.closed(15, 60).intersects(Range.closed(59, 200)));
        assertTrue(Range.closed(15, 60).intersects(Range.closed(60, 200)));
        assertFalse(Range.closed(15, 60).intersects(Range.closed(61, 200)));
        assertFalse(Range.closed(15, 60).intersects(Range.closed(62, 200)));
        assertTrue(Range.closed(59, 200).intersects(Range.closed(15, 60)));
        assertTrue(Range.closed(60, 200).intersects(Range.closed(15, 60)));
        assertFalse(Range.closed(61, 200).intersects(Range.closed(15, 60)));
        assertFalse(Range.closed(62, 200).intersects(Range.closed(15, 60)));
    }

    @Test
    void closed_intersects_open() {
        assertTrue(Range.closed(15, 60).intersects(Range.halfOpen(59, 200)));
        assertTrue(Range.closed(15, 60).intersects(Range.halfOpen(60, 200)));
        assertFalse(Range.closed(15, 60).intersects(Range.halfOpen(61, 200)));
        assertFalse(Range.closed(15, 60).intersects(Range.halfOpen(62, 200)));
        assertTrue(Range.halfOpen(59, 200).intersects(Range.closed(15, 60)));
        assertTrue(Range.halfOpen(60, 200).intersects(Range.closed(15, 60)));
        assertFalse(Range.halfOpen(61, 200).intersects(Range.closed(15, 60)));
        assertFalse(Range.halfOpen(62, 200).intersects(Range.closed(15, 60)));
    }

    @Test
    void open_intersects_closed() {
        assertTrue(Range.halfOpen(15, 60).intersects(Range.closed(59, 200)));
        assertFalse(Range.halfOpen(15, 60).intersects(Range.closed(60, 200)));
        assertFalse(Range.halfOpen(15, 60).intersects(Range.closed(61, 200)));
        assertFalse(Range.halfOpen(15, 60).intersects(Range.closed(62, 200)));
        assertTrue(Range.halfOpen(59, 200).intersects(Range.halfOpen(15, 60)));
        assertFalse(Range.halfOpen(60, 200).intersects(Range.halfOpen(15, 60)));
        assertFalse(Range.halfOpen(61, 200).intersects(Range.halfOpen(15, 60)));
        assertFalse(Range.halfOpen(62, 200).intersects(Range.halfOpen(15, 60)));
    }

    @Test
    void open_intersects_open() {
        assertTrue(Range.halfOpen(15, 60).intersects(Range.halfOpen(59, 200)));
        assertFalse(Range.halfOpen(15, 60).intersects(Range.halfOpen(60, 200)));
        assertFalse(Range.halfOpen(15, 60).intersects(Range.halfOpen(61, 200)));
        assertFalse(Range.halfOpen(15, 60).intersects(Range.halfOpen(62, 200)));
        assertTrue(Range.closed(59, 200).intersects(Range.halfOpen(15, 60)));
        assertFalse(Range.closed(60, 200).intersects(Range.halfOpen(15, 60)));
        assertFalse(Range.closed(61, 200).intersects(Range.halfOpen(15, 60)));
        assertFalse(Range.closed(62, 200).intersects(Range.halfOpen(15, 60)));
    }

    @Test
    void intersects_null() {
        assertThrows(NullPointerException.class,
                () -> Range.any().intersects(null));
    }

    @Test
    void to_string() {
        assertEquals("[5, 9]", Range.closed(5, 9).toString());
        assertEquals("[5, 10]", Range.closed(5, 10).toString());
        assertEquals("[4, 9]", Range.closed(4, 9).toString());
        assertEquals("[4, 10]", Range.closed(4, 10).toString());
        assertEquals("[5, 9)", Range.halfOpen(5, 9).toString());
        assertEquals("[5, 10)", Range.halfOpen(5, 10).toString());
        assertEquals("[4, 9)", Range.halfOpen(4, 9).toString());
        assertEquals("[4, 10)", Range.halfOpen(4, 10).toString());
        assertEquals("Range.any()", Range.any().toString());
    }

}
