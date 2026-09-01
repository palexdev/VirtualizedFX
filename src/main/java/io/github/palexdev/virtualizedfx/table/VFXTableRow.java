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

import java.util.ArrayList;
import java.util.List;
import java.util.SequencedMap;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.controls.MFXStyleable;
import io.github.palexdev.virtualizedfx.base.VFXContext;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap.RowsStateMap;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap.StateMapBase;
import io.github.palexdev.virtualizedfx.utils.Utils;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import static io.github.palexdev.mfxcore.base.beans.range.IntegerRange.inRangeOf;
import static java.util.Optional.ofNullable;

public abstract class VFXTableRow<T> extends Region implements VFXCell<T>, MFXStyleable {

    //================================================================================
    // Properties
    //================================================================================

    private VFXContext<T> context;

    private final ReadOnlyIntegerWrapper index = new ReadOnlyIntegerWrapper(-1);
    private final ReadOnlyObjectWrapper<T> item = new ReadOnlyObjectWrapper<>();

    protected IntegerRange columnsRange = Utils.INVALID_RANGE;
    protected RowsStateMap<T, VFXTableCell<T>> cells;

    protected int dirtyFrom = Integer.MAX_VALUE;
    protected int dirtyTo = Integer.MIN_VALUE;

    //================================================================================
    // Constructors
    //================================================================================

    public VFXTableRow(T item) {
        cells = new RowsStateMap<>();
        updateItem(item);
        setDefaultStyleClasses();
    }

    //================================================================================
    // Methods
    //================================================================================

    @SuppressWarnings("unchecked")
    protected void updateColumns(IntegerRange columnsRange, boolean listChanged) {
        if (!listChanged && this.columnsRange.equals(columnsRange)) return;

        VFXTable<T> table = getTable();
        RowsStateMap<T, VFXTableCell<T>> nCells = new RowsStateMap<>();
        List<Node> added = new ArrayList<>();
        for (Integer index : columnsRange) {
            VFXTableColumn<T, VFXTableCell<T>> column = (VFXTableColumn<T, VFXTableCell<T>>) table.columns().get(index);
            VFXTableCell<T> cell = cells.remove(column);

            // Common columns
            if (cell != null) {
                // Index needs to be updated only and only if the columns' list changed
                if (listChanged) cell.updateIndex(index);
                nCells.put(index, column, cell);
                continue;
            }

            // New columns
            markDirty(index, index);
            VFXTableCell<T> nCell = getCell(index, column);
            if (nCell == null) continue;
            nCells.put(index, column, nCell);
            added.add(nCell.toNode());
        }

        // Whatever is left in the old map is not needed anymore, collect the nodes before caching clears it
        List<Node> removed = getCellsAsNodes();
        saveAllCells();
        this.cells = nCells;
        this.columnsRange = columnsRange;

        if (!removed.isEmpty()) getChildren().removeAll(removed);
        getChildren().addAll(added);
        onUpdateChildren();
    }

    protected void replaceCells(VFXTableColumn<T, ?> column) {
        int cIdx = column.getIndex();
        if (!inRangeOf(cIdx, columnsRange)) return;

        VFXTableCell<T> oldCell = cells.remove(column);
        if (oldCell != null) {
            getChildren().remove(oldCell.toNode());
            oldCell.dispose();
        }

        VFXTableCell<T> newCell = getCell(cIdx, column);
        if (newCell == null) return;

        cells.put(cIdx, column, newCell);
        getChildren().add(newCell.toNode());
        markDirty(cIdx, cIdx);
    }

    @SuppressWarnings("unchecked")
    protected void copyStateFrom(VFXTableRow<T> other) {
        updateIndex(other.getIndex());
        this.columnsRange = other.columnsRange;
        this.cells = other.cells;
        other.cells = RowsStateMap.EMPTY;
        cells.getByIndex().values().forEach(c -> c.updateRow(this));
        this.dirtyFrom = other.dirtyFrom;
        this.dirtyTo = other.dirtyTo;
        updateChildren();
    }

