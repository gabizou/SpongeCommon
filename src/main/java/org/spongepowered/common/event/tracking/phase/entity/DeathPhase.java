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
package org.spongepowered.common.event.tracking.phase.entity;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.ExperienceOrb;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.spawn.EntitySpawnCause;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.event.InternalNamedCauses;
import org.spongepowered.common.event.tracking.ItemDropData;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.registry.type.event.InternalSpawnTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

final class DeathPhase extends EntityPhaseState {

    DeathPhase() {

    }

    @Override
    public boolean tracksBlockSpecificDrops() {
        return true;
    }

    @Override
    void unwind(PhaseContext<?> context) {
        final Entity dyingEntity =
                context.getSource(Entity.class)
                        .orElseThrow(TrackingUtil.throwWithContext("Dying entity not found!", context));
        final DamageSource damageSource = context.firstNamed(InternalNamedCauses.General.DAMAGE_SOURCE, DamageSource.class).get();
        final Cause cause = Cause.source(
                EntitySpawnCause.builder()
                        .entity(dyingEntity)
                        .type(InternalSpawnTypes.DROPPED_ITEM)
                        .build())
                .named(InternalNamedCauses.General.DAMAGE_SOURCE, damageSource)
                .build();
        final boolean isPlayer = dyingEntity instanceof EntityPlayer;
        final EntityPlayer entityPlayer = isPlayer ? (EntityPlayer) dyingEntity : null;
        final Optional<User> notifier = context.getNotifier();
        final Optional<User> owner = context.getOwner();
        final User entityCreator = notifier.orElseGet(() -> owner.orElse(null));
        context.getCapturedEntitySupplier()
                .ifPresentAndNotEmpty(entities -> {
                    // Separate experience orbs from other entity drops
                    final List<Entity> experience = entities.stream()
                            .filter(entity -> entity instanceof ExperienceOrb)
                            .collect(Collectors.toList());
                    if (!experience.isEmpty()) {
                        final Cause experienceCause = Cause.source(
                                EntitySpawnCause.builder()
                                        .entity(dyingEntity)
                                        .type(InternalSpawnTypes.EXPERIENCE)
                                        .build())
                                .named(InternalNamedCauses.General.DAMAGE_SOURCE, damageSource)
                                .build();
                        final SpawnEntityEvent
                                spawnEntityEvent =
                                SpongeEventFactory.createSpawnEntityEvent(experienceCause, experience, dyingEntity.getWorld());
                        SpongeImpl.postEvent(spawnEntityEvent);
                        if (!spawnEntityEvent.isCancelled()) {
                            for (Entity entity : spawnEntityEvent.getEntities()) {
                                EntityUtil.getMixinWorld(entity).forceSpawnEntity(entity);
                            }
                        }
                    }

                    // Now process other entities, this is separate from item drops specifically
                    final List<Entity> other = entities.stream()
                            .filter(entity -> !(entity instanceof ExperienceOrb))
                            .collect(Collectors.toList());
                    if (!other.isEmpty()) {
                        final Cause otherCause = Cause.source(
                                EntitySpawnCause.builder()
                                        .entity(dyingEntity)
                                        .type(InternalSpawnTypes.ENTITY_DEATH)
                                        .build())
                                .named(InternalNamedCauses.General.DAMAGE_SOURCE, damageSource)
                                .build();
                        final SpawnEntityEvent
                                spawnEntityEvent =
                                SpongeEventFactory.createSpawnEntityEvent(otherCause, experience, dyingEntity.getWorld());
                        SpongeImpl.postEvent(spawnEntityEvent);
                        if (!spawnEntityEvent.isCancelled()) {
                            for (Entity entity : spawnEntityEvent.getEntities()) {
                                EntityUtil.getMixinWorld(entity).forceSpawnEntity(entity);
                            }
                        }
                    }
                });
        // Forge always fires a living drop event even if nothing was captured
        // This allows mods such as Draconic Evolution to add items to the drop list
        if (context.getCapturedEntityItemDropSupplier().isEmpty() && context.getCapturedEntityDropSupplier().isEmpty()) {
            final ArrayList<Entity> entities = new ArrayList<>();
            final DropItemEvent.Destruct destruct = SpongeEventFactory.createDropItemEventDestruct(cause, entities, dyingEntity.getWorld());
            SpongeImpl.postEvent(destruct);
            if (!destruct.isCancelled()) {
                for (Entity entity : destruct.getEntities()) {
                    EntityUtil.getMixinWorld(entity).forceSpawnEntity(entity);
                }
            }
            return;
        }
        context.getCapturedEntityItemDropSupplier().ifPresentAndNotEmpty(map -> {
            final Collection<EntityItem> items = map.get(dyingEntity.getUniqueId());
            final ArrayList<Entity> entities = new ArrayList<>();
            for (EntityItem item : items) {
                entities.add(EntityUtil.fromNative(item));
            }

            if (isPlayer) {
                // Forge and Vanilla always clear items on player death BEFORE drops occur
                // This will also provide the highest compatibility with mods such as Tinkers Construct
                entityPlayer.inventory.clear();
            }

            final DropItemEvent.Destruct
                    destruct =
                    SpongeEventFactory.createDropItemEventDestruct(cause, entities, dyingEntity.getWorld());
            SpongeImpl.postEvent(destruct);
            if (!destruct.isCancelled()) {
                for (Entity entity : destruct.getEntities()) {
                   EntityUtil.getMixinWorld(entity).forceSpawnEntity(entity);
                }
            }

             // Note: If cancelled, the items do not spawn in the world and are NOT copied back to player inventory.
             // This avoids many issues with mods such as Tinkers Construct's soulbound items.
        });
        // Note that this is only used if and when item pre-merging is enabled.
        context.getCapturedEntityDropSupplier().ifPresentAndNotEmpty(map -> {
            final Collection<ItemDropData> itemStacks = map.get(dyingEntity.getUniqueId());
            if (itemStacks.isEmpty()) {
                return;
            }
            final List<ItemDropData> items = new ArrayList<>();
            items.addAll(itemStacks);

            if (!items.isEmpty()) {
                final net.minecraft.entity.Entity minecraftEntity = EntityUtil.toNative(dyingEntity);
                final List<Entity> itemEntities = items.stream()
                        .map(data -> data.create((WorldServer) minecraftEntity.worldObj))
                        .map(EntityUtil::fromNative)
                        .collect(Collectors.toList());

                if (isPlayer) {
                    // Forge and Vanilla always clear items on player death BEFORE drops occur
                    // This will also provide the highest compatibility with mods such as Tinkers Construct
                    entityPlayer.inventory.clear();
                }

                final DropItemEvent.Destruct
                        destruct =
                        SpongeEventFactory.createDropItemEventDestruct(cause, itemEntities, dyingEntity.getWorld());
                SpongeImpl.postEvent(destruct);
                if (!destruct.isCancelled()) {
                    for (Entity entity : destruct.getEntities()) {
                        if (entityCreator != null) {
                            EntityUtil.toMixin(entity).setCreator(entityCreator.getUniqueId());
                        }
                        EntityUtil.getMixinWorld(entity).forceSpawnEntity(entity);
                    }
                }

                // Note: If cancelled, the items do not spawn in the world and are NOT copied back to player inventory.
                // This avoids many issues with mods such as Tinkers Construct's soulbound items.
            }

        });

    }
}
