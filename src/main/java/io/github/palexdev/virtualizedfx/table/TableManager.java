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

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.NumberRange;
import io.github.palexdev.mfxcore.base.properties.range.IntegerRangeProperty;
import io.github.palexdev.mfxcore.utils.fx.ListChangeHelper;
import io.github.palexdev.mfxcore.utils.fx.ListChangeHelper.Change;
import io.github.palexdev.virtualizedfx.beans.TableStateProperty;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.flow.FlowState;
import javafx.collections.ListChangeListener;

import java.util.List;

/**
 * The {@code FlowManager} is responsible for managing the table's viewport, track its current {@link TableState}
 * and trigger states transitions.
 * <p>
 * It stores the state of the viewport at any given time with three properties:
 * <p> - the main one is the {@link #stateProperty()} which holds the {@link TableState} object representing the current
 * state of the viewport
 * <p> - the last rows range property, which holds the last range of displayed rows as a {@link IntegerRange}
 * <p> - the last columns range property, which holds the last range of displayed columns as a {@link IntegerRange}
 * <p></p>
 * As mentioned above this is also responsible for handling states transitions when: initializations occur (cells supply
 * and removals when the viewport size changes); vertical and horizontal scrolling; changes occurred in the items list,
 * as well as clear and reset of the viewport when needed
 *
 * @param <T> the type of items
 */
@SuppressWarnings({"unchecked"})
public class TableManager<T> {
	//================================================================================
	// Properties
	//================================================================================
	private final VirtualTable<T> table;
	private final TableStateProperty<T> state = new TableStateProperty<T>(TableState.EMPTY);
	private final IntegerRangeProperty lastRowsRange = new IntegerRangeProperty();
	private final IntegerRangeProperty lastColumnsRange = new IntegerRangeProperty();
	private boolean processingChange = false;

