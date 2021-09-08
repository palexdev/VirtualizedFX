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

import io.github.palexdev.virtualizedfx.ResourceManager;
import io.github.palexdev.virtualizedfx.cell.ISimpleCell;
import io.github.palexdev.virtualizedfx.enums.Gravity;
import io.github.palexdev.virtualizedfx.flow.base.VirtualFlow;
import io.github.palexdev.virtualizedfx.utils.NumberUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventDispatcher;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.Region;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * Simple implementation of {@link VirtualFlow}.
 * <p>
 * This Virtual Flow creates Cells of type {@link ISimpleCell}, it's features are:
 * <p> - Can prebuild and show a certain number of extra cells above and below the viewport, this is the overscan property
 * <p> - Can show the cells from TOP to BOTTOM or from LEFT to RIGHT, this is the gravity property
 * <p> - It's not necessary tp wrap the flow in a scroll pane as it already includes both the scroll bars
 * <p> - It's possible to set the speed of both the scroll bars
 * <p> - It's possible to scroll manually by pixels or to cell index
 * <p> - It's possible to get the currently shown/built cells or a specific cell by index
 * <p></p>
 * To build a SimpleVirtualFlow use the {@link Builder} class.
 * <p></p>
 * The cells are contained in a {@link Group} which is a {@link SimpleVirtualFlowContainer}.
 *
 * @param <T> the type of objects to represent
 * @param <C> the type of Cell to use
 */
public class SimpleVirtualFlow<T, C extends ISimpleCell> extends Region implements VirtualFlow<T, C> {
    //================================================================================
    // Properties
    //================================================================================
    private final ObjectProperty<ObservableList<T>> items = new SimpleObjectProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<Function<T, C>> cellFactory = new SimpleObjectProperty<>();
    private int overscan;
    private final ObjectProperty<Gravity> gravity = new SimpleObjectProperty<>();

    private final ScrollBar hBar = new ScrollBar();
    private final DoubleProperty horizontalPosition = new SimpleDoubleProperty();
    private final ScrollBar vBar = new ScrollBar();
    private final DoubleProperty verticalPosition = new SimpleDoubleProperty();

    final SimpleVirtualFlowContainer<T, C> container = new SimpleVirtualFlowContainer<>(this);

    //================================================================================
    // Constructors
    //================================================================================
    protected SimpleVirtualFlow() {}

    //================================================================================
    // Initialization
    //================================================================================

    /**
     * Adds the container to the children list, calls {@link #setupScrollBars()}.
     * <p>
     * Also adds listeners for the cellFactory property and the gravity property.
     */
    private void initialize() {
        getStyleClass().add("virtual-flow");
        getChildren().add(container);
        setupScrollBars();
        cellFactory.addListener((observable, oldValue, newValue) -> {
            container.cellsManger.clear();
            scrollTo(0);
            container.layoutManager.computeIndexes();
        });
        gravity.addListener(invalidated -> {
            container.cellsManger.clear();
            container.layoutManager.computeIndexes();
        });
    }

