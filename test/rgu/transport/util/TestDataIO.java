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

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public final class TestDataIO {

    @Test
    public void testSplitBasic() {
        assertEquals(
                List.of("1", "De Lijn", "http://www.delijn.be", "Europe/Brussels", "nl", "+3270220200"),
                DataIO.splitCommas("1,De Lijn,http://www.delijn.be,Europe/Brussels,nl,+3270220200"));
    }

    @Test
    public void testSplitBasicWithSpacing() {
        assertEquals(
                List.of("1", "De Lijn", "http://www.delijn.be", "Europe/Brussels", "nl", "+3270220200"),
                DataIO.splitCommas("   1,De Lijn,http://www.delijn.be,Europe/Brussels,nl,+3270220200   "));
    }

    @Test
    public void testSplitQuoted() {
        assertEquals(
                List.of("1", "De Lijn", "http://www.delijn.be", "Europe/Brussels", "nl", "+3270220200"),
                DataIO.splitCommas("   1,\"De Lijn\",\"http://www.delijn.be\",\"Europe/Brussels\",\"nl\",\"+3270220200\"   "));
    }

    @Test
    public void testSplitBasicBadComma() {
        assertEquals(
                List.of("1", "De Lijn", "http://www.delijn.be", "Europe", "Brussels", "nl", "+3270220200"),
                DataIO.splitCommas("   1,De Lijn,http://www.delijn.be,Europe,Brussels,nl,+3270220200   "));
    }

    @Test
    public void testSplitQuotesBadComma() {
        assertEquals(
                List.of("1", "De Lijn", "http://www.delijn.be", "Europe,Brussels", "nl", "+3270220200"),
                DataIO.splitCommas("   1,\"De Lijn\",\"http://www.delijn.be\",\"Europe,Brussels\",\"nl\",\"+3270220200\"   "));
    }

    @Test
    public void testSplitBadQuotes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DataIO.splitCommas("   1,\"De Lijn\",http://www.delijn.be\",\"Europe,Brussels\",\"nl\",\"+3270220200\"   "));
    }

    @Test
    public void testSplitQuotesEmptyEntries() {
        assertEquals(
                List.of("1", "De Lijn", "", "Europe,Brussels", "", "+3270220200"),
                DataIO.splitCommas("   1,\"De Lijn\",\"\",\"Europe,Brussels\",\"\",\"+3270220200\"   "));
    }

    @Test
    public void testSplitQuotesEmptyEntriesIncEnd() {
        assertEquals(
                List.of("1", "De Lijn", "", "Europe/Brussels", "", ""),
                DataIO.splitCommas("   1,\"De Lijn\",\"\",\"Europe/Brussels\",\"\",\"\"   "));
    }

    @Test
    public void testSplitAllBlank() {
        assertEquals(
                List.of("", "", "", "", "", ""),
                DataIO.splitCommas(",,,,,"));
    }

    @Test
    public void testSplitAllBlankWithQuotes() {
        assertEquals(
                List.of("", "", "", "", "", ""),
                DataIO.splitCommas("\"\",\"\",\"\",\"\",\"\",\"\""));
    }

    @Test
    public void testSplitAllBlankWithSomeQuotes1() {
        assertEquals(
                List.of("", "", "", "", "", ""),
                DataIO.splitCommas(",\"\",\"\",\"\",\"\",\"\""));
    }

    @Test
    public void testSplitAllBlankWithSomeQuotes2() {
        assertEquals(
                List.of("", "", "", "", "", ""),
                DataIO.splitCommas(",\"\",\"\",\"\",\"\","));
    }

    @Test
    public void testSplitAllBlankWithSomeQuotes3() {
        assertEquals(
                List.of("", "", "", "", "", ""),
                DataIO.splitCommas("\"\",\"\",\"\",\"\",\"\","));
    }

}
