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

import net.minecraft.nbt.CompoundTag;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.common.data.CompoundTagDataContainer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared helpers for the lossless reproducers: strict structural comparison,
 * value descriptions for reports, and translator round trips.
 */
final class LosslessSupport {

    static final DataQuery KEY = DataQuery.of("v");

    private LosslessSupport() {
    }

    static DataContainer memory() {
        return DataContainer.createNew();
    }

    static DataContainer memory(final DataView.SafetyMode safety) {
        return DataContainer.createNew(safety);
    }

    static DataContainer compound() {
        return new CompoundTagDataContainer(new CompoundTag());
    }

    /** Writes the view with {@link DataFormats#NBT} to bytes and reads it back. */
    static DataContainer nbtRoundTrip(final DataView view) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataFormats.NBT.get().writeTo(out, view);
        return DataFormats.NBT.get().readFrom(new ByteArrayInputStream(out.toByteArray()));
    }

    /** Writes the view with {@link DataFormats#JSON} to a string and reads it back. */
    static DataContainer jsonRoundTrip(final DataView view) throws IOException {
        return DataFormats.JSON.get().read(DataFormats.JSON.get().write(view));
    }

    /** Writes the view with {@link DataFormats#HOCON} to a string and reads it back. */
    static DataContainer hoconRoundTrip(final DataView view) throws IOException {
        return DataFormats.HOCON.get().read(DataFormats.HOCON.get().write(view));
    }

    /** The data formats a plugin can save through, in report column order. */
    enum Format {
        NBT,
        JSON,
        HOCON;

        DataContainer roundTrip(final DataView view) throws IOException {
            return switch (this) {
                case NBT -> LosslessSupport.nbtRoundTrip(view);
                case JSON -> LosslessSupport.jsonRoundTrip(view);
                case HOCON -> LosslessSupport.hoconRoundTrip(view);
            };
        }
    }

    static String json(final DataView view) throws IOException {
        return DataFormats.JSON.get().write(view);
    }

    /**
     * Strict comparison: same runtime class for scalars and arrays, element-wise
     * for lists, key-wise for views. Double/Float use boxed equals, so NaN equals
     * NaN and -0.0 differs from 0.0. A Map and a DataView are never "same".
     */
    static boolean same(final @Nullable Object expected, final @Nullable Object actual) {
        return LosslessSupport.difference(expected, actual) == null;
    }

    /** Returns null when {@link #same} holds, otherwise a short description of the first difference. */
    static @Nullable String difference(final @Nullable Object expected, final @Nullable Object actual) {
        if (expected == null || actual == null) {
            return expected == actual ? null : LosslessSupport.describe(expected) + " -> " + LosslessSupport.describe(actual);
        }
        if (expected instanceof DataView ev) {
            if (!(actual instanceof DataView av)) {
                return "DataView -> " + LosslessSupport.describe(actual);
            }
            final Set<String> ek = ev.keys(false).stream().map(DataQuery::toString).collect(Collectors.toSet());
            final Set<String> ak = av.keys(false).stream().map(DataQuery::toString).collect(Collectors.toSet());
            if (!ek.equals(ak)) {
                return "keys " + ek + " -> " + ak;
            }
            for (final DataQuery k : ev.keys(false)) {
                final String d = LosslessSupport.difference(ev.get(k).orElse(null), av.get(k).orElse(null));
                if (d != null) {
                    return k + ": " + d;
                }
            }
            return null;
        }
        if (expected instanceof Map<?, ?> em) {
            if (!(actual instanceof Map<?, ?> am)) {
                return "Map -> " + LosslessSupport.describe(actual);
            }
            if (!em.keySet().equals(am.keySet())) {
                return "keys " + em.keySet() + " -> " + am.keySet();
            }
            for (final Map.Entry<?, ?> e : em.entrySet()) {
                final String d = LosslessSupport.difference(e.getValue(), am.get(e.getKey()));
                if (d != null) {
                    return e.getKey() + ": " + d;
                }
            }
            return null;
        }
        if (expected instanceof List<?> el) {
            if (!(actual instanceof List<?> al)) {
                return "List -> " + LosslessSupport.describe(actual);
            }
            if (el.size() != al.size()) {
                return "size " + el.size() + " -> " + al.size();
            }
            final Iterator<?> ei = el.iterator();
            final Iterator<?> ai = al.iterator();
            int i = 0;
            while (ei.hasNext()) {
                final String d = LosslessSupport.difference(ei.next(), ai.next());
                if (d != null) {
                    return "[" + i + "] " + d;
                }
                i++;
            }
            return null;
        }
        if (expected.getClass() != actual.getClass()) {
            return LosslessSupport.describe(expected) + " -> " + LosslessSupport.describe(actual);
        }
        if (expected.getClass().isArray()) {
            final int len = Array.getLength(expected);
            if (len != Array.getLength(actual)) {
                return "length " + len + " -> " + Array.getLength(actual);
            }
            for (int i = 0; i < len; i++) {
                final String d = LosslessSupport.difference(Array.get(expected, i), Array.get(actual, i));
                if (d != null) {
                    return "[" + i + "] " + d;
                }
            }
            return null;
        }
        return expected.equals(actual) ? null : LosslessSupport.describe(expected) + " -> " + LosslessSupport.describe(actual);
    }

    static String describe(final @Nullable Object o) {
        if (o == null) {
            return "null";
        }
        if (o instanceof Optional<?> opt && opt.isEmpty()) {
            return "Optional.empty";
        }
        final String type = o instanceof DataView ? "DataView" : o instanceof List ? "List" : o instanceof Map ? "Map" : o.getClass().getSimpleName();
        final String value;
        if (o.getClass().isArray()) {
            final StringBuilder b = new StringBuilder("[");
            for (int i = 0; i < Math.min(Array.getLength(o), 4); i++) {
                b.append(i == 0 ? "" : ",").append(Array.get(o, i));
            }
            value = b.append(Array.getLength(o) > 4 ? ",...]" : "]").toString();
        } else if (o instanceof DataView v) {
            value = v.keys(false).toString();
        } else if (o instanceof List<?> l) {
            value = l.isEmpty() ? "[]" : "[" + LosslessSupport.describe(l.getFirst()) + (l.size() > 1 ? ",..." : "") + "]";
        } else {
            final String s = String.valueOf(o);
            value = s.length() > 24 ? s.substring(0, 24) + "...(" + s.length() + ")" : s;
        }
        return type + "(" + value + ")";
    }
}
