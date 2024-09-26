/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
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

import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.grid.VFXGrid;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import javafx.geometry.Orientation;

/**
 * Interface to quickly wrap a content in a {@link VFXScrollPane} and make it scrollable. How the wrapping is done
 * depends on the implementing class.
 * <p></p>
 * Since this is mostly intended for virtualized components (but not limited to!!!), there are also a bunch of util methods
 * specifically for {@link VFXContainer}.
 */
public interface VFXScrollable {
    /**
     * This multiplier is applied to the unit increment of a {@link VFXScrollPane} to determine the track increment.
     * <p>
     * So, if I have a unit increment of 30px, pressing on the track would result in a (30px * mul)px scroll.
     */
    double TRACK_MULTIPLIER = 5.0;

    /**
     * Wraps this in a {@link VFXScrollPane} to enable scrolling.
     */
    VFXScrollPane makeScrollable();

    /**
     * Sets the unit and track increments for both the scroll bars of the given {@link VFXScrollPane}, optionally binds
     * the relative properties if the {@code bind} parameter is {@code true}.
     * <pre> Formulas:
     * {@code
     * - vUnitIncrement  -> cellWidth * multiplier
     * - vTrackIncrement -> vUnitIncrement * TRACK_MULTIPLIER
     * - hUnitIncrement  -> cellHeight * multiplier
     * - hTrackIncrement -> hUnitIncrement * TRACK_MULTIPLIER
     * }
     * </pre>
     *
     * @see VFXGrid#cellSizeProperty()
     * @see #TRACK_MULTIPLIER
     */
    static void setSpeed(VFXScrollPane vsp, VFXGrid<?, ?> grid, double multiplier, boolean bind) {
        if (bind) {
            vsp.hUnitIncrementProperty().bind(grid.cellSizeProperty().map(s -> s.getWidth() * multiplier));
            vsp.hTrackIncrementProperty().bind(vsp.hUnitIncrementProperty().multiply(TRACK_MULTIPLIER));
            vsp.vUnitIncrementProperty().bind(grid.cellSizeProperty().map(s -> s.getHeight() * multiplier));
            vsp.vTrackIncrementProperty().bind(vsp.vUnitIncrementProperty().multiply(TRACK_MULTIPLIER));
            return;
        }
        vsp.setHUnitIncrement(grid.getCellSize().getWidth() * multiplier);
        vsp.setHTrackIncrement(vsp.getHUnitIncrement() * TRACK_MULTIPLIER);
        vsp.setVUnitIncrement(grid.getCellSize().getHeight() * multiplier);
        vsp.setVTrackIncrement(vsp.getVUnitIncrement() * TRACK_MULTIPLIER);
    }

    /**
     * Same as {@link #setSpeed(VFXScrollPane, VFXGrid, double, boolean)} with the {@code multiplier} parameter set to
     * {@code 1.0}.
     */
    static void setSpeed(VFXScrollPane vsp, VFXGrid<?, ?> grid, boolean bind) {
        setSpeed(vsp, grid, 1.0, bind);
    }

