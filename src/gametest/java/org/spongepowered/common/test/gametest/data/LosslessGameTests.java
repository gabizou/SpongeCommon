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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormat;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataSerializable;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.StringDataFormat;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.common.data.SpongeDataManager;
import org.spongepowered.common.data.SpongeDataRegistration;
import org.spongepowered.common.test.gametest.GameTest;
import org.spongepowered.common.test.gametest.GameTestGroup;
import org.spongepowered.common.util.VecHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lossless reproducers that need a world, the gametest half of
 * {@code src/test/java/org/spongepowered/common/data/lossless}. Each test builds a table of statuses
 * (PASS, LOSSY or ERROR, as in the unit tests), prints it, and fails if it differs from the pinned one.
 *
 * <ul>
 *     <li>custom data on an entity, through the entity's own save and load, with the saved tag written
 *         and read with {@link NbtIo} as a chunk would be</li>
 *     <li>{@link EntitySnapshot}, {@link BlockSnapshot} and {@link ServerLocation}: {@code toContainer()},
 *         each data format, then their {@code DataBuilder}</li>
 * </ul>
 */
@GameTestGroup("lossless")
public final class LosslessGameTests {

    enum Plain { ALPHA, BETA }

    record Custom<T>(String name, Key<? extends Value<T>> key, T value) {}

    private static final List<Custom<?>> CUSTOM = new ArrayList<>();
    private static boolean registered;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void custom(final String name, final Key key, final Object value) {
        LosslessGameTests.CUSTOM.add(new Custom(name, key, value));
    }

    private static ResourceKey key(final String value) {
        return ResourceKey.of("lossless_gametest", value);
    }

    private static synchronized List<Custom<?>> customKeys() {
        if (LosslessGameTests.registered) {
            return LosslessGameTests.CUSTOM;
        }
        LosslessGameTests.registered = true;
        LosslessGameTests.custom("Boolean", Key.from(LosslessGameTests.key("boolean"), Boolean.class), true);
        LosslessGameTests.custom("Byte", Key.from(LosslessGameTests.key("byte"), Byte.class), (byte) -7);
        LosslessGameTests.custom("Long (fits int)", Key.from(LosslessGameTests.key("long_small"), Long.class), 5L);
        LosslessGameTests.custom("Float", Key.from(LosslessGameTests.key("float"), Float.class), 0.1f);
        LosslessGameTests.custom("Double -0.0", Key.from(LosslessGameTests.key("double_neg_zero"), Double.class), -0.0d);
        LosslessGameTests.custom("String", Key.from(LosslessGameTests.key("string"), String.class), "héllo");
        LosslessGameTests.custom("String 70k chars", Key.from(LosslessGameTests.key("string_long"), String.class), "x".repeat(70_000));
        LosslessGameTests.custom("List<Boolean>", Key.fromList(LosslessGameTests.key("list_boolean"), Boolean.class), List.of(true, false));
        LosslessGameTests.custom("Map<String,Boolean>", Key.fromMap(LosslessGameTests.key("map_string_boolean"), String.class, Boolean.class), Map.of("on", true));
        for (final Custom<?> c : LosslessGameTests.CUSTOM) {
            SpongeDataManager.INSTANCE.registerCustomDataRegistration((SpongeDataRegistration) DataRegistration.of(c.key(), Entity.class));
        }
        SpongeDataManager.INSTANCE.registerCustomDataRegistration((SpongeDataRegistration) DataRegistration.of(LosslessGameTests.PLAIN_ENUM, Entity.class));
        return LosslessGameTests.CUSTOM;
    }

    static final Map<String, String> EXPECTED_ENTITY_CUSTOM_DATA = new LinkedHashMap<>();

    static {
        EXPECTED_ENTITY_CUSTOM_DATA.put("Boolean", "P");
        EXPECTED_ENTITY_CUSTOM_DATA.put("Byte", "P");
        EXPECTED_ENTITY_CUSTOM_DATA.put("Long (fits int)", "P");
        EXPECTED_ENTITY_CUSTOM_DATA.put("Float", "P");
        EXPECTED_ENTITY_CUSTOM_DATA.put("Double -0.0", "L");
        EXPECTED_ENTITY_CUSTOM_DATA.put("String", "P");
        EXPECTED_ENTITY_CUSTOM_DATA.put("String 70k chars", "L");
        EXPECTED_ENTITY_CUSTOM_DATA.put("List<Boolean>", "L");
        EXPECTED_ENTITY_CUSTOM_DATA.put("Map<String,Boolean>", "P");
    }

