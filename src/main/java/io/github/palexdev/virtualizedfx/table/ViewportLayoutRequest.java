/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
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

package io.github.palexdev.virtualizedfx.table;

import javafx.beans.property.ReadOnlyObjectWrapper;

public record ViewportLayoutRequest(int from, int to, boolean done) {

    //================================================================================
    // Static Properties
    //================================================================================

    public static final ViewportLayoutRequest NULL = new ViewportLayoutRequest(-1, -1, false);
    public static final ViewportLayoutRequest DONE = new ViewportLayoutRequest(-1, -1, true);
    public static final ViewportLayoutRequest Y_ONLY = new ViewportLayoutRequest(Integer.MAX_VALUE, Integer.MIN_VALUE);

    //================================================================================
    // Constructors
    //================================================================================

    public ViewportLayoutRequest(int from, int to) {
        this(from, to, false);
    }

    //================================================================================
    // Methods
    //================================================================================

    public boolean isValid() {
        return from >= 0;
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    //@formatter:off
    public static class ViewportLayoutRequestProperty extends ReadOnlyObjectWrapper<ViewportLayoutRequest> {
        public ViewportLayoutRequestProperty() {super(NULL);}
        public boolean isValid() {return getValue().isValid();}
    }
}
