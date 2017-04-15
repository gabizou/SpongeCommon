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
package org.spongepowered.common.event.tracking.phase;

import com.google.common.base.Objects;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.entity.teleport.TeleportCause;
import org.spongepowered.api.event.cause.entity.teleport.TeleportTypes;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.tracking.CauseTracker;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseData;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.phase.packet.PacketPhase;
import org.spongepowered.common.event.tracking.phase.tick.TickPhase;
import org.spongepowered.common.interfaces.IMixinChunk;
import org.spongepowered.common.interfaces.block.IMixinBlockEventData;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;
import org.spongepowered.common.registry.type.event.InternalSpawnTypes;

import java.util.ArrayList;
import java.util.Optional;

import javax.annotation.Nullable;

public abstract class TrackingPhase {

    public void processPostItemSpawns(IPhaseState<?> unwindingState, ArrayList<Entity> items) {
        TrackingUtil.splitAndSpawnEntities(InternalSpawnTypes.UNKNOWN_CAUSE, items);
    }



    // Default methods that are basic qualifiers, leaving uIPhaseState to the phase and state to decide
    // whether they perform capturing.


    // TODO
    public boolean ignoresBlockUpdateTick(PhaseData phaseData) {
        return false;
    }

    public boolean ignoresBlockEvent(IPhaseState<?> phaseState) {
        return false;
    }

    public boolean ignoresScheduledUpdates(IPhaseState<?> phaseState) {
        return false;
    }

    public boolean alreadyCapturingBlockTicks(IPhaseState<?> phaseState, PhaseContext<?> context) {
        return false;
    }

    public boolean alreadyCapturingEntitySpawns(IPhaseState<?> state) {
        return false;
    }

    public boolean alreadyCapturingEntityTicks(IPhaseState<?> state) {
        return false;
    }

    public boolean alreadyCapturingTileTicks(IPhaseState<?> state) {
        return false;
    }

    public boolean requiresPost(IPhaseState<?> state) {
        return true;
    }

    public boolean alreadyCapturingItemSpawns(IPhaseState<?> currentState) {
        return false;
    }

    public boolean ignoresItemPreMerging(IPhaseState<?> currentState) {
        return false;
    }

    public boolean isWorldGeneration(IPhaseState<?> state) {
        return false;
    }

    public boolean doesCaptureEntityDrops(IPhaseState<?> currentState) {
        return false;
    }

    public void associateAdditionalCauses(IPhaseState<?> state, PhaseContext<?> context, Cause.Builder builder) {

    }


    public boolean isRestoring(IPhaseState<?> state, PhaseContext<?> context, int updateFlag) {
        return false;
    }

    public void capturePlayerUsingStackToBreakBlock(@Nullable ItemStack itemStack, EntityPlayerMP playerMP, IPhaseState<?> state, PhaseContext<?> context,
            CauseTracker causeTracker) {

    }

    // Actual capture methods


    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .toString();
    }

    public Optional<DamageSource> createDestructionDamageSource(IPhaseState<?> state, PhaseContext<?> context, net.minecraft.entity.Entity entity) {
        return Optional.empty();
    }

    public void addNotifierToBlockEvent(IPhaseState<?> phaseState, PhaseContext<?> context, IMixinWorldServer mixinWorld, BlockPos pos, IMixinBlockEventData blockEvent) {

    }

    public void associateNeighborStateNotifier(IPhaseState<?> state, PhaseContext<?> context, @Nullable BlockPos sourcePos, Block block, BlockPos notifyPos,
            WorldServer minecraftWorld, PlayerTracker.Type notifier) {

    }

    public boolean populateCauseForNotifyNeighborEvent(IPhaseState<?> state, PhaseContext<?> context, Cause.Builder builder, CauseTracker causeTracker,
            IMixinChunk mixinChunk, BlockPos pos) {
        final Optional<User> notifier = context.firstNamed(NamedCause.NOTIFIER, User.class);
        if (notifier.isPresent()) {
            builder.named(NamedCause.notifier(notifier.get()));
            return true;
        } else {
            mixinChunk.getBlockNotifier(pos).ifPresent(user -> builder.named(NamedCause.notifier(user)));
            mixinChunk.getBlockOwner(pos).ifPresent(owner -> builder.named(NamedCause.owner(owner)));
        }
        return true;
    }

    public boolean isTicking(IPhaseState<?> state) {
        return false;
    }

    public boolean handlesOwnPhaseCompletion(IPhaseState<?> state) {
        return false;
    }

    public void appendContextPreExplosion(PhaseContext<?> phaseContext, PhaseData currentPhaseData) {

    }

    public void appendExplosionCause(PhaseData phaseData) {

    }
}
