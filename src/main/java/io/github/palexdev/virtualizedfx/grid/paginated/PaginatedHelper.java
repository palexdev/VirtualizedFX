/*
 * Copyright (C) 2022 Parisi Alessandro
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

package io.github.palexdev.virtualizedfx.grid.paginated;

import io.github.palexdev.virtualizedfx.grid.GridHelper;
import javafx.geometry.Orientation;

/**
 * Small API extension of {@link GridHelper}, to be used exclusively with {@link PaginatedVirtualGrid}.
 */
public interface PaginatedHelper extends GridHelper {

	/**
	 * Converts the given page number to the corresponding position in pixels.
	 */
	double pageToPos(int page);

	/**
	 * Scrolls the viewport to the desired page.
	 */
	void goToPage(int page);

	/**
	 * Concrete implementation of {@link PaginatedHelper} and extension of {@link DefaultGridHelper}.
	 */
	class PaginatedGridHelper extends DefaultGridHelper implements PaginatedHelper {
		protected PaginatedVirtualGrid<?, ?> pGrid;

		public PaginatedGridHelper(PaginatedVirtualGrid<?, ?> virtualGrid) {
			super(virtualGrid);
			this.pGrid = virtualGrid;
		}

		/**
		 * Overridden to return {@link PaginatedVirtualGrid#getRowsPerPage()}.
		 */
		@Override
		public int maxRows() {
			return pGrid.getRowsPerPage();
		}

		/**
		 * Overridden to return: {@code pageToPos(maxPage)}.
		 */
		@Override
		public double maxVScroll() {
			return pageToPos(pGrid.getMaxPage());
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This is given by {@code (page - 1) * rowsPerPage * cellHeight}.
		 */
		@Override
		public double pageToPos(int page) {
			return (page - 1) * pGrid.getRowsPerPage() * pGrid.getCellSize().getHeight();
		}

		@Override
		public void goToPage(int page) {
			super.scrollTo(pageToPos(page), Orientation.VERTICAL);
		}

		/**
		 * This is unsupported as the {@link PaginatedVirtualGrid} can only scroll to certain pixel values, given
		 * by the pages.
		 *
		 * @throws UnsupportedOperationException
		 */
		@Override
		public void scrollBy(double pixels, Orientation orientation) {
			throw new UnsupportedOperationException("The paginated grid cannot scroll by a given amount of pixels");
		}

		/**
		 * This is unsupported as the {@link PaginatedVirtualGrid} can only scroll to certain pixel values, given
		 * by the pages.
		 *
		 * @throws UnsupportedOperationException
		 */
		@Override
		public void scrollTo(double pixel, Orientation orientation) {
			throw new UnsupportedOperationException("The paginated grid cannot scroll to a given pixel position as it may be wrong");
		}

		/**
		 * This is unsupported as the {@link PaginatedVirtualGrid} can only scroll to certain pixel values, given
		 * by the pages.
		 *
		 * @throws UnsupportedOperationException
		 */
		@Override
		public void scrollToRow(int index) {
			throw new UnsupportedOperationException("The paginated grid cannot scroll to a given row index");
		}

		@Override
		public void dispose() {
			super.dispose();
			pGrid = null;
		}
	}
}
