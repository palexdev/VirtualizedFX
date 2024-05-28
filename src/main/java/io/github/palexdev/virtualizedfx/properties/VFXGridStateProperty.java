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

package io.github.palexdev.virtualizedfx.properties;

import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.grid.VFXGridState;
import javafx.beans.property.ReadOnlyObjectWrapper;

/**
 * Convenience property that extends {@link ReadOnlyObjectWrapper} for {@link VFXGridState}.
 */
public class VFXGridStateProperty<T, C extends VFXCell<T>> extends ReadOnlyObjectWrapper<VFXGridState<T, C>> {

	//================================================================================
	// Constructors
	//================================================================================
	public VFXGridStateProperty() {}

	public VFXGridStateProperty(VFXGridState<T, C> initialValue) {
		super(initialValue);
	}

	public VFXGridStateProperty(Object bean, String name) {
		super(bean, name);
	}

	public VFXGridStateProperty(Object bean, String name, VFXGridState<T, C> initialValue) {
		super(bean, name, initialValue);
	}
}
