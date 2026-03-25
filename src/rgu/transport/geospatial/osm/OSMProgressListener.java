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

/**
 *
 * @author Lee A. Christie
 */
public interface OSMProgressListener {

    default void setCancelEvent(Runnable r) {}

    default void incrementTotalLocationCount() {}

    default void incrementTotalWayCount() {}

    default void incrementAcceptedLocationCount() {}

    default void incrementAcceptedWayCount() {}

    default void incrementTotalRelationCount() {}

    default void done() {}

    /**
     * No-op progress listener. For when progress listener is not needed.
     */
    OSMProgressListener NONE = new OSMProgressListener() {};

}
