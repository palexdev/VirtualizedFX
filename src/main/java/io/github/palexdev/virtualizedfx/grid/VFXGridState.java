package io.github.palexdev.virtualizedfx.grid;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.virtualizedfx.cells.Cell;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.list.VFXListState;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap.StateMap;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import java.util.*;

/**
 * Immutable object to represent the state of a {@link VFXGrid} in a specific moment in time. In other words, each and
 * every state is given by a unique combination of the grid's properties (in terms of values).
 * <p>
 * The state carries four important pieces of information:
 * <p> 1) The range of rows to display
 * <p> 2) The range of columns to display
 * <p> 3) The cells that are currently in the viewport
 * <p> 4) A flag that indicates whether cells have changed since the last state
 * <p> Note: the combination of 1 and 2 determines the point 3, so the items to display from {@link VFXGrid#itemsProperty()}
 * <p></p>
 * <b>Indexes and loops in a 2D structure</b>
 * When implementing the {@link VFXGrid}'s "infrastructure", the state was one of the first classes to be written since
 * it is crucial to display the needed items. When you already have a good and stable example to look at
 * (in my case {@link VFXList}) it's only natural to try and follow it as much as possible. Other than adding another
 * {@link IntegerRange} variable (rows and columns), there's nothing new here compared to {@link VFXListState}.
 * However, because we have two ranges, an issue arises on how to store the cells; we can't map indexes to cells right,
 * we would have to store them by [r, c] coordinates right?
 * <p>
 * Actually, we can, and the concept I'm going to explain here is crucial and used pretty much everywhere in the
 * grid's "infrastructure". To be clear, yes, we could create a wrapper class to represent a coordinate, but that would
 * mean that the specialized data structure {@link StateMap} could not be used. We would need to adapt it to use our
 * {@code Coordinate} class, which inevitably would lead to code duplication.
 * <p>
 * To begin with, the grid's underlying data structure is one dimensional, a plain and simple {@link ObservableList}.
 * This means that item 0 is in the cell at coordinates [0, 0], item 1 would be at coordinates [0, 1] and so on.
 * Of course, it also depends on the number of columns and rows.
 * <p>
 * The important thing to notice here is that essentially what the grid implicitly does is <b>converting a position</b> from
 * the list <b>to a pair of coordinates</b>. Well, we can also do the opposite, starting from the two ranges, we can get
 * the corresponding position in the list for each pair of coordinates.
 * <p>
 * <b>Linear Index</b>: A linear index refers to a single integer value used to access an element in a one-dimensional
 * array or a multidimensional array that has been flattened into a one-dimensional sequence.
 * In other words, it's a way to map the element's position in the array to a single integer.
 * For example, in a 1D array, linear indexing is straightforward: each element has a unique linear index corresponding
 * to its position in the array.
 * However, in a multidimensional array, elements can be indexed using linear indexing by treating the array
 * as if it were one long sequence of elements, often following a row-major or column-major order.
 * <p></p>
 * <b>Subscripts</b>: Subscripts are used to refer to the indices of individual dimensions in a multidimensional array.
 * Each dimension of the array has its own subscript.
 * For example, in a 2D array, you might have subscripts (i, j),
 * where 'i' refers to the row index and 'j' refers to the column index.
 * <p></p>
 * The conversion from a pair of coordinates to a linear index is done by the utility {@link GridUtils#subToInd(int, int, int)}.
 * <p>
 * This strategy brings two main <b>consequences</b>:
 * <p> 1) the cells are still stored in a {@link StateMap}, but the index used in the mapping is the <b>linear index</b>.
 * Which means that if you want to get the coordinates, you'll have to convert them back using {@link GridUtils#indToSub(int, int)}.
 * <p> 2) this is the first time in my Java developer career I use labels to break out of loops. You see when you use two nested
 * for loops to iterate over the rows and columns ranges, it may happen that the resulting linear index is non-existing
 * in the list. In other words, the linear index is greater than {@code itemsNum - 1}. In such cases, it's useless to
 * continue the iterations. By placing the label above the external loop, we are able to immediately break out of the
 * <b>nested</b> loop, and as you may guess, this is indeed a nice performance optimization. An example of when this may
 * happen is in case of incomplete rows/columns.
 * <pre>
 * {@code
 * outer_loop:
 * for (rIdx : rowsRange) {
 *     for (cIdx : columnsRange) {
 *         ...
 *         ...
 *         if (...) break outer_loop;
 *     }
 * }
 * }
 * </pre>
 *
 * @see #EMPTY
 * @see StateMap
 */
