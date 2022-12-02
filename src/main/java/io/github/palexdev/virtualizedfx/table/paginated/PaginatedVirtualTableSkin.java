package io.github.palexdev.virtualizedfx.table.paginated;

import io.github.palexdev.mfxcore.utils.fx.NodeUtils;
import io.github.palexdev.virtualizedfx.table.VirtualTableSkin;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Default skin implementation for {@link PaginatedVirtualTable}, extends {@link VirtualTableSkin}.
 * <p></p>
 * This is adapted to adjust the table's height according to {@link PaginatedVirtualTable#rowsPerPageProperty()} and
 * {@link PaginatedVirtualTable#cellHeightProperty()}.
 */
public class PaginatedVirtualTableSkin<T> extends VirtualTableSkin<T> {

	//================================================================================
	// Constructors
	//================================================================================
	public PaginatedVirtualTableSkin(PaginatedVirtualTable<T> table) {
		super(table);
		NodeUtils.waitForScene(table, table::updateMaxPage, false, true);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * @return {@link #getSkinnable()} cast as {@link PaginatedVirtualTable}
	 */
	protected PaginatedVirtualTable<T> getTable() {
		return (PaginatedVirtualTable<T>) getSkinnable();
	}

	/**
	 * Core method used by:
	 * <p> {@link #computeMinHeight(double, double, double, double, double)}
	 * <p> {@link #computePrefHeight(double, double, double, double, double)}
	 * <p> {@link #computeMaxHeight(double, double, double, double, double)}
	 * <p></p>
	 * Computes the virtual table's height as {@code rowsPerPage * cellHeight}, in other terms
	 * the size of every single page.
	 */
	protected double getLength() {
		PaginatedVirtualTable<T> table = getTable();
		double cH = table.getColumnSize().getHeight();
		return table.getRowsPerPage() * table.getCellHeight() + cH;
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * {@inheritDoc}
	 * Overridden to also update the number of max pages.
	 */
	@Override
	protected void onListChanged(ObservableList<T> oldValue, ObservableList<T> newValue) {
		getTable().updateMaxPage();
		super.onListChanged(oldValue, newValue);
	}

	/**
	 * {@inheritDoc}
	 * Overridden to also update the number of max pages.
	 */
	@Override
	protected void onItemsChanged(ListChangeListener.Change<? extends T> change) {
		getTable().updateMaxPage();
		super.onItemsChanged(change);
	}

	@Override
	protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		return getLength();
	}

	@Override
	protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		return getLength();
	}

	@Override
	protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		return getLength();
	}
}
