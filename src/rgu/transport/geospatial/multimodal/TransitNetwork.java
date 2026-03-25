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

import rgu.transport.algorithms.collections.*;
import rgu.transport.algorithms.search.*;
import rgu.transport.algorithms.util.*;
import rgu.transport.geospatial.*;
import rgu.transport.impl.*;

import java.io.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.function.*;

/**
 * A multi-modal transit network. TransitNetwork is thread-safe, however, the builder object is not
 * thread-safe.
 *
 * @author Lee A. Christie
 */
public final class TransitNetwork {

    private static final String TRANSIT_TAG = "transit";
    private static final String WALK_TAG = "walk";
    private static final String CAR_TAG = "car";

    private final NetworkSearcher searcher = new NetworkSearcher(this);

    /**
     * Returns a search wrapper for this network.
     *
     * @return a search wrapper for this network
     */
    public NetworkSearcher searcher() {
        return searcher;
    }

    /**
     * Creates a builder by copying the contents of a collection of transit networks.
     *
     * @param networks the networks to copy, not null
     * @param edgeConsumer consumer for edges, can be null if not needed
     * @return a builder formed from a copy of the given networks
     */
    public static Builder copyAll(Collection<TransitNetwork> networks,
                                  BiConsumer<GeoLocation, GeoLocation> edgeConsumer) {

        Objects.requireNonNull(networks, "networks");
        List<TransitNetwork> listCopy = new ArrayList<>(networks.size());
        for (TransitNetwork n : networks) {
            if (n == null) {
                throw new NullPointerException("an element of networks was null");
            }
            listCopy.add(n);
        }

        Builder builder = builder(true);

        // locations
        for (TransitNetwork n : listCopy) {
            for (GeoLocation l : n.locations) {
                builder.addLocation(l);
            }
        }

        // roads
        for (TransitNetwork n : listCopy) {
            for (Map.Entry<GeoLocation, Set<GeoLocation>> e : n.roadEdges.entrySet()) {
                GeoLocation a = e.getKey();
                for (GeoLocation b : e.getValue()) {
                    builder.addRoad(a, b);
                }
            }
            for (Map.Entry<GeoLocation, Map<GeoLocation, Duration>> e
                    : n.artificialEdges.entrySet()) {
                GeoLocation a = e.getKey();
                for (Map.Entry<GeoLocation, Duration> f : e.getValue().entrySet()) {
                    GeoLocation b = f.getKey();
                    Duration d = f.getValue();
                    builder.addArtificialEdge(a, b, d);
                }
            }
        }

        // transit
        for (TransitNetwork n : listCopy) {
            for (Map.Entry<GeoLocation, List<Trip>> e : n.transitEdges.entrySet()) {
                GeoLocation a = e.getKey();
                for (Trip t : e.getValue()) {
                    GeoLocation b = t.destination();
                    builder.addTransit(a, b, t.departureTime(), t.arrivalTime());
                    if (edgeConsumer != null) {
                        edgeConsumer.accept(a, b);
                    }
                }
            }
        }

        return builder;
    }

    private final Set<GeoLocation> locations;
    private final Map<GeoLocation, Set<GeoLocation>> roadEdges;
    private final Map<GeoLocation, Map<GeoLocation, Duration>> artificialEdges;
    private final Map<GeoLocation, List<Trip>> transitEdges;

    private transient final Map<GeoLocation, List<ReverseTrip>> transitEdgesReversed;

    public static final Speed DEFAULT_WALK_SPEED = Speed.ofKilometersPerHour(5);
    public static final Speed DEFAULT_CAR_SPEED = Speed.ofKilometersPerHour(80);

    private final Speed carSpeed;
    private final Speed walkSpeed;

    private final NearestFinder<GeoLocation> nearestHaversine;
    private final TwoDTree<GeoLocation> nearestEuclidean;

    /**
     * Returns a neighbour finder based on which location is nearest on the Haversine space.
     *
     * @return a nearest finder
     */
    public NearestFinder<GeoLocation> nearestHaversine() {
        return nearestHaversine;
    }

    /**
     * Returns a neighbour finder based on which location is nearest on the Euclidean space.
     *
     * @return a nearest finder
     * @deprecated user {@link #nearestStretchedEuclidean(double)} or
     * {@link #nearestStretchedEuclidean(double)}
     */
    @Deprecated(forRemoval = true)
    public NearestFinder<GeoLocation> nearestEuclidean() {
        return nearestEuclidean;
    }

    /**
     * Returns a neighbour finder based on which location is nearest on the Euclidean space.
     *
     * @param ratio the stretch ratio, finite, positive
     * @return a nearest finder
     */
    public NearestFinder<GeoLocation> nearestStretchedEuclidean(double ratio) {
        return nearestEuclidean.withRatio(ratio);
    }

    /**
     * Returns a neighbour finder based on which location is nearest on the Euclidean space.
     *
     * @param at the location to used to estimate local curvature
     * @return a nearest finder
     */
    public NearestFinder<GeoLocation> nearestStretchedEuclidean(GeoLocation at) {
        Objects.requireNonNull(at);
        return nearestEuclidean.withRatio(at.estimateCurvature());
    }

    /**
     * Returns a list of locations which are departure points for transit edges.
     */
    public Set<GeoLocation> transitDeparturePoints() {
        return Set.copyOf(this.transitEdges.keySet());
    }

