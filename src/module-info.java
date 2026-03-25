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

/**
 * RGU transport modeling library.
 */
module rgu.transport {

    requires java.xml;

    exports rgu.transport.geospatial;
    exports rgu.transport.geospatial.multimodal;
    exports rgu.transport.geospatial.multimodal.gtfs;
    exports rgu.transport.geospatial.osm;
    exports rgu.transport.util;
    exports rgu.transport.algorithms.collections;
    exports rgu.transport.algorithms.util;
    exports rgu.transport.algorithms.search;

}
