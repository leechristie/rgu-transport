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
import java.math.*;
import java.util.*;

/**
 * Represents a Longitude from -180 degrees (exclusive) to +180 degrees (inclusive) to 7 decimal
 * places.
 *
 * @author Lee A. Christie
 */
public final class Longitude implements Comparable<Longitude> {

    /**
     * The value 0.
     */
    public static final Longitude PRIME_MERIDIAN = new Longitude(0);

    final static int SCALE = 7;
    private final static BigDecimal MULTIPLIER = new BigDecimal("10").pow(SCALE);
    final static double MULTIPLIER_DOUBLE = MULTIPLIER.doubleValue();

    private final static BigDecimal MINIMUM = new BigDecimal("-180");
    private final static BigDecimal MAXIMUM = new BigDecimal("180");

    private final static int MINIMUM_INTERNAL = MINIMUM.multiply(MULTIPLIER).intValueExact();
    private final static int MAXIMUM_INTERNAL = MAXIMUM.multiply(MULTIPLIER).intValueExact();

    final int intValue;

    Longitude(int intValue) {
        this.intValue = intValue;
    }

    /**
     * Parses the specified string to a longitude.
     *
     * @param string the longitude as a string
     * @return a Longitude
     */
    public static Longitude parseLongitude(String string) {
        Objects.requireNonNull(string, "string");
        final BigDecimal decimal = new BigDecimal(string);
        if (decimal.compareTo(MINIMUM) <= 0 || decimal.compareTo(MAXIMUM) > 0) {
            throw new IllegalArgumentException("longitude = " + decimal
                        + ", expected: within the half-open range (-180, 180]");
        }
        final int intValue;
        try {
            intValue = decimal.multiply(MULTIPLIER).intValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(
                    "longitude = " + decimal
                    + ", expected: 7 or fewer decimal places.", ex);
        }
        return new Longitude(intValue);
    }

    /**
     * Returns a longitude of the specified decimal value.
     *
     * @param decimal the longitude as a decimal
     * @return a Longitude
     */
    public static Longitude of(BigDecimal decimal) {
        Objects.requireNonNull(decimal, "decimal");
        if (decimal.compareTo(MINIMUM) <= 0 || decimal.compareTo(MAXIMUM) > 0) {
            throw new IllegalArgumentException(
                    "decimal = " + decimal + ", expected: -180 to 180");
        }
        final int intValue;
        try {
            intValue = decimal.multiply(MULTIPLIER).intValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(
                    "decimal = " + decimal
                    + ", expected: 7 or fewer decimal places.", ex);
        }
        return new Longitude(intValue);
    }

    /**
     * Rounds the specified double to a longitude.
     *
     * @param value the longitude as a double, finite
     * @return a Longitude
     */
    public static Longitude ofRounded(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value is not finite");
        }
        value = Math.round(value * 10_000_000.0) / 10_000_000.0;
        return parseLongitudeTruncate(Double.toString(value));
    }

    /**
     * Parses the specified string to a longitude.
     *
     * @param string the longitude as a string
     * @return a Longitude
     */
    public static Longitude parseLongitudeTruncate(String string) {
        Objects.requireNonNull(string, "string");
        final BigDecimal decimal = new BigDecimal(string);
        if (decimal.compareTo(MINIMUM) <= 0 || decimal.compareTo(MAXIMUM) > 0) {
            throw new IllegalArgumentException(
                    "decimal = " + decimal + ", expected: -180 to 180");
        }
        final int intValue = decimal.multiply(MULTIPLIER).intValue();
        return new Longitude(intValue);
    }

    /**
     * Returns the longitude as a decimal to 7 decimal places.
     * For example parseLongitude("-2.0943").doubleValue() returns -2.0943000
     *
     * @return the longitude as a decimal
     */
    public BigDecimal decimalValue() {
        return BigDecimal.valueOf(this.intValue, SCALE);
    }

    /**
     * Returns the longitude as a double.
     * For example parseLongitude("-2.0943").doubleValue() returns -2.0943
     *
     * @return the longitude as a double
     */
	public double doubleValue() {
		return this.intValue / MULTIPLIER_DOUBLE;
	}

	/**
	 * Returns a hash code for this longitude.
	 *
	 * @return a hash code
	 */
    @Override
    public int hashCode() {
        return 58153 + 42193 * Integer.hashCode(this.intValue);
    }

    /**
     * Returns true if the other object is the same longitude value.
     *
     * @param other the other object, not null
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object other) {
    	if (other == null) {
    		return false;
    	}
    	if (other instanceof Longitude) {
            return ((Longitude) other).intValue == this.intValue;
        }
        return false;
    }

    /**
     * Returns a String object representing this longitude's value.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return this.decimalValue().stripTrailingZeros().toPlainString();
    }

    /**
     * Compares two longitudes numerically.
     *
     * @return 0 if this longitude is the same as the other longitude, less than
     * 0 if less, or greater than 0 if greater than the other longitude.
     */
    @Override
    public int compareTo(Longitude other) {
        Objects.requireNonNull(other, "other");
        return Integer.compare(this.intValue, other.intValue);
    }

    /**
     * Writes this longitude to the specified output stream.
     *
     * @param out the output stream, not null
     * @throws IOException if an exception occurred writing
     */
    public void write(ObjectOutputStream out) throws IOException {
        Objects.requireNonNull(out, "out");
        out.writeInt(intValue);
    }

    /**
     * Reads a longitude from the specified input stream.
     *
     * @param in the input stream, not null
     * @return the longitude
     * @throws IOException if an exception occurred reading
     */
    public static Longitude read(ObjectInputStream in) throws IOException {
        Objects.requireNonNull(in, "in");
        int value = in.readInt();
        if (value <= MINIMUM_INTERNAL || value > MAXIMUM_INTERNAL) { // exc. start, inc. end
            throw new IOException("illegal longitude read");
        }
        return new Longitude(value);
    }

}
