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
import java.util.function.*;

/**
 * Represents a state of a journey, as location, time, and vehicle state.
 *
 * @author Lee A. Christie
 */
public final class JourneyState {

    private final JourneyPosition position;
    private final LocalDateTime time;

    private JourneyState(JourneyPosition position, LocalDateTime time) {
        this.position = position;
        this.time = time;
    }

    /**
     * Returns a journey state for the given location and time.
     *
     * @param location the location, not null
     * @param time the time, not null
     * @param vehicle whether the traveller has a vehicle
     * @return a journey state instance
     */
    public static JourneyState of(GeoLocation location,
                                  LocalDateTime time,
                                  boolean vehicle,
                                  boolean busPass) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(time, "time");
        JourneyPosition position = JourneyPosition.of(location, vehicle, busPass, false);
        return new JourneyState(position, time);
    }

    public static JourneyState of(GeoLocation location,
            LocalDateTime time,
            boolean vehicle,
            boolean busPass,
            String hiddenMetaData) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(time, "time");
        JourneyPosition position
            = JourneyPosition.of(location, vehicle, busPass, false, hiddenMetaData);
        return new JourneyState(position, time);
    }

    /**
     * Returns a journey state for the given location and time.
     *
     * @param location the location, not null
     * @param time the time, not null
     * @param vehicle whether the traveller has a vehicle
     * @return a journey state instance
     */
    public static JourneyState of(GeoLocation location,
                                  LocalDateTime time,
                                  boolean vehicle,
                                  boolean busPass,
                                  boolean artificial) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(time, "time");
        JourneyPosition position
                = JourneyPosition.of(location, vehicle, busPass, artificial);
        return new JourneyState(position, time);
    }

    public static JourneyState of(GeoLocation location,
            LocalDateTime time,
            boolean vehicle,
            boolean busPass,
            boolean artificial,
            String hiddenMetaData) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(time, "time");
        JourneyPosition position
                = JourneyPosition.of(location, vehicle, busPass, artificial, hiddenMetaData);
        return new JourneyState(position, time);
    }

    /**
     * Returns a journey state for the given location and time.
     *
     * @param position the journey position, not null
     * @param time the time, not null
     * @return a journey state instance
     */
    public static JourneyState of(JourneyPosition position,
                                  LocalDateTime time) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(time, "time");
        return new JourneyState(position, time);
    }

    public static JourneyState of(JourneyPosition position,
                                  LocalDateTime start,
                                  Duration duration) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(duration, "duration");
        return new JourneyState(position, start.plus(duration));
    }

    /**
     * Returns the position.
     *
     * @return the position
     */
    public JourneyPosition position() {
        return this.position;
    }

    /**
     * Returns the location.
     *
     * @return the location
     */
    public GeoLocation location() {
        return this.position.location();
    }

    /**
     * Returns the time.
     *
     * @return the time
     */
    public LocalDateTime time() {
        return this.time;
    }

    /**
     * Returns whether the traveller has a vehicle.
     *
     * @return whether the traveller has a vehicle
     */
    public boolean vehicle() {
        return this.position.vehicle();
    }

    public boolean busPass() {
        return this.position.busPass();
    }

    public boolean artificial() {
        return this.position.artificial();
    }

    public String hiddenMetadata() {
        return this.position.hiddenMetadata();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int rv = 2347 * 1 + this.position.hashCode();
        rv = 2347 * 1 + this.time.hashCode();
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
        if (!(o instanceof JourneyState)) {
            return false;
        }
        JourneyState other = (JourneyState) o;
        if (!this.position.equals(other.position)) {
            return false;
        }
        if (!this.time.equals(other.time)) {
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
        return this.position + " @ " + this.time;
    }

    /**
     * Creates a predicate which matches any JourneyState which is at the
     * specified location.
     *
     * @param location the target location, not null
     * @return a predicate
     */
    public static Predicate<JourneyState> atLocation(GeoLocation location) {
        Objects.requireNonNull(location, "location");
        return (state) -> location.equals(state.position.location());
    }

    /**
     * Creates a predicate which matches any JourneyState which is at the
     * specified location with the specified vehicle state.
     *
     * @param location the target location, not null
     * @param vehicle whether or not the traveller has a vehicle
     * @return a predicate
     */
    public static Predicate<JourneyState> atLocation(GeoLocation location,
                                                     boolean vehicle) {
        Objects.requireNonNull(location, "location");
        return (state) -> location.equals(state.position.location())
                          && state.position.vehicle() == vehicle;
    }

}
