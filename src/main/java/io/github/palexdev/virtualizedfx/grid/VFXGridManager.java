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

package io.github.palexdev.virtualizedfx.grid;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.behavior.MFXBehavior;
import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.list.VFXListManager;
import io.github.palexdev.virtualizedfx.properties.CellFactory;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap.StateMap;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ListProperty;
import javafx.geometry.Orientation;

/// Default behavior implementation for[VFXGrid]. Although, to be precise, and as the name also suggests,
/// this can be considered more like a 'manager' than a behavior. Behaviors typically respond to user input, and then
/// update the component's state. This behavior contains core methods to respond to various properties change
/// in [VFXGrid]. All computations here will generate a new [VFXGridState], if possible, and update the grid
/// and the layout (indirect, call to [VFXGrid#requestViewportLayout()]).
///
/// By default, manages the following changes:
/// - geometry changes (width/height changes), [#onGeometryChanged()]
/// - position changes, [#onPositionChanged(Orientation)]
/// - number of columns changes, [#onColumnsNumChanged()]
/// - cell factory changes, [#onCellFactoryChanged()]
/// - cell size changes, [#onCellSizeChanged()]
/// - spacing changes, [#onSpacingChanged()]
/// - items changes, [#onItemsChanged()]
///
/// Last but not least, some of these computations may need to ensure the current vertical and horizontal positions are correct,
/// so that a valid new state can be produced. To achieve this, [VFXGridHelper#invalidatePos()] is called when necessary.
/// However, invalidating the positions, also means that the [#onPositionChanged(Orientation)] method could be potentially
/// triggered, thus generating an unwanted 'middle' state. For this reason a special flag [#invalidatingPos] is set
/// to `true` before the invalidation, so that the other method will exit immediately. It's reset back to false
/// after the computation or if any of the checks before the actual computation fails.
public class VFXGridManager<T, C extends VFXCell<T>> extends MFXBehavior<VFXGrid<T, C>> {
    //================================================================================
    // Properties
    //================================================================================
    protected boolean invalidatingPos = false;

    //================================================================================
    // Constructors
    //================================================================================
    public VFXGridManager(VFXGrid<T, C> grid) {
        super(grid);
    }

    //================================================================================
    // Methods
    //================================================================================

    /// This core method is responsible for ensuring that the viewport always has the right number of cells.
    /// This is called every time the grid's geometry changes (width/height changes), which means that this is also
    /// responsible for the initialization (when width/height becomes > 0.0).
    ///
    /// After preliminary checks done by [#gridFactorySizeCheck()] and [#rangeCheck(IntegerRange, IntegerRange, boolean, boolean)],
    /// the computation of the new state is delegated to the [#moveReuseCreateAlgorithm(IntegerRange, IntegerRange, VFXGridState)].
    ///
    /// Note that to compute a valid new state, it is important to also validate the grid's positions by invoking
    /// [VFXGridHelper#invalidatePos()].
    protected void onGeometryChanged() {
        invalidatingPos = true;
        VFXGrid<T, C> grid = getNode();
        VFXGridHelper<T, C> helper = grid.getHelper();
        if (!gridFactorySizeCheck()) return;

        // Ensure positions are valid!
        helper.invalidatePos();

        IntegerRange rowsRange = helper.rowsRange();
        IntegerRange columnsRange = helper.columnsRange();
        if (!rangeCheck(rowsRange, columnsRange, true, true)) return;

        // Compute the new state
        VFXGridState<T, C> newState = new VFXGridState<>(grid, rowsRange, columnsRange);
        moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);

