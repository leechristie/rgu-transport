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

import org.junit.jupiter.api.*;

import java.util.concurrent.locks.*;

import static org.junit.jupiter.api.Assertions.*;

class TestThreadUtil {

    private volatile boolean threadReady = false;
    private Lock lock = new ReentrantLock();
    private volatile boolean failedNoThrow = false;
    private volatile boolean failedEarlyThrow = false;

    private Thread thread = new Thread(() -> {
        lock.lock();
        for (int i = 0; i < 42; i++) {
            try {
                ThreadUtil.checkInterrupt();
            } catch (InterruptedException ex) {
                failedEarlyThrow = true;
                lock.unlock();
                return;
            }
        }
        threadReady = true;
        lock.unlock();
        long start = System.nanoTime();
        while (System.nanoTime() - start < 1e+8) {
            try {
                ThreadUtil.checkInterrupt();
            } catch (InterruptedException ex) {
                return;
            }
        }
        failedNoThrow = true;
    });

    @Test
    void test_interrupt_correct() throws InterruptedException {
        thread.start();
        while (!threadReady) {
            if (failedEarlyThrow) {
                fail("Unexpected early interrupt");
            }
            Thread.yield();
        }
        System.out.println("ready");
        lock.lock();
        System.out.println("done");
        thread.interrupt();
        thread.join();
        if (failedNoThrow) {
            fail("Expected interrupt was not thrown");
        }
    }

}
