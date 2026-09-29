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

import javafx.beans.property.ReadOnlyObjectWrapper;

/// A layout request is a signal to a virtualized container to tell its viewport to compute the layout.
/// Every virtualized container has such mechanism, but most of the time the request is a simple boolean flag.
///
/// In the case of the table, however, the request is an object, because we want to optimize the layout computation as
/// much as possible. A table has many more nodes than the other containers, since every row has a cell for each column
/// in range, and often only some of them need to be sized and positioned again.
///
/// ## The interval
///
/// A request carries an interval of columns, `[from, to]`, by their **absolute** index in [VFXTable#columns()]. It says
/// which columns have moved or changed size, and therefore which columns and cells need to be laid out again. Two
/// examples:
///
/// - if a column in the middle changes its width, it and every column on its right move, the ones on its left do not.
///   So, the interval starts at that column and ends at [Integer#MAX_VALUE]
///
/// - when scrolling horizontally, the columns already in the viewport keep their position (the whole viewport is
///   translated instead), only the ones that enter the range need it. So, the interval covers just those
///
/// The rows are not part of the interval, they are always laid out, see [VFXTableSkin#layoutViewport()]. A full layout
/// is `[0, Integer.MAX_VALUE]`, see [VFXTable#requestViewportLayout()].
///
/// ## Special values
///
/// Requests are immutable, an actual request is always a new instance. There are three special values:
///
/// 1) [#Y_ONLY] is an actual request with an empty interval, for changes that move no column, e.g. a vertical scroll.
///    The rows still need to be laid out, and so do the cells they never positioned
///
/// 2) [#NULL] is the initial value, and the one the skin sets when the layout could **not** be computed
///
/// 3) [#DONE] is the one the skin sets when the layout was computed
///
/// [#NULL] and [#DONE] are the idle state, nothing pending, and they also spare us from potential `NullPointerExceptions`.
/// Neither of the two is a valid request, [#isValid()], so the skin ignores them. They differ only in [#done()].
///
/// @param from the first column to lay out, by its absolute index. A negative value means this is not a request
/// @param to the last column to lay out, by its absolute index
/// @param done whether the last request was computed. Meaningful only for [#NULL] and [#DONE]
public record ViewportLayoutRequest(int from, int to, boolean done) {

    //================================================================================
    // Static Properties
    //================================================================================

    /// The idle state when the last request could not be computed, and the initial value.
    public static final ViewportLayoutRequest NULL = new ViewportLayoutRequest(-1, -1, false);

    /// The idle state when the last request was computed.
    public static final ViewportLayoutRequest DONE = new ViewportLayoutRequest(-1, -1, true);

    /// A valid request with an empty interval: no column moved, only the rows need to be laid out, along with the cells
    /// they never positioned.
    ///
    /// The bounds are [Integer#MAX_VALUE] and [Integer#MIN_VALUE] for a reason. Each row merges this interval with its
    /// own, by taking the minimum of the starts and the maximum of the ends, see [VFXTableRow#layoutCells(int,int)].
    /// These two bounds leave the row's interval as it is, any other empty interval (e.g., `[0, -1]`) would widen it.
    public static final ViewportLayoutRequest Y_ONLY = new ViewportLayoutRequest(Integer.MAX_VALUE, Integer.MIN_VALUE);

    //================================================================================
    // Constructors
    //================================================================================

    /// Builds a request for the given interval of columns, see the class docs.
    public ViewportLayoutRequest(int from, int to) {
        this(from, to, false);
    }

    //================================================================================
    // Methods
    //================================================================================

    /// @return whether this instance is an actual request, thus neither [#NULL] nor [#DONE]
    public boolean isValid() {
        return from >= 0;
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    //@formatter:off
    /// A [ReadOnlyObjectWrapper] for [ViewportLayoutRequest]s, with [#NULL] as the initial value.
    public static class ViewportLayoutRequestProperty extends ReadOnlyObjectWrapper<ViewportLayoutRequest> {
        public ViewportLayoutRequestProperty() {super(NULL);}
        /// Delegate for [ViewportLayoutRequest#isValid()], on the current value.
        public boolean isValid() {return getValue().isValid();}
    }
}
