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
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.common.test.gametest.GameTest;
import org.spongepowered.common.test.gametest.GameTestGroup;
import org.spongepowered.common.util.Constants;

import java.io.IOException;
import java.util.Optional;

/**
 * The strict {@code exact*} DataView getters on real entity data: a pig's
 * {@code toContainer()}, whose {@code Data} view holds the entity's vanilla NBT
 * (Health is a float, Air a short, OnGround a byte, as vanilla writes them).
 */
@GameTestGroup("dataview_exact")
public final class DataViewExactGameTests {

    private static final BlockPos PIG = new BlockPos(1, 1, 1);
    private static final DataQuery HEALTH = Constants.Entity.V2.DATA.then("Health");
    private static final DataQuery AIR = Constants.Entity.V2.DATA.then("Air");
    private static final DataQuery ON_GROUND = Constants.Entity.V2.DATA.then("OnGround");

    // The exact getters read vanilla's stored types, and refuse the conversions the lenient getters make silently
    @GameTest
    void entityDataReadsExactly(final GameTestHelper helper) {
        final var pig = helper.spawn(EntityType.PIG, DataViewExactGameTests.PIG);
        final DataContainer container = ((org.spongepowered.api.entity.Entity) pig).toContainer();

        helper.assertTrue(container.exactFloat(DataViewExactGameTests.HEALTH).equals(Optional.of(pig.getHealth())),
            Component.literal("exactFloat(Health) should be the pig's health"));
        helper.assertTrue(container.exactShort(DataViewExactGameTests.AIR).equals(Optional.of((short) pig.getAirSupply())),
            Component.literal("exactShort(Air) should be the pig's air supply"));
        // a short widens to int exactly
        helper.assertTrue(container.exactInt(DataViewExactGameTests.AIR).getAsInt() == pig.getAirSupply(),
            Component.literal("exactInt(Air) should widen the stored short"));

        // Health is a float: getInt truncates it, exactInt refuses it
        helper.assertTrue(container.getInt(DataViewExactGameTests.HEALTH).isPresent(), Component.literal("getInt(Health) should coerce"));
        DataViewExactGameTests.assertInvalid(helper, () -> container.exactInt(DataViewExactGameTests.HEALTH), "exactInt(Health)");

        // vanilla writes booleans as byte tags, so the strict boolean getter refuses them and exactByte reads them
        helper.assertTrue(container.getBoolean(DataViewExactGameTests.ON_GROUND).isPresent(), Component.literal("getBoolean(OnGround) should coerce the byte"));
        DataViewExactGameTests.assertInvalid(helper, () -> container.exactBoolean(DataViewExactGameTests.ON_GROUND), "exactBoolean(OnGround)");
        helper.assertTrue(container.exactByte(DataViewExactGameTests.ON_GROUND).isPresent(), Component.literal("exactByte(OnGround) should read the byte"));

        // reading below a scalar is reported, where get() just says absent
        final DataQuery belowHealth = DataViewExactGameTests.HEALTH.then("value");
        helper.assertTrue(container.get(belowHealth).isEmpty(), Component.literal("get() below a scalar should be absent"));
        DataViewExactGameTests.assertInvalid(helper, () -> container.exactFloat(belowHealth), "exactFloat(Health.value)");

        helper.succeed();
    }

    // JSON stores Health as a double and Air as an int; the exact getters still return the entity's values
    @GameTest
    void entityDataSurvivesJson(final GameTestHelper helper) {
        final var pig = helper.spawn(EntityType.PIG, DataViewExactGameTests.PIG);
        final DataView read;
        try {
            read = DataFormats.JSON.get().read(DataFormats.JSON.get().write(((org.spongepowered.api.entity.Entity) pig).toContainer()));
        } catch (final IOException e) {
            throw new IllegalStateException("JSON round trip of the pig's data failed", e);
        }

        helper.assertTrue(read.get(DataViewExactGameTests.HEALTH).orElseThrow() instanceof Double, Component.literal("JSON should read Health back as a Double"));
        helper.assertTrue(read.get(DataViewExactGameTests.AIR).orElseThrow() instanceof Integer, Component.literal("JSON should read Air back as an Integer"));
        helper.assertTrue(read.exactFloat(DataViewExactGameTests.HEALTH).equals(Optional.of(pig.getHealth())),
            Component.literal("exactFloat(Health) should recover the pig's health after JSON"));
        helper.assertTrue(read.exactShort(DataViewExactGameTests.AIR).equals(Optional.of((short) pig.getAirSupply())),
            Component.literal("exactShort(Air) should recover the pig's air supply after JSON"));

        helper.succeed();
    }

    private static void assertInvalid(final GameTestHelper helper, final Runnable read, final String what) {
        try {
            read.run();
        } catch (final InvalidDataException expected) {
            return;
        }
        helper.fail(Component.literal(what + " should throw InvalidDataException"));
    }
}
