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

/// Object to represent the state of a [VFXTable] in a specific moment in time. In other words every state is given by
/// a unique combination of the table's properties (in terms of values).
///
/// States are produced by the table's manager, [VFXTableManager], one for every change it reacts to, and published
/// through [VFXTable#stateProperty()]. The manager fills a state while building it, once published it's meant to be
/// read only.
///
/// The state carries five important pieces of information:
///
/// 1) The range of rows to display
///
/// 2) The range of columns to display
///
/// 3) The rows that are currently in the viewport
///
/// 4) A flag that indicates whether the rows have changed since the last state
///
/// 5) A flag that indicates whether the columns have changed since the last state
///
/// ## Global state and "sub-state"
///
/// The table component is a bit different from others. Cells are not placed directly in the viewport but are wrapped in
/// rows, as also explained here [VFXTable]. Because of this, the state class keeps track of how many and which
/// rows/items are to be displayed in the viewport. However, there is absolutely no information regarding the cells.
/// This important piece of data is stored in each row, see [VFXTableRow].
///
/// I like to refer to this class as the `global state` mainly for two reasons: 1) this is what the table's subsystems
/// use to work, 2) Even if cells' infos are not directly available here, this keeps track of the rows, which means that
/// we can indeed get them.
///
/// ## The two flags
///
/// [#haveRowsChanged()] and [#haveColumnsChanged()] tell the skin whether it has to update the viewport's children,
/// which is a costly operation, so it's done only when needed. The skin may also use other information, for
/// example [#INVALID] and [#isEmpty()], see [VFXTableSkin].
///
/// @see #INVALID
/// @see StateMap
@SuppressWarnings({"DataFlowIssue", "rawtypes", "unchecked"})
public class VFXTableState<T> {

    //================================================================================
    // Properties
    //================================================================================

    /// Special instance of `VFXTableState` used to indicate that no columns can be present in the viewport at a certain
    /// time. The reasons can be many, for example, an invalid range, `width/height <= 0`, no columns at all, etc...
    ///
    /// Its map ignores additions and it cannot be disposed, since there is nothing to dispose. It's compared by
    /// identity, `state == INVALID`, so never build a state that just looks like it.
    ///
    /// This and [#isEmpty()] are two totally different things!! A state with no rows but a valid columns range still
    /// shows the columns, see [VFXTableManager#computeInvalidState()].
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

    /// Creates a state for the given table and ranges, with a new, empty, map of rows.
    public VFXTableState(VFXTable<T> table, IntegerRange rowsRange, IntegerRange columnsRange) {
        this.table = table;
        this.rowsRange = rowsRange;
        this.columnsRange = columnsRange;
        this.rows = new StateMap<>();
    }

    /// Creates a state for the given table and ranges, which uses the given map of rows. The map is **shared**, not
    /// copied, which makes this useful when the new state keeps all the rows of the old one.
    ///
    /// **Beware:** [#dispose()] clears the rows and the map. Once a state shares its map with another one, disposing
    /// the old state also destroys the rows of the new one.
    protected VFXTableState(VFXTable<T> table, IntegerRange rowsRange, IntegerRange columnsRange, StateMap<T, VFXTableRow<T>> rows) {
        this.table = table;
        this.rowsRange = rowsRange;
        this.columnsRange = columnsRange;
        this.rows = rows;
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Delegates to [#addRow(int,Object,VFXTableRow)] by retrieving the `T` item from the items' list at the given index.
    ///
    /// @see StateMap#put(Integer, Object, Object)
    protected void addRow(int index, VFXTableRow<T> row) {
        addRow(index, table.getItems().get(index), row);
    }

    /// Adds the given row to the [StateMap] of this state object.
    ///
    /// @see StateMap#put(Integer, Object, Object)
    protected void addRow(int index, T item, VFXTableRow<T> row) {
        rows.put(index, item, row);
    }

    /// Attempts to remove a row from the [StateMap] first by the given index, and in case it is not found by converting
    /// the index to an item in the items' list.
    ///
    /// @see StateMap#remove(Integer)
    /// @see StateMap#remove(Object)
    protected VFXTableRow<T> removeRow(int index) {
        VFXTableRow<T> r = rows.remove(index);
        if (r == null) r = removeRow(table.getItems().get(index));
        return r;
    }

    /// Removes a row by the given item from the [StateMap].
    ///
    /// @see StateMap#remove(Object)
    protected VFXTableRow<T> removeRow(T item) {
        return rows.remove(item);
    }

    /// Disposes this state by clearing all of its rows, [VFXTableRow#clear()], and caching them, see
    /// [VFXTable#getRowsCache()]. The [StateMap] is also cleared.
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

    /// @return the [VFXTable] instance this state is associated to
    public VFXTable<T> getTable() {
        return table;
    }

    /// @return the range of rows to display
    public IntegerRange getRowsRange() {
        return rowsRange;
    }

    /// @return the range of columns to display
    public IntegerRange getColumnsRange() {
        return columnsRange;
    }

    /// @return the map containing the rows
    /// @see StateMap
    protected StateMap<T, VFXTableRow<T>> getRows() {
        return rows;
    }

    /// @return the map containing the rows by their index, unmodifiable. It's a view, not a copy, so it reflects later
    /// changes to the state's map
    public SequencedMap<Integer, VFXTableRow<T>> getRowsByIndex() {
        return rows.getByIndex();
    }

    /// @return the list containing the rows by their item, as entries because of possible duplicates
    /// @see StateMap#resolve()
    public List<Map.Entry<T, VFXTableRow<T>>> getRowsByItem() {
        return rows.resolve();
    }

    /// @return the total number of cells, the sum of the cells of each row in the [StateMap]
    public int cellsNum() {
        return getRowsByIndex().values().stream()
            .mapToInt(r -> r.cells().size())
            .sum();
    }

    /// @return the number of rows in the [StateMap]
    public int size() {
        return rows.size();
    }

    /// @return whether the [StateMap] is empty, in other words whether the state has no rows
    /// @see #INVALID
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /// @return whether the rows have changed since the last state, either the rows range or the rows themselves.
    /// Used by the default skin to check whether the viewport has to update its children or not
    /// @see VFXTableSkin
    public boolean haveRowsChanged() {
        return rowsChanged;
    }

    /// @see #haveRowsChanged()
    protected void setRowsChanged(boolean rowsChanged) {
        this.rowsChanged = rowsChanged;
    }

    /// @return whether the columns have changed since the last state.
    /// Used by the default skin to check whether the viewport has to update its children or not
    /// @see VFXTableSkin
    public boolean haveColumnsChanged() {
        return columnsChanged;
    }

    /// @see #haveColumnsChanged()
    protected void setColumnsChanged(boolean columnsChanged) {
        this.columnsChanged = columnsChanged;
    }

    /// Sets the columns flag by comparing this state's columns range with the given state's one.
    ///
    /// The comparison is on the ranges only. A change in the columns' list can leave the range as it is and still
    /// change the columns in it, in such cases use [#setColumnsChanged(boolean)].
    protected void setColumnsChanged(VFXTableState<T> other) {
        setColumnsChanged(!Objects.equals(columnsRange, other.columnsRange));
    }
}
