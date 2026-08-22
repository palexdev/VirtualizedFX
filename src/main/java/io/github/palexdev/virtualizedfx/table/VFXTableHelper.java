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

import java.util.Objects;
import java.util.Optional;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.NumberRange;
import io.github.palexdev.mfxcore.base.properties.range.IntegerRangeProperty;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.virtualizedfx.base.VFXContainerHelper;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;

import static io.github.palexdev.mfxcore.base.beans.Position.position;

/// This interface is a utility API for [VFXTable], computations may change depending on the
/// [VFXTable#columnsLayoutModeProperty()]. For this reason, there are two concrete implementations:
/// [FixedTableHelper] and [VariableTableHelper].
public interface VFXTableHelper<T> extends VFXContainerHelper<T, VFXTable<T>> {

    /// @return the index of the first visible column
    int firstColumn();

    /// @return the index of the last visible column
    int lastColumn();

    /// @return the number of columns visible in the viewport. Not necessarily the same as [#totalColumns()]
    int visibleColumns();

    /// @return the total number of columns in the viewport which doesn't include only the number of visible columns but also
    /// the number of buffer columns
    /// @see VFXTable#columnsBufferSizeProperty()
    int totalColumns();

    /// Specifies the range of columns that should be present in the viewport. This also takes into account buffer columns,
    /// see [#visibleColumns()] and [#totalColumns()].
    ReadOnlyObjectProperty<NumberRange<Integer>> columnsRangeProperty();

    /// @return the range of columns that should be present in the viewport. This also takes into account buffer columns,
    /// see [#visibleColumns()] and [#totalColumns()]
    default IntegerRange columnsRange() {
        return (IntegerRange) columnsRangeProperty().get();
    }

    /// @return the index of the first visible row
    int firstRow();

    /// @return the index of the last visible row
    int lastRow();

    /// @return the number of rows visible in the viewport. Not necessarily the same as [#totalRows()]
    int visibleRows();

    /// @return the total number of rows in the viewport which doesn't include only the number of visible rows but also
    /// the number of buffer rows
    int totalRows();

    /// Specifies the range of rows that should be present in the viewport. This also takes into account buffer rows,
    /// see [#visibleRows()] and [#totalRows()].
    ReadOnlyObjectProperty<NumberRange<Integer>> rowsRangeProperty();

    /// @return the range of rows that should be present in the viewport. This also takes into account buffer rows,
    /// see [#visibleRows()] and [#totalRows()].
    default IntegerRange rowsRange() {
        return (IntegerRange) rowsRangeProperty().get();
    }

    /// @return the width for the given column
    double getColumnWidth(VFXTableColumn<T, ?> column);

    /// The index is the column's **absolute** index in [VFXTable#getColumns()], the same index space used by
    /// the columns range, the rows' cells map and [VFXTableColumn#indexProperty()]. Implementations that need a
    /// range-relative index convert it themselves.
    ///
    /// @return the x position for the given column
    double getColumnPos(int columnIdx, VFXTableColumn<T, ?> column);

    /// "Visible" here means membership in the current state's [VFXTableState#getColumnsRange()]. Both layout modes
    /// virtualize the x-axis, so a column outside that range has neither a header nor any cell in the viewport.
    /// Buffer columns count as in range, so this can be `true` for a column that is just off-screen.
    ///
    /// @return whether the given column is currently visible in the viewport
    default boolean isInViewport(VFXTableColumn<T, ?> column) {
        if (column.getTable() == null || column.getScene() == null || column.getParent() == null) return false;
        VFXTableState<T> state = getContainer().getState();
        if (state == VFXTableState.INVALID) return false;
        int index = getContainer().indexOf(column);
        return IntegerRange.inRangeOf(index, state.getColumnsRange());
    }

    /// Lays out the given column, identified by its **absolute** index in [VFXTable#getColumns()].
    ///
    /// Positions the column at `X: getColumnPos(columnIdx, column)` and `Y: 0`.
    ///
    /// Sizes the column to `W: getColumnWidth(column)` and `H: columnsHeight`
    ///
    /// @see VFXTableSkin#layoutColumns()
    default void layoutColumn(int columnIdx, VFXTableColumn<T, ?> column) {
        Size size = getContainer().getColumnsSize();
        double x = getColumnPos(columnIdx, column);
        double w = getColumnWidth(column);
        double h = size.height();
        column.resizeRelocate(x, 0, w, h);
    }

    /// Lays out the given row.
    /// The layout index is necessary to identify the position of a row among the others (comes above/below).
    /// Unlike the columns' methods, this index really is **range-relative**: rows sit at `0, h, 2h, ...` and
    /// [VFXTableSkin#layoutRows()] counts from 0, the offset to the first row being carried by
    /// [#viewportPositionProperty()].
    ///
    /// Positions the row at `X: 0` and `Y: index * rowsHeight`.
    ///
    /// Sizes the row to be `W: virtualMaxX` and `H: rowsHeight`.
    ///
    /// @see VFXTableSkin#layoutRows()
    default void layoutRow(int layoutIdx, VFXTableRow<T> row) {
        double w = getVirtualMaxX();
        double h = getContainer().getRowsHeight();
        double y = layoutIdx * h;
        row.beforeLayout();
        row.resizeRelocate(0, y, w, h);
        row.afterLayout();
    }

