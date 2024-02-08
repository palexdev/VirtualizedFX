package io.github.palexdev.virtualizedfx.grid;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.behavior.BehaviorBase;
import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.virtualizedfx.cells.Cell;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.list.VFXListHelper;
import io.github.palexdev.virtualizedfx.list.VFXListState;
import io.github.palexdev.virtualizedfx.utils.StateMap;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.geometry.Orientation;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;

public class VFXGridManager<T, C extends Cell<T>> extends BehaviorBase<VFXGrid<T, C>> {
	//================================================================================
	// Properties
	//================================================================================
	protected boolean invalidatingPos = false;

	//================================================================================
	// Constructors
	//================================================================================
	public VFXGridManager(VFXGrid<T, C> grid) {
		super(grid);
	}

	//================================================================================
	// Methods
	//================================================================================
	protected void onGeometryChanged() {
		invalidatingPos = true;
		VFXGrid<T, C> grid = getNode();
		VFXGridHelper<T, C> helper = grid.getHelper();
		if (!gridFactorySizeCheck()) return;

		// Ensure positions are valid!
		helper.invalidatePos();

		IntegerRange rowsRange = helper.rowsRange();
		IntegerRange columnsRange = helper.columnsRange();
		if (!rangeCheck(rowsRange, columnsRange, true, true)) return;

		// Compute the new state
		VFXGridState<T, C> newState = new VFXGridState<>(grid, rowsRange, columnsRange);
		moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);

