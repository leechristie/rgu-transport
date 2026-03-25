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

package rgu.transport.util;

import rgu.transport.algorithms.collections.*;
import rgu.transport.geospatial.*;
import rgu.transport.geospatial.osm.*;

import java.util.*;
import java.util.function.*;

/**
 * <p>Can apply a speed cap and default speed to a graph. The maximum speed is specified for a
 * predicate, and all edges for which one or both vertices test true will be limited to the given
 * speed.</p>
 *
 * <p>The maximum speed edges are set to the cap if the original value was over, and all other edges
 * are reduced by the same percentage. If the original maximum in the graph was under the stated
 * maximum, then no edges are reduced. Any edges which did have any specified speed will be set to
 * the minimum resulting speed of any other edge.</p>
 *
 * <p>Edges which are not inside the predicate will be capped to a maximum speed of the minimum
 * speed within any of the predicates.</p>
 *
 * <p>If predicates overlap, then the first one added takes priority.</p>
 */
public final class PercentSpeedCap {

    private final Graph<GeoLocation, RoadEdge> graph;
    private List<PredicateCap> caps = new ArrayList<>();

    /**
     * Creates an instance of percent speed cap.
     *
     * @param graph the graph, not null
     */
    public PercentSpeedCap(Graph<GeoLocation, RoadEdge> graph) {
        Objects.requireNonNull(graph, "graph");
        this.graph = graph;
    }

    /**
     * Adds a predicate with a speed cap.
     *
     * @param name the name, not empty, not null, unique
     * @param predicate the predicate for vertices, not null
     * @param maximum the maximum for the predicate, >= 0
     */
    public void add(String name, Predicate<GeoLocation> predicate, Speed maximum) {
        caps.add(new PredicateCap(name, predicate, maximum));
    }

    /**
     * Applies the speed cap to the given graph, returning a new speed-capped graph.
     *
     * @return the speed-capped graph
     */
    public Graph<GeoLocation, RoadEdge> apply() {
        return apply(false);
    }

    /**
     * Applies the speed cap to the given graph, returning a new speed-capped graph.
     *
     * @param verbose if true, extra information will be printed to STDOUT.
     * @return the speed-capped graph
     */
    public Graph<GeoLocation, RoadEdge> apply(boolean verbose) {
        if (verbose) {
            System.out.println("applying percent speed cap...");
        }
        if (this.caps.isEmpty()) {
            throw new NoSuchElementException("no predicates given");
        }
        HashGraph.Builder<GeoLocation, RoadEdge> builder = HashGraph.builder();
        builder.addVertices(graph.vertices());
        Map<String, Double> multipliers = calculateMultipliers(verbose);
        Speed minimumNewValueAllPredicate = applyMultipliersInsidePredicates(builder, multipliers);
        Speed maximumOldValueOutsidePredicate = calculateMaximumOldValueOutsidePredicate();
        if (maximumOldValueOutsidePredicate != null) {
            if (verbose) {
                System.out.println("  applying speed cap outside predicates...");
                System.out.println("    minimum new value in predicates is "
                                   + minimumNewValueAllPredicate);
            }
            Speed minimumNewValueOutsidePredicate = applyMultipliersOutside(
                    builder,
                    minimumNewValueAllPredicate,
                    maximumOldValueOutsidePredicate,
                    verbose);
            applyMinimumOutside(builder, minimumNewValueOutsidePredicate);
        } else {
            if (verbose) {
                System.out.println(
                    "  skipped applying cap to outside, and no edges were detected outside");
            }
        }
        return builder.build();
    }