    /// Lays out the given cell. The index is the **absolute** index of the cell's column in
    /// [VFXTable#getColumns()], the same index space the cell is mapped by in the row's state.
    ///
    /// [VFXCell#beforeLayout()] runs before the width and position are read, deliberately: a cell may change its
    /// content there, which changes its `prefWidth`, which feeds the column's width computation.
    ///
    /// If the cell's node already sits at exactly that x, width and height, the layout is skipped. [VFXCell#afterLayout()]
    /// still runs: both hooks are promised to always run, whatever this returns.
    ///
    /// @return whether the cell was actually moved or resized. `false` also for a `null` cell, which is legal since a
    /// column may have no cell factory or one that produces `null`
    /// @see VFXTableRow#layoutCells()
    default boolean layoutCell(int columnIdx, VFXTableCell<T> cell) {
        if (cell == null) return false;
        ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = getContainer().getColumns();
        VFXTableColumn<T, ? extends VFXTableCell<T>> column = columns.get(columnIdx);
        Node node = cell.toNode();
        cell.beforeLayout();
        double w = getColumnWidth(column);
        double h = getContainer().getRowsHeight();
        double x = getColumnPos(columnIdx, column);
        if (node.getLayoutX() == x &&
            node.getLayoutBounds().getWidth() == w &&
            node.getLayoutBounds().getHeight() == h) {
            cell.afterLayout();
            return false;
        }
        node.resizeRelocate(x, 0, w, h);
        cell.afterLayout();
        return true;
    }

    /// Determines and sets the ideal width for the given column, where 'ideal' means that
    /// the column's header as well as all the related cells' content will be fully visible.
    ///
    /// Note: for obvious reasons, the computation is done on the currently visible items! Since both layout modes
    /// virtualize the x-axis, the same is true along the other axis: only the columns in [#columnsRange()], buffer
    /// included, have cells in the viewport at all, so only those can be measured against their content.
    ///
    /// @return whether the resize was actually performed. There are conditions that may prevent it, in which case
    /// implementations are free to either delay the operation or ignore it
    boolean autosizeColumn(VFXTableColumn<T, ?> column);

    /// Depends on the implementation!
    ///
    /// @return whether the resize was actually performed
    boolean autosizeColumns();

    /// @return the **theoretical** number of cells visible in the viewport, `visibleColumns() * visibleRows()`.
    /// Theoretical because it excludes the buffer and assumes every column produces a cell, see [#totalCells()]
    default int visibleCells() {
        int nColumns = visibleColumns();
        int nRows = visibleRows();
        return nColumns * nRows;
    }

    /// @return the total number of cells in the viewport which doesn't include only the number of visible cells but also
    /// the number of buffer cells
    default int totalCells() {
        int nColumns = columnsRange().diff() + 1;
        int nRows = rowsRange().diff() + 1;
        return nColumns * nRows;
    }

    /// Converts the given index to a row. Uses [#itemToRow(Object)].
    default VFXTableRow<T> indexToRow(int index) {
        T item = indexToItem(index);
        return itemToRow(item);
    }

    /// Converts the given item to a row. The result is either on of the rows cached in [VFXCellsCache] that
    /// is updated with the given item, or a totally new one created by the [VFXTable#rowFactoryProperty()].
    default VFXTableRow<T> itemToRow(T item) {
        VFXCellsCache<T, VFXTableRow<T>> cache = getContainer().getCache();
        Optional<VFXTableRow<T>> opt = cache.tryTake();
        opt.ifPresent(c -> c.updateItem(item));
        return opt.orElseGet(() -> getContainer().rowFactoryProperty().create(item));
    }

    /// @return the viewport's height by taking into account the table's header height, which is given by
    /// [VFXTable#columnsSizeProperty()]
    default double getViewportHeight() {
        VFXTable<T> table = getContainer();
        return Math.max(0, table.getHeight() - table.getColumnsSize().height());
    }

    /// Checks whether the given column is the last in [VFXTable#getColumns()].
    /// Basically a shortcut for `table.getColumn().getLast() == column)`
    default boolean isLastColumn(VFXTableColumn<T, ?> column) {
        ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = getContainer().getColumns();
        if (columns.isEmpty()) return false;
        return columns.getLast() == column;
    }

