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
package org.spongepowered.common.test.gametest.entity;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.GameRules;
import org.spongepowered.api.data.Keys;
import org.spongepowered.common.test.gametest.AfterBatch;
import org.spongepowered.common.test.gametest.BeforeBatch;
import org.spongepowered.common.test.gametest.GameTest;
import org.spongepowered.common.test.gametest.GameTestGroup;

/**
 * Whether a creeper's explosion keeps blocks once the {@code mobGriefing} rule is turned off, as it does without
 * Sponge (SpongePowered/Sponge#4320). The rule is the level's, and this group's tests run together as one batch, so it
 * is turned off for the whole batch and restored afterwards, rather than by each test.
 *
 * @see CreeperGriefingGameTests
 */
@GameTestGroup("creeper_no_mob_griefing")
public final class CreeperNoMobGriefingGameTests {

    private boolean mobGriefing;

    @BeforeBatch
    void disableMobGriefing(final ServerLevel level) {
        final GameRules.BooleanValue rule = level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        this.mobGriefing = rule.get();
        rule.set(false, level.getServer());
    }

    @AfterBatch
    void restoreMobGriefing(final ServerLevel level) {
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(this.mobGriefing, level.getServer());
    }

    @GameTest(structure = CreeperExplosions.STONE_RING, maxTicks = CreeperExplosions.MAX_TICKS)
    void keepsBlocks(final GameTestHelper helper) {
        helper.assertFalse(helper.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING), "mobGriefing is still on");

        CreeperExplosions.detonate(CreeperExplosions.spawn(helper)).succeedWhenExploded(helper, false, false);
    }

    // The rule decides for every creeper; a creeper allowed to grief doesn't get around it
    @GameTest(structure = CreeperExplosions.STONE_RING, maxTicks = CreeperExplosions.MAX_TICKS)
    void canGriefDoesNotOverrideRule(final GameTestHelper helper) {
        final Creeper creeper = CreeperExplosions.spawn(helper);
        ((org.spongepowered.api.entity.living.monster.Creeper) creeper).offer(Keys.CAN_GRIEF, true);

        CreeperExplosions.detonate(creeper).succeedWhenExploded(helper, false, false);
    }

    // A plugin can still have one explosion break blocks, by changing it before it happens
    @GameTest(structure = CreeperExplosions.STONE_RING, maxTicks = CreeperExplosions.MAX_TICKS)
    void eventCanBreakBlocks(final GameTestHelper helper) {
        CreeperExplosions.detonate(CreeperExplosions.spawn(helper), event -> event.explosionBuilder().shouldBreakBlocks(true))
            .succeedWhenExploded(helper, false, true);
    }
}
