package org.spongepowered.common.data.generator;

import org.spongepowered.common.data.generator.strategy.KeySerialization;
import org.spongepowered.common.data.generator.strategy.ValueStrategyFactory;

import java.lang.reflect.Type;

import javax.annotation.Nullable;

public final class ValueGroupInfo {

    public final String matchedNameId;
    public final Type resolvedType;
    public final String valueDescriptor;
    public String generatedId;
    @Nullable public String getterDescriptor;
    @Nullable public String setterDescriptor;
    public final ValueStrategyFactory.StrategyType resolvedValueStrategy;
    public final KeySerialization keySerialization;

    public ValueGroupInfo(String matchedNameId, Type resolvedType, String valueDescriptor,
            ValueStrategyFactory.StrategyType resolvedValueStrategy, KeySerialization keySerialization) {
        this.matchedNameId = matchedNameId;
        this.resolvedType = resolvedType;
        this.valueDescriptor = valueDescriptor;
        this.resolvedValueStrategy = resolvedValueStrategy;
        this.keySerialization = keySerialization;
    }



}
