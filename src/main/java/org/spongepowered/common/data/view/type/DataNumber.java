package org.spongepowered.common.data.view.type;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.common.data.view.DataObject;

public abstract class DataNumber extends DataObject {

    public DataNumber(DataQuery query, TypeToken<?> token) {
        super(query, token);
    }

    public abstract byte asByte();

    public abstract short asShort();

    public abstract int asInt();

    public abstract long asLong();

    public abstract float asFloat();

    public abstract double asDouble();
}
