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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.entity.BlockEntityArchetype;
import org.spongepowered.api.block.entity.BlockEntityTypes;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.meta.BannerPatternLayer;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataSerializable;
import org.spongepowered.api.data.type.BannerPatternShapes;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.EntityArchetype;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.item.FireworkEffect;
import org.spongepowered.api.item.FireworkShapes;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Ticks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * API types plugins commonly store: {@code toContainer()}, optionally through a data format, then
 * {@code DataManager.deserialize} (their {@code DataBuilder}).
 *
 * <p>Each format cell compares two things with {@link LosslessSupport#difference}:</p>
 * <ul>
 *     <li>the container read back from the format against the original {@code toContainer()}; a difference
 *         here alone is reported as {@code PASS (container: ...)}, since the builder may tolerate it</li>
 *     <li>the rebuilt object's {@code toContainer()} against the original; a difference here is LOSSY,
 *         a builder failure is ERROR</li>
 * </ul>
 *
 * <p>Types that need a loaded world (ServerLocation, BlockSnapshot, EntitySnapshot) are covered by
 * {@code LosslessGameTests}.</p>
 */
@DisplayName("DataSerializable: toContainer -> format -> DataBuilder round trips")
class SerializableRoundTripTest {

    record Row(String name, Class<? extends DataSerializable> type, Supplier<? extends DataSerializable> value) {}

    static ItemStack sword() {
        return ItemStack.builder()
            .itemType(ItemTypes.DIAMOND_SWORD)
            .add(Keys.CUSTOM_NAME, Component.text("Blade", NamedTextColor.RED))
            .add(Keys.LORE, List.of(Component.text("first"), Component.text("second")))
            .add(Keys.APPLIED_ENCHANTMENTS, List.of(Enchantment.of(EnchantmentTypes.SHARPNESS, 3)))
            .add(Keys.ITEM_DURABILITY, 100)
            .build();
    }

    static List<Row> rows() {
        final List<Row> rows = new ArrayList<>();
        rows.add(new Row("ItemStack simple", ItemStack.class, () -> ItemStack.of(ItemTypes.COBBLESTONE, 64)));
        rows.add(new Row("ItemStack named, enchanted, damaged", ItemStack.class, SerializableRoundTripTest::sword));
        rows.add(new Row("ItemStack with unbreakable flag", ItemStack.class, () -> ItemStack.builder().itemType(ItemTypes.IRON_PICKAXE).add(Keys.IS_UNBREAKABLE, true).build()));
        rows.add(new Row("ItemStackSnapshot", ItemStackSnapshot.class, () -> SerializableRoundTripTest.sword().asImmutable()));
        rows.add(new Row("BlockState default", BlockState.class, () -> BlockTypes.STONE.get().defaultState()));
        rows.add(new Row("BlockState with properties", BlockState.class, () -> BlockState.fromString("minecraft:oak_stairs[facing=east,half=top,shape=straight,waterlogged=true]")));
        rows.add(new Row("GameProfile name only", GameProfile.class, () -> GameProfile.of(new UUID(1, 2), "Steve")));
        rows.add(new Row("GameProfile with signed property", GameProfile.class, () -> GameProfile.of(new UUID(1, 2), "Steve").withProperty(ProfileProperty.of("textures", "dmFsdWU=", "c2lnbmF0dXJl"))));
        rows.add(new Row("ProfileProperty unsigned", ProfileProperty.class, () -> ProfileProperty.of("textures", "dmFsdWU=")));
        rows.add(new Row("Enchantment", Enchantment.class, () -> Enchantment.of(EnchantmentTypes.SHARPNESS, 5)));
        rows.add(new Row("PotionEffect", PotionEffect.class, () -> PotionEffect.builder().potionType(PotionEffectTypes.SPEED).amplifier(2).duration(Ticks.of(200)).ambient(true).showParticles(false).showIcon(true).build()));
        rows.add(new Row("FireworkEffect", FireworkEffect.class, () -> FireworkEffect.builder().shape(FireworkShapes.STAR).color(Color.RED).fade(Color.BLUE).flicker(true).trail(true).build()));
        rows.add(new Row("Color", Color.class, () -> Color.ofRgb(0x123456)));
        rows.add(new Row("BannerPatternLayer", BannerPatternLayer.class, () -> BannerPatternLayer.of(BannerPatternShapes.CROSS, DyeColors.RED)));
        rows.add(new Row("EntityArchetype pig", EntityArchetype.class, () -> EntityArchetype.builder().type(EntityTypes.PIG.get()).build()));
        rows.add(new Row("BlockEntityArchetype chest", BlockEntityArchetype.class, () -> BlockEntityArchetype.builder().blockEntity(BlockEntityTypes.CHEST).state(BlockTypes.CHEST.get().defaultState()).build()));
        rows.add(new Row("PluginRecord (test type, version 2)", PluginRecord.class, PluginRecord::sample));
        return rows;
    }

    static final String[] COLUMNS = {"memory", "NBT", "JSON", "HOCON"};

    static final Map<String, String> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("ItemStack simple", "PPPP");
        EXPECTED.put("ItemStack named, enchanted, damaged", "PPPP");
        EXPECTED.put("ItemStack with unbreakable flag", "PPPP");
        EXPECTED.put("ItemStackSnapshot", "PPPP");
        EXPECTED.put("BlockState default", "PPPP");
        EXPECTED.put("BlockState with properties", "PPPP");
        EXPECTED.put("GameProfile name only", "PPPP");
        EXPECTED.put("GameProfile with signed property", "LLLL");
        EXPECTED.put("ProfileProperty unsigned", "PPPP");
        EXPECTED.put("Enchantment", "EEEE");
        EXPECTED.put("PotionEffect", "PPPP");
        EXPECTED.put("FireworkEffect", "LLLL");
        EXPECTED.put("Color", "PPPP");
        EXPECTED.put("BannerPatternLayer", "EEEE");
        EXPECTED.put("EntityArchetype pig", "PPPP");
        EXPECTED.put("BlockEntityArchetype chest", "PPPP");
        EXPECTED.put("PluginRecord (test type, version 2)", "PPPP");
    }

    static String rebuild(final Class<? extends DataSerializable> type, final DataContainer original, final DataContainer read) {
        final @Nullable String containerDiff = LosslessSupport.difference(original, read);
        final DataSerializable rebuilt = Sponge.dataManager().deserialize(type, read)
            .orElseThrow(() -> new IllegalStateException("builder returned empty"));
        final @Nullable String rebuiltDiff = LosslessSupport.difference(original, rebuilt.toContainer());
        if (rebuiltDiff != null) {
            return "LOSSY: rebuilt " + rebuiltDiff + (containerDiff == null ? "" : "; container " + containerDiff);
        }
        return containerDiff == null ? "PASS" : "PASS (container: " + containerDiff + ")";
    }

    static String[] evaluate(final Row row) {
        final DataContainer original;
        try {
            original = row.value().get().toContainer();
        } catch (final Throwable t) {
            final String e = "ERROR: toContainer: " + LosslessMatrixTest.error(t).substring("ERROR: ".length());
            return new String[] {e, e, e, e};
        }
        final List<String> cells = new ArrayList<>();
        cells.add(LosslessMatrixTest.cell(() -> SerializableRoundTripTest.rebuild(row.type(), original, original.copy())));
        for (final LosslessSupport.Format format : LosslessSupport.Format.values()) {
            cells.add(LosslessMatrixTest.cell(() -> SerializableRoundTripTest.rebuild(row.type(), original, format.roundTrip(original))));
        }
        return cells.toArray(String[]::new);
    }

    @Test
    @DisplayName("serializable table matches pinned statuses")
    void serializables() throws IOException {
        PluginRecord.register();
        final PinnedTable table = new PinnedTable("lossless-serializables", SerializableRoundTripTest.COLUMNS);
        for (final Row row : SerializableRoundTripTest.rows()) {
            table.row(row.name(), SerializableRoundTripTest.evaluate(row));
        }
        table.assertMatches(SerializableRoundTripTest.EXPECTED);
    }

    static final Map<String, String> EXPECTED_UPDATER = new LinkedHashMap<>();

    static {
        EXPECTED_UPDATER.put("PluginRecord version 1 (updated to 2)", "PPPP");
        EXPECTED_UPDATER.put("PluginRecord version 2", "PPPP");
    }

    static String record(final DataContainer read) {
        final PluginRecord expected = PluginRecord.sample();
        final PluginRecord rebuilt = Sponge.dataManager().deserialize(PluginRecord.class, read)
            .orElseThrow(() -> new IllegalStateException("builder returned empty"));
        if (expected.equals(rebuilt)) {
            return "PASS";
        }
        return "LOSSY: " + LosslessSupport.difference(expected.toContainer(), rebuilt.toContainer());
    }

    @Test
    @DisplayName("content updater runs after each format and the builder sees the same values")
    void contentUpdater() throws IOException {
        PluginRecord.register();
        final PinnedTable table = new PinnedTable("lossless-content-updater", SerializableRoundTripTest.COLUMNS);
        final Map<String, DataContainer> inputs = new LinkedHashMap<>();
        inputs.put("PluginRecord version 1 (updated to 2)", PluginRecord.sample().toVersion1Container());
        inputs.put("PluginRecord version 2", PluginRecord.sample().toContainer());
        inputs.forEach((name, container) -> {
            final List<String> cells = new ArrayList<>();
            cells.add(LosslessMatrixTest.cell(() -> SerializableRoundTripTest.record(container.copy())));
            for (final LosslessSupport.Format format : LosslessSupport.Format.values()) {
                cells.add(LosslessMatrixTest.cell(() -> SerializableRoundTripTest.record(format.roundTrip(container))));
            }
            table.row(name, cells.toArray(String[]::new));
        });
        table.assertMatches(SerializableRoundTripTest.EXPECTED_UPDATER);
    }
}
