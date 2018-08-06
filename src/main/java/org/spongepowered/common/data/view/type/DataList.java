package org.spongepowered.common.data.view.type;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.common.data.view.DataObject;

import java.util.List;

public abstract class DataList extends DataObject {

    private final List<?> list;

    public DataList(DataQuery query, TypeToken<?> token, List<?> list) {
        super(query, token);
        this.list = list;
    }

    public
}
