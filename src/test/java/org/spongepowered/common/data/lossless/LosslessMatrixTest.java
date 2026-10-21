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

import static org.spongepowered.common.data.lossless.LosslessSupport.KEY;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Lossless matrix: for each value, store it at key "v" and read it back.
 *
 * <ul>
 *     <li>primitive: {@code set(DataQuery, boolean|byte|...|String)} then {@code get}, compared to the input</li>
 *     <li>object: {@code set(DataQuery, Object)} then {@code get}, compared to the input</li>
 *     <li>nbt: the Object-overload container written and read with {@code DataFormats.NBT} (binary),
 *         compared to what the container held before writing</li>
 *     <li>json: same with {@code DataFormats.JSON}</li>
 *     <li>hocon: same with {@code DataFormats.HOCON}</li>
 *     <li>compound: {@code set(DataQuery, Object)} on a {@code CompoundTagDataContainer}
 *         (the view Sponge uses for custom data), compared to the input</li>
 * </ul>
 *
 * <p>Comparison is strict ({@link LosslessSupport#difference}): runtime class
 * must match, so Long 1 read back as Integer 1 is LOSSY.</p>
 *
 * <p>Statuses are pinned for api-17 (Minecraft 1.21.10). Compared with api-12: booleans in
 * CompoundTagDataView read back as Byte (X20), and mixed lists round-trip through NBT.</p>
 *
 * <p>The table is written to {@code lossless-matrix.md} in the test working
 * directory and the statuses are pinned below so a behavior change fails the test.</p>
 */
@DisplayName("Lossless matrix: in-memory, NBT and JSON round trips per value type")
class LosslessMatrixTest {

    enum Plain { ALPHA, BETA }

    record Row(String name, @Nullable Object value) {}

    static final String[] COLUMNS = {"primitive overload", "Object overload", "NBT round trip", "JSON round trip", "HOCON round trip", "CompoundTagDataView (extra)"};

    static List<Row> rows() {
        final List<Row> rows = new ArrayList<>();
        rows.add(new Row("Boolean true", true));
        rows.add(new Row("Boolean false", false));
        rows.add(new Row("Byte 0", (byte) 0));
        rows.add(new Row("Byte MIN", Byte.MIN_VALUE));
        rows.add(new Row("Byte MAX", Byte.MAX_VALUE));
        rows.add(new Row("Short MIN", Short.MIN_VALUE));
        rows.add(new Row("Short MAX", Short.MAX_VALUE));
        rows.add(new Row("Integer 0", 0));
        rows.add(new Row("Integer MIN", Integer.MIN_VALUE));
        rows.add(new Row("Integer MAX", Integer.MAX_VALUE));
        rows.add(new Row("Long 1 (fits int)", 1L));
        rows.add(new Row("Long MIN", Long.MIN_VALUE));
        rows.add(new Row("Long MAX", Long.MAX_VALUE));
        rows.add(new Row("Float 0.1", 0.1f));
        rows.add(new Row("Float 1.0", 1.0f));
        rows.add(new Row("Float NaN", Float.NaN));
        rows.add(new Row("Float +Inf", Float.POSITIVE_INFINITY));
        rows.add(new Row("Float -Inf", Float.NEGATIVE_INFINITY));
        rows.add(new Row("Float -0.0", -0.0f));
        rows.add(new Row("Float MIN_VALUE", Float.MIN_VALUE));
        rows.add(new Row("Float MAX_VALUE", Float.MAX_VALUE));
        rows.add(new Row("Double 0.1", 0.1d));
        rows.add(new Row("Double 1.0", 1.0d));
        rows.add(new Row("Double 1e20", 1e20d));
        rows.add(new Row("Double NaN", Double.NaN));
        rows.add(new Row("Double +Inf", Double.POSITIVE_INFINITY));
        rows.add(new Row("Double -Inf", Double.NEGATIVE_INFINITY));
        rows.add(new Row("Double -0.0", -0.0d));
        rows.add(new Row("Double MIN_VALUE", Double.MIN_VALUE));
        rows.add(new Row("Double MAX_VALUE", Double.MAX_VALUE));
        rows.add(new Row("String empty", ""));
        rows.add(new Row("String ascii", "hello"));
        rows.add(new Row("String unicode", "h\u00e9llo \u2713 \uD83D\uDE00 \u0000"));
        rows.add(new Row("String with '.'", "a.b.c"));
        rows.add(new Row("String 70k chars", "x".repeat(70_000)));
        rows.add(new Row("byte[] empty", new byte[0]));
        rows.add(new Row("byte[]", new byte[] {1, -1, Byte.MIN_VALUE, Byte.MAX_VALUE}));
        rows.add(new Row("int[]", new int[] {1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE}));
        rows.add(new Row("long[] small values", new long[] {1L, 2L}));
        rows.add(new Row("long[] large values", new long[] {Long.MIN_VALUE, Long.MAX_VALUE}));
        rows.add(new Row("short[] (extra)", new short[] {1, 2}));
        rows.add(new Row("double[] (extra)", new double[] {1.5, 2.5}));
        rows.add(new Row("String[] (extra)", new String[] {"a", "b"}));
        rows.add(new Row("List<Boolean>", List.of(true, false)));
        rows.add(new Row("List<Byte>", List.of((byte) 1, (byte) 2)));
        rows.add(new Row("List<Short>", List.of((short) 1, (short) 2)));
        rows.add(new Row("List<Integer>", List.of(1, 2)));
        rows.add(new Row("List<Long>", List.of(1L, Long.MAX_VALUE)));
        rows.add(new Row("List<Float>", List.of(1.5f, 2.5f)));
        rows.add(new Row("List<Double>", List.of(1.5d, 2.5d)));
        rows.add(new Row("List<String>", List.of("a", "b.c")));
        rows.add(new Row("List empty", List.of()));
        rows.add(new Row("List mixed [1, \"a\"] (extra)", Arrays.asList(1, "a")));
        rows.add(new Row("List<List<Integer>> (extra)", List.of(List.of(1), List.of(2, 3))));
        final Map<String, Object> stringMap = new LinkedHashMap<>();
        stringMap.put("x", 1);
        stringMap.put("y", "z");
        rows.add(new Row("Map<String,?>", stringMap));
        rows.add(new Row("Map<String,?> key with '.'", Map.of("a.b", 1)));
        rows.add(new Row("Map<Integer,?>", Map.of(1, "a")));
        rows.add(new Row("Map<Long,?>", Map.of(1L, "a")));
        rows.add(new Row("Map<UUID,?>", Map.of(new UUID(1, 2), "a")));
        rows.add(new Row("Map<Enum,?>", Map.of(Plain.ALPHA, "a")));
        rows.add(new Row("UUID", new UUID(0x0123456789abcdefL, 0xfedcba9876543210L)));
        rows.add(new Row("enum (plain, no translator)", Plain.BETA));
        rows.add(new Row("enum DayOfWeek (no translator)", DayOfWeek.MONDAY));
        rows.add(new Row("enum Month (has translator)", Month.MARCH));
        final DataContainer nested = DataContainer.createNew();
        nested.set(DataQuery.of("i"), 1).set(DataQuery.of("s"), "t").set(DataQuery.of("deep", "l"), 5L);
        rows.add(new Row("nested DataView", nested));
        rows.add(new Row("nested DataView with boolean (extra)", DataContainer.createNew().set(DataQuery.of("b"), true)));
        rows.add(new Row("Optional.empty() (extra)", Optional.empty()));
        rows.add(new Row("null", null));
        return rows;
    }

    /** Pinned observed statuses, in COLUMNS order: P = PASS, L = LOSSY, E = ERROR, - = N/A. */
    static final Map<String, String> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("Boolean true", "PPPPPL");
        EXPECTED.put("Boolean false", "PPPPPL");
        EXPECTED.put("Byte 0", "PPPLLP");
        EXPECTED.put("Byte MIN", "PPPLLP");
        EXPECTED.put("Byte MAX", "PPPLLP");
        EXPECTED.put("Short MIN", "PPPLLP");
        EXPECTED.put("Short MAX", "PPPLLP");
        EXPECTED.put("Integer 0", "PPPPPP");
        EXPECTED.put("Integer MIN", "PPPPPP");
        EXPECTED.put("Integer MAX", "PPPPPP");
        EXPECTED.put("Long 1 (fits int)", "PPPLLP");
        EXPECTED.put("Long MIN", "PPPPPP");
        EXPECTED.put("Long MAX", "PPPPPP");
        EXPECTED.put("Float 0.1", "PPPLLP");
        EXPECTED.put("Float 1.0", "PPPLLP");
        EXPECTED.put("Float NaN", "PPPELP");
        EXPECTED.put("Float +Inf", "PPPELP");
        EXPECTED.put("Float -Inf", "PPPELP");
        EXPECTED.put("Float -0.0", "PPLLLL");
        EXPECTED.put("Float MIN_VALUE", "PPPLLP");
        EXPECTED.put("Float MAX_VALUE", "PPPLLP");
        EXPECTED.put("Double 0.1", "PPPPPP");
        EXPECTED.put("Double 1.0", "PPPPLP");
        EXPECTED.put("Double 1e20", "PPPPPP");
        EXPECTED.put("Double NaN", "PPPELP");
        EXPECTED.put("Double +Inf", "PPPELP");
        EXPECTED.put("Double -Inf", "PPPELP");
        EXPECTED.put("Double -0.0", "PPLPLL");
        EXPECTED.put("Double MIN_VALUE", "PPPPPP");
        EXPECTED.put("Double MAX_VALUE", "PPPPPP");
        EXPECTED.put("String empty", "PPPPPP");
        EXPECTED.put("String ascii", "PPPPPP");
        EXPECTED.put("String unicode", "PPPPPP");
        EXPECTED.put("String with '.'", "PPPPPP");
        EXPECTED.put("String 70k chars", "PPLPPP");
        EXPECTED.put("byte[] empty", "-PPPLP");
        EXPECTED.put("byte[]", "-PPELP");
        EXPECTED.put("int[]", "-PPPLP");
        EXPECTED.put("long[] small values", "-PPELP");
        EXPECTED.put("long[] large values", "-PPPLP");
        EXPECTED.put("short[] (extra)", "-PEELE");
        EXPECTED.put("double[] (extra)", "-PEELE");
        EXPECTED.put("String[] (extra)", "-PEELE");
        EXPECTED.put("List<Boolean>", "-PLPPL");
        EXPECTED.put("List<Byte>", "-PPLLP");
        EXPECTED.put("List<Short>", "-PPLLP");
        EXPECTED.put("List<Integer>", "-PPPPP");
        EXPECTED.put("List<Long>", "-PPLLP");
        EXPECTED.put("List<Float>", "-PPLLP");
        EXPECTED.put("List<Double>", "-PPPPP");
        EXPECTED.put("List<String>", "-PPPPP");
        EXPECTED.put("List empty", "-PPPPP");
        EXPECTED.put("List mixed [1, \"a\"] (extra)", "-PPPPP");
        EXPECTED.put("List<List<Integer>> (extra)", "-PPPPP");
        EXPECTED.put("Map<String,?>", "-LPPPL");
        EXPECTED.put("Map<String,?> key with '.'", "-LPPPL");
        EXPECTED.put("Map<Integer,?>", "-LPPPL");
        EXPECTED.put("Map<Long,?>", "-E---E");
        EXPECTED.put("Map<UUID,?>", "-LPPPL");
        EXPECTED.put("Map<Enum,?>", "-LPPPL");
        EXPECTED.put("UUID", "-LPPPL");
        EXPECTED.put("enum (plain, no translator)", "-PEELE");
        EXPECTED.put("enum DayOfWeek (no translator)", "-PEELE");
        EXPECTED.put("enum Month (has translator)", "-LPPPL");
        EXPECTED.put("nested DataView", "-PPLLP");
        EXPECTED.put("nested DataView with boolean (extra)", "-PPPPL");
        EXPECTED.put("Optional.empty() (extra)", "-PEEEE");
        EXPECTED.put("null", "EE---E");
    }

    static String status(final String cell) {
        if (cell.startsWith("PASS")) {
            return "P";
        }
        if (cell.startsWith("LOSSY")) {
            return "L";
        }
        if (cell.startsWith("ERROR")) {
            return "E";
        }
        return "-";
    }

    static String error(final Throwable t) {
        final String msg = String.valueOf(t.getMessage());
        return "ERROR: " + t.getClass().getSimpleName() + ": " + (msg.length() > 70 ? msg.substring(0, 70) + "..." : msg);
    }

    static String compare(final @Nullable Object expected, final Optional<Object> actual) {
        if (actual.isEmpty()) {
            return "LOSSY: value absent after read";
        }
        final String d = LosslessSupport.difference(expected, actual.get());
        return d == null ? "PASS" : "LOSSY: " + d;
    }

    static boolean setPrimitive(final DataView view, final Object v) {
        switch (v) {
            case Boolean b -> view.set(KEY, b.booleanValue());
            case Byte b -> view.set(KEY, b.byteValue());
            case Short s -> view.set(KEY, s.shortValue());
            case Integer i -> view.set(KEY, i.intValue());
            case Long l -> view.set(KEY, l.longValue());
            case Float f -> view.set(KEY, f.floatValue());
            case Double d -> view.set(KEY, d.doubleValue());
            case String s -> view.set(KEY, s);
            default -> {
                return false;
            }
        }
        return true;
    }

    interface Cell {
        String run() throws Exception;
    }

    static String cell(final Cell c) {
        try {
            return c.run();
        } catch (final Throwable t) {
            return LosslessMatrixTest.error(t);
        }
    }

    static String[] evaluate(final Row row) {
        final Object v = row.value();
        final String primitive = LosslessMatrixTest.cell(() -> {
            if (v == null) {
                final DataContainer c = LosslessSupport.memory();
                c.set(KEY, (String) null);
                return LosslessMatrixTest.compare(null, c.get(KEY));
            }
            final DataContainer c = LosslessSupport.memory();
            if (!LosslessMatrixTest.setPrimitive(c, v)) {
                return "N/A";
            }
            return LosslessMatrixTest.compare(v, c.get(KEY));
        });
        final String[] stored = new String[1];
        final DataContainer objectContainer = LosslessSupport.memory();
        final String object = LosslessMatrixTest.cell(() -> {
            objectContainer.set(KEY, v);
            stored[0] = "ok";
            return LosslessMatrixTest.compare(v, objectContainer.get(KEY));
        });
        final String nbt;
        final String json;
        final String hocon;
        if (stored[0] == null) {
            nbt = "N/A (value cannot be stored)";
            json = "N/A (value cannot be stored)";
            hocon = "N/A (value cannot be stored)";
        } else {
            final Object inView = objectContainer.get(KEY).get();
            nbt = LosslessMatrixTest.cell(() -> LosslessMatrixTest.compare(inView, LosslessSupport.nbtRoundTrip(objectContainer).get(KEY)));
            json = LosslessMatrixTest.cell(() -> LosslessMatrixTest.compare(inView, LosslessSupport.jsonRoundTrip(objectContainer).get(KEY)));
            hocon = LosslessMatrixTest.cell(() -> LosslessMatrixTest.compare(inView, LosslessSupport.hoconRoundTrip(objectContainer).get(KEY)));
        }
        final String compound = LosslessMatrixTest.cell(() -> {
            final DataContainer c = LosslessSupport.compound();
            c.set(KEY, v);
            return LosslessMatrixTest.compare(v, c.get(KEY));
        });
        return new String[] {primitive, object, nbt, json, hocon, compound};
    }

    @Test
    @DisplayName("matrix matches pinned statuses")
    void matrix() throws IOException {
        final PinnedTable table = new PinnedTable("lossless-matrix", LosslessMatrixTest.COLUMNS);
        for (final Row row : LosslessMatrixTest.rows()) {
            table.row(row.name(), LosslessMatrixTest.evaluate(row));
        }
        table.assertMatches(LosslessMatrixTest.EXPECTED);
    }
}
