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

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataContentUpdater;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataSerializable;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.math.vector.Vector3d;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A DataSerializable written the way plugins commonly write one: typed getters in the builder,
 * translated values through {@code getObject}, a nested serializable, and content version 2 with an
 * updater from version 1, which stored the name under {@code OldName}.
 */
record PluginRecord(boolean flag, byte b, short s, int i, long small, long big, float f, double d, String name,
                    UUID id, Vector3d pos, Instant when, List<Boolean> flags, List<String> tags,
                    Map<String, Integer> counts, ItemStackSnapshot item) implements DataSerializable {

    static final int VERSION = 2;
    static final DataQuery FLAG = DataQuery.of("Flag");
    static final DataQuery B = DataQuery.of("B");
    static final DataQuery S = DataQuery.of("S");
    static final DataQuery I = DataQuery.of("I");
    static final DataQuery SMALL = DataQuery.of("Small");
    static final DataQuery BIG = DataQuery.of("Big");
    static final DataQuery F = DataQuery.of("F");
    static final DataQuery D = DataQuery.of("D");
    static final DataQuery NAME = DataQuery.of("Name");
    static final DataQuery OLD_NAME = DataQuery.of("OldName");
    static final DataQuery ID = DataQuery.of("Id");
    static final DataQuery POS = DataQuery.of("Pos");
    static final DataQuery WHEN = DataQuery.of("When");
    static final DataQuery FLAGS = DataQuery.of("Flags");
    static final DataQuery TAGS = DataQuery.of("Tags");
    static final DataQuery COUNTS = DataQuery.of("Counts");
    static final DataQuery ITEM = DataQuery.of("Item");

    private static boolean registered;

    static PluginRecord sample() {
        final Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("a", 1);
        counts.put("b", 2);
        return new PluginRecord(true, (byte) -7, (short) 300, 42, 5L, Long.MAX_VALUE, 0.1f, 0.1d, "record",
            new UUID(3, 4), new Vector3d(1.5, 64, -3.25), Instant.ofEpochSecond(1_792_000_000L, 5),
            List.of(true, false, true), List.of("x", "y.z"), counts, ItemStack.of(ItemTypes.APPLE, 3).asImmutable());
    }

    static synchronized void register() {
        if (PluginRecord.registered) {
            return;
        }
        PluginRecord.registered = true;
        Sponge.dataManager().registerBuilder(PluginRecord.class, new Builder());
        Sponge.dataManager().registerContentUpdater(PluginRecord.class, new V1ToV2());
    }

    @Override
    public int contentVersion() {
        return PluginRecord.VERSION;
    }

    @Override
    public DataContainer toContainer() {
        final DataContainer c = DataContainer.createNew()
            .set(Queries.CONTENT_VERSION, PluginRecord.VERSION)
            .set(PluginRecord.FLAG, this.flag)
            .set(PluginRecord.B, this.b)
            .set(PluginRecord.S, this.s)
            .set(PluginRecord.I, this.i)
            .set(PluginRecord.SMALL, this.small)
            .set(PluginRecord.BIG, this.big)
            .set(PluginRecord.F, this.f)
            .set(PluginRecord.D, this.d)
            .set(PluginRecord.NAME, this.name)
            .set(PluginRecord.ID, this.id)
            .set(PluginRecord.POS, this.pos)
            .set(PluginRecord.WHEN, this.when)
            .set(PluginRecord.FLAGS, this.flags)
            .set(PluginRecord.TAGS, this.tags)
            .set(PluginRecord.ITEM, this.item);
        final DataView countsView = c.createView(PluginRecord.COUNTS);
        this.counts.forEach((k, v) -> countsView.set(DataQuery.of(k), v));
        return c;
    }

    /** The same record as version 1 stored it. */
    DataContainer toVersion1Container() {
        final DataContainer c = this.toContainer();
        c.set(Queries.CONTENT_VERSION, 1);
        c.remove(PluginRecord.NAME);
        c.set(PluginRecord.OLD_NAME, this.name);
        return c;
    }

    static final class V1ToV2 implements DataContentUpdater {

        @Override
        public int inputVersion() {
            return 1;
        }

        @Override
        public int outputVersion() {
            return 2;
        }

        @Override
        public DataView update(final DataView content) {
            final String old = content.getString(PluginRecord.OLD_NAME).orElseThrow();
            content.remove(PluginRecord.OLD_NAME);
            content.set(PluginRecord.NAME, old);
            content.set(Queries.CONTENT_VERSION, 2);
            return content;
        }
    }

    static final class Builder extends AbstractDataBuilder<PluginRecord> {

        Builder() {
            super(PluginRecord.class, PluginRecord.VERSION);
        }

        @Override
        protected Optional<PluginRecord> buildContent(final DataView v) throws InvalidDataException {
            final Map<String, Integer> counts = new LinkedHashMap<>();
            final DataView countsView = v.getView(PluginRecord.COUNTS).orElseThrow();
            for (final DataQuery key : countsView.keys(false)) {
                counts.put(key.toString(), countsView.getInt(key).orElseThrow());
            }
            return Optional.of(new PluginRecord(
                v.getBoolean(PluginRecord.FLAG).orElseThrow(),
                v.getByte(PluginRecord.B).orElseThrow(),
                v.getShort(PluginRecord.S).orElseThrow(),
                v.getInt(PluginRecord.I).orElseThrow(),
                v.getLong(PluginRecord.SMALL).orElseThrow(),
                v.getLong(PluginRecord.BIG).orElseThrow(),
                v.getFloat(PluginRecord.F).orElseThrow(),
                v.getDouble(PluginRecord.D).orElseThrow(),
                v.getString(PluginRecord.NAME).orElseThrow(),
                v.getObject(PluginRecord.ID, UUID.class).orElseThrow(),
                v.getObject(PluginRecord.POS, Vector3d.class).orElseThrow(),
                v.getObject(PluginRecord.WHEN, Instant.class).orElseThrow(),
                v.getBooleanList(PluginRecord.FLAGS).orElseThrow(),
                v.getStringList(PluginRecord.TAGS).orElseThrow(),
                counts,
                v.getSerializable(PluginRecord.ITEM, ItemStackSnapshot.class).orElseThrow()));
        }
    }
}
