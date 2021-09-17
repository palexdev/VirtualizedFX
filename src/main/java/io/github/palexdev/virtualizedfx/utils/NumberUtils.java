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

public class NumberUtils {

    /**
     * Limits the given value to the given min-max range by returning the nearest bound
     * if it exceeds or val if it's in range.
     */
    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }


    /**
     * Limits the given value to the given min-max range by returning the nearest bound
     * if it exceeds or val if it's in range.
     */
    public static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * Formats the given double value to have the given number of decimal places.
     */
    public static double formatTo(double value, int decimalPrecision) {
        int calcScale = (int) Math.pow(10, decimalPrecision);
        return (double) Math.round(value * calcScale) / calcScale;
    }
}
