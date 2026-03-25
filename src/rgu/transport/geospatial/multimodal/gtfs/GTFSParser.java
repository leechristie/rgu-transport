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
import rgu.transport.algorithms.util.*;
import rgu.transport.geospatial.*;
import rgu.transport.geospatial.multimodal.*;
import rgu.transport.util.*;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Utility class to load GTFS data files as a TransitNetwork.
 *
 * @author Lee A. Christie
 */
public class GTFSParser {

    private GTFSParser() {
        throw new AssertionError("utility class constructor");
    }

    /**
     * Loads the specified GTFS data source.
     *
     * @param source the data source, not null
     * @param builder the network builder, not null
     * @param dateRange the data range, not null
     * @param locationFilter the location filter, not null
     * @param allowBrokenRoutes if true, routes can be split up if there are missing stop
     *                          coordinates, not null
     * @param progress the progress listener, not null
     * @param cancel the cancel operation to pass to the progress listener, no null
     * @param edgeLocationConsumer a consumer for pairs of geolocations connected by an edge
     * @throws IOException if an error was encountered in parsing the data source
     * @throws InterruptedException if the thread was interrupted
     */
    public static void load(GTFSDataSource source,
                            TransitNetwork.Builder builder,
                            Range<LocalDate> dateRange,
                            Predicate<GeoLocation> locationFilter,
                            boolean allowBrokenRoutes,
                            GTFSProgressListener progress,
                            AtomicReference<Runnable> cancel,
                            BiConsumer<GeoLocation, GeoLocation> edgeLocationConsumer)
            throws IOException, InterruptedException {

        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(builder, "builder");
        Objects.requireNonNull(dateRange, "dateRange");
        Objects.requireNonNull(locationFilter, "locationFilter");
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(cancel, "cancel");
        Objects.requireNonNull(edgeLocationConsumer, "edgeLocationConsumer");

        // sets up the progress listener
        progress.setFilename(source.toString());
        progress.setCancelAction(cancel);

        // first three stages (calendar, trips, stops)
        Predicate<String> activeServices = dateRange.isAny() ? Times.all()
                                                             : activeServices(source.calendar(),
                                                                              dateRange, progress);
        Set<String> activeTrips = activeTrips(source.trips(), activeServices, progress);
        Map<String, GeoLocation> stops = stops(source.stops(), locationFilter, progress);

        // final stage (stop_times), actually does the building
        times(source.stopTimes(), activeTrips, stops, builder, allowBrokenRoutes, progress,
              edgeLocationConsumer);

        // notifies the progress listener
        progress.done();

    }

    // --- the first three stages (calendar, trips, stops) ---

