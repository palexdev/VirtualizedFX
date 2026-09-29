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

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.virtualizedfx.base.VFXContainerHelper;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsFillPolicy;
import io.github.palexdev.virtualizedfx.table.defaults.VFXTableColumnBehavior;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;

import static io.github.palexdev.mfxcore.base.beans.Position.position;

/// Utility API for [VFXTable]. The helper computes the ranges of rows and columns to display, the virtual sizes and the
/// viewport's position, decides the columns' width and position, and lays out columns, rows and cells.
/// The default implementation is [VFXDefaultTableHelper], you can change it through [VFXTable#helperFactoryProperty()].
///
/// ## Indexes
///
/// Columns and cells are always identified by the column's **absolute** index in [VFXTable#columns()], the same index
/// space used by the columns range and [VFXTableColumn#indexProperty()]. Rows instead are laid out by a **layout index**,
/// relative to the rows range, see [#layoutRow(int,VFXTableRow)].
///
/// ## Columns' width
///
/// The helper is the one deciding each column's width and position. The table's changes that can affect them are
/// forwarded to the `on...` methods below. Those that return an `int` also tell which columns need to be laid out again:
/// the index of the first column whose position or width changed, or -1 if none did.
///
/// ## Ranges
///
/// The two ranges, [#columnsRangeProperty()] and [#rowsRangeProperty()], are observable values, but they are not required
/// to observe everything they depend on. The table's manager calls [#invalidateRange(Orientation)] whenever a property
/// that affects a range changes (the position, the table's size, the buffers...), and before it reads the range to
/// compute the new state.
public interface VFXTableHelper<T> extends VFXContainerHelper<T, VFXTable<T>> {

    //================================================================================
    // Columns
    //================================================================================

    /// @return the index of the first visible column
    int firstColumn();

    /// @return the index of the last column to display, buffer included
    int lastColumn();

    /// @return the number of columns visible in the viewport. Not necessarily the same as [#totalColumns()]
    int visibleColumns();

    /// @return the total number of columns in the viewport which doesn't include only the number of visible columns but also
    /// the number of buffer columns
    /// @see VFXTable#columnsBufferSizeProperty()
    int totalColumns();

    /// @return the number of columns in the table, [VFXTable#columns()]. Not to be confused with [#visibleColumns()] or
    /// [#totalColumns()], which count columns in the viewport
    default int columnsCount() {
        return getContainer().columns().size();
    }

    /// Specifies the range of columns that should be present in the viewport. This also takes into account buffer columns,
    /// see [#visibleColumns()] and [#totalColumns()].
    ObservableValue<IntegerRange> columnsRangeProperty();

    default IntegerRange columnsRange() {
        return columnsRangeProperty().getValue();
    }

    /// Called by the table's manager when the width specified by [VFXTable#columnsSizeProperty()] changes.
    void onColumnsSizeChanged();

    /// Called by the table's manager when a column's [VFXTableColumn#userPrefWidthProperty()] changes.
    ///
    /// @return the index of the first column whose position or width changed, -1 if none did
    int onColumnResized(VFXTableColumn<T, ?> column);

    /// Called by the table's manager when the table's width changes.
    ///
    /// @return the index of the first column whose position or width changed, -1 if none did
    int onTableWidthChanged();

    /// Called by the table's manager when the [VFXTable#columnsFillPolicyProperty()] changes.
    void onFillPolicyChanged();

    /// Called by the table's manager when a column's weight changes, see [VFXTable#setWeight(VFXTableColumn,int)].
    ///
    /// @return the index of the first column whose position or width changed, -1 if none did
    int onColumnWeightChanged(VFXTableColumn<T, ?> column);

    /// Called by the table when its columns' list changes, before the manager computes the new state.
    void onColumnsChanged(ListChangeListener.Change<? extends VFXTableColumn<T, ?>> change);

    /// @return whether the given column takes part of the leftover width, but not all of it, since other columns absorb
    /// too. See [ColumnsFillPolicy] for what the leftover width is. Used by the resize rule of the default column behavior,
    /// [VFXTableColumnBehavior]
    boolean isSharedAbsorber(VFXTableColumn<T, ?> column);

    /// Lays out the given column, identified by its absolute index in [VFXTable#columns()].
    void layoutColumn(int columnIdx, VFXTableColumn<T, ?> column);

