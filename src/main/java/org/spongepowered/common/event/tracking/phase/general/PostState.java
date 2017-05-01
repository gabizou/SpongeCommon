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
package org.spongepowered.common.event.tracking.phase.general;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.InternalNamedCauses;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseData;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.UnwindingPhaseContext;
import org.spongepowered.common.event.tracking.phase.TrackingPhases;
import org.spongepowered.common.event.tracking.phase.block.BlockPhase;

import java.util.ArrayList;
import java.util.List;

final class PostState extends GeneralState<UnwindingPhaseContext> {

    @Override
    public UnwindingPhaseContext start() {
        throw new IllegalArgumentException("Cannot construct an UnwindingPhaseContext normally!");
    }

    @Override
    public boolean canSwitchTo(IPhaseState<?> state) {
        return state.getPhase() == TrackingPhases.GENERATION || state == BlockPhase.State.RESTORING_BLOCKS;
    }

    @Override
    public boolean tracksBlockRestores() {
        return false; // TODO - check that this really is needed.
    }

    @Override
    public void unwind(UnwindingPhaseContext context) {
        final IPhaseState<?> unwindingState = context.firstNamed(InternalNamedCauses.Tracker.UNWINDING_STATE, IPhaseState.class)
                .orElseThrow(TrackingUtil.throwWithContext("Expected to be unwinding a phase, but no phase found!", context));
        final PhaseContext<?> unwindingContext = context.firstNamed(InternalNamedCauses.Tracker.UNWINDING_CONTEXT, PhaseContext.class)
                .orElseThrow(TrackingUtil.throwWithContext("Expected to be unwinding a phase, but no context found!", context));
        this.postDispatch(unwindingState, unwindingContext, context);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void appendContextPreExplosion(ExplosionContext phaseContext, UnwindingPhaseContext context) {
        final PhaseContext<?> unwinding = context.first(PhaseContext.class)
                .orElseThrow(TrackingUtil.throwWithContext("Expected to be unwinding with a PhaseContext, but couldn't!", context));
        final IPhaseState<?> phaseState = context.first(IPhaseState.class)
                .orElseThrow(TrackingUtil.throwWithContext("Expected to be unwinding with an IPhaseState, but couldn't!", context));
        ((IPhaseState) phaseState).appendContextPreExplosion(phaseContext, unwinding);

    }

    @Override
    public boolean spawnEntityOrCapture(UnwindingPhaseContext context, Entity entity, int chunkX, int chunkZ) {
        return context.getCapturedEntities().add(entity);
    }

    /**
     * This is the post dispatch method that is automatically handled for
     * states that deem it necessary to have some post processing for
     * advanced game mechanics. This is always performed when capturing
     * has been turned on during a phases's
     * dispatched. The rules of post dispatch are as follows:
     * - Entering extra phases is not allowed: This is to avoid
     *  potential recursion in various corner cases.
     * - The unwinding phase context is provided solely as a root
     *  cause tracking for any nested notifications that require
     *  association of causes
     * - The unwinding phase is used with the unwinding state to
     *  further exemplify during what state that was unwinding
     *  caused notifications. This narrows down to the exact cause
     *  of the notifications.
     * - post dispatch may loop several times until no more notifications
     *  are required to be dispatched. This may include block physics for
     *  neighbor notification events.
     * @param unwindingState The state that was unwinding
     * @param unwindingContext The context of the state that was unwinding,
     *     contains the root cause for the state
     * @param postContext The post dispatch context captures containing any
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void postDispatch(IPhaseState<?> unwindingState, PhaseContext<?> unwindingContext, UnwindingPhaseContext postContext) {
        final List<BlockSnapshot> contextBlocks = postContext.getCapturedBlockSupplier().orEmptyList();
        final List<Entity> contextEntities = postContext.getCapturedEntitySupplier().orEmptyList();
        final List<Entity> contextItems = (List<Entity>) (List<?>) postContext.getCapturedItemsSupplier().orEmptyList();
        if (contextBlocks.isEmpty() && contextEntities.isEmpty() && contextItems.isEmpty()) {
            return;
        }
        if (!contextBlocks.isEmpty()) {
            final List<BlockSnapshot> blockSnapshots = new ArrayList<>(contextBlocks);
            contextBlocks.clear();
            GeneralPhase.processBlockTransactionListsPost(postContext, blockSnapshots, unwindingState, unwindingContext);
        }
        if (!contextEntities.isEmpty()) {
            final ArrayList<Entity> entities = new ArrayList<>(contextEntities);
            contextEntities.clear();
            ((IPhaseState) unwindingState).processPostEntitySpawns(unwindingContext, entities);
        }
        if (!contextItems.isEmpty()) {
            final ArrayList<Entity> items = new ArrayList<>(contextItems);
            contextItems.clear();
            unwindingState.getPhase().processPostItemSpawns(unwindingState, items);
        }

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void associateNeighborStateNotifier(UnwindingPhaseContext context, BlockPos sourcePos, Block block, BlockPos notifyPos,
        WorldServer mixinWorld, PlayerTracker.Type notifier) {
        final IPhaseState unwindingState = context.firstNamed(InternalNamedCauses.Tracker.UNWINDING_STATE, IPhaseState.class)
            .orElseThrow(TrackingUtil.throwWithContext("Intended to be unwinding a phase but no phase unwinding found!", context));
        final PhaseContext<?> unwindingContext = context.firstNamed(InternalNamedCauses.Tracker.UNWINDING_CONTEXT, PhaseContext.class)
            .orElseThrow(TrackingUtil.throwWithContext("Intended to be unwinding a phase with a context, but no context found!", context));
        unwindingState
            .associateNeighborStateNotifier(unwindingContext, sourcePos, block, notifyPos, mixinWorld, notifier);
    }
}
