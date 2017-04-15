package org.spongepowered.common.event.tracking.phase.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import org.spongepowered.common.event.tracking.PhaseContext;

public class PacketContext extends PhaseContext<PacketContext> {

    EntityPlayerMP packetPlayer;
    Packet<?> packet;

    protected PacketContext() {
    }



}
