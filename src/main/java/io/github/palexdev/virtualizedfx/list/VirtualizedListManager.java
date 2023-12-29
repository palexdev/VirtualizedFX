package io.github.palexdev.virtualizedfx.list;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.behavior.BehaviorBase;
import io.github.palexdev.virtualizedfx.cells.Cell;
import io.github.palexdev.virtualizedfx.utils.Utils;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ListProperty;

import java.util.*;

/**
 * Default behavior implementation for {@link VirtualizedList}. Although, to be precise, and as the name also suggests,
 * this can be considered more like a 'manager' than a behavior. Behaviors typically respond to user input, and then update
 * the component's state. This behavior contains core methods to respond to various properties change in {@link VirtualizedList}.
 * All computations here will generate a new {@link VirtualizedListState}, if possible, and update the list and the
 * layout (indirect, call to {@link VirtualizedList#requestViewportLayout()}).
 * <p>
 * By default, manages the following changes:
 * <p> - geometry changes (width/height changes), {@link #onGeometryChanged()}
 * <p> - position changes, {@link #onPositionChanged()}
 * <p> - cell factory changes, {@link #onCellFactoryChanged()}
 * <p> - items change, {@link #onItemsChanged()}
 * <p> - fit to breadth flag changes, {@link #onFitToBreadthChanged()}
 * <p> - cell size changes, {@link #onCellSizeChanged()}
 * <p> - orientation changes, {@link #onOrientationChanged()}
 * <p> - spacing changes {@link #onSpacingChanged()}
 * <p></p>
 * Last but not least, some of these computations may need to ensure the current vertical and horizontal positions are correct,
 * so that a valid state can be produced. To achieve this, {@link VirtualizedListHelper#invalidatePos()} is called.
 * However, invalidating the positions, also means that the {@link #onPositionChanged()} method could be potentially
 * triggered, thus generating an unwanted 'middle' state. For this reason a special flag {@link #invalidatingPos} is used
 * before the invalidation to avoid triggering that method.
 */
public class VirtualizedListManager<T, C extends Cell<T>> extends BehaviorBase<VirtualizedList<T, C>> {
	//================================================================================
	// Properties
	//================================================================================
	protected boolean invalidatingPos = false;

