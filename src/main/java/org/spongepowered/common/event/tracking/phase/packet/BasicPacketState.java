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
package org.spongepowered.common.event.tracking.phase.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.common.event.InternalNamedCauses;
import org.spongepowered.common.event.tracking.CauseTracker;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.phase.TrackingPhase;
import org.spongepowered.common.event.tracking.phase.TrackingPhases;
import org.spongepowered.common.interfaces.IMixinChunk;
import org.spongepowered.common.interfaces.block.IMixinBlockEventData;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;

import java.util.Optional;

public abstract class BasicPacketState<P extends PacketContext<P>> implements IPhaseState<P>, IPacketState<P> {

    BasicPacketState() {

    }

    @Override
    public final TrackingPhase getPhase() {
        return TrackingPhases.PACKET;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void unwind(P context) {
        if (this == PacketPhase.General.INVALID) { // Invalid doesn't capture any packets.
            return;
        }
        final Packet<?> packetIn = context.firstNamed(InternalNamedCauses.Packet.CAPTURED_PACKET, Packet.class).get();
        final EntityPlayerMP player = context.getSource(EntityPlayerMP.class).get();
        final Class<? extends Packet<?>> packetInClass = (Class<? extends Packet<?>>) packetIn.getClass();

        final PacketFunction unwindFunction = PacketPhase.getInstance().packetUnwindMap.get(packetInClass);
        if (unwindFunction != null) {
            unwindFunction.unwind(packetIn, this, player, context);
        } else {
            PacketFunction.UNKNOWN_PACKET.unwind(packetIn, this, player, context);
        }
    }

    @Override
    public boolean matches(int packetState) {
        return false;
    }

    @Override
    public void addNotifierToBlockEvent(P context, IMixinWorldServer mixinWorldServer, BlockPos pos, IMixinBlockEventData blockEvent) {

    }

    @Override
    public boolean populateCauseForNotifyNeighborEvent(P context, Cause.Builder builder, CauseTracker causeTracker,
        IMixinChunk mixinChunk, BlockPos pos) {
        final Optional<User> notifier = context.firstNamed(NamedCause.NOTIFIER, User.class);
        if (notifier.isPresent()) {
            builder.named(NamedCause.notifier(notifier.get()));
            return true;
        } else {
            mixinChunk.getBlockNotifier(pos).ifPresent(user -> builder.named(NamedCause.notifier(user)));
            mixinChunk.getBlockOwner(pos).ifPresent(owner -> builder.named(NamedCause.owner(owner)));
        }
        final Player player = context.getSource(Player.class)
            .orElseThrow(TrackingUtil.throwWithContext("Processing a Player PAcket, expecting a player, but had none!", context));
        builder.named(NamedCause.notifier(player));
        return true;
    }
}
