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

import rgu.transport.algorithms.util.*;

import java.util.*;

/**
 * An object which can be used to find the nearest match within a predetermined
 * set of elements. A {@code NearestFinder}
 * cannot refer to an empty set.
 *
 * @param <T> The type of elements
 * @author Lee A. Christie
 */
public interface NearestFinder<T> {

    /**
     * Returns the element which is nearest to the specified target.
     *
     * @param target the target, not null
     * @return the nearest match
     * @throws NoSuchElementException if there is no nearest element
     */
    T nearest(T target);

    /**
     * Returns the element which is nearest to a target, for a collection of specified targets.
     *
     * @param targets the targets, not null
     * @return a mapping of targets to the nearest matches
     * @throws InterruptedException if the thread is interrupted while matching the points to
     *                              neighbours
     */
    default Map<T, T> nearest(Collection<T> targets)
            throws InterruptedException {
        Objects.requireNonNull(targets, "targets");
        Map<T, T> rv = new HashMap<>();
        for (T t : targets) {
            ThreadUtil.checkInterrupt();
            try {
                rv.put(t, nearest(t));
            } catch (NoSuchElementException ex) {
                // do not add anything if there is no match
            }
        }
        return rv;
    }

    /**
     * Returns an optional containing the element which is nearest to a target, for a collection of
     * specified targets.
     *
     * @param target the target, not null
     * @return a mapping of targets to the nearest matches or
     * {@code Optional.empty} if there is no nearest element
     */
    default Optional<T> nearestOrEmpty(T target) {
        try {
            return Optional.of(nearest(target));
        } catch (NoSuchElementException ex) {
            return Optional.empty();
        }
    }

    /**
     * Returns the element which is nearest to a target, for a collection of specified targets.
     *
     * @param targets      the targets, not null
     * @param progress     listener for the progress between 0.0 and 1.0, not null
     * @param subStageOnly if ture, will not call onNewStage or onCompletion on the progress
     *                     listener
     * @return a mapping of targets to the nearest matches
     * @throws InterruptedException if the thread is interrupted while matching the points to
     *                              neighbours
     */
    default Map<T, T> nearest(Collection<T> targets,
                              ProgressListener progress,
                              boolean subStageOnly)
            throws InterruptedException {
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(progress, "progress");
        final int total = targets.size();
        int done = 0;
        Map<T, T> rv = new HashMap<>();
        if (!subStageOnly) {
            progress.onNewStage("Finding nearest points...");
        }
        progress.onUpdateProgress(0.0);
        for (T t : targets) {
            ThreadUtil.checkInterrupt();
            rv.put(t, nearest(t));
            done++;
            if (done < total) {
                progress.onUpdateProgress(((double) done) / total);
            }
        }
        progress.onUpdateProgress(1.0);
        if (!subStageOnly) {
            progress.onCompletion();
        }
        return rv;
    }

    /**
     * Returns a cached version of the finder.
     *
     * @return a cached version of the finder
     */
    default NearestFinder<T> cache() {
        return CachedFinder.of(this);
    }

    /**
     * Returns a cached version of the finder seeded with the specified starting map. Elements in
     * the starting map take priority over the backing finder.
     *
     * @param cache the cache to wrap, not null
     * @return a cached version of the finder
     */
    default NearestFinder<T> cache(Map<T, T> cache) {
        Objects.requireNonNull(cache, "cache");
        return CachedFinder.ofNonOptional(this, cache);
    }

    /**
     * Returns a thread-safe cached version of the finder.
     *
     * @return a thread-safe cached version of the finder
     */
    default NearestFinder<T> concurrentCache() {
        return ConcurrentCachedFinder.of(this);
    }

    /**
     * Returns a thread-safe cached version of the finder seeded with the specified starting map.
     * Elements in the starting map take priority over the backing finder.
     *
     * @param cache the cache to wrap, not null
     * @return a thread-safe cached version of the finder
     */
    default NearestFinder<T> concurrentCache(Map<T, T> cache) {
        Objects.requireNonNull(cache, "cache");
        return ConcurrentCachedFinder.ofNonOptional(this, cache);
    }

}
