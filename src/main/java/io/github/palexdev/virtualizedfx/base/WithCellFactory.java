/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
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

package io.github.palexdev.virtualizedfx.base;

import java.util.function.Function;

import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.properties.CellFactory;
import io.github.palexdev.virtualizedfx.table.VFXTable;

/// Not all containers are directly responsible for creating their cells. For example, in the [VFXTable], each
/// column builds cells for a specific data type. This design prevents the [VFXContainer] API from exposing a cell
/// factory directly.
///
/// This interface makes those classes that are directly responsible for building cells expose the cell factory function.
///
/// **Note:** implementations must expose the wrapper class [CellFactory] rather than the function directly.
public interface WithCellFactory<T, C extends VFXCell<T>> {

    /// Convenience method, shortcut for `getCellFactory().create(...)`
    default C create(T item) {
        return getCellFactory().create(item);
    }

    /// Specifies the wrapper class [CellFactory] for the cell factory function
    CellFactory<T, C> getCellFactory();

    /// Sets the cell factory function used to create cells.
    default void setCellFactory(Function<T, C> cellFactory) {
        getCellFactory().setValue(cellFactory);
    }
}
