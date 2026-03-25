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

import org.junit.jupiter.api.*;
import org.opentest4j.*;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class TestConsoleProgressListener {

    MockOutputStream mock = new MockOutputStream();
    PrintStream out = new PrintStream(mock);

    @Test
    void can_create_listener() {
        ConsoleProgressListener listener
                = (ConsoleProgressListener) ProgressListener.console(out);
        assertNotNull(listener);
        assertSame(out, listener.out());
    }

    @Test
    void no_output_on_end_before_begin() {

        ProgressListener listener = ProgressListener.console(out);

        listener.onCompletion();
        assertEquals("", mock.get());

    }

    @Test
    void start_and_end_string() {

        ProgressListener listener = ProgressListener.console(out);
        assertNotContains("Hello World", mock.get());
        assertNotContains("(time taken: ", mock.get());

        listener.onNewStage("Hello World");
        assertContains("Hello World", mock.get());
        assertNotContains("(time taken: ", mock.get());

        listener.onCompletion();
        assertContains("(time taken: ", mock.get());

    }

    @Test
    void percentage_saved() {

        ProgressListener listener = ProgressListener.console(out);

        listener.onNewStage("Start");
        assertNotContains("12.3%", mock.get());
        assertNotContains("20.9%", mock.get());

        listener.onUpdateProgress(0.123456);
        assertContains("12.3%", mock.get());
        assertNotContains("20.9%", mock.get());

        listener.onUpdateProgress(0.209);
        assertContains("12.3%", mock.get());
        assertContains("20.9%", mock.get());

    }

    @Test
    void percent_same_does_not_reprint() {

        ProgressListener listener = ProgressListener.console(out);

        listener.onNewStage("Start");
        assertEquals(0, count(mock.get(), "7"));

        listener.onUpdateProgress(0.17300);
        assertEquals(1, count(mock.get(), "7"));

        listener.onUpdateProgress(0.17301); // rounds to same thing
        assertEquals(1, count(mock.get(), "7")); // still same number

        listener.onUpdateProgress(0.17399);
        assertEquals(2, count(mock.get(), "7")); // increased

    }

    int count(String string, String subString) {
        return string.split(subString).length - 1;
    }

    static void assertNotContains(String unexpected, String message) {
        if (message.contains(unexpected)) {
            throw new AssertionFailedError("contained unexpected substring", unexpected, message);
        }
    }

    static void assertContains(String expected, String message) {
        if (!message.contains(expected)) {
            throw new AssertionFailedError("did not contain expected substring", expected, message);
        }
    }

//    @Test
//    void test_stdout() {
//        ProgressListener listener
//                = ProgressListener.console(new PrintStream(mock));
//        assertSame(System.out, ((ConsoleProgressListener) listener).out());
//    }

    @Test
    void no_effect_from_cancel_action() {
        ProgressListener listener
                = ProgressListener.console(new PrintStream(mock));
        listener.onNewStage("Start");
        listener.onUpdateProgress(0.12345);
        String before = mock.get();
        listener.setCancelAction(null);
        assertEquals(before, mock.get());
        listener.setCancelAction(() -> {});
        assertEquals(before, mock.get());
    }

}
