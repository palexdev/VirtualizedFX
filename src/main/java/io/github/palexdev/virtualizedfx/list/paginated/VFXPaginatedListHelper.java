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

package io.github.palexdev.virtualizedfx.list.paginated;

import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.list.VFXListHelper;
import javafx.geometry.Orientation;

/// Simple extension of [VFXListHelper] with two concrete implementations, [VerticalHelper] and [HorizontalHelper],
/// which override the behavior of [#visibleNum()] so that it always returns the value of cells per page
/// ([VFXPaginatedList#cellsPerPageProperty()]).
///
/// That single override is what turns the range computation into a paginated one: everything above it, buffer
/// included, keeps working exactly as in [VFXListHelper], it just windows a page's worth of cells instead of a
/// viewport's worth. The pixel-scrolling methods are then refused, since the position is driven by the page.
public interface VFXPaginatedListHelper<T, C extends VFXCell<T>> extends VFXListHelper<T, C> {

    /// Concrete implementation of [VFXPaginatedListHelper] for [Orientation#VERTICAL], on top of
    /// [VFXListHelper.VerticalHelper].
    class VerticalHelper<T, C extends VFXCell<T>> extends VFXListHelper.VerticalHelper<T, C> implements VFXPaginatedListHelper<T, C> {
        public VerticalHelper(VFXPaginatedList<T, C> list) {
            super(list);
        }

        /// {@inheritDoc}
        ///
        /// Given by [VFXPaginatedList#cellsPerPageProperty()], regardless of the container's height. A page shows the
        /// same number of cells whether or not they all fit.
        @Override
        public int visibleNum() {
            return getContainer().getCellsPerPage();
        }

        /// @throws UnsupportedOperationException because the paginated variant cannot scroll by pixels
        @Override
        public void scrollBy(double pixels) {
            throw new UnsupportedOperationException("This scrolls by page not pixels");
        }

        /// @throws UnsupportedOperationException because the paginated variant cannot scroll by pixels
        @Override
        public void scrollToPixel(double pixel) {
            throw new UnsupportedOperationException("This scrolls by page not pixels");
        }

        /// Scrolls to the page containing the given item index.
        @Override
        public void scrollToIndex(int index) {
            VFXPaginatedList<T, C> list = getContainer();
            list.setPage(list.findPageByIndex(index));
        }

        @Override
        public VFXPaginatedList<T, C> getContainer() {
            return (VFXPaginatedList<T, C>) super.getContainer();
        }
    }

    /// Concrete implementation of [VFXPaginatedListHelper] for [Orientation#HORIZONTAL], on top of
    /// [VFXListHelper.HorizontalHelper].
    class HorizontalHelper<T, C extends VFXCell<T>> extends VFXListHelper.HorizontalHelper<T, C> implements VFXPaginatedListHelper<T, C> {
        public HorizontalHelper(VFXPaginatedList<T, C> list) {
            super(list);
        }

        /// {@inheritDoc}
        ///
        /// Given by [VFXPaginatedList#cellsPerPageProperty()], regardless of the container's width. A page shows the
        /// same number of cells whether or not they all fit.
        @Override
        public int visibleNum() {
            return getContainer().getCellsPerPage();
        }

        /// @throws UnsupportedOperationException because the paginated variant cannot scroll by pixels
        @Override
        public void scrollBy(double pixels) {
            throw new UnsupportedOperationException("This scrolls by page not pixels");
        }

        /// @throws UnsupportedOperationException because the paginated variant cannot scroll by pixels
        @Override
        public void scrollToPixel(double pixel) {
            throw new UnsupportedOperationException("This scrolls by page not pixels");
        }

        /// Scrolls to the page containing the given item index.
        @Override
        public void scrollToIndex(int index) {
            VFXPaginatedList<T, C> list = getContainer();
            list.setPage(list.findPageByIndex(index));
        }

        @Override
        public VFXPaginatedList<T, C> getContainer() {
            return ((VFXPaginatedList<T, C>) super.getContainer());
        }
    }
}
