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

/**
 * Simple extension of {@link VFXListHelper} with to concrete implementations {@link VerticalHelper} and {@link HorizontalHelper}
 * to override the behavior of {@link #visibleNum()}, so that it always returns the value of cells per page
 * ({@link VFXPaginatedList#cellsPerPageProperty()}).
 */
public interface VFXPaginatedListHelper<T, C extends VFXCell<T>> extends VFXListHelper<T, C> {

    class VerticalHelper<T, C extends VFXCell<T>> extends VFXListHelper.VerticalHelper<T, C> implements VFXPaginatedListHelper<T, C> {
        public VerticalHelper(VFXPaginatedList<T, C> list) {
            super(list);
        }

        @Override
        public int visibleNum() {
            return getList().getCellsPerPage();
        }

        /**
         * @throws UnsupportedOperationException because the paginated variant cannot scroll by pixels
         */
        @Override
        public void scrollBy(double pixels) {
            throw new UnsupportedOperationException("This scrolls by page not pixels");
        }

        /**
         * @throws UnsupportedOperationException because the paginated variant cannot scroll by pixels
         */
        @Override
        public void scrollToPixel(double pixel) {
            throw new UnsupportedOperationException("This scrolls by page not pixels");
        }

        /**
         * Scrolls to the page containing the given item index.
         */
        @Override
        public void scrollToIndex(int index) {
            VFXPaginatedList<T, C> list = getList();
            list.setPage(list.findPageByIndex(index));
        }

        @Override
        public VFXPaginatedList<T, C> getList() {
            return ((VFXPaginatedList<T, C>) super.getList());
        }
    }

    class HorizontalHelper<T, C extends VFXCell<T>> extends VFXListHelper.HorizontalHelper<T, C> implements VFXPaginatedListHelper<T, C> {
        public HorizontalHelper(VFXPaginatedList<T, C> list) {
            super(list);
        }

        @Override
        public int visibleNum() {
            return getList().getCellsPerPage();
        }

        /**
         * @throws UnsupportedOperationException because the paginated variant cannot scroll by pixels
         */
        @Override
        public void scrollBy(double pixels) {
            throw new UnsupportedOperationException("This scrolls by page not pixels");
        }

        /**
         * @throws UnsupportedOperationException because the paginated variant cannot scroll by pixels
         */
        @Override
        public void scrollToPixel(double pixel) {
            throw new UnsupportedOperationException("This scrolls by page not pixels");
        }

        /**
         * Scrolls to the page containing the given item index.
         */
        @Override
        public void scrollToIndex(int index) {
            VFXPaginatedList<T, C> list = getList();
            list.setPage(list.findPageByIndex(index));
        }

        @Override
        public VFXPaginatedList<T, C> getList() {
            return ((VFXPaginatedList<T, C>) super.getList());
        }
    }
}
