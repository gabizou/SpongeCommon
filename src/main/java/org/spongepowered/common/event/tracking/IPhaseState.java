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
package org.spongepowered.common.event.tracking;

import net.minecraft.block.Block;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.entity.teleport.TeleportCause;
import org.spongepowered.api.event.cause.entity.teleport.TeleportTypes;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.tracking.phase.TrackingPhase;
import org.spongepowered.common.event.tracking.phase.entity.EntityPhase;
import org.spongepowered.common.event.tracking.phase.general.ExplosionContext;
import org.spongepowered.common.event.tracking.phase.tick.TickPhase;
import org.spongepowered.common.interfaces.IMixinChunk;
import org.spongepowered.common.interfaces.block.IMixinBlockEventData;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;
import org.spongepowered.common.registry.type.event.InternalSpawnTypes;
import org.spongepowered.common.world.BlockChange;

import java.util.ArrayList;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 * A literal phase state of which the {@link World} is currently running
 * in. The state itself is owned by {@link TrackingPhase}s as the phase
 * defines what to do upon
 * As these should be enums, there's no data that should be stored on
 * this state. It can have control flow with {@link #canSwitchTo(IPhaseState)}
 * where preventing switching to another state is possible (likely points out
 * either errors or runaway states not being unwound).
 */
public interface IPhaseState<P extends PhaseContext<P>> {

    TrackingPhase getPhase();

    P start();

    // Stateful accessors

    default boolean canSwitchTo(IPhaseState<?> state) {
        return false;
    }

    default boolean ignoresBlockTracking() {
        return false;
    }

    default void handleBlockChangeWithUser(@Nullable BlockChange blockChange, Transaction<BlockSnapshot> snapshotTransaction, P context) {

    }

    default boolean tracksBlockRestores() {
        return false;
    }

    default boolean tracksBlockSpecificDrops() {
        return false;
    }

    /**
     * A simple boolean switch to whether an {@link net.minecraft.entity.EntityLivingBase#onDeath(DamageSource)}
     * should enter a specific phase to handle the destructed drops until either after this current phase
     * has completed (if returning {@code true}) or whether the entity is going to enter a specific
     * phase directly to handle entity drops (if returning {@code false}). Most all phases should
     * return true, except certain few that require it. The reasoning for a phase to return
     * {@code false} would be if it's own phase can handle entity drops with appropriate causes
     * on it's own.
     *
     * @return True if this phase is aware enough to handle entity death drops per entity, or will
     *     cause {@link EntityPhase.State#DEATH} to be entered and handle it's own drops
     */
    default boolean tracksEntitySpecificDrops() {
        return false;
    }

    default boolean ignoresEntityCollisions() {
        return false;
    }

    default boolean isExpectedForReEntrance() {
        return false;
    }

    default boolean tracksEntityDeaths() {
        return false;
    }

    default boolean shouldCaptureBlockChangeOrSkip(P phaseContext, BlockPos pos) {
        return true;
    }

    default boolean isInteraction() {
        return false;
    }

    default void postTrackBlock(BlockSnapshot snapshot, CauseTracker tracker, P context) { }

    default boolean requiresBlockPosTracking() {
        return false;
    }

    default void unwind(P context) { }
    default boolean requiresBlockCapturing() {
        return false;
    }

    default void processPostEntitySpawns(P phaseContext, ArrayList<Entity> entities) {
        final Optional<User> creator = Optional.ofNullable(phaseContext
            .getNotifier()
            .orElseGet(() ->
                phaseContext.getOwner()
                    .orElse(null)));
        TrackingUtil.splitAndSpawnEntities(InternalSpawnTypes.UNKNOWN_CAUSE,
            entities,
            entity -> creator
                .map(User::getUniqueId)
                .ifPresent(entity::setCreator)
        );


    }

    default boolean allowEntitySpawns() {
        return true;
    }

    /**
     * This is SteIPhaseState 3 of entity spawning. It is used for the sole purpose of capturing an entity spawn
     * and doesn't actually spawn an entity into the world until the current phase is unwound.
     * The method itself should technically capture entity spawns, however, in the event it
     * is required that the entity cannot be captured, returning {@code false} will mark it
     * to spawn into the world, bypassing any of the bulk spawn events or capturing.
     *
     * <p>NOTE: This method should only be called and handled if and only if {@link IPhaseState#allowEntitySpawns()}
     * returns {@code true}. Violation of this will have unforseen consequences.</p>
     *
     *
     * @param context The current context
     * @param entity The entity being captured
     * @param chunkX The chunk x position
     * @param chunkZ The chunk z position
     * @return True if the entity was successfully captured
     */
    default boolean spawnEntityOrCapture(P context, Entity entity, int chunkX, int chunkZ) {
        final net.minecraft.entity.Entity minecraftEntity = (net.minecraft.entity.Entity) entity;
        final WorldServer minecraftWorld = (WorldServer) minecraftEntity.worldObj;
        final User user = context.getNotifier().orElseGet(() -> context.getOwner().orElse(null));
        if (user != null) {
            entity.setCreator(user.getUniqueId());
        }
        final ArrayList<Entity> entities = new ArrayList<>(1);
        entities.add(entity);
        final SpawnEntityEvent event = SpongeEventFactory.createSpawnEntityEvent(InternalSpawnTypes.UNKNOWN_CAUSE,
            entities, (World) minecraftWorld);
        SpongeImpl.postEvent(event);
        if (!event.isCancelled() && event.getEntities().size() > 0) {
            for (Entity item: event.getEntities()) {
                ((IMixinWorldServer) item.getWorld()).forceSpawnEntity(item);
            }
            return true;
        }
        return false;
    }
    default Cause generateTeleportCause(P context) {
        return Cause.of(NamedCause.source(TeleportCause.builder().type(TeleportTypes.UNKNOWN).build()));
    }

    default boolean populateCauseForNotifyNeighborEvent(P context, Cause.Builder builder, CauseTracker causeTracker,
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

    default boolean appendPreBlockProtectedCheck(Cause.Builder builder, P context, CauseTracker causeTracker) {
        if (context.getSource(Player.class).isPresent()) {
            builder.named(NamedCause.notifier(context.getSource(Player.class).get()));
            return true;
        }
        return false;
    }

    /**
     * Associates any notifiers and owners for tracking as to what caused
     * the next {@link TickPhase.Tick} to enter for a block to be updated.
     * The interesting thing is that since the current state and context
     * are already known, we can associate the notifiers/owners appropriately.
     * This may have the side effect of a long winded "bubble down" from
     * a single lever pull to blocks getting updated hundreds of blocks
     * away.
     *  @param mixinWorld
     * @param pos
     * @param context
     * @param newContext
     */
    default void appendNotifierPreBlockTick(IMixinWorldServer mixinWorld, BlockPos pos, P context, PhaseContext<?> newContext) {
        final Chunk chunk = mixinWorld.asMinecraftWorld().getChunkFromBlockCoords(pos);
        final IMixinChunk mixinChunk = (IMixinChunk) chunk;
        if (chunk != null && !chunk.isEmpty()) {
            mixinChunk.getBlockOwner(pos).ifPresent(newContext::owner);
            mixinChunk.getBlockNotifier(pos).ifPresent(newContext::notifier);
        }
    }
    default void appendContextPreExplosion(ExplosionContext phaseContext, P context) {

    }

    default void addNotifierToBlockEvent(P context, IMixinWorldServer mixinWorld, BlockPos pos, IMixinBlockEventData blockEvent) {

    }
    default void associateNeighborStateNotifier(P context, BlockPos sourcePos, Block block, BlockPos notifyPos, WorldServer mixinWorld,
        PlayerTracker.Type notifier) {

    }
    // Context aware methods (Merged contexts into states, since the states themselves are just classes)

}
