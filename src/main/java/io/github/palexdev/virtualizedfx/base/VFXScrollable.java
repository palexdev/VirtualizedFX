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

import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.enums.ScrollUnits;
import io.github.palexdev.virtualizedfx.utils.ScrollParams;
import javafx.geometry.Orientation;

/**
 * Interface to quickly wrap a content in a {@link VFXScrollPane} and make it scrollable. How the wrapping is done
 * depends on the implementing class.
 * <p></p>
 * It also offers two utilities to set or bind the speed of a {@link VFXScrollPane}:
 * {@link #setSpeed(VFXScrollPane, ScrollParams, ScrollParams)}, {@link #bindSpeed(VFXScrollPane, ScrollParams, ScrollParams)}
 */
public interface VFXScrollable {

    /**
     * Wraps this in a {@link VFXScrollPane} to enable scrolling.
     */
    VFXScrollPane makeScrollable();

    /**
     * Applies fixed scroll increments to the given {@link VFXScrollPane}, using the provided {@link ScrollParams}
     * for vertical and horizontal scrolling.
     * <p></p>
     * If you want to apply a certain speed dynamically, use {@link #bindSpeed(VFXScrollPane, ScrollParams, ScrollParams)}
     * instead.
     *
     * @see ScrollParams
     * @see ScrollUnits
     */
    static void setSpeed(VFXScrollPane vsp, ScrollParams vSpeed, ScrollParams hSpeed) {
        vSpeed.apply(vsp, Orientation.VERTICAL);
        hSpeed.apply(vsp, Orientation.HORIZONTAL);
    }

    /**
     * Binds scroll increments to the given {@link VFXScrollPane}, using the provided {@link ScrollParams}
     * for vertical and horizontal scrolling.
     * <p>
     * This method establishes bindings that automatically update scroll increments
     * when relevant properties (e.g., bounds, layout size) change.
     *
     * @see ScrollParams
     * @see ScrollUnits
     */
    static void bindSpeed(VFXScrollPane vsp, ScrollParams vSpeed, ScrollParams hSpeed) {
        vSpeed.bind(vsp, Orientation.VERTICAL);
        hSpeed.bind(vsp, Orientation.HORIZONTAL);
    }
}
