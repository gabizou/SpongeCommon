package org.spongepowered.common.event.tracking.phase.generation;

import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.gen.Populator;
import org.spongepowered.api.world.gen.PopulatorType;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;

import javax.annotation.Nullable;

public class GenerationContext extends PhaseContext<GenerationContext> {


    private World world;
    private PopulatorType populatorType;

    protected GenerationContext(IPhaseState<GenerationContext> state) {
        super(state);
    }

    public GenerationContext world(World world) {
        this.world = world;
        return this;
    }

    public GenerationContext world(IMixinWorldServer worldServer) {
        this.world = worldServer.asSpongeWorld();
        return this;
    }

    public GenerationContext world(net.minecraft.world.World worldServer) {
        this.world = (World) worldServer;
        return this;
    }

    public World getWorld() throws IllegalStateException {
        if (this.world == null) {
            throw new IllegalStateException("Expected to be capturing a world during generation!");
        }
        return this.world;
    }

    public GenerationContext populator(PopulatorType type) {
        this.populatorType = type;
        return this;
    }

    @Nullable
    public PopulatorType getPopulatorTypeOrNull() {
        return this.populatorType;
    }

    public PopulatorType getPopulatorType() throws IllegalStateException {
        if (this.populatorType == null) {
            throw new IllegalStateException("Expected to be capturing a populator!");
        }
        return this.populatorType;
    }



}
