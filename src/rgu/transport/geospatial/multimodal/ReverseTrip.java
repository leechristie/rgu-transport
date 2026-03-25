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

// package class for reverse version of trip class
// author Lee A. Christie
final class ReverseTrip {

    private final LocalTime departureTime;
    private final LocalTime arrivalTime;
    private final GeoLocation source;

    @Override
    public int hashCode() {
        return Objects.hash(this.arrivalTime, this.departureTime, this.source);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ReverseTrip)) {
            return false;
        }
        ReverseTrip other = (ReverseTrip) obj;
        return Objects.equals(this.arrivalTime, other.arrivalTime)
               && Objects.equals(this.departureTime, other.departureTime)
               && Objects.equals(this.source, other.source);
    }

    public ReverseTrip(LocalTime departureTime,
                       LocalTime arrivalTime,
                       GeoLocation source) {
        Objects.requireNonNull(departureTime, "departureTime");
        Objects.requireNonNull(arrivalTime, "arrivalTime");
        Objects.requireNonNull(source, "source");
        if (departureTime.isAfter(arrivalTime)) {
            throw new IllegalArgumentException("departureTime > arrivalTime");
        }
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.source = source;
    }

    public LocalTime departureTime() {
        return this.departureTime;
    }

    public LocalTime arrivalTime() {
        return this.arrivalTime;
    }

    public GeoLocation source() {
        return this.source;
    }

    @Override
    public String toString() {
        return "Trip[departureTime=" + departureTime + ", arrivalTime="
               + arrivalTime + ", source=" + source + "]";
    }

}
