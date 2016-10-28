package org.spongepowered.common.data.generator;

import com.google.common.reflect.TypeToken;
import org.spongepowered.common.data.generator.strategy.GetterValueStrategy;
import org.spongepowered.common.data.generator.strategy.KeySerializationStrategy;

import java.lang.reflect.Type;

import javax.annotation.Nullable;

public class ValueGroupDisambiguator {

    // Step 1 : Find matched value method and name from annotation
    public String matchedNameId;
    public String valueDescriptor;

    // Step 2 : Locate any getters and setters, if available
    @Nullable public String getterDescriptor;
    @Nullable public String setterDescriptor;

    // Step 3 : Gather and determine the generated id based on the key
    public TypeToken resolvedType;
    public String generatedId;

    // Step 4 : After resolved types are defined
    public GetterValueStrategy resolvedValueStrategy;
    public KeySerializationStrategy keySerializationStrategy;


}