    private Map<String, Double> calculateMultipliers(boolean verbose) {
        Map<String, Speed> currentMaximum = new HashMap<>();
        Map<String, Speed> currentMinimum = new HashMap<>();
        for (PredicateCap pc : caps) {
            String name = pc.name;
            if (currentMaximum.containsKey(name)) {
                throw new IllegalArgumentException("duplicate predicate name \"" + name + "\"");
            }
            currentMaximum.put(name, null);
            currentMinimum.put(name, null);
        }
        Map<String, Double> multiplier = new HashMap<>();
        if (verbose) {
            System.out.println("  calculating multipliers...");
        }
        for (PredicateCap pc : caps) {
            String name = pc.name;
            if (verbose) {
                System.out.println("    calculating multiplier for \"" + name + "\"...");
            }
            Predicate<GeoLocation> predicate = pc.predicate;
            Speed targetMaximum = pc.maximum;
            graph.preserveEdges(e -> (predicate.test(e.to()) || predicate.test(e.from())))
                    .forEachEdge(e -> {
                        currentMaximum.put(name, max(currentMaximum.get(name), e.value().speed()));
                        currentMinimum.put(name, min(currentMinimum.get(name), e.value().speed()));
                    });
            if (currentMaximum.get(name) == null) {
                throw new NoSuchElementException(
                    "predicate \"" + name
                    + "\" did not find any edges, or did not find any edges with speed assigned");
            }
            if (verbose) {
                System.out.println("      current maximum value for is "
                    + currentMaximum.get(name));
                System.out.println("      target maximum value for is "
                    + targetMaximum);
            }

            if (lessThanEq(currentMaximum.get(name), targetMaximum)) {
                multiplier.put(name, 1.0);
            } else {
                multiplier.put(name, divide(targetMaximum, currentMaximum.get(name)));
            }
            if (verbose) {
                System.out.println("      calculated multiplier will be x"
                    + multiplier.get(name));
                System.out.println("      current minimum value is "
                    + currentMinimum.get(name));
                System.out.println("      calculate new minimum will be "
                    + multiply(currentMinimum.get(name), multiplier.get(name)));
            }
        }
        return multiplier;
    }

    private Speed multiply(Speed speed, Double multiplier) {
        return Speed.ofMetersPerSecond(speed.asMetersPerSecond() * multiplier);
    }

    private Double divide(Speed a, Speed b) {
        return a.asMetersPerSecond() / b.asMetersPerSecond();
    }

    private boolean lessThanEq(Speed speed, Speed targetMaximum) {
        return speed.compareTo(targetMaximum) <= 0;
    }

    private static Speed min(Speed a, Speed b) {
        double aVal = a == null ? Double.NaN : a.asMetersPerSecond();
        double bVal = b == null ? Double.NaN : b.asMetersPerSecond();
        double rv = min(aVal, bVal);
        if (Double.isFinite(rv)) {
            return Speed.ofMetersPerSecond(rv);
        }
        return null;
    }

    private static Speed max(Speed a, Speed b) {
        double aVal = a == null ? Double.NaN : a.asMetersPerSecond();
        double bVal = b == null ? Double.NaN : b.asMetersPerSecond();
        double rv = max(aVal, bVal);
        if (Double.isFinite(rv)) {
            return Speed.ofMetersPerSecond(rv);
        }
        return null;
    }

    private static double min(double a, double b) {
        if (Double.isFinite(a) && Double.isFinite(b)) {
            if (a < b) {
                return a;
            }
            return b;
        } else if (Double.isFinite(a)) {
            return a;
        } else if (Double.isFinite(b)) {
            return b;
        }
        return Double.NaN;
    }

    private static double max(double a, double b) {
        if (Double.isFinite(a) && Double.isFinite(b)) {
            return Math.max(a, b);
        } else if (Double.isFinite(a)) {
            return a;
        } else if (Double.isFinite(b)) {
            return b;
        }
        return Double.NaN;
    }


    // Using the specified multipliers, each predicate has the speeds reduced
    // Priority in case of overlap given to first in list
    // Returns the minimum new speed value after reduction (in any predicate)
    private Speed applyMultipliersInsidePredicates(HashGraph.Builder<GeoLocation, RoadEdge> builder,
                                                    Map<String, Double> multipliers) {

        Speed minimumNewValueAllPredicate = null;
        for (PredicateCap pc : caps) {

            String name = pc.name;
            Predicate<GeoLocation> predicate = pc.predicate;
            Double multiplier = multipliers.get(name);

            // first pass will add all defined speed edges and calculate new minimum
            Speed minimumForThisPredicate = null;
            for (Graph.Edge<GeoLocation, RoadEdge> edge : edges(predicate)) {
                if (hasSpeed(edge)) {
                    RoadEdge newValue = multiply(edge.value(), multiplier);
                    if (builder.tryAddEdge(edge.from(), edge.to(), newValue)) {
                        minimumForThisPredicate = min(minimumForThisPredicate, newValue.speed());
                    }
                }
            }
            minimumNewValueAllPredicate = min(minimumNewValueAllPredicate, minimumForThisPredicate);

            // second pass for minimum assignment
            for (Graph.Edge<GeoLocation, RoadEdge> edge : edges(predicate)) {
                RoadEdge newEdge = new RoadEdge(edge.value().distance(), minimumForThisPredicate);
                builder.tryAddEdge(edge.from(), edge.to(), newEdge);
            }

        }

        return minimumNewValueAllPredicate;

    }

