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

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.DoubleRange;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.NumberRange;
import io.github.palexdev.mfxcore.base.properties.range.IntegerRangeProperty;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.observables.OnInvalidated;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.virtualizedfx.base.VFXContainerHelper;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;

/**
 * This interface is a utility API for {@link VFXTable}, computations may change depending on the
 * {@link VFXTable#columnsLayoutModeProperty()}. For this reason, there are two concrete implementations:
 * {@link FixedTableHelper} and {@link VariableTableHelper}.
 */
public interface VFXTableHelper<T> extends VFXContainerHelper<T, VFXTable<T>> {

    /**
     * @return the index of the first visible column
     */
    int firstColumn();

    /**
     * @return the index of the last visible column
     */
    int lastColumn();

    /**
     * @return the number of columns visible in the viewport. Not necessarily the same as {@link #totalColumns()}
     */
    int visibleColumns();

    /**
     * @return the total number of columns in the viewport which doesn't include only the number of visible columns but also
     * the number of buffer columns
     * @see VFXTable#columnsBufferSizeProperty()
     */
    int totalColumns();

    /**
     * Specifies the range of columns that should be present in the viewport. This also takes into account buffer columns,
     * see {@link #visibleColumns()} and {@link #totalColumns()}.
     */
    ReadOnlyObjectProperty<NumberRange<Integer>> columnsRangeProperty();

    /**
     * @return the range of columns that should be present in the viewport. This also takes into account buffer columns,
     * see {@link #visibleColumns()} and {@link #totalColumns()}
     */
    default IntegerRange columnsRange() {
        return (IntegerRange) columnsRangeProperty().get();
    }

    /**
     * @return the index of the first visible row
     */
    int firstRow();

    /**
     * @return the index of the last visible row
     */
    int lastRow();

    /**
     * @return the number of rows visible in the viewport. Not necessarily the same as {@link #totalRows()}
     */
    int visibleRows();

    /**
     * @return the total number of rows in the viewport which doesn't include only the number of visible rows but also
     * the number of buffer rows
     */
    int totalRows();

    /**
     * Specifies the range of rows that should be present in the viewport. This also takes into account buffer rows,
     * see {@link #visibleRows()} and {@link #totalRows()}.
     */
    ReadOnlyObjectProperty<NumberRange<Integer>> rowsRangeProperty();

    /**
     * @return the range of rows that should be present in the viewport. This also takes into account buffer rows,
     * see {@link #visibleRows()} and {@link #totalRows()}.
     */
    default IntegerRange rowsRange() {
        return (IntegerRange) rowsRangeProperty().get();
    }

    /**
     * @return the width for the given column
     */
    double getColumnWidth(VFXTableColumn<T, ?> column);

    /**
     * @return the x position for the given column and its layout index in the viewport
     */
    double getColumnPos(int layoutIdx, VFXTableColumn<T, ?> column);

    /**
     * @return whether the given column is currently visible in the viewport
     */
    boolean isInViewport(VFXTableColumn<T, ?> column);

    /**
     * Lays out the given column.
     * The layout index is necessary to identify the position of a column among the others (comes before/after).
     *
     * @return whether the column was sized and positioned successfully
     * @see VFXTableSkin#layoutColumns()
     */
    boolean layoutColumn(int layoutIdx, VFXTableColumn<T, ?> column);

    /**
     * Lays out the given row.
     * The layout index is necessary to identify the position of a row among the others (comes above/below).
     * <p></p>
     * Positions the row at {@code X: 0} and {@code Y: index * rowsHeight}.
     * <p>
     * Sizes the row to be {@code W: virtualMaxX} and {@code H: rowsHeight}.
     *
     * @see VFXTableSkin#layoutRows()
     */
    default void layoutRow(int layoutIdx, VFXTableRow<T> row) {
        double w = getVirtualMaxX();
        double h = getContainer().getRowsHeight();
        double y = layoutIdx * h;
        row.beforeLayout();
        row.resizeRelocate(0, y, w, h);
        row.afterLayout();
    }

    /**
     * Lays out the given cell.
     * The layout index is necessary to identify the position of a cell among the others (comes before/after).
     *
     * @see VFXTableRow#layoutCells()
     */
    boolean layoutCell(int layoutIdx, VFXTableCell<T> cell);

    /**
     * Determines and sets the ideal width for the given column, where 'ideal' means that
     * the column's header as well as all the related cells' content will be fully visible.
     * <p>
     * Note: for obvious reasons, the computation is done on the currently visible items!
     */
    void autosizeColumn(VFXTableColumn<T, ?> column);

    /**
     * Depends on the implementation!
     */
    void autosizeColumns();

    /**
     * Depends on the implementation!
     */
    int visibleCells();

    /**
     * @return the total number of cells in the viewport which doesn't include only the number of visible cells but also
     * the number of buffer cells
     */
    default int totalCells() {
        int nColumns = columnsRange().diff() + 1;
        int nRows = rowsRange().diff() + 1;
        return nColumns * nRows;
    }

    /**
     * Converts the given index to a row. Uses {@link #itemToRow(Object)}.
     */
    default VFXTableRow<T> indexToRow(int index) {
        T item = indexToItem(index);
        return itemToRow(item);
    }

