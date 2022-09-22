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

import io.github.palexdev.mfxcore.base.beans.SizeBean;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.collections.Grid.Coordinate;
import io.github.palexdev.mfxcore.collections.ObservableGrid;
import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import io.github.palexdev.virtualizedfx.enums.UpdateType;
import javafx.scene.Node;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Class used by the {@link ViewportManager} to represent the state of the viewport at a given time.
 * <p>
 * The idea is to have an immutable state so that each state is a different object, with some exceptional cases
 * when the state doesn't need to be re-computed, so the old object is returned.
 * <p></p>
 * This offers information such as:
 * <p> - The range of rows contained in the state, {@link #getRowsRange()}. <b>Note</b> that sometimes this range doesn't
 * correspond to the needed range of rows. To always get a 100% accurate range use the {@link GridHelper}.
 * <p> - The range of columns contained in the state, {@link #getColumnsRange()}. <b>Note</b> that sometimes this range doesn't
 * correspond to the needed range of columns. To always get a 100% accurate range use the {@link GridHelper}.
 * <p> - The cells in the viewport. These are stored in {@link LayoutRow}s, each of these has a cell for each of the needed
 * columns. These are kept in a map rowIndex -> layoutRow.
 * <p> - The expected number of rows, {@link #getTargetSize()}.
 * <p> - The type of event that lead the old state to transition to the new one, see {@link UpdateType}
 * <p> - A flag to check if new cells were created or some were deleted, {@link #haveCellsChanged()}.
 * This is used by {@link VirtualGridSkin} since we want to update the viewport children only when the cells
 * changed.
 * <p></p>
 * This also contains a particular global state, {@link #EMPTY}, typically used to indicate that the viewport
 * is empty, and no state can be created.
 */
@SuppressWarnings("rawtypes")
public class GridState<T, C extends GridCell<T>> {
	//================================================================================
	// Static Properties
	//================================================================================
	public static final GridState EMPTY = new GridState<>();

	//================================================================================
	// Properties
	//================================================================================
	private final VirtualGrid<T, C> virtualGrid;
	private final IntegerRange rowsRange;
	private final IntegerRange columnsRange;
	private final Map<Integer, LayoutRow<T, C>> rows = new TreeMap<>();
	private final int targetSize;
	private UpdateType type = UpdateType.INIT;
	private boolean cellsChanged = false;

	//================================================================================
	// Constructors
	//================================================================================
	private GridState() {
		this.virtualGrid = null;
		this.rowsRange = IntegerRange.of(-1);
		this.columnsRange = IntegerRange.of(-1);
		this.targetSize = 0;
	}

	public GridState(VirtualGrid<T, C> virtualGrid, IntegerRange rowsRange, IntegerRange columnsRange) {
		this.virtualGrid = virtualGrid;
		this.rowsRange = rowsRange;
		this.columnsRange = columnsRange;
		this.targetSize = Math.min(
				virtualGrid.getGridHelper().maxRows(),
				virtualGrid.getRowsNum()
		);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Instructs every rows present in the {@link #getRows()} map to initialize itself,
	 * {@link LayoutRow#init()}.
	 */
	protected void init() {
		rows.values().forEach(LayoutRow::init);
	}

	/**
	 * This is responsible for transitioning to a new state by adding/removing rows when {@link ViewportManager#init()}
	 * is called and the old state is not empty.
	 */
	protected GridState<T, C> initTransition(IntegerRange rowsRange, IntegerRange columnsRange) {
		//if (this.rowsRange.equals(rowsRange) && this.columnsRange.equals(columnsRange)) return this;
		GridState<T, C> newState = new GridState<>(virtualGrid, rowsRange, columnsRange);

		Set<Integer> available = IntegerRange.expandRangeToSet(rowsRange);
		int targetSize = rowsRange.diff() + 1;

		// Commons can be reused, make sure to update columns range always
		Iterator<Map.Entry<Integer, LayoutRow<T, C>>> rIt = rows.entrySet().iterator();
		while (rIt.hasNext() && !available.isEmpty()) {
			Map.Entry<Integer, LayoutRow<T, C>> next = rIt.next();
			Integer rIndex = next.getKey();
			if (available.remove(rIndex)) {
				LayoutRow<T, C> row = next.getValue();
				row.updateColumns(columnsRange);
				newState.addRow(rIndex, row);
				rIt.remove();
			}
		}

		// Remaining processing
		if (newState.size() < targetSize) {
			Deque<Integer> remaining = new ArrayDeque<>(rows.keySet());
			for (Integer row : available) {
				Integer rIndex = remaining.poll();
				LayoutRow<T, C> lr;
				if (rIndex != null) {
					lr = rows.remove(rIndex);
					lr.updateIndex(row);
					lr.updateColumns(columnsRange);
				} else {
					lr = LayoutRow.of(virtualGrid, row, columnsRange);
					lr.init();
				}
				newState.addRow(row, lr);
			}
		}

		disposeAndClear();
		return newState;
	}

	/**
	 * This is responsible for transitioning to a new state when the vertical scrolling caused the viewport
	 * to display a different range of rows.
	 * <p></p>
	 * Every common row (the old and new ranges intersection) is extracted from the old state and moved to the new state.
	 * <p>
	 * Every new index is put in a deque. Iterating over the remaining rows in the old state and with the indexes
	 * extracted from the deque, rows are updated, {@link LayoutRow#updateIndex(int)}, and moved to the new state.
	 */
	public GridState<T, C> rowsTransition(IntegerRange newRange) {
		if (newRange.equals(rowsRange)) return this;

		GridState<T, C> newState = new GridState<>(virtualGrid, newRange, columnsRange);
		newState.type = UpdateType.SCROLL;

		Deque<Integer> toUpdate = new ArrayDeque<>();
		for (Integer row : newRange) {
			LayoutRow<T, C> common = rows.remove(row);
			if (common != null) {
				newState.addRow(row, common);
				continue;
			}
			toUpdate.add(row);
		}

		Iterator<LayoutRow<T, C>> it = rows.values().iterator();
		while (!toUpdate.isEmpty() && it.hasNext()) {
			LayoutRow<T, C> next = it.next();
			int index = toUpdate.removeFirst();
			next.updateIndex(index);
			newState.addRow(index, next);
			next.setReusablePositions(false);
			it.remove();
		}
		return newState;
	}

	/**
	 * This is responsible for transitioning to a new state when the horizontal scrolling caused the viewport
	 * to display a different range of columns.
	 * <p></p>
	 * Every row is updated with {@link LayoutRow#updateColumns(IntegerRange)}, and moved to the new state.
	 */
	public GridState<T, C> columnsTransition(IntegerRange newRange) {
		if (newRange.equals(columnsRange)) return this;

		GridState<T, C> newState = new GridState<>(virtualGrid, rowsRange, newRange);
		newState.type = UpdateType.SCROLL;

		Iterator<Map.Entry<Integer, LayoutRow<T, C>>> it = rows.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, LayoutRow<T, C>> entry = it.next();
			Integer index = entry.getKey();
			LayoutRow<T, C> row = entry.getValue();
			row.updateColumns(newRange);
			newState.addRow(index, row);
			it.remove();
		}
		return newState;
	}

	/**
	 * This is responsible for transitioning to a new state when a change occurs in the items grid.
	 * <p></p>
	 * <b>ELEMENT REPLACEMENT</b>
	 * Simple case, from the number of columns and the linear index at which the change started we compute the coordinates
	 * at which the change occurred using {@link GridUtils#indToSub(int, int, BiFunction)}.
	 * If there is a row at the computed coordinates and also if there is a cell at the given coordinates we update
	 * the cell's item. The new item is simply retrieved from the change's added list at position 0.
	 * <p>
	 * Note that this type of change returns the old state.
	 * <p></p>
	 * <b>REPLACE DIAGONAL</b>
	 * The new diagonal is given by the change's added list, we put these new items in a {@link Deque}.
	 * <p>
	 * For each row then we get an item from the deque with {@link Deque#poll()} and call {@link LayoutRow#onDiagonalUpdate(Object)}
	 * <p>
	 * Note that this type of change returns the old state.
	 * <p></p>
	 * <b>REPLACE ROW</b>
	 * From the grid's number of columns and the linear index at which the change occurred we get the index
	 * of the replaced row with {@link GridUtils#indToRow(int, int)}.
	 * Then if the replaced row is present in this state we call {@link LayoutRow#onRowReplacement()}
	 * <p>
	 * Note that this type of change returns the old state.
	 * <p></p>
	 * <b>COLUMN REPLACEMENT</b>
	 * From the grid's number of column and the linear index at which the change occurred, we get the
	 * column index at which the change occurred, for each row in the state we call {@link LayoutRow#onColumnReplacement(int)}.
	 * <p>
	 * Note that this type of change returns the old state.
	 * <p></p>
	 * <b>ROW ADDITION</b>
	 * From the grid's number of columns and the linear index at which the change occurred, we get the index
	 * at which the row was added.
	 * <p>
	 * If the index is greater than the state's range and the viewport has all the needed rows, we make sure
	 * that all rows will re-use the already computed positions for layout and then immediately exit, returning the
	 * old state.
	 * <p>
	 * In any other case the computation begins. It is divided in two main phases:
	 * <p> - The first step is to copy all the valid rows, those who come before the index at which the row
	 * was added
	 * <p> - The second step is to update all rows that come after the change. Some will only require a partial
	 * update, only an index update. Others will need to be fully updated (both index and item) or even created if the
	 * viewport was not full.
	 * <p></p>
	 * <b>COLUMN ADDITION</b>
	 * Column addition is a quite expensive operation on a 2D structure like the grid since all the rows
	 * need to be updated.
	 * <p>
	 * For every row in the state we either call {@link LayoutRow#onColumnAdd(int)} or {@link LayoutRow#updateColumns(IntegerRange)}
	 * depending on the new range of needed columns, if it's the same as in the old state that
	 * the first is called, otherwise the latter is invoked.
	 * <p></p>
	 * <b>ROW REMOVAL</b>
	 * From the grid's number of columns and the linear index at which the change occurred we get the
	 * index at which the removal occurred. If it happened after the state's rows range that exits immediately and
	 * returns the old state.
	 * <p></p>
	 * The computation is divided in three main phases:
	 * <p> - The first step is to move all the valid rows to the new state, those rows that come before the index at which
	 * change occurred
	 * <p> - The second step is to update all the rows that come after that index, these need a partial update
	 * (only index), it's enough to call {@link LayoutRow#partialUpdate(int)}
	 * <p> - The final step is to update those rows that need a full update (both index and item). If there are remaining
	 * rows in the old state, these are reused by calling {@link LayoutRow#updateIndex(int)}, otherwise new rows are created
	 * <p></p>
	 * <b>COLUMN REMOVAL</b>
	 * Column removal is a quite expensive operation on a 2D structure like the grid since all the rows
	 * need to be updated.
	 * <p>
	 * From the grid's number of columns and the linear index at which the change occurred we get the index of the removed
	 * column with {@link GridUtils#indToCol(int, int)}, note that for the nColumns parameter it's important to use
	 * the old number of columns so {@code nColumns + 1}.
	 * <p>
	 * For each row in the old state, we call {@link LayoutRow#onColumnRemoval(int, IntegerRange)} and move it to the new state.
	 */
	public GridState<T, C> transition(ObservableGrid.Change<T> change) {
		int cellsNum = totalSize();
		GridState<T, C> state = this;

		switch (change.getType()) {
			case REPLACE_ELEMENT: {
				int nColumns = virtualGrid.getColumnsNum();
				Coordinate coordinate = GridUtils.indToSub(nColumns, change.getStart(), Coordinate::new);
				if (rows.containsKey(coordinate.getRow())) {
					T item = change.getAdded().get(0);
					LayoutRow<T, C> row = rows.get(coordinate.getRow());
					Optional.ofNullable(row.getCells().get(coordinate.getColumn())).ifPresent(c -> c.updateItem(item));
				}
				break;
			}
			case REPLACE_DIAGONAL: {
				Deque<T> diag = new ArrayDeque<>(change.getAdded());
				rows.values().forEach(row -> {
					T item = diag.poll();
					row.onDiagonalUpdate(item);
				});
				break;
			}
			case REPLACE_ROW: {
				int nColumns = virtualGrid.getColumnsNum();
				int row = GridUtils.indToRow(nColumns, change.getStart());
				if (rows.containsKey(row)) {
					rows.get(row).onRowReplacement();
				}
				break;
			}
			case REPLACE_COLUMN: {
				int nColumns = virtualGrid.getColumnsNum();
				int column = GridUtils.indToCol(nColumns, change.getStart());
				rows.values().forEach(row -> row.onColumnReplacement(column));
				break;
			}
			case ADD_ROW: {
				int nColumns = virtualGrid.getColumnsNum();
				int addedRow = GridUtils.indToRow(nColumns, change.getStart());
				if (addedRow > rowsRange.getMax() && rowsFilled()) {
					rows.values().forEach(row -> row.setReusablePositions(true));
					break;
				}

				GridHelper helper = virtualGrid.getGridHelper();
				IntegerRange range = helper.rowsRange();
				state = new GridState<>(virtualGrid, range, columnsRange);

				Set<Integer> available = IntegerRange.expandRangeToSet(range);

				// Valid rows
				for (int i = range.getMin(); i < addedRow; i++) {
					state.addRow(i, rows.remove(i));
					available.remove(i);
				}

				// Partial and full updates
				int from = Math.max(addedRow, range.getMin());
				int targetSize = range.diff() + 1;
				Deque<Integer> keys = new ArrayDeque<>(rows.keySet());
				while (!available.isEmpty() || state.size() != targetSize) {
					Integer key = keys.poll();
					if (key != null) {
						int newIndex = key + 1;
						if (IntegerRange.inRangeOf(newIndex, range) && newIndex != addedRow && available.contains(newIndex)) {
							LayoutRow<T, C> toUpdate = rows.remove(key);
							toUpdate.partialUpdate(newIndex);
							state.addRow(newIndex, toUpdate);
							available.remove(newIndex);
							continue;
						}
					}

					LayoutRow<T, C> row;
					if (key != null) {
						row = rows.remove(key);
						row.updateIndex(from);
					} else {
						row = LayoutRow.of(virtualGrid, from, columnsRange);
						row.init();
					}
					state.addRow(from, row);
					available.remove(from);
					from++;
				}
				break;
			}
			case ADD_COLUMN: {
				int nColumns = virtualGrid.getColumnsNum();
				int addedColumn = GridUtils.indToCol(nColumns, change.getStart());
				GridHelper helper = virtualGrid.getGridHelper();
				IntegerRange range = helper.columnsRange();
				state = new GridState<>(virtualGrid, rowsRange, columnsRange);

				Iterator<Map.Entry<Integer, LayoutRow<T, C>>> it = rows.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<Integer, LayoutRow<T, C>> next = it.next();
					Integer index = next.getKey();
					LayoutRow<T, C> row = next.getValue();
					if (columnsRange.equals(range)) {
						row.onColumnAdd(addedColumn);
					} else {
						row.updateColumns(range);
					}
					state.addRow(index, row);
					it.remove();
				}
				break;
			}
			case REMOVE_ROW: {
				int nColumns = virtualGrid.getColumnsNum();
				int removedRow = GridUtils.indToRow(nColumns, change.getStart());
				if (removedRow > rowsRange.getMax()) return this;

				GridHelper helper = virtualGrid.getGridHelper();
				IntegerRange range = helper.rowsRange();
				state = new GridState<>(virtualGrid, range, columnsRange);

				Set<Integer> available = IntegerRange.expandRangeToSet(range);

				// Valid
				for (int i = range.getMin(); i < removedRow; i++) {
					state.addRow(i, rows.remove(i));
					available.remove(i);
				}

				// Partial
				for (int i = removedRow + 1; i <= range.getMax(); i++) {
					int newIndex = i - 1;
					LayoutRow<T, C> row = rows.remove(i);
					row.partialUpdate(newIndex);
					state.addRow(newIndex, row);
					available.remove(newIndex);
				}

				// Full
				Deque<Integer> remaining = new ArrayDeque<>(rows.keySet());
				for (Integer index : available) {
					Integer rIndex = remaining.poll();
					if (rIndex != null) {
						LayoutRow<T, C> toUpdate = rows.remove(rIndex);
						toUpdate.updateIndex(index);
						state.addRow(index, toUpdate);
						continue;
					}

					LayoutRow<T, C> row = LayoutRow.of(virtualGrid, index, columnsRange);
					row.init();
					state.addRow(index, row);
				}
				break;
			}
			case REMOVE_COLUMN: {
				int nColumns = virtualGrid.getColumnsNum();
				int removedColumn = GridUtils.indToCol(nColumns + 1, change.getStart());

				GridHelper helper = virtualGrid.getGridHelper();
				IntegerRange range = helper.columnsRange();
				state = new GridState<>(virtualGrid, rowsRange, range);

				Iterator<LayoutRow<T, C>> it = rows.values().iterator();
				while (it.hasNext()) {
					LayoutRow<T, C> row = it.next();
					row.onColumnRemoval(removedColumn, range);
					state.addRow(row.getIndex(), row);
					it.remove();
				}
				break;
			}
		}

		state.type = UpdateType.CHANGE;
		state.setCellsChanged(state.totalSize() != cellsNum);
		change.endChange();
		return state;
	}

	/**
	 * This is one of the main methods responsible for positioning and resizing the cells.
	 * <p>
	 * If the state is empty exits immediately.
	 * <p></p>
	 * Before starting the computation we get a series of useful parameters such as:
	 * <p> - The size of the cells, {@link VirtualGrid#getCellSize()}
	 * <p> - The grid's number of rows and columns
	 * <p> - The first visible row and column indexes
	 * <p> - The last visible row and column indexes
	 * <p> - Two special flags to check if rows or columns need to be positioned is a special way
	 * (more on that below)
	 * <p></p>
	 * {@code GridState} is only responsible for computing the position of the rows, the cells are actually
	 * laid out by the {@link LayoutRow} objects. In fact, here we start from the vertical bottom position,
	 * which is given by {@code rowsRange.diff() * cellHeight}, and then iterating over the grids
	 * from the end to the start (reverse, with a {@link ListIterator}) we call {@link LayoutRow#layoutCells(double, boolean)}
	 * by passing the computed row position and the special flag for the columns.
	 * <p></p>
	 * About the special flags.
	 * <p>
	 * Long story short. The way the {@link GridHelper} works is that the viewport cannot "scroll" beyond
	 * {@link VirtualGrid#getCellSize()} and the virtual grid always has one row and one column of overscan/buffer
	 * whatever you want to call it.
	 * This means that when you reach the end of the viewport you always have that one row and column of overscan that needs
	 * to be positioned above/before all the others, because of course you cannot add anything more at the end. This issue is
	 * rather problematic and lead to this solution. These special flags are needed to check if we are dealing with
	 * such special cases and the positions are shifted.
	 * <p>
	 * For rows the bottom position is shifted as {@code bottom -= cellHeight}.
	 * <p>
	 * For columns the right position is shifted as {@code right -= cellWidth}.
	 */
	public void layoutRows() {
		if (isEmpty()) return;

		GridHelper helper = virtualGrid.getGridHelper();
		SizeBean size = virtualGrid.getCellSize();
		int gRows = virtualGrid.getRowsNum(); // Grid Rows
		int gColumns = virtualGrid.getColumnsNum(); // Grid Columns

		int firstRow = helper.firstRow();
		int lastRow = firstRow + helper.maxRows() - 1;
		boolean adjustRows = lastRow > gRows - 1 && rowsFilled();

		int firstColumn = helper.firstColumn();
		int lastColumn = firstColumn + helper.maxColumns() - 1;
		boolean adjustColumns = lastColumn > gColumns - 1 && columnsFilled();

		double bottom = rowsRange.diff() * size.getHeight();
		if (adjustRows) bottom -= size.getHeight();

		List<LayoutRow<T, C>> tmp = new ArrayList<>(rows.values());
		ListIterator<LayoutRow<T, C>> it = tmp.listIterator(tmp.size());
		while (it.hasPrevious()) {
			LayoutRow<T, C> row = it.previous();
			row.layoutCells((row.canReusePositions() ? row.getRowPos() : bottom), adjustColumns);
			bottom -= size.getHeight();
		}
	}

	/**
	 * Adds a new {@link LayoutRow} for the given index in the rows map. Note that
	 * this new row will need to be initialized with {@link LayoutRow#init()}.
	 */
	protected void addRow(int index) {
		rows.put(index, LayoutRow.of(virtualGrid, index, columnsRange));
	}

	/**
	 * Adds the given {@link LayoutRow} at the given index in the rows map.
	 */
	protected void addRow(int index, LayoutRow<T, C> row) {
		rows.put(index, row);
	}

	/**
	 * Disposes all the rows in the state, also removes them from the map.
	 */
	protected void disposeAndClear() {
		Iterator<Map.Entry<Integer, LayoutRow<T, C>>> it = rows.entrySet().iterator();
		while (it.hasNext()) {
			LayoutRow<T, C> row = it.next().getValue();
			row.clear();
			it.remove();
		}
	}

	/**
	 * @return whether the rows map is empty
	 */
	public boolean isEmpty() {
		return rows.isEmpty();
	}

	/**
	 * @return the number of rows in this state
	 */
	public int size() {
		return rows.size();
	}

	/**
	 * @return the number of cells as the sum of the cells of every row in
	 * this state
	 */
	public int totalSize() {
		return rows.values().stream()
				.mapToInt(LayoutRow::size)
				.sum();
	}

	/**
	 * @return whether there are enough rows is this state to fill the viewport
	 */
	public boolean rowsFilled() {
		GridHelper helper = virtualGrid.getGridHelper();
		int targetRows = helper.maxRows();
		return size() >= targetRows;
	}

	/**
	 * @return whether there are enough columns in this state to fill the viewport
	 */
	public boolean columnsFilled() {
		GridHelper helper = virtualGrid.getGridHelper();
		int targetColumns = helper.maxColumns();
		return rows.values().stream()
				.allMatch(row -> row.size() >= targetColumns);
	}

	/**
	 * @return whether both {@link #rowsFilled()} and {@link #columnsFilled()} are true
	 */
	public boolean isViewportFull() {
		return rowsFilled() && columnsFilled();
	}

	//================================================================================
	// Getters Setters
	//================================================================================

	/**
	 * @return the {@link VirtualGrid} instance associated to this state
	 */
	public VirtualGrid<T, C> getVirtualGrid() {
		return virtualGrid;
	}

	/**
	 * @return the range of rows the state should display
	 */
	public IntegerRange getRowsRange() {
		return rowsRange;
	}

	/**
	 * @return the range of columns the state should display
	 */
	public IntegerRange getColumnsRange() {
		return columnsRange;
	}

	/**
	 * @return the rows map
	 */
	protected Map<Integer, LayoutRow<T, C>> getRows() {
		return rows;
	}

	/**
	 * @return the rows map an unmodifiable collection
	 */
	public Map<Integer, LayoutRow<T, C>> getRowsUnmodifiable() {
		return Collections.unmodifiableMap(rows);
	}

	/**
	 * @return a map containing all the cells of the state with their linear index as keys
	 */
	public Map<Integer, C> getIndexedCells() {
		return rows.values().stream()
				.flatMap(row -> row.getLinearMap().entrySet().stream())
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue
				));
	}

	/**
	 * @return a list containing all the cells of the state
	 */
	public List<C> getCells() {
		return rows.values().stream()
				.flatMap(row -> row.getCells().values().stream())
				.collect(Collectors.toList());
	}

	/**
	 * @return a list of all the nodes, converting the cells to nodes with {@link Cell#getNode()}
	 */
	public List<Node> getNodes() {
		return rows.values().stream()
				.flatMap(row -> row.getCells().values().stream())
				.map(C::getNode)
				.collect(Collectors.toList());
	}

	/**
	 * @return the number of rows the state should have
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
	 * @return whether new cells were created or some removed
	 */
	public boolean haveCellsChanged() {
		return cellsChanged;
	}

	protected void setCellsChanged(boolean cellsChanged) {
		this.cellsChanged = cellsChanged;
	}

}
