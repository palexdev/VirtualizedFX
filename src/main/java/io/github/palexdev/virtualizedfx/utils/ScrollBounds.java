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

package io.github.palexdev.virtualizedfx.utils;

import java.util.Objects;

import javafx.geometry.Orientation;

/**
 * Wrapper class to express the bounds of a scrollable area. It contains four pieces of data:
 * the width and height of the content, the width and the height of the view area (also called 'viewport').
 * <p>
 * The reason this is not a {@code record} is to allow methods to be overridden if needed.
 */
public final class ScrollBounds {
    //================================================================================
    // Static Properties
    //================================================================================
    public static final ScrollBounds ZERO = new ScrollBounds();

    //================================================================================
    // Properties
    //================================================================================
    private final double contentWidth;
    private final double contentHeight;
    private final double viewportWidth;
    private final double viewportHeight;


    //================================================================================
    // Constructors
    //================================================================================
    private ScrollBounds() {
        this(0, 0, 0, 0);
    }

    public ScrollBounds(double contentWidth, double contentHeight, double viewportWidth, double viewportHeight) {
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Computes how much content is visible by using its size and the view area's size. The value is a percentage in
     * the range [0.0, 1.0].
     *
     * @param o the orientation determines which size to use, width or height
     */
    public double visibleAmount(Orientation o) {
        double va = (o == Orientation.VERTICAL) ?
            viewportHeight / contentHeight :
            viewportWidth / contentWidth;
        return Double.isNaN(va) ? 0.0 : va;
    }

    /**
     * Computes the maximum number of scrollable pixels by using the content's size and the view area's size:
     * {@code Math.max(0, contentSize - viewportSize)}.
     * <p>
     *
     * @param o the orientation determines which size to use, width or height
     */
    public double maxScroll(Orientation o) {
        if (o == Orientation.VERTICAL) {
            return Math.max(0, contentHeight - viewportHeight);
        }
        return Math.max(0, contentWidth - viewportWidth);
    }

    //================================================================================
    // Getters
    //================================================================================

    /**
     * @return the content's width
     */
    public double contentWidth() {return contentWidth;}

    /**
     * @return the content's height
     */
    public double contentHeight() {return contentHeight;}

    /**
     * @return the view area's width
     */
    public double viewportWidth() {return viewportWidth;}

    /**
     * @return the view area's height
     */
    public double viewportHeight() {return viewportHeight;}

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ScrollBounds) obj;
        return Double.doubleToLongBits(this.contentWidth) == Double.doubleToLongBits(that.contentWidth) &&
               Double.doubleToLongBits(this.contentHeight) == Double.doubleToLongBits(that.contentHeight) &&
               Double.doubleToLongBits(this.viewportWidth) == Double.doubleToLongBits(that.viewportWidth) &&
               Double.doubleToLongBits(this.viewportHeight) == Double.doubleToLongBits(that.viewportHeight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentWidth, contentHeight, viewportWidth, viewportHeight);
    }

    @Override
    public String toString() {
        return "ScrollBounds[" +
               "contentWidth=" + contentWidth + ", " +
               "contentHeight=" + contentHeight + ", " +
               "viewportWidth=" + viewportWidth + ", " +
               "viewportHeight=" + viewportHeight + ']';
    }

}
