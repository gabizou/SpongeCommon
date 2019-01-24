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
package org.spongepowered.common.world.chunk;

import com.flowpowered.math.vector.Vector3i;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.fluid.FluidState;
import org.spongepowered.api.fluid.FluidType;
import org.spongepowered.api.scheduler.ScheduledTaskList;
import org.spongepowered.api.world.LightType;
import org.spongepowered.api.world.LightTypes;
import org.spongepowered.api.world.ProtoWorld;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.biome.ImmutableBiomeVolume;
import org.spongepowered.api.world.biome.UnmodifiableBiomeVolume;
import org.spongepowered.api.world.biome.worker.MutableBiomeVolumeWorker;
import org.spongepowered.api.world.chunk.ChunkState;
import org.spongepowered.api.world.chunk.ProtoChunk;
import org.spongepowered.api.world.volume.block.ImmutableBlockVolume;
import org.spongepowered.api.world.volume.block.UnmodifiableBlockVolume;
import org.spongepowered.api.world.volume.block.worker.MutableBlockVolumeWorker;
import org.spongepowered.api.world.volume.tileentity.worker.MutableTileEntityWorker;
import org.spongepowered.common.util.VecHelper;

import java.util.Collection;
import java.util.Optional;

public abstract class AbstractWrapperChunk<C extends IChunk, A extends ProtoChunk<A>> implements ProtoChunk<A> {

    private final Vector3i blockMin;
    private final Vector3i blockMax;
    private final Vector3i size;
    private final C chunk;

    public AbstractWrapperChunk(Vector3i blockMin, Vector3i blockMax, Vector3i size, C chunk) {
        this.blockMin = blockMin;
        this.blockMax = blockMax;
        this.size = size;
        this.chunk = chunk;
    }

    protected C getChunk() {
        return (C) this.chunk;
    }

    @Override
    public void addEntity(Entity entity) {

    }

    @Override
    public ChunkState getState() {
        return (ChunkState) (Object) getChunk().getStatus();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Vector3i getChunkPosition() {
        return VecHelper.toVec3i(getChunk().getPos());
    }

    @Override
    public Vector3i getBiomeMin() {
        return this.blockMin;
    }

    @Override
    public Vector3i getBiomeMax() {
        return this.blockMax;
    }

    @Override
    public Vector3i getBiomeSize() {
        return this.size;
    }

    @Override
    public UnmodifiableBiomeVolume<?> asUnmodifiableBiomeVolume() {
        return null;
    }

    @Override
    public ImmutableBiomeVolume asImmutableBiomeVolume() {
        return null;
    }

    @Override
    public void addTileEntity(int x, int y, int z, TileEntity tileEntity) {

    }

    @Override
    public void removeTileEntity(int x, int y, int z) {

    }

    @Override
    public Collection<TileEntity> getTileEntities() {
        return null;
    }

    @Override
    public Optional<TileEntity> getTileEntity(int x, int y, int z) {
        return Optional.empty();
    }

    @Override
    public Vector3i getBlockMin() {
        return this.blockMin;
    }

    @Override
    public Vector3i getBlockMax() {
        return this.blockMax;
    }

    @Override
    public Vector3i getBlockSize() {
        return this.size;
    }

    @Override
    public boolean containsBlock(int x, int y, int z) {
        return VecHelper.inBounds(x, y, z, this.blockMin, this.blockMax);
    }

    @Override
    public boolean isAreaAvailable(int x, int y, int z) {
        return VecHelper.inBounds(x, y, z, this.blockMin, this.blockMax);
    }

    @Override
    public A getView(Vector3i newMin, Vector3i newMax) {
        return null;
    }

    @Override
    public MutableTileEntityWorker<A> getTileEntityWorker() {
        return null;
    }

    @Override
    public ScheduledTaskList<FluidType> getPendingFluidTicks() {
        return null;
    }

    @Override
    public ScheduledTaskList<BlockType> getPendingBlockTicks() {
        return null;
    }

    @Override
    public int getLight(LightType type, int x, int y, int z) {
        return getChunk().getLight(null, new BlockPos(x, y, z), false);
    }

    @Override
    public int getLight(int x, int y, int z) {
        return getLight(LightTypes.BLOCK, x, y, z);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockState block) {
        return getChunk().setBlockState(new BlockPos(x, y, z,), (IBlockState) block);
    }

    @Override
    public boolean removeBlock(int x, int y, int z) {
        return getChunk().setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), false) != Blocks.AIR.getDefaultState();
    }

    @Override
    public MutableBlockVolumeWorker<A> getBlockWorker() {
        return null;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return getChunk().getBlockState(new BlockPos(x, y, z));
    }

    @Override
    public FluidState getFluid(int x, int y, int z) {
        return null;
    }

    @Override
    public UnmodifiableBlockVolume<?> asUnmodifiableBlockVolume() {
        return null;
    }

    @Override
    public ImmutableBlockVolume asImmutableBlockVolume() {
        return null;
    }

    @Override
    public int getHighestYAt(int x, int z) {
        return 0;
    }
}
