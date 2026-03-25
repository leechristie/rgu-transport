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

import java.util.*;
import java.util.concurrent.*;

// Implementation of a thread-safe cached finder.
// author Lee A. Christie
final class ConcurrentCachedFinder<T> extends CachedFinder<T> {

    ConcurrentCachedFinder(NearestFinder<T> parent,
                           ConcurrentMap<T, Optional<T>> cache) {
        super(parent, cache);
    }

    static <T> ConcurrentCachedFinder<T> of(NearestFinder<T> parent) {
        return new ConcurrentCachedFinder<>(parent, new ConcurrentHashMap<>());
    }

    static <T> ConcurrentCachedFinder<T> ofNonOptional(NearestFinder<T> parent,
                                                       Map<T, T> cache) {
        ConcurrentMap<T, Optional<T>> withOptionals = new ConcurrentHashMap<>();
        for (var e : cache.entrySet()) {
            withOptionals.put(e.getKey(), Optional.of(e.getValue()));
        }
        return new ConcurrentCachedFinder<>(parent, withOptionals);
    }

    @Override
    public NearestFinder<T> concurrentCache() {
        return this;
    }

}
