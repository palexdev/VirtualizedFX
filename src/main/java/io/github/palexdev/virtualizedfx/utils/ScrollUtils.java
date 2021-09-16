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


import javafx.geometry.Orientation;

/**
 * Utility class for VirtualFlows.
 */
public class ScrollUtils {

    public enum ScrollDirection {
        UP(-1), RIGHT(-1), DOWN(1), LEFT(1);

        final int intDirection;

        ScrollDirection(int intDirection) {
            this.intDirection = intDirection;
        }

        public int intDirection() {
            return intDirection;
        }
    }

    private ScrollUtils() {
    }

    /**
     * Determines if a scroll event comes from the trackpad from the given delta value.
     * <p></p>
     * There's no real way to distinguish a scroll event from trackpad or mouse but usually all
     * trackpad events have a much lower delta value.
     */
    public static boolean isTrackPad(double delta) {
        return Math.abs(delta) < 10;
    }

    /**
     * Determines the direction of the scroll from the given orientation and the given delta,
     * uses {@link #upOrDown(double)} if orientation is VERTICAL or {@link #leftOrRight(double)}
     * if orientation is HORIZONTAL.
     */
    public static ScrollDirection determineScrollDirection(Orientation orientation, double delta) {
        return orientation == Orientation.VERTICAL ? upOrDown(delta) : leftOrRight(delta);
    }

    /**
     * If the given delta is lesser than 0 returns LEFT, otherwise returns RIGHT.
     */
    public static ScrollDirection leftOrRight(double delta) {
        return delta < 0 ? ScrollDirection.LEFT : ScrollDirection.RIGHT;
    }

    /**
     * If the given delta is lesser than 0 returns DOWN, otherwise returns UP.
     */
    public static ScrollDirection upOrDown(double delta) {
        return delta < 0 ? ScrollDirection.DOWN : ScrollDirection.UP;
    }
}
