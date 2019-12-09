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
package org.spongepowered.common.world.volume;

import org.spongepowered.api.world.volume.MutableVolume;
import org.spongepowered.api.world.volume.UnmodifiableVolume;
import org.spongepowered.api.world.volume.Volume;
import org.spongepowered.api.world.volume.function.VolumeResult;
import org.spongepowered.api.world.volume.stream.VolumeStream;
import org.spongepowered.api.world.volume.function.VolumeConsumer;
import org.spongepowered.api.world.volume.function.VolumeMapper;
import org.spongepowered.api.world.volume.function.VolumeMerger;
import org.spongepowered.api.world.volume.function.VolumePredicate;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class AbstractVolumeStream<V extends Volume, U extends UnmodifiableVolume, I, M extends MutableVolume> implements VolumeStream<V, U, M> {

    private final Supplier<? extends V> volumeSupplier;
    private final

    public AbstractVolumeStream(Supplier<? extends V> volumeSupplier) {
        this.volumeSupplier = volumeSupplier;
    }

    @Override
    public V getVolume() {
        return this.volumeSupplier.get();
    }

    @Override
    public VolumeStream<V, U, M> filter(VolumePredicate<V, I> predicate) {
        return null;
    }

    @Override
    public <T> VolumeStream<V, U, M> map(VolumeMapper<I, U, T> mapper) {
        return null;
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public boolean allMatch(VolumePredicate<? super V, ? super I> predicate) {
        return false;
    }

    @Override
    public boolean allMatch(Predicate<VolumeResult<? super V, ? super I>> predicate) {
        return false;
    }

    @Override
    public boolean noneMatch(VolumePredicate<? super V, ? super I> predicate) {
        return false;
    }

    @Override
    public boolean noneMatch(Predicate<VolumeResult<? super V, ? super I>> predicate) {
        return false;
    }

    @Override
    public boolean anyMatch(VolumePredicate<? super V, ? super I> predicate) {
        return false;
    }

    @Override
    public boolean anyMatch(Predicate<VolumeResult<? super V, ? super I>> predicate) {
        return false;
    }

    @Override
    public Optional<VolumeResult<V, I>> findFirst() {
        return Optional.empty();
    }

    @Override
    public Optional<VolumeResult<V, I>> findAny() {
        return Optional.empty();
    }

    @Override
    public Stream<VolumeResult<V, I>> toStream() {
        return null;
    }

    @Override
    public void merge(V second, VolumeMerger<I, U> merger, M destination) {

    }

    @Override
    public void forEach(VolumeConsumer<V, I> visitor) {

    }

    @Override
    public void forEach(Consumer<VolumeResult<V, I>> consumer) {

    }
}
