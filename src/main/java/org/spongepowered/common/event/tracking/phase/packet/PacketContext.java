package org.spongepowered.common.event.tracking.phase.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;

public class PacketContext<P extends PacketContext<P>> extends PhaseContext<P> {

    EntityPlayerMP packetPlayer;
    Packet<?> packet;

    protected PacketContext(IPhaseState<P> state) {
        super(state);
    }



}
