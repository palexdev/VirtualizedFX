/*
 * Copyright (C) 2025 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.base;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.properties.PositionProperty;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.layout.Region;

/**
 * Base API for all helpers used in virtualized containers.
 * <p>
 * Groups common properties, computations and utilities, minimizing redundancy and improving maintainability.
 */
public interface VFXContainerHelper<T, C extends Region & VFXContainer<T>> {

    /**
     * @return the total number of pixels on the x-axis.
     */
    default double getVirtualMaxX() {
        return virtualMaxXProperty().get();
    }

    /**
     * Specifies the total number of pixels on the x-axis.
     */
    ReadOnlyDoubleProperty virtualMaxXProperty();

    /**
     * @return the total number of pixels on the y-axis.
     */
    default double getVirtualMaxY() {
        return virtualMaxYProperty().get();
    }

    /**
     * Specifies the total number of pixels on the y-axis.
     */
    ReadOnlyDoubleProperty virtualMaxYProperty();

    /**
     * @return the maximum possible vertical position.
     */
    default double getMaxVScroll() {
        return maxVScrollProperty().get();
    }

    /**
     * Specifies the maximum possible vertical position.
     */
    ReadOnlyDoubleProperty maxVScrollProperty();

    /**
     * @return the maximum possible horizontal position.
     */
    default double getMaxHScroll() {
        return maxHScrollProperty().get();
    }

    /**
     * Specifies the maximum possible horizontal position.
     */
    ReadOnlyDoubleProperty maxHScrollProperty();

    /**
     * @return the position the viewport should be at in the container
     */
    default Position getViewportPosition() {
        return viewportPositionProperty().get();
    }

    /**
     * Cells are actually contained in a separate pane called 'viewport'. The scroll is applied on this pane.
     * <p>
     * This property specifies the translation of the viewport, the value depends on the implementation.
     */
    ReadOnlyObjectProperty<Position> viewportPositionProperty();

    /**
     * @return the {@link VFXContainer} implementation instance associated to this helper
     */
    C getContainer();

    /**
     * Forces the {@link VFXContainer#vPosProperty()} and {@link VFXContainer#hPosProperty()} to be invalidated.
     * <p>
     * This is simply done by calling the respective setters with their current respective values. Those two properties
     * will automatically call {@link #getMaxVScroll()} and {@link #getMaxHScroll()} to ensure the values are correct.
     * <p>
     * Automatically invoked when needed.
     */
    default void invalidatePos() {
        C container = getContainer();
        container.setVPos(container.getVPos());
        container.setHPos(container.getHPos());
    }

    /**
     * Implementations should define the logic to manually invalidate the virtual sizes ({@link #virtualMaxXProperty()}
     * and {@link #virtualMaxYProperty()}) of the container when needed.
     * <p>
     * There are exceptional cases where we can't rely on automatic invalidation because it could lead to incorrect states,
     * the easiest and most stable solution for those is manual invalidation.
     */
    void invalidateVirtualSizes();

    /**
     * Converts the given index to an item (shortcut for {@code getContainer().getItems().get(index)}).
     */
    default T indexToItem(int index) {
        return getContainer().getItems().get(index);
    }

    /**
     * If the helper uses listeners/bindings that may lead to memory leaks, this is the right place to remove them.
     */
    default void dispose() {
    }

    //================================================================================
    // Base Implementation
    //================================================================================

    /**
     * Abstract implementation of {@link VFXContainerHelper}.
     * <p>
     * This is the recommended class onto which base concrete helpers.
     * <p>
     * Stores the virtualized container's instance, defines common properties and thus implementing some of the APIs
     * from {@link VFXContainerHelper}, and in addition defines some other APIs that should be hidden and known only to
     * its implementations.
     */
    abstract class VFXContainerHelperBase<T, C extends Region & VFXContainer<T>> implements VFXContainerHelper<T, C> {
        protected C container;

