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

import io.github.palexdev.virtualizedfx.table.TableColumn;
import io.github.palexdev.virtualizedfx.table.TableHelper.VariableTableHelper;
import io.github.palexdev.virtualizedfx.table.VirtualTable;

/**
 * Enumerator to specify the layout modes for columns in {@link VirtualTable}.
 */
public enum ColumnsLayoutMode {

	/**
	 * In this mode, all columns will have the same width specified by {@link VirtualTable#columnSizeProperty()}.
	 */
	FIXED,

	/**
	 * In this mode, columns are allowed to have different widths. This enables features like:
	 * columns auto-sizing ({@link VariableTableHelper#autosizeColumn(TableColumn)}), or resizing at runtime
	 * through gestures (not implemented by the default column type)
	 */
	VARIABLE
}
