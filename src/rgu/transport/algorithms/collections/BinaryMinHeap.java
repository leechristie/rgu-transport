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
import java.util.function.*;

/**
 * A {@linkplain MinHeap minimum heap} implemented as a binary heap. All mutations are O(log n).
 * Not safe for concurrent use.
 *
 * @param <E> the type of elements, must be a hashable type
 * @param <K> the type of key (also called priority)
 * @author Lee A. Christie
 */
public final class BinaryMinHeap<E, K> implements MinHeap<E, K> {

    private final List<Node> heap = new ArrayList<>();
    private final BiPredicate<K, K> lessThan;
    private final Map<E, Node> lookup = new HashMap<>();

    /**
     * Constructs a new {@code BinaryMinHeap}.
     *
     * @param lessThan a pure predicate which tests whether a &lt; b, not null
     */
    public BinaryMinHeap(final BiPredicate<K, K> lessThan) {
        Objects.requireNonNull(lessThan, "lessThan");
        this.lessThan = lessThan;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return this.heap.isEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException {@inheritDoc}
     */
    @Override
    public void addElement(final E element, final K key) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(key, "key");
        if (this.lookup.containsKey(element)) {
            throw new IllegalStateException("duplicate element");
        }
        appendNode(element, key);
        fixHeapProperty(key, this.heap.size() - 1);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException  {@inheritDoc}
     * @throws NoSuchElementException {@inheritDoc}
     */
    @Override
    public void decreaseKey(final E element, final K key) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(key, "key");
        if (!this.lookup.containsKey(element)) {
            throw new NoSuchElementException(element + " not found");
        }

        final Node node = this.lookup.get(element);
        final K oldKey = node.key;
        if (!this.lessThan.test(key, oldKey)) {
            throw new IllegalStateException("can only decrease key, current key = " + oldKey
                                            + ", new key = " + key + ", which is not smaller");
        }
        node.key = key;
        fixHeapProperty(key, node.index);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NoSuchElementException {@inheritDoc}
     */
    @Override
    public E deleteMinimum() {
        if (isEmpty()) {
            throw new NoSuchElementException("heap is empty");
        }

        final E rootElement;

        // special case for only one element, empties the arrays
        if (this.heap.size() == 1) {

            // remove and get the root element
            rootElement = this.heap.removeFirst().element;

        } else {

            // get the root element
            rootElement = this.heap.getFirst().element;

            // delete element[0] and replace with element[n-1]
            // size will be decreased by 1
            swapNodes(0, this.heap.size() - 1);
            this.heap.removeLast();

            // recursively heapify the tree
            heapify(0);

        }

        // remove the reference for the root element
        this.lookup.remove(rootElement);

        // return what was previously the root
        return rootElement;

    }

    /**
     * Returns a string representation of this heap. The order of elements is unspecified.
     *
     * @return a string representation of this heap
     */
    @Override
    public String toString() {
        final StringBuilder rv = new StringBuilder("{");
        for (Node node : this.heap) {
            if (rv.length() > 1) {
                rv.append(", ");
            }
            rv.append(node.key).append(':').append(node.element);
        }
        return rv.append('}').toString();
    }

    // add a new node to the heap
    private void appendNode(final E element, final K key) {
        final int index = this.heap.size();
        final Node node = new Node(element, key, index);
        this.lookup.put(element, node);
        this.heap.add(node);
    }

    // swap two nodes in the heap
    private void swapNodes(final int a, final int b) {
        final Node nodeA = this.heap.get(a);
        final Node nodeB = this.heap.get(b);
        nodeA.index = b;
        nodeB.index = a;
        this.heap.set(a, nodeB);
        this.heap.set(b, nodeA);
    }

    // iterative repair function for adding and decreasing key
    private void fixHeapProperty(final K key, int index) {
        int parentIndex = (index - 1) / 2;
        K parentKey = this.heap.get(parentIndex).key;
        while (this.lessThan.test(key, parentKey)) {
            swapNodes(index, parentIndex);
            index = parentIndex;
            parentIndex = (index - 1) / 2;
            parentKey = this.heap.get(parentIndex).key;
        }
    }

    // recursive repair function for removing minimum element
    private void heapify(final int index) {

        final int leftIndex = 2 * index + 1;
        final int rightIndex = leftIndex + 1;

        final K key = this.heap.get(index).key;
        final K leftKey = leftIndex < this.heap.size()
                ? this.heap.get(leftIndex).key : null;
        final K rightKey = rightIndex < this.heap.size()
                ? this.heap.get(rightIndex).key : null;

        int smallestIndex = index;
        K smallestKey = key;

        // left < current
        if (leftKey != null && this.lessThan.test(leftKey, smallestKey)) {
            smallestKey = leftKey;
            smallestIndex = leftIndex;
        }

        // right < min(left, current)
        if (rightKey != null && this.lessThan.test(rightKey, smallestKey)) {
            // we don't set `smallestKey = rightKey` since
            // smallestKey is not used again in scope
            smallestIndex = rightIndex;
        }

        // repair heap property recursively if needed
        if (smallestIndex != index) {
            swapNodes(index, smallestIndex);
            heapify(smallestIndex);
        }

    }

    // mutable to change key/index, kept as reference in two places
    private class Node {
        private final E element;
        private K key; // mutable to decrease key
        private int index; // mutable to swap nodes

        private Node(final E element, final K key, final int index) {
            this.element = element;
            this.key = key;
            this.index = index;
        }
    }

}
