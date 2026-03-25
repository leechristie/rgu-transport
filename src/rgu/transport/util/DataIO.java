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

package rgu.transport.util;

import rgu.transport.algorithms.collections.*;
import rgu.transport.geospatial.*;
import rgu.transport.geospatial.multimodal.*;
import rgu.transport.geospatial.osm.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

/**
 * Methods for custom serialization code.
 *
 * @author Lee A. Christie
 */
public final class DataIO {

    /**
     * Splits a string on commas, removing quotation marks around entries.
     *
     * @param string a comma-seperated string, not null
     * @return the entries
     */
    public static List<String> splitCommas(String string) {
        Objects.requireNonNull(string, "string");
        List<String> rv = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder buffer = new StringBuilder();
        for (char c : string.strip().toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',') {
                if (inQuotes) {
                    buffer.append(c);
                } else {
                    if (buffer.isEmpty()) {
                        rv.add("");
                    } else {
                        rv.add(buffer.toString());
                        buffer = new StringBuilder();
                    }
                }
            } else {
                buffer.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("mismatched quotes in string: " + string);
        }
        rv.add(buffer.toString());
        return rv;
    }

    /**
     * Writes the given roads graph to the specified file.
     *
     * @param file the file to write to, not null
     * @param graph the graph to write, not null
     * @throws IOException if an exception occurred writing
     */
    public static void writeRoads(File file, Graph<GeoLocation, RoadEdge> graph) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(graph, "graph");
        {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                Map<GeoLocation, Integer> vertices = new HashMap<>();
                int i = 0;
                out.writeInt(graph.vertices().size());
                // serializing vertices
                for (GeoLocation vertex : graph.vertices()) {
                    vertices.put(vertex, i++);
                    vertex.write(out);
                }
                // serialized `i` vertices
                // serializing edges
                //int edgeCount = 0;
                for (GeoLocation a : graph.vertices()) {
                    for (GeoLocation b : graph.neighbours(a)) {
                        RoadEdge edge = graph.edge(a, b);
                        int indexA = vertices.get(a);
                        int indexB = vertices.get(b);
                        out.writeInt(indexA);
                        out.writeInt(indexB);
                        edge.write(out);
                        //edgeCount++;
                    }
                }
                // serialized `edgeCount` edges
                out.flush();
            }
        }
    }

    /**
     * Reads a road graph from the specified file.
     *
     * @param file the file, not null
     * @return the road graph
     * @throws IOException if an exception occurred reading
     */
    public static Graph<GeoLocation, RoadEdge> readRoads(File file) throws IOException {
        return readRoads(file, null);
    }

    /**
     * Reads a road graph from the specified file, with an optional consumer for edges.
     *
     * @param file the file, not null
     * @param edgeConsumer a callback for the edges, can be null if not used
     * @return the road graph
     * @throws IOException if an exception occurred reading
     */
    public static Graph<GeoLocation, RoadEdge> readRoads(File file,
                BiConsumer<GeoLocation, GeoLocation> edgeConsumer)
            throws IOException {
        return readRoads(file, null, edgeConsumer);
    }

    /**
     * Reads a road graph from the specified file, with an optional consumer for edges and spatial
     * filter.
     *
     * @param file the file, not null
     * @param spatialFilter a filter for which points to load, can be null if not used
     * @param edgeConsumer a callback for the edges, can be null if not used
     * @return the road graph
     * @throws IOException if an exception occurred reading
     */
    public static Graph<GeoLocation, RoadEdge> readRoads(File file,
                Predicate<GeoLocation> spatialFilter,
                BiConsumer<GeoLocation, GeoLocation> edgeConsumer)
            throws IOException {
        Objects.requireNonNull(file, "file");
        if (spatialFilter == null) {
            spatialFilter = (g) -> true;
        }
        Graph<GeoLocation, RoadEdge> rv;
        {
            HashGraph.Builder<GeoLocation, RoadEdge> builder = HashGraph.builder();
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                int numVertices = in.readInt();
                List<GeoLocation> vertices = new ArrayList<>();
                // deserializing vertices
                for (int i = 0; i < numVertices; i++) {
                    GeoLocation vertex = GeoLocation.read(in);
                    vertices.add(vertex);
                    if (spatialFilter.test(vertex)) {
                        builder.addVertex(vertex);
                    }
                }
                // deserialized `vertices.size()` vertices
                // deserializing edges
                //int edgeCount = 0;
                try {
                    while (true) {
                        GeoLocation a = vertices.get(in.readInt());
                        GeoLocation b = vertices.get(in.readInt());
                        RoadEdge edge = RoadEdge.read(in);
                        if (spatialFilter.test(a) && spatialFilter.test(b)) {
                            builder.addEdge(a, b, edge);
                            //edgeCount++;
                            if (edgeConsumer != null) {
                                edgeConsumer.accept(a, b);
                            }
                        }
                    }
                } catch (EOFException eof) {
                    // deserialized `edgeCount` edges
                }
            }
            rv = builder.build();
        }
        return rv;
    }

    /**
     * Writes the given network to the specified file.
     *
     * @param file the file, not null
     * @param network the network, not null
     * @throws IOException if an exception occurred writing
     */
    public static void writeNetwork(File file, TransitNetwork network) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(network, "network");
        {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                network.write(out);
            }
        }
    }

    /**
     * Reads a transit network from the specified file.
     *
     * @param file the file, not null
     * @return the network
     * @throws IOException if an exception occurred reading
     */
    public static TransitNetwork readNetwork(File file) throws IOException {
        return readNetwork(file, null, null);
    }

    /**
     * Reads a transit network from the specified file, with optional consumers for edges.
     *
     * @param file the file, not null
     * @param edgeConsumer a callback for the edges, can be null if not used
     * @param edgeConsumerArtificial a callback for the artificial edges, can be null if not used
     * @return the network
     * @throws IOException if an exception occurred reading
     */
    public static TransitNetwork readNetwork(File file,
            BiConsumer<GeoLocation, GeoLocation> edgeConsumer,
            BiConsumer<GeoLocation, GeoLocation> edgeConsumerArtificial) throws IOException {
        return readNetwork(file, null, edgeConsumer, edgeConsumerArtificial);
    }

    /**
     * Reads a transit network from the specified file, with optional consumers for edges and
     * spatial filter.
     *
     * @param file the file, not null
     * @param spatialFilter a filter for whether to include a location, can be null if not used
     * @param edgeConsumer a callback for the edges, can be null if not used
     * @param edgeConsumerArtificial a callback for the artificial edges, can be null if not used
     * @return the network
     * @throws IOException if an exception occurred reading
     */
    public static TransitNetwork readNetwork(File file,
            Predicate<GeoLocation> spatialFilter,
            BiConsumer<GeoLocation, GeoLocation> edgeConsumer,
            BiConsumer<GeoLocation, GeoLocation> edgeConsumerArtificial) throws IOException {
        Objects.requireNonNull(file, "file");
        TransitNetwork rv;
        {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                rv = TransitNetwork.read(in, spatialFilter, edgeConsumer, edgeConsumerArtificial);
            }
        }
        return rv;
    }

}
