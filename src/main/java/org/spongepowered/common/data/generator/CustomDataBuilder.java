package org.spongepowered.common.data.generator;

import static com.google.common.base.Preconditions.checkNotNull;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.manipulator.generator.CustomDataProvider;
import org.spongepowered.api.data.manipulator.generator.DataRegistration;
import org.spongepowered.api.data.manipulator.generator.KeyValue;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.BoundedValue;
import org.spongepowered.api.data.value.mutable.Value;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class CustomDataBuilder<T extends DataManipulator<T, I>, I extends ImmutableDataManipulator<I, T>> implements CustomDataProvider.TypeBuilder<T, I> {

    private DataImpl data;
    private List<ValueGroupDisambiguator> disambiguators = new ArrayList<>();

    public CustomDataBuilder(Class<T> manipulatorClass, Class<I> immutableClass) {
        final DataImpl data = new DataImpl();
        data.dataInterface = checkNotNull(manipulatorClass, "DataManipulator class cannot be null!");
        data.immutableDataInterface = checkNotNull(immutableClass, "ImmutableDataManipulator class cannot be null!");
        data.manipulatorClassName = Type.getInternalName(data.dataInterface) + "_Impl";

        // First pass is to resolve all Value getter methods
        for (Method method : manipulatorClass.getMethods()) {
            final KeyValue annotation = method.getAnnotation(KeyValue.class);
            if (annotation != null) {
                final String value = annotation.value();
                if (Value.class.isAssignableFrom(method.getReturnType())) {
                    // We found a value returning method
                    final ValueGroupDisambiguator valueGroupDisambiguator = new ValueGroupDisambiguator();
                    valueGroupDisambiguator.matchedNameId = value;
                    valueGroupDisambiguator.valueDescriptor = method.getName();
                }
            }
        }
    }

    @Override
    public CustomDataProvider.TypeBuilder<T, I> key(Key<?> key, String id) throws IllegalArgumentException {
        return null;
    }

    @Override
    public <E> CustomDataProvider.TypeBuilder<T, I> boundedKey(Key<? extends BoundedValue<E>> key, String id, E lowerBound, E upperBound)
            throws IllegalArgumentException {
        return null;
    }

    @Override
    public CustomDataProvider.TypeBuilder<T, I> predicate(Predicate<? extends DataHolder> predicate) throws IllegalArgumentException {
        return null;
    }

    @Override
    public CustomDataProvider.TypeBuilder<T, I> version(int contentVersion) {
        return null;
    }

    @Override
    public DataRegistration<T, I> build(Object pluginInstance, String id) throws IllegalArgumentException, IllegalStateException {
        return null;
    }
}
