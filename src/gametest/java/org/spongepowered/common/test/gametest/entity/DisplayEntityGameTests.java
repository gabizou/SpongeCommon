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

import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.display.DisplayEntity;
import org.spongepowered.api.entity.display.TextDisplay;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.util.Transform;
import org.spongepowered.common.test.gametest.GameTest;
import org.spongepowered.common.test.gametest.GameTestGroup;
import org.spongepowered.math.imaginary.Quaterniond;
import org.spongepowered.math.vector.Vector3d;

/**
 * Checks the display entity keys against what vanilla saves for the entity, which is what the client is sent
 * and what the wiki documents, and that what vanilla loads back reads the same through the keys.
 */
@GameTestGroup("display_entity")
public final class DisplayEntityGameTests {

    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final double EPSILON = 1.0E-4;

    @GameTest
    void textIsNotTheDisplayName(final GameTestHelper helper) {
        final Display.TextDisplay vanilla = helper.spawn(EntityType.TEXT_DISPLAY, DisplayEntityGameTests.POS);
        final TextDisplay display = (TextDisplay) vanilla;
        final Component text = Component.text("shown text");

        DisplayEntityGameTests.assertOffered(helper, display, Keys.TEXT, text);
        helper.assertValueEqual(text, display.displayedText(), net.minecraft.network.chat.Component.literal("displayedText"));
        helper.assertValueEqual("shown text", DisplayEntityGameTests.save(vanilla).read("text", ComponentSerialization.CODEC)
            .map(net.minecraft.network.chat.Component::getString).orElse(null), net.minecraft.network.chat.Component.literal("saved text"));
        // The display name stays the entity's own name, and cannot be offered
        helper.assertTrue(!display.require(Keys.DISPLAY_NAME).equals(text), net.minecraft.network.chat.Component.literal("display name is the text"));
        helper.assertTrue(!display.offer(Keys.DISPLAY_NAME, Component.text("other")).isSuccessful(),
            net.minecraft.network.chat.Component.literal("display name was offered"));
        helper.assertValueEqual(text, display.displayedText(), net.minecraft.network.chat.Component.literal("displayedText after display name offer"));
        helper.succeed();
    }

    @GameTest
    void lightOverrides(final GameTestHelper helper) {
        final Display.BlockDisplay vanilla = helper.spawn(EntityType.BLOCK_DISPLAY, DisplayEntityGameTests.POS);
        final DisplayEntity display = (DisplayEntity) vanilla;

        DisplayEntityGameTests.assertOffered(helper, display, Keys.SKY_LIGHT, 3);
        CompoundTag brightness = DisplayEntityGameTests.saveTag(vanilla).getCompoundOrEmpty("brightness");
        helper.assertValueEqual(3, brightness.getIntOr("sky", -1), net.minecraft.network.chat.Component.literal("saved sky light"));
        helper.assertValueEqual(15, brightness.getIntOr("block", -1), net.minecraft.network.chat.Component.literal("saved block light"));

        DisplayEntityGameTests.assertOffered(helper, display, Keys.BLOCK_LIGHT, 7);
        brightness = DisplayEntityGameTests.saveTag(vanilla).getCompoundOrEmpty("brightness");
        helper.assertValueEqual(3, brightness.getIntOr("sky", -1), net.minecraft.network.chat.Component.literal("saved sky light"));
        helper.assertValueEqual(7, brightness.getIntOr("block", -1), net.minecraft.network.chat.Component.literal("saved block light"));
        helper.assertValueEqual(3, display.skyLight().orElse(-1), net.minecraft.network.chat.Component.literal("sky light"));
        helper.assertValueEqual(7, display.blockLight().orElse(-1), net.minecraft.network.chat.Component.literal("block light"));

        // Out of range values would pack into the other light's bits, and fail to save
        helper.assertTrue(!display.offer(Keys.SKY_LIGHT, 16).isSuccessful(), net.minecraft.network.chat.Component.literal("sky light 16 was offered"));
        helper.assertTrue(!display.offer(Keys.BLOCK_LIGHT, -1).isSuccessful(), net.minecraft.network.chat.Component.literal("block light -1 was offered"));

        display.remove(Keys.SKY_LIGHT);
        helper.assertTrue(display.skyLight().isEmpty() && display.blockLight().isEmpty(),
            net.minecraft.network.chat.Component.literal("light override left after removing it"));
        helper.assertTrue(!DisplayEntityGameTests.saveTag(vanilla).contains("brightness"), net.minecraft.network.chat.Component.literal("brightness saved"));
        helper.succeed();
    }

