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
import org.spongepowered.api.data.value.BoundedValue;

import java.lang.reflect.Method;
import java.util.function.Predicate;

public class CustomDataBuilder<T extends DataManipulator<T, I>, I extends ImmutableDataManipulator<I, T>> implements CustomDataProvider.TypeBuilder<T, I> {

    public CustomDataBuilder(Class<T> manipulatorClass, Class<I> immutableClass) {
        final DataImpl data = new DataImpl();
        data.dataInterface = checkNotNull(manipulatorClass, "DataManipulator class cannot be null!");
        data.immutableDataInterface = checkNotNull(immutableClass, "ImmutableDataManipulator class cannot be null!");
        data.manipulatorClassName = Type.getInternalName(data.dataInterface) + "_Impl";

        for (Method method : manipulatorClass.getMethods()) {
            final KeyValue annotation = method.getAnnotation(KeyValue.class);
            if (annotation != null) {
                final String value = annotation.value();

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
