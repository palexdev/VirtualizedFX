package io.github.palexdev.virtualizedfx.table.defaults;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper;
import io.github.palexdev.virtualizedfx.table.VFXTableRow;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap.RowsStateMap;
import javafx.scene.Node;

import java.util.Optional;

/**
 * Concrete and simple implementation of {@link VFXTableRow}. Nothing special here, just the default implementations
 * of the abstract core APIs.
 */
public class VFXDefaultTableRow<T> extends VFXTableRow<T> {

	//================================================================================
	// Constructors
	//================================================================================
	public VFXDefaultTableRow(T item) {
		super(item);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * First and foremost, the algorithm will run only if: the given columns range is different from the current one, or
	 * if the {@code columnsChanged} flag is true (which means that the table's columns' list has changed).
	 * <p>
	 * The computation for the new row's state is simple. First we create a new state map which is going to replace the
	 * current one, considered now as 'old'. For each column (index) in the new range, we attempt at removing the corresponding
	 * cell from the old map. <b>Beware!</b> The removal is done by column not index because the list changed and now columns
	 * may be at different positions!!
	 * <p>
	 * If the retrieved cell is not {@code null}, then it means that the column is still present in the viewport and thus we can
	 * reuse its cell. We update the cell's index (only if {@code columnsChanged} is true), then put it in the new state
	 * map and continue to the next index.
	 * <p>
	 * If the cell is {@code null} then it means that the column is not visible anymore (either because by scrolling is
	 * now outside the new range, or because it was removed). In this case we request a new cell from the column at which
	 * the loop is by calling {@link #getCell(int, VFXTableColumn, boolean)} (we use the cache if possible) and put it
	 * in the new state map.
	 * <p>
	 * At the end of the loop, this invokes {@link #saveAllCells()} to cache all the remaining cells in the old map,
	 * then replace it with the new built map, update the columns range and if any cell has changed this also calls
	 * {@link #onCellsChanged()}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void updateColumns(IntegerRange columnsRange, boolean columnsChanged) {
		if (!columnsChanged && super.columnsRange.equals(columnsRange)) return;
		VFXTable<T> table = getTable();
		RowsStateMap<T, VFXTableCell<T>> nCells = new RowsStateMap<>();
		boolean update = false;
		for (Integer index : columnsRange) {
			VFXTableColumn<T, VFXTableCell<T>> column = (VFXTableColumn<T, VFXTableCell<T>>) table.getColumns().get(index);
			VFXTableCell<T> cell = cells.remove(column);

			// Commons
			if (cell != null) {
				// Index needs to be updated only and only if the columns' list changed
				if (columnsChanged) cell.updateIndex(index);
				nCells.put(index, column, cell);
				continue;
			}

			// New columns
			update = true;
			VFXTableCell<T> nCell = getCell(index, column, true);
			if (nCell != null) nCells.put(index, column, nCell);
		}

		// Dispose before replacing state
		update = saveAllCells() || update;
		super.cells = nCells;
		super.columnsRange = columnsRange;
		if (update) onCellsChanged();
	}

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Before detailing the internals, there is a special property of the row's state map that should be mentioned.
	 * Maps of such type ({@link IndexBiMap}) must deal with duplicates and for this reason they store the
	 * 'reverse mappings' in a collection. However, the {@link RowsStateMap} is a special case. You see, every
	 * cell is associated with one unique column index, but it's also true that every cell is also associated with one
	 * and one only column (the one that built it). So, the collection will contain at most one single element.
	 * <p>
	 * First we remove the cells from the map by the given column using {@link RowsStateMap#remove(Object)}.
	 * If it is not {@code null} then we cache it with {@link #saveCell(VFXTableColumn, VFXTableCell)}, and remove it from
	 * the children list.
	 * <p>
	 * As for the new cell, we request a new one by calling {@link #getCell(int, VFXTableColumn, boolean)}
	 * (we don't use the cache as the cells stored in it may be invalid if the cell factory changed).
	 * The cell is added to the state map, to the children list and finally sized and positioned by calling
	 * {@link VFXTableHelper#layoutCell(int, VFXTableCell)}.
	 * <p>
	 * Note that to get a new cell, and to lay out it, we need two different indexes. The index of the column is retrieved
	 * by using {@link VFXTable#indexOf(VFXTableColumn)} and it's needed to update the cell. The other index is an important
	 * piece information for the layout method to decide at which x position to put the cell. This index is also called
	 * the 'layout index' depends on the {@link ColumnsLayoutMode} and its 'absolute'. For the {@code FIXED} mode it
	 * is given by {@code columnIndex - columnsRange.getMin()}, while for the {@code VARIABLE} mode is the column's index
	 * itself (since all columns are added to the viewport, layout indexes go from 0 to the number of column).
	 *
	 * @return whether the substitution was done successfully
	 */
	@Override
	protected boolean replaceCells(VFXTableColumn<T, VFXTableCell<T>> column) {
		VFXTable<T> table = getTable();
		VFXTableHelper<T> helper = table.getHelper();
		if (!IntegerRange.inRangeOf(column.getIndex(), columnsRange)) return false;

		VFXTableCell<T> oCell = cells.remove(column);
		if (oCell != null) {
			getChildren().remove(oCell.toNode());
			saveCell(column, oCell);
		}

		int cIdx = table.indexOf(column);
		int lIdx = (table.getColumnsLayoutMode() == ColumnsLayoutMode.FIXED) ? cIdx - columnsRange.getMin() : cIdx;
		VFXTableCell<T> nCell = getCell(cIdx, column, false);
		if (nCell == null) return false;

		Node nNode = nCell.toNode();
		cells.put(cIdx, column, nCell);
		getChildren().add(nNode);
		helper.layoutCell(lIdx, nCell);
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Extract a cell from the state map by the given column using {@link RowsStateMap#getSingle(VFXTableColumn)}.
	 * If the cell is not {@code null} computes its width by calling {@link Node#prefWidth(double)}. Before doing so,
	 * if the {@code forceLayout} flag is true, also calls {@link Node#applyCss()}.
	 * <p>
	 * If the width can't be computed or if anything goes wrong, returns -1.0.
	 */
	@Override
	protected double getWidthOf(VFXTableColumn<T, ?> column, boolean forceLayout) {
		try {
			return Optional.ofNullable(cells.getSingle(column))
				.map(c -> {
					Node node = c.toNode();
					if (forceLayout) applyCss();
					return node.prefWidth(-1);
				})
				.orElse(-1.0);
		} catch (Exception ex) {
			return -1.0;
		}
	}

	/**
	 * Responsible for both updating the row's item property and all of its cells' item property.
	 */
	@Override
	public void updateItem(T item) {
		super.updateItem(item);
		getCellsByIndex().values().forEach(c -> c.updateItem(item));
	}
}
