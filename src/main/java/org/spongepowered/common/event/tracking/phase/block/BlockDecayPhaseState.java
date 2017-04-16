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
package org.spongepowered.common.event.tracking.phase.block;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.entity.spawn.LocatableBlockSpawnCause;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.interfaces.IMixinChunk;
import org.spongepowered.common.interfaces.world.IMixinLocation;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;
import org.spongepowered.common.registry.type.event.InternalSpawnTypes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

final class BlockDecayPhaseState extends BlockPhaseState<BlockDecayPhaseState, BlockDecayPhaseState.DecayContext> {

    BlockDecayPhaseState() {
    }

    @Override
    public DecayContext start() {
        return new DecayContext();
    }

    public static final class DecayContext extends BlockContext<DecayContext> {

        private LocatableBlock block;
        private IMixinWorldServer worldServer;
        private BlockPos position;

        public DecayContext block(LocatableBlock block) {
            this.block = block;
            final Location<World> location = block.getLocation();
            this.position = ((IMixinLocation) (Object) location).getBlockPos();
            this.worldServer = (IMixinWorldServer) location.getExtent();
            return this;
        }

        public LocatableBlock getBlock() throws IllegalStateException {
            if (this.block == null) {
                throw new IllegalStateException("Expected to be ticking over a location!");
            }
            return this.block;
        }

        public BlockPos getPosition() {
            if (this.position == null) {
                throw new IllegalStateException("Expected to be ticking over a location!");
            }
            return this.position;
        }

        public IMixinWorldServer getWorld() {
            if (this.worldServer == null) {
                throw new IllegalStateException("Expected to be ticking over a location!");
            }
            return this.worldServer;
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    void unwind(DecayContext context) {
        final LocatableBlock locatable = context.getBlock();
        final BlockPos blockPos = context.getPosition();
        final IMixinWorldServer mixinWorld = context.getWorld();
        final IMixinChunk mixinChunk = (IMixinChunk) mixinWorld.asMinecraftWorld().getChunkFromBlockCoords(blockPos);
        final Optional<User> notifier = mixinChunk.getBlockNotifier(blockPos);
        final Optional<User> creator = mixinChunk.getBlockOwner(blockPos);

        context.getCapturedItemsSupplier()
                .ifPresentAndNotEmpty(items -> {
                    // Nothing happens here yet for some reason.
                });
        context.getCapturedEntitySupplier()
                .ifPresentAndNotEmpty(entities -> {
                    final Cause.Builder builder = Cause.source(LocatableBlockSpawnCause.builder()
                            .locatableBlock(locatable)
                            .type(InternalSpawnTypes.BLOCK_SPAWNING)
                            .build());
                    context.getNotifier()
                            .ifPresent(builder::notifier);
                    context.getOwner()
                            .ifPresent(builder::owner);

                    final Cause cause = builder
                            .build();
                    final SpawnEntityEvent
                            event =
                            SpongeEventFactory.createSpawnEntityEvent(cause, entities, mixinWorld.asSpongeWorld());
                    SpongeImpl.postEvent(event);
                    if (!event.isCancelled()) {
                        for (Entity entity : event.getEntities()) {
                            EntityUtil.getMixinWorld(entity).forceSpawnEntity(entity);
                        }
                    }
                });
        context.getCapturedBlockSupplier()
                .ifPresentAndNotEmpty(blocks ->TrackingUtil.processBlockCaptures(blocks, this, context));
        context.getCapturedItemStackSupplier()
                .ifPresentAndNotEmpty(drops -> {
                    final List<EntityItem> items = drops.stream()
                            .map(drop -> drop.create(mixinWorld.asMinecraftWorld()))
                            .collect(Collectors.toList());
                    final Cause.Builder builder = Cause.source(
                            LocatableBlockSpawnCause.builder()
                                    .locatableBlock(locatable)
                                    .type(InternalSpawnTypes.BLOCK_SPAWNING)
                                    .build()
                    );
                    notifier.ifPresent(user -> builder.named(NamedCause.notifier(user)));
                    creator.ifPresent(user -> builder.named(NamedCause.owner(user)));
                    final Cause cause = builder.build();
                    final List<Entity> entities = (List<Entity>) (List<?>) items;
                    if (!entities.isEmpty()) {
                        DropItemEvent.Custom event = SpongeEventFactory.createDropItemEventCustom(cause, entities, mixinWorld.asSpongeWorld());
                        SpongeImpl.postEvent(event);
                        if (!event.isCancelled()) {
                            for (Entity droppedItem : event.getEntities()) {
                                mixinWorld.forceSpawnEntity(droppedItem);
                            }
                        }
                    }
                });
    }
}
