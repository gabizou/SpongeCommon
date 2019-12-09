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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.world.volume.MutableVolume;
import org.spongepowered.api.world.volume.UnmodifiableVolume;
import org.spongepowered.api.world.volume.Volume;
import org.spongepowered.api.world.volume.stream.BaseVolumeStream;
import org.spongepowered.api.world.volume.stream.VolumeSpliterator;

import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.BaseStream;

/**
 * A Node-based linked pipeline for stream operations
 * @param <I> Type coming in upstream
 * @param <O> Type of elements going out
 */
public abstract class AbstractVolumePipeline
        <I, O, B extends BaseVolumeStream<B, V, O>, V extends Volume>
    extends VolumePipelineHelper<V, O>
        implements BaseVolumeStream<B, V, O> {

    @NonNull private AbstractVolumePipeline sourcePipeline;
    @Nullable private AbstractVolumePipeline previous;
    private V sourceVolume;
    @Nullable private AbstractVolumePipeline next;
    private int depth;
    @Nullable private VolumeSpliterator sourceSpliterator;
    private boolean linkedOrConsumed;
    @Nullable private Runnable sourceCloseAction;

    AbstractVolumePipeline(V volume, AbstractVolumePipeline<?, I, ?, V, ?, ?> previous) {
        this.previous = previous;
        this.sourcePipeline = previous.sourcePipeline;
        this.sourceVolume = volume;
        this.depth = previous.depth + 1;
    }

    AbstractVolumePipeline(V volume, VolumeSpliterator<V, ?> spliterator) {
        this.previous = null;
        this.sourcePipeline = this;
        this.sourceVolume = volume;
        this.depth = 0;
        this.sourceSpliterator = spliterator;
    }

    /**
     * Create a spliterator that wraps a source spliterator, compatible with
     * this stream shape, and operations associated with.
     *
     * @param ph the pipeline helper describing the pipeline stages
     * @param supplier the supplier of a spliterator
     * @return a wrapping spliterator compatible with this shape
     */
    abstract <P_IN> Spliterator<O> wrap(VolumePipelineHelper<V, O> ph,
                                            Supplier<Spliterator<P_IN>> supplier,
                                            boolean isParallel);
    /**
     * Accepts a {@code Sink} which will receive the results of this operation,
     * and return a {@code Sink} which accepts elements of the input type of
     * this operation and which performs the operation, passing the results to
     * the provided {@code Sink}.
     *
     * @apiNote
     * The implementation may use the {@code flags} parameter to optimize the
     * sink wrapping.  For example, if the input is already {@code DISTINCT},
     * the implementation for the {@code Stream#distinct()} method could just
     * return the sink it was passed.
     *
     * @param flags The combined stream and operation flags up to, but not
     *        including, this operation
     * @param sink sink to which elements should be sent after processing
     * @return a sink which accepts elements, perform the operation upon
     *         each element, and passes the results (if any) to the provided
     *         {@code Sink}.
     */
    abstract KitchenSink<V, I> opWrapSink(int flags, KitchenSink<V, O> sink);

    @Override
    public V getVolume() {
        return this.sourceVolume;
    }

    @Override
    public void close() throws Exception {
        this.linkedOrConsumed = true;
        this.sourceVolume = null;
        this.sourceSpliterator = null;
        this.sourcePipeline = null;
    }

    @Override
    public B sequential() {
        return null;
    }

    @Override
    public B onClose(Runnable runnable) {
        final Runnable existing = this.sourcePipeline.sourceCloseAction;
        this.sourcePipeline.sourceCloseAction = (existing == null)
                ? runnable
                : composeWithExceptions(existing, runnable);
        return (B) this;
    }

    @Override
    public VolumeSpliterator<V, O> spliterator() {
        if (this.linkedOrConsumed) {
            throw new IllegalStateException("VolumeStream is consumed");
        }
        this.linkedOrConsumed = true;
        if (this == this.sourcePipeline) {
            if (this.sourcePipeline.sourceSpliterator != null) {
                @SuppressWarnings("unchecked")
                VolumeSpliterator<V, O> iterator = this.sourcePipeline.sourceSpliterator;
                this.sourcePipeline.sourceSpliterator = null;
                return iterator;
            }
            throw new IllegalStateException("VolumeStream is consumed");
        }
        return null;
    }

    /**
     * Given two Runnables, return a Runnable that executes both in sequence,
     * even if the first throws an exception, and if both throw exceptions, add
     * any exceptions thrown by the second as suppressed exceptions of the first.
     */
    static Runnable composeWithExceptions(Runnable a, Runnable b) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    a.run();
                }
                catch (Throwable e1) {
                    try {
                        b.run();
                    }
                    catch (Throwable e2) {
                        try {
                            e1.addSuppressed(e2);
                        } catch (Throwable ignore) {}
                    }
                    throw e1;
                }
                b.run();
            }
        };
    }

    /**
     * Given two streams, return a Runnable that
     * executes both of their {@link BaseStream#close} methods in sequence,
     * even if the first throws an exception, and if both throw exceptions, add
     * any exceptions thrown by the second as suppressed exceptions of the first.
     */
    static Runnable composedClose(BaseStream<?, ?> a, BaseStream<?, ?> b) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    a.close();
                }
                catch (Throwable e1) {
                    try {
                        b.close();
                    }
                    catch (Throwable e2) {
                        try {
                            e1.addSuppressed(e2);
                        } catch (Throwable ignore) {}
                    }
                    throw e1;
                }
                b.close();
            }
        };
    }
}
