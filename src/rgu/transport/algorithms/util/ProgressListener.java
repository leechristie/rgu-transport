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

/**
 * Interface for progress bar or other progress display.
 *
 * @author Lee A. Christie
 */
@FunctionalInterface
public interface ProgressListener {

    /**
     * A no-op progress listener.
     *
     * @return a no-op progress listener
     */
    static ProgressListener none() {
        return new NoneProgressListener();
    }

    /**
     * A progress listener which prints progress to standard output.
     *
     * @return a progress listener which prints progress to standard output
     */
    @SuppressWarnings("java:S106")
    static ProgressListener console() {
        return new ConsoleProgressListener(System.out);
    }

    /**
     * A progress listener which prints progress to the given output stream.
     *
     * @param out the output stream, not null
     * @return a progress listener which prints progress to the given output
     * stream
     */
    static ProgressListener console(PrintStream out) {
        return new ConsoleProgressListener(out);
    }

    /**
     * Called to indicate the start of a stage of loading. If there is only one
     * stage, then this will only be called once, at the start. If there are
     * multiple stages, this will be called at the start of each stage.
     *
     * @param description the description of the stage of loading, not null
     */
    default void onNewStage(String description) {
        // do nothing
    }

    /**
     * Called to update the progress.
     *
     * @param progress the progress in the current loading stage [0.0, 1.0]
     */
    void onUpdateProgress(double progress);

    /**
     * Called when all loading is complete and the progress indicator is no
     * longer needed.
     */
    default void onCompletion() {
        // do nothing
    }

    /**
     * Updates the action to be performed by the cancel button, if there is one.
     * If there is no cancel button, this does nothing. {@code null} can be used
     * to indicate no-operation.
     *
     * @param action the cancel action
     */
    default void setCancelAction(Runnable action) {
        // do nothing
    }

}
