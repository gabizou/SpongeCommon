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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;

import java.util.List;

/**
 * H3: nested writes call getUnsafeView then createView; a scalar at an
 * intermediate key is silently replaced by a view (MemoryDataView.java:232-237).
 */
@DisplayName("H3 CONFIRMED: a nested set silently replaces a scalar intermediate with a DataView")
class H3SetChildReplacesScalarTest {

    private static final DataQuery A = DataQuery.of("a");
    private static final DataQuery A_B = DataQuery.of("a", "b");

    @Test
    @DisplayName("Object overload: scalar at 'a' is dropped when setting 'a.b'")
    void objectOverloadReplacesScalar() {
        final DataContainer c = LosslessSupport.memory();
        c.set(A, (Object) "scalar");
        c.set(A_B, (Object) "nested");
        assertInstanceOf(DataView.class, c.get(A).get());
        assertEquals("nested", c.getString(A_B).get());
        assertEquals(1, c.keys(false).size());
    }

    @Test
    @DisplayName("primitive overload takes the same setChild path (MemoryDataView.java:224-229)")
    void primitiveOverloadReplacesScalar() {
        final DataContainer c = LosslessSupport.memory();
        c.set(A, 42);
        c.set(A_B, 7L);
        assertInstanceOf(DataView.class, c.get(A).get());
    }

    @Test
    @DisplayName("a list at the intermediate key is also replaced")
    void listIntermediateIsReplaced() {
        final DataContainer c = LosslessSupport.memory();
        c.set(A, List.of(1, 2, 3));
        c.set(A_B, 1);
        assertInstanceOf(DataView.class, c.get(A).get());
    }

    @Test
    @DisplayName("CompoundTagDataView behaves the same via getOrCreateView (CompoundTagDataView.java:135,358-360)")
    void compoundViewReplacesScalar() {
        final DataContainer c = LosslessSupport.compound();
        c.set(A, (Object) "scalar");
        c.set(A_B, (Object) "nested");
        assertTrue(c.get(A).get() instanceof DataView);
        assertEquals("nested", c.getString(A_B).get());
    }
}
