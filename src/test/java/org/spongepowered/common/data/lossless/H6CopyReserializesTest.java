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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.spongepowered.common.data.lossless.LosslessSupport.KEY;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;

/**
 * H6: copy() re-runs set on every root value (MemoryDataView.java:384-406), so
 * every value goes through DataSerializer again, under the copy's safety mode.
 */
@DisplayName("H6 CONFIRMED: copy() re-sets each root value, so DataSerializer runs again under the target safety mode")
class H6CopyReserializesTest {

    @Test
    @DisplayName("copy() under NO_DATA_CLONED shares array instances with the original (DataSerializer.java:139-145)")
    void noDataClonedCopySharesArrays() {
        final DataContainer c = LosslessSupport.memory(DataView.SafetyMode.NO_DATA_CLONED);
        final byte[] array = {1, 2, 3};
        c.set(KEY, array);
        final DataContainer copy = c.copy();
        assertSame(array, copy.get(KEY).get());
        // mutating through the "copy" changes the original
        ((byte[]) copy.get(KEY).get())[0] = 99;
        assertEquals(99, ((byte[]) c.get(KEY).get())[0]);
    }

    @Test
    @DisplayName("copy(CLONED_ON_SET) of the same container clones: the result depends on the copy's mode, so serializeArray ran again")
    void copyWithClonedOnSetClones() {
        final DataContainer c = LosslessSupport.memory(DataView.SafetyMode.NO_DATA_CLONED);
        final byte[] array = {1, 2, 3};
        c.set(KEY, array);
        final DataContainer copy = c.copy(DataView.SafetyMode.CLONED_ON_SET);
        assertNotSame(array, copy.get(KEY).get());
        assertArrayEquals(array, (byte[]) copy.get(KEY).get());
    }

    @Test
    @DisplayName("nested views are rebuilt with the copy's safety mode (DataSerializer.java:58-60 + MemoryDataView.java:352-356)")
    void nestedViewsTakeTheCopysSafetyMode() {
        final DataContainer c = LosslessSupport.memory(DataView.SafetyMode.ALL_DATA_CLONED);
        c.set(DataQuery.of("a", "b"), 1);
        final DataContainer copy = c.copy(DataView.SafetyMode.NO_DATA_CLONED);
        assertEquals(DataView.SafetyMode.NO_DATA_CLONED, copy.getView(DataQuery.of("a")).get().safetyMode());
        assertNotSame(c.getView(DataQuery.of("a")).get(), copy.getView(DataQuery.of("a")).get());
    }

    @Test
    @DisplayName("copy() of a child view is not equal to the child: paths differ (SpongeDataView.java:505-506, MemoryDataView.java:428-429)")
    void copyOfChildIsNotEqualToChild() {
        final DataContainer c = LosslessSupport.memory();
        c.set(DataQuery.of("a", "b"), 1);
        final DataView child = c.getView(DataQuery.of("a")).get();
        final DataContainer copy = child.copy();
        assertEquals(child.values(true), copy.values(true));
        assertNotEquals(child, copy);
    }

    @Test
    @DisplayName("CompoundTagDataView.copy(safety) ignores its argument (CompoundTagDataView.java:378)")
    void compoundCopyIgnoresSafetyArgument() {
        final DataContainer c = LosslessSupport.compound();
        c.set(KEY, 1);
        assertEquals(DataView.SafetyMode.ALL_DATA_CLONED, c.copy(DataView.SafetyMode.NO_DATA_CLONED).safetyMode());
    }
}