    //================================================================================
    // Rows
    //================================================================================

    /// @return the index of the first visible row
    int firstRow();

    /// @return the index of the last row to display, buffer included
    int lastRow();

    /// @return the number of rows visible in the viewport. Not necessarily the same as [#totalRows()]
    int visibleRows();

    /// @return the total number of rows in the viewport which doesn't include only the number of visible rows but also
    /// the number of buffer rows
    /// @see VFXTable#rowsBufferSizeProperty()
    int totalRows();

    /// Specifies the range of rows that should be present in the viewport. This also takes into account buffer rows,
    /// see [#visibleRows()] and [#totalRows()].
    ObservableValue<IntegerRange> rowsRangeProperty();

    /// @return the range of rows that should be present in the viewport. This also takes into account buffer rows,
    /// see [#visibleRows()] and [#totalRows()]
    default IntegerRange rowsRange() {
        return rowsRangeProperty().getValue();
    }

    /// Lays out the given row.
    ///
    /// The layout index is necessary to identify the position of a row among the others (comes above/below). Unlike the
    /// columns' indexes, this one is **relative** to the rows range: the first row in range has layout index 0, and
    /// the offset to it is carried by the [#viewportPositionProperty()].
    void layoutRow(int layoutIdx, VFXTableRow<T> row);

    /// Converts the given index to a row. Uses [#itemToRow(Object)].
    default VFXTableRow<T> indexToRow(int index) {
        T item = indexToItem(index);
        return itemToRow(item);
    }

    /// Converts the given item to a row. The result is either one of the rows cached in [VFXCellsCache] that is updated
    /// with the given item, or a totally new one created by the [VFXTable#rowsFactoryProperty()].
    default VFXTableRow<T> itemToRow(T item) {
        VFXCellsCache<T, VFXTableRow<T>> cache = getContainer().getRowsCache();
        VFXTableRow<T> row;
        if ((row = cache.take()) != null) {
            row.updateItem(item);
        } else {
            row = getContainer().rowsFactoryProperty().create(item);
        }
        return row;
    }

    //================================================================================
    // Cells
    //================================================================================

    /// @return the **theoretical** number of cells visible in the viewport, `visibleColumns() * visibleRows()`.
    /// Theoretical because it excludes the buffer and assumes every column produces a cell, see [#totalCells()]
    default int visibleCells() {
        int nColumns = visibleColumns();
        int nRows = visibleRows();
        return nColumns * nRows;
    }

    /// @return the total number of cells in the viewport which doesn't include only the number of visible cells but also
    /// the number of buffer cells. Given by the size of the columns range times the size of the rows range
    default int totalCells() {
        int nColumns = columnsRange().diff() + 1;
        int nRows = rowsRange().diff() + 1;
        return Math.max(0, nColumns * nRows);
    }

    /// Lays out the given cell, inside its row. The index is the absolute index of the cell's column in [VFXTable#columns()].
    void layoutCell(int columnIdx, VFXTableCell<T> cell);

    //================================================================================
    // Misc
    //================================================================================

    /// Invalidates the range of the given axis, [#columnsRangeProperty()] for [Orientation#HORIZONTAL],
    /// [#rowsRangeProperty()] for [Orientation#VERTICAL].
    ///
    /// Called by the table's manager when a property that affects the range changes, see the class docs.
    void invalidateRange(Orientation axis);

    /// @return the viewport's height by taking into account the table's header height, which is given by
    /// [VFXTable#columnsSizeProperty()]
    default double viewportHeight() {
        VFXTable<T> table = getContainer();
        return Math.max(0, table.getHeight() - table.getColumnsSize().height());
    }

    /// Scrolls in the viewport, in the given direction (orientation) by the given number of pixels.
    default void scrollBy(Orientation orientation, double pixels) {
        VFXTable<T> table = getContainer();
        if (orientation == Orientation.HORIZONTAL) {
            table.setHPos(table.getHPos() + pixels);
        } else {
            table.setVPos(table.getVPos() + pixels);
        }
    }

    /// Scrolls in the viewport, in the given direction (orientation) to the given pixel value.
    default void scrollToPixel(Orientation orientation, double pixel) {
        VFXTable<T> table = getContainer();
        if (orientation == Orientation.HORIZONTAL) {
            table.setHPos(pixel);
        } else {
            table.setVPos(pixel);
        }
    }

