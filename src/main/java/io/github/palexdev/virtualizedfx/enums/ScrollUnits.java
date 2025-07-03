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

package io.github.palexdev.virtualizedfx.enums;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.grid.VFXGrid;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.Node;

/**
 * Defines the unit of measurement used when setting the scroll increment for a {@link VFXScrollPane}.
 * <p>
 * Scroll units are used to convert a logical amount (e.g., 3 cells, 100 pixels) into a percentage value
 * in the range {@code [0.0, 1.0]}, which is then applied to the scroll pane.
 *
 * @see #calc(VFXScrollPane, double, Orientation)
 * @see #deps(VFXScrollPane, Orientation)
 */
public enum ScrollUnits {
    /**
     * Scrolls by a number of cells/rows/columns, depending on the orientation.
     * <p></p>
     * This unit is only valid when the scroll pane's content is an instance of {@link VFXContainer}.
     * The pixel size of each cell/row/column is multiplied by the specified amount and then converted into a percentage
     * relative to the container's max scroll value ({@link VFXContainer#maxVScrollProperty()} or {@link VFXContainer#maxHScrollProperty()}).
     * <p></p>
     * Binding dependencies include the container's scroll bounds and its cell size property.
     */
    CELL {
        @Override
        public Supplier<Double> calc(VFXScrollPane vsp, double amount, Orientation orientation) {
            Node node = vsp.getContent();
            if (node == null) throw new NullPointerException("Node cannot be null");

            VFXContainer<?> c = asVirtualized(node);
            Supplier<Double> css = switch (c) {
                case VFXGrid<?, ?> g when orientation == Orientation.VERTICAL -> () -> g.getCellSize().getHeight();
                case VFXGrid<?, ?> g when orientation == Orientation.HORIZONTAL -> () -> g.getCellSize().getWidth();
                case VFXList<?, ?> l -> l::getCellSize;
                case VFXTable<?> t when orientation == Orientation.VERTICAL -> t::getRowsHeight;
                case VFXTable<?> t when orientation == Orientation.HORIZONTAL -> () -> t.getColumnsSize().getWidth();
                default -> () -> 0.0;
            };
            return PIXELS.calc(vsp, css.get() * amount, orientation);
        }

        @Override
        public ObservableValue<?>[] deps(VFXScrollPane vsp, Orientation orientation) {
            Node node = vsp.getContent();
            if (node == null) throw new NullPointerException("Node cannot be null");

            VFXContainer<?> c = asVirtualized(node);
            List<ObservableValue<?>> base = new ArrayList<>();
            base.add((orientation == Orientation.VERTICAL) ? c.maxVScrollProperty() : c.maxHScrollProperty());
            switch (c) {
                case VFXGrid<?, ?> g -> base.add(g.cellSizeProperty());
                case VFXList<?, ?> l -> base.add(l.cellSizeProperty());
                case VFXTable<?> t -> base.add(t.rowsHeightProperty());
                default -> {}
            }
            return base.toArray(ObservableValue<?>[]::new);
        }

        private VFXContainer<?> asVirtualized(Node node) {
            if (node instanceof VFXContainer<?> c) return c;
            throw new IllegalArgumentException("Node must be a virtualized container");

        }
    },

    /**
     * Scrolls by a percentage of the total scrollable area.
     * <p>
     * This is the simplest unit, no conversion is performed. Because of that, bindings for this unit are ignored and
     * fallback to a standard apply.
     * <p>
     * Use this when you want to directly specify the exact scroll delta in percentage terms.
     */
    PERCENTAGE {
        @Override
        public Supplier<Double> calc(VFXScrollPane vsp, double amount, Orientation orientation) {
            return () -> amount;
        }

        @Override
        public ObservableValue<?>[] deps(VFXScrollPane vsp, Orientation orientation) {
            return new ObservableValue[0];
        }
    },

    /**
     * Scrolls by a fixed number of pixels.
     * <p>
     * The specified pixel amount is converted into a percentage based on the scrollable content's
     * size and the scroll pane's viewport size. If the content is a {@link VFXContainer}, its virtualized
     * scroll bounds are used. Otherwise, the scrollable extent is derived from the layout bounds.
     * <p></p>
     * Binding dependencies include either virtual scroll bounds or layout bounds, depending on content type.
     */
    PIXELS {
        @Override
        public Supplier<Double> calc(VFXScrollPane vsp, double amount, Orientation orientation) {
            Node node = vsp.getContent();
            if (node == null) throw new NullPointerException("Node cannot be null");
            Supplier<Double> scale = switch (node) {
                case VFXContainer<?> c when orientation == Orientation.VERTICAL -> c::getMaxVScroll;
                case VFXContainer<?> c when orientation == Orientation.HORIZONTAL -> c::getMaxHScroll;
                case Node n when orientation == Orientation.VERTICAL ->
                    () -> n.getLayoutBounds().getHeight() - vsp.getHeight();
                case Node n when orientation == Orientation.HORIZONTAL ->
                    () -> n.getLayoutBounds().getWidth() - vsp.getWidth();
                default -> () -> 0.0;
            };
            return () -> {
                double s = scale.get();
                return s > 0 ? amount / s : 0.0;
            };
        }

        @Override
        public ObservableValue<?>[] deps(VFXScrollPane vsp, Orientation orientation) {
            Node node = vsp.getContent();
            if (node == null) throw new NullPointerException("Node cannot be null");
            return switch (node) {
                case VFXContainer<?> c when orientation == Orientation.VERTICAL ->
                    new ObservableValue<?>[]{c.maxVScrollProperty()};
                case VFXContainer<?> c when orientation == Orientation.HORIZONTAL ->
                    new ObservableValue<?>[]{c.maxHScrollProperty()};
                default -> new ObservableValue<?>[]{node.layoutBoundsProperty(), vsp.layoutBoundsProperty()};
            };
        }
    },
    ;

    /**
     * Calculates the scroll amount as a percentage value in {@code [0.0, 1.0]}, based on the given unit type.
     * <p></p>
     * The {@code orientation} parameters specifies the axis on which select the properties.
     * E.g: VERTICAL -> height, HORIZONTAL -> width
     */
    public abstract Supplier<Double> calc(VFXScrollPane vsp, double amount, Orientation orientation);

    /**
     * Lists observable properties that should be watched when using this unit in a binding context.
     * <p>
     * An empty array indicates that the binding is not necessary and should be ignored. This is the case of {@link #PERCENTAGE}.
     */
    public abstract ObservableValue<?>[] deps(VFXScrollPane vsp, Orientation orientation);
}
