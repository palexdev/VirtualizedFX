/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.table;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.behavior.BehaviorBase;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.enums.GeometryChangeType;
import io.github.palexdev.virtualizedfx.utils.ExcludingRange;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap.StateMap;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ListProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;

import java.util.HashSet;
import java.util.SequencedMap;
import java.util.Set;
import java.util.function.Function;

/**
 * Default behavior implementation for {@link VFXTable}. Although, to be precise, and as the name also suggests,
 * this can be considered more like a 'manager' than a behavior. Behaviors typically respond to user input, and then update
 * the component's state. This behavior contains core methods to respond to various properties change in {@link VFXTable}.
 * All computations here will generate a new {@link VFXTableState}, if possible, and update the table and the layout
 * (indirectly, call to {@link VFXTable#requestViewportLayout()}. Beware, some changes may end up generating 'clone' states,
 * because apparently nothing changed, see {@link VFXTableState}, {@link VFXTableState#clone()}, {@link VFXTableState#isClone()}.
 * <p>
 * By default, manages the following changes:
 * <p> - geometry changes (width/height changes), {@link #onGeometryChanged(GeometryChangeType)}
 * <p> - columns' width changes, {@link #onColumnWidthChanged(VFXTableColumn)}
 * <p> - columns list changes, {@link #onColumnsChanged(ListChangeListener.Change)}
 * <p> - items change, {@link #onItemsChanged()}
 * <p> - position changes, {@link #onPositionChanged(Orientation)}
 * <p> - row factory changes, {@link #onRowFactoryChanged()}
 * <p> - cell factory changes in columns, {@link #onCellFactoryChanged(VFXTableColumn)}
 * <p> - row height changes, {@link #onRowHeightChanged()}
 * <p> - columns size changes, {@link #onColumnsSizeChanged()} (specified by {@link VFXTable#columnsSizeProperty()})
 * <p> - layout mode changes {@link #onColumnsLayoutModeChanged()}
 * <p></p>
 * Last but not least, some of these computations may need to ensure the current vertical and horizontal positions are correct,
 * so that a valid state can be produced. To achieve this, {@link VFXTableHelper#invalidatePos()} is called.
 * However, invalidating the positions, also means that the {@link #onPositionChanged(Orientation)} method could be potentially
 * triggered, thus generating an unwanted 'middle' state. For this reason a special flag {@link #invalidatingPos} is set
 * to {@code true} before the invalidation, so that the other method will exit immediately. It's reset back to false
 * after the computation or if any of the checks before the actual computation fails.
 */
public class VFXTableManager<T> extends BehaviorBase<VFXTable<T>> {
	//================================================================================
	// Properties
	//================================================================================
	protected boolean invalidatingPos = false;
	protected boolean wasGeometryChange = false;

