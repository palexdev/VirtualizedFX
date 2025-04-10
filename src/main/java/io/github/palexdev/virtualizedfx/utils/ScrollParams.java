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

package io.github.palexdev.virtualizedfx.utils;

import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.enums.ScrollUnits;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;

/**
 * Encapsulates the configuration for scrolling behavior in a {@link VFXScrollPane},
 * including the scroll amount, track multiplier, and unit of measurement.
 * <p>
 * Provides a consistent way to apply or bind scrolling increments
 * to a scroll pane, using either fixed values or dynamically computed ones.
 *
 * @param amount          the base scroll value, interpreted based on the {@code unit}
 * @param trackMultiplier a multiplier applied to the unit increment to compute the track increment the default value
 *                        is: {@link #DEFAULT_TRACK_MULTIPLIER}
 * @param unit            the unit of measurement that determines how the {@code amount} is interpreted
 * @see ScrollUnits
 */
public record ScrollParams(double amount, double trackMultiplier, ScrollUnits unit) {
    //================================================================================
    // Static Properties
    //================================================================================

    /**
     * Default multiplier applied to compute the track increment from the unit increment.
     * <p>
     * Used by all factory methods unless a custom multiplier is provided manually or set afterward using
     * {@link #withTrackMultiplier(double)}.
     */
    public static final double DEFAULT_TRACK_MULTIPLIER = 3.0;

    //================================================================================
    // Static Methods
    //================================================================================

    /**
     * Shortcut for {@code new ScrollParams(count, DEFAULT_TRACK_MULTIPLIER, ScrollUnits.CELL)}.
     */
    public static ScrollParams cells(double count) {
        return new ScrollParams(count, DEFAULT_TRACK_MULTIPLIER, ScrollUnits.CELL);
    }

    /**
     * Shortcut for {@code new ScrollParams(count, DEFAULT_TRACK_MULTIPLIER, ScrollUnits.PERCENTAGE)}.
     */
    public static ScrollParams percentage(double frac) {
        return new ScrollParams(frac, DEFAULT_TRACK_MULTIPLIER, ScrollUnits.PERCENTAGE);
    }

    /**
     * Shortcut for {@code new ScrollParams(count, DEFAULT_TRACK_MULTIPLIER, ScrollUnits.PIXELS)}.
     */
    public static ScrollParams pixels(double px) {
        return new ScrollParams(px, DEFAULT_TRACK_MULTIPLIER, ScrollUnits.PIXELS);
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Applies the scroll parameters to the given scroll pane in the specified orientation.
     * <p>
     * Sets the unit and track increment as raw values, based on the calculated scroll percentage.
     * No bindings are used — values are evaluated once and applied directly.
     * <p></p>
     * The track increment is obtained by multiplying the found unit increment by the {@link #trackMultiplier} value.
     */
    public void apply(VFXScrollPane vsp, Orientation orientation) {
        if (orientation == Orientation.VERTICAL) {
            vsp.setVUnitIncrement(unit.calc(vsp, amount, orientation).get());
            vsp.setVTrackIncrement(trackMultiplier * vsp.getVUnitIncrement());
        } else {
            vsp.setHUnitIncrement(unit.calc(vsp, amount, orientation).get());
            vsp.setHTrackIncrement(trackMultiplier * vsp.getHUnitIncrement());
        }
    }

    /**
     * Binds the scroll parameters to the given scroll pane in the specified orientation.
     * <p>
     * Creates bindings that automatically update the unit and track increments
     * when any relevant dependencies change (e.g., content bounds, cell size).
     * <p></p>
     * The dependencies are determined by the chosen scroll unit, see {@link ScrollUnits#deps(VFXScrollPane, Orientation)}.
     * <p>
     * If the scroll unit declares no dependencies, this method falls back to {@link #apply}.
     * <p></p>
     * The track increment properties are bound to the relative unit increment property and multiplied by the
     * {@link #trackMultiplier} value.
     */
    public void bind(VFXScrollPane vsp, Orientation orientation) {
        ObservableValue<?>[] deps = unit.deps(vsp, orientation);
        // If there are no dependencies then it means the bind is not necessary, simply apply
        if (deps.length == 0) {
            apply(vsp, orientation);
            return;
        }

        if (orientation == Orientation.VERTICAL) {
            vsp.vUnitIncrementProperty().bind(DoubleBindingBuilder.build()
                .setMapper(() -> unit.calc(vsp, amount, orientation).get())
                .addSources(deps)
                .get()
            );
            vsp.vTrackIncrementProperty().bind(vsp.vUnitIncrementProperty().multiply(trackMultiplier));
        } else {
            vsp.hUnitIncrementProperty().bind(DoubleBindingBuilder.build()
                .setMapper(() -> unit.calc(vsp, amount, orientation).get())
                .addSources(deps)
                .get()
            );
            vsp.hTrackIncrementProperty().bind(vsp.hUnitIncrementProperty().multiply(trackMultiplier));
        }
    }

    //================================================================================
    // Withers
    //================================================================================

    /**
     * @return a new {@code ScrollParams} instance with the same amount and unit, but with the specified {@code trackMultiplier}
     */
    public ScrollParams withTrackMultiplier(double trackMultiplier) {
        return new ScrollParams(amount, trackMultiplier, unit);
    }
}
