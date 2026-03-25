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

import java.math.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.*;

import static org.junit.jupiter.api.Assertions.*;

class TestGeoLocation {

    @Test
    void null_island_is_0_0() {

        GeoLocation nullIsland = GeoLocation.NULL_ISLAND;

        assertEquals(0.0, nullIsland.latitude().doubleValue());
        assertEquals(0.0, nullIsland.longitude().doubleValue());

        assertEquals(0.0, nullIsland.latitudeAsDouble());
        assertEquals(0.0, nullIsland.longitudeAsDouble());

        assertEquals(0, nullIsland.latitudeAsDecimal().compareTo(BigDecimal.valueOf(0)));
        assertEquals(0, nullIsland.longitudeAsDecimal().compareTo(BigDecimal.valueOf(0)));

    }

    @Test
    void parse_lat_lon_as_str() {

        GeoLocation loc;

        loc = GeoLocation.parseLocation("1.2222222", "2.3333333");
        assertEquals("1.2222222", loc.latitude().toString());
        assertEquals("2.3333333", loc.longitude().toString());
        loc = GeoLocation.parseLocation("0.1111111", "0.2222222");
        assertEquals("0.1111111", loc.latitude().toString());
        assertEquals("0.2222222", loc.longitude().toString());

        loc = GeoLocation.parseLocation("-1.2222222", "-2.3333333");
        assertEquals("-1.2222222", loc.latitude().toString());
        assertEquals("-2.3333333", loc.longitude().toString());
        loc = GeoLocation.parseLocation("-0.1111111", "-0.2222222");
        assertEquals("-0.1111111", loc.latitude().toString());
        assertEquals("-0.2222222", loc.longitude().toString());

        loc = GeoLocation.parseLocation("1.2222222", "-2.3333333");
        assertEquals("1.2222222", loc.latitude().toString());
        assertEquals("-2.3333333", loc.longitude().toString());
        loc = GeoLocation.parseLocation("0.1111111", "-0.2222222");
        assertEquals("0.1111111", loc.latitude().toString());
        assertEquals("-0.2222222", loc.longitude().toString());

        loc = GeoLocation.parseLocation("-1.2222222", "2.3333333");
        assertEquals("-1.2222222", loc.latitude().toString());
        assertEquals("2.3333333", loc.longitude().toString());
        loc = GeoLocation.parseLocation("-0.1111111", "0.2222222");
        assertEquals("-0.1111111", loc.latitude().toString());
        assertEquals("0.2222222", loc.longitude().toString());

    }

    @Test
    void parse_too_long() {
        assertThrows(IllegalArgumentException.class,
                () -> GeoLocation.parseLocation("1.22222222", "2.3333333"));
        assertErrorContains("7 or fewer decimal places",
                () -> GeoLocation.parseLocation("1.22222222", "2.3333333"));
        assertErrorContains("latitude",
                () -> GeoLocation.parseLocation("1.22222222", "2.3333333"));
        assertThrows(IllegalArgumentException.class,
                () -> GeoLocation.parseLocation("1.2222222", "2.33333333"));
        assertErrorContains("7 or fewer decimal places",
                () -> GeoLocation.parseLocation("1.2222222", "2.33333333"));
        assertErrorContains("longitude",
                () -> GeoLocation.parseLocation("1.2222222", "2.33333333"));
    }

    @Test
    void lat_can_go_to_plus_minus_90() {
        assertEquals("89.9999999",
                GeoLocation.parseLocation("89.9999999", "0").latitude().toString());
        assertEquals("-89.9999999",
                GeoLocation.parseLocation("-89.9999999", "0").latitude().toString());
        assertEquals("90",
                GeoLocation.parseLocation("90.0000000", "0").latitude().toString());
        assertEquals("-90",
                GeoLocation.parseLocation("-90.0000000", "0").latitude().toString());
        assertThrows(IllegalArgumentException.class,
                () -> GeoLocation.parseLocation("90.0000001", "0"));
        assertErrorContains("latitude",
                () -> GeoLocation.parseLocation("90.0000001", "0"));
        assertErrorContains("[-90, 90]",
                () -> GeoLocation.parseLocation("90.0000001", "0"));
        assertThrows(IllegalArgumentException.class,
                () -> GeoLocation.parseLocation("-90.0000001", "0"));
        assertErrorContains("latitude",
                () -> GeoLocation.parseLocation("-90.0000001", "0"));
        assertErrorContains("[-90, 90]",
                () -> GeoLocation.parseLocation("-90.0000001", "0"));
    }

