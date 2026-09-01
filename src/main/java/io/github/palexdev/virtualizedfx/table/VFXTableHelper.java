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
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;

import static io.github.palexdev.mfxcore.base.beans.Position.position;

public interface VFXTableHelper<T> extends VFXContainerHelper<T, VFXTable<T>> {

    //================================================================================
    // Columns
    //================================================================================

    int firstColumn();

    int lastColumn();

    int visibleColumns();

    int totalColumns();

    default int columnsCount() {
        return getContainer().columns().size();
    }

    ObservableValue<IntegerRange> columnsRangeProperty();

    default IntegerRange columnsRange() {
        return columnsRangeProperty().getValue();
    }

    int onColumnWidthChanged(VFXTableColumn<T, ?> column);

    void onColumnsSizeChanged();

    void onColumnsChanged(ListChangeListener.Change<? extends VFXTableColumn<T, ?>> change);

    void layoutColumn(int columnIdx, VFXTableColumn<T, ?> column);

    //================================================================================
    // Rows
    //================================================================================

    int firstRow();

    int lastRow();

    int visibleRows();

    int totalRows();

    ObservableValue<IntegerRange> rowsRangeProperty();

    default IntegerRange rowsRange() {
        return rowsRangeProperty().getValue();
    }

    void layoutRow(int layoutIdx, VFXTableRow<T> row);

    default VFXTableRow<T> indexToRow(int index) {
        T item = indexToItem(index);
        return itemToRow(item);
    }

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

    default int visibleCells() {
        int nColumns = visibleColumns();
        int nRows = visibleRows();
        return nColumns * nRows;
    }

    default int totalCells() {
        int nColumns = columnsRange().diff() + 1;
        int nRows = rowsRange().diff() + 1;
        return Math.max(0, nColumns * nRows);
    }

    void layoutCell(int columnIdx, VFXTableCell<T> cell);

    //================================================================================
    // Misc
    //================================================================================

    void invalidateRange(Orientation axis);

    default double viewportHeight() {
        VFXTable<T> table = getContainer();
        return Math.max(0, table.getHeight() - table.getColumnsSize().height());
    }

    default void scrollBy(Orientation orientation, double pixels) {
        VFXTable<T> table = getContainer();
        if (orientation == Orientation.HORIZONTAL) {
            table.setHPos(table.getHPos() + pixels);
        } else {
            table.setVPos(table.getVPos() + pixels);
        }
    }

    default void scrollToPixel(Orientation orientation, double pixel) {
        VFXTable<T> table = getContainer();
        if (orientation == Orientation.HORIZONTAL) {
            table.setHPos(pixel);
        } else {
            table.setVPos(pixel);
        }
    }

    void scrollToIndex(Orientation orientation, int index);

    //================================================================================
    // Impl
    //================================================================================

    class VFXDefaultTableHelper<T> extends VFXContainerHelperBase<T, VFXTable<T>> implements VFXTableHelper<T> {

        private ObjectBinding<IntegerRange> columnsRange;
        private ObjectBinding<IntegerRange> rowsRange;

        private final ColumnsLayoutCache<T> layoutCache;

        public VFXDefaultTableHelper(VFXTable<T> table) {
            layoutCache = new ColumnsLayoutCache<>(table).init();
            super(table);
            createBindings();
        }

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
                .addSources(container.columns(), container.columnsSizeProperty())
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
                .addSources(container.columnsSizeProperty())
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
                    return position(x, y);
                })
                .addSources(container.layoutBoundsProperty())
                .addSources(container.vPosProperty(), container.hPosProperty())
                .addSources(container.rowsHeightProperty(), container.columnsSizeProperty())
                .get());
        }

        @Override
        protected DoubleBinding createVirtualMaxXBinding() {
            return layoutCache;
        }

        @Override
        protected DoubleBinding createVirtualMaxYBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> (columnsCount() == 0) ? 0.0 : container.size() * container.getRowsHeight())
                .addSources(container.sizeProperty(), container.columns())
                .addSources(container.rowsHeightProperty(), container.columnsSizeProperty())
                .get();
        }

        @Override
        protected DoubleBinding createMaxVScrollBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> Math.max(0, getVirtualMaxY() - viewportHeight()))
                .addSources(virtualMaxY, container.heightProperty(), container.columnsSizeProperty())
                .get();
        }

        @Override
        public int firstColumn() {
            if (columnsCount() == 0) return 0;
            return NumberUtils.clamp(columnAt(container.getHPos()), 0, columnsCount() - 1);
        }

        @Override
        public int lastColumn() {
            return columnsRange().getMax();
        }

        @Override
        public int visibleColumns() {
            if (columnsCount() == 0 || container.getWidth() <= 0) return 0;
            return columnAt(container.getHPos() + container.getWidth()) - firstColumn() + 1;
        }

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

        @Override
        public int onColumnWidthChanged(VFXTableColumn<T, ?> column) {
            return layoutCache.onColumnWidthChanged(column);
        }

        @Override
        public void onColumnsSizeChanged() {
            layoutCache.onColumnsSizeChanged();
        }

        @Override
        public void onColumnsChanged(ListChangeListener.Change<? extends VFXTableColumn<T, ?>> change) {
            layoutCache.onColumnsChanged(change);
        }

        @Override
        public void layoutColumn(int columnIdx, VFXTableColumn<T, ?> column) {
            column.resizeRelocate(
                layoutCache.posAt(columnIdx),
                0,
                layoutCache.widthAt(columnIdx),
                container.getColumnsSize().height()
            );
        }

        @Override
        public int firstRow() {
            return NumberUtils.clamp(
                (int) Math.floor(container.getVPos() / container.getRowsHeight()),
                0,
                container.size() - 1
            );
        }

        @Override
        public int lastRow() {
            return rowsRange().getMax();
        }

        @Override
        public int visibleRows() {
            double height = container.getRowsHeight();
            return height > 0 ? (int) Math.ceil(viewportHeight() / height) : 0;
        }

        @Override
        public int totalRows() {
            int visible = visibleRows();
            return visible == 0 ? 0 : Math.min(visible + container.getRowsBufferSize().val() * 2, container.size());
        }

        @Override
        public ObservableValue<IntegerRange> rowsRangeProperty() {
            return rowsRange;
        }

        @Override
        public void layoutRow(int layoutIdx, VFXTableRow<T> row) {
            double h = container.getRowsHeight();
            row.beforeLayout();
            row.resizeRelocate(0, layoutIdx * h, getVirtualMaxX(), h);
            row.afterLayout();
        }

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

        @Override
        public void scrollToIndex(Orientation orientation, int index) {
            if (orientation == Orientation.HORIZONTAL) {
                if (index < 0 || index >= columnsCount()) return;
                container.setHPos(layoutCache.posAt(index));
            } else {
                container.setVPos(container.getRowsHeight() * index);
            }
        }

        @Override
        public void invalidateRange(Orientation axis) {
            if (axis == Orientation.HORIZONTAL) columnsRange.invalidate();
            else rowsRange.invalidate();
        }

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
