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

package io.github.palexdev.virtualizedfx.grid;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.NumberRange;
import io.github.palexdev.mfxcore.base.properties.range.IntegerRangeProperty;
import io.github.palexdev.mfxcore.collections.ObservableGrid.Change;
import io.github.palexdev.mfxcore.enums.GridChangeType;
import io.github.palexdev.virtualizedfx.beans.GridStateProperty;
import io.github.palexdev.virtualizedfx.cell.GridCell;

/**
 * The {@code ViewportManager} is responsible for managing the grid's viewport, track its current {@link GridState}
 * and trigger states transitions.
 * <p>
 * It stores the state of the viewport at any given time with three properties:
 * <p> - the main one is the {@link #stateProperty()} which olds the {@link GridState} object representing the current state
 * of the viewport
 * <p> - the last row range property, which holds the last range of displayed rows as a {@link IntegerRange}
 * <p> - the last columns range property which holds the last range of displayed columns as a {@link IntegerRange}
 * <p></p>
 * As mentioned above this is also responsible for handling states transitions when: initializations occur (cells supply
 * and removals when the viewport size changes; vertical and horizontal scrolling; changes occurred in the data structure;
 * clear and reset the viewport when needed.
 *
 * @param <T> the type of items
 * @param <C> the type of cell used
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ViewportManager<T, C extends GridCell<T>> {
	//================================================================================
	// Properties
	//================================================================================
	private final VirtualGrid<T, C> grid;
	private final GridStateProperty<T, C> state = new GridStateProperty<>(GridState.EMPTY);
	private final IntegerRangeProperty lastRowsRange = new IntegerRangeProperty();
	private final IntegerRangeProperty lastColumnsRange = new IntegerRangeProperty();

	//================================================================================
	// Constructors
	//================================================================================
	ViewportManager(VirtualGrid<T, C> grid) {
		this.grid = grid;
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * This is responsible for filling the viewport with the right amount of rows/columns/cells.
	 * So, it is a bit more than just an initialization method since this is also called for example when the viewport
	 * size changes and cells may need to be added or removed.
	 * <p></p>
	 * The first step is to gather a series of useful information such as:
	 * <p> - the expected range of rows, {@link GridHelper#rowsRange()}
	 * <p> - the expected range of columns, {@link GridHelper#columnsRange()}
	 * <p> - the old/current state, {@link #stateProperty()}
	 * <p>
	 * We also ensure that the estimated size of the viewport is correct by calling {@link GridHelper#computeEstimatedSize()}.
	 * <p></p>
	 * The second step is to distinguish between two cases:
	 * <p> 1) The old/current state is {@link GridState#EMPTY}
	 * <p> 2) The old/current state is a valid state
	 * <p></p>
	 * In the first case it means that the viewport is empty. For each row in the expected rows range we add a row
	 * to the new state with {@link GridState#addRow(int)}. Since the viewport is empty we also need to call
	 * {@link GridState#cellsChanged()}. Last but not least we update all the properties of the manager, and then
	 * request the viewport layout with {@link VirtualGrid#requestViewportLayout()}.
	 * <p></p>
	 * In the second case we call {@link GridState#init(IntegerRange, IntegerRange)} on the old/current state so that
	 * a new state, which reuses when possible the current already present rows, can be computed. At this point a special
	 * check is needed, the aforementioned method could also return the old/current state for whatever reason, in such case
	 * there's no need to proceed and the method exits immediately. Otherwise, same as above, update all the properties,
	 * call {@link GridState#cellsChanged()} and then {@link VirtualGrid#requestViewportLayout()}.
	 *
	 * @return in addition to the various computations made in this method, it also returns a boolean value to indicate
	 * whether computations lead to a layout request or not, {@link VirtualGrid#requestViewportLayout()}
	 */
	public boolean init() {
		if (grid.getCellFactory() == null || itemsEmpty()) return false;

		// Pre-computation
		GridHelper helper = grid.getGridHelper();
		IntegerRange rowsRange = helper.rowsRange();
		IntegerRange columnsRange = helper.columnsRange();
		helper.computeEstimatedSize();

		// Check old state
		GridState<T, C> oldState = getState();
		GridState<T, C> newState = new GridState<>(grid, rowsRange, columnsRange);

		if (oldState == GridState.EMPTY) {
			for (Integer row : rowsRange) {
				newState.addRow(row);
			}
			newState.cellsChanged();
			setState(newState);
			setLastRowsRange(rowsRange);
			setLastColumnsRange(columnsRange);
			grid.requestViewportLayout();
			return true;
		}

		// Transition from old state to new state
		newState = oldState.init(rowsRange, columnsRange);
		if (newState == oldState) return false;
		newState.cellsChanged();
		setState(newState);
		setLastRowsRange(rowsRange);
		setLastColumnsRange(columnsRange);
		grid.requestViewportLayout();
		return true;
	}

	/**
	 * This is responsible for handling vertical scrolling.
	 * <p>
	 * If the current state is empty exits immediately.
	 * <p></p>
	 * Before transitioning to a new state and eventually requesting a layout computation to the grid,
	 * we must check some parameters.
	 * <p>
	 * First we compute the rows range and if that is not equal to the range of the current state then
	 * we call {@link GridState#vScroll(IntegerRange)} to create a new state which will contain
	 * all the needed rows for the new range
	 * <p>
	 * The layout instead uses a different range and is compared against the last rows range, if they are not
	 * equal then {@link VirtualGrid#requestViewportLayout()} is invoked.
	 * <p>
	 * At the end the last rows range property is updated.
	 */
	public void onVScroll() {
		GridState<T, C> state = getState();
		if (state == GridState.EMPTY || state.isEmpty() || grid.isEmpty()) return;

		GridHelper helper = grid.getGridHelper();
		int rows = helper.maxRows();

		// State Computation
		int sFirstRow = helper.firstRow();
		int sLastRow = helper.lastRow();
		int sTrueFirstRow = Math.max(sLastRow - rows + 1, 0);
		IntegerRange sRange = IntegerRange.of(sTrueFirstRow, sLastRow);

		if (!sRange.equals(state.getRowsRange())) {
			setState(state.vScroll(sRange));
		}

		// Layout Computation
		IntegerRange lRange = IntegerRange.of(sFirstRow, sLastRow);
		if (!lRange.equals(getLastRowsRange())) {
			grid.requestViewportLayout();
		}

		setLastRowsRange(lRange);
	}

	/**
	 * This is responsible for handling horizontal scrolling.
	 * <p>
	 * If the current state is empty exits immediately.
	 * <p></p>
	 * Before transitioning to a new state and eventually requesting a layout computation to the grid,
	 * we must check some parameters.
	 * <p>
	 * First we compute the columns range and if that is not equal to the range of the current state then
	 * we call {@link GridState#hScroll(IntegerRange)} to create a new state which will contain
	 * all the needed columns for the new range
	 * <p>
	 * The layout instead uses a different range and is compared against the last columns range, if they are not
	 * equal then {@link VirtualGrid#requestViewportLayout()} is invoked.
	 * <p>
	 * At the end the last columns range property is updated.
	 */
	public void onHScroll() {
		GridState<T, C> state = getState();
		if (state == GridState.EMPTY || state.isEmpty()) return;

		GridHelper helper = grid.getGridHelper();
		int columns = helper.maxColumns();

		// State Computation
		int sFirstColumn = helper.firstColumn();
		int sLastColumn = helper.lastColumn();
		int sTrueFirstColumn = Math.max(sLastColumn - columns + 1, 0);
		IntegerRange sRange = IntegerRange.of(sTrueFirstColumn, sLastColumn);

		if (!sRange.equals(state.getColumnsRange())) {
			setState(state.hScroll(sRange));
		}

		// Layout Computation
		IntegerRange lRange = IntegerRange.of(sFirstColumn, sLastColumn);
		if (!lRange.equals(getLastColumnsRange())) {
			grid.requestViewportLayout();
		}

		setLastColumnsRange(lRange);
	}

	/**
	 * This is responsible for handling changes occurring in the grid's items data structure.
	 * <p></p>
	 * Before transitioning to a new state, there are three special cases that need to be covered:
	 * <p> - if there are no items in the data structure (was cleared), we invoke {@link #clear()}
	 * <p> - if the current state is the {@link GridState#EMPTY} state than we must call {@link #init()}
	 * <p> - if the change is of type {@link GridChangeType#TRANSPOSE} then we {@link #reset()} the viewport
	 * <p></p>
	 * In any other case we can call {@link GridState#change(Change)} on the current state
	 * to produce a new state that reflects the changes occurred in the data structure.
	 * At the end a layout request is sent to the grid, {@link VirtualGrid#requestViewportLayout()} and
	 * both the last rows and columns ranges are updated.
	 */
	public void onChange(Change<T> change) {
		GridState<T, C> state = getState();
		if (itemsEmpty()) {
			clear();
			change.endChange();
			return;
		}

		if (state == GridState.EMPTY) {
			init();
			change.endChange();
			return;
		}

		if (change.getType() == GridChangeType.TRANSPOSE) {
			reset();
			change.endChange();
			return;
		}

		state = state.change(change);
		setState(state);
		grid.requestViewportLayout();
		setLastRowsRange(state.getRowsRange());
		setLastColumnsRange(state.getColumnsRange());
	}

	/**
	 * Responsible for clearing the viewport and resetting the manager' state.
	 */
	public void clear() {
		getState().clear();
		setState(GridState.EMPTY);
		setLastRowsRange(IntegerRange.of(-1));
		setLastColumnsRange(IntegerRange.of(-1));
		GridHelper helper = grid.getGridHelper();
		helper.computeEstimatedSize();
		helper.invalidatedPos();
	}

	/**
	 * Responsible for resetting the viewport. First it calls {@link #clear()}
	 * then {@link #init()}.
	 */
	public void reset() {
		clear();
		init();
	}

	/**
	 * @return whether the data structure is empty
	 */
	protected boolean itemsEmpty() {
		return grid.isEmpty();
	}

	//================================================================================
	// Getters/Setters
	//================================================================================
	public GridState<T, C> getState() {
		return state.get();
	}

	/**
	 * Keeps the {@link GridState} object which represents the current state of the viewport.
	 */
	public GridStateProperty<T, C> stateProperty() {
		return state;
	}

	protected void setState(GridState<T, C> state) {
		this.state.set(state);
	}

	public NumberRange<Integer> getLastRowsRange() {
		return lastRowsRange.get();
	}

	/**
	 * Specifies the last range of rows.
	 */
	public IntegerRangeProperty lastRowsRangeProperty() {
		return lastRowsRange;
	}

	public void setLastRowsRange(NumberRange<Integer> lastRowsRange) {
		this.lastRowsRange.set(lastRowsRange);
	}

	public NumberRange<Integer> getLastColumnsRange() {
		return lastColumnsRange.get();
	}

	/**
	 * Specifies the last range of columns.
	 */
	public IntegerRangeProperty lastColumnsRangeProperty() {
		return lastColumnsRange;
	}

	public void setLastColumnsRange(NumberRange<Integer> lastColumnsRange) {
		this.lastColumnsRange.set(lastColumnsRange);
	}
}
