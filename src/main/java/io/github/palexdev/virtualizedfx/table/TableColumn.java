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

package io.github.palexdev.virtualizedfx.table;

import io.github.palexdev.mfxcore.base.properties.functional.FunctionProperty;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.defaults.DefaultTableColumn;
import io.github.palexdev.virtualizedfx.table.defaults.SimpleTableCell;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.layout.Region;

import java.util.function.Function;

/**
 * Interface to define the public API every column for a {@link VirtualTable} should expose.
 *
 * @param <T> the type of items in the table
 * @param <C> the type of {@link TableCell}s the column will produce
 */
public interface TableColumn<T, C extends TableCell<T>> {

	/**
	 * @return every implementation of this interface should store and offer
	 * the reference to the {@link VirtualTable} it's assigned to
	 */
	VirtualTable<T> getTable();

	/**
	 * Returns the column's region.
	 * The ideal way to implement a column would be to extend a JavaFX's pane/region
	 * and override this method to return "this".
	 */
	Region getRegion();

	Function<T, C> getCellFactory();

	/**
	 * Specifies the function used to produce {@link TableCell}s
	 */
	FunctionProperty<T, C> cellFactoryProperty();

	void setCellFactory(Function<T, C> cellFactory);

	boolean isInViewport();

	/**
	 * Specifies whether the column is visible in the viewport at any time.
	 * <p>
	 * For an example on how to implement this, see {@link DefaultTableColumn} source code (initialize() method).
	 * <p></p>
	 * An usage example is given by {@link SimpleTableCell} which hides itself automatically if the column is not visible,
	 * this is done only if the {@link VirtualTable#columnsLayoutModeProperty()} is set to {@link ColumnsLayoutMode#VARIABLE}.
	 */
	ReadOnlyBooleanProperty inViewportProperty();

	/**
	 * Ideally this should specify the behavior for when the {@link #inViewportProperty()} changes.
	 * <p>
	 * For example, in {@link DefaultTableColumn} this is called by an inline override of the "set()" method
	 * of the "inViewport" property.
	 */
	default void onVisibilityChanged(boolean before, boolean now) {
	}
}
