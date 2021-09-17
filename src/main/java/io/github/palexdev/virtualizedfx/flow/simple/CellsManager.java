/*
 * Copyright (C) 2021 Parisi Alessandro
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

package io.github.palexdev.virtualizedfx.flow.simple;

import io.github.palexdev.virtualizedfx.beans.NumberRange;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.flow.base.OrientationHelper;
import io.github.palexdev.virtualizedfx.utils.NumberUtils;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// TODO memoize cell factory anyway?

/**
 * Helper class to properly keep track of the needed cells, add them
 * to the container, update them on scroll, and update the cells layout.
 * <p></p>
 * The 'system' is quite simple yet super efficient: it keeps a List called cellsPool that contains
 * a number of cells (this number is the maximum number of cells that can be shown into the viewport).
 * During initialization the cellPool is populated by {@link #initCells(int)} and then added to the container,
 * they are also updated to be sure they are showing the right content. On scroll {@link #updateCells(int, int)}
 * is called, this method processes the needed updates to do ({@link CellUpdate}), then updates the cells, the layout
 * by calling {@link #processLayout(List)} and finally updates the indexes range of shown items.
 * <p></p>
 * The important part to understand is that rather than creating new cells everytime the flow scrolls
 * we update the already created cells so the scroll is very smooth, and it's also efficient
 * in terms of memory usage. This allows the flow to handle huge amounts of items (depending on cells' complexity of course).
 *
 * @param <T> the type of object to represent
 * @param <C> the type of Cell to use
 */
public class CellsManager<T, C extends Cell<T>> {
    //================================================================================
    // Properties
    //================================================================================
    private final SimpleVirtualFlow<T, C> virtualFlow;
    private final SimpleVirtualFlowContainer<T, C> container;
    private final List<C> cellsPool = new ArrayList<>();
    private final List<CellUpdate> updates = new ArrayList<>();
    private NumberRange<Integer> lastRange = NumberRange.of(-1);
    private boolean listChanged;