    /**
     * Sets up the scroll bars. Adds a style class for both: "vbar" for the vertical
     * and "hbar" for the horizontal. By the default the unit increment of both bars is set to 15.
     * Redirects any ScrollEvent to the scroll bars by modifying the {@link EventDispatcher} and
     * then adds the scroll bars to the children list.
     */
    protected void setupScrollBars() {
        hBar.getStyleClass().add("hbar");
        hBar.setManaged(false);
        hBar.setOrientation(Orientation.HORIZONTAL);
        hBar.setUnitIncrement(15);
        hBar.maxProperty().bind(Bindings.createDoubleBinding(
                () -> {
                    double val = snapSpaceX(container.getTotalWidth() - getWidth());
                    double currScroll = getHorizontalPosition();
                    if (currScroll < 0 || currScroll > val) {
                        setHorizontalPosition(NumberUtils.clamp(currScroll, 0, val));
                    }
                    return val;
                },
                container.totalWidthProperty(), widthProperty(), gravityProperty()
        ));
        hBar.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> container.getTotalWidth() > getWidth(),
                container.totalWidthProperty(), widthProperty()
        ));
        horizontalPositionProperty().bindBidirectional(hBar.valueProperty());
        horizontalPositionProperty().addListener((observable, oldValue, newValue) -> container.setLayoutX(-newValue.doubleValue()));

        vBar.getStyleClass().add("vbar");
        vBar.setManaged(false);
        vBar.setOrientation(Orientation.VERTICAL);
        vBar.setUnitIncrement(15);
        vBar.maxProperty().bind(Bindings.createDoubleBinding(
                () -> {
                    double val = snapSpaceY(container.getTotalHeight() - getHeight());
                    double currScroll = getVerticalPosition();
                    if (currScroll < 0 || currScroll > val) {
                        setVerticalPosition(NumberUtils.clamp(currScroll, 0, val));
                    }
                    return val;
                },
                container.totalHeightProperty(), heightProperty(), gravityProperty()
        ));
        vBar.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> container.getTotalHeight() > getHeight(),
                container.totalHeightProperty(), heightProperty()
        ));
        verticalPositionProperty().bindBidirectional(vBar.valueProperty());
        verticalPositionProperty().addListener((observable, oldValue, newValue) -> container.setLayoutY(-newValue.doubleValue()));

        EventDispatcher original = getEventDispatcher();
        setEventDispatcher((event, tail) -> {
            tail.prepend(vBar.getEventDispatcher());
            tail.prepend(hBar.getEventDispatcher());
            return original.dispatchEvent(event, tail);
        });
        getChildren().addAll(hBar, vBar);
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * @return the cell at the specified index, null if not found
     */
    public C getCell(int index) {
        try {
            return container.cellsManger.getCells().get(index);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * @return all the currently built cells
     */
    public Map<Integer, C> getCells() {
        return Collections.unmodifiableMap(container.cellsManger.getCells());
    }

    /**
     * Scrolls by the given amount of pixels.
     */
    public void scrollBy(double pixels) {
        if (getGravity() == Gravity.TOP_BOTTOM) {
            double baseVal = getVerticalPosition();
            double val = NumberUtils.clamp(baseVal + pixels, 0, vBar.getMax());
            setVerticalPosition(val);
        } else {
            double baseVal = getHorizontalPosition();
            double val = NumberUtils.clamp(baseVal + pixels, 0, hBar.getMax());
            setHorizontalPosition(val);
        }
    }

    /**
     * Scrolls to the first cell.
     */
    public void scrollToFirst() {
        scrollTo(0);
    }

    /**
     * Scrolls to the last cell.
     */
    public void scrollToLast() {
        scrollTo(getItems().size() - 1);
    }

    /**
     * Scrolls to the given cell index.
     */
    public void scrollTo(int index) {
        if (getGravity() == Gravity.TOP_BOTTOM) {
            setVerticalPosition(NumberUtils.clamp(container.layoutManager.getCellHeight() * index, 0, vBar.getMax()));
        } else {
            setHorizontalPosition(NumberUtils.clamp(container.layoutManager.getCellWidth() * index, 0, hBar.getMax()));
        }
    }

    /**
     * Scrolls to the given pixel value.
     */
    public void scrollToPixel(double pixel) {
        if (getGravity() == Gravity.TOP_BOTTOM) {
            setVerticalPosition(NumberUtils.clamp(pixel, 0, vBar.getMax()));
        } else {
            setHorizontalPosition(NumberUtils.clamp(pixel, 0, hBar.getMax()));
        }
    }

    /**
     * Sets the horizontal scroll bar speeds.
     */
    public void setHSpeed(double unit, double block) {
        hBar.setUnitIncrement(unit);
        hBar.setBlockIncrement(block);
    }

    /**
     * Sets the vertical scroll bar speeds.
     */
    public void setVSpeed(double unit, double block) {
        vBar.setUnitIncrement(unit);
        vBar.setBlockIncrement(block);
    }

    //================================================================================
    // Override Methods
    //================================================================================

    @Override
    public String getUserAgentStylesheet() {
        return ResourceManager.loadResource("SimpleVirtualFlow.css");
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        //container.resize(getWidth(), getHeight());
        double prefVerticalWidth = vBar.getPrefWidth();
        double prefHorizontalHeight = hBar.getPrefHeight();
        vBar.resizeRelocate(getWidth() - vBar.getWidth(), 0, prefVerticalWidth, getHeight());
        hBar.resizeRelocate(0, getHeight() - hBar.getHeight(), getWidth(), prefHorizontalHeight);
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    /**
     * {@inheritDoc}
     */
    public ObservableList<T> getItems() {
        return items.get();
    }

    /**
     * Property for the items list.
     */
    public ObjectProperty<ObservableList<T>> itemsProperty() {
        return items;
    }

    /**
     * {@inheritDoc}
     */
    public void setItems(ObservableList<T> items) {
        this.items.set(items);
    }

    /**
     * {@inheritDoc}
     */
    public Function<T, C> getCellFactory() {
        return cellFactory.get();
    }

    /**
     * {@inheritDoc}
     */
    public ObjectProperty<Function<T, C>> cellFactoryProperty() {
        return cellFactory;
    }

    /**
     * {@inheritDoc}
     */
    public void setCellFactory(Function<T, C> cellFactory) {
        this.cellFactory.set(cellFactory);
    }

    /**
     * @return the number of extra cells to build
     */
    public int getOverscan() {
        return overscan;
    }

    /**
     * Sets the number of extra cells to build.
     */
    public void setOverscan(int overscan) {
        this.overscan = overscan;
    }

    /**
     * @return the orientation of the VirtualFlow
     */
    public Gravity getGravity() {
        return gravity.get();
    }

    /**
     * The orientation property of the VirtualFlow.
     */
    public ObjectProperty<Gravity> gravityProperty() {
        return gravity;
    }

    /**
     * Sets the orientation of the VirtualFlow.
     */
    public void setGravity(Gravity gravity) {
        this.gravity.set(gravity);
    }

    /**
     * @return the vertical scroll bar's value
     */
    public double getVerticalPosition() {
        return verticalPosition.get();
    }

    /**
     * Property for the vertical scroll bar's value.
     */
    public DoubleProperty verticalPositionProperty() {
        return verticalPosition;
    }

    /**
     * Sets the vertical scroll bar's value
     */
    public void setVerticalPosition(double vValue) {
        this.verticalPosition.set(vValue);
    }

    /**
     * @return the horizontal scroll bar's value
     */
    public double getHorizontalPosition() {
        return horizontalPosition.get();
    }

    /**
     * Property for the horizontal scroll bar's value.
     */
    public DoubleProperty horizontalPositionProperty() {
        return horizontalPosition;
    }

    /**
     * Sets the horizontal scroll bar's value
     */
    public void setHorizontalPosition(double hValue) {
        this.horizontalPosition.set(hValue);
    }

    //================================================================================
    // Builder
    //================================================================================

    /**
     * Builder class to create {@link SimpleVirtualFlow}s.
     */
    public static class Builder {

        private Builder() {
        }

        /**
         * @param items       The items list
         * @param cellFactory The function to convert items to cells
         * @param gravity     The orientation
         * @param <T>         The type of objects
         * @param <C>         The type of cells
         */
        public static <T, C extends ISimpleCell> SimpleVirtualFlow<T, C> create(ObservableList<T> items, Function<T, C> cellFactory, Gravity gravity) {
            return create(items, cellFactory, gravity, 0);
        }

        /**
         * @param items       The items list
         * @param cellFactory The function to convert items to cells
         * @param gravity     The orientation
         * @param overscan    The number of extra cells to build
         * @param <T>         The type of objects
         * @param <C>         The type of cells
         */
        public static <T, C extends ISimpleCell> SimpleVirtualFlow<T, C> create(ObservableList<T> items, Function<T, C> cellFactory, Gravity gravity, int overscan) {
            SimpleVirtualFlow<T, C> virtualFlow = new SimpleVirtualFlow<>();
            virtualFlow.setItems(items);
            virtualFlow.setCellFactory(cellFactory);
            virtualFlow.setGravity(gravity);
            virtualFlow.overscan = overscan;
            virtualFlow.initialize();
            return virtualFlow;
        }
    }
}
