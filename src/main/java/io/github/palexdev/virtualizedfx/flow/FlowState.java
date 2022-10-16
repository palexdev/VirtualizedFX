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
import io.github.palexdev.virtualizedfx.flow.FlowMapping.FullMapping;
import io.github.palexdev.virtualizedfx.flow.FlowMapping.PartialMapping;
import io.github.palexdev.virtualizedfx.flow.FlowMapping.ValidMapping;
import io.github.palexdev.virtualizedfx.flow.paginated.PaginatedVirtualFlow;

import java.util.*;

/**
 * Class used by the {@link ViewportManager} to represent the state of the viewport at a given time.
 * <p>
 * The idea is to have an immutable state so each state is a different object, with some exceptional cases
 * when the state doesn't need to be re-computed, so the old object is returned.
 * <p></p>
 * This offers information such as:
 * <p> - The range of items contained in the state, {@link #getRange()}. <b>Note</b> that sometimes this range doesn't
 * correspond to the needed range of items. To always get a 100% accurate range use the {@link OrientationHelper}.
 * An example could be the {@code PaginatedVirtualFlow} in case a page has not enough items to be full, still all the needed
 * cells are built anyway and stored in the state.
 * <p> - The cells in the viewport, mapped like follows itemIndex -> cell
 * <p> - The position each cell must have in the viewport
 * <p> - The expected number of cells, {@link #getTargetSize()}
 * <p> - The type of event that lead the old state to transition to this new one, see {@link UpdateType}
 * <p> - A flag to check if new cells were created or some were deleted, {@link #haveCellsChanged()}.
 * This is used by {@link VirtualFlowSkin} since we want to update the viewport children only when the cells
 * changed.
 * <p> - A flag specifically for the {@link PaginatedVirtualFlow} to indicate whether the state also contains some
 * cells that are hidden in the viewport, {@link #anyHidden()}
 * <p></p>
 * This also contains a particular global state, {@link #EMPTY}, typically used to indicate that the viewport
 * is empty, and no state can be created.
 */
@SuppressWarnings("rawtypes")
public class FlowState<T, C extends Cell<T>> {
	//================================================================================
	// Static Properties
	//================================================================================
	public static final FlowState EMPTY = new FlowState<>();

	//================================================================================
	// Properties
	//================================================================================
	private final VirtualFlow<T, C> virtualFlow;
	private final IntegerRange range;
	private final Map<Integer, C> cells = new TreeMap<>();
	private final Set<Double> positions = new TreeSet<>();
	private final int targetSize;
	private UpdateType type = UpdateType.INIT;
	private boolean cellsChanged = false;
	private boolean hidden = false;

	//================================================================================
	// Constructors
	//================================================================================
	private FlowState() {
		this.virtualFlow = null;
		this.range = IntegerRange.of(-1);
		this.targetSize = -1;
	}

