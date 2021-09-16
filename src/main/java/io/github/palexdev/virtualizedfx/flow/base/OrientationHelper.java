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

package io.github.palexdev.virtualizedfx.flow.base;

import io.github.palexdev.virtualizedfx.flow.simple.SimpleVirtualFlowContainer;
import io.github.palexdev.virtualizedfx.utils.NumberUtils;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;

/**
 * Helper class to avoid as much as possible if/else statements to check
 * the VirtualFlow orientation.
 */
public interface OrientationHelper {

    //================================================================================
    // Layout
    //================================================================================

    /**
     * @return the index of the first visible cell in the viewport
     */
    int firstVisible();

    /**
     * @return the index of the last visible cell in the viewport
     */
    int lastVisible();

    /**
     * Computes the estimated height of the flow.
     */
    double computeEstimatedHeight(double cellHeight);

    /**
     * Computes the estimated width of the flow.
     */
    double computeEstimatedWidth(double cellWidth);

    /**
     * @return the height of the given node
     */
    double getHeight(Node node);

    /**
     * @return the width of the given node
     */
    double getWidth(Node node);

    /**
     * Resizes and relocates the given node with the given parameters.
     */
    void resizeRelocate(Node node, double pos, double w, double h);

    /**
     * Lays out the given node.
     */
    void layout(Node node, int index, double cellW, double cellH);

    /**
     * Computes size of a Node between its pref, min and max sizes.
     * <p></p>
     * The formula is:
     * <p>
     * <pre>
     * {@code
     *     double a = Math.max(pref, min);
     *     double b = Math.max(min, max);
     *     return Math.min(a, b);
     * }
     * </pre>
     * <p>
     */
    static double boundSize(double pref, double min, double max) {
        double a = Math.max(pref, min);
        double b = Math.max(min, max);
        return Math.min(a, b);
    }

    //================================================================================
    // Scrolling
    //================================================================================

    /**
     * Scrolls the flow by the given amount of pixels.
     */
    void scrollBy(double pixels);

    /**
     * Scrolls the flow to the given cell index.
     */
    void scrollTo(int index);

    /**
     * Scrolls the flow to the first cell.
     */
    void scrollToFirst();

    /**
     * Scrolls the flow to the last cell.
     */
    void scrollToLast();

    /**
     * Scrolls the flow to the given pixel value.
     */
    void scrollToPixel(double pixel);

    //================================================================================
    // Others
    //================================================================================

    /**
     * Removes and clears any listener.
     * <p>
     * This must be called every time the OrientationHelper of the VirtualFlow
     * is changed (typically occurs when the orientation changes).
     */
    void dispose();


    //================================================================================
    // Implementations
    //================================================================================

    /**
     * Implementation of {@link OrientationHelper} for {@link Orientation#HORIZONTAL}.
     * <p>
     * This helper is also responsible for listening to changes to the VirtualFlow's width,
     * because in such cases it's needed to recompute the number of cells needed (to add or remove),
     * and changes to the {@link VirtualFlow#horizontalPositionProperty()} to update the container's layoutX
     * property and call {@link SimpleVirtualFlowContainer#update(double)} with the new position.
     * <p></p>
     * Note that these listeners are added late during VirtualFlow's initialization as adding them already in the constructor
     * would lead to an infinite loop and RAM eating for some reason.
     *
     * @see OrientationHelper
     */
    class HorizontalHelper implements OrientationHelper {
        //================================================================================
        // Properties
        //================================================================================
        private final VirtualFlow<?, ?> virtualFlow;
        private final SimpleVirtualFlowContainer<?, ?> container;

        private ChangeListener<? super Number> widthChanged;
        private ChangeListener<? super Number> posChanged;

        //================================================================================
        // Constructors
        //================================================================================
        public HorizontalHelper(VirtualFlow<?, ?> virtualFlow, SimpleVirtualFlowContainer<?, ?> container) {
            this.virtualFlow = virtualFlow;
            this.container = container;

            this.widthChanged = (observable, oldValue, newValue) -> container.update(virtualFlow.getHorizontalPosition());
            this.posChanged = (observable, oldValue, newValue) -> {
                container.setLayoutX(-newValue.doubleValue());
                container.update(newValue.doubleValue());
            };
        }