    @Test
    void lon_can_go_to_plus_minus_189() {
        assertEquals("179.9999999",
                GeoLocation.parseLocation("0", "179.9999999").longitude().toString());
        assertEquals("-179.9999999",
                GeoLocation.parseLocation("0", "-179.9999999").longitude().toString());
        assertEquals("180",
                GeoLocation.parseLocation("0", "180.0000000").longitude().toString());
        assertThrows(IllegalArgumentException.class,
                () -> GeoLocation.parseLocation("0", "-180.0000000").longitude().toString());
        assertErrorContains("longitude",
                () -> GeoLocation.parseLocation("0", "-180.0000000").longitude().toString());
        assertErrorContains("(-180, 180]",
                () -> GeoLocation.parseLocation("0", "-180.0000000").longitude().toString());
        assertThrows(IllegalArgumentException.class,
                () -> GeoLocation.parseLocation("0", "180.0000001"));
        assertErrorContains("longitude",
                () -> GeoLocation.parseLocation("0", "180.0000001"));
        assertErrorContains("(-180, 180]",
                () -> GeoLocation.parseLocation("0", "180.0000001"));
        assertThrows(IllegalArgumentException.class,
                () -> GeoLocation.parseLocation("0", "-180.0000001"));
        assertErrorContains("longitude",
                () -> GeoLocation.parseLocation("0", "-180.0000001"));
        assertErrorContains("(-180, 180]",
                () -> GeoLocation.parseLocation("0", "-180.0000001"));
    }

    @Test
    void equality_simple() {
        GeoLocation a = GeoLocation.parseLocation("1", "2");
        GeoLocation b = GeoLocation.parseLocation("1", "2");
        GeoLocation c = GeoLocation.parseLocation("2", "2");
        GeoLocation d = GeoLocation.parseLocation("1", "3");
        GeoLocation e = GeoLocation.parseLocation("2", "3");
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(a.equals(d));
        assertFalse(a.equals(e));
        assertTrue(b.equals(a));
        assertFalse(c.equals(a));
        assertFalse(d.equals(a));
        assertFalse(e.equals(a));
    }

    @Test
    void equality_last_decimal() {
        GeoLocation a = GeoLocation.parseLocation("1.0000001", "1.0000002");
        GeoLocation b = GeoLocation.parseLocation("1.0000001", "1.0000002");
        GeoLocation c = GeoLocation.parseLocation("1.0000002", "1.0000002");
        GeoLocation d = GeoLocation.parseLocation("1.0000001", "1.0000003");
        GeoLocation e = GeoLocation.parseLocation("1.0000002", "1.0000003");
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(a.equals(d));
        assertFalse(a.equals(e));
        assertTrue(b.equals(a));
        assertFalse(c.equals(a));
        assertFalse(d.equals(a));
        assertFalse(e.equals(a));
    }

    @Test
    void diff_factories() {
        assertEquals(GeoLocation.parseLocation("3.0000004", "0").latitude(),
                     Latitude.of(BigDecimal.valueOf(30000004, 7)));
        assertEquals(GeoLocation.parseLocation("0", "1.0000002").longitude(),
                     Longitude.of(BigDecimal.valueOf(10000002, 7)));
    }

    @Test
    void equality_diff_factories() {
        GeoLocation a = GeoLocation.parseLocation("1.0000001", "1.0000002");
        GeoLocation b = GeoLocation.ofRounded(1.0000001, 1.0000002);
        GeoLocation c = GeoLocation.of(Latitude.of(BigDecimal.valueOf(10000001, 7)),
                                       Longitude.of(BigDecimal.valueOf(10000002, 7)));
        assertTrue(a.equals(b));
        assertTrue(a.equals(c));
        assertTrue(b.equals(c));
        assertTrue(b.equals(a));
        assertTrue(c.equals(a));
        assertTrue(c.equals(a));
    }

    @Test
    void haversine_example() {
        GeoLocation a = GeoLocation.parseLocation("1.23", "1.34");
        GeoLocation b = GeoLocation.parseLocation("1.24", "1.35");
        assertEquals(1.572, GeoLocation.HAVERSINE.distance(a, b).asKilometers(), 0.0005); // 4 s.f.
        assertEquals(1.572, GeoLocation.HAVERSINE.distance(b, a).asKilometers(), 0.0005); // 4 s.f.
        GeoLocation c = GeoLocation.parseLocation("57.1439", "2.0958");
        GeoLocation d = GeoLocation.parseLocation("57.1476", "2.0942");
        assertEquals(0.4226, GeoLocation.HAVERSINE.distance(c, d).asKilometers(), 0.00005); // 4 s.f.
        assertEquals(0.4226, GeoLocation.HAVERSINE.distance(d, c).asKilometers(), 0.00005); // 4 s.f.
    }

    private void assertErrorContains(String substring, Executable executable) {
        try {
            executable.execute();
            fail("nothing was thrown");
        } catch (Throwable t) {
            assertTrue(t.getMessage().contains(substring), "Expected substring: \"" + substring
                    + "\"\nException message: " + t.getMessage());
        }
    }

}
