package org.spongepowered.common.data.generator;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.Queries;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.generator.testing.DummyCustomDataData;
import org.spongepowered.api.data.manipulator.generator.testing.DummyManipulator;
import org.spongepowered.api.data.manipulator.generator.testing.ImmutableDummyManipulator;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.meta.ItemEnchantment;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.common.data.value.SpongeValueFactory;
import org.spongepowered.common.data.value.mutable.SpongeListValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DummyManipulatorImpl implements DummyManipulator {

    // Pulled in from the TypeBuilder
    private static final Key<MutableBoundedValue<Integer>> MY_INT_KEY$aabc000111 = DummyCustomDataData.MY_INT_KEY; // pretend this is not illegal
    private static final Key<ListValue<ItemEnchantment>> MY_ENCHANTMENT_KEY$aabbcc000011 = DummyCustomDataData.MY_ENCHANTMENT_KEY; // pretend this is not illegal
    private static final ImmutableSet<Key<?>> KEYS = ImmutableSet.of(MY_INT_KEY$aabc000111, MY_ENCHANTMENT_KEY$aabbcc000011);

    private int myInt$aabc0001;
    private List<ItemEnchantment> myEnchantment$aabcls001;

    DummyManipulatorImpl() {
    }

    @Override
    public Value<Integer> myInt() {
        return SpongeValueFactory.boundedBuilder(MY_INT_KEY$aabc000111)
                .defaultValue(0)
                .minimum(0)
                .maximum(10)
                .actualValue(this.myInt$aabc0001)
                .build();
    }

    @Override
    public ListValue<ItemEnchantment> enchantment() {
        return new SpongeListValue<>(MY_ENCHANTMENT_KEY$aabbcc000011, this.myEnchantment$aabcls001);
    }

    @Override
    public int myIntValue() {
        return this.myInt$aabc0001;
    }

    @Override
    public void setMyInt(int value) {
        this.myInt$aabc0001 = value;
    }

    @Override
    public List<ItemEnchantment> getEnchantment() {
        return this.myEnchantment$aabcls001;
    }

    @Override
    public void setEnchantment(List<ItemEnchantment> enchantment) {
        this.myEnchantment$aabcls001 = checkNotNull(enchantment, "myEnchantment cannot be null!");
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        return new MemoryDataContainer()
                .set(Queries.CONTENT_VERSION, 1)
                .set(MY_INT_KEY$aabc000111.getQuery(), this.myInt$aabc0001)
                .set(MY_ENCHANTMENT_KEY$aabbcc000011.getQuery(), this.myEnchantment$aabcls001);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> Optional<E> get(Key<? extends BaseValue<E>> key) {
        if (((Object) key) == MY_INT_KEY$aabc000111) {
            return Optional.of((E) (Object) this.myInt$aabc0001);
        } else if (((Object) key) == MY_ENCHANTMENT_KEY$aabbcc000011) {
            return Optional.of((E) (Object) this.myEnchantment$aabcls001);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E, V extends BaseValue<E>> Optional<V> getValue(Key<V> key) {
        if (key == MY_INT_KEY$aabc000111) {
            return Optional.of((V) SpongeValueFactory.boundedBuilder(MY_INT_KEY$aabc000111)
                    .defaultValue(0)
                    .minimum(0)
                    .maximum(10)
                    .actualValue(this.myInt$aabc0001)
                    .build());
        }
        if (key == MY_ENCHANTMENT_KEY$aabbcc000011) {
            return Optional.of((V) new SpongeListValue<>(MY_ENCHANTMENT_KEY$aabbcc000011, this.myEnchantment$aabcls001));
        }
        return Optional.empty();
    }

    @Override
    public boolean supports(Key<?> key) {
        return key == MY_INT_KEY$aabc000111 || key == MY_ENCHANTMENT_KEY$aabbcc000011;
    }


    @Override
    public Set<Key<?>> getKeys() {
        return KEYS;
    }

    @Override
    public Set<ImmutableValue<?>> getValues() {
        final HashSet<ImmutableValue<?>> values = new HashSet<>();
        values.add(myInt().asImmutable());
        values.add(enchantment().asImmutable());
        return values;
    }

    @Override
    public Optional<DummyManipulator> fill(DataHolder dataHolder, MergeFunction overlap) {
        return null;
    }

    @Override
    public Optional<DummyManipulator> from(DataContainer container) {
        if (container.contains(MY_INT_KEY$aabc000111.getQuery())) {
            this.myInt$aabc0001 = container.getInt(MY_INT_KEY$aabc000111.getQuery()).get();
        }
        if (container.contains(MY_ENCHANTMENT_KEY$aabbcc000011.getQuery())) {
            this.myEnchantment$aabcls001 = container.getSerializableList(MY_ENCHANTMENT_KEY$aabbcc000011.getQuery(), ItemEnchantment.class).get();
        }
        return Optional.of(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> DummyManipulator set(Key<? extends BaseValue<E>> key, E value) {
        if (((Object) key) == MY_INT_KEY$aabc000111) {
            this.myInt$aabc0001 = (int) (Object) value;
        }
        if (((Object) key) == MY_ENCHANTMENT_KEY$aabbcc000011) {
            this.myEnchantment$aabcls001 = (List) value;
        }
        return this;
    }

    @Override
    public DummyManipulator copy() {
        final DummyManipulatorImpl dummyManipulator = new DummyManipulatorImpl();
        dummyManipulator.myInt$aabc0001 = this.myInt$aabc0001;
        final ArrayList<ItemEnchantment> objects = new ArrayList<>();
        for (ItemEnchantment itemEnchantment : this.myEnchantment$aabcls001) {
            objects.add(itemEnchantment);
        }
        dummyManipulator.myEnchantment$aabcls001 = objects;
        return dummyManipulator;
    }

    @Override
    public ImmutableDummyManipulator asImmutable() {
        return null;
    }
}
