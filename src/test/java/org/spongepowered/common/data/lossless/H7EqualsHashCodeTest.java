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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.spongepowered.common.data.lossless.LosslessSupport.KEY;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;

/**
 * H7: MemoryDataView.equals compares raw entry sets and path, falling back to
 * SpongeDataView.equals (values(true) + path) for other classes; hashCode uses
 * only raw map and path. Check whether they disagree.
 */
@DisplayName("H7 CONFIRMED: equals and hashCode disagree across DataView implementations")
class H7EqualsHashCodeTest {

    @Test
    @DisplayName("MemoryDataContainer equals CompoundTagDataContainer with the same content, but hashCodes differ")
    void crossImplementationEqualsWithDifferentHashCode() {
        final DataContainer memory = LosslessSupport.memory().set(DataQuery.of("a", "b"), 1);
        final DataContainer compound = LosslessSupport.compound().set(DataQuery.of("a", "b"), 1);
        // MemoryDataView.java:423-424 -> SpongeDataView.java:500-507 compares values(true)
        assertEquals(memory, compound);
        assertEquals(compound, memory);
        // MemoryDataView.java:415 hashes the LinkedHashMap, CompoundTagDataView.java:388 the CompoundTag
        // nested MemoryDataView hashes include their path; nested CompoundTags do not
        assertNotEquals(memory.hashCode(), compound.hashCode());
    }

    @Test
    @DisplayName("for flat int/String content the hashCodes agree only by coincidence: IntTag/StringTag hash like Integer/String")
    void flatContentHashCodesCoincide() {
        final DataContainer memory = LosslessSupport.memory().set(KEY, 1).set(DataQuery.of("s"), "x");
        final DataContainer compound = LosslessSupport.compound().set(KEY, 1).set(DataQuery.of("s"), "x");
        assertEquals(memory, compound);
        assertEquals(memory.hashCode(), compound.hashCode());
    }

    @Test
    @DisplayName("same-class comparison uses the raw map: equal content gives equal hashCode")
    void sameClassIsConsistent() {
        final DataContainer a = LosslessSupport.memory().set(DataQuery.of("a", "b"), 1L);
        final DataContainer b = LosslessSupport.memory().set(DataQuery.of("a", "b"), 1L);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("equals ignores safety mode, hashCode too (consistent, but a NO_DATA_CLONED view equals an ALL_DATA_CLONED one)")
    void safetyModeIsIgnored() {
        final DataContainer a = LosslessSupport.memory(DataView.SafetyMode.NO_DATA_CLONED).set(KEY, 1);
        final DataContainer b = LosslessSupport.memory(DataView.SafetyMode.ALL_DATA_CLONED).set(KEY, 1);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("the values(true) fallback can never match arrays under ALL_DATA_CLONED: get() clones them (MemoryDataView.java:70,147-159)")
    void crossClassFallbackFailsOnArrays() {
        final byte[] bytes = {1, 2};
        final DataContainer memory = LosslessSupport.memory().set(KEY, bytes);
        final DataContainer compound = LosslessSupport.compound().set(KEY, bytes);
        assertNotEquals(memory, compound);
    }

    @Test
    @DisplayName("a HashSet of views can hold two 'equal' views because their hashCodes differ")
    void hashSetHoldsEqualViewsTwice() {
        final DataContainer memory = LosslessSupport.memory().set(DataQuery.of("a", "b"), 1);
        final DataContainer compound = LosslessSupport.compound().set(DataQuery.of("a", "b"), 1);
        final java.util.Set<DataView> set = new java.util.HashSet<>();
        set.add(memory);
        set.add(compound);
        assertTrue(memory.equals(compound));
        assertEquals(2, set.size());
    }
}
