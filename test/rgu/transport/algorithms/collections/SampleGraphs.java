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

import static java.lang.Math.*;

public class SampleGraphs {

    public static Point a, b, c, x, y, z;

    public record Point(int x, int y) {
        double distance(Point other) {
            double dx = x - other.x;
            double dy = y - other.y;
            return hypot(dx, dy);
        }
    }

    public static void oneWay(HashGraph.Builder<Point, Double> builder,
                              Point from,
                              Point to) {
        builder.addEdge(from, to, from.distance(to));
    }

    public static void twoWay(HashGraph.Builder<Point, Double> builder,
                               Point from,
                               Point to) {
        builder.addEdge(from, to, from.distance(to));
        builder.addEdge(to, from, to.distance(from));
    }

    public static Graph<Point, Double> sampleGraph1() {

        HashGraph.Builder<Point, Double> builder = HashGraph.builder();

        // component 1
        a = new Point(2, 1);
        b = new Point(3, 1);
        c = new Point(4, 2);
        builder.addVertices(a, b, c);
        oneWay(builder, a, b);
        twoWay(builder, b, c);

        // component 2
        x = new Point(1, 1);
        y = new Point(1, 0);
        z = new Point(0, 0);
        builder.addVertices(x, y, z);
        oneWay(builder, x, y);
        twoWay(builder, y, z);

        return builder.build();

    }

    public static Graph<String, Integer> sampleGraph2() {

        HashGraph.Builder<String, Integer> builder = HashGraph.builder();
        builder.addVertices("ABCDExyz".split(""));
        builder.addEdge("A", "x", 2);
        builder.addEdge("x", "A", 2);
        builder.addEdge("A", "B", 4);
        builder.addEdge("B", "A", 4);
        builder.addEdge("x", "B", 1);
        builder.addEdge("B", "x", 1);
        builder.addEdge("C", "B", 5);
        builder.addEdge("B", "y", 1);
        builder.addEdge("y", "C", 1);
        builder.addEdge("B", "z", 2);
        builder.addEdge("z", "B", 2);
        builder.addEdge("z", "C", 2);
        builder.addEdge("C", "z", 2);
        builder.addEdge("D", "C", 1);
        builder.addEdge("D", "E", 2);
        builder.addEdge("E", "D", 2);
        return builder.build();

    }

}
