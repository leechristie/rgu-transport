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

import rgu.transport.algorithms.search.*;
import rgu.transport.geospatial.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

/**
 * Utility class to read OSM XML files.
 *
 * @author Lee A. Christie
 */
public final class OSMReader {

    private OSMReader() {
        throw new AssertionError("Utility class constructor called.");
    }

    /**
     * Reads OSM data from the specified file to the specified OSM
     * listener.
     *
     * @param listener the listener, not null
     * @throws IOException if unable to read the OSM data
     */
    public static void read(File file, OSMListener listener)
            throws IOException, InterruptedException {

        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(listener, "listener");

        try (InputStream stream = new FileInputStream(file)) {
            read(stream, listener);
        }

    }

    /**
     * Reads OSM data from the specified input stream to the specified OSM listener. The stream is
     * not closed by the reader.
     *
     * @param stream the input stream, not null
     * @param listener the listener, not null
     * @throws IOException if unable to read the OSM data
     */
    public static void read(InputStream stream, OSMListener listener)
            throws IOException, InterruptedException {

        Objects.requireNonNull(stream, "stream");
        Objects.requireNonNull(listener, "listener");

        Map<String, Map<String, String>> deferred = new HashMap<>();
        List<RelationMember> refLoc = new ArrayList<>();
        List<RelationMember> refWay = new ArrayList<>();
        List<RelationMember> refRel = new ArrayList<>();
        Map<String, String> tags = new HashMap<>();
        List<Long> points = new ArrayList<>();

        forEachEvent(

            stream,

            // start element
            (name, attributes) -> {

                if (name.equals("osm")) {

                    listener.version(
                        attributes.get("version"),
                        attributes.get("generator")
                    );

                } else if (name.equals("bounds")) {

                    listener.bounds(
                        Latitude.parseLatitude(attributes.get("minlat")),
                        Longitude.parseLongitude(attributes.get("minlon")),
                        Latitude.parseLatitude(attributes.get("maxlat")),
                        Longitude.parseLongitude(attributes.get("maxlon"))
                    );

                } else if (name.equals("node")) {

                    if (deferred.containsKey("node")) {
                        throw new IllegalStateException("<node> within <node>");
                    }
                    deferred.put("node", attributes);

                } else if (name.equals("way")) {

                    if (deferred.containsKey("way")) {
                        throw new IllegalStateException("<way> within <way>");
                    }
                    deferred.put("way", attributes);

                } else if (name.equals("relation")) {

                    if (deferred.containsKey("relation")) {
                        throw new IllegalStateException(
                                "<relation> within <relation>");
                    }
                    deferred.put("relation", attributes);

                } else if (name.equals("tag")) {

                    tags.put(attributes.get("k"), attributes.get("v"));

                } else if (name.equals("nd")) {

                    points.add(Long.valueOf(attributes.get("ref")));

                } else if (name.equals("member")) {

                    String type = attributes.get("type");
                    String role = attributes.get("role");
                    long reference = Long.valueOf(attributes.get("ref"));
                    RelationMember entry = RelationMember.of(reference, role);

                    if (type.equals("node")) {

                        refLoc.add(entry);

                    } else if (type.equals("way")) {

                        refWay.add(entry);

                    } else if (type.equals("relation")) {

                        refRel.add(entry);

                    } else {

                        throw new IllegalArgumentException(
                                "bad member type: " + attributes.get("type"));

                    }

                }

            },

            // end element
            (name) -> {

                if (name.equals("node")) {

                    if (!deferred.containsKey("node")) {
                        throw new IllegalStateException("missing <node>");
                    }
                    Map<String, String> attributes = deferred.get("node");
                    deferred.remove("node");

                    long id = Long.parseLong(attributes.get("id"));
                    String version = attributes.get("version");
                    String unparsed = attributes.get("timestamp");
                    LocalDateTime timestamp = LocalDateTime.parse(
                            unparsed.substring(0, unparsed.length()-1));
                    Latitude latitude = Latitude.parseLatitude(
                            attributes.get("lat"));
                    Longitude longitude = Longitude.parseLongitude(
                            attributes.get("lon"));
                    GeoLocation location = GeoLocation.of(latitude, longitude);

                    if (tags.isEmpty()) {
                        listener.location(id, location, version, timestamp,
                                Map.of());
                    } else {
                        listener.location(id, location, version, timestamp,
                                          new HashMap<>(tags));
                        tags.clear();
                    }

                } else if (name.equals("way")) {

                    if (!deferred.containsKey("way")) {
                        throw new IllegalStateException("missing <way>");
                    }
                    Map<String, String> attributes = deferred.get("way");
                    deferred.remove("way");

                    long id = Long.parseLong(attributes.get("id"));
                    String version = attributes.get("version");
                    String unparsed = attributes.get("timestamp");
                    LocalDateTime timestamp = LocalDateTime.parse(
                            unparsed.substring(0, unparsed.length()-1));

                    if (tags.isEmpty()) {
                        listener.way(id, version, timestamp,
                                new ArrayList<>(points),
                                Map.of());
                    } else {
                        listener.way(id, version, timestamp,
                                     new ArrayList<>(points),
                                     new HashMap<>(tags));
                        tags.clear();
                    }

                    points.clear();

                } else if (name.equals("relation")) {

                    if (!deferred.containsKey("relation")) {
                        throw new IllegalStateException("missing <relation>");
                    }
                    Map<String, String> attributes = deferred.get("relation");
                    deferred.remove("relation");

                    long id = Long.parseLong(attributes.get("id"));
                    String version = attributes.get("version");
                    String unparsed = attributes.get("timestamp");
                    LocalDateTime timestamp = LocalDateTime.parse(
                            unparsed.substring(0, unparsed.length()-1));

                    List<RelationMember> currentRefLoc =
                            refLoc.isEmpty() ? List.of() : new LinkedList<>(refLoc);
                    List<RelationMember> currentRefWay =
                            refWay.isEmpty() ? List.of() : new LinkedList<>(refWay);
                    List<RelationMember> currentRefRel =
                            refRel.isEmpty() ? List.of() : new LinkedList<>(refRel);
                    Map<String, String> currentTags =
                            tags.isEmpty() ? Map.of() : new HashMap<>(tags);
                    refLoc.clear();
                    refWay.clear();
                    tags.clear();

                    listener.relation(id, version, timestamp,
                                      currentRefLoc,
                                      currentRefWay,
                                      currentRefRel,
                                      currentTags);

                }

            }

        );

    }

    // wrapper for all the XML boilerplate
    private static void forEachEvent(InputStream stream,
            BiConsumer<String, Map<String, String>> start,
            Consumer<String> end) throws IOException, InterruptedException {

        try {

            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLEventReader reader = xmlInputFactory.createXMLEventReader(
                    stream, "UTF-8");
            while (reader.hasNext()) {
                ThreadUtil.checkInterrupt();
                XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) {
                    StartElement element = event.asStartElement();
                    String name = element.getName().getLocalPart();
                    Map<String, String> attributes = new HashMap<>();
                    Iterator<Attribute> it = element.getAttributes();
                    while (it.hasNext()) {
                        Attribute a = it.next();
                        attributes.put(a.getName().getLocalPart().toString(),
                                       a.getValue());
                    }
                    start.accept(name, attributes);
                } else if (event.isEndElement()) {
                    EndElement element = event.asEndElement();
                    String name = element.getName().getLocalPart();
                    end.accept(name);
                }
            }

        } catch (XMLStreamException exception) {
            throw new IOException(exception);
        }

    }

}
