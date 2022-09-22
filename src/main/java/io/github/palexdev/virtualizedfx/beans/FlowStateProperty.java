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

package io.github.palexdev.virtualizedfx.beans;

import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.flow.FlowState;
import javafx.beans.property.ReadOnlyObjectWrapper;

/**
 * Convenience property that extends {@link ReadOnlyObjectWrapper} for {@link FlowState}.
 */
public class FlowStateProperty<T, C extends Cell<T>> extends ReadOnlyObjectWrapper<FlowState<T, C>> {

	//================================================================================
	// Constructors
	//================================================================================
	public FlowStateProperty() {
	}

	public FlowStateProperty(FlowState<T, C> initialValue) {
		super(initialValue);
	}

	public FlowStateProperty(Object bean, String name) {
		super(bean, name);
	}

	public FlowStateProperty(Object bean, String name, FlowState<T, C> initialValue) {
		super(bean, name, initialValue);
	}
}
