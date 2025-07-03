/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.utils;

import java.util.Collection;
import java.util.LinkedList;

import io.github.palexdev.mfxcore.collections.CircularQueue;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;

/**
 * A special kind of {@link LinkedList} which discards the oldest added item once it reaches the set capacity.
 * <p>
 * Basically the same as {@link CircularQueue}, but automatically invokes {@link C#dispose()} on cell that are discarded.
 */
public class CellsQueue<T, C extends VFXCell<T>> extends LinkedList<C> {
    //================================================================================
    // Properties
    //================================================================================
    private int capacity;

    //================================================================================
    // Constructors
    //================================================================================
    public CellsQueue(int capacity) {
        super();
        this.capacity = capacity;
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Adds the given cell to the queue. If at capacity, the oldest cell is disposed and discarded.
     *
     * @return whether the cell was added
     */
    public boolean queue(C c) {
        if (capacity == 0) {
            // If cells cannot be cached because this is disabled (capacity = 0), they should be disposed
            c.dispose();
            return false;
        }
        if (super.size() == capacity) {
            C excess = super.remove();
            excess.dispose();
        }
        return super.add(c);
    }

    /**
     * @return the queue's capacity, the maximum number of cells that can be added
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Sets the queue's capacity. If the parameter is 0, every cell is disposed and removed. If the new capacity is
     * lesser than the current one, then the oldest cell is removed until the capacity is reached.
     */
    public void setCapacity(int capacity) {
        if (capacity == 0) {
            forEach(C::dispose);
            clear();
            this.capacity = capacity;
            return;
        }

        if (capacity < super.size()) {
            for (int i = 0; i < (super.size() - capacity); i++) {
                C excess = super.remove();
                excess.dispose();
            }
        }
        this.capacity = capacity;
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public boolean add(C c) {
        return queue(c);
    }

    @Override
    public boolean addAll(Collection<? extends C> cells) {
        boolean result = false;
        for (C c : cells) result |= add(c);
        return result;
    }
}
