package io.github.palexdev.virtualizedfx.flow.simple;

import io.github.palexdev.virtualizedfx.beans.NumberRange;
import io.github.palexdev.virtualizedfx.cell.base.ISimpleCell;
import io.github.palexdev.virtualizedfx.collections.CellsManagerUpdater;
import io.github.palexdev.virtualizedfx.collections.SetsDiff;
import io.github.palexdev.virtualizedfx.utils.ListChangeHelper;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import java.util.*;
import java.util.stream.Collectors;

public class CellsManager<T, C extends ISimpleCell> {
    private final SimpleVirtualFlow<T, C> virtualFlow;
    private final LayoutManager<T, C> layoutManager;
    private NumberRange<Integer> indexes = NumberRange.of(-1);
    private Map<Integer, C> cells = new HashMap<>();
    private Map<Integer, C> toUpdate = new HashMap<>();

    public CellsManager(SimpleVirtualFlow<T, C> virtualFlow, LayoutManager<T, C> layoutManager) {
        this.virtualFlow = virtualFlow;
        this.layoutManager = layoutManager;
    }

    void initialize() {
        updateContent();
    }

    void updateContent() {
        int min = layoutManager.getIndexes().getMin();
        int max = layoutManager.getIndexes().getMax();
        NumberRange<Integer> newRange = NumberRange.of(min, max);
        if (newRange.equals(indexes)) return;

        SetsDiff<Integer> diff = SetsDiff.intDifference(NumberRange.expandRangeToSet(indexes), NumberRange.expandRangeToSet(newRange));
        Set<Node> toRemove = new HashSet<>();
        for (Integer index : diff.getRemoved()) {
            C cell = cells.remove(index);
            toRemove.add(cell.getNode());
        }
        getChildren().removeAll(toRemove);

        toUpdate = diff.getAdded().stream().collect(Collectors.toMap(
                i -> i,
                i -> virtualFlow.getCellFactory().apply(virtualFlow.getItems().get(i))
        ));
        cells.putAll(toUpdate);
        getChildren().addAll(toUpdate.values().stream().map(C::getNode).collect(Collectors.toList()));
        processLayout();
        indexes = newRange;
    }

    private void processLayout() {
        double ch = layoutManager.getCellHeight();
        double cw = layoutManager.getCellWidth();
        double ny;
        double nw = ((cw <= 0) ? virtualFlow.getWidth() : cw);
        double nh = ((ch <= 0) ? virtualFlow.getHeight() : ch);

        for (Map.Entry<Integer, C> entry : toUpdate.entrySet()) {
            C cell = entry.getValue();
            int index = entry.getKey();
            cell.updateIndex(index);
            cell.beforeLayout();
            ny = nh * index;
            cell.getNode().resizeRelocate(0, ny, nw, nh);
            cell.afterLayout();
        }
    }

    public void itemsChanged(ListChangeListener.Change<? extends T> change) {
        ListChangeHelper.Change c = ListChangeHelper.processChange(change, indexes);
        c.processReplacement(this::replace);
        c.processAddition((from, to, added) -> add(added, from));
        c.processRemoval((from, to, added) -> remove(added, from));
    }

    private void add(Set<Integer> added, int offset) {
        CellsManagerUpdater<T, C> updater = new CellsManagerUpdater<>(this, i -> {
            T item = virtualFlow.getItems().get(i);
            return virtualFlow.getCellFactory().apply(item);
        });
        updater.computeAddition(added, offset);
        this.cells = updater.getCompleteMap();
        this.toUpdate = updater.getToUpdate();
        updater.getToAdd().forEach((integer, c) -> getChildren().add(c.getNode()));
        updater.getToRemove().forEach((integer, c) -> getChildren().remove(c.getNode()));
        processLayout();
    }

    private void remove(Set<Integer> removed, int to) {
        CellsManagerUpdater<T, C> updater = new CellsManagerUpdater<>(this, i -> {
            T item = virtualFlow.getItems().get(i);
            return virtualFlow.getCellFactory().apply(item);
        });
        updater.computeRemoval(removed, to);
        this.cells = updater.getCompleteMap();
        this.toUpdate = updater.getToUpdate();
        updater.getToAdd().forEach((integer, c) -> getChildren().add(c.getNode()));
        updater.getToRemove().forEach((integer, c) -> getChildren().remove(c.getNode()));
        processLayout();
    }

    private void replace(Set<Integer> changed, Set<Integer> removed) {
/*            if (changed.isEmpty()) return;
            Set<Integer> toChange = changed.parallelStream().filter(i -> NumberRange.inRangeOf(i, indexes)).collect(Collectors.toSet());
            if (toChange.isEmpty()) return;

            toUpdate = toChange.stream().collect(Collectors.toMap(
                    i -> i,
                    i -> virtualFlow.getCellFactory().apply(virtualFlow.getItems().get(i))
            ));
 */
        toUpdate = changed.stream().collect(Collectors.toMap(
                i -> i,
                i -> virtualFlow.getCellFactory().apply(virtualFlow.getItems().get(i))
        ));
        Map<Integer, C> toRemove = removed.stream().collect(Collectors.toMap(
                i -> i,
                cells::remove
        ));
        cells.putAll(toUpdate);

        for (Map.Entry<Integer, C> entry : toUpdate.entrySet()) {
            C cell = entry.getValue();
            getChildren().set(entry.getKey(), cell.getNode());
        }
        for (Map.Entry<Integer, C> entry : toRemove.entrySet()) {
            C cell = entry.getValue();
            getChildren().remove(cell.getNode());
        }
        processLayout();
    }

    public Map<Integer, C> getCells() {
        return Collections.unmodifiableMap(cells);
    }

    public NumberRange<Integer> getIndexes() {
        return indexes;
    }

    public int itemsSize() {
        return virtualFlow.getItems().size();
    }

    private ObservableList<Node> getChildren() {
        return virtualFlow.container.getChildren();
    }
}