	//================================================================================
	// Constructors
	//================================================================================
	public VFXTableManager(VFXTable<T> table) {
		super(table);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * This core method is responsible for ensuring that the viewport always has the right number of columns, rows and cells.
	 * This is called every time the table's geometry changes (width/height depending on the orientation),
	 * which means that this is also responsible for initialization (when width/height becomes > 0.0).
	 * <p>
	 * After preliminary checks done by {@link #tableFactorySizeCheck()} and {@link #rangeCheck(IntegerRange, boolean, boolean)},
	 * (on the columns range) the computation for the new state is delegated to the
	 * {@link #moveReuseCreateAlgorithm(IntegerRange, IntegerRange, VFXTableState)}.
	 * <p></p>
	 * Note that to compute a valid new state, it is important to also validate the table's positions by invoking
	 * {@link VFXTableHelper#invalidatePos()}.
	 * <p></p>
	 * Note that this is also responsible for the last column to always fill the table, when the table's width changes
	 * and {@link VFXTableState#isLayoutNeeded()} is {@code false}, this will invoke {@link VFXTable#requestViewportLayout(VFXTableColumn)}
	 * with the last column as parameter.
	 */
	protected void onGeometryChanged(GeometryChangeType gct) {
		invalidatingPos = true;
		VFXTable<T> table = getNode();
		VFXTableHelper<T> helper = table.getHelper();
		if (!tableFactorySizeCheck()) return;

		// Ensure positions are correct!
		helper.invalidatePos();

		IntegerRange rowsRange = helper.rowsRange();
		IntegerRange columnsRange = helper.columnsRange();
		if (!rangeCheck(columnsRange, true, true)) return;

		// Compute the new state
		VFXTableState<T> newState = new VFXTableState<>(table, rowsRange, columnsRange);
		newState.setColumnsChanged(table.getState());
		moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);

		if (disposeCurrent()) newState.setRowsChanged(true);
		table.update(newState);
		invalidatingPos = false;

		if (gct == GeometryChangeType.WIDTH && !newState.isLayoutNeeded()) {
			wasGeometryChange = true;
			VFXTableColumn<T, ? extends VFXTableCell<T>> last = table.getColumns().getLast();
			table.requestViewportLayout(last);
			wasGeometryChange = false;
		}
	}

	/**
	 * Used in {@link ColumnsLayoutMode#VARIABLE} mode to call {@link VFXTable#requestViewportLayout(VFXTableColumn)}.
	 * Essentially, this should trigger a partial layout computation.
	 *
	 * @see VFXTableSkin#partialLayout()
	 */
	protected void onColumnWidthChanged(VFXTableColumn<T, ?> column) {
		VFXTable<T> table = getNode();
		if (table.getColumnsLayoutMode() == ColumnsLayoutMode.FIXED) return;
		table.requestViewportLayout(column);
	}

	/**
	 * This is responsible for handling changes in {@link VFXTable#getColumns()} as well as initializing the
	 * {@link VFXTableColumn#tableProperty()} of each column. A {@code null} parameter indicates that we want to just
	 * initialize the columns, and no change occurred. Removed columns will have both the table instance and index
	 * properties reset to {@code null} and -1 respectively.
	 * <p></p>
	 * Before starting the new state computation, we must make sure that the viewport position is valid by calling
	 * {@link VFXTableHelper#invalidatePos()}. Then we get both the columns and rows ranges by using
	 * {@link VFXTableHelper#columnsRange()} and {@link VFXTableHelper#rowsRange()}.
	 * If the column range is invalid, then we set the state to {@link VFXTableState#INVALID}, dispose the old one and exit
	 * immediately, all of this is done by {@link #rangeCheck(IntegerRange, boolean, boolean)}.
	 * <p>
	 * At this point, we can compute the new state. If the rows range is valid, we iterate over it and for each row we
	 * call {@link VFXTableRow#updateColumns(IntegerRange, boolean)} with {@code true} as parameter, thus we ensure
	 * that all the cells have the right cells.
	 * Finally, calls {@link VFXTable#update(VFXTableState)} to set the new state and trigger the layout computation.
	 */
	protected void onColumnsChanged(ListChangeListener.Change<? extends VFXTableColumn<T, ?>> change) {
		VFXTable<T> table = getNode();

		// Init
		if (change == null) {
			table.getColumns().forEach(c -> c.setTable(table));
			return;
		}

		// A setAll operation may end up adding the same columns as before (or even just some of them)
		// Which means that both wasRemoved and wasAdded computation will run, we don't want that here.
		// Simply handle removals after ensuring that a column that "was removed" is not still in the list
		Set<VFXTableColumn<T, ?>> rm = new HashSet<>();
		while (change.next()) {
			if (change.wasRemoved()) rm.addAll(change.getRemoved());
			if (change.wasAdded()) {
				for (VFXTableColumn<T, ?> c : change.getAddedSubList()) {
					if (rm.contains(c)) {
						rm.remove(c);
						continue;
					}
					c.setTable(table);
				}
			}
		}
		rm.forEach(c -> {
			c.setTable(null);
			c.setIndex(-1);
		});

		VFXTableHelper<T> helper = table.getHelper();
		invalidatingPos = true;
		helper.invalidatePos(); // Changes to the columns' list may invalidate the hPos

		// Compute the new ranges
		IntegerRange columnsRange = helper.columnsRange();
		IntegerRange rowsRange = helper.rowsRange();
		if (!rangeCheck(columnsRange, true, true)) {
			return; // If invalid (no columns), dispose current and set INVALID state
		}

		VFXTableState<T> state = table.getState();
		VFXTableState<T> newState = new VFXTableState<>(table, rowsRange, columnsRange, state.getRows());
		newState.setColumnsChanged(true);
		if (rangeCheck(rowsRange, false, false)) {
			for (Integer idx : rowsRange) {
				VFXTableRow<T> row = newState.getRows().get(idx);
				if (row == null) {
					row = helper.indexToRow(idx);
					newState.addRow(idx, row);
					newState.setRowsChanged(true);
				}
				row.updateColumns(columnsRange, true);
			}
		}
		table.update(newState);
		invalidatingPos = false;
	}

	/**
	 * Before describing the operations performed by this method, it's important for the reader to understand the difference
	 * between the two changes caught by this method. {@link VFXTable} makes use of a {@link ListProperty} to store
	 * the items to display. The property is essentially the equivalent of this {@code ObjectProperty<ObservableList>}. Now,
	 * if you are familiar with JavaFX, you probably know that there are two possible changes to listen to: one is changes
	 * to {@code ObjectProperty} (if the {@code ObservableList} instance changes), and the other are changes in the {@code ObservableList}
	 * instance itself. As you may guess, managing both these changes with a simple {@code ObjectProperty} is quite cumbersome,
	 * because you need two listeners: one that that catches changes in the list, and another to catch changes to the property.
	 * In particular, the latter has the task to add the first listener to the new {@code ObservableList} instance.
	 * <p>
	 * And here is where {@link ListProperty} comes in handy. By adding an {@link InvalidationListener} to this special
	 * property we are able to intercept both the type of changes always, even if the {@code ObservableList} instance changes,
	 * everything is handled automatically.
	 * <p>
	 * Needless to say, we use a {@code Property} to store the items to allow the usage of bindings!
	 * <p></p>
	 * This core method is responsible for updating the table's state when any of the two aforementioned changes happen.
	 * <p>
	 * These kind of updates are the most tricky and expensive. In particular, additions and removals can occur at any
	 * position in the list, which means that calculating the new state solely on the indexes is a no-go. It is indeed
	 * possible by, in theory, isolating the indexes at which the changes occurred, separating the rows that need only
	 * an index update from the ones that actually need a full update. However, such an approach requires a lot of code,
	 * is error-prone, and a bit heavy on performance. The new approach implemented here requires changes to the state
	 * class as well, {@link VFXTableState}.
	 * <p>
	 * The computation for the new state is similar to the {@link #moveReuseCreateAlgorithm(IntegerRange, IntegerRange, VFXTableState)},
	 * but the first step, which tries to identify the common cells, is quite different. You see, as I said before, additions
	 * and removals can occur at any place in the list. Picture it with this example:
	 * <pre>
	 * {@code
	 * In list before: 0 1 2 3 4 5
	 * Add at index 2 these items: 99, 98
	 * In list after: 0 1 99 98 2 3 4 5
	 *
	 * Now let's suppose the range of displayed items is the same: [0, 5] (6 items)
	 * (I'm going now to write items with the index too, like this Index:Item)
	 * Items before: [0:0, 1:1, 2:2, 3:3, 4:4, 5:5]
	 * Items after: [0:0, 1:1, 2:99, 3:98, 4:2, 5:3]
	 * See? Items 2 and 3 are still there but in a different position (index) Since we assume item updates are more
	 * expensive than index updates, we must ensure to take those two rows and update them just by index
	 * }
	 * </pre>
	 * <p></p>
	 * For this reason, rows from the old state are not removed by index, but by <b>item</b>,
	 * {@link VFXTableState#removeRow(Object)}. First, we retrieve the item from the list that is now at index i
	 * (this index comes from the loop on the range), then we try to remove the row for this item from the old state.
	 * If the row is found, we update it by index and add it to the new state. Note that the index is also excluded from the range.
	 * <p>
	 * Now that 'common' rows have been properly updated, the remaining items are processed by the
	 * {@link #remainingAlgorithm(ExcludingRange, VFXTableState)}.
	 * <p></p>
	 * <p> 1) This is one of those methods that to produce a valid new state needs to validate the table's positions,
	 * so it calls {@link VFXTableHelper#invalidatePos()}
	 * <p> 2) To make sure the layout is always correct, at the end we always invoke {@link VFXTable#requestViewportLayout()}.
	 * You can guess why from the above example, items 2 and 3 are still in the viewport, but at different indexes,
	 * which also means at different layout positions. There is no easy way to detect this, so better safe than sorry,
	 * always update the layout.
	 */
	protected void onItemsChanged() {
		invalidatingPos = true;
		VFXTable<T> table = getNode();
		VFXTableHelper<T> helper = table.getHelper();

		helper.invalidatePos();

		// If the table is now empty, then set empty state
		if (!tableFactorySizeCheck()) return;

		// Compute rows ranges and new state
		VFXTableState<T> current = table.getState();
		IntegerRange rowsRange = helper.rowsRange();
		ExcludingRange eRange = ExcludingRange.of(rowsRange);
		VFXTableState<T> newState = new VFXTableState<>(table, rowsRange, current.getColumnsRange());

		// First update by index
		for (Integer idx : rowsRange) {
			T item = helper.indexToItem(idx);
			VFXTableRow<T> row = current.removeRow(item);
			if (row != null) {
				eRange.exclude(idx);
				row.updateIndex(idx);
				newState.addRow(idx, item, row);
			}
		}

		// Process remaining with the "remaining" algorithm
		remainingAlgorithm(eRange, newState);

		if (disposeCurrent()) newState.setRowsChanged(true);
		table.update(newState);
		if (!newState.haveRowsChanged()) table.requestViewportLayout();
		invalidatingPos = false;
	}

	/**
	 * This core method is responsible for updating the table's state when the vertical and horizontal positions change.
	 * Since the table doesn't use any throttling technique to limit the number of events/changes,
	 * and since scrolling can happen very fast, performance here is crucial.
	 * <p>
	 * Immediately exits if: the special flag {@link #invalidatingPos} is true or the current state is {@link VFXTableState#INVALID}.
	 * Many other computations here need to validate the positions by calling {@link VFXTableHelper#invalidatePos()},
	 * to ensure that the resulting state is valid.
	 * However, invalidating the positions may trigger this method, causing two or more state computations to run at the
	 * 'same time'; this behavior must be avoided, and that flag exists specifically for this reason.
	 * <p></p>
	 * Before further discussing the internal mechanisms of this method, notice that this accepts a parameter of type
	 * {@link Orientation}. The reason is simple. The table is virtualized on both the x-axis and y-axis, but changes
	 * are 'atomic', meaning that only one can be processed at a time. For the scroll it's the same.
	 * If you imagine scroll on a timeline, each event occurs after the other. Even if you scroll in both directions at the same time,
	 * there's a difference between what you perceive and what happens under the hood. For this reason, to also
	 * avoid code duplication, that parameter tells this method on which axis the scroll happened.
	 * <p>
	 * The state computation changes depending on the {@link Orientation} parameter.
	 * <p>
	 * <b>Horizontal</b>
	 * <p>
	 * Requests the layout computation through {@link VFXTable#requestViewportLayout()} and exits immediately if the layout
	 * mode is set to {@link ColumnsLayoutMode#VARIABLE}. In such mode, the columns range will never change, but the
	 * layout is still required.
	 * <p>
	 * Does nothing and exits if the new columns range is the same as the old one.
	 * <p>
	 * The new state is computed by simply copying all the rows from the old state and just calling
	 * {@link VFXTableRow#updateColumns(IntegerRange, boolean)} on each of them.
	 * <p></p>
	 * <b>Vertical</b>
	 * <p>
	 * Does nothing and exits if the new rows range is the same as the old one.
	 * <p>
	 * The computation is delegated to the {@link #moveReuseCreateAlgorithm(IntegerRange, IntegerRange, VFXTableState)}
	 * algorithm.
	 */
	protected void onPositionChanged(Orientation axis) {
		if (invalidatingPos) return;
		VFXTable<T> table = getNode();
		VFXTableState<T> state = table.getState();
		if (state == VFXTableState.INVALID) return;

		VFXTableHelper<T> helper = table.getHelper();
		IntegerRange columnsRange = helper.columnsRange();

		// If the scroll was alongside the x-axis, then the columns range may change
		// We have two cases here: layout mode fixed and variable.
		// However, in variable mode the range is always the same, so no update in the rows,
		// but we still update the layout because of the "partial layout" feature.
		if (axis == Orientation.HORIZONTAL) {
			if (table.getColumnsLayoutMode() == ColumnsLayoutMode.VARIABLE) {
				table.requestViewportLayout();
				return;
			}

			// If the range didn't change, don't update
			if (state.getColumnsRange().equals(columnsRange)) return;

			// Here rather than moving the rows, we use the same map of the old state and just tell the rows to update
			VFXTableState<T> newState = new VFXTableState<>(table, state.getRowsRange(), columnsRange, state.getRows());
			newState.setColumnsChanged(true);
			state.getRowsByIndex().values().forEach(r -> r.updateColumns(columnsRange, false));
			table.update(newState);
			return;
		}

		// If the scroll was alongside the y-axis, then we use the classic moveReuseCreate algorithm
		// If the range didn't change, then do nothing
		IntegerRange rowsRange = helper.rowsRange();
		if (state.getRowsRange().equals(rowsRange)) return;
		VFXTableState<T> newState = new VFXTableState<>(table, rowsRange, columnsRange);
		moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);

		if (disposeCurrent()) newState.setRowsChanged(true);
		table.update(newState);
		if (!newState.haveRowsChanged()) table.requestViewportLayout();
	}

	/**
	 * This method is responsible for updating the table's state when the {@link VFXTable#rowFactoryProperty()} changes.
	 * Before proceeding, it checks whether a new state can be generated by using {@link #tableFactorySizeCheck()}, if not
	 * the rows' cache given by {@link VFXTable#getCache()} is cleared.
	 * <p>
	 * If the old state is valid and not empty, then we can optimize the algorithm by copying each of the old rows' state
	 * to the corresponding new ones. This is possible because ranges cannot change by simply switching the factory.
	 * For each index in the rows range, a new row is created and its state is set to the one at the same index in the
	 * old state by using {@link VFXTableRow#copyState(VFXTableRow)}.
	 * <p>
	 * Otherwise, if the old state has no rows from which copy the state, then it calls both {@link VFXTableRow#updateIndex(int)}
	 * and {@link VFXTableRow#updateColumns(IntegerRange, boolean)}.
	 * <p>
	 * Finally, the old state is disposed, {@link VFXTableState#dispose()}, the rows' cache is cleared (old rows cannot
	 * be used since the factory changed), and the table updated with the new state.
	 */
	protected void onRowFactoryChanged() {
		VFXTable<T> table = getNode();
		VFXTableState<T> state = table.getState();

		// First check basic properties to ensure we can generate a valid state
		if (!tableFactorySizeCheck()) {
			// Make sure to also invalidate the cache!
			table.getCache().clear();
			return;
		}

		// At this point, we can generate a new state
		VFXTableHelper<T> helper = table.getHelper();
		Function<T, VFXTableRow<T>> rf = table.getRowFactory();
		IntegerRange rowsRange = helper.rowsRange();
		IntegerRange columnsRange = helper.columnsRange();

		VFXTableState<T> newState = new VFXTableState<>(table, rowsRange, columnsRange);
		newState.setRowsChanged(true);

		// Iterate over the rows range and generate a row with the new factory for each index/item.
		// The new rows will copy the state of the previous row at the same index (expect if the old state is INVALID or empty)
		for (Integer idx : rowsRange) {
			T item = table.getItems().get(idx);
			VFXTableRow<T> row = rf.apply(item);
			if (state != VFXTableState.INVALID && !state.isEmpty()) {
				row.copyState(state.getRows().get(idx));
			} else {
				row.updateIndex(idx);
				row.updateColumns(columnsRange, false);
			}
			newState.addRow(idx, item, row);
		}

		disposeCurrent();
		table.getCache().clear();
		table.update(newState);
	}

	/**
	 * This method should be called by {@link VFXTableColumn}s when their cell factory changes.
	 * <p>
	 * For each row in the current state, this calls {@link VFXTableRow#replaceCells(VFXTableColumn)} which makes
	 * the operation as efficient as possible.
	 * <p>
	 * This is one of those methods that do not really change the table's state, rather it updates the rows' state,
	 * see {@link VFXTableState}. If the replacement was done, then the table's state is set to a clone of the current one.
	 */
	protected void onCellFactoryChanged(VFXTableColumn<T, VFXTableCell<T>> column) {
		VFXTable<T> table = getNode();
		VFXTableState<T> state = table.getState();
		boolean updated = false;
		for (VFXTableRow<T> row : state.getRowsByIndex().values()) {
			updated = row.replaceCells(column) || updated; // Order here is crucial because || operator is 'short circuit'
		}
		if (updated) table.update(state.clone());
	}

	/**
	 * This method is responsible for computing a new state when the {@link VFXTable#rowsHeightProperty()} changes.
	 * We could say that this is essentially equal to changing the cells' height.
	 * <p>
	 * After preliminary checks done by {@link #tableFactorySizeCheck()}, the computation for the new state
	 * is delegated to the {@link #intersectionAlgorithm()}.
	 * <p></p>
	 * Note that to compute a valid new state, it is important to also validate the table's positions by invoking
	 * {@link VFXTableHelper#invalidatePos()}.
	 */
	protected void onRowHeightChanged() {
		invalidatingPos = true;
		VFXTable<T> table = getNode();
		VFXTableHelper<T> helper = table.getHelper();

		// Ensure positions are correct
		helper.invalidatePos();

		if (!tableFactorySizeCheck()) return;

		// Compute the new state with the intersection algorithm
		VFXTableState<T> newState = intersectionAlgorithm();

		if (disposeCurrent()) newState.setRowsChanged(true);
		table.update(newState);
		invalidatingPos = false;
	}

	/**
	 * This method is responsible for computing a new state when the {@link VFXTable#columnsSizeProperty()} changes.
	 * Since the property specifies both the width and height of the columns, both columns and rows ranges can vary.
	 * <p>
	 * First, it checks if the new columns range is valid by using {@link #rangeCheck(IntegerRange, boolean, boolean)}.
	 * Then it checks if the rows range changed, and if that's the case, delegates the update to {@link #onGeometryChanged(GeometryChangeType)}.
	 * <p>
	 * Otherwise, we can reuse the rows from the old state, and only if the columns range has changed, we also update them
	 * by calling {@link VFXTableRow#updateColumns(IntegerRange, boolean)} on each of them.
	 * <p>
	 * In any case, this will trigger a layout computation, since columns and rows may need to be resized/repositioned.
	 * <p></p>
	 * This is one of those methods that to produce a valid new state needs to validate the table's positions,
	 * so it calls {@link VFXTableHelper#invalidatePos()}
	 */
	protected void onColumnsSizeChanged() {
		invalidatingPos = true;
		VFXTable<T> table = getNode();
		VFXTableHelper<T> helper = table.getHelper();

		// I believe such change could potentially mess with positions, even if ranges do not change
		// So, just to make sure, let's invalidate them
		helper.invalidatePos();

		// Get the current state, as well as both the rows and columns ranges
		VFXTableState<T> state = table.getState();
		IntegerRange rowsRange = helper.rowsRange();
		IntegerRange columnsRange = helper.columnsRange();

		// Before proceeding, make sure to check the ranges are valid
		// This essentially also checks that the current state is not INVALID
		if (!rangeCheck(columnsRange, true, true)) return;

		// Three possible cases
		// 1) The rows range changed: delegate to geometry change
		// 2) The columns range changed: update the rows and create a new state with the same rows map from the old one,
		//    but the columnsChanged flag set to true
		// 3) Neither of the two ranges changed, we still need to trigger the layout so that columns, rows and cells are
		//    positioned and sized correctly
		if (!rowsRange.equals(state.getRowsRange())) {
			onGeometryChanged(GeometryChangeType.OTHER);
			return;
		}
		if (!columnsRange.equals(state.getColumnsRange())) {
			for (VFXTableRow<T> row : state.getRowsByIndex().values()) {
				row.updateColumns(columnsRange, false);
			}
			VFXTableState<T> newState = new VFXTableState<>(table, rowsRange, columnsRange, state.getRows());
			newState.setColumnsChanged(true);
			table.update(newState);
			return;
		}
		table.requestViewportLayout();
	}

	/**
	 * This is responsible for updating the table's state when the {@link VFXTable#columnsLayoutModeProperty()} changes.
	 * <p>
	 * The algorithm is almost the same for both cases: [FIXED -> VARIABLE], [VARIABLE -> FIXED].
	 * The only thing that may change is the columns range, so, on each of the rows from the old state this calls
	 * {@link VFXTableRow#updateColumns(IntegerRange, boolean)}. The new state is almost a copy of the old one except for
	 * the columns range.
	 * <p>
	 * There are three extra steps when it's switching from VARIABLE to FIXED mode:
	 * <p> 1) Before updating the rows, we need to validate the horizontal position by using {@link VFXTableHelper#invalidatePos()}.
	 * Also, since in VARIABLE mode columns and cells may be hidden to enhance performance, this also resets all columns' visibility.
	 * <p> 2) When updating the rows we also reset every cell's visibility.
	 * <p> 3) At the end of the computation, if {@link VFXTableState#isLayoutNeeded()} returns false, we still trigger
	 * a layout computation with {@link VFXTable#requestViewportLayout()}.
	 */
	protected void onColumnsLayoutModeChanged() {
		VFXTable<T> table = getNode();
		VFXTableHelper<T> helper = table.getHelper();
		VFXTableState<T> current = table.getState();

		// Only when the mode switches from VARIABLE to FIXED, we must invalidate the hPos
		ColumnsLayoutMode newMode = table.getColumnsLayoutMode();
		if (newMode == ColumnsLayoutMode.FIXED) {
			invalidatingPos = true;
			table.getColumns().forEach(c -> c.setVisible(true));
			helper.invalidatePos();
		}

		// For both cases (VARIABLE -> FIXED, FIXED -> VARIABLE) we have to do the same exact update.
		// What may change when switching modes is having a different columns range and maybe an invalid hPos (see above).
		// The estimated height, the rows range and the vPos are valid, which means that we can simply create a new state
		// which uses all the rows from the current state and just update them with the new columns range.
		IntegerRange columnsRange = helper.columnsRange();
		VFXTableState<T> newState = new VFXTableState<>(table, current.getRowsRange(), columnsRange, current.getRows());
		newState.getRowsByIndex().values().forEach(r -> {
			if (newMode == ColumnsLayoutMode.FIXED)
				r.getCellsByIndex().values().forEach(c -> c.toNode().setVisible(true));
			r.updateColumns(columnsRange, false);
		});
		newState.setColumnsChanged(current);

		table.update(newState);
		if (newMode == ColumnsLayoutMode.FIXED && !newState.isLayoutNeeded()) table.requestViewportLayout();
		invalidatingPos = false;
	}

	//================================================================================
	// Common
	//================================================================================

	/**
	 * Avoids code duplication. Typically used when, while iterating on the rows and columns ranges,
	 * it's enough to move the rows from the current state to the new state. For indexes which are not found
	 * in the current state, a new row is either taken from the old state, taken from cache or created by the row factory.
	 * <p>
	 * (The last operations are delegated to the {@link #remainingAlgorithm(ExcludingRange, VFXTableState)}).
	 * <p>
	 * Note that the columns range parameter is only needed to ensure each row is displaying the correct cells by invoking
	 * {@link VFXTableRow#updateColumns(IntegerRange, boolean)}. We don't know when this is needed and when not, we simply
	 * do it always and delegate to the method the check (if the given range is not equal to the old one then update).
	 *
	 * @see VFXTableHelper#indexToRow(int)
	 * @see VFXTable#rowFactoryProperty()
	 */
	protected void moveReuseCreateAlgorithm(IntegerRange rowsRange, IntegerRange columnsRange, VFXTableState<T> newState) {
		if (Utils.INVALID_RANGE.equals(rowsRange)) return;
		VFXTable<T> table = getNode();
		VFXTableState<T> current = table.getState();
		ExcludingRange eRange = ExcludingRange.of(rowsRange);
		if (!current.isEmpty()) {
			for (Integer idx : rowsRange) {
				VFXTableRow<T> row = current.removeRow(idx);
				if (row == null) continue;
				eRange.exclude(idx);
				row.updateColumns(columnsRange, false); // This will always be called! To the row checking if the update is actually needed
				newState.addRow(idx, row);
			}
		}
		remainingAlgorithm(eRange, newState);
	}

	/**
	 * Avoids code duplication. Typically used in situations where the previous rows range and the new one are likely to be
	 * very close, but most importantly, that do not involve any change in the items' list.
	 * In such cases, the computation for the new state is divided in two parts:
	 * <p> 0) Prerequisites: the new rows range [min, max], the excluding range (a helper class to keep track of common rows),
	 * the current state, and the intersection between the current rows range and the new rows range
	 * <p> 1) The intersection allows us to distinguish between rows that can be moved as they are, without any update,
	 * from the current state to the new one. For this, it's enough to check that the intersection range is valid, and then
	 * a for loop. Common indexes are also excluded from the range!
	 * <p> 2) The remaining indexes are items that are new. Which means that if there are still rows
	 * in the current state, they need to be updated (both index, item, and maybe columns range too).
	 * Otherwise, new ones are created by the row factory.
	 * <p></p>
	 * <p> - See {@link Utils#intersection}: used to find the intersection between two ranges
	 * <p> - See {@link #rangeCheck(IntegerRange, boolean, boolean)}: used to validate the intersection range, both parameters
	 * are false!
	 * <p> - See {@link #remainingAlgorithm(ExcludingRange, VFXTableState)}: the second part of the algorithm is delegated to this
	 * method
	 *
	 * @see ExcludingRange
	 */
	protected VFXTableState<T> intersectionAlgorithm() {
		VFXTable<T> table = getNode();
		VFXTableHelper<T> helper = table.getHelper();

		// New range
		IntegerRange rowsRange = helper.rowsRange();
		ExcludingRange eRange = ExcludingRange.of(rowsRange);

		// Current and new states, intersection between current and new range
		VFXTableState<T> current = table.getState();
		VFXTableState<T> newState = new VFXTableState<>(table, rowsRange, current.getColumnsRange());
		IntegerRange intersection = Utils.intersection(current.getRowsRange(), rowsRange);

		// If range valid, move common rows from current to new state. Also, exclude common indexes
		if (rangeCheck(intersection, false, false)) {
			for (Integer common : intersection) {
				newState.addRow(common, current.removeRow(common));
				eRange.exclude(common);
			}
		}

		// Process remaining with the "remaining' algorithm"
		remainingAlgorithm(eRange, newState);
		return newState;
	}

	/**
	 * Avoids code duplication. Typically used to process indexes not found in the current state.
	 * <p>
	 * For any index in the given collection, a row is needed. Also, it needs to be updated by index, item and
	 * maybe columns range too.
	 * This row can come from three sources:
	 * <p> 1) from the current state if it's not empty yet. Since the rows are stored in a {@link SequencedMap}, one
	 * is removed by calling {@link StateMap#pollFirst()}.
	 * <p> 2) from the {@link VFXCellsCache} if not empty (here {@link VFXTable#getCache()})
	 * <p> 3) created by the row factory
	 * <p></p>
	 * <p> - See {@link VFXTableHelper#indexToRow(int)}: this handles the second and third cases. If a row can
	 * be taken from the cache, automatically updates its item then returns it. Otherwise, invokes the
	 * {@link VFXTable#rowFactoryProperty()} to create a new one
	 * <p></p>
	 * After a row is retrieved from any of the three sources, this calls {@link VFXTableRow#updateColumns(IntegerRange, boolean)}
	 * to ensure it is displaying the correct cells.
	 */
	protected void remainingAlgorithm(ExcludingRange eRange, VFXTableState<T> newState) {
		VFXTable<T> table = getNode();
		VFXTableHelper<T> helper = table.getHelper();
		VFXTableState<T> current = table.getState();

		// Indexes in the given set were not found in the current state.
		// Which means item updates. Rows are retrieved either from the current state (if not empty), from the cache,
		// or created from the factory
		for (Integer idx : eRange) {
			T item = helper.indexToItem(idx);
			VFXTableRow<T> row;
			if (!current.isEmpty()) {
				row = current.getRows().pollFirst().getValue();
				row.updateIndex(idx);
				row.updateItem(item);
			} else {
				row = helper.itemToRow(item);
				row.updateIndex(idx);
				newState.setRowsChanged(true);
			}
			row.updateColumns(newState.getColumnsRange(), false); // This needs to be done for new rows as well!
			newState.addRow(idx, item, row);
		}
	}

	/**
	 * Avoids code duplication. This method checks for six things:
	 * <p> 1) If the columns' list is empty
	 * <p> 2) If the items' list is empty
	 * <p> 3) If the row factory is {@code null}
	 * <p> 4) If the rows' height is lesser or equal to 0
	 * <p> 5) If the table's width is lesser or equal to 0
	 * <p> 6) If the table's height is lesser or equal to 0
	 * <p>
	 * If any of those checks is true: the table's state is set to {@link #computeInvalidState()}, the
	 * current state is disposed, the 'invalidatingPos' flag is reset, finally returns false.
	 * <p>
	 * Otherwise, does nothing and returns true.
	 * <p></p>
	 * <p> - See {@link VFXTable#rowFactoryProperty()}
	 * <p> - See {@link VFXTable#rowsHeightProperty()}
	 * <p> - See {@link #disposeCurrent()}: for the current state disposal
	 *
	 * @return whether all the aforementioned checks have passed
	 */
	protected boolean tableFactorySizeCheck() {
		VFXTable<T> table = getNode();
		if (table.getColumns().isEmpty() ||
			table.isEmpty() ||
			table.getRowFactory() == null ||
			table.getRowsHeight() <= 0 ||
			table.getWidth() <= 0 ||
			table.getHeight() <= 0) {
			disposeCurrent();
			table.update(computeInvalidState());
			invalidatingPos = false;
			return false;
		}
		return true;
	}

	/**
	 * Avoids code duplication. Used to check whether the given range is valid, not equal to {@link Utils#INVALID_RANGE}.
	 * <p>
	 * When invalid, returns false, but first runs the following operations: disposes the current state (only if the
	 * 'dispose' parameter is true), sets the table's state to {@link VFXTableState#INVALID} (only if the 'update'
	 * parameter is true), resets the 'invalidatingPos' flag.
	 * Otherwise, does nothing and returns true.
	 * <p>
	 * Last but not least, this is a note for the future on why the method is structured like this. It's crucial for
	 * the disposal operation to happen <b>before</b> the table's state is set to {@link VFXTableState#INVALID}, otherwise
	 * the disposal method will fail, since it will then retrieve the empty state instead of the correct one.
	 * <p></p>
	 * <p> - See {@link #disposeCurrent()}: for the current state disposal
	 *
	 * @param range   the range to check
	 * @param update  whether to set the table's state to 'empty' if the range is not valid
	 * @param dispose whether to dispose the current/old state if the range is not valid
	 * @return whether the range is valid or not
	 */
	@SuppressWarnings("unchecked")
	protected boolean rangeCheck(IntegerRange range, boolean update, boolean dispose) {
		VFXTable<T> table = getNode();
		if (Utils.INVALID_RANGE.equals(range)) {
			if (dispose) disposeCurrent();
			if (update) table.update(VFXTableState.INVALID);
			invalidatingPos = false;
			return false;
		}
		return true;
	}

	/**
	 * Avoids code duplication. Responsible for disposing the current state if it is not empty.
	 * <p></p>
	 * <p> - See {@link VFXTableState#dispose()}
	 *
	 * @return whether the disposal was done or not
	 */
	protected boolean disposeCurrent() {
		VFXTableState<T> state = getNode().getState();
		if (!state.isEmpty()) {
			state.dispose();
			return true;
		}
		return false;
	}

	/**
	 * This method is responsible for properly compute an invalid {@link VFXTableState} depending on certain conditions.
	 * You see, the table is a special component also because it can technically work even if there are no items in it.
	 * The {@link VFXTableState#INVALID} state is to be used only if there are no columns in the table, or in general if
	 * {@link VFXTableHelper#columnsRange()} returns {@link Utils#INVALID_RANGE}.
	 * <p>
	 * Otherwise, this will return a new state with {@link Utils#INVALID_RANGE} as the rows range,
	 * and {@link VFXTableHelper#columnsRange()} as the columns range.
	 * <p>
	 * Note that this new state will have its {@link VFXTableState#haveRowsChanged()} and {@link VFXTableState#haveColumnsChanged()}
	 * flags set depending on the old state.
	 */
	@SuppressWarnings("unchecked")
	protected VFXTableState<T> computeInvalidState() {
		VFXTable<T> table = getNode();
		VFXTableHelper<T> helper = table.getHelper();
		IntegerRange columnsRange = helper.columnsRange();
		if (Utils.INVALID_RANGE.equals(columnsRange)) return VFXTableState.INVALID;

		VFXTableState<T> partial = new VFXTableState<>(table, Utils.INVALID_RANGE, columnsRange);
		partial.setColumnsChanged(table.getState());
		partial.setRowsChanged(!table.getState().isEmpty());
		return partial;
	}
}
