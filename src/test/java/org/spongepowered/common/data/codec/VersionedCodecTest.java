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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.data.persistence.DataCodec;
import org.spongepowered.api.data.persistence.DataCodecException;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataContentUpdater;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.FieldCodec;
import org.spongepowered.api.data.persistence.RecordCodec;
import org.spongepowered.api.data.persistence.VersionedCodec;

import java.util.List;
import java.util.Optional;

@DisplayName("VersionedCodec")
class VersionedCodecTest {

    // version 1 stored the name as "nick"; version 2 renamed it to "name"; version 3 added "level", defaulting to 1
    record Profile(String name, int level) {
        static final FieldCodec<Profile, String> NAME = DataCodec.STRING.fieldOf("name", Profile::name);
        static final FieldCodec<Profile, Integer> LEVEL = DataCodec.INT.fieldOf("level", Profile::level);
        static final RecordCodec<Profile> CODEC = DataCodec.record(f -> new Profile(f.get(NAME), f.get(LEVEL)), NAME, LEVEL);
    }

    private static final DataQuery NICK = DataQuery.of("nick");
    private static final DataQuery NAME = DataQuery.of("name");
    private static final DataQuery LEVEL = DataQuery.of("level");
    private static final DataQuery VERSION = DataQuery.of("ContentVersion");

    private static final VersionedCodec<Profile> CODEC = VersionedCodec.builder(Profile.CODEC, 3)
        .updater(1, 2, view -> view.set(VersionedCodecTest.NAME, view.get(VersionedCodecTest.NICK).orElseThrow()).remove(VersionedCodecTest.NICK))
        .updater(2, 3, view -> view.set(VersionedCodecTest.LEVEL, 1))
        .build();

    @Test
    @DisplayName("encoding writes the current version next to the fields")
    void encodeWritesVersion() {
        final DataContainer encoded = VersionedCodecTest.CODEC.encode(new Profile("Alex", 5));
        assertEquals(Optional.of(3), encoded.getInt(VersionedCodecTest.VERSION));
        assertEquals(List.of("name", "level", "ContentVersion"), VersionedCodecTest.CODEC.keys());
        assertEquals(new Profile("Alex", 5), VersionedCodecTest.CODEC.decode(encoded));
    }

    @Test
    @DisplayName("older data is updated step by step, without modifying the input")
    void updatesOlderData() {
        final DataContainer v1 = DataContainer.createNew().set(VersionedCodecTest.VERSION, 1).set(VersionedCodecTest.NICK, "Alex");
        assertEquals(new Profile("Alex", 1), VersionedCodecTest.CODEC.decode(v1));
        assertEquals(Optional.of("Alex"), v1.getString(VersionedCodecTest.NICK));
        assertEquals(Optional.empty(), v1.get(VersionedCodecTest.NAME));

        final DataContainer v2 = DataContainer.createNew().set(VersionedCodecTest.VERSION, 2).set(VersionedCodecTest.NAME, "Sam");
        assertEquals(new Profile("Sam", 1), VersionedCodecTest.CODEC.decode(v2));
    }

    @Test
    @DisplayName("JSON-read old data (version as Integer) is updated the same way")
    void updatesAfterJson() {
        final DataContainer v1 = DataContainer.createNew().set(VersionedCodecTest.VERSION, (byte) 1).set(VersionedCodecTest.NICK, "Alex");
        assertEquals(new Profile("Alex", 1), VersionedCodecTest.CODEC.decode(DataCodecTest.json(v1)));
    }

    @Test
    @DisplayName("data without a version is rejected unless a version is assumed for it")
    void missingVersion() {
        final DataContainer unversioned = DataContainer.createNew().set(VersionedCodecTest.NICK, "Alex");
        assertEquals("Expected a version (an int) at 'ContentVersion' but found nothing",
            assertThrows(DataCodecException.class, () -> VersionedCodecTest.CODEC.decode(unversioned)).getMessage());

        final VersionedCodec<Profile> assumingOne = VersionedCodec.builder(Profile.CODEC, 3)
            .versionWhenMissing(1)
            .updater(1, 3, view -> view.set(VersionedCodecTest.NAME, view.get(VersionedCodecTest.NICK).orElseThrow()).set(VersionedCodecTest.LEVEL, 1))
            .build();
        assertEquals(new Profile("Alex", 1), assumingOne.decode(unversioned));
    }

