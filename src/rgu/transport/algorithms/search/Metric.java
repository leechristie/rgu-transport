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
 * A mathematical distance metric between two points.
 *
 * @param <T> the type of points
 * @param <D> the type of distance
 * @author Lee A. Christie
 */
public interface Metric<T, D extends Comparable<? super D>> {

    /**
     * Returns the distance between points a and b.
     *
     * @param a point a, not null
     * @param b point b, not null
     * @return the distance
     */
    D distance(T a, T b);

    /**
     * Creates a heuristic function which takes any point and returns its istance to a predefined
     * goal point.
     *
     * @param goal the goal to which the heuristic in aiming, not null.
     * @return a function which calculates the heuristic form any given point to the fixed goal
     */
    default Function<T, D> asHeuristic(T goal) {
        Objects.requireNonNull(goal, "goal");
        return (T point) -> distance(point, goal);
    }

}
