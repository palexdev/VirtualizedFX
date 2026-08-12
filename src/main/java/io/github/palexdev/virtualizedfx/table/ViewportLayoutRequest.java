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

import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import javafx.beans.property.ReadOnlyObjectWrapper;

/// A layout request is a signal to a virtualized container to tell its viewport to compute the layout.
/// Every virtualized container has such mechanism, but most of the time the request is a simple boolean flag.
///
/// In the case of the table, however, the request is a class because we want to optimize the layout computation as much
/// as possible in both [ColumnsLayoutMode].
///
/// We may want to do such optimizations mainly in two cases:
///
/// 1) In [ColumnsLayoutMode#FIXED] if the table becomes bigger than all the columns' widths summed, then we
/// want the last column to take all the available space. In such case, we want to lay out just the last column, and all
/// the cells related to it, no need to re-size and re-position everything.
///
/// 2) The same logic applies to [ColumnsLayoutMode#VARIABLE] for the last column as well as for any other column.
/// If a column, say in the middle, changes its size, then we want to re-compute the layout only from the column that
/// changed to the end. And this is a great optimization indeed!
///
/// Also, this class can also be used as a callback, because you can query the [#wasDone()] flag to check
/// whether the request lead to layout computation or not (there may be conditions that prevent it!)
///
/// Instances of this class are **immutable**. An actual request is always a new instance, while the idle state
/// (nothing pending) is represented by two special values, which also spare us from potential `NullPointerExceptions`:
///
/// 1) [#NULL] is the initial value, and the one the skin sets when the layout could **not** be computed
///
/// 2) [#DONE] is the one the skin sets when the layout was computed successfully
///
/// Neither of the two is a valid request, [#isValid()], so the skin ignores them; they differ only in what
/// [#wasDone()] reports.
///
/// @see #isValid()
/// @see #isPartial()
@SuppressWarnings({"rawtypes", "unchecked"})
public class ViewportLayoutRequest<T> {
    //================================================================================
    // Static Properties
    //================================================================================
    public static final ViewportLayoutRequest NULL = new ViewportLayoutRequest<>();
    public static final ViewportLayoutRequest DONE = new ViewportLayoutRequest<>();

    //================================================================================
    // Properties
    //================================================================================
    private final VFXTableColumn<T, ?> column;

    //================================================================================
    // Constructors
    //================================================================================

    /// Builds a request for a full layout computation.
    public ViewportLayoutRequest() {
        this.column = null;
    }

    /// Builds a request for a partial layout computation, starting from the given column.
    public ViewportLayoutRequest(VFXTableColumn<T, ?> column) {
        assert column != null;
        this.column = column;
    }

    //================================================================================
    // Methods
    //================================================================================

    /// @return whether this instance is an actual request, thus neither [#NULL] nor [#DONE]
    public boolean isValid() {
        return this != NULL && this != DONE;
    }

    /// @return whether the column instance passed to this request is not `null`.
    public boolean isPartial() {
        return column != null;
    }

    /// @return the column's instance that will serve as an indicator for a partial layout computation
    public VFXTableColumn<T, ?> column() {
        return column;
    }

    /// @return whether it was possible to fulfill the last layout request
    public boolean wasDone() {
        return this == DONE;
    }

    //================================================================================
    // Inner Classes
    //================================================================================
    public static class ViewportLayoutRequestProperty<T> extends ReadOnlyObjectWrapper<ViewportLayoutRequest<T>> {
        public ViewportLayoutRequestProperty() {
            super(NULL);
        }

        public boolean isValid() {
            return get().isValid();
        }
    }
}
