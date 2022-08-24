/*
 * Copyright (C) 2022 Parisi Alessandro
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX).
 *
 * VirtualizedFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.flow;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.utils.fx.ListChangeHelper;
import io.github.palexdev.mfxcore.utils.fx.ListChangeHelper.Change;
import io.github.palexdev.mfxcore.utils.fx.ListChangeHelper.ChangeType;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.enums.UpdateType;
import io.github.palexdev.virtualizedfx.flow.paginated.PaginatedVirtualFlow;

import java.util.*;

/**
 * Class used by the {@link ViewportManager} to represent the state of the viewport at a given time.
 * <p>
 * The idea is to have an immutable state so each state is a different object.
 * <p></p>
 * This offers information such as:
 * <p> - The range of displayed items, {@link #getRange()}
 * <p> - The cells in the viewport, mapped like follows itemIndex -> cell
 * <p> - The position of the cells in the viewport, this is typically used by the {@link OrientationHelper} to lay out the
 * cells according to the state, mapped like follows cell -> position
 * <p> - The expected number of cells, {@link #getTargetSize()}
 * <p> - The type of event that lead the old state to transition to this new one, see {@link UpdateType}
 * <p> - A flag to check if new cells were created or some were deleted, {@link #haveCellsChanged()}.
 * This is used by {@link VirtualFlowSkin} since we want to update the viewport children only when the cells
 * changed.
 * <p></p>
 * This also contains a particular global state, {@link #EMPTY}, typically used to indicate that the viewport
 * is empty, and no state can be created.
 */
@SuppressWarnings("rawtypes")
public class ViewportState<T, C extends Cell<T>> {
	//================================================================================
	// Static Properties
	//================================================================================
	public static final ViewportState EMPTY = new ViewportState<>();

	//================================================================================
	// Properties
	//================================================================================
	private final VirtualFlow<T, C> virtualFlow;
	private final IntegerRange range;
	private final Map<Integer, C> cells = new TreeMap<>();
	private final Map<C, Double> layoutMap = new LinkedHashMap<>();
	private final int targetSize;
	private UpdateType type = UpdateType.INIT;
	private boolean cellsChanged = false;

	//================================================================================
	// Constructors
	//================================================================================
	private ViewportState() {
		this.virtualFlow = null;
		this.range = IntegerRange.of(-1);
		this.targetSize = -1;
	}

