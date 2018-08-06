package org.spongepowered.common.data.view;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataQuery;

public abstract class DataObject {

    private final DataQuery query;
    private final TypeToken<?> token;

    public DataObject(DataQuery query, TypeToken<?> token) {
        this.query = query;
        this.token = token;
    }

    public final DataQuery getQuery() {
        return this.query;
    }

    public TypeToken<?> getToken() {
        return this.token;
    }
}
