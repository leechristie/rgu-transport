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

package rgu.transport.geospatial;

import java.io.*;
import java.util.*;

/**
 * Represents a physical distance.
 *
 * @author Lee A. Christie
 */
public final class Distance implements Comparable<Distance> {

    /**
     * A distance of 0.
     */
    public static Distance ZERO = new Distance(0.0);

    private final double meters;

    private Distance(double meters) {
        this.meters = meters;
    }

    /**
     * Returns an distance specified by meters.
     *
     * @param meters the distance
     * @return a distance
     */
    public static Distance ofMeters(double meters) {
        if (meters == 0.0) {
            return ZERO;
        }
        if (!Double.isFinite(meters) || meters < 0.0) {
            throw new IllegalArgumentException(
                    "meters = " + meters + ", expected [0, inf)");
        }
        return new Distance(meters);
    }

    /**
     * Returns an distance specified by kilometers.
     *
     * @param kilometers the distance
     * @return a distance
     */
    public static Distance ofKilometers(double kilometers) {
        return ofMeters(kilometers * 1000.0);
    }

    /**
     * Returns an distance specified by miles.
     *
     * @param miles the distance
     * @return a distance
     */
    public static Distance ofMiles(double miles) {
        return ofMeters(miles * 1609.34);
    }

    /**
     * Returns the distance as meters.
     *
     * @return the distance
     */
    public double asMeters() {
        return this.meters;
    }

    /**
     * Returns the distance as kilometers.
     *
     * @return the distance
     */
    public double asKilometers() {
        return this.meters / 1000;
    }

    /**
     * Returns the distance as miles.
     *
     * @return the distance
     */
    public double asMiles() {
        return this.meters / 1609.34;
    }

    /**
     * Returns a string representation of the distance as meters.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return this.meters + " m";
    }

    /**
     * Compares this distance to another distance.
     *
     * @param other the other distance, not null
     * @return 0, -1, or 1 based on Comparable definition
     */
    @Override
    public int compareTo(Distance other) {
        Objects.requireNonNull(other, "other");
        return Double.compare(this.meters, other.meters);
    }

    /**
     * Returns a hash code for this object.
     *
     * @return a hash code
     */
    @Override
    public int hashCode() {
        return 1831 + 22039 * Double.hashCode(this.meters);
    }

    /**
     * Checks if the other object represents the same distance.
     *
     * @param other another object
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof Distance) {
            return this.meters == ((Distance) other).meters;
        }
        return false;
    }

    /**
     * Returns the sum of this distance and another distance.
     *
     * @param other the other distance
     * @return the sum
     */
    public Distance plus(Distance other) {
        Objects.requireNonNull(other, "other");
        return ofMeters(this.meters + other.meters);
    }

    /**
     * Writes this distance to the specified output stream.
     *
     * @param out the output stream, not null
     * @throws IOException if an exception occurred writing
     */
    public void write(ObjectOutputStream out) throws IOException {
        out.writeDouble(meters);
    }

    /**
     * Reads a distance from the specified input stream.
     *
     * @param in the input stream, not null
     * @return the read distance
     * @throws IOException if an exception occurred reading
     */
    public static Distance read(ObjectInputStream in) throws IOException {
        return ofMeters(in.readDouble());
    }

}