    private boolean isOutsidePredicates(Graph.Edge<GeoLocation, RoadEdge> edge) {
        for (PredicateCap pc : caps) {
            if (pc.predicate.test(edge.from()) || pc.predicate.test(edge.to())) {
                return false;
            }
        }
        return true;
    }

    private List<Graph.Edge<GeoLocation, RoadEdge>> edges(Predicate<GeoLocation> predicate) {
        List<Graph.Edge<GeoLocation, RoadEdge>> rv = new ArrayList<>();
        for (GeoLocation from : graph.vertices()) {
            for (GeoLocation to : graph.neighbours(from)) {
                if (predicate.test(from) || predicate.test(to)) {
                    rv.add(new Graph.Edge<>(from, to, graph.edge(from, to)));
                }
            }
        }
        return rv;
    }

    private List<Graph.Edge<GeoLocation, RoadEdge>> edges() {
        return edges(x -> true);
    }

    private Speed calculateMaximumOldValueOutsidePredicate() {
        Speed maximumOldValueOutsidePredicate = null;
        for (var edge : edges()) {
            if (isOutsidePredicates(edge)) {
                maximumOldValueOutsidePredicate
                    = max(maximumOldValueOutsidePredicate, edge.value().speed());
            }
        }
        return maximumOldValueOutsidePredicate;
    }


    // the first pass for edges outside the predicate, sets the edges with speed
    private Speed applyMultipliersOutside(HashGraph.Builder<GeoLocation, RoadEdge> builder,
                                          Speed minimumNewValueAllPredicate,
                                          Speed maximumOldValueOutsidePredicate,
                                          boolean verbose) {
        Speed minimumOldValueOutsidePredicate = null;
        Speed minimumNewValueOutsidePredicate = null;
        double outsideMultiplier = minimumNewValueAllPredicate.asMetersPerSecond()
            / maximumOldValueOutsidePredicate.asMetersPerSecond();
        if (verbose) {
            System.out.println("    maximum value outside predicates is "
                + maximumOldValueOutsidePredicate);
            System.out.println("    outside multiplier will be x"
                + outsideMultiplier);
        }
        for (var edge : edges()) {
            if (isOutsidePredicates(edge) && hasSpeed(edge)) {
                RoadEdge newValue = multiply(edge.value(), outsideMultiplier);
                builder.addEdge(edge.from(), edge.to(), newValue);
                minimumOldValueOutsidePredicate = min(minimumOldValueOutsidePredicate,
                                                      edge.value().speed());
                minimumNewValueOutsidePredicate = min(minimumNewValueOutsidePredicate,
                                                      newValue.speed());
            }
        }
        if (verbose) {
            System.out.println("    minimum current value outside predicates is "
                + minimumOldValueOutsidePredicate);
            System.out.println("    minimum new value outside predicates will be "
                + minimumNewValueOutsidePredicate);
        }
        return minimumNewValueOutsidePredicate;
    }

    private Graph.Edge<GeoLocation, RoadEdge> multiply(
            Graph.Edge<GeoLocation, RoadEdge> edge, double m) {
        return new Graph.Edge<>(edge.from(), edge.to(), multiply(edge.value(), m));
    }

    private RoadEdge multiply(RoadEdge edge, double m) {
        return new RoadEdge(edge.distance(),
            Speed.ofMetersPerSecond(edge.speed().asMetersPerSecond() * m));
    }

    private Graph.Edge<GeoLocation, RoadEdge> multiply(
            Graph.Edge<GeoLocation, RoadEdge> edge, Double multiplier) {
        return new Graph.Edge<>(edge.from(), edge.to(), multiply(edge.value(), multiplier));
    }

    private boolean hasSpeed(Graph.Edge<GeoLocation, RoadEdge> edge) {
        return edge.value().speed() != null;
    }

    // the second pass for edges outside the predicate, sets the edges with no speed
    private void applyMinimumOutside(HashGraph.Builder<GeoLocation, RoadEdge> builder,
                                     Speed minimumNewValueOutsidePredicate) {
        for (var edge : edges()) {
            if (isOutsidePredicates(edge)) {
                if (edge.value().speed() == null) {
                    RoadEdge newEdge = new RoadEdge(
                            edge.value().distance(),
                            minimumNewValueOutsidePredicate);
                    builder.addEdge(edge.from(), edge.to(), newEdge);
                }
            }
        }
    }

    private record PredicateCap(String name, Predicate<GeoLocation> predicate, Speed maximum) {
        public PredicateCap {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(predicate, "predicate");
            Objects.requireNonNull(maximum, "maximum");
            if (name.isEmpty()) {
                throw new IllegalArgumentException("name = \"\", expected: length >= 1");
            }
        }
    }

}
