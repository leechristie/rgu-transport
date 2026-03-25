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
 * Represents a Latitude from -90 degrees (inclusive) to +90 degrees (inclusive) to 7 decimal
 * places.
 *
 * @author Lee A. Christie
 */
public final class Latitude implements Comparable<Latitude> {

    /**
     * The value 0.
     */
    public static final Latitude EQUATOR = new Latitude(0);

    final static int SCALE = 7;
    private final static BigDecimal MULTIPLIER = new BigDecimal("10").pow(SCALE);
    final static double MULTIPLIER_DOUBLE = MULTIPLIER.doubleValue();

    private final static BigDecimal MINIMUM = new BigDecimal("-90");
    private final static BigDecimal MAXIMUM = new BigDecimal("90");

    private final static int MINIMUM_INTERNAL = MINIMUM.multiply(MULTIPLIER).intValueExact();
    private final static int MAXIMUM_INTERNAL = MAXIMUM.multiply(MULTIPLIER).intValueExact();

    final int intValue;

    Latitude(int intValue) {
        this.intValue = intValue;
    }

    /**
     * Parses the specified string to a latitude.
     *
     * @param string the latitude as a string
     * @return a Latitude
     */
    public static Latitude parseLatitude(final String string) {
        Objects.requireNonNull(string, "string");
        final BigDecimal decimal = new BigDecimal(string);
        if (decimal.compareTo(MINIMUM) < 0 || decimal.compareTo(MAXIMUM) > 0) {
            throw new IllegalArgumentException(
                    "latitude = " + decimal + ", expected: within the closed range [-90, 90]");
        }
        final int intValue;
        try {
            intValue = decimal.multiply(MULTIPLIER).intValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(
                    "latitude = " + decimal
                    + ", expected: 7 or fewer decimal places.", ex);
        }
        return new Latitude(intValue);
    }

    /**
     * Returns a latitude of the specified decimal value.
     *
     * @param decimal the latitude as a decimal
     * @return a Latitude
     */
    public static Latitude of(BigDecimal decimal) {
        Objects.requireNonNull(decimal, "decimal");
        if (decimal.compareTo(MINIMUM) < 0 || decimal.compareTo(MAXIMUM) > 0) {
            throw new IllegalArgumentException(
                    "decimal = " + decimal + ", expected: -90 to 90");
        }
        final int intValue;
        try {
            intValue = decimal.multiply(MULTIPLIER).intValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(
                    "decimal = " + decimal
                    + ", expected: 7 or fewer decimal places.", ex);
        }
        return new Latitude(intValue);
    }

    /**
     * Rounds the specified double to a latitude.
     *
     * @param value the latitude as a double, finite
     * @return a Latitude
     */
    public static Latitude ofRounded(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value is not finite");
        }
        value = Math.round(value * 10_000_000.0) / 10_000_000.0;
        return parseLatitudeTruncate(Double.toString(value));
    }

    /**
     * Parses the specified string to a latitude.
     *
     * @param string the latitude as a string
     * @return a Latitude
     */
    public static Latitude parseLatitudeTruncate(String string) {
        Objects.requireNonNull(string, "string");
        final BigDecimal decimal = new BigDecimal(string);
        if (decimal.compareTo(MINIMUM) < 0 || decimal.compareTo(MAXIMUM) > 0) {
            throw new IllegalArgumentException(
                    "decimal = " + decimal + ", expected: -90 to 90");
        }
        final int intValue = decimal.multiply(MULTIPLIER).intValue();
        return new Latitude(intValue);
    }

    /**
     * Returns the latitude as a decimal to 7 decimal places.
     * For example parseLatitude("57.1497").doubleValue() returns 57.1497000
     *
     * @return the latitude as a decimal
     */
    public BigDecimal decimalValue() {
        return BigDecimal.valueOf(this.intValue, SCALE);
    }

    /**
     * Returns the latitude as a double.
     * For example parseLatitude("57.1497").doubleValue() returns 57.1497
     *
     * @return the latitude as a double
     */
	public double doubleValue() {
		return this.intValue / MULTIPLIER_DOUBLE;
	}

	/**
	 * Returns a hash code for this latitude.
	 *
	 * @return a hash code
	 */
    @Override
    public int hashCode() {
        return 3967 + 36739 * Integer.hashCode(this.intValue);
    }

    /**
     * Returns true if the other object is the same latitude value.
     *
     * @param other the other object
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object other) {
    	if (other == null) {
    		return false;
    	}
    	if (other instanceof Latitude) {
            return ((Latitude) other).intValue == this.intValue;
        }
        return false;
    }

    /**
     * Returns a String object representing this latitude's value.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return this.decimalValue().stripTrailingZeros().toPlainString();
    }

    /**
     * Compares two latitudes numerically.
     *
     * @return 0 if this latitude is the same as the other latitude, less than
     * 0 if less, or greater than 0 if greater than the other latitude.
     */
    @Override
    public int compareTo(Latitude other) {
        Objects.requireNonNull(other, "other");
        return Integer.compare(this.intValue, other.intValue);
    }

    /**
     * Writes this latitude to the specified output stream.
     *
     * @param out the output stream, not null
     * @throws IOException if an exception occurred writing
     */
    public void write(ObjectOutputStream out) throws IOException {
        Objects.requireNonNull(out, "out");
        out.writeInt(intValue);
    }

    /**
     * Reads a latitude from the specified input stream.
     *
     * @param in the input stream, not null
     * @return the latitude
     * @throws IOException if an exception occurred reading
     */
    public static Latitude read(ObjectInputStream in) throws IOException {
        Objects.requireNonNull(in, "in");
        int value = in.readInt();
        if (value < MINIMUM_INTERNAL || value > MAXIMUM_INTERNAL) { // inclusive
            throw new IOException("illegal latitude read");
        }
        return new Latitude(value);
    }

}
