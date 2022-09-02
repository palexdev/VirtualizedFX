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
import java.util.stream.Collectors;

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
	 * <p>
	 * In some cases (especially for the {@link PaginatedVirtualFlow}) it may happed that some cells are still
	 * present in the old state, for this reason a check makes sure that they are properly disposed and removed.
	 *
	 * @param newRange the new state's range of items
	 * @return the new state
	 */
	public ViewportState<T, C> transition(IntegerRange newRange) {
		if (range.equals(newRange)) return this;
		type = UpdateType.SCROLL;

		ViewportState<T, C> newState = new ViewportState<>(virtualFlow, newRange);
		Deque<Integer> toUpdate = new ArrayDeque<>();
		for (int i = newRange.getMin(); i <= newRange.getMax(); i++) {
			C common = cells.remove(i);
			if (common != null) {
				newState.addCell(i, common);
				continue;
			}
			toUpdate.add(i);
		}

		if (!toUpdate.isEmpty()) {
			Iterator<Map.Entry<Integer, C>> it = cells.entrySet().iterator();
			while (it.hasNext() && !toUpdate.isEmpty()) {
				Map.Entry<Integer, C> next = it.next();
				C cell = next.getValue();
				int cIndex = toUpdate.removeFirst();
				T item = virtualFlow.getItems().get(cIndex);
				cell.updateIndex(cIndex);
				cell.updateItem(item);
				newState.addCell(cIndex, cell);
				it.remove();
			}
		}

		// Ensure that no cells remains in the old state, also dispose them
		// if that's the case
		if (!cells.isEmpty())
			disposeAndClearCells();

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
	 * in all possible situations, included exceptional cases, and that would work for the {@link PaginatedVirtualFlow} too.
	 * <p>
	 * The simplest of these cases is when the viewport is full and the range at which changes occurred is greater than
	 * the state's range, any item added after the last displayed item is not a relevant change, the old state will be
	 * returned. In all other cases the computation for the new state can begin.
	 * <p>
	 * Before everything else, we get a series of useful information like the first cell of the range retrieved with
	 * {@link #getFirst()} (note that this depends on the flow implementation), the last cell of the range with
	 * {@link OrientationHelper#lastVisible()} and then we copy the cells keySet (so their indexes) to a temporary
	 * {@link TreeSet} which will tell us the available/remaining cells/indexes.
	 * <p>
	 * At this point the computation begins, there are several cases to consider.
	 * <p>
	 * The first case are the cells that appear before the index at which the change occurred. Those cells are copied as
	 * they are from the old state to the new one, their index is also removed from the "availables" set.
	 * <p>
	 * Now we have to distinguish two other cases: the cells that need a partial update (only index update) and the ones
	 * that need a full update (both index and item updates).
	 * <p>
	 * The partial updates are computes from the remaining available indexes and stored in a {@link Deque}.
	 * Before proceeding there are two operations to do:
	 * <p>
	 * In case we are dealing with a {@link PaginatedVirtualFlow} the available indexes may not be correct. For example,
	 * for pages having only a bunch of visible cells the ones that are hidden must be excluded. In this case the
	 * partial update indexes are computed iterating on the deque and filtering only by visible cells.
	 * <p>
	 * The next operation for all types of flows is to remove some indexes from the deque under the following conditions:
	 * <p> 1) until the for loop index "i" must be lesser that the change size
	 * <p> 2) the availables indexes must not be empty
	 * <p> 3) the viewport must be full, {@link #isViewportFull()}
	 * <p></p>
	 * Now for each of the indexes remaining in the deque cells are removed from the old state and updated only by index.
	 * The index is also removed from the "availables" set.
	 * <p></p>
	 * The last remaining case, cells that need a full update, branches out into two sub-cases.
	 * Starting from the cellIndex we need (given by {@code Math.max(first, change.getFrom())}),
	 * and for the remaining number of cells we need (given by {@code newState.getRange().diff() - newState.cellsNum() + 1}),
	 * an index is removed from the remaining available ones and here we have the two sub-cases.
	 * <p> 1) The index is not null, which means that the cell can be extracted from the old state, updated and added
	 * to the new state
	 * <p> 2) The index is null, which means we need to create a new cell with the flow's factory, update its index and
	 * add it to the new state
	 * <p></p>
	 * The last steps are:
	 * <p> 1) For {@link PaginatedVirtualFlow} we must ensure that no cells remain in the old state, as this would lead
	 * to an exception later on when computing their positions, keep in mind that the {@link PaginatedVirtualFlow} uses the
	 * visibility trick for the viewport. It may seem that some cells are missing, but they are there, just hidden.
	 * For this reason, remaining cells are copied to the new state and then removed from the old state.
	 * <p> 2) We must check if the cells number has changed between the old state and the new state as this determines
	 * whether the viewport should update its children.
	 * <p>
	 * After all of that the new state object is returned.
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
				boolean viewportFull = isViewportFull();
				if (viewportFull && change.getFrom() > range.getMax()) break;

				// Pre-computation
				OrientationHelper helper = virtualFlow.getOrientationHelper();
				int first = getFirst();
				int last = helper.lastVisible();
				int cellsNum = cellsNum();
				Set<Integer> availables = new TreeSet<>(cells.keySet());
				ViewportState<T, C> newState = new ViewportState<>(virtualFlow, IntegerRange.of(first, last));

				// Copy valid
				for (int i = first; i < change.getFrom(); i++) {
					newState.addCell(i, cells.remove(i));
					availables.remove(i);
				}

				// Compute the indexes for which cells will be partially updated
				Deque<Integer> pUpdates = new ArrayDeque<>(availables);
				if (virtualFlow instanceof PaginatedVirtualFlow) {
					pUpdates = pUpdates.stream()
							.filter(i -> cells.get(i).getNode().isVisible())
							.collect(Collectors.toCollection(ArrayDeque::new));
				}
				for (int i = 0; i < change.size() && !availables.isEmpty() && viewportFull; i++) {
					pUpdates.removeLast();
				}

				for (Integer index : pUpdates) {
					int newIndex = index + change.size();
					C cell = cells.remove(index);
					cell.updateIndex(newIndex);
					newState.addCell(newIndex, cell);
					availables.remove(index);
				}

				// Perform the remaining full updates
				int cellIndex = Math.max(first, change.getFrom());
				int remaining = newState.getRange().diff() - newState.cellsNum() + 1;
				Deque<Integer> availableQueue = new ArrayDeque<>(availables);
				for (int i = 0; i < remaining; i++) {
					C cell;
					Integer index = availableQueue.pollLast();
					T item = virtualFlow.getItems().get(cellIndex);
					if (index != null) {
						cell = cells.remove(index);
						cell.updateItem(item);
					} else {
						cell = virtualFlow.getCellFactory().apply(item);
					}
					cell.updateIndex(cellIndex);
					newState.addCell(cellIndex, cell);
					cellIndex++;
				}

				// Special handling for PaginatedVirtualFlow
				// Ensure that remaining cells in the old state are carried by
				// the new state too
				if (virtualFlow instanceof PaginatedVirtualFlow) {
					newState.addCells(cells);
					cells.clear();
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
	public Map<C, Double> computePositions() {
		if (isEmpty()) return Map.of();
		if (virtualFlow instanceof PaginatedVirtualFlow) {
			return computePaginatedPositions();
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
			return layoutMap;
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
		return layoutMap;
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
	public Map<C, Double> computePaginatedPositions() {
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
		return layoutMap;
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
	 * @return whether the number of cells is greater or equal to {@link #getTargetSize()}.
	 * Special handling for {@code PaginatedVirtualFlow} covered, the cellsNum is computed
	 * as the number of visible nodes in the cells map
	 */
	public boolean isViewportFull() {
		int cellsNum = cellsNum();
		if (virtualFlow instanceof PaginatedVirtualFlow) {
			cellsNum = (int) cells.values().stream()
					.filter(c -> c.getNode().isVisible())
					.count();
		}
		return cellsNum >= targetSize;
	}

	/**
	 * @return the state's range start index.
	 * Special handling for {@code PaginatedVirtualFlow} covered, the min is computed
	 * with {@link OrientationHelper#firstVisible()} as some cells may be hidden
	 */
	public int getFirst() {
		int first = range.getMin();
		if (virtualFlow instanceof PaginatedVirtualFlow) {
			first = virtualFlow.getOrientationHelper().firstVisible();
		}
		return first;
	}

	/**
	 * @return the last displayed cell. To make sure that this returns a correct value
	 * (even for special cases for {@code PaginatedVirtualFlow}), the value is computed by iterating on the cells keySet
	 * to get the max index
	 */
	public int getLast() {
		return cells.keySet().stream()
				.max(Integer::compareTo)
				.orElse(range.getMax());
	}

	/**
	 * Shortcut to dispose all cells present in this state's cells map and then clear it.
	 */
	protected void disposeAndClearCells() {
		cells.values().forEach(C::dispose);
		cells.clear();
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
