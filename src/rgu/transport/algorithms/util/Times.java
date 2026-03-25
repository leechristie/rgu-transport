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

import java.time.*;
import java.util.*;
import java.util.function.*;

/**
 * Utility class for working with time.
 *
 * @author Lee A. Christie
 */
public final class Times {

    private Times() {
        throw new AssertionError("utility class constructor");
    }

    /**
     * A predicate which always returns true.
     *
     * @param <T> any type
     * @return a predicate which returns true
     */
    public static <T> Predicate<T> all() {
        return t -> true;
    }

    private static final Predicate<LocalTime> ALWAYS = (time) -> {
        Objects.requireNonNull(time, "time");
        return true;
    };

    private static final Predicate<LocalTime> NEVER = (time) -> {
        Objects.requireNonNull(time, "time");
        return false;
    };

    /**
     * Returns a predicate which accepts all times.
     *
     * @return a predicate which accepts all times
     */
    public static Predicate<LocalTime> always() {
        return ALWAYS;
    }

    /**
     * Returns a predicate which rejects all times.
     *
     * @return a predicate which rejects all times
     */
    public static Predicate<LocalTime> never() {
        return NEVER;
    }

    /**
     * Returns a predicate which accepts times between two give times, inclusive.
     *
     * @param start the start time, not null
     * @param end the end time, not null
     * @return a predicate which tests for the given range
     * @throws IllegalArgumentException if start &gt; end
     * @deprecated use {@link Range}
     */
    @Deprecated(forRemoval = true)
    public static Predicate<LocalTime> betweenInclusive(LocalTime start, LocalTime end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start > end");
        }
        return (time) -> {
            Objects.requireNonNull(time, "time");
            return !time.isBefore(start) && !time.isAfter(end);
        };
    }

    /**
     * Returns a predicate which accepts times between two give times, including start but excluding
     * end.
     *
     * @param start the start time, not null
     * @param bound the upper bound time, not null
     * @return a predicate which tests for the given range
     * @throws IllegalArgumentException if start &gt;= end
     * @deprecated use {@link Range}
     */
    @Deprecated(forRemoval = true)
    public static Predicate<LocalTime> betweenHalfOpen(LocalTime start, LocalTime bound) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(bound, "bound");
        if (!start.isBefore(bound)) {
            throw new IllegalArgumentException("start >= end");
        }
        return (time) -> {
            Objects.requireNonNull(time, "time");
            return !time.isBefore(start) && time.isBefore(bound);
        };
    }

}
