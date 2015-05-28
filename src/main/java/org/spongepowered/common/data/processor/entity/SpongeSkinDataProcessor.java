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

package org.spongepowered.common.data.processor.entity;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spongepowered.common.data.DataTransactionBuilder.fail;

import com.google.common.base.Optional;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataPriority;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.entity.SkinComponent;
import org.spongepowered.api.service.persistence.InvalidDataException;
import org.spongepowered.common.data.SpongeDataProcessor;
import org.spongepowered.common.data.component.entity.SpongeSkinComponent;
import org.spongepowered.common.entity.living.HumanEntity;

import java.util.UUID;

public class SpongeSkinDataProcessor implements SpongeDataProcessor<SkinComponent> {

    @Override
    public Optional<SkinComponent> build(DataView container) throws InvalidDataException {
        if (container.contains(SpongeSkinComponent.SKIN_UUID)) {
            final String uuidString = container.getString(SpongeSkinComponent.SKIN_UUID).get();
            final UUID uuid = UUID.fromString(uuidString);
            return Optional.of(create().setValue(uuid));
        }
        return Optional.absent();
    }

    @Override
    public SkinData create() {
        return new SpongeSkinComponent();
    }

    @Override
    public Optional<SkinComponent> createFrom(DataHolder dataHolder) {
        if (!(dataHolder instanceof HumanEntity)) {
            return Optional.absent();
        }
        return Optional.of(((HumanEntity) dataHolder).createSkinData());
    }

    @Override
    public Optional<SkinComponent> getFrom(DataHolder holder) {
        if (!(holder instanceof HumanEntity)) {
            return Optional.absent();
        }
        return ((HumanEntity) holder).getSkinData();
    }

    @Override
    public Optional<SkinComponent> fillData(DataHolder holder, SkinData manipulator, DataPriority priority) {
        if (!(holder instanceof HumanEntity)) {
            return Optional.absent();
        }
        switch (checkNotNull(priority)) {
            case DATA_HOLDER:
            case PRE_MERGE:
                return ((HumanEntity) holder).fillSkinData(manipulator);
            default:
                return Optional.absent();
        }
    }

    @Override
    public DataTransactionResult setData(DataHolder dataHolder, SkinData manipulator, DataPriority priority) {
        if (dataHolder instanceof HumanEntity) {
            switch (checkNotNull(priority)) {
                case COMPONENT:
                case PRE_MERGE:
                    return ((HumanEntity) dataHolder).setSkinData(manipulator);
                default:
                    return successNoData();
            }
        }
        return fail(manipulator);
    }

    @Override
    public boolean remove(DataHolder dataHolder) {
        return dataHolder instanceof HumanEntity && ((HumanEntity) dataHolder).removeSkin();
    }

}
