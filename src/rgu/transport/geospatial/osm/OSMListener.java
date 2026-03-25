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

import rgu.transport.geospatial.*;
import java.time.*;
import java.util.*;

/**
 *
 * @author Lee A. Christie
 */
public interface OSMListener {

    /**
     * Called on loading the OSM version information.
     *
     * @param version the OSM file version, not null
     * @param generator the OSM generator, noy null
     */
    default void version(String version, String generator) {}

    /**
     * Called on loading the map bounds information.
     *
     * @param minlat the minimum latitude, not null
     * @param minlon the minimum longitude, not null
     * @param maxlat the maximum latitude, not null
     * @param maxlon the maximum longitude, not null
     */
    default void bounds(Latitude minlat, Longitude minlon,
                        Latitude maxlat, Longitude maxlon) {}

    /**
     * Called on loading new location (node) information.
     *
     * @param id the unique node ID
     * @param location the node location, not null
     * @param version the version information, not null
     * @param timestamp the timestamp for the node, not null
     * @param tags the tags for the node, not null
     */
    default void location(long id, GeoLocation location,
                          String version, LocalDateTime timestamp,
                          Map<String, String> tags) {}

    /**
     * Called on loading new way information.
     *
     * @param id the unique way ID
     * @param version the version information, not null
     * @param timestamp the timestamp for the way, not null
     * @param points the way points, not null
     * @param tags the tags for the way, not null
     */
    default void way(long id, String version, LocalDateTime timestamp,
                     List<Long> points, Map<String, String> tags) {}

    /**
     * Called on leading new relation information.
     *
     * @param id the unique way ID
     * @param version the version information, not null
     * @param timestamp the timestamp for the way, not null
     * @param referencedLocations the referenced locations, not null
     * @param referencedWays the referenced ways, not null
     * @param referencedRelations the referenced relations, not null
     * @param tags the tags for the way, not null
     */
    default void relation(long id, String version, LocalDateTime timestamp,
                          List<RelationMember> referencedLocations,
                          List<RelationMember> referencedWays,
                          List<RelationMember> referencedRelations,
                          Map<String, String> tags) {}

    /**
     * No-op  listener. For when listener is not needed.
     */
    OSMListener NONE = new OSMListener() {};

}
