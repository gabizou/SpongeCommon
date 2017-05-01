package org.spongepowered.common.event.tracking.phase.general;

import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;

public class GeneralPhaseContext<C extends GeneralPhaseContext<C>> extends PhaseContext<C> {

    public GeneralPhaseContext(IPhaseState<C> state) {
        super(state);
    }
}
