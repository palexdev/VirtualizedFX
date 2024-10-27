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

package io.github.palexdev.virtualizedfx.cells;

import io.github.palexdev.mfxcore.behavior.BehaviorBase;

/**
 * Base, empty behavior for cells of type {@link VFXCellBase}, extends {@link BehaviorBase}.
 *
 * @param <T> the type of item displayed by the cell
 */
public class CellBaseBehavior<T> extends BehaviorBase<VFXCellBase<T>> {

    //================================================================================
    // Constructors
    //================================================================================
    public CellBaseBehavior(VFXCellBase<T> cell) {
        super(cell);
    }
}
