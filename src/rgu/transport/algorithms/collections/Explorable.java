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

import java.util.*;

/**
 * An explorable graph-like structure. The explorable interface defines the minimum operations used
 * to enable the exploration of a graph, whether in-memory or dynamically-generated.
 *
 * @param <V> the vertex type
 * @param <E> the edge type
 * @author Lee A. Christie
 */
public interface Explorable<V, E> {

    /**
     * The vertices for which there is an out-edge from the specified vertex to that vertex.
     *
     * @param vertex the specified vertex, not null
     * @return the set of neighbours
     */
    Set<V> neighbours(V vertex);

    /**
     * returns the edge between the specified pair of vertices.
     *
     * @param from the vertex for which this will be an out-edge, not null
     * @param to   the vertex for which this will be an in-edge, not null
     * @return the edge
     * @throws NoSuchElementException if the edge does not exist between from
     *                                and to
     */
    E edge(V from, V to);

}
