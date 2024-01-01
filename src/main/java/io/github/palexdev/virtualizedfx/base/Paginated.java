package io.github.palexdev.virtualizedfx.base;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;

/**
 * Defines the common API for every paginated virtualized container offered by VirtualizedFX. Extends {@link VFXContainer}.
 */
public interface Paginated extends VFXContainer {
	int getPage();

	/**
	 * Specifies the page at which the container is.
	 */
	IntegerProperty pageProperty();

	void setPage(int page);

	int getMaxPage();

	/**
	 * Specifies the maximum page index at which the container can go.
	 */
	ReadOnlyIntegerProperty maxPageProperty();

	int getCellsPerPage();

	/**
	 * Specifies the number of cells/items to show per each page.
	 */
	IntegerProperty cellsPerPageProperty();

	void setCellsPerPage(int cellsPerPage);

	/**
	 * Goes to the next page if possible.
	 */
	default void next() {
		setPage(getPage() + 1);
	}

	/**
	 * Goes to the previous page if possible.
	 */
	default void previous() {
		setPage(getPage() - 1);
	}

	/**
	 * Changes the page by the given delta.
	 */
	default void moveBy(int delta) {
		setPage(getPage() + delta);
	}

	/**
	 * Given an index, returns the page at which it would be displayed by the container.
	 * <p></p>
	 * Note that this will never generate an exception, rather acts as follows for edge cases:
	 * <p> - empty list or max page is 0 or index < 0: returns 0
	 * <p> - index > size: returns max page
	 */
	default int findPageByIndex(int index) {
		if (isEmpty() || getMaxPage() == 0 || index < 0) return 0;
		if (index > size() - 1) return getMaxPage();
		return index / getCellsPerPage();
	}

	/**
	 * Computes the maximum page index reachable by the container. This depends on the number of items and the number
	 * of cells/items per page.
	 * <p>
	 * The exact formula is as follows: {@code Math.max(0, ((int) Math.ceil(items / (double) cpp)) - 1)}
	 */
	default int computeMaxPage() {
		int items = size();
		int cpp = getCellsPerPage();
		return Math.max(0, ((int) Math.ceil(items / (double) cpp)) - 1);
	}
}
