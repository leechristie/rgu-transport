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

import java.io.*;
import java.util.*;

/**
 * An object which provides methods to load input streams of each of the data files in a GTFS data
 * source.
 *
 * @author Lee A. Christie
 */
public interface GTFSDataSource {

    /**
     * Returns an input stream with the contents of the agency.txt data.
     *
     * @return input stream, not null
     * @throws IOException if the source could not be loaded
     */
    InputStream agency() throws IOException;

    /**
     * Returns an input stream with the contents of the calendar_dates.txt data.
     *
     * @return input stream, not null
     * @throws IOException if the source could not be loaded
     */
    InputStream calendarDates() throws IOException;

    /**
     * Returns an input stream with the contents of the calendar.txt data.
     *
     * @return input stream, not null
     * @throws IOException if the source could not be loaded
     */
    InputStream calendar() throws IOException;

    /**
     * Returns an input stream with the contents of the routes.txt data.
     *
     * @return input stream, not null
     * @throws IOException if the source could not be loaded
     */
    InputStream routes() throws IOException;

    /**
     * Returns an input stream with the contents of the stop_times.txt data.
     *
     * @return input stream, not null
     * @throws IOException if the source could not be loaded
     */
    InputStream stopTimes() throws IOException;

    /**
     * Returns an input stream with the contents of the stops.txt data.
     *
     * @return input stream, not null
     * @throws IOException if the source could not be loaded
     */
    InputStream stops() throws IOException;

    /**
     * Returns an input stream with the contents of the trips.txt data.
     *
     * @return input stream, not null
     * @throws IOException if the source could not be loaded
     */
    InputStream trips() throws IOException;

    /**
     * Returns a GTFS data source which reads from a specified directory
     * in the file system.
     *
     * @return the GTFS data source
     */
    static GTFSDataSource ofFolder(File directory) {
        Objects.requireNonNull(directory, "directory");
        if (!directory.exists()) {
            throw new IllegalArgumentException("directory does not exist");
        }
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("directory is not a directory");
        }
        return new GTFSDataSource() {
            @Override
            public InputStream agency() throws IOException {
                return new FileInputStream(new File(directory, "agency.txt"));
            }
            @Override
            public InputStream calendarDates() throws IOException {
                return new FileInputStream(new File(directory, "calendar_dates.txt"));
            }
            @Override
            public InputStream calendar() throws IOException {
                return new FileInputStream(new File(directory, "calendar.txt"));
            }
            @Override
            public InputStream routes() throws IOException {
                return new FileInputStream(new File(directory, "routes.txt"));
            }
            @Override
            public InputStream stopTimes() throws IOException {
                return new FileInputStream(new File(directory, "stop_times.txt"));
            }
            @Override
            public InputStream stops() throws IOException {
                return new FileInputStream(new File(directory, "stops.txt"));
            }
            @Override
            public InputStream trips() throws IOException {
                return new FileInputStream(new File(directory, "trips.txt"));
            }
            @Override
            public String toString() {
                return directory.toString();
            }
        };
    }

}
