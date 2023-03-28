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
import io.github.palexdev.mfxcore.collections.ObservableGrid;
import io.github.palexdev.mfxcore.enums.GridChangeType;
import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import javafx.scene.Node;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This is a helper class to support {@link GridState}.
 * <p>
 * The major issue in managing {@link VirtualGrid} is that we are dealing with a 2D data structure, and a type of cell
 * {@link GridCell}, that works on both linear indexes and coordinated. All of this makes the algorithms a lot more complex
 * and easy to screw up.
 * <p></p>
 * {@code GridRow}'s goal is to simplify this mess by having one specific task: managing the columns for one specific row
 * of items. So, in the end with {@code GridRow} we have to manage a 1D structure which is the map containing the cells
 * representing the columns of a single row.
 * <p></p>
 * {@code GridRow}s contain information such as:
 * <p> - the row's index
 * <p> - the range of columns for the row
 * <p> - the actual cells that will be displayed in the viewport, kept in a map as: columnIndex -> Cell
 * <p> - a map containing the horizontal positions of the cells, so that in some cases these can be reused avoiding
 * a part of the {@link #layoutCells(double, boolean)} computation
 * <p> - the vertical position at which the row (every cell) will be positioned
 */
public class GridRow<T, C extends GridCell<T>> {
	//================================================================================
	// Properties
	//================================================================================
	private final VirtualGrid<T, C> grid;
	private int index;
	private IntegerRange columns;
	private final Map<Integer, C> cells = new TreeMap<>();
	private final Set<Double> positions = new TreeSet<>();
	private double position;
	private boolean reusablePositions = false;
	private boolean visible = true;

	//================================================================================
	// Constructors
	//================================================================================
	public GridRow(VirtualGrid<T, C> grid, int index, IntegerRange columns) {
		this.grid = grid;
		this.index = index;
		this.columns = columns;
	}

	public static <T, C extends GridCell<T>> GridRow<T, C> of(VirtualGrid<T, C> virtualGrid, int index, IntegerRange columns) {
		return new GridRow<>(virtualGrid, index, columns);
	}

	//================================================================================
	// Methods
	//================================================================================

	// Init

	/**
	 * Initializes {@code GridRow} by creating the needed amount of cells given by the {@link #getColumns()} range.
	 * <p>
	 * If the row has already been initialized before all the cells are disposed, cleared and re-created.
	 */
	public GridRow<T, C> init() {
		if (index < 0 || IntegerRange.of(-1).equals(columns)) return this;

		clear();
		for (Integer column : columns) {
			int linear = toLinear(index, column);
			T item = grid.getItems().getElement(linear);
			C cell = grid.getCellFactory().apply(item);
			cell.updateIndex(linear);
			cell.updateCoordinates(index, column);
			cells.put(column, cell);
		}
		return this;
	}

	/**
	 * This is responsible for supplying of removing cells according to the new given columns range.
	 * <p>
	 * This is used by {@link GridManager#init()}. If the given range is equal to the current one no operation
	 * is done.
	 */
	protected void onInit(IntegerRange columns) {
		if (this.columns.equals(columns)) return;

		Map<Integer, C> tmp = new HashMap<>();
		Set<Integer> range = IntegerRange.expandRangeToSet(columns);
		int targetSize = columns.diff() + 1;

		for (Integer column : columns) {
			if (cells.containsKey(column)) {
				tmp.put(column, cells.remove(column));
				range.remove(column);
			}
		}

		Deque<Integer> reusable = new ArrayDeque<>(cells.keySet());
		Deque<Integer> remaining = new ArrayDeque<>(range);
		while (tmp.size() != targetSize) {
			int rIndex = remaining.removeFirst();
			Integer oIndex = reusable.poll();

			int linear = toLinear(index, rIndex);
			T item = grid.getItems().getElement(linear);
			C cell;
			if (oIndex != null) {
				cell = cells.remove(oIndex);
				cell.updateItem(item);
			} else {
				cell = grid.getCellFactory().apply(item);
			}
			cell.updateIndex(linear);
			cell.updateCoordinates(index, rIndex);
			tmp.put(rIndex, cell);
		}

		clear();
		cells.putAll(tmp);
		this.columns = columns;
	}

	// Update & Scroll

	/**
	 * This is responsible for updating the cells when the row's index must change to the given one.
	 * All cells are fully updated (both index and item).
	 */
	protected void updateIndex(int index) {
		if (this.index == index) return;

		for (Map.Entry<Integer, C> e : cells.entrySet()) {
			int column = e.getKey();
			int linear = toLinear(index, column);
			C cell = e.getValue();
			T item = grid.getItems().getElement(linear);
			cell.updateItem(item);
			cell.updateIndex(linear);
			cell.updateCoordinates(index, column);
		}
		this.index = index;
		reusablePositions = true;
	}

	/**
	 * This is responsible for updating the {@code GridRow} when the viewport scrolls horizontally.
	 */
	protected void onScroll(IntegerRange columns) {
		Map<Integer, C> tmp = new HashMap<>();
		Set<Integer> range = IntegerRange.expandRangeToSet(columns);
		int targetSize = columns.diff() + 1;

		for (Integer column : columns) {
			if (cells.containsKey(column)) {
				int linear = toLinear(index, column);
				C cell = cells.remove(column);
				cell.updateIndex(linear);
				cell.updateCoordinates(index, column);
				tmp.put(column, cell);
				range.remove(column);
			}
		}

		Deque<Integer> reusable = new ArrayDeque<>(cells.keySet());
		Deque<Integer> remaining = new ArrayDeque<>(range);
		while (tmp.size() != targetSize) {
			int rIndex = remaining.removeFirst();
			int oIndex = reusable.removeFirst();
			int linear = toLinear(index, rIndex);
			T item = grid.getItems().getElement(linear);
			C cell = cells.remove(oIndex);
			cell.updateItem(item);
			cell.updateIndex(linear);
			cell.updateCoordinates(index, rIndex);
			tmp.put(rIndex, cell);
		}

		clear();
		cells.putAll(tmp);
		this.columns = columns;
	}

	// Changes

	/**
	 * This is responsible for updating the {@code GridRow} when the row has been replaced.
	 * All the cells keep their indexes but their item must be updated.
	 */
	protected void onReplace() {
		cells.forEach((i, c) -> {
			int linear = toLinear(index, i);
			T item = grid.getItems().getElement(linear);
			c.updateItem(item);
		});
		reusablePositions = true;
	}

	/**
	 * This is responsible for updating only the cell at the given column index (if present in the map)
	 * with the given item.
	 */
	protected void onReplace(int column, T item) {
		Optional.ofNullable(cells.get(column))
				.ifPresent(cell -> cell.updateItem(item));
		reusablePositions = true;
	}

	/**
	 * Calls {@link #onReplace(int, Object)}, since it is a diagonal replacement
	 * the column index is the same as the row's index.
	 */
	protected void onDiagReplace(T item) {
		onReplace(index, item);
	}

	/**
	 * Calls {@link #partialUpdate(int)}.
	 */
	protected void onRowAdd(int newIndex) {
		partialUpdate(newIndex);
	}

	/**
	 * Calls {@link #partialUpdate(int)}.
	 */
	protected void onRowRemove(int newIndex) {
		partialUpdate(newIndex);
	}

	/**
	 * This is responsible for correctly updating the {@code GridRow} when a {@link GridChangeType#ADD_COLUMN} change
	 * occurs in the grid's data structure.
	 * <p></p>
	 * The algorithm is very similar to the one used for {@link GridChangeType#ADD_ROW},
	 * described here {@link GridState#change(ObservableGrid.Change)}.
	 */
	protected void onColumnAdd(int column, IntegerRange columns) {
		Map<Integer, C> processed = new HashMap<>();
		Set<Integer> available = new HashSet<>(cells.keySet());
		int targetSize = columns.diff() + 1;

		// Before change index
		for (int i = columns.getMin(); i < column; i++) {
			C cell = cells.remove(i);
			if (index != 0) {
				int linear = toLinear(index, i);
				cell.updateIndex(linear);
			}
			processed.put(i, cell);
			available.remove(i);
		}

		// After change index
		Iterator<Map.Entry<Integer, C>> it = cells.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, C> next = it.next();
			int cIndex = next.getKey();
			int newIndex = cIndex + 1;
			if (!IntegerRange.inRangeOf(newIndex, columns) || (newIndex >= columns.getMin() && newIndex < column)) {
				continue;
			}

			int lIndex = (index == 0) ? newIndex : toLinear(index, newIndex);
			C cell = next.getValue();
			cell.updateIndex(lIndex);
			cell.updateCoordinates(index, newIndex);
			processed.put(newIndex, cell);
			available.remove(cIndex);
			it.remove();
		}

		// Remaining
		if (processed.size() != targetSize) {
			Integer oIndex = new ArrayDeque<>(available).poll();
			int lIndex = (index == 0) ? column : toLinear(index, column);
			T item = grid.getItems().getElement(lIndex);
			C cell;
			if (oIndex != null) {
				cell = cells.remove(oIndex);
				cell.updateItem(item);
			} else {
				cell = grid.getCellFactory().apply(item);
			}
			cell.updateIndex(lIndex);
			cell.updateCoordinates(index, column);
			processed.put(column, cell);
		}

		clear();
		cells.putAll(processed);
		this.columns = columns;
	}

	/**
	 * This is responsible for correctly updating the {@code GridRow} when a {@link GridChangeType#REMOVE_COLUMN} change
	 * occurs in the grid's data structure.
	 * <p></p>
	 * The algorithm is very similar to the one used for {@link GridChangeType#REMOVE_ROW},
	 * described here {@link GridState#change(ObservableGrid.Change)}.
	 */
	protected void onColumnRemove(int column, IntegerRange columns) {
		Map<Integer, C> processed = new HashMap<>();
		Set<Integer> available = new HashSet<>(cells.keySet());
		Set<Integer> rangeSet = IntegerRange.expandRangeToSet(columns);

		// Valid
		int start = Math.max(this.columns.getMin(), columns.getMin());
		int end = Math.min(columns.getMax(), column);
		for (int i = start; i < end; i++) {
			C cell = cells.remove(i);
			if (index != 0) {
				int linear = toLinear(index, i);
				cell.updateIndex(linear);
			}
			processed.put(i, cell);
			available.remove(i);
			rangeSet.remove(i);
		}

		// After change index
		Iterator<Map.Entry<Integer, C>> it = cells.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, C> next = it.next();
			int cIndex = next.getKey();
			int newIndex = cIndex - 1;
			if (!IntegerRange.inRangeOf(newIndex, columns) || (newIndex >= start && newIndex < end)) {
				continue;
			}

			int lIndex = (index == 0) ? newIndex : toLinear(index, newIndex);
			C cell = next.getValue();
			cell.updateIndex(lIndex);
			cell.updateCoordinates(index, newIndex);
			processed.put(newIndex, cell);
			available.remove(cIndex);
			rangeSet.remove(newIndex);
			it.remove();
		}

		// Remaining
		if (!rangeSet.isEmpty()) {
			Integer oIndex = new ArrayDeque<>(available).poll();
			int nIndex = new ArrayDeque<>(rangeSet).removeFirst();
			int lIndex = (index == 0) ? nIndex : toLinear(index, nIndex);
			T item = grid.getItems().getElement(lIndex);
			C cell;
			if (oIndex != null) {
				cell = cells.remove(oIndex);
				cell.updateItem(item);
			} else {
				cell = grid.getCellFactory().apply(item);
			}
			cell.updateIndex(lIndex);
			cell.updateCoordinates(index, nIndex);
			processed.put(nIndex, cell);
		}

		clear();
		cells.putAll(processed);
		this.columns = columns;
	}

	/**
	 * This is responsible for partially updating all the cells in the row. Only the indexes of the cell
	 * will be updated as the item is expected to be valid.
	 */
	private void partialUpdate(int newIndex) {
		cells.forEach((i, c) -> {
			int linear = toLinear(newIndex, i);
			c.updateIndex(linear);
			c.updateCoordinates(newIndex, i);
		});
		this.index = newIndex;
		reusablePositions = true;
	}

	// Layout

	/**
	 * This core method is called by {@link GridState#layoutRows()} and it's responsible for laying out all the cells in
	 * the {@code GridRow}. Two information are required, the vertical position of the row in the viewport and whether
	 * we are in an exceptional case as described here {@link GridState#layoutRows()}.
	 * <p></p>
	 * The horizontal positions for each column/cell are computed or reused if possible.
	 * <p>
	 * Then from the last cell and position, cells are laid out using {@link GridHelper#layout(Node, double, double)}.
	 */
	public void layoutCells(double position, boolean adjustColumns) {
		if (cells.isEmpty()) return;

		GridHelper helper = grid.getGridHelper();
		Size size = grid.getCellSize();
		double right = columns.diff() * size.getWidth();
		if (adjustColumns) right -= size.getWidth();

		// Compute positions
		if (!canReusePositions() || (positions.size() != columns.diff() + 1 || adjustColumns)) {
			positions.clear();
			for (int i = 0; i < size(); i++) {
				positions.add(right);
				right -= size.getWidth();
			}
		}

		// Layout
		ListIterator<C> cIt = new ArrayList<>(cells.values()).listIterator(size());
		ListIterator<Double> pIt = new ArrayList<>(positions).listIterator(size());
		while (cIt.hasPrevious()) {
			C cell = cIt.previous();
			Double pos = pIt.previous();
			Node node = cell.getNode();
			cell.beforeLayout();
			helper.layout(node, pos, position);
			cell.afterLayout();
		}
		this.position = position;
		this.reusablePositions = false;
	}

	// Misc

	/**
	 * Calls {@link C#dispose()} on all the cells in the map, then clears the map, leading to
	 * an empty {@code GridRow}.
	 */
	protected void clear() {
		cells.values().forEach(C::dispose);
		cells.clear();
	}

	/**
	 * @return the number of cells/columns in the {@code GridRow}
	 */
	public int size() {
		return cells.size();
	}

	/**
	 * Converts the given coordinates to a linear index using {@link GridUtils#subToInd(int, int, int)}.
	 */
	public int toLinear(int row, int column) {
		return GridUtils.subToInd(grid.getColumnsNum(), row, column);
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

	/**
	 * @return the {@link VirtualGrid} instance associated to this row
	 */
	public VirtualGrid<T, C> getVirtualGrid() {
		return grid;
	}

	/**
	 * @return this row's index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @return the range of columns represented by this row
	 */
	public IntegerRange getColumns() {
		return columns;
	}

	/**
	 * @return the cells map
	 */
	protected Map<Integer, C> getCells() {
		return cells;
	}

	/**
	 * @return the cells map as an unmodifiable map
	 */
	public Map<Integer, C> getCellsUnmodifiable() {
		return Collections.unmodifiableMap(cells);
	}

	/**
	 * Converts the cells map to a new map with a different key -> value mapping.
	 * <p>
	 * Instead of using the column index as the key, this map converts each index to a linear index
	 * using {@link #toLinear(int, int)}.
	 */
	public Map<Integer, C> getLinearCells() {
		return cells.entrySet().stream()
				.collect(Collectors.toMap(
						e -> toLinear(index, e.getKey()),
						Map.Entry::getValue
				));
	}

	/**
	 * @return the cells' positions set
	 */
	protected Set<Double> getPositions() {
		return positions;
	}

	/**
	 * @return the cells' positions set as an unmodifiable set
	 */
	public Set<Double> getPositionsUnmodifiable() {
		return Collections.unmodifiableSet(positions);
	}

	/**
	 * @return the vertical position at which the row should be positioned in the viewport
	 */
	public double getPosition() {
		return position;
	}

	/**
	 * @return whether the already computed positions for the cells can be reused by {@link #layoutCells(double, boolean)}
	 */
	public boolean canReusePositions() {
		return reusablePositions;
	}

	protected void setReusablePositions(boolean reusablePositions) {
		this.reusablePositions = reusablePositions;
	}

	/**
	 * @return whether the cells of this row are visible in the viewport
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Responsible for showing/hiding the cells of this row. Cells are visible by default but various
	 * implementations of the Grid may decide to hide rows under certain conditions.
	 */
	protected void setVisible(boolean visible) {
		cells.values().forEach(c -> c.getNode().setVisible(visible));
		this.visible = visible;
	}

	//================================================================================
	// Ignore
	//================================================================================
	/*
	// This is the old onColumnsAdd() algorithm implementation before refactoring to a common
	// solution for both index = 0 and index > 0
	protected void onColumnAdd(int column, IntegerRange range) {
		Map<Integer, C> processed = new HashMap<>();
		Set<Integer> available = new HashSet<>(cells.keySet());
		int targetSize = columns.diff() + 1;
		Iterator<Map.Entry<Integer, C>> it;

		if (index == 0) {
			// Before change index
			for (int i = columns.getMin(); i < column; i++) {
				processed.put(i, cells.remove(i));
				available.remove(i);
			}

			// After change index
			it = cells.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, C> next = it.next();
				int cIndex = next.getKey();
				int newIndex = cIndex + 1;
				if (!IntegerRange.inRangeOf(newIndex, columns) || (newIndex >= columns.getMin() && newIndex < column)) {
					continue;
				}

				C cell = next.getValue();
				cell.updateIndex(newIndex); // For row 0, linear and coordinate are the same
				cell.updateCoordinates(index, newIndex);
				processed.put(newIndex, cell);
				available.remove(cIndex);
				it.remove();
			}

			// Remaining
			if (processed.size() != targetSize) {
				Integer oIndex = new ArrayDeque<>(available).poll();
				C cell;
				T item = grid.getItems().getElement(column); // For row 0, linear and coordinate are the same
				if (oIndex != null) {
					cell = cells.remove(oIndex);
					cell.updateItem(item);
				} else {
					cell = grid.getCellFactory().apply(item);
				}
				cell.updateIndex(column);
				cell.updateCoordinates(index, column);
				processed.put(column, cell);
			}

			clear();
			cells.putAll(processed);
			this.columns = columns;
			return;
		}

		// Before change index
		for (int i = columns.getMin(); i < column; i++) {
			int linear = toLinear(index, i);
			C cell = cells.remove(i);
			cell.updateIndex(linear);
			processed.put(i, cell);
			available.remove(i);
		}

		// After change index
		it = cells.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, C> next = it.next();
			int cIndex = next.getKey();
			int newIndex = cIndex + 1;
			if (!IntegerRange.inRangeOf(newIndex, columns) || (newIndex >= columns.getMin() && newIndex < column)) {
				continue;
			}

			C cell = next.getValue();
			cell.updateIndex(toLinear(index, newIndex));
			cell.updateCoordinates(index, newIndex);
			processed.put(newIndex, cell);
			available.remove(cIndex);
			it.remove();
		}

		// Remaining
		if (processed.size() != targetSize) {
			Integer oIndex = new ArrayDeque<>(available).poll();
			int linear = toLinear(index, column);
			T item = grid.getItems().getElement(linear);
			C cell;
			if (oIndex != null) {
				cell = cells.remove(oIndex);
				cell.updateItem(item);
			} else {
				cell = grid.getCellFactory().apply(item);
			}
			cell.updateIndex(linear);
			cell.updateCoordinates(index, column);
			processed.put(column, cell);
		}

		clear();
		cells.putAll(processed);
		this.columns = columns;
	}
	*/
}
