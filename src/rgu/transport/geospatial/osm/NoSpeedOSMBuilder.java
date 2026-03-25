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

import rgu.transport.algorithms.collections.Graph;
import rgu.transport.algorithms.collections.HashGraph;
import rgu.transport.algorithms.search.Metric;
import rgu.transport.geospatial.*;

import java.time.*;
import java.util.*;
import java.util.function.*;

/**
 *
 * @author Lee A. Christie
 */
public final class NoSpeedOSMBuilder implements OSMListener {

    final Metric<GeoLocation, Distance> metric
        = GeoLocation.HAVERSINE;
    final Map<Long, GeoLocation> locations
        = new HashMap<>(1_000_000);
    final Map<GeoLocation, Long> reverse
        = new HashMap<>(1_000_000);
    final HashGraph.Builder<GeoLocation, RoadEdge> builder
        = HashGraph.builder();
    final Set<GeoLocation> vertices = new HashSet<>();
    final Predicate<GeoLocation> vertexFilter;
    BiPredicate<Speed, Map<String, String>> edgeFilter;

    public static final BiPredicate<Speed, Map<String, String>> IS_HIGHWAY
        = (speed, tags) -> tags.containsKey("highway");
    public static final BiPredicate<Speed, Map<String, String>> HAS_SPEED
        = (speed, tags) -> speed != null;
    public static final BiPredicate<Speed, Map<String, String>> IS_HIGHWAY_OR_HAS_SPEED
        = (speed, tags) -> (speed != null || tags.containsKey("highway"));
    public static final BiPredicate<Speed, Map<String, String>> IS_HIGHWAY_AND_HAS_SPEED
        = (speed, tags) -> (speed != null && tags.containsKey("highway"));

    private OSMProgressListener progress;
    private final BiConsumer<GeoLocation, GeoLocation> drawLine;

    public void setCancelEvent(Runnable r) {
        if (this.progress != null) {
            this.progress.setCancelEvent(r);
        }
    }

    public NoSpeedOSMBuilder(BiPredicate<Speed, Map<String, String>> edgeFilter,
                             BiConsumer<GeoLocation, GeoLocation> drawLine) {
        this(edgeFilter, (location) -> true, drawLine);
    }

    public NoSpeedOSMBuilder(BiPredicate<Speed, Map<String, String>> edgeFilter,
                             BiConsumer<GeoLocation, GeoLocation> drawLine,
                             OSMProgressListener progress) {
        this(edgeFilter, (location) -> true, drawLine, progress);
    }

    public NoSpeedOSMBuilder(BiPredicate<Speed, Map<String, String>> edgeFilter,
                             Predicate<GeoLocation> vertexFilter,
                             BiConsumer<GeoLocation, GeoLocation> drawLine) {
        this(edgeFilter, vertexFilter, drawLine, null);
    }

    public NoSpeedOSMBuilder(BiPredicate<Speed, Map<String, String>> edgeFilter,
                             Predicate<GeoLocation> vertexFilter,
                             BiConsumer<GeoLocation, GeoLocation> drawLine,
                             OSMProgressListener progress) {
        Objects.requireNonNull(vertexFilter, "filter");
        Objects.requireNonNull(edgeFilter, "edgeFilter");
        this.vertexFilter = vertexFilter;
        this.edgeFilter = edgeFilter;
        this.drawLine = drawLine;
        if (progress != null) {
            this.progress = progress;
        } else {
            this.progress = OSMProgressListener.NONE;
        }
    }

    public Graph<GeoLocation, RoadEdge> build() {
        this.progress.done();
        return this.builder.build();
    }

    public Set<GeoLocation> vertices() {
        return Collections.unmodifiableSet(this.vertices);
    }

    @Override
    public void version(String version, String generator) {
        // not used
    }

    @Override
    public void bounds(Latitude minlat, Longitude minlon,
                       Latitude maxlat, Longitude maxlon) {
        // not used
    }

    @Override
    public void location(long id, GeoLocation location, String version,
                         LocalDateTime timestamp, Map<String, String> tags) {
        this.progress.incrementTotalLocationCount();
        if (this.locations.containsKey(id)) {
            throw new AssertionError("Duplicate ID: " + id);
        }
        this.locations.put(id, location);
        if (this.reverse.containsKey(location)) {
            // Duplicate Location: `location`
        } else {
            this.reverse.put(location, id);
        }
    }

    @Override
    public void way(long id, String version, LocalDateTime timestamp,
                    List<Long> points, Map<String, String> tags) {
        Speed speed = parseSpeed(tags);
        if (!this.edgeFilter.test(speed, tags)) {
            return;
        }
        GeoLocation previous = null;
        for (long currentID : points) {
            boolean added = false;
            GeoLocation current = this.locations.get(currentID);
            if (previous != null) {
                this.progress.incrementTotalWayCount();
                Distance distance = this.metric.distance(previous, current);
                boolean previousTest = this.vertexFilter.test(previous);
                boolean currentTest = this.vertexFilter.test(current);
                boolean bothTest = previousTest && currentTest;
                if (bothTest && !this.vertices.contains(previous)) {
                    this.vertices.add(previous);
                    this.builder.addVertex(previous);
                    this.progress.incrementAcceptedLocationCount();
                }
                if (bothTest && !this.vertices.contains(current)) {
                    this.vertices.add(current);
                    this.builder.addVertex(current);
                    this.progress.incrementAcceptedLocationCount();
                }
                if (bothTest) {
                    try {
                        this.builder.addEdge(previous, current, new RoadEdge(distance, speed));
                        added = true;
                    } catch (IllegalStateException ex) {
                        // ignored
                    }
                    try {
                        this.builder.addEdge(current, previous, new RoadEdge(distance, speed));
                        added = true;
                    } catch (IllegalStateException ex) {
                        // ignored
                    }
                }
                if (added) {
                    if (this.drawLine != null) {
                        this.drawLine.accept(previous, current);
                    }
                    this.progress.incrementAcceptedWayCount();
                }
            }
            previous = current;
        }
    }

    @Override
    public void relation(long id, String version, LocalDateTime timestamp,
                         List<RelationMember> referencedLocations,
                         List<RelationMember> referencedWays,
                         List<RelationMember> referencedRelations,
                         Map<String, String> tags) {
        this.progress.incrementTotalRelationCount();
    }

    private static Speed parseSpeed(Map<String, String> tags) {
        if (tags.containsKey("maxspeed")) {
            String str = tags.get("maxspeed");
            if (str.endsWith("mph")) {
                str = str.substring(0, str.length() - 3);
            }
            str = str.strip();
            try {
                return Speed.ofMilesPerHour(Integer.parseInt(str));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

}
