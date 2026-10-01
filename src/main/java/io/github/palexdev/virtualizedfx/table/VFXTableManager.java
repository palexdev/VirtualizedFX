/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
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

import io.github.palexdev.mfxcore.base.beans.range.ExcludingIntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.virtualizedfx.enums.GeometryChangeType;
import io.github.palexdev.virtualizedfx.properties.CellFactory;
import io.github.palexdev.virtualizedfx.utils.Utils;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;

import static io.github.palexdev.mfxcore.observables.When.onChanged;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;
import static io.github.palexdev.virtualizedfx.utils.Utils.INVALID_RANGE;
import static java.util.Objects.requireNonNull;

/// The table's 'manager', the piece that reacts to changes in [VFXTable] and produces a new [VFXTableState] for each
/// of them. It's created by [VFXTable#createManager()] when the table is built, [#install()] registers the listeners
/// it needs, and it lives as long as the table does.
///
/// ## What it reacts to
///
/// - geometry changes (width, height and the buffer sizes), [#onGeometryChanged(GeometryChangeType)]
/// - position changes, [#onPositionChanged(Orientation)]
/// - items changes, [#onItemsChanged()]
/// - changes in the columns' list, [#onColumnsChanged(int)]
/// - the columns' size, [#onColumnsSizeChanged()], and the fill policy, [#onFillPolicyChanged()]
/// - a column's width, [#onColumnResized(VFXTableColumn)], and a column's weight, [#onColumnWeightChanged(VFXTableColumn)]
/// - the rows' height, [#onRowsHeightChanged()]
/// - the rows factory, [#onRowsFactoryChanged()], and a column's cell factory, [#onCellFactoryChanged(VFXTableColumn)]
///
/// Not all of them come from a listener registered here. The columns' list is watched by the table itself, while a
/// column's width, weight and cell factory are pushed by [VFXTableColumn].
///
/// The changes that can affect a column's width or position go to the table's [ColumnsLayoutCache] first, so that
/// the ranges and the layout are computed on up to date values.
///
/// ## How a state is produced
///
/// Most of these computations follow the same skeleton. Make sure the positions are valid. Check that a state can be
/// produced at all, [#tableFactorySizeCheck()] and [#rangeCheck(IntegerRange,boolean,boolean)]. Ask the helper for
/// the two ranges. Fill the new state with one of the three algorithms below. Hand it to the table, which also
/// schedules the layout, [VFXTable#updateState(VFXTableState)] and [VFXTable#updateState(VFXTableState,IntegerRange)].
///
/// The three algorithms are [#moveReuseCreateAlgorithm(IntegerRange,IntegerRange,VFXTableState)],
/// [#intersectionAlgorithm(IntegerRange,VFXTableState)] and
/// [#remainingAlgorithm(ExcludingIntegerRange,VFXTableState)]. They all work on rows, since the cells are the rows'
/// business, see [VFXTableRow].
///
/// ## Positions, and the flag that guards them
///
/// Several computations need the positions to be valid before they can produce a state, so they call
/// [#invalidatePos()]. The catch is that clamping a position fires a change, which would run
/// [#onPositionChanged(Orientation)] right in the middle of another computation and produce an unwanted 'middle'
/// state. A flag is set for the duration of the invalidation, and that method exits immediately while it is set. It's
/// also reset by the two checks above, since they can leave the computation early.
public class VFXTableManager<T> {

    //================================================================================
    // Properties
    //================================================================================

    private final VFXTable<T> table;
    private boolean invalidatingPos = false;

    //================================================================================
    // Constructors
    //================================================================================

