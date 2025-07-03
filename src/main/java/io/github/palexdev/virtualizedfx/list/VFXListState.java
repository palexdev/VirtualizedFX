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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.SequencedMap;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap.StateMap;
import io.github.palexdev.virtualizedfx.utils.Utils;
import io.github.palexdev.virtualizedfx.utils.VFXCellsCache;
import javafx.scene.Node;

/**
 * Immutable object to represent the state of a {@link VFXList} in a specific moment in time. In other words, each and
 * every state is given by a unique combination of the list's properties (in terms of values).
 * <p>
 * The state carries three important information:
 * <p> 1) The range of items to display from the {@link VFXList#itemsProperty()}
 * <p> 2) The cells that are currently in the viewport
 * <p> 3) A flag that indicates whether cells have changed since last state
 * <p></p>
 * <b>Note</b> that the data structure used to store the cells is particular, see {@link StateMap}.
 *
 * @see #INVALID
 * @see StateMap
 */
// NullPointerException warnings due to special INVALID state
@SuppressWarnings({"DataFlowIssue", "rawtypes", "SameParameterValue"})
public class VFXListState<T, C extends VFXCell<T>> {
    //================================================================================
    // Static Properties
    //================================================================================
    /**
     * Special instance of {@code VFXListState} used to indicate that no cells can be present in the viewport at
     * a certain time. The reasons can be many, for example, no cell factory, invalid range, {@literal width/height <= 0}, etc...
     * <p>
     * This and {@link #isEmpty()} are two total different things!!
     */
    public static final VFXListState INVALID = new VFXListState<>() {
        @Override
        protected VFXCell<Object> removeCell(int index) {return null;}

        @Override
        protected VFXCell<Object> removeCell(Object item) {return null;}

        @Override
        protected void dispose() {}
    };

    //================================================================================
    // Properties
    //================================================================================
    private final VFXList<T, C> list;
    private final IntegerRange range;
    private final StateMap<T, C> cells = new StateMap<>();
    private boolean cellsChanged = false;

    //================================================================================
    // Constructors
    //================================================================================
    private VFXListState() {
        this.list = null;
        this.range = Utils.INVALID_RANGE;
    }

    public VFXListState(VFXList<T, C> list, IntegerRange range) {
        this.list = list;
        this.range = range;
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Retrieves the item at the given index from {@link VFXList#itemsProperty()} and delegates to
     * {@link #addCell(int, Object, VFXCell)}.
     *
     * @see StateMap
     */
    protected void addCell(int index, C cell) {
        addCell(index, list.getItems().get(index), cell);
    }

    /**
     * Adds the given cell to the {@link StateMap} of this state object.
     *
     * @see StateMap
     */
    protected void addCell(int index, T item, C cell) {
        cells.put(index, item, cell);
    }

    /**
     * Removes a cell from the {@link StateMap} for the given index. If the cell is not found, the next attempt
     * is to remove it by the item at the given index in the {@link VFXList#itemsProperty()}.
     */
    protected C removeCell(int index) {
        C c = cells.remove(index);
        if (c == null) c = removeCell(list.getItems().get(index));
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
        VFXCellsCache<T, C> cache = list.getCache();
        cache.cache(getCellsByIndex().values());
        cells.clear();
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    /**
     * @return the {@link VFXList} instance this state is associated to
     */
    public VFXList<T, C> getList() {
        return list;
    }

    /**
     * @return the range of items to display
     */
    public IntegerRange getRange() {
        return range;
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
    protected List<Entry<T, C>> getCellsByItem() {
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
    public List<Entry<T, C>> getCellsByItemUnmodifiable() {
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
     * @see VFXListSkin
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
