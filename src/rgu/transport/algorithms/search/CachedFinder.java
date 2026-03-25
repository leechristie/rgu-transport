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

// Implementation of a cached finder.
// author Lee A. Christie
class CachedFinder<T> implements NearestFinder<T> {

    final NearestFinder<T> parent;
    final Map<T, Optional<T>> cache;

    CachedFinder(NearestFinder<T> parent, Map<T, Optional<T>> cache) {
        this.parent = parent;
        this.cache = cache;
    }

    static <T> CachedFinder<T> of(NearestFinder<T> parent) {
        return new CachedFinder<>(parent, new HashMap<>());
    }

    static <T> CachedFinder<T> ofNonOptional(NearestFinder<T> parent,
                                             Map<T, T> cache) {
        Map<T, Optional<T>> withOptionals = new HashMap<>();
        for (var e : cache.entrySet()) {
            withOptionals.put(e.getKey(), Optional.of(e.getValue()));
        }
        return new CachedFinder<>(parent, withOptionals);
    }

    @Override
    public T nearest(T target) {
        Optional<T> rv = cache.computeIfAbsent(target, parent::nearestOrEmpty);
        if (rv.isEmpty()) {
            throw new NoSuchElementException("no nearest found for " + target);
        }
        return rv.get();
    }

    @Override
    public Optional<T> nearestOrEmpty(T target) {
        return cache.computeIfAbsent(target, parent::nearestOrEmpty);
    }

    @Override
    public NearestFinder<T> cache() {
        return this;
    }

    @Override
    public NearestFinder<T> concurrentCache() {
        return new ConcurrentCachedFinder<>(parent, new ConcurrentHashMap<>(cache));
    }

}
