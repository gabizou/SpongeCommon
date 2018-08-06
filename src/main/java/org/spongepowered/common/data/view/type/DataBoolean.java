package org.spongepowered.common.data.view.type;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.util.TypeTokens;
import org.spongepowered.common.data.view.DataObject;

public class DataBoolean extends DataObject {

    private final boolean value;

    public DataBoolean(DataQuery query, boolean value) {
        super(query, TypeTokens.BOOLEAN_TOKEN);
        this.value = value;
    }

    public boolean getValue() {
        return this.value;
    }

}
