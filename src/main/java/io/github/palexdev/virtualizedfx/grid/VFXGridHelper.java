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

package io.github.palexdev.virtualizedfx.grid;

import java.util.Optional;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.NumberRange;
import io.github.palexdev.mfxcore.base.properties.SizeProperty;
import io.github.palexdev.mfxcore.base.properties.range.IntegerRangeProperty;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.virtualizedfx.base.VFXContainerHelper;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.Node;

/**
 * This interface is a utility API for {@link VFXGrid}, despite computations not depending on other properties
 * (some VFXList values depend on the orientation, for example),
 * it's still a nice way to adhere to the encapsulation and separation of concerns principles.
 * Has one concrete implementation: {@link DefaultHelper}.
 */
public interface VFXGridHelper<T, C extends VFXCell<T>> extends VFXContainerHelper<T, VFXGrid<T, C>> {

    /**
     * @return the maximum number of columns the grid can have. This value is essentially the same as
     * {@link VFXGrid#columnsNumProperty()} but it's also taken into account the number of items (you can't have more
     * columns than the number of items)
     */
    int maxColumns();

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
     * @return the maximum number of rows the grid can have. This value depends on the number of items and the number of
     * columns
     */
    int maxRows();

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
     * Lays out the given cell.
     * The row and column layout indexes are necessary to identify the position of a cell among the others
     * (comes before/after, above/below).
     *
     * @see VFXGridSkin#layout()
     */
    void layout(int rowLayoutIndex, int columnLayoutIndex, VFXCell<T> cell);

    /**
     * @return the total size of each cell, given by the {@link VFXGrid#cellSizeProperty()} summed to the horizontal and
     * vertical spacing values
     */
    Size getTotalCellSize();

    /**
     * @return the theoretical number of cells in the viewport. The value depends on the number of visible columns and rows,
     * however, doesn't take into account the possibility of incomplete columns/rows. For a precise value,
     * use {@link #totalCells()} instead
     */
    default int visibleCells() {
        int nColumns = visibleColumns();
        int nRows = visibleRows();
        return nColumns * nRows;
    }

    /**
     * @return the precise number of cells present in the viewport at a given time. The value depends on the current range
     * of rows and columns. Unfortunately, it's not very efficient as the count is computed by iterating over each row and
     * column, but it's the only stable way I found to have a correct value.
     */
    default int totalCells() {
        // TODO can't find a better algorithm, probably with some nasty stupid math formula that I hate so much
        VFXGrid<T, C> grid = getContainer();
        int cnt = 0;
        int maxColumns = maxColumns();
        IntegerRange rRange = rowsRange();
        IntegerRange cRange = columnsRange();
        int nColumns = cRange.diff() + 1;
        for (Integer rIdx : rRange) {
            int linear = GridUtils.subToInd(maxColumns, rIdx, cRange.getMax());
            if (linear < grid.size()) {
                cnt += nColumns;
                continue;
            }
            int rowStart = GridUtils.subToInd(maxColumns, rIdx, cRange.getMin());
            int max = Math.min(grid.size(), linear) - 1;
            if (max < rowStart) break;
            cnt += max - rowStart + 1;
        }
        return cnt;
    }

    /**
     * Scrolls to the given row index by setting the {@link VFXGrid#vPosProperty()} to {@code rowIndex * totalCellHeight}.
     */
    default void scrollToRow(int row) {
        double h = getTotalCellSize().getHeight();
        getContainer().setVPos((row * h));
    }

    /**
     * Scrolls to the given column index by setting the {@link VFXGrid#hPosProperty()} to {@code columnIndex * totalCellWidth}.
     */
    default void scrollToColumn(int column) {
        double w = getTotalCellSize().getWidth();
        getContainer().setHPos((column * w));
    }

    /**
     * Converts the given index to a cell. Uses {@link #itemToCell(Object)}.
     */
    default C indexToCell(int index) {
        T item = indexToItem(index);
        return itemToCell(item);
    }

    /**
     * Converts the given item to a cell. The result is either on of the cells cached in {@link VFXCellsCache} that
     * is updated with the given item, or a totally new one created by the {@link VFXGrid#getCellFactory()}.
     */
    default C itemToCell(T item) {
        VFXGrid<T, C> grid = getContainer();
        VFXCellsCache<T, C> cache = grid.getCache();
        Optional<C> opt = cache.tryTake();
        opt.ifPresent(c -> c.updateItem(item));
        return opt.orElseGet(() -> grid.create(item));
    }

