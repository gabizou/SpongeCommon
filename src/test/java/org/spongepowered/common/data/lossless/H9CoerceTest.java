/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.data.lossless;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.spongepowered.common.data.lossless.LosslessSupport.KEY;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.common.data.builder.Coerce;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * H9: typed getters go through Coerce (SpongeDataView.java:164-202) and list
 * getters silently drop elements that do not coerce (SpongeDataView.java:275-404).
 */
@DisplayName("H9 CONFIRMED: typed getters coerce, truncate and parse; list getters drop non-matching elements")
class H9CoerceTest {

    private static DataContainer with(final Object value) {
        return LosslessSupport.memory().set(KEY, value);
    }

    @Test
    @DisplayName("narrowing number conversions truncate silently (Coerce.java:93-95,183-185,208-210)")
    void numbersAreTruncated() {
        assertEquals(Optional.of(0), with(1L << 32).getInt(KEY));
        assertEquals(Optional.of(1), with(1.9d).getInt(KEY));
        assertEquals(Optional.of((byte) 44), with(300).getByte(KEY));
        assertEquals(Optional.of((short) -31072), with(100_000).getShort(KEY));
        assertEquals(Optional.of(0), with(Double.NaN).getInt(KEY));
        assertEquals(Optional.of(Long.MAX_VALUE), with(Double.POSITIVE_INFINITY).getLong(KEY));
        assertEquals(Optional.of(Float.POSITIVE_INFINITY), with(1e300d).getFloat(KEY));
    }

    @Test
    @DisplayName("strings are parsed, with surprising results for empty/garbage input (Coerce.java:97-110,275-297)")
    void stringsAreParsed() {
        assertEquals(Optional.of(12), with("12").getInt(KEY));
        assertEquals(Optional.of(1), with("1.5").getInt(KEY));
        assertEquals(Optional.of(0), with("").getInt(KEY));
        assertEquals(Optional.of(0L), with("").getLong(KEY));
        assertEquals(Optional.of((short) 0), with("   ").getShort(KEY));
        assertEquals(Optional.of(0), with("1-2").getInt(KEY));
        assertEquals(Optional.of(1000), with("1,000").getInt(KEY));
        assertEquals(Optional.of(9), with("(9, 10)").getInt(KEY));
        assertEquals(Optional.of(9.5), with("[9.5, 10.6]").getDouble(KEY));
        assertEquals(Optional.empty(), with("abc").getInt(KEY));
        assertEquals(Optional.of(Double.NaN), with("NaN").getDouble(KEY));
    }

    @Test
    @DisplayName("getBoolean accepts only Boolean and Byte; \"true\" and Integer 1 are absent (Coerce.java:71-78)")
    void booleanCoercion() {
        assertEquals(Optional.of(true), with((byte) 1).getBoolean(KEY));
        assertEquals(Optional.of(true), with((byte) -5).getBoolean(KEY));
        assertEquals(Optional.empty(), with("true").getBoolean(KEY));
        assertEquals(Optional.empty(), with(1).getBoolean(KEY));
    }

    @Test
    @DisplayName("getString returns toString() of anything, including views and lists (Coerce.java:55-63)")
    void stringCoercionStringifiesEverything() {
        assertEquals(Optional.of("5"), with(5).getString(KEY));
        assertEquals(Optional.of("[1, 2]"), with(List.of(1, 2)).getString(KEY));
        final DataContainer nested = LosslessSupport.memory().set(DataQuery.of("v", "x"), 1);
        assertEquals(nested.getView(KEY).get().toString(), nested.getString(KEY).get());
    }

    @Test
    @DisplayName("getInt on a Boolean is absent, not 0/1")
    void booleanIsNotANumber() {
        assertEquals(Optional.empty(), with(true).getInt(KEY));
    }

    @Test
    @DisplayName("list getters drop elements that fail to coerce, and coerce the rest")
    void listGettersDropAndCoerce() {
        final DataContainer c = with(Arrays.asList(1, "2", "x", 3.7d, true));
        assertEquals(Optional.of(List.of(1, 2, 3)), c.getIntegerList(KEY));
        assertEquals(Optional.of(List.of(true)), c.getBooleanList(KEY));
        assertEquals(Optional.of(List.of("1", "2", "x", "3.7", "true")), c.getStringList(KEY));
        assertEquals(Optional.of(List.of('1', '2', 'x', '3', 't')), c.getCharacterList(KEY));
        assertEquals(Optional.of(List.of()), c.getViewList(KEY));
    }

    @Test
    @DisplayName("getMapList returns an empty list for a stored List<Map>: set() turned the maps into views (DataSerializer.java:103-109)")
    void mapListIsLostAfterSet() {
        final DataContainer c = with(List.of(Map.of("k", 1), Map.of("k", 2)));
        assertEquals(Optional.of(List.of()), c.getMapList(KEY));
        assertEquals(2, c.getViewList(KEY).get().size());
    }

    @Test
    @DisplayName("list getters on a scalar return absent, on an empty list return an empty list")
    void listGettersOnNonLists() {
        assertEquals(Optional.empty(), with(5).getIntegerList(KEY));
        assertEquals(Optional.of(List.of()), with(List.of()).getIntegerList(KEY));
    }

    @Test
    @DisplayName("Coerce.asChar on an empty string is absent (Coerce.java:258-263)")
    void charOfEmptyString() {
        assertEquals(Optional.empty(), Coerce.asChar(""));
    }
}
