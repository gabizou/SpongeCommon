package org.spongepowered.common.event.tracking.phase.packet;

import org.spongepowered.common.event.tracking.IPhaseState;

public class BasicPacketContext extends PacketContext<BasicPacketContext> {

    public BasicPacketContext(IPhaseState<BasicPacketContext> state) {
        super(state);
    }
}