	//================================================================================
	// Constructors
	//================================================================================
	TableManager(VirtualTable<T> table) {
		this.table = table;
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
	 * <p> - the expected range of rows, {@link TableHelper#rowsRange()}
	 * <p> - the expected range of columns, {@link TableHelper#columnsRange()}
	 * <p> - the old/current state, {@link #stateProperty()}
	 * <p>
	 * We also ensure that the estimated size of the viewport is correct by calling {@link TableHelper#computeEstimatedSize()}.
	 * <p></p>
	 * The second step is to distinguish between two cases:
	 * <p> 1) The old/current state is {@link TableState#EMPTY}
	 * <p> 2) The old/current state is a valid state
	 * <p></p>
	 * In the first case it means that the viewport is empty. For each row in the expected rows range we add a row
	 * to the new state with {@link TableState#addRow(int)}. Since the viewport is empty we also need to call
	 * {@link TableState#rowsChanged()}. Last but not least we update all the properties of the manager, and then
	 * request the viewport layout with {@link VirtualTable#requestViewportLayout()}.
	 * <p></p>
	 * In the second case we call {@link TableState#init(IntegerRange, IntegerRange)} on the old/current state so that
	 * a new state, which reuses when possible the current already present rows, can be computed. At this point a special
	 * check is needed, the aforementioned method could also return the old/current state for whatever reason, in such case
	 * there's no need to proceed and the method exits immediately. Otherwise, same as above, update all the properties,
	 * call {@link TableState#rowsChanged()} and then {@link VirtualTable#requestViewportLayout()}.
	 *
	 * @return in addition to the various computations made in this method, it also returns a boolean value to indicate
	 * whether computations lead to a layout request or not, {@link VirtualTable#requestViewportLayout()}
	 */
	public boolean init() {
		if (itemsEmpty() || columnsEmpty()) return false;

		// Pre-Computation
		TableHelper helper = table.getTableHelper();
		IntegerRange rowsRange = helper.rowsRange();
		IntegerRange columnsRange = helper.columnsRange();
		helper.computeEstimatedSize();

		// Check old state
		TableState<T> oldState = getState();
		TableState<T> newState = new TableState<>(table, rowsRange, columnsRange);

		if (oldState == TableState.EMPTY) {
			for (Integer row : rowsRange) {
				newState.addRow(row);
			}
			newState.rowsChanged();
			setState(newState);
			setLastRowsRange(rowsRange);
			setLastColumnsRange(columnsRange);
			table.requestViewportLayout();
			return true;
		}

		// Transition from old state to new state
		newState = oldState.init(rowsRange, columnsRange);
		if (newState == oldState) return false;
		newState.rowsChanged();
		setState(newState);
		setLastRowsRange(rowsRange);
		setLastColumnsRange(columnsRange);
		table.requestViewportLayout();
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
	 * we call {@link TableState#vScroll(IntegerRange)} to create a new state which will contain
	 * all the needed rows for the new range
	 * <p>
	 * The layout instead uses a different range and is compared against the last rows range, if they are not
	 * equal then {@link VirtualTable#requestViewportLayout()} is invoked.
	 * <p>
	 * At the end the last rows range property is updated.
	 */
	public void onVScroll() {
		TableState<T> state = getState();
		if (state == TableState.EMPTY || state.isEmpty() || itemsEmpty()) return;

		TableHelper helper = table.getTableHelper();
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
			table.requestViewportLayout();
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
	 * we call {@link TableState#hScroll(IntegerRange)} to create a new state which will contain
	 * all the needed columns for the new range
	 * <p>
	 * The layout instead uses a different range and is compared against the last columns range, if they are not
	 * equal then {@link VirtualTable#requestViewportLayout()} is invoked.
	 * <p>
	 * At the end the last columns range property is updated.
	 */
	public void onHScroll() {
		TableState<T> state = getState();
		if (state == TableState.EMPTY || state.isEmpty()) return;

		TableHelper helper = table.getTableHelper();
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
			table.requestViewportLayout();
		}

		setLastColumnsRange(lRange);
	}

	/**
	 * This is responsible for updating the viewport state whenever a change occurs in the items list.
	 * <p></p>
	 * There are three separate situations:
	 * <p> 1) The items list is empty, calls {@link #clear()} and exits
	 * <p> 2) The current state is {@link TableState#EMPTY}, calls {@link #init()} and exits
	 * <p> 3) The given change is processed by using the {@link ListChangeHelper} utility class,
	 * then the new state is computed by using {@link TableState#processChange(Change)}, finally
	 * {@link VirtualTable#requestViewportLayout()} is called and the last range property is updated.
	 */
	public void onChange(ListChangeListener.Change<? extends T> change) {
		try {
			processingChange = true;
			TableState<T> oState = getState();
			if (itemsEmpty()) {
				clear();
				return;
			}

			if (oState == TableState.EMPTY) {
				init();
				return;
			}

			List<Change> changes = ListChangeHelper.instance().processChange(change);
			TableState<T> nState = oState.change(changes);
			if (nState != oState) {
				setState(nState);
				table.requestViewportLayout();
				setLastRowsRange(nState.getRowsRange());
				setLastColumnsRange(nState.getColumnsRange());
			}
		} catch (RuntimeException ex) {
			ex.printStackTrace();
		} finally {
			processingChange = false;
		}
	}

	/**
	 * This is responsible for transitioning to a new valid state when a cell factory ahs changed
	 * for a certain column. The state is computed with {@link TableState#columnChangedFactory(TableColumn)}.
	 * <p>
	 * At the end {@link VirtualTable#requestViewportLayout()} is invoked.
	 */
	public void onColumnChangedFactory(TableColumn<T, ? extends TableCell<T>> column) {
		TableState<T> state = getState();
		setState(state.columnChangedFactory(column));
		table.requestViewportLayout();
	}

	/**
	 * Clears the viewport. Sets the state to {@link FlowState#EMPTY}.
	 */
	public void clear() {
		getState().clear();
		setState(TableState.EMPTY);
		TableHelper helper = table.getTableHelper();
		helper.computeEstimatedSize();
		helper.invalidatedPos();
	}

	/**
	 * Resets the viewport by first calling {@link #clear()}, then {@link #init()}.
	 */
	public void reset() {
		clear();
		init();
	}

	/**
	 * @return whether the items list is empty
	 */
	private boolean itemsEmpty() {
		return table.getItems().isEmpty();
	}

	/**
	 * @return whether the columns list is empty
	 */
	private boolean columnsEmpty() {
		return table.getColumns().isEmpty();
	}

	//================================================================================
	// Getters/Setters
	//================================================================================
	public TableState<T> getState() {
		return state.get();
	}

	/**
	 * Specifies the current state of the viewport as a {@link TableState} object.
	 */
	public TableStateProperty<T> stateProperty() {
		return state;
	}

	protected void setState(TableState<T> state) {
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

	protected void setLastRowsRange(NumberRange<Integer> lastRowsRange) {
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

	protected void setLastColumnsRange(NumberRange<Integer> lastColumnsRange) {
		this.lastColumnsRange.set(lastColumnsRange);
	}

	/**
	 * @return whether a change is being processed by {@link #onChange(ListChangeListener.Change)}
	 */
	public boolean isProcessingChange() {
		return processingChange;
	}
}