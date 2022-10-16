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

import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.collections.Grid.Coordinates;
import io.github.palexdev.mfxcore.collections.ObservableGrid.Change;
import io.github.palexdev.mfxcore.enums.GridChangeType;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import io.github.palexdev.virtualizedfx.enums.UpdateType;
import io.github.palexdev.virtualizedfx.grid.paginated.PaginatedVirtualGrid;
import javafx.scene.Node;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class used by the {@link ViewportManager} to represent the state of the viewport at a given time.
 * <p>
 * The idea is to have an immutable state so that each state is a different object, with some exceptional cases
 * when the state doesn't need to be re-computed, so the old object is returned.
 * <p></p>
 * This offers information such as:
 * <p> - The range of rows contained in the state, {@link #getRowsRange()}
 * <p> - The range of columns contained by each {@link GridRow} in the state, {@link #getColumnsRange()}
 * <p> - The cells in the viewport. These are stored in {@link GridRow}s, each of these has a cell for each column.
 * {@link GridRow}s are kept in a map: rowIndex -> gridRow
 * <p> - The expected number of rows, {@link #getTargetSize()}. Note that this is computed by {@link GridHelper#maxRows()},
 * so the result may be greater than the number of rows available in the data structure
 * <p> - The type of event that lead the old state to transition to the new one, see {@link UpdateType}
 * <p> -A flag to check if new cells were created or some were deleted, {@link #haveCellsChanged()}.
 * This is used by {@link VirtualGridSkin} since we want to update the viewport children only when the cells change.
 * <p></p>
 * This also contains a particular global state, {@link #EMPTY}, typically used to indicate that the viewport
 * is empty, and no state can be created.
 */
@SuppressWarnings("rawtypes")
public class GridState<T, C extends GridCell<T>> {
	//================================================================================
	// Static Members
	//================================================================================
	public static final GridState EMPTY = new GridState();

	//================================================================================
	// Properties
	//================================================================================
	private final VirtualGrid<T, C> grid;
	private final Map<Integer, GridRow<T, C>> rows = new TreeMap<>();
	private final IntegerRange rowsRange;
	private final IntegerRange columnsRange;
	private final int targetSize;
	private UpdateType type = UpdateType.INIT;
	private boolean cellsChanged;

	//================================================================================
	// Constructors
	//================================================================================
	public GridState() {
		this.grid = null;
		this.rowsRange = IntegerRange.of(-1);
		this.columnsRange = IntegerRange.of(-1);
		this.targetSize = 0;
	}

	public GridState(VirtualGrid<T, C> grid, IntegerRange rowsRange, IntegerRange columnsRange) {
		this.grid = grid;
		this.rowsRange = rowsRange;
		this.columnsRange = columnsRange;
		this.targetSize = grid.getGridHelper().maxRows();
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Responsible for filling the viewport the needed amount of rows/cells. So this may supply or remove
	 * cells according to the viewport size.
	 * <p>
	 * If the given ranges for rows and columns are the same as the ones of the state then the old state is returned.
	 * <p>
	 * This is used by {@link ViewportManager#init()}.
	 *
	 * @return a new {@code GridState} which is the result of transitioning from this state to
	 * a new one given the new ranges for rows and columns
	 */
	protected GridState<T, C> init(IntegerRange rowsRange, IntegerRange columnsRange) {
		if (this.rowsRange.equals(rowsRange) && this.columnsRange.equals(columnsRange)) return this;

		GridState<T, C> newState = new GridState<>(grid, rowsRange, columnsRange);
		Set<Integer> range = IntegerRange.expandRangeToSet(rowsRange);
		int targetSize = rowsRange.diff() + 1;

		for (Integer rowIndex : rowsRange) {
			GridRow<T, C> row = rows.remove(rowIndex);
			if (row != null) {
				row.onInit(columnsRange);
				newState.addRow(rowIndex, row);
				range.remove(rowIndex);
			}
		}

		Deque<Integer> reusable = new ArrayDeque<>(rows.keySet());
		Deque<Integer> remaining = new ArrayDeque<>(range);
		while (newState.size() != targetSize) {
			int rIndex = remaining.removeFirst();
			Integer oIndex = reusable.poll();
			if (oIndex != null) {
				GridRow<T, C> row = rows.remove(oIndex);
				row.updateIndex(rIndex);
				newState.addRow(rIndex, row);
			} else {
				newState.addRow(rIndex);
			}
		}
		return newState;
	}

	/**
	 * This is responsible for transitioning to a new state when the viewport scrolls vertically.
	 * <p>
	 * Used by {@link ViewportManager#onVScroll()}.
	 */
	protected GridState<T, C> vScroll(IntegerRange rowsRange) {
		if (this.rowsRange.equals(rowsRange)) return this;

		GridState<T, C> newState = new GridState<>(grid, rowsRange, columnsRange);
		newState.type = UpdateType.SCROLL;
		Set<Integer> range = IntegerRange.expandRangeToSet(rowsRange);

		for (Integer rowIndex : rowsRange) {
			GridRow<T, C> row = rows.remove(rowIndex);
			if (row != null) {
				newState.addRow(rowIndex, row);
				range.remove(rowIndex);
			}
		}

		Deque<Integer> reusable = new ArrayDeque<>(rows.keySet());
		Deque<Integer> remaining = new ArrayDeque<>(range);
		while (!remaining.isEmpty()) {
			int rIndex = remaining.removeFirst();
			int oIndex = reusable.removeFirst();
			GridRow<T, C> row = rows.remove(oIndex);
			row.updateIndex(rIndex);
			row.setReusablePositions(false);
			newState.addRow(rIndex, row);
		}
		return newState;
	}

	/**
	 * This is responsible for transitioning to a new state when the viewport scrolls horizontally.
	 * <p>
	 * Used by {@link ViewportManager#onHScroll()}.
	 */
	protected GridState<T, C> hScroll(IntegerRange columnsRange) {
		if (this.columnsRange.equals(columnsRange)) return this;

		GridState<T, C> newState = new GridState<>(grid, rowsRange, columnsRange);
		newState.type = UpdateType.SCROLL;
		Iterator<Map.Entry<Integer, GridRow<T, C>>> it = rows.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, GridRow<T, C>> next = it.next();
			Integer index = next.getKey();
			GridRow<T, C> row = next.getValue();
			row.onScroll(columnsRange);
			newState.addRow(index, row);
			it.remove();
		}
		return newState;
	}

	/**
	 * This is responsible for transitioning to a new state when a change occurs in the grid's items data structure.
	 * <p>
	 * Specifically this handles the following {@link GridChangeType}s:
	 * <p></p>
	 * <b>REPLACE_ELEMENT</b>
	 * Simple case, if the row at which the change occurred, {@link Change#getCoordinates()}, is in range of this state,
	 * calls {@link GridRow#onReplace(int, Object)} on it.
	 * <p>
	 * Note that this type of change returns the old state.
	 * <p></p>
	 * <b>REPLACE_DIAGONAL</b>
	 * The new diagonal is given by the change's added list, we put these new items in a {@link Deque}.
	 * <p>
	 * For each row then we get an item from the deque with {@link Deque#poll()} and call {@link GridRow#onDiagReplace(Object)}.
	 * <p>
	 * Note that this type of change returns the old state.
	 * <p></p>
	 * <b>REPLACE_ROW</b>
	 * Simple case, if the row at which the change occurred, {@link Change#getCoordinates()}, is in range of this state,
	 * calls {@link GridRow#onReplace()} on it.
	 * <p>
	 * Note that this type of change returns the old state.
	 * <p></p>
	 * <b>REPLACE_COLUMN</b>
	 * The new column is given by the change's added list, we put these new items in a {@link Deque}.
	 * Note though that since not all the column may be displayed in the viewport we get only the items in the columns range,
	 * {@link #getColumnsRange()}.
	 * <p>
	 * For each row then we get an item from the deque with {@link Deque#poll()} and call {@link GridRow#onReplace(int, Object)}.
	 * <p>
	 * Note that this type of change returns the old state.
	 * <p></p>
	 * <b>ADD_ROW</b>
	 * This is a more complex operation. Before starting the computation we get the index at which the row was added and
	 * the rows range we expect with {@link GridHelper#rowsRange()}.
	 * <p>
	 * If the range is equal to the one in the state, the insertion index is greater than the range max and the viewport is filled,
	 * {@link #rowsFilled()}, then the old state is returned. In any other case the computation can begin.
	 * <p></p>
	 * A {@link Set} will keep track of the rows we have re-used (to be precise it will contain the remaining ones).
	 * <p>
	 * The computation is divided in three main parts:
	 * <p> - Valid rows: these are the ones that come before the insertion index
	 * <p> - Partial rows: these are the ones that come after the insertion index but their index must be updated to be +1.
	 * Rows that were valid are ignored. Rows for which the new index would be outside the expected range are also ignored.
	 * <p> - Remaining rows: for insertions we always expect one row to be remaining at this point. If the viewport was full,
	 * then this row is one of the ignored ones (not the valid ones though). If the viewport was not full then a new row needs
	 * to be created
	 * <p></p>
	 * <b>REMOVE_ROW</b>
	 * This is a more complex operation. Before starting the computation we get the index at which the row was removed and
	 * the rows range we expect with {@link GridHelper#rowsRange()}.
	 * <p>
	 * If the range is equal to the one in the state and the removal index is greater than the range max, the old
	 * state is returned. In any other case the computation can begin.
	 * <p></p>
	 * This time two {@link Set}s are used: one to keep track of the rows we have re-used (to be precise it will contain
	 * the remaining ones), and another to keep track of the "covered" indexes of the expected range.
	 * <p>
	 * The computation is divided in three main parts:
	 * <p> - Valid rows: these are the ones that come before the removal index. Note that the start index for the loop
	 * is computes as the maximum between the state's range minimum and the expected range minimum, so
	 * {@code Math.max(expRange.getMin(), stateRange.getMin())}.
	 * <p> - Partial rows: these are the ones that come after the removal index but their index must be updated to be -1.
	 * Rows that were valid are ignored. Rows for which the new index would be outside the expected range are also ignored.
	 * <p> - Remaining rows: for removals this part is a bit different. Before proceeding, we must check if the {@link Set}
	 * used to keep track of the "covered" indexes is empty, because in such case it means that we already have all the rows
	 * needed to fill the viewport. In this case we call {@link #clear()} to make sure that remaining rows in the old state
	 * are disposed and cleared, then exit the switch.
	 * In case the {@link Set} is not empty then we always expect to have one remaining index in the second {@link Set},
	 * while in the first {@link Set} there may be or not an index which will be a reusable row. In either cases, we need
	 * another row to fill the viewport, so one is reused if available or created.
	 * <p></p>
	 * <b>ADD_COLUMN</b>
	 * This is a more complex and costly operation. No matter where the change occurred, almost all the cells will
	 * need either a partial (index only) or full (index and item) update.
	 * <p>
	 * Once we get the index at which the column has been added and the expected range with {@link GridHelper#columnsRange()},
	 * on each row in the state we delegate the computation to {@link GridRow#onColumnAdd(int, IntegerRange)}, rows
	 * are moved then to the new state.
	 * <p></p>
	 * <b>REMOVE_COLUMN</b>
	 * This is a more complex and costly operation. No matter where the change occurred, almost all the cells will need
	 * either a partial (index only) or full (index and item) update.
	 * <p>
	 * Once we get the index at which the column has been removed and the expected range with {@link GridHelper#columnsRange()},
	 * on each row in the state we delegate the computation to {@link GridRow#onColumnRemove(int, IntegerRange)}, rows
	 * are moved then to the new state.
	 * <p></p>
	 * <p></p>
	 * Before returning the state which is the result of one of the above changes, we make sure to update the state's type,
	 * which will be {@link UpdateType#CHANGE}, then we check if the total size of the new state ({@link #totalSize()}) is
	 * different from the old state total size so that in such case we can also tell the new state that the viewport
	 * will also need to update its children. Then we call {@link Change#endChange()} to dispose the current change and finally
	 * return the state.
	 */
	protected GridState<T, C> change(Change<T> change) {
		int cellsNum = totalSize();
		GridState<T, C> state = this;

		switch (change.getType()) {
			case REPLACE_ELEMENT: {
				Coordinates coordinates = change.getCoordinates();
				Optional.ofNullable(rows.get(coordinates.getRow()))
						.ifPresent(row -> row.onReplace(coordinates.getColumn(), change.getAdded().get(0)));
				break;
			}
			case REPLACE_DIAGONAL: {
				Deque<T> diag = new ArrayDeque<>(change.getAdded());
				rows.values().forEach(row -> row.onDiagReplace(diag.poll()));
				break;
			}
			case REPLACE_ROW: {
				int row = change.getCoordinates().getRow();
				Optional.ofNullable(rows.get(row))
						.ifPresent(GridRow::onReplace);
				break;
			}
			case REPLACE_COLUMN: {
				int index = change.getCoordinates().getColumn();
				Deque<T> col = new ArrayDeque<>(change.getAdded().subList(columnsRange.getMin(), columnsRange.getMax()));
				rows.values().forEach(row -> row.onReplace(index, col.poll()));
				break;
			}
			case ADD_ROW: {
				GridHelper helper = grid.getGridHelper();
				int index = change.getCoordinates().getRow();
				IntegerRange range = helper.rowsRange();
				if (range.equals(rowsRange) && index > rowsRange.getMax() && rowsFilled()) break;

				state = new GridState<>(grid, range, columnsRange);
				Set<Integer> available = new HashSet<>(rows.keySet());

				// Valid
				for (int i = range.getMin(); i < index; i++) {
					GridRow<T, C> row = rows.remove(i);
					row.setReusablePositions(true);
					state.addRow(i, row);
					available.remove(i);
				}

				// Partial
				Iterator<Map.Entry<Integer, GridRow<T, C>>> it = rows.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<Integer, GridRow<T, C>> next = it.next();
					int rIndex = next.getKey();
					int newIndex = rIndex + 1;
					if (!IntegerRange.inRangeOf(newIndex, range) || (newIndex >= range.getMin() && newIndex < index)) {
						continue;
					}

					GridRow<T, C> row = next.getValue();
					row.onRowAdd(newIndex);
					state.addRow(newIndex, row);
					available.remove(rIndex);
					it.remove();
				}

				// Remaining
				Integer oIndex = new ArrayDeque<>(available).poll();
				GridRow<T, C> row;
				if (oIndex != null) {
					row = rows.remove(oIndex);
					row.updateIndex(index);
				} else {
					row = GridRow.of(grid, index, columnsRange).init();
				}
				state.addRow(index, row);
				break;
			}
			case REMOVE_ROW: {
				GridHelper helper = grid.getGridHelper();
				int index = change.getCoordinates().getRow();
				IntegerRange range = helper.rowsRange();
				if (range.equals(rowsRange) && index > range.getMax()) break;

				state = new GridState<>(grid, range, columnsRange);
				Set<Integer> available = new HashSet<>(rows.keySet());
				Set<Integer> rangeSet = IntegerRange.expandRangeToSet(range);

				// Valid
				int start = Math.max(range.getMin(), rowsRange.getMin());
				for (int i = start; i < index; i++) {
					GridRow<T, C> row = rows.remove(i);
					row.setReusablePositions(true);
					state.addRow(i, row);
					available.remove(i);
					rangeSet.remove(i);
				}

				// Partial
				Iterator<Map.Entry<Integer, GridRow<T, C>>> it = rows.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<Integer, GridRow<T, C>> next = it.next();
					int rIndex = next.getKey();
					int newIndex = rIndex - 1;
					if (!IntegerRange.inRangeOf(newIndex, range) || (newIndex >= start && newIndex < index)) {
						continue;
					}

					GridRow<T, C> row = next.getValue();
					row.onRowRemove(newIndex);
					state.addRow(newIndex, row);
					available.remove(rIndex);
					rangeSet.remove(newIndex);
					it.remove();
				}

				// Remaining
				if (rangeSet.isEmpty()) {
					clear();
					break;
				}

				Integer oIndex = new ArrayDeque<>(available).poll();
				int nIndex = new ArrayDeque<>(rangeSet).removeFirst();
				GridRow<T, C> row;
				if (oIndex != null) {
					row = rows.remove(oIndex);
					row.updateIndex(nIndex);
				} else {
					row = GridRow.of(grid, nIndex, columnsRange).init();
				}
				state.addRow(nIndex, row);
				break;
			}
			case ADD_COLUMN: {
				GridHelper helper = grid.getGridHelper();
				IntegerRange range = helper.columnsRange();
				int index = change.getCoordinates().getColumn();

				state = new GridState<>(grid, rowsRange, range);
				Iterator<Map.Entry<Integer, GridRow<T, C>>> it = rows.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<Integer, GridRow<T, C>> next = it.next();
					int rIndex = next.getKey();
					GridRow<T, C> row = next.getValue();
					row.onColumnAdd(index, range);
					state.addRow(rIndex, row);
					it.remove();
				}
				break;
			}
			case REMOVE_COLUMN: {
				GridHelper helper = grid.getGridHelper();
				IntegerRange range = helper.columnsRange();
				int index = change.getCoordinates().getColumn();

				state = new GridState<>(grid, rowsRange, columnsRange);
				Iterator<Map.Entry<Integer, GridRow<T, C>>> it = rows.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<Integer, GridRow<T, C>> next = it.next();
					Integer rIndex = next.getKey();
					GridRow<T, C> row = next.getValue();
					row.onColumnRemove(index, range);
					state.addRow(rIndex, row);
					it.remove();
				}
				break;
			}
		}

		state.type = UpdateType.CHANGE;
		if (state.totalSize() != cellsNum) state.cellsChanged();
		change.endChange();
		return state;
	}

	/**
	 * This is responsible for laying out the rows in the viewport with the help of {@link GridHelper#layout(Node, double, double)}.
	 * <p></p>
	 * Before starting the actual layout computation there are a bunch of information required for it to work properly:
	 * <p> - The cells' size, as well as the number of rows and columns of the grid
	 * <p> - The rows and columns ranges. These are not computed with {@link GridHelper} because in some exceptional cases
	 * they may differ
	 * <p> - The computed ranges are needed to check for two exceptional cases. For how the viewport works we always have
	 * one Cell of overscan/buffer whatever you want to call it. But when we are at the end (last column or last row, or both)
	 * that one Cell will need to be moved to be the first cell of the range since there are no more items to the right/bottom.
	 * Two boolean flags are used to detect such cases.
	 * <p> - Cells are laid out from the bottom-right. The two positions are computed as follows:
	 * <p> &#10240; &#x2800; - The bottom position for rows is given by: {@code rowsRange.diff() * cellHeight} or in the above exceptional case
	 * by {@code (rowsRange.diff() - 1) * cellHeight}
	 * <p> &#10240; &#x2800; - The right position for columns is given by: {@code columnsRange.diff() * cellWidth} or in the above exceptional case
	 * by {@code (columnsRange.diff() - 1) * cellWidth}
	 * <p></p>
	 * At this point rows are laid out from the bottom to the top, and each row is responsible for laying out its cells
	 * with {@link GridRow#layoutCells(double, boolean)}.
	 */
	public void layoutRows() {
		if (isEmpty()) return;
		if (grid instanceof PaginatedVirtualGrid) {
			layoutPaginatedRows();
			return;
		}

		GridHelper helper = grid.getGridHelper();
		Size size = grid.getCellSize();
		int gRows = grid.getRowsNum(); // Grid Rows
		int gColumns = grid.getColumnsNum(); // Grid Columns

		int firstRow = helper.firstRow();
		int lastRow = firstRow + helper.maxRows() - 1;
		boolean adjustRows = lastRow > gRows - 1 && rowsFilled();

		int firstColumn = helper.firstColumn();
		int lastColumn = firstColumn + helper.maxColumns() - 1;
		boolean adjustColumns = lastColumn > gColumns - 1 && columnsFilled();

		double bottom = rowsRange.diff() * size.getHeight();
		if (adjustRows) bottom -= size.getHeight();

		ListIterator<GridRow<T, C>> it = new ArrayList<>(rows.values()).listIterator(size());
		while (it.hasPrevious()) {
			GridRow<T, C> row = it.previous();
			row.layoutCells(bottom, adjustColumns);
			bottom -= size.getHeight();
		}
	}

	/**
	 * This is the implementation of {@link #layoutRows()} exclusively for {@link PaginatedVirtualGrid}s.
	 * <p>
	 * This is simpler as there is no "free" vertical scrolling, all cells will have a precise vertical position at any
	 * time in the page.
	 * <p></p>
	 * Before starting the actual layout computation there are a bunch of information required for it to work properly:
	 * <p> - The cells' size, as well as the number columns of the grid
	 * <p> - The rows and columns ranges. These are not computed with {@link GridHelper} because in some exceptional cases
	 * they may differ
	 * <p> - The computed ranges are needed to check for two exceptional cases. For how the viewport works we always have
	 * one Cell of overscan/buffer whatever you want to call it. But when we are at the end (last column or last row, or both)
	 * that one Cell will need to be moved to be the first cell of the range since there are no more items to the right/bottom.
	 * For {@link PaginatedVirtualGrid} we only need a boolean flag for the columns as there is no overscan for the rows.
	 * <p> - Cells are laid out from the top-right. The two positions are computed as follows:
	 * <p> &#10240; &#x2800; - The top position starts at 0 and increases by {@code cellHeight} at each loop iteration
	 * <p> &#10240; &#x2800; - The right position for columns is given by: {@code columnsRange.diff() * cellWidth} or in the above exceptional case
	 * by {@code (columnsRange.diff() - 1) * cellWidth}
	 * <p></p>
	 * At this point rows are laid out from the top to the bottom, and each row is responsible for laying out its cells
	 * with {@link GridRow#layoutCells(double, boolean)}.
	 * <p></p>
	 * Last but not least, for {@link PaginatedVirtualGrid}s it may happen that there aren't enough rows to entirely
	 * fill a page, in such case extra rows are still present in the viewport, but they need to be hidden with
	 * {@link GridRow#setVisible(boolean)}.
	 */
	public void layoutPaginatedRows() {
		PaginatedVirtualGrid pGrid = ((PaginatedVirtualGrid) grid);
		GridHelper helper = pGrid.getGridHelper();
		Size size = pGrid.getCellSize();
		int gColumns = pGrid.getColumnsNum(); // Grid Columns

		int firstRow = helper.firstRow();
		int lastRow = Math.min(firstRow + helper.maxRows() - 1, pGrid.getRowsNum() - 1);
		IntegerRange range = IntegerRange.of(firstRow, lastRow);

		int firstColumn = helper.firstColumn();
		int lastColumn = firstColumn + helper.maxColumns() - 1;
		boolean adjustColumns = lastColumn > gColumns - 1 && columnsFilled();

		double pos = 0;
		for (int i = firstRow; i <= lastRow; i++) {
			GridRow<T, C> row = rows.get(i);
			row.layoutCells(pos, adjustColumns);
			pos += size.getHeight();
			row.setVisible(true);
		}

		// Hide extra rows that are not in range
		rows.entrySet().stream()
				.filter(e -> !IntegerRange.inRangeOf(e.getKey(), range))
				.forEach(e -> e.getValue().setVisible(false));
	}

	/**
	 * Creates a new {@link GridRow} with the given index and the state's columns range, initializes it with {@link GridRow#init()}
	 * then adds it to the state's map.
	 */
	protected void addRow(int index) {
		rows.put(index, GridRow.of(grid, index, columnsRange).init());
	}

	/**
	 * Adds the given {@link GridRow} to the state's map at the given index.
	 */
	protected void addRow(int index, GridRow<T, C> row) {
		rows.put(index, row);
	}

	/**
	 * For every {@link GridRow} in the state's map calls {@link GridRow#clear()} then clears the map,
	 * making the state empty.
	 */
	protected void clear() {
		rows.values().forEach(GridRow::clear);
		rows.clear();
	}

	/**
	 * By iterating over all the rows in the state (using Streams) this converts the cells contained in the rows
	 * to a list of {@link Node}s, with {@link C#getNode()}
	 */
	public List<Node> getNodes() {
		return rows.values().stream()
				.flatMap(row -> row.getCells().values().stream())
				.map(C::getNode)
				.collect(Collectors.toUnmodifiableList());
	}

	/**
	 * By iterating over all the rows in the state (using Streams) this gathers all the cells contained in the rows
	 * into one list.
	 */
	public List<C> getCells() {
		return rows.values().stream()
				.flatMap(row -> row.getCells().values().stream())
				.collect(Collectors.toList());
	}

	/**
	 * By iterating over all the rows in the state (using Streams) this gathers all the cells contained in the rows
	 * in one map. Cells are mapped as follows: linearIndex -> Cell.
	 * <p>
	 * Note that since Cells are kept by their column index in the rows, we use flatMap on {@link GridRow#getLinearCells()}.
	 */
	public Map<Integer, C> getIndexedCells() {
		return rows.values().stream()
				.flatMap(row -> row.getLinearCells().entrySet().stream())
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue
				));
	}

	/**
	 * @return whether there are enough rows is this state to fill the viewport
	 */
	public boolean rowsFilled() {
		if (grid instanceof PaginatedVirtualGrid) {
			return rows.values().stream()
					.allMatch(GridRow::isVisible);
		}
		return size() >= targetSize;
	}

	/**
	 * @return whether there are enough columns in this state to fill the viewport
	 */
	public boolean columnsFilled() {
		GridHelper helper = grid.getGridHelper();
		int targetColumns = helper.maxColumns();
		return rows.values().stream()
				.allMatch(row -> row.size() >= targetColumns);
	}

	/**
	 * @return the number of rows in this state
	 */
	public int size() {
		return rows.size();
	}

	/**
	 * @return the total number of cells in this state, as the sum of all the cells of each individual {@link GridRow}
	 */
	public int totalSize() {
		return rows.values().stream()
				.mapToInt(GridRow::size)
				.sum();
	}

	/**
	 * @return whether the state is empty, no rows in it
	 */
	public boolean isEmpty() {
		return rows.isEmpty();
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

	/**
	 * @return the map used to keep the {@link GridRow}s
	 */
	protected Map<Integer, GridRow<T, C>> getRows() {
		return rows;
	}

	/**
	 * @return {@link #getRows()} as an unmodifiable map
	 */
	public Map<Integer, GridRow<T, C>> getRowsUnmodifiable() {
		return Collections.unmodifiableMap(rows);
	}

	/**
	 * @return the range of rows in the state
	 */
	public IntegerRange getRowsRange() {
		return rowsRange;
	}

	/**
	 * @return the range of columns of each {@link GridRow} in the state
	 */
	public IntegerRange getColumnsRange() {
		return columnsRange;
	}

	/**
	 * @return the expected number of rows
	 */
	public int getTargetSize() {
		return targetSize;
	}

	/**
	 * @return the type of change/event that caused an old state to transition to this new one
	 */
	public UpdateType getType() {
		return type;
	}

	/**
	 * @return whether a change caused the total size of the new state to change compared to the old state
	 */
	public boolean haveCellsChanged() {
		return cellsChanged;
	}

	/**
	 * Sets the cellsChanged flag to true, causing {@link #haveCellsChanged()} to return true, which will tell the
	 * viewport to update its children.
	 */
	protected void cellsChanged() {
		this.cellsChanged = true;
	}
}