    /**
     * Returns a list of locations which are departure or arrival points for transit edges.
     * Requires complete scan of all trips.
     */
    public Set<GeoLocation> scanForAllTransitPoints() {
        Set<GeoLocation> rv = new HashSet<>();
        for (var entry : this.transitEdges.entrySet()) {
            GeoLocation from = entry.getKey();
            for (Trip trip : entry.getValue()) {
                GeoLocation to = trip.destination();
                rv.add(from);
                rv.add(to);
            }
        }
        return Set.copyOf(rv);
    }

    /**
     * Returns a list of Trips which depart from a given location.
     */
    public List<Trip> outgoingTrips(GeoLocation from) {
        return List.copyOf(this.transitEdges.get(from));
    }

    /**
     * Returns a list of Trips which arrive at a given location. Requires complete scan of all
     * trips.
     */
    public List<Trip> scanForIncomingTrips(GeoLocation to) {
        List<Trip> rv = new ArrayList<>();
        for (var entry : this.transitEdges.entrySet()) {
            for (Trip trip : entry.getValue()) {
                if (trip.destination().equals(to)) {
                    rv.add(trip);
                }
            }
        }
        return rv;
    }

    /**
     * A hashcode.
     *
     * @return a hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.locations, this.roadEdges, this.transitEdges);
    }

    /**
     * Checks whether this transit network is equal to the other object.
     *
     * @param obj the other object
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TransitNetwork)) {
            return false;
        }
        TransitNetwork other = (TransitNetwork) obj;
        return Objects.equals(this.locations, other.locations)
                && Objects.equals(this.roadEdges, other.roadEdges)
                && Objects.equals(this.transitEdges, other.transitEdges);
    }

    private TransitNetwork(Set<GeoLocation> locations,
            Map<GeoLocation, Set<GeoLocation>> roadEdges,
            Map<GeoLocation, Map<GeoLocation, Duration>> roadEdgesWithDuration,
            Map<GeoLocation, List<Trip>> transitEdges,
            Map<GeoLocation, List<ReverseTrip>> transitEdgesReversed,
            Speed carSpeed,
            Speed walkSpeed,
            NearestFinder<GeoLocation> nearestHaversine) {
        this.locations = Collections.unmodifiableSet(locations);
        this.roadEdges = roadEdges;
        this.artificialEdges = roadEdgesWithDuration;
        this.transitEdges = transitEdges;
        this.transitEdgesReversed = transitEdgesReversed;
        this.carSpeed = carSpeed;
        this.walkSpeed = walkSpeed;
        this.nearestHaversine = nearestHaversine;
        this.nearestEuclidean = TwoDTree.of(locations);
    }

    /**
     * Creates a builder object from HashTransitNetwork. Builder is not thread-safe.
     *
     * @return a new builder
     */
    public static TransitNetwork.Builder builder() {
        return builder(false);
    }

    /**
     * Creates a builder object from HashTransitNetwork. If suppressDuplicate is set, then no
     * exception will be throw on adding duplicates, instead, the operation will be ignored. Builder
     * is not thread-safe.
     *
     * @return a new builder
     */
    public static TransitNetwork.Builder
            builder(final boolean suppressDuplicates) {
        return new Builder(suppressDuplicates);
    }

    /**
     * Builder class, instance is obtained from {@link TransitNetwork#builder()}.
     */
    public static class Builder {

        private final boolean suppressDuplicates;
        private Speed carSpeed = DEFAULT_CAR_SPEED;
        private Speed walkSpeed = DEFAULT_WALK_SPEED;

        /**
         * Sets the driving speed.
         *
         * @param carSpeed the driving speed, not null
         */
        public void setCarSpeed(Speed carSpeed) {
            Objects.requireNonNull(carSpeed, "carSpeed");
            this.carSpeed = carSpeed;
        }

        /**
         * Sets the walking speed.
         *
         * @param walkSpeed the walking speed, not null
         */
        public void setWalkSpeed(Speed walkSpeed) {
            Objects.requireNonNull(walkSpeed, "walkSpeed");
            this.walkSpeed = walkSpeed;
        }

        private Builder(final boolean suppressDuplicates) {
            this.suppressDuplicates = suppressDuplicates;
        }

        private final Set<GeoLocation> locations = new HashSet<>();
        private final Set<GeoLocation> carParking = new HashSet<>();
        private final Map<GeoLocation, Set<GeoLocation>> roadEdges = new HashMap<>();
        private final Map<GeoLocation, Map<GeoLocation, Duration>> artificialEdges
            = new HashMap<>();
        private final Map<GeoLocation, List<Trip>> transitEdges = new HashMap<>();
        private final Map<GeoLocation, List<ReverseTrip>> transitEdgesReverse = new HashMap<>();

        private boolean built = false;

        /**
         * Add the specified location.
         *
         * @param location the location, not null
         * @return this object
         * @throws IllegalStateException if the location is a duplicate and the suppressDuplicates
         * flag is not set
         */
        public TransitNetwork.Builder addLocation(GeoLocation location) {
            Objects.requireNonNull(location, "location");
            if (this.built) {
                throw new IllegalStateException("cannot build twice");
            }
            if (this.locations.contains(location)) {
                if (suppressDuplicates) {
                    return this;
                }
                throw new IllegalStateException(
                        location + " already exists");
            }
            this.locations.add(location);
            this.roadEdges.put(location, new HashSet<>());
            return this;
        }

