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

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.registry.type.event.InternalSpawnTypes;

import java.util.ArrayList;

class WeatherTickPhaseState extends TickPhaseState {

    WeatherTickPhaseState() {
    }

    @Override
    public void processPostTick(TickContext phaseContext) {
        phaseContext.getCapturedEntitySupplier().ifPresentAndNotEmpty(entities -> {
            final Cause.Builder builder = Cause.source(SpawnCause.builder()
                    .type(InternalSpawnTypes.WEATHER)
                    .build());
            final SpawnEntityEvent
                    spawnEntityEvent =
                    SpongeEventFactory.createSpawnEntityEvent(builder.build(), entities, entities.get(0).getWorld());
            SpongeImpl.postEvent(spawnEntityEvent);
            for (Entity entity : spawnEntityEvent.getEntities()) {
                EntityUtil.getMixinWorld(entity).forceSpawnEntity(entity);
            }
        });
        phaseContext.getCapturedBlockSupplier().ifPresentAndNotEmpty(blockSnapshots -> {
            TrackingUtil.processBlockCaptures(blockSnapshots, this, phaseContext);
        });
    }
    @Override
    public void associateAdditionalBlockChangeCauses(TickContext context, Cause.Builder builder) {

    }

    @Override
    public boolean spawnEntityOrCapture(PhaseContext<?> context, Entity entity, int chunkX, int chunkZ) {
        final Cause.Builder builder = Cause.source(SpawnCause.builder()
                .type(InternalSpawnTypes.WEATHER)
                .build());
        final ArrayList<Entity> capturedEntities = new ArrayList<>();
        capturedEntities.add(entity);
        final SpawnEntityEvent
                spawnEntityEvent =
                SpongeEventFactory.createSpawnEntityEvent(builder.build(), capturedEntities, entity.getWorld());
        SpongeImpl.postEvent(spawnEntityEvent);
        if (!spawnEntityEvent.isCancelled()) {
            for (Entity anEntity : spawnEntityEvent.getEntities()) {
                EntityUtil.getMixinWorld(anEntity).forceSpawnEntity(anEntity);
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "WeatherTickPhase";
    }
}
