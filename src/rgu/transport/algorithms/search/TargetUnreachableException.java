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

import java.io.*;

/**
 * The specified target cannot be reached from the specified source.
 *
 * @author Lee A. Christie
 */
public final class TargetUnreachableException extends Exception {

    /**
     * Serial version UID required because Exception is Serializable.
     */
    @Serial
    private static final long serialVersionUID = -3072526424293914481L;

    /**
     * Default constructor
     */
    public TargetUnreachableException() {
    }

    /**
     * Constructor with given message.
     *
     * @param message the message, not null
     */
    public TargetUnreachableException(String message) {
        super(message);
    }

    /**
     * Constructor with given message and cause.
     *
     * @param message the message, not null
     * @param cause   the cause, not null
     */
    public TargetUnreachableException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with given cause.
     *
     * @param cause the cause, not null
     */
    public TargetUnreachableException(Throwable cause) {
        super(cause);
    }

}
