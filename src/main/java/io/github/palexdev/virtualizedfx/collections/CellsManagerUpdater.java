package io.github.palexdev.virtualizedfx.collections;

import io.github.palexdev.virtualizedfx.cell.base.ISimpleCell;
import io.github.palexdev.virtualizedfx.flow.simple.CellsManager;
import io.github.palexdev.virtualizedfx.utils.NumberUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CellsManagerUpdater<T, C extends ISimpleCell> {
    private final CellsManager<T, C> cellsManager;
    private final Function<Integer, C> cellFactory;
    private final Map<Integer, C> completeMap = new HashMap<>();
    private final Map<Integer, C> toUpdate = new HashMap<>();
    private final Map<Integer, C> toAdd = new HashMap<>();
    private final Map<Integer, C> toRemove = new HashMap<>();

    public CellsManagerUpdater(CellsManager<T, C> cellsManager, Function<Integer, C> cellFactory) {
        this.cellsManager = cellsManager;
        this.cellFactory = cellFactory;
    }

    public void computeAddition(Set<Integer> added, int to) {
        Map<Integer, C> cells = cellsManager.getCells();
        Map<Integer, C> tmp = new HashMap<>(cells);
        for (int i = to + 1; i < cells.size(); i++) {
            C cell = tmp.remove(i);
            int index = i + added.size();
            if (index > cellsManager.getIndexes().getMax()) {
                toRemove.put(i, cell);
                continue;
            }
            toUpdate.put(index, cell);
        }
        toAdd.putAll(added.stream().collect(Collectors.toMap(i -> i + 1, cellFactory)));
        toUpdate.putAll(toAdd);
        completeMap.putAll(tmp);
        completeMap.putAll(toUpdate);
    }

    public void computeRemoval(Set<Integer> removed, int to) {
        Map<Integer, C> cells = cellsManager.getCells();
        Map<Integer, C> tmp = new HashMap<>(cells);
        toRemove.putAll(removed.stream().collect(Collectors.toMap(i -> i, tmp::get)));

        int shift = 0;
        int max = 0;
        for (int i = to; i < cells.size(); i++) {
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

    private Map<Integer, C> createNewCells(int from, int to) {
        int cFrom = NumberUtils.clamp(from, 0, cellsManager.itemsSize() - 1);
        int cTo = NumberUtils.clamp(to, 0, cellsManager.itemsSize() - 1);
        return IntStream.rangeClosed(cFrom, cTo).boxed().collect(Collectors.toMap(i -> i, cellFactory));
    }

    public Map<Integer, C> getCompleteMap() {
        return completeMap;
    }

    public Map<Integer, C> getToUpdate() {
        return toUpdate;
    }

    public Map<Integer, C> getToAdd() {
        return toAdd;
    }

    public Map<Integer, C> getToRemove() {
        return toRemove;
    }
}
