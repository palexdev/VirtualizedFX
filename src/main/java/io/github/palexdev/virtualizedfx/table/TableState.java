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
import io.github.palexdev.mfxcore.utils.fx.ListChangeHelper;
import io.github.palexdev.mfxcore.utils.fx.ListChangeHelper.Change;
import io.github.palexdev.mfxcore.utils.fx.ListChangeHelper.ChangeType;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.enums.UpdateType;
import io.github.palexdev.virtualizedfx.table.paginated.PaginatedVirtualTable;
import javafx.scene.layout.Region;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class used by the {@link ViewportManager} to represent the state of the viewport at a given time.
 * <p>
 * The idea is to have an immutable state so that each state is a different object, with some exceptional cases
 * when the state doesn't need to be re-computed, so the old object is returned.
 * <p></p>
 * This offers information such as:
 * <p> - The range of rows contained in the state, {@link #getRowsRange()}
 * <p> - The range of columns contained by each {@link TableRow} in the state, {@link #getColumnsRange()}
 * <p> - The cells in the viewport. These are stored in {@link TableRow}s, each of these has a cell for each column.
 * {@link TableRow}s are kept in a map: rowIndex -> tableRow
 * <p> - The expected number of rows, {@link #getTargetSize()}. Note that this is computed by {@link TableHelper#maxRows()},
 * so the result may be greater than the number of items available in the data structure
 * <p> - The type of event that lead the old state to transition to the new one, see {@link UpdateType}
 * <p> - A flag to check if new rows were created or some were deleted, {@link #haveRowsChanged()}.
 * This is used by {@link VirtualTableSkin} since we want to update the rContainer's children only when the rows change.
 * <p> - A flag specifically for the {@link PaginatedVirtualTable} to indicate whether the state also contains some
 * rows that are hidden in the viewport, {@link #anyHidden()}
 * <p></p>
 * This also contains a particular global state, {@link #EMPTY}, typically used to indicate that the viewport
 * is empty, and no state can be created.
 */
public class TableState<T> {
	//================================================================================
	// Static Members
	//================================================================================
	public static final TableState EMPTY = new TableState();

	//================================================================================
	// Properties
	//================================================================================
	private final VirtualTable<T> table;
	private final IntegerRange rowsRange;
	private final IntegerRange columnsRange;
	private final Map<Integer, TableRow<T>> rows = new TreeMap<>();
	private final int targetSize;
	private UpdateType type = UpdateType.INIT;
	private boolean rowsChanged;
	private Boolean hidden = null;

	//================================================================================
	// Constructors
	//================================================================================
	private TableState() {
		this.table = null;
		this.rowsRange = IntegerRange.of(-1);
		this.columnsRange = IntegerRange.of(-1);
		this.targetSize = 0;
	}

	public TableState(VirtualTable<T> table, IntegerRange rowsRange, IntegerRange columnsRange) {
		this.table = table;
		this.rowsRange = rowsRange;
		this.columnsRange = columnsRange;
		this.targetSize = table.getTableHelper().maxRows();
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
	 * @return a new {@code TableState} which is the result of transitioning from this state to
	 * a new one given the new ranges for rows and columns
	 */
	protected TableState<T> init(IntegerRange rowsRange, IntegerRange columnsRange) {
		if (this.rowsRange.equals(rowsRange) && this.columnsRange.equals(columnsRange)) return this;

		TableState<T> newState = new TableState<>(table, rowsRange, columnsRange);
		Set<Integer> range = IntegerRange.expandRangeToSet(rowsRange);
		int rowsNum = size();
		int targetSize = rowsRange.diff() + 1;

		for (Integer rowIndex : rowsRange) {
			TableRow<T> row = rows.remove(rowIndex);
			if (row != null) {
				row.updateColumns(columnsRange);
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
				TableRow<T> row = rows.remove(oIndex);
				row.updateFull(rIndex);
				newState.addRow(rIndex, row);
			} else {
				newState.addRow(rIndex);
			}
		}

		if (newState.size() != rowsNum) newState.rowsChanged();
		clear();
		return newState;
	}

	/**
	 * This is responsible for transitioning to a new state when the viewport scrolls vertically.
	 * <p>
	 * Used by {@link ViewportManager#onVScroll()}.
	 */
	protected TableState<T> vScroll(IntegerRange rowsRange) {
		if (this.rowsRange.equals(rowsRange)) return this;

		TableState<T> newState = new TableState<>(table, rowsRange, columnsRange);
		newState.type = UpdateType.SCROLL;
		Set<Integer> range = IntegerRange.expandRangeToSet(rowsRange);

		for (Integer rowIndex : rowsRange) {
			TableRow<T> row = rows.remove(rowIndex);
			if (row != null) {
				newState.addRow(rowIndex, row);
				range.remove(rowIndex);
			}
		}

		Deque<Integer> reusable = new ArrayDeque<>(rows.keySet());
		Deque<Integer> remaining = new ArrayDeque<>(range);
		while (!remaining.isEmpty()) {
			int rIndex = remaining.removeFirst();
			Integer oIndex = reusable.poll();
			if (oIndex == null) {
				// This is a strange situation that may occur in some specific occasions...
				// The reality is that such case should not even happen, as a solution, request layout to the viewport
				table.requestViewportLayout();
				continue;
			}
			TableRow<T> row = rows.remove(oIndex);
			row.updateFull(rIndex);
			newState.addRow(rIndex, row);
		}

		return newState;
	}

	/**
	 * This is responsible for transitioning to a new state when the viewport scrolls horizontally.
	 * <p>
	 * Used by {@link ViewportManager#onHScroll()}.
	 */
	protected TableState<T> hScroll(IntegerRange columnsRange) {
		if (this.columnsRange.equals(columnsRange)) return this;

		TableState<T> newState = new TableState<>(table, rowsRange, columnsRange);
		newState.type = UpdateType.SCROLL;
		Iterator<Map.Entry<Integer, TableRow<T>>> it = rows.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, TableRow<T>> next = it.next();
			Integer index = next.getKey();
			TableRow<T> row = next.getValue();
			row.updateColumns(columnsRange);
			newState.addRow(index, row);
			it.remove();
		}
		return newState;
	}

	/**
	 * This is responsible for transitioning this state to a new one given a series of changes occurred
	 * in the items list.
	 * <p>
	 * Note that this is made to work with {@link ListChangeHelper} and {@link Change}.
	 * <p>
	 * Why? Tl;dr: For performance reasons.
	 */
	protected TableState<T> change(List<Change> changes) {
		TableState<T> newState = this;
		for (Change change : changes) {
			newState = newState.processChange(change);
		}
		return newState;
	}

	/**
	 * This is responsible for processing a single {@link Change} bean and produce a new state
	 * according to the change's {@link ChangeType}.
	 * <p></p>
	 * In the following lines I'm going to document each type.
	 * <p></p>
	 * <b>PERMUTATION</b>
	 * The permutation case while not being the most complicated can still be considered a bit heavy to compute
	 * since cells keep their index but all their items must be updated, so the performance is totally dependent
	 * on the cell's {@link TableCell#updateItem(Object)} implementation.
	 * <p></p>
	 * <b>REPLACE</b>
	 * The algorithm for replacements is quite complex because of the shitty JavaFX apis. A replacement is also
	 * considered a removal immediately followed by an addition and this leads to some serious issues. For example
	 * there is no easy way to distinguish between a simple replace and a "setAll()" (which also clears the list).
	 * <p>
	 * That being said the first thing to check is whether the change occurred before the last displayed rows,
	 * otherwise we simply ignore it and return the old state.
	 * <p>
	 * Among the current displayed cells, those who have not been changed are simply moved to the new state.
	 * The rest of the rows are updated (if there are still rows to be reused) or created.
	 * <p>
	 * At the end we ensure that the old state is empty by clearing and disposing the remaining rows.
	 * <p></p>
	 * <b>ADDITION</b>
	 * The computation in case of added items to the list is complex and a bit heavy on performance.
	 * There are several things to consider, and it was hard to find a generic algorithm that would correctly
	 * compute the new state in all possible situations, included exceptional cases and for {@link PaginatedVirtualTable}
	 * too.
	 * <p></p>
	 * The simplest of these cases is when changes occur after the displayed range of rows
	 * and there are already enough rows to fill the viewport, the old state is returned.
	 * In all the other cases the computation for the new state can begin.
	 * <p>
	 * The first step is to get a series of information such as:
	 * <p> - the index of the first row to display, {@link TableHelper#firstRow()}
	 * <p> - the index of the last row to display, {@link TableHelper#lastRow()}
	 * <p>
	 * At this point the computation begins. The algorithm makes a distinction between the rows:
	 * some are <b>valid</b> and can be moved to the new state; some others are <b>partially valid</b>
	 * meaning that they can be reused as the item is the same but the index has changed; the remaining ones
	 * are <b>invalid</b>, meaning that they need to be updated both for the item and the index
	 * <p>
	 * A Set keeps track of the available rows, the ones that will be reused/updated.
	 * <p> - Valid rows are moved to the new state and removed from the Set
	 * <p> - Partially valid rows are first removed from the current state, then after their new index
	 * has been computed as the {@code oldIndex + change.size() (number of added items)}, they are updated
	 * with {@link TableRow#updateIndex(int)} and finally copied to the new state
	 * <p> - Invalid rows are the ones whose index is not included in the new "rowsRange". We have two cases here:
	 * <p> 1) The row removed from the current state is not null, so we can reuse it, it is updated with {@link TableRow#updateFull(int)},
	 * then moved to the new state
	 * <p> 2) The row removed from the current state is null, a new row is created and added to the new state with
	 * {@link #addRow(int)}
	 * <p></p>
	 * <b>REMOVAL</b>
	 * The computation in case of removed items from the list is complex and a bit heavy on performance. There are
	 * several things to consider, and it was hard to find a generic algorithm that would correctly compute the new state
	 * in all possible situations, included exceptional cases.
	 * <p></p>
	 * The simplest of these cases is when changes occur after the displayed range of rows, the old state will be
	 * returned. In all the other cases the computation for the new state can begin.
	 * <p>
	 * The first step is to separate those cells that only require a partial update (only index) from the others.
	 * First we get a Set of indexes from the state's range using {@link IntegerRange#expandRangeToSet(IntegerRange)},
	 * then we remove from the Set all the indexes at which the removal occurred. Before looping on these indexes we
	 * also convert the {@link Change#getIndexes()} to a sorted List.
	 * <p>
	 * In the loop we extract the row at index "i" and to update its index we must first compute the shift. We do this
	 * by using binary search, {@link Collections#binarySearch(List, Object)}.
	 * The new index will be: {@code int newIndex = index - findShift(list, index)}, see {@link #findShift(List, int)}.
	 * <p>
	 * If the new index is below the range min the update is skipped and the loop goes to the next index, otherwise,
	 * the index is updated and the row moved to the new state.
	 * <p>
	 * The next step is to update those cells which need a full update (both item and index). First we compute their
	 * indexes by expanding the new state's range to a Set and then removing from it all the rows that have already been
	 * added to the new state (which means they have been updated already, also keep in mind that we always operate on indexes
	 * so newState.rows.keySet()).
	 * <p>
	 * Now a {@link Deque} is built on the remaining cells in the old state (cells.keySet() again) and the update can begin.
	 * We loop over the previous built Set of indexes:
	 * <p> 1) We get one of the indexes from the deque as {@link Deque#removeFirst()}
	 * <p> 3) We remove the row at that index from the old state
	 * <p> 4) We update the row with {@link TableRow#updateFull(int)} using the loop "i"
	 * <p> 5) The row is added to the new state
	 * <p> 6) We ensure that no rows are left in the old state
	 * <p></p>
	 * At the end we change the new state's type to {@link UpdateType#CHANGE} and check if the number of rows
	 * has changed (in such case {@link #rowsChanged()} is called) and then return the new state.
	 */
	protected TableState<T> processChange(Change change) {
		TableHelper helper = table.getTableHelper();
		TableState<T> state = this;
		int rowsNum = rows.size();

		switch (change.getType()) {
			case PERMUTATION: {
				rows.values().forEach(TableRow::updateItem);
				break;
			}
			case REPLACE: {
				IntegerRange rowsRange = helper.rowsRange();
				if (change.getFrom() > rowsRange.getMax()) break;

				state = new TableState<>(table, rowsRange, columnsRange);

				Deque<Integer> available = new ArrayDeque<>(rows.keySet());
				for (Integer i : rowsRange) {
					if (!change.hasChanged(i)) {
						state.addRow(i, rows.remove(i));
						available.remove(i);
						continue;
					}

					Integer index = available.poll();
					if (index != null) {
						TableRow<T> row = rows.remove(index);
						row.updateFull(i); // TODO in theory this could also just be updateItem()
						state.addRow(i, row);
					} else {
						state.addRow(i);
						state.rowsChanged();
					}
				}

				clear();
				break;
			}
			case ADD: {
				boolean rowsFilled = rowsFilled();
				if (rowsFilled && change.getFrom() > rowsRange.getMax()) break;

				// Pre Computation
				int first = helper.firstRow();
				int last = helper.lastRow();
				IntegerRange rowsRange = IntegerRange.of(first, last);
				state = new TableState<>(table, rowsRange, columnsRange);

				// Valid/Semi-Valid/Invalid computation
				// Old indexes are "mapped" to what they should be in the new state.
				// Valid ones remain the same, rows are moved to the new state
				// Partials (item unchanged, index changed) are shifted by updating the row index,
				// then moved to the new state
				// Invalids (item changed) are fully updated to represent the new needed items
				// then moved to the new state
				// A Set keeps track of which indexes have been processed
				Set<Integer> available = IntegerRange.expandRangeToSet(rowsRange);
				Set<Integer> processed = new HashSet<>();
				for (int i = first; i < change.getFrom(); i++) {
					state.addRow(i, rows.remove(i));
					available.remove(i);
					processed.add(i);
				}

				int from = Math.max(change.getFrom(), first);
				int targetSize = computeTargetSize(state.targetSize);
				int lastValid = -1;
				Deque<Integer> keys = getKeysDequeue();
				while (!available.isEmpty() || processed.size() != targetSize) {
					Integer index = keys.poll();
					if (processed.contains(index)) continue;

					if (index != null) {
						int newIndex = index + change.size();
						if (IntegerRange.inRangeOf(newIndex, rowsRange) && !change.hasChanged(newIndex) && available.contains(newIndex)) {
							TableRow<T> row = rows.remove(index);
							row.updateIndex(newIndex);
							state.addRow(newIndex, row);
							available.remove(newIndex);
							processed.add(index);
							continue;
						}
					}

					int fIndex;
					if (index == null) {
						fIndex = lastValid;
						lastValid--;
					} else {
						fIndex = index;
					}

					TableRow<T> row = rows.remove(fIndex);
					if (row != null) {
						row.updateFull(from);
						state.addRow(from, row);
					} else {
						state.addRow(from);
						state.rowsChanged();
					}
					available.remove(from);
					processed.add(fIndex);
					from++;
				}

				if (table instanceof PaginatedVirtualTable) {
					// Ensure that remaining rows in the old state are carried by the new state too
					state.addRows(rows);
					rows.clear();
				}

				break;
			}
			case REMOVE: {
				if (change.getFrom() > rowsRange.getMax()) break;

				int max = Math.min(rowsRange.getMin() + targetSize - 1, table.getItems().size() - 1);
				int min = Math.max(0, max - targetSize + 1);
				IntegerRange rowsRange = IntegerRange.of(min, max);
				state = new TableState<>(table, rowsRange, columnsRange);

				Set<Integer> pUpdate = IntegerRange.expandRangeToSet(this.rowsRange);
				pUpdate.removeAll(change.getIndexes());

				List<Integer> changeIndexes = change.getIndexes().stream()
						.sorted()
						.collect(Collectors.toList());
				for (Integer index : pUpdate) {
					int newIndex = index - findShift(changeIndexes, index);
					if (newIndex < this.rowsRange.getMin()) continue;
					TableRow<T> row = rows.remove(index);
					row.updateIndex(newIndex);
					state.addRow(newIndex, row);
				}

				Set<Integer> fUpdate = IntegerRange.expandRangeToSet(rowsRange);
				fUpdate.removeAll(state.rows.keySet());

				Deque<Integer> available = new ArrayDeque<>(rows.keySet());
				for (Integer index : fUpdate) {
					int rowIndex = available.removeFirst();
					TableRow<T> row = rows.remove(rowIndex);
					row.updateFull(index);
					state.addRow(index, row);
				}
				clear();
			}
		}

		state.type = UpdateType.CHANGE;
		if (state.size() != rowsNum) state.rowsChanged();
		return state;
	}

	/**
	 * Given a certain column, this method will find its index with {@link VirtualTable#getColumnIndex(TableColumn)},
	 * and, if the index is in the current {@link #getColumnsRange()}, will tell all the rows in the state to update
	 * the cell at the given index with {@link TableRow#updateColumnFactory(int)}.
	 * <p>
	 * Two side notes:
	 * <p> 1) Since the factory changed for the given column, all cells cached for that column in {@link TableCache} are
	 * invalid, so {@link TableCache#clear(TableColumn)} must also be called
	 * <p> 2) This will produce a new state object
	 *
	 * @param column the column for which the cell factory changed
	 */
	protected TableState<T> columnChangedFactory(TableColumn<T, ? extends TableCell<T>> column) {
		if (isEmpty()) return this;

		int cIndex = table.getColumnIndex(column);
		if (!IntegerRange.inRangeOf(cIndex, columnsRange)) return this;

		// Make sure that there aren't any cached cell from the previous factory
		TableCache<T> cache = table.getTableCache();
		cache.clear(column);

		TableState<T> newState = new TableState<>(table, rowsRange, columnsRange);
		Iterator<Map.Entry<Integer, TableRow<T>>> it = rows.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, TableRow<T>> next = it.next();
			Integer index = next.getKey();
			TableRow<T> row = next.getValue();
			row.updateColumnFactory(cIndex);
			newState.addRow(index, row);
			it.remove();
		}
		return newState;
	}

	/**
	 * Given an ordered list of indexes and the index to find, returns
	 * the index at which resides. If the index is not present, returns
	 * the index at which it would be located.
	 *
	 * @see Collections#binarySearch(List, Object)
	 */
	protected int findShift(List<Integer> indexes, int index) {
		int shift = Collections.binarySearch(indexes, index);
		return shift > -1 ? shift : -(shift + 1);
	}

	/**
	 * Creates a new {@link TableRow} using {@link VirtualTable#rowFactoryProperty()}, then
	 * adds it to the state at the given index.
	 */
	protected void addRow(int index) {
		BiFunction<Integer, IntegerRange, TableRow<T>> factory = table.getRowFactory();
		rows.put(index, factory.apply(index, columnsRange).init());
	}

	/**
	 * Adds an already built {@link TableRow} to the state at the given index.
	 */
	protected void addRow(int index, TableRow<T> row) {
		rows.put(index, row);
	}

	/**
	 * Adds all the rows contained in the given map to the state.
	 */
	protected void addRows(Map<Integer, TableRow<T>> rows) {
		this.rows.putAll(rows);
	}

	/**
	 * @return converts the cells keySet to a {@link Deque}.
	 */
	protected Deque<Integer> getKeysDequeue() {
		if (table instanceof PaginatedVirtualTable && anyHidden()) {
			Deque<Integer> deque = new ArrayDeque<>();
			Set<Integer> keys = new LinkedHashSet<>(rows.keySet());
			Iterator<Integer> it = keys.iterator();
			while (it.hasNext()) {
				Integer index = it.next();
				TableRow<T> row = rows.get(index);
				if (!row.isVisible()) continue;
				deque.add(index);
				it.remove();
			}
			deque.addAll(keys);
			return deque;
		}
		return new ArrayDeque<>(rows.keySet());
	}

	/**
	 * @return the expected number of items of the state
	 */
	protected int computeTargetSize(int expectedSize) {
		if (table instanceof PaginatedVirtualTable) {
			PaginatedVirtualTable<T> pTable = (PaginatedVirtualTable<T>) table;
			if (pTable.getCurrentPage() == pTable.getMaxPage()) {
				int remainder = pTable.getItems().size() % pTable.getRowsPerPage();
				return (remainder != 0) ? remainder : expectedSize;
			}
		}
		return Math.min(table.getItems().size(), expectedSize);
	}

	/**
	 * Shortcut to dispose all rows present in this state's cells map and then clear it.
	 *
	 * @see TableRow#clear()
	 */
	protected void clear() {
		rows.values().forEach(TableRow::clear);
		rows.clear();
	}

	/**
	 * Checks if {@link #size()} is greater or equal to the expected {@link #getTargetSize()},
	 * in other words if there are enough rows to fill the viewport vertically.
	 */
	public boolean rowsFilled() {
		if (table instanceof PaginatedVirtualTable) {
			return rows.values().stream()
					.allMatch(TableRow::isVisible);
		}
		return size() >= targetSize;
	}

	/**
	 * Checks whether there are enough columns in this state to fill the viewport horizontally.
	 */
	public boolean columnsFilled() {
		TableHelper helper = table.getTableHelper();
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
	 * @return the total number of cells in this state, as the sum of all the cells of each individual {@link TableRow}
	 */
	public int totalSize() {
		return rows.values().stream()
				.mapToInt(TableRow::size)
				.sum();
	}

	/**
	 * @return whether the state is empty, no rows in it
	 */
	public boolean isEmpty() {
		return rows.isEmpty();
	}

	public boolean anyHidden() {
		if (hidden == null) {
			TableHelper helper = table.getTableHelper();
			int firstRow = helper.firstRow();
			int lastRow = Math.min(firstRow + helper.maxRows() - 1, table.getItems().size() - 1);
			IntegerRange range = IntegerRange.of(firstRow, lastRow);
			hidden = rows.keySet().stream()
					.anyMatch(i -> !IntegerRange.inRangeOf(i, range));
		}
		return hidden;
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

	/**
	 * @return the {@link VirtualTable} instance this state is referring to
	 */
	public VirtualTable<T> getTable() {
		return table;
	}

	/**
	 * @return the rows map
	 */
	protected Map<Integer, TableRow<T>> getRows() {
		return rows;
	}

	/**
	 * @return the rows as an unmodifiable map
	 */
	public Map<Integer, TableRow<T>> getRowsUnmodifiable() {
		return Collections.unmodifiableMap(rows);
	}

	/**
	 * Converts the columns to a list or {@code Regions} by iterating over the
	 * {@link #getColumnsRange()} and using {@link VirtualTable#getColumn(int)}.
	 */
	public List<Region> getColumnsAsNodes() {
		if (IntegerRange.of(-1).equals(columnsRange)) return List.of();
		return IntStream.rangeClosed(columnsRange.getMin(), columnsRange.getMax())
				.mapToObj(table::getColumn)
				.map(TableColumn::getRegion)
				.collect(Collectors.toList());
	}

	/**
	 * @return the range of rows in the state
	 */
	public IntegerRange getRowsRange() {
		return rowsRange;
	}

	/**
	 * @return the range of columns of each {@link TableRow} in the state
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
	 * @return whether a change caused the number of rows of the new state to change compared
	 * to the old state
	 */
	public boolean haveRowsChanged() {
		return rowsChanged;
	}

	/**
	 * Sets the rowsChanged flag to true, causing {@link #haveRowsChanged()} to return true, which will tell the
	 * rContainer to update its children.
	 *
	 * @see VirtualTableSkin
	 */
	protected void rowsChanged() {
		this.rowsChanged = true;
	}
}
