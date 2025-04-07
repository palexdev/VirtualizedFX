/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.list;

import io.github.palexdev.mfxcore.base.beans.range.ExcludingIntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.behavior.BehaviorBase;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.properties.CellFactory;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap.StateMap;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import java.util.*;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ListProperty;

/**
 * Default behavior implementation for {@link VFXList}. Although, to be precise, and as the name also suggests,
 * this can be considered more like a 'manager' than a behavior. Behaviors typically respond to user input, and then update
 * the component's state. This behavior contains core methods to respond to various properties change in {@link VFXList}.
 * All computations here will generate a new {@link VFXListState}, if possible, and update the list and the
 * layout (indirectly, call to {@link VFXList#requestViewportLayout()}).
 * <p>
 * By default, manages the following changes:
 * <p> - geometry changes (width/height changes), {@link #onGeometryChanged()}
 * <p> - position changes, {@link #onPositionChanged()}
 * <p> - cell factory changes, {@link #onCellFactoryChanged()}
 * <p> - items change, {@link #onItemsChanged()}
 * <p> - fit to viewport flag changes, {@link #onFitToViewportChanged()}
 * <p> - cell size changes, {@link #onCellSizeChanged()}
 * <p> - orientation changes, {@link #onOrientationChanged()}
 * <p> - spacing changes {@link #onSpacingChanged()}
 * <p></p>
 * Last but not least, some of these computations may need to ensure the current vertical and horizontal positions are correct,
 * so that a valid state can be produced. To achieve this, {@link VFXListHelper#invalidatePos()} is called.
 * However, invalidating the positions, also means that the {@link #onPositionChanged()} method could be potentially
 * triggered, thus generating an unwanted 'middle' state. For this reason a special flag {@link #invalidatingPos} is set
 * to {@code true} before the invalidation, so that the other method will exit immediately. It's reset back to false
 * after the computation or if any of the checks before the actual computation fails.
 */
public class VFXListManager<T, C extends VFXCell<T>> extends BehaviorBase<VFXList<T, C>> {
    //================================================================================
    // Properties
    //================================================================================
    protected boolean invalidatingPos = false;

