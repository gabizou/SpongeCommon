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
package org.spongepowered.common.event.tracking.phase.tick;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.phase.generation.GenerationPhase;
import org.spongepowered.common.interfaces.IMixinChunk;
import org.spongepowered.common.interfaces.world.IMixinLocation;
import org.spongepowered.common.world.BlockChange;

import javax.annotation.Nullable;

abstract class LocationBasedTickPhaseState extends TickPhaseState {

    LocationBasedTickPhaseState() {
    }

    abstract Location<World> getLocationSourceFromContext(PhaseContext<?> context);

    abstract LocatableBlock getLocatableBlockSourceFromContext(PhaseContext<?> context);

    @Override
    public void associateNeighborBlockNotifier(PhaseContext<?> context, @Nullable BlockPos sourcePos, Block block, BlockPos notifyPos,
            WorldServer minecraftWorld, PlayerTracker.Type notifier) {
        if (sourcePos == null) {
            LocatableBlock locatableBlock = this.getLocatableBlockSourceFromContext(context);
            sourcePos = ((IMixinLocation)(Object) locatableBlock.getLocation()).getBlockPos();
        }
        User user = context.getNotifier().orElse(TrackingUtil.getNotifierOrOwnerFromBlock(minecraftWorld, sourcePos));
        if (user != null) {
            final IMixinChunk mixinChunk = (IMixinChunk) minecraftWorld.getChunkFromBlockCoords(notifyPos);
            mixinChunk.addTrackedBlockPosition(block, notifyPos, user, PlayerTracker.Type.NOTIFIER);
        }
    }

    @Override
    public void handleBlockChangeWithUser(@Nullable BlockChange blockChange,
        Transaction<BlockSnapshot> snapshotTransaction, PhaseContext<?> context) {
        final Location<World> location = getLocatableBlockSourceFromContext(context).getLocation();
        final Block block = (Block) snapshotTransaction.getOriginal().getState().getType();
        final Location<World> changedLocation = snapshotTransaction.getOriginal().getLocation().get();
        final BlockPos changedBlockPos = ((IMixinLocation)(Object) changedLocation).getBlockPos();
        final IMixinChunk changedMixinChunk = (IMixinChunk) ((WorldServer) changedLocation.getExtent()).getChunkFromBlockCoords(changedBlockPos);
        final User user = context.getNotifier().orElse(TrackingUtil.getNotifierOrOwnerFromBlock(location));
        if (user != null) {
            changedMixinChunk.addTrackedBlockPosition(block, changedBlockPos, user, PlayerTracker.Type.NOTIFIER);
        }
    }


    @Override
    public boolean canSwitchTo(IPhaseState<?> state) {
        return super.canSwitchTo(state) || state == GenerationPhase.State.CHUNK_LOADING;
    }

}
