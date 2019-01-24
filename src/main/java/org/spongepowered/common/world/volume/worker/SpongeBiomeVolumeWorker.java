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
package org.spongepowered.common.world.volume.worker;

import static com.google.common.base.Preconditions.checkArgument;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.biome.MutableBiomeVolume;
import org.spongepowered.api.world.biome.ReadableBiomeVolume;
import org.spongepowered.api.world.biome.UnmodifiableBiomeVolume;
import org.spongepowered.api.world.biome.WorkableBiomeVolume;
import org.spongepowered.api.world.biome.worker.BiomeVolumeWorker;
import org.spongepowered.api.world.volume.worker.function.VolumeMapper;
import org.spongepowered.api.world.volume.worker.function.VolumeMerger;
import org.spongepowered.api.world.volume.worker.function.VolumeVisitor;

/**
 *
 */
public class SpongeBiomeVolumeWorker<V extends WorkableBiomeVolume<V>, M extends MutableBiomeVolume<M>> implements BiomeVolumeWorker<V, M> {

    protected final V volume;

    public SpongeBiomeVolumeWorker(V volume) {
        this.volume = volume;
    }

    @Override
    public V getVolume() {
        return this.volume;
    }

    @Override
    public void map(VolumeMapper<BiomeType, UnmodifiableBiomeVolume<?>> mapper, M destination) {
        final Vector3i offset = align(destination);
        final int xOffset = offset.getX();
        final int yOffset = offset.getY();
        final int zOffset = offset.getZ();
        final UnmodifiableBiomeVolume unmodifiableArea = this.volume.asUnmodifiableBiomeVolume();
        final int xMin = unmodifiableArea.getBiomeMin().getX();
        final int yMin = unmodifiableArea.getBiomeMin().getY();
        final int zMin = unmodifiableArea.getBiomeMin().getZ();
        final int xMax = unmodifiableArea.getBiomeMax().getX();
        final int yMax = unmodifiableArea.getBiomeMax().getY();
        final int zMax = unmodifiableArea.getBiomeMax().getZ();
        for (int z = zMin; z <= zMax; z++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int x = xMin; x <= xMax; x++) {
                    final BiomeType biome = mapper.map(unmodifiableArea, x, y, z);
                    destination.setBiome(x + xOffset, y + yOffset, z + zOffset, biome);
                }
            }
        }
    }

    @Override
    public void merge(V second, VolumeMerger<BiomeType, UnmodifiableBiomeVolume<?>> merger, M destination) {
        final Vector3i offsetSecond = align(second);
        final int xOffsetSecond = offsetSecond.getX();
        final int yOffsetSecond = offsetSecond.getY();
        final int zOffsetSecond = offsetSecond.getZ();
        final Vector3i offsetDestination = align(destination);
        final int xOffsetDestination = offsetDestination.getX();
        final int yOffsetDestination = offsetDestination.getY();
        final int zOffsetDestination = offsetDestination.getZ();
        final UnmodifiableBiomeVolume firstUnmodifiableArea = this.volume.asUnmodifiableBiomeVolume();
        final int xMin = firstUnmodifiableArea.getBiomeMin().getX();
        final int yMin = firstUnmodifiableArea.getBiomeMin().getY();
        final int zMin = firstUnmodifiableArea.getBiomeMin().getZ();
        final int xMax = firstUnmodifiableArea.getBiomeMax().getX();
        final int yMax = firstUnmodifiableArea.getBiomeMax().getY();
        final int zMax = firstUnmodifiableArea.getBiomeMax().getZ();
        final UnmodifiableBiomeVolume secondUnmodifiableArea = second.asUnmodifiableBiomeVolume();
        for (int z = zMin; z <= zMax; z++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int x = xMin; x <= xMax; x++) {
                    final BiomeType biome = merger.merge(firstUnmodifiableArea, x, y, z, secondUnmodifiableArea, x + xOffsetSecond, y + yOffsetSecond,
                            z + zOffsetSecond);
                    destination.setBiome(x + xOffsetDestination, y + yOffsetDestination, z + zOffsetDestination, biome);
                }
            }
        }
    }

    @Override
    public void iterate(VolumeVisitor<V> visitor) {
        final int xMin = this.volume.getBiomeMin().getX();
        final int yMin = this.volume.getBiomeMin().getY();
        final int zMin = this.volume.getBiomeMin().getZ();
        final int xMax = this.volume.getBiomeMax().getX();
        final int yMax = this.volume.getBiomeMax().getY();
        final int zMax = this.volume.getBiomeMax().getZ();
        for (int z = zMin; z <= zMax; z++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int x = xMin; x <= xMax; x++) {
                    visitor.visit(this.volume, x, y, z);
                }
            }
        }
    }

    private Vector3i align(ReadableBiomeVolume other) {
        final Vector3i thisSize = this.volume.getBiomeSize();
        final Vector3i otherSize = other.getBiomeSize();
        checkArgument(otherSize.getX() >= thisSize.getX() && otherSize.getY() >= thisSize.getY() && otherSize.getZ() >= thisSize.getZ(),
                "Other volume is smaller than work volume");
        return other.getBiomeMin().sub(this.volume.getBiomeMin());
    }

}
