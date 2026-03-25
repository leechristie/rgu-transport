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

package rgu.transport.geospatial.multimodal;

import rgu.transport.geospatial.*;
import java.time.*;
import java.util.*;

/**
 * Represents a trip, specified by departure time, arrival time, and destination,
 * omitting departure location.
 * @author Lee A. Christie
 */
public final class Trip {

    private final LocalTime departureTime;
    private final LocalTime arrivalTime;
    private final GeoLocation destination;

    @Override
    public int hashCode() {
        return Objects.hash(this.arrivalTime, this.departureTime, this.destination);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Trip)) {
            return false;
        }
        Trip other = (Trip) obj;
        return Objects.equals(this.arrivalTime, other.arrivalTime)
                && Objects.equals(this.departureTime, other.departureTime)
                && Objects.equals(this.destination, other.destination);
    }

    /**
     * Creates a trip.
     *
     * @param departureTime the departure time, not null
     * @param arrivalTime the arrival time, not null
     * @param destination the destination, not null
     */
    public Trip(LocalTime departureTime,
                LocalTime arrivalTime,
                GeoLocation destination) {
        Objects.requireNonNull(departureTime, "departureTime");
        Objects.requireNonNull(arrivalTime, "arrivalTime");
        Objects.requireNonNull(destination, "destination");
        if (departureTime.isAfter(arrivalTime)) {
            throw new IllegalArgumentException("departureTime > arrivalTime");
        }
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.destination = destination;
    }

    /**
     * Returns the departure time.
     *
     * @return the departure time
     */
    public LocalTime departureTime() {
        return this.departureTime;
    }
    /**
     * Returns the arrival time.
     *
     * @return the arrival time
     */

    public LocalTime arrivalTime() {
        return this.arrivalTime;
    }

    /**
     * Returns the destination.
     *
     * @return the destination
     */
    public GeoLocation destination() {
        return this.destination;
    }

    @Override
    public String toString() {
        return "Trip[departureTime=" + departureTime + ", arrivalTime=" + arrivalTime
                + ", destination=" + destination + "]";
    }

}
