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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap.StateMap;
import io.github.palexdev.virtualizedfx.utils.Utils;

@SuppressWarnings({"DataFlowIssue", "rawtypes", "unchecked"})
public class VFXTableState<T> {

    //================================================================================
    // Properties
    //================================================================================

    //@formatter:off
    public static final VFXTableState INVALID = new VFXTableState() {
        @Override protected VFXTableRow removeRow(int index) {return null;}
        @Override protected VFXTableRow removeRow(Object item) {return null;}
        @Override protected void dispose() {}
    };
    //@formatter:on

    private final VFXTable<T> table;
    private final IntegerRange rowsRange;
    private final IntegerRange columnsRange;
    private final StateMap<T, VFXTableRow<T>> rows;
    private boolean rowsChanged = false;
    private boolean columnsChanged = false;

    //================================================================================
    // Constructors
    //================================================================================

    private VFXTableState() {
        this.table = null;
        this.columnsRange = Utils.INVALID_RANGE;
        this.rowsRange = Utils.INVALID_RANGE;
        this.rows = StateMap.EMPTY;
    }

    public VFXTableState(VFXTable<T> table, IntegerRange rowsRange, IntegerRange columnsRange) {
        this.table = table;
        this.rowsRange = rowsRange;
        this.columnsRange = columnsRange;
        this.rows = new StateMap<>();
    }

    protected VFXTableState(VFXTable<T> table, IntegerRange rowsRange, IntegerRange columnsRange, StateMap<T, VFXTableRow<T>> rows) {
        this.table = table;
        this.rowsRange = rowsRange;
        this.columnsRange = columnsRange;
        this.rows = rows;
    }

    //================================================================================
    // Methods
    //================================================================================

    protected void addRow(int index, VFXTableRow<T> row) {
        addRow(index, table.getItems().get(index), row);
    }

    protected void addRow(int index, T item, VFXTableRow<T> row) {
        rows.put(index, item, row);
    }

    protected VFXTableRow<T> removeRow(int index) {
        VFXTableRow<T> r = rows.remove(index);
        if (r == null) r = removeRow(table.getItems().get(index));
        return r;
    }

    protected VFXTableRow<T> removeRow(T item) {
        return rows.remove(item);
    }

    protected void dispose() {
        getRowsByIndex().values().forEach(r -> {
            r.clear();
            table.getRowsCache().cache(r);
        });
        rows.clear();
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    public VFXTable<T> getTable() {
        return table;
    }

    public IntegerRange getRowsRange() {
        return rowsRange;
    }

    public IntegerRange getColumnsRange() {
        return columnsRange;
    }

    protected StateMap<T, VFXTableRow<T>> getRows() {
        return rows;
    }

    public SequencedMap<Integer, VFXTableRow<T>> getRowsByIndex() {
        return rows.getByIndex();
    }

    public List<Map.Entry<T, VFXTableRow<T>>> getRowsByItem() {
        return rows.resolve();
    }

    public int cellsNum() {
        return getRowsByIndex().values().stream()
            .mapToInt(r -> r.cells().size())
            .sum();
    }

    public int size() {
        return rows.size();
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public boolean haveRowsChanged() {
        return rowsChanged;
    }

    protected void setRowsChanged(boolean rowsChanged) {
        this.rowsChanged = rowsChanged;
    }

    public boolean haveColumnsChanged() {
        return columnsChanged;
    }

    protected void setColumnsChanged(boolean columnsChanged) {
        this.columnsChanged = columnsChanged;
    }

    protected void setColumnsChanged(VFXTableState<T> other) {
        setColumnsChanged(!Objects.equals(columnsRange, other.columnsRange));
    }
}