        /**
         * Adds the specified road edge.
         *
         * @param from the start point, not null
         * @param to the end point, not null
         * @return this object
         * @throws IllegalStateException if the edge is a duplicate and the suppressDuplicates flag
         * is not set
         */
        public TransitNetwork.Builder addRoad(GeoLocation from, GeoLocation to) {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            if (this.built) {
                throw new IllegalStateException("cannot build twice");
            }
            if (!this.locations.contains(from)) {
                throw new NoSuchElementException(from + " does not exist");
            }
            if (!this.locations.contains(to)) {
                throw new NoSuchElementException(to + " does not exist");
            }
            Set<GeoLocation> neighbours = this.roadEdges.get(from);
            if (neighbours.contains(to)) {
                if (suppressDuplicates) {
                    return this;
                }
                throw new IllegalStateException("road from " + from
                        + " to " + to + " already exists");
            }
            neighbours.add(to);
            return this;
        }

        public TransitNetwork.Builder addTransit(GeoLocation from,
                GeoLocation to,
                LocalTime departureTime,
                LocalTime arrivalTime) {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            Objects.requireNonNull(departureTime, "departureTime");
            Objects.requireNonNull(arrivalTime, "arrivalTime");
            List<Trip> trips = this.transitEdges.get(from);
            List<ReverseTrip> reverseTrips = this.transitEdgesReverse.get(to);
            if (trips == null) {
                trips = new ArrayList<>();
                this.transitEdges.put(from, trips);
            }
            if (reverseTrips == null) {
                reverseTrips = new ArrayList<>();
                this.transitEdgesReverse.put(to, reverseTrips);
            }
            trips.add(new Trip(departureTime, arrivalTime, to));
            reverseTrips.add(new ReverseTrip(departureTime, arrivalTime, from));
            return this;
        }

        public TransitNetwork build() {
            //System.err.println(roadEdges.size());
            int empty = 0;
            int nonempty = 0;
            for (GeoLocation a : roadEdges.keySet()) {
                if (roadEdges.get(a).isEmpty()) {
                    empty++;
                } else {
                    nonempty++;
                }
            }
            //System.err.println("locations=" + locations.size());
            //System.err.println(empty + " : " + nonempty + " roadEdges=" + roadEdges.size());
            this.built = true;
            return new TransitNetwork(this.locations,
                    this.roadEdges,
                    this.artificialEdges,
                    this.transitEdges,
                    this.transitEdgesReverse,
                    this.carSpeed,
                    this.walkSpeed,
                    new ExhaustiveGeoLocationNearestFinderImpl<>(GeoLocation.HAVERSINE,
                            locations,
                            (d) -> true)
                    );
        }

        public Collection<GeoLocation> locations() {
            return Collections.unmodifiableCollection(this.locations);
        }

        /**
         * Returns the neighbours reachable by transit.
         *
         * @param location the location
         * @return the available trips to neighbours
         */
        public Collection<Trip> transitNeighbours(GeoLocation location) {
            Collection<Trip> rv = this.transitEdges.get(location);
            if (rv == null) {
                return List.of();
            }
            return Collections.unmodifiableCollection(rv);
        }

        public TransitNetwork.Builder addRoadGraph(
                Graph<GeoLocation, ?> graph,
                ProgressListener progress) throws InterruptedException {
            NearestFinder<GeoLocation> finder
                    = GeoLocation.nearestHaversine(graph.vertices());
            Thread addThread = Thread.currentThread();
            progress.setCancelAction(addThread::interrupt);
            progress.onNewStage("Searching for nearest nodes...");
            Map<GeoLocation, GeoLocation> links = finder.nearest(locations(), progress, true);
            progress.onNewStage("Creating joining edges...");
            double size = links.size();
            int i = 0;
            for (Map.Entry<GeoLocation, GeoLocation> e : links.entrySet()) {
                ThreadUtil.checkInterrupt();
                GeoLocation from = e.getKey();
                GeoLocation to = e.getValue();
                addLocation(from);
                addLocation(to);
                addRoad(from, to);
                addRoad(to, from);
                i++;
                progress.onUpdateProgress(i / size);
            }
            progress.onNewStage("Adding vertices...");
            size = graph.vertices().size();
            i = 0;
            for (GeoLocation from : graph.vertices()) {
                ThreadUtil.checkInterrupt();
                for (GeoLocation to : graph.neighbours(from)) {
                    addLocation(from);
                    addLocation(to);
                    addRoad(from, to);
                }
                i++;
                progress.onUpdateProgress(i / size);
            }
            progress.onCompletion();
            return this;
        }

        public void addArtificialEdge(GeoLocation a, GeoLocation b, Duration duration) {
            Map<GeoLocation, Duration> map = this.artificialEdges.get(a);
            if (map == null) {
                map = new HashMap<>();
            }
            if (map.containsKey(b) && !suppressDuplicates) {
                throw new IllegalStateException("road with duration from " + a
                        + " to " + b + " already exists");
            }
            map.put(b, duration);
            this.artificialEdges.put(a, map);
            Map<GeoLocation, Duration> reverseMap = this.artificialEdges.get(b);
            if (reverseMap == null) {
                reverseMap = new HashMap<>();
            }
            if (reverseMap.containsKey(a) && !suppressDuplicates) {
                throw new IllegalStateException("road with duration from " + a
                        + " to " + b + " already exists");
            }
            reverseMap.put(a, duration);
            this.artificialEdges.put(b, reverseMap);
        }

    }