        //================================================================================
        // Initialization
        //================================================================================

        // TODO investigate more

        /**
         * DO NOT CALL THIS METHOD, it's automatically handled by the VirtualFlow.
         */
        public void initialize() {
            virtualFlow.getVirtualFlow().widthProperty().addListener(widthChanged);
            virtualFlow.horizontalPositionProperty().addListener(posChanged);
        }

        //================================================================================
        // Implemented Methods
        //================================================================================

        /**
         * Computes the first visible cell index in the viewport with this formula:
         * <p>
         * {@code (int) Math.floor(scrolled / cellWidth)}
         * <p>
         * The scrolled parameter is the amount of pixels scrolled from the left
         * and cellWidth is the fixed cells' width.
         * <p>
         * The result is clamped between 0 and the number of items -1.
         */
        @Override
        public int firstVisible() {
            return NumberUtils.clamp(
                    (int) Math.floor(container.getScrolled() / container.getCellWidth()),
                    0,
                    virtualFlow.getItems().size() - 1
            );
        }

        /**
         * Computes the first visible cell index in the viewport with this formula:
         * <p>
         * {@code (int) Math.ceil((scrolled + virtualFlowWidth) / cellWidth - 1)}
         * <p>
         * The scrolled parameter is the amount of pixels scrolled from the left,
         * the virtualFlowWidth well... is self-explanatory, and cellWidth is the fixed cells' width.
         * <p>
         * The result is clamped between 0 and the number of items -1.
         */
        @Override
        public int lastVisible() {
            return NumberUtils.clamp(
                    (int) Math.ceil((container.getScrolled() + virtualFlow.getVirtualFlow().getWidth()) / container.getCellWidth() - 1),
                    0,
                    virtualFlow.getItems().size() - 1
            );
        }

        /**
         * @return the VirtualFlow's height
         */
        @Override
        public double computeEstimatedHeight(double cellHeight) {
            return virtualFlow.getVirtualFlow().getHeight();
        }

        /**
         * @return the given cellWidth multiplied by the number of items
         */
        @Override
        public double computeEstimatedWidth(double cellWidth) {
            return virtualFlow.getItems().size() * cellWidth;
        }

        /**
         * If the Node's maxHeight is set to {@link Double#MAX_VALUE} then returns
         * the VirtualFlow's height, otherwise calls {@link #boundSize(double, double, double)}.
         */
        @Override
        public double getHeight(Node node) {
            double max = node.maxHeight(-1);
            return max == Double.MAX_VALUE ?
                    virtualFlow.getVirtualFlow().getHeight() :
                    boundSize(node.prefHeight(-1), node.minHeight(-1), max);
        }

        /**
         * @return the Node's pref width
         */
        @Override
        public double getWidth(Node node) {
            return node.prefWidth(-1);
        }

        /**
         * Resizes and relocated the given node with the given parameters,
         * the y offset is always 0.
         *
         * @param node the Node to resize and relocate
         * @param pos  the x position
         * @param w    the width
         * @param h    the height
         */
        @Override
        public void resizeRelocate(Node node, double pos, double w, double h) {
            node.resizeRelocate(pos, 0, w, h);
        }

