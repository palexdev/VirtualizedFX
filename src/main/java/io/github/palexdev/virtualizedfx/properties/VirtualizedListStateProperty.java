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

package io.github.palexdev.virtualizedfx.properties;

import io.github.palexdev.virtualizedfx.cells.Cell;
import io.github.palexdev.virtualizedfx.list.VirtualizedListState;
import javafx.beans.property.ReadOnlyObjectWrapper;

/**
 * Convenience property that extends {@link ReadOnlyObjectWrapper} for {@link VirtualizedListState}.
 */
public class VirtualizedListStateProperty<T, C extends Cell<T>> extends ReadOnlyObjectWrapper<VirtualizedListState<T, C>> {

	//================================================================================
	// Constructors
	//================================================================================
	public VirtualizedListStateProperty() {
	}

	public VirtualizedListStateProperty(VirtualizedListState<T, C> initialValue) {
		super(initialValue);
	}

	public VirtualizedListStateProperty(Object bean, String name) {
		super(bean, name);
	}

	public VirtualizedListStateProperty(Object bean, String name, VirtualizedListState<T, C> initialValue) {
		super(bean, name, initialValue);
	}
}
