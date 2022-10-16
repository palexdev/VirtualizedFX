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

package io.github.palexdev.virtualizedfx.flow.paginated;

import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.flow.VirtualFlowSkin;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;

/**
 * Default skin implementation for {@link PaginatedVirtualFlow}, extends {@link VirtualFlowSkin}.
 * <p></p>
 * This is adapted to adjust the flow's width or height according to {@link PaginatedVirtualFlow#cellsPerPageProperty()},
 * {@link PaginatedVirtualFlow#cellSizeProperty()} and of course {@link PaginatedVirtualFlow#orientationProperty()}.
 */
public class PaginatedVirtualFlowSkin<T, C extends Cell<T>> extends VirtualFlowSkin<T, C> {

	//================================================================================
	// Constructors
	//================================================================================
	public PaginatedVirtualFlowSkin(PaginatedVirtualFlow<T, C> virtualFlow) {
		super(virtualFlow);
	}

	//================================================================================
	// Methods
	//================================================================================
	/**
	 * @return {@link #getSkinnable()} cast as {@link PaginatedVirtualFlow}
	 */
	protected PaginatedVirtualFlow<T, C> getFlow() {
		return ((PaginatedVirtualFlow<T, C>) getSkinnable());
	}

	/**
	 * Core method used by:
	 * <p> When HORIZONTAL:
	 * <p> {@link #computeMinWidth(double, double, double, double, double)}
	 * <p> {@link #computePrefWidth(double, double, double, double, double)}
	 * <p> {@link #computeMaxWidth(double, double, double, double, double)}
	 * <p> When VERTICAL:
	 * <p> {@link #computeMinHeight(double, double, double, double, double)}
	 * <p> {@link #computePrefHeight(double, double, double, double, double)}
	 * <p> {@link #computeMaxHeight(double, double, double, double, double)}
	 * <p></p>
	 * Computes the virtual flow's size as {@code cellsPerPage * cellSize}, in other terms the
	 * size of every single page.
	 */
	protected double getLength() {
		PaginatedVirtualFlow<T, C> flow = getFlow();
		return flow.getCellsPerPage() * flow.getCellSize();
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * {@inheritDoc}
	 * Overridden to also update the number of max pages.
	 */
	@Override
	protected void onListChanged(ObservableList<? extends T> oldList, ObservableList<? extends T> newList) {
		getFlow().updateMaxPage();
		super.onListChanged(oldList, newList);
	}

	/**
	 * {@inheritDoc}
	 * Overridden to also update the number of max pages.
	 */
	@Override
	protected void onItemsChanged(ListChangeListener.Change<? extends T> c) {
		getFlow().updateMaxPage();
		super.onItemsChanged(c);
	}

	@Override
	protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		Orientation o = getSkinnable().getOrientation();
		if (o == Orientation.HORIZONTAL) return getLength();
		return super.computeMinWidth(height, topInset, rightInset, bottomInset, leftInset);
	}

	@Override
	protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		Orientation o = getSkinnable().getOrientation();
		if (o == Orientation.VERTICAL) return getLength();
		return super.computeMinHeight(width, topInset, rightInset, bottomInset, leftInset);
	}

	@Override
	protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		Orientation o = getSkinnable().getOrientation();
		if (o == Orientation.HORIZONTAL) return getLength();
		return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
	}

	@Override
	protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		Orientation o = getSkinnable().getOrientation();
		if (o == Orientation.VERTICAL) return getLength();
		return super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
	}

	@Override
	protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		Orientation o = getSkinnable().getOrientation();
		if (o == Orientation.HORIZONTAL) return getLength();
		return super.computeMaxWidth(height, topInset, rightInset, bottomInset, leftInset);
	}

	@Override
	protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		Orientation o = getSkinnable().getOrientation();
		if (o == Orientation.VERTICAL) return getLength();
		return super.computeMaxHeight(width, topInset, rightInset, bottomInset, leftInset);
	}
}
