package org.spongepowered.common.data.generator;

import org.spongepowered.common.data.generator.strategy.GetterValueStrategy;
import org.spongepowered.common.data.generator.strategy.KeySerializationStrategy;

import java.lang.reflect.Type;

import javax.annotation.Nullable;

public final class ValueGroupInfo {

    public final String matchedNameId;
    public final Type resolvedType;
    public final String valueDescriptor;
    public String generatedId;
    @Nullable public String getterDescriptor;
    @Nullable public String setterDescriptor;
    public final GetterValueStrategy resolvedValueStrategy;
    public final KeySerializationStrategy keySerializationStrategy;

    public ValueGroupInfo(String matchedNameId, Type resolvedType, String valueDescriptor,
            GetterValueStrategy resolvedValueStrategy, KeySerializationStrategy keySerializationStrategy) {
        this.matchedNameId = matchedNameId;
        this.resolvedType = resolvedType;
        this.valueDescriptor = valueDescriptor;
        this.resolvedValueStrategy = resolvedValueStrategy;
        this.keySerializationStrategy = keySerializationStrategy;
    }



}
