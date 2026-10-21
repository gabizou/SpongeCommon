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
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;

import java.util.Optional;

/**
 * H5: get through a scalar intermediate returns Optional.empty(), the same as
 * absence (MemoryDataView.java:137-140).
 */
@DisplayName("H5 CONFIRMED: get/contains through a scalar intermediate look exactly like absence")
class H5GetThroughScalarTest {

    private static final DataQuery A = DataQuery.of("a");
    private static final DataQuery A_B = DataQuery.of("a", "b");

    @Test
    @DisplayName("MemoryDataView: scalar intermediate and missing key give identical results")
    void memoryViewScalarIntermediateLooksAbsent() {
        final DataContainer scalar = LosslessSupport.memory().set(A, 1);
        final DataContainer empty = LosslessSupport.memory();
        assertEquals(Optional.empty(), scalar.get(A_B));
        assertEquals(empty.get(A_B), scalar.get(A_B));
        assertEquals(empty.getInt(A_B), scalar.getInt(A_B));
        assertEquals(empty.getView(A_B), scalar.getView(A_B));
        assertFalse(scalar.contains(A_B));
    }

    @Test
    @DisplayName("CompoundTagDataView: same (CompoundTagDataView.java:110)")
    void compoundViewScalarIntermediateLooksAbsent() {
        final DataContainer scalar = LosslessSupport.compound().set(A, 1);
        assertEquals(Optional.empty(), scalar.get(A_B));
        assertFalse(scalar.contains(A_B));
    }

    @Test
    @DisplayName("remove through a scalar intermediate is a silent no-op (MemoryDataView.java:316-319)")
    void removeThroughScalarIsNoOp() {
        final DataContainer c = LosslessSupport.memory().set(A, 1);
        c.remove(A_B);
        assertEquals(1, c.getInt(A).get());
    }
}