    public VFXTableManager(VFXTable<T> table) {
        this.table = table;
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Registers all the listeners on the table's properties. Called once, when the table builds this manager with
    /// [VFXTable#createManager()].
    protected void install() {
        // Geometry
        onInvalidated(table.widthProperty()).then(_ -> onGeometryChanged(GeometryChangeType.WIDTH)).listen();
        onInvalidated(table.heightProperty()).then(_ -> onGeometryChanged(GeometryChangeType.HEIGHT)).listen();
        onInvalidated(table.columnsBufferSizeProperty()).then(_ -> onGeometryChanged(GeometryChangeType.OTHER)).listen();
        onInvalidated(table.rowsBufferSizeProperty()).then(_ -> onGeometryChanged(GeometryChangeType.OTHER)).listen();
        // Position
        onInvalidated(table.hPosProperty()).then(_ -> onPositionChanged(Orientation.HORIZONTAL)).listen();
        onInvalidated(table.vPosProperty()).then(_ -> onPositionChanged(Orientation.VERTICAL)).listen();
        // Others
        onInvalidated(table.itemsProperty()).then(_ -> onItemsChanged()).listen();
        onChanged(table.columnsSizeProperty()).then((_, _) -> {
            layoutCache().onColumnsSizeChanged();
            onColumnsSizeChanged();
        }).listen();
        onInvalidated(table.columnsFillPolicyProperty()).then(_ -> {
            layoutCache().onFillPolicyChanged();
            onFillPolicyChanged();
        }).listen();
        onInvalidated(table.rowsHeightProperty()).then(_ -> onRowsHeightChanged()).listen();
        onInvalidated(table.rowsFactoryProperty()).then(_ -> onRowsFactoryChanged()).listen();
    }

    /// This core method is responsible for making sure the viewport always has the right number of columns, rows and
    /// cells. Since it runs on width and height changes, it's also the one that initializes the table, when the sizes
    /// become greater than 0. The buffer sizes take this path too.
    ///
    /// On a width change the layout cache comes first, [ColumnsLayoutCache#onTableWidthChanged()]: the leftover width is
    /// redistributed as specified by the [VFXTable#columnsFillPolicyProperty()], and the index it gives back is the
    /// first column that ends up somewhere else because of it.
    ///
    /// After the preliminary checks, [#tableFactorySizeCheck()] and [#rangeCheck(IntegerRange,boolean,boolean)] on the
    /// columns range, the new state is computed by
    /// [#moveReuseCreateAlgorithm(IntegerRange,IntegerRange,VFXTableState)].
    ///
    /// Only a part of the viewport needs to be laid out afterwards: the columns that entered the range, plus everything
    /// from the redistribution's first column rightwards when the width changed.
    protected void onGeometryChanged(GeometryChangeType gct) {
        VFXTableHelper<T> helper = helper();

        int fillFrom = gct == GeometryChangeType.WIDTH ? layoutCache().onTableWidthChanged() : -1; // redistribute leftover width
        invalidatePos(); // Ensure positions are correct before potentially producing an empty state!
        if (!tableFactorySizeCheck()) return;

        IntegerRange rowsRange = helper.rowsRange();
        IntegerRange columnsRange = helper.columnsRange();
        if (!rangeCheck(columnsRange, true, true)) return;

        // Compute the new state
        VFXTableState<T> newState = new VFXTableState<>(table, rowsRange, columnsRange);
        newState.setColumnsChanged(state());
        moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);

        IntegerRange interval = Utils.difference(columnsRange, state().getColumnsRange());
        if (fillFrom >= 0) interval = INVALID_RANGE.equals(interval) ?
            IntegerRange.of(fillFrom, Integer.MAX_VALUE) :
            IntegerRange.of(Math.min(interval.getMin(), fillFrom), Integer.MAX_VALUE);

        if (disposeCurrent()) newState.setRowsChanged(true);
        table.updateState(newState, interval);
    }