    //================================================================================
    // Constructors
    //================================================================================
    public CellsManager(SimpleVirtualFlowContainer<T, C> container) {
        this.virtualFlow = container.getVirtualFlow();
        this.container = container;
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Populates the cellsPool, adds them to the container and then calls {@link #updateCells(int, int)}
     * (indexes from 0 to num) to ensure the cells are displaying the right content (should not be needed though)
     *
     * @param num the number of cells to create
     */
    protected void initCells(int num) {
        int diff = num - cellsPool.size();
        for (int i = 0; i <= diff && i < itemsSize(); i++) {
            cellsPool.add(cellForIndex(i));
        }

        // Add the cells to the container
        container.getChildren().setAll(cellsPool.stream().map(C::getNode).collect(Collectors.toList()));

        // Ensure that cells are properly updated
        updateCells(0, num);
    }

    /**
     * Responsible for updating the cells in the viewport.
     * <p></p>
     * If the items list is empty returns immediately.
     * <p>
     * If the list was cleared, so start and end are invalid, computes the maximum
     * number of cells the viewport can show and calls {@link #initCells(int)}, then exits
     * as that method will then call this with valid indexes.
     * <p>
     * If the new indexes range (start-end) is equal to the previous stored range
     * it's not necessary to update the cells therefore exits. This check is ignored
     * if the items list was changed (items replaced, added or removed), int this case
     * it's needed to update.
     * <p></p>
     * The next check is to verify there are enough cells to show all the items from
     * 'start' to 'end', the number of cells to show is computed in "optimal" conditions,
     * but at some point it's possible for example that the first visible cell is only shown
     * partially, this means that the last visible cell should be a new cell that the cellsPool
     * can't offer, in cases like this it's needed to create new cells by calling {@link #supplyCells(int, int)}
     * <p>
     * A similar check is made if there are too many cells, in this case cells are removed.
     * <p></p>
     * The next step is a bit tricky. The cellsPool indexes go from 0 to size() of course,
     * but the start and end parameters do not start from 0, let's say I have to show items
     * from 10 to 20, so it's needed to use an "external" counter to get the cells from 0 to size(),
     * while the items are retrieved from the list from 'start' to 'end', in this loop we create
     * {@link CellUpdate} beans to make the code cleaner, after the loop {@link CellUpdate#update()} is
     * called for each bean, then the layout is updated with {@link #processLayout(List)} and
     * finally the range of shown items is updated.
     *
     * @param start the start index from which retrieve the items from the items list
     * @param end   the final index up to which retrieve items from the items list
     */
    protected void updateCells(int start, int end) {
        // If the items list is empty return immediately
        if (itemsEmpty()) return;

        // If the list was cleared (so start and end are invalid) cells must be rebuilt
        // by calling initCells(numOfCells), then return since that method will re-call this one
        // with valid indexes.
        if (start == -1 || end == -1) {
            int num = container.getLayoutManager().lastVisible();
            initCells(num);
            return;
        }

        // If range not changed or empty items return
        NumberRange<Integer> newRange = NumberRange.of(start, end);
        if (lastRange.equals(newRange) && !listChanged) return;

        // If there are not enough cells build them and add to the container
        Set<Integer> itemsIndexes = NumberRange.expandRangeToSet(newRange);
        if (itemsIndexes.size() > cellsPool.size()) {
            supplyCells(cellsPool.size(), itemsIndexes.size());
        } else if (itemsIndexes.size() < cellsPool.size()) {
            int overFlow = cellsPool.size() - itemsIndexes.size();
            for (int i = 0; i < overFlow; i++) {
                cellsPool.remove(0);
                container.getChildren().remove(0);
            }
        }

        // Items index can go from 0 to size() of items list,
        // cells can go from 0 to size() of the cells pool,
        // Use a counter to get the cells and itemIndex to
        // get the correct item and index to call
        // updateIndex() and updateItem() later
        updates.clear();
        int poolIndex = 0;
        for (Integer itemIndex : itemsIndexes) {
            T item = virtualFlow.getItems().get(itemIndex);
            CellUpdate update = new CellUpdate(item, cellsPool.get(poolIndex), itemIndex);
            updates.add(update);
            poolIndex++;
        }

        // Finally, update the cells, the layout and the range of items processed
        updates.forEach(CellUpdate::update);
        processLayout(updates);
        lastRange = newRange;
    }

    /**
     * Called when the items list changes, if the list
     * has been cleared removes all the cells from the container
     * then calls {@link #clear()} and then exits.
     * <p>
     * Otherwise calls {@link #updateCells(int, int)}, the indexes
     * used are the ones stored by the CellsManager.
     */
    public void itemsChanged() {
        if (itemsEmpty()) {
            container.getChildren().clear();
            clear();
            return;
        }

        // Cells were previously cleared, re-initialize
        if (lastRange.getMin() == -1 || lastRange.getMax() == -1) {
            int num = container.getLayoutManager().lastVisible();
            initCells(num);
            return;
        }

        // If there are not enough cells build them and add to the container
        int num = virtualFlow.getOrientationHelper().lastVisible() + 1;
        if (cellsPool.size() < itemsSize() && cellsPool.size() < num) {
            supplyCells(cellsPool.size(), itemsSize());
        }

        listChanged = true;
        int start = NumberUtils.clamp(lastRange.getMin(), 0, itemsSize() - 1);
        int end = (lastRange.getMax() < num) ? num - 1 : NumberUtils.clamp(lastRange.getMax(), 0, itemsSize() - 1);
        updateCells(start, end);
        listChanged = false;
    }

    /**
     * Responsible for laying out the cells from a list of {@link CellUpdate}s.
     * <p></p>
     * Since the layoutX/layoutY properties of the container are updated according to the ScrollBars'
     * value, the cells are positioned according to the item's index in the items list.
     * <p></p>
     * To avoid if/else statements as much as possible the actual layout is computed by
     * {@link OrientationHelper#layout(Node, int, double, double)}.
     */
    protected void processLayout(List<CellUpdate> updates) {
        double cellW = container.getCellWidth();
        double cellH = container.getCellHeight();
        for (CellUpdate update : updates) {
            int index = update.index;
            C cell = update.cell;
            Node node = cell.getNode();
            cell.beforeLayout();
            virtualFlow.getOrientationHelper().layout(node, index, cellW, cellH);
            cell.afterLayout();
        }
    }

    /**
     * Resets the CellsManager by clearing the cellsPool, clearing the updates list and
     * resetting the stored indexes range to [-1, -1].
     */
    protected void clear() {
        cellsPool.clear();
        updates.clear();
        lastRange = NumberRange.of(-1);
    }

    /**
     * Creates new cells from the given 'start' index to the given 'end' index,
     * the new cells are added to the cellsPool and to the container.
     */
    protected void supplyCells(int start, int end) {
        for (int i = start; i < end; i++) {
            C cell = cellForIndex(i);
            cellsPool.add(cell);
            container.getChildren().add(cell.getNode());
        }
    }

    /**
     * Exchanges an index for a Cell.
     * <p>
     * Gets the item at the given index in the items list
     * then calls {@link #cellForItem(Object)}.
     */
    protected C cellForIndex(int index) {
        T item = virtualFlow.getItems().get(index);
        return cellForItem(item);
    }

    /**
     * Exchanges an item for a Cell.
     * <p>
     * Simply applies the VirtualFlow's cell factory to the given item.
     */
    protected C cellForItem(T item) {
        return virtualFlow.getCellFactory().apply(item);
    }

    /**
     * Convenience method to retrieve the items list size.
     */
    private int itemsSize() {
        return virtualFlow.getItems().size();
    }

    /**
     * Convenience method to check if the items list is empty.
     */
    private boolean itemsEmpty() {
        return virtualFlow.getItems().isEmpty();
    }

    //================================================================================
    // Getters
    //================================================================================

    /**
     * @return a map of the currently shown cells by the item's index
     */
    protected Map<Integer, C> getCells() {
        return updates.stream().collect(Collectors.toMap(
                u -> u.index,
                u -> u.cell
        ));
    }

    //================================================================================
    // Internal Classes
    //================================================================================

    /**
     * Simple bean to contain a Cell, an item and the item's index.
     * Responsible for calling {@link Cell#updateIndex(int)} and {@link Cell#updateItem(Object)}.
     */
    private class CellUpdate {
        private final T item;
        private final C cell;
        private final int index;

        public CellUpdate(T item, C cell, int index) {
            this.item = item;
            this.cell = cell;
            this.index = index;
        }

        public void update() {
            cell.updateIndex(index);
            cell.updateItem(item);
        }
    }
}
