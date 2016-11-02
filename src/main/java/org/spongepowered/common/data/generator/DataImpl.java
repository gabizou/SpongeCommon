package org.spongepowered.common.data.generator;

import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;

import java.util.List;
import java.util.function.Predicate;

public class DataImpl {

    Class<? extends DataManipulator<?, ?>> dataInterface;
    Class<? extends ImmutableDataManipulator<?, ?>> immutableDataInterface;
    public String manipulatorClassName;
    public String manipulatorDescriptor;
    public String immutableClassName;
    List<ValueGroupInfo> valueGroups;
    List<KeyContainer> containers;
    Predicate<? extends DataHolder> applyPredicate;
    String name;

    Object plugin;
    public int version;
}
