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

package io.github.palexdev.virtualizedfx.enums;

import io.github.palexdev.virtualizedfx.flow.ViewportState;

/**
 * Enumeration to differentiate the various types of {@link ViewportState}.
 * <p>
 * To be more precise, this is used to indicate which event lead to the creation of the new state.
 */
public enum UpdateType {

	/**
	 * Indicates that the state has been created after an initialization request
	 */
	INIT,

	/**
	 * Indicates that the state has been created after a scroll request
	 */
	SCROLL,

	/**
	 * Indicates that the state has been created after a change in the items list
	 */
	CHANGE
}
