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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.util.Direction;
import org.spongepowered.math.imaginary.Complexd;
import org.spongepowered.math.imaginary.Complexf;
import org.spongepowered.math.imaginary.Quaterniond;
import org.spongepowered.math.imaginary.Quaternionf;
import org.spongepowered.math.vector.Vector2d;
import org.spongepowered.math.vector.Vector2f;
import org.spongepowered.math.vector.Vector2i;
import org.spongepowered.math.vector.Vector2l;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3f;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.math.vector.Vector3l;
import org.spongepowered.math.vector.Vector4d;
import org.spongepowered.math.vector.Vector4f;
import org.spongepowered.math.vector.Vector4i;
import org.spongepowered.math.vector.Vector4l;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Values that {@code DataSerializer} hands to a registered {@code DataTranslator}: store with
 * {@code set(DataQuery, Object)}, optionally save through a data format, then read back with
 * {@code getObject(DataQuery, Class)} and compare with {@code equals}.
 *
 * <ul>
 *     <li>stored as: what {@code get} returns after {@code set}; "DataView" means a translator ran</li>
 *     <li>memory / NBT / JSON / HOCON: {@code getObject} on the in-memory container, or on the container read
 *         back after writing it with that format</li>
 *     <li>CompoundTagDataView: {@code set} and {@code getObject} on the view used for custom data</li>
 * </ul>
 */
@DisplayName("Translators: DataSerializer dispatch and getObject round trips per format")
class TranslatorRoundTripTest {

    record Row(String name, Object value, Class<?> type) {}

    static List<Row> rows() {
        final List<Row> rows = new ArrayList<>();
        rows.add(new Row("UUID", new UUID(0x0123456789abcdefL, 0xfedcba9876543210L), UUID.class));
        rows.add(new Row("Vector2d", new Vector2d(0.1, -2.5), Vector2d.class));
        rows.add(new Row("Vector2f", new Vector2f(0.1f, -2.5f), Vector2f.class));
        rows.add(new Row("Vector2i", new Vector2i(1, Integer.MIN_VALUE), Vector2i.class));
        rows.add(new Row("Vector2l", new Vector2l(1L, Long.MAX_VALUE), Vector2l.class));
        rows.add(new Row("Vector3d", new Vector3d(0.1, -2.5, 1e20), Vector3d.class));
        rows.add(new Row("Vector3d -0.0", new Vector3d(-0.0, 0, 0), Vector3d.class));
        rows.add(new Row("Vector3f", new Vector3f(0.1f, -2.5f, 3f), Vector3f.class));
        rows.add(new Row("Vector3i", new Vector3i(1, -64, 30_000_000), Vector3i.class));
        rows.add(new Row("Vector3l small values", new Vector3l(1L, 2L, 3L), Vector3l.class));
        rows.add(new Row("Vector3l large values", new Vector3l(Long.MIN_VALUE, 0L, Long.MAX_VALUE), Vector3l.class));
        rows.add(new Row("Vector4d", new Vector4d(0.1, 0.2, 0.3, 0.4), Vector4d.class));
        rows.add(new Row("Vector4f", new Vector4f(0.1f, 0.2f, 0.3f, 0.4f), Vector4f.class));
        rows.add(new Row("Vector4i", new Vector4i(1, 2, 3, 4), Vector4i.class));
        rows.add(new Row("Vector4l", new Vector4l(1L, 2L, 3L, Long.MAX_VALUE), Vector4l.class));
        rows.add(new Row("Complexd", new Complexd(0.6, 0.8), Complexd.class));
        rows.add(new Row("Complexf", new Complexf(0.6f, 0.8f), Complexf.class));
        rows.add(new Row("Quaterniond", new Quaterniond(0.1, 0.2, 0.3, 0.9), Quaterniond.class));
        rows.add(new Row("Quaternionf", new Quaternionf(0.1f, 0.2f, 0.3f, 0.9f), Quaternionf.class));
        rows.add(new Row("LocalTime", LocalTime.of(13, 37, 42, 123_456_789), LocalTime.class));
        rows.add(new Row("LocalDate", LocalDate.of(2026, 10, 21), LocalDate.class));
        rows.add(new Row("LocalDateTime", LocalDateTime.of(2026, 10, 21, 13, 37, 42, 123_456_789), LocalDateTime.class));
        rows.add(new Row("Instant", Instant.ofEpochSecond(1_792_000_000L, 123_456_789), Instant.class));
        rows.add(new Row("ZonedDateTime offset", ZonedDateTime.of(2026, 10, 21, 13, 37, 42, 5, ZoneOffset.ofHours(2)), ZonedDateTime.class));
        rows.add(new Row("ZonedDateTime region", ZonedDateTime.of(2026, 10, 21, 13, 37, 42, 5, ZoneId.of("Europe/Berlin")), ZonedDateTime.class));
        rows.add(new Row("Month", Month.MARCH, Month.class));
        rows.add(new Row("Direction NORTH", Direction.NORTH, Direction.class));
        rows.add(new Row("Direction SOUTH_SOUTHWEST", Direction.SOUTH_SOUTHWEST, Direction.class));
        rows.add(new Row("Component text", Component.text("hello", NamedTextColor.RED).decorate(TextDecoration.BOLD), Component.class));
        rows.add(new Row("Component translatable", Component.translatable("block.minecraft.stone"), Component.class));
        rows.add(new Row("Component empty", Component.empty(), Component.class));
        return rows;
    }

