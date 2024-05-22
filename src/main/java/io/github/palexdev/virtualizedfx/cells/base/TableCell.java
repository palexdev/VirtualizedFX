package io.github.palexdev.virtualizedfx.cells.base;

import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import io.github.palexdev.virtualizedfx.table.VFXTableRow;

/**
 * Extension of {@link Cell} to be used specifically with {@link VFXTable}. This virtualized container has both
 * rows and columns as concrete nodes with their own properties and states. It may be useful for table cells to store
 * their instances; So, this extension exposes two extra methods {@link #updateColumn(VFXTableColumn)} and
 * {@link #updateRow(VFXTableRow)}.
 */
public interface TableCell<T> extends Cell<T> {

	/**
	 * Automatically called by the {@link VFXTable} subsystem on all its cells to allow storing the instance
	 * of the column that created them.
	 */
	default void updateColumn(VFXTableColumn<T, ? extends TableCell<T>> column) {}

	/**
	 * Automatically called by the {@link VFXTable} subsystem on all its cells to allow storing the instance of
	 * the row that contains them.
	 */
	default void updateRow(VFXTableRow<T> row) {}
}
