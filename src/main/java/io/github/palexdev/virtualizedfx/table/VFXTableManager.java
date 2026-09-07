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

import java.util.ArrayList;
import java.util.List;

import io.github.palexdev.mfxcore.base.Disposable;
import io.github.palexdev.mfxcore.base.beans.range.ExcludingIntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.behavior.MFXBehavior;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.virtualizedfx.enums.GeometryChangeType;
import io.github.palexdev.virtualizedfx.properties.CellFactory;
import io.github.palexdev.virtualizedfx.utils.Utils;
import javafx.geometry.Orientation;

import static io.github.palexdev.mfxcore.observables.When.onChanged;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;
import static io.github.palexdev.virtualizedfx.utils.Utils.INVALID_RANGE;
import static java.util.Objects.requireNonNull;

public class VFXTableManager<T> extends MFXBehavior<VFXTable<T>> {

    //================================================================================
    // Properties
    //================================================================================

    private boolean invalidatingPos = false;
    private final List<Disposable> disposables = new ArrayList<>();

    //================================================================================
    // Constructors
    //================================================================================

    public VFXTableManager(VFXTable<T> table) {
        super(table);
    }

    //================================================================================
    // Methods
    //================================================================================

    protected void onGeometryChanged(GeometryChangeType gct) {
        VFXTable<T> table = getNode();
        VFXTableHelper<T> helper = helper();

        int fillFrom = gct == GeometryChangeType.WIDTH ? helper.onWeightsChanged() : -1; // redistribute blank space
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

    protected void onPositionChanged(Orientation axis) {
        if (invalidatingPos) return;
        VFXTable<T> table = getNode();
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

    protected void onRowsHeightChanged() {
        VFXTable<T> table = getNode();
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

    protected void onColumnsChanged(int from) {
        VFXTable<T> table = getNode();
        VFXTableHelper<T> helper = helper();

        // The number of columns changed, so both virtual sizes and hPos may be stale
        helper.invalidateVirtualSizes();
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

    protected void onItemsChanged() {
        VFXTable<T> table = getNode();
        VFXTableHelper<T> helper = helper();

        // Ensure that both virtual sizes and position (which depends on the first) are correct
        helper.invalidateVirtualSizes();
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

    protected void onColumnsSizeChanged() {
        VFXTable<T> table = getNode();
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

    protected void onColumnResized(VFXTableColumn<T, ?> column) {
        VFXTable<T> table = getNode();
        VFXTableHelper<T> helper = helper();
        VFXTableState<T> state = state();

        int first = helper.onColumnResized(column);
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

    protected void onWeightsChanged() {
        onColumnsSizeChanged();
    }

    protected void onCellFactoryChanged(VFXTableColumn<T, ?> column) {
        VFXTable<T> table = getNode();
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

    protected void onRowsFactoryChanged() {
        VFXTable<T> table = getNode();
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

    protected void invalidatePos() {
        VFXTable<T> table = getNode();
        VFXTableHelper<T> helper = helper();
        invalidatingPos = true;
        helper.invalidatePos();
        invalidatingPos = false;
    }

    protected boolean tableFactorySizeCheck() {
        VFXTable<T> table = getNode();
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

    @SuppressWarnings("unchecked")
    protected boolean rangeCheck(IntegerRange range, boolean update, boolean dispose) {
        VFXTable<T> table = getNode();
        if (INVALID_RANGE.equals(range)) {
            if (dispose) disposeCurrent();
            if (update) table.updateState(VFXTableState.INVALID);
            invalidatingPos = false;
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    protected VFXTableState<T> computeInvalidState() {
        VFXTable<T> table = getNode();
        VFXTableHelper<T> helper = helper();
        IntegerRange columnsRange = helper.columnsRange();
        if (INVALID_RANGE.equals(columnsRange)) return VFXTableState.INVALID;

        VFXTableState<T> partial = new VFXTableState<>(table, INVALID_RANGE, columnsRange);
        partial.setColumnsChanged(state());
        partial.setRowsChanged(!state().isEmpty());
        return partial;
    }

    protected boolean disposeCurrent() {
        VFXTableState<T> state = getNode().getState();
        if (!state.isEmpty()) {
            state.dispose();
            return true;
        }
        return false;
    }

    protected VFXTableHelper<T> helper() {
        return requireNonNull(getNode().getHelper(), "The table's manager cannot operate without a helper");
    }

    protected VFXTableState<T> state() {
        return getNode().getState();
    }

    protected void register(When<?>... whens) {
        for (When<?> w : whens) {
            if (!w.isActive()) w.listen();
            disposables.add(w);
        }
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    @Override
    public void init() {
        VFXTable<T> table = getNode();
        register(
            // Geometry
            onInvalidated(table.widthProperty()).then(_ -> {
                helper().invalidateRange(Orientation.HORIZONTAL);
                onGeometryChanged(GeometryChangeType.WIDTH);
            }),
            onInvalidated(table.heightProperty()).then(_ -> {
                helper().invalidateRange(Orientation.VERTICAL);
                onGeometryChanged(GeometryChangeType.HEIGHT);
            }),
            onInvalidated(table.columnsBufferSizeProperty()).then(_ -> {
                helper().invalidateRange(Orientation.HORIZONTAL);
                onGeometryChanged(GeometryChangeType.OTHER);
            }),
            onInvalidated(table.rowsBufferSizeProperty()).then(_ -> {
                helper().invalidateRange(Orientation.VERTICAL);
                onGeometryChanged(GeometryChangeType.OTHER);
            }),
            // Position
            onInvalidated(table.hPosProperty()).then(_ -> {
                helper().invalidateRange(Orientation.HORIZONTAL);
                onPositionChanged(Orientation.HORIZONTAL);
            }),
            onInvalidated(table.vPosProperty()).then(_ -> {
                helper().invalidateRange(Orientation.VERTICAL);
                onPositionChanged(Orientation.VERTICAL);
            }),
            // Others
            onInvalidated(table.itemsProperty()).then(_ -> onItemsChanged()),
            onChanged(table.columnsSizeProperty()).then((o, n) -> {
                // TODO helper().onColumnsSizeChanged(); see VFXTable
                if (o.width() != n.width()) helper().invalidateRange(Orientation.HORIZONTAL);
                if (o.height() != n.height()) helper().invalidateRange(Orientation.VERTICAL);
                onColumnsSizeChanged();
            }),
            onInvalidated(table.columnsFillPolicyProperty()).then(_ -> {
                helper().onWeightsChanged();
                helper().invalidateRange(Orientation.HORIZONTAL);
                onWeightsChanged();
            }),
            onInvalidated(table.rowsHeightProperty()).then(_ -> {
                helper().invalidateRange(Orientation.VERTICAL);
                onRowsHeightChanged();
            }),
            onInvalidated(table.rowsFactoryProperty()).then(_ -> onRowsFactoryChanged())
        );
    }

    @Override
    public void dispose() {
        disposables.forEach(Disposable::dispose);
        disposables.clear();
        super.dispose();
    }
}
