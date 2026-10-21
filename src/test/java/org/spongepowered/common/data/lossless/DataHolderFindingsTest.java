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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.spongepowered.common.data.lossless.LosslessSupport.KEY;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.common.data.SpongeDataManager;
import org.spongepowered.common.data.SpongeDataRegistration;
import org.spongepowered.common.data.persistence.NBTTranslator;

import java.util.Optional;

/**
 * Findings from extending the reproducers to the DataSerializable and DataHolder paths that need more
 * than a pinned table cell to show. The rest are pinned in {@link TranslatorRoundTripTest},
 * {@link SerializableRoundTripTest}, {@link CustomDataRoundTripTest} and the gametest {@code LosslessGameTests}.
 */
@DisplayName("DataSerializable and DataHolder findings")
class DataHolderFindingsTest {

    private static final Key<Value<String>> FIRST = Key.from(ResourceKey.of("lossless_findings", "first"), String.class);
    private static final Key<Value<String>> SECOND = Key.from(ResourceKey.of("lossless_findings", "second"), String.class);
    private static boolean registered;

    private static synchronized void register() {
        if (!DataHolderFindingsTest.registered) {
            DataHolderFindingsTest.registered = true;
            SpongeDataManager.INSTANCE.registerCustomDataRegistration((SpongeDataRegistration) DataRegistration.of(DataHolderFindingsTest.FIRST, ItemStack.class));
            SpongeDataManager.INSTANCE.registerCustomDataRegistration((SpongeDataRegistration) DataRegistration.of(DataHolderFindingsTest.SECOND, ItemStack.class));
        }
    }

    @Test
    @DisplayName("X20b silent corruption: copying a CompoundTagDataView drops the $Boolean marker for good")
    void x20bCopyDropsBooleanMarker() {
        final DataContainer compound = LosslessSupport.compound().set(KEY, true);
        // streamRootValues hands out ("v", Byte 1), so a copy into any view stores a byte
        assertEquals(Byte.valueOf((byte) 1), LosslessSupport.memory().set(DataQuery.of("c"), compound).get(DataQuery.of("c", "v")).get());
        // and translating it back to NBT writes "v" as a plain byte, without the marker
        final CompoundTag tag = NBTTranslator.INSTANCE.translate(compound);
        assertTrue(tag.contains("v"));
        assertFalse(tag.contains("v" + NBTTranslator.BOOLEAN_IDENTIFIER));
    }

    @Test
    @DisplayName("X22 data loss on read: ItemStack.builder().fromContainer() hides custom data until something writes custom data")
    void x22BuilderHidesCustomData() {
        DataHolderFindingsTest.register();
        final ItemStack stack = ItemStack.of(ItemTypes.STONE);
        stack.offer(DataHolderFindingsTest.FIRST, "kept");
        final DataContainer container = stack.toContainer();

        // DataManager.deserialize goes through the codec constructor, which syncs the custom data tag
        assertEquals(Optional.of("kept"), Sponge.dataManager().deserialize(ItemStack.class, container).get().get(DataHolderFindingsTest.FIRST));

        // The builder applies the same components with applyComponents, which does not
        final ItemStack built = ItemStack.builder().fromContainer(container).build();
        assertEquals(Optional.empty(), built.get(DataHolderFindingsTest.FIRST));
        // The tag still holds it, so toContainer() is unchanged...
        assertEquals(container, built.toContainer());
        // Writing any custom data re-reads the tag, so the hidden value comes back rather than being dropped
        built.offer(DataHolderFindingsTest.SECOND, "new");
        assertEquals(Optional.of("kept"), built.get(DataHolderFindingsTest.FIRST));
        final ItemStack reread = Sponge.dataManager().deserialize(ItemStack.class, built.toContainer()).get();
        assertEquals(Optional.of("new"), reread.get(DataHolderFindingsTest.SECOND));
        assertEquals(Optional.of("kept"), reread.get(DataHolderFindingsTest.FIRST));
    }
}
