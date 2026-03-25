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

package rgu.transport.algorithms.search;

import rgu.transport.algorithms.util.*;

import java.util.*;

// mock object for progress listener callback testing
class MockProgressListener implements ProgressListener {

    String description = null;
    double progress = Double.NaN;
    Runnable action = null;
    boolean done = false;

    int onNewStageCalls = 0;
    int onUpdateProgressCalls = 0;
    int onCompletionCalls = 0;
    int setCancelActionCalls = 0;

    List<Double> progressHistory = new ArrayList<>();

    @Override
    public void onNewStage(String description) {
        onNewStageCalls++;
        this.description = description;
    }

    @Override
    public void onUpdateProgress(double progress) {
        onUpdateProgressCalls++;
        this.progress = progress;
        progressHistory.add(progress);
    }

    @Override
    public void onCompletion() {
        onCompletionCalls++;
        this.done = true;
    }

    @Override
    public void setCancelAction(Runnable action) {
        setCancelActionCalls++;
        this.action = action;
    }

}