@SuppressWarnings({"rawtypes", "SameParameterValue"})
public class VFXGridState<T, C extends Cell<T>> {
	//================================================================================
	// Static Properties
	//================================================================================
	/**
	 * Special instance of {@code VFXGridState} used to indicate that no cells can be present in the viewport at
	 * a certain time. The reasons can be many, for example, no cell factory, invalid range, width/height <= 0, etc...
	 * <p>
	 * This and {@link #isEmpty()} are two total different things!!
	 */
	public static final VFXGridState EMPTY = new VFXGridState() {
		@Override
		protected Cell<Object> removeCell(int index) {
			return null;
		}

		@Override
		protected Cell removeCell(Object item) {return null;}

		@Override
		protected Cell removeCell(int rIndex, int cIndex) {return null;}

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

	/**
	 * Converts the given row and column indexes to a linear index and delegates to {@link #addCell(int, Object, Cell)}.
	 */
	protected void addCell(int rIndex, int cIndex, C cell) {
		int linear = GridUtils.subToInd(nColumns, rIndex, cIndex);
		addCell(linear, grid.getItems().get(linear), cell);
	}

	/**
	 * Delegates to {@link #addCell(int, Object, Cell)}.
	 */
	protected void addCell(int index, C cell) {
		addCell(index, grid.getItems().get(index), cell);
	}

	/**
	 * Adds the given cell to the {@link StateMap} of this state object.
	 */
	protected void addCell(int index, T item, C cell) {
		cells.put(index, item, cell);
	}

	/**
	 * Delegates to {@link #removeCell(int)} by first converting the given row and column indexes to a linear index.
	 */
	protected C removeCell(int rIndex, int cIndex) {
		return removeCell(GridUtils.subToInd(nColumns, rIndex, cIndex));
	}

	/**
	 * Removes a cell from the {@link StateMap} for the given linear index. If the cell is not found, the next attempt
	 * is to remove it by the item at the given linear index in the {@link VFXGrid#itemsProperty()}.
	 */
	protected C removeCell(int index) {
		C c = cells.remove(index);
		if (c == null) c = removeCell(grid.getItems().get(index));
		return c;
	}

	/**
	 * Removes a cell from the {@link StateMap} for the given item.
	 */
	protected C removeCell(T item) {
		return cells.remove(item);
	}

	/**
	 * Disposes this state object by: caching all the cells ({@link VFXCellsCache#cache(Collection)}), and then
	 * clearing the {@link StateMap} by calling {@link StateMap#clear()}.
	 *
	 * @see StateMap
	 */
	protected void dispose() {
		VFXCellsCache<T, C> cache = grid.getCache();
		cache.cache(getCellsByIndex().values());
		cells.clear();
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

	/**
	 * @return the {@link VFXGrid} instance this state is associated to
	 */
	public VFXGrid<T, C> getGrid() {
		return grid;
	}

	/**
	 * @return the range of rows to display
	 */
	public IntegerRange getRowsRange() {
		return rowsRange;
	}

	/**
	 * @return the range of columns to display
	 */
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
	 * @return the list containing the cells by their item, as entries because of possible duplicates
	 * @see StateMap#resolve()
	 */
	protected List<Map.Entry<T, C>> getCellsByItem() {
		return cells.resolve();
	}

	/**
	 * @return the map containing the cells by their index, unmodifiable
	 */
	public SequencedMap<Integer, C> getCellsByIndexUnmodifiable() {
		return Collections.unmodifiableSequencedMap(cells.getByIndex());
	}

	/**
	 * @return the list containing the cells by their item, as entries because of possible duplicates, unmodifiable
	 * @see StateMap#resolve()
	 */
	public List<Map.Entry<T, C>> getCellsByItemUnmodifiable() {
		return Collections.unmodifiableList(cells.resolve());
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