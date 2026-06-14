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

import java.util.*;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap.StateMap;
import io.github.palexdev.virtualizedfx.utils.Utils;

/// Immutable object to represent the state of a [VFXTable] is a specific moment in time. In other words
/// every state is given by a unique combination of the table's properties (in terms of values).
///
/// The state carries six important pieces of information:
///
/// 1) The range of rows to display
///
/// 2) The range of columns to display
///
/// 3) The rows that are currently in the viewport
///
/// 4) A flag that indicates whether rows have changed since the last state (either the rows map or range)
///
/// 5) A flag that indicates whether columns have changed since the last state (the range)
///
/// 6) A flag that indicates whether this state is a clone of a previous state
///
/// **Global state and "sub-state"**
///
/// The table component is a bit different from others. Cells are not placed directly in the viewport but are
/// wrapped in rows as also explained here [VFXTable]. Because of this, the state class keeps track of how many and
/// which rows/items are to be displayed in the viewport. However, there is absolutely no information regarding the cells.
/// This important piece of data is stored in each row; they also have a particular type of [IndexBiMap] to keep
/// track of how many cells and which columns are to be displayed in the viewport.
///
/// I like to refer to this class as the `global state` mainly for two reasons: 1) this is what the table's subsystems
/// use to work, 2) Even if cells' infos are not directly available here, this keeps track of the rows, which means that
/// we can indeed get them.
///
/// **Clones**
///
/// As already said above, the state class has a special flag which indicates whether an instance was produced by cloning
/// another state object (with [#clone()]).
///
/// Q: Why?
///
/// A: This mechanism is mainly a consequence of the above explanation about global and sub-states. There may be changes
/// in the table that do not need to produce a new `VFXTableState` (e.g., a column changing its cell factory).
/// Still, I wanted the system to inform users that a change did indeed happen. Creating a new state from scratch would have
/// been a waste of performance. So, I decided to implement the [#clone()] method to pass every variable from one
/// state to a new one without any alteration. This way, the [VFXTable#stateProperty()] would trigger a change event
/// because instances would be different.
///
/// The only issue with such mechanism is that in most cases a clone state can be used, we don't want the table's skin to
/// do anything (no layout should be needed). This can be done by checking the flag with [#isClone()].
///
/// @see #INVALID
/// @see StateMap
@SuppressWarnings({"rawtypes", "DataFlowIssue", "SameParameterValue"})
public class VFXTableState<T> implements Cloneable {
    //================================================================================
    // Static Properties
    //================================================================================
    /// Special instance of `VFXTableState` used to indicate that no columns can be present in the viewport at
    /// a certain time. The reasons can be many, for example, invalid range, `width/height <= 0`, etc...
    ///
    /// This and [#isEmpty()] are two totally different things!!
    public static final VFXTableState INVALID = new VFXTableState() {
        @Override
        protected VFXTableRow removeRow(int index) {return null;}

        @Override
        protected VFXTableRow removeRow(Object item) {return null;}

        @Override
        protected void dispose() {}

        // This must not generate any clones, this is a special state
        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public VFXTableState clone() {
            return this;
        }
    };

    //================================================================================
    // Properties
    //================================================================================
    private final VFXTable<T> table;
    private final IntegerRange rowsRange;
    private final IntegerRange columnsRange;
    private final StateMap<T, VFXTableRow<T>> rows;
    private boolean rowsChanged = false;
    private boolean columnsChanged = false;
    private boolean clone = false;

    //================================================================================
    // Constructors
    //================================================================================
    private VFXTableState() {
        this.table = null;
        this.rowsRange = Utils.INVALID_RANGE;
        this.columnsRange = Utils.INVALID_RANGE;
        this.rows = new StateMap<>();
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

    /// Delegates to [#addRow(int, Object, VFXTableRow)] by retrieving the `T` item from the items' list at
    /// the given index.
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

    /// Attempts at to remove a row from the [StateMap] first by the given index and in case it is not found
    /// by converting the index to an object in the items' list.
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

    /// Disposes this state by caching and clearing all of its rows. The [StateMap] is also cleared.
    ///
    /// @see VFXTableRow#clear()
    protected void dispose() {
        getRowsByIndex().values().forEach(r -> {
            r.clear();
            table.getCache().cache(r);
        });
        rows.clear();
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /// Creates a new [VFXTableState] instance with the exact same parameters of this state.
    /// Also, the new state's `clone` flag is set to true.
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public VFXTableState<T> clone() {
        VFXTableState<T> clone = new VFXTableState<>(
            table,
            rowsRange,
            columnsRange,
            rows
        );
        clone.clone = true;
        return clone;
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

    /// @return the map containing the rows by their index
    protected SequencedMap<Integer, VFXTableRow<T>> getRowsByIndex() {
        return rows.getByIndex();
    }

    /// @return the list containing the rows by their item, as entries because of possible duplicates
    /// @see StateMap#resolve()
    protected List<Map.Entry<T, VFXTableRow<T>>> getRowsByItem() {
        return rows.resolve();
    }

    /// @return the map containing the rows by their index, unmodifiable
    public SequencedMap<Integer, VFXTableRow<T>> getRowsByIndexUnmodifiable() {
        return Collections.unmodifiableSequencedMap(rows.getByIndex());
    }

    /// @return the list containing the rows by their item, as entries because of possible duplicates, unmodifiable
    /// @see StateMap#resolve()
    public List<Map.Entry<T, VFXTableRow<T>>> getRowsByItemUnmodifiable() {
        return Collections.unmodifiableList(rows.resolve());
    }

    /// @return the total number of cells by summing the number cells of each row in the [StateMap]
    public int cellsNum() {
        return getRowsByIndex().values().stream()
            .mapToInt(r -> r.getCells().size())
            .sum();
    }

    /// @return the number of rows in the [StateMap]
    public int size() {
        return rows.size();
    }

    /// @return whether the [StateMap] is empty
    /// @see StateMap
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /// @return whether the rows have changed since the last state.
    /// Used by the default skin to check whether the viewport has to update its children or not.
    /// @see VFXTableSkin
    public boolean haveRowsChanged() {
        return rowsChanged;
    }

    /// @see #haveRowsChanged()
    protected void setRowsChanged(boolean rowsChanged) {
        this.rowsChanged = rowsChanged;
    }

    /// @return whether the columns have changed since the last state.
    /// Used by the default skin to check whether the viewport has to update its children or not.
    /// @see VFXTableSkin
    public boolean haveColumnsChanged() {
        return columnsChanged;
    }

    /// @see #haveColumnsChanged()
    protected void setColumnsChanged(boolean columnsChanged) {
        this.columnsChanged = columnsChanged;
    }

    /// @see #haveColumnsChanged()
    protected void setColumnsChanged(VFXTableState<T> other) {
        setColumnsChanged(!Objects.equals(columnsRange, other.columnsRange));
    }

    /// @return whether either [#haveRowsChanged()] or [#haveColumnsChanged()] are true, which in general
    /// means that layout needs to be re-computed.
    public boolean isLayoutNeeded() {
        return columnsChanged || rowsChanged;
    }

    /// @return whether this state object is a clone of a previous state
    public boolean isClone() {
        return clone;
    }

}
