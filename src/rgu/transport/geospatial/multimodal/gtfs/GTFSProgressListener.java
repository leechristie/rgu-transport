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

import java.util.concurrent.atomic.*;

/**
 * A listener interface for GTFS loading progress.
 *
 * @author Lee A. Christie
 */
public interface GTFSProgressListener {

    /**
     * Called when the filename is known.
     *
     * @param filename the filename, not null
     */
    default void setFilename(String filename) {}

    /**
     * Called to indicate that invoking the specified runnable will cancel the loading, the action
     * may be updated later, so the listener should hold the atomic reference.
     *
     * @param cancel the cancel action, not null but may contain a null atomic reference if there is
     *               no cancel option
     */
    default void setCancelAction(AtomicReference<Runnable> cancel) {}

    /**
     * Called to indicate that invoking the specified runnable will cancel the loading.
     *
     * @param cancel the cancel action, may be null if there is not cancel option
     */
    default void setCancelAction(Runnable cancel) {}

    /**
     * Called when a stop has been processed.
     */
    default void incrementTotalStops() {}

    /**
     * Called when a stop has been accepted.
     */
    default void incrementAcceptedStops() {}

    /**
     * Called when a time has been processed.
     */
    default void incrementTotalTimes() {}

    /**
     * Called when a time has be accepted.
     */
    default void incrementAcceptedTimes() {}

    /**
     * Called to notify of the number of skipped overnight times.
     *
     * @param count the number of skipped times, &gt;= 0
     */
    default void skippedOvernight(int count) {}

    /**
     * Called to notify of the number of skipped missing coordinate stops.
     *
     * @param count the number of skipped stops, &gt;= 0
     */
    default void skippedMissingCoordinates(int count) {}

    /**
     * Called when the loading process is over.
     */
    default void done() {}

    /**
     * Called when a service calendar entry has been processed.
     */
    default void incrementTotalCalendarEntry() {}

    /**
     * Called when a service calendar entry has been accepted.
     */
    default void incrementAcceptedCalendarEntry() {}

    /**
     * Called when a trip entry has been processed.
     */
    default void incrementTotalTripEntry() {}

    /**
     * Called when a trip entry has been accepted.
     */
    default void incrementAcceptedTripEntry() {}

    /**
     * No-op progress listener. For when progress listener is not needed.
     */
    GTFSProgressListener NONE = new GTFSProgressListener() {};

}
