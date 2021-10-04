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

import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.flow.base.OrientationHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * Helper class to keep track of some parameters like:
 * <p> - the amount of pixels scrolled down/right in the viewport
 * <p> - the estimated height property, {@link #estimatedHeightProperty()}
 * <p> - the estimated width property, {@link #estimatedWidthProperty()}
 * <p> - The cells fixes size (both height and width)
 *
 * @param <T> the type of objects to represent
 * @param <C> the type of Cell to use
 */
public class LayoutManager<T, C extends Cell<T>> {
    //================================================================================
    // Properties
    //================================================================================
    private final SimpleVirtualFlow<T, C> virtualFlow;
    private final SimpleVirtualFlowContainer<T, C> container;
    private boolean initialized;

    private double scrolled;
    private final DoubleProperty estimatedHeight = new SimpleDoubleProperty();
    private final DoubleProperty estimatedWidth = new SimpleDoubleProperty();
    private double cellHeight;
    private double cellWidth;

    private DoubleBinding heightBinding;
    private DoubleBinding widthBinding;

    //================================================================================
    // Constructors
    //================================================================================
    public LayoutManager(SimpleVirtualFlowContainer<T, C> container) {
        this.virtualFlow = container.getVirtualFlow();
        this.container = container;
    }

    //================================================================================
    // Initialization
    //================================================================================

    /**
     * Gets the first item from the items list and builds a cell from it.
     * This is needed to get the Cell's fixed height and width values.
     * Then calls {@link #initFlow()}.
     * <p>
     * If the list is empty the method exits immediately, when
     * the list property changes or the current list is not empty anymore
     * the {@link SimpleVirtualFlowContainer} will tell the LayoutManager to initialize.
     */
    protected void initialize() {
        if (virtualFlow.getItems().isEmpty()) {
            return;
        }

        C cell = virtualFlow.getCellFactory().apply(virtualFlow.getItems().get(0));
        retrieveCellsSizes(cell);
        initFlow();
        setInitialized(true);
    }

    /**
     * Calls {@link #bindings()} and {@link CellsManager#initCells(int)},
     * the num of cells passed is computed by calling {@link OrientationHelper#computeCellsNumber()}.
     */
    protected void initFlow() {
        bindings();
        int num = virtualFlow.getOrientationHelper().computeCellsNumber();
        container.initCells(num);
    }

    /**
     * Unbinds the estimatedHeight and estimatedWidth properties, then creates the bindings
     * and finally re-binds the properties to the new created bindings.
     * <p></p>
     * This must be done every time the items list property changes since these bindings
     * listen for changes in the items list.
     */
    private void bindings() {
        estimatedHeight.unbind();
        estimatedHeight.unbind();

        heightBinding = Bindings.createDoubleBinding(
                () -> virtualFlow.getOrientationHelper().computeEstimatedHeight(cellHeight),
                virtualFlow.getItems(), virtualFlow.heightProperty(), virtualFlow.orientationProperty()
        );
        widthBinding = Bindings.createDoubleBinding(
                () -> virtualFlow.getOrientationHelper().computeEstimatedWidth(cellWidth),
                virtualFlow.getItems(), virtualFlow.widthProperty(), virtualFlow.orientationProperty()
        );

        estimatedHeight.bind(heightBinding);
        estimatedWidth.bind(widthBinding);
    }

    /**
     * Called on scroll to update the amount of pixels scrolled and
     * then calls {@link CellsManager#updateCells(int)},
     * the passed start index is computed by {@link #firstVisible()}.
     */
    public void update(double scrolled) {
        this.scrolled = scrolled;
        int start = firstVisible();
        container.updateCells(start);
    }

    /**
     * Delegate method of {@link OrientationHelper#firstVisible()}.
     */
    public int firstVisible() {
        return virtualFlow.getOrientationHelper().firstVisible();
    }

    /**
     * Delegate method of {@link OrientationHelper#lastVisible()}.
     */
    public int lastVisible() {
        return virtualFlow.getOrientationHelper().lastVisible();
    }

    /**
     * Called during initialization to compute the sizes of a Cell
     * before it's laid out.
     * <p>
     * This is done by adding the cell to a {@link Group} and then
     * the group is set as the root of a new {@link Scene}.
     * <p>
     * By calling {@link Parent#applyCss()} and {@link Parent#layout()},
     * the cell will be properly resized, and we can get its sizes with
     * {@link OrientationHelper#getHeight(Node)} and {@link OrientationHelper#getWidth(Node)}.
     */
    private void retrieveCellsSizes(C cell) {
        Node node = cell.getNode();
        Group group = new Group();
        group.getChildren().add(node);
        new Scene(group);
        group.applyCss();
        group.layout();
        cellHeight = virtualFlow.getOrientationHelper().getHeight(node);
        cellWidth = virtualFlow.getOrientationHelper().getWidth(node);
    }

    /**
     * @return the sum of all cells' height
     */
    public double getEstimatedHeight() {
        return estimatedHeight.get();
    }

    /**
     * The total height property, the sum of all cells' height.
     * This is needed by the VirtualFlow to properly compute the
     * scroll bars max values.
     */
    public DoubleProperty estimatedHeightProperty() {
        return estimatedHeight;
    }

    /**
     * @return the sum of all cells' width
     */
    public double getEstimatedWidth() {
        return estimatedWidth.get();
    }

    /**
     * The total width property, the sum of all cells' width.
     * This is needed by the VirtualFlow to properly compute the
     * scroll bars max values.
     */
    public DoubleProperty estimatedWidthProperty() {
        return estimatedWidth;
    }

    /**
     * @return the fixed Cells height
     */
    public double getCellHeight() {
        return cellHeight;
    }

    /**
     * @return the fixed Cells width
     */
    public double getCellWidth() {
        return cellWidth;
    }

    /**
     * @return the amount of scrolled pixels
     */
    public double getScrolled() {
        return scrolled;
    }

    /**
     * @return the init state of the LayoutManager
     */
    protected boolean isInitialized() {
        return initialized;
    }

    /**
     * Sets the init state of the LayoutManager
     */
    protected void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
}