        /**
         * Computes the Node's x position and then calls {@link #resizeRelocate(Node, double, double, double)}.
         */
        @Override
        public void layout(Node node, int index, double cellW, double cellH) {
            double pos = index * cellW;
            resizeRelocate(node, pos, cellW, cellH);
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Gets the current horizontal position and adds the given amount of
         * pixels to it. The result is clamped between 0 and the hBar's max property.
         */
        @Override
        public void scrollBy(double pixels) {
            ScrollBar hBar = virtualFlow.getHBar();
            double currVal = hBar.getValue();
            double newVal = NumberUtils.clamp(currVal + pixels, 0, hBar.getMax());
            virtualFlow.setHorizontalPosition(newVal);
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Computes the pixel value by multiplying the given index for the fixed cells' width,
         * then calls {@link #scrollToPixel(double)} with the result.
         */
        @Override
        public void scrollTo(int index) {
            scrollToPixel(container.getCellWidth() * index);
        }

        /**
         * Calls {@link #scrollTo(int)} with 0 as argument.
         */
        @Override
        public void scrollToFirst() {
            scrollTo(0);
        }

        /**
         * Calls {@link #scrollTo(int)} with items size - 1 as argument.
         */
        @Override
        public void scrollToLast() {
            scrollTo(virtualFlow.getItems().size() - 1);
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * The given pixel value is clamped between 0 and the hBar's max property before.
         */
        @Override
        public void scrollToPixel(double pixel) {
            ScrollBar hBar = virtualFlow.getHBar();
            double val = NumberUtils.clamp(pixel, 0, hBar.getMax());
            virtualFlow.setHorizontalPosition(val);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose() {
            virtualFlow.getVirtualFlow().widthProperty().removeListener(widthChanged);
            virtualFlow.horizontalPositionProperty().removeListener(posChanged);
            widthChanged = null;
            posChanged = null;
        }
    }

    /**
     * Implementation of {@link OrientationHelper} for {@link Orientation#VERTICAL}.
     * <p>
     * This helper is also responsible for listening to changes to the VirtualFlow's height,
     * because in such cases it's needed to recompute the number of cells needed (to add or remove),
     * and changes to the {@link VirtualFlow#verticalPositionProperty()} to update the container's layoutY
     * property and call {@link SimpleVirtualFlowContainer#update(double)} with the new position.
     * <p></p>
     * Note that these listeners are added late during VirtualFlow's initialization as adding them already in the constructor
     * would lead to an infinite loop and RAM eating for some reason.
     *
     * @see OrientationHelper
     */
    class VerticalHelper implements OrientationHelper {
        //================================================================================
        // Properties
        //================================================================================
        private final VirtualFlow<?, ?> virtualFlow;
        private final SimpleVirtualFlowContainer<?, ?> container;

        private ChangeListener<? super Number> heightChanged;
        private ChangeListener<? super Number> posChanged;

        //================================================================================
        // Constructors
        //================================================================================
        public VerticalHelper(VirtualFlow<?, ?> virtualFlow, SimpleVirtualFlowContainer<?, ?> container) {
            this.virtualFlow = virtualFlow;
            this.container = container;

            this.heightChanged = (observable, oldValue, newValue) -> container.update(virtualFlow.getVerticalPosition());
            this.posChanged = (observable, oldValue, newValue) -> {
                container.setLayoutY(-newValue.doubleValue());
                container.update(newValue.doubleValue());
            };
        }

        //================================================================================
        // Initialization
        //================================================================================

        // TODO investigate more

        /**
         * DO NOT CALL THIS METHOD, it's automatically handled by the VirtualFlow.
         */
        public void initialize() {
            virtualFlow.getVirtualFlow().heightProperty().addListener(heightChanged);
            virtualFlow.verticalPositionProperty().addListener(posChanged);
        }

        //================================================================================
        // Implemented Methods
        //================================================================================

        /**
         * Computes the first visible cell index in the viewport with this formula:
         * <p>
         * {@code (int) Math.floor(scrolled / cellHeight)}
         * <p>
         * The scrolled parameter is the amount of pixels scrolled from the top
         * and cellHeight is the fixed cells' height.
         * <p>
         * The result is clamped between 0 and the number of items -1.
         */
        @Override
        public int firstVisible() {
            return NumberUtils.clamp(
                    (int) Math.floor(container.getScrolled() / container.getCellHeight()),
                    0,
                    virtualFlow.getItems().size() - 1
            );
        }

        /**
         * Computes the first visible cell index in the viewport with this formula:
         * <p>
         * {@code (int) Math.ceil((scrolled + virtualFlowHeight) / cellHeight - 1)}
         * <p>
         * The scrolled parameter is the amount of pixels scrolled from the top,
         * the virtualFlowHeight well... is self-explanatory, and cellHeight is the fixed cells' height.
         * <p>
         * The result is clamped between 0 and the number of items -1.
         */
        @Override
        public int lastVisible() {
            return NumberUtils.clamp(
                    (int) Math.ceil((container.getScrolled() + virtualFlow.getVirtualFlow().getHeight()) / container.getCellHeight() - 1),
                    0,
                    virtualFlow.getItems().size() - 1
            );
        }

        /**
         * @return the given cellHeight multiplied by the number of items
         */
        @Override
        public double computeEstimatedHeight(double cellHeight) {
            return virtualFlow.getItems().size() * cellHeight;
        }

        /**
         * @return the VirtualFlow's width
         */
        @Override
        public double computeEstimatedWidth(double cellWidth) {
            return virtualFlow.getVirtualFlow().getWidth();
        }

        /**
         * @return the Node's pref height
         */
        @Override
        public double getHeight(Node node) {
            return node.prefHeight(-1);
        }

        /**
         * If the Node's maxWidth is set to {@link Double#MAX_VALUE} then returns
         * the VirtualFlow's width, otherwise calls {@link #boundSize(double, double, double)}.
         */
        @Override
        public double getWidth(Node node) {
            double max = node.maxWidth(-1);
            return max == Double.MAX_VALUE ?
                    virtualFlow.getVirtualFlow().getWidth() :
                    boundSize(node.prefWidth(-1), node.minWidth(-1), max);
        }

        /**
         * Resizes and relocated the given node with the given parameters,
         * the x offset is always 0.
         *
         * @param node the Node to resize and relocate
         * @param pos  the y position
         * @param w    the width
         * @param h    the height
         */
        @Override
        public void resizeRelocate(Node node, double pos, double w, double h) {
            node.resizeRelocate(0, pos, w, h);
        }

        /**
         * Computes the Node's y position and then calls {@link #resizeRelocate(Node, double, double, double)}.
         */
        @Override
        public void layout(Node node, int index, double cellW, double cellH) {
            double pos = index * cellH;
            resizeRelocate(node, pos, cellW, cellH);
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Gets the current vertical position and adds the given amount of
         * pixels to it. The result is clamped between 0 and the vBar's max property.
         */
        @Override
        public void scrollBy(double pixels) {
            ScrollBar vBar = virtualFlow.getVBar();
            double currVal = vBar.getValue();
            double newVal = NumberUtils.clamp(currVal + pixels, 0, vBar.getMax());
            virtualFlow.setVerticalPosition(newVal);
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * Computes the pixel value by multiplying the given index for the fixed cells' height,
         * then calls {@link #scrollToPixel(double)} with the result.
         */
        @Override
        public void scrollTo(int index) {
            scrollToPixel(container.getCellHeight() * index);
        }

        /**
         * Calls {@link #scrollTo(int)} with 0 as argument.
         */
        @Override
        public void scrollToFirst() {
            scrollTo(0);
        }

        /**
         * Calls {@link #scrollTo(int)} with items size - 1 as argument.
         */
        @Override
        public void scrollToLast() {
            scrollTo(virtualFlow.getItems().size() - 1);
        }

        /**
         * {@inheritDoc}
         * <p></p>
         * The given pixel value is clamped between 0 and the vBar's max property before.
         */
        @Override
        public void scrollToPixel(double pixel) {
            ScrollBar vBar = virtualFlow.getVBar();
            double val = NumberUtils.clamp(pixel, 0, vBar.getMax());
            virtualFlow.setVerticalPosition(val);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose() {
            virtualFlow.getVirtualFlow().heightProperty().removeListener(heightChanged);
            virtualFlow.verticalPositionProperty().removeListener(posChanged);
            heightChanged = null;
            posChanged = null;
        }
    }
}
