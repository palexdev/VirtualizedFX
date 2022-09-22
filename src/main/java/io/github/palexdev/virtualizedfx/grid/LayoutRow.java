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
import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import javafx.scene.Node;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class to support {@link GridState}.
 * <p>
 * The major issue in managing {@link VirtualGrid} is that we are dealing with a 2D data structure, which makes all the
 * algorithms a lot more complex.
 * <p></p>
 * {@link LayoutRow}'s goal is to simplify this by dividing tasks, at the end we want to only manage a 1D structure which
 * are the map of columns, {@code columnIndex -> cell}. So, the {@link GridState} is mainly responsible for managing exclusively
 * rows related changes, this instead is mainly responsible for correctly handling the columns.
 * <p></p>
 * {@link LayoutRow}s contain information such as:
 * <p> - the row's index
 * <p> - the range of columns
 * <p> - the actual cells that will be displayed in the viewport
 * <p> - a map containing the horizontal positions of the rows, these can in some cases be reused
 */
public class LayoutRow<T, C extends GridCell<T>> {
	//================================================================================
	// Properties
	//================================================================================
	private final VirtualGrid<T, C> virtualGrid;
	private int index;
	private IntegerRange columns;
	private final Map<Integer, C> cells = new TreeMap<>();
	private final Set<Double> positions = new TreeSet<>();
	private double rowPos;
	private boolean reusablePositions = false;

	//================================================================================
	// Constructors
	//================================================================================
	public LayoutRow(VirtualGrid<T, C> virtualGrid, int index, IntegerRange columns) {
		this.virtualGrid = virtualGrid;
		this.index = index;
		this.columns = columns;
	}

	public static <T, C extends GridCell<T>> LayoutRow<T, C> of(VirtualGrid<T, C> virtualGrid, int index, IntegerRange columns) {
		return new LayoutRow<>(virtualGrid, index, columns);
	}

	//================================================================================
	// Methods
	//================================================================================
	// Initialization

	/**
	 * Initializes the row by creating a cell for each column given by the columns range.
	 */
	public void init() {
		if (index < 0 || IntegerRange.of(-1).equals(columns))
			return;

		clear();
		for (Integer column : columns) {
			int linear = GridUtils.subToInd(virtualGrid.getColumnsNum(), index, column);
			T item = virtualGrid.getItems().getElement(linear);
			C cell = virtualGrid.getCellFactory().apply(item);
			cell.updateIndex(linear);
			// TODO update coordinate
			cells.put(column, cell);
		}
	}

	// Updates

	/**
	 * Responsible for updating the row to a new index.
	 * This means that all the cells will need to be updated fully (both index and item).
	 */
	public void updateIndex(int index) {
		for (Map.Entry<Integer, C> e : cells.entrySet()) {
			int column = e.getKey();
			int linear = toLinear(index, column);
			C cell = e.getValue();
			T item = virtualGrid.getItems().getElement(linear);
			cell.updateItem(item);
			cell.updateIndex(linear);
			// TODO update coordinate
		}
		this.index = index;
		this.reusablePositions = true;
	}

	/**
	 * Differs from {@link #init()} because this assumes that there are already some cells in the map,
	 * and tries to reuse them. This method is useful when the columns range changes.
	 */
	public void updateColumns(IntegerRange columns) {
		int targetSize = columns.diff() + 1;
		if (this.columns.equals(columns) && size() == targetSize) return;

		Map<Integer, C> tmp = new HashMap<>();
		Set<Integer> available = IntegerRange.expandRangeToSet(columns);

		// Commons can be reused
		Iterator<Integer> kIt = cells.keySet().iterator();
		while (kIt.hasNext() && !available.isEmpty()) {
			Integer column = kIt.next();
			if (available.remove(column)) {
				tmp.put(column, cells.get(column));
				kIt.remove();
			}
		}

		// Remaining processing
		if (tmp.size() < targetSize) {
			Deque<Integer> remaining = new ArrayDeque<>(cells.keySet());
			for (Integer column : available) {
				Integer rIndex = remaining.poll();
				int linear = toLinear(index, column);
				T item = virtualGrid.getItems().getElement(linear);
				C cell;
				if (rIndex != null) {
					cell = cells.remove(rIndex);
					cell.updateItem(item);
				} else {
					cell = virtualGrid.getCellFactory().apply(item);
				}
				cell.updateIndex(linear);
				tmp.put(column, cell);
			}
		}

		clear();
		cells.putAll(tmp);
		this.columns = columns;
	}

