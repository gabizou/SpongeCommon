/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.util.collection;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectCollections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

@SuppressWarnings("unchecked")
public class Int2ObjectArrayBiMap<V> implements Int2ObjectBiMap<V> {

    private int[] keys = new int[0];
    private Object[] values = new Object[0];
    private int size;

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    private int findKey(final int key) {
        final int[] keys = this.keys;
        for (int i = this.size; i-- != 0;) {
            if (keys[i] == key) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean containsKey(int key) {
        return findKey(key) != -1;
    }

    @Override
    public boolean containsValue(V value) {
        for (int i = this.size; i-- != 0;) {
            if (this.values[i] == null ? value == null : this.values[i].equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(int key) {
        final int[] keys = this.keys;
        for (int i = this.size; i-- != 0;) {
            if (keys[i] == key) {
                return (V) this.values[i];
            }
        }
        return null;
    }

    @Override
    public V put(int key, @Nullable V value) {
        final int oldKey = findKey(key);
        if (oldKey != -1) {
            final V oldValue = (V) this.values[oldKey];
            throw new IllegalArgumentException("value already present: " + oldValue);
        }
        if (this.size == this.keys.length) {
            final int[] newKeys = new int[this.size == 0 ? 2 : this.size * 2];
            final Object[] newValues = new Object[this.size == 0 ? 2 : this.size * 2];
            for (int i = this.size; i-- != 0;) {
                newKeys[i] = this.keys[i];
                newValues[i] = this.values[i];
            }
            this.keys = newKeys;
            this.values = newValues;
        }
        this.keys[this.size] = key;
        this.values[this.size] = value;
        this.size++;
        return value;
    }

    @Override
    public V remove(int key) {
        final int oldKey = findKey(key);
        if (oldKey == -1) {
            return null;
        }
        final V oldValue = (V) this.values[oldKey];
        final int tail = this.size - oldKey - 1;
        System.arraycopy(this.keys, oldKey + 1, this.keys, oldKey, tail);
        System.arraycopy(this.values, oldKey + 1, this.values, oldKey, tail);
        this.size--;
        this.values[this.size] = null;
        return oldValue;
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends V> map) {

        int n = map.size();
        final Iterator<? extends Map.Entry<? extends Integer, ? extends V>> i = map
            .entrySet().iterator();
        if (map instanceof Int2ObjectMap) {
            Int2ObjectMap.Entry<? extends V> e;
            while (n-- != 0) {
                e = (Int2ObjectMap.Entry<? extends V>) i.next();
                put(e.getIntKey(), e.getValue());
            }
        } else {
            Map.Entry<? extends Integer, ? extends V> e;
            while (n-- != 0) {
                e = i.next();
                put(e.getKey(), e.getValue());
            }
        }

    }

    @Override
    public void clear() {
        for (int i = this.size; i-- != 0;) {
            this.values[i] = null;
        }
        this.size = 0;
    }

    @Override
    public IntSet keySet() {
        return new IntArraySet(this.keys, this.size);
    }

    @Override
    public ObjectCollection<V> values() {
        return ObjectCollections
            .unmodifiable(new ObjectArraySet<V>(this.values, this.size));
    }

    @Override
    public Set<Int2ObjectMap.FastEntrySet<V>> entrySet() {
        return null;
    }

    @Override
    public Object2IntBiMap<V> inverse() {
        return new Inverse();
    }

    private final class Inverse implements Object2IntBiMap<V> {

        @Override
        public int size() {
            return Int2ObjectArrayBiMap.this.size;
        }

        @Override
        public boolean isEmpty() {
            return Int2ObjectArrayBiMap.this.isEmpty();
        }

        @Override
        public boolean containsKey(V key) {
            return forward().containsValue(key);
        }

        @Override
        public boolean containsValue(int value) {
            return false;
        }

        @Override
        public int get(V key) {
            final Object[] values = Int2ObjectArrayBiMap.this.values;
            for (int i = Int2ObjectArrayBiMap.this.size; i-- != 0;) {
                if (values[i] == key) {
                    return Int2ObjectArrayBiMap.this.keys[i];
                }
            }
            return -1;
        }

        @Override
        public int put(@Nullable V key, int value) {
            forward().put(value, key);
            return value;
        }

        @Override
        public int remove(V key) {
            return 0;
        }

        @Override
        public void putAll(Map<? extends V, ? extends Integer> map) {

        }

        @Override
        public void clear() {

        }

        @Override
        public Collection<V> keySet() {
            return forward().values();
        }

        @Override
        public IntSet values() {
            return forward().keySet();
        }

        @Override
        public Set<Object2IntMap.Entry<V>> entrySet() {
            return null;
        }

        @Override
        public Int2ObjectBiMap<V> inverse() {
            return Int2ObjectArrayBiMap.this;
        }

        private Int2ObjectArrayBiMap<V> forward() {
            return Int2ObjectArrayBiMap.this;
        }




    }
}
