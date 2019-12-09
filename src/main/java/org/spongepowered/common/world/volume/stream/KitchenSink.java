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

public interface KitchenSink<V extends Volume, T> {

    void accept(V volume, T element, int x, int y, int z);

    default void begin() {}

    default void end() {}

    default boolean isAcceptingMoreData() {
        return true;
    }

    static abstract class ChainedReference<V extends Volume, E_OUT, T> implements KitchenSink<V, T> {
        protected final KitchenSink<? super V, ? super E_OUT> downstream;

        public ChainedReference(KitchenSink<? super V, ? super E_OUT> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void begin() {
            this.downstream.begin();
        }

        @Override
        public void end() {
            this.downstream.end();
        }

        @Override
        public boolean isAcceptingMoreData() {
            return this.downstream.isAcceptingMoreData();
        }
    }

}