    public Set<GeoLocation> locations() {
        return this.locations;
    }

    public Graph<JourneyState, Duration> spatialTemporalGraph() {
        return spatialTemporalGraph(false);
    }

    public Graph<JourneyState, Duration> spatialTemporalGraph(boolean freeArtificial) {
        return spatialTemporalGraph(Map.of(), freeArtificial);
    }

    public Graph<JourneyState, Duration> spatialTemporalGraph(
            Map<GeoLocation, List<Trip>> extra) {
        return spatialTemporalGraph(extra, false);
    }

    public Graph<JourneyState, Duration> spatialTemporalGraph(
            Map<GeoLocation, List<Trip>> extra, boolean freeArtificial) {
        Objects.requireNonNull(extra);
        Map<GeoLocation, List<Trip>> copy = new HashMap<>();
        Map<GeoLocation, List<ReverseTrip>> reverse = new HashMap<>();
        for (Map.Entry<GeoLocation, List<Trip>> entry : extra.entrySet()) {
            GeoLocation from = entry.getKey();
            List<Trip> trips = entry.getValue();
            Objects.requireNonNull(from);
            Objects.requireNonNull(trips);
            List<Trip> tripsCopy = new ArrayList<>(trips.size());
            for (Trip t : trips) {
                Objects.requireNonNull(t);
                tripsCopy.add(t);
                add(reverse, from, t);
            }
            copy.put(from, tripsCopy);
        }
        return new STGraph(copy, reverse, freeArtificial);
    }

    // helper
    private void add(Map<GeoLocation, List<ReverseTrip>> reverse, GeoLocation from, Trip t) {
        GeoLocation to = t.destination();
        LocalTime departureTime = t.departureTime();
        LocalTime arrivalTime = t.arrivalTime();
        if (!reverse.containsKey(to)) {
            reverse.put(to, new ArrayList<>());
        }
        reverse.get(to).add(new ReverseTrip(departureTime, arrivalTime, from));
    }

    private class STGraph implements Graph<JourneyState, Duration> {

        private final Map<GeoLocation, List<Trip>> extra;
        private final Map<GeoLocation, List<ReverseTrip>> reverseExtra;
        private final boolean freeArtificial;

        private STGraph(Map<GeoLocation, List<Trip>> extra,
                        Map<GeoLocation, List<ReverseTrip>> reverseExtra,
                        boolean freeArtificial) {
            this.extra = extra;
            this.reverseExtra = reverseExtra;
            this.freeArtificial = freeArtificial;
        }

        @Override
        public boolean containsVertex(JourneyState vertex) {
            Objects.requireNonNull(vertex, "vertex");
            return TransitNetwork.this.locations
                    .contains(vertex.location());
        }

        @Override
        public Set<JourneyState> neighbours(JourneyState vertex) {
            Objects.requireNonNull(vertex, "vertex");
            if (!containsVertex(vertex)) {
                throw new NoSuchElementException(
                        vertex + " does not exist");
            }
            // road edges
            Set<JourneyState> rv = new HashSet<>();
            Set<GeoLocation> edges = TransitNetwork.this.roadEdges.get(vertex.location());
            Map<GeoLocation, Duration> artificialEdges
                = TransitNetwork.this.artificialEdges.get(vertex.location());
            if (edges != null) {
                for (GeoLocation neighbourLocation : edges) {
                    rv.add(travel(vertex, neighbourLocation));
                }
            }
            if (artificialEdges != null) {
                if (!vertex.artificial() || freeArtificial) {
                    for (Map.Entry<GeoLocation, Duration> neighbour : artificialEdges.entrySet()) {
                        rv.add(travel(vertex, neighbour.getKey(), neighbour.getValue()));
                    }
                }
            }
            if (!vertex.vehicle() && vertex.busPass()) {
                List<Trip> trips = TransitNetwork.this.transitEdges.get(
                        vertex.location());
                List<Trip> extra = this.extra.get(vertex.location());
                if (trips != null || extra != null) {
                    Map<GeoLocation, Trip> nextPerDestination
                            = nextPerDestination(
                                    vertex.time().toLocalTime(), trips, extra);
                    for (Trip t : nextPerDestination.values()) {
                        rv.add(ride(vertex, t));
                    }
                }
            }
            return rv;
        }

        @Override
        public Duration edge(JourneyState from, JourneyState to) {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            if (!containsVertex(from)) {
                throw new NoSuchElementException(
                        from + " does not exist");
            }
            if (!containsVertex(to)) {
                throw new NoSuchElementException(
                        to + " does not exist");
            }
            if (neighbours(from).contains(to)) {
                return timeDelta(from, to);
            }
            throw new NoSuchElementException(
                    "edge from " + from + " to "
                    + to + " does not exist");
        }

        @Override
        public boolean containsEdge(JourneyState from, JourneyState to) {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            if (!containsVertex(from)) {
                throw new NoSuchElementException(
                        from + " does not exist");
            }
            if (!containsVertex(to)) {
                throw new NoSuchElementException(
                        to + " does not exist");
            }
            if (neighbours(from).contains(to)) {
                return true;
            }
            return false;
        }

