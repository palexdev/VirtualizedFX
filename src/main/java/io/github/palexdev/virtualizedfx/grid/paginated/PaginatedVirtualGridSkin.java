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

package io.github.palexdev.virtualizedfx.grid.paginated;

import io.github.palexdev.mfxcore.collections.ObservableGrid;
import io.github.palexdev.mfxcore.collections.ObservableGrid.Change;
import io.github.palexdev.mfxcore.utils.fx.NodeUtils;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import io.github.palexdev.virtualizedfx.grid.VirtualGridSkin;

/**
 * Default skin implementation for {@link PaginatedVirtualGrid}, extends {@link VirtualGridSkin}.
 * <p></p>
 * This is adapted to adjust the grid's height according to {@link PaginatedVirtualGrid#rowsPerPageProperty()} and
 * {@link PaginatedVirtualGrid#cellSizeProperty()}.
 */
public class PaginatedVirtualGridSkin<T, C extends GridCell<T>> extends VirtualGridSkin<T, C> {

	//================================================================================
	// Constructors
	//================================================================================
	public PaginatedVirtualGridSkin(PaginatedVirtualGrid<T, C> virtualGrid) {
		super(virtualGrid);
		NodeUtils.waitForScene(virtualGrid, virtualGrid::updateMaxPage, false, true);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * @return {@link #getSkinnable()} cast as {@link PaginatedVirtualGrid}
	 */
	protected PaginatedVirtualGrid<T, C> getGrid() {
		return (PaginatedVirtualGrid<T, C>) getSkinnable();
	}

	/**
	 * Core method used by:
	 * <p> {@link #computeMinHeight(double, double, double, double, double)}
	 * <p> {@link #computePrefHeight(double, double, double, double, double)}
	 * <p> {@link #computeMaxHeight(double, double, double, double, double)}
	 * <p></p>
	 * Computes the virtual grid's height as {@code rowsPerPage * cellHeight}, in other terms
	 * the size of every single page.
	 */
	protected double getLength() {
		PaginatedVirtualGrid<T, C> grid = getGrid();
		return grid.getRowsPerPage() * grid.getCellSize().getHeight();
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * {@inheritDoc}
	 * Overridden to also update the number of max pages.
	 */
	@Override
	protected void onGridChanged(ObservableGrid<T> oldValue, ObservableGrid<T> newValue) {
		getGrid().updateMaxPage();
		super.onGridChanged(oldValue, newValue);
	}

	/**
	 * {@inheritDoc}
	 * Overridden to also update the number of max pages.
	 */
	@Override
	protected void onItemsChanged(Change<T> change) {
		getGrid().updateMaxPage();
		super.onItemsChanged(change);
	}

	@Override
	protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		return getLength();
	}

	@Override
	protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		return getLength();
	}

	@Override
	protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		return getLength();
	}
}
