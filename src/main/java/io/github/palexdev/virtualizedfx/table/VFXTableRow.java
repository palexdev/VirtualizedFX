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

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.virtualizedfx.base.VFXStyleable;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.table.defaults.VFXDefaultTableRow;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap.RowsStateMap;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import java.util.Collections;
import java.util.List;
import java.util.SequencedMap;
import java.util.function.Function;

/**
 * Base class that defines common properties and behaviors for all rows to be used with {@link VFXTable}.
 * The default style class is set to '.vfx-row'.
 * <p>
 * This class has two peculiarities:
 * <p> 1) Extends {@link Region} because each row is actually a wrapping container for the table's cells
 * <p> 2) Implements {@link VFXCell} because most of the API is the same! Let's see the benefits:
 * First and foremost this allows us to use the same cache class {@link VFXCellsCache} for the rows too
 * which is super convenient. As for the {@link #updateIndex(int)} and {@link #updateItem(Object)} methods,
 * well the row is no different from any other cell really. Each row wraps a cell for each table's column.
 * All its cells 'operate' on the same item, they just process the object differently (generally speaking).
 * So, a row which displays an item 'T' at index 17 in the list will have its index property set to 17, its item property
 * as well as all of its cells' item property will be set to 'T'.
 * As you can imagine, when we scroll in the viewport, make changes to the list (to cite only a few of the many changes types)
 * we want to update the cells by either or both index and item. The table does so through rows
 * (see {@link #updateColumns(IntegerRange, boolean)} for example).
 * <p></p>
 * There are two more properties which we need to discuss:
 * <p> 1) Besides the index and item properties, each row also has the current range of columns the table needs to display,
 * {@link #getColumnsRange()}. This piece of information is crucial for the row to build (and thus wrap) the correct type
 * of cells. For example, let's say for a hypothetical User class I can see the 'First Name' column but not the 'Last Name'
 * one. We want the row to ask the 'First Name' column to give us a cell for this field
 * (it could be created or de-cached see {@link #getCell(int, VFXTableColumn, boolean)}).
 * Additionally, we want to make sure that every other cell produced by columns that are not visible anymore to be removed
 * from the children list (such cells can be disposed or cached, see {@link #saveCell(VFXTableColumn, VFXTableCell)}).
 * Every time something relevant to the rows changes in the table, each row computes its new state and if cells change
 * they call {@link #onCellsChanged()} to update the children list.
 * <p> 2) What do we mean by row's state? As probably already mentioned here {@link VFXTable}, this container is a bit special
 * because we have two kinds of states. The global state which is a separate class {@link VFXTableState}, but then each row
 * has its own "mini-state". The global state specifies which and how many items/rows should be present in the viewport,
 * but it misses one crucial detail which is covered by the rows' state, which and how many cells to show. Exactly as it
 * would be for a traditional state object, the cells keep a map of all their cells. When changes happen, we can easily
 * compute which cells to keep using, which ones need to be disposed and whether new ones are needed.
 * The type of map used is {@link RowsStateMap}.
 * <p></p>
 * Aside from those core details, rows also have much more going on: they can copy the state of other rows
 * (for optimization reasons, see {@link #copyState(VFXTableRow)}), the layout is completely 'manual' and will
 * not respond to the traditional JavaFX 'triggers' but only to {@link VFXTable#needsViewportLayoutProperty()}.
 * <p></p>
 * <b>Note:</b> because some of the base methods are actually quite complex to implement it's not recommended to use this
 * as a base class for extension but rather {@link VFXDefaultTableRow}. Either way, always take a look at how original
 * algorithms work before customizing!
 */
public abstract class VFXTableRow<T> extends Region implements VFXCell<T>, VFXStyleable {
	//================================================================================
	// Properties
	//================================================================================
	private final ReadOnlyObjectWrapper<VFXTable<T>> table = new ReadOnlyObjectWrapper<>();
	private final ReadOnlyIntegerWrapper index = new ReadOnlyIntegerWrapper(-1);
	private final ReadOnlyObjectWrapper<T> item = new ReadOnlyObjectWrapper<>();
	protected IntegerRange columnsRange = Utils.INVALID_RANGE;
	protected RowsStateMap<T, VFXTableCell<T>> cells;

	//================================================================================
	// Constructors
	//================================================================================
	public VFXTableRow(T item) {
		cells = new RowsStateMap<>();
		updateItem(item);
		initialize();
	}

	//================================================================================
	// Abstract Methods
	//===============================================================================

	/**
	 * Implementations of this method should mainly react to two types of change in the table:
	 * <p> 1) the visible columns changes, or more in general the state's columns range changes
	 * <p> 2) changes in the columns' list that may not necessarily change the range. In particular, this type of change
	 * is distinguished by the 'change' parameter set to true. In this case, the row's state should always be computed as
	 * there is no information on how the list changed, a 'better safe than sorry' approach.
	 */
	protected abstract void updateColumns(IntegerRange columnsRange, boolean changed);

