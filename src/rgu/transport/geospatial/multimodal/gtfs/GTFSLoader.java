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

package rgu.transport.geospatial.multimodal.gtfs;

import rgu.transport.algorithms.search.*;
import rgu.transport.geospatial.*;
import rgu.transport.geospatial.multimodal.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Old utility class to load GTFS data files as a TransitNetwork.
 *
 * @author Lee A. Christie
 * @deprecated Use {@link GTFSParser}
 */
@Deprecated(forRemoval = true)
public final class GTFSLoader {

    private GTFSLoader() {
        throw new AssertionError("utility class constructor");
    }

    public static void load(GTFSDataSource source,
                            TransitNetwork.Builder builder,
                            AtomicReference<Runnable> cancel,
                            BiConsumer<GeoLocation, GeoLocation> drawLine,
                            boolean skipMissingInRoute)
            throws IOException, InterruptedException {
        load(source, builder, null, null, null, cancel, drawLine, skipMissingInRoute, null);
    }

    public static void load(GTFSDataSource source,
                            TransitNetwork.Builder builder,
                            AtomicReference<Runnable> cancel,
                            BiConsumer<GeoLocation, GeoLocation> drawLine,
                            boolean skipMissingInRoute,
                            GTFSProgressListener progress)
            throws IOException, InterruptedException {
        load(source, builder, null, null, null, cancel, drawLine, skipMissingInRoute, progress);
    }

    public static void load(GTFSDataSource source,
                            TransitNetwork.Builder builder,
                            Predicate<GeoLocation> spatialFilter,
                            Predicate<LocalTime> temporalFilter,
                            AtomicReference<Runnable> cancel,
                            BiConsumer<GeoLocation, GeoLocation> drawLine,
                            boolean skipMissingInRoute)
            throws IOException, InterruptedException {
        load(source, builder, null, spatialFilter, temporalFilter, cancel, drawLine, skipMissingInRoute, null);
    }

    public static void load(GTFSDataSource source,
                            TransitNetwork.Builder builder,
                            Predicate<GeoLocation> spatialFilter,
                            Predicate<LocalTime> temporalFilter,
                            AtomicReference<Runnable> cancel,
                            BiConsumer<GeoLocation, GeoLocation> drawLine,
                            boolean skipMissingInRoute,
                            GTFSProgressListener progress)
            throws IOException, InterruptedException {
        load(source, builder, null, spatialFilter, temporalFilter, cancel, drawLine,
             skipMissingInRoute, progress);
    }

    public static void load(GTFSDataSource source,
                            TransitNetwork.Builder builder,
                            Set<GeoLocation> points,
                            AtomicReference<Runnable> cancel,
                            BiConsumer<GeoLocation, GeoLocation> drawLine,
                            boolean skipMissingInRoute) throws IOException, InterruptedException {
        load(source, builder, points, null, null, cancel, drawLine, skipMissingInRoute, null);
    }

    public static void load(GTFSDataSource source,
                            TransitNetwork.Builder builder,
                            Set<GeoLocation> points,
                            AtomicReference<Runnable> cancel,
                            BiConsumer<GeoLocation, GeoLocation> drawLine,
                            boolean skipMissingInRoute,
                            GTFSProgressListener progress) throws IOException, InterruptedException {
        load(source, builder, points, null, null, cancel, drawLine, skipMissingInRoute, progress);
    }

    public static void load(GTFSDataSource source,
                            TransitNetwork.Builder builder,
                            Set<GeoLocation> points,
                            Predicate<GeoLocation> spatialFilter,
                            Predicate<LocalTime> temporalFilter,
                            AtomicReference<Runnable> cancel,
                            BiConsumer<GeoLocation, GeoLocation> drawLine,
                            boolean skipMissingInRoute)
            throws IOException, InterruptedException {
        load(source, builder, points, spatialFilter, temporalFilter, cancel, drawLine,
             skipMissingInRoute, null);
    }

    public static void load(GTFSDataSource source,
                            TransitNetwork.Builder builder,
                            Set<GeoLocation> points,
                            Predicate<GeoLocation> spatialFilter,
                            Predicate<LocalTime> temporalFilter,
                            AtomicReference<Runnable> cancel,
                            BiConsumer<GeoLocation, GeoLocation> drawLine,
                            boolean skipMissingInRoute,
                            GTFSProgressListener progress)
            throws IOException, InterruptedException {
        progress.setFilename(source.toString());
        progress.setCancelAction(cancel);
        try {
            Map<String, GeoLocation> stopIDs = new HashMap<>();
            loadStops(stopIDs, source, builder, points, progress, spatialFilter);
            loadTimes(source, stopIDs, builder, progress, temporalFilter, spatialFilter,
                      drawLine, skipMissingInRoute);
        } finally {
            progress.done();
        }
    }