    @GameTest
    void displayTransform(final GameTestHelper helper) {
        final Display.BlockDisplay vanilla = helper.spawn(EntityType.BLOCK_DISPLAY, DisplayEntityGameTests.POS);
        final DisplayEntity display = (DisplayEntity) vanilla;
        final Vector3d position = display.position();
        final Transform transform = Transform.of(new Vector3d(0.5, 1, -0.25), new Vector3d(30, 45, 10), new Vector3d(2, 2, 2));

        // Vanilla keeps the transformation in floats, so it reads back close to what was offered rather than equal
        helper.assertTrue(display.offer(Keys.TRANSFORM, transform).isSuccessful(), net.minecraft.network.chat.Component.literal("transform was not offered"));
        final CompoundTag saved = DisplayEntityGameTests.saveTag(vanilla).getCompoundOrEmpty("transformation");
        DisplayEntityGameTests.assertVector(helper, "saved translation", transform.position(), saved.getListOrEmpty("translation"));
        DisplayEntityGameTests.assertVector(helper, "saved scale", transform.scale(), saved.getListOrEmpty("scale"));
        final Quaterniond rotation = transform.rotationAsQuaternion();
        final ListTag left = saved.getListOrEmpty("left_rotation");
        final Quaterniond savedRotation = Quaterniond.from(left.getFloatOr(0, 0), left.getFloatOr(1, 0), left.getFloatOr(2, 0), left.getFloatOr(3, 0));
        helper.assertTrue(Math.abs(Math.abs(rotation.dot(savedRotation)) - 1) < DisplayEntityGameTests.EPSILON,
            net.minecraft.network.chat.Component.literal("saved left rotation " + savedRotation + " is not " + rotation));

        final Transform read = display.displayTransform();
        DisplayEntityGameTests.assertClose(helper, "translation", transform.position(), read.position());
        DisplayEntityGameTests.assertClose(helper, "rotation", transform.rotation(), read.rotation());
        DisplayEntityGameTests.assertClose(helper, "scale", transform.scale(), read.scale());

        // The entity's own transform is still where it is in the world, so it can be set back
        DisplayEntityGameTests.assertClose(helper, "entity position", position, display.transform().position());
        display.setTransform(display.transform());
        DisplayEntityGameTests.assertClose(helper, "entity position after setTransform", position, display.position());
        helper.succeed();
    }

    @GameTest
    void interpolationDurations(final GameTestHelper helper) {
        final Display.ItemDisplay vanilla = helper.spawn(EntityType.ITEM_DISPLAY, DisplayEntityGameTests.POS);
        final DisplayEntity display = (DisplayEntity) vanilla;

        DisplayEntityGameTests.assertOffered(helper, display, Keys.TELEPORT_DURATION, Ticks.of(59));
        helper.assertTrue(!display.offer(Keys.TELEPORT_DURATION, Ticks.of(60)).isSuccessful(),
            net.minecraft.network.chat.Component.literal("teleport duration over vanilla's 59 tick limit was offered"));
        helper.assertValueEqual(59, DisplayEntityGameTests.saveTag(vanilla).getIntOr("teleport_duration", -1),
            net.minecraft.network.chat.Component.literal("saved teleport duration"));

        DisplayEntityGameTests.assertOffered(helper, display, Keys.INTERPOLATION_DURATION, Ticks.of(40));
        DisplayEntityGameTests.assertOffered(helper, display, Keys.INTERPOLATION_DELAY, Ticks.of(5));
        helper.assertValueEqual(40, DisplayEntityGameTests.saveTag(vanilla).getIntOr("interpolation_duration", -1),
            net.minecraft.network.chat.Component.literal("saved interpolation duration"));
        helper.succeed();
    }

    @GameTest
    void cullingAndGlowColor(final GameTestHelper helper) {
        final Display.BlockDisplay vanilla = helper.spawn(EntityType.BLOCK_DISPLAY, DisplayEntityGameTests.POS);
        final DisplayEntity display = (DisplayEntity) vanilla;

        helper.assertTrue(display.glowColor().isEmpty(), net.minecraft.network.chat.Component.literal("glow color override by default"));
        DisplayEntityGameTests.assertOffered(helper, display, Keys.CULLING_WIDTH, 2.5);
        DisplayEntityGameTests.assertOffered(helper, display, Keys.CULLING_HEIGHT, 4.0);
        helper.assertTrue(!display.offer(Keys.CULLING_WIDTH, -1.0).isSuccessful(), net.minecraft.network.chat.Component.literal("negative culling width was offered"));
        DisplayEntityGameTests.assertOffered(helper, display, Keys.GLOW_COLOR, Color.ofRgb(0x12AB34));

        final CompoundTag saved = DisplayEntityGameTests.saveTag(vanilla);
        helper.assertValueEqual(2.5F, saved.getFloatOr("width", -1), net.minecraft.network.chat.Component.literal("saved width"));
        helper.assertValueEqual(4.0F, saved.getFloatOr("height", -1), net.minecraft.network.chat.Component.literal("saved height"));
        helper.assertValueEqual(0x12AB34, saved.getIntOr("glow_color_override", 0), net.minecraft.network.chat.Component.literal("saved glow color"));
        helper.assertValueEqual(0x12AB34, vanilla.getTeamColor(), net.minecraft.network.chat.Component.literal("team color"));

        display.remove(Keys.GLOW_COLOR);
        helper.assertTrue(display.glowColor().isEmpty(), net.minecraft.network.chat.Component.literal("glow color override left after removing it"));
        helper.assertValueEqual(-1, DisplayEntityGameTests.saveTag(vanilla).getIntOr("glow_color_override", 0),
            net.minecraft.network.chat.Component.literal("saved glow color after removing it"));
        helper.succeed();
    }

