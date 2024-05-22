package io.github.palexdev.virtualizedfx.list.paginated;

import io.github.palexdev.virtualizedfx.cells.base.Cell;
import io.github.palexdev.virtualizedfx.list.VFXListHelper;

/**
 * Simple extension of {@link VFXListHelper} with to concrete implementations {@link VerticalHelper} and {@link HorizontalHelper}
 * to override the behavior of {@link #visibleNum()}, so that it always returns the value of cells per page
 * ({@link VFXPaginatedList#cellsPerPageProperty()}).
 */
public interface VFXPaginatedListHelper<T, C extends Cell<T>> extends VFXListHelper<T, C> {

	class VerticalHelper<T, C extends Cell<T>> extends VFXListHelper.VerticalHelper<T, C> implements VFXPaginatedListHelper<T, C> {
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

	class HorizontalHelper<T, C extends Cell<T>> extends VFXListHelper.HorizontalHelper<T, C> implements VFXPaginatedListHelper<T, C> {
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