    /**
     * Sets the unit and track increments for both the scroll bars of the given {@link VFXScrollPane}, optionally binds
     * the relative properties if the {@code bind} parameter is {@code true}.
     * <pre> Formulas (VERTICAL):
     * {@code
     * - hUnitIncrement  -> oppositeSpeed.width
     * - hTrackIncrement -> hUnitIncrement * TRACK_MULTIPLIER
     * - vUnitIncrement  -> cellSize * multiplier
     * - vTrackIncrement -> vUnitIncrement * TRACK_MULTIPLIER
     * }
     * </pre>
     *
     * <p></p>
     *
     * <pre> Formulas (HORIZONTAL):
     * {@code
     * - hUnitIncrement  -> cellSize * multiplier
     * - hTrackIncrement -> hUnitIncrement * TRACK_MULTIPLIER
     * - vUnitIncrement  -> oppositeSpeed.height
     * - vTrackIncrement -> vUnitIncrement * TRACK_MULTIPLIER
     * }
     * </pre>
     *
     * @see VFXList#cellSizeProperty()
     * @see #TRACK_MULTIPLIER
     */
    static void setSpeed(VFXScrollPane vsp, VFXList<?, ?> list, Size oppositeSpeed, double multiplier, boolean bind) {
        if (bind) {
            vsp.hUnitIncrementProperty().bind(DoubleBindingBuilder.build()
                .setMapper(() -> (list.getOrientation() == Orientation.HORIZONTAL) ? list.getCellSize() * multiplier : oppositeSpeed.getWidth())
                .addSources(list.orientationProperty(), list.cellSizeProperty())
                .get()
            );
            vsp.hTrackIncrementProperty().bind(vsp.hUnitIncrementProperty().multiply(TRACK_MULTIPLIER));
            vsp.vUnitIncrementProperty().bind(DoubleBindingBuilder.build()
                .setMapper(() -> (list.getOrientation() == Orientation.VERTICAL) ? list.getCellSize() * multiplier : oppositeSpeed.getHeight())
                .addSources(list.orientationProperty(), list.cellSizeProperty())
                .get()
            );
            vsp.vTrackIncrementProperty().bind(vsp.vUnitIncrementProperty().multiply(TRACK_MULTIPLIER));
            return;
        }
        Orientation o = list.getOrientation();
        if (o == Orientation.VERTICAL) {
            vsp.setVUnitIncrement(list.getCellSize() * multiplier);
            vsp.setVTrackIncrement(list.getCellSize() * multiplier * TRACK_MULTIPLIER);
            vsp.setHUnitIncrement(oppositeSpeed.getWidth());
            vsp.setHTrackIncrement(oppositeSpeed.getWidth() * TRACK_MULTIPLIER);
        } else {
            vsp.setHUnitIncrement(list.getCellSize() * multiplier);
            vsp.setHTrackIncrement(list.getCellSize() * multiplier * TRACK_MULTIPLIER);
            vsp.setVUnitIncrement(oppositeSpeed.getHeight());
            vsp.setVTrackIncrement(oppositeSpeed.getHeight() * TRACK_MULTIPLIER);
        }
    }

    /**
     * Same as {@link #setSpeed(VFXScrollPane, VFXList, Size, double, boolean)} with the {@code multiplier} parameter
     * set to {@code 1.0}.
     */
    static void setSpeed(VFXScrollPane vsp, VFXList<?, ?> list, Size oppositeSpeed, boolean bind) {
        setSpeed(vsp, list, oppositeSpeed, 1.0, bind);
    }

    /**
     * Sets the unit and track increments for both scroll bars of the given {@link VFXScrollPane}, optionally
     * binds the relative properties if the {@code bind} parameter is {@code true}.
     * <pre> Formulas:
     * {@code
     * - hUnitIncrement  -> columnWidth * cMultiplier
     * - hTrackIncrement -> hUnitIncrement * (0.5 + cMultiplier)
     * - vUnitIncrement  -> rowsHeight * rMultiplier
     * - vTrackIncrement -> vUnitIncrement * TRACK_MULTIPLIER
     * }
     * </pre>
     *
     * @see VFXTable#columnsSizeProperty()
     * @see VFXTable#rowsHeightProperty()
     * @see #TRACK_MULTIPLIER
     */
    static void setSpeed(VFXScrollPane vsp, VFXTable<?> table, double cMultiplier, double rMultiplier, boolean bind) {
        if (bind) {
            vsp.hUnitIncrementProperty().bind(table.columnsSizeProperty().map(s -> s.getWidth() * cMultiplier));
            vsp.hTrackIncrementProperty().bind(vsp.hUnitIncrementProperty().multiply(0.5 + cMultiplier));
            vsp.vUnitIncrementProperty().bind(table.rowsHeightProperty().multiply(rMultiplier));
            vsp.vTrackIncrementProperty().bind(vsp.vUnitIncrementProperty().multiply(TRACK_MULTIPLIER));
            return;
        }
        vsp.setHUnitIncrement(table.getColumnsSize().getWidth() * cMultiplier);
        vsp.setHTrackIncrement(vsp.getHUnitIncrement() * (0.5 + cMultiplier));
        vsp.setVUnitIncrement(table.getRowsHeight() * rMultiplier);
        vsp.setVTrackIncrement(vsp.getVUnitIncrement() * TRACK_MULTIPLIER);
    }

    /**
     * Same as {@link #setSpeed(VFXScrollPane, VFXTable, double, double, boolean)} with {@code 1.0} as both the
     * {@code cMultiplier} and {@code rMultiplier} parameters.
     */
    static void setSpeed(VFXScrollPane vsp, VFXTable<?> table, boolean bind) {
        setSpeed(vsp, table, 1.0, 1.0, bind);
    }
}
