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
import java.util.*;
import java.util.function.*;

/**
 * A Geolocation and position in a journey.
 *
 * @author Lee A. Christie
 */
public final class JourneyPosition {

    private final GeoLocation location;
    private final boolean vehicle;
    private final boolean busPass;
    private final boolean artificial;
    private final String hiddenMetaData;

    private JourneyPosition(GeoLocation location,
                            boolean vehicle,
                            boolean busPass,
                            boolean artifical,
                            String hiddenMetaData) {
        this.location = location;
        this.vehicle = vehicle;
        this.busPass = busPass;
        this.artificial = artifical;
        this.hiddenMetaData = hiddenMetaData;
    }

    public static JourneyPosition of(GeoLocation location,
                                  boolean vehicle,
                                  boolean busPass,
                                  boolean artifical,
                                  String hiddenMetaData) {
        Objects.requireNonNull(location, "location");
        return new JourneyPosition(location, vehicle, busPass, artifical, hiddenMetaData);
    }

    public static JourneyPosition of(GeoLocation location,
                                  boolean vehicle,
                                  boolean busPass,
                                  boolean artifical) {
        Objects.requireNonNull(location, "location");
        return new JourneyPosition(location, vehicle, busPass, artifical, "");
    }

    public static JourneyPosition of(GeoLocation location,
                                  boolean vehicle,
                                  boolean busPass,
                                  String hiddenMetaData) {
        Objects.requireNonNull(location, "location");
        return new JourneyPosition(location, vehicle, busPass, false, hiddenMetaData);
    }

    public static JourneyPosition of(GeoLocation location,
                                  boolean vehicle,
                                  boolean busPass) {
        Objects.requireNonNull(location, "location");
        return new JourneyPosition(location, vehicle, busPass, false, "");
    }

    /**
     * Returns the location.
     *
     * @return the location
     */
    public GeoLocation location() {
        return this.location;
    }

    /**
     * Returns whether the traveller has a vehicle.
     *
     * @return whether the traveller has a vehicle
     */
    public boolean vehicle() {
        return this.vehicle;
    }

    /**
     * Returns whether the traveller has a bus pass.
     *
     * @return whether the traveller has a bus pass
     */
    public boolean busPass() {
        return this.busPass;
    }

    public boolean artificial() {
        return this.artificial;
    }

    public String hiddenMetadata() {
        return this.hiddenMetaData;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int rv = 2347 + this.location.hashCode();
        rv += 2347 + Boolean.hashCode(this.vehicle);
        rv += 2347 + Boolean.hashCode(this.busPass);
        rv += 2347 + Boolean.hashCode(this.artificial);
        return rv;
    }

    /**
     * {@inheritDoc}
     *
     * @param o {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JourneyPosition)) {
            return false;
        }
        JourneyPosition other = (JourneyPosition) o;
        if (!this.location.equals(other.location)) {
            return false;
        }
        if (this.vehicle != other.vehicle) {
            return false;
        }
        if (this.busPass != other.busPass) {
            return false;
        }
        if (this.artificial != other.artificial) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public String toString() {
        if (this.vehicle) {
            if (this.busPass) {
                return this.location + " with vehicle and bus pass";
            }
            return this.location + " with vehicle";
        }
        if (this.busPass) {
            return this.location + " with bus pass";
        }
        if (this.artificial) {
            return this.location.toString() + " [artificial]";
        } else {
            return this.location.toString();
        }
    }

    public static Predicate<JourneyPosition> atLocation(GeoLocation location) {
        Objects.requireNonNull(location, "location");
        return (state) -> location.equals(state.location);
    }

    public static Predicate<JourneyPosition> atLocation(GeoLocation location,
                                                     boolean vehicle) {
        Objects.requireNonNull(location, "location");
        return (state) -> location.equals(state.location)
                          && state.vehicle == vehicle;
    }

}
