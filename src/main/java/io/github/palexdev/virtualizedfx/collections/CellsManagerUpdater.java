package io.github.palexdev.virtualizedfx.collections;

import io.github.palexdev.virtualizedfx.beans.NumberRange;
import io.github.palexdev.virtualizedfx.cell.ISimpleCell;
import io.github.palexdev.virtualizedfx.flow.simple.CellsManager;
import io.github.palexdev.virtualizedfx.utils.NumberUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper class to process changes in a list of items od type T.
 * <p></p>
 * Produces 4 maps:
 * <p> completeMap: the complete map of cells by their index
 * <p> toUpdate: the map of cells by their index that needs to be updated (in terms of layout)
 * <p> - toAdd: the map of cells by their index to add
 * <p> - toRemove: the map of cells by their index to remove
 * <p></p>
 * This is built from a {@link CellsManager} instance and also needs a cell factory to convert an
 * index to a Cell (in case it's needed to build new cells too).
 *
 * @param <T> the type of object to represent
 * @param <C> the type of Cell used
 */
public class CellsManagerUpdater<T, C extends ISimpleCell> {
    //================================================================================
    // Properties
    //================================================================================
    private final CellsManager<T, C> cellsManager;
    private final Function<Integer, C> cellFactory;
    private final Map<Integer, C> completeMap = new HashMap<>();
    private final Map<Integer, C> toUpdate = new HashMap<>();
    private final Map<Integer, C> toAdd = new HashMap<>();
    private final Map<Integer, C> toRemove = new HashMap<>();

    //================================================================================
    // Constructors
    //================================================================================
    public CellsManagerUpdater(CellsManager<T, C> cellsManager, Function<Integer, C> cellFactory) {
        this.cellsManager = cellsManager;
        this.cellFactory = cellFactory;
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Processes the addition of new items from the given Set of indexes and offset.
     * <p></p>
     * The offset is the index at which new items have been added, this means that
     * from offset to cells.size() it's needed to shift the cell's index to (i + added.size()).
     * <p>
     * Also note that {@link CellsManager}'s cells are stored in a new tmp map and removed from this one
     * in the loop, keep this tmp map in mind.
     * <p>
     * Extra cells that exceed the {@link CellsManager} indexes range are added to the toRemove map,
     * all other cells are added to the toUpdate map.
     * <p>
     * Then the toAdd map in built from the added Set, all these index-cell entries are also added to the
     * toUpdate map (since they need to be laid out).
     * <p>
     * At the end the completeMap is built from the remaining cells in the tmp map and the cells from the
     * toUpdate map.
     *
     * @param added the Set of added indexes
     * @param offset the index at which items were added
     */
    public void computeAddition(Set<Integer> added, int offset) {
        Map<Integer, C> cells = cellsManager.getCells();
        Map<Integer, C> tmp = new HashMap<>(cells);
        for (int i = offset; i < cells.size(); i++) {
            C cell = tmp.remove(i);
            int index = i + added.size();
            if (index > cellsManager.getIndexes().getMax()) {
                toRemove.put(i, cell);
                continue;
            }
            toUpdate.put(index, cell);
        }
        toAdd.putAll(added.stream().collect(Collectors.toMap(i -> i, cellFactory)));
        toUpdate.putAll(toAdd);
        completeMap.putAll(tmp);
        completeMap.putAll(toUpdate);
    }

    /**
     * Processes the removal of items from the given Set of indexes and offset.
     * <p></p>
     * The offset is the index at which items have been removed, this means that
     * from offset to cells.size() it's needed to shift the cell's index.
     * Now since removals can also be non-contiguous it's also needed to properly compute the correct
     * number to shift the indexes, so the new indexes will be (i - computedShift).
     * <p>
     * Basically what it does is to check if the `i` value is contained in the removed Set, if so
     * the removal is contiguous and the shift is incremented by one. When `i` is not contained it
     * means the element needs to be shifted.
     * Shifted cells are added to the toUpdate map.
     * <p>
     * While doing this it also keeps track of the max index reached, which is equal to the last index shifted.
     * This is done because when removing cells it's also needed to build new cells if possible.
     * New cells are created with {@link #createNewCells(int, int)} and are added to both toAdd and toUpdate maps.
     * <p>
     * At the end the completeMap is built from the remaining cells in the tmp map and the cells from the
     * toUpdate map.
     *
     * @param removed the Set of removed indexes
     * @param offset the index at which items were removed
     */
    public void computeRemoval(Set<Integer> removed, int offset) {
        Map<Integer, C> cells = cellsManager.getCells();
        Map<Integer, C> tmp = new HashMap<>(cells);
        toRemove.putAll(removed.parallelStream().filter(tmp::containsKey).collect(Collectors.toMap(i -> i, tmp::get)));

        int shift = 0;
        int max = 0;
        for (int i = offset; i < cells.size(); i++) {
            C cell = tmp.remove(i);
            if (removed.contains(i)) {
                shift++;
                continue;
            }
            int index = i - shift;
            max = index;
            toUpdate.put(index, cell);
        }

        if (max > 0) {
            Map<Integer, C> created = createNewCells(max + 1, cellsManager.getIndexes().getMax());
            toAdd.putAll(created);
            toUpdate.putAll(created);
        }

        completeMap.putAll(tmp);
        completeMap.putAll(toUpdate);
    }

    /**
     * Creates new cells in the given range.
     * <p></p>
     * In case they are valid, first they are clamped between 0 and the items list size - 1, then
     * new cells are stores in a Map by their index and returned.
     */
    private Map<Integer, C> createNewCells(int from, int to) {
        if (cellsManager.itemsSize() == 0 || cellsManager.itemsSize() <= to) return new HashMap<>();
        int cFrom = NumberUtils.clamp(from, 0, cellsManager.itemsSize() - 1);
        int cTo = NumberUtils.clamp(to, 0, cellsManager.itemsSize() - 1);
        return NumberRange.expandRangeToSet(NumberRange.of(cFrom, cTo)).stream().collect(Collectors.toMap(i -> i, cellFactory));
    }

    //================================================================================
    // Getters
    //================================================================================

    /**
     * @return the computed complete map of cells
     */
    public Map<Integer, C> getCompleteMap() {
        return completeMap;
    }

    /**
     * @return the computed map of cells that need to be laid out (or just updated in terms of layout)
     */
    public Map<Integer, C> getToUpdate() {
        return toUpdate;
    }

    /**
     * @return the computed map of cells that need to be added to the container
     */
    public Map<Integer, C> getToAdd() {
        return toAdd;
    }

    /**
     * @return the computed map of cells that need to be removed from the container
     */
    public Map<Integer, C> getToRemove() {
        return toRemove;
    }
}
