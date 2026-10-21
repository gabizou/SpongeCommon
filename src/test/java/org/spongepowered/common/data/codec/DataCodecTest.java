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
package org.spongepowered.common.data.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.spongepowered.api.data.persistence.DataCodec;
import org.spongepowered.api.data.persistence.DataCodecException;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.FieldCodec;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.RecordCodec;
import org.spongepowered.common.data.CompoundTagDataContainer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@DisplayName("DataCodec")
class DataCodecTest {

    enum Kind { CAT, DOG }

    record Pet(String name, Optional<Integer> age, Kind kind, List<String> tags, UUID id, boolean tame) {
        static final FieldCodec<Pet, String> NAME = DataCodec.STRING.fieldOf("name", Pet::name);
        static final FieldCodec<Pet, Optional<Integer>> AGE = DataCodec.INT.optionalFieldOf("age", Pet::age);
        static final FieldCodec<Pet, Kind> KIND = DataCodec.ofEnum(Kind.class).fieldOf("kind", Pet::kind);
        static final FieldCodec<Pet, List<String>> TAGS = DataCodec.STRING.listOf().optionalFieldOf("tags", List.of(), Pet::tags);
        static final FieldCodec<Pet, UUID> ID = DataCodec.UUID.fieldOf("id", Pet::id);
        static final FieldCodec<Pet, Boolean> TAME = DataCodec.BOOLEAN.optionalFieldOf("tame", false, Pet::tame);
        static final RecordCodec<Pet> CODEC = DataCodec.record(
            f -> new Pet(f.get(NAME), f.get(AGE), f.get(KIND), f.get(TAGS), f.get(ID), f.get(TAME)), NAME, AGE, KIND, TAGS, ID, TAME);
    }

    record Owner(String name, List<Pet> pets) {
        static final FieldCodec<Owner, String> NAME = DataCodec.STRING.fieldOf("name", Owner::name);
        static final FieldCodec<Owner, List<Pet>> PETS = Pet.CODEC.listOf().fieldOf("pets", Owner::pets);
        static final RecordCodec<Owner> CODEC = DataCodec.record(f -> new Owner(f.get(NAME), f.get(PETS)), NAME, PETS);
    }

    private static final UUID ID = new UUID(0x0123456789abcdefL, 0xfedcba9876543210L);
    private static final Pet REX = new Pet("Rex", Optional.of(3), Kind.DOG, List.of("good", "a.b"), DataCodecTest.ID, true);
    private static final Pet TOM = new Pet("Tom", Optional.empty(), Kind.CAT, List.of(), DataCodecTest.ID, false);
    private static final Owner ALEX = new Owner("Alex", List.of(DataCodecTest.REX, DataCodecTest.TOM));

    static Stream<Arguments> roundTrips() {
        return Stream.of(
            Arguments.of("in memory", (UnaryOperator<DataContainer>) c -> c),
            Arguments.of("JSON", (UnaryOperator<DataContainer>) DataCodecTest::json),
            Arguments.of("NBT", (UnaryOperator<DataContainer>) DataCodecTest::nbt),
            Arguments.of("CompoundTagDataContainer", (UnaryOperator<DataContainer>) DataCodecTest::compound)
        );
    }

    static Stream<Arguments> views() {
        return Stream.of(
            Arguments.of("MemoryDataContainer", (Supplier<DataContainer>) DataContainer::createNew),
            Arguments.of("CompoundTagDataContainer", (Supplier<DataContainer>) () -> new CompoundTagDataContainer(new CompoundTag()))
        );
    }

