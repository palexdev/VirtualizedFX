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
import io.github.palexdev.mfxcore.collections.ObservableGrid;
import io.github.palexdev.mfxcore.enums.GridChangeType;
import io.github.palexdev.virtualizedfx.beans.GridStateProperty;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import javafx.beans.property.ReadOnlyObjectProperty;

/**
 * The {@code ViewportManager} is responsible for managing the grid's viewport and its cells.
 * <p>
 * It stores the current state of the viewport at any given time with three properties:
 * <p> - The state property which olds an object of type {@link GridState}
 * <p> - The last row range property which holds the last range of displayed rows as a {@link IntegerRange}
 * <p> - The last columns range property which holds the last range of displayed columns as a {@link IntegerRange}
 * <p></p>
 * This is responsible for handling initializations (cells supply and removals when the viewport size changes), vertical
 * and horizontal scrolling, changes occurred in the items list, clear or resetting the viewport when needed.
 *
 * @param <T> the type of items
 * @param <C> the type of cell used
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ViewportManager<T, C extends GridCell<T>> {
	//================================================================================
	// Properties
	//================================================================================
	private final VirtualGrid<T, C> virtualGrid;
	private final GridStateProperty<T, C> state = new GridStateProperty<>(GridState.EMPTY);
	private final IntegerRangeProperty lastRowsRange = new IntegerRangeProperty();
	private final IntegerRangeProperty lastColumnsRange = new IntegerRangeProperty();

	//================================================================================
	// Constructors
	//================================================================================
	ViewportManager(VirtualGrid<T, C> virtualGrid) {
		this.virtualGrid = virtualGrid;
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * This method is responsible for filling the viewport with the necessary number of cells.
	 * So, it is a bit more than just an initializing method, since this is also called for example
	 * when the viewport size changes and cells need to be added or removed.
	 * <p></p>
	 * First we compute both the range of rows and columns with {@link GridHelper#rowsRange()} and {@link GridHelper#columnsRange()}.
	 * Then we create a new state and start processing the two ranges.
	 * <p>
	 * If the old state is the {@link GridState#EMPTY} state then for each index in the rows range we add
	 * a row to the new state with {@link GridState#addRow(int)}. Then we ensure the new rows are initialized
	 * by calling {@link GridState#init()}, we ensure that the viewport children list will be updated by setting
	 * {@link GridState#haveCellsChanged()} to true, and lastly we update all the properties of the manager and call
	 * {@link VirtualGrid#requestViewportLayout()}.
	 * <p></p>
	 * If the old state is not empty then the new state is created as result of {@link GridState#initTransition(IntegerRange, IntegerRange)},
	 * this will correctly handle the supply or removals of needed/unneeded rows.
	 * As for before, we ensure that the viewport children list will be updated by setting {@link GridState#haveCellsChanged()}
	 * to true, we update all the properties of the manager, and finally call {@link VirtualGrid#requestViewportLayout()}.
	 */
	public void init() {
		if (virtualGrid.getCellFactory() == null) return;

		// Compute the range of items to display
		GridHelper helper = virtualGrid.getGridHelper();
		IntegerRange rRange = helper.rowsRange();
		IntegerRange cRange = helper.columnsRange();

		// Compute the new state
		GridState<T, C> newState = new GridState<>(virtualGrid, rRange, cRange);
		GridState<T, C> oldState = getState();

		if (oldState == GridState.EMPTY) {
			for (Integer row : rRange) {
				newState.addRow(row);
			}
			newState.init();
			newState.setCellsChanged(true);
			setState(newState);
			setLastRowsRange(rRange);
			setLastColumnsRange(cRange);
			virtualGrid.requestViewportLayout();
			return;
		}

		newState = oldState.initTransition(rRange, cRange);
		newState.setCellsChanged(true);
		setState(newState);
		setLastRowsRange(rRange);
		setLastColumnsRange(cRange);
		virtualGrid.requestViewportLayout();
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
	 * we call {@link GridState#rowsTransition(IntegerRange)} to create a new state which will contain
	 * all the needed rows for the new range
	 * <p>
	 * The layout instead uses a different range and is compared to the last rows range, if they are not
	 * equal then {@link VirtualGrid#requestViewportLayout()} is invoked.
	 * <p>
	 * At the end the last rows range property is updated.
	 */
	public void onVScroll() {
		GridState<T, C> state = getState();
		if (state == GridState.EMPTY || state.isEmpty()) return;

		GridHelper helper = virtualGrid.getGridHelper();
		int rows = helper.maxRows();

		// State Computation
		int sFirstRow = helper.firstRow();
		int sLastRow = helper.lastRow();
		int sTrueFirstRow = Math.max(sLastRow - rows + 1, 0);
		IntegerRange sRange = IntegerRange.of(sTrueFirstRow, sLastRow);

		if (!sRange.equals(state.getRowsRange())) {
			setState(state.rowsTransition(sRange));
		}

		// Layout Computation
		IntegerRange lRange = IntegerRange.of(sFirstRow, sLastRow);
		if (!lRange.equals(getLastRowsRange())) {
			virtualGrid.requestViewportLayout();
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
	 * we call {@link GridState#columnsTransition(IntegerRange)} to create a new state which will contain
	 * all the needed columns for the new range
	 * <p>
	 * The layout instead uses a different range and is compared to the last columns range, if they are not
	 * equal then {@link VirtualGrid#requestViewportLayout()} is invoked.
	 * <p>
	 * At the end the last columns range property is updated.
	 */
	public void onHScroll() {
		GridState<T, C> state = getState();
		if (state == GridState.EMPTY || state.isEmpty()) return;

		GridHelper helper = virtualGrid.getGridHelper();
		int columns = helper.maxColumns();

		// State Computation
		int sFirstColumn = helper.firstColumn();
		int sLastColumn = helper.lastColumn();
		int sTrueFirstColumn = Math.max(sLastColumn - columns + 1, 0);
		IntegerRange sRange = IntegerRange.of(sTrueFirstColumn, sLastColumn);

		if (!sRange.equals(state.getColumnsRange())) {
			setState(state.columnsTransition(sRange));
		}

		// Layout Computation
		IntegerRange lRange = IntegerRange.of(sFirstColumn, sLastColumn);
		if (!lRange.equals(getLastColumnsRange())) {
			virtualGrid.requestViewportLayout();
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
	 * In any other case we can call {@link GridState#transition(ObservableGrid.Change)} on the current state
	 * to produce a new state that reflects the changes occurred in the data structure.
	 * Both the last rows and columns ranges are updated too.
	 */
	public void onChange(ObservableGrid.Change<T> c) {
		if (itemsEmpty()) {
			clear();
			c.endChange();
			return;
		}

		if (getState() == GridState.EMPTY) {
			init();
			c.endChange();
			return;
		}

		if (c.getType() == GridChangeType.TRANSPOSE) {
			reset();
			c.endChange();
			return;
		}

		setState(getState().transition(c));
		virtualGrid.requestViewportLayout();
		setLastRowsRange(getState().getRowsRange());
		setLastColumnsRange(getState().getColumnsRange());
	}

	/**
	 * Responsible for clearing the viewport and resetting the manager' state.
	 */
	public void clear() {
		getState().disposeAndClear();
		setState(GridState.EMPTY);
		GridHelper helper = virtualGrid.getGridHelper();
		helper.computeEstimatedLength();
		helper.computeEstimatedBreadth();
		helper.invalidatePos();
		virtualGrid.requestViewportLayout();
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
	 * @return the number of items in the list
	 */
	public final int itemsNum() {
		return virtualGrid.getItems().totalSize();
	}

	/**
	 * @return whether the list is empty
	 */
	public final boolean itemsEmpty() {
		return virtualGrid.getItems().isEmpty();
	}

	public GridState<T, C> getState() {
		return state.get();
	}

	/**
	 * Specifies the current state of the viewport.
	 */
	public ReadOnlyObjectProperty<GridState<T, C>> stateProperty() {
		return state.getReadOnlyProperty();
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
	public ReadOnlyObjectProperty<NumberRange<Integer>> lastRowsRangeProperty() {
		return lastRowsRange.getReadOnlyProperty();
	}

	protected void setLastRowsRange(NumberRange<Integer> lastRowsRange) {
		this.lastRowsRange.set(lastRowsRange);
	}

	public NumberRange<Integer> getLastColumnsRange() {
		return lastColumnsRange.get();
	}

	/**
	 * Specifies the last range of columns.
	 */
	public ReadOnlyObjectProperty<NumberRange<Integer>> lastColumnsRangeProperty() {
		return lastColumnsRange.getReadOnlyProperty();
	}

	protected void setLastColumnsRange(NumberRange<Integer> lastColumnsRange) {
		this.lastColumnsRange.set(lastColumnsRange);
	}
}
