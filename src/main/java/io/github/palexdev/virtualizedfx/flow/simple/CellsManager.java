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
import io.github.palexdev.virtualizedfx.cell.ISimpleCell;
import io.github.palexdev.virtualizedfx.collections.CellsManagerUpdater;
import io.github.palexdev.virtualizedfx.collections.SetsDiff;
import io.github.palexdev.virtualizedfx.enums.Gravity;
import io.github.palexdev.virtualizedfx.utils.ListChangeHelper;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class to properly keep track of the needed cells, add the
 * cells' nodes to the container and update the layout when needed.
 * <p></p>
 * This uses a separate range of indexes for the cells which reflects the
 * {@link LayoutManager} indexes but they are updated lazily, see {@link #updateContent()}.
 * <p></p>
 * For optimization reasons this helper uses only Map and Sets, indexes are unique so
 * using Lists would not make sense, and it's way slower.
 * <p>
 * There are two Maps:
 * <p> - cells: the currently built cells by their index
 * <p> - toUpdate: this map contains only the cells that need to be updated (in terms of layout) by their index
 *
 * @param <T> the type of object to represent
 * @param <C> the type of Cell to use
 */
public class CellsManager<T, C extends ISimpleCell> {
    //================================================================================
    // Properties
    //================================================================================
    private final SimpleVirtualFlow<T, C> virtualFlow;
    private final LayoutManager<T, C> layoutManager;
    private NumberRange<Integer> indexes = NumberRange.of(-1);
    private Map<Integer, C> cells = new HashMap<>();
    private Map<Integer, C> toUpdate = new HashMap<>();

    //================================================================================
    // Properties
    //================================================================================
    public CellsManager(SimpleVirtualFlow<T, C> virtualFlow, LayoutManager<T, C> layoutManager) {
        this.virtualFlow = virtualFlow;
        this.layoutManager = layoutManager;
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Before doing anything checks if the {@link LayoutManager} indexes are the same of
     * the cellsManager. In case they are this method exists immediately.
     * <p>
     * It also exits if the items list is empty.
     * <p>
     * From now on we assume that the indexes are different and the terms are:
     * <p> - newRange for the layoutManger indexes
     * <p> - indexes for the cellsManager indexes
     * <p></p>
     * First computes the difference between the indexes and the newRange using {@link SetsDiff#indexDifference(Set, Set)},
     * since it takes two Sets as arguments the two ranges are expanded using {@link NumberRange#expandRangeToSet(NumberRange)}.
     * <p>
     * The result is a {@link SetsDiff} instance which contains removed and added indexes.
     * For each removed index a cell is removed from the cells map and the nodes are accumulated in a temp Set.
     * Then all the accumulated nodes are removed by using {@link ObservableList#removeAll(Collection)}.
     * <p>
     * After the removal the toUpdate map is build by mapping the added indexes to a Map of
     * index-cell. The new cells are added to the cells map.
     * Then all the new nodes are added to the container using {@link ObservableList#addAll(Collection)}.
     * <p>
     * At the end calls {@link #processLayout(Map)} with the toUpdate map as parameter and
     * updates the indexes with the newRange.
     */
    void updateContent() {
        int min = layoutManager.getIndexes().getMin();
        int max = layoutManager.getIndexes().getMax();
        NumberRange<Integer> newRange = NumberRange.of(min, max);
        if (newRange.equals(indexes) || itemsSize() == 0) return;

        SetsDiff<Integer> diff = SetsDiff.indexDifference(NumberRange.expandRangeToSet(indexes), NumberRange.expandRangeToSet(newRange));
        Set<Node> toRemove = new HashSet<>();
        for (Integer index : diff.getRemoved()) {
            C cell = cells.remove(index);
            toRemove.add(cell.getNode());
            cell.dispose();
        }
        getChildren().removeAll(toRemove);

        toUpdate = diff.getAdded().stream().collect(Collectors.toMap(
                i -> i,
                i -> virtualFlow.getCellFactory().apply(virtualFlow.getItems().get(i))
        ));
        cells.putAll(toUpdate);
        getChildren().addAll(toUpdate.values().stream().map(C::getNode).collect(Collectors.toList()));
        processLayout(toUpdate);
        indexes = newRange;
    }

    /**
     * Resets the CellsManager, also removes all nodes from the container.
     * <p>
     * Needed for example when the items list is changed.
     */
    void clear() {
        indexes = NumberRange.of(-1);
        cells.clear();
        toUpdate.clear();
        getChildren().clear();
    }

    /**
     * This method is responsible for laying out the cells' nodes properly.
     * <p>
     * It is also responsible for calling {@link ISimpleCell#updateIndex(int)}, {@link ISimpleCell#beforeLayout()}, {@link ISimpleCell#afterLayout()}.
     *
     * @param cells the cells to layout
     */
    private void processLayout(Map<Integer, C> cells) {
        double ch = layoutManager.getCellHeight();
        double cw = layoutManager.getCellWidth();
        double nx = 0;
        double ny = 0;
        double nw = ((cw <= 0) ? virtualFlow.getWidth() : cw);
        double nh = ((ch <= 0) ? virtualFlow.getHeight() : ch);

        Gravity gravity = virtualFlow.getGravity();
        if (gravity == Gravity.TOP_BOTTOM) {
            for (Map.Entry<Integer, C> entry : cells.entrySet()) {
                C cell = entry.getValue();
                int index = entry.getKey();
                cell.updateIndex(index);
                cell.beforeLayout();
                ny = nh * index;
                cell.getNode().resizeRelocate(nx, ny, virtualFlow.getWidth(), nh);
                cell.afterLayout();
            }
        } else {
            for (Map.Entry<Integer, C> entry : cells.entrySet()) {
                C cell = entry.getValue();
                int index = entry.getKey();
                cell.updateIndex(index);
                cell.beforeLayout();
                nx = nw * index;
                cell.getNode().resizeRelocate(nx, ny, nw, virtualFlow.getHeight());
                cell.afterLayout();
            }
        }
    }

    /**
     * Calls {@link #processLayout(Map)} with the complete cells map.
     * <p>
     * This is needed for example when the layout bounds of the VirtualFlow change.
     */
    public void recomputeLayout() {
        if (cells.isEmpty()) return;
        processLayout(cells);
    }

    /**
     * This method is responsible for processing changes in the items list.
     * <p>
     * To keep things clear, easy, organized the computation is managed by a separate
     * helper class, {@link ListChangeHelper}.
     * <p></p>
     * Replacements are managed by {@link #replace(Set, Set)}. <p>
     * Additions are managed by {@link #add(Set, int)}. <p>
     * Removals are managed by {@link #remove(Set, int)}
     */
    public void itemsChanged(ListChangeListener.Change<? extends T> change) {
        ListChangeHelper.Change c = ListChangeHelper.processChange(change, indexes);
        c.processReplacement(this::replace);
        c.processAddition((from, to, added) -> add(added, from));
        c.processRemoval((from, to, added) -> remove(added, from));
    }

    /**
     * Processes additions in the items list.
     * <p></p>
     * The computation is deferred to a helper class, {@link CellsManagerUpdater#computeAddition(Set, int)}.
     * <p>
     * Computes nodes additions and removals and then calls {@link #processLayout(Map)} with the toUpdate map
     * which has been previously computed by the CellsManagerUpdater.
     */
    private void add(Set<Integer> added, int offset) {
        CellsManagerUpdater<T, C> updater = new CellsManagerUpdater<>(this, i -> {
            T item = virtualFlow.getItems().get(i);
            return virtualFlow.getCellFactory().apply(item);
        });
        updater.computeAddition(added, offset);
        this.cells = updater.getCompleteMap();
        this.toUpdate = updater.getToUpdate();
        updater.getToAdd().forEach((integer, c) -> getChildren().add(c.getNode()));
        updater.getToRemove().forEach((integer, c) -> {
            c.dispose();
            getChildren().remove(c.getNode());
        });
        processLayout(toUpdate);
    }

    /**
     * Processes removals in the items list.
     * <p></p>
     * The computation is deferred to a helper class, {@link CellsManagerUpdater#computeRemoval(Set, int)}.
     * <p>
     * Computes nodes additions and removals and then calls {@link #processLayout(Map)} with the toUpdate map
     * which has been previously computed by the CellsManagerUpdater.
     */
    private void remove(Set<Integer> removed, int offset) {
        CellsManagerUpdater<T, C> updater = new CellsManagerUpdater<>(this, i -> {
            if (virtualFlow.getItems().isEmpty()) return null;
            T item = virtualFlow.getItems().get(i);
            return virtualFlow.getCellFactory().apply(item);
        });
        updater.computeRemoval(removed, offset);
        this.cells = updater.getCompleteMap();
        this.toUpdate = updater.getToUpdate();
        updater.getToAdd().forEach((integer, c) -> getChildren().add(c.getNode()));
        updater.getToRemove().forEach((integer, c) -> {
            c.dispose();
            getChildren().remove(c.getNode());
        });
        processLayout(toUpdate);
    }

    /**
     * Processes replacements in the items list.
     * <p></p>
     * The toUpdate map is processed by mapping the changed Set to index-cell,
     * a toRemove local map is built by mapping the removed Set to index-cell.
     * <p></p>
     * The cells map is updated by adding the toUpdate map then replacements are processed
     * by using the {@link ObservableList#set(int, Object)} method and removals are processed
     * by using the {@link ObservableList#remove(Object)} method.
     */
    private void replace(Set<Integer> changed, Set<Integer> removed) {
        toUpdate = changed.stream().collect(Collectors.toMap(
                i -> i,
                i -> virtualFlow.getCellFactory().apply(virtualFlow.getItems().get(i))
        ));
        Map<Integer, C> toRemove = removed.stream().collect(Collectors.toMap(
                i -> i,
                cells::remove
        ));
        cells.putAll(toUpdate);

        for (Map.Entry<Integer, C> entry : toRemove.entrySet()) {
            C cell = entry.getValue();
            cell.dispose();
            getChildren().remove(cell.getNode());
        }
        for (Map.Entry<Integer, C> entry : toUpdate.entrySet()) {
            C cell = entry.getValue();
            getChildren().set(entry.getKey(), cell.getNode());
        }
        processLayout(toUpdate);
    }

    //================================================================================
    // Getters
    //================================================================================

    /**
     * @return an unmodifiable maps containing all the built cells
     */
    public Map<Integer, C> getCells() {
        return Collections.unmodifiableMap(cells);
    }

    /**
     * @return the cells index range
     */
    public NumberRange<Integer> getIndexes() {
        return indexes;
    }

    /**
     * @return the items list size
     */
    public int itemsSize() {
        return virtualFlow.getItems().size();
    }

    /**
     * Delegate to {@link SimpleVirtualFlowContainer#getChildren()}.
     */
    private ObservableList<Node> getChildren() {
        return virtualFlow.container.getChildren();
    }
}