    /// @return the number of columns in the table, [VFXTable#getColumns()]. Not to be confused with
    /// [#visibleColumns()] or [#totalColumns()], which count columns in the *viewport*
    default int columnsCount() {
        return getContainer().getColumns().size();
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
    ///
    /// - the item at the given index if it's [Orientation#VERTICAL]
    ///
    /// - the column at the given index if it's [Orientation#HORIZONTAL]
    void scrollToIndex(Orientation orientation, int index);

    /// Abstract implementation of [VFXTableHelper], contains common members for the two concrete implementations
    /// [FixedTableHelper] and [VariableTableHelper], such as:
    ///
    /// - the range of columns to display as a [IntegerRangeProperty], and its binding
    ///
    /// - the range of rows to display as a [IntegerRangeProperty], and its binding
    ///
    /// Both range bindings are defined here, in [#createBindings()], and are identical for the two modes. What
    /// differs is what feeds them: [#firstColumn()], [#visibleColumns()] and [#totalColumns()] are left to the
    /// subclasses. So the windowing policy is written once and the geometry twice.
    abstract class AbstractHelper<T> extends VFXContainerHelperBase<T, VFXTable<T>> implements VFXTableHelper<T> {
        protected final IntegerRangeProperty columnsRange = new IntegerRangeProperty();
        protected final IntegerRangeProperty rowsRange = new IntegerRangeProperty();

        // Resizing a column (or the columns' size) issues a layout request, and its completion may very well lead to
        // another autosize request, recursing until the stack blows up. This flag is here to break such loops
        protected boolean autosizing = false;

        public AbstractHelper(VFXTable<T> table) {
            super(table);
        }

        /// Defines the two range bindings, which share the same shape.
        ///
        /// The start is the first visible row/column minus the buffer size ([VFXTable#rowsBufferSizeProperty()],
        /// [VFXTable#columnsBufferSizeProperty()]), never negative. The end is that start plus the total number of
        /// needed rows/columns ([#totalRows()], [#totalColumns()]), never past the last index. It may happen that the
        /// resulting `end - start + 1` is lesser than what is needed, typically when the position reaches the max
        /// scroll; in such cases the start is corrected back to `end - needed + 1`.
        ///
        /// If the table's width (the viewport's height for the rows) is 0, or the number of needed rows/columns is 0,
        /// the range is [Utils#INVALID_RANGE].
        ///
        /// The columns range depends on: the columns' list, the table's width, the horizontal position, the columns
        /// buffer size, the columns' size and [#virtualMaxXProperty()]. That last one matters only in
        /// [ColumnsLayoutMode#VARIABLE], where resizing a column moves every column after it and thus changes which
        /// ones fall in the viewport; the columns' size alone would only cover [ColumnsLayoutMode#FIXED], where it
        /// *is* the column width.
        ///
        /// The rows range depends on: the items' list size, the table's height, the columns' size (which also
        /// specifies the header height, and therefore influences the viewport's height), the vertical position, the
        /// rows buffer size and the rows' height.
        @Override
        protected void createBindings() {
            super.createBindings();
            columnsRange.bind(ObjectBindingBuilder.<IntegerRange>build()
                .setMapper(() -> {
                    if (container.getWidth() <= 0) return Utils.INVALID_RANGE;
                    int needed = totalColumns();
                    if (needed == 0) return Utils.INVALID_RANGE;

                    int start = Math.max(0, firstColumn() - container.getColumnsBufferSize().val());
                    int end = Math.min(columnsCount() - 1, start + needed - 1);
                    if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
                    return IntegerRange.of(start, end);
                })
                .addSources(container.getColumns())
                .addSources(container.widthProperty())
                .addSources(container.hPosProperty())
                .addSources(container.columnsBufferSizeProperty())
                .addSources(container.columnsSizeProperty())
                .addSources(virtualMaxX)
                .get()
            );
            rowsRange.bind(ObjectBindingBuilder.<IntegerRange>build()
                .setMapper(() -> {
                    if (getViewportHeight() <= 0) return Utils.INVALID_RANGE;
                    int needed = totalRows();
                    if (needed == 0) return Utils.INVALID_RANGE;

                    int start = Math.max(0, firstRow() - container.getRowsBufferSize().val());
                    int end = Math.min(container.size() - 1, start + needed - 1);
                    if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
                    return IntegerRange.of(start, end);
                })
                .addSources(container.sizeProperty())
                .addSources(container.heightProperty(), container.columnsSizeProperty())
                .addSources(container.vPosProperty())
                .addSources(container.rowsBufferSizeProperty())
                .addSources(container.rowsHeightProperty())
                .get()
            );
        }

        @Override
        protected DoubleBinding createMaxVScrollBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> Math.max(0, getVirtualMaxY() - getViewportHeight()))
                .addSources(virtualMaxY, container.heightProperty(), container.columnsSizeProperty())
                .get();
        }

        /// {@inheritDoc}
        ///
        /// Given by `columnsRange().getMax()`
        @Override
        public int lastColumn() {
            return columnsRange().getMax();
        }

        @Override
        public ReadOnlyObjectProperty<NumberRange<Integer>> columnsRangeProperty() {
            return columnsRange;
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
        /// Given by `rowsRange().getMax()`
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
            return height > 0 ?
                (int) Math.ceil(getViewportHeight() / height) :
                0;
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
        public ReadOnlyObjectProperty<NumberRange<Integer>> rowsRangeProperty() {
            return rowsRange;
        }

        @Override
        public ReadOnlyDoubleProperty virtualMaxXProperty() {
            return virtualMaxX;
        }

        @Override
        public ReadOnlyDoubleProperty virtualMaxYProperty() {
            return virtualMaxY;
        }

        @Override
        public ReadOnlyDoubleProperty maxVScrollProperty() {
            return maxVScroll.getReadOnlyProperty();
        }

        @Override
        public ReadOnlyDoubleProperty maxHScrollProperty() {
            return maxHScroll.getReadOnlyProperty();
        }

        @Override
        public ReadOnlyObjectProperty<Position> viewportPositionProperty() {
            return viewportPosition;
        }
    }

