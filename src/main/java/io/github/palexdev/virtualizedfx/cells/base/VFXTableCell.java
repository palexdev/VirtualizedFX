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

package io.github.palexdev.virtualizedfx.cells.base;

import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import io.github.palexdev.virtualizedfx.table.VFXTableRow;

/**
 * Extension of {@link VFXCell} to be used specifically with {@link VFXTable}. This virtualized container has both
 * rows and columns as concrete nodes with their own properties and states. It may be useful for table cells to store
 * their instances; So, this extension exposes two extra methods {@link #updateColumn(VFXTableColumn)} and
 * {@link #updateRow(VFXTableRow)}.
 */
public interface VFXTableCell<T> extends VFXCell<T> {

	/**
	 * Automatically called by the {@link VFXTable} subsystem on all its cells to allow storing the instance
	 * of the column that created them.
	 */
	default void updateColumn(VFXTableColumn<T, ? extends VFXTableCell<T>> column) {}

	/**
	 * Automatically called by the {@link VFXTable} subsystem on all its cells to allow storing the instance of
	 * the row that contains them.
	 */
	default void updateRow(VFXTableRow<T> row) {}
}
