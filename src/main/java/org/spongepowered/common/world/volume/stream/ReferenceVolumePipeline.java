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
package org.spongepowered.common.world.volume.stream;

import org.spongepowered.api.world.volume.MutableVolume;
import org.spongepowered.api.world.volume.UnmodifiableVolume;
import org.spongepowered.api.world.volume.Volume;
import org.spongepowered.api.world.volume.function.VolumeConsumer;
import org.spongepowered.api.world.volume.function.VolumeMapper;
import org.spongepowered.api.world.volume.function.VolumePredicate;
import org.spongepowered.api.world.volume.function.VolumeResult;
import org.spongepowered.api.world.volume.function.VolumeResultSupplier;
import org.spongepowered.api.world.volume.stream.VolumeStream;

import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class ReferenceVolumePipeline
        <VOL extends Volume, E_IN, E_OUT>
    extends AbstractVolumePipeline<E_IN, E_OUT, VolumeStream<VOL, E_OUT>, VOL>
    implements VolumeStream<VOL, E_OUT>
{

    VolumeResultSupplier<? super VOL, E_OUT> supplier;

    ReferenceVolumePipeline(VOL volume, AbstractVolumePipeline<?, E_IN, ?, VOL, ?, ?> upstream) {
        super(volume, upstream);
    }

    @Override
    <P_IN> Spliterator<E_OUT> wrap(VolumePipelineHelper<VOL, E_OUT> ph, Supplier<Spliterator<P_IN>> supplier, boolean isParallel) {
        return null;
    }

    @Override
    public VolumeResultSupplier<? super VOL, E_OUT> getSupplier() {
        return this.supplier;
    }

    @Override
    public VolumeStream<VOL, E_OUT, MUT_V> filter(VolumePredicate<VOL, E_OUT> predicate) {
        return new StatelessOp<VOL, E_OUT, E_OUT, UN_V, MUT_V>(this.getVolume(), this) {

            @Override
            KitchenSink<VOL, E_OUT> opWrapSink(int flags, KitchenSink<VOL, E_OUT> sink) {
                return new KitchenSink.ChainedReference<VOL, E_OUT, E_OUT>(sink) {
                    @Override
                    public void begin() {
                        downstream.begin();
                    }

                    @Override
                    public void accept(VOL volume, E_OUT element, int x, int y, int z) {
                        if (predicate.test(volume, element, x, y, z)) {
                            this.downstream.accept(volume, element, x, y, z);
                        }
                    }
                };
            }
        };
    }

    @Override
    public <T> VolumeStream<VOL, T, MUT_V> map(VolumeMapper<E_OUT, UN_V, T> mapper) {
        return new StatelessOp<VOL, E_OUT, T, UN_V, MUT_V>(this.getVolume(), this) {


            @Override
            KitchenSink opWrapSink(int flags, KitchenSink sink) {
                return new KitchenSink.ChainedReference<VOL, T, E_OUT>(sink) {

                    @Override
                    public void accept(VOL volume, E_OUT element, int x, int y, int z) {
                        downstream.accept(volume, mapper.map((UN_V) volume, element, x, y, z), x, y, z);
                    }
                };
            }
        };
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public boolean allMatch(VolumePredicate<? super VOL, ? super E_OUT> predicate) {
        return false;
    }

    @Override
    public boolean allMatch(Predicate<VolumeResult<? super VOL, ? super E_OUT>> predicate) {
        return false;
    }

    @Override
    public boolean noneMatch(VolumePredicate<? super VOL, ? super E_OUT> predicate) {
        return false;
    }

    @Override
    public boolean noneMatch(Predicate<VolumeResult<? super VOL, ? super E_OUT>> predicate) {
        return false;
    }

    @Override
    public boolean anyMatch(VolumePredicate<? super VOL, ? super E_OUT> predicate) {
        return false;
    }

    @Override
    public boolean anyMatch(Predicate<VolumeResult<? super VOL, ? super E_OUT>> predicate) {
        return false;
    }

    @Override
    public Optional<VolumeResult<VOL, E_OUT>> findFirst() {
        return Optional.empty();
    }

    @Override
    public Optional<VolumeResult<VOL, E_OUT>> findAny() {
        return Optional.empty();
    }

    @Override
    public Stream<VolumeResult<VOL, E_OUT>> toStream() {
        return null;
    }

    @Override
    public void forEach(VolumeConsumer<VOL, E_OUT> visitor) {

    }

    @Override
    public void forEach(Consumer<VolumeResult<VOL, E_OUT>> consumer) {

    }

    @Override
    public VolumeStream<VOL, E_OUT, MUT_V> sequential() {
        return null;
    }

    abstract static class StatelessOp
            <V extends Volume, E_IN, E_OUT, U extends UnmodifiableVolume, M extends MutableVolume>
            extends ReferenceVolumePipeline<V, E_IN, E_OUT, M, U> {

        StatelessOp(V volume, AbstractVolumePipeline<?, E_IN, ?, V, ?, ?> upstream) {
            super(volume, upstream);
        }
    }

    @Override
    <P_IN> long exactOutputSizeIfKnown(Spliterator<P_IN> spliterator) {
        return spliterator.getExactSizeIfKnown();
    }

    @Override
    <P_IN, S extends KitchenSink<VOL, E_OUT>> S wrapAndCopyInto(S sink, Spliterator<P_IN> spliterator) {
        return null;
    }

    @Override
    <P_IN> void copyInto(KitchenSink<VOL, P_IN> wrappedSink, Spliterator<P_IN> spliterator) {

    }

    @Override
    <P_IN> void copyIntoWithCancel(KitchenSink<VOL, P_IN> wrappedSink, Spliterator<P_IN> spliterator) {

    }

    @Override
    <P_IN> KitchenSink<VOL, P_IN> wrapSink(KitchenSink<VOL, E_OUT> sink) {
        return null;
    }

    @Override
    <P_IN> Spliterator<E_OUT> wrapSpliterator(Spliterator<P_IN> spliterator) {
        return null;
    }
}
