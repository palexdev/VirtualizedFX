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

import io.github.palexdev.mfxcore.base.TriConsumer;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.collections.Grid;

import java.util.function.Consumer;
import java.util.function.Supplier;

// TODO move to MFXCore?
public class GridIterator<T> {
	private final Grid<T> grid;
	private Iterable<Integer> range;
	private IntegerRange bounds;
	private Consumer<Integer> rowAction = i -> {
	};
	private Supplier<Boolean> breakCondition = () -> false;
	private int nColumns;
	private IntegerRange currRange;

	private GridIterator(Grid<T> grid) {
		this.grid = grid;
		this.nColumns = grid.getColumnsNum();
	}

	public static <T> GridIterator<T> on(Grid<T> grid) {
		return new GridIterator<>(grid);
	}

	public static <T> GridIterator<T> on(VirtualGrid<T, ?> virtualGrid) {
		return on(virtualGrid.getItems());
	}

	public GridIterator<T> forRange(Iterable<Integer> range) {
		this.range = range;
		return this;
	}

	public GridIterator<T> withBounds(IntegerRange bounds) {
		this.bounds = bounds;
		return this;
	}

	public GridIterator<T> forRow(Consumer<Integer> rowAction) {
		this.rowAction = rowAction;
		return this;
	}

	public GridIterator<T> breakCondition(Supplier<Boolean> breakCondition) {
		this.breakCondition = breakCondition;
		return this;
	}

	public GridIterator<T> columnsNum(int nColumns) {
		this.nColumns = nColumns;
		return this;
	}

	public int getColumnsNum() {
		return (nColumns != 0) ? nColumns : grid.getColumnsNum();
	}

	public IntegerRange getCurrRange() {
		return currRange;
	}

	// TODO maybe offer alternatives with only Consumer and BiConsumer, more lightweight
	public void execute(TriConsumer<Integer, Integer, Integer> action) {
		if (range == null ||
				range.equals(IntegerRange.of(-1)) ||
				bounds == null) {
			return;
		}

		int column = bounds.getMin();
		for (Integer row : range) {
			int start = row * getColumnsNum() + bounds.getMin();
			int end = start + bounds.diff();
			currRange = IntegerRange.of(start, end);
			for (int j = start; j <= end; j++) {
				if (breakCondition.get()) return;
				action.accept(row, column, j);
				column++;
			}
			column = bounds.getMin();
			rowAction.accept(row);
		}
	}

	public void reverseExecute(TriConsumer<Integer, Integer, Integer> action) {
		if (range == null ||
				range.equals(IntegerRange.of(-1)) ||
				bounds == null) {
			return;
		}
		if (!(range instanceof IntegerRange)) {
			throw new UnsupportedOperationException("Reverse loop is supported only by IntegerRange");
		}

		IntegerRange range = (IntegerRange) this.range;
		int column = bounds.getMax();
		for (int i = range.getMax(); i >= range.getMin(); i--) {
			int start = i * getColumnsNum() + bounds.getMin();
			int end = start + bounds.diff();
			currRange = IntegerRange.of(start, end);
			for (int j = end; j >= start; j--) {
				if (breakCondition.get()) return;
				action.accept(i, column, j);
				column++;
			}
			column = bounds.getMax();
			rowAction.accept(i);
		}
	}
}