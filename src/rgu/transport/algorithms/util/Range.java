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

import java.util.*;
import java.util.function.*;

/**
 * A non-empty range in between two values. This also implements the {@link Predicate} functional
 * interface and tests true if the tested value is contained in the range.
 *
 * @param <T> the type
 * @author Lee A. Christie
 */
public final class Range<T extends Comparable<? super T>> implements Predicate<T> {

    private final T lower;
    private final T upper;
    private final T bound;
    private final boolean any;

    private Range(T lower, T upperOrBound, boolean closed, boolean any) {
        this.lower = lower;
        if (closed) {
            this.upper = upperOrBound;
            this.bound = null;
        } else {
            this.upper = null;
            this.bound = upperOrBound;
        }
        this.any = any;
    }

    /**
     * Creates a closed range.
     *
     * @param lower the minimum value in the range, not null
     * @param upper the maximum value (inclusive), not null
     * @param <T>   the type
     * @return a range
     * @throws IllegalArgumentException if lower &gt; bound
     */
    public static <T extends Comparable<? super T>> Range<T>
    closed(T lower, T upper) {
        Objects.requireNonNull(lower);
        Objects.requireNonNull(upper);
        if (lower.compareTo(upper) > 0) {
            throw new IllegalArgumentException("lower > upper, expected lower <= upper");
        }
        return new Range<>(lower, upper, true, false);
    }

    /**
     * Creates a half-open range.
     *
     * @param lower the minimum value in the range, not null
     * @param bound the upper bound (exclusive), not null
     * @param <T>   the type
     * @return a range
     * @throws IllegalArgumentException if lower &gt;= bound
     */
    public static <T extends Comparable<? super T>> Range<T>
    halfOpen(T lower, T bound) {
        Objects.requireNonNull(lower);
        Objects.requireNonNull(bound);
        if (lower.compareTo(bound) >= 0) {
            throw new IllegalArgumentException("lower >= bound, expected lower < bound");
        }
        return new Range<>(lower, bound, false, false);
    }

    /**
     * Creates a range of all values (contains everything of type T).
     *
     * @param <T> the type
     * @return a range
     */
    public static <T extends Comparable<? super T>> Range<T> any() {
        return new Range<>(null, null, true, true);
    }

    // intersection of two open ranges
    private static <T extends Comparable<? super T>> boolean
    oo(Range<T> a, Range<T> b) {
        if (b.lower.compareTo(a.bound) >= 0) {
            return false;
        }
        if (b.lower.compareTo(a.lower) < 0) {
            return b.bound.compareTo(a.lower) > 0;
        }
        return true;
    }

    // intersection of open and closed ranges
    private static <T extends Comparable<? super T>> boolean
    oc(Range<T> a, Range<T> b) {
        if (b.lower.compareTo(a.bound) >= 0) {
            return false;
        }
        if (b.lower.compareTo(a.lower) < 0) {
            return b.upper.compareTo(a.lower) >= 0;
        }
        return true;
    }

    // intersection of two closed ranges
    private static <T extends Comparable<? super T>> boolean
    cc(Range<T> a, Range<T> b) {
        if (b.lower.compareTo(a.upper) > 0) {
            return false;
        }
        if (b.lower.compareTo(a.lower) < 0) {
            return b.upper.compareTo(a.lower) >= 0;
        }
        return true;
    }

    /**
     * Returns true if this range is an instance of {@linkplain #any() any}. Note that there may
     * be other instances for a given type <code>T</code> for which {@link #test(Comparable)} always
     * returns <code>true</code>, but for which {@code isAny()} returns false.
     *
     * @return true if any, false otherwise
     */
    public boolean isAny() {
        return any;
    }

    /**
     * Returns a string representation of the range, <code>[lower, upper]</code> is closed,
     * <code>[lower, bound)</code> if half-open.
     *
     * @return a string representation
     */
    public String toString() {
        if (isAny()) {
            return "Range.any()";
        }
        if (isHalfOpen()) {
            return "[" + lower + ", " + bound + ")";
        }
        return "[" + lower + ", " + upper + "]";
    }

    /**
     * Checks if this range intersects another range or not.
     *
     * @param other the other range, not null
     * @return true if intersecting, false otherwise
     */
    public boolean intersects(Range<T> other) {
        Objects.requireNonNull(other, "other");
        if (this.any || other.any) {
            return true;
        }
        if (this.isHalfOpen()) {
            if (other.isHalfOpen()) {
                return oo(this, other);
            }
            return oc(this, other);
        }
        if (other.isHalfOpen()) {
            return oc(other, this);
        }
        return cc(this, other);
    }

    private boolean isHalfOpen() {
        return upper == null;
    }

    /**
     * Checks if a given object is contained with the range.
     *
     * @param object the object, not null
     * @return true if contained, false otherwise
     */
    public boolean test(T object) {
        Objects.requireNonNull(object);
        if (any) {
            return true;
        }
        if (isHalfOpen()) {
            return (lower.compareTo(object) <= 0)
                    && (object.compareTo(bound) < 0);
        }
        return (lower.compareTo(object) <= 0)
                && (object.compareTo(upper) <= 0);
    }

}
