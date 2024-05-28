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

package io.github.palexdev.virtualizedfx.enums;

import io.github.palexdev.mfxcore.utils.EnumUtils;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper.VariableTableHelper;

/**
 * Enumerator to specify the layout modes for columns in {@link VFXTable}.
 */
public enum ColumnsLayoutMode {

	/**
	 * In this mode, all columns will have the same width specified by {@link VFXTable#columnsSizeProperty()}.
	 */
	FIXED,

	/**
	 * In this mode, columns are allowed to have different widths. This enables features like:
	 * columns auto-sizing ({@link VariableTableHelper#autosizeColumn(VFXTableColumn)}), or resizing at runtime
	 * through gestures.
	 * <p>
	 * A downside of such mode is that basically, virtualization along the x-axis is disabled. Which means that all columns
	 * will be added to the viewport. Internal optimizations should make this issue less impactful on performance.
	 */
	VARIABLE,
	;

	public static ColumnsLayoutMode next(ColumnsLayoutMode mode) {
		return EnumUtils.next(ColumnsLayoutMode.class, mode);
	}
}
