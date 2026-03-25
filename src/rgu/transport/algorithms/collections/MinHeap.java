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

/**
 * A minimum heap priority queue. Elements in {@code MinHeap} are given a key (also called
 * priority), which may be non-unique. This differs from {@link java.util.PriorityQueue} in the Java
 * standard library in which {@link Comparable} or {@link java.util.Comparator} is used.
 *
 * <p>Elements with the minimum key are removed first. The key of an existing element may be
 * decreased by calling the {@link #decreaseKey(Object, Object)} method, which will throw an
 * exception if the new key is not smaller.
 *
 * <p>Implementations are not required to support increasing the key of an existing element but may
 * provide supplementary methods. Implementations are not required to be safe for concurrent use.
 *
 * @param <E> the type of elements, must be a hashable type
 * @param <K> the type of key (also called priority)
 * @author Lee A. Christie
 */
public interface MinHeap<E, K> {

    /**
     * Returns {@code true} if this heap contains no elements.
     *
     * @return {@code true} if this heap contains no elements
     */
    boolean isEmpty();

    /**
     * Adds the specified element with the specified key.
     *
     * @param element the element, not null
     * @param key     the key, not null
     * @throws IllegalStateException if the element already exists
     */
    void addElement(E element, K key);

    /**
     * Replaces the key of the specified element with a specified smaller value.
     *
     * @param element the element, not null
     * @param key     the new, smaller key value, not null
     * @throws IllegalStateException            if the new key is not smaller than the current key
     * @throws java.util.NoSuchElementException if the element does not exist in the heap
     */
    void decreaseKey(E element, K key);

    /**
     * Removes and returns the element with the minimum key.
     *
     * @return the element with the minimum value
     * @throws java.util.NoSuchElementException if the heap is empty
     */
    E deleteMinimum();

    /**
     * Returns a string representation of this heap, typically used for debugging. The order of
     * elements is unspecified and depends on the implementation.
     *
     * @return a string representation of this heap
     */
    @Override
    String toString();

}
