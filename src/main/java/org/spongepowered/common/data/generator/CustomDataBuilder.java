package org.spongepowered.common.data.generator;

import static com.google.common.base.Preconditions.checkNotNull;

import org.objectweb.asm.Type;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.manipulator.generator.CustomDataProvider;
import org.spongepowered.api.data.manipulator.generator.DataRegistration;
import org.spongepowered.api.data.manipulator.generator.KeyValue;
import org.spongepowered.api.data.value.BoundedValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.common.data.SpongeDataManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class CustomDataBuilder<T extends DataManipulator<T, I>, I extends ImmutableDataManipulator<I, T>> implements CustomDataProvider.TypeBuilder<T, I> {

    private static final String MULTIPLE_VALUE_METHODS = "Multiple value methods are using the same name! %s and %s are both using %s, which is not allowed";
    private static final String MISSING_VALUE_METHOD = "Found a getter/setter method for %1$s with the method %2$s, but there's no value getter annotated with the name of %1$s";
    private static final String KEY_ALREADY_REGISTERED = "The key id for %1$s is already linked to %3$s, cannot link to a different key %2$s!";
    private static final String MISSING_VALUE_METHOD_FOR_KEY = "The key id, %s, is not linked to any value methods! They Key: %s is not being used!";
    private DataImpl data;
    private List<ValueGroupDisambiguator> disambiguators = new ArrayList<>();

    public CustomDataBuilder(Class<T> manipulatorClass, Class<I> immutableClass) {
        this.data = new DataImpl();
        this.data.dataInterface = checkNotNull(manipulatorClass, "DataManipulator class cannot be null!");
        this.data.immutableDataInterface = checkNotNull(immutableClass, "ImmutableDataManipulator class cannot be null!");
        this.data.manipulatorClassName = Type.getInternalName(this.data.dataInterface) + "_Impl";

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
                    for (ValueGroupDisambiguator disambiguator : this.disambiguators) {
                        if (disambiguator.matchedNameId.equals(value)) {
                            throw new IllegalStateException(String.format(MULTIPLE_VALUE_METHODS, method.getName(), disambiguator.matchedNameId, disambiguator.matchedNameId));
                        }
                    }
                    this.disambiguators.add(valueGroupDisambiguator);
                }
            }
        }

        // At this stage, all we have are the found ValueGroupDisambiguators targeting the "id"s and the value "methods".
        // Now we need to resolve any "getter"s and "setter"s
        for (Method method : manipulatorClass.getMethods()) {
            final KeyValue annotation = method.getAnnotation(KeyValue.class);
            if (annotation != null) {
                final String value = annotation.value();
                if (!Value.class.isAssignableFrom(method.getReturnType())) { // Filter any non-value methods, since we already located them in the previous pass
                    ValueGroupDisambiguator resolvedDisambiguator = null;
                    for (ValueGroupDisambiguator disambiguator : this.disambiguators) {
                        if (disambiguator.matchedNameId.equals(value)) {
                            resolvedDisambiguator = disambiguator;
                            break;
                        }
                    }
                    if (resolvedDisambiguator == null) {
                        throw new IllegalStateException(String.format(MISSING_VALUE_METHOD, value, method.getName()));
                    }

                }
            }
        }


    }

    @Override
    public CustomDataProvider.TypeBuilder<T, I> key(Key<?> key, String id) throws IllegalArgumentException {
        // Always check that the key has not already been registered
        SpongeDataManager.getInstance().validateKeyRegistration(key, this.data.dataInterface);

        // Basically going to search for the disambiguator we already created for the desired id,
        // If we don't have one for the id, then the developer didn't associate it correctly. If they did
        // but are registering multiple keys to the same annotation id, then they're not using it right.
        // In layman's terms, you should only register a key to a single id, never two id's for the same key,
        // nor should you register two keys to the same id. It's a very strict 1:1 relationship.
        for (ValueGroupDisambiguator disambiguator : this.disambiguators) {
            if (disambiguator.matchedNameId.equals(id)) {
                if (disambiguator.key != null) {
                    throw new IllegalStateException(String.format(KEY_ALREADY_REGISTERED, id, key.getId(), disambiguator.key.getId()));
                }
                disambiguator.key = key;
                disambiguator.resolvedType = key.getElementToken();

                return this;
            }
        }
        throw new IllegalStateException(String.format(MISSING_VALUE_METHOD_FOR_KEY, id, key.getId()));
    }

    @Override
    public <E> CustomDataProvider.TypeBuilder<T, I> boundedKey(Key<? extends BoundedValue<E>> key, String id, E defaultValue, E lowerBound,
            E upperBound)
            throws IllegalArgumentException {
        // Always check that the key has not already been registered
        SpongeDataManager.getInstance().validateKeyRegistration(key, this.data.dataInterface);

        // Basically going to search for the disambiguator we already created for the desired id,
        // If we don't have one for the id, then the developer didn't associate it correctly. If they did
        // but are registering multiple keys to the same annotation id, then they're not using it right.
        // In layman's terms, you should only register a key to a single id, never two id's for the same key,
        // nor should you register two keys to the same id. It's a very strict 1:1 relationship.
        for (ValueGroupDisambiguator disambiguator : this.disambiguators) {
            if (disambiguator.matchedNameId.equals(id)) {
                if (disambiguator.key != null) {
                    throw new IllegalStateException(String.format(KEY_ALREADY_REGISTERED, id, key.getId(), disambiguator.key.getId()));
                }
                disambiguator.key = key;
                disambiguator.resolvedType = key.getElementToken();

                disambiguator.minimumValue = lowerBound;
                disambiguator.maximumValue = upperBound;
                disambiguator.defaultValue = defaultValue;
                return this;
            }
        }
        throw new IllegalStateException(String.format(MISSING_VALUE_METHOD_FOR_KEY, id, key.getId()));
    }

    @Override
    public CustomDataProvider.TypeBuilder<T, I> predicate(Predicate<? extends DataHolder> predicate) throws IllegalArgumentException {
        this.data.applyPredicate = checkNotNull(predicate, "Predicate cannot be null!");
        return this;
    }

    @Override
    public CustomDataProvider.TypeBuilder<T, I> version(int contentVersion) {
        this.data.version = contentVersion;
        return this;
    }

    @Override
    public DataRegistration<T, I> build(Object pluginInstance, String id) throws IllegalArgumentException, IllegalStateException {
        if (this.disambiguators.isEmpty()) {
            throw new IllegalStateException()
        }
        return null;
    }
}
