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

package org.spongepowered.common.data.component.entity;

import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.component.entity.SkinComponent;
import org.spongepowered.api.data.token.Tokens;
import org.spongepowered.common.data.component.AbstractSingleValueComponent;

import java.util.UUID;

public class SpongeSkinComponent extends AbstractSingleValueComponent<UUID, SkinComponent> implements SkinComponent {

    public SpongeSkinComponent() {
        this(new UUID(0, 0));
    }

    public SpongeSkinComponent(UUID value) {
        super(SkinComponent.class, value);
    }

    @Override
    public SkinComponent copy() {
        return new SpongeSkinComponent(this.getValue());
    }

    @Override
    public SkinComponent reset() {
        return setValue(new UUID(0, 0));
    }

    @Override
    public int compareTo(SkinComponent o) {
        return this.getValue().compareTo(o.getValue());
    }

    @Override
    public DataContainer toContainer() {
        return new MemoryDataContainer().set(Tokens.SKIN_UUID.getQuery(), this.getValue().toString());
    }

}
