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

package io.github.palexdev.virtualizedfx.utils;

import javafx.animation.Interpolator;

public class AnimationUtils {
    public static final Interpolator INTERPOLATOR_V1 = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);
    public static final Interpolator INTERPOLATOR_V2 = Interpolator.SPLINE(0.0825D, 0.3025D, 0.0875D, 0.9975D);

    private AnimationUtils() {
    }

}
