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
import java.util.stream.Collectors;

// TODO memoize cell factory anyway?

/**
 * Helper class to properly keep track of the needed cells, add them
 * to the container, update them on scroll, and update the cells layout.
 * <p></p>
 * The 'system' is quite simple yet super efficient: it keeps a List called cellsPool that contains
 * a number of cells (this number is the maximum number of cells that can be shown into the viewport).
 * During initialization the cellPool is populated by {@link #initCells(int)} and then added to the container,
 * they are also updated to be sure they are showing the right content. On scroll {@link #updateCells(int)}
 * is called, this method processes the needed updates to do, see ({@link CellUpdate}), then updates the cells,
 * updates the layout by calling {@link #processLayout(List)} and finally updates the indexes range of shown items.
 * <p></p>
 * The important part to understand is that rather than creating new cells everytime the flow scrolls
 * we update the already created cells, so the scroll is very smooth, and it's also efficient
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
     * Populates the cellsPool, adds them to the container and then calls {@link #updateCells(int)}
     * (indexes from 0 to num) to ensure the cells are displaying the right content (should not be needed though)
     *
     * @param num the number of cells to create
     */
    protected void initCells(int num) {
        cellsPool.clear();
        for (int i = 0; i < num && i < itemsSize(); i++) {
            cellsPool.add(cellForIndex(i));
        }

        // Add the cells to the container
        container.getChildren().setAll(cellsPool.stream().map(C::getNode).collect(Collectors.toList()));

        // Ensure that cells are properly updated
        updateCells(0);
    }

    /**
     * Responsible for updating the cells in the viewport.
     * <p></p>
     * If the items list is empty returns immediately.
     * <p></p>
     * Computes the max number of cells to show by calling {@link OrientationHelper#computeCellsNumber()}
     * then computes the {@code end} index by adding the {@code start} index to it (clamped between 0 and number of items).
     * <p></p>
     * If the list was cleared, so {@code start} is invalid, calls {@link #initCells(int)} with
     * the previous computed max, then exits as that method will then call this with a valid start index.
     * <p>
     * If the new indexes range (start-end) is equal to the previous stored range
     * it's not necessary to update the cells therefore exits. This check is ignored
     * if the items list was changed (items replaced, added or removed), int this case
     * it's needed to update.
     * <p></p>
     * The next step is a bit tricky. The cellsPool indexes go from 0 to size() - 1 of course,
     * but the start and end parameters do not start from 0, let's say I have to show items
     * from 10 to 20, so it's needed to use an "external" counter to get the cells from 0 to size() - 1,
     * while the items are retrieved from the list from {@code start} to {@code end}, in this loop we create
     * {@link CellUpdate} beans to make the code cleaner, after the loop {@link CellUpdate#update()} is
     * called for each bean, then the layout is updated with {@link #processLayout(List)} and
     * finally the range of shown items is updated.
     *
     * @param start the start index from which retrieve the items from the items list
     */
    protected void updateCells(int start) {
        if (itemsEmpty()) return;

        // Compute the range of items to show, from the given start to
        // start + max, where max is the number of cells that can be shown at any time.
        int max = virtualFlow.getOrientationHelper().computeCellsNumber();
        int rEnd = NumberUtils.clamp(start + max, 0, itemsSize());
        NumberRange<Integer> range = NumberRange.of(start, rEnd);

        // If the start index is invalid (list was cleared for example),
        // it's needed to re-initialize the cells.
        if (start == -1) {
            initCells(max);
            return;
        }

        // If the last range of shown items is equal to the computed one
        // there's no need to update. Exception being if the list was changed (listChanged flag).
        if (lastRange.equals(range) && !listChanged) return;

        // If everything went well, the last updates are cleared
        // and recomputed. Here's where the real update process starts.
        updates.clear();
        int poolIndex = 0;
        for (int i = range.getMin(); i < range.getMax(); i++) {
            T item = virtualFlow.getItems().get(i);
            CellUpdate update = new CellUpdate(item, cellsPool.get(poolIndex), i);
            updates.add(update);
            poolIndex++;
        }

        // Update the cells, process the layout and update the range of shown items.
        updates.forEach(CellUpdate::update);
        processLayout(updates);
        lastRange = range;
    }

    /**
     * Called when the items list changes, if the list
     * has been cleared calls {@link #clear()} and then exits.
     * <p>
     * If the last range is invalid (meaning that the container is empty) computes the
     * max number of cells to show with {@link OrientationHelper#computeCellsNumber()},
     * then calls {@link #initCells(int)} with that number.
     * <p></p>
     * If none of the above conditions are met then we have to make several checks:
     * <p>
     * It's needed to check if there are too many cells in the pool or too few,
     * depending on the case, it's needed to remove/supply some cells.
     * <p></p>
     * Finally the listChanged flag is set to true to force the update and
     * {@link #updateCells(int)} is called, the start index is the last start index.
     */
    public void itemsChanged() {
        if (itemsEmpty()) {
            clear();
            return;
        }

        // Cells were previously cleared, re-initialize
        if (lastRange.getMin() == -1 || lastRange.getMax() == -1) {
            int num = virtualFlow.getOrientationHelper().computeCellsNumber();
            initCells(num);
            return;
        }

        // If there are too many cells, remove them from the pool and the container.
        // If there are not enough cells then supply the needed amount.
        int num = virtualFlow.getOrientationHelper().computeCellsNumber();
        if (cellsPool.size() > num || cellsPool.size() > itemsSize()) {
            int overflow = (cellsPool.size() > num) ? cellsPool.size() - num :
                    cellsPool.size() > itemsSize() ? cellsPool.size() - itemsSize() : 0;
            for (int i = 0; i < overflow; i++) {
                C cell = cellsPool.remove(0);
                cell.dispose();
                container.getChildren().remove(0);
            }
        } else if (cellsPool.size() < num) {
            int max = NumberUtils.clamp(num, 0, itemsSize());
            supplyCells(cellsPool.size(), max);
        }

        listChanged = true;
        updateCells(lastRange.getMin());
        listChanged = false;
    }

    /**
     * Responsible for laying out the cells from a list of {@link CellUpdate}s.
     * <p></p>
     * The positioning is absolute, it always starts from [0, 0], this ensures
     * that there is no white space above or below any cell.
     * <p>
     * For each update (from 0 to updates.size()) cell's position is computed by
     * {@link OrientationHelper#layout(Node, int, double, double)}.
     */
    protected void processLayout(List<CellUpdate> updates) {
        double cellW = container.getCellWidth();
        double cellH = container.getCellHeight();

        for (int i = 0; i < updates.size(); i++) {
            CellUpdate update = updates.get(i);
            C cell = update.cell;
            Node node = cell.getNode();
            cell.beforeLayout();
            virtualFlow.getOrientationHelper().layout(node, i, cellW, cellH);
            cell.afterLayout();
        }
    }

    /**
     * Drastic reset of the VirtualFlow, scroll position is set back
     * to 0.0, {@link #clear()} is called, cells are re-initialized {@link #initCells(int)}.
     * <p>
     * Typically happens when the layout bounds of the VirtualFlow changed.
     */
    protected void reset() {
        virtualFlow.scrollToPixel(0.0);
        clear();
        int max = virtualFlow.getOrientationHelper().computeCellsNumber();
        initCells(max);
    }

    /**
     * Resets the CellsManager by clearing the container's children, clearing the cellsPool,
     * clearing the updates list and resetting the stored indexes range to [-1, -1].
     */
    protected void clear() {
        container.getChildren().clear();
        cellsPool.clear();
        updates.clear();
        lastRange = NumberRange.of(-1);
    }

    /**
     * Creates new cells from the given {@code from} index, keeps creating cells
     * until the cell pool size has reached {@code targetSize}, new cells are also added
     * to the container.
     */
    protected void supplyCells(int from, int targetSize) {
        for (int i = from; cellsPool.size() < targetSize; i++) {
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