        if (disposeCurrent()) newState.setCellsChanged(true);
        grid.update(newState);
        invalidatingPos = false;
    }

    /// This core method is responsible for updating the grid's state when the vertical and horizontal positions change.
    /// Since the grid doesn't use any throttling technique to limit the number of events/changes,
    /// and since scrolling can happen very fast, performance here is crucial.
    ///
    /// Immediately exits if: the special flag [#invalidatingPos] is true or the current state is [VFXGridState#INVALID].
    /// Many other computations here need to validate the positions by calling [VFXGridHelper#invalidatePos()],
    /// to ensure that the resulting state is valid.
    /// However, invalidating the positions may trigger this method, causing two or more state computations to run at the
    /// 'same time'; this behavior must be avoided, and that flag exists specifically for this reason.
    ///
    /// Before further discussing the internal mechanisms of this method, notice that this accepts a parameter of type
    /// [Orientation]. The reason is simple. The grid is virtualized on both the x-axis and y-axis, but changes
    /// are 'atomic', meaning that only one can be processed at a time. For the scroll it's the same.
    /// If you imagine scroll on a timeline, each event occurs after the other. Even if you scroll in both directions at the same time,
    /// there's a difference between what you perceive and what happens under the hood. For this reason, to also
    /// avoid code duplication, that parameter tells this method on which axis the scroll happened, so that we can get the
    /// right ranges and values for the computation: vertical -> rowsRange, horizontal -> columnsRange.
    ///
    /// Also, note that for the same reason, the algorithm differs from the one of [VFXListManager]. When scrolling
    /// in the grid, it is indeed possible to have less or more cells than the current state. That's because rows/columns
    /// can be incomplete. So, the [#moveReuseCreateAlgorithm(IntegerRange, IntegerRange, VFXGridState)] will do
    /// the trick here.
    ///
    /// Before delegating the computation to the aforementioned algorithm though, there are a few prerequisites and checks:
    ///
    /// - the last range (retrieved from the current state)
    ///
    /// - the new range (give by [VFXGridHelper])
    ///
    /// - exits if these two ranges are equal or the new range in invalid ([-1,-1])
    ///
    /// At the end, the current state is disposed by calling [#disposeCurrent()], and if it was not empty we also set
    /// the new state's [VFXGridState#haveCellsChanged()] flag to `true`. Finally, we can call [VFXGrid#update(VFXGridState)]
    /// with the new state and also call [VFXGrid#requestViewportLayout()]. Note that this last call will be done only
    /// if the [VFXGridState#haveCellsChanged()] was not set to `true`. In other words, when the position changes,
    /// we always want to update the layout to ensure that cells are at the correct x and y coordinates.
    protected void onPositionChanged(Orientation axis) {
        if (invalidatingPos) return;
        VFXGrid<T, C> grid = getNode();
        VFXGridState<T, C> state = grid.getState();
        if (state == VFXGridState.INVALID) return;

        VFXGridHelper<T, C> helper = grid.getHelper();
        IntegerRange lastRange = (axis == Orientation.VERTICAL) ?
            state.getRowsRange() :
            state.getColumnsRange();
        IntegerRange range = (axis == Orientation.VERTICAL) ?
            helper.rowsRange() :
            helper.columnsRange();
        if (Objects.equals(lastRange, range) || Utils.INVALID_RANGE.equals(range)) return;

        // Compute the new state
        IntegerRange rowsRange = (axis == Orientation.VERTICAL) ? range : helper.rowsRange();
        IntegerRange columnsRange = (axis == Orientation.VERTICAL) ? helper.columnsRange() : range;
        VFXGridState<T, C> newState = new VFXGridState<>(grid, rowsRange, columnsRange);
        moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);

        // Always request layout update because cells need to be in the right place after scrolling
        if (disposeCurrent()) newState.setCellsChanged(true);
        grid.update(newState);
        if (!newState.haveCellsChanged()) grid.requestViewportLayout();
    }

    /// This core method is responsible for updating the grid's state when the number of columns specified by
    /// [VFXGrid#columnsNumProperty()] changes.
    ///
    /// After preliminary checks done by [#gridFactorySizeCheck()] and [#rangeCheck(IntegerRange, IntegerRange, boolean, boolean)],
    /// the computation for the new state is delegated to the [#moveReuseCreateAlgorithm(IntegerRange, IntegerRange, VFXGridState)].
    ///
    /// At the end, the current state is disposed by calling [#disposeCurrent()], and if it was not empty we also set
    /// the new state's [VFXGridState#haveCellsChanged()] flag to `true`. Finally, we can call [VFXGrid#update(VFXGridState)]
    /// with the new state and also call [VFXGrid#requestViewportLayout()]. Note that this last call will be done only
    /// if the [VFXGridState#haveCellsChanged()] was not set to `true`. In other words, when the position changes,
    /// we always want to update the layout to ensure that cells are at the correct x and y coordinates.
    ///
    /// Note that to compute a valid new state, it is important to also validate the grid's positions by invoking
    /// [VFXGridHelper#invalidatePos()].
    protected void onColumnsNumChanged() {
        invalidatingPos = true;
        VFXGrid<T, C> grid = getNode();
        if (!gridFactorySizeCheck()) return;
        VFXGridHelper<T, C> helper = grid.getHelper();

        // Ensure positions are valid!
        // When the number of columns changes, both the estimated width and height change too
        // As a result of that, it is indeed needed to ensure that the current scroll positions are valid
        helper.invalidatePos();

        // Second check: ensure that both ranges are valid
        IntegerRange rowsRange = helper.rowsRange();
        IntegerRange columnsRange = helper.columnsRange();
        if (!rangeCheck(rowsRange, columnsRange, true, true)) return;

        // Finally proceed with the 'moveReuseCreate()' algorithm
        VFXGridState<T, C> newState = new VFXGridState<>(grid, rowsRange, columnsRange);
        moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);

        // I believe a layout request is always needed as the algorithm may move cells around in the state
        // Which means that they should be repositioned too.
        if (disposeCurrent()) newState.setCellsChanged(true);
        grid.update(newState);
        if (!newState.haveCellsChanged()) grid.requestViewportLayout();
        invalidatingPos = false;
    }

    /// This method is responsible for updating the grid's state when the [VFXGrid#getCellFactory()]
    /// changes. Unfortunately, this is always a costly operation because all cells need to be re-created, and the
    /// [VFXCellsCache] cleaned. In fact, the very first operation done by this method is exactly this,
    /// the disposal of the current state and the cleaning of the cache.
    /// Luckily, this kind of change is likely to not happen very often.
    ///
    /// After preliminary checks done by [#gridFactorySizeCheck()] and [#rangeCheck(IntegerRange, IntegerRange, boolean, boolean)],
    /// the computation for the new state is delegated to the [#moveReuseCreateAlgorithm(IntegerRange, IntegerRange, VFXGridState)].
    ///
    /// The new state's [VFXGridState#haveCellsChanged()] flag will always be `true` of course.
    /// The great thing about the factory change is that there is no need to invalidate the position.
    protected void onCellFactoryChanged() {
        VFXGrid<T, C> grid = getNode();

        // Dispose current state, cells if any (not INVALID) are now in cache
        // Purge cache too, cells are from old factory
        if (disposeCurrent()) grid.getCache().clear();
        if (!gridFactorySizeCheck()) return;

        VFXGridState<T, C> current = grid.getState();
        IntegerRange rowsRange = current.getRowsRange();
        IntegerRange columnsRange = current.getColumnsRange();
        if (!rangeCheck(rowsRange, columnsRange, true, true)) return;

        VFXGridState<T, C> newState = new VFXGridState<>(grid, rowsRange, columnsRange);
        moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);
        newState.setCellsChanged(true);
        grid.update(newState);
    }

    /// This method is responsible for computing a new state when the [VFXGrid#cellSizeProperty()] changes.
    ///
    /// After preliminary checks done by [#gridFactorySizeCheck()], the computation for the new state is delegated to
    /// the [#moveReuseCreateAlgorithm(IntegerRange, IntegerRange, VFXGridState)].
    ///
    /// Note that to compute a valid new state, it is important to also validate the grid's positions by invoking
    /// [VFXGridHelper#invalidatePos()].
    ///
    /// Note that this will request the layout computation, [VFXGrid#requestViewportLayout()], even if the cells
    /// didn't change for obvious reasons.
    protected void onCellSizeChanged() {
        invalidatingPos = true;
        VFXGrid<T, C> grid = getNode();
        VFXGridHelper<T, C> helper = grid.getHelper();

        // Ensure positions are valid!
        helper.invalidatePos();

        if (!gridFactorySizeCheck()) return;

        // Compute new state with the move/reuse/create algorithm
        // We don't use an intersection algorithm here because performance-wise is the same
        IntegerRange rowsRange = helper.rowsRange();
        IntegerRange columnsRange = helper.columnsRange();
        VFXGridState<T, C> newState = new VFXGridState<>(grid, rowsRange, columnsRange);
        moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);
        if (disposeCurrent()) newState.setCellsChanged(true);
        grid.update(newState);
        if (!newState.haveCellsChanged()) grid.requestViewportLayout();
        invalidatingPos = false;
    }

    /// This method is responsible for updating the grid's state when either the [VFXGrid#hSpacingProperty()] or
    /// [VFXGrid#vSpacingProperty()] change.
    ///
    /// After preliminary checks done by [#gridFactorySizeCheck()] and [#rangeCheck(IntegerRange, IntegerRange, boolean, boolean)],
    /// the computation for the new state is delegated to the [#moveReuseCreateAlgorithm(IntegerRange, IntegerRange, VFXGridState)].
    ///
    /// Note that to compute a valid new state, it is important to also validate the grid's positions by invoking
    /// [VFXGridHelper#invalidatePos()]. Also, this will request the layout computation,
    /// [VFXGrid#requestViewportLayout()], even if the cells didn't change for obvious reasons.
    protected void onSpacingChanged() {
        invalidatingPos = true;
        VFXGrid<T, C> grid = getNode();
        if (!gridFactorySizeCheck()) return;
        VFXGridHelper<T, C> helper = grid.getHelper();

        // Ensure positions are valid!
        helper.invalidatePos();

        // Ensure ranges are valid
        IntegerRange rowsRange = helper.rowsRange();
        IntegerRange columnsRange = helper.columnsRange();
        if (!rangeCheck(rowsRange, columnsRange, true, true)) return;

        // Compute new state
        VFXGridState<T, C> newState = new VFXGridState<>(grid, rowsRange, columnsRange);
        moveReuseCreateAlgorithm(rowsRange, columnsRange, newState);

        if (disposeCurrent()) newState.setCellsChanged(true);
        grid.update(newState);
        if (!newState.haveCellsChanged()) grid.requestViewportLayout();
        invalidatingPos = false;
    }

    /// Before describing the operations performed by this method, it's important for the reader to understand the difference
    /// between the two changes caught by this method. [VFXGrid] makes use of a [ListProperty] to store
    /// the items to display. The property is essentially the equivalent of this `ObjectProperty<ObservableList>`. Now,
    /// if you are familiar with JavaFX, you probably know that there are two possible changes to listen to: one is changes
    /// to `ObjectProperty` (if the `ObservableList` instance changes), and the other are changes in the `ObservableList`
    /// instance itself. As you may guess, managing both these changes with a simple `ObjectProperty` is quite cumbersome,
    /// because you need two listeners: one that catches changes in the list, and another to catch changes to the property.
    /// In particular, the latter has the task to add the first listener to the new `ObservableList` instance.
    ///
    /// And here is where [ListProperty] comes in handy. By adding an [InvalidationListener] to this special
    /// property we are able to intercept both the type of changes always, even if the `ObservableList` instance changes,
    /// everything is handled automatically.
    ///
    /// Needless to say, we use a `Property` to store the items to allow the usage of bindings!
    ///
    /// This core method is responsible for updating the grid's state when any of the two aforementioned changes happen.
    ///
    /// These kind of updates are the most tricky and expensive. In particular, additions and removals can occur at any
    /// position in the list, which means that calculating the new state solely on the indexes is a no-go. It is indeed
    /// possible by, in theory, isolating the indexes at which the changes occurred, separating the cells that need only
    /// an index update from the ones that actually need a full update. However, such an approach requires a lot of code,
    /// is error-prone, and a bit heavy on performance. The new approach implemented here requires changes to the state
    /// class as well, [VFXGridState].
    ///
    /// The computation for the new state is similar to the [#moveReuseCreateAlgorithm(IntegerRange, IntegerRange, VFXGridState)],
    /// but the first step, which tries to identify the common cells, is quite different. You see, as I said before, additions
    /// and removals can occur at any place in the list. Making an example for a 2D structure such as the grid would take
    /// a lot of space and effort, so to better understand what can happen, you can check the example I wrote here
    /// [VFXListManager] (go to the same method).
    ///
    /// For this reason, cells from the old state are not removed by index, but by **item**,
    /// [VFXGridState#removeCell(Object)]. First, we retrieve the item from the list from the row and column
    /// coordinates (make sure to read the [VFXGridState] docs to understand how indexes are managed!), then we try
    /// to remove tge cell for that item from the current state. If the cell is found, we update it by index and add it to
    /// the new state. Here's a difference between this method and the one used by [VFXListManager]: the latter expands
    /// the range of items to display to a `Set` and removes every index for which a cell is found in the current state.
    /// This, however, is not possible here, because we have two ranges, so instead we do the opposite. While iterating on
    /// the ranges, if the cell was not found, then we add the index to a `Set`, this set represents the remaining items,
    /// those for which any of these three things may happen: a new cell is created, a cell is polled from the cache and
    /// updated, a cell from the current state is updated and moved to the new state.
    ///
    /// The processing of those remaining elements is delegated to the [#remainingAlgorithm(Set, VFXGridState)].
    ///
    /// Last notes:
    ///
    /// 1) This is one of those methods that to produce a valid new state needs to validate the grid's positions,
    /// so it calls [VFXGridHelper#invalidatePos()]
    ///
    /// 2) Before invalidating the position, this must also request the re-computation of the container's virtual sizes
    /// by calling [VFXGridHelper#invalidateVirtualSizes()]
    ///
    /// 3) To make sure the layout is always correct, at the end we always invoke [VFXGrid#requestViewportLayout()].
    /// After changes like this, items may be still present in the viewport, but at different indexes, which translates to
    /// different layout positions. There is no easy way to detect this, so better safe than sorry, always update the layout.
    protected void onItemsChanged() {
        invalidatingPos = true;
        VFXGrid<T, C> grid = getNode();
        VFXGridHelper<T, C> helper = grid.getHelper();

        /*
         * Force the re-computation of the container's virtual sizes which depend on the number of items.
         * Doing this here is crucial because an automatic invalidation may trigger the onPositionChanged(...) method
         * before this, therefore leading to an incorrect state.
         */
        helper.invalidateVirtualSizes();


        /*
         * Ensure positions are valid!
         * In theory, this is only needed if the list now is smaller than before (or the old one)
         * But since that information is lost (we would have to track it here in some way), we always
         * invalidate positions, after all, it's not a big deal anyway.
         */
        helper.invalidatePos();

        VFXGridState<T, C> current = grid.getState();
        if (!gridFactorySizeCheck()) return;

        // Compute the ranges and new state
        int nColumns = helper.maxColumns();
        IntegerRange rowsRange = helper.rowsRange();
        IntegerRange columnsRange = helper.columnsRange();
        Set<Integer> remaining = new LinkedHashSet<>();
        VFXGridState<T, C> newState = new VFXGridState<>(grid, rowsRange, columnsRange);

        // Index updates
        outer_loop:
        for (Integer rIdx : rowsRange) {
            for (Integer cIdx : columnsRange) {
                int linear = GridUtils.subToInd(nColumns, rIdx, cIdx);
                if (linear >= grid.size()) break outer_loop;
                T item = helper.indexToItem(linear);
                C c = current.removeCell(item);
                if (c != null) {
                    c.updateIndex(linear);
                    newState.addCell(linear, item, c);
                    continue;
                }
                remaining.add(linear);
            }
        }

        // Process remaining with the "remaining' algorithm"
        remainingAlgorithm(remaining, newState);

        if (disposeCurrent()) newState.setCellsChanged(true);
        grid.update(newState);
        if (!newState.haveCellsChanged()) grid.requestViewportLayout();
        invalidatingPos = false;
    }

    //================================================================================
    // Common
    //================================================================================

    /// Avoids code duplication. Typically used when, while iterating on the rows and columns ranges,
    /// it's enough to move the cells from the current state to the new state. For indexes which are not found
    /// in the current state, a new cell is either taken from the old state, taken from cache or created by the cell factory.
    ///
    /// (The last operations are delegated to the [#remainingAlgorithm(Set, VFXGridState)]).
    ///
    /// @see VFXGridHelper#indexToCell(int)
    /// @see VFXGrid#getCellFactory()
    protected void moveReuseCreateAlgorithm(IntegerRange rowsRange, IntegerRange columnsRange, VFXGridState<T, C> newState) {
        VFXGrid<T, C> grid = getNode();
        int nColumns = grid.getHelper().maxColumns();
        VFXGridState<T, C> current = grid.getState();
        Set<Integer> remaining = new LinkedHashSet<>();
        outer_loop:
        for (Integer rIdx : rowsRange) {
            for (Integer cIdx : columnsRange) {
                int linear = GridUtils.subToInd(nColumns, rIdx, cIdx);
                if (linear < 0 || linear >= grid.size()) break outer_loop;
                C c = current.removeCell(linear);
                if (c == null) {
                    remaining.add(linear);
                    continue;
                }
                newState.addCell(linear, c);
            }
        }
        remainingAlgorithm(remaining, newState);
    }

    /// Avoids code duplication. Typically used to process indexes not found in the current state.
    ///
    /// For any index in the given collection, a cell is needed. Also, it needs to be updated by index and item both.
    /// This cell can come from three sources:
    ///
    /// 1) from the current state if it's not empty yet. Since the cells are stored in a [SequencedMap], one
    /// is removed by calling [StateMap#pollFirst()].
    ///
    /// 2) from the [VFXCellsCache] if not empty
    ///
    /// 3) created by the cell factory
    ///
    /// - See [VFXGridHelper#indexToCell(int)]: this handles the second and third cases. If a cell can
    /// be taken from the cache, automatically updates its item then returns it. Otherwise, invokes the
    /// [VFXGrid#getCellFactory()] to create a new one
    protected void remainingAlgorithm(Set<Integer> remaining, VFXGridState<T, C> newState) {
        VFXGrid<T, C> grid = getNode();
        VFXGridHelper<T, C> helper = grid.getHelper();
        VFXGridState<T, C> current = grid.getState();

        // Indexes in the given set were not found in the current state.
        // Which means item updates. Cells are retrieved either from the current state (if not empty), from the cache,
        // or created from the factory
        for (Integer index : remaining) {
            // We don't check the index here since we expect that the 'remaining' Set already contains valid indexes
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

    /// Avoids code duplication. This method checks for three things:
    ///
    /// 1) If the list is empty
    ///
    /// 2) If the cell factory is `null`
    ///
    /// 3) If the cell size is lesser or equal to 0
    ///
    /// If any of those checks is true: the grid's state is set to [VFXGridState#INVALID], the
    /// current state is disposed, the 'invalidatingPos' flag is reset, finally returns false.
    ///
    /// Otherwise, does nothing and returns true.
    ///
    /// - See [CellFactory#canCreate()]
    ///
    /// - See [VFXGrid#cellSizeProperty()]
    ///
    /// - See [#disposeCurrent()]: for the current state disposal
    ///
    /// @return whether all the aforementioned checks have passed
    @SuppressWarnings("unchecked")
    protected boolean gridFactorySizeCheck() {
        VFXGrid<T, C> grid = getNode();
        if (grid.isEmpty() || !grid.getCellFactory().canCreate() ||
            grid.getCellSize().width() <= 0 || grid.getCellSize().height() <= 0) {
            disposeCurrent();
            grid.update(VFXGridState.INVALID);
            invalidatingPos = false;
            return false;
        }
        return true;
    }

    /// Avoids code duplication. Used to check whether the given ranges are valid, not equal to [Utils#INVALID_RANGE].
    ///
    /// When invalid, returns false, but first runs the following operations: disposes the current state (only if the
    /// 'dispose' parameter is true), sets the grid's state to [VFXGridState#INVALID] (only if the 'update'
    /// parameter is true), resets the 'invalidatingPos' flag.
    /// Otherwise, does nothing and returns true.
    ///
    /// Last but not least, this is a note for the future on why the method is structured like this. It's crucial for
    /// the disposal operation to happen **before** the list's state is set to [VFXGridState#INVALID], otherwise
    /// the disposal method will fail, since it will then retrieve the empty state instead of the correct one.
    ///
    /// - See [#disposeCurrent()]: for the current state disposal
    ///
    /// @param rowsRange the rows range to check
    /// @param columnsRange the columns range to check
    /// @param update whether to set the grid's state to 'empty' if the range is not valid
    /// @param dispose whether to dispose the current/old state if the range is not valid
    /// @return whether the range is valid or not
    @SuppressWarnings("unchecked")
    protected boolean rangeCheck(IntegerRange rowsRange, IntegerRange columnsRange, boolean update, boolean dispose) {
        VFXGrid<T, C> grid = getNode();
        if (Utils.INVALID_RANGE.equals(rowsRange) || Utils.INVALID_RANGE.equals(columnsRange)) {
            if (dispose) disposeCurrent();
            if (update) grid.update(VFXGridState.INVALID);
            invalidatingPos = false;
            return false;
        }
        return true;
    }

    /// Avoids code duplication. Responsible for disposing the current state if it is not empty.
    ///
    /// - See [VFXGridState#dispose()]
    ///
    /// @return whether the disposal was done or not
    protected boolean disposeCurrent() {
        VFXGridState<T, C> state = getNode().getState();
        if (!state.isEmpty()) {
            state.dispose();
            return true;
        }
        return false;
    }
}
