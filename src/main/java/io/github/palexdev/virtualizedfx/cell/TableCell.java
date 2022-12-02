/*
 * Copyright (C) 2022 Parisi Alessandro
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX).
 *
 * VirtualizedFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.cell;

import io.github.palexdev.virtualizedfx.table.TableColumn;
import io.github.palexdev.virtualizedfx.table.VirtualTable;
import io.github.palexdev.virtualizedfx.table.defaults.DefaultTableRow;

/**
 * Extension of {@link Cell} specifically used by {@link VirtualTable} and subclasses.
 *
 * @see #updateColumn(TableColumn)
 * @see #updateRow(int, DefaultTableRow)
 * @see #invalidate()
 */
public interface TableCell<T> extends Cell<T> {

	/**
	 * This is a way for cells to obtain and perhaps hold the reference of the column it
	 * is associated to.
	 */
	default void updateColumn(TableColumn<T, ? extends TableCell<T>> column) {
	}

	/**
	 * This is a way for cells to obtain and perhaps hold the reference of the row which contains
	 * the cell. The given rIndex is the index of the given row.
	 * <p></p>
	 * <b>Note</b> that the row may be the same for different calls, but the rIndex may differ.
	 */
	default void updateRow(int rIndex, DefaultTableRow<T> row) {
	}

	/**
	 * This is a way for cells to specify the behavior for when any of its properties
	 * are updated.
	 */
	default void invalidate() {
	}
}