    private static void loadStops(Map<String, GeoLocation> stopIDs,
                                  GTFSDataSource source,
                                  TransitNetwork.Builder builder,
                                  Set<GeoLocation> points,
                                  GTFSProgressListener progress,
                                  Predicate<GeoLocation> spatialFilter)
            throws IOException, InterruptedException {
        Set<GeoLocation> locs = new HashSet<>();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(source.stops()))) {
            String line;
            boolean first = true;
            int stop_id = -1;
            int stop_lat = -1;
            int stop_lon = -1;
            while ((line = in.readLine()) != null) {
                ThreadUtil.checkInterrupt();
                if (first) {
                    List<String> header = List.of(line.split(","));
                    stop_id = header.indexOf("stop_id");
                    stop_lat = header.indexOf("stop_lat");
                    stop_lon = header.indexOf("stop_lon");
                }
                if (!first) {
                    if (progress != null) {
                        progress.incrementTotalStops();
                    }
                    String[] tokens = line.split(",");
                    String stopID = null;
                    GeoLocation location = null;
                    if (!tokens[stop_id].equals("NA")) {
                        stopID = tokens[0];
                    }
                    if (!tokens[stop_lon].equals("NA")
                            && !tokens[stop_lat].equals("NA")) {
                        location = GeoLocation.parseLocationTruncate(
                                tokens[stop_lat], tokens[stop_lon]);
                        if (location.equals(GeoLocation.NULL_ISLAND)) {
                            location = null;
                        }
                    }
                    if (stopID != null && location != null) {
                        if (spatialFilter != null
                                && !spatialFilter.test(location)) {
                            continue;
                        }
                        if (stopIDs.containsKey(stopID)) {
                            throw new IOException("duplicate stopID " + stopID);
                        }
                        stopIDs.put(stopID, location);
                        if (!locs.contains(location)) {
                            builder.addLocation(location);
                            if (progress != null) {
                                progress.incrementAcceptedStops();
                            }
                            locs.add(location);
                            if (points != null) {
                                points.add(location);
                            }
                        }
                    }
                }
                first = false;
            }
        }
    }

    private static void loadTimes(GTFSDataSource source,
            Map<String, GeoLocation> stopIDs,
            TransitNetwork.Builder builder,
                                  GTFSProgressListener progress,
            Predicate<LocalTime> filter,
            Predicate<GeoLocation> spatialFilter,
            BiConsumer<GeoLocation, GeoLocation> drawLine,
            boolean skipMissingInRoute) throws IOException, InterruptedException {
        int skip_on = 0;
        int skip_miss = 0;
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(source.stopTimes()))) {
            String line;
            boolean first = true;
            String previousTripID = null;
            String previousStopID = null;
            LocalTime previousDepartureTime = null;
            int trip_id = -1;
            int arrival_time = -1;
            int departure_time = -1;
            while ((line = in.readLine()) != null) {
                ThreadUtil.checkInterrupt();
                if (first) {
                    List<String> header = List.of(line.split(","));
                    trip_id = header.indexOf("trip_id");
                    arrival_time = header.indexOf("arrival_time");
                    departure_time = header.indexOf("departure_time");
                }
                if (!first) {
                    if (progress != null) {
                        progress.incrementTotalTimes();
                    }
                    String[] tokens = line.split(",");
                    String tripID = tokens[trip_id];
                    if (Integer.parseInt(
                            tokens[arrival_time].split(":")[0]) > 23) {
                        skip_on++;
                        previousTripID = null;
                        previousStopID = null;
                        previousDepartureTime = null;
                        continue;
                    }
                    LocalTime arrival = LocalTime.parse(tokens[1]);
                    if (Integer.parseInt(
                            tokens[departure_time].split(":")[0]) > 23) {
                        skip_on++;
                        previousTripID = null;
                        previousStopID = null;
                        previousDepartureTime = null;
                        continue;
                    }
                    LocalTime departure = LocalTime.parse(tokens[2]);
                    String stopID = tokens[3];
                    if (!stopIDs.containsKey(stopID)) {
                        skip_miss++;
                        if (!skipMissingInRoute) {
                            previousTripID = null;
                            previousStopID = null;
                            previousDepartureTime = null;
                        }
                        continue;
                    }
                    if (tripID.equals(previousTripID)) {
                        if (filter != null &&
                                (!filter.test(previousDepartureTime)
                                        || !filter.test(arrival))) {
                            previousTripID = null;
                            previousStopID = null;
                            previousDepartureTime = null;
                            continue;
                        }
                        GeoLocation from = stopIDs.get(previousStopID);
                        GeoLocation to = stopIDs.get(stopID);
                        if (spatialFilter.test(from) && spatialFilter.test(to)) {
                            builder.addTransit(
                                    from, to, previousDepartureTime, arrival);
                            if (drawLine != null) {
                                drawLine.accept(from, to);
                            }
                            if (progress != null) {
                                progress.incrementAcceptedTimes();
                            }
                        }
                    }
                    previousTripID = tripID;
                    previousStopID = stopID;
                    previousDepartureTime = departure;
                }
                first = false;
            }
        }
        if (skip_on > 0) {
            progress.skippedOvernight(skip_on);
        }
        if (skip_miss > 0) {
            progress.skippedMissingCoordinates(skip_miss);
        }
    }

}