        public Set<JourneyState> reverseNeighbours(JourneyState vertex) {
            Objects.requireNonNull(vertex, "vertex");
            if (!containsVertex(vertex)) {
                throw new NoSuchElementException(
                        vertex + " does not exist");
            }
            // road edges
            Set<JourneyState> rv = new HashSet<>();
            Set<GeoLocation> edges = TransitNetwork.this.roadEdges.get(vertex.location());
            Map<GeoLocation, Duration> artificialEdges
                = TransitNetwork.this.artificialEdges.get(vertex.location());
            if (edges != null) {
                for (GeoLocation neighbourLocation : edges) {
                    rv.add(travelReverse(vertex, neighbourLocation));
                }
            }
            if (artificialEdges != null) {
                if (!vertex.artificial()) {
                    for (Map.Entry<GeoLocation, Duration> neighbour : artificialEdges.entrySet()) {
                        rv.add(travelReverse(vertex, neighbour.getKey(), neighbour.getValue()));
                    }
                }
            }
            if (!vertex.vehicle() && vertex.busPass()) {
                List<ReverseTrip> reverseTrips = TransitNetwork.this.transitEdgesReversed.get(
                        vertex.location());
                List<ReverseTrip> reverseExtra = this.reverseExtra.get(vertex.location());
                if (reverseTrips != null || reverseExtra != null) {
                    Map<GeoLocation, ReverseTrip> previousPerSource
                            = previousPerSource(
                                    vertex.time().toLocalTime(), reverseTrips, reverseExtra);
                    for (ReverseTrip t : previousPerSource.values()) {
                        rv.add(reverseRide(vertex, t));
                    }
                }
            }
            return rv;
        }

        @Override
        public Graph<JourneyState, Duration> reverse() {
            return new Reverse();
        }

        private class Reverse implements Graph<JourneyState, Duration> {

            @Override
            public boolean containsVertex(JourneyState vertex) {
                Objects.requireNonNull(vertex, "vertex");
                return STGraph.this.containsVertex(vertex);
            }

            @Override
            public Set<JourneyState> neighbours(JourneyState vertex) {
                Objects.requireNonNull(vertex, "vertex");
                if (!containsVertex(vertex)) {
                    throw new NoSuchElementException(
                            vertex + " does not exist");
                }
                return STGraph.this.reverseNeighbours(vertex);
            }

            @Override
            public Duration edge(JourneyState from, JourneyState to) {
                Objects.requireNonNull(from, "from");
                Objects.requireNonNull(to, "to");
                if (!containsVertex(from)) {
                    throw new NoSuchElementException(
                            from + " does not exist");
                }
                if (!containsVertex(to)) {
                    throw new NoSuchElementException(
                            to + " does not exist");
                }
                if (Reverse.this.neighbours(from).contains(to)) {
                    return timeDelta(from, to);
                }
                throw new NoSuchElementException(
                        "edge from " + from + " to "
                        + to + " does not exist");
            }

            @Override
            public boolean containsEdge(JourneyState from, JourneyState to) {
                Objects.requireNonNull(from, "from");
                Objects.requireNonNull(to, "to");
                if (!containsVertex(from)) {
                    throw new NoSuchElementException(
                            from + " does not exist");
                }
                if (!containsVertex(to)) {
                    throw new NoSuchElementException(
                            to + " does not exist");
                }
                if (Reverse.this.neighbours(from).contains(to)) {
                    return true;
                }
                return false;
            }

        }

    }

    // given start state and end location, returns end state
    private JourneyState travel(JourneyState from,
            GeoLocation to) {
        Speed v;
        String tag;
        if (from.vehicle()) {
            v = this.carSpeed;
            tag = CAR_TAG;
        } else {
            v = this.walkSpeed;
            tag = WALK_TAG;
        }
        Distance s = GeoLocation.HAVERSINE.distance(from.location(), to);
        Duration t = v.timeTruncatedToSeconds(s);
        return JourneyState.of(to, from.time().plus(t),
                from.vehicle(), from.busPass(), tag);
    }

    // given an end state and start location, returns start state
    private JourneyState travelReverse(JourneyState to,
            GeoLocation from) {
        double v;
        String tag;
        if (to.vehicle()) {
            v = carSpeed.asMetersPerSecond();
            tag = CAR_TAG;
        } else {
            v = walkSpeed.asMetersPerSecond();
            tag = WALK_TAG;
        }
        double s = GeoLocation.HAVERSINE.distance(from, to.location()).asMeters();
        long t = (long) (s / v);
        Duration duration = Duration.ofSeconds(t);
        return JourneyState.of(from, to.time().minus(duration),
                to.vehicle(), to.busPass(), tag);
    }

    // given start state and end location, returns end state
    private static JourneyState travel(JourneyState from,
            GeoLocation to,
            Duration duration) {
        return JourneyState.of(to, from.time().plus(duration),
                from.vehicle(), from.busPass(), true, "artificial");
    }

    // given end state and start location, returns start state
    private static JourneyState travelReverse(JourneyState from,
            GeoLocation to,
            Duration duration) {
        return JourneyState.of(to, from.time().minus(duration),
                from.vehicle(), from.busPass(), true, "artificial");
    }