    /// This core method is responsible for updating the table's state when the vertical or horizontal position change.
    /// The table doesn't throttle those changes in any way, and scrolling can happen very fast, so performance here is
    /// crucial.
    ///
    /// Exits immediately if the positions are being invalidated by another computation (see the class docs) or if the
    /// current state is [VFXTableState#INVALID].
    ///
    /// The table is virtualized on both axes, but changes are 'atomic', only one can be processed at a time. Even when
    /// you scroll in both directions at once, under the hood the events come one after the other. The [Orientation]
    /// parameter tells on which axis the scroll happened, which also avoids duplicating this code twice.
    ///
    /// #### Horizontal
    ///
    /// Does nothing if the new columns range is the same as the old one. This early exit is what keeps horizontal
    /// scrolling cheap, since most events do not move the window at all.
    ///
    /// Otherwise the new state copies the rows of the current one and calls
    /// [VFXTableRow#updateColumns(IntegerRange,boolean)] on each of them. Only the columns that entered the range are
    /// laid out.
    ///
    /// #### Vertical
    ///
    /// Does nothing if the new rows range is the same as the old one, otherwise the computation is delegated to
    /// [#moveReuseCreateAlgorithm(IntegerRange,IntegerRange,VFXTableState)]. No column moves here, so the layout is
    /// asked to reposition the rows only.
    protected void onPositionChanged(Orientation axis) {
        if (invalidatingPos) return;
        VFXTableState<T> state = state();
        if (state == VFXTableState.INVALID) return;

        VFXTableHelper<T> helper = helper();
        if (axis == Orientation.HORIZONTAL) {
            IntegerRange columnsRange = helper.columnsRange();
            if (state.getColumnsRange().equals(columnsRange)) return;

            VFXTableState<T> newState = new VFXTableState<>(table, state.getRowsRange(), columnsRange, state.getRows());
            newState.setColumnsChanged(true);
            newState.getRowsByIndex().values().forEach(r -> r.updateColumns(columnsRange, false));
            table.updateState(newState, Utils.difference(columnsRange, state.getColumnsRange()));
            return;
        }

        IntegerRange rowsRange = helper.rowsRange();
        if (state.getRowsRange().equals(rowsRange)) return;

        VFXTableState<T> newState = new VFXTableState<>(table, rowsRange, state.getColumnsRange());
        moveReuseCreateAlgorithm(rowsRange, newState.getColumnsRange(), newState);

        if (disposeCurrent()) newState.setRowsChanged(true);
        table.updateState(newState, INVALID_RANGE);
    }

    /// This method is responsible for computing a new state when the [VFXTable#rowsHeightProperty()] changes. You could
    /// say it's the equivalent of changing the cells' height.
    ///
    /// After the check done by [#tableFactorySizeCheck()], the computation is delegated to
    /// [#intersectionAlgorithm(IntegerRange,VFXTableState)]. The columns range cannot change here, so it's taken from
    /// the current state.
    ///
    /// The whole viewport is laid out at the end, for obvious reasons, even when the rows themselves didn't change.
    protected void onRowsHeightChanged() {
        VFXTableHelper<T> helper = helper();

        invalidatePos(); // Ensure positions are correct before potentially producing an empty state!
        if (!tableFactorySizeCheck()) return;

        IntegerRange rowsRange = helper.rowsRange();
        IntegerRange columnsRange = state().getColumnsRange();
        if (!rangeCheck(columnsRange, true, true)) return;

        // Compute the new state with the intersection algorithm
        VFXTableState<T> newState = new VFXTableState<>(table, rowsRange, columnsRange);
        intersectionAlgorithm(rowsRange, newState);

        if (disposeCurrent()) newState.setRowsChanged(true);
        table.updateState(newState);
    }

    /// This method is responsible for computing a new state when the columns' list changes. It's called by
    /// [VFXTable#onColumnsChanged(ListChangeListener.Change)], which does the bookkeeping on the columns first (the
    /// table reference and the index hint), and by then the layout cache has already seen the change too.
    ///
    /// The number of columns changed, so the horizontal position may be stale and is invalidated before anything else.
    ///
    /// Rows are kept, and each of them is asked to update its cells with `true` as the second parameter,
    /// [VFXTableRow#updateColumns(IntegerRange,boolean)]: this is the one case where the columns can be the same
    /// objects at different indexes, so the rows cannot detect the change by comparing ranges. Indexes in the rows
    /// range that have no row yet get one, which happens when this is the change that makes the table renderable.
    ///
    /// @param from the first index in the columns' list the change touched. Columns before it keep their position, so
    /// the layout starts here, or at the first column that entered the range if that one comes before
    protected void onColumnsChanged(int from) {
        VFXTableHelper<T> helper = helper();

        // The number of columns changed, so hPos may be stale
        invalidatePos();

        IntegerRange columnsRange = helper.columnsRange();
        IntegerRange rowsRange = helper.rowsRange();
        if (!rangeCheck(columnsRange, true, true)) return;

        VFXTableState<T> current = state();
        VFXTableState<T> newState = new VFXTableState<>(table, rowsRange, columnsRange, current.getRows());
        newState.setColumnsChanged(true);
        if (rangeCheck(rowsRange, false, false)) {
            for (Integer idx : rowsRange) {
                VFXTableRow<T> row = newState.getRows().get(idx);
                if (row == null) {
                    row = helper.indexToRow(idx);
                    row.updateIndex(idx);
                    newState.addRow(idx, row);
                    newState.setRowsChanged(true);
                }
                row.updateColumns(columnsRange, true);
            }
        }

        IntegerRange diff = Utils.difference(columnsRange, current.getColumnsRange());
        from = INVALID_RANGE.equals(diff) ? from : Math.min(from, diff.getMin());
        table.updateState(newState, IntegerRange.of(from, Integer.MAX_VALUE));
    }