    /**
     * Converts the given item to a row. The result is either on of the rows cached in {@link VFXCellsCache} that
     * is updated with the given item, or a totally new one created by the {@link VFXTable#rowFactoryProperty()}.
     */
    default VFXTableRow<T> itemToRow(T item) {
        VFXCellsCache<T, VFXTableRow<T>> cache = getContainer().getCache();
        Optional<VFXTableRow<T>> opt = cache.tryTake();
        opt.ifPresent(c -> c.updateItem(item));
        return opt.orElseGet(() -> getContainer().rowFactoryProperty().create(item));
    }

    /**
     * @return the viewport's height by taking into account the table's header height, which is given by
     * {@link VFXTable#columnsSizeProperty()}
     */
    default double getViewportHeight() {
        VFXTable<T> table = getContainer();
        return Math.max(0, table.getHeight() - table.getColumnsSize().getHeight());
    }

    /**
     * Checks whether the given column is the last in {@link VFXTable#getColumns()}.
     * Basically a shortcut for {@code table.getColumn().getLast() == column)}
     */
    default boolean isLastColumn(VFXTableColumn<T, ?> column) {
        ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = getContainer().getColumns();
        if (columns.isEmpty()) return false;
        return columns.getLast() == column;
    }

    /**
     * Scrolls in the viewport, in the given direction (orientation) by the given number of pixels.
     */
    default void scrollBy(Orientation orientation, double pixels) {
        VFXTable<T> table = getContainer();
        if (orientation == Orientation.HORIZONTAL) {
            table.setHPos(table.getHPos() + pixels);
        } else {
            table.setVPos(table.getVPos() + pixels);
        }
    }

    /**
     * Scrolls in the viewport, in the given direction (orientation) to the given pixel value.
     */
    default void scrollToPixel(Orientation orientation, double pixel) {
        VFXTable<T> table = getContainer();
        if (orientation == Orientation.HORIZONTAL) {
            table.setHPos(pixel);
        } else {
            table.setVPos(pixel);
        }
    }

    /**
     * Scrolls in the viewport, depending on the given direction (orientation) to:
     * <p> - the item at the given index if it's {@link Orientation#VERTICAL}
     * <p> - the column at the given index if it's {@link Orientation#HORIZONTAL}
     */
    void scrollToIndex(Orientation orientation, int index);

    /**
     * Abstract implementation of {@link VFXTableHelper}, contains common members for the two concrete implementations
     * {@link FixedTableHelper} and {@link VariableTableHelper}, such as:
     * <p> - the range of columns to display as a {@link IntegerRangeProperty}
     * <p> - the range of rows to display as a {@link IntegerRangeProperty}
     *
     * @param <T>
     */
    abstract class AbstractHelper<T> extends VFXContainerHelperBase<T, VFXTable<T>> implements VFXTableHelper<T> {
        protected final IntegerRangeProperty columnsRange = new IntegerRangeProperty();
        protected final IntegerRangeProperty rowsRange = new IntegerRangeProperty();

        public AbstractHelper(VFXTable<T> table) {
            super(table);
        }