    // get the list of services (service_id as int) active on (at least one day of) the specified
    // date range
    private static Predicate<String> activeServices(InputStream calendar,
                                                    Range<LocalDate> dateRangeFilter,
                                                    GTFSProgressListener progress)
            throws IOException, InterruptedException {

        Set<String> rv = new HashSet<>();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(calendar))) {

            // processes the header
            Map<String, Integer> header = parseHeader(in, "calendar",
                Set.of("service_id", "start_date", "end_date"));
            String line;
            final int service_id = header.get("service_id");
            final int start_date = header.get("start_date");
            final int end_date = header.get("end_date");

            int lineNum = 2; // 1 is header, 2 is first data line
            while ((line = in.readLine()) != null) {

                ThreadUtil.checkInterrupt();

                // get the values from the required columns
                List<String> tokens = DataIO.splitCommas(line);
                String id = parseString(
                    tokens.get(service_id), "service_id", lineNum, "calendar");
                LocalDate start = parseDate(
                    tokens.get(start_date), "start_date", lineNum, "calendar");
                LocalDate end = parseDate(
                    tokens.get(end_date), "end_date", lineNum, "calendar");

                // create a range
                Range<LocalDate> range;
                try {
                    range = Range.closed(start, end);
                } catch (IllegalArgumentException ex) {
                    throw new IOException("start_date > end_date on line "
                                          + lineNum + " of calendar");
                }

                // accept the entry if in range or if there is no range filter
                if (dateRangeFilter.intersects(range)) {

                    // check for duplicates
                    if (rv.contains(id)) {
                        throw new IOException("duplicate service_id " + id + " of calendar");
                    }

                    // adds ID as the entry overlaps with the required range by at least one day
                    rv.add(id);

                    // notify progress listener of an accepted calendar entry
                    progress.incrementAcceptedCalendarEntry();

                }

                // notify progress listener of a calendar entry (even if rejected)
                progress.incrementTotalCalendarEntry();

                lineNum++;

            }

        }

        return rv::contains;

    }

    // get the list of trips (trip_is as int) corresponding to one of the specified services
    private static Set<String> activeTrips(InputStream trips,
                                           Predicate<String> serviceFilter,
                                           GTFSProgressListener progress)
            throws IOException, InterruptedException {

        Set<String> rv = new HashSet<>();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(trips))) {

            // processes the header
            Map<String, Integer> header = parseHeader(in, "trips", Set.of("service_id", "trip_id"));
            String line;
            final int service_id = header.get("service_id");
            final int trip_id = header.get("trip_id");

            int lineNum = 2; // 1 is header, 2 is first data line
            while ((line = in.readLine()) != null) {

                ThreadUtil.checkInterrupt();

                // get the values from the required columns
                List<String> tokens = DataIO.splitCommas(line);
                String service = parseString(tokens.get(service_id), "service_id", lineNum, "trips");
                String trip = parseString(tokens.get(trip_id), "trip_id", lineNum, "trips");

                // accept the trip if corresponding to one of the given services
                if (serviceFilter.test(service)) {

                    // check for duplicates
                    if (rv.contains(trip)) {
                        throw new IOException("duplicate trip_id " + trip + " of trips");
                    }

                    // adds ID as is a trip for one of the specified services
                    rv.add(trip);

                    // notify progress listener of an accepted trip entry
                    progress.incrementAcceptedTripEntry();

                }

                // notify progress listener of a trip entry (even if rejected)
                progress.incrementTotalTripEntry();

                lineNum++;

            }

        }

        return rv;

    }

    // get the list of stops (which are not at 0, 0)
    private static Map<String, GeoLocation> stops(InputStream stops,
                                                  Predicate<GeoLocation> locationFilter,
                                                  GTFSProgressListener progress)
            throws IOException, InterruptedException {

        Map<String, GeoLocation>  rv = new HashMap<>();
        int nullIslandCount = 0;

        try (BufferedReader in = new BufferedReader(new InputStreamReader(stops))) {

            // processes the header
            Map<String, Integer> header = parseHeader(in, "stops",
                Set.of("stop_id", "stop_lat", "stop_lon"));
            String line;
            final int stop_id = header.get("stop_id");
            final int stop_lat = header.get("stop_lat");
            final int stop_lon = header.get("stop_lon");

            int lineNum = 2; // 1 is header, 2 is first data line
            while ((line = in.readLine()) != null) {

                ThreadUtil.checkInterrupt();

                // get the values from the required columns
                List<String> tokens = DataIO.splitCommas(line);
                String stop = parseString(tokens.get(stop_id), "stop_id", lineNum, "stops");
                Latitude lat = parseLatitude(tokens.get(stop_lat), "stop_lat", lineNum, "stops");
                Longitude lon = parseLongitude(tokens.get(stop_lon), "stop_lon", lineNum, "stops");

                // creates a location
                GeoLocation location = GeoLocation.of(lat, lon);

                // adds the location, if it is not 0, 0, and passes the filter
                if (!location.equals(GeoLocation.NULL_ISLAND) && locationFilter.test(location)) {

                    // check for duplicates
                    if (rv.containsKey(stop)) {
                        throw new IOException("duplicate stop_id " + stop + " of stops");
                    }

                    // add the non-zero location
                    rv.put(stop, location);

                    // NOTE: we don't notify listener of accepted stop, this is done in the stop
                    // times loader later

                } else if (location.equals(GeoLocation.NULL_ISLAND)) {

                    // count zeros
                    nullIslandCount++;

                }

                // notify progress listener of a stop (even if rejected)
                progress.incrementTotalStops();

                lineNum++;

            }

        }

        // notify the progress listener of the number of skipped stops due to missing coordinates
        progress.skippedMissingCoordinates(nullIslandCount);

        return rv;

    }

    // --- the final stage of building (stop_times) ---

    // get the times and build the network
    private static void times(InputStream times,
                              Set<String> activeTrips,
                              Map<String, GeoLocation> stops,
                              TransitNetwork.Builder builder,
                              boolean allowBrokenRoutes,
                              GTFSProgressListener progress,
                              BiConsumer<GeoLocation, GeoLocation> edgeLocationConsumer)
            throws IOException, InterruptedException {

        int overnightCount = 0;

        try (BufferedReader in = new BufferedReader(new InputStreamReader(times))) {

            // processes the header
            Map<String, Integer> header = parseHeader(in, "times",
                    Set.of("trip_id", "arrival_time", "departure_time", "stop_id"));
            String line;
            final int trip_id = header.get("trip_id");
            final int arrival_time = header.get("arrival_time");
            final int departure_time = header.get("departure_time");
            final int stop_id = header.get("stop_id");

            // the data from the previous line of the file
            String previousTrip = "";
            LocalTime previousDeparture = null;
            String previousStop = null;

            int lineNum = 2; // 1 is header, 2 is first data line
            while ((line = in.readLine()) != null) {

                ThreadUtil.checkInterrupt();

                // get the values from the required columns
                List<String> tokens = DataIO.splitCommas(line);
                String trip = parseString(
                    tokens.get(trip_id), "trip_id", lineNum, "times");
                LocalTime arrival = parseTime(
                    tokens.get(arrival_time), "arrival_time", lineNum, "times");
                LocalTime departure = parseTime(
                    tokens.get(departure_time), "departure_time", lineNum, "times");
                String stop = parseString(
                    tokens.get(stop_id), "stop_id", lineNum, "times");

                // if this line is part of the same trip as the previous line
                // and the trip is active
                if (previousTrip.equals(trip) && activeTrips.contains(trip)) {

                    // should not happen when previousTrip == trip
                    if (previousDeparture == null) {
                        throw new AssertionError(
                            "previousTrip == trip && previousDeparture == null");
                    }
                    if (previousStop == null) {
                        throw new AssertionError(
                            "previousTrip == trip && previousStop == null");
                    }

                    // if we are moving forward in time or staying the same
                    if (previousDeparture != null && arrival.compareTo(previousDeparture) >= 0) {

                        // get the actual locations of the stops to connect
                        GeoLocation previousLocation = stops.get(previousStop);
                        GeoLocation location = stops.get(stop);

                        // loc -> loc, we can connect the locations
                        if (previousLocation != null && location != null) {

                            // adds the edge
                            addVertex(builder, previousLocation, progress);
                            addVertex(builder, location, progress);
                            addEdge(builder, previousLocation, location, previousDeparture,
                                    arrival, progress, edgeLocationConsumer);

                            // record the line as the previous line
                            previousTrip = trip;
                            previousDeparture = departure;
                            previousStop = stop;

                        }

                        // if we allow missing stops to break the route
                        if (allowBrokenRoutes) {

                            // record the line as the previous line (even if we did not add an edge)
                            previousTrip = trip;
                            previousDeparture = departure;
                            previousStop = stop;

                        }

                    } else {

                        // we went backwards in time, so record skipped overnight edge
                        overnightCount++;

                    }

                } else {

                    // record the line as the previous line (trip has changed)
                    previousTrip = trip;
                    previousDeparture = departure;
                    previousStop = stop;

                }

                lineNum++;

                // count total times
                progress.incrementTotalTimes();

            }

        }

        // notify progress listener of num overnight skipped
        progress.skippedOvernight(overnightCount);

    }

    // --- code which modifies the builder ---

    private static void addVertex(TransitNetwork.Builder builder,
                                  GeoLocation location,
                                  GTFSProgressListener progress) {
        try {
            builder.addLocation(location);
            progress.incrementAcceptedStops();
        } catch (IllegalStateException ex) {}
    }

    private static void addEdge(TransitNetwork.Builder builder,
                                GeoLocation from,
                                GeoLocation to,
                                LocalTime departure,
                                LocalTime arrival,
                                GTFSProgressListener progress,
                                BiConsumer<GeoLocation, GeoLocation> edgeLocationConsumer) {
        try {
            builder.addTransit(from, to, departure, arrival);
            progress.incrementAcceptedTimes();
            edgeLocationConsumer.accept(from, to);
        } catch (IllegalStateException ex) {}
    }

    // --- Parsing particular cell values from the CSV ---

    // parse a cell as a string (not empty)
    private static String parseString(String cellValue,
                                      String columnName,
                                      int lineNum,
                                      String filename)
            throws IOException {

        // checks that the cell is not empty
        cellValue = cellValue.strip();
        if (cellValue.isEmpty()) {
            throw new IOException(
                "empty cell in " + columnName + " on line " + lineNum + " of " + filename);
        }

        // returns the non-empty cell
        return cellValue;

    }

    // parse a cell as a latitude
    private static Latitude parseLatitude(String cellValue,
                                          String columnName,
                                          int lineNum,
                                          String filename)
            throws IOException {

        // special case for NA
        if (cellValue.equals("NA")) {
            return Latitude.EQUATOR;
        }

        // checks that the cell is not empty
        cellValue = cellValue.strip();
        if (cellValue.isEmpty()) {
            throw new IOException(
                "empty cell in " + columnName + " on line " + lineNum + " of " + filename);
        }

        // parses the latitude
        try {
            return Latitude.parseLatitudeTruncate(cellValue);
        } catch (IllegalArgumentException ex) {
            throw new IOException(
                "invalid latitude in " + columnName + " on line " + lineNum + " of " + filename);
        }

    }

    // parse a cell as a longitude
    private static Longitude parseLongitude(String cellValue,
                                            String columnName,
                                            int lineNum,
                                            String filename)
            throws IOException {

        // special case for NA
        if (cellValue.equals("NA")) {
            return Longitude.PRIME_MERIDIAN;
        }

        // checks that the cell is not empty
        cellValue = cellValue.strip();
        if (cellValue.isEmpty()) {
            throw new IOException(
                "empty cell in " + columnName + " on line " + lineNum + " of " + filename);
        }

        // parses the longitude
        try {
            return Longitude.parseLongitudeTruncate(cellValue);
        } catch (IllegalArgumentException ex) {
            throw new IOException(
                "invalid longitude in " + columnName + " on line " + lineNum + " of " + filename);
        }

    }

    // parse a cell as a non-negative integer (no leading zeros)
    private static int parseNonNegativeInt(String cellValue,
                                           String columnName,
                                           int lineNum,
                                           String filename)
            throws IOException {

        // checks that the cell is not empty
        cellValue = cellValue.strip();
        if (cellValue.isEmpty()) {
            throw new IOException(
                "empty cell in " + columnName + " on line " + lineNum + " of " + filename);
        }

        // special case for 0
        if (cellValue.equals("0")) {
            return 0;
        }

        // checks that there are no leading zeros
        if (cellValue.charAt(0) == '0') {
            throw new IOException(
                "leading zero in " + columnName + " on line " + lineNum + " of " + filename);
        }

        // parses the integer
        try {

            int rv = Integer.parseInt(cellValue);

            // negative int
            if (rv < 0) {
                throw new IOException(
                    "negative integer in " + columnName + " on line " + lineNum + " of " + filename);
            }

            return rv;

        } catch (NumberFormatException ex) {
            throw new IOException(
                "invalid int in " + columnName + " on line " + lineNum + " of " + filename);
        }

    }

    // parse a cell as a time
    private static LocalTime parseTime(String cellValue,
                                       String columnName,
                                       int lineNum,
                                       String filename)
            throws IOException {

        // checks that the cell is not empty
        cellValue = cellValue.strip();
        if (cellValue.isEmpty()) {
            throw new IOException(
                "empty cell in " + columnName + " on line " + lineNum + " of " + filename);
        }

        if (cellValue.length() == 7) {
            cellValue = "0" + cellValue;
        }
        if (cellValue.length() != 8) {
            throw new IOException(
                "invalid date in " + columnName + " on line " + lineNum + " of " + filename);
        }
        if (cellValue.charAt(2) != ':') {
            throw new IOException(
                "invalid date in " + columnName + " on line " + lineNum + " of " + filename);
        }
        String strHour = cellValue.substring(0, 2);
        String strMinSec = cellValue.substring(3);

        // adjust for overnight
        int intHour = Integer.parseInt(strHour);
        if (intHour > 23) {
            intHour -= 24;
        }
        strHour = "" + intHour;
        if (strHour.length() < 2) {
            strHour = "0" + strHour;
        }
        cellValue = strHour + ":" + strMinSec;

        // parses and returns the data as a LocalDate object
        try {
            return LocalTime.parse(cellValue);
        } catch (NumberFormatException | DateTimeException ex) {
            throw new IOException(
                "invalid date in " + columnName + " on line " + lineNum + " of " + filename);
        }

    }

    // parse a cell in the format YYYYMMDD or YYYY-MM-DD
    private static LocalDate parseDate(String cellValue,
                                       String columnName,
                                       int lineNum,
                                       String filename)
            throws IOException {

        // checks that the cell is not empty
        cellValue = cellValue.strip();
        if (cellValue.isEmpty()) {
            throw new IOException(
                "empty cell in " + columnName + " on line " + lineNum + " of " + filename);
        }

        // gets the YYYY, MM, and DD as strings
        String strYear;
        String strMonth;
        String strDay;
        if (cellValue.length() == 8) {

            // reads YYYYMMDD
            strYear = cellValue.substring(0, 4);
            strMonth = cellValue.substring(4, 6);
            strDay = cellValue.substring(6, 8);

        } else if (cellValue.length() == 10) {

            // reads YYYY-MM-DD
            if (cellValue.charAt(4) != '-' || cellValue.charAt(7) != '-') {
                throw new IOException(
                    "invalid date in " + columnName + " on line " + lineNum + " of " + filename);
            }
            strYear = cellValue.substring(0, 4);
            strMonth = cellValue.substring(5, 7);
            strDay = cellValue.substring(8, 10);

        } else {
            throw new IOException(
                "invalid date in " + columnName + " on line " + lineNum + " of " + filename);
        }

        // parses and returns the data as a LocalDate object
        try {
            int intYear = Integer.parseInt(strYear);
            int intMonth = Integer.parseInt(strMonth);
            int intDay = Integer.parseInt(strDay);
            return LocalDate.of(intYear, intMonth, intDay);
        } catch (NumberFormatException | DateTimeException ex) {
            throw new IOException(
                "invalid date in " + columnName + " on line " + lineNum + " of " + filename);
        }

    }

    // --- Parsing a header line from the CSV ---

    // parses a CSV header
    private static Map<String, Integer> parseHeader(BufferedReader in,
                                                    String filename,
                                                    Set<String> required)
            throws IOException {

        // read the header and check that it exists
        String line = in.readLine();
        if (line == null) {
            throw new IOException("missing header of " + filename);
        }

        // parsed return value
        Map<String, Integer> rv = new HashMap<>();

        // get all of the elements of the header
        int i = 0;
        for (String h : DataIO.splitCommas(line)) {
            if (rv.containsKey(h)) {
                throw new IOException("duplicate column header " + h + " of " + filename);
            }
            rv.put(h, i++);
        }

        // checks that all required elements are present
        for (String e : required) {
            if (!rv.containsKey(e)) {
                throw new IOException("missing \"" + e + "\" in header of " + filename);
            }
        }

        // returns the lookup of all header items, required and optional
        return rv;

    }

}
