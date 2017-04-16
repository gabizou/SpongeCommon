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
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.spawn.BlockSpawnCause;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.event.tracking.DefaultPhaseContext;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.registry.type.event.InternalSpawnTypes;

import java.util.ArrayList;

final class DispensePhaseState extends BlockPhaseState<DispensePhaseState, DefaultPhaseContext> {

    DispensePhaseState() {
    }



    @SuppressWarnings("unchecked")
    @Override
    void unwind(DefaultPhaseContext phaseContext) {
        final BlockSnapshot blockSnapshot = phaseContext.getSource(BlockSnapshot.class)
                .orElseThrow(TrackingUtil.throwWithContext("Could not find a block dispensing items!", phaseContext));
        final World spongeWorld = blockSnapshot.getLocation().get().getExtent();
        phaseContext.getCapturedBlockSupplier()
                .ifPresentAndNotEmpty(blockSnapshots -> TrackingUtil.processBlockCaptures(blockSnapshots, this, phaseContext));
        phaseContext.getCapturedItemsSupplier()
                .ifPresentAndNotEmpty(items -> {
                    final Cause.Builder builder = Cause.source(BlockSpawnCause.builder()
                            .block(blockSnapshot)
                            .type(InternalSpawnTypes.DISPENSE)
                            .build());
                    phaseContext.getNotifier()
                            .ifPresent(builder::notifier);
                    phaseContext.getOwner()
                            .ifPresent(builder::owner);

                    final Cause cause = builder
                            .build();
                    final ArrayList<Entity> entities = new ArrayList<>();
                    for (EntityItem item : items) {
                        entities.add(EntityUtil.fromNative(item));
                    }
                    final DropItemEvent.Dispense
                            event =
                            SpongeEventFactory.createDropItemEventDispense(cause, entities, spongeWorld);
                    SpongeImpl.postEvent(event);
                    if (!event.isCancelled()) {
                        for (Entity entity : event.getEntities()) {
                            EntityUtil.getMixinWorld(entity).forceSpawnEntity(entity);
                        }
                    }
                });
        phaseContext.getCapturedEntitySupplier()
                .ifPresentAndNotEmpty(entities -> {
                    final Cause.Builder builder = Cause.source(BlockSpawnCause.builder()
                            .block(blockSnapshot)
                            .type(InternalSpawnTypes.DISPENSE)
                            .build());
                    phaseContext.getNotifier()
                            .ifPresent(builder::notifier);
                    phaseContext.getOwner()
                            .ifPresent(builder::owner);

                    final Cause cause = builder
                            .build();
                    final SpawnEntityEvent
                            event =
                            SpongeEventFactory.createSpawnEntityEvent(cause, entities, spongeWorld);
                    SpongeImpl.postEvent(event);
                    final User user = phaseContext.getNotifier().orElseGet(() -> phaseContext.getOwner().orElse(null));
                    if (!event.isCancelled()) {
                        for (Entity entity : event.getEntities()) {
                            if (user != null) {
                                EntityUtil.toMixin(entity).setCreator(user.getUniqueId());
                            }
                            EntityUtil.getMixinWorld(entity).forceSpawnEntity(entity);
                        }
                    }
                });
    }

    @Override
    public DefaultPhaseContext start() {
        return PhaseContext.start();
    }
}
