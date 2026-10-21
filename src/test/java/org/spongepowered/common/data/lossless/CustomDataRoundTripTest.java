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
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.common.data.SpongeDataManager;
import org.spongepowered.common.data.SpongeDataRegistration;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.io.IOException;
import java.time.Instant;
import java.time.Month;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Plugin custom data on a DataHolder: a key registered with {@code DataRegistration.of(key, ItemStack.class)}
 * (so a default {@code DataStore}), offered to an ItemStack. Reading it back goes through {@code DataUtil},
 * the item's custom data {@code CompoundTag}, {@code CompoundTagDataView} and {@code DataDeserializer}.
 *
 * <ul>
 *     <li>holder: {@code get(key)} right after {@code offer}</li>
 *     <li>deserialize: {@code DataManager.deserialize(ItemStack.class, stack.toContainer())} (the
 *         {@code DataBuilder}) then {@code get(key)}</li>
 *     <li>builder: {@code ItemStack.builder().fromContainer(stack.toContainer()).build()} then {@code get(key)}</li>
 *     <li>NBT / JSON / HOCON: as deserialize, with {@code toContainer()} written and read with that format first</li>
 * </ul>
 */
@DisplayName("Custom data: DataStore round trips through an ItemStack and each format")
class CustomDataRoundTripTest {

    enum Plain { ALPHA, BETA }

    record Row<T>(String name, Key<? extends Value<T>> key, T value) {}

    private static final List<Row<?>> ROWS = new ArrayList<>();
    private static boolean registered;

