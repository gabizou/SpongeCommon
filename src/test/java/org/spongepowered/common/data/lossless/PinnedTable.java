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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A result table whose cell statuses are pinned, like {@link LosslessMatrixTest}: each cell is
 * "PASS...", "LOSSY: ...", "ERROR: ..." or "N/A...". The table is written as markdown to the test
 * working directory, and on a mismatch the observed statuses are printed as Java source to re-pin.
 */
final class PinnedTable {

    private final String name;
    private final String[] columns;
    private final StringBuilder md = new StringBuilder();
    private final Map<String, String> observed = new LinkedHashMap<>();

    PinnedTable(final String name, final String... columns) {
        this.name = name;
        this.columns = columns;
        this.md.append("| Value | ").append(String.join(" | ", columns)).append(" |\n|---|").append("---|".repeat(columns.length)).append('\n');
    }

    void row(final String row, final String... cells) {
        if (cells.length != this.columns.length) {
            throw new IllegalArgumentException(row + ": " + cells.length + " cells for " + this.columns.length + " columns");
        }
        this.md.append("| ").append(PinnedTable.escape(row));
        final StringBuilder codes = new StringBuilder();
        for (final String cell : cells) {
            this.md.append(" | ").append(PinnedTable.escape(cell));
            codes.append(LosslessMatrixTest.status(cell));
        }
        this.md.append(" |\n");
        this.observed.put(row, codes.toString());
    }

    void assertMatches(final Map<String, String> expected) throws IOException {
        final Path out = Path.of(this.name + ".md").toAbsolutePath();
        Files.writeString(out, this.md.toString(), StandardCharsets.UTF_8);
        System.out.println(this.name + " written to " + out + "\n" + this.md);
        if (!expected.equals(this.observed)) {
            final StringBuilder java = new StringBuilder("Observed statuses for " + this.name + ":\n");
            this.observed.forEach((k, v) -> java.append("        EXPECTED.put(\"").append(k.replace("\"", "\\\"")).append("\", \"").append(v).append("\");\n"));
            System.out.println(java);
        }
        assertEquals(expected, this.observed);
    }

    static String escape(final String s) {
        return s.replace("|", "\\|").replace("\u0000", "\\0").replace("\n", " ");
    }
}
