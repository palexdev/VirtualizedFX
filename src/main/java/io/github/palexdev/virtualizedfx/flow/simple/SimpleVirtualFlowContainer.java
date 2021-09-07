package io.github.palexdev.virtualizedfx.flow.simple;

import io.github.palexdev.virtualizedfx.cell.base.ISimpleCell;
import io.github.palexdev.virtualizedfx.utils.ExecutionUtils;
import javafx.beans.property.DoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Group;
import javafx.scene.shape.Rectangle;

public class SimpleVirtualFlowContainer<T, C extends ISimpleCell> extends Group {
    private final SimpleVirtualFlow<T, C> virtualFlow;
    final CellsManager<T, C> cellsManger;
    final LayoutManager<T, C> layoutManager;

    public SimpleVirtualFlowContainer(SimpleVirtualFlow<T, C> virtualFlow) {
        this.virtualFlow = virtualFlow;
        this.layoutManager = new LayoutManager<>(virtualFlow);
        this.cellsManger = new CellsManager<>(virtualFlow, layoutManager);
        initialize();
    }

    private void initialize() {
        setStyle("-fx-border-color: blue");
        buildClip();
        ExecutionUtils.executeWhen(
                virtualFlow.heightProperty(),
                (oldValue, newValue) -> {
                    layoutManager.initialize();
                    cellsManger.initialize();
                    virtualFlow.getItems().addListener((ListChangeListener<? super T>) cellsManger::itemsChanged);
                },
                false,
                (oldValue, newValue) -> newValue != null && newValue.doubleValue() > 0,
                true
        );
    }

    private void buildClip() {
        Rectangle rectangle = new Rectangle();
        rectangle.widthProperty().bind(virtualFlow.widthProperty());
        rectangle.heightProperty().bind(virtualFlow.heightProperty());
        rectangle.layoutYProperty().bind(layoutYProperty().multiply(-1));
        setClip(rectangle);
    }

    public double getTotalHeight() {
        return layoutManager.getTotalHeight();
    }

    public DoubleProperty totalHeightProperty() {
        return layoutManager.totalHeightProperty();
    }

    @Override
    protected void layoutChildren() {}
}