    @GameTest
    void textOpacityIsUnsigned(final GameTestHelper helper) {
        final Display.TextDisplay vanilla = helper.spawn(EntityType.TEXT_DISPLAY, DisplayEntityGameTests.POS);
        final TextDisplay display = (TextDisplay) vanilla;

        // Vanilla defaults to fully opaque text, stored as -1
        helper.assertValueEqual(255, display.textOpacity(), net.minecraft.network.chat.Component.literal("default text opacity"));
        DisplayEntityGameTests.assertOffered(helper, display, Keys.TEXT_BACKGROUND_OPACITY, (byte) 200);
        helper.assertValueEqual(200, display.backgroundOpacity(), net.minecraft.network.chat.Component.literal("background opacity"));
        helper.assertValueEqual(200, DisplayEntityGameTests.saveTag(vanilla).getIntOr("background", 0) >>> 24,
            net.minecraft.network.chat.Component.literal("saved background alpha"));
        helper.succeed();
    }

    @GameTest
    void survivesSaving(final GameTestHelper helper) {
        final Display.TextDisplay vanilla = helper.spawn(EntityType.TEXT_DISPLAY, DisplayEntityGameTests.POS);
        final TextDisplay display = (TextDisplay) vanilla;
        final Transform transform = Transform.of(new Vector3d(0, 0.5, 0), new Vector3d(0, 90, 0), new Vector3d(1, 3, 1));
        display.offer(Keys.TEXT, Component.text("saved"));
        display.offer(Keys.TRANSFORM, transform);
        display.offer(Keys.TELEPORT_DURATION, Ticks.of(10));
        display.offer(Keys.GLOW_COLOR, Color.RED);
        display.offer(Keys.CULLING_WIDTH, 1.5);
        display.offer(Keys.SKY_LIGHT, 4);

        final Display.TextDisplay loaded = EntityType.TEXT_DISPLAY.create(helper.getLevel(), EntitySpawnReason.LOAD);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), DisplayEntityGameTests.saveTag(vanilla)));
        final TextDisplay reloaded = (TextDisplay) loaded;
        helper.assertValueEqual(Component.text("saved"), reloaded.displayedText(), net.minecraft.network.chat.Component.literal("text"));
        DisplayEntityGameTests.assertClose(helper, "translation", transform.position(), reloaded.displayTransform().position());
        DisplayEntityGameTests.assertClose(helper, "rotation", transform.rotation(), reloaded.displayTransform().rotation());
        DisplayEntityGameTests.assertClose(helper, "scale", transform.scale(), reloaded.displayTransform().scale());
        helper.assertValueEqual(Ticks.of(10), reloaded.teleportDuration(), net.minecraft.network.chat.Component.literal("teleport duration"));
        helper.assertValueEqual(Color.RED, reloaded.glowColor().orElse(null), net.minecraft.network.chat.Component.literal("glow color"));
        helper.assertValueEqual(1.5, reloaded.cullingWidth(), net.minecraft.network.chat.Component.literal("culling width"));
        helper.assertValueEqual(4, reloaded.skyLight().orElse(-1), net.minecraft.network.chat.Component.literal("sky light"));
        helper.succeed();
    }

    private static <E> void assertOffered(final GameTestHelper helper, final DisplayEntity display, final org.spongepowered.api.data.Key<Value<E>> key, final E value) {
        helper.assertTrue(display.offer(key, value).isSuccessful(), net.minecraft.network.chat.Component.literal(key.key() + " was not offered"));
        helper.assertValueEqual(value, display.get(key).orElse(null), net.minecraft.network.chat.Component.literal(key.key().toString()));
    }

    private static void assertClose(final GameTestHelper helper, final String name, final Vector3d expected, final Vector3d actual) {
        helper.assertTrue(expected.distance(actual) < DisplayEntityGameTests.EPSILON * 100,
            net.minecraft.network.chat.Component.literal(name + " " + actual + " is not " + expected));
    }

    private static void assertVector(final GameTestHelper helper, final String name, final Vector3d expected, final ListTag actual) {
        DisplayEntityGameTests.assertClose(helper, name, expected, new Vector3d(actual.getFloatOr(0, 0), actual.getFloatOr(1, 0), actual.getFloatOr(2, 0)));
    }

    private static CompoundTag saveTag(final net.minecraft.world.entity.Entity entity) {
        final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    private static net.minecraft.world.level.storage.ValueInput save(final net.minecraft.world.entity.Entity entity) {
        return TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), DisplayEntityGameTests.saveTag(entity));
    }
}
