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
import io.github.palexdev.virtualizedfx.flow.base.VirtualFlow;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Map;

/**
 * This is the {@link Region} used by {@link SimpleVirtualFlow} to contain the cells.
 * <p>
 * To keep things easy and organized this container makes use of two helper classes, the {@link CellsManager} and {@link LayoutManager},
 * and also acts as a bridge between those two helpers and the VirtualFlow.
 * <p></p>
 * Since it keeps references to the CellsManager and the LayoutManager too, it's also responsible for listening to changes to the items list,
 * and the items list property.
 *
 * @param <T> the type of object to represent
 * @param <C> the type of Cell to use
 */
public class SimpleVirtualFlowContainer<T, C extends Cell<T>> extends Region {
    //================================================================================
    // Properties
    //================================================================================
    private final SimpleVirtualFlow<T, C> virtualFlow;
    private final CellsManager<T, C> cellsManager;
    private final LayoutManager<T, C> layoutManager;

    private final ChangeListener<? super ObservableList<T>> listChanged;
    private final ListChangeListener<? super T> itemsChanged;

    //================================================================================
    // Constructors
    //================================================================================
    public SimpleVirtualFlowContainer(SimpleVirtualFlow<T, C> virtualFlow) {
        this.virtualFlow = virtualFlow;
        this.cellsManager = new CellsManager<>(this);
        this.layoutManager = new LayoutManager<>(this);

        itemsChanged = c -> cellsManager.itemsChanged();
        listChanged = (observable, oldValue, newValue) -> {
            cellsManager.clear();
            virtualFlow.scrollToPixel(0.0);
            oldValue.removeListener(itemsChanged);
            newValue.addListener(itemsChanged);
            layoutManager.initFlow();
        };
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Calls {@link #buildClip()}, adds the needed listeners for the items list and the corresponding property,
     * also calls {@link LayoutManager#initialize()}.
     */
    protected void initialize() {
        buildClip();
        virtualFlow.getItems().addListener(itemsChanged);
        virtualFlow.itemsProperty().addListener(listChanged);
        layoutManager.initialize();
    }

    /**
     * Builds and sets the container's clip to hide the cells outside the viewport.
     * The clip not only has its width and height bound to the VirtualFlow's ones but also
     * the layoutX and layoutY properties bound to the VirtualFlow's horizontal and vertical positions,
     * {@link VirtualFlow#horizontalPositionProperty()}, {@link VirtualFlow#verticalPositionProperty()}.
     */
    private void buildClip() {
        Rectangle clip = new Rectangle();
        clip.heightProperty().bind(virtualFlow.heightProperty());
        clip.widthProperty().bind(virtualFlow.widthProperty());
        clip.layoutXProperty().bind(layoutXProperty().multiply(-1));
        clip.layoutYProperty().bind(layoutYProperty().multiply(-1));
        setClip(clip);
    }

    //================================================================================
    // Delegate Methods
    //================================================================================

    /**
     * Delegate method for {@link CellsManager#initCells(int)}.
     */
    protected void initCells(int num) {
        cellsManager.initCells(num);
    }

    /**
     * Delegate method for {@link CellsManager#updateCells(int, int)}.
     */
    protected void updateCells(int start, int end) {
        cellsManager.updateCells(start, end);
    }

    /**
     * Delegate method for {@link LayoutManager#update(double)}.
     */
    public void update(double scrolled) {
        layoutManager.update(scrolled);
    }

    /**
     * Delegate method for {@link CellsManager#getCells()}.
     */
    protected Map<Integer, C> getCells() {
        return cellsManager.getCells();
    }

    /**
     * Delegate method for {@link LayoutManager#getEstimatedHeight()}.
     */
    protected double getEstimatedHeight() {
        return layoutManager.getEstimatedHeight();
    }

    /**
     * Delegate method for {@link LayoutManager#estimatedHeightProperty()}.
     */
    protected DoubleProperty estimatedHeightProperty() {
        return layoutManager.estimatedHeightProperty();
    }

    /**
     * Delegate method for {@link LayoutManager#getEstimatedWidth()}.
     */
    protected double getEstimatedWidth() {
        return layoutManager.getEstimatedWidth();
    }

    /**
     * Delegate method for {@link LayoutManager#estimatedWidthProperty()}.
     */
    protected DoubleProperty estimatedWidthProperty() {
        return layoutManager.estimatedWidthProperty();
    }

    /**
     * Delegate method for {@link LayoutManager#getCellHeight()}.
     */
    public double getCellHeight() {
        return layoutManager.getCellHeight();
    }

    /**
     * Delegate method for {@link LayoutManager#getCellWidth()}.
     */
    public double getCellWidth() {
        return layoutManager.getCellWidth();
    }

    /**
     * Delegate method for {@link LayoutManager#getScrolled()}.
     */
    public double getScrolled() {
        return layoutManager.getScrolled();
    }

    //================================================================================
    // Getters
    //================================================================================

    /**
     * @return the VirtualFlow instance
     */
    protected SimpleVirtualFlow<T, C> getVirtualFlow() {
        return virtualFlow;
    }

    /**
     * @return the CellsManager instance
     */
    protected CellsManager<T, C> getCellsManager() {
        return cellsManager;
    }

    /**
     * @return the LayoutManager instance
     */
    protected LayoutManager<T, C> getLayoutManager() {
        return layoutManager;
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /**
     * {@inheritDoc}
     * <p></p>
     * Overridden to be accessible in this package.
     *
     * @return
     */
    @Override
    protected ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    /**
     * Overridden to be empty. Layout is managed by {@link CellsManager#processLayout(List)}.
     */
    @Override
    protected void layoutChildren() {
    }
}