    private static Map<GeoLocation, Trip>
            nextPerDestination(LocalTime now, List<Trip> trips, List<Trip> extra) {

        Map<GeoLocation, Trip> nextPerDestination = new HashMap<>();

        if (trips != null) {

            for (Trip t : trips) {

                // departs from now
                if (t.departureTime().compareTo(now) >= 0) {

                    GeoLocation destination = t.destination();
                    Trip earliest = nextPerDestination.get(destination);

                    // earliest seen (arrival)
                    if (earliest == null
                            || t.arrivalTime().compareTo(
                                    earliest.arrivalTime()) < 0) {
                        nextPerDestination.put(destination, t);
                    }

                }

            }

        }

        if (extra != null) {

            for (Trip t : extra) {

                // departs from now
                if (t.departureTime().compareTo(now) >= 0) {

                    GeoLocation destination = t.destination();
                    Trip earliest = nextPerDestination.get(destination);

                    // earliest seen (arrival)
                    if (earliest == null
                            || t.arrivalTime().compareTo(
                                    earliest.arrivalTime()) < 0) {
                        nextPerDestination.put(destination, t);
                    }

                }

            }

        }

        return nextPerDestination;

    }

    private static Map<GeoLocation, ReverseTrip> previousPerSource(LocalTime now,
            List<ReverseTrip> reverseTrips, List<ReverseTrip> reverseExtra) {

        Map<GeoLocation, ReverseTrip> previousPerSource = new HashMap<>();

        if (reverseTrips != null) {

            for (ReverseTrip t : reverseTrips) {

                // arrives at latest now
                if (t.arrivalTime().compareTo(now) <= 0) {

                    GeoLocation source = t.source();
                    ReverseTrip latest = previousPerSource.get(source);

                    // latest seen (departure)
                    if (latest == null
                            || t.departureTime().compareTo(
                                    latest.departureTime()) > 0) {
                        previousPerSource.put(source, t);
                    }

                }

            }

        }

        if (reverseExtra != null) {

            for (ReverseTrip t : reverseExtra) {

                // arrives at latest now
                if (t.arrivalTime().compareTo(now) <= 0) {

                    GeoLocation source = t.source();
                    ReverseTrip latest = previousPerSource.get(source);

                    // departureTime: `departureTime`

                    // latest seen (departure)
                    if (latest == null
                            || t.departureTime().compareTo(
                                    latest.departureTime()) > 0) {
                        previousPerSource.put(source, t);
                    }

                }

            }

        }

        return previousPerSource;

    }

    private static JourneyState ride(JourneyState vertex, Trip trip) {
        if (vertex.vehicle()) {
            throw new AssertionError("cannot ride with vehicle");
        }
        if (!vertex.busPass()) {
            throw new AssertionError("cannot ride without bus pass");
        }
        LocalDateTime time = LocalDateTime.of(
                vertex.time().toLocalDate(), trip.arrivalTime());
        return JourneyState.of(trip.destination(), time, false, true, TRANSIT_TAG);
    }

    private static JourneyState reverseRide(JourneyState vertex, ReverseTrip trip) {
        if (vertex.vehicle()) {
            throw new AssertionError("cannot ride with vehicle");
        }
        if (!vertex.busPass()) {
            throw new AssertionError("cannot ride without bus pass");
        }
        LocalDateTime time = LocalDateTime.of(
                vertex.time().toLocalDate(), trip.departureTime());
        return JourneyState.of(trip.source(), time, false, true, TRANSIT_TAG);
    }

    private static Duration timeDelta(JourneyState from, JourneyState to) {
        return Duration.ofSeconds(
                from.time().until(to.time(), ChronoUnit.SECONDS));
    }

    public Set<GeoLocation> roadNeighbours(GeoLocation location) {
        if (this.roadEdges.containsKey(location)) {
            return Collections.unmodifiableSet(this.roadEdges.get(location));
        }
        return Set.of();
    }

    public List<Trip> transitNeighbours(GeoLocation location) {
        if (this.transitEdges.containsKey(location)) {
            return Collections.unmodifiableList(
                    this.transitEdges.get(location));
        }
        return List.of();
    }