	//================================================================================
	// Constructors
	//================================================================================
	public VirtualizedListManager(VirtualizedList<T, C> list) {
		super(list);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * This core method is responsible for ensuring that the viewport always has the right amount of cells. This is called
	 * every time the list's geometry changes (width/height depending on the orientation), which means that this is also
	 * responsible for initialization (when width/height changes from 0.0 to > 0.0).
	 * <p>
	 * After preliminary checks done by {@link #listFactorySizeCheck()} and {@link #rangeCheck(IntegerRange, boolean, boolean)},
	 * the computation for the new state is delegated to the {@link #moveOrCreateAlgorithm(IntegerRange, VirtualizedListState)}.
	 * <p></p>
	 * Note that to compute a valid new state, it is important to also validate the list's positions by invoking
	 * {@link VirtualizedListHelper#invalidatePos()}.
	 */
	protected void onGeometryChanged() {
		invalidatingPos = true;
		VirtualizedList<T, C> list = getNode();
		VirtualizedListHelper<T, C> helper = list.getHelper();
		if (!listFactorySizeCheck()) return;

		// Ensure positions are correct!
		helper.invalidatePos();

		// If for whatever reason, the computed range is invalid, then set the state to EMPTY
		IntegerRange range = helper.range();
		if (!rangeCheck(range, true, true)) return;

		// Compute the new state
		VirtualizedListState<T, C> newState = new VirtualizedListState<>(list, range);
		moveOrCreateAlgorithm(range, newState);

		if (disposeCurrent()) newState.setCellsChanged(true);
		list.update(newState);
		invalidatingPos = false;
	}

	/**
	 * This core method is responsible for updating the list's state when the 'main' position changes (vPos for VERTICAL
	 * orientation, hPos for HORIZONTAL orientation). Since the list doesn't use throttling to limit the number of events/changes,
	 * and since scrolling can be very fast, performance here is crucial.
	 * <p>
	 * Immediately exists if: the special flag {@link #invalidatingPos} is true or the current state is {@link VirtualizedListState#EMPTY}.
	 * That boolean flag exists exactly for this, stop this method from executing. Many other computations here need to validate
	 * the positions by calling {@link VirtualizedListHelper#invalidatePos()}, so that the resulting state is valid.
	 * However, invalidating the positions may trigger this method, causing two or more state computations at the 'same' time;
	 * this behavior must be avoided.
	 * <p></p>
	 * For the sake of performance, this method tries to update only the cells which need it. The computation is divided
	 * in two steps:
	 * <p> 0) Prerequisites: the last range (retrieved from the current state), the new range (given by {@link VirtualizedListHelper#range()}).
	 * Note that if these two ranges are equal, then the method exits.
	 * <p> 1) First of all, we check for common indexes. Cells are removed from the old state and copied to the new one
	 * without updating, since it's not needed. For cells that are not found in the old state (not in common), the index
	 * is added to a queue
	 * <p> 2) Now we assume that the number of indexes in the queue and the number of cells remaining in the old state
	 * are equal. This is because a position change is not a geometry change, the number cannot change by just scrolling.
	 * Remaining cells are removed one by one from the old state, an index is also removed from the queue, then the cell
	 * is updated both in index and item. Finally, it is added to the new state, with the index removed from the queue.
	 * <p></p>
	 * The list's state is updated, {@link VirtualizedList#update(VirtualizedListState)}, but most importantly this also
	 * calls {@link VirtualizedList#requestViewportLayout()}. The reason for this 'forced' layout is that the remaining
	 * cells, due to the scroll, can now be higher or lower than their previous index, which means that they need to be
	 * repositioned in the viewport.
	 * <p></p>
	 * Last but not least: this algorithm is very similar to the {@link #intersectionAlgorithm()} one conceptually.
	 * Implementation-wise, there are a few different details that make this method much, much faster. For example, one
	 * assumption we can make here is that scrolling will not change the number of items to display, which means that all
	 * cells from the current/old state can be reused. Even the checks on some preconditions (like list size, cell size,
	 * ranges validity) are avoided. We assume that if any of those are not met, the positions cannot be changed,
	 * in other words, this will never be called.
	 */
	protected void onPositionChanged() {
		if (invalidatingPos) return;
		VirtualizedList<T, C> list = getNode();
		VirtualizedListState<T, C> state = list.getState();
		if (state == VirtualizedListState.EMPTY) return;

		VirtualizedListHelper<T, C> helper = list.getHelper();
		IntegerRange lastRange = state.getRange();
		IntegerRange range = helper.range();
		if (Objects.equals(lastRange, range) || Utils.INVALID_RANGE.equals(range)) return;

		// Compute the new state
		// Commons are just moved to the new state
		// New indexes are stored in a queue
		VirtualizedListState<T, C> newState = new VirtualizedListState<>(list, range);
		Deque<Integer> needed = new ArrayDeque<>();
		for (Integer i : range) {
			C common = state.removeCell(i);
			if (common != null) {
				newState.addCell(i, common);
				continue;
			}
			needed.add(i);
		}

		// Remaining cells are updated according to the above-built queue
		Iterator<C> it = state.getCellsByIndex().values().iterator();
		while (it.hasNext()) {
			int idx = needed.removeFirst();
			C cell = it.next();
			cell.updateIndex(idx);
			cell.updateItem(helper.indexToItem(idx));
			newState.addCell(idx, cell);
			it.remove();
		}
		list.update(newState);
		list.requestViewportLayout();
	}

	/**
	 * This method is responsible for updating the list's state when the {@link VirtualizedList#cellFactoryProperty()}
	 * changes. Unfortunately, this is always a costly operation because all cells need to be re-created, and the
	 * {@link VirtualizedListCache} cleaned. In fact, the very first operation done by this method is exactly this,
	 * the disposal of the current/old state and the cleaning of the cache. Luckily, this kind of change is likely to not happen very often.
	 * <p>
	 * After preliminary checks done by {@link #listFactorySizeCheck()} and {@link #rangeCheck(IntegerRange, boolean, boolean)},
	 * the computation for the new state is delegated to the {@link #moveOrCreateAlgorithm(IntegerRange, VirtualizedListState)}.
	 * <p>
	 * The new state's {@link VirtualizedListState#haveCellsChanged()} flag will always be {@code true} od course.
	 * The great thing about the factory change is that there is no need to invalidate the position.
	 */
	protected void onCellFactoryChanged() {
		VirtualizedList<T, C> list = getNode();

		// Dispose current state, cells if any (not EMPTY) are now in cache
		// Purge cache too, cells are from old factory
		if (disposeCurrent()) list.getCache().clear();
		if (!listFactorySizeCheck()) return;

		VirtualizedListState<T, C> current = list.getState();
		IntegerRange range = current.getRange();
		if (!rangeCheck(range, true, true)) return;

		VirtualizedListState<T, C> newState = new VirtualizedListState<>(list, range);
		moveOrCreateAlgorithm(range, newState);
		newState.setCellsChanged(true);
		list.update(newState);
	}

	/**
	 * Before describing the operations performed by this method, it's important for the reader to understand the difference
	 * between the two changes caught by this method. {@link VirtualizedList} makes use of a {@link ListProperty} to store
	 * the items to display. The property is essentially the equivalent of this {@code ObjectProperty<ObservableList>}. Now,
	 * if you are familiar with JavaFX, you probably know that there are two possible changes to listen to: one is changes
	 * to {@code ObjectProperty} (if the {@code ObservableList} instance changes), and the other are changes in the {@code ObservableList}
	 * instance itself. As you may guess, managing both these changes with a simple {@code ObjectProperty} is quite cumbersome,
	 * because you need two listeners: one that that catches changes in the list, and another to catch changes to the property.
	 * In particular, the latter has the task to add the first listener to the new {@code ObservableList} instance.
	 * <p>
	 * And here is where {@link ListProperty} comes in handy. By adding an {@link InvalidationListener} to this special
	 * property we are able to intercept both the type of changes always, even if the {@code ObservableList} instance changes,
	 * everything is handled automatically.
	 * <p>
	 * Needless to say, we use a {@code Property} to store the items to allow the usage of bindings!
	 * <p></p>
	 * This core method is responsible for updating the list's state when any of the two aforementioned changes happen.
	 * <p>
	 * These kind of updates are the most tricky and expensive. In particular, additions and removals can occur at any
	 * position in the list, which means that calculating the new state solely on the indexes is a no-go. It is indeed
	 * possible by, in theory, isolating the indexes at which the changes occurred, separating the cells that need only
	 * an index update from the ones that actually need a full update. However, such an approach requires a lot of code,
	 * is error-prone, and a bit heavy on performance. The new approach implemented here requires changes to the state
	 * class as well, {@link VirtualizedListState}.
	 * <p>
	 * The computation for the new state is very similar to the {@link #moveOrCreateAlgorithm(IntegerRange, VirtualizedListState)},
	 * but the first step, which tries to identify the common cells, is quite different. You see, as I said before, additions
	 * and removals can occur at any place in the list. Picture it with this example:
	 * <pre>
	 * {@code
	 * In list before: 0 1 2 3 4 5
	 * Add at index 2 these items: 99, 98
	 * In list after: 0 1 99 98 2 3 4 5
	 *
	 * Now let's suppose the range of displayed items is the same: [0, 5] (6 items)
	 * (I'm going now to write items with the index too, like this Index:Item)
	 * Items before: [0:0, 1:1, 2:2, 3:3, 4:4, 5:5]
	 * Items after: [0:0, 1:1, 2:99, 3:98, 4:2, 5:3]
	 * See? Items 2 and 3 are still there but in a different position (index) Since we assume item updates are more
	 * expensive than index updates, we must ensure to take those two cells and update them just by index
	 * }
	 * </pre>
	 * <p></p>
	 * For this reason, cells from the old state are not removed by index, but by <b>item</b>,
	 * {@link VirtualizedListState#removeCell(Object)}. First, we retrieve the item from the list that is now at index i
	 * (this index comes from the loop on the range), then we try to remove the cell for this item from the old state.
	 * If the cell is found, we update it by index and add it to the new state. Note that the index is also removed from
	 * the expanded range.
	 * <p>
	 * Now that 'common' cells have been properly updated, the remaining items are processed by the {@link #remainingsAlgorithm(Set, VirtualizedListState)}.
	 * <p></p>
	 * Last notes:
	 * <p> 1) This is one of those methods that to produce a valid new state needs to validate the list's positions,
	 * so it calls {@link VirtualizedListHelper#invalidatePos()}
	 * <p> 2) To make sure the layout is always correct, at the end we always invoke {@link VirtualizedList#requestViewportLayout()}.
	 * You can guess why from the above example, items 2 and 3 are still in the viewport, but at different indexes,
	 * which also means at different layout positions. There is no easy way to detect this, so better safe than sorry, always update the layout.
	 */
	protected void onItemsChanged() {
		invalidatingPos = true;
		VirtualizedList<T, C> list = getNode();
		VirtualizedListHelper<T, C> helper = list.getHelper();

		/*
		 * Ensure positions are correct
		 * In theory this is only needed if the list now is smaller than before (or the old one)
		 * But since that information is lost (we would have to track it here in some way), we always
		 * invalidate positions, after all, it's not a big deal anyway.
		 */
		helper.invalidatePos();

		// If the list is now empty, then set EMPTY state
		VirtualizedListState<T, C> current = list.getState();
		if (!listFactorySizeCheck()) return;

		// Compute range and new state
		IntegerRange range = helper.range();
		Set<Integer> expanded = IntegerRange.expandRangeToSet(range);
		VirtualizedListState<T, C> newState = new VirtualizedListState<>(list, range);

		// First update by index
		for (Integer index : range) {
			T item = helper.indexToItem(index);
			C c = current.removeCell(item);
			if (c != null) {
				expanded.remove(index);
				c.updateIndex(index);
				newState.addCell(index, item, c);
			}
		}

		// Process remainings with the "remainings' algorithm"
		remainingsAlgorithm(expanded, newState);

		if (disposeCurrent()) newState.setCellsChanged(true);
		list.update(newState);
		if (!newState.haveCellsChanged()) list.requestViewportLayout();
		invalidatingPos = false;
	}

	/**
	 * The easiest of all changes. It's enough to request a viewport layout, {@link VirtualizedList#requestViewportLayout()},
	 * and to make sure that the horizontal position is valid, {@link VirtualizedListHelper#invalidatePos()}.
	 */
	protected void onFitToBreadthChanged() {
		VirtualizedList<T, C> list = getNode();
		VirtualizedListHelper<T, C> helper = list.getHelper();
		list.requestViewportLayout();
		helper.invalidatePos(); // Not necessary to set invalidatingPos flag
	}

	/**
	 * This method is responsible for computing a new state when the {@link VirtualizedList#cellSizeProperty()} changes.
	 * <p>
	 * After preliminary checks done by {@link #listFactorySizeCheck()}, the computation for the new state is delegated to
	 * the {@link #intersectionAlgorithm()}.
	 * <p></p>
	 * Note that to compute a valid new state, it is important to also validate the list's positions by invoking
	 * {@link VirtualizedListHelper#invalidatePos()}.
	 */
	protected void onCellSizeChanged() {
		invalidatingPos = true;
		VirtualizedList<T, C> list = getNode();
		VirtualizedListHelper<T, C> helper = list.getHelper();

		// Ensure positions are correct
		helper.invalidatePos();

		if (!listFactorySizeCheck()) return;

		// Compute new state with the intersection algorithm
		VirtualizedListState<T, C> newState = intersectionAlgorithm();

		if (disposeCurrent()) newState.setCellsChanged(true);
		list.update(newState);
		invalidatingPos = false;
	}

	/**
	 * This method is responsible for computing a new state when the {@link VirtualizedList#orientationProperty()} changes.
	 * <p>
	 * After preliminary checks done by {@link #listFactorySizeCheck()}, the computation for the new state is delegated to
	 * the {@link #intersectionAlgorithm()}.
	 * <p></p>
	 * Note that to compute a valid new state, it is important to also validate the list's positions by invoking
	 * {@link VirtualizedListHelper#invalidatePos()}. Also, this will request the layout computation,
	 * {@link VirtualizedList#requestViewportLayout()}, even if the cells didn't change.
	 */
	protected void onOrientationChanged() {
		invalidatingPos = true;
		VirtualizedList<T, C> list = getNode();
		VirtualizedListHelper<T, C> helper = list.getHelper();
		if (!listFactorySizeCheck()) return;

		// Ensure positions are correct
		helper.invalidatePos();

		// Compute new state with the intersection algorithm
		VirtualizedListState<T, C> newState = intersectionAlgorithm();

		if (disposeCurrent()) newState.setCellsChanged(true);
		list.update(newState);
		if (!newState.haveCellsChanged()) list.requestViewportLayout();
		invalidatingPos = false;
	}

	/**
	 * This method is responsible for updating the list's state when the {@link VirtualizedList#spacingProperty()} changes.
	 * <p>
	 * After preliminary checks done by {@link #listFactorySizeCheck()} and {@link #rangeCheck(IntegerRange, boolean, boolean)},
	 * the computation for the new state is delegated to the {@link #moveOrCreateAlgorithm(IntegerRange, VirtualizedListState)}.
	 * <p></p>
	 * Note that to compute a valid new state, it is important to also validate the list's positions by invoking
	 * {@link VirtualizedListHelper#invalidatePos()}. Also, this will request the layout computation,
	 * {@link VirtualizedList#requestViewportLayout()}, even if the cells didn't change.
	 */
	protected void onSpacingChanged() {
		invalidatingPos = true;
		VirtualizedList<T, C> list = getNode();
		VirtualizedListHelper<T, C> helper = list.getHelper();

		// Ensure positions are correct
		helper.invalidatePos();

		// If range is invalid
		IntegerRange range = helper.range();
		if (!rangeCheck(range, true, true)) return;

		// Compute new state
		VirtualizedListState<T, C> newState = new VirtualizedListState<>(list, range);
		moveOrCreateAlgorithm(range, newState);

		if (disposeCurrent()) newState.setCellsChanged(true);
		list.update(newState);
		if (!newState.haveCellsChanged()) list.requestViewportLayout();
		invalidatingPos = false;
	}

	//================================================================================
	// Common
	//================================================================================

	/**
	 * Avoids code duplication. Typically used when, while iterating on a range,
	 * it's enough to move the cells from the current state to the new state. For indexes which are not found
	 * in the current state, a new cell is either taken from cache or created from the cell factory.
	 *
	 * @see VirtualizedListHelper#indexToCell(int)
	 * @see VirtualizedList#cellFactoryProperty()
	 */
	protected void moveOrCreateAlgorithm(IntegerRange range, VirtualizedListState<T, C> newState) {
		VirtualizedList<T, C> list = getNode();
		VirtualizedListHelper<T, C> helper = list.getHelper();
		VirtualizedListState<T, C> current = list.getState();
		for (Integer index : range) {
			C c = current.removeCell(index);
			if (c == null) {
				c = helper.indexToCell(index);
				c.updateIndex(index);
				newState.setCellsChanged(true);
			}
			newState.addCell(index, c);
		}
	}

	/**
	 * Avoids code duplication. Typically used in situations where the previous range and the new one are likely to be
	 * very close, but most importantly, that do not involve any change in the items' list.
	 * In such cases, the computation for the new state is divided in two parts:
	 * <p> 0) Prerequisites: the new range [min, max], the expanded range (a collection of indexes that goes from min to max),
	 * the current state, and the intersection between the current state's range and the new range
	 * <p> 1) The intersection allows us to distinguish between cells that can be moved as they are, without any update,
	 * from the current state to the new one. For this, it's enough to check that the intersection range is valid, and then
	 * a for loop. Common indexes are also removed from the expanded range!
	 * <p> 2) The remaining indexes in the expanded range are items that are new. Which means that if there are still cells
	 * in the current state, they need to be updated (both index and item). Otherwise, new ones are created by the cell factory.
	 * <p></p>
	 * <p> - See {@link Utils#intersection}: used to find the intersection between two ranges
	 * <p> - See {@link #rangeCheck(IntegerRange, boolean, boolean)}: used to validate the intersection range, both parameters
	 * are false!
	 * <p> - See {@link #remainingsAlgorithm(Set, VirtualizedListState)}: the second part of the algorithm is delegated to this
	 * method
	 */
	protected VirtualizedListState<T, C> intersectionAlgorithm() {
		VirtualizedList<T, C> list = getNode();
		VirtualizedListHelper<T, C> helper = list.getHelper();

		// New range, also expanded
		IntegerRange range = helper.range();
		Set<Integer> expandedRange = IntegerRange.expandRangeToSet(range);

		// Current and new states, intersection between current and new range
		VirtualizedListState<T, C> current = list.getState();
		VirtualizedListState<T, C> newState = new VirtualizedListState<>(list, range);
		IntegerRange intersection = Utils.intersection(current.getRange(), range);

		// If range valid, move common cells from current to new state. Also, remove common indexes from expanded range
		if (rangeCheck(intersection, false, false))
			for (Integer common : intersection) {
				newState.addCell(common, current.removeCell(common));
				expandedRange.remove(common);
			}

		// Process remainings with the "remainings' algorithm"
		remainingsAlgorithm(expandedRange, newState);
		return newState;
	}

	/**
	 * Avoids code duplication. Typically used to process indexes not found in the current state. Needed by the
	 * {@link #intersectionAlgorithm()} or by {@link #onItemsChanged()}.
	 * <p>
	 * For any index in the given collection, a cell is needed. Also, it needs to be updated by index and item both.
	 * This cell can come from three sources:
	 * <p> 1) from the current state if it's not empty yet. Since the cells are stored in a {@link SequencedMap}, one
	 * is removed by calling {@link SequencedMap#pollFirstEntry()}.
	 * <p> 2) from the {@link VirtualizedListCache} if not empty
	 * <p> 3) created by the cell factory
	 * <p></p>
	 * <p> - See {@link VirtualizedListHelper#indexToCell(int)}: this handles the second and third cases. If a cell can
	 * be taken from the cache, automatically updates its item then returns it. Otherwise, invokes the
	 * {@link VirtualizedList#cellFactoryProperty()} to create a new one
	 */
	protected void remainingsAlgorithm(Set<Integer> expandedRange, VirtualizedListState<T, C> newState) {
		VirtualizedList<T, C> list = getNode();
		VirtualizedListHelper<T, C> helper = list.getHelper();
		VirtualizedListState<T, C> current = list.getState();

		// Indexes in the given set were not found in the current state.
		// Which means item updates. Cells are retrieved either from the current state (if not empty), from the cache,
		// or created from the factory
		for (Integer index : expandedRange) {
			T item = helper.indexToItem(index);
			C c;
			if (!current.isEmpty()) {
				c = current.getCellsByIndex().pollFirstEntry().getValue();
				c.updateIndex(index);
				c.updateItem(item);
			} else {
				c = helper.itemToCell(item);
				c.updateIndex(index);
				newState.setCellsChanged(true);
			}
			newState.addCell(index, item, c);
		}
	}

	/**
	 * Avoids code duplication. This method checks for three things:
	 * <p> 1) If the list is empty
	 * <p> 2) If the cell factory is null
	 * <p> 3) If the cell size is lesser or equal to 0
	 * <p>
	 * If any of those checks is true: the list's state is set to {@link VirtualizedListState#EMPTY}, the
	 * current state is disposed, the 'invalidatingPos' flag is reset, finally returns false.
	 * Otherwise, does nothing and returns true.
	 * <p></p>
	 * <p> - See {@link VirtualizedList#cellFactoryProperty()}
	 * <p> - See {@link VirtualizedList#cellSizeProperty()}
	 * <p> - See {@link #disposeCurrent()}: for the current state disposal
	 *
	 * @return whether all the aforementioned checks have passed
	 */
	@SuppressWarnings("unchecked")
	protected boolean listFactorySizeCheck() {
		VirtualizedList<T, C> list = getNode();
		if (list.isEmpty() || list.getCellFactory() == null || list.getCellSize() <= 0) {
			disposeCurrent();
			list.update(VirtualizedListState.EMPTY);
			invalidatingPos = false;
			return false;
		}
		return true;
	}

	/**
	 * Avoids code duplication. Used to check whether the given range is valid, not equal to {@link Utils#INVALID_RANGE}.
	 * <p>
	 * When invalid, returns false, but first runs the following operations: disposes the current state (only if the
	 * 'dispose' parameter is true), sets the list's state to {@link VirtualizedListState#EMPTY} (only if the 'update'
	 * parameter is true), resets the 'invalidatingPos' flag.
	 * Otherwise, does nothing and returns true.
	 * <p>
	 * Last but not least, this is a note for the future on why the method is structured like this. It's crucial for
	 * the disposal operation to happen <b>before</b> the list's state is set to {@link VirtualizedListState#EMPTY}, otherwise
	 * the disposal method will fail, since it will then retrieve the empty state instead of the correct one.
	 * <p></p>
	 * <p> - See {@link #disposeCurrent()}: for the current state disposal
	 *
	 * @param range   the range to check
	 * @param update  whether to set the list's state to 'empty' if the range is not valid
	 * @param dispose whether to dispose the current/old state if the range is not valid
	 * @return whether the range is valid or not
	 */
	@SuppressWarnings("unchecked")
	protected boolean rangeCheck(IntegerRange range, boolean update, boolean dispose) {
		VirtualizedList<T, C> list = getNode();
		if (Utils.INVALID_RANGE.equals(range)) {
			if (dispose) disposeCurrent();
			if (update) list.update(VirtualizedListState.EMPTY);
			invalidatingPos = false;
			return false;
		}
		return true;
	}

	/**
	 * Avoids code duplication. Responsible for disposing the current state if it is not empty.
	 * <p></p>
	 * <p> - See {@link VirtualizedListState#dispose()}
	 *
	 * @return whether the disposal was done or not
	 */
	protected boolean disposeCurrent() {
		VirtualizedListState<T, C> state = getNode().getState();
		if (!state.isEmpty()) {
			state.dispose();
			return true;
		}
		return false;
	}
}