    static final String[] COLUMNS = {"stored as", "memory", "NBT", "JSON", "HOCON", "CompoundTagDataView"};

    static final Map<String, String> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("UUID", "PPPPPP");
        EXPECTED.put("Vector2d", "PPPPPP");
        EXPECTED.put("Vector2f", "PPPPPP");
        EXPECTED.put("Vector2i", "PPPPPP");
        EXPECTED.put("Vector2l", "PPPPPP");
        EXPECTED.put("Vector3d", "PPPPPP");
        EXPECTED.put("Vector3d -0.0", "PPLPLL");
        EXPECTED.put("Vector3f", "PPPPPP");
        EXPECTED.put("Vector3i", "PPPPPP");
        EXPECTED.put("Vector3l small values", "PPPPPP");
        EXPECTED.put("Vector3l large values", "PPPPPP");
        EXPECTED.put("Vector4d", "PPPPPP");
        EXPECTED.put("Vector4f", "PPPPPP");
        EXPECTED.put("Vector4i", "PPPPPP");
        EXPECTED.put("Vector4l", "PPPPPP");
        EXPECTED.put("Complexd", "PPPPPP");
        EXPECTED.put("Complexf", "PPPPPP");
        EXPECTED.put("Quaterniond", "PPPPPP");
        EXPECTED.put("Quaternionf", "PPPPPP");
        EXPECTED.put("LocalTime", "PPPPPP");
        EXPECTED.put("LocalDate", "PPPPPP");
        EXPECTED.put("LocalDateTime", "PPPPPP");
        EXPECTED.put("Instant", "PPPPPP");
        EXPECTED.put("ZonedDateTime offset", "PEEEEE");
        EXPECTED.put("ZonedDateTime region", "PPPPPP");
        EXPECTED.put("Month", "PPPPPP");
        EXPECTED.put("Direction NORTH", "PPPPPP");
        EXPECTED.put("Direction SOUTH_SOUTHWEST", "PPPPPP");
        EXPECTED.put("Component text", "LLEEEE");
        EXPECTED.put("Component translatable", "LLEEEE");
        EXPECTED.put("Component empty", "LLEEEE");
    }

    static String compare(final Object expected, final Optional<?> actual) {
        if (actual.isEmpty()) {
            return "LOSSY: absent after read";
        }
        return Objects.equals(expected, actual.get()) ? "PASS" : "LOSSY: " + expected + " -> " + actual.get();
    }

    static String storedAs(final Object value) {
        return LosslessMatrixTest.cell(() -> {
            final DataContainer c = LosslessSupport.memory();
            c.set(KEY, value);
            final Object stored = c.get(KEY).get();
            final boolean viaTranslator = stored instanceof DataView;
            final String what = viaTranslator ? "DataView " + ((DataView) stored).keys(false) : "raw " + stored.getClass().getSimpleName();
            final String translator = Sponge.dataManager().translator(value.getClass()).isPresent() ? "" : " (no translator for " + value.getClass().getSimpleName() + ")";
            return (viaTranslator ? "PASS: " : "LOSSY: ") + what + translator;
        });
    }

    static String[] evaluate(final Row row) {
        final String stored = TranslatorRoundTripTest.storedAs(row.value());
        final DataContainer memory = LosslessSupport.memory();
        final String mem = LosslessMatrixTest.cell(() -> {
            memory.set(KEY, row.value());
            return TranslatorRoundTripTest.compare(row.value(), memory.getObject(KEY, row.type()));
        });
        final List<String> cells = new ArrayList<>();
        cells.add(stored);
        cells.add(mem);
        for (final LosslessSupport.Format format : LosslessSupport.Format.values()) {
            cells.add(LosslessMatrixTest.cell(() -> TranslatorRoundTripTest.compare(row.value(), format.roundTrip(memory).getObject(KEY, row.type()))));
        }
        cells.add(LosslessMatrixTest.cell(() -> {
            final DataContainer c = LosslessSupport.compound();
            c.set(KEY, row.value());
            return TranslatorRoundTripTest.compare(row.value(), c.getObject(KEY, row.type()));
        }));
        return cells.toArray(String[]::new);
    }

    @Test
    @DisplayName("translator table matches pinned statuses")
    void translators() throws IOException {
        final PinnedTable table = new PinnedTable("lossless-translators", TranslatorRoundTripTest.COLUMNS);
        for (final Row row : TranslatorRoundTripTest.rows()) {
            table.row(row.name(), TranslatorRoundTripTest.evaluate(row));
        }
        table.assertMatches(TranslatorRoundTripTest.EXPECTED);
    }
}
