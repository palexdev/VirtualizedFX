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

import io.github.palexdev.mfxcore.collections.Grid;
import io.github.palexdev.virtualizedfx.grid.VirtualGrid;

/**
 * Specialization of {@link Cell} specifically used by {@link VirtualGrid} and subclasses.
 *
 * @see #updateIndex(int)
 * @see #updateCoordinates(int, int)
 */
public interface GridCell<T> extends Cell<T> {

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * For {@code GridCells} the given index is the {@code linear index} of the item,
	 * to use the coordinates cells should rely on {@link #updateCoordinates(int, int)}.
	 *
	 * @see VirtualGrid
	 * @see Grid
	 */
	@Override
	default void updateIndex(int index) {
	}

	/**
	 * Allows implementations to store the cell index as a pair of coordinates [row, column].
	 * This method already provides the two coordinates.
	 */
	default void updateCoordinates(int row, int column) {
	}
}
