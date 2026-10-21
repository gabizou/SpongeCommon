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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.spongepowered.common.data.lossless.LosslessSupport.KEY;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;

/**
 * H8: arrays are compared by identity in equals and hashCode, so equal
 * contents give unequal views.
 */
@DisplayName("H8 CONFIRMED for MemoryDataView, REFUTED for CompoundTagDataView: array contents are compared by identity only in memory")
class H8ArrayIdentityTest {

    @Test
    @DisplayName("MemoryDataView: two views holding equal int[] contents are not equal")
    void memoryViewsWithEqualArraysAreUnequal() {
        final DataContainer a = LosslessSupport.memory().set(KEY, new int[] {1, 2, 3});
        final DataContainer b = LosslessSupport.memory().set(KEY, new int[] {1, 2, 3});
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("MemoryDataView: a view is not equal to its own copy() when it holds an array")
    void memoryViewIsNotEqualToItsCopy() {
        final DataContainer a = LosslessSupport.memory().set(KEY, new byte[] {1});
        assertNotEquals(a, a.copy());
    }

    @Test
    @DisplayName("MemoryDataView: arrays nested in a child view break equality of the parents too")
    void nestedArraysBreakParentEquality() {
        final DataContainer a = LosslessSupport.memory().set(DataQuery.of("x", "y"), new long[] {1L});
        final DataContainer b = LosslessSupport.memory().set(DataQuery.of("x", "y"), new long[] {1L});
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("CompoundTagDataView compares ByteArrayTag/IntArrayTag by content, so equal arrays are equal")
    void compoundViewsCompareArrayContent() {
        final DataContainer a = LosslessSupport.compound().set(KEY, new int[] {1, 2, 3});
        final DataContainer b = LosslessSupport.compound().set(KEY, new int[] {1, 2, 3});
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