	/**
	 * This method mainly exists to react to cell factory changes. When a column changes its factory, there is no need to
	 * re-compute each rows' state, rather we can just replace the cells for that column with new ones.
	 * Implementations should have a proper algorithm for the sake of performance.
	 */
	protected abstract boolean replaceCells(VFXTableColumn<T, VFXTableCell<T>> column);

	/**
	 * This method should be responsible for computing the ideal width of a cell given the corresponding column so that
	 * its content can be fully shown. This is important for the table's autosize feature to work properly!
	 *
	 * @param forceLayout it may happen that this garbage that is JavaFX to still have an incomplete scene graph, meaning
	 *                    for example that skins are not still available for some controls/cells, which means that we could
	 *                    fail in computing any size. This may happen even if the table's layout has been already computed
	 *                    at least once. Internal checks try to identify such occasions and may pass 'true' to this method.
	 *                    What implementations could do to get a correct value is to force the cells to compute their
	 *                    layout (as well as all of their children) by invoking {@link Node#applyCss()}.
	 *                    Note, however, that this is going to be a <b>costly</b> operation at init, but it's pretty much
	 *                    the only way.
	 */
	protected abstract double getWidthOf(VFXTableColumn<T, ?> column, boolean forceLayout);

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().setAll(defaultStyleClasses());
	}

	/**
	 * Sets this row's state to be exactly the same as the one given as parameter. This is mainly useful when the table
	 * changes its {@link VFXTable#rowFactoryProperty()} because while it's true that the old rows are to be disposed
	 * abd removed, the new ones would still have the same state of the old ones. In such occasions, it's a great
	 * optimization to just copy the state of the old corresponsig row rather than re-computing it from zero.
	 * <p>
	 * To further detail what happens when this is called:
	 * <p> - the index is updated to be the same as the 'other'
	 * <p> - the columns range is copied over
	 * <p> - the cells' map is copied over and the instance in the 'other' row is set to {@link RowsStateMap#EMPTY}
	 * <p> - calls {@link VFXTableCell#updateRow(VFXTableRow)} on all the cells from the 'other' row
	 * <p> - finally calls {@link #onCellsChanged()}
	 * <p></p>
	 * Last but not least, note that such operation is likely going to need a layout request, but it's not the rows'
	 * responsibility to do so.
	 */
	@SuppressWarnings("unchecked")
	protected void copyState(VFXTableRow<T> other) {
		updateIndex(other.getIndex());
		this.columnsRange = other.columnsRange;
		this.cells = other.cells;
		other.cells = RowsStateMap.EMPTY;
		cells.getByIndex().values().forEach(c -> c.updateRow(this));
		onCellsChanged();
		// A layout request is also needed!
	}

	/**
	 * Clears the row's state without disposing it. This will cause all cells to be cached by {@link #saveAllCells()},
	 * the index set to -1, the item set to {@code null}, the columns range set to {@link Utils#INVALID_RANGE} and the children
	 * list to be cleared.
	 */
	protected void clear() {
		saveAllCells();
		setIndex(-1);
		setItem(null);
		columnsRange = Utils.INVALID_RANGE;
		getChildren().clear();
	}

	/**
	 * This is crucial to call when the row's cells change. All cells are 'collected' as nodes by {@link #getCellsAsNodes()}
	 * and added to the row's children list.
	 */
	protected void onCellsChanged() {
		getChildren().setAll(getCellsAsNodes());
	}

	/**
	 * This method is responsible for creating cells given the "parent" column (from which takes the cell factory),
	 * and its index. Before creating a new cell using the factory, this attempts to retrieve one from the column's
	 * cells' cache, and only if there are no cached cells, a new one is built. The cache usage is optional and can be
	 * avoided by passing false as the {@code useCache} parameter.
	 * <p>
	 * In any case, the cell will be fully updated: {@link VFXTableCell#updateRow(VFXTableRow)}, {@link VFXTableCell#updateColumn(VFXTableColumn)},
	 * {@link VFXTableCell#updateItem(Object)} (if de-cached), and {@link VFXTableCell#updateIndex(int)}.
	 */
	protected VFXTableCell<T> getCell(int index, VFXTableColumn<T, VFXTableCell<T>> column, boolean useCache) {
		T item = getItem();
		VFXTableCell<T> cell;
		if (useCache && column.cacheSize() > 0) { // Try cache first
			cell = column.cache().take();
			cell.updateItem(item);
		} else { // Create new otherwise
			Function<T, VFXTableCell<T>> cellFactory = column.getCellFactory();
			if (cellFactory == null) return null; // Take into account null generators
			cell = cellFactory.apply(item);
		}
		cell.updateRow(this);
		cell.updateColumn(column);
		cell.updateIndex(index);
		return cell;
	}

	/**
	 * Asks the given column to save the given cell in its cache. Beware that this operation won't remove the cell
	 * from the state map and the children list; therefore, you must do it before calling this
	 * <p>
	 * By convention, when this is called, the cell's row and column properties are reset to {@code null}.
	 * This is to clearly indicate that the cell is not in the viewport anymore.
	 */
	protected void saveCell(VFXTableColumn<T, VFXTableCell<T>> column, VFXTableCell<T> cell) {
		column.cache().cache(cell);
		cell.updateRow(null);
		cell.updateColumn(null);
		// A cell (or row) that is not in the viewport anymore should state it clearly
		// This is the convention, therefore, set both to 'null'
	}

	/**
	 * Caches all the row's cells by iterating over the state map and calling {@link #saveCell(VFXTableColumn, VFXTableCell)}.
	 * The difference here is that the state map is also cleared at the end.
	 * <p>
	 * Beware that this will not call {@link #onCellsChanged()}, therefore, if needed, you will have to do it afterward.
	 */
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

	/**
	 * This core method is responsible for sizing and positioning the cells in the row.
	 * This is done by iterating over the columns range, getting every cell and, if not {@code null}, delegating the
	 * operation to {@link VFXTableHelper#layoutCell(int, VFXTableCell)}.
	 * <p>
	 * This only defines the algorithm and is not automatically called by the row. Rather, it's the default table skin
	 * to call this on each row upon a layout request received from the {@link VFXTable#needsViewportLayoutProperty()}.
	 * <p></p>
	 * <b>Note</b> that this implementation allows having columns that produce {@code null} cells.
	 */
	protected void layoutCells() {
		// It's crucial to process the layout this way.
		// Some columns may not be present in the map as the cell factory could be null or produce null cells.
		// So, we have to skip such cases, but we still need to increment the 'i' counter to get the correct absolute position
		VFXTable<T> table = getTable();
		if (table == null || !table.isNeedsViewportLayout()) return;
		VFXTableHelper<T> helper = table.getHelper();
		int i = 0;
		for (Integer idx : columnsRange) {
			VFXTableCell<T> cell = cells.get(idx);
			if (cell != null) helper.layoutCell(i, cell);
			i++;
		}
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	public Region toNode() {
		return this;
	}

	@Override
	public void updateIndex(int index) {
		setIndex(index);
	}

	@Override
	public void updateItem(T item) {
		setItem(item);
	}

	@Override
	public List<String> defaultStyleClasses() {
		return List.of("vfx-row");
	}

	/**
	 * Overridden to be a no-op. We manage the layout manually like real giga-chads.
	 */
	@Override
	protected void layoutChildren() {}


	/**
	 * Automatically called by the table's system when the row is not needed anymore. Most of the operations are performed
	 * by {@link #clear()}. In addition, the table's instance is set to {@code null}.
	 */
	@Override
	public void dispose() {
		clear();
		setTable(null);
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

	public VFXTable<T> getTable() {
		return table.get();
	}

	/**
	 * Specifies the table's instance this row belongs to.
	 */
	public ReadOnlyObjectProperty<VFXTable<T>> tableProperty() {
		return table.getReadOnlyProperty();
	}

	protected void setTable(VFXTable<T> table) {
		this.table.set(table);
	}

	public int getIndex() {
		return index.get();
	}

	/**
	 * Specifies the index of the item displayed by the row and its cells.
	 */
	public ReadOnlyIntegerProperty indexProperty() {
		return index.getReadOnlyProperty();
	}

	protected void setIndex(int index) {
		this.index.set(index);
	}

	public T getItem() {
		return item.get();
	}

	/**
	 * Specifies the object displayed by the row and its cells.
	 */
	public ReadOnlyObjectProperty<T> itemProperty() {
		return item.getReadOnlyProperty();
	}

	protected void setItem(T item) {
		this.item.set(item);
	}

	/**
	 * The range of columns visible in the viewport. This should always be the same as the current {@link VFXTableState},
	 * and it's used to make the row always have the correct cells displayed (in accord to the visualized columns).
	 */
	public IntegerRange getColumnsRange() {
		return columnsRange;
	}

	/**
	 * @return the row's cells as an unmodifiable {@link SequencedMap}, mapped by the row's {@link #indexProperty()}.
	 */
	public SequencedMap<Integer, VFXTableCell<T>> getCellsUnmodifiable() {
		return Collections.unmodifiableSequencedMap(cells.getByIndex());
	}

	/**
	 * @return the row's state map, which contains the cells both mapped by the row's index or the cell's "parent" column.
	 */
	protected RowsStateMap<T, VFXTableCell<T>> getCells() {
		return cells;
	}

	/**
	 * @return the row's cells as a {@link SequencedMap}, mapped by the row's {@link #indexProperty()}.
	 */
	protected SequencedMap<Integer, VFXTableCell<T>> getCellsByIndex() {
		return cells.getByIndex();
	}

	/**
	 * Converts and collects all the cells from the row's state map to JavaFX nodes by using {@link VFXCell#toNode()}.
	 */
	public List<Node> getCellsAsNodes() {
		return getCellsByIndex().values().stream()
			.map(VFXCell::toNode)
			.toList();
	}
}
