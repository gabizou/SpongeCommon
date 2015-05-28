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

import static org.spongepowered.api.data.DataQuery.of;

import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.component.entity.LeashComponent;
import org.spongepowered.api.data.token.Tokens;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.common.data.component.AbstractSingleValueComponent;

public class SpongeLeashComponent extends AbstractSingleValueComponent<Entity, LeashComponent> implements LeashComponent {

    public SpongeLeashComponent(Entity entity) {
        super(LeashComponent.class, entity);
    }

    @Override
    public LeashComponent copy() {
        return new SpongeLeashComponent(this.getValue());
    }

    @Override
    public LeashComponent reset() {
        return this;
    }

    @Override
    public int compareTo(LeashComponent o) {
        return o.getValue().getUniqueId().compareTo(this.getValue().getUniqueId());
    }

    @Override
    public DataContainer toContainer() {
        return new MemoryDataContainer().set(Tokens.LEASH_HOLDER.getQuery(), this.getValue().getUniqueId());
    }
}