	// Changes

	/**
	 * Responsible for updating the diagonal with the given item.
	 * The diagonal cell is the cell at the same index as the row.
	 */
	public void onDiagonalUpdate(T item) {
		C cell = cells.get(index);
		if (cell != null) {
			cell.updateItem(item);
		}
		this.reusablePositions = true;
	}

	/**
	 * Responsible for updating the cells' item when a row replacement occurs.
	 */
	public void onRowReplacement() {
		cells.forEach((i, c) -> {
			int linear = toLinear(index, i);
			T item = virtualGrid.getItems().getElement(linear);
			c.updateItem(item);
		});
		this.reusablePositions = true;
	}

	/**
	 * Responsible for updating the row's index and the cells' index,
	 * used instead of {@link #updateIndex(int)} when a partial update is needed.
	 */
	public void partialUpdate(int index) {
		cells.forEach((column, cell) -> {
			int linear = toLinear(index, column);
			cell.updateIndex(linear);
		});
		this.index = index;
		this.reusablePositions = true;
	}

	/**
	 * Responsible for updating cells when a column replacement occurs.
	 * If a cell exists at the given column index its item is updated.
	 */
	public void onColumnReplacement(int column) {
		if (cells.containsKey(column)) {
			int linear = toLinear(index, column);
			T item = virtualGrid.getItems().getElement(linear);
			C cell = cells.get(column);
			cell.updateItem(item);
		}
		this.reusablePositions = true;
	}

	/**
	 * Responsible for correctly computing column additions.
	 * <p>
	 * Special case if the row is at index 0, and the added column is in the row's columns range, we use
	 * {@link #addColumn(int)} instead.
	 * <p></p>
	 * Otherwise, every cell in the columns range is fully updated.
	 */
	public void onColumnAdd(int column) {
		if (index == 0) {
			if (IntegerRange.inRangeOf(column, columns)) {
				addColumn(column);
			}
			return;
		}

		for (Integer c : columns) {
			int linear = toLinear(index, c);
			T item = virtualGrid.getItems().getElement(linear);
			C cell = cells.get(c);
			cell.updateItem(item); // TODO unnecessary
			cell.updateIndex(linear);
		}
		this.reusablePositions = true;
	}

