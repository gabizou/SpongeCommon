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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.spongepowered.common.data.lossless.LosslessSupport.KEY;

import net.kyori.adventure.text.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.time.Month;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Issues found while verifying H1-H9 that are not in the original list.
 * Each test is named Xn to match docs/analysis/dataview-lossless.md.
 */
@DisplayName("Additional findings X1-X19")
class AdditionalFindingsTest {

    @Test
    @DisplayName("X1 data loss: NBT binary write replaces strings over 65535 UTF-8 bytes with \"\" (NbtIo$StringFallbackDataOutput)")
    void x1NbtLongStringsBecomeEmpty() throws IOException {
        final DataContainer c = LosslessSupport.memory()
            .set(DataQuery.of("ascii"), "x".repeat(65_536))
            .set(DataQuery.of("threeByte"), "✓".repeat(21_846)) // 65538 bytes, only 21846 chars
            .set(DataQuery.of("fits"), "x".repeat(65_535));
        final DataContainer read = LosslessSupport.nbtRoundTrip(c);
        assertEquals("", read.getString(DataQuery.of("ascii")).get());
        assertEquals("", read.getString(DataQuery.of("threeByte")).get());
        assertEquals(65_535, read.getString(DataQuery.of("fits")).get().length());
    }

    @Test
    @DisplayName("X1 fix path: NbtIo.writeUnnamedTag (no fallback) throws instead of writing \"\" for the same tag")
    void x1NoFallbackWriteThrows() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("ascii", "x".repeat(65_536));
        final DataOutputStream out = new DataOutputStream(new ByteArrayOutputStream());
        assertThrows(UTFDataFormatException.class, () -> NbtIo.writeUnnamedTag(tag, out));
    }

    @Test
    @DisplayName("X2 silent corruption: FloatTag/DoubleTag.valueOf map -0.0 to the cached ZERO, so NBT loses the sign")
    void x2NbtNegativeZero() {
        assertSame(FloatTag.ZERO, FloatTag.valueOf(-0.0f));
        assertSame(DoubleTag.ZERO, DoubleTag.valueOf(-0.0d));
    }

    @Test
    @DisplayName("X3 silent corruption: NBT treats any key containing \"$Boolean\" as a boolean marker (NBTTranslator.java:83,184-185)")
    void x3NbtBooleanSuffixKeys() throws IOException {
        final DataContainer c = LosslessSupport.memory().set(DataQuery.of("flag$Boolean"), (byte) 7);
        final DataContainer read = LosslessSupport.nbtRoundTrip(c);
        assertEquals(Set.of(DataQuery.of("flag")), read.keys(false));
        assertEquals(Boolean.TRUE, read.get(DataQuery.of("flag")).get());

        // "x" (boolean) and "x$Boolean" (byte) collide on the same NBT key; one is dropped
        final DataContainer collide = LosslessSupport.memory()
            .set(DataQuery.of("x"), true)
            .set(DataQuery.of("x$Boolean"), (byte) 0);
        assertEquals(2, collide.keys(false).size());
        assertEquals(1, LosslessSupport.nbtRoundTrip(collide).keys(false).size());
    }

    @Test
    @DisplayName("X4 error path: JSON input containing null fails with NPE (JsonDataFormat.java:119,137-139 -> MemoryDataView.java:168)")
    void x4JsonNullInput() {
        assertThrows(NullPointerException.class, () -> DataFormats.JSON.get().read("{\"a\":null}"));
    }

    @Test
    @DisplayName("X5 silent corruption: a map with an \"_arraytype\" key is read back as a primitive array (JsonDataFormat.java:114-117,132)")
    void x5JsonArrayTypeHijack() throws IOException {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("_arraytype", "int");
        map.put("value", List.of(1, 2));
        final DataContainer c = LosslessSupport.memory().set(KEY, map);
        assertInstanceOf(DataView.class, c.get(KEY).get());
        final Object read = LosslessSupport.jsonRoundTrip(c).get(KEY).get();
        assertArrayEquals(new int[] {1, 2}, (int[]) read);

        final Map<String, Object> other = Map.of("_arraytype", "text", "value", List.of(1));
        final DataContainer bad = LosslessSupport.memory().set(KEY, other);
        final String json = LosslessSupport.json(bad);
        assertThrows(IllegalArgumentException.class, () -> DataFormats.JSON.get().read(json));
    }

    @Test
    @DisplayName("X6 data loss: one non-finite number makes the whole JSON write fail (JsonDataFormat.java:241-242, Gson strict mode)")
    void x6JsonNonFiniteFailsWholeDocument() {
        final DataContainer c = LosslessSupport.memory().set(DataQuery.of("ok"), 1).set(DataQuery.of("bad"), Double.NaN);
        assertThrows(IllegalArgumentException.class, () -> LosslessSupport.json(c));
    }

    @Test
    @DisplayName("X7 data loss: JSON writes byte[] and small long[] that it cannot read back (JsonDataFormat.java:161-169,184-188)")
    void x7JsonWritesUnreadableArrays() throws IOException {
        final String bytes = LosslessSupport.json(LosslessSupport.memory().set(KEY, new byte[] {1}));
        assertEquals("{\"v\":{\"_arraytype\":\"byte\",\"value\":[1]}}", bytes);
        assertThrows(ClassCastException.class, () -> DataFormats.JSON.get().read(bytes));
        final String longs = LosslessSupport.json(LosslessSupport.memory().set(KEY, new long[] {1L}));
        assertThrows(ClassCastException.class, () -> DataFormats.JSON.get().read(longs));
    }

    @Test
    @DisplayName("X8 silent corruption: createView(path, map) splits dotted keys, set(path, map) does not (SpongeDataView.java:211,213 vs DataSerializer.java:124-126)")
    void x8CreateViewSplitsDottedMapKeys() {
        final Map<String, Object> map = Map.of("a.b", 1);
        final DataContainer viaCreate = LosslessSupport.memory();
        viaCreate.createView(KEY, map);
        final DataContainer viaSet = LosslessSupport.memory().set(KEY, map);
        assertTrue(viaCreate.contains(DataQuery.of("v", "a", "b")));
        assertFalse(viaCreate.contains(DataQuery.of("v", "a.b")));
        assertTrue(viaSet.contains(DataQuery.of("v", "a.b")));
        assertFalse(viaSet.contains(DataQuery.of("v", "a", "b")));
    }

    @Test
    @DisplayName("X9 silent corruption: map keys 1 and \"1\" collapse to one entry (DataSerializer.java:111-129)")
    void x9MapKeyCollision() {
        final Map<Object, Object> map = new LinkedHashMap<>();
        map.put(1, "int");
        map.put("1", "string");
        final DataContainer c = LosslessSupport.memory().set(KEY, map);
        assertEquals(1, c.getView(KEY).get().keys(false).size());
        assertEquals("string", c.getString(DataQuery.of("v", "1")).get());
    }

    @Test
    @DisplayName("X10 error path: Component values are stored raw because translators are looked up by exact class (DataTranslatorProvider.java getSerializer)")
    void x10ComponentIsNotTranslated() {
        final Component text = Component.text("hi");
        final DataContainer c = LosslessSupport.memory().set(KEY, text);
        assertSame(text, c.get(KEY).get());
        assertThrows(IllegalArgumentException.class, () -> LosslessSupport.nbtRoundTrip(c));
        assertThrows(IllegalArgumentException.class, () -> LosslessSupport.json(c));
    }

    @Test
    @DisplayName("X11 silent corruption: CompoundTagDataView hands out internal arrays despite ALL_DATA_CLONED")
    void x11CompoundArrayAliasing() {
        final DataContainer compound = LosslessSupport.compound();
        assertEquals(DataView.SafetyMode.ALL_DATA_CLONED, compound.safetyMode());
        compound.set(KEY, new int[] {1, 2});
        ((int[]) compound.get(KEY).get())[0] = 99;
        assertEquals(99, ((int[]) compound.get(KEY).get())[0]);

        final DataContainer memory = LosslessSupport.memory().set(KEY, new int[] {1, 2});
        ((int[]) memory.get(KEY).get())[0] = 99;
        assertEquals(1, ((int[]) memory.get(KEY).get())[0]);
    }

    @Test
    @DisplayName("X12 silent corruption: CompoundTagDataView strips \"$Boolean\" from any key (CompoundTagDataView.java:63,69)")
    void x12CompoundBooleanSuffixKeys() {
        final DataContainer c = LosslessSupport.compound().set(DataQuery.of("x$Boolean"), 5);
        assertEquals(Set.of(DataQuery.of("x")), c.keys(false));
        assertEquals(5, c.get(DataQuery.of("x")).get());
    }

    @Test
    @DisplayName("X13 typed read-back: getObject restores UUID and Month, getMap restores keys only as Strings")
    void x13TypedReadBack() {
        final UUID uuid = UUID.randomUUID();
        final DataContainer c = LosslessSupport.memory()
            .set(DataQuery.of("u"), uuid)
            .set(DataQuery.of("m"), Month.MARCH)
            .set(DataQuery.of("map"), Map.of(1, "a"));
        assertEquals(uuid, c.getObject(DataQuery.of("u"), UUID.class).get());
        assertEquals(Month.MARCH, c.getObject(DataQuery.of("m"), Month.class).get());
        final Map<?, ?> map = c.getMap(DataQuery.of("map")).get();
        assertEquals(Set.of("1"), map.keySet().stream().map(Object::toString).collect(Collectors.toSet()));
        assertInstanceOf(String.class, map.keySet().iterator().next());
    }

    @Test
    @DisplayName("X14 error path: an empty key is accepted for scalars but rejected for views")
    void x14EmptyKey() {
        final DataContainer c = LosslessSupport.memory();
        c.set(DataQuery.of(""), 1);
        assertEquals(1, c.getInt(DataQuery.of("")).get());
        assertThrows(IllegalArgumentException.class, () -> c.set(DataQuery.of(""), Map.of("k", 1)));
    }

    @Test
    @DisplayName("X15 error path: JSON exponent integers are not parsed (JsonDataFormat.java:180-184)")
    void x15JsonExponentNumbers() {
        assertThrows(NumberFormatException.class, () -> DataFormats.JSON.get().read("{\"a\":1E5}"));
    }

    @Test
    @DisplayName("X16 data loss: values NBT and JSON cannot write are accepted in memory and only fail on save (enum, Optional, short[], Object[])")
    void x16AcceptedButUnwritable() {
        final DataContainer c = LosslessSupport.memory().set(DataQuery.of("ok"), 1).set(KEY, new short[] {1});
        assertThrows(IllegalArgumentException.class, () -> LosslessSupport.nbtRoundTrip(c));
        assertThrows(IllegalArgumentException.class, () -> LosslessSupport.json(c));
    }

    @Test
    @DisplayName("X17 silent corruption: NBT list of booleans reads back as bytes, and mixed lists cannot be written (NBTTranslator.java:138-145)")
    void x17NbtListTyping() throws IOException {
        final DataContainer bools = LosslessSupport.memory().set(KEY, List.of(true));
        assertEquals(List.of((byte) 1), LosslessSupport.nbtRoundTrip(bools).getList(KEY).get());
        final DataContainer mixed = LosslessSupport.memory().set(KEY, List.of(1, "a"));
        assertThrows(UnsupportedOperationException.class, () -> LosslessSupport.nbtRoundTrip(mixed));
    }

    @Test
    @DisplayName("X18 cosmetic: JSON collapses number widths (Byte/Short/small Long -> Integer, Float -> Double) (JsonDataFormat.java:176-190)")
    void x18JsonNumberWidths() throws IOException {
        final DataContainer c = LosslessSupport.memory()
            .set(DataQuery.of("b"), (byte) 1)
            .set(DataQuery.of("l"), 1L)
            .set(DataQuery.of("f"), 0.1f);
        final DataContainer read = LosslessSupport.jsonRoundTrip(c);
        assertEquals(Integer.valueOf(1), read.get(DataQuery.of("b")).get());
        assertEquals(Integer.valueOf(1), read.get(DataQuery.of("l")).get());
        assertEquals(Double.valueOf(0.1d), read.get(DataQuery.of("f")).get());
        // typed getters hide the width change
        assertEquals(1L, read.getLong(DataQuery.of("l")).get());
        assertEquals(0.1f, read.getFloat(DataQuery.of("f")).get());
    }

    @Test
    @DisplayName("X19 cosmetic (API Javadoc): CLONED_ON_SET clones arrays on set but returns the stored array on get (DataSerializer.java:139-142, MemoryDataView.java:147)")
    void x19ClonedOnSetSemantics() {
        final byte[] array = {1, 2};
        final DataContainer c = LosslessSupport.memory(DataView.SafetyMode.CLONED_ON_SET).set(KEY, array);
        final Object first = c.get(KEY).get();
        assertFalse(first == array);
        assertSame(first, c.get(KEY).get());
        final DataContainer all = LosslessSupport.memory(DataView.SafetyMode.ALL_DATA_CLONED).set(KEY, array);
        assertFalse(all.get(KEY).get() == all.get(KEY).get());
    }
}
