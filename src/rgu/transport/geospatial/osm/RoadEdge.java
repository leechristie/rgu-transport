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

package rgu.transport.geospatial.osm;

import java.io.*;
import java.util.*;
import java.util.function.*;

import rgu.transport.algorithms.collections.*;
import rgu.transport.geospatial.*;

/**
 *
 * @author Lee A. Christie
 */
public final class RoadEdge {

    private final Distance distance;
    private final Speed speed;

    public RoadEdge(Distance distance, Speed speed) {
        Objects.requireNonNull(distance, "distance");
        this.distance = distance;
        this.speed = speed;
    }

    public Distance distance() {
        return distance;
    }

    public Speed speed() {
        return speed;
    }

    public void write(ObjectOutputStream out) throws IOException {
        distance.write(out);
        if (speed == null) {
            out.writeDouble(Double.NaN);
        } else {
            speed.write(out);
        }
    }

    public static RoadEdge read(ObjectInputStream in) throws IOException {
        return new RoadEdge(Distance.read(in), Speed.read(in));
    }

    /**
     * A predicate which is true if either end point of a graph edge matches the given predicate.
     *
     * @param filter the predicate to test end points
     * @return a predicate on graph edges
     */
    public static Predicate<Graph.Edge<GeoLocation, RoadEdge>> containsEitherEndPoint(Predicate<GeoLocation> filter) {
        return edge -> filter.test(edge.from()) || filter.test((edge.to()));
    }

    /**
     * A predicate which is true if both end points of a graph edge match the given predicate.
     *
     * @param filter the predicate to test end points
     * @return a predicate on graph edges
     */
    public static Predicate<Graph.Edge<GeoLocation, RoadEdge>> containsBothEndPoints(Predicate<GeoLocation> filter) {
        return edge -> filter.test(edge.from()) && filter.test((edge.to()));
    }

    /**
     * A predicate which is true if the edge has a defined speed, and the speed value is 0 m/s.
     *
     * @return a predicate on graph edges
     */
    public static Predicate<Graph.Edge<GeoLocation, RoadEdge>> hasZeroSpeed() {
        return edge -> edge.value().speed() != null & edge.value().speed().isZero();
    }

    /**
     * A predicate which is true if the edge has a defined speed, whether positive or zero.
     *
     * @return a predicate on graph edges
     */
    public static Predicate<Graph.Edge<GeoLocation, RoadEdge>> hasDefinedSpeed() {
        return edge -> edge.value().speed != null;
    }

    /**
     * A predicate which is true if the edge has a undefined speed.
     *
     * @return a predicate on graph edges
     */
    public static Predicate<Graph.Edge<GeoLocation, RoadEdge>> hasUndefinedSpeed() {
        return edge -> edge.value().speed == null;
    }

    /**
     * A function which multiplies the speed limit by a given amount. Edges not matching the filter remain unchanged.
     * Edges matching the filter but with undefined speed limits will also remain unchanged.
     *
     * @param filter the filter to check whether to apply the limit, not null
     * @param multiplicationFactor the factor by which to multiply the speed limit, &gt; 0, finite
     * @return the mapping function
     */
    public static Function<Graph.Edge<GeoLocation, RoadEdge>, RoadEdge> speedMultiplier(
            Predicate<Graph.Edge<GeoLocation, RoadEdge>> filter, double multiplicationFactor) {
        Objects.requireNonNull(filter, "filter");
        if (multiplicationFactor <= 0.0 || !Double.isFinite(multiplicationFactor)) {
            throw new IllegalArgumentException(
                    "multiplicationFactor = " + multiplicationFactor + ", expected > 0, finite");
        }
        return edge -> (edge.value().speed() == null || !filter.test(edge)) ? edge.value()
                        : new RoadEdge(edge.value().distance(), Speed.ofMetersPerSecond(
                        Math.round(edge.value().speed().asMetersPerSecond() * multiplicationFactor)));
    }

    /**
     * A function which replaces the speed limit by a given amount. Edges not matching the filter remain unchanged.
     *
     * @param filter, not null
     * @param newSpeed the new speed limit, not null
     * @return the mapping function
     */
    public static Function<Graph.Edge<GeoLocation, RoadEdge>, RoadEdge> speedReplacement(
            Predicate<Graph.Edge<GeoLocation, RoadEdge>> filter, Speed newSpeed) {
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(newSpeed, "newSpeed");
        return edge -> filter.test(edge) ? new RoadEdge(edge.value().distance(), newSpeed) : edge.value();
    }

    /**
     * A function which applies a default speed to edges with undefined speed. Edges not matching the filter remain
     * unchanged. Edges matching the filter but which already have a defined speed limit will also remain unchanged.
     *
     * @param filter, not null
     * @param defaultSpeed the new speed limit, not null
     * @return the mapping function
     */
    public static Function<Graph.Edge<GeoLocation, RoadEdge>, RoadEdge> speedDefault(
            Predicate<Graph.Edge<GeoLocation, RoadEdge>> filter, Speed defaultSpeed) {
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(defaultSpeed, "newSpeed");
        return edge -> (edge.value().speed() != null || !filter.test(edge)) ? edge.value()
                       : new RoadEdge(edge.value().distance(), defaultSpeed);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoadEdge roadEdge = (RoadEdge) o;
        return Objects.equals(distance, roadEdge.distance) && Objects.equals(speed, roadEdge.speed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(distance, speed);
    }
}
