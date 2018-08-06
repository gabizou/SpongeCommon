package org.spongepowered.common.data.view;

import org.spongepowered.api.CatalogType;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.BaseValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

public abstract class BaseDataView implements DataView {

    @Nullable private final DataContainer parent;
    private final DataObject object;
    // our current object. DataView knows exactly what type it can support because

    public BaseDataView(@Nullable DataContainer parent, DataObject object) {
        this.parent = parent;
        this.object = object;
    }

    @Override
    public DataContainer getContainer() {
        return this.parent;
    }

    @Override
    public DataQuery getCurrentPath() {
        return this.object.getQuery();
    }

    @Override
    public String getName() {
        return this.object.getQuery().toString();
    }

    @Override
    public Optional<DataView> getParent() {
        return Optional.empty();
    }

    public Set<DataQuery> getKeys(boolean deep) {
        return null;
    }

    @Override
    public Map<DataQuery, Object> getValues(boolean deep) {
        return null;
    }

    @Override
    public boolean contains(DataQuery path) {
        return false;
    }

    @Override
    public boolean contains(DataQuery path, DataQuery... paths) {
        return false;
    }

    @Override
    public Optional<Object> get(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public DataView set(DataQuery path, Object value) {
        return null;
    }

    @Override
    public <E> DataView set(Key<? extends BaseValue<E>> key, E value) {
        return null;
    }

    @Override
    public DataView remove(DataQuery path) {
        return null;
    }

    @Override
    public DataView createView(DataQuery path) {
        return null;
    }

    @Override
    public DataView createView(DataQuery path, Map<?, ?> map) {
        return null;
    }

    @Override
    public Optional<DataView> getView(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<? extends Map<?, ?>> getMap(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> getBoolean(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<Short> getShort(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<Byte> getByte(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getInt(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> getLong(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<Float> getFloat(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<Double> getDouble(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getString(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<List<?>> getList(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<List<String>> getStringList(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<List<Character>> getCharacterList(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<List<Boolean>> getBooleanList(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<List<Byte>> getByteList(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<List<Short>> getShortList(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<List<Integer>> getIntegerList(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<List<Long>> getLongList(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<List<Float>> getFloatList(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<List<Double>> getDoubleList(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<List<Map<?, ?>>> getMapList(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public Optional<List<DataView>> getViewList(DataQuery path) {
        return Optional.empty();
    }

    @Override
    public <T extends DataSerializable> Optional<T> getSerializable(DataQuery path, Class<T> clazz) {
        return Optional.empty();
    }

    @Override
    public <T extends DataSerializable> Optional<List<T>> getSerializableList(DataQuery path, Class<T> clazz) {
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getObject(DataQuery path, Class<T> objectClass) {
        return Optional.empty();
    }

    @Override
    public <T> Optional<List<T>> getObjectList(DataQuery path, Class<T> objectClass) {
        return Optional.empty();
    }

    @Override
    public <T extends CatalogType> Optional<T> getCatalogType(DataQuery path, Class<T> catalogType) {
        return Optional.empty();
    }

    @Override
    public <T extends CatalogType> Optional<List<T>> getCatalogTypeList(DataQuery path, Class<T> catalogType) {
        return Optional.empty();
    }

    @Override
    public DataContainer copy() {
        return null;
    }

    @Override
    public DataContainer copy(SafetyMode safety) {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public SafetyMode getSafetyMode() {
        return null;
    }
}