	public ViewportState(VirtualFlow<T, C> virtualFlow, IntegerRange range) {
		this.virtualFlow = virtualFlow;
		this.range = range;
		this.targetSize = virtualFlow.getOrientationHelper().maxCells();
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * This is responsible for transitioning this state to a new one given the new range of items.
	 * <p></p>
	 * If the given range is equal to {@link #getRange()} then exits and returns itself.
	 * <p>
	 * The first operation is to set the update type to {@link UpdateType#SCROLL}.
	 * <p>
	 * Then all the common cells between {@link #getRange()} and the given range are added to the new state,
	 * no update on them since they are already valid.
	 * <p>
	 * {@code ThisRange: [0, 10]. NewRange: [3, 13]. ValidCells: [3, 10]}
	 * <p>
	 * For every missing index cells are removed from this state, updated both in item and index, then
	 * added to the new state.
	 * <p>
	 * So in the above example, cells {@code [0, 2]} are removed from this state, updated, then
	 * added to the new state as {@code [11, 13]}
	 *
	 * @param newRange the new state's range of items
	 * @return the new state
	 */
	public ViewportState<T, C> transition(IntegerRange newRange) {
		if (range.equals(newRange)) return this;
		type = UpdateType.SCROLL;

		ViewportState<T, C> newState = new ViewportState<>(virtualFlow, newRange);
		List<Integer> toUpdate = new ArrayList<>();
		for (int i = newRange.getMin(); i <= newRange.getMax(); i++) {
			C common = cells.remove(i);
			if (common != null) {
				newState.addCell(i, common);
				continue;
			}
			toUpdate.add(i);
		}

		if (!toUpdate.isEmpty()) {
			int index = 0;
			Iterator<Map.Entry<Integer, C>> it = cells.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, C> next = it.next();
				C cell = next.getValue();
				int cIndex = toUpdate.get(index);
				T item = virtualFlow.getItems().get(cIndex);
				cell.updateIndex(cIndex);
				cell.updateItem(item);
				newState.addCell(cIndex, cell);
				index++;
				it.remove();
			}
		}

		return newState;
	}

	/**
	 * This is responsible for transitioning this state to a new one given a series of changes occurred
	 * in the items list.
	 * <p>
	 * Note that this is made to work with {@link ListChangeHelper} and {@link Change}.
	 * <p>
	 * Why? Tl;dr: For performance reasons.
	 * <p>
	 * In JavaFX when multiple changes occur in the list you have to process them one by one, typically in a while loop.
	 * This is not great for a virtual flow, which needs to be super efficient, the less are the updates on the viewport
	 * the better the performance. {@link ListChangeHelper} helps with this by processing all the changes in one big {@link Change}
	 * bean. Note that since the change is only one, indexes may differ from the JavaFX's ones (during change processing).
	 * Why still a list of changes? Well, the JavaFX's API documentation about list change listeners....sucks, and I'm not
	 * entirely sure if it will always be a single change, for this reason it is designed to return a list of changes, BUT,
	 * it should always be one.
	 * <p></p>
	 * For each change in the list creates a new state from the previous one, starting by this one, using {@link #transition(Change)}.
	 * Once the new state has been computed, changes the update type to {@link UpdateType#CHANGE} and copies the layout map
	 * too (for reusable positions)
	 *
	 * @param changes the list of {@link ListChangeHelper.Change}s processed by the {@link ListChangeHelper}
	 * @return the new state
	 */
	public ViewportState<T, C> transition(List<Change> changes) {
		ViewportState<T, C> newState = this;
		for (Change change : changes) {
			newState = newState.transition(change);
		}
		newState.layoutMap.putAll(layoutMap);
		newState.type = UpdateType.CHANGE;
		return newState;
	}

	/**
	 * This is responsible for processing a single {@link Change} bean and produce a new state
	 * according to the change's {@link ChangeType}.
	 * <p></p>
	 * In the following line I'm going to document each type.
	 * <p></p>
	 * <b>PERMUTATION</b>
	 * The permutation case while not being the most complicated can still be considered a bit heavy to compute
	 * since cells keep their index but all their items must be updated, so the performance is totally dependent
	 * on the cell's {@link Cell#updateItem(Object)} implementation.
	 * <p></p>
	 * <b>REPLACE</b>
	 * When items are replaced in the list there are several things to consider.
	 * The algorithm iterates over the state's range, from min to max cells are removed by index from the
	 * old state. If the cell is null it means that a new one must be created, then it's added to the new state.
	 * Otherwise, if the cell is not null, we check whether the index is one at which a change occurred by using
	 * {@link Change#hasChanged(int)}, in that case it's needed to update the cell's item before adding it to the
	 * new state.
	 * <p>
	 * It may happen that the old state still contains some cell, but at this point we assume
	 * that the viewport is already full, so, these cells must be disposed and removed from the layout map as well.
	 * <p>
	 * Last but not least, we must correctly set the {@link #haveCellsChanged()} flag so that the viewport can update its
	 * children if needed, to set it we simply check if the number of cells in the new state is equal to the old state,
	 * note that the latter is stored in a variable before starting the whole process as cells are then removed from the
	 * old state.
	 * <p></p>
	 * <b>ADDITION</b>
	 * The computation in case of added items to the list is complex and a bit heavy on performance. There are
	 * several things to consider, and it was hard to find a generic algorithm that would correctly compute the new state
	 * in all possible situations, included exceptional cases.
	 * <p>
	 * The simplest of these cases is when the viewport is full and the range at which changes occurred is greater than
	 * the state's range, any item added after the last displayed item is not a relevant change, the old state will be
	 * returned. In all other cases the computation for the new state can begin.
	 * <p>
	 * First we copy all the valid cells, the ones for which both the index and the item are correct, a simple
	 * loop from the start of the range to the start index of the change will do the trick, cells are removed from the old
	 * state to be added to the new state.
	 * <p>
	 * In the second step we need to calculate a series of int values:
	 * <p> - The start index which is the maximum between the range's min and the change's start index.
	 * This is because if changes occurred above the state's range we still want to operate in its range
	 * <p> - The end index which is the start index plus the number of added items - 1
	 * <p> - The tail index which will keep track of the last index currently available in the cells map
	 * <p> - The missing number of cells, this is used in case the viewport is not full, so we want to create a number of
	 * new cells to fill it.
	 * <p>
	 * After computing those indexes a loop from the start to the end indexes will:
	 * <p> - Retrieve the item at index i
	 * <p> - Check if the missing counter is still greater than 0, in such case a new cell is created, added to the new state
	 * and then the counter decreased by one
	 * <p> - If the missing counter is 0 we remove the tail cell from the old state, this cell will be used to represent
	 * one of the added items at index i, so both the item and index must be updated, then the cell is added to the
	 * new state and the tail counter decreased by one
	 * <p>
	 * After this loop, we still need to update the semi valid cells. These cells are the ones that have the correct item
	 * but for which the index must be shifted by the number of added items.
	 * Another loop from the start index to the current tail value will remove the cell at i from the old state, update
	 * its index as i + numberOfAddedItems, then added to the new state.
	 * <p>
	 * Last but not least, we must correctly set the {@link #haveCellsChanged()} flag so that the viewport can update its
	 * children if needed, to set it we simply check if the number of cells in the new state is equal to the old state,
	 * note that the latter is stored in a variable before starting the whole process as cells are then removed from the
	 * old state.
	 * <p></p>
	 * <b>REMOVAL</b>
	 * The computation in case of added items to the list is complex and a bit heavy on performance. There are
	 * several things to consider, and it was hard to find a generic algorithm that would correctly compute the new state
	 * in all possible situations, included exceptional cases.
	 * <p>
	 * The simplest of these cases is when changes occurred after the displayed range of items, the old state will be
	 * returned. In all the other cases the computation for the new state can begin.
	 * <p>
	 * The first step is to separate those cells that only require a partial update (only index) from the others.
	 * First we get a Set of indexes from the state's range using {@link IntegerRange#expandRangeToSet(IntegerRange)},
	 * then we remove from the Set all the indexes at which the removal occurred. Before looping on these indexes we
	 * also convert the {@link Change#getIndexes()} to an array of primitives.
	 * <p>
	 * In the loop we extract the cell at index i and to update its index we must first compute the shift. We do this
	 * by using binary search(it needs the previous array).
	 * The new index will be: {@code int newIndex = index - findShift(array, index)}, see {@link #findShift(int[], int)}.
	 * <p>
	 * If the new index is below the range min the update is skipped and the loop goes to the next index, otherwise,
	 * the index is updated and the cell added to the new state.
	 * <p>
	 * The next step is to update those cells which need a full update (both item and index). First we compute their
	 * indexes by expanding the new state's range to a Set and then removing from it all the cells that have already been
	 * added to the new state (which means they have been updated already, also keep in mind that we always operate on indexes
	 * so newState.cells.keySet()).
	 * <p>
	 * Now a {@link Deque} is built on the remaining cells in the old state (cells.keySet() again) and the update can begin.
	 * We loop over the previous built Set of indexes:
	 * <p> 1) We get the item at index i
	 * <p> 2) We get one of the indexes from the deque as {@link Deque#removeFirst()}
	 * <p> 3) We remove the cell at that index from the old state
	 * <p> 4) We update the cell with the index i (from the loop) and the previously extracted item
	 * <p> 5) The cell is added to the new state
	 * <p>
	 * Last but not least we check if any cells are still available in the old state. These need to be disposed
	 * and removed from the layout map. Note that in such case we also need to indicate that the viewport needs to
	 * update its children, as always just by setting {@link #haveCellsChanged()} to true.
	 *
	 * @param change the {@link Change} to process which eventually will lead to the new state
	 * @return a new {@code ViewportState} object or in exceptional cases the old state
	 */
	protected ViewportState<T, C> transition(Change change) {
		switch (change.getType()) {
			case PERMUTATION: {
				ViewportState<T, C> newState = new ViewportState<>(virtualFlow, range);
				cells.forEach((index, cell) -> {
					T item = virtualFlow.getItems().get(index);
					cell.updateItem(item);
				});
				newState.addCells(cells);
				cells.clear();
				return newState;
			}
			case REPLACE: {
				int cellsNum = cellsNum();
				int min = range.getMin();
				int max = Math.min(min + targetSize - 1, virtualFlow.getItems().size() - 1);
				IntegerRange newRange = IntegerRange.of(min, max);
				ViewportState<T, C> newState = new ViewportState<>(virtualFlow, newRange);

				for (int i = min; i <= max; i++) {
					C cell = cells.remove(i);

					if (cell == null) {
						T item = virtualFlow.getItems().get(i);
						cell = virtualFlow.getCellFactory().apply(item);
						cell.updateIndex(i);
						newState.addCell(i, cell);
						continue;
					}

					if (change.hasChanged(i)) {
						T item = virtualFlow.getItems().get(i);
						cell.updateItem(item);
					}
					newState.addCell(i, cell);
				}

				if (!isEmpty()) {
					Iterator<Map.Entry<Integer, C>> it = cells.entrySet().iterator();
					while (it.hasNext()) {
						C cell = it.next().getValue();
						cell.dispose();
						layoutMap.remove(cell);
						it.remove();
					}
				}

				newState.setCellsChanged(newState.cellsNum() != cellsNum);
				return newState;
			}
			case ADD: {
				if (isViewportFull() && change.getFrom() > range.getMax()) break;

				int cellsNum = cellsNum();
				int min = range.getMin();
				int max = Math.min(min + targetSize - 1, virtualFlow.getItems().size() - 1);
				IntegerRange newRange = IntegerRange.of(min, max);
				ViewportState<T, C> newState = new ViewportState<>(virtualFlow, newRange);

				// Copy valid
				for (int i = min; i < change.getFrom(); i++) {
					newState.addCell(i, cells.remove(i));
				}

				// Add new items
				int from = Math.max(min, change.getFrom());
				int to = from + change.size() - 1;
				int tail = range.getMax();
				int missing = Math.min(targetSize, virtualFlow.getItems().size()) - (cells.size() + newState.cells.size());
				for (int i = from; i <= to; i++) {
					T item = virtualFlow.getItems().get(i);
					if (missing > 0) {
						C cell = virtualFlow.getCellFactory().apply(item);
						cell.updateIndex(i);
						newState.addCell(i, cell);
						missing--;
						continue;
					}

					C cell = cells.remove(tail);
					cell.updateItem(item);
					cell.updateIndex(i);
					newState.addCell(i, cell);
					tail--;
				}

				// Update semi-valid
				for (int i = from; i <= tail; i++) {
					int newIndex = i + change.size();
					C cell = cells.remove(i);
					cell.updateIndex(newIndex);
					newState.addCell(newIndex, cell);
				}

				newState.setCellsChanged(newState.cellsNum() != cellsNum);
				return newState;
			}
			case REMOVE: {
				if (change.getFrom() > range.getMax()) break;

				int max = Math.min(range.getMin() + targetSize - 1, virtualFlow.getItems().size() - 1);
				int min = Math.max(0, max - targetSize + 1);
				IntegerRange newRange = IntegerRange.of(min, max);
				ViewportState<T, C> newState = new ViewportState<>(virtualFlow, newRange);

				Set<Integer> pUpdate = IntegerRange.expandRangeToSet(range);
				pUpdate.removeAll(change.getIndexes());

				int[] changeIndexes = change.getIndexes().stream()
						.sorted()
						.mapToInt(Integer::intValue)
						.toArray();
				for (Integer index : pUpdate) {
					int newIndex = index - findShift(changeIndexes, index);
					if (newIndex < range.getMin()) continue;
					C cell = cells.remove(index);
					cell.updateIndex(newIndex);
					newState.addCell(newIndex, cell);
				}

				Set<Integer> fUpdate = IntegerRange.expandRangeToSet(newRange);
				fUpdate.removeAll(newState.cells.keySet());

				Deque<Integer> available = new ArrayDeque<>(cells.keySet());
				for (Integer index : fUpdate) {
					T item = virtualFlow.getItems().get(index);
					int cellIndex = available.removeFirst();
					C cell = cells.remove(cellIndex);
					cell.updateIndex(index);
					cell.updateItem(item);
					newState.addCell(index, cell);
				}

				if (!isEmpty()) {
					Iterator<Map.Entry<Integer, C>> it = cells.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry<Integer, C> next = it.next();
						C cell = next.getValue();
						cell.dispose();
						layoutMap.remove(cell);
						it.remove();
					}
					newState.setCellsChanged(true);
				}

				return newState;
			}
		}
		return this;
	}

	/**
	 * This is responsible for computing the layout map which will be used by the {@link OrientationHelper}
	 * to correctly position the cells in the viewport.
	 * <p></p>
	 * There are two cases in which the method won't execute:
	 * <p> 1) This is empty
	 * <p> 2) The virtual flow using this state is instance of {@link PaginatedVirtualFlow}, in this case
	 * {@link #computePaginatedPositions()} is used instead.
	 * <p></p>
	 * This is rather complex as this also takes into account some exceptional cases which otherwise would lead
	 * to cells being positioned outside the viewport.
	 * <p>
	 * Long story short. The way the {@link OrientationHelper} works is that the viewport cannot "scroll" beyond
	 * {@link VirtualFlow#getCellSize()} and the virtual flow always has one cell of overscan/buffer whatever you want to
	 * call it. This means that when you reach the end of the viewport you always have that one cell of overscan that needs
	 * to be positioned above all the others, because of course you cannot add anything more at the end. This issue is
	 * rather problematic and lead to this solution. The state is also responsible for computing the cells' position
	 * as it depends on many factors such as the current range/position of the virtual flow, the type of the state
	 * (see {@link UpdateType})
	 * <p></p>
	 * I'm going to try to explain the functioning:
	 * <p>
	 * First we compute some variables that will be needed later for the layout computation such as:
	 * <p> - The cells' size
	 * <p> - The first and last indexes
	 * <p> - And a flag to indicate whether special adjustments are needed, named "adjust"
	 * Just to make it more clear, this is what determines the value of this flag:
	 * {@code last > virtualFlow.getItems().size() - 1 && isViewportFull()}
	 * <p></p>
	 * Then we have two separate cases.
	 * <p> 1) The first one is pretty specific, it leads to the layout computation
	 * only if the adjust flag is false, the type of state is {@link UpdateType#CHANGE} and the layout map size
	 * is greater or equal to the {@link #getTargetSize()}.
	 * <p>
	 * In this case we can use the old positions since some cell may have changed, in index, item or both, but the positions
	 * (not considering which cell has the specific position) remain the same.
	 * <p> 2) The second case is where the real layout computation happens.
	 * We start from the bottom, so: {@code bottom = (cells.size() - 1) * cellSize}. This value is adjusted if the
	 * adjust flag is true as {@code bottom -= cellSize}.
	 * <p>
	 * At this point we iterate from the end of the range to the start (so reverse order), each cell is put in the
	 * layout map with the current bottom position, updated at each iteration as follows {@code bottom -= cellSize}.
	 */
	public void computePositions() {
		if (isEmpty()) return;
		if (virtualFlow instanceof PaginatedVirtualFlow) {
			computePaginatedPositions();
			return;
		}

		OrientationHelper helper = virtualFlow.getOrientationHelper();
		double cellSize = virtualFlow.getCellSize();
		int first = helper.firstVisible();
		int last = first + helper.maxCells() - 1;
		boolean adjust = last > virtualFlow.getItems().size() - 1 && isViewportFull();

		if (!adjust && type == UpdateType.CHANGE && layoutMap.size() >= targetSize) {
			List<Double> positions = new ArrayList<>(layoutMap.values());
			positions.sort(Comparator.reverseOrder());
			for (int i = range.getMax(); i >= range.getMin(); i--) {
				C cell = cells.get(i);
				double pos = positions.remove(0);
				layoutMap.put(cell, pos);
			}
			return;
		}

		double bottom = (cells.size() - 1) * cellSize;
		if (adjust) {
			bottom -= cellSize;
		}

		for (int i = range.getMax(); i >= range.getMin(); i--) {
			C cell = cells.get(i);
			layoutMap.put(cell, bottom);
			bottom -= cellSize;
		}
	}

	/**
	 * This is the implementation of {@link #computePositions()} exclusively for {@link PaginatedVirtualFlow}s.
	 * <p>
	 * This is much simpler as there is no free scrolling, all cells will have a precise position at any time in the
	 * page.
	 * <p></p>
	 * First we clear the layout map to ensure there's no garbage in it.
	 * <p>
	 * Then we get a series of important parameters, such as:
	 * <p> - The cells' size
	 * <p> - The first and last visible cells
	 * <p>
	 * At this point the computation can begin. This method differs in this from {@link #computePositions()} because
	 * it positions cells from top to bottom.
	 * <p>
	 * Another crucial difference is that we must ensure that only the needed cells will be visible. Let's suppose we
	 * want to show 5 cells per page but because of the number of items, the last page can show only 2 items. The other 3
	 * cells are not removed from the viewport, but they are hidden and not laid out.
	 */
	public void computePaginatedPositions() {
		layoutMap.clear();

		PaginatedVirtualFlow pFlow = (PaginatedVirtualFlow) virtualFlow;
		OrientationHelper helper = pFlow.getOrientationHelper();
		double cellSize = virtualFlow.getCellSize();
		int first = helper.firstVisible();
		int last = Math.min(first + helper.maxCells() - 1, pFlow.getItems().size() - 1);
		IntegerRange range = new IntegerRange(first, last);

		double pos;
		for (int i = first; i <= last; i++) {
			C cell = cells.get(i);
			pos = layoutMap.size() * cellSize;
			layoutMap.put(cell, pos);
			cell.getNode().setVisible(true);
		}

		cells.keySet().stream()
				.filter(i -> !IntegerRange.inRangeOf(i, range))
				.map(i -> cells.get(i).getNode())
				.forEach(n -> n.setVisible(false));
	}

	/**
	 * Given an ordered array of indexes and the index to find, returns
	 * the index at which resides. If the index is not present, returns
	 * the index at which it would be located.
	 *
	 * @see Arrays#binarySearch(int[], int)
	 */
	protected int findShift(int[] indexes, int index) {
		int shift = Arrays.binarySearch(indexes, index);
		return shift > -1 ? shift : -(shift + 1);
	}

	/**
	 * Adds the given [index, cell] entry in the cells map.
	 */
	protected void addCell(int index, C cell) {
		cells.put(index, cell);
	}

	/**
	 * Adds all the given cells to this state's cells map.
	 */
	protected void addCells(Map<Integer, C> cells) {
		this.cells.putAll(cells);
	}

	/**
	 * Retrieves the last cell index with {@link #getLast()}, then removes and returns
	 * the cell from the cells map.
	 */
	protected C removeLast() {
		int last = getLast();
		return cells.remove(last);
	}

	/**
	 * @return whether the cells map is empty
	 */
	public boolean isEmpty() {
		return cells.isEmpty();
	}

	/**
	 * @return the number of cells in the cells map
	 */
	public int cellsNum() {
		return cells.size();
	}

	/**
	 * @return whether the number of cells is greater or equal to {@link #getTargetSize()}
	 */
	public boolean isViewportFull() {
		return cellsNum() >= targetSize;
	}

	/**
	 * @return the state's range start index
	 */
	public int getFirst() {
		return range.getMin();
	}

	/**
	 * @return the last displayed index as the minimum between ({@link #getFirst()} + {@link #cellsNum()}) and
	 * the range end index
	 */
	public int getLast() {
		return Math.min(getFirst() + cellsNum(), range.getMax());
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

	/**
	 * @return the range of items displayed
	 */
	public IntegerRange getRange() {
		return range;
	}

	/**
	 * @return the Map containing the cells mapped by the items' index
	 */
	protected Map<Integer, C> getCells() {
		return cells;
	}

	/**
	 * @return {@link #getCells()} but public and wrapped in an unmodifiable Map
	 */
	public Map<Integer, C> getCellsUnmodifiable() {
		return Collections.unmodifiableMap(cells);
	}

	/**
	 * @return the Map containing the cells mapped by their position in the viewport
	 */
	protected Map<C, Double> getLayoutMap() {
		return layoutMap;
	}

	/**
	 * @return {@link #getLayoutMap()} but public and wrapped in an unmodifiable Map
	 */
	public Map<C, Double> getLayoutMapUnmodifiable() {
		return Collections.unmodifiableMap(layoutMap);
	}

	/**
	 * This is the minimum number of cells for which the viewport can be considered full.
	 */
	public int getTargetSize() {
		return targetSize;
	}

	/**
	 * @return the event type that lead to the creation of this state
	 */
	public UpdateType getType() {
		return type;
	}

	/**
	 * @return whether changes occurred that lead to the addition or removal of cells, which means that the
	 * viewport must update its children
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
