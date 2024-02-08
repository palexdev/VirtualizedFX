package io.github.palexdev.virtualizedfx.grid;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.virtualizedfx.cells.Cell;
import io.github.palexdev.virtualizedfx.utils.StateMap;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.scene.Node;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

@SuppressWarnings({"rawtypes", "SameParameterValue"})
public class VFXGridState<T, C extends Cell<T>> {
	//================================================================================
	// Static Properties
	//================================================================================
	public static final VFXGridState EMPTY = new VFXGridState() {
		@Override
		protected Cell<Object> removeCell(int index) {
			return null;
		}

		@Override
		protected void dispose() {}
	};

	//================================================================================
	// Properties
	//================================================================================
	private final VFXGrid<T, C> grid;
	private final IntegerRange rowsRange;
	private final IntegerRange columnsRange;
	private final int nColumns;
	private final StateMap<T, C> cells = new StateMap<>();
	private boolean cellsChanged = false;

	//================================================================================
	// Constructors
	//================================================================================
	private VFXGridState() {
		this.grid = null;
		this.rowsRange = Utils.INVALID_RANGE;
		this.columnsRange = Utils.INVALID_RANGE;
		this.nColumns = 0;
	}

	public VFXGridState(VFXGrid<T, C> grid, IntegerRange rowsRange, IntegerRange columnsRange) {
		this.grid = grid;
		this.rowsRange = rowsRange;
		this.columnsRange = columnsRange;
		this.nColumns = grid.getHelper().maxColumns();
	}

	//================================================================================
	// Methods
	//================================================================================
	protected void addCell(int rIndex, int cIndex, C cell) {
		int linear = GridUtils.subToInd(nColumns, rIndex, cIndex);
		addCell(linear, grid.getItems().get(linear), cell);
	}

	protected void addCell(int index, C cell) {
		addCell(index, grid.getItems().get(index), cell);
	}

	protected void addCell(int index, T item, C cell) {
		cells.put(index, item, cell);
	}

	protected C removeCell(int rIndex, int cIndex) {
		return removeCell(GridUtils.subToInd(nColumns, rIndex, cIndex));
	}

	protected C removeCell(int index) {
		C c = cells.remove(index);
		if (c == null) c = removeCell(grid.getItems().get(index));
		return c;
	}

	protected C removeCell(T item) {
		return cells.remove(item);
	}

	protected void dispose() {
		VFXCellsCache<T, C> cache = grid.getCache();
		cache.cache(getCellsByIndex().values());
		cells.clear();
	}

	//================================================================================
	// Getters/Setters
	//================================================================================
	public VFXGrid<T, C> getGrid() {
		return grid;
	}

	public IntegerRange getRowsRange() {
		return rowsRange;
	}

	public IntegerRange getColumnsRange() {
		return columnsRange;
	}

	/**
	 * @return the map containing the cells
	 * @see StateMap
	 */
	protected StateMap<T, C> getCells() {
		return cells;
	}

	/**
	 * @return the map containing the cells by their index
	 */
	protected SequencedMap<Integer, C> getCellsByIndex() {
		return cells.getByIndex();
	}

	/**
	 * @return the map containing the cells by their item
	 * @see StateMap#resolve()
	 */
	protected Map<T, C> getCellsByItem() {
		return cells.resolve();
	}

	/**
	 * @return the map containing the cells by their index, unmodifiable
	 */
	public SequencedMap<Integer, C> getCellsByIndexUnmodifiable() {
		return Collections.unmodifiableSequencedMap(cells.getByIndex());
	}

	/**
	 * @return the map containing the cells by their item, unmodifiable
	 * @see StateMap#resolve()
	 */
	public Map<T, C> getCellsByItemUnmodifiable() {
		return Collections.unmodifiableMap(cells.resolve());
	}

	/**
	 * @return converts the cells' map to a list of nodes by calling {@link C#toNode()} on each cell
	 */
	public List<Node> getNodes() {
		return getCellsByIndex().values().stream()
			.map(C::toNode)
			.toList();
	}

	/**
	 * @return the number of cells in the {@link StateMap}
	 */
	public int size() {
		return cells.size();
	}

	/**
	 * @return whether the {@link StateMap} is empty
	 * @see StateMap
	 */
	public boolean isEmpty() {
		return cells.isEmpty();
	}

	/**
	 * @return whether the cells have changed since the last state. This is used to indicate if more or less cells are
	 * present in this state compared to the old one. Used by the default skin to check whether the viewport has to
	 * update its children or not.
	 * @see VFXGridSkin
	 */
	public boolean haveCellsChanged() {
		return cellsChanged;
	}

	/**
	 * @see #haveCellsChanged()
	 */
	protected void setCellsChanged(boolean cellsChanged) {
		this.cellsChanged = cellsChanged;
	}
}