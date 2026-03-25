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
 * A consumer callback for reachability algorithms. Used both to consume the result of the search
 * and to halt the search early.
 *
 * @param <V> the vertex type
 * @param <E> the edge type
 * @author Lee A. Christie
 */
@FunctionalInterface
public interface ReachabilityCallback<V, E> {

    /**
     * Converts a {@linkplain BiConsumer bi-consumer} to a reachability callback which never halts
     * early.
     *
     * @param consumer the bi-consumer, not null
     * @param <V>      the vertex type
     * @param <E>      the edge type
     * @return a reachability callback
     */
    static <V, E> ReachabilityCallback<V, E> from(BiConsumer<V, E> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        return (vertex, cost) -> {
            consumer.accept(vertex, cost);
            return Action.CONTINUE;
        };
    }

    /**
     * Converts a {@linkplain BiConsumer bi-consumer} to a reachability callback which halts early
     * based on a separate call to a {@linkplain BiPredicate bi-predicate}, where {@code true}
     * indicates halt.
     *
     * @param consumer   the bi-consumer, not null
     * @param shouldHalt the bi-predicate, not null
     * @param <V>        the vertex type
     * @param <E>        the edge type
     * @return a reachability callback
     */
    static <V, E> ReachabilityCallback<V, E> from(
            BiConsumer<V, E> consumer,
            BiPredicate<V, E> shouldHalt) {
        Objects.requireNonNull(consumer, "consumer");
        Objects.requireNonNull(shouldHalt, "predicate");
        return (vertex, cost) -> {
            consumer.accept(vertex, cost);
            return shouldHalt.test(vertex, cost)
                    ? Action.BREAK : Action.CONTINUE;
        };
    }

    /**
     * Consumes the current discovered vertex and instructs whether to stop.
     *
     * @param vertex the current vertex, not null
     * @param cost   the current cost, not null
     * @return {@link Action#BREAK} if the search should halt now, otherwise
     * {@link Action#CONTINUE}
     */
    Action consume(V vertex, E cost);

    /**
     * The result of a callback, instructs the algorithm to either continue or halt.
     */
    enum Action {

        /**
         * The search should continue.
         */
        CONTINUE,

        /**
         * The search should stop, as further results are no longer needed.
         */
        BREAK

    }

}
