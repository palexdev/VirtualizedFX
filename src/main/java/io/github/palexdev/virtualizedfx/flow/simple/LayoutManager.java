package io.github.palexdev.virtualizedfx.flow.simple;

import io.github.palexdev.virtualizedfx.beans.NumberRange;
import io.github.palexdev.virtualizedfx.beans.NumberRangeProperty;
import io.github.palexdev.virtualizedfx.cell.base.ISimpleCell;
import io.github.palexdev.virtualizedfx.utils.ExecutionUtils;
import io.github.palexdev.virtualizedfx.utils.NumberUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class LayoutManager<T, C extends ISimpleCell> {
    private final SimpleVirtualFlow<T, C> virtualFlow;
    private final NumberRangeProperty<Integer> indexes = new NumberRangeProperty<>();
    private final NumberRangeProperty<Double> containerBounds = new NumberRangeProperty<>();
    private final DoubleProperty totalHeight = new SimpleDoubleProperty();
    private double cellHeight;
    private double cellWidth;

    public LayoutManager(SimpleVirtualFlow<T, C> virtualFlow) {
        this.virtualFlow = virtualFlow;
    }

    void initialize() {
        if (virtualFlow.getItems().isEmpty()) {
            ExecutionUtils.executeWhen(
                    virtualFlow.getItems(),
                    (oldList, newList) -> {
                        C cell = virtualFlow.getCellFactory().apply(virtualFlow.getItems().get(0));
                        cellHeight = cell.getFixedHeight();
                        cellWidth = cell.getFixedWidth();
                        initFlow();
                    },
                    false,
                    (oldList, newList) -> !newList.isEmpty(),
                    true
            );
        } else {
            C cell = virtualFlow.getCellFactory().apply(virtualFlow.getItems().get(0));
            cellHeight = cell.getFixedHeight();
            cellWidth = cell.getFixedWidth();
            initFlow();
        }
    }

    private void initFlow() {
        containerBounds.addListener((observable, oldValue, newValue) -> computeIndexes());
        totalHeight.bind(Bindings.createDoubleBinding(
                () -> cellHeight * virtualFlow.getItems().size(),
                virtualFlow.getItems()
        ));
        containerBounds.bind(Bindings.createObjectBinding(
                () -> {
                    double min = -virtualFlow.vValueProperty().doubleValue();
                    double max = getTotalHeight() + min;
                    return NumberRange.of(min, max);
                }, virtualFlow.vValueProperty()
        ));
    }

    private void computeIndexes() {
        //if (!overscanReached()) return;
        int min = NumberUtils.clamp(firstVisible() - virtualFlow.getOverscan(), 0, virtualFlow.getItems().size() - 1);
        int max = NumberUtils.clamp(lastVisible() + virtualFlow.getOverscan(), 0, virtualFlow.getItems().size() - 1);
        setIndexes(NumberRange.of(min, max));
        virtualFlow.container.cellsManger.updateContent();
    }

    private boolean overscanReached() {
        if (getIndexes() == null) return true;
        NumberRange<Integer> layoutIndexes = getIndexes();
        return firstVisible() < layoutIndexes.getMin() || lastVisible() > layoutIndexes.getMax();
    }

    private int firstVisible() {
        double absMin = Math.abs(getContainerBounds().getMin());
        return (int) Math.floor(absMin / cellHeight);
    }

    private int lastVisible() {
        double absMin = Math.abs(getContainerBounds().getMin());
        double max = NumberUtils.clamp(absMin + virtualFlow.getHeight(), 0, getTotalHeight());
        return (int) Math.floor(max / cellHeight);
    }

    public double getCellHeight() {
        return cellHeight;
    }

    public double getCellWidth() {
        return cellWidth;
    }

    public double getTotalHeight() {
        return totalHeight.get();
    }

    public DoubleProperty totalHeightProperty() {
        return totalHeight;
    }

    public NumberRange<Double> getContainerBounds() {
        return containerBounds.get();
    }

    public NumberRange<Integer> getIndexes() {
        return indexes.get();
    }

    public void setIndexes(NumberRange<Integer> indexes) {
        this.indexes.set(indexes);
    }
}
