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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Backwards compatibility: data saved by unmodified api-17 must still read back the same, or better.
 *
 * <p>{@link #write()} runs only when {@code SPONGE_LOSSLESS_FIXTURES_OUT} names a directory. Run on
 * unmodified api-17 code, it saves a corpus through NBT, JSON and HOCON ({@code corpus.nbt},
 * {@code corpus.json}, {@code corpus.conf}) and records, per entry, the result that code got when reading
 * the file back ({@code expected-<ext>.tsv}: file, storage key, entry, result). An entry the format can
 * write but api-17 cannot read back goes to its own {@code single-<key>.<ext>}, since in the shared corpus
 * it would make the whole file unreadable (X7). An entry the format cannot write is left out. The files are committed under
 * {@code src/test/resources/org/spongepowered/common/data/lossless/api-17/}.</p>
 *
 * <p>{@link #read()} reads those committed files with the current code. Every entry must give the
 * recorded result or {@code PASS}; anything else is a regression for already saved data.</p>
 *
 * <p>Corpus groups: {@code matrix} (each {@link LosslessMatrixTest} value the format could write),
 * {@code translators} ({@link TranslatorRoundTripTest}), {@code serializables}
 * ({@link SerializableRoundTripTest}, including {@link PluginRecord} at content version 1 and 2), and
 * {@code customData}: an ItemStack per {@link CustomDataRoundTripTest} key, carrying that key.</p>
 */
@DisplayName("api-17 fixtures: data saved by unmodified api-17 reads back the same or better")
class Api17FixtureTest {

    static final String RESOURCES = "/org/spongepowered/common/data/lossless/api-17/";
    static final String ENV = "SPONGE_LOSSLESS_FIXTURES_OUT";

    record Entry(String id, DataContainer stored, Function<DataView, String> check) {}

    /** One entry per value; {@code check} turns the read-back entry into a matrix-style cell. */
    static Map<String, Entry> entries() {
        PluginRecord.register();
        final Map<String, Entry> entries = new LinkedHashMap<>();
        int i = 0;
        for (final LosslessMatrixTest.Row row : LosslessMatrixTest.rows()) {
            final String id = String.format("matrix/%02d %s", i++, row.name());
            final DataContainer c = DataContainer.createNew();
            try {
                c.set(LosslessSupport.KEY, row.value());
            } catch (final RuntimeException e) {
                continue;
            }
            final Object inView = c.get(LosslessSupport.KEY).get();
            entries.put(id, new Entry(id, c, v -> LosslessMatrixTest.compare(inView, v.get(LosslessSupport.KEY))));
        }
        for (final TranslatorRoundTripTest.Row row : TranslatorRoundTripTest.rows()) {
            final String id = "translators/" + row.name();
            final DataContainer c = DataContainer.createNew().set(LosslessSupport.KEY, row.value());
            entries.put(id, new Entry(id, c, v -> TranslatorRoundTripTest.compare(row.value(), v.getObject(LosslessSupport.KEY, row.type()))));
        }
        for (final SerializableRoundTripTest.Row row : SerializableRoundTripTest.rows()) {
            final String id = "serializables/" + row.name();
            final DataContainer original;
            try {
                original = row.value().get().toContainer();
            } catch (final RuntimeException e) {
                continue;
            }
            entries.put(id, new Entry(id, original, v -> SerializableRoundTripTest.rebuild(row.type(), original, v.copy())));
        }
        entries.put("serializables/PluginRecord version 1", new Entry("serializables/PluginRecord version 1",
            PluginRecord.sample().toVersion1Container(), v -> SerializableRoundTripTest.record(v.copy())));
        for (final CustomDataRoundTripTest.Row<?> row : CustomDataRoundTripTest.rows()) {
            final String id = "customData/" + row.name();
            final ItemStack stack = ItemStack.of(ItemTypes.STONE);
            if (!Api17FixtureTest.offer(stack, row)) {
                continue;
            }
            final DataContainer stackContainer;
            try {
                stackContainer = stack.toContainer();
            } catch (final RuntimeException e) {
                continue;
            }
            entries.put(id, new Entry(id, stackContainer, v -> {
                return CustomDataRoundTripTest.compare(row.value(), CustomDataRoundTripTest.deserialize(v).get(row.key()));
            }));
        }
        return entries;
    }

    /** False when api-17 cannot store the value at all; {@link CustomDataRoundTripTest} records that case. */
    private static <T> boolean offer(final ItemStack stack, final CustomDataRoundTripTest.Row<T> row) {
        try {
            return stack.offer(row.key(), row.value()).isSuccessful();
        } catch (final RuntimeException e) {
            return false;
        }
    }

    static byte[] write(final LosslessSupport.Format format, final DataView view) throws IOException {
        return switch (format) {
            case NBT -> {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataFormats.NBT.get().writeTo(out, view);
                yield out.toByteArray();
            }
            case JSON -> DataFormats.JSON.get().write(view).getBytes(StandardCharsets.UTF_8);
            case HOCON -> DataFormats.HOCON.get().write(view).getBytes(StandardCharsets.UTF_8);
        };
    }

    static DataContainer read(final LosslessSupport.Format format, final byte[] bytes) throws IOException {
        return switch (format) {
            case NBT -> DataFormats.NBT.get().readFrom(new ByteArrayInputStream(bytes));
            case JSON -> DataFormats.JSON.get().read(new String(bytes, StandardCharsets.UTF_8));
            case HOCON -> DataFormats.HOCON.get().read(new String(bytes, StandardCharsets.UTF_8));
        };
    }

    static String extension(final LosslessSupport.Format format) {
        return switch (format) {
            case NBT -> "nbt";
            case JSON -> "json";
            case HOCON -> "conf";
        };
    }

    static byte[] resource(final String file) throws IOException {
        try (InputStream in = Api17FixtureTest.class.getResourceAsStream(Api17FixtureTest.RESOURCES + file)) {
            assertNotNull(in, "missing fixture " + file);
            return in.readAllBytes();
        }
    }

    /** Storage key for the n-th entry; plain, so no format splits or escapes it. */
    static DataQuery query(final int n) {
        return DataQuery.of(String.format("e%03d", n));
    }

    static String escape(final String s) {
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n");
    }

    static String unescape(final String s) {
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                final char n = s.charAt(++i);
                b.append(n == 't' ? '\t' : n == 'n' ? '\n' : n);
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    @Test
    @DisplayName("write fixtures (only with " + Api17FixtureTest.ENV + ")")
    void write() throws IOException {
        final @Nullable String out = System.getenv(Api17FixtureTest.ENV);
        Assumptions.assumeTrue(out != null && !out.isEmpty(), Api17FixtureTest.ENV + " not set");
        final Path dir = Path.of(out);
        Files.createDirectories(dir);
        final Map<String, Entry> entries = Api17FixtureTest.entries();
        for (final LosslessSupport.Format format : LosslessSupport.Format.values()) {
            final String ext = Api17FixtureTest.extension(format);
            final DataContainer corpus = DataContainer.createNew();
            final Map<DataQuery, Entry> inCorpus = new LinkedHashMap<>();
            final StringBuilder expected = new StringBuilder();
            int n = 0;
            for (final Entry entry : entries.values()) {
                final DataQuery key = Api17FixtureTest.query(n++);
                final DataContainer single = DataContainer.createNew().set(key, entry.stored());
                final byte[] bytes;
                try {
                    bytes = Api17FixtureTest.write(format, single);
                } catch (final Exception e) {
                    // api-17 cannot save it with this format at all, so there is nothing saved to stay compatible with
                    continue;
                }
                try {
                    Api17FixtureTest.read(format, bytes);
                } catch (final Exception e) {
                    // Saved, but api-17 cannot read it back: in the shared corpus it would make every entry unreadable
                    final String file = "single-" + key + "." + ext;
                    Files.write(dir.resolve(file), bytes);
                    final String cell = LosslessMatrixTest.cell(() -> entry.check().apply(
                        Api17FixtureTest.read(format, bytes).getView(key).orElseThrow()));
                    Api17FixtureTest.line(expected, file, key, entry, cell);
                    continue;
                }
                corpus.set(key, entry.stored());
                inCorpus.put(key, entry);
            }
            final String file = "corpus." + ext;
            final byte[] bytes = Api17FixtureTest.write(format, corpus);
            Files.write(dir.resolve(file), bytes);
            final DataContainer read = Api17FixtureTest.read(format, bytes);
            inCorpus.forEach((key, entry) -> Api17FixtureTest.line(expected, file, key, entry,
                LosslessMatrixTest.cell(() -> entry.check().apply(read.getView(key).orElseThrow()))));
            Files.writeString(dir.resolve("expected-" + ext + ".tsv"), expected.toString(), StandardCharsets.UTF_8);
        }
    }

    private static void line(final StringBuilder out, final String file, final DataQuery key, final Entry entry, final String cell) {
        out.append(file).append('\t').append(key).append('\t').append(Api17FixtureTest.escape(entry.id()))
            .append('\t').append(Api17FixtureTest.escape(cell)).append('\n');
    }

    @Test
    @DisplayName("read fixtures saved by unmodified api-17")
    void read() throws IOException {
        final Map<String, Entry> entries = Api17FixtureTest.entries();
        final Map<String, String> regressions = new LinkedHashMap<>();
        final StringBuilder report = new StringBuilder();
        for (final LosslessSupport.Format format : LosslessSupport.Format.values()) {
            final String ext = Api17FixtureTest.extension(format);
            final String expected = new String(Api17FixtureTest.resource("expected-" + ext + ".tsv"), StandardCharsets.UTF_8);
            final Map<String, DataContainer> files = new LinkedHashMap<>();
            for (final String line : expected.split("\n")) {
                if (line.isEmpty()) {
                    continue;
                }
                final String[] parts = line.split("\t", 4);
                final String file = parts[0];
                final DataQuery key = DataQuery.of(parts[1]);
                final String id = Api17FixtureTest.unescape(parts[2]);
                final String api17 = Api17FixtureTest.unescape(parts[3]);
                final @Nullable Entry entry = entries.get(id);
                assertNotNull(entry, "fixture entry no longer generated: " + id);
                final String now = LosslessMatrixTest.cell(() -> {
                    final DataContainer read = files.containsKey(file) ? files.get(file) : Api17FixtureTest.read(format, Api17FixtureTest.resource(file));
                    files.put(file, read);
                    return entry.check().apply(read.getView(key).orElseThrow());
                });
                final boolean ok = now.equals(api17) || now.startsWith("PASS");
                report.append(format).append(" | ").append(PinnedTable.escape(id)).append(" | ").append(PinnedTable.escape(api17))
                    .append(" | ").append(PinnedTable.escape(now)).append(" | ").append(ok ? (now.equals(api17) ? "same" : "better") : "REGRESSION").append('\n');
                if (!ok) {
                    regressions.put(format + " " + id, api17 + " => " + now);
                }
            }
        }
        final Path out = Path.of("lossless-api17-fixtures.md").toAbsolutePath();
        Files.writeString(out, "| Format | Entry | api-17 read | now | |\n|---|---|---|---|---|\n" + report, StandardCharsets.UTF_8);
        System.out.println("api-17 fixture report written to " + out + "\n" + report);
        assertEquals(Map.of(), regressions);
    }
}