    /// This core method is responsible for updating the table's state when the items' list changes, be it a change in
    /// the list or a whole new list, see [VFXTable#itemsProperty()].
    ///
    /// These updates are the trickiest and the most expensive ones. Additions and removals can occur at any position,
    /// which means computing the new state on the indexes alone is a no-go. Picture it with this example:
    /// ```
    /// In list before: 0 1 2 3 4 5
    /// Add at index 2 these items: 99, 98
    /// In list after: 0 1 99 98 2 3 4 5
    /// Now let's suppose the range of displayed items is the same: [0, 5](6 items)
    ///(I'm going now to write items with the index too, like this Index:Item)
    /// Items before: [0:0, 1:1, 2:2, 3:3, 4:4, 5:5]
    /// Items after: [0:0, 1:1, 2:99, 3:98, 4:2, 5:3]
    /// See? Items 2 and 3 are still there but in a different position (index). Since we assume item updates are more
    /// expensive than index updates, we must ensure to take those two rows and update them just by index
    ///```
    ///
    /// For this reason rows are not taken from the old state by index but by **item**,
    /// [VFXTableState#removeRow(Object)]. For each index in the new rows range we get the item that is now there, and
    /// if a row for it exists we only update its index and move it to the new state, excluding that index from the
    /// ones still to process. What remains goes to [#remainingAlgorithm(ExcludingIntegerRange,VFXTableState)].
    ///
    /// The positions are invalidated before the computation, since the number of items decides how far the table can
    /// scroll. The number of items may also have made the table unrenderable, which is what
    /// [#tableFactorySizeCheck()] is for.
    ///
    /// No column moves here, so only the rows are laid out at the end. As the example shows, rows that are still in
    /// the viewport can be at a different index, and therefore at a different position.
    protected void onItemsChanged() {
        VFXTableHelper<T> helper = helper();

        // Ensure the positions are correct
        invalidatePos();

        if (!tableFactorySizeCheck()) return; // If the table is now empty, then set empty state

        // Compute rows ranges and new state
        VFXTableState<T> current = state();
        IntegerRange rowsRange = helper.rowsRange();
        ExcludingIntegerRange eRange = ExcludingIntegerRange.of(rowsRange);
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
        table.updateState(newState, INVALID_RANGE);
    }

    /// This method is responsible for computing a new state when the [VFXTable#columnsSizeProperty()] changes. The
    /// property specifies both the columns' height and their minimum width, so both ranges can vary here.
    ///
    /// After the usual checks the new state is computed by
    /// [#moveReuseCreateAlgorithm(IntegerRange,IntegerRange,VFXTableState)], and the whole viewport is laid out, since
    /// columns, rows and cells may all need to be resized or moved.
    protected void onColumnsSizeChanged() {
        VFXTableHelper<T> helper = helper();

        invalidatePos(); // Ensure positions are correct before potentially producing an empty state!
        if (!tableFactorySizeCheck()) return;

        IntegerRange rowsRange = helper.rowsRange();
        IntegerRange columnsRange = helper.columnsRange();
        if (!rangeCheck(columnsRange, true, true)) return;

        // The columns range can move here, so common rows must update their cells too
        VFXTableState<T> newState = new VFXTableState<>(table, rowsRange, columnsRange);
        newState.setColumnsChanged(state());
        moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);