    protected void clear() {
        saveAllCells();
        setIndex(-1);
        setItem(null);
        columnsRange = Utils.INVALID_RANGE;
        markDirty(0, Integer.MAX_VALUE);
        getChildren().clear();
    }

    protected VFXTableCell<T> getCell(int index, VFXTableColumn<T, ?> column) {
        T item = getItem();
        VFXTableCell<T> cell;
        if (column.cacheSize() > 0) {
            cell = column.getCellsCache().take();
            cell.updateItem(item);
        } else {
            cell = column.create(item);
            if (cell == null) return null; // Take into account null generators
        }
        cell.updateRow(this);
        cell.updateColumn(column);
        cell.updateIndex(index);
        return cell;
    }

    protected void saveCell(VFXTableColumn<T, VFXTableCell<T>> column, VFXTableCell<T> cell) {
        column.getCellsCache().cache(cell);
        cell.updateRow(null);
        cell.updateColumn(null);
    }

    @SuppressWarnings("unchecked")
    protected boolean saveAllCells() {
        if (cells.isEmpty()) return false;
        cells.getByKey().forEach((c, idxs) -> {
            for (Integer idx : idxs) {
                saveCell((VFXTableColumn<T, VFXTableCell<T>>) c, cells.get(idx));
            }
        });
        cells.clear();
        return true;
    }

    protected void updateChildren() {
        getChildren().setAll(getCellsAsNodes());
        onUpdateChildren();
    }

    protected void onUpdateChildren() {}

    protected void markDirty(int from, int to) {
        dirtyFrom = Math.min(dirtyFrom, from);
        dirtyTo = Math.max(dirtyTo, to);
    }

    protected void layoutCells(int from, int to) {
        VFXTable<T> table = getTable();
        if (table == null) return;

        from = Math.min(from, dirtyFrom);
        to = Math.max(to, dirtyTo);
        if (from > to) return; // not dirty

        int lo = Math.max(columnsRange.getMin(), from);
        int hi = Math.min(columnsRange.getMax(), to);

        VFXTableHelper<T> helper = table.getHelper();
        for (int i = lo; i <= hi; i++) {
            VFXTableCell<T> cell = cells.get(i);
            if (cell != null) helper.layoutCell(i, cell);
        }
        dirtyFrom = Integer.MAX_VALUE;
        dirtyTo = Integer.MIN_VALUE;
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    @Override
    public Region toNode() {
        return this;
    }

    @Override
    public void onCreated(VFXContext<T> context) {
        if (this.context == null)
            this.context = context;
    }

    @Override
    public void updateIndex(int index) {
        setIndex(index);
    }

    @Override
    public void updateItem(T item) {
        setItem(item);
        cellsByIndex().values().forEach(c -> c.updateItem(item));
    }

    @Override
    protected void layoutChildren() {/*manual, no-op*/}

    @Override
    public void dispose() {
        clear();
        context = null;
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    public VFXContext<T> context() {
        return context;
    }

    public VFXTable<T> getTable() {
        return (VFXTable<T>) ofNullable(context()).map(VFXContext::getContainer).orElse(null);
    }

    public int getIndex() {
        return index.get();
    }

    public ReadOnlyIntegerProperty indexProperty() {
        return index.getReadOnlyProperty();
    }

    protected void setIndex(int index) {
        this.index.set(index);
    }

    public T getItem() {
        return item.get();
    }

    public ReadOnlyObjectProperty<T> itemProperty() {
        return item.getReadOnlyProperty();
    }

    protected void setItem(T item) {
        this.item.set(item);
    }

    public IntegerRange columnsRange() {
        return columnsRange;
    }

    public StateMapBase<VFXTableColumn<T, ?>, T, VFXTableCell<T>> cells() {
        return cells;
    }

    public SequencedMap<Integer, VFXTableCell<T>> cellsByIndex() {
        return cells.getByIndex();
    }

    public List<Node> getCellsAsNodes() {
        return cellsByIndex().values().stream().map(VFXCell::toNode).toList();
    }
}
