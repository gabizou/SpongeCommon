package org.spongepowered.common.event.tracking.phase.block;

import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;

public class BlockContext<T extends BlockContext<T>> extends PhaseContext<T> {

    protected BlockContext(IPhaseState<T> phaseState) {
        super(phaseState);
    }
}