    /// Scrolls in the viewport, depending on the given direction (orientation) to:
    /// - the item at the given index if it's [Orientation#VERTICAL]
    /// - the column at the given index if it's [Orientation#HORIZONTAL]
    void scrollToIndex(Orientation orientation, int index);

    //================================================================================
    // Impl
    //================================================================================

    /// Default implementation of [VFXTableHelper].
    ///
    /// ## How the x-axis is virtualized
    ///
    /// Columns may have different widths, so the x-axis geometry cannot be derived by multiplying a single value. Every
    /// column's width and position come from a [ColumnsLayoutCache], which also implements the fill policy and computes
    /// the [#virtualMaxXProperty()]. The `on...` methods are simply forwarded to it.
    ///
    /// A column's position is the sum of every previous column's width, a prefix sum. Prefix sums are monotonic, therefore
    /// binary-searchable: [#columnAt(double)] finds in `O(log n)` the last column whose position is still `<= x`. The
    /// visible span then falls out of two such probes, one at `hPos` and one at `hPos + tableWidth`, and the range binding
    /// widens the result by the buffer.
    ///
    /// Positions are not recomputed per query, the cache keeps them and invalidates them only when something can
    /// genuinely move a column: a width change, a change to [VFXTable#columnsSizeProperty()], a change of the leftover
    /// width, or a change in [VFXTable#columns()]. **Scrolling invalidates nothing.** So a scroll event costs the searches
    /// and little else, no matter how many columns there are.
    ///
    /// Virtualizing a 2D structure like [VFXTable] is worth optimizing hard, because the two axes multiply: for each column
    /// in range there are as many cells as there are rows in the viewport, so every re-computation avoided is paid back
    /// once per row. For example, the width and the position of a column are computed once, not once for the column and
    /// again for every one of its cells.
    ///
    /// ## The viewport's position
    ///
    /// A computation that is at the core of virtual scrolling. The viewport, which contains the columns and the cells
    /// (even though the table's viewport is a bit more complex), is not supposed to scroll by insane numbers of pixels
    /// both for performance reasons and because it is not necessary.
    ///
    /// For the vertical position, first we get the range of rows to display and the rows' height. We compute the range to
    /// the first visible row, which is given by `IntegerRange.of(range.getMin(), firstRow())`, in other words we limit the
    /// 'complete' range to the start buffer including the first row after the buffer. The number of indexes in the
    /// newfound range (given by [IntegerRange#diff()]) is multiplied by the rows' height, this way we are finding the
    /// number of pixels to the first visible row, `pixelsToFirst`. At this point, we are missing only one last piece of
    /// information: how much of the first row do we actually see? We call this amount `visibleAmountFirst` and it's given
    /// by `vPos % rowsHeight`. Finally, the viewport's vertical position is given by this formula
    /// `-(pixelsToFirst + visibleAmountFirst)`.
    ///
    /// The horizontal position needs none of that and is simply `-hPos`: columns and cells are laid out at their absolute
    /// x positions, so translating by the raw scroll offset already lands them in the right place.
    ///
    /// If a range is equal to [Utils#INVALID_RANGE], the respective position will be 0! Both values are snapped, so that
    /// the snapped positions of columns and cells land on whole pixels on screen too.
    class VFXDefaultTableHelper<T> extends VFXContainerHelperBase<T, VFXTable<T>> implements VFXTableHelper<T> {

        private ObjectBinding<IntegerRange> columnsRange;
        private ObjectBinding<IntegerRange> rowsRange;

        private final ColumnsLayoutCache<T> layoutCache;

        public VFXDefaultTableHelper(VFXTable<T> table) {
            layoutCache = new ColumnsLayoutCache<>(table).init();
            super(table);
            createBindings();
        }

