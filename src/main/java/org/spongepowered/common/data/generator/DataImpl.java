package org.spongepowered.common.data.generator;

import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;

import java.util.List;

public class DataImpl {

    Class<? extends DataManipulator<?, ?>> dataInterface;
    Class<? extends ImmutableDataManipulator<?, ?>> immutableDataInterface;
    public String manipulatorClassName;
    public String manipulatorDescriptor;
    public String immutableClassName;
    List<KeyContainer> containers;
    String name;

    Object plugin;
}
