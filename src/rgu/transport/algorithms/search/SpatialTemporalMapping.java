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
import java.util.function.*;

/**
 * A mapping between spatial and spatial-temporal representation.
 *
 * @param toSpatial         converts to spatial representation, not null
 * @param toSpatialTemporal converts to spatial-temporal representation, not null
 * @param <S>               the spatial representation
 * @param <E>               the edge representation
 * @param <ST>              the spatial-temporal representation
 * @author Lee A. Christie
 */
@SuppressWarnings("java:S119") // use of non-standard 2-letter type <ST>
public record SpatialTemporalMapping<S, E, ST>(
        Function<ST, S> toSpatial, BiFunction<S, E, ST> toSpatialTemporal) {

    /**
     * Canonical constructor with null-checks.
     *
     * @param toSpatial         converts to spatial representation, not null
     * @param toSpatialTemporal converts to spatial-temporal representation, not null
     */
    public SpatialTemporalMapping {
        Objects.requireNonNull(toSpatial, "toSpatial");
        Objects.requireNonNull(toSpatialTemporal, "toSpatialTemporal");
    }

    /**
     * Identity function for when the temporal information is not needed.
     *
     * @param <V> the vertex representation
     * @param <E> the edge representation
     * @return an identity mapping
     */
    public static <V, E> SpatialTemporalMapping<V, E, V> identity() {
        return new SpatialTemporalMapping<>(UnaryOperator.identity(),
                (vertex, _) -> vertex);
    }

}
