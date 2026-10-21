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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.spongepowered.common.data.lossless.LosslessSupport.KEY;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataView;

import java.time.Month;
import java.util.ArrayList;
import java.util.List;

/**
 * H1: primitive set overloads store the boxed value as given; set(DataQuery, Object)
 * stores only String and Integer directly and routes everything else through
 * DataSerializer.serialize (MemoryDataView#set0, MemoryDataView.java:298-307).
 */
@DisplayName("H1 CONFIRMED: primitive overloads store directly, Object overload routes non String/Integer through DataSerializer")
class H1SetOverloadRoutingTest {

    @Test
    @DisplayName("primitive overloads store the boxed type unchanged (MemoryDataView.java:205-229)")
    void primitiveOverloadsStoreBoxedValue() {
        final DataContainer c = LosslessSupport.memory();
        c.set(KEY, 5L);
        assertEquals(Long.valueOf(5L), c.get(KEY).get());
        c.set(KEY, (byte) 5);
        assertEquals(Byte.valueOf((byte) 5), c.get(KEY).get());
        c.set(KEY, 5.0f);
        assertEquals(Float.valueOf(5.0f), c.get(KEY).get());
        c.set(KEY, true);
        assertEquals(Boolean.TRUE, c.get(KEY).get());
    }

    @Test
    @DisplayName("Object overload stores String and Integer instances directly (MemoryDataView.java:299-301)")
    void objectOverloadStoresStringAndIntegerDirectly() {
        final DataContainer c = LosslessSupport.memory();
        final String s = new String("abc");
        c.set(KEY, (Object) s);
        assertSame(s, c.get(KEY).get());
        final Integer i = Integer.valueOf(123456);
        c.set(KEY, (Object) i);
        assertSame(i, c.get(KEY).get());
    }

    @Test
    @DisplayName("Object overload sends other values through DataSerializer: a translator turns Month into a DataView")
    void objectOverloadRoutesThroughDataSerializer() {
        final DataContainer c = LosslessSupport.memory();
        c.set(KEY, (Object) Month.MARCH);
        // DataSerializer.java:64-69 finds DataTranslatorProvider's Month translator
        assertInstanceOf(DataView.class, c.get(KEY).get());
    }

    @Test
    @DisplayName("Object overload copies collections into an ImmutableList (DataSerializer.java:84-85,103-109)")
    void objectOverloadCopiesCollections() {
        final DataContainer c = LosslessSupport.memory();
        final List<String> list = new ArrayList<>(List.of("a", "b"));
        c.set(KEY, list);
        final Object stored = c.get(KEY).get();
        assertNotSame(list, stored);
        assertInstanceOf(ImmutableList.class, stored);
    }

    @Test
    @DisplayName("boxed scalars other than Integer/String still pass through DataSerializer unchanged (DataSerializer.java:91)")
    void boxedScalarsSurviveSerializerUnchanged() {
        final DataContainer c = LosslessSupport.memory();
        final Long boxed = 1L << 40;
        c.set(KEY, (Object) boxed);
        assertSame(boxed, c.get(KEY).get());
    }

    @Test
    @DisplayName("String-key Object overload uses the same set0 routing (MemoryDataView.java:240-247)")
    void stringKeyOverloadRoutesTheSame() {
        final DataContainer c = LosslessSupport.memory();
        c.set("v", (Object) Month.MARCH);
        assertInstanceOf(DataView.class, c.get(KEY).get());
    }
}
