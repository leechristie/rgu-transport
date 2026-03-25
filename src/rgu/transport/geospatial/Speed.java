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
import java.time.*;
import java.util.*;

/**
 * A (non-negative) scalar travel speed.
 *
 * @author Lee A. Christie
 */
public final class Speed implements Comparable<Speed> {

	/**
	 * A speed of zero (stationary).
	 */
	public static final Speed ZERO = new Speed(0.0);

	// conversion factors
	private static final double KPH = 3.6;
	private static final double MPH = 2.23693629205;

	private final double metersPerSecond;

	private Speed(double metersPerSecond) {
		this.metersPerSecond = metersPerSecond;
	}

	/**
	 * Returns an instance of Speed for the specified meters per second.
	 *
	 * @param metersPerSecond the speed, in the range [0, inf)
	 * @return an instance of Speed
	 */
	public static Speed ofMetersPerSecond(double metersPerSecond) {
		if (metersPerSecond == 0.0) {
			return ZERO;
		}
		if (!Double.isFinite(metersPerSecond) || metersPerSecond < 0.0) {
			throw new IllegalArgumentException(
					"metersPerSecond = " + metersPerSecond
					+ ", expected [0, inf)");
		}
		return new Speed(metersPerSecond);
	}

	/**
	 * Returns an instance of Speed for the specified kilometers per hour.
	 *
	 * @param kilometersPerHour the speed, in the range [0, inf)
	 * @return an instance of Speed
	 */
	public static Speed ofKilometersPerHour(double kilometersPerHour) {
		return ofMetersPerSecond(kilometersPerHour / KPH);
	}

	/**
	 * Returns an instance of Speed for the specified miles peer hour.
	 *
	 * @param milesPerHour the speed, in the range [0, inf)
	 * @return an instance of Speed
	 */
	public static Speed ofMilesPerHour(double milesPerHour) {
		return ofMetersPerSecond(milesPerHour / MPH);
	}

	/**
	 * Returns the speed in meters per second.
	 *
	 * @return the speed
	 */
	public double asMetersPerSecond() {
		return this.metersPerSecond;
	}

	/**
	 * Returns the speed in kilometers per hour.
	 *
	 * @return the speed
	 */
	public double asKilometersPerHour() {
		return this.metersPerSecond * KPH;
	}

	/**
	 * Returns the speed in miles per hour.
	 *
	 * @return the speed
	 */
	public double asMilesPerHour() {
		return this.metersPerSecond * MPH;
	}

	/**
	 * Returns a string representation of the speed (shown in meters per
	 * second).
	 *
	 * @return a string representation
	 */
	@Override
	public String toString() {
		return this.metersPerSecond + " m/s";
	}

	/**
	 * Compares this speed to another speed.
	 *
	 * @param other the other speed, not null
     * @return 0, -1, or 1 based on Comparable definition
	 */
	@Override
	public int compareTo(Speed other) {
	    Objects.requireNonNull(other, "other");
		return Double.compare(this.metersPerSecond, other.metersPerSecond);
	}

	/**
	 * Returns a hash code for this object.
	 *
	 * @return a hash code
	 */
	@Override
	public int hashCode() {
		return 10369 + 41081 * Double.hashCode(this.metersPerSecond);
	}

	/**
	 * Checks if the other object represents the same speed.
	 *
	 * @param other another object
	 * @return true if equal, false otherwise
	 */
	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (other instanceof Speed otherSpeed) {
			return this.metersPerSecond == otherSpeed.metersPerSecond;
		}
		return false;
	}

	/**
	 * Writes the speed value to the specified output stream.
	 *
	 * @param out the output stream, not null
	 * @throws IOException if an exception occurred in writing
	 */
    public void write(ObjectOutputStream out) throws IOException {
		Objects.requireNonNull(out, "out");
        out.writeDouble(metersPerSecond);
    }

	/**
	 * Reads the speed value from the specified input stream. There is a special case where if the
     * read value is the NaN, the value null will be returned.
	 *
	 * @param in the input stream, not null
	 * @return the speed or null
	 * @throws IOException if an exception occurred in reading
	 */
	public static Speed read(ObjectInputStream in) throws IOException {
		Objects.requireNonNull(in, "in");
        double d = in.readDouble();
        if (Double.isNaN(d)) {
            return null;
        } else {
            return Speed.ofMetersPerSecond(d);
        }
    }

	/**
	 * Returns the total time taken to travel a given distance as this constant velocity, rounded
     * down to a whole number of seconds.
	 *
	 * @param distance the distance, not null
	 * @return the time
	 */
	public Duration timeTruncatedToSeconds(Distance distance) {
		Objects.requireNonNull(distance, "distance");
		long seconds = (long) (distance.asMeters() / this.metersPerSecond);
		return Duration.ofSeconds(seconds);
	}

	/**
	 * Returns the total time taken to travel a given distance as this constant velocity, rounded to
     * the nearest whole number of seconds.
	 *
	 * @param distance the distance, not null
	 * @return the time
	 */
	public Duration timeRoundedToSeconds(Distance distance) {
		Objects.requireNonNull(distance, "distance");
		long seconds = Math.round(distance.asMeters() / this.metersPerSecond);
		return Duration.ofSeconds(seconds);
	}

	/**
	 * Returns the total time taken to travel a given distance as this constant velocity, using
     * double precision rounded to the nearest nanosecond. Precision loss is likely due to
     * floating-point error.
	 *
	 * @param distance the distance, not null
	 * @return the time
	 */
	public Duration time(Distance distance) {
		Objects.requireNonNull(distance, "distance");
		double time = distance.asMeters() / this.metersPerSecond;
        long seconds = (long) time;
		long nanoAdjustment = Math.round((time - seconds) * 1_000_000_000L);
		return Duration.ofSeconds(seconds, nanoAdjustment);
	}

	/**
	 * Returns true if the speed is positive, false otherwise. If the speed is not positive, then it
     * must be zero.
	 *
	 * @return true if positive, false if zero
	 */
	public boolean isPositive() {
		return this.metersPerSecond > 0;
	}

	/**
	 * Returns true if the speed is zero, false otherwise. If the speed is not zero, then it must be
     * positive.
	 *
	 * @return true if zero, false if positive
	 */
	public boolean isZero() {
		return this == Speed.ZERO || !isPositive();
	}

}
