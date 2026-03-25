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

import org.junit.jupiter.api.*;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

public final class TestSpeed {

    @Test
    public void testTime() {

        Speed speed = Speed.ofMetersPerSecond(50);
        Distance distance = Distance.ofMeters(100);
        assertEquals(Duration.ofSeconds(2), speed.timeTruncatedToSeconds(distance));
        assertEquals(Duration.ofSeconds(2), speed.timeRoundedToSeconds(distance));
        assertEquals(Duration.ofSeconds(2), speed.time(distance));

        speed = Speed.ofMetersPerSecond(50);
        distance = Distance.ofMeters(110);
        assertEquals(Duration.ofSeconds(2), speed.timeTruncatedToSeconds(distance));
        assertEquals(Duration.ofSeconds(2), speed.timeRoundedToSeconds(distance));
        assertEquals(Duration.ofSeconds(2, 200000000), speed.time(distance));

        speed = Speed.ofMetersPerSecond(50);
        distance = Distance.ofMeters(145);
        assertEquals(Duration.ofSeconds(2), speed.timeTruncatedToSeconds(distance));
        assertEquals(Duration.ofSeconds(3), speed.timeRoundedToSeconds(distance));
        assertEquals(Duration.ofSeconds(2, 900000000), speed.time(distance));

        distance = Distance.ofMeters(100);
        speed = Speed.ofMetersPerSecond(distance.asMeters() / 42.0123456789);
        assertEquals(Duration.ofSeconds(42), speed.timeTruncatedToSeconds(distance));
        assertEquals(Duration.ofSeconds(42), speed.timeRoundedToSeconds(distance));
        assertEquals(Duration.ofSeconds(42, 12345679), speed.time(distance));

        speed = Speed.ofMetersPerSecond(123456789);
        distance = Distance.ofMeters(123456789);
        assertEquals(Duration.ofSeconds(1), speed.timeTruncatedToSeconds(distance));
        assertEquals(Duration.ofSeconds(1), speed.timeRoundedToSeconds(distance));
        assertEquals(Duration.ofSeconds(1), speed.time(distance));

        speed = Speed.ofMetersPerSecond(100);
        distance = Distance.ofMeters(100000000);
        assertEquals(Duration.ofSeconds(1000000), speed.timeTruncatedToSeconds(distance));
        assertEquals(Duration.ofSeconds(1000000), speed.timeRoundedToSeconds(distance));
        assertEquals(Duration.ofSeconds(1000000), speed.time(distance));

        speed = Speed.ofMetersPerSecond(100);
        distance = Distance.ofMeters(12345678);
        assertEquals(Duration.ofSeconds(123456), speed.timeTruncatedToSeconds(distance));
        assertEquals(Duration.ofSeconds(123457), speed.timeRoundedToSeconds(distance));
        assertEquals(Duration.ofSeconds(123456, 780000000), speed.time(distance));

    }

}