    /**
     * Saves the transit network to the specified output stream.
     *
     * @param out the output stream, not null
     * @throws IOException if unable to write the network
     */
    public void write(ObjectOutputStream out) throws IOException {

        GeoLocation A = GeoLocation.ofRounded(58.9814606,-2.9607088);
        GeoLocation B = GeoLocation.ofRounded(58.98102, -2.958743);

        // serializing locations
        Map<GeoLocation, Integer> locationsMap = new HashMap<>();
        int i = 0;
        out.writeInt(this.locations.size());
        //if (DEBUG_SERIALIZATION) {
        //    System.err.println("this.locations.size() = " + this.locations.size());
        //}
        for (GeoLocation location : this.locations) {
            locationsMap.put(location, i++);
            location.write(out);
            //if (DEBUG_SERIALIZATION && location.equals(A)) {
            //    System.err.println("A writen as loc");
            //}
            //if (DEBUG_SERIALIZATION && location.equals(B)) {
            //    System.err.println("B written as loc");
            //}
        }
        // serialized `i` locations
        //if (DEBUG_SERIALIZATION) {
        //    System.err.println("serialized " + i + " locations");
        //}

        // write 0 for backward compatibility with earlier version of the file format
        out.writeInt(0);

        // serializing road edges
        out.writeInt(this.roadEdges.size());
        i = 0;
        //if (DEBUG_SERIALIZATION) {
        //    System.err.println("this.roadEdges.size() = " + this.roadEdges.size());
        //    int empty = 0;
        //    int nonempty = 0;
        //    for (GeoLocation a : this.roadEdges.keySet()) {
        //        if (this.roadEdges.get(a).isEmpty()) {
        //            empty++;
        //        } else {
        //            nonempty++;
        //        }
        //    }
        //    System.out.println("empty : nonempty = " + empty + " : " + nonempty);
        //}
        for (GeoLocation a : this.roadEdges.keySet()) {
            int indexA = locationsMap.get(a);
            out.writeInt(indexA);
            Set<GeoLocation> destinations = this.roadEdges.get(a);
            out.writeInt(destinations.size());
            for (GeoLocation b : destinations) {
                int indexB = locationsMap.get(b);
                out.writeInt(indexB);
                i++;
                //if (DEBUG_SERIALIZATION && a.equals(A)) {
                //    System.err.println("A writen as road outgoing");
                //}
                //if (DEBUG_SERIALIZATION && a.equals(B)) {
                //    System.err.println("B written as road outgoing");
                //}
                //if (DEBUG_SERIALIZATION && b.equals(A)) {
                //    System.err.println("A writen as road incomming");
                //}
                //if (DEBUG_SERIALIZATION && b.equals(B)) {
                //    System.err.println("B written as road incomming");
                //}
            }
        }
        // serialized `i` road edges
        //if (DEBUG_SERIALIZATION) {
        //    System.err.println("serialized " + i + " road edges");
        //}

        // serializing artificial edges
        out.writeInt(this.artificialEdges.size());
        i = 0;
        //if (DEBUG_SERIALIZATION) {
        //    System.err.println("this.artificialEdges.size() = " + this.artificialEdges.size());
        //}
        for (GeoLocation a : this.artificialEdges.keySet()) {
            int indexA = locationsMap.get(a);
            out.writeInt(indexA);
            Map<GeoLocation, Duration> destinations = this.artificialEdges.get(a);
            out.writeInt(destinations.size());
            for (Map.Entry<GeoLocation, Duration> entry : destinations.entrySet()) {
                GeoLocation b = entry.getKey();
                Duration duration = entry.getValue();
                int indexB = locationsMap.get(b);
                out.writeInt(indexB);
                writeDuration(out, duration);
                i++;
                //if (DEBUG_SERIALIZATION && a.equals(A)) {
                //    System.err.println("A writen as artificial outgoing");
                //}
                //if (DEBUG_SERIALIZATION && a.equals(B)) {
                //    System.err.println("B written as artificial outgoing");
                //}
                //if (DEBUG_SERIALIZATION && b.equals(A)) {
                //    System.err.println("A writen as artificial incomming");
                //}
                //if (DEBUG_SERIALIZATION && b.equals(B)) {
                //    System.err.println("B written as artificial incomming");
                //}
            }
        }
        // serialized `i` artificial edges
        //if (DEBUG_SERIALIZATION) {
        //    System.err.println("serialized " + i + " artificial edges");
        //}

        // serializing transit edges
        out.writeInt(this.transitEdges.size());
        i = 0;
        //if (DEBUG_SERIALIZATION) {
        //    System.err.println("this.transitEdges.size() = " + this.transitEdges.size());
        //}
        for (GeoLocation a : this.transitEdges.keySet()) {
            int indexA = locationsMap.get(a);
            out.writeInt(indexA);
            List<Trip> trips = this.transitEdges.get(a);
            out.writeInt(trips.size());
            for (Trip trip : trips) {
                GeoLocation b = trip.destination();
                int indexB = locationsMap.get(b);
                LocalTime departureTime = trip.departureTime();
                LocalTime arrivalTime = trip.arrivalTime();
                out.writeInt(indexB);
                writeTime(out, departureTime);
                writeTime(out, arrivalTime);
                i++;
                //if (DEBUG_SERIALIZATION && a.equals(A)) {
                //    System.err.println("A writen as transit outgoing");
                //}
                //if (DEBUG_SERIALIZATION && a.equals(B)) {
                //    System.err.println("B written as transit outgoing");
                //}
                //if (DEBUG_SERIALIZATION && b.equals(A)) {
                //    System.err.println("A writen as transit incomming");
                //}
                //if (DEBUG_SERIALIZATION && b.equals(B)) {
                //    System.err.println("B written as transit incomming");
                //}
            }
        }
        // serialized `i` transit edges
        //if (DEBUG_SERIALIZATION) {
        //    System.err.println("serialized " + i + " transit edges");
        //}

    }

    public static final TransitNetwork read(ObjectInputStream in) throws IOException {
        return read(in, null, null);
    }

    public static final TransitNetwork read(ObjectInputStream in,
                                            BiConsumer<GeoLocation, GeoLocation> consumer,
                                            BiConsumer<GeoLocation, GeoLocation> consumerArtificial)
            throws IOException {
        return read(in, (g) -> true, consumer, consumerArtificial);
    }

