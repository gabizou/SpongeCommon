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
package org.spongepowered.common.world.volume;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Maps;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.TileEntityArchetype;
import org.spongepowered.api.util.DiscreteTransform3;
import org.spongepowered.api.util.PositionOutOfBoundsException;
import org.spongepowered.api.world.volume.StorageType;
import org.spongepowered.common.util.gen.ArrayMutableBlockBuffer;
import org.spongepowered.common.util.gen.ByteArrayImmutableBiomeBuffer;
import org.spongepowered.common.util.gen.ByteArrayMutableBiomeBuffer;
import org.spongepowered.common.world.volume.worker.SpongeMutableBiomeVolumeWorker;
import org.spongepowered.common.world.volume.worker.SpongeMutableBlockVolumeWorker;
import org.spongepowered.common.world.schematic.BimapPalette;
import org.spongepowered.common.world.schematic.SpongeArchetypeVolume;

import java.util.Map;
import java.util.Optional;

/**
 * The Extent interface with extra defaults that are only available in the
 * implementation.
 */
public interface DefaultedExtent extends Extent {

    @Override
    default MutableBiomeVolume getBiomeView(Vector3i newMin, Vector3i newMax) {
        if (!containsBiome(newMin.getX(), newMin.getY(), newMin.getZ())) {
            throw new PositionOutOfBoundsException(newMin, getBiomeMin(), getBiomeMax());
        }
        if (!containsBiome(newMax.getX(), newMin.getY(), newMax.getZ())) {
            throw new PositionOutOfBoundsException(newMax, getBiomeMin(), getBiomeMax());
        }
        return new MutableBiomeViewDownsize(this, newMin, newMax);
    }

    @Override
    default UnmodifiableBiomeVolume getUnmodifiableBiomeView() {
        return new UnmodifiableBiomeVolumeWrapper(this);
    }

    @Override
    default MutableBiomeVolume getBiomeCopy(StorageType type) {
        switch (type) {
            case STANDARD:
                return new ByteArrayMutableBiomeBuffer(ExtentBufferUtil.copyToArray((BiomeVolume) this, getBiomeMin(), getBiomeMax(), getBiomeSize()),
                        getBiomeMin(), getBiomeSize());
            case THREAD_SAFE:
            default:
                throw new UnsupportedOperationException(type.name());
        }
    }

    @Override
    default ImmutableBiomeVolume getImmutableBiomeCopy() {
        return ByteArrayImmutableBiomeBuffer.newWithoutArrayClone(ExtentBufferUtil.copyToArray((BiomeVolume) this, getBiomeMin(), getBiomeMax(),
                getBiomeSize()), getBiomeMin(), getBiomeSize());
    }

    @Override
    default MutableBlockVolume getBlockView(DiscreteTransform3 transform) {
        return new MutableBlockViewTransform(this, transform);
    }

    @Override
    default UnmodifiableBlockVolume getUnmodifiableBlockView() {
        return new UnmodifiableBlockVolumeWrapper(this);
    }

    @Override
    default MutableBiomeVolumeWorker<? extends Extent> getBiomeWorker() {
        return new SpongeMutableBiomeVolumeWorker<>(this);
    }

    @Override
    default MutableBlockVolumeWorker<? extends Extent> getBlockWorker() {
        return new SpongeMutableBlockVolumeWorker<>(this);
    }

    @Override
    default ArchetypeVolume createArchetypeVolume(Vector3i min, Vector3i max, Vector3i origin) {
        Vector3i tmin = min.min(max);
        Vector3i tmax = max.max(min);
        min = tmin;
        max = tmax;
        Extent volume = getExtentView(min, max);
        BimapPalette palette = new BimapPalette();
        volume.getBlockWorker().iterate((v, x, y, z) -> {
            palette.getOrAssign(v.getBlock(x, y, z));
        });
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();
        final MutableBlockVolume backing = new ArrayMutableBlockBuffer(min.sub(origin), max.sub(min).add(1, 1, 1));
        Map<Vector3i, TileEntityArchetype> tiles = Maps.newHashMap();
        volume.getBlockWorker().iterate((extent, x, y, z) -> {
            BlockState state = extent.getBlock(x, y, z);
            backing.setBlock(x - ox, y - oy, z - oz, state);
            Optional<TileEntity> tile = extent.getTileEntity(x, y, z);
            if (tile.isPresent()) {
                tiles.put(new Vector3i(x - ox, y - oy, z - oz), tile.get().createArchetype());
            }
        });
        return new SpongeArchetypeVolume(backing, tiles);
    }

}
