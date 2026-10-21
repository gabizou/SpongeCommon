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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.spongepowered.common.data.lossless.LosslessSupport.KEY;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;

import java.util.Optional;

/**
 * H2: set rejects null via requireNonNull; there is no way to store a null value.
 */
@DisplayName("H2 CONFIRMED: every set overload rejects null; nothing can represent null")
class H2NullRejectedTest {

    @Test
    @DisplayName("set(DataQuery, Object null) throws NPE (MemoryDataView.java:168)")
    void objectOverloadRejectsNull() {
        final DataContainer c = LosslessSupport.memory();
        assertThrows(NullPointerException.class, () -> c.set(KEY, (Object) null));
        assertFalse(c.contains(KEY));
    }

    @Test
    @DisplayName("set(DataQuery, String null) throws NPE (MemoryDataView.java:181)")
    void stringOverloadRejectsNull() {
        final DataContainer c = LosslessSupport.memory();
        assertThrows(NullPointerException.class, () -> c.set(KEY, (String) null));
    }

    @Test
    @DisplayName("set(String key, Object null) throws NPE (MemoryDataView.java:242)")
    void stringKeyOverloadRejectsNull() {
        final DataContainer c = LosslessSupport.memory();
        assertThrows(NullPointerException.class, () -> c.set("v", (Object) null));
    }

    @Test
    @DisplayName("nested path also rejects null before creating intermediates (MemoryDataView.java:168 runs before setChild)")
    void nestedNullDoesNotCreateIntermediates() {
        final DataContainer c = LosslessSupport.memory();
        assertThrows(NullPointerException.class, () -> c.set(DataQuery.of("a", "b"), (Object) null));
        assertFalse(c.contains(DataQuery.of("a")));
    }

    @Test
    @DisplayName("CompoundTagDataView also rejects null (CompoundTagDataView.java:128)")
    void compoundViewRejectsNull() {
        final DataContainer c = LosslessSupport.compound();
        assertThrows(NullPointerException.class, () -> c.set(KEY, (Object) null));
    }

    @Test
    @DisplayName("absence and 'null' are indistinguishable: get returns Optional.empty for a missing key")
    void absenceIsTheOnlyNull() {
        final DataContainer c = LosslessSupport.memory();
        assertEquals(Optional.empty(), c.get(KEY));
    }

    @Test
    @DisplayName("Optional.empty() is accepted and stored as a raw Optional object (DataSerializer.java:91), not as absence")
    void optionalIsStoredRaw() {
        final DataContainer c = LosslessSupport.memory();
        c.set(KEY, Optional.empty());
        assertEquals(Optional.of(Optional.empty()), c.get(KEY));
    }
}