    //================================================================================
    // Constructors
    //================================================================================
    public VFXListManager(VFXList<T, C> list) {
        super(list);
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * This core method is responsible for ensuring that the viewport always has the right number of cells.
     * This is called every time the list's geometry changes (width/height depending on the orientation),
     * which means that this is also responsible for initialization (when width/height becomes > 0.0).
     * <p>
     * After preliminary checks done by {@link #listFactorySizeCheck()} and {@link #rangeCheck(IntegerRange, boolean, boolean)},
     * the computation for the new state is delegated to the {@link #moveReuseCreateAlgorithm(IntegerRange, VFXListState)}.
     * <p></p>
     * Note that to compute a valid new state, it is important to also validate the list's positions by invoking
     * {@link VFXListHelper#invalidatePos()}.
     */
    protected void onGeometryChanged() {
        invalidatingPos = true;
        VFXList<T, C> list = getNode();
        VFXListHelper<T, C> helper = list.getHelper();
        if (!listFactorySizeCheck()) return;

        // Ensure positions are correct!
        helper.invalidatePos();

        // If for whatever reason, the computed range is invalid, then set the state to INVALID
        IntegerRange range = helper.range();
        if (!rangeCheck(range, true, true)) return;

        // Compute the new state
        VFXListState<T, C> newState = new VFXListState<>(list, range);
        moveReuseCreateAlgorithm(range, newState);

        if (disposeCurrent()) newState.setCellsChanged(true);
        list.update(newState);
        invalidatingPos = false;
    }

    /**
     * This core method is responsible for updating the list's state when the 'main' position changes (vPos for VERTICAL
     * orientation, hPos for HORIZONTAL orientation). Since the list doesn't use any throttling technique to limit the number of events/changes,
     * and since scrolling can happen very fast, performance here is crucial.
     * <p>
     * Immediately exists if: the special flag {@link #invalidatingPos} is true or the current state is {@link VFXListState#INVALID}.
     * Many other computations here need to validate the positions by calling {@link VFXListHelper#invalidatePos()}, so that
     * the resulting state is valid.
     * However, invalidating the positions may trigger this method, causing two or more state computations to run at the
     * 'same time'; this behavior must be avoided, and that flag exists specifically for this reason.
     * <p></p>
     * For the sake of performance, this method tries to update only the cells which need it. The computation is divided
     * in two steps:
     * <p> 0) Prerequisites: the last range (retrieved from the current state), the new range (given by {@link VFXListHelper#range()}).
     * Note that if these two ranges are equal or the new range in invalid ([-1, -1]), or the number of elements differ,
     * the method exits. The latter condition essentially means that the change is not a position change but something else,
     * if you think about it, when scrolling the number of items cannot change in any way, if it does, then it's surely
     * something else.
     * <p> 1) First of all, we check for common indexes. Cells are removed from the old state and copied to the new one
     * without updating, since it's not needed. For cells that are not found in the old state (not in common), the index
     * is added to a queue
     * <p> 2) Now we assume that the number of indexes in the queue and the number of cells remaining in the old state
     * are equal. This is because a position change is not a geometry change, the number cannot change by just scrolling.
     * Remaining cells are removed one by one from the old state, an index is also removed from the queue, then the cell
     * is updated both in index and item. Finally, it is added to the new state, with the index removed from the queue.
     * <p></p>
     * The list's state is updated, {@link VFXList#update(VFXListState)}, but most importantly this also
     * calls {@link VFXList#requestViewportLayout()}. The reason for this 'forced' layout is that the remaining
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
        VFXList<T, C> list = getNode();
        VFXListState<T, C> state = list.getState();
        if (state == VFXListState.INVALID) return;

        VFXListHelper<T, C> helper = list.getHelper();
        IntegerRange lastRange = state.getRange();
        IntegerRange range = helper.range();
        if (Objects.equals(lastRange, range) ||
            Utils.INVALID_RANGE.equals(range) ||
            !lastRange.diff().equals(range.diff())) return;
        // Why this last check?
        // When the container changes its position, there will be updates, but the number of cells present in the viewport
        // will not change. In fact, such changes occur only when the container's geometry changes, or its list is modified, etc...
        // If the ranges differ in size, then what caused this method to trigger was probably not a position change.

        // Compute the new state
        // Commons are just moved to the new state
        // New indexes are stored in a queue
        VFXListState<T, C> newState = new VFXListState<>(list, range);
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
     * This method is responsible for updating the list's state when the {@link VFXList#getCellFactory()} changes.
     * Unfortunately, this is always a costly operation because all cells need to be re-created, and the
     * {@link VFXCellsCache} cleaned. In fact, the very first operation done by this method is exactly this,
     * the disposal of the current/old state and the cleaning of the cache.
     * Luckily, this kind of change is likely to not happen very often.
     * <p>
     * After preliminary checks done by {@link #listFactorySizeCheck()} and {@link #rangeCheck(IntegerRange, boolean, boolean)},
     * the computation for the new state is delegated to the {@link #moveReuseCreateAlgorithm(IntegerRange, VFXListState)}.
     * <p>
     * The new state's {@link VFXListState#haveCellsChanged()} flag will always be {@code true} of course.
     * The great thing about the factory change is that there is no need to invalidate the position.
     */
    protected void onCellFactoryChanged() {
        VFXList<T, C> list = getNode();

        // Dispose current state, cells if any (not INVALID) are now in cache
        // Purge cache too, cells are from old factory
        if (disposeCurrent()) list.getCache().clear();
        if (!listFactorySizeCheck()) return;

        VFXListState<T, C> current = list.getState();
        IntegerRange range = current.getRange();
        if (!rangeCheck(range, true, true)) return;

        VFXListState<T, C> newState = new VFXListState<>(list, range);
        moveReuseCreateAlgorithm(range, newState);
        newState.setCellsChanged(true);
        list.update(newState);
    }

    /**
     * Before describing the operations performed by this method, it's important for the reader to understand the difference
     * between the two changes caught by this method. {@link VFXList} makes use of a {@link ListProperty} to store
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
     * class as well, {@link VFXListState}.
     * <p>
     * The computation for the new state is similar to the {@link #moveReuseCreateAlgorithm(IntegerRange, VFXListState)},
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
     * {@link VFXListState#removeCell(Object)}. First, we retrieve the item from the list that is now at index i
     * (this index comes from the loop on the range), then we try to remove the cell for this item from the old state.
     * If the cell is found, we update it by index and add it to the new state. Note that the index is also excluded from the range.
     * <p>
     * Now that 'common' cells have been properly updated, the remaining items are processed by the
     * {@link #remainingAlgorithm(ExcludingIntegerRange, VFXListState)}.
     * <p></p>
     * Last notes:
     * <p> 1) This is one of those methods that to produce a valid new state needs to validate the list's positions,
     * so it calls {@link VFXListHelper#invalidatePos()}
     * <p> 2) Before invalidating the position, this must also request the re-computation of the container's virtual sizes
     * by calling {@link VFXListHelper#invalidateVirtualSizes()}
     * <p> 3) To make sure the layout is always correct, at the end we always invoke {@link VFXList#requestViewportLayout()}.
     * You can guess why from the above example, items 2 and 3 are still in the viewport, but at different indexes,
     * which also means at different layout positions. There is no easy way to detect this, so better safe than sorry,
     * always update the layout.
     */
    protected void onItemsChanged() {
        invalidatingPos = true;
        VFXList<T, C> list = getNode();
        VFXListHelper<T, C> helper = list.getHelper();

        /*
         * Force the re-computation of the container's virtual sizes which depend on the number of items.
         * Doing this here is crucial because an automatic invalidation may trigger the onPositionChanged(...) method
         * before this, therefore leading to an incorrect state.
         */
        helper.invalidateVirtualSizes();

        /*
         * Ensure positions are correct
         * In theory this is only needed if the list now is smaller than before (or the old one)
         * But since that information is lost (we would have to track it here in some way), we always
         * invalidate positions, after all, it's not a big deal anyway.
         */
        helper.invalidatePos();

        // If the list is now empty, then set INVALID state
        VFXListState<T, C> current = list.getState();
        if (!listFactorySizeCheck()) return;

        // Compute range and new state
        IntegerRange range = helper.range();
        ExcludingIntegerRange eRange = ExcludingIntegerRange.of(range);
        VFXListState<T, C> newState = new VFXListState<>(list, range);

        // First update by index
        for (Integer index : range) {
            T item = helper.indexToItem(index);
            C c = current.removeCell(item);
            if (c != null) {
                eRange.exclude(index);
                c.updateIndex(index);
                newState.addCell(index, item, c);
            }
        }

        // Process remaining with the "remaining' algorithm"
        remainingAlgorithm(eRange, newState);

        if (disposeCurrent()) newState.setCellsChanged(true);
        list.update(newState);
        if (!newState.haveCellsChanged()) list.requestViewportLayout();
        invalidatingPos = false;
    }

    /**
     * The easiest of all changes. It's enough to request a viewport layout, {@link VFXList#requestViewportLayout()},
     * and to make sure that the horizontal position is valid, {@link VFXListHelper#invalidatePos()}.
     */
    protected void onFitToViewportChanged() {
        VFXList<T, C> list = getNode();
        VFXListHelper<T, C> helper = list.getHelper();
        list.requestViewportLayout();
        helper.invalidatePos(); // Not necessary to set invalidatingPos flag
    }

    /**
     * This method is responsible for computing a new state when the {@link VFXList#cellSizeProperty()} changes.
     * <p>
     * After preliminary checks done by {@link #listFactorySizeCheck()}, the computation for the new state is delegated to
     * the {@link #intersectionAlgorithm()}.
     * <p></p>
     * Note that to compute a valid new state, it is important to also validate the list's positions by invoking
     * {@link VFXListHelper#invalidatePos()}.
     * <p></p>
     * Note that this will request the layout computation, {@link VFXList#requestViewportLayout()}, even if the cells
     * didn't change for obvious reasons.
     */
    protected void onCellSizeChanged() {
        invalidatingPos = true;
        VFXList<T, C> list = getNode();
        VFXListHelper<T, C> helper = list.getHelper();

        // Ensure positions are correct
        helper.invalidatePos();

        if (!listFactorySizeCheck()) return;

        // Compute new state with the intersection algorithm
        VFXListState<T, C> newState = intersectionAlgorithm();

        if (disposeCurrent()) newState.setCellsChanged(true);
        list.update(newState);
        if (!newState.haveCellsChanged()) list.requestViewportLayout();
        invalidatingPos = false;
    }

    /**
     * This method is responsible for computing a new state when the {@link VFXList#orientationProperty()} changes.
     * <p>
     * After preliminary checks done by {@link #listFactorySizeCheck()}, the computation for the new state is delegated to
     * the {@link #intersectionAlgorithm()}.
     * <p></p>
     * Note that the default behavior resets both the positions to 0.0, as maintaining them doesn't make too much sense.
     * Note that to compute a valid new state, it is important to also validate the list's positions by invoking
     * This will also request the layout computation, {@link VFXList#requestViewportLayout()}, even if the cells didn't change.
     */
    protected void onOrientationChanged() {
        invalidatingPos = true;
        VFXList<T, C> list = getNode();
        if (!listFactorySizeCheck()) return;

        // When the orientation changes, it's a better behavior to just reset the positions
        list.setVPos(0.0);
        list.setHPos(0.0);

        // Compute new state with the intersection algorithm
        VFXListState<T, C> newState = intersectionAlgorithm();

        if (disposeCurrent()) newState.setCellsChanged(true);
        list.update(newState);
        if (!newState.haveCellsChanged()) list.requestViewportLayout();
        invalidatingPos = false;
    }

    /**
     * This method is responsible for updating the list's state when the {@link VFXList#spacingProperty()} changes.
     * <p>
     * After preliminary checks done by {@link #listFactorySizeCheck()} and {@link #rangeCheck(IntegerRange, boolean, boolean)},
     * the computation for the new state is delegated to the {@link #moveReuseCreateAlgorithm(IntegerRange, VFXListState)}.
     * <p></p>
     * Note that to compute a valid new state, it is important to also validate the list's positions by invoking
     * {@link VFXListHelper#invalidatePos()}. Also, this will request the layout computation,
     * {@link VFXList#requestViewportLayout()}, even if the cells didn't change.
     */
    protected void onSpacingChanged() {
        invalidatingPos = true;
        VFXList<T, C> list = getNode();
        VFXListHelper<T, C> helper = list.getHelper();

        // Ensure positions are correct
        helper.invalidatePos();

        // If range is invalid
        IntegerRange range = helper.range();
        if (!rangeCheck(range, true, true)) return;

        // Compute new state
        VFXListState<T, C> newState = new VFXListState<>(list, range);
        moveReuseCreateAlgorithm(range, newState);

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
     * in the current state, a new cell is either taken from the old state, taken from cache or created by the cell factory.
     * <p>
     * (The last operations are delegated to the {@link #remainingAlgorithm(ExcludingIntegerRange, VFXListState)}).
     *
     * @see VFXListHelper#indexToCell(int)
     * @see VFXList#getCellFactory()
     */
    protected void moveReuseCreateAlgorithm(IntegerRange range, VFXListState<T, C> newState) {
        VFXList<T, C> list = getNode();
        VFXListState<T, C> current = list.getState();
        ExcludingIntegerRange eRange = ExcludingIntegerRange.of(range);
        if (!current.isEmpty()) {
            for (Integer index : range) {
                C c = current.removeCell(index);
                if (c == null) continue;
                eRange.exclude(index);
                newState.addCell(index, c);
            }
        }
        remainingAlgorithm(eRange, newState);
    }

    /**
     * Avoids code duplication. Typically used in situations where the previous range and the new one are likely to be
     * very close, but most importantly, that do not involve any change in the items' list.
     * In such cases, the computation for the new state is divided in two parts:
     * <p> 0) Prerequisites: the new range [min, max], the excluding range (a helper class to keep track of common cells),
     * the current state, and the intersection between the current state's range and the new range
     * <p> 1) The intersection allows us to distinguish between cells that can be moved as they are, without any update,
     * from the current state to the new one. For this, it's enough to check that the intersection range is valid, and then
     * a for loop. Common indexes are also excluded from the range!
     * <p> 2) The remaining indexes are items that are new. Which means that if there are still cells
     * in the current state, they need to be updated (both index and item). Otherwise, new ones are created by the cell factory.
     * <p></p>
     * <p> - See {@link Utils#intersection}: used to find the intersection between two ranges
     * <p> - See {@link #rangeCheck(IntegerRange, boolean, boolean)}: used to validate the intersection range, both parameters
     * are false!
     * <p> - See {@link #remainingAlgorithm(ExcludingIntegerRange, VFXListState)}: the second part of the algorithm is delegated to this
     * method
     *
     * @see ExcludingIntegerRange
     */
    protected VFXListState<T, C> intersectionAlgorithm() {
        VFXList<T, C> list = getNode();
        VFXListHelper<T, C> helper = list.getHelper();

        // New range
        IntegerRange range = helper.range();
        ExcludingIntegerRange eRange = ExcludingIntegerRange.of(range);

        // Current and new states, intersection between current and new range
        VFXListState<T, C> current = list.getState();
        VFXListState<T, C> newState = new VFXListState<>(list, range);
        IntegerRange intersection = Utils.intersection(current.getRange(), range);

        // If range valid, move common cells from current to new state. Also, exclude common indexes
        if (rangeCheck(intersection, false, false))
            for (Integer common : intersection) {
                newState.addCell(common, current.removeCell(common));
                eRange.exclude(common);
            }

        // Process remaining with the "remaining' algorithm"
        remainingAlgorithm(eRange, newState);
        return newState;
    }

    /**
     * Avoids code duplication. Typically used to process indexes not found in the current state.
     * <p>
     * For any index in the given {@link ExcludingIntegerRange}, a cell is needed. Also, it needs to be updated by index and item both.
     * This cell can come from three sources:
     * <p> 1) from the current state if it's not empty yet. Since the cells are stored in a {@link SequencedMap}, one
     * is removed by calling {@link StateMap#pollFirst()}.
     * <p> 2) from the {@link VFXCellsCache} if not empty
     * <p> 3) created by the cell factory
     * <p></p>
     * <p> - See {@link VFXListHelper#indexToCell(int)}: this handles the second and third cases. If a cell can
     * be taken from the cache, automatically updates its item then returns it. Otherwise, invokes the
     * {@link VFXList#getCellFactory()} to create a new one
     */
    protected void remainingAlgorithm(ExcludingIntegerRange eRange, VFXListState<T, C> newState) {
        VFXList<T, C> list = getNode();
        VFXListHelper<T, C> helper = list.getHelper();
        VFXListState<T, C> current = list.getState();

        // Indexes in the given set were not found in the current state.
        // Which means item updates. Cells are retrieved either from the current state (if not empty), from the cache,
        // or created from the factory
        for (Integer index : eRange) {
            T item = helper.indexToItem(index);
            C c;
            if (!current.isEmpty()) {
                c = current.getCells().pollFirst().getValue();
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
     * <p> 2) If the cell factory is {@code null}
     * <p> 3) If the cell size is lesser or equal to 0
     * <p>
     * If any of those checks is true: the list's state is set to {@link VFXListState#INVALID}, the
     * current state is disposed, the 'invalidatingPos' flag is reset, finally returns false.
     * Otherwise, does nothing and returns true.
     * <p></p>
     * <p> - See {@link CellFactory#canCreate()}
     * <p> - See {@link VFXList#cellSizeProperty()}
     * <p> - See {@link #disposeCurrent()}: for the current state disposal
     *
     * @return whether all the aforementioned checks have passed
     */
    @SuppressWarnings("unchecked")
    protected boolean listFactorySizeCheck() {
        VFXList<T, C> list = getNode();
        if (list.isEmpty() || !list.getCellFactory().canCreate() || list.getCellSize() <= 0) {
            disposeCurrent();
            list.update(VFXListState.INVALID);
            invalidatingPos = false;
            return false;
        }
        return true;
    }

    /**
     * Avoids code duplication. Used to check whether the given range is valid, not equal to {@link Utils#INVALID_RANGE}.
     * <p>
     * When invalid, returns false, but first runs the following operations: disposes the current state (only if the
     * 'dispose' parameter is true), sets the list's state to {@link VFXListState#INVALID} (only if the 'update'
     * parameter is true), resets the 'invalidatingPos' flag.
     * Otherwise, does nothing and returns true.
     * <p>
     * Last but not least, this is a note for the future on why the method is structured like this. It's crucial for
     * the disposal operation to happen <b>before</b> the list's state is set to {@link VFXListState#INVALID}, otherwise
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
        VFXList<T, C> list = getNode();
        if (Utils.INVALID_RANGE.equals(range)) {
            if (dispose) disposeCurrent();
            if (update) list.update(VFXListState.INVALID);
            invalidatingPos = false;
            return false;
        }
        return true;
    }

    /**
     * Avoids code duplication. Responsible for disposing the current state if it is not empty.
     * <p></p>
     * <p> - See {@link VFXListState#dispose()}
     *
     * @return whether the disposal was done or not
     */
    protected boolean disposeCurrent() {
        VFXListState<T, C> state = getNode().getState();
        if (!state.isEmpty()) {
            state.dispose();
            return true;
        }
        return false;
    }
}