    /**
     * Concrete implementation of {@link VFXGridHelper}, here the range of rows and columns to display, as well as the
     * viewport position, the virtual max x and y properties are defined as follows:
     * <p> - the columns range is given by the {@link #firstColumn()} element minus the buffer size {@link VFXGrid#bufferSizeProperty()},
     * (cannot be negative) and the sum between this start index and the total number of needed columns given by {@link #totalColumns()}
     * (cannot exceed the maximum number of columns {@link #maxColumns()}). It may happen that the number of indexes given
     * by the range {@code end - start + 1} is lesser than the total number of columns we need. In such cases, the range
     * start is corrected to be {@code end - needed + 1}. A typical situation for this is when the grid's horizontal position
     * reaches the max scroll.
     * The range computation has the following dependencies: the number of columns, the grid's width, horizontal position,
     * the buffer size, the number of items, the cell size and the horizontal spacing.
     * <p> - the rows range is given by the {@link #firstRow()} element minus the buffer size {@link VFXGrid#bufferSizeProperty()},
     * (cannot be negative) and the sum between this start index and the total number of needed rows given by {@link #totalRows()}
     * (cannot exceed the maximum number of rows {@link #maxRows()}). It may happen that the number of indexes given
     * by the range {@code end - start + 1} is lesser than the total number of rows we need. In such cases, the range
     * start is corrected to be {@code end - needed + 1}. A typical situation for this is when the grid's vertical position
     * reaches the max scroll.
     * The range computation has the following dependencies: the number of columns, the grid's height, vertical position,
     * the buffer size, the number of items, the cell size and the vertical spacing.
     * <p> - the viewport position, a computation that is at the core of virtual scrolling. The viewport, which contains the cells,
     * is not supposed to scroll by insane numbers of pixels both for performance reasons and because it is not necessary.
     * For both the horizontal and vertical positions, we use the same technique, just using the appropriate values according
     * to the axis we are working on.
     * First we get the range of rows/columns to display, then the total cell size given by {@link #getTotalCellSize()},
     * yes, the spacing also affects the position. Then we compute the ranges to the first visible row/column, which
     * are given by {@code IntegerRange.of(range.getMin(), first())}, in other words we limit the 'complete' ranges to the
     * start buffer including the first row/column after the buffer. The number of indexes in the newfound ranges
     * (given by {@link IntegerRange#diff()}) is multiplied by the total cell size, this way we are finding the number of pixels to the
     * first visible cell, {@code pixelsToFirst}. We are missing only one last piece of information: how much of the first row/column
     * do we actually see? We call this amount {@code visibleAmountFirst} and it's given by {@code pos % totalCellSize}.
     * Finally, the viewport's position is given by this formula {@code -(pixelsToFirst + visibleAmountFirst)}
     * (for both hPos and vPos of course).
     * While it's true that the calculations are more complex and 'needy', it's important to note that this approach
     * allows avoiding 'hacks' to correctly lay out the cells in the viewport. No need for special offsets at the top
     * or bottom anymore.
     * The viewport's position computation has the following dependencies: the horizontal position, the vertical position,
     * the cell size and both the vertical and horizontal spacing.
     * <p> - the virtual max x and y properties, which give the total number of pixels on the x-axis and y-axis. Virtual
     * means that it's not the actual size of the container, rather the size it would have if it was not virtualized.
     * The two values are given by the max number of rows/columns multiplied by the total cell size, minus the spacing
     * (otherwise we would have the spacing applied between the last row/column and the grid's border too).
     * The computations have the following dependencies: the number of items, the number of columns, the cell size and
     * the horizontal/vertical spacing (respectively).
     */
    class DefaultHelper<T, C extends VFXCell<T>> extends VFXContainerHelperBase<T, VFXGrid<T, C>> implements VFXGridHelper<T, C> {
        protected final IntegerRangeProperty columnsRange = new IntegerRangeProperty();
        protected final IntegerRangeProperty rowsRange = new IntegerRangeProperty();
        protected final SizeProperty totalCellSize = new SizeProperty(Size.zero());

