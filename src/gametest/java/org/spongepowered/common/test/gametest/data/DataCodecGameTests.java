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
package org.spongepowered.common.test.gametest.data;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.api.data.persistence.DataCodec;
import org.spongepowered.api.data.persistence.DataCodecException;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.FieldCodec;
import org.spongepowered.api.data.persistence.RecordCodec;
import org.spongepowered.common.test.gametest.GameTest;
import org.spongepowered.common.test.gametest.GameTestGroup;
import org.spongepowered.common.util.Constants;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * {@link DataCodec}s reading real entity data: a record codec for part of a
 * pig's vanilla NBT, from its {@code toContainer()} {@code Data} view.
 */
@GameTestGroup("data_codec")
public final class DataCodecGameTests {

    private static final BlockPos PIG = new BlockPos(1, 1, 1);

    /** A few of the keys vanilla writes for every living entity, in vanilla's own types. */
    record EntityBasics(float health, short air, boolean onGround, List<Double> pos, UUID uuid) {
        static final FieldCodec<EntityBasics, Float> HEALTH = DataCodec.FLOAT.fieldOf("Health", EntityBasics::health);
        static final FieldCodec<EntityBasics, Short> AIR = DataCodec.SHORT.fieldOf("Air", EntityBasics::air);
        static final FieldCodec<EntityBasics, Boolean> ON_GROUND = DataCodec.BOOLEAN.fieldOf("OnGround", EntityBasics::onGround);
        static final FieldCodec<EntityBasics, List<Double>> POS = DataCodec.DOUBLE.listOf().fieldOf("Pos", EntityBasics::pos);
        static final FieldCodec<EntityBasics, UUID> UUID_FIELD = DataCodec.UUID.fieldOf("UUID", EntityBasics::uuid);
        static final RecordCodec<EntityBasics> CODEC = DataCodec.record(
            f -> new EntityBasics(f.get(HEALTH), f.get(AIR), f.get(ON_GROUND), f.get(POS), f.get(UUID_FIELD)),
            HEALTH, AIR, ON_GROUND, POS, UUID_FIELD);
    }

    // The codec decodes vanilla's float, short, byte-tag boolean, double list and four-int UUID,
    // in memory and after a JSON round trip that changes their stored widths
    @GameTest
    void decodesVanillaEntityData(final GameTestHelper helper) {
        final var pig = helper.spawn(EntityType.PIG, DataCodecGameTests.PIG);
        final DataContainer container = ((org.spongepowered.api.entity.Entity) pig).toContainer();
        final DataView data = container.exactView(Constants.Entity.V2.DATA).orElseThrow();
        final EntityBasics expected = new EntityBasics(pig.getHealth(), (short) pig.getAirSupply(), pig.onGround(),
            List.of(pig.getX(), pig.getY(), pig.getZ()), pig.getUUID());

        final EntityBasics decoded = EntityBasics.CODEC.decode(data);
        helper.assertTrue(decoded.equals(expected), Component.literal("Decoded " + decoded + ", expected " + expected));

        final DataView json;
        try {
            json = DataFormats.JSON.get().read(DataFormats.JSON.get().write(data));
        } catch (final IOException e) {
            throw new IllegalStateException("JSON round trip of the pig's data failed", e);
        }
        final EntityBasics fromJson = EntityBasics.CODEC.decode(json);
        helper.assertTrue(fromJson.equals(expected), Component.literal("Decoded from JSON " + fromJson + ", expected " + expected));

        // encoding writes what decoding reads, and vanilla's UUID form
        final DataContainer encoded = EntityBasics.CODEC.encode(decoded);
        helper.assertTrue(EntityBasics.CODEC.decode(encoded).equals(expected), Component.literal("Re-encoded data should decode to the same value"));
        helper.assertTrue(encoded.get(DataQuery.of(EntityBasics.UUID_FIELD.key())).orElseThrow() instanceof int[],
            Component.literal("UUID should be encoded as four ints, like vanilla"));

        helper.succeed();
    }

    // A malformed value in real data is reported with its key instead of being converted
    @GameTest
    void reportsMalformedEntityData(final GameTestHelper helper) {
        final var pig = helper.spawn(EntityType.PIG, DataCodecGameTests.PIG);
        final DataView data = ((org.spongepowered.api.entity.Entity) pig).toContainer().exactView(Constants.Entity.V2.DATA).orElseThrow().copy();
        data.set(DataQuery.of("Air"), 100_000);
        try {
            EntityBasics.CODEC.decode(data);
            helper.fail(Component.literal("An out-of-range Air should not decode"));
        } catch (final DataCodecException e) {
            helper.assertTrue(e.getMessage().equals("Expected a short at 'Air' but found Integer: 100000"), Component.literal(e.getMessage()));
        }
        helper.succeed();
    }
}
