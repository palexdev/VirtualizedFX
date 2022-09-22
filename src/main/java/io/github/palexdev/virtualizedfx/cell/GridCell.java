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

import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.virtualizedfx.grid.VirtualGrid;

/**
 * Specialization of {@link Cell} specifically used by {@link VirtualGrid} and subclasses.
 */
public interface GridCell<T> extends Cell<T> {

	/**
	 * Allows implementations to store the cell index as a pair of coordinates [row, column].
	 * This method accepts the linear index of the cell. Implementations should use the
	 * grid's properties and {@link GridUtils} to correctly compute the two coordinates.
	 */
	default void updateCoordinates(int linearIndex) {
	}

	/**
	 * Allows implementations to store the cell index as a pair of coordinates [row, column].
	 * This method already provides the two coordinates.
	 */
	default void updateCoordinates(int row, int column) {
	}
}
