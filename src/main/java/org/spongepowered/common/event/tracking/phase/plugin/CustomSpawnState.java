package org.spongepowered.common.event.tracking.phase.plugin;

public class CustomSpawnState extends PluginPhaseState<CustomSpawnState.Context> {

    @Override
    public Context start() {
        return new Context();
    }

    public static final class Context extends PluginPhaseContext<Context> {

        protected Context() {
            super(PluginPhase.State.CUSTOM_SPAWN);
        }
    }

}
