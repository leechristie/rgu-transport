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

import rgu.transport.algorithms.search.*;
import rgu.transport.algorithms.util.*;

import java.util.*;
import static java.lang.Math.*;

/**
 * A 2D tree used as a {@linkplain NearestFinder nearest finder} with a compatible metric.
 *
 * @param <T> the type of object
 * @author Lee A. Christie
 */
public class TwoDTree<T extends TwoDTree.Point> implements NearestFinder<T> {

    private static final Comparator<Point> COMPARE_X = Comparator.comparing(Point::x);
    private static final Comparator<Point> COMPARE_Y = Comparator.comparing(Point::y);
    private final Node root;

    private TwoDTree(Point[] array, ProgressListener progress) {
        int[] num = new int[1];
        this.root = build(array, 0, 0, array.length, () -> {
            num[0]++;
            progress.onUpdateProgress(num[0] / (double) array.length);
        });
    }

    /**
     * Creates a new 2D tree from the given set of points.
     *
     * @param points the points, not null
     * @param <T>    the element type
     * @return the tree
     */
    public static <T extends Point> TwoDTree<T> of(Set<T> points) {
        return of(points, ProgressListener.none(), false);
    }

    /**
     * Creates a new 2D tree from the given set of points.
     *
     * @param points   the points, not null
     * @param progress listener for the progress between 0.0 and 1.0, not null
     * @param <T>      the element type
     * @return the tree
     */
    public static <T extends Point> TwoDTree<T> of(Set<T> points, ProgressListener progress) {
        return of(points, progress, false);
    }

    /**
     * Creates a new 2D tree from the given set of points.
     *
     * @param points       the points, not null
     * @param progress     listener for the progress between 0.0 and 1.0, not null
     * @param subStageOnly if ture, will not call onNewStage or onCompletion on the progress
     *                     listener
     * @param <T>          the element type
     * @return the tree
     * @throws NoSuchElementException if the collection is empty
     */
    public static <T extends Point> TwoDTree<T> of(Set<T> points, ProgressListener progress,
                                                   boolean subStageOnly) {

        Objects.requireNonNull(points, "points");

        if (!subStageOnly) {
            progress.onNewStage("Creating KD Tree...");
        }

        // defensive copy to array:
        // the array is reused throughout the sort so this is the only array allocation
        int n = points.size();
        if (n == 0) {
            throw new NoSuchElementException("no elements with which to build a tree");
        }
        Point[] array = new Point[n];
        int i = 0;
        for (Point p : points) {
            if (i >= n) {
                throw new ConcurrentModificationException("list size changed while building tree");
            }
            if (p == null) {
                throw new NullPointerException("points[" + i + "]");
            }
            array[i++] = p;
        }
        if (i != n) {
            throw new ConcurrentModificationException("list size changed while building tree");
        }

        TwoDTree<T> rv = new TwoDTree<>(array, progress);
        progress.onUpdateProgress(1.0);
        if (!subStageOnly) {
            progress.onCompletion();
        }
        return rv;

    }

    private static void partition(Point[] array,
                                  int start,
                                  int bound,
                                  Comparator<Point> comparator) {

        // TODO: replace with faster method of partition
        Arrays.sort(array, start, bound, comparator);

    }

    private Node build(Point[] array, int depth, int start, int bound, Runnable onProcessPoint) {

        int length = bound - start;
        if (length == 0) {
            return null;
        }

        Comparator<Point> comparator = (depth % 2 == 0) ? COMPARE_X : COMPARE_Y;
        partition(array, start, bound, comparator);

        int mid = start + length / 2;
        Point point = array[mid];
        onProcessPoint.run();
        Node left = build(array, depth + 1, start, mid, onProcessPoint);
        Node right = build(array, depth + 1, mid + 1, bound, onProcessPoint);

        return new Node(left, point, right);

    }

    private Point search(Node root, Point point, int depth, double ratio) {

        if (root == null) {
            return null;
        }

        int axis = depth % 2;

        Node next;
        Node other;
        if (axis == 0) {
            if (point.x() < root.point.x()) {
                next = root.left;
                other = root.right;
            } else {
                next = root.right;
                other = root.left;
            }
        } else {
            if (point.y() < root.point.y()) {
                next = root.left;
                other = root.right;
            } else {
                next = root.right;
                other = root.left;
            }
        }

        Point best = closest(point, search(next, point, depth + 1, ratio), root.point, ratio);

        double distanceSquared = abs(
                pow((point.x() - best.x()) * ratio, 2) // stretch x-axis
                    + pow(point.y() - best.y(), 2));

        if (axis == 0) {

            // stretch x-axis
            if (distanceSquared > pow((point.x() - root.point.x()) * ratio, 2)) {
                // orthogonal distance to other branch is short so need to explore the other branch too
                best = closest(point, search(other, point, depth + 1, ratio), best, ratio);
            }

        } else if (distanceSquared > pow(point.y() - root.point.y(), 2)) {
            // orthogonal distance to other branch is short so need to explore the other branch too
            best = closest(point, search(other, point, depth + 1, ratio), best, ratio);
        }

        return best;

    }

    private Point closest(Point point, Point a, Point b, double ratio) {

        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        double distanceSquaredA
                = abs(pow((point.x() - a.x()) * ratio, 2) + pow(point.y() - a.y(), 2));
        double distanceSquaredB
                = abs(pow((point.x() - b.x()) * ratio, 2) + pow(point.y() - b.y(), 2));

        return distanceSquaredA < distanceSquaredB ? a : b;

    }

    /**
     * Finds the nearest neighbour in the tree based on a Euclidean distance.
     *
     * @param target the target, not null
     * @return the nearest neighbour
     */
    @Override
    @SuppressWarnings("unchecked")
    public T nearest(T target) {

        // this line uses an unchecked cast since we're storing the elements in
        // an array of Point but the search can only possibly return an element
        // from the tree (from the original Collection<T> in the static factory
        // method), or null, thus can always be cast to T
        return (T) search(this.root, target, 0, 1.0);

    }

    /**
     * Returns this 2D tree adapted to use a given stretch ratio (applied to the x-axis).
     *
     * @param ratio the ratio, finite, positive
     * @return this 2D tree with the given scale ratio
     */
    public NearestFinder<T> withRatio(double ratio) {

        if (!Double.isFinite(ratio) || ratio <= 0.0) {
            throw new AssertionError("ratio = " + ratio + ", expected: positive, finite");
        }

        return new NearestFinder<>() { // no lambda to mark unchecked cast

            // this line uses an unchecked cast since we're storing the elements
            // in an array of Point but the search can only possibly return an
            // element from the tree (from the original Collection<T> in the
            // static factory method), or null, thus can always be cast to T
            @Override
            @SuppressWarnings("unchecked")
            public T nearest(T target) {
                return (T) search(TwoDTree.this.root, target, 0, ratio);
            }

        };

    }

    /**
     * Point usable by the 2D tree. Implementation must be immutable.
     */
    public interface Point {

        /**
         * Returns the x-coordinate of the point.
         *
         * @return the x-coordinate
         */
        double x();

        /**
         * Returns the y-coordinate of the point.
         *
         * @return the y-coordinate
         */
        double y();

    }

    private record Node(Node left, Point point, Node right) {
    }

}