    @GameTest
    void entityCustomDataSurvivesSaveAndLoad(final GameTestHelper helper) {
        final Map<String, String> observed = new LinkedHashMap<>();
        final List<Custom<?>> keys = LosslessGameTests.customKeys();
        final net.minecraft.world.entity.animal.Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        final Entity entity = (Entity) pig;
        for (final Custom<?> c : keys) {
            observed.put(c.name(), LosslessGameTests.cell(() -> LosslessGameTests.offer(entity, c)));
        }
        final String saved = LosslessGameTests.cell(() -> {
            final var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
            pig.saveWithoutId(output);
            final CompoundTag written = LosslessGameTests.nbtIo(output.buildResult());
            final net.minecraft.world.entity.animal.Pig loaded = EntityType.PIG.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), written));
            for (final Custom<?> c : keys) {
                if (observed.get(c.name()).startsWith("PASS")) {
                    observed.put(c.name(), LosslessGameTests.compare(c.value(), ((Entity) loaded).get(c.key())));
                }
            }
            return "PASS";
        });
        if (!saved.startsWith("PASS")) {
            keys.forEach(c -> observed.put(c.name(), saved));
        }
        LosslessGameTests.check(helper, "entity custom data", observed, LosslessGameTests.EXPECTED_ENTITY_CUSTOM_DATA);
    }

    private static <T> String offer(final Entity entity, final Custom<T> c) {
        if (!entity.offer(c.key(), c.value()).isSuccessful()) {
            return "ERROR: offer rejected";
        }
        return "PASS";
    }

    /** Writes the tag with {@link NbtIo} and reads it back, as chunk saving does. */
    private static CompoundTag nbtIo(final CompoundTag tag) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            NbtIo.write(tag, out);
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return NbtIo.read(in);
        }
    }

    private static final Key<Value<Plain>> PLAIN_ENUM = Key.from(LosslessGameTests.key("plain_enum"), Plain.class);

    static final Map<String, String> EXPECTED_UNSAVABLE = new LinkedHashMap<>();

    static {
        EXPECTED_UNSAVABLE.put("offer enum (plain)", "E");
        EXPECTED_UNSAVABLE.put("save entity", "E");
    }

    // A value NBT cannot hold: offer throws, but the value stays on the entity, so every later save throws too (X16)
    @GameTest
    void entityWithUnsavableCustomData(final GameTestHelper helper) {
        LosslessGameTests.customKeys();
        final Map<String, String> observed = new LinkedHashMap<>();
        final net.minecraft.world.entity.animal.Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        observed.put("offer enum (plain)", LosslessGameTests.cell(() -> LosslessGameTests.offer((Entity) pig,
            new Custom<>("enum (plain)", LosslessGameTests.PLAIN_ENUM, Plain.BETA))));
        observed.put("save entity", LosslessGameTests.cell(() -> {
            final var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
            pig.saveWithoutId(output);
            return "PASS";
        }));
        LosslessGameTests.check(helper, "unsavable custom data", observed, LosslessGameTests.EXPECTED_UNSAVABLE);
    }

    static final Map<String, String> EXPECTED_SERIALIZABLES = new LinkedHashMap<>();

    static {
        EXPECTED_SERIALIZABLES.put("ServerLocation", "LLLL");
        EXPECTED_SERIALIZABLES.put("BlockSnapshot chest", "PPPP");
        EXPECTED_SERIALIZABLES.put("EntitySnapshot pig", "PPLL");
    }

    @GameTest
    void worldSerializablesRoundTrip(final GameTestHelper helper) {
        final ServerWorld world = (ServerWorld) helper.getLevel();
        final BlockPos chest = new BlockPos(1, 1, 1);
        helper.setBlock(chest, Blocks.CHEST);
        final net.minecraft.world.entity.animal.Pig pig = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 1));
        final Map<String, DataSerializable> values = new LinkedHashMap<>();
        values.put("ServerLocation", world.location(VecHelper.toVector3d(helper.absolutePos(chest)).add(0.25, 0, 0.5)));
        values.put("BlockSnapshot chest", world.createSnapshot(VecHelper.toVector3i(helper.absolutePos(chest))));
        values.put("EntitySnapshot pig", ((Entity) pig).createSnapshot());
        final Map<String, Class<? extends DataSerializable>> types = Map.of(
            "ServerLocation", ServerLocation.class, "BlockSnapshot chest", BlockSnapshot.class, "EntitySnapshot pig", EntitySnapshot.class);
        final Map<String, String> observed = new LinkedHashMap<>();
        values.forEach((name, value) -> {
            final DataContainer original = value.toContainer();
            final StringBuilder codes = new StringBuilder();
            for (final String format : List.of("memory", "nbt", "json", "hocon")) {
                final String cell = LosslessGameTests.cell(() -> {
                    final DataContainer read = format.equals("memory") ? original.copy() : LosslessGameTests.roundTrip(format, original);
                    final DataSerializable rebuilt = org.spongepowered.api.Sponge.dataManager().deserialize(types.get(name), read)
                        .orElseThrow(() -> new IllegalStateException("builder returned empty"));
                    final @Nullable String diff = LosslessGameTests.difference(original, rebuilt.toContainer());
                    return diff == null ? "PASS" : "LOSSY: rebuilt " + diff;
                });
                System.out.println("[lossless] " + name + " / " + format + ": " + cell);
                codes.append(cell.substring(0, 1));
            }
            observed.put(name, codes.toString());
        });
        LosslessGameTests.check(helper, "world serializables", observed, LosslessGameTests.EXPECTED_SERIALIZABLES);
    }

    private static DataContainer roundTrip(final String format, final DataView view) throws IOException {
        return switch (format) {
            case "nbt" -> {
                final DataFormat nbt = DataFormats.NBT.get();
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                nbt.writeTo(out, view);
                yield nbt.readFrom(new ByteArrayInputStream(out.toByteArray()));
            }
            case "json" -> LosslessGameTests.string(DataFormats.JSON.get(), view);
            case "hocon" -> LosslessGameTests.string(DataFormats.HOCON.get(), view);
            default -> throw new IllegalArgumentException(format);
        };
    }

    private static DataContainer string(final StringDataFormat format, final DataView view) throws IOException {
        return format.read(format.write(view));
    }

    interface Cell {
        String run() throws Exception;
    }

    private static String cell(final Cell cell) {
        try {
            return cell.run();
        } catch (final Throwable t) {
            return "ERROR: " + t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }

    private static String compare(final Object expected, final Optional<?> actual) {
        if (actual.isEmpty()) {
            return "LOSSY: absent after load";
        }
        final @Nullable String d = LosslessGameTests.difference(expected, actual.get());
        return d == null ? "PASS" : "LOSSY: " + d;
    }

    private static void check(final GameTestHelper helper, final String what, final Map<String, String> observed, final Map<String, String> expected) {
        final Map<String, String> codes = new LinkedHashMap<>();
        observed.forEach((k, v) -> codes.put(k, v.length() <= 4 && v.chars().allMatch(ch -> "PLE-".indexOf(ch) >= 0) ? v : v.substring(0, 1)));
        observed.forEach((k, v) -> System.out.println("[lossless] " + what + ": " + k + " -> " + v));
        if (!codes.equals(expected)) {
            final StringBuilder java = new StringBuilder("[lossless] observed " + what + ":\n");
            codes.forEach((k, v) -> java.append("        EXPECTED.put(\"").append(k).append("\", \"").append(v).append("\");\n"));
            System.out.println(java);
            helper.fail(net.minecraft.network.chat.Component.literal(what + ": expected " + expected + " but was " + codes));
        }
        helper.succeed();
    }

    /** As {@code LosslessSupport.difference} in the unit tests: runtime classes must match. */
    private static @Nullable String difference(final @Nullable Object expected, final @Nullable Object actual) {
        if (expected == null || actual == null) {
            return expected == actual ? null : expected + " -> " + actual;
        }
        if (expected instanceof DataView ev) {
            if (!(actual instanceof DataView av)) {
                return "DataView -> " + actual.getClass().getSimpleName();
            }
            final Set<String> ek = ev.keys(false).stream().map(DataQuery::toString).collect(Collectors.toSet());
            final Set<String> ak = av.keys(false).stream().map(DataQuery::toString).collect(Collectors.toSet());
            if (!ek.equals(ak)) {
                return "keys " + ek + " -> " + ak;
            }
            for (final DataQuery k : ev.keys(false)) {
                final @Nullable String d = LosslessGameTests.difference(ev.get(k).orElse(null), av.get(k).orElse(null));
                if (d != null) {
                    return k + ": " + d;
                }
            }
            return null;
        }
        if (expected instanceof Map<?, ?> em && actual instanceof Map<?, ?> am) {
            if (!em.keySet().equals(am.keySet())) {
                return "keys " + em.keySet() + " -> " + am.keySet();
            }
            for (final Map.Entry<?, ?> e : em.entrySet()) {
                final @Nullable String d = LosslessGameTests.difference(e.getValue(), am.get(e.getKey()));
                if (d != null) {
                    return e.getKey() + ": " + d;
                }
            }
            return null;
        }
        if (expected instanceof List<?> el && actual instanceof List<?> al) {
            if (el.size() != al.size()) {
                return "size " + el.size() + " -> " + al.size();
            }
            for (int i = 0; i < el.size(); i++) {
                final @Nullable String d = LosslessGameTests.difference(el.get(i), al.get(i));
                if (d != null) {
                    return "[" + i + "] " + d;
                }
            }
            return null;
        }
        if (expected.getClass() != actual.getClass()) {
            return expected.getClass().getSimpleName() + "(" + LosslessGameTests.shorten(expected) + ") -> "
                + actual.getClass().getSimpleName() + "(" + LosslessGameTests.shorten(actual) + ")";
        }
        if (expected.getClass().isArray()) {
            final int len = Array.getLength(expected);
            if (len != Array.getLength(actual)) {
                return "length " + len + " -> " + Array.getLength(actual);
            }
            for (int i = 0; i < len; i++) {
                if (!Objects.equals(Array.get(expected, i), Array.get(actual, i))) {
                    return "[" + i + "] " + Array.get(expected, i) + " -> " + Array.get(actual, i);
                }
            }
            return null;
        }
        return expected.equals(actual) ? null : LosslessGameTests.shorten(expected) + " -> " + LosslessGameTests.shorten(actual);
    }

    private static String shorten(final Object o) {
        final String s = String.valueOf(o);
        return s.length() > 24 ? s.substring(0, 24) + "...(" + s.length() + ")" : s;
    }
}