    @Test
    @DisplayName("data from a newer version, or with a malformed version, is rejected")
    void newerOrMalformedVersion() {
        final DataContainer v4 = DataContainer.createNew().set(VersionedCodecTest.VERSION, 4).set(VersionedCodecTest.NAME, "Alex").set(VersionedCodecTest.LEVEL, 1);
        assertEquals("At 'ContentVersion': Data version 4 is newer than 3, the newest version this codec reads",
            assertThrows(DataCodecException.class, () -> VersionedCodecTest.CODEC.decode(v4)).getMessage());
        final DataContainer text = DataContainer.createNew().set(VersionedCodecTest.VERSION, "3");
        assertEquals("Expected an int at 'ContentVersion' but found String: 3",
            assertThrows(DataCodecException.class, () -> VersionedCodecTest.CODEC.decode(text)).getMessage());
    }

    @Test
    @DisplayName("errors after updating name the field")
    void errorsAfterUpdate() {
        final DataContainer v2 = DataContainer.createNew().set(VersionedCodecTest.VERSION, 2).set(VersionedCodecTest.NAME, 42);
        assertEquals("Expected a String at 'name' but found Integer: 42",
            assertThrows(DataCodecException.class, () -> VersionedCodecTest.CODEC.decode(v2)).getMessage());
    }

    @Test
    @DisplayName("the builder rejects gaps, overlaps, backwards updaters and key collisions")
    void builderValidation() {
        assertThrows(IllegalStateException.class, () -> VersionedCodec.builder(Profile.CODEC, 3).updater(1, 2, v -> v).build());
        assertThrows(IllegalArgumentException.class, () -> VersionedCodec.builder(Profile.CODEC, 3).updater(2, 2, v -> v));
        assertThrows(IllegalArgumentException.class, () -> VersionedCodec.builder(Profile.CODEC, 3).updater(2, 4, v -> v));
        assertThrows(IllegalArgumentException.class, () -> VersionedCodec.builder(Profile.CODEC, 3).updater(1, 2, v -> v).updater(1, 3, v -> v));
        assertThrows(IllegalStateException.class, () -> VersionedCodec.builder(Profile.CODEC, 3).versionKey("name").build());
        assertThrows(IllegalStateException.class, () -> VersionedCodec.builder(Profile.CODEC, 3).versionWhenMissing(4).build());
        assertThrows(IllegalStateException.class, () -> VersionedCodec.builder(Profile.CODEC, 3).versionWhenMissing(1).build());
        assertThrows(IllegalArgumentException.class, () -> VersionedCodec.builder(Profile.CODEC, 3).versionKey("v$Boolean"));
    }

    @Test
    @DisplayName("DataContentUpdater implementations can be used directly, and a custom version key is honoured")
    void contentUpdaterAndCustomKey() {
        final DataContentUpdater renameNick = new DataContentUpdater() {
            @Override
            public int inputVersion() {
                return 1;
            }

            @Override
            public int outputVersion() {
                return 3;
            }

            @Override
            public DataView update(final DataView content) {
                return content.set(VersionedCodecTest.NAME, content.get(VersionedCodecTest.NICK).orElseThrow()).set(VersionedCodecTest.LEVEL, 1);
            }
        };
        final VersionedCodec<Profile> codec = VersionedCodec.builder(Profile.CODEC, 3).versionKey("v").updater(renameNick).build();
        assertEquals(new Profile("Alex", 1), codec.decode(DataContainer.createNew().set(DataQuery.of("v"), 1).set(VersionedCodecTest.NICK, "Alex")));
        assertEquals(Optional.of(3), codec.encode(new Profile("Sam", 2)).getInt(DataQuery.of("v")));
    }
}
