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

import org.spongepowered.api.world.volume.Volume;

import java.util.Spliterator;

abstract class VolumePipelineHelper<V extends Volume, P_OUT> {


    /**
     * Returns the exact output size of the portion of the output resulting from
     * applying the pipeline stages described by this {@code PipelineHelper} to
     * the the portion of the input described by the provided
     * {@code Spliterator}, if known.  If not known or known infinite, will
     * return {@code -1}.
     *
     * @apiNote
     * The exact output size is known if the {@code Spliterator} has the
     * {@code SIZED} characteristic, and the operation flags
     * {@code StreamOpFlag#SIZED} is known on the combined stream and operation
     * flags.
     *
     * @param spliterator the spliterator describing the relevant portion of the
     *        source data
     * @return the exact size if known, or -1 if infinite or unknown
     */
    abstract<P_IN> long exactOutputSizeIfKnown(Spliterator<P_IN> spliterator);

    /**
     * Applies the pipeline stages described by this {@code PipelineHelper} to
     * the provided {@code Spliterator} and send the results to the provided
     * {@code Sink}.
     *
     * @implSpec
     * The implementation behaves as if:
     * <pre>{@code
     *     intoWrapped(wrapSink(sink), spliterator);
     * }</pre>
     *
     * @param sink the {@code Sink} to receive the results
     * @param spliterator the spliterator describing the source input to process
     */
    abstract<P_IN, S extends KitchenSink<V, P_OUT>> S wrapAndCopyInto(S sink, Spliterator<P_IN> spliterator);

    /**
     * Pushes elements obtained from the {@code Spliterator} into the provided
     * {@code Sink}.  If the stream pipeline is known to have short-circuiting
     * stages in it (see {@code StreamOpFlag#SHORT_CIRCUIT}), the
     * {@link KitchenSink#isAcceptingMoreData()} is checked after each
     * element, stopping if cancellation is requested.
     *
     * @implSpec
     * This method conforms to the {@code Sink} protocol of calling
     * {@code Sink.begin} before pushing elements, via {@code Sink.accept}, and
     * calling {@code Sink.end} after all elements have been pushed.
     *
     * @param wrappedSink the destination {@code Sink}
     * @param spliterator the source {@code Spliterator}
     */
    abstract<P_IN> void copyInto(KitchenSink<V, P_IN> wrappedSink, Spliterator<P_IN> spliterator);

    /**
     * Pushes elements obtained from the {@code Spliterator} into the provided
     * {@code Sink}, checking {@link KitchenSink#isAcceptingMoreData()} after each
     * element, and stopping if cancellation is requested.
     *
     * @implSpec
     * This method conforms to the {@code Sink} protocol of calling
     * {@code Sink.begin} before pushing elements, via {@code Sink.accept}, and
     * calling {@code Sink.end} after all elements have been pushed or if
     * cancellation is requested.
     *
     * @param wrappedSink the destination {@code Sink}
     * @param spliterator the source {@code Spliterator}
     */
    abstract <P_IN> void copyIntoWithCancel(KitchenSink<V, P_IN> wrappedSink, Spliterator<P_IN> spliterator);

    /**
     * Takes a {@code Sink} that accepts elements of the output type of the
     * {@code PipelineHelper}, and wrap it with a {@code Sink} that accepts
     * elements of the input type and implements all the intermediate operations
     * described by this {@code PipelineHelper}, delivering the result into the
     * provided {@code Sink}.
     *
     * @param sink the {@code Sink} to receive the results
     * @return a {@code Sink} that implements the pipeline stages and sends
     *         results to the provided {@code Sink}
     */
    abstract<P_IN> KitchenSink<V, P_IN> wrapSink(KitchenSink<V, P_OUT> sink);

    /**
     *
     * @param spliterator
     * @param <P_IN>
     * @return
     */
    abstract<P_IN> Spliterator<P_OUT> wrapSpliterator(Spliterator<P_IN> spliterator);

}
