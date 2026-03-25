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

package rgu.transport.impl;

import rgu.transport.algorithms.search.Metric;
import rgu.transport.algorithms.search.NearestFinder;
import rgu.transport.geospatial.*;

import java.util.*;
import java.util.function.*;

/**
 * Exhaustive search on a set of geo locations. Not exported as this requires defensive copy outside
 * the call. Can be bypassed when the underlying set is known to be unmodifiable.
 *
 * @param <D> the edge type
 */
public final class ExhaustiveGeoLocationNearestFinderImpl<D extends Comparable<? super D>>
        implements NearestFinder<GeoLocation> {

    private final Collection<GeoLocation> elements;
    private final Metric<GeoLocation, D> metric;
    private final Predicate<D> filter;

    /**
     * Creates a new instance of ExhaustiveGeoLocationNearestFinderImpl.
     *
     * @param metric the distance metric, not null
     * @param elements the elements, not null, reference will be maintained to this collection for
     *                 read access
     * @param filter the filter for elements, not null, targets with no filter-accepted elements
     *               will be mapped to null
     */
    public ExhaustiveGeoLocationNearestFinderImpl(
            Metric<GeoLocation, D> metric,
            Collection<GeoLocation> elements,
            Predicate<D> filter) {
        this.metric = metric;
        this.elements = elements;
        this.filter = filter;
    }


    /**
     * Returns the element which is nearest to the specified target. Targets with no filter-accepted
     * elements will be mapped to null.
     *
     * @param target the target, not null
     * @return the nearest match, null if no filter-accepted elements exist
     */
    @Override
    public GeoLocation nearest(GeoLocation target) {
        Objects.requireNonNull(target, "target");
        D minDistance = null;
        GeoLocation nearest = null;
        for (GeoLocation other : this.elements) {
            D newDistance = this.metric.distance(other, target);
            if (filter.test(newDistance)) {
                if (nearest == null || newDistance.compareTo(minDistance) < 0) {
                    minDistance = newDistance;
                    nearest = other;
                }
            }
        }
        return nearest;
    }

}