	public FlowState(VirtualFlow<T, C> virtualFlow, IntegerRange range) {
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
	 * In some cases (especially for the {@link PaginatedVirtualFlow}) it may happen that some cells are still
	 * present in the old state, for this reason a check makes sure that they are properly disposed and removed.
	 *
	 * @param newRange the new state's range of items
	 * @return the new state
	 */
	public FlowState<T, C> transition(IntegerRange newRange) {
		if (range.equals(newRange)) return this;
		type = UpdateType.SCROLL;

		FlowState<T, C> newState = new FlowState<>(virtualFlow, newRange);
		Deque<Integer> toUpdate = new ArrayDeque<>();
		for (int i = newRange.getMin(); i <= newRange.getMax(); i++) {
			C common = cells.remove(i);
			if (common != null) {
				newState.addCell(i, common);
				continue;
			}
			toUpdate.add(i);
		}

		Iterator<Map.Entry<Integer, C>> it = cells.entrySet().iterator();
		while (!toUpdate.isEmpty() && it.hasNext()) {
			Map.Entry<Integer, C> next = it.next();
			C cell = next.getValue();
			int cIndex = toUpdate.removeFirst();
			T item = virtualFlow.getItems().get(cIndex);
			cell.updateIndex(cIndex);
			cell.updateItem(item);
			newState.addCell(cIndex, cell);
			it.remove();
		}

		// Ensure that no cells remains in the old state, also dispose them
		// if that's the case
		if (!cells.isEmpty())
			disposeAndClear();

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
	 * Once the new state has been computed, changes the update type to {@link UpdateType#CHANGE} and copies the positions
	 * {@code Set} too (for reusable positions)
	 *
	 * @param changes the list of {@link ListChangeHelper.Change}s processed by the {@link ListChangeHelper}
	 * @return the new state
	 */
	public FlowState<T, C> transition(List<Change> changes) {
		FlowState<T, C> newState = this;
		for (Change change : changes) {
			newState = newState.transition(change);
		}
		newState.positions.addAll(positions);
		newState.type = UpdateType.CHANGE;
		return newState;
	}

	/**
	 * This is responsible for processing a single {@link Change} bean and produce a new state
	 * according to the change's {@link ChangeType}.
	 * <p></p>
	 * In the following lines I'm going to document each type.
	 * <p></p>
	 * <b>PERMUTATION</b>
	 * The permutation case while not being the most complicated can still be considered a bit heavy to compute
	 * since cells keep their index but all their items must be updated, so the performance is totally dependent
	 * on the cell's {@link Cell#updateItem(Object)} implementation.
	 * <p></p>
	 * <b>REPLACE</b>
	 * The algorithm for replacements gets a {@link Deque} of the cells keySet, then from the first index to the last one.
	 * In this loop it checks if the at the "i" index no change occurred, in this case the cell is removed from the old
	 * state and copied as is in the new state.
	 * <p>
	 * If a change occurred, an index is extracted from the deque and at this point two things can happen:
	 * <p> If the index is null it means that a new cell need to be created
	 * <p> Otherwise a cell is extracted from the old state, its item updated
	 * <p> Then the cell index is updated and the cell moved to the new state
	 * <p></p>
	 * Last but not least we check if there are any remaining cells in the old state. In such case those are disposed,
	 * removed from the old state, and also the positions {@code Set} is invalidated
	 * <p></p>
	 * <b>ADDITION</b>
	 * The computation in case of added items to the list is complex and a bit heavy on performance. There are
	 * several things to consider, and it was hard to find a generic algorithm that would correctly compute the new state
	 * in all possible situations, included exceptional cases and for {@link PaginatedVirtualFlow} too.
	 * <p></p>
	 * The simplest of these cases is when changes occur after the displayed range of items, the old state will be
	 * returned. In all the other cases the computation for the new state can begin.
	 * <p>
	 * The first step is to get a series of useful information such as:
	 * <p> - The index of the first item to display, {@link #getFirst()}
	 * <p> - The index of the last item to display, {@link #getLast()} for {@link PaginatedVirtualFlow} and
	 * {@link #getLastAvailable()} for {@link VirtualFlow}
	 * <p>
	 * At this point the computation begins. The new algorithm uses "mappings" to process how old cells will be used
	 * in the new state, see also {@link FlowMapping}
	 * <p></p>
	 * There are three types of mappings:
	 * <p>
	 * The first mappings we do are the ones for valid cells, those that come before the index at which the change
	 * occurred. The built mappings are of type {@link ValidMapping}, cells are moved as they are to the new state
	 * <p>
	 * Then we get the cells keySet as a {@link Deque} with {@link #getKeysDeque()} and we start checking for
	 * partial updates and full updates.
	 * <p>
	 * For each index in the deque (until the new state doesn't need any more cells) we have three checks:
	 * <p> - We check if the extracted index has already been mapped before, in this case we go to the next iteration
	 * <p> - We check that the index is not null (see {@link Deque#poll()}) and that the expected new index is in the
	 * range of the new state and that it is not one of the indexes at which a change occurred. If all the conditions are
	 * met a {@link PartialMapping} is built, this will extract the cell from the old state, update its index and then
	 * add it to the new state
	 * <p> - In case the previous conditions were not met there are yet two cases to distinguish. If the inner conditions
	 * were not met then there are cells that can be reused, if the index was null, there are no cells that can be reused.
	 * In any of these two cases a {@link FullMapping} is built. The mapping will then decide if a new cell is needed,
	 * or an old one must be updated.
	 * <p>
	 * Once the mappings have been created they are "executed" by calling {@link FlowMapping#manage(FlowState, FlowState)}
	 * for each of them.
	 * <p></p>
	 * Last steps depend on the flow implementation. For {@link VirtualFlow} we check if the old range and the new range
	 * are not the same. When an addition occur, the range is sure to not change exception for specific cases, for which
	 * the positions {@code Set} is invalidated. For {@link PaginatedVirtualFlow} we ensure that no cells are left in the
	 * old state by moving them to the new state (hidden cells for example)
	 * <p></p>
	 * <b>REMOVAL</b>
	 * The computation in case of removed items from the list is complex and a bit heavy on performance. There are
	 * several things to consider, and it was hard to find a generic algorithm that would correctly compute the new state
	 * in all possible situations, included exceptional cases.
	 * <p></p>
	 * The simplest of these cases is when changes occur after the displayed range of items, the old state will be
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
	 * and removed from the positions {@code Set}. Note that in such case we also need to indicate that the viewport needs to
	 * update its children, as always just by setting {@link #haveCellsChanged()} to true.
	 *
	 * @param change the {@link Change} to process which eventually will lead to the new state
	 * @return a new {@code ViewportState} object or in exceptional cases the old state
	 */
	protected FlowState<T, C> transition(Change change) {
		switch (change.getType()) {
			case PERMUTATION: {
				FlowState<T, C> newState = new FlowState<>(virtualFlow, range);
				cells.forEach((index, cell) -> {
					T item = virtualFlow.getItems().get(index);
					cell.updateItem(item);
				});
				newState.addCells(cells);
				cells.clear();
				return newState;
			}
			case REPLACE: {
				OrientationHelper helper = virtualFlow.getOrientationHelper();
				int cellsNum = cellsNum();
				int last = helper.lastVisible();
				int first = last - cellsNum + 1;
				if (change.getFrom() > last) return this;

				IntegerRange newRange = IntegerRange.of(first, last);
				FlowState<T, C> newState = new FlowState<>(virtualFlow, newRange);

				Deque<Integer> available = new ArrayDeque<>(cells.keySet());
				for (int i = first; i <= last; i++) {
					if (!change.hasChanged(i)) {
						newState.addCell(i, cells.remove(i));
						available.remove(i);
						continue;
					}

					Integer index = available.poll();
					T item = virtualFlow.getItems().get(i);
					C cell;
					if (index != null) {
						cell = cells.remove(index);
						cell.updateItem(item);
					} else {
						cell = virtualFlow.getCellFactory().apply(item);
					}
					cell.updateIndex(i);
					newState.addCell(i, cell);
				}

				if (!isEmpty()) {
					Iterator<Map.Entry<Integer, C>> it = cells.entrySet().iterator();
					while (it.hasNext()) {
						C cell = it.next().getValue();
						cell.dispose();
						it.remove();
					}
					positions.clear();
				}

				newState.setCellsChanged(newState.cellsNum() != cellsNum);
				return newState;
			}
			case ADD: {
				boolean viewportFull = isViewportFull();
				if (viewportFull && change.getFrom() > range.getMax()) return this;

				// Pre-computation
				int first = getFirst();
				int last = getLast();
				int cellsNum = cellsNum();
				IntegerRange newRange = IntegerRange.of(first, last);
				FlowState<T, C> newState = new FlowState<>(virtualFlow, newRange);

				// Compute mappings
				// In short: old indexes will be mapped to what they should
				// be in the new state.
				// Valid ones remain the same
				// Partials (item unchanged, index to update) are shifted
				// Full (item changed) are mapped to the new needed items
				// Once mappings are computed they are "executed"
				Set<Integer> available = IntegerRange.expandRangeToSet(newRange);
				Map<Integer, FlowMapping<T, C>> mappings = new HashMap<>();
				for (int i = first; i < change.getFrom(); i++) {
					mappings.put(i, new ValidMapping<>(i, i));
					available.remove(i);
				}

				int from = Math.max(change.getFrom(), first);
				int targetSize = computeTargetSize(newState.targetSize);
				int lastInvalid = -1;
				Deque<Integer> keys = getKeysDeque();
				while (!available.isEmpty() || mappings.size() != targetSize) {
					Integer index = keys.poll();
					if (mappings.containsKey(index)) continue;

					if (index != null) {
						int newIndex = index + change.size();
						if (IntegerRange.inRangeOf(newIndex, newRange) && !change.hasChanged(newIndex) && available.contains(newIndex)) {
							mappings.put(index, new PartialMapping<>(index, newIndex));
							available.remove(newIndex);
							continue;
						}
					}

					int fIndex;
					if (index == null) {
						fIndex = lastInvalid;
						lastInvalid--;
					} else {
						fIndex = index;
					}
					mappings.put(fIndex, new FullMapping<>(fIndex, from));
					available.remove(from);
					from++;
				}
				mappings.values().forEach(m -> m.manage(this, newState));

				if (virtualFlow instanceof PaginatedVirtualFlow) {
					// Ensure that remaining cells in the old state are carried by the new state too
					newState.addCells(cells);
					cells.clear();
				} else if (!range.equals(newRange)) {
					// If ranges are not the same there was an exceptional case
					// as described by computePositions() documentation
					// In such cases the old positions are not valid
					positions.clear();
				}

				newState.setCellsChanged(newState.cellsNum() != cellsNum);
				return newState;
			}
			case REMOVE: {
				if (change.getFrom() > range.getMax()) break;

				int max = Math.min(range.getMin() + targetSize - 1, virtualFlow.getItems().size() - 1);
				int min = Math.max(0, max - targetSize + 1);
				IntegerRange newRange = IntegerRange.of(min, max);
				FlowState<T, C> newState = new FlowState<>(virtualFlow, newRange);

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
						it.remove();
					}
					newState.setCellsChanged(true);
					positions.clear();
				}

				return newState;
			}
		}
		return this;
	}

	/**
	 * This is responsible for computing the positions {@code Set} which will be used by the {@link OrientationHelper}
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
	 * only if the adjust flag is false, the type of state is {@link UpdateType#CHANGE} and the positions {@code Set}
	 * size is greater or equal to the {@link #getTargetSize()}.
	 * <p>
	 * In this case we can use the old positions since some cell may have changed, in index, item or both, but the positions
	 * (not considering which cell has the specific position) remain the same.
	 * <p> 2) The second case is where the real layout computation happens.
	 * We start from the bottom, so: {@code bottom = (cells.size() - 1) * cellSize}. This value is adjusted if the
	 * adjust flag is true as {@code bottom -= cellSize}.
	 * <p>
	 * At this point we iterate from the end of the range to the start (so reverse order), each cell is put in the
	 * positions {@code Set} with the current bottom position, updated at each iteration as
	 * follows {@code bottom -= cellSize}.
	 */
	public Set<Double> computePositions() {
		if (isEmpty()) return Set.of();
		if (virtualFlow instanceof PaginatedVirtualFlow) {
			return computePaginatedPositions();
		}

		OrientationHelper helper = virtualFlow.getOrientationHelper();
		double cellSize = virtualFlow.getCellSize();
		int first = helper.firstVisible();
		int last = first + helper.maxCells() - 1;
		boolean adjust = last > virtualFlow.getItems().size() - 1 && isViewportFull();

		if (!adjust && type == UpdateType.CHANGE && positions.size() >= targetSize) {
			return positions;
		}

		positions.clear();
		double bottom = (cells.size() - 1) * cellSize;
		if (adjust) {
			bottom -= cellSize;
		}

		for (int i = range.getMax(); i >= range.getMin(); i--) {
			positions.add(bottom);
			bottom -= cellSize;
		}
		return positions;
	}

	/**
	 * This is the implementation of {@link #computePositions()} exclusively for {@link PaginatedVirtualFlow}s.
	 * <p>
	 * This is much simpler as there is no "free" scrolling, all cells will have a precise position at any time in the
	 * page.
	 * <p></p>
	 * First we clear the positions {@code Set} to ensure there's no garbage in it.
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
	public Set<Double> computePaginatedPositions() {
		positions.clear();

		PaginatedVirtualFlow pFlow = (PaginatedVirtualFlow) virtualFlow;
		OrientationHelper helper = pFlow.getOrientationHelper();
		double cellSize = virtualFlow.getCellSize();
		int first = helper.firstVisible();
		int last = Math.min(first + helper.maxCells() - 1, pFlow.getItems().size() - 1);
		IntegerRange range = new IntegerRange(first, last);

		double pos;
		for (int i = first; i <= last; i++) {
			C cell = cells.get(i);
			pos = positions.size() * cellSize;
			positions.add(pos);
			cell.getNode().setVisible(true);
		}

		cells.keySet().stream()
				.filter(i -> !IntegerRange.inRangeOf(i, range))
				.map(i -> cells.get(i).getNode())
				.peek(node -> hidden = true)
				.forEach(n -> n.setVisible(false));
		return positions;
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
	 * Retrieves the last cell index with {@link #getLastAvailable()}, then removes and returns
	 * the cell from the cells map.
	 */
	protected C removeLast() {
		int last = getLastAvailable();
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
	 * Shortcut for {@link OrientationHelper#firstVisible()}.
	 */
	public int getFirst() {
		return virtualFlow.getOrientationHelper().firstVisible();
	}

	/**
	 * Shortcut for {@link OrientationHelper#lastVisible()}.
	 */
	public int getLast() {
		return virtualFlow.getOrientationHelper().lastVisible();
	}

	/**
	 * @return the last cell available in the cells map
	 */
	public int getLastAvailable() {
		return cells.keySet().stream()
				.max(Integer::compareTo)
				.orElse(range.getMax());
	}

	/**
	 * @return converts the cells keySet to a {@link Deque}. For {@link PaginatedVirtualFlow}
	 * extra care is needed since hidden cells must appear at the end of the deque
	 */
	protected Deque<Integer> getKeysDeque() {
		if (virtualFlow instanceof PaginatedVirtualFlow && hidden) {
			Deque<Integer> deque = new ArrayDeque<>();
			Set<Integer> keys = new LinkedHashSet<>(cells.keySet());
			Iterator<Integer> it = keys.iterator();
			while (it.hasNext()) {
				Integer index = it.next();
				C cell = cells.get(index);
				if (!cell.getNode().isVisible()) continue;
				deque.add(index);
				it.remove();
			}
			deque.addAll(keys);
			return deque;
		}
		return new ArrayDeque<>(cells.keySet());
	}

	/**
	 * @return the expected number of items of the state
	 */
	protected int computeTargetSize(int expectedSize) {
		if (virtualFlow instanceof PaginatedVirtualFlow) {
			PaginatedVirtualFlow<T, C> pFlow = (PaginatedVirtualFlow<T, C>) virtualFlow;
			if (pFlow.getCurrentPage() == pFlow.getMaxPage()) {
				int remainder = pFlow.getItems().size() % pFlow.getCellsPerPage();
				return (remainder != 0) ? remainder : expectedSize;
			}
		}
		return Math.min(virtualFlow.getItems().size(), expectedSize);
	}

	/**
	 * Shortcut to dispose all cells present in this state's cells map and then clear it.
	 */
	protected void disposeAndClear() {
		cells.values().forEach(C::dispose);
		cells.clear();
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

	/**
	 * @return the {@link VirtualFlow} instance associated to this state
	 */
	public VirtualFlow<T, C> getVirtualFlow() {
		return virtualFlow;
	}

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
	protected Set<Double> getPositions() {
		return positions;
	}

	/**
	 * @return {@link #getPositions()} but public and wrapped in an unmodifiable Map
	 */
	public Set<Double> getPositionsUnmodifiable() {
		return Collections.unmodifiableSet(positions);
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

	/**
	 * @return whether any of the cells in the state have been hidden
	 */
	public boolean anyHidden() {
		return hidden;
	}
}