        @Override
        protected DoubleBinding createMaxVScrollBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> Math.max(0, getVirtualMaxY() - getViewportHeight()))
                .addSources(virtualMaxY, container.heightProperty(), container.columnsSizeProperty())
                .get();
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code columnsRange().getMax()}
         */
        @Override
        public int lastColumn() {
            return columnsRange().getMax();
        }

        @Override
        public ReadOnlyObjectProperty<NumberRange<Integer>> columnsRangeProperty() {
            return columnsRange;
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code Math.floor(vPos / rowsHeight)}, clamped between 0 and {@link VFXTable#size()} - 1.
         */
        @Override
        public int firstRow() {
            return NumberUtils.clamp(
                (int) Math.floor(container.getVPos() / container.getRowsHeight()),
                0,
                container.size() - 1
            );
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code rowsRange().getMax()}
         */
        @Override
        public int lastRow() {
            return rowsRange().getMax();
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code Math.ceil(viewportHeight / rowsHeight)}. 0 if the rows height is also 0.
         */
        @Override
        public int visibleRows() {
            double height = container.getRowsHeight();
            return height > 0 ?
                (int) Math.ceil(getViewportHeight() / height) :
                0;
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code visibleRows + rowsBuffer * 2}, can't exceed {@link VFXTable#size()} and it's 0 if the number
         * of visible rows is also 0.
         */
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

    /**
     * Concrete implementation of {@link AbstractHelper} for {@link ColumnsLayoutMode#FIXED}.
     * Here the range of rows and columns to display, as well as the viewport position,
     * the virtual max x and y properties are defined as follows:
     * <p> - the columns range is given by the {@link #firstColumn()} element minus the buffer size {@link VFXTable#columnsBufferSizeProperty()},
     * (cannot be negative) and the sum between this start index and the total number of needed columns given by {@link #totalColumns()}.
     * It may happen that the number of indexes given by the range {@code end - start + 1} is
     * lesser than the total number of columns we need. In such cases, the range start is corrected to be
     * {@code end - needed + 1}. A typical situation for this is when the table's horizontal position reaches the max scroll.
     * If the table's width is 0 or the number of needed columns is 0, then the range will be {@link Utils#INVALID_RANGE}.
     * The computation has the following dependencies: the columns' list, the table width, the horizontal position,
     * the columns buffer size and the columns' size.
     * <p> - the rows range is given by the {@link #firstRow()} element minus the buffer size {@link VFXTable#rowsBufferSizeProperty()},
     * (cannot be negative) and the sum between this start index and the total number of needed rows given by {@link #totalRows()}.
     * It may happen that the number of indexes given by the range {@code end - start + 1} is
     * lesser than the number of rows we need. In such cases, the range start is corrected to be
     * {@code end - needed + 1}. A typical situation for this is when the table's vertical position reaches the max scroll.
     * If the viewport's height is 0 or the number of needed rows is 0, then the range will be {@link Utils#INVALID_RANGE}.
     * The computation has the following dependencies: the table's height, the column's size (because it also specifies the
     * header height, which influences the viewport's height), the vertical position, the rows buffer size, the rows' height
     * and the items' list size.
     * <p> - the viewport's position, a computation that is at the core of virtual scrolling. The viewport, which contains
     * the columns and the cells (even though the table's viewport is a bit more complex), is not supposed to scroll by insane
     * numbers of pixels both for performance reasons and because it is not necessary.
     * For both the horizontal and vertical positions, we use the same technique, just using the appropriate values according
     * to the axis we are working on.
     * First we get the range of rows/columns to display, then their respective sizes (rows' height, columns' width).
     * We compute the ranges to the first visible row/column, which are given by {@code IntegerRange.of(range.getMin(), first())},
     * in other words we limit the 'complete' ranges to the start buffer including the first row/column after the buffer.
     * The number of indexes in the newfound ranges (given by {@link IntegerRange#diff()}) is multiplied by the respective
     * sizes, this way we are finding the number of pixels to the first visible row/column, {@code pixelsToFirst}.
     * At this point, we are missing only one last piece of information: how much of the first row/column do we actually see?
     * We call this amount {@code visibleAmountFirst} and it's given by {@code pos % size}.
     * Finally, the viewport's position is given by this formula {@code -(pixelsToFirst + visibleAmountFirst)}
     * (for both hPos and vPos of course).
     * If a range is equal to {@link Utils#INVALID_RANGE}, the respective position will be 0!
     * While it's true that the calculations are more complex and 'needy', it's important to note that this approach
     * allows avoiding 'hacks' to correctly lay out the cells in the viewport. No need for special offsets at the top
     * or bottom anymore.
     * The viewport's position computation has the following dependencies: the vertical and horizontal positions,
     * the rows' height and the columns' size.
     * <p> - the virtual max x and y properties, which give the total number of pixels on the x-axis and y-axis. Virtual
     * means that it's not the actual size of the container, rather the size it would have if it was not virtualized.
     * The two values are given by the number of rows/columns multiplied by the respective size (rows' height, columns' width).
     * Notes: 1) the virtualMaxX is the maximum between the aforementioned computation and the table's width (because the last
     * column must always take all the available space). 2) the virtualMaxY is going to be 0 if there are no columns in the table.
     * The computations have the following dependencies: the table's width, the number of columns and items, the columns' size,
     * the rows' height.
     */
    @SuppressWarnings("JavadocReference") // I don't know why since the method is public
    class FixedTableHelper<T> extends AbstractHelper<T> {
        private boolean forceLayout = false;

        public FixedTableHelper(VFXTable<T> table) {
            super(table);
            createBindings();
        }

        @Override
        protected void createBindings() {
            columnsRange.bind(ObjectBindingBuilder.<IntegerRange>build()
                .setMapper(() -> {
                    if (container.getWidth() <= 0) return Utils.INVALID_RANGE;
                    int needed = totalColumns();
                    if (needed == 0) return Utils.INVALID_RANGE;

                    int start = Math.max(0, firstColumn() - container.getColumnsBufferSize().val());
                    int end = Math.min(container.getColumns().size() - 1, start + needed - 1);
                    if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
                    return IntegerRange.of(start, end);
                })
                .addSources(container.getColumns())
                .addSources(container.widthProperty())
                .addSources(container.hPosProperty())
                .addSources(container.columnsBufferSizeProperty())
                .addSources(container.columnsSizeProperty())
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
                        double cWidth = container.getColumnsSize().getWidth();
                        IntegerRange cRangeToFirstVisible = IntegerRange.of(columnsRange.getMin(), firstColumn());
                        double cPixelsToFirst = cRangeToFirstVisible.diff() * cWidth;
                        double cVisibleAmount = container.getHPos() % cWidth;
                        x = -(cPixelsToFirst + cVisibleAmount);
                    }
                    return Position.of(x, y);
                })
                .addSources(container.layoutBoundsProperty())
                .addSources(container.vPosProperty(), container.hPosProperty())
                .addSources(container.rowsHeightProperty(), container.columnsSizeProperty())
                .get()
            );

            super.createBindings();
        }

        @Override
        protected DoubleBinding createVirtualMaxXBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> Math.max(container.getWidth(), container.getColumns().size() * container.getColumnsSize().getWidth()))
                .addSources(container.widthProperty(), container.columnsSizeProperty())
                .get();
        }

        @Override
        protected DoubleBinding createVirtualMaxYBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> container.getColumns().isEmpty() ? 0.0 : container.size() * container.getRowsHeight())
                .addSources(container.getColumns())
                .addSources(container.columnsSizeProperty())
                .addSources(container.rowsHeightProperty())
                .get();
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code Math.floor(hPos / columnsWidth)}, clamped between 0 and the number of columns - 1.
         */
        @Override
        public int firstColumn() {
            return NumberUtils.clamp(
                (int) Math.floor(container.getHPos() / container.getColumnsSize().getWidth()),
                0,
                container.getColumns().size() - 1
            );
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code Math.ceil(tableWidth / columnsWidth)}. 0 if the columns' width is also 0.
         */
        @Override
        public int visibleColumns() {
            double width = container.getColumnsSize().getWidth();
            return width > 0 ?
                (int) Math.ceil(container.getWidth() / width) :
                0;
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@link #visibleColumns()} plus double the value of {@link VFXTable#columnsBufferSizeProperty()}, cannot
         * exceed the number of columns in the table.
         */
        @Override
        public int totalColumns() {
            int visible = visibleColumns();
            return visible == 0 ? 0 : Math.min(visible + container.getColumnsBufferSize().val() * 2, container.getColumns().size());
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * For the {@link ColumnsLayoutMode#FIXED} mode, all columns will have the same width specified by
         * {@link VFXTable#columnsSizeProperty()}, except for the last one that needs to take all the available
         * space (if any left). The last column's width is given by
         * {@code Math.max(fixedWidth, tableWidth - ((nColumns - 1) * fixedWidth))}.
         */
        @Override
        public double getColumnWidth(VFXTableColumn<T, ?> column) {
            VFXTable<T> table = getContainer();
            double width = table.getColumnsSize().getWidth();
            if (!isLastColumn(column)) return width;
            return Math.max(width, table.getWidth() - ((table.getColumns().size() - 1) * width));
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code columnsWidth * layoutIndex}.
         */
        @Override
        public double getColumnPos(int layoutIdx, VFXTableColumn<T, ?> column) {
            return container.getColumnsSize().getWidth() * layoutIdx;
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Positions the column at {@code X: getColumnPos(index, column)} and {@code Y: 0}.
         * <p>
         * Sizes the column to {@code W: getColumnWidth(column)} and {@code H: columnsHeight}
         *
         * @return always true
         */
        @Override
        public boolean layoutColumn(int layoutIdx, VFXTableColumn<T, ?> column) {
            Size size = getContainer().getColumnsSize();
            double x = getColumnPos(layoutIdx, column);
            double w = getColumnWidth(column);
            double h = size.getHeight();
            column.resizeRelocate(x, 0, w, h);
            return true;
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * The width and x position values are the exact same used by the {@link #lastColumn()} method.
         * So both {@link #getColumnPos(int, VFXTableColumn)} and {@link #getColumnWidth(VFXTableColumn)} are used
         * to find the cell's x and w respectively. However, before doing so, we must convert the given layout index to
         * the respective column index and then extract the column (since the aforementioned methods need the column as a parameter).
         * The conversion is done by this simple formula: {@code columnsRange.getMin() + layoutIdx}.
         * <p>
         * The y position will be 0 and the height will be {@code rowsHeight}.
         *
         * @return always true
         */
        @Override
        public boolean layoutCell(int layoutIdx, VFXTableCell<T> cell) {
            VFXTable<T> table = getContainer();
            IntegerRange columnsRange = columnsRange();
            int colIndex = columnsRange.getMin() + layoutIdx;
            VFXTableColumn<T, ? extends VFXTableCell<T>> column = table.getColumns().get(colIndex);
            Node node = cell.toNode();
            double x = getColumnPos(layoutIdx, column);
            double w = getColumnWidth(column);
            double h = table.getRowsHeight();
            cell.beforeLayout();
            node.resizeRelocate(x, 0, w, h);
            cell.afterLayout();
            return true;
        }

        /**
         * This method is a no-op as the operation is not possible in {@link ColumnsLayoutMode#FIXED}.
         */
        @Override
        public void autosizeColumn(VFXTableColumn<T, ?> column) {/*NO-OP*/}

        /**
         * In {@link ColumnsLayoutMode#FIXED} this can be still used by setting the {@link VFXTable#columnsSizeProperty()}
         * rather than the width of each column.
         * <p>
         * If the current state is {@link VFXTableState#INVALID} then exits immediately.
         * <p>
         * If the last column in range ({@link #columnsRange()}), has its skin still {@code null}, then we assume that
         * every other column is in the same situation. In such case, we need to 'delay' the operation and wait for the
         * skin to be created, so that we can compute the columns' width.
         * <p>
         * The first pass is to get the widest column by iterating over them, computing the width with
         * {@link VFXTableColumn#computePrefWidth(double)} and keeping the maximum value found.
         * <p>
         * If the state is empty (no rows), the computation ends and the {@link VFXTable#columnsSizeProperty()} is set to:
         * {@code Math.max(fixedW, foundMax + extra)}, where 'fixedW' is the current width specified by the
         * property itself.
         * <p>
         * The second pass is to get the widest cell among the ones in the viewport by using
         * {@link VFXTableRow#getWidthOf(VFXTableColumn, boolean)}. The {@code forceLayout} flag is {@code true} if
         * this operation was 'delayed' before for the aforementioned reasons.
         * <p>
         * Finally, the {@link VFXTable#columnsSizeProperty()} is set to:
         * {@code Math.max(Math.max(fixedW, maxColumnsW + extra), maxCellsW + extra)}, where 'fixedW' is the current width
         * specified by the property itself.
         *
         * @see VFXTable#extraAutosizeWidthProperty()
         */
        @Override
        public void autosizeColumns() {
            VFXTableState<T> state = container.getState();
            if (state == VFXTableState.INVALID) return;
            ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = container.getColumns();

            // It may happen that the columns still have a null skin
            // In such case, we must delay the autosize while also ensuring that layout infos are available
            // by forcing the computation of CSS.
            //
            // Check this by getting the last column in range
            // If the first's skin is still null, then most probably every other column is in the same situation
            IntegerRange columnsRange = columnsRange();
            VFXTableColumn<T, ? extends VFXTableCell<T>> column = columns.get(columnsRange.getMax());
            if (column.getSkin() == null) {
                When.onInvalidated(column.skinProperty())
                    .condition(Objects::nonNull)
                    .then(v -> {
                        forceLayout = true;
                        container.applyCss();
                        autosizeColumns();
                    })
                    .oneShot()
                    .listen();
                return;
            }

            double extra = container.getExtraAutosizeWidth();
            double fixedW = container.getColumnsSize().getWidth();
            double maxColumnsW = columns.stream()
                .mapToDouble(c -> c.computePrefWidth(-1))
                .max()
                .orElse(-1);
            if (state.isEmpty()) {
                container.setColumnsWidth(Math.max(fixedW, maxColumnsW + extra));
                return;
            }

            double maxCellsW = columns.stream()
                .mapToDouble(c -> state.getRowsByIndex().values()
                    .stream()
                    .mapToDouble(r -> r.getWidthOf(c, forceLayout))
                    .max()
                    .orElse(-1.0)
                )
                .max()
                .orElse(-1.0);
            container.setColumnsWidth(Math.max(Math.max(fixedW, maxColumnsW + extra), maxCellsW + extra));
            forceLayout = false;
        }

        /**
         * @return the theoretical number of cells present in the viewport. It's given by {@code visibleRows * visibleColumns},
         * which means that it does not take into account {@code null} cells or anything else
         */
        @Override
        public int visibleCells() {
            int nColumns = visibleColumns();
            int nRows = visibleRows();
            return nColumns * nRows;
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * To check whether the given column is visible this uses its index and the current state's columns range to
         * call {@link IntegerRange#inRangeOf(int, IntegerRange)}.
         * <p>
         * The index is retrieved with {@link VFXTable#indexOf(VFXTableColumn)}.
         */
        @Override
        public boolean isInViewport(VFXTableColumn<T, ?> column) {
            if (column.getTable() == null || column.getScene() == null || column.getParent() == null) return false;
            VFXTableState<T> state = container.getState();
            if (state == VFXTableState.INVALID) return false;
            int index = container.indexOf(column);
            return IntegerRange.inRangeOf(index, state.getColumnsRange());
        }

        @Override
        public void scrollToIndex(Orientation orientation, int index) {
            if (orientation == Orientation.HORIZONTAL) {
                container.setHPos(container.getColumnsSize().getWidth() * index);
            } else {
                container.setVPos(container.getRowsHeight() * index);
            }
        }
    }

    /**
     * Concrete implementation of {@link AbstractHelper} for {@link ColumnsLayoutMode#VARIABLE}.
     * Here the range of rows and columns to display, as well as the viewport position,
     * the virtual max x and y properties are defined as follows:
     * <p> - the columns range is always given by the number of columns in the table - 1. If there are no column, then it
     * will be {@link Utils#INVALID_RANGE}.
     * The computation depends only on the columns' list.
     * <p> - the rows range is given by the {@link #firstRow()} element minus the buffer size {@link VFXTable#rowsBufferSizeProperty()},
     * (cannot be negative) and the sum between this start index and the total number of needed rows given by {@link #totalRows()}.
     * It may happen that the number of indexes given by the range {@code end - start + 1} is
     * lesser than the number of rows we need. In such cases, the range start is corrected to be
     * {@code end - needed + 1}. A typical situation for this is when the table's vertical position reaches the max scroll.
     * If the viewport's height is 0 or the number of needed rows is 0, then the range will be {@link Utils#INVALID_RANGE}.
     * The computation has the following dependencies: the table's height, the column's size (because it also specifies the
     * header height, which influences the viewport's height), the vertical position, the rows buffer size, the rows' height
     * and the items' list size.
     * <p> - the viewport's position, a computation that is at the core of virtual scrolling. The viewport, which contains
     * the columns and the cells (even though the table's viewport is a bit more complex), is not supposed to scroll by insane
     * numbers of pixels both for performance reasons and because it is not necessary.
     * For the vertical positions, first we get the range of rows to display and the rows' height.
     * We compute the range to the first visible row, which is given by {@code IntegerRange.of(range.getMin(), first())},
     * in other words we limit the 'complete' range to the start buffer including the first row after the buffer.
     * The number of indexes in the newfound range (given by {@link IntegerRange#diff()}) is multiplied by the rows' height,
     * this way we are finding the number of pixels to the first visible row, {@code pixelsToFirst}.
     * At this point, we are missing only one last piece of information: how much of the first row do we actually see?
     * We call this amount {@code visibleAmountFirst} and it's given by {@code vPos % size}.
     * Finally, the viewport's vertical position is given by this formula {@code -(pixelsToFirst + visibleAmountFirst)}.
     * Since this layout mode disables virtualization along the x-axis, the horizontal position is simply given by
     * {@code -hPos}.
     * If a range is equal to {@link Utils#INVALID_RANGE}, the respective position will be 0!
     * While it's true that the calculations are more complex and 'needy', it's important to note that this approach
     * allows avoiding 'hacks' to correctly lay out the cells in the viewport. No need for special offsets at the top
     * or bottom anymore.
     * The viewport's position computation has the following dependencies: the vertical and horizontal positions,
     * the rows' height and the columns' size.
     * <p> - the virtual max y property, which gives the total number of pixels on the y-axis.
     * Virtual means that it's not the actual size of the container, rather the size it would have if it was not virtualized.
     * The value is given by the number of rows multiplied by the rows' height. The computation depends on the columns' list,
     * the columns' size (because the viewport height also depends on the height specified by the columns' size property),
     * the table's size (number of items), and the rows' height.
     * <p> - the virtual max x property, which gives the total number of pixels on the x-axis. Since virtualization is
     * disabled in this axis, the value is simply the sum of all the table's columns (to be precise, the vaòue is given by
     * the cache-binding, see below).
     * <p></p>
     * <b>Performance Optimizations</b>
     * Disabling virtualization on a complex 2D structure like {@link VFXTable} is indeed dangerous. While it's safe to
     * assume that the number of columns is pretty much never going to be a big enough number to cause performance issues,
     * it's also true that: 1) we can't be sure on how many columns are actually too many; 2) since it is a 2D structure
     * having {@code n} more columns in the viewport does not mean that we will have {@code n} more nodes in the scene graph.
     * Rather, we can affirm that <b>at best</b> we will have {@code n} more nodes, but keep in mind that for each column
     * there are going to be a number of cells equal to the number of rows.
     * <p>
     * I believe it's worth to optimize the helper as much as possible to mitigate the issue. So, for this reason, this
     * helper makes use of special cache {@link ColumnsLayoutCache} which aims to improve layout operations by avoiding
     * re-computations when they are not needed. For example, if we compute the width and the position of column,
     * then we don't need to re-compute it again when laying out corresponding cells, that would be a waste!
     * <p>
     * The cache can compute columns' widths, their x positions and even check whether they are visible in the viewport.
     * I won't go into many details here on how the cache exactly works, read its docs to know more about it, just know that
     * after the first computation, values will be memorized. Further requests will be as fast as a simple 'getter' method.
     * The cache is also responsible for automatically invalidate the cached values when certain conditions change.
     * <p>
     * For {@link ColumnsLayoutCache} to work properly, this helper defines a series of methods which are actually
     * responsible for the computations. I decided to keep such methods here rather than defining them in the cache mainly
     * for two reasons: 1) I strongly believe such operations are the helper's responsibility; 2) By doing so we generalize
     * the cache class, making it flexible to use, and suitable for more use-cases. These methods are:
     * {@link #computeColumnWidth(VFXTableColumn, boolean)}, {@link #computeColumnPos(int, double)}, {@link #computeVisibility(VFXTableColumn)}.
     */
    @SuppressWarnings("JavadocReference") // I don't know why since the method is public
    class VariableTableHelper<T> extends AbstractHelper<T> {
        private ColumnsLayoutCache<T> layoutCache;
        private boolean forceLayout = false;
        private boolean forceAll = false;

        public VariableTableHelper(VFXTable<T> table) {
            super(table);
            createBindings();
        }

        /**
         * This is used by the {@link ColumnsLayoutCache} to compute the width of the given column.
         * The value is given by {@code Math.max(minW, prefW)}, where:
         * <p> - {@code minW} is given by {@link VFXTable#columnsSizeProperty()}
         * <p> - {@code prefW} is given by {@link VFXTableColumn#prefWidth(double)}
         * <p>
         * If there's only one column in the table, then the returned value is the maximum between the above formula and
         * the table's width.
         * <p>
         * If the column is the last one in the list, then the final value is given by
         * {@code Math.max(Math.max(minW, prefW), tableW - partialW)}, where {@code partialW} is given by
         * {@link ColumnsLayoutCache#getPartialWidth()}.
         */
        protected double computeColumnWidth(VFXTableColumn<T, ?> column, boolean isLast) {
            double minW = container.getColumnsSize().getWidth();
            double prefW = Math.max(column.prefWidth(-1), minW);
            if (container.getColumns().size() == 1) return Math.max(prefW, container.getWidth());
            if (!isLast) return column.snapSizeX(prefW);

            double partialW = layoutCache.getPartialWidth();
            return column.snapSizeX(Math.max(prefW, container.getWidth() - partialW));
        }

        /**
         * This is used by the {@link ColumnsLayoutCache} to compute the x position of a given column (by its index)
         * and the last know position. For example, let's suppose I want to lay out the column at index 1, and the column
         * at index 0 (the previous one) is 100px wider. The {@code prevPos} parameter passed to this method will be 100px.
         * This makes the computation for index 1 easy, as it is simply is the sum {@code prevPos + col1Width}.
         */
        protected double computeColumnPos(int index, double prevPos) {
            VFXTableColumn<T, ? extends VFXTableCell<T>> column = container.getColumns().get(index);
            return column.snapPositionX(prevPos + layoutCache.getColumnWidth(column));
        }

        /**
         * This is used by the {@link ColumnsLayoutCache} to compute the visibility of a given column (and all its
         * related cells ofc). There are a lot of requirements for this check but the concept is quite simple.
         * <p>
         * Before getting all the dependencies, we ensure that the column's table instance is not {@code null}, that its index
         * is not negative, that the column is in the scene graph ({@code null} check on the column's Scene and Parent).
         * If any of these conditions fail {@code false} is returned.
         * <p>
         * The idea is to use something similar to {@link Bounds#intersects(Bounds)} but only for the width and x position.
         * First we compute the viewport bounds which are given by {@code [hPos, hPos + tableWidth]}, then we get the
         * column's position and width by using {@link ColumnsLayoutCache#getColumnPos(int)} and {@link ColumnsLayoutCache#getColumnWidth(VFXTableColumn)}.
         * <p>
         * The result is given by this formula: {@code (columnX + columnWidth >= vBounds.getMin()) && (columnX <= vBounds.getMax())}
         */
        protected boolean computeVisibility(VFXTableColumn<T, ?> column) {
            VFXTable<T> table = column.getTable();
            int index = column.getIndex();
            if (table == null ||
                index < 0 ||
                column.getScene() == null ||
                column.getParent() == null
            ) return false;
            try {
                double tableW = table.getWidth();
                double hPos = table.getHPos();
                DoubleRange viewBounds = DoubleRange.of(hPos, hPos + tableW);
                double columnX = layoutCache.getColumnPos(index);
                double columnW = layoutCache.getColumnWidth(column);
                return (columnX + columnW >= viewBounds.getMin()) && (columnX <= viewBounds.getMax());
            } catch (Exception ex) {
                return false;
            }
        }

        @Override
        protected void createBindings() {
            // Initialize layout cache
            layoutCache = new ColumnsLayoutCache<>(container)
                .setWidthFunction(this::computeColumnWidth)
                .setPositionFunction(this::computeColumnPos)
                .setVisibilityFunction(this::computeVisibility)
                .init();

            // Initialize bindings
            columnsRange.bind(ObjectBindingBuilder.<IntegerRange>build()
                .setMapper(() -> {
                    ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = container.getColumns();
                    if (columns.isEmpty()) return Utils.INVALID_RANGE;
                    return IntegerRange.of(0, columns.size() - 1);
                })
                .addSources(container.getColumns())
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
                .addSources(container.heightProperty(), container.columnsSizeProperty())
                .addSources(container.vPosProperty())
                .addSources(container.rowsBufferSizeProperty())
                .addSources(container.sizeProperty(), container.rowsHeightProperty())
                .get()
            );

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
                    return Position.of(x, y);
                })
                .addSources(container.layoutBoundsProperty())
                .addSources(container.vPosProperty(), container.hPosProperty())
                .addSources(container.rowsHeightProperty(), container.columnsSizeProperty())
                .get()
            );

            super.createBindings();
        }

        @Override
        protected DoubleBinding createVirtualMaxXBinding() {
            return null; // bound to the cache
        }

        @Override
        protected DoubleBinding createVirtualMaxYBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> container.getColumns().isEmpty() ? 0.0 : container.size() * container.getRowsHeight())
                .addSources(container.getColumns())
                .addSources(container.columnsSizeProperty())
                .addSources(container.rowsHeightProperty())
                .get();
        }

        /**
         * Always 0.
         */
        @Override
        public int firstColumn() {
            return 0;
        }

        /**
         * Always the size of {@link VFXTable#getColumns()}.
         */
        @Override
        public int visibleColumns() {
            return container.getColumns().size();
        }

        /**
         * Always the size of {@link VFXTable#getColumns()}.
         */
        @Override
        public int totalColumns() {
            return container.getColumns().size();
        }

        /**
         * Delegates to {@link ColumnsLayoutCache#getColumnWidth(VFXTableColumn)}.
         */
        @Override
        public double getColumnWidth(VFXTableColumn<T, ?> column) {
            return layoutCache.getColumnWidth(column);
        }

        /**
         * Delegates to {@link ColumnsLayoutCache#getColumnPos(int)}.
         */
        @Override
        public double getColumnPos(int layoutIdx, VFXTableColumn<T, ?> column) {
            return layoutCache.getColumnPos(layoutIdx);
        }

        /**
         * Delegates to {@link ColumnsLayoutCache#isInViewport(VFXTableColumn)}.
         */
        @Override
        public boolean isInViewport(VFXTableColumn<T, ?> column) {
            return layoutCache.isInViewport(column);
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * In {@link ColumnsLayoutMode#VARIABLE} the {@code layoutIndex} is always the same as the column's index.
         * <p>
         * Positions the column at {@code X: getColumnPos(index, column)} and {@code Y: 0}.
         * <p>
         * Sizes the column to {@code W: getColumnWidth(column)} and {@code H: columnsHeight}
         * <p></p>
         * Additionally, this method makes use of the 'inViewport' functionality to hide and not lay out columns when they
         * are not visible in the viewport. The layout operation is also avoided in case the column is visible and both
         * its x positions and width are already good to go.
         *
         * @return {@code false} if the column was hidden or no layout operation was run, {@code true} otherwise
         */
        @Override
        public boolean layoutColumn(int layoutIdx, VFXTableColumn<T, ?> column) {
            if (!isInViewport(column)) {
                column.setVisible(false);
                return false;
            }
            Size size = getContainer().getColumnsSize();
            double x = getColumnPos(layoutIdx, column);
            double w = getColumnWidth(column);
            double h = size.getHeight();
            if (column.isVisible() && column.getLayoutX() == x && column.getWidth() == w) return false;
            column.resizeRelocate(x, 0, w, h);
            column.setVisible(true);
            return true;
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * In {@link ColumnsLayoutMode#VARIABLE} the {@code layoutIndex} is always the same as the column's index.
         * <p>
         * The layout logic is the exact same as described here {@link #layoutColumn(int, VFXTableColumn)}.
         * <p>
         * Here's where the {@link ColumnsLayoutCache} shines. Since cells are laid out the exact same way as
         * the corresponding column, the width, positions and visibility computations are already done when invoking
         * {@link #layoutColumn(int, VFXTableColumn)}. Obviously, to do so, we first need to get the cell's corresponding
         * column from {@link VFXTable#getColumns()} by the given index.
         * <p></p>
         * Note: the pre/post layout hooks defined by {@link VFXCell} are called even if the cell will only be set to
         * hidden. This allows implementations to perform actions depending on the visibility state without relying on
         * listeners.
         */
        @Override
        public boolean layoutCell(int layoutIdx, VFXTableCell<T> cell) {
            ObservableList<VFXTableColumn<T, ? extends VFXTableCell<T>>> columns = container.getColumns();
            VFXTableColumn<T, ? extends VFXTableCell<T>> column = columns.get(layoutIdx);
            Node node = cell.toNode();
            cell.beforeLayout();
            if (!isInViewport(column)) {
                node.setVisible(false);
                cell.afterLayout();
                return false;
            }
            double w = getColumnWidth(column);
            double h = getContainer().getRowsHeight();
            double x = getColumnPos(layoutIdx, column);
            if (node.isVisible() && node.getLayoutX() == x && node.getLayoutBounds().getWidth() == w) return false;
            node.resizeRelocate(x, 0, w, h);
            node.setVisible(true);
            cell.afterLayout();
            return true;
        }

        /**
         * If the current state is {@link VFXTableState#INVALID} then exits immediately.
         * <p>
         * If the given column's skin is still {@code null}, then we must 'delay' the operation and wait for the skin to
         * be created, so that we can compute the column's width.
         * <p>
         * The first pass is to get the column's ideal width which is given by {@code Math.max(minW, prefW) + extra} where:
         * <p> - {@code minW} is specified by {@link VFXTable#columnsSizeProperty()}
         * <p> - {@code prefW} is obtained by calling {@link VFXTableColumn#computePrefWidth(double)}
         * <p> - {@code extra} is an extra number of pixels added to the final value specified by {@link VFXTable#extraAutosizeWidthProperty()}
         * <p>
         * If the state is empty (no rows), the computation ends and the column's width is set to the value found by the
         * above formula.
         * <p>
         * The second pass is to get the widest cell among the ones in the viewport by using
         * {@link VFXTableRow#getWidthOf(VFXTableColumn, boolean)}. The {@code forceLayout} flag is {@code true} if
         * this operation was 'delayed' before for the aforementioned reasons.
         * <p>
         * Finally, the column's width is set to: {@code Math.max(Math.max(minW, prefW), maxCellsWidth) + extra}.
         * <p>
         * <b>Note:</b> the columns are resized using the method {@link VFXTableColumn#resize(double)}.
         *
         * @see VFXTableColumn
         * @see VFXTable#extraAutosizeWidthProperty()
         */
        @Override
        public void autosizeColumn(VFXTableColumn<T, ?> column) {
            VFXTableState<T> state = container.getState();
            if (state == VFXTableState.INVALID) return;

            // It may happen that the column still has a null skin
            // In such cases, we must delay the autosize while also ensuring that layout infos are available
            // by forcing the computation of CSS.
            if (column.getSkin() == null) {
                new OnInvalidated<>(column.skinProperty()) {
                    static boolean forced = false;

                    {
                        condition(Objects::nonNull);
                        then(v -> {
                            if (!forced) {
                                forced = true;
                                forceLayout = true;
                                container.applyCss();
                            }
                            autosizeColumn(column);
                        });
                        oneShot();
                        listen();
                    }
                };
                return;
            }

            double extra = container.getExtraAutosizeWidth();
            double minW = container.getColumnsSize().getWidth();
            double prefW = column.computePrefWidth(-1);
            if (state.isEmpty()) {
                column.resize(Math.max(minW, prefW) + extra);
                return;
            }

            double maxCellsW = state.getRowsByIndex().values().stream()
                .mapToDouble(r -> r.getWidthOf(column, forceLayout))
                .max()
                .orElse(-1.0);
            column.resize(Math.max(Math.max(minW, prefW), maxCellsW) + extra);
            if (!forceAll) forceLayout = false;
        }

        /**
         * This simply calls {@link #autosizeColumn(VFXTableColumn)} on all the table's columns.
         */
        @Override
        public void autosizeColumns() {
            VFXTableState<T> state = container.getState();
            if (state == VFXTableState.INVALID) return;
            forceAll = true;
            container.getColumns().forEach(this::autosizeColumn);
            forceLayout = false;
        }

        /**
         * @return the number of cells for which the corresponding column is visible in the viewport
         * @see #isInViewport(VFXTableColumn)
         */
        @Override
        public int visibleCells() {
            VFXTableState<T> state = container.getState();
            if (state.isEmpty()) return 0;
            IntegerRange cRange = state.getColumnsRange();
            int nRows = state.getRowsRange().diff() + 1;
            int nCellsPerRow = (int) IntStream.rangeClosed(cRange.getMin(), cRange.getMax())
                .mapToObj(container.getColumns()::get)
                .filter(this::isInViewport)
                .count();
            return nRows * nCellsPerRow;
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

        /**
         * {@inheritDoc}
         * <p></p>
         * Overridden here to also dispose the {@link ColumnsLayoutCache}.
         */
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
