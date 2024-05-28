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

package io.github.palexdev.virtualizedfx.base;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;

/**
 * Defines the common API for every paginated virtualized container offered by VirtualizedFX. Extends {@link VFXContainer}.
 */
public interface VFXPaginated<T> extends VFXContainer<T> {

	default int getPage() {
		return pageProperty().get();
	}

	/**
	 * Specifies the page at which the container is.
	 */
	IntegerProperty pageProperty();

	default void setPage(int page) {
		pageProperty().set(page);
	}

	default int getMaxPage() {
		return maxPageProperty().get();
	}

	/**
	 * Specifies the maximum page index at which the container can go.
	 */
	ReadOnlyIntegerProperty maxPageProperty();

	default int getCellsPerPage() {
		return cellsPerPageProperty().get();
	}

	/**
	 * Specifies the number of cells/items to show per each page.
	 */
	IntegerProperty cellsPerPageProperty();

	default void setCellsPerPage(int cellsPerPage) {
		cellsPerPageProperty().set(cellsPerPage);
	}

	/**
	 * Goes to the next page if possible.
	 */
	default void next() {
		setPage(getPage() + 1);
	}

	/**
	 * Goes to the previous page if possible.
	 */
	default void previous() {
		setPage(getPage() - 1);
	}

	/**
	 * Changes the page by the given delta.
	 */
	default void moveBy(int delta) {
		setPage(getPage() + delta);
	}

	/**
	 * Given an index, returns the page at which it would be displayed by the container.
	 * <p></p>
	 * Note that this will never generate an exception, rather acts as follows for edge cases:
	 * <p> - empty list or max page is 0 or {@literal index < 0}: returns 0
	 * <p> - index > size: returns max page
	 */
	default int findPageByIndex(int index) {
		if (isEmpty() || getMaxPage() == 0 || index < 0) return 0;
		if (index > size() - 1) return getMaxPage();
		return index / getCellsPerPage();
	}

	/**
	 * Computes the maximum page index reachable by the container. This depends on the number of items and the number
	 * of cells/items per page.
	 * <p>
	 * The exact formula is as follows: {@code Math.max(0, ((int) Math.ceil(items / (double) cpp)) - 1)}
	 */
	default int computeMaxPage() {
		int items = size();
		int cpp = getCellsPerPage();
		return Math.max(0, ((int) Math.ceil(items / (double) cpp)) - 1);
	}
}