        /// Binary search over the columns' positions, [ColumnsLayoutCache#posAt(int)], for the **last** column whose
        /// position is still `<= x`. This is what makes the columns range computable in `O(log n)` rather than by walking
        /// the list, see the class docs.
        ///
        /// Two things worth knowing. The search spans the whole columns' list and never the current range: bounding it by
        /// the range would let the range define its own bounds. And it probes arbitrary indexes, so it can force the cache
        /// to compute the positions up to there. That's a one-off cost after an invalidation, never a per-scroll one.
        ///
        /// @return the index of the column the given x coordinate falls into, 0 if there are no columns
        protected int columnAt(double x) {
            int lo = 0;
            int hi = columnsCount() - 1;
            int res = 0;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (layoutCache.posAt(mid) <= x) {
                    res = mid;
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            return res;
        }

        /// {@inheritDoc}
        ///
        /// On top of those, defines the two range bindings and the viewport's position (see the class docs).
        ///
        /// The two ranges share the same shape. The start is the first visible row/column minus the buffer size
        /// ([VFXTable#rowsBufferSizeProperty()], [VFXTable#columnsBufferSizeProperty()]), never negative. The end is that
        /// start plus the total number of needed rows/columns ([#totalRows()], [#totalColumns()]), never past the last
        /// index. It may happen that the resulting `end - start + 1` is lesser than what is needed, typically when the
        /// position reaches the max scroll, in such cases the start is corrected back to `end - needed + 1`.
        /// If the table's width (the viewport's height for the rows) is 0, or the number of needed rows/columns is 0,
        /// the range is [Utils#INVALID_RANGE].
        ///
        /// The columns range observes the columns' list, the columns' size and the [ColumnsLayoutCache]. The rows range
        /// observes the number of items and the columns' size. Everything else arrives through
        /// [#invalidateRange(Orientation)], see [VFXTableHelper].
        @Override
        protected void createBindings() {
            super.createBindings();
            columnsRange = ObjectBindingBuilder.<IntegerRange>build()
                .setMapper(() -> {
                    if (container.getWidth() <= 0) return Utils.INVALID_RANGE;
                    int needed = totalColumns();
                    if (needed == 0) return Utils.INVALID_RANGE;

                    int start = Math.max(0, firstColumn() - container.getColumnsBufferSize().val());
                    int end = Math.min(columnsCount() - 1, start + needed - 1);
                    if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
                    return IntegerRange.of(start, end);
                })
                .addSources(container.columns())
                .addSources(layoutCache)
                .get();
            rowsRange = ObjectBindingBuilder.<IntegerRange>build()
                .setMapper(() -> {
                    if (viewportHeight() <= 0) return Utils.INVALID_RANGE;
                    int needed = totalRows();
                    if (needed == 0) return Utils.INVALID_RANGE;

                    int start = Math.max(0, firstRow() - container.getRowsBufferSize().val());
                    int end = Math.min(container.size() - 1, start + needed - 1);
                    if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
                    return IntegerRange.of(start, end);
                })
                .addSources(container.sizeProperty())
                .get();
            viewportPosition.bind(ObjectBindingBuilder.<Position>build()
                .setMapper(() -> {
                    double x = 0;
                    double y = 0;
                    IntegerRange rowsRange = rowsRange();
                    IntegerRange columnsRange = columnsRange();

                    if (!Utils.INVALID_RANGE.equals(rowsRange)) {
                        double cHeight = container.getRowsHeight();
                        IntegerRange rRangeToFirstVisible = IntegerRange.of(rowsRange.getMin(), firstRow());
                        double rPixelsToFirst = rRangeToFirstVisible.diff() * cHeight;
                        double rVisibleAmount = container.getVPos() % cHeight;
                        y = -(rPixelsToFirst + rVisibleAmount);
                    }
                    if (!Utils.INVALID_RANGE.equals(columnsRange)) {
                        x = -container.getHPos();
                    }
                    return position(container.snapPositionX(x), container.snapPositionY(y));
                })
                .addSources(container.layoutBoundsProperty())
                .addSources(container.vPosProperty(), container.hPosProperty())
                .addSources(container.rowsHeightProperty(), container.columnsSizeProperty())
                .get());
        }

        /// @return the [ColumnsLayoutCache], which computes where the columns end, see [ColumnsLayoutCache#computeValue()]
        @Override
        protected DoubleBinding createVirtualMaxXBinding() {
            return layoutCache;
        }

        /// The value is given by the number of items multiplied by the rows' height, 0 if there are no columns.
        @Override
        protected DoubleBinding createVirtualMaxYBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> (columnsCount() == 0) ? 0.0 : container.size() * container.getRowsHeight())
                .addSources(container.sizeProperty(), container.columns())
                .addSources(container.rowsHeightProperty(), container.columnsSizeProperty())
                .get();
        }

