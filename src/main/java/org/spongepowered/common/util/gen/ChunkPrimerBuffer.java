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

import static com.google.common.base.Preconditions.checkArgument;

import com.flowpowered.math.vector.Vector3i;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.world.ProtoWorld;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.biome.worker.MutableBiomeVolumeWorker;
import org.spongepowered.api.world.chunk.ProtoChunk;
import org.spongepowered.api.world.volume.block.worker.MutableBlockVolumeWorker;
import org.spongepowered.common.world.chunk.AbstractWrapperChunk;
import org.spongepowered.common.world.volume.worker.SpongeMutableBlockVolumeWorker;
import org.spongepowered.common.world.storage.SpongeChunkLayout;

import java.util.Optional;

/**
 * Makes a {@link ChunkPrimer} usable as a {@link ProtoChunk}.
 */
public final class ChunkPrimerBuffer extends AbstractWrapperChunk<IChunk, ChunkPrimerBuffer> implements ProtoChunk<ChunkPrimerBuffer> {

    private final IChunk chunkPrimer;
    // Use a WorldGenRegionWrapper of sorts
    // to generate
    private final World world; // Will use WorldGenRegion with a wrapper on top


    public ChunkPrimerBuffer(World world, IChunk chunkPrimer, int chunkX, int chunkZ) {
        super(getBlockStart(chunkX, chunkZ), getBlockEnd(chunkX, chunkZ), SpongeChunkLayout.CHUNK_SIZE);
        this.world = world;
        this.chunkPrimer = chunkPrimer;
    }

    private static Vector3i getBlockStart(int chunkX, int chunkZ) {
        final Optional<Vector3i> worldCoords = SpongeChunkLayout.instance.toWorld(chunkX, 0, chunkZ);
        checkArgument(worldCoords.isPresent(), "Chunk coordinates are not valid" + chunkX + ", " + chunkZ);
        return worldCoords.get();
    }

    private static Vector3i getBlockEnd(int chunkX, int chunkZ) {
        return new Vector3i(chunkX, 0, chunkZ); // TODO - this obviously isn't right, just too lazy to do it right now.
    }


    @Override
    public BlockState getBlock(int x, int y, int z) {
        checkRange(x, y, z);
        return (BlockState) this.chunkPrimer.getBlockState(x & 0xf, y, z & 0xf);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockState block) {
        checkRange(x, y, z);
        this.chunkPrimer.setBlockState(x & 0xf, y, z & 0xF, (IBlockState) block);
        return true;
    }


    @Override
    public MutableBlockVolumeWorker<ChunkPrimerBuffer> getBlockWorker() {
        return new SpongeMutableBlockVolumeWorker<>(this);
    }

    @Override
    public ProtoWorld<?> getWorld() {
        return (ProtoWorld) this.world; // Will actually use WorldGenRegion instead.
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType block) {
        return false;
    }

    @Override
    public MutableBiomeVolumeWorker<ChunkPrimerBuffer> getBiomeWorker() {
        return null;
    }

    @Override
    public BiomeType getBiome(int x, int y, int z) {
        return null;
    }

    @Override
    public boolean containsBiome(int x, int y, int z) {
        return false;
    }
}