        protected final ReadOnlyDoubleWrapper virtualMaxX = new ReadOnlyDoubleWrapper();
        protected DoubleBinding vmxBinding;

        protected final ReadOnlyDoubleWrapper virtualMaxY = new ReadOnlyDoubleWrapper();
        protected DoubleBinding vmyBinding;

        protected final ReadOnlyDoubleWrapper maxVScroll = new ReadOnlyDoubleWrapper();
        protected final ReadOnlyDoubleWrapper maxHScroll = new ReadOnlyDoubleWrapper();
        protected final PositionProperty viewportPosition = new PositionProperty();

        protected VFXContainerHelperBase(C container) {
            this.container = container;
        }

        protected void createBindings() {
            vmxBinding = createVirtualMaxXBinding();
            if (vmxBinding != null) virtualMaxX.bind(vmxBinding);

            vmyBinding = createVirtualMaxYBinding();
            if (vmyBinding != null) virtualMaxY.bind(vmyBinding);

            maxVScroll.bind(createMaxVScrollBinding());
            maxHScroll.bind(createMaxHScrollBinding());
        }

        /**
         * Implementations should use this build and return the {@link DoubleBinding} with the appropriate dependencies
         * responsible for the {@link #virtualMaxXProperty()}'s value.
         */
        protected abstract DoubleBinding createVirtualMaxXBinding();

        /**
         * Implementations should use this build and return the {@link DoubleBinding} with the appropriate dependencies
         * responsible for the {@link #virtualMaxXProperty()}'s value.
         */
        protected abstract DoubleBinding createVirtualMaxYBinding();

        /**
         * Builds and returns the binding which computes the {@link #maxVScrollProperty()}'s value.
         * <p>
         * For most containers the value is given by: {@code virtualMaxY - containerHeight}.
         * <p>
         * <i>The formula may vary for some containers!</i>
         */
        protected DoubleBinding createMaxVScrollBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> Math.max(0, getVirtualMaxY() - container.getHeight()))
                .addSources(virtualMaxY, container.heightProperty())
                .get();
        }

        /**
         * Builds and returns the binding which computes the {@link #maxHScrollProperty()}'s value.
         * <p>
         * For most containers the value is given by: {@code virtualMaxX - containerWidth}.
         * <p>
         * <i>The formula may vary for some containers!</i>
         */
        protected DoubleBinding createMaxHScrollBinding() {
            return DoubleBindingBuilder.build()
                .setMapper(() -> Math.max(0, getVirtualMaxX() - container.getWidth()))
                .addSources(virtualMaxX, container.widthProperty())
                .get();
        }

        @Override
        public ReadOnlyDoubleProperty virtualMaxXProperty() {
            return virtualMaxX.getReadOnlyProperty();
        }

        @Override
        public ReadOnlyDoubleProperty virtualMaxYProperty() {
            return virtualMaxY.getReadOnlyProperty();
        }

        @Override
        public ReadOnlyDoubleProperty maxVScrollProperty() {
            return maxVScroll.getReadOnlyProperty();
        }

        @Override
        public ReadOnlyDoubleProperty maxHScrollProperty() {
            return maxHScroll.getReadOnlyProperty();
        }

        @Override
        public ReadOnlyObjectProperty<Position> viewportPositionProperty() {
            return viewportPosition.getReadOnlyProperty();
        }

        @Override
        public C getContainer() {
            return container;
        }

        @Override
        public void invalidateVirtualSizes() {
            if (vmxBinding != null) vmxBinding.invalidate();
            if (vmyBinding != null) vmyBinding.invalidate();
        }

        @Override
        public void dispose() {
            virtualMaxX.unbind();
            if (vmxBinding != null) {
                vmxBinding.dispose();
                vmxBinding = null;
            }
            virtualMaxY.unbind();
            if (vmyBinding != null) {
                vmyBinding.dispose();
                vmyBinding = null;
            }

            maxVScroll.unbind();
            maxHScroll.unbind();
            container = null;
        }
    }
}
