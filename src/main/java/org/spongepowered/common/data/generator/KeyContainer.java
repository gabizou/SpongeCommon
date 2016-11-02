package org.spongepowered.common.data.generator;

import org.spongepowered.api.data.key.Key;
import org.spongepowered.common.data.generator.key.KeyObject;

public class KeyContainer {

    public Key key;
    public String name;
    public boolean requiresBaseField;
    public String baseFieldType;

    // Assigned and generated during class generation
    public String staticFieldName;
    public String baseFieldName;

}