        public DefaultHelper(VFXGrid<T, C> grid) {
            super(grid);
            createBindings();
        }

        @Override
        protected void createBindings() {
            columnsRange.bind(ObjectBindingBuilder.<IntegerRange>build()
                .setMapper(() -> {
                    if (container.getWidth() <= 0) return Utils.INVALID_RANGE;
                    int needed = totalColumns();
                    if (needed == 0) return Utils.INVALID_RANGE;

                    int start = Math.max(0, firstColumn() - container.getBufferSize().val());
                    int end = Math.min(maxColumns() - 1, start + needed - 1);
                    if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
                    return IntegerRange.of(start, end);
                })
                .addSources(container.columnsNumProperty())
                .addSources(container.widthProperty())
                .addSources(container.hPosProperty())
                .addSources(container.bufferSizeProperty())
                .addSources(container.sizeProperty(), container.cellSizeProperty(), container.hSpacingProperty())
                .get()
            );
            rowsRange.bind(ObjectBindingBuilder.<IntegerRange>build()
                .setMapper(() -> {
                    if (container.getHeight() <= 0) return Utils.INVALID_RANGE;
                    int needed = totalRows();
                    if (needed == 0) return Utils.INVALID_RANGE;

                    int start = Math.max(0, firstRow() - container.getBufferSize().val());
                    int end = Math.min(maxRows() - 1, start + needed - 1);
                    if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
                    return IntegerRange.of(start, end);
                })
                .addSources(container.columnsNumProperty())
                .addSources(container.heightProperty())
                .addSources(container.vPosProperty())
                .addSources(container.bufferSizeProperty())
                .addSources(container.sizeProperty(), container.cellSizeProperty(), container.vSpacingProperty())
                .get()
            );

            viewportPosition.bind(ObjectBindingBuilder.<Position>build()
                .setMapper(() -> {
                    if (container.isEmpty()) return Position.origin();
                    IntegerRange rowsRange = rowsRange();
                    IntegerRange columnsRange = columnsRange();
                    if (Utils.INVALID_RANGE.equals(rowsRange) || Utils.INVALID_RANGE.equals(columnsRange))
                        return Position.origin();

                    Size size = getTotalCellSize();
                    IntegerRange rRangeToFirstVisible = IntegerRange.of(rowsRange.getMin(), firstRow());
                    double rPixelsToFirst = rRangeToFirstVisible.diff() * size.getHeight();
                    double rVisibleAmount = container.getVPos() % size.getHeight();
                    IntegerRange cRangeToFirstVisible = IntegerRange.of(columnsRange.getMin(), firstColumn());
                    double cPixelsToFirst = cRangeToFirstVisible.diff() * size.getWidth();
                    double cVisibleAmount = container.getHPos() % size.getWidth();

                    double x = -(cPixelsToFirst + cVisibleAmount);
                    double y = -(rPixelsToFirst + rVisibleAmount);
                    return Position.of(x, y);

                })
                .addSources(container.layoutBoundsProperty())
                .addSources(container.vPosProperty(), container.hPosProperty())
                .addSources(container.cellSizeProperty())
                .addSources(container.hSpacingProperty(), container.vSpacingProperty())
                .get()
            );

            totalCellSize.bind(ObjectBindingBuilder.<Size>build()
                .setMapper(() -> {
                    Size size = container.getCellSize();
                    return Size.of(
                        size.getWidth() + container.getHSpacing(),
                        size.getHeight() + container.getVSpacing()
                    );
                })
                .addSources(container.cellSizeProperty(), container.vSpacingProperty(), container.hSpacingProperty())
                .get()
            );
            super.createBindings();
        }

