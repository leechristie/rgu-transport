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
import rgu.transport.algorithms.search.*;
import rgu.transport.algorithms.util.*;
import rgu.transport.geospatial.*;
import rgu.transport.geospatial.multimodal.*;

import java.util.*;

/**
 * Utility class for working with locations.
 *
 * @author Lee A. Christie
 */
public class GeoLocations {

    private GeoLocations() {
        throw new AssertionError("utility class constructor");
    }

    public static Set<GeoLocation> allLocations(Collection<TransitNetwork> networks)
            throws InterruptedException {
        Set<GeoLocation> rv = new HashSet<>();
        for (TransitNetwork network : networks) {
            ThreadUtil.checkInterrupt();
            rv.addAll(network.locations());
        }
        return rv;
    }

    public static Map<GeoLocation, GeoLocation> findWithTwoDTree(
            TwoDTree<GeoLocation> tree, Collection<GeoLocation> targets,
            ProgressListener progress, boolean subStageOnly) throws InterruptedException {
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(progress, "progress");
        final int total = targets.size();
        int done = 0;
        Map<GeoLocation, GeoLocation> rv = new HashMap<>();
        if (!subStageOnly) {
            progress.onNewStage("Finding nearest points...");
        }
        progress.onUpdateProgress(0.0);
        for (GeoLocation t : targets) {
            ThreadUtil.checkInterrupt();
            rv.put(t, tree.withRatio(t.estimateCurvature()).nearest(t));
            done++;
            if (done < total) {
                progress.onUpdateProgress(((double) done) / total);
            }
        }
        progress.onUpdateProgress(1.0);
        if (!subStageOnly) {
            progress.onCompletion();
        }
        return rv;
    }

}