    /// Concrete implementation of [AbstractHelper] for [ColumnsLayoutMode#FIXED].
    /// Every column has the same width, given by [VFXTable#columnsSizeProperty()], which makes all of the x-axis
    /// geometry a matter of dividing and multiplying by that one value.
    ///
    /// The two range bindings are not defined here, they live in [AbstractHelper#createBindings()] and are shared
    /// with the other mode. What this class provides are the values they run on: [#firstColumn()],
    /// [#visibleColumns()] and [#totalColumns()]. On top of those, it defines the viewport position and the virtual
    /// max x and y as follows:
    ///
    /// - the viewport's position, a computation that is at the core of virtual scrolling. The viewport, which contains
    /// the columns and the cells (even though the table's viewport is a bit more complex), is not supposed to scroll by insane
    /// numbers of pixels both for performance reasons and because it is not necessary.
    /// For both the horizontal and vertical positions, we use the same technique, just using the appropriate values according
    /// to the axis we are working on.
    /// First we get the range of rows/columns to display, then their respective sizes (rows' height, columns' width).
    /// We compute the ranges to the first visible row/column, which are given by `IntegerRange.of(range.getMin(), first())`,
    /// in other words we limit the 'complete' ranges to the start buffer including the first row/column after the buffer.
    /// The number of indexes in the newfound ranges (given by [IntegerRange#diff()]) is multiplied by the respective
    /// sizes, this way we are finding the number of pixels to the first visible row/column, `pixelsToFirst`.
    /// At this point, we are missing only one last piece of information: how much of the first row/column do we actually see?
    /// We call this amount `visibleAmountFirst` and it's given by `pos % size`.
    /// Finally, the viewport's position is given by this formula `-(pixelsToFirst + visibleAmountFirst)`
    /// (for both hPos and vPos of course).
    /// If a range is equal to [Utils#INVALID_RANGE], the respective position will be 0!
    /// While it's true that the calculations are more complex and 'needy', it's important to note that this approach
    /// allows avoiding 'hacks' to correctly lay out the cells in the viewport. No need for special offsets at the top
    /// or bottom anymore.
    /// The viewport's position computation has the following dependencies: the vertical and horizontal positions,
    /// the rows' height and the columns' size.
    ///
    /// - the virtual max x and y properties, which give the total number of pixels on the x-axis and y-axis. Virtual
    /// means that it's not the actual size of the container, rather the size it would have if it was not virtualized.
    /// The two values are given by the number of rows/columns multiplied by the respective size (rows' height, columns' width).
    /// Notes: 1) the virtualMaxX is the maximum between the aforementioned computation and the table's width (because the last
    /// column must always take all the available space). 2) the virtualMaxY is going to be 0 if there are no columns in the table.
    /// The computations have the following dependencies: the table's width, the number of columns and items, the columns' size,
    /// the rows' height.
    @SuppressWarnings("JavadocReference") // I don't know why since the method is public
    class FixedTableHelper<T> extends AbstractHelper<T> {

        public FixedTableHelper(VFXTable<T> table) {
            super(table);
            createBindings();
        }

