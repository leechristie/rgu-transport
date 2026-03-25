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

package rgu.transport.algorithms.util;

import java.io.*;
import java.time.*;
import java.util.*;

// A simple progress listener which prints to a print stream.
// author Lee A. Christie
final class ConsoleProgressListener implements ProgressListener {

    private final PrintStream out;

    private boolean hasCurrentStage = false;
    private long stageStart = 0;
    private String lastPercent = null;

    PrintStream out() {
        return this.out;
    }

    ConsoleProgressListener(PrintStream out) {
        Objects.requireNonNull(out, "out");
        this.out = out;
    }

    @Override
    public void onNewStage(String description) {
        Objects.requireNonNull(description, "description");
        out.println(description);
        out.flush();
        complete();
        start(description);
    }

    @Override
    public void onUpdateProgress(double progress) {
        String percent = percent(progress);
        if (!percent.equals(lastPercent)) {
            lastPercent = percent;
            out.println(percent);
            out.flush();
        }
    }

    @Override
    public void onCompletion() {
        complete();
    }

    private String percent(double progress) {
        long perThousand = Math.round(progress * 1000.0);
        String perHundred = "" + (perThousand / 10.0);
        return perHundred.substring(0, perHundred.indexOf('.') + 2) + "%";
    }

    private void start(String description) {
        complete();
        hasCurrentStage = true;
        stageStart = System.nanoTime();
        out.println("started stage: " + description);
        out.flush();
        onUpdateProgress(0.0);
    }

    private void complete() {
        if (hasCurrentStage) {
            onUpdateProgress(1.0);
            hasCurrentStage = false;
            long time = System.nanoTime() - stageStart;
            out.println("    (time taken: " + Duration.ofNanos(time) + ")");
            out.flush();
            stageStart = 0;
            lastPercent = null;
        }
    }

}