		if (disposeCurrent()) newState.setCellsChanged(true);
		grid.update(newState);
		invalidatingPos = false;
	}

	protected void onPositionChanged(Orientation axis) {
		if (invalidatingPos) return;
		VFXGrid<T, C> grid = getNode();
		VFXGridState<T, C> state = grid.getState();
		if (state == VFXGridState.EMPTY) return;

		VFXGridHelper<T, C> helper = grid.getHelper();
		IntegerRange lastRange = (axis == Orientation.VERTICAL) ?
			state.getRowsRange() :
			state.getColumnsRange();
		IntegerRange range = (axis == Orientation.VERTICAL) ?
			helper.rowsRange() :
			helper.columnsRange();
		if (Objects.equals(lastRange, range) || Utils.INVALID_RANGE.equals(range)) return;

		// Compute the new state
		IntegerRange rowsRange = (axis == Orientation.VERTICAL) ? range : helper.rowsRange();
		IntegerRange columnsRange = (axis == Orientation.VERTICAL) ? helper.columnsRange() : range;
		VFXGridState<T, C> newState = new VFXGridState<>(grid, rowsRange, columnsRange);
		moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);

		// Always request layout update because cells need to be in the right place after scrolling
		if (disposeCurrent()) newState.setCellsChanged(true);
		grid.update(newState);
		if (!newState.haveCellsChanged()) grid.requestViewportLayout();
	}

	protected void onColumnsNumChanged() {
		invalidatingPos = true;
		VFXGrid<T, C> grid = getNode();
		if (!gridFactorySizeCheck()) return;
		VFXGridHelper<T, C> helper = grid.getHelper();

		// Ensure positions are valid!
		// When the number of columns changes, both the estimated width and height change too
		// As a result of that, it is indeed needed to ensure that the current scroll positions are valid
		helper.invalidatePos();

		// First check: ensure that both ranges are valid
		IntegerRange rowsRange = helper.rowsRange();
		IntegerRange columnsRange = helper.columnsRange();
		if (!rangeCheck(rowsRange, columnsRange, true, true)) return;

		VFXGridState<T, C> current = grid.getState();
		// Second check: ensure that new ranges are different from the old ones
		if (rowsRange.equals(current.getRowsRange()) &&
			columnsRange.equals(current.getColumnsRange()))
			return;

		// Finally proceed with the 'moveReuseCreate()' algorithm
		VFXGridState<T, C> newState = new VFXGridState<>(grid, rowsRange, columnsRange);
		moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);

		// I believe a layout request is always needed as the algorithm may move cells around in the state
		// Which means that they should be repositioned too.
		if (disposeCurrent()) newState.setCellsChanged(true);
		grid.update(newState);
		if (!newState.haveCellsChanged()) grid.requestViewportLayout();
		invalidatingPos = false;
	}

	protected void onCellFactoryChanged() {
		VFXGrid<T, C> grid = getNode();

		// Dispose current state, cells if any (not EMPTY) are now in cache
		// Purge cache too, cells are from old factory
		if (disposeCurrent()) grid.getCache().clear();
		if (!gridFactorySizeCheck()) return;

		VFXGridState<T, C> current = grid.getState();
		IntegerRange rowsRange = current.getRowsRange();
		IntegerRange columnsRange = current.getColumnsRange();
		if (!rangeCheck(rowsRange, columnsRange, true, true)) return;

		VFXGridState<T, C> newState = new VFXGridState<>(grid, rowsRange, columnsRange);
		moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);
		newState.setCellsChanged(true);
		grid.update(newState);
	}

	protected void onCellSizeChanged() {
		invalidatingPos = true;
		VFXGrid<T, C> grid = getNode();
		VFXGridHelper<T, C> helper = grid.getHelper();

		// Ensure positions are valid!
		helper.invalidatePos();

		if (!gridFactorySizeCheck()) return;

		// Compute new state with the move/reuse/create algorithm
		// We don't use an intersection algorithm here because performance-wise is the same
		IntegerRange rowsRange = helper.rowsRange();
		IntegerRange columnsRange = helper.columnsRange();
		VFXGridState<T, C> newState = new VFXGridState<>(grid, rowsRange, columnsRange);
		moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);
		if (disposeCurrent()) newState.setCellsChanged(true);
		grid.update(newState);
		invalidatingPos = true;
	}

	protected void onSpacingChanged() {
		invalidatingPos = true;
		VFXGrid<T, C> grid = getNode();
		if (!gridFactorySizeCheck()) return;
		VFXGridHelper<T, C> helper = grid.getHelper();

		// Ensure positions are valid!
		helper.invalidatePos();

		// Ensure ranges are valid
		IntegerRange rowsRange = helper.rowsRange();
		IntegerRange columnsRange = helper.columnsRange();
		if (!rangeCheck(rowsRange, columnsRange, true, true)) return;

		// Compute new state
		VFXGridState<T, C> newState = new VFXGridState<>(grid, rowsRange, columnsRange);
		moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);

		if (disposeCurrent()) newState.setCellsChanged(true);
		grid.update(newState);
		if (!newState.haveCellsChanged()) grid.requestViewportLayout();
		invalidatingPos = false;
	}

	protected void onItemsChanged() {
		invalidatingPos = true;
		VFXGrid<T, C> grid = getNode();
		VFXGridHelper<T, C> helper = grid.getHelper();

		/*
		 * Ensure positions are valid!
		 * In theory, this is only needed if the list now is smaller than before (or the old one)
		 * But since that information is lost (we would have to track it here in some way), we always
		 * invalidate positions, after all, it's not a big deal anyway.
		 */
		helper.invalidatePos();

		VFXGridState<T, C> current = grid.getState();
		if (!gridFactorySizeCheck()) return;

		// Compute the ranges and new state
		int nColumns = helper.maxColumns();
		IntegerRange rowsRange = helper.rowsRange();
		IntegerRange columnsRange = helper.columnsRange();
		Set<Integer> remaining = new LinkedHashSet<>();
		VFXGridState<T, C> newState = new VFXGridState<>(grid, rowsRange, columnsRange);

		// Index updates
		outer_loop:
		for (Integer rIdx : rowsRange) {
			for (Integer cIdx : columnsRange) {
				int linear = GridUtils.subToInd(nColumns, rIdx, cIdx);
				if (linear >= grid.size()) break outer_loop;
				T item = helper.indexToItem(linear);
				C c = current.removeCell(item);
				if (c != null) {
					c.updateIndex(linear);
					newState.addCell(linear, item, c);
					continue;
				}
				remaining.add(linear);
			}
		}

		// Process remaining with the "remaining' algorithm"
		remainingAlgorithm(remaining, newState);

		if (disposeCurrent()) newState.setCellsChanged(true);
		grid.update(newState);
		if (!newState.haveCellsChanged()) grid.requestViewportLayout();
		invalidatingPos = false;
	}

	//================================================================================
	// Common
	//================================================================================

	/**
	 * Avoids code duplication. Typically used when, while iterating on the rows and columns ranges,
	 * it's enough to move the cells from the current state to the new state. For indexes which are not found
	 * in the current state, a new cell is either taken from the old state, taken from cache or created by the cell factory.
	 * <p>
	 * (The last operations are delegated to the {@link #remainingAlgorithm(Set, VFXGridState)}).
	 *
	 * @see VFXGridHelper#indexToCell(int)
	 * @see VFXGrid#cellFactoryProperty()
	 */
	protected void moveReuseCreateAlgorithm(IntegerRange rowsRange, IntegerRange columnsRange, VFXGridState<T, C> newState) {
		VFXGrid<T, C> grid = getNode();
		int nColumns = grid.getHelper().maxColumns();
		VFXGridState<T, C> current = grid.getState();
		Set<Integer> remaining = new LinkedHashSet<>();
		outer_loop:
		for (Integer rIdx : rowsRange) {
			for (Integer cIdx : columnsRange) {
				int linear = GridUtils.subToInd(nColumns, rIdx, cIdx);
				if (linear >= grid.size()) break outer_loop;
				C c = current.removeCell(linear);
				if (c == null) {
					remaining.add(linear);
					continue;
				}
				newState.addCell(linear, c);
			}
		}
		remainingAlgorithm(remaining, newState);
	}

	/**
	 * Avoids code duplication. Typically used to process indexes not found in the current state.
	 * <p>
	 * For any index in the given collection, a cell is needed. Also, it needs to be updated by index and item both.
	 * This cell can come from three sources:
	 * <p> 1) from the current state if it's not empty yet. Since the cells are stored in a {@link SequencedMap}, one
	 * is removed by calling {@link StateMap#poll()}.
	 * <p> 2) from the {@link VFXCellsCache} if not empty
	 * <p> 3) created by the cell factory
	 * <p></p>
	 * <p> - See {@link VFXListHelper#indexToCell(int)}: this handles the second and third cases. If a cell can
	 * be taken from the cache, automatically updates its item then returns it. Otherwise, invokes the
	 * {@link VFXList#cellFactoryProperty()} to create a new one
	 */
	protected void remainingAlgorithm(Set<Integer> remaining, VFXGridState<T, C> newState) {
		VFXGrid<T, C> grid = getNode();
		VFXGridHelper<T, C> helper = grid.getHelper();
		VFXGridState<T, C> current = grid.getState();

		// Indexes in the given set were not found in the current state.
		// Which means item updates. Cells are retrieved either from the current state (if not empty), from the cache,
		// or created from the factory
		for (Integer index : remaining) {
			// We don't check the index here since we expect that the 'remaining' Set already contains valid indexes
			T item = helper.indexToItem(index);
			C c;
			if (!current.isEmpty()) {
				c = current.getCells().poll().getValue();
				c.updateIndex(index);
				c.updateItem(item);
			} else {
				c = helper.itemToCell(item);
				c.updateIndex(index);
				newState.setCellsChanged(true);
			}
			newState.addCell(index, item, c);
		}
	}

	/**
	 * Avoids code duplication. This method checks for three things:
	 * <p> 1) If the list is empty
	 * <p> 2) If the cell factory is null
	 * <p> 3) If the cell size is lesser or equal to 0
	 * <p>
	 * If any of those checks is true: the list's state is set to {@link VFXListState#EMPTY}, the
	 * current state is disposed, the 'invalidatingPos' flag is reset, finally returns false.
	 * Otherwise, does nothing and returns true.
	 * <p></p>
	 * <p> - See {@link VFXGrid#cellFactoryProperty()}
	 * <p> - See {@link VFXGrid#cellSizeProperty()}
	 * <p> - See {@link #disposeCurrent()}: for the current state disposal
	 *
	 * @return whether all the aforementioned checks have passed
	 */
	@SuppressWarnings("unchecked")
	protected boolean gridFactorySizeCheck() {
		VFXGrid<T, C> grid = getNode();
		if (grid.isEmpty() || grid.getCellFactory() == null ||
			grid.getCellSize().getWidth() <= 0 || grid.getCellSize().getHeight() <= 0) {
			disposeCurrent();
			grid.update(VFXGridState.EMPTY);
			invalidatingPos = false;
			return false;
		}
		return true;
	}

	/**
	 * Avoids code duplication. Used to check whether the given ranges are valid, not equal to {@link Utils#INVALID_RANGE}.
	 * <p>
	 * When invalid, returns false, but first runs the following operations: disposes the current state (only if the
	 * 'dispose' parameter is true), sets the grid's state to {@link VFXGridState#EMPTY} (only if the 'update'
	 * parameter is true), resets the 'invalidatingPos' flag.
	 * Otherwise, does nothing and returns true.
	 * <p>
	 * Last but not least, this is a note for the future on why the method is structured like this. It's crucial for
	 * the disposal operation to happen <b>before</b> the list's state is set to {@link VFXGridState#EMPTY}, otherwise
	 * the disposal method will fail, since it will then retrieve the empty state instead of the correct one.
	 * <p></p>
	 * <p> - See {@link #disposeCurrent()}: for the current state disposal
	 *
	 * @param rowsRange    the rows range to check
	 * @param columnsRange the columns range to check
	 * @param update       whether to set the grid's state to 'empty' if the range is not valid
	 * @param dispose      whether to dispose the current/old state if the range is not valid
	 * @return whether the range is valid or not
	 */
	@SuppressWarnings("unchecked")
	protected boolean rangeCheck(IntegerRange rowsRange, IntegerRange columnsRange, boolean update, boolean dispose) {
		VFXGrid<T, C> grid = getNode();
		if (Utils.INVALID_RANGE.equals(rowsRange) || Utils.INVALID_RANGE.equals(columnsRange)) {
			if (dispose) disposeCurrent();
			if (update) grid.update(VFXGridState.EMPTY);
			invalidatingPos = false;
			return false;
		}
		return true;
	}

	/**
	 * Avoids code duplication. Responsible for disposing the current state if it is not empty.
	 * <p></p>
	 * <p> - See {@link VFXGridState#dispose()}
	 *
	 * @return whether the disposal was done or not
	 */
	protected boolean disposeCurrent() {
		VFXGridState<T, C> state = getNode().getState();
		if (!state.isEmpty()) {
			state.dispose();
			return true;
		}
		return false;
	}
}
