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
package org.spongepowered.common.world.volume.worker;

import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.world.volume.block.MutableBlockVolume;
import org.spongepowered.api.world.volume.block.worker.MutableBlockVolumeWorker;
import org.spongepowered.api.world.volume.worker.function.VolumeFiller;

/**
 *
 */
public class SpongeMutableBlockVolumeWorker<M extends MutableBlockVolume<M>> extends SpongeBlockVolumeWorker<M, M> implements MutableBlockVolumeWorker<M> {

    public SpongeMutableBlockVolumeWorker(M volume) {
        super(volume);
    }

    @Override
    public void fill(VolumeFiller<BlockState> filler) {
        final int xMin = this.volume.getBlockMin().getX();
        final int yMin = this.volume.getBlockMin().getY();
        final int zMin = this.volume.getBlockMin().getZ();
        final int xMax = this.volume.getBlockMax().getX();
        final int yMax = this.volume.getBlockMax().getY();
        final int zMax = this.volume.getBlockMax().getZ();
        for (int z = zMin; z <= zMax; z++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int x = xMin; x <= xMax; x++) {
                    final BlockState block = filler.provide(x, y, z);
                    this.volume.setBlock(x, y, z, block);
                }
            }
        }
    }
}
