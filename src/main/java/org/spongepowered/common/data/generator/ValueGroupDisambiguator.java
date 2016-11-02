package org.spongepowered.common.data.generator;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.common.data.generator.strategy.KeySerializationGenerator;
import org.spongepowered.common.data.generator.strategy.ValueStrategyFactory;

import javax.annotation.Nullable;

public class ValueGroupDisambiguator {

    // Step 1 : Find matched value method and name from annotation
    public String matchedNameId;
    public String valueDescriptor;

    // Step 2 : Locate any getters and setters, if available
    @Nullable public String getterDescriptor;
    @Nullable public String setterDescriptor;

    // Step 3 : Gather and determine the generated id based on the key
    public Key key;
    public TypeToken resolvedType;
    public String generatedId;

    // Step 3a : Retrieved from the Key and TypeToken, determine whether we're a bounded value instance
    @Nullable public Object defaultValue;
    @Nullable public Object minimumValue;
    @Nullable public Object maximumValue;

    // Step 4 : After resolved types are defined
    public ValueStrategyFactory.StrategyType resolvedValueStrategy;
    public KeySerializationGenerator.DeserializationType keySerialization;


}