        /// {@inheritDoc}
        ///
        /// For the table the value is given by `virtualMaxY - viewportHeight`, since the header takes part of the table's
        /// height, see [#viewportHeight()].
        @Override
        protected DoubleBinding createMaxVScrollBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> Math.max(0, getVirtualMaxY() - viewportHeight()))
                .addSources(virtualMaxY, container.heightProperty(), container.columnsSizeProperty())
                .get();
        }

        /// {@inheritDoc}
        ///
        /// Given by `columnAt(hPos)`, clamped between 0 and the number of columns - 1. 0 if there are no columns.
        @Override
        public int firstColumn() {
            if (columnsCount() == 0) return 0;
            return NumberUtils.clamp(columnAt(container.getHPos()), 0, columnsCount() - 1);
        }

        /// {@inheritDoc}
        ///
        /// Given by `columnsRange().getMax()`.
        @Override
        public int lastColumn() {
            return columnsRange().getMax();
        }

        /// {@inheritDoc}
        ///
        /// Given by `columnAt(hPos + tableWidth) - firstColumn() + 1`, so it counts the columns the viewport actually
        /// straddles, however wide they are. 0 if there are no columns or the table's width is also 0.
        @Override
        public int visibleColumns() {
            if (columnsCount() == 0 || container.getWidth() <= 0) return 0;
            return columnAt(container.getHPos() + container.getWidth()) - firstColumn() + 1;
        }

        /// {@inheritDoc}
        ///
        /// Given by [#visibleColumns()] plus double the value of [VFXTable#columnsBufferSizeProperty()], cannot exceed
        /// the number of columns in the table, and it's 0 if the number of visible columns is also 0.
        @Override
        public int totalColumns() {
            int visible = visibleColumns();
            int buffer = container.getColumnsBufferSize().val();
            return visible == 0 ? 0 : Math.min(visible + buffer * 2, columnsCount());
        }

        @Override
        public ObservableValue<IntegerRange> columnsRangeProperty() {
            return columnsRange;
        }

        /// Delegate for [ColumnsLayoutCache#onColumnsSizeChanged()].
        @Override
        public void onColumnsSizeChanged() {
            layoutCache.onColumnsSizeChanged();
        }

        /// Delegate for [ColumnsLayoutCache#onColumnResized(VFXTableColumn)].
        @Override
        public int onColumnResized(VFXTableColumn<T, ?> column) {
            return layoutCache.onColumnResized(column);
        }

        /// Delegate for [ColumnsLayoutCache#onTableWidthChanged()].
        @Override
        public int onTableWidthChanged() {
            return layoutCache.onTableWidthChanged();
        }

        /// Delegate for [ColumnsLayoutCache#onFillPolicyChanged()].
        @Override
        public void onFillPolicyChanged() {
            layoutCache.onFillPolicyChanged();
        }

        /// Delegate for [ColumnsLayoutCache#onColumnWeightChanged(VFXTableColumn)].
        @Override
        public int onColumnWeightChanged(VFXTableColumn<T, ?> column) {
            return layoutCache.onColumnWeightChanged(column);
        }

        /// Delegate for [ColumnsLayoutCache#onColumnsChanged(ListChangeListener.Change)].
        @Override
        public void onColumnsChanged(ListChangeListener.Change<? extends VFXTableColumn<T, ?>> change) {
            layoutCache.onColumnsChanged(change);
        }

        /// Delegate for [ColumnsLayoutCache#isSharedAbsorber(int)].
        @Override
        public boolean isSharedAbsorber(VFXTableColumn<T, ?> column) {
            return layoutCache.isSharedAbsorber(column.getIndex());
        }

        /// {@inheritDoc}
        ///
        /// Positions the column at `X: posAt(columnIdx)` and `Y: 0`.<br >
        /// Sizes the column to `W: widthAt(columnIdx)` and `H: columnsHeight`.<br >
        /// See [ColumnsLayoutCache#posAt(int)] and [ColumnsLayoutCache#widthAt(int)].
        @Override
        public void layoutColumn(int columnIdx, VFXTableColumn<T, ?> column) {
            column.resizeRelocate(
                layoutCache.posAt(columnIdx),
                0,
                layoutCache.widthAt(columnIdx),
                container.getColumnsSize().height()
            );
        }

        /// {@inheritDoc}
        ///
        /// Given by `Math.floor(vPos / rowsHeight)`, clamped between 0 and [VFXTable#size()] - 1.
        @Override
        public int firstRow() {
            return NumberUtils.clamp(
                (int) Math.floor(container.getVPos() / container.getRowsHeight()),
                0,
                container.size() - 1
            );
        }

        /// {@inheritDoc}
        ///
        /// Given by `rowsRange().getMax()`.
        @Override
        public int lastRow() {
            return rowsRange().getMax();
        }

        /// {@inheritDoc}
        ///
        /// Given by `Math.ceil(viewportHeight / rowsHeight)`. 0 if the rows height is also 0.
        @Override
        public int visibleRows() {
            double height = container.getRowsHeight();
            return height > 0 ? (int) Math.ceil(viewportHeight() / height) : 0;
        }

        /// {@inheritDoc}
        ///
        /// Given by `visibleRows + rowsBuffer * 2`, can't exceed [VFXTable#size()] and it's 0 if the number
        /// of visible rows is also 0.
        @Override
        public int totalRows() {
            int visible = visibleRows();
            return visible == 0 ? 0 : Math.min(visible + container.getRowsBufferSize().val() * 2, container.size());
        }

        @Override
        public ObservableValue<IntegerRange> rowsRangeProperty() {
            return rowsRange;
        }

        /// {@inheritDoc}
        ///
        /// Positions the row at `X: 0` and `Y: layoutIdx * rowsHeight`.<br >
        /// Sizes the row to be `W: virtualMaxX` and `H: rowsHeight`.<br >
        /// The layout is wrapped by the row's [VFXTableRow#beforeLayout()] and [VFXTableRow#afterLayout()] hooks.
        @Override
        public void layoutRow(int layoutIdx, VFXTableRow<T> row) {
            double h = container.getRowsHeight();
            row.beforeLayout();
            row.resizeRelocate(0, layoutIdx * h, getVirtualMaxX(), h);
            row.afterLayout();
        }

        /// {@inheritDoc}
        ///
        /// Positions the cell at `X: posAt(columnIdx)` and `Y: 0`.<br >
        /// Sizes the cell to `W: widthAt(columnIdx)` and `H: rowsHeight`.<br >
        /// The layout is wrapped by the cell's [VFXCell#beforeLayout()] and [VFXCell#afterLayout()] hooks.
        @Override
        public void layoutCell(int columnIdx, VFXTableCell<T> cell) {
            cell.beforeLayout();
            cell.toNode().resizeRelocate(
                layoutCache.posAt(columnIdx),
                0,
                layoutCache.widthAt(columnIdx),
                container.getRowsHeight()
            );
            cell.afterLayout();
        }

        /// {@inheritDoc}
        ///
        /// Horizontally, the position is set to the column's position, [ColumnsLayoutCache#posAt(int)], with the index
        /// clamped between 0 and the number of columns. Vertically, the position is set to `rowsHeight * index`.<br >
        /// Indexes past the end are harmless on both axes anyway, since [VFXTable#hPosProperty()] and
        /// [VFXTable#vPosProperty()] clamp themselves to the maximum scroll.
        @Override
        public void scrollToIndex(Orientation orientation, int index) {
            if (orientation == Orientation.HORIZONTAL) {
                container.setHPos(layoutCache.posAt(NumberUtils.clamp(index, 0, columnsCount())));
            } else {
                container.setVPos(container.getRowsHeight() * index);
            }
        }

        @Override
        public void invalidateRange(Orientation axis) {
            if (axis == Orientation.HORIZONTAL) columnsRange.invalidate();
            else rowsRange.invalidate();
        }

        /// {@inheritDoc}
        ///
        /// Disposes the [ColumnsLayoutCache] and the two range bindings, and unbinds the viewport's position.
        @Override
        public void dispose() {
            layoutCache.dispose();
            columnsRange.dispose();
            rowsRange.dispose();
            viewportPosition.unbind();
            super.dispose();
        }
    }
}
