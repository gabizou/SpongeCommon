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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;

/**
 * H4: createView(DataQuery) casts map.get(key) to DataView without a check
 * (MemoryDataView.java:343); over a scalar this throws ClassCastException,
 * which is inconsistent with H3 where set() silently replaces.
 */
@DisplayName("H4 CONFIRMED: multi-part createView over a scalar throws ClassCastException; set() over the same data replaces it")
class H4CreateViewOverScalarTest {

    private static final DataQuery A = DataQuery.of("a");
    private static final DataQuery A_B = DataQuery.of("a", "b");

    @Test
    @DisplayName("MemoryDataView.createView(a.b) over scalar a throws ClassCastException")
    void createViewOverScalarThrows() {
        final DataContainer c = LosslessSupport.memory();
        c.set(A, "scalar");
        assertThrows(ClassCastException.class, () -> c.createView(A_B));
        // the scalar survives the failed call
        assertEquals("scalar", c.get(A).get());
    }

    @Test
    @DisplayName("inconsistency: the same structure accepts set(a.b, x) and replaces the scalar")
    void setOverSameStructureSucceeds() {
        final DataContainer c = LosslessSupport.memory();
        c.set(A, "scalar");
        c.set(A_B, "x");
        assertInstanceOf(DataView.class, c.get(A).get());
    }

    @Test
    @DisplayName("single-part createView(a) overwrites a scalar, as the Javadoc says (MemoryDataView.java:339-340,352-356)")
    void singlePartCreateViewOverwrites() {
        final DataContainer c = LosslessSupport.memory();
        c.set(A, "scalar");
        c.createView(A);
        assertInstanceOf(DataView.class, c.get(A).get());
    }

    @Test
    @DisplayName("createView(path, map) over a scalar intermediate also throws, because it calls createView(path) first (SpongeDataView.java:207)")
    void createViewWithMapOverScalarThrows() {
        final DataContainer c = LosslessSupport.memory();
        c.set(A, "scalar");
        assertThrows(ClassCastException.class, () -> c.createView(A_B, java.util.Map.of("k", 1)));
    }

    @Test
    @DisplayName("CompoundTagDataView does not throw: getOrCreateView replaces the scalar (CompoundTagDataView.java:324,358-360)")
    void compoundViewReplacesInstead() {
        final DataContainer c = LosslessSupport.compound();
        c.set(A, "scalar");
        final DataView created = c.createView(A_B);
        assertEquals(A_B, created.currentPath());
        assertInstanceOf(DataView.class, c.get(A).get());
    }
}
