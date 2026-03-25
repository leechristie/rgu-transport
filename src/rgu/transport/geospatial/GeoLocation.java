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

import rgu.transport.algorithms.collections.*;
import rgu.transport.algorithms.search.*;
import rgu.transport.impl.*;
import static java.lang.Math.*;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.function.*;

/**
 * Represents a location on Earth, uniquely defined by latitude and longitude.
 *
 * @author Lee A. Christie
 */
public final class GeoLocation implements TwoDTree.Point {

    /**
     * The coordinates 0, 0.
     */
    public static GeoLocation NULL_ISLAND = new GeoLocation(Latitude.EQUATOR,
                                                            Longitude.PRIME_MERIDIAN);

    private final int latitudeIntValue;
    private final int longitudeIntValue;

    /**
     * The haversine distance, the "great circle" distance across the surface of the Earth as a
     * {@linkplain Distance distance}.
     */
    public static Metric<GeoLocation, Distance> HAVERSINE = (from, to) -> {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        final double lat1 = from.latitudeAsDouble();
        final double lat2 = to.latitudeAsDouble();
        final double lon1 = from.longitudeAsDouble();
        final double lon2 = to.longitudeAsDouble();
        return Distance.ofMeters(haversineMeters(lat1, lon1, lat2, lon2));
    };

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double a = pow(sin((lat2 - lat1) * Math.PI / 180 / 2), 2)
                         + cos(lat1 * Math.PI / 180)
                         * cos(lat2 * Math.PI / 180)
                         * pow(sin((lon2 - lon1) * Math.PI / 180 / 2), 2);
        return 6_371_000 * 2.0 * atan2(sqrt(a), sqrt(1 - a));
    }

    /**
     * The Euclidean distance, with a stretch ratio set.
     *
     * @param ratio the skew ratio, finite, positive
     */
    public static Metric<GeoLocation, Double> stretchedEuclidean(double ratio) {
        if (!Double.isFinite(ratio) || ratio <= 0.0) {
            throw new AssertionError("ratio = " + ratio + ", expected: positive, finite");
        }
        return (from, to) -> {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            return Math.hypot((from.latitudeAsDouble() - to.latitudeAsDouble()) * ratio,
                               from.longitudeAsDouble() - to.longitudeAsDouble());
        };
    }

    private GeoLocation(Latitude latitude, Longitude longitude) {
        this.latitudeIntValue = latitude.intValue;
        this.longitudeIntValue = longitude.intValue;
    }

    /**
     * Returns an instance of location for the specified latitude and longitude.
     *
     * @param latitude the latitude, not null
     * @param longitude the longitude, not null
     * @return a location
     */
    public static GeoLocation of(Latitude latitude, Longitude longitude) {
        Objects.requireNonNull(latitude, "latitude");
        Objects.requireNonNull(longitude, "longitude");
        return new GeoLocation(latitude, longitude);
    }

    /**
     * Rounds the specified doubles to a location.
     *
     * @param latitude the latitude as a double, finite
     * @param longitude the longitude as a double, finite
     * @return a location
     */
    public static GeoLocation ofRounded(double latitude, double longitude) {
        return new GeoLocation(Latitude.ofRounded(latitude), Longitude.ofRounded(longitude));
    }

    /**
     * Parses and returns latitude, longitude pair of strings.
     *
     * @param latitude the latitude
     * @param longitude the longitude
     * @return the parsed location
     */
    public static GeoLocation parseLocation(String latitude, String longitude) {
        Objects.requireNonNull(latitude, "latitude");
        Objects.requireNonNull(longitude, "longitude");
        return of(Latitude.parseLatitude(latitude),
                Longitude.parseLongitude(longitude));
    }

    /**
     * Parses and returns latitude, longitude pair of strings.
     *
     * @param latitude the latitude
     * @param longitude the longitude
     * @return the parsed location
     */
    public static GeoLocation parseLocationTruncate(
            String latitude, String longitude) {
        Objects.requireNonNull(latitude, "latitude");
        Objects.requireNonNull(longitude, "longitude");
        return of(Latitude.parseLatitudeTruncate(latitude),
                  Longitude.parseLongitudeTruncate(longitude));
    }

    /**
     * Returns the latitude of this location.
     *
     * @return the latitude
     */
    public Latitude latitude() {
        return new Latitude(this.latitudeIntValue);
    }

    /**
     * Returns the latitude as a decimal.
     *
     * @return the latitude as a decimal
     */
    public BigDecimal latitudeAsDecimal() {
        return BigDecimal.valueOf(this.latitudeIntValue, Latitude.SCALE);
    }

    /**
     * Returns the latitude as a double.
     *
     * @return the latitude as a double
     */
    public double latitudeAsDouble() {
        return this.latitudeIntValue / Latitude.MULTIPLIER_DOUBLE;
    }

    /**
     * Returns the longitude as a decimal.
     *
     * @return the longitude as a decimal
     */
    public BigDecimal longitudeAsDecimal() {
        return BigDecimal.valueOf(this.longitudeIntValue, Longitude.SCALE);
    }

    /**
     * Returns the longitude of this location.
     *
     * @return the longitude
     */
    public Longitude longitude() {
        return new Longitude(this.longitudeIntValue);
    }

    /**
     * Returns the longitude as a double.
     *
     * @return the longitude as a double
     */
    public double longitudeAsDouble() {
        return this.longitudeIntValue / Longitude.MULTIPLIER_DOUBLE;
    }

    /**
     * Returns a predicate which tests whether a point is contained within a
     * given rectangle.
     *
     * @return a predicate
     */
    public static Predicate<GeoLocation> rectangle(Latitude latMin, Latitude latMax,
                                                   Longitude lonMin, Longitude lonMax) {
        Objects.requireNonNull(latMin, "latMin");
        Objects.requireNonNull(latMax, "latMax");
        Objects.requireNonNull(lonMin, "lonMin");
        Objects.requireNonNull(lonMax, "lonMax");
        if (latMin.compareTo(latMax) >= 0) {
            throw new IllegalArgumentException("latMin >= latMax");
        }
        if (lonMin.compareTo(lonMax) >= 0) {
            throw new IllegalArgumentException("lonMin >= lonMax");
        }
        return (location) -> {
            Objects.requireNonNull(location, "location");
            if (location.latitude().compareTo(latMin) < 0) {
                return false;
            }
            if (location.latitude().compareTo(latMax) > 0) {
                return false;
            }
            if (location.longitude().compareTo(lonMin) < 0) {
                return false;
            }
            if (location.longitude().compareTo(lonMax) > 0) {
                return false;
            }
            return true;
        };
    }

    /**
     * Returns a grid of locations in a rectangle bounded by latitude and longitude, with given
     * increments.
     *
     * @param latMin the minimum latitude, not null
     * @param latMax the maximum latitude, not null
     * @param latDelta the delta to step by in the latitude direction
     * @param lonMin the minimum longitude, not null
     * @param lonMax the maximum longitude, not null
     * @param lonDelta the delta to step by in the longitude direction
     * @return an iterable grid of locations
     */
    public static Iterable<GeoLocation> grid(Latitude latMin, Latitude latMax,
                                             BigDecimal latDelta,
                                             Longitude lonMin, Longitude lonMax,
                                             BigDecimal lonDelta) {
        Objects.requireNonNull(latMin, "latMin");
        Objects.requireNonNull(latMax, "latMax");
        Objects.requireNonNull(latDelta, "latDelta");
        Objects.requireNonNull(lonMin, "lonMin");
        Objects.requireNonNull(lonMax, "lonMax");
        Objects.requireNonNull(lonDelta, "lonDelta");
        return () -> new Iterator<>() {
            BigDecimal nextLat = latMin.decimalValue();
            BigDecimal nextLon = lonMin.decimalValue();
            @Override
            public boolean hasNext() {
                return this.nextLat != null
                        && this.nextLon != null;
            }
            @Override
            public GeoLocation next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                GeoLocation rv = GeoLocation.of(Latitude.of(this.nextLat),
                        Longitude.of(this.nextLon));
                this.nextLat = this.nextLat.add(latDelta);
                if (this.nextLat.compareTo(latMax.decimalValue()) > 0) {
                    this.nextLat = latMin.decimalValue();
                    this.nextLon = this.nextLon.add(lonDelta);
                    if (this.nextLon.compareTo(lonMax.decimalValue()) > 0) {
                        this.nextLat = null;
                        this.nextLon = null;
                    }
                }
                return rv;
            }

        };
    }

    /**
     * Returns a hash code for this location.
     *
     * @return a hash code
     */
    @Override
    public int hashCode() {
        return 6173 + 24203 * (3967 + 36739 * Integer.hashCode(this.latitudeIntValue))
               + 47659 * (58153 + 42193 * Integer.hashCode(this.longitudeIntValue));
    }

    @Override
    public double x() {
        return this.latitudeIntValue / Latitude.MULTIPLIER_DOUBLE;
    }

    @Override
    public double y() {
        return this.longitudeIntValue / Longitude.MULTIPLIER_DOUBLE;
    }

    /**
     * Returns true if the other object is the same latitude and longitude.
     *
     * @param other the other object
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof GeoLocation otherLocation) {
            return this.latitudeIntValue == otherLocation.latitudeIntValue
                && this.longitudeIntValue == otherLocation.longitudeIntValue;
        }
        // TODO: Not be needed anymore after re-write?
        if (other instanceof Collection<?> otherCollection) {
            return otherCollection.equals(this);
        }
        return false;
    }

    /**
     * Returns a String object representing this location's value.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "(" + BigDecimal.valueOf(this.latitudeIntValue, Latitude.SCALE)
               .stripTrailingZeros().toPlainString()
               + ", " + BigDecimal.valueOf(this.longitudeIntValue, Longitude.SCALE)
               .stripTrailingZeros().toPlainString() + ")";
    }

    /**
     * Returns a nearest finder for the given collection of points, using the Euclidean distance.
     *
     * @param elements the set of elements, not null
     * @return a nearest finder
     */
    public static NearestFinder<GeoLocation> nearestHaversine(Collection<GeoLocation> elements) {
        return new ExhaustiveGeoLocationNearestFinderImpl<>(GeoLocation.HAVERSINE,
                Set.copyOf(elements),
                (d) -> true);
    }

    /**
     * Returns a nearest finder for the given collection of points, using the Euclidean distance,
     * limited to a given maximum distance.
     *
     * @param elements the set of elements, not null
     * @param maxDistance the maximum distance, not null, [0, inf)
     * @return a nearest finder
     */
    public static NearestFinder<GeoLocation> nearestHaversine(
            Collection<GeoLocation> elements, Distance maxDistance) {
        Objects.requireNonNull(maxDistance, "maxDistance");
        if (maxDistance.compareTo(Distance.ZERO) <= 0) {
            throw new IllegalArgumentException("maxDistance = " + maxDistance
                                               + ", expected not null, positive");
        }
        return new ExhaustiveGeoLocationNearestFinderImpl<>(GeoLocation.HAVERSINE,
                Set.copyOf(elements),
                (d) -> d.compareTo(maxDistance) <= 0);
    }

    /**
     * Returns a nearest finder for the given collection of points, using the Euclidean distance.
     *
     * @param elements the set of elements, not null
     * @param ratio the stretch ratio, finite, positive
     * @return a nearest finder
     */
    public static NearestFinder<GeoLocation> nearestStretchedEuclidean(
            double ratio, Collection<GeoLocation> elements) {
        return new ExhaustiveGeoLocationNearestFinderImpl<>(GeoLocation.stretchedEuclidean(ratio),
                Set.copyOf(elements),
                (d) -> true);
    }

    /**
     * Returns a nearest finder for the given collection of points, using the Euclidean distance,
     * limited to a given maximum distance.
     *
     * @param elements the set of elements, not null
     * @param ratio the stretch ratio, finite, positive
     * @param maxDistance the maximum distance, [0, inf)
     * @return a nearest finder
     */
    public static NearestFinder<GeoLocation> nearestStretchedEuclidean(
            double ratio, Collection<GeoLocation> elements, double maxDistance) {
        if (!Double.isFinite(maxDistance)) {
            throw new IllegalArgumentException("maxDistance = " + maxDistance
                                               + ", expected finite, positive");
        }
        if (maxDistance <= 0) {
            throw new IllegalArgumentException("maxDistance = " + maxDistance
                                               + ", expected finite, positive");
        }
        return new ExhaustiveGeoLocationNearestFinderImpl<>(GeoLocation.stretchedEuclidean(ratio),
                Set.copyOf(elements),
                (d) -> d.compareTo(maxDistance) <= 0);
    }

    /**
     * Writes this location to the specified output stream.
     *
     * @param out the output stream, not null
     * @throws IOException if an exception occurred writing
     */
    public void write(ObjectOutputStream out) throws IOException {
        out.write(this.latitudeIntValue);
        out.write(this.longitudeIntValue);
    }

    /**
     * Reads a location from the specified input stream.
     *
     * @param in the input stream, not null
     * @return the location
     * @throws IOException if an exception occurred reading
     */
    public static GeoLocation read(ObjectInputStream in) throws IOException {
        return new GeoLocation(Latitude.read(in), Longitude.read(in));
    }

    /**
     * Estimates the ratio by which the Euclidean distance would need to be stretched at this point
     * to adjust for the curvature of the Earth.
     *
     * @return the estimated curvature
     */
    public double estimateCurvature() {
        double lat = this.latitudeAsDouble();
        double lon = this.longitudeAsDouble();
        double latMinus = lat - 0.5;
        double latPlus = lat + 0.5;
        double lonMinus = lon - 0.5;
        double lonPlus = lon + 0.5;
        double latDelta = haversineMeters(latMinus, lon, latPlus, lon);
        double lonDelta = haversineMeters(lat, lonMinus, lat, lonPlus);
        return latDelta / lonDelta;
    }

}
