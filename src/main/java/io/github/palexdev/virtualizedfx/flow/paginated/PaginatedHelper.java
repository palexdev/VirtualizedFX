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

package io.github.palexdev.virtualizedfx.flow.paginated;

import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.virtualizedfx.flow.OrientationHelper;
import io.github.palexdev.virtualizedfx.flow.ViewportManager;

/**
 * Small API extension of {@link OrientationHelper}, to be used exclusively with {@link PaginatedVirtualFlow}.
 */
public interface PaginatedHelper extends OrientationHelper {

	/**
	 * Converts the given page number to the corresponding position in pixels.
	 */
	double pageToPos(int page);

	/**
	 * Scrolls the viewport to the desired page.
	 */
	void goToPage(int page);

	/**
	 * Concrete implementation of {@link PaginatedHelper} to be used with {@link PaginatedVirtualFlow} when its
	 * {@link PaginatedVirtualFlow#orientationProperty()} is HORIZONTAL, extends {@link HorizontalHelper}.
	 */
	class PaginatedHorizontalHelper extends HorizontalHelper implements PaginatedHelper {
		protected PaginatedVirtualFlow<?, ?> pFlow;

		public PaginatedHorizontalHelper(PaginatedVirtualFlow<?, ?> virtualFlow, ViewportManager<?, ?> viewportManager) {
			super(virtualFlow, viewportManager);
			this.pFlow = virtualFlow;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Overridden to return {@link PaginatedVirtualFlow#cellsPerPageProperty()}.
		 */
		@Override
		public int maxCells() {
			return pFlow.getCellsPerPage();
		}

		/**
		 * Overridden to return the pixel position of the last page.
		 */
		@Override
		public double maxHScroll() {
			return pageToPos(pFlow.getMaxPage());
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This is given by {@code (page - 1) * cellsPerPage * cellSize}.
		 */
		@Override
		public double pageToPos(int page) {
			return (page - 1) * pFlow.getCellsPerPage() * pFlow.getCellSize();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Implemented to use both {@link #pageToPos(int)} and {@link #scrollToPixel(double)}.
		 */
		@Override
		public void goToPage(int page) {
			scrollToPixel(pageToPos(page));
		}

		/**
		 * This is unsupported as the {@link PaginatedVirtualFlow} can only scroll to certain pixel values, given
		 * by the pages.
		 *
		 * @throws UnsupportedOperationException
		 */
		@Override
		public void scrollBy(double pixels) {
			throw new UnsupportedOperationException("The paginated flow cannot scroll by a given amount of pixels");
		}

		/**
		 * Basically the same as {@link #goToPage(int)}, the index parameter is always clamped
		 * between 1 and {@link PaginatedVirtualFlow#maxPageProperty()}.
		 */
		@Override
		public void scrollToIndex(int index) {
			int clamped = NumberUtils.clamp(index, 1, pFlow.getMaxPage());
			goToPage(clamped);
		}

		@Override
		public void dispose() {
			super.dispose();
			pFlow = null;
		}
	}

	/**
	 * Concrete implementation of {@link PaginatedHelper} to be used with {@link PaginatedVirtualFlow} when its
	 * {@link PaginatedVirtualFlow#orientationProperty()} is VERTICAL, extends {@link VerticalHelper}.
	 */
	class PaginatedVerticalHelper extends VerticalHelper implements PaginatedHelper {
		protected PaginatedVirtualFlow<?, ?> pFlow;

		public PaginatedVerticalHelper(PaginatedVirtualFlow<?, ?> virtualFlow, ViewportManager<?, ?> viewportManager) {
			super(virtualFlow, viewportManager);
			this.pFlow = virtualFlow;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Overridden to return {@link PaginatedVirtualFlow#cellsPerPageProperty()}.
		 */
		@Override
		public int maxCells() {
			return pFlow.getCellsPerPage();
		}

		/**
		 * Overridden to return the pixel position of the last page.
		 */
		@Override
		public double maxVScroll() {
			return pageToPos(pFlow.getMaxPage());
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This is given by {@code (page - 1) * cellsPerPage * cellSize}.
		 */
		@Override
		public double pageToPos(int page) {
			return (page - 1) * pFlow.getCellsPerPage() * pFlow.getCellSize();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Implemented to use both {@link #pageToPos(int)} and {@link #scrollToPixel(double)}.
		 */
		@Override
		public void goToPage(int page) {
			scrollToPixel(pageToPos(page));
		}

		/**
		 * This is unsupported as the {@link PaginatedVirtualFlow} can only scroll to certain pixel values, given
		 * by the pages.
		 *
		 * @throws UnsupportedOperationException
		 */
		@Override
		public void scrollBy(double pixels) {
			throw new UnsupportedOperationException("The paginated flow cannot scroll by a given amount of pixels");
		}

		/**
		 * Basically the same as {@link #goToPage(int)}, the index parameter is always clamped
		 * between 1 and {@link PaginatedVirtualFlow#maxPageProperty()}.
		 */
		@Override
		public void scrollToIndex(int index) {
			int clamped = NumberUtils.clamp(index, 1, pFlow.getMaxPage());
			goToPage(clamped);
		}

		@Override
		public void dispose() {
			super.dispose();
			pFlow = null;
		}
	}
}
