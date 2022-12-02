package io.github.palexdev.virtualizedfx.table.defaults;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.table.*;
import javafx.scene.Node;

import java.util.*;
import java.util.function.Supplier;

/**
 * Default, concrete implementation of {@link TableRow}.
 * <p></p>
 * It is highly recommended to use this as a base class for custom rows rather than {@link TableRow}, as the
 * behaviors specified by the various overridden methods are quite complex but stable.
 * <p></p>
 * Another suggestion is to avoid UI changes in custom implementation. The {@link #layoutChildren()} method
 * is overridden to do nothing as the layout of the row is entirely managed by {@link TableHelper}. This because
 * the row is intended to contain only the cells produced by the columns.
 * <p>
 * What you should do instead in custom implementations is to introduce new features, for example selection handling.
 */
public class DefaultTableRow<T> extends TableRow<T> {

	//================================================================================
	// Constructors
	//================================================================================
	public DefaultTableRow(VirtualTable<T> table, int index, IntegerRange columns) {
		super(table, index, columns);
	}

	public static <T> DefaultTableRow<T> of(VirtualTable<T> table, int index, IntegerRange columns) {
		return new DefaultTableRow<>(table, index, columns);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	// Init

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Initializes {@code DefaultTableRow} by creating the needed amount of cells given by the {@link #getColumns()} range.
	 * <p>
	 * If the row has already been initialized before all the cells are disposed, cleared and re-created.
	 * <p></p>
	 * A call to {@link #cellsChanged()} updates the children list of the row.
	 */
	@Override
	protected TableRow<T> init() {
		if (index < 0 || IntegerRange.of(-1).equals(columns)) return this;

		clear();
		for (Integer cIndex : columns) {
			TableColumn<T, ? extends TableCell<T>> column = getColumn(cIndex);
			T item = table.getItems().get(index);
			TableCell<T> cell = takeFromCacheOrCreate(column, item);
			cell.updateIndex(cIndex);
			cells.put(cIndex, cell);
		}
		cellsChanged();
		return this;
	}

	// Updates

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * This is responsible for supplying of removing cells according to the new given columns range.
	 * <p>
	 * This is used by {@link ViewportManager#init()}. If the given range is equal to the current one no operation
	 * is done. But can also be used when scrolling horizontally, {@link ViewportManager#onHScroll()}.
	 */
	@Override
	protected void updateColumns(IntegerRange columns) {
		if (super.columns.equals(columns)) return;

		Map<Integer, TableCell<T>> tmp = new HashMap<>();
		Set<Integer> range = IntegerRange.expandRangeToSet(columns);
		int targetSize = columns.diff() + 1;

		for (Integer column : columns) {
			if (cells.containsKey(column)) {
				tmp.put(column, cells.remove(column));
				range.remove(column);
			}
		}

		Deque<Integer> remaining = new ArrayDeque<>(range);
		T item = table.getItems().get(index);
		while (tmp.size() != targetSize) {
			int rIndex = remaining.removeFirst();
			TableColumn<T, ? extends TableCell<T>> column = getColumn(rIndex);
			TableCell<T> cell = takeFromCacheOrCreate(column, item);
			cell.updateIndex(rIndex);
			tmp.put(rIndex, cell);
		}

		Iterator<Map.Entry<Integer, TableCell<T>>> it = cells.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, TableCell<T>> next = it.next();
			cacheCell(next.getKey(), next.getValue());
			it.remove();
		}

		cells.putAll(tmp);
		super.columns = columns;
		cellsChanged();
	}

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Retrieves the item at index {@link #getIndex()} from the table then calls
	 * {@link TableCell#updateItem(Object)} on all the cells of the row.
	 */
	@Override
	protected void updateItem() {
		T item = table.getItems().get(index);
		cells.values().forEach(c -> c.updateItem(item));
	}

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Updates the row's index. Then calls {@link TableCell#updateRow(int, DefaultTableRow)}
	 * on all the cells of the row.
	 */
	@Override
	protected void updateIndex(int index) {
		if (super.index == index) return;
		super.index = index;
		cells.values().forEach(c -> c.updateRow(index, this));
	}

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Updates the row's index. Then retrieves the item at the new index from the table and calls
	 * {@link TableCell#updateRow(int, DefaultTableRow)} and {@link TableCell#updateItem(Object)} on
	 * all the cells of the row.
	 *
	 * @param index
	 */
	@Override
	protected void updateFull(int index) {
		super.index = index;

		T item = table.getItems().get(index);
		cells.values().forEach(c -> {
			c.updateRow(index, this);
			c.updateItem(item);
		});
	}

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * If the row cells map contains a cell at the given column index, removes and disposes it.
	 * At this point, after retrieving the item at index {@link #getIndex()} and the column at the given index, creates
	 * a new cell and calls {@link #cellsChanged()} to update the children list.
	 */
	@Override
	protected void updateColumnFactory(int cIndex) {
		TableCell<T> oldCell = cells.remove(cIndex);
		if (oldCell != null) oldCell.dispose();

		T item = table.getItems().get(index);
		TableColumn<T, ? extends TableCell<T>> column = table.getColumn(cIndex);
		TableCell<T> cell = column.getCellFactory().apply(item);
		cell.updateIndex(cIndex);
		cell.updateColumn(column);
		cell.updateRow(index, this);
		cells.put(cIndex, cell);
		cellsChanged();
	}

	// Misc

	/**
	 * If a cell is present in the map at the given column index returns {@link Node#prefWidth(double)} on
	 * the cell's node, otherwise returns 0.0.
	 */
	@Override
	protected double getWidthOf(int cIndex) {
		return Optional.ofNullable(cells.get(cIndex))
				.map(c -> c.getNode().prefWidth(-1))
				.orElse(0.0);
	}

	/**
	 * Converts the cells map to nodes with {@link #getCellsAsNodes()} and updates the children list.
	 */
	@Override
	protected void cellsChanged() {
		List<Node> nodes = getCellsAsNodes();
		getChildren().setAll(nodes);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Tries to reuse a previously created cell by taking it from the table's cache with {@link TableCache#tryTake(TableColumn)}.
	 * <p></p>
	 * If the cache is not enabled, {@link VirtualTable#enableColumnsCacheProperty()}, or no cached cells are available,
	 * creates a new one.
	 */
	protected TableCell<T> takeFromCacheOrCreate(TableColumn<T, ? extends TableCell<T>> column, T item) {
		Supplier<TableCell<T>> creator = () -> {
			TableCell<T> cell = column.getCellFactory().apply(item);
			cell.updateColumn(column);
			cell.updateRow(index, this);
			return cell;
		};
		if (!table.isColumnsCacheEnabled()) return creator.get();

		TableCache<T> cache = table.getTableCache();
		Optional<TableCell<T>> optCell = cache.tryTake(column);
		optCell.ifPresent(c -> {
			c.updateColumn(column);
			c.updateRow(index, this);
			c.updateItem(item);
		});
		return optCell.orElseGet(creator);
		// TODO eventually make my own class duh
	}

	/**
	 * Caches the given cell for the given column index.
	 * If the cache is disabled the cell is disposed.
	 * <p></p>
	 * Note that when cells are cached {@link TableCell#updateRow(int, DefaultTableRow)} is invoked
	 * with {-1 and null} as parameters, and {@link TableCell#updateColumn(TableColumn)} is invoked
	 * with {null} as parameter.
	 */
	protected void cacheCell(int index, TableCell<T> cell) {
		if (!table.isColumnsCacheEnabled()) {
			cell.dispose();
			return;
		}
		TableCache<T> cache = table.getTableCache();
		TableColumn<T, ? extends TableCell<T>> column = getColumn(index);
		cell.updateRow(-1, null);
		cell.updateColumn(null);
		cache.cache(column, cell);
	}
}
