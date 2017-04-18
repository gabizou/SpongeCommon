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
package org.spongepowered.common.event.tracking.phase.plugin;

import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.phase.block.BlockPhaseState;
import org.spongepowered.common.event.tracking.phase.entity.EntityPhaseState;
import org.spongepowered.common.event.tracking.phase.generation.GenerationPhase;

/**
 * Used for tasks scheduled with both the Sponge scheduler, and the built-in 'scheduled task' system in MinecraftServer
 */
public class ScheduledTaskPhaseState extends PluginPhaseState<ScheduledTaskPhaseState.ScheduledContext> {

    @Override
    public ScheduledContext start() {
        return new ScheduledContext();
    }

    public static final class ScheduledContext extends PluginPhaseContext<ScheduledContext> {

        protected ScheduledContext() {
            super(PluginPhase.State.SCHEDULED_TASK);
        }
    }

    @Override
    public boolean canSwitchTo(IPhaseState<?> state) {
        return state instanceof BlockPhaseState || state instanceof EntityPhaseState || state == GenerationPhase.State.TERRAIN_GENERATION;
    }

    @Override
    public void unwind(ScheduledContext phaseContext) {
        phaseContext.getCapturedBlockSupplier().ifPresentAndNotEmpty(blocks -> {
            TrackingUtil.processBlockCaptures(blocks, this, phaseContext);
        });
    }

}
