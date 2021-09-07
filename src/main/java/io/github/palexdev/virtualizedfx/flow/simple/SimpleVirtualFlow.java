package io.github.palexdev.virtualizedfx.flow.simple;

import io.github.palexdev.virtualizedfx.cell.base.ISimpleCell;
import io.github.palexdev.virtualizedfx.utils.NumberUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.event.EventDispatcher;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;

import java.util.function.Function;

public class SimpleVirtualFlow<T, C extends ISimpleCell> extends Region {
    //================================================================================
    // Properties
    //================================================================================

    private final ListProperty<T> items = new SimpleListProperty<>();
    private final ObjectProperty<Function<T, C>> cellFactory = new SimpleObjectProperty<>();
    private int overscan;

    private final ScrollBar vBar = new ScrollBar();
    private final DoubleProperty vValue = new SimpleDoubleProperty();
    final SimpleVirtualFlowContainer<T, C> container = new SimpleVirtualFlowContainer<>(this);

    //================================================================================
    // Constructors
    //================================================================================
    protected SimpleVirtualFlow() {
        initialize();
    }

    //================================================================================
    // Methods
    //================================================================================

    private void initialize() {
        getChildren().add(container);
        setupScrollBars();
        EventDispatcher original = getEventDispatcher();
        setEventDispatcher((event, tail) -> {
            tail.prepend(vBar.getEventDispatcher());
            return original.dispatchEvent(event, tail);
        });
    }

    protected void setupScrollBars() {
        addEventFilter(ScrollEvent.ANY, event -> System.out.println("Scroll"));

        vBar.getStyleClass().add("vbar");
        vBar.setManaged(false);
        vBar.setOrientation(Orientation.VERTICAL);
        vBar.setUnitIncrement(15);
        vBar.maxProperty().bind(Bindings.createDoubleBinding(
                () -> snapSpaceY(container.getTotalHeight() - getHeight()),
                container.totalHeightProperty(), heightProperty()
        ));
        vBar.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> container.getTotalHeight() > getHeight(),
                container.totalHeightProperty(), heightProperty()
        ));
        vValueProperty().bindBidirectional(vBar.valueProperty());
        vValueProperty().addListener((observable, oldValue, newValue) -> container.setLayoutY(-newValue.doubleValue()));

        getChildren().addAll(vBar);
    }

    public void scrollTo(int index) {
        setvValue(container.layoutManager.getCellHeight() * index);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        container.resize(getWidth(), -1);
        vBar.resizeRelocate(getWidth() - 10, 0, 10, getHeight());
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    public ListProperty<T> getItems() {
        return items;
    }

    public void setItems(ObservableList<T> items) {
        this.items.set(items);
    }

    public Function<T, C> getCellFactory() {
        return cellFactory.get();
    }

    public ObjectProperty<Function<T, C>> cellFactoryProperty() {
        return cellFactory;
    }

    public void setCellFactory(Function<T, C> cellFactory) {
        this.cellFactory.set(cellFactory);
    }

    public int getOverscan() {
        return overscan;
    }

    public void setOverscan(int overscan) {
        this.overscan = overscan;
    }

    public double getvValue() {
        return vValue.get();
    }

    public DoubleProperty vValueProperty() {
        return vValue;
    }

    public void setvValue(double vValue) {
        this.vValue.set(vValue);
    }

    public void setVSpeed(double unit, double block) {
        vBar.setUnitIncrement(unit);
        vBar.setBlockIncrement(block);
    }

    //================================================================================
    // Builder
    //================================================================================
    public static class Builder<T, C extends ISimpleCell> {
        private final SimpleVirtualFlow<T, C> virtualFlow = new SimpleVirtualFlow<>();

        public static <T, C extends ISimpleCell> Builder<T, C> get() {
            return new Builder<>();
        }

        public Builder<T, C> setItems(ObservableList<T> items) {
            virtualFlow.setItems(items);
            return this;
        }

        public Builder<T, C> setCellFactory(Function<T, C> cellFactory) {
            virtualFlow.setCellFactory(cellFactory);
            return this;
        }

        public Builder<T, C> setOverscan(int overscan) {
            virtualFlow.setOverscan(NumberUtils.clamp(overscan, 0, Integer.MAX_VALUE));
            return this;
        }

        public SimpleVirtualFlow<T, C> build() {
            checkParameters();
            return virtualFlow;
        }

        private void checkParameters() {
            if (virtualFlow.getItems() == null) {
                throw new IllegalStateException("Items list not set!");
            }
            if (virtualFlow.getCellFactory() == null) {
                throw new IllegalStateException("Cell factory not set!");
            }
        }
    }
}