    public static final TransitNetwork read(ObjectInputStream in,
                                            Predicate<GeoLocation> spatialFilter,
                                            BiConsumer<GeoLocation, GeoLocation> consumer,
                                            BiConsumer<GeoLocation, GeoLocation> consumerArtificial)
            throws IOException {

        if (spatialFilter == null) {
            spatialFilter = (g) -> true;
        }
        if (consumer == null) {
            consumer = (a, b) -> {};
        }
        if (consumerArtificial == null) {
            consumerArtificial = (a, b) -> {};
        }

        Builder builder = TransitNetwork.builder(true);

        // deserializing locations
        final int numLocations = in.readInt();
        List<GeoLocation> locations = new ArrayList<>(numLocations);
        for (int i = 0; i < numLocations; i++) {
            GeoLocation location = GeoLocation.read(in);
            locations.add(location);
            if (spatialFilter.test(location)) {
                builder.addLocation(location);
            }
        }
        // deserialized `numLocations` locations

        // read 0 here for file format backward compatibility with the old version of the file format
        final int numCarParks = in.readInt();
        if (numCarParks != 0) {
            throw new IOException("invalid file on transit network deserialization");
        }

        // deserializing road edges
        final int numRoadEdgeOrigins = in.readInt();
        //int numRoadEdges = 0;
        for (int i = 0; i < numRoadEdgeOrigins; i++) {
            int indexA = in.readInt();
            GeoLocation a = locations.get(indexA);
            int numB = in.readInt();
            for (int j = 0; j < numB; j++) {
                int indexB = in.readInt();
                GeoLocation b = locations.get(indexB);
                if (spatialFilter.test(a) && spatialFilter.test(b)) {
                    builder.addRoad(a, b);
                }
                //numRoadEdges++;
            }
        }
        // deserialized `numRoadEdges` road edges

        // deserializing artificial edges
        final int numArtificialEdgeOrigins = in.readInt();
        //int numArtificialEdges = 0;
        for (int i = 0; i < numArtificialEdgeOrigins; i++) {
            int indexA = in.readInt();
            GeoLocation a = locations.get(indexA);
            int numB = in.readInt();
            for (int j = 0; j < numB; j++) {
                int indexB = in.readInt();
                GeoLocation b = locations.get(indexB);
                Duration duration = readDuration(in);
                if (spatialFilter.test(a) && spatialFilter.test(b)) {
                    builder.addArtificialEdge(a, b, duration);
                    //numArtificialEdges++;
                    consumerArtificial.accept(a, b);
                }
            }
        }
        // deserialized `numArtificialEdges` artificial edges

        // deserializing transit edges
        final int numTransitEdgeOrigins = in.readInt();
        //int numTransitEdges = 0;
        for (int i = 0; i < numTransitEdgeOrigins; i++) {
            int indexA = in.readInt();
            GeoLocation a = locations.get(indexA);
            int numB = in.readInt();
            for (int j = 0; j < numB; j++) {
                int indexB = in.readInt();
                GeoLocation b = locations.get(indexB);
                LocalTime departureTime = readTime(in);
                LocalTime arrivalTime = readTime(in);
                if (spatialFilter.test(a) && spatialFilter.test(b)) {
                    builder.addTransit(a, b, departureTime, arrivalTime);
                    //numTransitEdges++;
                    consumer.accept(a, b);
                }
            }
        }
        // deserialized `numTransitEdges` transit edges

        return builder.build();

    }

    private static void writeDuration(ObjectOutputStream out, Duration value) throws IOException {
        long seconds = value.toSeconds();
        int nanos = value.toNanosPart();
        out.writeLong(seconds);
        out.writeInt(nanos);
        if (!Duration.ofSeconds(seconds, nanos).equals(value)) {
            throw new AssertionError("duration write error");
        }
    }

    private static Duration readDuration(ObjectInputStream in) throws IOException {
        long seconds = in.readLong();
        int nanos = in.readInt();
        return Duration.ofSeconds(seconds, nanos);
    }

    private static void writeTime(ObjectOutputStream out, LocalTime time) throws IOException {
        byte hour = (byte) time.getHour();
        byte minute = (byte) time.getMinute();
        byte second = (byte) time.getSecond();
        int nanos = time.getNano();
        out.writeByte(hour);
        out.writeByte(minute);
        out.writeByte(second);
        out.writeInt(nanos);
    }

    private static LocalTime readTime(ObjectInputStream in) throws IOException {
        byte hour = in.readByte();
        byte minute = in.readByte();
        byte second = in.readByte();
        int nanos = in.readInt();
        return LocalTime.of(hour, minute, second, nanos);
    }

    @Deprecated
    public void debugDump(File folder) throws FileNotFoundException {
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
                new File(folder, "DEBUG.csv"))))) {

            out.println("Latitude,Longitude,Type");

            for (GeoLocation location : this.locations) {
                out.println(location.latitude() + "," + location.longitude() + ",Location");
            }

            Set<GeoLocation> set = new HashSet<>();
            for (GeoLocation a : this.roadEdges.keySet()) {
                for (GeoLocation b : this.roadEdges.get(a)) {
                    set.add(a);
                    set.add(b);
                }
            }
            for (GeoLocation location : set) {
                out.println(location.latitude() + "," + location.longitude() + ",Road");
            }

            set = new HashSet<>();
            for (GeoLocation a : this.transitEdges.keySet()) {
                for (Trip trip : this.transitEdges.get(a)) {
                    set.add(a);
                    set.add(trip.destination());
                }
            }
            for (GeoLocation location : set) {
                out.println(location.latitude() + "," + location.longitude() + ",Transit");
            }

            set = new HashSet<>();
            for (GeoLocation a : this.artificialEdges.keySet()) {
                for (Map.Entry<GeoLocation, Duration> edge
                        : this.artificialEdges.get(a).entrySet()) {
                    set.add(a);
                    set.add(edge.getKey());
                }
            }
            for (GeoLocation location : set) {
                out.println(location.latitude() + "," + location.longitude() + ",Artificial");
            }
        }
    }

}
