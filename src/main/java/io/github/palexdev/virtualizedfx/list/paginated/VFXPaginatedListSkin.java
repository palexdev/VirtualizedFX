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

package io.github.palexdev.virtualizedfx.list.paginated;

import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.list.VFXListSkin;
import javafx.geometry.Orientation;
import javafx.scene.control.SkinBase;

/// Default skin implementation for the paginated variant of [VFXList]: [VFXPaginatedList].
/// Extends [VFXListSkin].
///
/// There's not much going on here, just the bare minimum to get the paginated variant work as intended. The only thing
/// that changes is that the container's size will adapt to the cell size and the number of cells per page, the exact
/// computation is described and done by [#getLength()].
public class VFXPaginatedListSkin<T, C extends VFXCell<T>> extends VFXListSkin<T, C> {

    //================================================================================
    // Constructors
    //================================================================================

    public VFXPaginatedListSkin(VFXPaginatedList<T, C> list) {
        super(list);
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Computes the length the container should have, according to the following three properties:
    ///
    /// - [VFXPaginatedList#cellsPerPageProperty()]
    ///
    /// - [VFXPaginatedList#cellSizeProperty()]
    ///
    /// - [VFXPaginatedList#spacingProperty()]
    ///
    /// The formula is as follows: `(cellsPerPage * (cellSize + spacing)) - spacing`.
    ///
    /// The result is enforced by the 'compute min/pref/max width/height' methods defined by [SkinBase] and overridden here.
    /// Note that only the methods relative to the current orientation (VERTICAL -> height / HORIZONTAL -> width) will use
    /// the resulting value. Which means that the size in the opposite direction can be changed as preferred.
    protected final double getLength() {
        VFXPaginatedList<T, C> list = getList();
        return (list.getCellsPerPage() * (list.getCellSize() + list.getSpacing())) - list.getSpacing();
    }

    /// Convenience method to cast [#getSkinnable()] to [VFXPaginatedList].
    protected VFXPaginatedList<T, C> getList() {
        return (VFXPaginatedList<T, C>) getSkinnable();
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        Orientation o = getSkinnable().getOrientation();
        if (o == Orientation.HORIZONTAL) return getLength();
        return super.computeMinWidth(height, topInset, rightInset, bottomInset, leftInset);
    }

    @Override
    protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        Orientation o = getSkinnable().getOrientation();
        if (o == Orientation.VERTICAL) return getLength();
        return super.computeMinHeight(width, topInset, rightInset, bottomInset, leftInset);
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        Orientation o = getSkinnable().getOrientation();
        if (o == Orientation.HORIZONTAL) return getLength();
        return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        Orientation o = getSkinnable().getOrientation();
        if (o == Orientation.VERTICAL) return getLength();
        return super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
    }

    @Override
    protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        Orientation o = getSkinnable().getOrientation();
        if (o == Orientation.HORIZONTAL) return getLength();
        return super.computeMaxWidth(height, topInset, rightInset, bottomInset, leftInset);
    }

    @Override
    protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        Orientation o = getSkinnable().getOrientation();
        if (o == Orientation.VERTICAL) return getLength();
        return super.computeMaxHeight(width, topInset, rightInset, bottomInset, leftInset);
    }
}