        if (disposeCurrent()) newState.setRowsChanged(true);
        table.updateState(newState);
    }

    /// Called by a column when its [VFXTableColumn#userPrefWidthProperty()] changes. Tells the layout cache,
    /// [ColumnsLayoutCache#onColumnResized(VFXTableColumn)], and hands the first affected column to
    /// [#layoutColumnsFrom(int)].
    protected void onColumnResized(VFXTableColumn<T, ?> column) {
        layoutColumnsFrom(layoutCache().onColumnResized(column));
    }

    /// Called when a column's weight changes, see [VFXTable#setWeight(VFXTableColumn,int)]. Tells the layout cache,
    /// [ColumnsLayoutCache#onColumnWeightChanged(VFXTableColumn)], and hands the first affected column to
    /// [#layoutColumnsFrom(int)].
    protected void onColumnWeightChanged(VFXTableColumn<T, ?> column) {
        layoutColumnsFrom(layoutCache().onColumnWeightChanged(column));
    }

    /// Both a column's resize and a weight change end up here, with the index of the first column whose width or
    /// position changed. A negative index means nothing actually changed, and there is nothing to do.
    ///
    /// Everything from that column rightwards moves, so the columns range may change with it.<br >
    /// If it did **not**, this is just a partial layout starting at that column, a nice optimization over a full one.
    /// If it **did**, a new state is computed by
    /// [#moveReuseCreateAlgorithm(IntegerRange,IntegerRange,VFXTableState)] and the layout follows from it, still
    /// starting at that column.
    ///
    /// The horizontal position is invalidated first: a width change moves [VFXTable#virtualMaxXProperty()] and thus
    /// the maximum horizontal scroll, which may leave the current one out of bounds.
    ///
    /// @param first the index of the first column to lay out, -1 if no width or position changed
    protected void layoutColumnsFrom(int first) {
        VFXTableHelper<T> helper = helper();
        VFXTableState<T> state = state();

        if (first < 0 || state.isEmpty()) return; // -1: no effective width change, nothing to lay out
        invalidatePos();

        IntegerRange layoutInterval = IntegerRange.of(first, Integer.MAX_VALUE);

        // Range unchanged, partial layout from resized column
        IntegerRange columnsRange = helper.columnsRange();
        if (state.getColumnsRange().equals(columnsRange)) {
            table.requestViewportLayout(layoutInterval);
            return;
        }

        IntegerRange rowsRange = state.getRowsRange();
        VFXTableState<T> newState = new VFXTableState<>(table, rowsRange, columnsRange);
        newState.setColumnsChanged(state);
        moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);
        if (disposeCurrent()) newState.setRowsChanged(true);
        table.updateState(newState, layoutInterval);
    }

    /// Called when the [VFXTable#columnsFillPolicyProperty()] changes. The policy decides how the leftover width is
    /// shared, so every column can end up with a different width and position, and the columns range can move too.
    /// That is exactly what [#onColumnsSizeChanged()] deals with, so the computation is delegated to it.
    protected void onFillPolicyChanged() {
        onColumnsSizeChanged();
    }

    /// Called by a column when its cell factory changes, see [VFXTableColumn#getCellFactory()].
    ///
    /// Only the cells built by that column are replaced, which each row does on its own as efficiently as it can,
    /// [VFXTableRow#replaceCells(VFXTableColumn)].
    ///
    /// Nothing about the table's state really changes here, the rows' state does. Still, a new state object is
    /// produced as a signal that something happened, so that this path looks like all the others to the skin.
    protected void onCellFactoryChanged(VFXTableColumn<T, ?> column) {
        VFXTableState<T> state = state();
        if (state.isEmpty()) return;

        for (VFXTableRow<T> row : state.getRowsByIndex().values()) row.replaceCells(column);
        // Produce a "fake" new state, purely as a signal that something changed (uniforms to the rest)
        VFXTableState<T> newState = new VFXTableState<>(
            table,
            state.getRowsRange(), state.getColumnsRange(),
            state.getRows()
        );
        table.updateState(newState, INVALID_RANGE);
    }

    /// This method is responsible for updating the table's state when the [VFXTable#rowsFactoryProperty()] changes.
    ///
    /// Rows can't change the ranges, so the new state is built by creating a row for every index in the current rows
    /// range. When the old state has something to offer, each new row copies the state of the row at the same index,
    /// [VFXTableRow#copyStateFrom(VFXTableRow)], which saves building its cells from scratch. Otherwise the new rows
    /// are simply updated by index and given their cells.
    ///
    /// The old state is disposed and the rows' cache is cleared in any case, since rows built by the old factory
    /// cannot be reused, not even if the table cannot produce a state at all.
    protected void onRowsFactoryChanged() {
        VFXTableState<T> state = state();

        if (!tableFactorySizeCheck()) {
            // Rows in cache are from the old factory, clear cache!
            table.getRowsCache().clear();
            return;
        }

        // Generate the new state
        VFXTableHelper<T> helper = helper();
        CellFactory<T, VFXTableRow<T>> rf = table.rowsFactoryProperty();
        IntegerRange rowsRange = helper.rowsRange();
        IntegerRange columnsRange = helper.columnsRange();

        VFXTableState<T> newState = new VFXTableState<>(table, rowsRange, columnsRange);
        newState.setRowsChanged(true);

        // Iterate over the rows range and generate a row with the new factory for each index/item.
        // The new rows will copy the state of the previous row at the same index (except if the current state is INVALID or empty)
        for (Integer idx : rowsRange) {
            T item = helper.indexToItem(idx);
            VFXTableRow<T> row = rf.create(item);
            if (state != VFXTableState.INVALID && !state.isEmpty()) {
                row.copyStateFrom(state.getRows().get(idx));
            } else {
                row.updateIndex(idx);
                row.updateColumns(columnsRange, false);
            }
            newState.addRow(idx, item, row);
        }

        disposeCurrent();
        table.getRowsCache().clear();
        table.updateState(newState, INVALID_RANGE);
    }

    /* CORE ALGORITHMS */

    /// Avoids code duplication. Used when it's enough to move the rows from the current state to the new one, index by
    /// index. Indexes that are not in the current state are delegated to [#remainingAlgorithm(ExcludingIntegerRange,VFXTableState)],
    /// which gets a row from the old state, the cache or the factory.
    ///
    /// The columns range is needed to make sure each moved row is displaying the right cells,
    /// [VFXTableRow#updateColumns(IntegerRange,boolean)]. The call is always made, it's the row that checks whether
    /// there is anything to do.
    protected void moveReuseCreateAlgorithm(IntegerRange rowsRange, IntegerRange columnsRange, VFXTableState<T> newState) {
        if (INVALID_RANGE.equals(rowsRange)) return;
        VFXTableState<T> current = state();
        ExcludingIntegerRange eRange = ExcludingIntegerRange.of(rowsRange);
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

    /// Avoids code duplication. Used when the old rows range and the new one are likely to be very close, and when the
    /// items' list did not change. The computation is in two parts.
    ///
    /// The intersection between the two ranges tells which rows can be moved to the new state as they are, with no
    /// update at all. Those indexes are then excluded.
    ///
    /// What remains is delegated to [#remainingAlgorithm(ExcludingIntegerRange,VFXTableState)]: those indexes show
    /// items that are not in the viewport yet.
    ///
    /// @see Utils#intersection(IntegerRange, IntegerRange)
    /// @see ExcludingIntegerRange
    protected void intersectionAlgorithm(IntegerRange rowsRange, VFXTableState<T> newState) {
        // Current and new states, intersection between current and new range
        VFXTableState<T> current = state();
        ExcludingIntegerRange eRange = ExcludingIntegerRange.of(rowsRange);
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
    }

    /// Avoids code duplication. Processes the indexes that were not found in the current state, which means both an
    /// index and an item update. The row itself can come from two places: from the current state if it still has any,
    /// otherwise from [VFXTableHelper#itemToRow(Object)], which takes one from the cache or builds it with the
    /// factory.
    ///
    /// Either way the row is also asked to update its cells, which new rows need just as much as reused ones.
    protected void remainingAlgorithm(ExcludingIntegerRange eRange, VFXTableState<T> newState) {
        VFXTableHelper<T> helper = helper();
        VFXTableState<T> current = state();

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

    /* UTILS */

    /// Forces the [VFXTable#vPosProperty()] and the [VFXTable#hPosProperty()] to be validated again, by calling the
    /// respective setters with their current values: the two properties clamp themselves between 0 and the max scroll.
    ///
    /// The flag keeps [#onPositionChanged(Orientation)] from running while it happens, see the class docs.
    protected void invalidatePos() {
        invalidatingPos = true;
        table.setVPos(table.getVPos());
        table.setHPos(table.getHPos());
        invalidatingPos = false;
    }

    /// Avoids code duplication. Checks whether the table can produce a state at all, which is false when any of these
    /// is true:
    ///
    /// 1) the columns' list is empty
    ///
    /// 2) the items' list is empty
    ///
    /// 3) the rows factory is `null`
    ///
    /// 4) the rows' height is lesser or equal to 0
    ///
    /// 5) the table's width is lesser or equal to 0
    ///
    /// 6) the table's height is lesser or equal to 0
    ///
    /// In that case the current state is disposed, the table's state is set to [#computeInvalidState()], the flag
    /// guarding the positions is reset, and this returns false. Otherwise it does nothing and returns true.
    ///
    /// @return whether the table can produce a state
    protected boolean tableFactorySizeCheck() {
        if (table.columns().isEmpty() ||
            table.isEmpty() ||
            table.rowsFactoryProperty().getValue() == null ||
            table.getRowsHeight() <= 0 ||
            table.getWidth() <= 0 ||
            table.getHeight() <= 0) {
            disposeCurrent();
            table.updateState(computeInvalidState());
            invalidatingPos = false;
            return false;
        }
        return true;
    }

    /// Avoids code duplication. Checks whether the given range is valid, in other words not equal to
    /// [Utils#INVALID_RANGE].
    ///
    /// When it's not, the current state is disposed (only if `dispose` is true), the table's state is set to
    /// [VFXTableState#INVALID] (only if `update` is true), the flag guarding the positions is reset, and this returns
    /// false. Otherwise it does nothing and returns true.
    ///
    /// A note for the future on why the order matters: the disposal must happen **before** the table's state is set to
    /// [VFXTableState#INVALID], otherwise it would dispose the empty state instead of the right one.
    ///
    /// @param range the range to check
    /// @param update whether to set the table's state to [VFXTableState#INVALID] if the range is not valid
    /// @param dispose whether to dispose the current state if the range is not valid
    /// @return whether the range is valid
    @SuppressWarnings("unchecked")
    protected boolean rangeCheck(IntegerRange range, boolean update, boolean dispose) {
        if (INVALID_RANGE.equals(range)) {
            if (dispose) disposeCurrent();
            if (update) table.updateState(VFXTableState.INVALID);
            invalidatingPos = false;
            return false;
        }
        return true;
    }

    /// The table is a special component also because it can technically work with no items in it, as long as it has
    /// columns to show. So, [VFXTableState#INVALID] is used only when there are no columns at all, or in general when
    /// the columns range is [Utils#INVALID_RANGE].
    ///
    /// Otherwise the state that comes out of here has an invalid rows range and a valid columns range, with both its
    /// flags set depending on the old state, see [VFXTableState#haveRowsChanged()] and
    /// [VFXTableState#haveColumnsChanged()].
    ///
    /// @return the state to use when no rows can be displayed
    @SuppressWarnings("unchecked")
    protected VFXTableState<T> computeInvalidState() {
        VFXTableHelper<T> helper = helper();
        IntegerRange columnsRange = helper.columnsRange();
        if (INVALID_RANGE.equals(columnsRange)) return VFXTableState.INVALID;

        VFXTableState<T> partial = new VFXTableState<>(table, INVALID_RANGE, columnsRange);
        partial.setColumnsChanged(state());
        partial.setRowsChanged(!state().isEmpty());
        return partial;
    }

    /// Avoids code duplication. Disposes the current state if it is not empty, [VFXTableState#dispose()].
    ///
    /// @return whether the disposal was done
    protected boolean disposeCurrent() {
        VFXTableState<T> state = state();
        if (!state.isEmpty()) {
            state.dispose();
            return true;
        }
        return false;
    }

    //================================================================================
    // Getters
    //================================================================================

    public VFXTable<T> table() {
        return table;
    }

    public VFXTableHelper<T> helper() {
        return requireNonNull(table.getHelper(), "The table's manager cannot operate without a helper");
    }

    protected ColumnsLayoutCache<T> layoutCache() {
        return table.getLayoutCache();
    }

    public VFXTableState<T> state() {
        return table.getState();
    }
}