	/**
	 * Responsible for correctly computing column removals.
	 * <p>
	 * Special case if the row is at index 0 since the cells that come before the removed column are valid and do not
	 * need any update.
	 * <p>
	 * For any other case {@link #removeColumn(int, IntegerRange)} is used instead.
	 * <p></p>
	 * These algorithms are pretty complex, so I'm not going to describe their internals this time,
	 * if you are brave enough check the src code.
	 */
	public void onColumnRemoval(int column, IntegerRange range) {
		if (index == 0) {
			Map<Integer, C> tmp = new HashMap<>();

			int start = Math.max(range.getMin(), column + 1);
			Iterator<Map.Entry<Integer, C>> it = cells.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, C> next = it.next();
				int c = next.getKey();
				if (c == column) continue;
				if (c < start) {
					tmp.put(c, cells.get(c));
					it.remove();
					continue;
				}

				C cell = next.getValue();
				cell.updateIndex(c - 1);
				tmp.put(c - 1, cell);
				it.remove();
			}

			int targetSize = range.diff() + 1;
			int newCol = (tmp.containsKey(range.getMax())) ?
					range.getMin() :
					range.getMax();
			if (tmp.size() < targetSize) {
				int linear = toLinear(getColumnsNum(), index, newCol);
				T item = virtualGrid.getItems().getElement(linear);
				C cell = cells.remove(column);
				cell.updateItem(item);
				cell.updateIndex(linear);
				tmp.put(newCol, cell);
			}

			clear();
			this.cells.putAll(tmp);
		} else {
			removeColumn(column, range);
		}
		this.columns = range;
	}

	protected void addColumn(int column) {
		Map<Integer, C> tmp = new HashMap<>();

		int from = Math.max(columns.getMin(), column);
		int to = columns.getMax() - 1;
		for (int i = from; i <= to; i++) {
			int linear = toLinear(index, i + 1);
			C cell = cells.remove(i);
			cell.updateIndex(linear);
			tmp.put(i + 1, cell);
		}

		int linear = toLinear(index, column);
		T item = virtualGrid.getItems().getElement(linear);
		C cell = cells.remove(to + 1);
		cell.updateItem(item);
		cell.updateIndex(linear);
		tmp.put(column, cell);

		cells.putAll(tmp);
	}

	protected void removeColumn(int column, IntegerRange range) {
		Map<Integer, C> tmp = new HashMap<>();
		Iterator<Map.Entry<Integer, C>> it = cells.entrySet().iterator();
		int start = range.getMin();
		while (it.hasNext()) {
			Map.Entry<Integer, C> next = it.next();
			int c = next.getKey();
			if (c == column) continue;

			int offset = (c < column) ? index : index + 1;
			int linear = toLinear(getColumnsNum() + 1, index, c);
			C cell = next.getValue();
			cell.updateIndex(linear - offset);
			tmp.put(start, cell);
			it.remove();
			start++;
		}

		int targetSize = range.diff() + 1;
		if (tmp.size() != targetSize && size() != targetSize) {
			int linear = toLinear(index, range.getMax());
			T item = virtualGrid.getItems().getElement(linear);
			C cell = cells.remove(column);
			cell.updateItem(item);
			cell.updateIndex(linear);
			tmp.put(range.getMax(), cell);
		}

		clear();
		cells.putAll(tmp);
	}

	// Layout

	/**
	 * Core method responsible for laying out the cells.
	 * This is used by {@link GridState#layoutRows()}.
	 * <p></p>
	 * If the cells' positions have already been computed previously, and they can be reused their re-computation
	 * is skipped, and it goes directly to the layout part.
	 * <p></p>
	 * For the layout, there are two different {@link ListIterator}s. One iterates over the cells and the other one
	 * over the positions map. Both go in reverse.
	 */
	public void layoutCells(double rowPos, boolean adjustColumns) {
		if (cells.isEmpty()) return;

		GridHelper helper = virtualGrid.getGridHelper();
		SizeBean size = virtualGrid.getCellSize();
		double right = columns.diff() * size.getWidth();
		if (adjustColumns) right -= size.getWidth();

		if (!reusablePositions || (positions.size() != columns.diff() + 1) || adjustColumns) {
			positions.clear();
			for (int i = 0; i < size(); i++) {
				positions.add(right);
				right -= size.getWidth();
			}
		}

		ListIterator<C> cIt = new ArrayList<>(cells.values()).listIterator(cells.size());
		ListIterator<Double> pIt = new ArrayList<>(positions).listIterator(positions.size());
		while (cIt.hasPrevious()) {
			C cell = cIt.previous();
			Double pos = pIt.previous();
			Node node = cell.getNode();
			cell.beforeLayout();
			helper.layout(node, pos, rowPos);
			cell.afterLayout();
		}
		this.rowPos = rowPos;
		this.reusablePositions = false;
	}

	// Misc

	/**
	 * Adds the given cell at the given index in the cells map.
	 */
	public void addCell(int index, C cell) {
		cells.put(index, cell);
	}

	/**
	 * Disposes all the cells and clears the map.
	 */
	public void clear() {
		cells.values().forEach(C::dispose);
		cells.clear();
	}

	/**
	 * @return the number of cells in this row
	 */
	public int size() {
		return cells.size();
	}

	/**
	 * Converts the given row and column coordinates to a linear index, given the number of columns by using
	 * {@link GridUtils#subToInd(int, int, int)}.
	 */
	protected int toLinear(int columnsNum, int row, int column) {
		return GridUtils.subToInd(columnsNum, row, column);
	}

	/**
	 * Converts the given row and column coordinates to a linear index, by using {@link GridUtils#subToInd(int, int, int)},
	 * the columns number is extracted from the grid, {@link #getColumnsNum()}.
	 */
	protected int toLinear(int row, int column) {
		return GridUtils.subToInd(getColumnsNum(), row, column);
	}

	/**
	 * @return the grid's number of columns, {@link VirtualGrid#getColumnsNum()}
	 */
	protected int getColumnsNum() {
		return virtualGrid.getColumnsNum();
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

	/**
	 * @return the {@link VirtualGrid} instance associated to this row
	 */
	public VirtualGrid<T, C> getVirtualGrid() {
		return virtualGrid;
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
	 * Instead of using the column index as the key, this map converts each index to a linear index.
	 */
	public Map<Integer, C> getLinearMap() {
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
	 * @return the position at which the row should be positioned in the viewport
	 */
	public double getRowPos() {
		return rowPos;
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
}