    static DataContainer json(final DataContainer container) {
        try {
            return DataFormats.JSON.get().read(DataFormats.JSON.get().write(container));
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    static DataContainer nbt(final DataContainer container) {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataFormats.NBT.get().writeTo(out, container);
            return DataFormats.NBT.get().readFrom(new ByteArrayInputStream(out.toByteArray()));
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    static DataContainer compound(final DataContainer container) {
        final DataContainer target = new CompoundTagDataContainer(new CompoundTag());
        container.keys(false).forEach(key -> target.set(key, container.get(key).orElseThrow()));
        return target;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roundTrips")
    @DisplayName("records round-trip unchanged")
    void recordsRoundTrip(final String name, final UnaryOperator<DataContainer> roundTrip) {
        final DataContainer encoded = Owner.CODEC.encode(DataCodecTest.ALEX);
        assertEquals(DataCodecTest.ALEX, Owner.CODEC.decode(roundTrip.apply(encoded)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roundTrips")
    @DisplayName("primitive values round-trip unchanged, whatever width the format reads them back at")
    void primitivesRoundTrip(final String name, final UnaryOperator<DataContainer> roundTrip) {
        final DataContainer c = DataContainer.createNew();
        DataCodec.BYTE.write(c, DataQuery.of("byte"), (byte) -7);
        DataCodec.SHORT.write(c, DataQuery.of("short"), (short) 300);
        DataCodec.INT.write(c, DataQuery.of("int"), Integer.MIN_VALUE);
        DataCodec.LONG.write(c, DataQuery.of("smallLong"), 1L);
        DataCodec.LONG.write(c, DataQuery.of("long"), Long.MAX_VALUE);
        DataCodec.FLOAT.write(c, DataQuery.of("float"), 0.1f);
        DataCodec.DOUBLE.write(c, DataQuery.of("double"), Double.MAX_VALUE);
        DataCodec.BOOLEAN.write(c, DataQuery.of("bool"), true);
        DataCodec.STRING.write(c, DataQuery.of("string"), "héllo ✓");
        DataCodec.INT_ARRAY.write(c, DataQuery.of("ints"), new int[] {1, -1});
        DataCodec.LONG_ARRAY.write(c, DataQuery.of("longs"), new long[] {1L, Long.MIN_VALUE});
        DataCodec.BYTE_ARRAY.write(c, DataQuery.of("bytes"), new byte[] {1, -128});
        DataCodec.UUID.write(c, DataQuery.of("uuid"), DataCodecTest.ID);
        final DataContainer read = roundTrip.apply(c);
        assertEquals(Optional.of((byte) -7), DataCodec.BYTE.read(read, DataQuery.of("byte")));
        assertEquals(Optional.of((short) 300), DataCodec.SHORT.read(read, DataQuery.of("short")));
        assertEquals(Optional.of(Integer.MIN_VALUE), DataCodec.INT.read(read, DataQuery.of("int")));
        assertEquals(Optional.of(1L), DataCodec.LONG.read(read, DataQuery.of("smallLong")));
        assertEquals(Optional.of(Long.MAX_VALUE), DataCodec.LONG.read(read, DataQuery.of("long")));
        assertEquals(Optional.of(0.1f), DataCodec.FLOAT.read(read, DataQuery.of("float")));
        assertEquals(Optional.of(Double.MAX_VALUE), DataCodec.DOUBLE.read(read, DataQuery.of("double")));
        assertEquals(Optional.of(true), DataCodec.BOOLEAN.read(read, DataQuery.of("bool")));
        assertEquals(Optional.of("héllo ✓"), DataCodec.STRING.read(read, DataQuery.of("string")));
        assertArrayEquals(new int[] {1, -1}, DataCodec.INT_ARRAY.read(read, DataQuery.of("ints")).orElseThrow());
        assertArrayEquals(new long[] {1L, Long.MIN_VALUE}, DataCodec.LONG_ARRAY.read(read, DataQuery.of("longs")).orElseThrow());
        assertArrayEquals(new byte[] {1, -128}, DataCodec.BYTE_ARRAY.read(read, DataQuery.of("bytes")).orElseThrow());
        assertEquals(Optional.of(DataCodecTest.ID), DataCodec.UUID.read(read, DataQuery.of("uuid")));
    }

    @Test
    @DisplayName("optional and default fields are omitted when empty or default, and read back")
    void absenceIsAnOmittedKey() {
        final DataContainer tom = Pet.CODEC.encode(DataCodecTest.TOM);
        assertEquals(List.of("name", "kind", "id"), tom.keys(false).stream().map(DataQuery::toString).toList());
        assertEquals(DataCodecTest.TOM, Pet.CODEC.decode(tom));
    }

    @Test
    @DisplayName("a missing required field is reported with its path")
    void missingRequiredField() {
        final DataContainer encoded = Owner.CODEC.encode(DataCodecTest.ALEX);
        encoded.remove(DataQuery.of("pets")).set(DataQuery.of("pets"), List.of(Pet.CODEC.encode(DataCodecTest.REX).remove(DataQuery.of("name"))));
        final DataCodecException e = assertThrows(DataCodecException.class, () -> Owner.CODEC.decode(encoded));
        assertEquals("Expected a value at 'pets[0].name' but found nothing", e.getMessage());
        assertEquals("pets[0].name", e.path());
    }

    @Test
    @DisplayName("a malformed nested value is reported with its full path, and never converted")
    void malformedNestedValue() {
        final DataContainer encoded = Owner.CODEC.encode(DataCodecTest.ALEX);
        final List<DataView> pets = encoded.getViewList(DataQuery.of("pets")).orElseThrow();
        pets.get(1).set(DataQuery.of("age"), "7");
        encoded.set(DataQuery.of("pets"), pets);
        final DataCodecException e = assertThrows(DataCodecException.class, () -> Owner.CODEC.decode(encoded));
        assertEquals("Expected an int at 'pets[1].age' but found String: 7", e.getMessage());
        assertInstanceOf(InvalidDataException.class, e);
    }

    @Test
    @DisplayName("enums, UUIDs and lists report bad values precisely")
    void elementErrors() {
        final DataContainer pet = Pet.CODEC.encode(DataCodecTest.REX);
        pet.set(DataQuery.of("kind"), "BIRD");
        assertEquals("Expected one of [CAT, DOG] at 'kind' but found String: BIRD",
            assertThrows(DataCodecException.class, () -> Pet.CODEC.decode(pet)).getMessage());
        pet.set(DataQuery.of("kind"), "CAT").set(DataQuery.of("tags"), List.of("ok", 5));
        assertEquals("Expected a String at 'tags[1]' but found Integer: 5",
            assertThrows(DataCodecException.class, () -> Pet.CODEC.decode(pet)).getMessage());
        pet.set(DataQuery.of("tags"), List.of()).set(DataQuery.of("id"), new int[] {1, 2, 3});
        assertTrue(assertThrows(DataCodecException.class, () -> Pet.CODEC.decode(pet)).getMessage().startsWith("Expected a UUID"));
    }

    @Test
    @DisplayName("UUIDs are written as four ints like Minecraft, and also read from strings and UuidMost/UuidLeast views")
    void uuidForms() {
        final int[] ints = (int[]) DataCodec.UUID.encode(DataCodecTest.ID);
        assertArrayEquals(new int[] {0x01234567, 0x89abcdef, 0xfedcba98, 0x76543210}, ints);
        assertEquals(DataCodecTest.ID, DataCodec.UUID.decode(DataCodecTest.ID.toString()));
        final DataContainer legacy = DataContainer.createNew()
            .set(DataQuery.of("UuidMost"), DataCodecTest.ID.getMostSignificantBits())
            .set(DataQuery.of("UuidLeast"), DataCodecTest.ID.getLeastSignificantBits());
        assertEquals(DataCodecTest.ID, DataCodec.UUID.decode(legacy));
        assertThrows(DataCodecException.class, () -> DataCodec.UUID.decode("not-a-uuid"));
    }

    @Test
    @DisplayName("byte[] and long[] are written as lists, and native arrays are still read")
    void arrayForms() {
        assertEquals(List.of((byte) 1, (byte) 2), DataCodec.BYTE_ARRAY.encode(new byte[] {1, 2}));
        assertEquals(List.of(1L, 2L), DataCodec.LONG_ARRAY.encode(new long[] {1L, 2L}));
        assertArrayEquals(new byte[] {1, 2}, DataCodec.BYTE_ARRAY.decode(new byte[] {1, 2}));
        assertArrayEquals(new long[] {1L, 2L}, DataCodec.LONG_ARRAY.decode(new long[] {1L, 2L}));
        assertArrayEquals(new long[] {1L, 2L}, DataCodec.LONG_ARRAY.decode(new int[] {1, 2}));
        assertEquals("Expected a byte at '[1]' but found Integer: 300",
            assertThrows(DataCodecException.class, () -> DataCodec.BYTE_ARRAY.decode(List.of(1, 300))).getMessage());
    }

    @Test
    @DisplayName("maps with String keys are stored as views; invalid keys are rejected")
    void maps() {
        final DataCodec<Map<String, Integer>> codec = DataCodec.mapOf(DataCodec.INT);
        final Object encoded = codec.encode(Map.of("a.b", 1));
        assertInstanceOf(DataView.class, encoded);
        assertEquals(Map.of("a.b", 1), codec.decode(DataCodecTest.json((DataContainer) encoded)));
        assertEquals("Expected a valid key (not empty, not containing '$Boolean', not '_arraytype') at 'x$Boolean' but found String: x$Boolean",
            assertThrows(DataCodecException.class, () -> codec.encode(Map.of("x$Boolean", 1))).getMessage());
        assertThrows(DataCodecException.class, () -> codec.encode(Map.of("_arraytype", 1)));
        assertThrows(DataCodecException.class, () -> codec.encode(Map.of("", 1)));
    }

    @Test
    @DisplayName("encoding rejects values that would be lost or fail when saved")
    void encodeRejectsUnportableValues() {
        final DataContainer c = DataContainer.createNew();
        assertEquals("Expected a finite number at 'd' but found Double: NaN",
            assertThrows(DataCodecException.class, () -> DataCodec.DOUBLE.write(c, DataQuery.of("d"), Double.NaN)).getMessage());
        assertThrows(DataCodecException.class, () -> DataCodec.FLOAT.write(c, DataQuery.of("f"), Float.POSITIVE_INFINITY));
        assertEquals("At 's': String is 65536 bytes in modified UTF-8; at most 65535 can be stored",
            assertThrows(DataCodecException.class, () -> DataCodec.STRING.write(c, DataQuery.of("s"), "x".repeat(65_536))).getMessage());
        // three-byte characters count three times
        assertThrows(DataCodecException.class, () -> DataCodec.STRING.write(c, DataQuery.of("s"), "✓".repeat(21_846)));
        DataCodec.STRING.write(c, DataQuery.of("s"), "x".repeat(65_535));
        // a custom codec that produces an unportable value is caught when it is written
        final DataCodec<Short> shortArray = DataCodec.of(s -> new short[] {s}, d -> ((short[]) d)[0]);
        assertTrue(assertThrows(DataCodecException.class, () -> shortArray.write(c, DataQuery.of("x"), (short) 1)).getMessage()
            .startsWith("Expected a portable value"));
        assertFalse(c.contains(DataQuery.of("d")) || c.contains(DataQuery.of("f")) || c.contains(DataQuery.of("x")));
    }

    @Test
    @DisplayName("field keys are checked when the field is created")
    void invalidFieldKeys() {
        assertThrows(IllegalArgumentException.class, () -> DataCodec.INT.fieldOf("flag$Boolean", (Pet p) -> 1));
        assertThrows(IllegalArgumentException.class, () -> DataCodec.INT.fieldOf("_arraytype", (Pet p) -> 1));
        assertThrows(IllegalArgumentException.class, () -> DataCodec.INT.fieldOf("", (Pet p) -> 1));
        assertThrows(IllegalArgumentException.class, () -> DataCodec.record(p -> null, Pet.NAME, DataCodec.STRING.fieldOf("name", Pet::name)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("views")
    @DisplayName("read and write through a non-view value throw instead of replacing or hiding it")
    void pathsThroughScalars(final String name, final Supplier<DataContainer> factory) {
        final DataContainer c = factory.get().set(DataQuery.of("a"), 1);
        assertEquals("Expected a DataView at 'a' but found Integer: 1",
            assertThrows(DataCodecException.class, () -> DataCodec.INT.write(c, DataQuery.of("a", "b"), 2)).getMessage());
        assertEquals(Optional.of(1), c.getInt(DataQuery.of("a")));
        assertThrows(DataCodecException.class, () -> DataCodec.INT.read(c, DataQuery.of("a", "b")));
        DataCodec.INT.write(c, DataQuery.of("x", "y", "z"), 3);
        assertEquals(Optional.of(3), DataCodec.INT.read(c, DataQuery.of("x", "y", "z")));
        assertEquals(Optional.empty(), DataCodec.INT.read(c, DataQuery.of("missing", "z")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("views")
    @DisplayName("records can be written into either DataView implementation")
    void writeRecordIntoView(final String name, final Supplier<DataContainer> factory) {
        final DataContainer c = factory.get();
        Owner.CODEC.write(c, DataQuery.of("owner"), DataCodecTest.ALEX);
        assertEquals(Optional.of(DataCodecTest.ALEX), Owner.CODEC.read(c, DataQuery.of("owner")));
    }

    @Test
    @DisplayName("map() derives a codec and keeps error paths")
    void mappedCodec() {
        record Celsius(double degrees) { }
        final DataCodec<Celsius> codec = DataCodec.DOUBLE.map(Celsius::new, Celsius::degrees);
        final DataContainer c = DataContainer.createNew();
        codec.write(c, DataQuery.of("t"), new Celsius(21.5));
        assertEquals(Optional.of(new Celsius(21.5)), codec.read(c, DataQuery.of("t")));
        c.set(DataQuery.of("t"), "warm");
        assertEquals("Expected a double at 't' but found String: warm",
            assertThrows(DataCodecException.class, () -> codec.read(c, DataQuery.of("t"))).getMessage());
    }
}