    private static <T> void add(final String name, final String id, final Class<T> type, final T value) {
        CustomDataRoundTripTest.ROWS.add(new Row<>(name, Key.from(ResourceKey.of("lossless", id), type), value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> void addList(final String name, final String id, final Class<T> type, final List<T> value) {
        CustomDataRoundTripTest.ROWS.add(new Row(name, Key.fromList(ResourceKey.of("lossless", id), type), value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> void addSet(final String name, final String id, final Class<T> type, final Set<T> value) {
        CustomDataRoundTripTest.ROWS.add(new Row(name, Key.fromSet(ResourceKey.of("lossless", id), type), value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <K, V> void addMap(final String name, final String id, final Class<K> k, final Class<V> v, final Map<K, V> value) {
        CustomDataRoundTripTest.ROWS.add(new Row(name, Key.fromMap(ResourceKey.of("lossless", id), k, v), value));
    }

    static synchronized List<Row<?>> rows() {
        if (CustomDataRoundTripTest.registered) {
            return CustomDataRoundTripTest.ROWS;
        }
        CustomDataRoundTripTest.registered = true;
        CustomDataRoundTripTest.add("Boolean", "boolean", Boolean.class, true);
        CustomDataRoundTripTest.add("Byte", "byte", Byte.class, (byte) -7);
        CustomDataRoundTripTest.add("Short", "short", Short.class, (short) 300);
        CustomDataRoundTripTest.add("Integer", "int", Integer.class, 42);
        CustomDataRoundTripTest.add("Long (fits int)", "long_small", Long.class, 5L);
        CustomDataRoundTripTest.add("Long MAX", "long_big", Long.class, Long.MAX_VALUE);
        CustomDataRoundTripTest.add("Float", "float", Float.class, 0.1f);
        CustomDataRoundTripTest.add("Double", "double", Double.class, 0.1d);
        CustomDataRoundTripTest.add("Double -0.0", "double_neg_zero", Double.class, -0.0d);
        CustomDataRoundTripTest.add("String", "string", String.class, "héllo");
        CustomDataRoundTripTest.add("String 70k chars", "string_long", String.class, "x".repeat(70_000));
        CustomDataRoundTripTest.add("UUID", "uuid", UUID.class, new UUID(5, 6));
        CustomDataRoundTripTest.add("Vector3d", "vector3d", Vector3d.class, new Vector3d(0.5, 64, -1));
        CustomDataRoundTripTest.add("Vector3i", "vector3i", Vector3i.class, new Vector3i(1, -64, 3));
        CustomDataRoundTripTest.add("Instant", "instant", Instant.class, Instant.ofEpochSecond(1_792_000_000L, 5));
        CustomDataRoundTripTest.add("Month (translated enum)", "month", Month.class, Month.MARCH);
        CustomDataRoundTripTest.add("enum (plain)", "plain_enum", Plain.class, Plain.BETA);
        CustomDataRoundTripTest.add("Component", "component", Component.class, Component.text("hi", NamedTextColor.GOLD));
        CustomDataRoundTripTest.add("ResourceKey", "resource_key", ResourceKey.class, ResourceKey.of("plugin", "thing"));
        CustomDataRoundTripTest.add("registry value (ItemType)", "item_type", ItemType.class, ItemTypes.DIAMOND.get());
        CustomDataRoundTripTest.add("ItemStackSnapshot", "snapshot", ItemStackSnapshot.class, ItemStack.of(ItemTypes.APPLE, 3).asImmutable());
        CustomDataRoundTripTest.add("DataView", "view", DataView.class, DataContainer.createNew().set(DataQuery.of("a"), 1).set(DataQuery.of("b"), true));
        CustomDataRoundTripTest.addList("List<Boolean>", "list_boolean", Boolean.class, List.of(true, false));
        CustomDataRoundTripTest.addList("List<Integer>", "list_int", Integer.class, List.of(1, 2));
        CustomDataRoundTripTest.addList("List<Long>", "list_long", Long.class, List.of(1L, Long.MAX_VALUE));
        CustomDataRoundTripTest.addList("List<Float>", "list_float", Float.class, List.of(1.5f));
        CustomDataRoundTripTest.addList("List<String>", "list_string", String.class, List.of("a", "b"));
        CustomDataRoundTripTest.addList("List<UUID>", "list_uuid", UUID.class, List.of(new UUID(1, 1)));
        CustomDataRoundTripTest.addSet("Set<String>", "set_string", String.class, new LinkedHashSet<>(List.of("a", "b")));
        final Map<String, Integer> ints = new LinkedHashMap<>();
        ints.put("a", 1);
        ints.put("b", 2);
        CustomDataRoundTripTest.addMap("Map<String,Integer>", "map_string_int", String.class, Integer.class, ints);
        CustomDataRoundTripTest.addMap("Map<String,Boolean>", "map_string_boolean", String.class, Boolean.class, Map.of("on", true));
        CustomDataRoundTripTest.addMap("Map<UUID,String>", "map_uuid_string", UUID.class, String.class, Map.of(new UUID(7, 8), "v"));
        for (final Row<?> row : CustomDataRoundTripTest.ROWS) {
            SpongeDataManager.INSTANCE.registerCustomDataRegistration((SpongeDataRegistration) DataRegistration.of(row.key(), ItemStack.class));
        }
        return CustomDataRoundTripTest.ROWS;
    }

    static final String[] COLUMNS = {"holder", "deserialize", "builder", "NBT", "JSON", "HOCON"};

    static final Map<String, String> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("Boolean", "PPLPPP");
        EXPECTED.put("Byte", "PPLPPP");
        EXPECTED.put("Short", "PPLPPP");
        EXPECTED.put("Integer", "PPLPPP");
        EXPECTED.put("Long (fits int)", "PPLPPP");
        EXPECTED.put("Long MAX", "PPLPPP");
        EXPECTED.put("Float", "PPLPPP");
        EXPECTED.put("Double", "PPLPPP");
        EXPECTED.put("Double -0.0", "PLLLLL");
        EXPECTED.put("String", "PPLPPP");
        EXPECTED.put("String 70k chars", "PPLLPP");
        EXPECTED.put("UUID", "PPLPPP");
        EXPECTED.put("Vector3d", "PPLPPP");
        EXPECTED.put("Vector3i", "PPLPPP");
        EXPECTED.put("Instant", "PPLPPP");
        EXPECTED.put("Month (translated enum)", "PPLPPP");
        EXPECTED.put("enum (plain)", "EEEEEE");
        EXPECTED.put("Component", "EEEEEE");
        EXPECTED.put("ResourceKey", "PPLPPP");
        EXPECTED.put("registry value (ItemType)", "PPLPPP");
        EXPECTED.put("ItemStackSnapshot", "PPLPPP");
        EXPECTED.put("DataView", "PLLLLL");
        EXPECTED.put("List<Boolean>", "PLLLLL");
        EXPECTED.put("List<Integer>", "PPLPPP");
        EXPECTED.put("List<Long>", "PPLPLL");
        EXPECTED.put("List<Float>", "PPLPLL");
        EXPECTED.put("List<String>", "PPLPPP");
        EXPECTED.put("List<UUID>", "PPLPPP");
        EXPECTED.put("Set<String>", "PPLPPP");
        EXPECTED.put("Map<String,Integer>", "PPLPPP");
        EXPECTED.put("Map<String,Boolean>", "PPLPPP");
        EXPECTED.put("Map<UUID,String>", "PPLPPP");
    }

    static String compare(final Object expected, final Optional<?> actual) {
        if (actual.isEmpty()) {
            return "LOSSY: absent after read";
        }
        final @Nullable String d = expected instanceof Set<?> || expected instanceof Component || expected instanceof ItemStackSnapshot
            ? (expected.equals(actual.get()) ? null : expected + " -> " + actual.get())
            : LosslessSupport.difference(expected, actual.get());
        return d == null ? "PASS" : "LOSSY: " + d;
    }

    static ItemStack deserialize(final DataView container) {
        return Sponge.dataManager().deserialize(ItemStack.class, container).orElseThrow(() -> new IllegalStateException("builder returned empty"));
    }

    static <T> String[] evaluate(final Row<T> row) {
        final ItemStack stack = ItemStack.of(ItemTypes.STONE);
        final List<String> cells = new ArrayList<>();
        final DataTransactionResult result;
        try {
            result = stack.offer(row.key(), row.value());
        } catch (final Throwable t) {
            final String e = LosslessMatrixTest.error(t);
            return new String[] {e, e, e, e, e, e};
        }
        if (!result.isSuccessful()) {
            return new String[] {"ERROR: offer rejected", "N/A", "N/A", "N/A", "N/A", "N/A"};
        }
        cells.add(LosslessMatrixTest.cell(() -> CustomDataRoundTripTest.compare(row.value(), stack.get(row.key()))));
        final DataContainer[] container = new DataContainer[1];
        cells.add(LosslessMatrixTest.cell(() -> {
            container[0] = stack.toContainer();
            return CustomDataRoundTripTest.compare(row.value(), CustomDataRoundTripTest.deserialize(container[0]).get(row.key()));
        }));
        cells.add(LosslessMatrixTest.cell(() -> {
            final ItemStack rebuilt = ItemStack.builder().fromContainer(stack.toContainer()).build();
            return CustomDataRoundTripTest.compare(row.value(), rebuilt.get(row.key()));
        }));
        for (final LosslessSupport.Format format : LosslessSupport.Format.values()) {
            cells.add(LosslessMatrixTest.cell(() -> {
                if (container[0] == null) {
                    return "N/A (toContainer failed)";
                }
                return CustomDataRoundTripTest.compare(row.value(), CustomDataRoundTripTest.deserialize(format.roundTrip(container[0])).get(row.key()));
            }));
        }
        return cells.toArray(String[]::new);
    }

    @Test
    @DisplayName("custom data table matches pinned statuses")
    void customData() throws IOException {
        final PinnedTable table = new PinnedTable("lossless-custom-data", CustomDataRoundTripTest.COLUMNS);
        for (final Row<?> row : CustomDataRoundTripTest.rows()) {
            table.row(row.name(), CustomDataRoundTripTest.evaluate(row));
        }
        table.assertMatches(CustomDataRoundTripTest.EXPECTED);
    }
}