        @Override
        protected DoubleBinding createVirtualMaxXBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> (maxColumns() * getTotalCellSize().getWidth()) - container.getHSpacing())
                .addSources(container.columnsNumProperty(), totalCellSize)
                .get();
        }

        @Override
        protected DoubleBinding createVirtualMaxYBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> (maxRows() * getTotalCellSize().getHeight()) - container.getVSpacing())
                .addSources(container.columnsNumProperty(), totalCellSize)
                .get();
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code Math.min(nItems, nColumns)}.
         */
        @Override
        public int maxColumns() {
            return Math.min(container.size(), container.getColumnsNum());
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code Math.floor(hPos / totalCellWidth)}, clamped between 0 and {@link #maxColumns()} - 1.
         */
        @Override
        public int firstColumn() {
            return NumberUtils.clamp(
                (int) Math.floor(container.getHPos() / getTotalCellSize().getWidth()),
                0,
                maxColumns() - 1
            );
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by the max in {@link #columnsRange()}.
         */
        @Override
        public int lastColumn() {
            return columnsRange().getMax();
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code Math.ceil(gridWidth / totalCellWidth)}. 0 if the total cells' width is also 0.
         */
        @Override
        public int visibleColumns() {
            double width = getTotalCellSize().getWidth();
            return width > 0 ?
                (int) Math.ceil(container.getWidth() / width) :
                0;
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@link #visibleColumns()} plus double the value of {@link VFXGrid#bufferSizeProperty()} restricted to
         * the maximum number of columns allowed, {@link #maxColumns()}.
         */
        @Override
        public int totalColumns() {
            int visible = visibleColumns();
            return visible == 0 ? 0 : Math.min(visible + container.getBufferSize().val() * 2, maxColumns());
        }

        @Override
        public ReadOnlyObjectProperty<NumberRange<Integer>> columnsRangeProperty() {
            return columnsRange.getReadOnlyProperty();
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code Math.ceil(itemsNum / maxColumns())}, see {@link #maxColumns()}.
         */
        @Override
        public int maxRows() {
            return NumberUtils.clamp(
                (int) Math.ceil((double) container.size() / maxColumns()),
                0,
                container.size()
            );
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code Math.floor(vPos / totalCellHeight)}, clamped between 0 and {@link #maxRows()} - 1.
         */
        @Override
        public int firstRow() {
            return NumberUtils.clamp(
                (int) Math.floor(container.getVPos() / getTotalCellSize().getHeight()),
                0,
                maxRows() - 1
            );
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by the max in {@link #maxRows()}.
         */
        @Override
        public int lastRow() {
            return rowsRange().getMax();
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@code Math.ceil(gridHeight / totalCellHeight)}.
         */
        @Override
        public int visibleRows() {
            double height = getTotalCellSize().getHeight();
            return height > 0 ?
                (int) Math.ceil(container.getHeight() / height) :
                0;
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Given by {@link #visibleRows()} plus double the value of {@link VFXGrid#bufferSizeProperty()} restricted to
         * the maximum number of rows allowed, {@link #maxRows()}.
         */
        @Override
        public int totalRows() {
            int visible = visibleRows();
            return visible == 0 ? 0 : Math.min(visible + container.getBufferSize().val() * 2, maxRows());
        }

        @Override
        public ReadOnlyObjectProperty<NumberRange<Integer>> rowsRangeProperty() {
            return rowsRange;
        }

        @Override
        public ReadOnlyDoubleProperty virtualMaxXProperty() {
            return virtualMaxX.getReadOnlyProperty();
        }

        @Override
        public ReadOnlyDoubleProperty virtualMaxYProperty() {
            return virtualMaxY.getReadOnlyProperty();
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
            return viewportPosition.getReadOnlyProperty();
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * The x position is given by {@code totalCellWidth * columnIndex}, the y position is given by
         * {@code totalCellHeight * rowIndex}, the width and height are given by the {@link VFXGrid#cellSizeProperty()}.
         */
        @Override
        public void layout(int rowLayoutIndex, int columnLayoutIndex, VFXCell<T> cell) {
            Node node = cell.toNode();
            double x = getTotalCellSize().getWidth() * columnLayoutIndex;
            double y = getTotalCellSize().getHeight() * rowLayoutIndex;
            double w = container.getCellSize().getWidth();
            double h = container.getCellSize().getHeight();
            cell.beforeLayout();
            node.resizeRelocate(x, y, w, h);
            cell.afterLayout();
        }

        @Override
        public Size getTotalCellSize() {
            return totalCellSize.get();
        }

        @Override
        public void dispose() {
            columnsRange.unbind();
            rowsRange.unbind();
            viewportPosition.unbind();
            totalCellSize.unbind();
            super.dispose();
        }
    }
}