        @Override
        protected void createBindings() {
            super.createBindings();
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
                        double cWidth = container.getColumnsSize().width();
                        IntegerRange cRangeToFirstVisible = IntegerRange.of(columnsRange.getMin(), firstColumn());
                        double cPixelsToFirst = cRangeToFirstVisible.diff() * cWidth;
                        double cVisibleAmount = container.getHPos() % cWidth;
                        x = -(cPixelsToFirst + cVisibleAmount);
                    }
                    return position(x, y);
                })
                .addSources(container.layoutBoundsProperty())
                .addSources(container.vPosProperty(), container.hPosProperty())
                .addSources(container.rowsHeightProperty(), container.columnsSizeProperty())
                .get()
            );
        }

        @Override
        protected DoubleBinding createVirtualMaxXBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> Math.max(container.getWidth(), columnsCount() * container.getColumnsSize().width()))
                .addSources(container.widthProperty())
                .addSources(container.getColumns(), container.columnsSizeProperty())
                .get();
        }

        @Override
        protected DoubleBinding createVirtualMaxYBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> (columnsCount() == 0) ? 0.0 : container.size() * container.getRowsHeight())
                .addSources(container.getColumns(), container.columnsSizeProperty())
                .addSources(container.sizeProperty(), container.rowsHeightProperty())
                .get();
        }

        /// {@inheritDoc}
        ///
        /// Given by `Math.floor(hPos / columnsWidth)`, clamped between 0 and the number of columns - 1.
        @Override
        public int firstColumn() {
            return NumberUtils.clamp(
                (int) Math.floor(container.getHPos() / container.getColumnsSize().width()),
                0,
                columnsCount() - 1
            );
        }

        /// {@inheritDoc}
        ///
        /// Given by `Math.ceil(tableWidth / columnsWidth)`. 0 if the columns' width is also 0.
        @Override
        public int visibleColumns() {
            double width = container.getColumnsSize().width();
            return width > 0 ?
                (int) Math.ceil(container.getWidth() / width) :
                0;
        }

        /// {@inheritDoc}
        ///
        /// Given by [#visibleColumns()] plus double the value of [VFXTable#columnsBufferSizeProperty()], cannot
        /// exceed the number of columns in the table.
        @Override
        public int totalColumns() {
            int visible = visibleColumns();
            return visible == 0 ? 0 : Math.min(visible + container.getColumnsBufferSize().val() * 2, columnsCount());
        }

        /// {@inheritDoc}
        ///
        /// For the [ColumnsLayoutMode#FIXED] mode, all columns will have the same width specified by
        /// [VFXTable#columnsSizeProperty()], except for the last one that needs to take all the available
        /// space (if any left). The last column's width is given by
        /// `Math.max(fixedWidth, tableWidth - ((nColumns - 1) * fixedWidth))`.
        @Override
        public double getColumnWidth(VFXTableColumn<T, ?> column) {
            VFXTable<T> table = getContainer();
            double width = table.getColumnsSize().width();
            if (!isLastColumn(column)) return width;
            return Math.max(width, table.getWidth() - ((columnsCount() - 1) * width));
        }

        /// {@inheritDoc}
        ///
        /// Given by `columnsWidth * (columnIdx - columnsRange().getMin())`.
        ///
        /// This is the only place in the table that still needs a range-relative index, and the reason is local to
        /// this mode: [#viewportPositionProperty()] already encodes the offset from the range's start to the first
        /// visible column, so columns and cells are laid out at `0, w, 2w, ...` relative to the range.
        /// [ColumnsLayoutMode#VARIABLE] instead lays out at absolute positions with `x = -hPos`.
        @Override
        public double getColumnPos(int columnIdx, VFXTableColumn<T, ?> column) {
            return container.getColumnsSize().width() * (columnIdx - columnsRange().getMin());
        }

        /// This method is a no-op as the operation is not possible in [ColumnsLayoutMode#FIXED].
        ///
        /// @return always `false`
        @Override
        public boolean autosizeColumn(VFXTableColumn<T, ?> column) {
            return false;
        }

        /// In [ColumnsLayoutMode#FIXED] this can be still used by setting the [VFXTable#columnsSizeProperty()]
        /// rather than the width of each column.
        ///
        /// If the current state is [VFXTableState#INVALID] then exits immediately.
        ///
        /// If the last column in range ([#columnsRange()]), has its skin still `null`, then we assume that
        /// every other column is in the same situation. In such case, we need to 'delay' the operation and wait for the
        /// skin to be created, so that we can compute the columns' width.
        ///
        /// The first pass is to get the widest column by iterating over them, computing the width with
        /// [VFXTableColumn#computePrefWidth(double)] and keeping the maximum value found.
        ///
        /// If the state is empty (no rows), the computation ends and the [VFXTable#columnsSizeProperty()] is set to:
        /// `Math.max(fixedW, foundMax + extra)`, where 'fixedW' is the current width specified by the
        /// property itself.
        ///
        /// The second pass is to get the widest cell among the ones in the viewport by using
        /// [VFXTableRow#getWidthOf(VFXTableColumn)]. Note that the loop runs over every column, but only those in
        /// [#columnsRange()] have cells in the rows, so the others contribute nothing to this pass.
        ///
        /// Finally, the [VFXTable#columnsSizeProperty()] is set to:
        /// `Math.max(Math.max(fixedW, maxColumnsW + extra), maxCellsW + extra)`, where 'fixedW' is the current width
        /// specified by the property itself.
        ///
        /// @return whether the resize was actually performed. `false` if the state is invalid or if the operation was
        /// delayed because of the columns' skin
        /// @see VFXTable#extraAutosizeWidthProperty()
        @Override
        public boolean autosizeColumns() {
            VFXTableState<T> state = container.getState();
            if (autosizing || state == VFXTableState.INVALID) return false;
            ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = container.getColumns();

            // It may happen that the columns still have a null skin, in such cases we must delay the operation,
            // otherwise there would be no way to compute their width.
            //
            // Check this by getting the last column in range
            // If the first's skin is still null, then most probably every other column is in the same situation
            IntegerRange columnsRange = columnsRange();
            VFXTableColumn<T, ? extends VFXTableCell<T>> column = columns.get(columnsRange.getMax());
            if (column.getSkin() == null) {
                When.onInvalidated(column.skinProperty())
                    .condition(Objects::nonNull)
                    .then(v -> autosizeColumns())
                    .oneShot()
                    .listen();
                return false;
            }

            try {
                autosizing = true;
                double extra = container.getExtraAutosizeWidth();
                double fixedW = container.getColumnsSize().width();
                double maxColumnsW = columns.stream()
                    .mapToDouble(c -> c.computePrefWidth(-1))
                    .max()
                    .orElse(-1);
                if (state.isEmpty()) {
                    container.setColumnsWidth(Math.max(fixedW, maxColumnsW + extra));
                    return true;
                }

                double maxCellsW = columns.stream()
                    .mapToDouble(c -> state.getRowsByIndex().values()
                        .stream()
                        .mapToDouble(r -> r.getWidthOf(c))
                        .max()
                        .orElse(-1.0)
                    )
                    .max()
                    .orElse(-1.0);
                container.setColumnsWidth(Math.max(Math.max(fixedW, maxColumnsW + extra), maxCellsW + extra));
                return true;
            } finally {
                autosizing = false;
            }
        }

        @Override
        public void scrollToIndex(Orientation orientation, int index) {
            if (orientation == Orientation.HORIZONTAL) {
                container.setHPos(container.getColumnsSize().width() * index);
            } else {
                container.setVPos(container.getRowsHeight() * index);
            }
        }
    }

    /// Concrete implementation of [AbstractHelper] for [ColumnsLayoutMode#VARIABLE].
    /// Columns are allowed to have different widths, so none of the x-axis geometry can be derived from a single
    /// value the way [FixedTableHelper] does it.
    ///
    /// The two range bindings are not defined here, they live in [AbstractHelper#createBindings()] and are shared
    /// with the other mode. What this class provides are the values they run on: [#firstColumn()],
    /// [#visibleColumns()] and [#totalColumns()], all three built on [#columnAt(double)]. On top of those, it defines
    /// the viewport position and the virtual max x and y as follows:
    ///
    /// - the viewport's position, a computation that is at the core of virtual scrolling. The viewport, which contains
    /// the columns and the cells (even though the table's viewport is a bit more complex), is not supposed to scroll by insane
    /// numbers of pixels both for performance reasons and because it is not necessary.
    /// For the vertical positions, first we get the range of rows to display and the rows' height.
    /// We compute the range to the first visible row, which is given by `IntegerRange.of(range.getMin(), first())`,
    /// in other words we limit the 'complete' range to the start buffer including the first row after the buffer.
    /// The number of indexes in the newfound range (given by [IntegerRange#diff()]) is multiplied by the rows' height,
    /// this way we are finding the number of pixels to the first visible row, `pixelsToFirst`.
    /// At this point, we are missing only one last piece of information: how much of the first row do we actually see?
    /// We call this amount `visibleAmountFirst` and it's given by `vPos % size`.
    /// Finally, the viewport's vertical position is given by this formula `-(pixelsToFirst + visibleAmountFirst)`.
    /// The horizontal position needs none of that and is simply `-hPos`: unlike [FixedTableHelper], columns and cells
    /// here are laid out at their **absolute** x positions, so translating by the raw scroll offset already lands
    /// them in the right place.
    /// If a range is equal to [Utils#INVALID_RANGE], the respective position will be 0!
    /// While it's true that the calculations are more complex and 'needy', it's important to note that this approach
    /// allows avoiding 'hacks' to correctly lay out the cells in the viewport. No need for special offsets at the top
    /// or bottom anymore.
    /// The viewport's position computation has the following dependencies: the vertical and horizontal positions,
    /// the rows' height and the columns' size.
    ///
    /// - the virtual max y property, which gives the total number of pixels on the y-axis.
    /// Virtual means that it's not the actual size of the container, rather the size it would have if it was not virtualized.
    /// The value is given by the number of rows multiplied by the rows' height. The computation depends on the columns' list,
    /// the columns' size (because the viewport height also depends on the height specified by the columns' size property),
    /// the table's size (number of items), and the rows' height.
    ///
    /// - the virtual max x property, which gives the total number of pixels on the x-axis, the sum of every column's
    /// width. There's no binding for it here: it is bound straight to the [ColumnsLayoutCache], which **is** a
    /// [DoubleBinding] computing exactly that sum, hence [#createVirtualMaxXBinding()] returning `null`.
    ///
    /// **How the x-axis is virtualized here**
    ///
    /// With variable widths, a column's x position is the sum of every previous column's width, a prefix sum. Prefix
    /// sums are monotonic, therefore binary-searchable: [#columnAt(double)] finds in `O(log n)` the last column whose
    /// position is still `<= x`. The visible span then falls out of two such probes, one at `hPos` and one at
    /// `hPos + tableWidth`, and the shared range binding widens the result by the buffer.
    ///
    /// Those prefix sums are not recomputed per query, they are memoized by [ColumnsLayoutCache] and invalidated only
    /// by what can genuinely move a column: a width change, a change to [VFXTable#columnsSizeProperty()], or a
    /// structural change in [VFXTable#getColumns()]. **Scrolling invalidates nothing.** So a scroll event costs the
    /// searches and little else, no matter how many columns there are.
    ///
    /// **The layout cache**
    ///
    /// Virtualizing a 2D structure like [VFXTable] is worth optimizing hard, because the two axes multiply: for each
    /// column in range there are as many cells as there are rows in the viewport, so every re-computation avoided is
    /// paid back once per row. For this reason, this helper makes use of a special cache, [ColumnsLayoutCache], which
    /// aims to improve layout operations by avoiding re-computations when they are not needed. For example, if we
    /// compute the width and the position of a column, then we don't need to re-compute it again when laying out the
    /// corresponding cells, that would be a waste!
    ///
    /// The cache computes columns' widths and their x positions. I won't go into many details here on how the cache
    /// exactly works, read its docs to know more about it, just know that after the first computation, values will be
    /// memorized. Further requests will be as fast as a simple 'getter' method. The cache is also responsible for
    /// automatically invalidate the cached values when certain conditions change.
    ///
    /// For [ColumnsLayoutCache] to work properly, this helper defines the methods which are actually responsible for
    /// the computations. I decided to keep such methods here rather than defining them in the cache mainly
    /// for two reasons: 1) I strongly believe such operations are the helper's responsibility; 2) By doing so we generalize
    /// the cache class, making it flexible to use, and suitable for more use-cases. These methods are:
    /// [#computeColumnWidth(VFXTableColumn, boolean)] and [#computeColumnPos(int, double)].
    @SuppressWarnings("JavadocReference") // I don't know why since the method is public
    class VariableTableHelper<T> extends AbstractHelper<T> {
        private ColumnsLayoutCache<T> layoutCache;

        public VariableTableHelper(VFXTable<T> table) {
            super(table);
            createBindings();
        }

        /// This is used by the [ColumnsLayoutCache] to compute the width of the given column.
        /// The value is given by `Math.max(minW, prefW)`, where:
        ///
        /// - `minW` is given by [VFXTable#columnsSizeProperty()]
        ///
        /// - `prefW` is given by [VFXTableColumn#prefWidth(double)]
        ///
        /// If there's only one column in the table, then the returned value is the maximum between the above formula and
        /// the table's width.
        ///
        /// If the column is the last one in the list, then the final value is given by
        /// `Math.max(Math.max(minW, prefW), tableW - partialW)`, where `partialW` is given by
        /// [ColumnsLayoutCache#getPartialWidth()].
        protected double computeColumnWidth(VFXTableColumn<T, ?> column, boolean isLast) {
            double minW = container.getColumnsSize().width();
            double prefW = Math.max(column.prefWidth(-1), minW);
            if (columnsCount() == 1) return Math.max(prefW, container.getWidth());
            if (!isLast) return column.snapSizeX(prefW);

            double partialW = layoutCache.getPartialWidth();
            return column.snapSizeX(Math.max(prefW, container.getWidth() - partialW));
        }

        /// This is used by the [ColumnsLayoutCache] to compute the x position of a column.
        ///
        /// **Careful, both parameters describe the previous column, not the one being positioned.** Given the
        /// index and the position of the column at `index`, this returns the position of the column at
        /// `index + 1`, which is simply `prevPos + width(index)`.
        ///
        /// For example, to lay out the column at index 1, the cache calls this with `index = 0` and
        /// `prevPos = 0.0` (column 0 always sits at x 0). If column 0 is 100px wide, the result is
        /// `0.0 + 100 = 100`, which is where column 1 goes. To then place column 2, the cache calls this
        /// again with `index = 1` and `prevPos = 100`.
        protected double computeColumnPos(int index, double prevPos) {
            VFXTableColumn<T, ? extends VFXTableCell<T>> column = container.getColumns().get(index);
            return column.snapPositionX(prevPos + layoutCache.getColumnWidth(column));
        }

        /// Binary search over the columns' x positions, [ColumnsLayoutCache#getColumnPos(int)], for the **last**
        /// column whose position is still `<= x`. This is what makes the columns range computable in `O(log n)`
        /// rather than by walking the list, see the class docs.
        ///
        /// Two things worth knowing. The search spans the whole columns' list and never the current window: bounding
        /// it by the window would let the window define its own bounds. And it probes arbitrary indexes, so it can
        /// land on a cold cache and force [ColumnsLayoutCache#getColumnPos(int)] to fill the positions up to there;
        /// that's a one-off cost after an invalidation, never a per-scroll one.
        ///
        /// @return the index of the column the given x coordinate falls into, 0 if there are no columns
        protected int columnAt(double x) {
            int lo = 0;
            int hi = columnsCount() - 1;
            int res = 0;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (layoutCache.getColumnPos(mid) <= x) {
                    res = mid;
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            return res;
        }

        @Override
        protected void createBindings() {
            // Initialize layout cache
            layoutCache = new ColumnsLayoutCache<>(container)
                .setWidthFunction(this::computeColumnWidth)
                .setPositionFunction(this::computeColumnPos)
                .init();

            // Initialize bindings
            super.createBindings();
            virtualMaxX.bind(layoutCache);
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
                    return position(x, y);
                })
                .addSources(container.layoutBoundsProperty())
                .addSources(container.vPosProperty(), container.hPosProperty())
                .addSources(container.rowsHeightProperty(), container.columnsSizeProperty())
                .get()
            );
        }

        @Override
        protected DoubleBinding createVirtualMaxXBinding() {
            return null; // bound to the cache
        }

        @Override
        protected DoubleBinding createVirtualMaxYBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> (columnsCount() == 0) ? 0.0 : container.size() * container.getRowsHeight())
                .addSources(container.getColumns(), container.columnsSizeProperty())
                .addSources(container.sizeProperty(), container.rowsHeightProperty())
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
        /// Given by `columnAt(hPos + tableWidth) - firstColumn() + 1`, so it counts the columns the viewport actually
        /// straddles, however wide they are. 0 if there are no columns or the table's width is also 0.
        @Override
        public int visibleColumns() {
            if (columnsCount() == 0 || container.getWidth() <= 0) return 0;
            return columnAt(container.getHPos() + container.getWidth()) - firstColumn() + 1;
        }

        /// {@inheritDoc}
        ///
        /// Given by [#visibleColumns()] plus double the value of [VFXTable#columnsBufferSizeProperty()], cannot
        /// exceed the number of columns in the table, and it's 0 if the number of visible columns is also 0.
        @Override
        public int totalColumns() {
            int visible = visibleColumns();
            int buffer = container.getColumnsBufferSize().val();
            return visible == 0 ? 0 : Math.min(visible + buffer * 2, columnsCount());
        }

        /// Delegates to [ColumnsLayoutCache#getColumnWidth(VFXTableColumn)].
        @Override
        public double getColumnWidth(VFXTableColumn<T, ?> column) {
            return layoutCache.getColumnWidth(column);
        }

        /// {@inheritDoc}
        ///
        /// Delegates to [ColumnsLayoutCache#getColumnPos(int)]. Positions are absolute in this mode, so the index
        /// needs no conversion at all; compare with [FixedTableHelper#getColumnPos(int, VFXTableColumn)].
        @Override
        public double getColumnPos(int columnIdx, VFXTableColumn<T, ?> column) {
            return layoutCache.getColumnPos(columnIdx);
        }

        /// If the current state is [VFXTableState#INVALID] then exits immediately.
        ///
        /// If the given column's skin is still `null`, then we must 'delay' the operation and wait for the skin to
        /// be created, so that we can compute the column's width.
        ///
        /// The first pass is to get the column's ideal width which is given by `Math.max(minW, prefW) + extra` where:
        ///
        /// - `minW` is specified by [VFXTable#columnsSizeProperty()]
        ///
        /// - `prefW` is obtained by calling [VFXTableColumn#computePrefWidth(double)]
        ///
        /// - `extra` is an extra number of pixels added to the final value specified by [VFXTable#extraAutosizeWidthProperty()]
        ///
        /// If the state is empty (no rows), the computation ends and the column's width is set to the value found by the
        /// above formula.
        ///
        /// The second pass is to get the widest cell among the ones in the viewport by using
        /// [VFXTableRow#getWidthOf(VFXTableColumn)].
        ///
        /// Finally, the column's width is set to: `Math.max(Math.max(minW, prefW), maxCellsWidth) + extra`.
        ///
        /// **Note:** the columns are resized using the method [VFXTableColumn#resize(double)].
        ///
        /// @return whether the resize was actually performed. `false` if the state is invalid or if the operation was
        /// delayed because of the column's skin
        /// @see VFXTableColumn
        /// @see VFXTable#extraAutosizeWidthProperty()
        @Override
        public boolean autosizeColumn(VFXTableColumn<T, ?> column) {
            VFXTableState<T> state = container.getState();
            if (autosizing || state == VFXTableState.INVALID) return false;

            // It may happen that the column still has a null skin, in such cases we must delay the operation,
            // otherwise there would be no way to compute its width
            if (column.getSkin() == null) {
                When.onInvalidated(column.skinProperty())
                    .condition(Objects::nonNull)
                    .then(_ -> autosizeColumn(column))
                    .oneShot()
                    .listen();
                return false;
            }

            try {
                autosizing = true;
                double extra = container.getExtraAutosizeWidth();
                double minW = container.getColumnsSize().width();
                double prefW = column.computePrefWidth(-1);
                if (state.isEmpty()) {
                    column.resize(Math.max(minW, prefW) + extra);
                    return true;
                }

                double maxCellsW = state.getRowsByIndex().values().stream()
                    .mapToDouble(r -> r.getWidthOf(column))
                    .max()
                    .orElse(-1.0);
                column.resize(Math.max(Math.max(minW, prefW), maxCellsW) + extra);
                return true;
            } finally {
                autosizing = false;
            }
        }

        /// This simply calls [#autosizeColumn(VFXTableColumn)] on all the table's columns, whether they are in range
        /// or not. Beware that a column outside [#columnsRange()] has no cells in the viewport to measure, so it ends
        /// up sized to fit its header alone.
        ///
        /// @return whether **every** column was resized
        @Override
        public boolean autosizeColumns() {
            VFXTableState<T> state = container.getState();
            if (autosizing || state == VFXTableState.INVALID) return false;
            boolean done = true;
            for (VFXTableColumn<T, ?> column : container.getColumns()) {
                done &= autosizeColumn(column);
            }
            return done;
        }

        @Override
        public void scrollToIndex(Orientation orientation, int index) {
            if (orientation == Orientation.HORIZONTAL) {
                try {
                    VFXTableColumn<T, ? extends VFXTableCell<T>> column = container.getColumns().get(index);
                    container.setHPos(getColumnPos(container.indexOf(column), column));
                } catch (Exception ignored) {}
            } else {
                container.setVPos(container.getRowsHeight() * index);
            }
        }

        /// {@inheritDoc}
        ///
        /// Overridden here to also dispose the [ColumnsLayoutCache].
        @Override
        public void dispose() {
            if (layoutCache != null) {
                layoutCache.dispose();
                layoutCache = null;
            }
            columnsRange.unbind();
            rowsRange.unbind();
            viewportPosition.unbind();
            super.dispose();
        }
    }
}
