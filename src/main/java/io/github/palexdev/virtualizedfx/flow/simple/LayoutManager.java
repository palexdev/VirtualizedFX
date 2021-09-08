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
import io.github.palexdev.virtualizedfx.beans.NumberRangeProperty;
import io.github.palexdev.virtualizedfx.cell.ISimpleCell;
import io.github.palexdev.virtualizedfx.enums.Gravity;
import io.github.palexdev.virtualizedfx.utils.ExecutionUtils;
import io.github.palexdev.virtualizedfx.utils.NumberUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Helper class to keep track of some parameters like:
 * <p> - The range of cells to build (which are the first visible cell index minus the overscan and the last visible cell index plus the overscan,
 * of course the values are clamped to never be negative or exceed the items list size - 1).
 * <p> - The container bounds which are the current layoutX/layoutY (depends on the orientation) and the totalWidth/totalHeight plus the layout value.
 * <p> - The total height property, {@link #totalHeightProperty()}.
 * <p> - The total width property, {@link #totalWidthProperty()}.
 * <p> - The {@link ISimpleCell} fixed sizes.
 *
 * @param <T> the type of objects to represent
 * @param <C> the type of Cell to use
 */
public class LayoutManager<T, C extends ISimpleCell> {
    //================================================================================
    // Properties
    //================================================================================
    private final SimpleVirtualFlow<T, C> virtualFlow;
    private final NumberRangeProperty<Integer> indexes = new NumberRangeProperty<>();
    private final NumberRangeProperty<Double> containerBounds = new NumberRangeProperty<>();
    private final DoubleProperty totalHeight = new SimpleDoubleProperty();
    private final DoubleProperty totalWidth = new SimpleDoubleProperty();
    private double cellHeight;
    private double cellWidth;

    private DoubleBinding heightBinding;
    private DoubleBinding widthBinding;
    private final ObjectBinding<NumberRange<Double>> boundsBinding;

    //================================================================================
    // Constructors
    //================================================================================
    public LayoutManager(SimpleVirtualFlow<T, C> virtualFlow) {
        this.virtualFlow = virtualFlow;
        heightBinding = Bindings.createDoubleBinding(
                () -> (virtualFlow.getGravity() == Gravity.TOP_BOTTOM) ? cellHeight * virtualFlow.getItems().size() : virtualFlow.getHeight(),
                virtualFlow.getItems(), virtualFlow.heightProperty(), virtualFlow.gravityProperty()
        );
        widthBinding = Bindings.createDoubleBinding(
                () -> (virtualFlow.getGravity() == Gravity.TOP_BOTTOM) ? virtualFlow.getWidth() : (cellWidth * virtualFlow.getItems().size()),
                virtualFlow.getItems(), virtualFlow.widthProperty(), virtualFlow.gravityProperty()
        );
        boundsBinding = Bindings.createObjectBinding(
                () -> {
                    double min = (virtualFlow.getGravity() == Gravity.TOP_BOTTOM) ? -virtualFlow.getVerticalPosition() : -virtualFlow.getHorizontalPosition();
                    double max = ((virtualFlow.getGravity() == Gravity.TOP_BOTTOM) ? getTotalHeight() : getTotalWidth()) + min;
                    return NumberRange.of(min, max);
                }, virtualFlow.verticalPositionProperty(), virtualFlow.horizontalPositionProperty()
        );
    }

    //================================================================================
    // Initialization
    //================================================================================

    /**
     * Gets the first item from the items list and builds a cell from it.
     * This is needed to get the {@link ISimpleCell} fixed height and width values.
     * Then calls {@link #initFlow()}.
     * <p>
     * If the list is empty the initialization is deferred with
     * {@link ExecutionUtils#executeWhen(ObservableValue, BiConsumer, boolean, BiFunction, boolean)},
     * this will execute the initialization when the list is not empty anymore.
     */
    void initialize() {
        if (virtualFlow.getItems().isEmpty()) {
            ExecutionUtils.executeWhen(
                    virtualFlow.itemsProperty(),
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

    /**
     * Adds two listeners:
     * <p> One to the VirtualFlow's layout bounds property, because if its sizes change
     * it's needed to {@link CellsManager#recomputeLayout()} and re-compute the indexes, {@link #computeIndexes()}.
     * <p>
     * Also calls {@link #bindings()}.
     */
    private void initFlow() {
        virtualFlow.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            virtualFlow.container.cellsManger.recomputeLayout();
            computeIndexes();
        });
        containerBounds.addListener((observable, oldValue, newValue) -> computeIndexes());
        bindings();
    }

    /**
     * Re-initializes the layout manager by unbinding every property and re-binding them again.
     */
    void reinitialize() {
        totalHeight.unbind();
        totalWidth.unbind();
        containerBounds.unbind();
        bindings();
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Responsible for computing the range of cells to build.
     * <p>
     * The minimum is computed like this: {@link #firstVisible()} - overscan
     * <p>
     * The maximum is computed like this: {@link #lastVisible()} + overscan
     * <p>
     * All the values are clamped between 0 and the items list size - 1.
     * <p></p>
     * At the end calls {@link CellsManager#updateContent()}.
     */
    void computeIndexes() {
        //if (!overscanReached()) return;
        int min = NumberUtils.clamp(firstVisible() - virtualFlow.getOverscan(), 0, virtualFlow.getItems().size() - 1);
        int max = NumberUtils.clamp(lastVisible() + virtualFlow.getOverscan(), 0, virtualFlow.getItems().size() - 1);
        setIndexes(NumberRange.of(min, max));
        virtualFlow.container.cellsManger.updateContent();
    }

    /**
     * Checks if the overscan index is reached.
     * Currently, unused.
     */
    private boolean overscanReached() {
        if (getIndexes() == null) return true;
        NumberRange<Integer> layoutIndexes = getIndexes();
        return firstVisible() < layoutIndexes.getMin() || lastVisible() > layoutIndexes.getMax();
    }

    /**
     * Returns the first visible cell index, computed by dividing the absolute
     * value of the container's upper bound for the cellHeight/cellWidth (depending on orientation).
     * The end value is wrapped in {@link Math#floor(double)} and cast to int.
     */
    private int firstVisible() {
        double absMin = Math.abs(getContainerBounds().getMin());
        return (virtualFlow.getGravity() == Gravity.TOP_BOTTOM) ? (int) Math.floor(absMin / cellHeight) : (int) Math.floor(absMin / cellWidth);
    }

    /**
     * Returns the last visible cell index, computed by dividing the absolute
     * value of the container's upper bound plus the VirtualFlow's height/width (depending on orientation)
     * for the cellHeight/cellWidth (depending on orientation).
     * The end value is wrapped in {@link Math#floor(double)} and cast to int.
     */
    private int lastVisible() {
        double absMin = Math.abs(getContainerBounds().getMin());
        if (virtualFlow.getGravity() == Gravity.TOP_BOTTOM) {
            double max = NumberUtils.clamp(absMin + virtualFlow.getHeight(), 0, getTotalHeight());
            return (int) Math.floor(max / cellHeight);
        } else {
            double max = NumberUtils.clamp(absMin + virtualFlow.getWidth(), 0, getTotalWidth());
            return (int) Math.floor(max / cellWidth);
        }
    }

    /**
     * Establishes the needed binding to compute the totalHeight, the totalWidth and the containerBounds.
     */
    private void bindings() {
        heightBinding = Bindings.createDoubleBinding(
                () -> (virtualFlow.getGravity() == Gravity.TOP_BOTTOM) ? cellHeight * virtualFlow.getItems().size() : virtualFlow.getHeight(),
                virtualFlow.getItems(), virtualFlow.heightProperty(), virtualFlow.gravityProperty()
        );
        widthBinding = Bindings.createDoubleBinding(
                () -> (virtualFlow.getGravity() == Gravity.TOP_BOTTOM) ? virtualFlow.getWidth() : (cellWidth * virtualFlow.getItems().size()),
                virtualFlow.getItems(), virtualFlow.widthProperty(), virtualFlow.gravityProperty()
        );
        totalHeight.bind(heightBinding);
        totalWidth.bind(widthBinding);
        containerBounds.bind(boundsBinding);
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    /**
     * @return the container bounds
     */
    public NumberRange<Double> getContainerBounds() {
        return containerBounds.get();
    }

    /**
     * @return the cells range
     */
    public NumberRange<Integer> getIndexes() {
        return indexes.get();
    }

    /**
     * Sets the cells range.
     */
    public void setIndexes(NumberRange<Integer> indexes) {
        this.indexes.set(indexes);
    }

    /**
     * @return the sum of all cell's height. For {@link ISimpleCell}s which have a fixed
     * height it's simply ({@link ISimpleCell#getFixedHeight()} * the size of the items list)
     */
    public double getTotalHeight() {
        return totalHeight.get();
    }

    /**
     * The total height property. This is needed by the VirtualFlow to properly compute the
     * scroll bars max values.
     */
    public DoubleProperty totalHeightProperty() {
        return totalHeight;
    }

    /**
     * @return the sum of all cell's width. For {@link ISimpleCell}s which have a fixed
     * width it's simply ({@link ISimpleCell#getFixedWidth()} * the size of the items list)
     */
    public double getTotalWidth() {
        return totalWidth.get();
    }

    /**
     * The total width property. This is needed by the VirtualFlow to properly compute the
     * scroll bars max values.
     */
    public DoubleProperty totalWidthProperty() {
        return totalWidth;
    }

    /**
     * @return the fixed {@link ISimpleCell} height
     */
    public double getCellHeight() {
        return cellHeight;
    }

    /**
     * @return the fixed {@link ISimpleCell} width
     */
    public double getCellWidth() {
        return cellWidth;
    }

}
