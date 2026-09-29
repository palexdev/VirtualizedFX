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
import io.github.palexdev.virtualizedfx.table.defaults.VFXDefaultTableRow;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap.RowsStateMap;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap.StateMapBase;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import static io.github.palexdev.mfxcore.base.beans.range.IntegerRange.inRangeOf;
import static java.util.Optional.ofNullable;

/// Base class that defines common properties and behaviors for all rows to be used with [VFXTable].
///
/// This class has two peculiarities:
///
/// 1) Extends [Region] because each row is actually a wrapping container for the table's cells
///
/// 2) Implements [VFXCell] because most of the API is the same! Let's see the benefits:
/// First and foremost this allows us to use the same cache class [VFXCellsCache] for the rows too
/// which is super convenient. As for the [#updateIndex(int)] and [#updateItem(Object)] methods,
/// well the row is no different from any other cell really. Each row wraps a cell for each table's column.
/// All its cells 'operate' on the same item, they just process the object differently (generally speaking).
/// So, a row which displays an item 'T' at index 17 in the list will have its index property set to 17, its item
/// property as well as all of its cells' item property will be set to 'T'.
///
/// ## The row's own state
///
/// The table has two kinds of state. The global one, [VFXTableState], says how many rows there should be in the
/// viewport and for which items. It misses one crucial detail, which and how many cells each row shows, and that is
/// the row's own 'mini-state': the range of columns it covers, [#columnsRange()], and a map of its cells, [#cells()].
///
/// The range is what lets the row build the right cells. Say that for a hypothetical `User` class I can see the
/// 'First Name' column but not the 'Last Name' one. We want the row to ask the 'First Name' column for a cell
/// (created or taken from its cache, see [#getCell(int,VFXTableColumn)]), and every cell produced by a column that
/// is not shown anymore to leave the children list and go back to its column's cache,
/// [#saveCell(VFXTableColumn,VFXTableCell)].
///
/// The map is a [RowsStateMap], which keeps the cells by their column's **absolute** index and by the column
/// instance itself. Both ways of looking a cell up are needed: by index when the range moves,
/// [#updateColumns(IntegerRange,boolean)], by column when a single column changes something,
/// [#replaceCells(VFXTableColumn)].
///
/// ## Layout
///
/// The layout is completely manual, [#layoutChildren()] is a no-op. Cells are positioned by [#layoutCells(int,int)],
/// which the table's skin calls on each row when it processes a layout request, see
/// [VFXTable#needsViewportLayoutProperty()].
///
/// A row also keeps track of which of its cells are not positioned yet, an interval extended by
/// [#markDirty(int,int)] every time a cell is created or replaced. The skin's request says which columns moved, the
/// row's interval says which of its cells were never placed, and the layout covers both, see [#layoutCells(int,int)].
///
/// **Note:** because some of the base methods are actually quite complex to implement it's not recommended to use
/// this as a base class for extension but rather [VFXDefaultTableRow]. Either way, always take a look at how
/// original algorithms work before customizing!
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

    /// This core method is responsible for making the row show the cells of the given columns range.
    ///
    /// It reacts to two kinds of change:
    ///
    /// 1) the columns range changed, which happens when scrolling horizontally or when the columns' geometry changes
    ///
    /// 2) the columns' list itself changed, signaled by the `listChanged` parameter. In this case there is no
    /// information on how the list changed, so the row's state is always recomputed, a 'better safe than sorry'
    /// approach. Cells that are kept also have their index updated, since the same column can now be at another index
    ///
    /// For every index in the range, the cell of the corresponding column is taken from the current map. If there is
    /// one, it's moved to the new map as is. If there is none, a new cell is built or taken from the column's cache,
    /// [#getCell(int,VFXTableColumn)], and marked as to be laid out, [#markDirty(int,int)].
    ///
    /// Whatever is left in the old map belongs to columns that are not shown anymore: those cells are cached,
    /// [#saveAllCells()], and their nodes removed from the children list, while the new cells' nodes are added. Both
    /// operations happen at once, followed by [#onUpdateChildren(boolean)] with `false`, only the cells' nodes were
    /// touched.
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
        onUpdateChildren(false);
    }

    /// This method exists to react to cell factory changes. When a column changes its factory, there is no need to
    /// recompute the row's state, it's enough to replace the cells built by that column with new ones.
    ///
    /// Does nothing if the column is not in the row's range. Otherwise the old cell is removed from the children list
    /// and disposed (it cannot go back to its column's cache, the factory that built it is gone), a new one takes its
    /// place, and the row is marked as to be laid out at that index. Ends with [#onUpdateChildren(boolean)] and
    /// `false`, only one cell's node changed.
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
        onUpdateChildren(false);
    }

    /// Sets this row's state to be exactly the same as the one given as parameter. This is mainly useful when the
    /// table changes its [VFXTable#rowsFactoryProperty()] because while it's true that the old rows are to be disposed
    /// and removed, the new ones would still have the same state of the old ones. In such occasions, it's a great
    /// optimization to just copy the state of the old corresponding row rather than recomputing it from zero.
    ///
    /// To further detail what happens when this is called:
    ///
    /// - the index is updated to be the same as the 'other'
    ///
    /// - the columns range is copied over
    ///
    /// - the cells' map is copied over and the instance in the 'other' row is set to [RowsStateMap#EMPTY]
    ///
    /// - calls [VFXTableCell#updateRow(VFXTableRow)] on all the copied cells, they belong to this row now
    ///
    /// - the interval of cells to lay out is copied over too
    ///
    /// - finally calls [#updateChildren()]
    ///
    /// Last but not least, note that such operation is likely going to need a layout request, but it's not the rows'
    /// responsibility to do so.
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

    /// Clears the row's state without disposing it. This will cause all cells to be cached by [#saveAllCells()],
    /// the index set to -1, the item set to `null`, the columns range set to [Utils#INVALID_RANGE], the whole row
    /// marked as to be laid out, and the children list to be cleared.
    ///
    /// The children are emptied for good, subclasses' nodes included, and [#onUpdateChildren(boolean)] is not called:
    /// a row is cleared when it's on its way out, so there is nothing to put back.
    protected void clear() {
        saveAllCells();
        setIndex(-1);
        setItem(null);
        columnsRange = Utils.INVALID_RANGE;
        markDirty(0, Integer.MAX_VALUE);
        getChildren().clear();
    }

    /// This method is responsible for getting cells from the given "parent" column, at the given index. A cached cell
    /// is used when the column has one, otherwise a new cell is built by the column's factory.
    ///
    /// In any case, the cell is fully updated: [VFXTableCell#updateItem(Object)] (only if taken from the cache, a new
    /// cell already gets the item from the factory), [VFXTableCell#updateRow(VFXTableRow)],
    /// [VFXTableCell#updateColumn(VFXTableColumn)] and [VFXTableCell#updateIndex(int)].
    ///
    /// @return the cell for the given column, or `null` if the column's factory produced none
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

    /// Asks the given column to save the given cell in its cache. Beware that this operation won't remove the cell
    /// from the state map and the children list, therefore, you must do it before calling this.
    ///
    /// By convention, when this is called, the cell's row and column properties are reset to `null`.
    /// This is to clearly indicate that the cell is not in the viewport anymore.
    protected void saveCell(VFXTableColumn<T, VFXTableCell<T>> column, VFXTableCell<T> cell) {
        column.getCellsCache().cache(cell);
        cell.updateRow(null);
        cell.updateColumn(null);
    }

    /// Caches all the row's cells by iterating over the state map and calling
    /// [#saveCell(VFXTableColumn,VFXTableCell)]. The difference here is that the state map is also cleared at the end.
    ///
    /// Beware that this will not touch the children list, therefore, if needed, you will have to do it afterward.
    ///
    /// @return whether there was anything to cache
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

    /// Sets the children list to all the row's cells, collected as nodes by [#getCellsAsNodes()], then calls
    /// [#onUpdateChildren(boolean)] with `true`, since anything that was in the list is gone.
    protected final void updateChildren() {
        getChildren().setAll(getCellsAsNodes());
        onUpdateChildren(true);
    }

    /// Called every time the row's children change, which is the hook subclasses should use to add nodes of their own
    /// to the row or to react to the change. Does nothing by default.
    ///
    /// The row adds and removes its cells' nodes in two ways, and the parameter tells which one just happened.<br >
    /// When it is `true` the children list was emptied, so nodes that do not belong to the row's cells are gone and
    /// have to be added again. When it is `false` only some cells' nodes were added or removed, everything else is
    /// still in the list, and adding it again would throw, JavaFX does not allow duplicate children.
    ///
    /// @param cleared whether the children list was wiped before the row's cells were put in it
    protected void onUpdateChildren(boolean cleared) {}

    /// Extends the interval of cells that need to be laid out, so that it covers the given one too. Called every time
    /// a cell is created or replaced, since such cells have no position yet, see [#layoutCells(int,int)].
    protected void markDirty(int from, int to) {
        dirtyFrom = Math.min(dirtyFrom, from);
        dirtyTo = Math.max(dirtyTo, to);
    }

    /// This core method is responsible for sizing and positioning the cells in the row, which is done by delegating
    /// to [VFXTableHelper#layoutCell(int,VFXTableCell)] for each of them.
    ///
    /// The given interval comes from the layout request the skin is processing and says which columns moved. The
    /// row's own interval says which of its cells were never positioned, see [#markDirty(int,int)]. The two are
    /// merged, then clipped to the row's columns range, and the cells in between are laid out. The row's interval is
    /// empty afterwards, nothing is left unpositioned.
    ///
    /// Exits immediately if the row is not in a table, or if there is nothing to lay out.
    ///
    /// This only defines the algorithm and is not automatically called by the row. Rather, it's the default table skin
    /// that calls this on each row upon a layout request received from [VFXTable#needsViewportLayoutProperty()].
    ///
    /// **Note** that this implementation allows having columns that produce `null` cells.
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

    /// {@inheritDoc}
    ///
    /// The context is what gives the row access to its table, [#getTable()], and is set only once.
    @Override
    public void onCreated(VFXContext<T> context) {
        if (this.context == null)
            this.context = context;
    }

    /// {@inheritDoc}
    ///
    /// Sets the row's index, the cells do not need it, every cell in a row carries its column's index instead.
    @Override
    public void updateIndex(int index) {
        setIndex(index);
    }

    /// {@inheritDoc}
    ///
    /// Sets the row's item and propagates it to every cell, they all show the same object, each in its own way.
    @Override
    public void updateItem(T item) {
        setItem(item);
        cellsByIndex().values().forEach(c -> c.updateItem(item));
    }

    /// Overridden to be a no-op. We manage the layout manually like real giga-chads, see [#layoutCells(int,int)].
    @Override
    protected void layoutChildren() {/*manual, no-op*/}

    /// {@inheritDoc}
    ///
    /// Automatically called by the table's system when the row is not needed anymore. Most of the operations are
    /// performed by [#clear()]. In addition, the context is set to `null`.
    @Override
    public void dispose() {
        clear();
        context = null;
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    /// @return the context the row was created with, see [VFXCell#onCreated(VFXContext)]
    public VFXContext<T> context() {
        return context;
    }

    /// @return the table the row belongs to, taken from its context, `null` if it has none
    public VFXTable<T> getTable() {
        return (VFXTable<T>) ofNullable(context()).map(VFXContext::getContainer).orElse(null);
    }

    public int getIndex() {
        return index.get();
    }

    /// Specifies the index of the item displayed by the row and its cells.
    public ReadOnlyIntegerProperty indexProperty() {
        return index.getReadOnlyProperty();
    }

    protected void setIndex(int index) {
        this.index.set(index);
    }

    public T getItem() {
        return item.get();
    }

    /// Specifies the object displayed by the row and its cells.
    public ReadOnlyObjectProperty<T> itemProperty() {
        return item.getReadOnlyProperty();
    }

    protected void setItem(T item) {
        this.item.set(item);
    }

    /// @return the range of columns the row shows a cell for. This should always be the same as the one in the current [VFXTableState]
    public IntegerRange columnsRange() {
        return columnsRange;
    }

    /// @return the row's state map, which contains the cells mapped both by their column's **absolute** index in
    /// [VFXTable#columns()] and by the cell's "parent" column instance
    public StateMapBase<VFXTableColumn<T, ?>, T, VFXTableCell<T>> cells() {
        return cells;
    }

    /// @return the row's cells as a [SequencedMap], mapped by their column's **absolute** index in
    /// [VFXTable#columns()] (not by the row's own [#indexProperty()], every cell in a row shares that one)
    public SequencedMap<Integer, VFXTableCell<T>> cellsByIndex() {
        return cells.getByIndex();
    }

    /// Converts and collects all the cells from the row's state map to JavaFX nodes by using [VFXCell#toNode()].
    public List<Node> getCellsAsNodes() {
        return cellsByIndex().values().stream().map(VFXCell::toNode).toList();
    }
}
