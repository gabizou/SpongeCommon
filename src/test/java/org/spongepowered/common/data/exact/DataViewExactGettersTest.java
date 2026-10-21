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
package org.spongepowered.common.data.exact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.common.data.CompoundTagDataContainer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Tests for the strict {@code exact*} getters on {@link DataView}, run against
 * both Sponge implementations.
 */
@DisplayName("DataView exact* getters")
class DataViewExactGettersTest {

    private static final DataQuery V = DataQuery.of("v");
    private static final DataQuery A = DataQuery.of("a");
    private static final DataQuery A_B = DataQuery.of("a", "b");

    static Stream<Arguments> views() {
        return Stream.of(
            Arguments.of("MemoryDataContainer", (Supplier<DataContainer>) DataContainer::createNew),
            Arguments.of("CompoundTagDataContainer", (Supplier<DataContainer>) () -> new CompoundTagDataContainer(new CompoundTag()))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("views")
    @DisplayName("missing values are absent")
    void missingIsAbsent(final String name, final Supplier<DataContainer> factory) {
        final DataContainer c = factory.get();
        assertEquals(Optional.empty(), c.exactBoolean(V));
        assertEquals(Optional.empty(), c.exactByte(V));
        assertEquals(Optional.empty(), c.exactShort(V));
        assertEquals(OptionalInt.empty(), c.exactInt(V));
        assertEquals(OptionalLong.empty(), c.exactLong(V));
        assertEquals(Optional.empty(), c.exactFloat(V));
        assertEquals(OptionalDouble.empty(), c.exactDouble(V));
        assertEquals(Optional.empty(), c.exactString(V));
        assertEquals(Optional.empty(), c.exactView(V));
        // a missing intermediate view is also just absent
        assertEquals(OptionalInt.empty(), c.exactInt(A_B));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("views")
    @DisplayName("values of the exact type are returned")
    void exactTypesAreReturned(final String name, final Supplier<DataContainer> factory) {
        final DataContainer c = factory.get()
            .set(DataQuery.of("byte"), (byte) -7)
            .set(DataQuery.of("short"), (short) 300)
            .set(DataQuery.of("int"), Integer.MIN_VALUE)
            .set(DataQuery.of("long"), Long.MAX_VALUE)
            .set(DataQuery.of("float"), 0.1f)
            .set(DataQuery.of("double"), -2.5d)
            .set(DataQuery.of("string"), "a.b")
            .set(A_B, 1);
        assertEquals(Optional.of((byte) -7), c.exactByte(DataQuery.of("byte")));
        assertEquals(Optional.of((short) 300), c.exactShort(DataQuery.of("short")));
        assertEquals(OptionalInt.of(Integer.MIN_VALUE), c.exactInt(DataQuery.of("int")));
        assertEquals(OptionalLong.of(Long.MAX_VALUE), c.exactLong(DataQuery.of("long")));
        assertEquals(Optional.of(0.1f), c.exactFloat(DataQuery.of("float")));
        // -0.0 is avoided here: CompoundTag stores it as 0.0 (DoubleTag.valueOf, X2), which is not what this test is about
        assertEquals(OptionalDouble.of(-2.5d), c.exactDouble(DataQuery.of("double")));
        assertEquals(Optional.of("a.b"), c.exactString(DataQuery.of("string")));
        assertEquals(OptionalInt.of(1), c.exactView(A).get().exactInt(DataQuery.of("b")));
        assertEquals(OptionalInt.of(1), c.exactInt(A_B));
    }

    @Test
    @DisplayName("exactBoolean returns a stored Boolean (MemoryDataContainer)")
    void exactBooleanOnMemory() {
        final DataContainer c = DataContainer.createNew().set(V, true);
        assertEquals(Optional.of(true), c.exactBoolean(V));
    }

    @Test
    @DisplayName("exactBoolean on CompoundTagDataContainer currently throws: booleans read back as Byte on api-17 (issue draft 04)")
    void exactBooleanOnCompoundUntilIssue04IsFixed() {
        final DataContainer c = new CompoundTagDataContainer(new CompoundTag()).set(V, true);
        // The stored value comes back as Byte(1) (X20). The strict getter refuses it instead of guessing;
        // the lenient getter hides it. Update this test when issue draft 04 is fixed.
        assertThrows(InvalidDataException.class, () -> c.exactBoolean(V));
        assertEquals(Optional.of(true), c.getBoolean(V));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("views")
    @DisplayName("integral values widen and narrow only within range")
    void integralRanges(final String name, final Supplier<DataContainer> factory) {
        final DataContainer c = factory.get()
            .set(DataQuery.of("smallLong"), 5L)
            .set(DataQuery.of("bigLong"), 1L << 40)
            .set(DataQuery.of("int300"), 300)
            .set(DataQuery.of("byte"), (byte) 1);
        assertEquals(OptionalInt.of(5), c.exactInt(DataQuery.of("smallLong")));
        assertEquals(Optional.of((byte) 5), c.exactByte(DataQuery.of("smallLong")));
        assertEquals(OptionalLong.of(1), c.exactLong(DataQuery.of("byte")));
        assertEquals(Optional.of((short) 300), c.exactShort(DataQuery.of("int300")));
        assertThrows(InvalidDataException.class, () -> c.exactInt(DataQuery.of("bigLong")));
        assertThrows(InvalidDataException.class, () -> c.exactByte(DataQuery.of("int300")));
        // the lenient getters truncate the same values
        assertEquals(Optional.of(0), c.getInt(DataQuery.of("bigLong")));
        assertEquals(Optional.of((byte) 44), c.getByte(DataQuery.of("int300")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("views")
    @DisplayName("values of the wrong type throw instead of being converted")
    void wrongTypesThrow(final String name, final Supplier<DataContainer> factory) {
        final DataContainer c = factory.get()
            .set(DataQuery.of("numericString"), "12")
            .set(DataQuery.of("emptyString"), "")
            .set(DataQuery.of("fraction"), 1.9d)
            .set(DataQuery.of("int"), 5)
            .set(DataQuery.of("byteOne"), (byte) 1)
            .set(A_B, 1);
        assertThrows(InvalidDataException.class, () -> c.exactInt(DataQuery.of("numericString")));
        assertThrows(InvalidDataException.class, () -> c.exactLong(DataQuery.of("emptyString")));
        assertThrows(InvalidDataException.class, () -> c.exactDouble(DataQuery.of("numericString")));
        assertThrows(InvalidDataException.class, () -> c.exactInt(DataQuery.of("fraction")));
        assertThrows(InvalidDataException.class, () -> c.exactString(DataQuery.of("int")));
        assertThrows(InvalidDataException.class, () -> c.exactBoolean(DataQuery.of("byteOne")));
        assertThrows(InvalidDataException.class, () -> c.exactView(DataQuery.of("int")));
        assertThrows(InvalidDataException.class, () -> c.exactInt(A));
        // what the lenient getters return for the same data
        assertEquals(Optional.of(12), c.getInt(DataQuery.of("numericString")));
        assertEquals(Optional.of(0L), c.getLong(DataQuery.of("emptyString")));
        assertEquals(Optional.of(1), c.getInt(DataQuery.of("fraction")));
        assertEquals(Optional.of("5"), c.getString(DataQuery.of("int")));
        assertEquals(Optional.of(true), c.getBoolean(DataQuery.of("byteOne")));
        assertEquals(Optional.empty(), c.getView(DataQuery.of("int")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("views")
    @DisplayName("a path through a non-view value throws, where get() reports absent")
    void pathThroughScalarThrows(final String name, final Supplier<DataContainer> factory) {
        final DataContainer c = factory.get().set(A, 1);
        final InvalidDataException e = assertThrows(InvalidDataException.class, () -> c.exactInt(A_B));
        assertTrue(e.getMessage().contains("'a'"), e.getMessage());
        assertTrue(e.getMessage().contains("a.b"), e.getMessage());
        assertEquals(Optional.empty(), c.get(A_B));
        assertEquals(Optional.empty(), c.getInt(A_B));
    }

    @Test
    @DisplayName("the exception message names the path, the expected type and what was found")
    void messageNamesPathAndValue() {
        final DataContainer c = DataContainer.createNew().set(A_B, "12");
        final InvalidDataException e = assertThrows(InvalidDataException.class, () -> c.exactInt(A_B));
        assertEquals("Expected an int at 'a.b' but found String: 12", e.getMessage());
    }

    @Test
    @DisplayName("floating point: Double narrows to float within range, integral values only when exact")
    void floatingPoint() {
        final DataContainer c = DataContainer.createNew()
            .set(DataQuery.of("d01"), 0.1d)
            .set(DataQuery.of("huge"), 1e300d)
            .set(DataQuery.of("nan"), Double.NaN)
            .set(DataQuery.of("f"), 1.5f)
            .set(DataQuery.of("pow30"), 1L << 30)
            .set(DataQuery.of("pow24plus1"), (1L << 24) + 1)
            .set(DataQuery.of("pow53plus1"), (1L << 53) + 1)
            .set(DataQuery.of("pow60"), 1L << 60)
            .set(DataQuery.of("longMax"), Long.MAX_VALUE);
        assertEquals(Optional.of(0.1f), c.exactFloat(DataQuery.of("d01")));
        assertThrows(InvalidDataException.class, () -> c.exactFloat(DataQuery.of("huge")));
        assertTrue(Float.isNaN(c.exactFloat(DataQuery.of("nan")).get()));
        assertEquals(OptionalDouble.of(1.5d), c.exactDouble(DataQuery.of("f")));
        assertEquals(Optional.of((float) (1L << 30)), c.exactFloat(DataQuery.of("pow30")));
        assertThrows(InvalidDataException.class, () -> c.exactFloat(DataQuery.of("pow24plus1")));
        assertThrows(InvalidDataException.class, () -> c.exactDouble(DataQuery.of("pow53plus1")));
        assertEquals(OptionalDouble.of(0x1p60), c.exactDouble(DataQuery.of("pow60")));
        // (long) (double) Long.MAX_VALUE saturates back to Long.MAX_VALUE; it must still be rejected
        assertThrows(InvalidDataException.class, () -> c.exactDouble(DataQuery.of("longMax")));
        assertThrows(InvalidDataException.class, () -> c.exactFloat(DataQuery.of("longMax")));
    }

    @Test
    @DisplayName("the empty path returns the view itself")
    void emptyPath() {
        final DataContainer c = DataContainer.createNew();
        assertSame(c, c.exactView(DataQuery.of()).get());
    }

    @Test
    @DisplayName("JSON round trip: exact getters recover values whose stored width JSON changed")
    void jsonWidthCollapseIsTolerated() throws IOException {
        final DataContainer original = DataContainer.createNew()
            .set(DataQuery.of("byte"), (byte) 7)
            .set(DataQuery.of("short"), (short) -2)
            .set(DataQuery.of("long"), 1L)
            .set(DataQuery.of("float"), 0.1f)
            .set(DataQuery.of("nested", "long"), 42L);
        final DataContainer read = DataFormats.JSON.get().read(DataFormats.JSON.get().write(original));
        // the stored types changed ...
        assertEquals(Integer.class, read.get(DataQuery.of("byte")).get().getClass());
        assertEquals(Integer.class, read.get(DataQuery.of("long")).get().getClass());
        assertEquals(Double.class, read.get(DataQuery.of("float")).get().getClass());
        // ... but the exact getters return the original values
        assertEquals(Optional.of((byte) 7), read.exactByte(DataQuery.of("byte")));
        assertEquals(Optional.of((short) -2), read.exactShort(DataQuery.of("short")));
        assertEquals(OptionalLong.of(1L), read.exactLong(DataQuery.of("long")));
        assertEquals(Optional.of(0.1f), read.exactFloat(DataQuery.of("float")));
        assertEquals(OptionalLong.of(42L), read.exactLong(DataQuery.of("nested", "long")));
    }

    @Test
    @DisplayName("NBT round trip: exact getters return the stored values")
    void nbtRoundTrip() throws IOException {
        final DataContainer original = DataContainer.createNew()
            .set(DataQuery.of("bool"), true)
            .set(DataQuery.of("short"), (short) -2)
            .set(DataQuery.of("long"), 1L)
            .set(DataQuery.of("float"), 0.1f)
            .set(DataQuery.of("string"), "x");
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataFormats.NBT.get().writeTo(out, original);
        final DataContainer read = DataFormats.NBT.get().readFrom(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(Optional.of(true), read.exactBoolean(DataQuery.of("bool")));
        assertEquals(Optional.of((short) -2), read.exactShort(DataQuery.of("short")));
        assertEquals(OptionalLong.of(1L), read.exactLong(DataQuery.of("long")));
        assertEquals(Optional.of(0.1f), read.exactFloat(DataQuery.of("float")));
        assertEquals(Optional.of("x"), read.exactString(DataQuery.of("string")));
    }

    @Test
    @DisplayName("a null path is rejected")
    void nullPath() {
        final DataContainer c = DataContainer.createNew();
        assertThrows(NullPointerException.class, () -> c.exactInt(null));
    }
}
