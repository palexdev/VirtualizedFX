package io.github.palexdev.virtualizedfx.table;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.grid.GridRow;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Although this can be considered a helper class to support {@link TableState}, unlike {@link GridRow} it
 * is actually more than that. In fact, this class extends {@link Region} making it a concrete part of the
 * table's layout. This node is intended to contain the cell produced by each column as well as helping the
 * state with changes such as:
 * <p> - Initialization of the row
 * <p> - Changes in the range of displayed columns
 * <p> - Changes in the item represented by the row
 * <p> - Changes in the index at which the index is
 * <p> - Changes of cell factories from columns
 * <p></p>
 * To accomplish such tasks {@code TableRow}s contains information such as:
 * <p> - the row's index
 * <p> - the range of columns displayed (in other words the range of cells it will contain)
 * <p> - a map which holds the cells contained in itself, mapped as: columnIndex -> cell
 * <p></p>
 * Also note that this is an abstract class. {@link VirtualTable} allows to use any kind of custom
 * implementation as long as it implements the API defined by this class. I opted for an abstract class
 * because I didn't want to make the api public since these <b>should not</b> be called by the user but
 * are automatically managed by the {@link TableState} class.
 */
public abstract class TableRow<T> extends Region {
	//================================================================================
	// Properties
	//================================================================================
	private final String STYLE_CLASS = "table-row";
	protected final VirtualTable<T> table;
	protected int index;
	protected IntegerRange columns;
	protected final Map<Integer, TableCell<T>> cells = new TreeMap<>();

	//================================================================================
	// Constructors
	//================================================================================
	public TableRow(VirtualTable<T> table, int index, IntegerRange columns) {
		this.table = table;
		this.index = index;
		this.columns = columns;
		getStyleClass().add(STYLE_CLASS);
	}

	//================================================================================
	// Abstract Methods
	//================================================================================

	// Init

	/**
	 * Should specify the behavior for when the row is created.
	 */
	protected abstract TableRow<T> init();

	// Updates

	/**
	 * Should specify the behavior for when the range of displayed columns changes.
	 */
	protected abstract void updateColumns(IntegerRange columns);

	/**
	 * Should specify the behavior for when the item represented by the row and its cells changes.
	 */
	protected abstract void updateItem();

	/**
	 * Should specify the behavior for when the index of the row changes.
	 */
	protected abstract void updateIndex(int index);

	/**
	 * Should specify the behavior for when both the item represented by the row and its cells, as
	 * well as the index of the row change.
	 */
	protected abstract void updateFull(int index);

	/**
	 * Should specify the behavior for then one of the columns has changed its cell factory at runtime.
	 * <p>
	 * Note that this takes the column index as a parameter since if that index is present in the
	 * cells map then that cell is the one that will have to be replaced.
	 */
	protected abstract void updateColumnFactory(int cIndex);

	// Misc

	/**
	 * Should specify the behavior to convert the index of a column to a cell width.
	 * In other words, should take the cell at the given index from the map and retrieve its width.
	 */
	protected abstract double getWidthOf(int cIndex);

	/**
	 * Should specify the behavior for when the cells have changed.
	 */
	protected abstract void cellsChanged();

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Disposes all the cells in this row, then clears the map.
	 * <p></p>
	 * Note that this will also attempt at removing the cells from the table's cache.
	 */
	protected void clear() {
		cells.forEach((i, c) -> {
			c.dispose();
			if (table.isColumnsCacheEnabled()) {
				TableCache<T> cache = table.getTableCache();
				Optional.ofNullable(getColumn(i))
						.ifPresent(col -> cache.remove(col, c));
			}
		});
		cells.clear();
	}

	/**
	 * @return the number of cells in this row
	 */
	public int size() {
		return cells.size();
	}

	/**
	 * Shortcut for {@link VirtualTable#getColumn(int)}.
	 */
	public TableColumn<T, ? extends TableCell<T>> getColumn(int cIndex) {
		return table.getColumn(cIndex);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * Overridden to do nothing. Layout is handled by {@link TableHelper}.
	 */
	@Override
	protected void layoutChildren() {
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

	/**
	 * @return the {@link VirtualTable} instance this row is associated to
	 */
	public VirtualTable<T> getTable() {
		return table;
	}

	/**
	 * @return the index of the row. In other words the index of the item represented by
	 * this row
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @return the range of columns/cells displayed by the row
	 */
	public IntegerRange getColumns() {
		return columns;
	}

	/**
	 * @return the map containing the cells
	 */
	protected final Map<Integer, TableCell<T>> getCells() {
		return cells;
	}

	/**
	 * @return the cells as an unmodifiable map
	 */
	public Map<Integer, TableCell<T>> getCellsUnmodifiable() {
		return Collections.unmodifiableMap(cells);
	}

	/**
	 * Converts the cells to a list of nodes using {@link TableCell#getNode()}.
	 */
	public List<Node> getCellsAsNodes() {
		return cells.values().stream()
				.map(TableCell::getNode)
				.collect(Collectors.toList());
	}
}
