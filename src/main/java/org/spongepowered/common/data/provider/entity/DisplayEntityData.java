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
package org.spongepowered.common.data.provider.entity;

import com.mojang.math.Transformation;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.display.BillboardType;
import org.spongepowered.api.entity.display.ItemDisplayType;
import org.spongepowered.api.entity.display.TextAlignment;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.util.Transform;
import org.spongepowered.common.accessor.world.entity.DisplayAccessor;
import org.spongepowered.common.accessor.world.entity.Display_BlockDisplayAccessor;
import org.spongepowered.common.accessor.world.entity.Display_ItemDisplayAccessor;
import org.spongepowered.common.accessor.world.entity.Display_TextDisplayAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.SpongeTicks;
import org.spongepowered.math.imaginary.Quaterniond;
import org.spongepowered.math.matrix.Matrix4d;
import org.spongepowered.math.vector.Vector3d;

public class DisplayEntityData {

    private static final int MAX_TELEPORT_DURATION = 59;
    private static final int NO_GLOW_COLOR_OVERRIDE = -1;

    private DisplayEntityData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(Display.class)
                    .create(Keys.TRANSFORM)
                        .get(DisplayEntityData::getTransform)
                        .set(DisplayEntityData::setTransform)
                    .create(Keys.MATRIX)
                        .get(DisplayEntityData::getMatrix)
                        .set(DisplayEntityData::setTransform)
                .asMutable(DisplayAccessor.class)
                    .create(Keys.BILLBOARD_TYPE)
                        .get(h -> (BillboardType) (Object) h.invoker$getBillboardConstraints())
                        .set((h, v) -> h.invoker$setBillboardConstraints((Display.BillboardConstraints) (Object) v))
                    .create(Keys.SKY_LIGHT)
                        .get(h -> DisplayEntityData.skyLight(h.invoker$getPackedBrightnessOverride()))
                        .setAnd((h, v) -> {
                            if (!DisplayEntityData.isLightValue(v)) {
                                return false;
                            }
                            h.invoker$setBrightnessOverride(DisplayEntityData.brightness(h.invoker$getPackedBrightnessOverride(), null, v));
                            return true;
                        })
                        .delete(h -> h.invoker$setBrightnessOverride(null))
                    .create(Keys.BLOCK_LIGHT)
                        .get(h -> DisplayEntityData.blockLight(h.invoker$getPackedBrightnessOverride()))
                        .setAnd((h, v) -> {
                            if (!DisplayEntityData.isLightValue(v)) {
                                return false;
                            }
                            h.invoker$setBrightnessOverride(DisplayEntityData.brightness(h.invoker$getPackedBrightnessOverride(), v, null));
                            return true;
                        })
                        .delete(h -> h.invoker$setBrightnessOverride(null))
                    .create(Keys.INTERPOLATION_DURATION)
                        .get(h -> Ticks.of(h.invoker$getInterpolationDuration()))
                        .setAnd((h ,v) -> {
                            if (v.isInfinite()) {
                                return false;
                            }
                            h.invoker$setInterpolationDuration(SpongeTicks.toSaturatedIntOrInfinite(v));
                            return true;
                        })
                    .create(Keys.INTERPOLATION_DELAY)
                        .get(h -> Ticks.of(h.invoker$getInterpolationDelay()))
                        .setAnd((h ,v) -> {
                            if (v.isInfinite()) {
                                return false;
                            }
                            h.invoker$setInterpolationDelay(SpongeTicks.toSaturatedIntOrInfinite(v));
                            return true;
                        })
                    .create(Keys.TELEPORT_DURATION)
                        .get(h -> Ticks.of(h.invoker$getPosRotInterpolationDuration()))
                        .setAnd((h ,v) -> {
                            // Vanilla clamps it to this range when loading the entity
                            if (v.isInfinite() || v.ticks() > DisplayEntityData.MAX_TELEPORT_DURATION) {
                                return false;
                            }
                            h.invoker$setPosRotInterpolationDuration(SpongeTicks.toSaturatedIntOrInfinite(v));
                            return true;
                        })
                    .create(Keys.SHADOW_RADIUS)
                        .get(h -> (double) h.invoker$getShadowRadius())
                        .set((h ,v) -> h.invoker$setShadowRadius(v.floatValue()))
                    .create(Keys.SHADOW_STRENGTH)
                        .get(h -> (double) h.invoker$getShadowStrength())
                        .set((h ,v) -> h.invoker$setShadowStrength(v.floatValue()))
                    .create(Keys.VIEW_RANGE)
                        .get(h -> (double) h.invoker$getViewRange())
                        .set((h ,v) -> h.invoker$setViewRange(v.floatValue()))
                    .create(Keys.CULLING_WIDTH)
                        .get(h -> (double) h.invoker$getWidth())
                        .setAnd((h, v) -> {
                            if (v < 0) {
                                return false;
                            }
                            h.invoker$setWidth(v.floatValue());
                            return true;
                        })
                    .create(Keys.CULLING_HEIGHT)
                        .get(h -> (double) h.invoker$getHeight())
                        .setAnd((h, v) -> {
                            if (v < 0) {
                                return false;
                            }
                            h.invoker$setHeight(v.floatValue());
                            return true;
                        })
                    .create(Keys.GLOW_COLOR)
                        .get(h -> {
                            final int color = h.invoker$getGlowColorOverride();
                            return color == DisplayEntityData.NO_GLOW_COLOR_OVERRIDE ? null : Color.ofRgb(color & 0x00ffffff);
                        })
                        .set((h, v) -> h.invoker$setGlowColorOverride(v.rgb()))
                        .delete(h -> h.invoker$setGlowColorOverride(DisplayEntityData.NO_GLOW_COLOR_OVERRIDE))
                .asMutable(Display_BlockDisplayAccessor.class)
                    .create(Keys.BLOCK_STATE)
                        .get(h -> ((BlockState) h.invoker$getBlockState()))
                        .set((h, v) -> h.invoker$setBlockState((net.minecraft.world.level.block.state.BlockState) v))
                .asMutable(Display_ItemDisplayAccessor.class)
                    .create(Keys.ITEM_STACK_SNAPSHOT)
                        .get(h -> ItemStackUtil.snapshotOf(h.invoker$getItemStack()))
                        .set((h, v) -> h.invoker$setItemStack(ItemStackUtil.fromSnapshotToNative(v)))
                    .create(Keys.ITEM_DISPLAY_TYPE)
                        .get(h -> (ItemDisplayType) (Object) h.invoker$getItemTransform())
                        .set((h, v) -> h.invoker$setItemTransform(((ItemDisplayContext) (Object) v)))
                .asMutable(Display_TextDisplayAccessor.class)
                    .create(Keys.TEXT)
                        .get(h -> SpongeAdventure.asAdventure(h.invoker$getText()))
                        .set((h, v) -> h.invoker$setText(SpongeAdventure.asVanilla(v)))
                    .create(Keys.LINE_WIDTH)
                        .get(Display_TextDisplayAccessor::invoker$getLineWidth)
                        .set(Display_TextDisplayAccessor::invoker$setLineWidth)
                    .create(Keys.OPACITY)
                        .get(Display_TextDisplayAccessor::invoker$getTextOpacity)
                        .set(Display_TextDisplayAccessor::invoker$setTextOpacity)
                    .create(Keys.SEE_THROUGH_BLOCKS)
                        .get(h -> DisplayEntityData.getFlagValue(h, Display.TextDisplay.FLAG_SEE_THROUGH))
                        .set((h, v) -> DisplayEntityData.setFlagValue(h, v, Display.TextDisplay.FLAG_SEE_THROUGH))
                    .create(Keys.HAS_TEXT_SHADOW)
                        .get(h -> DisplayEntityData.getFlagValue(h, Display.TextDisplay.FLAG_SHADOW))
                        .set((h, v) -> DisplayEntityData.setFlagValue(h, v, Display.TextDisplay.FLAG_SHADOW))
                    .create(Keys.HAS_DEFAULT_BACKGROUND)
                        .get(h -> DisplayEntityData.getFlagValue(h, Display.TextDisplay.FLAG_USE_DEFAULT_BACKGROUND))
                        .set((h, v) -> DisplayEntityData.setFlagValue(h, v, Display.TextDisplay.FLAG_USE_DEFAULT_BACKGROUND))
                    .create(Keys.TEXT_ALIGNMENT)
                        .get(DisplayEntityData::getAlignment)
                        .set(DisplayEntityData::setAlignment)
                    .create(Keys.TEXT_BACKGROUND_COLOR)
                        .get(h -> DisplayEntityData.argbToColor(h.invoker$getBackgroundColor()))
                        .set((h, v) -> h.invoker$setBackgroundColor(DisplayEntityData.argbWithColor(h.invoker$getBackgroundColor(), v)))
                    .create(Keys.TEXT_BACKGROUND_OPACITY)
                        .get(h -> DisplayEntityData.argbToOpacity(h.invoker$getBackgroundColor()))
                        .set((h, v) -> h.invoker$setBackgroundColor(DisplayEntityData.argbWithOpacity(h.invoker$getBackgroundColor(), v)))
        ;
    }
    // @formatter:on

    private static boolean getFlagValue(final Display_TextDisplayAccessor h, final byte flag) {
        return (h.invoker$getFlags() & flag) != 0;
    }

    private static void setFlagValue(final Display_TextDisplayAccessor h, final Boolean set, final byte flag) {
        byte flags = h.invoker$getFlags();
        if (set) {
            flags = (byte) (flags | flag);
        } else {
            flags = (byte) (flags & ~flag);
        }
        h.invoker$setFlags(flags);
    }

    private static Brightness brightness(final int original, @Nullable Integer block, @Nullable Integer sky) {
        int blockValue;
        int skyValue;
        if (original != -1) {
            final Brightness unpacked = Brightness.unpack(original);
            // Take existing value if not getting set
            blockValue = block == null ? unpacked.block() : block;
            skyValue = sky == null ? unpacked.sky() : sky;
        } else {
            // If no existing value set unset values to 15
            blockValue = block == null ? 15 : block;
            skyValue = sky == null ? 15 : sky;
        }
        return new Brightness(blockValue, skyValue);
    }

    private static boolean isLightValue(final int value) {
        return value >= 0 && value <= 15;
    }

    @Nullable
    private static Integer blockLight(final int original) {
        if (original == -1) {
            return null;
        }
        return Brightness.unpack(original).block();
    }

    @Nullable
    private static Integer skyLight(final int original) {
        if (original == -1) {
            return null;
        }
        return Brightness.unpack(original).sky();
    }

    private static Color argbToColor(final int argb) {
        return Color.ofRgb(argb & 0x00ffffff);
    }

    private static byte argbToOpacity(final int argb) {
        return (byte) (argb >> 24);
    }

    private static int argbWithColor(final int argb, final Color color) {
        final int alpha = argb & 0xff000000;
        final int rgb = color.rgb();
        return alpha | rgb;
    }

    private static int argbWithOpacity(final int argb, final Byte opacity) {
        final int alpha = opacity << 24;
        final int rgb = argb & 0x00ffffff;
        return alpha | rgb;
    }

    private static TextAlignment getAlignment(final Display_TextDisplayAccessor h) {
        return (TextAlignment) (Object) Display.TextDisplay.getAlign(h.invoker$getFlags());
    }

    private static void setAlignment(final Display_TextDisplayAccessor h, final TextAlignment alignment) {
        switch ((Display.TextDisplay.Align) (Object) alignment) {
            case LEFT -> {
                DisplayEntityData.setFlagValue(h, true, Display.TextDisplay.FLAG_ALIGN_LEFT);
                DisplayEntityData.setFlagValue(h, false, Display.TextDisplay.FLAG_ALIGN_RIGHT);
            }
            case RIGHT -> {
                DisplayEntityData.setFlagValue(h, false, Display.TextDisplay.FLAG_ALIGN_LEFT);
                DisplayEntityData.setFlagValue(h, true, Display.TextDisplay.FLAG_ALIGN_RIGHT);
            }
            case CENTER -> {
                DisplayEntityData.setFlagValue(h, false, Display.TextDisplay.FLAG_ALIGN_LEFT);
                DisplayEntityData.setFlagValue(h, false, Display.TextDisplay.FLAG_ALIGN_RIGHT);
            }
        }
    }

    // A Transform has no right rotation: it is folded into the left one, which is exact while the scale is uniform
    private static Transform getTransform(final Display display) {
        final Transformation transformation = DisplayAccessor.invoker$createTransformation(display.getEntityData());
        final Vector3f translation = transformation.getTranslation();
        final Vector3f scale = transformation.getScale();
        final Quaternionf rotation = new Quaternionf(transformation.getLeftRotation()).mul(transformation.getRightRotation());
        return Transform.of(new Vector3d(translation.x, translation.y, translation.z))
            .withRotation(Quaterniond.from(rotation.x, rotation.y, rotation.z, rotation.w))
            .withScale(new Vector3d(scale.x, scale.y, scale.z));
    }

    private static void setTransform(final Display display, final Transform transform) {
        final Vector3d translation = transform.position();
        final Quaterniond rotation = transform.rotationAsQuaternion();
        final Vector3d scale = transform.scale();
        ((DisplayAccessor) display).invoker$setTransformation(new Transformation(
            new Vector3f((float) translation.x(), (float) translation.y(), (float) translation.z()),
            new Quaternionf((float) rotation.x(), (float) rotation.y(), (float) rotation.z(), (float) rotation.w()),
            new Vector3f((float) scale.x(), (float) scale.y(), (float) scale.z()),
            null));
    }

    private static Matrix4d getMatrix(final Display display) {
        var vanillaTransform = DisplayAccessor.invoker$createTransformation(display.getEntityData());
        var vMatrix = vanillaTransform.getMatrix();
        return Matrix4d.from(
            vMatrix.get(0, 0), vMatrix.get(1, 0), vMatrix.get(2, 0), vMatrix.get(3, 0),
            vMatrix.get(0, 1), vMatrix.get(1, 1), vMatrix.get(2, 1), vMatrix.get(3, 1),
            vMatrix.get(0, 2), vMatrix.get(1, 2), vMatrix.get(2, 2), vMatrix.get(3, 2),
            vMatrix.get(0, 3), vMatrix.get(1, 3), vMatrix.get(2, 3), vMatrix.get(3, 3));
    }

    private static void setTransform(final Display h, final Matrix4d matrix) {
        var vMatrix = new org.joml.Matrix4f(
                (float) matrix.get(0, 0), (float) matrix.get(1, 0), (float) matrix.get(2, 0), (float) matrix.get(3, 0),
                (float) matrix.get(0, 1), (float) matrix.get(1, 1), (float) matrix.get(2, 1), (float) matrix.get(3, 1),
                (float) matrix.get(0, 2), (float) matrix.get(1, 2), (float) matrix.get(2, 2), (float) matrix.get(3, 2),
                (float) matrix.get(0, 3), (float) matrix.get(1, 3), (float) matrix.get(2, 3), (float) matrix.get(3, 3)
        );
        var vanillaTransform = new Transformation(vMatrix);
        ((DisplayAccessor) h).invoker$setTransformation(vanillaTransform);
    }
}
