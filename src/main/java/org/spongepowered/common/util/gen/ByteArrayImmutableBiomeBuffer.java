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
package org.spongepowered.common.util.gen;

import com.flowpowered.math.vector.Vector3i;
import net.minecraft.world.biome.Biome;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.biome.BiomeTypes;
import org.spongepowered.api.world.biome.ImmutableBiomeVolume;
import org.spongepowered.api.world.biome.worker.BiomeVolumeWorker;
import org.spongepowered.common.world.volume.ImmutableBiomeViewDownsize;
import org.spongepowered.common.world.volume.worker.SpongeBiomeVolumeWorker;

/**
 * Immutable biome volume, backed by a byte array. The array passed to the
 * constructor is copied to ensure that the instance is immutable.
 */
public final class ByteArrayImmutableBiomeBuffer extends AbstractBiomeBuffer implements ImmutableBiomeVolume {

    private final byte[] biomes;

    public ByteArrayImmutableBiomeBuffer(byte[] biomes, Vector3i start, Vector3i size) {
        super(start, size);
        this.biomes = biomes.clone();
    }

    private ByteArrayImmutableBiomeBuffer(Vector3i start, Vector3i size, byte[] biomes) {
        super(start, size);
        this.biomes = biomes;
    }

    @Override
    public BiomeType getBiome(int x, int y, int z) {
        checkRange(x, y, z);
        BiomeType biomeType = (BiomeType) Biome.getBiomeForId(this.biomes[getIndex(x, z)] & 255);
        return biomeType == null ? BiomeTypes.OCEAN : biomeType;
    }

    @Override
    public ImmutableBiomeVolume getView(Vector3i newMin, Vector3i newMax) {
        checkRange(newMin.getX(), newMin.getY(), newMin.getZ());
        checkRange(newMax.getX(), newMax.getY(), newMax.getZ());
        return new ImmutableBiomeViewDownsize(this, newMin, newMax);
    }

    @Override
    public BiomeVolumeWorker<ImmutableBiomeVolume, ?> getBiomeWorker() {
        return new SpongeBiomeVolumeWorker<>(this);
    }


    /**
     * This method doesn't clone the array passed into it. INTERNAL USE ONLY.
     * Make sure your code doesn't leak the reference if you're using it.
     *
     * @param biomes The biomes to store
     * @param start The start of the volume
     * @param size The size of the volume
     * @return A new buffer using the same array reference
     */
    public static ImmutableBiomeVolume newWithoutArrayClone(byte[] biomes, Vector3i start, Vector3i size) {
        return new ByteArrayImmutableBiomeBuffer(start, size, biomes);
    }

}
