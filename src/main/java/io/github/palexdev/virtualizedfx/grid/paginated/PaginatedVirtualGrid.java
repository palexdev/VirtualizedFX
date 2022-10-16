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

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableIntegerProperty;
import io.github.palexdev.mfxcore.collections.ObservableGrid;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import io.github.palexdev.virtualizedfx.grid.GridHelper;
import io.github.palexdev.virtualizedfx.grid.GridRow;
import io.github.palexdev.virtualizedfx.grid.GridState;
import io.github.palexdev.virtualizedfx.grid.VirtualGrid;
import io.github.palexdev.virtualizedfx.grid.paginated.PaginatedHelper.PaginatedGridHelper;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.control.Skin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Extension of {@link VirtualGrid} to offer pagination features.
 * <p></p>
 * In addition to all inherited features this adds:
 * <p> - The current displayed page, {@link #currentPageProperty()}
 * <p> - The max number of pages, {@link #maxPageProperty()}
 * <p> - The number of rows per page, {@link #rowsPerPageProperty()}. Note that this is also
 * settable via CSS
 * <p>
 * Note that pages start from index 1.
 * <p></p>
 * This flow also has its own skin, which is basically the same as {@code VirtualFlow} but adapted to
 * resize the control depending on the {@link #rowsPerPageProperty()}.
 * <p></p>
 * Little tips and <b>warnings</b>:
 * <p>
 * Note that this is a naive implementation. Some things/components, like the {@link GridState} or {@link GridRow}, have been
 * adapted to work with this but if you are not careful you could potentially break or mess things up.
 * <p>
 * For example: the correct way to scroll is to change the current page property, but nothing prevents you to set the position
 * using the related setters.
 * To be more precise you can still set the hPos freely as pages are vertically arranged.
 * <p>
 * This grid is intended to use implementations of {@link PaginatedHelper}. Again, nothing prevents you from setting a
 * {@link #gridHelperSupplierProperty()} that only implements {@link GridHelper}, don't do that! In such case note that
 * {@link #goToPage(int)} won't work and will end with an exception.
 */
public class PaginatedVirtualGrid<T, C extends GridCell<T>> extends VirtualGrid<T, C> {
	//================================================================================
	// Properties
	//================================================================================
	private final String STYLE_CLASS = "paginated-virtual-grid";

	private final IntegerProperty currentPage = new SimpleIntegerProperty(1) {
		@Override
		public void set(int newValue) {
			int clamped = NumberUtils.clamp(newValue, 1, getMaxPage());
			super.set(clamped);
		}

		@Override
		protected void invalidated() {
			int page = get();
			changePage(page);
		}
	};

	private final ReadOnlyIntegerWrapper maxPage = new ReadOnlyIntegerWrapper() {
		@Override
		public void set(int newValue) {
			int clamped = Math.max(1, newValue);
			super.set(clamped);
		}

		@Override
		protected void invalidated() {
			int max = get();
			int curr = NumberUtils.clamp(getCurrentPage(), 1, max);
			setCurrentPage(curr);
		}
	};

	//================================================================================
	// Constructors
	//================================================================================
	public PaginatedVirtualGrid() {
		super();
		initialize();
	}

	public PaginatedVirtualGrid(ObservableGrid<T> items, Function<T, C> cellFactory) {
		super(items, cellFactory);
		initialize();
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().add(STYLE_CLASS);
		setGridHelperSupplier(() -> new PaginatedGridHelper(this));
	}

	/**
	 * Shortcut for {@link #setRowsPerPage(int)}.
	 * When the {@link #currentPageProperty()} is invalidated {@link #changePage(int)} is automatically called
	 */
	public void goToPage(int page) {
		setCurrentPage(page);
	}

	/**
	 * Shortcut for {@link #goToPage(int)} with 1 as parameter.
	 */
	public void goToFirstPage() {
		goToPage(1);
	}

	/**
	 * Shortcut for {@link #goToPage(int)} with {@link #getMaxPage()} as parameter.
	 */
	public void goToLastPage() {
		goToPage(getMaxPage());
	}

	/**
	 * Responsible for updating {@link #maxPageProperty()} when needed.
	 * <p>
	 * The value is given by {@code Math.ceil(rows / rowsPerPage)}.
	 */
	public void updateMaxPage() {
		int rows = getRowsNum();
		int rpp = getRowsPerPage();
		int max = (int) Math.ceil(rows / (double) rpp);
		setMaxPage(max);
	}

	/**
	 * Gets the current {@link PaginatedHelper} and calls {@link PaginatedHelper#goToPage(int)}, but
	 * before doing so it ensures that the max page is correct by calling {@link #updateMaxPage()}.
	 */
	protected void changePage(int page) {
		GridHelper helper = getGridHelper();
		if (!(helper instanceof PaginatedHelper))
			throw new IllegalStateException("The grid's helper is not of type PaginatedHelper!");

		updateMaxPage();
		PaginatedHelper pHelper = (PaginatedHelper) helper;
		pHelper.goToPage(page);
	}

	/**
	 * Returns the range of displayed rows in the current page.
	 * <p>
	 * It is preferable to use this instead of {@link GridState#getRowsRange()} as this range doesn't take
	 * into account the cells that have been hidden, see {@link GridState#layoutPaginatedRows()}.
	 * <p></p>
	 * In case the current {@link #gridHelperProperty()} is null, returns {@code IntegerRange.of(-1)}.
	 */
	public IntegerRange getRowsRange() {
		return Optional.ofNullable(getGridHelper())
				.map(h -> {
					int firstRow = h.firstRow();
					int lastRow = Math.min(firstRow + h.maxRows() - 1, getRowsNum() - 1);
					return IntegerRange.of(firstRow, lastRow);
				})
				.orElseGet(() -> IntegerRange.of(-1));
	}

	/**
	 * Returns a map containing all the currently visible cells in the page.
	 * This is build starting from {@link GridState#getIndexedCells()} and then filtering by the
	 * computed range, {@link #getRowsRange()}.
	 * <p></p>
	 * It is preferable to use this with {@link PaginatedVirtualGrid} instead of {@link #getIndexedCells()} because
	 * of this {@link GridState#layoutPaginatedRows()}
	 * <p></p>
	 * In case the range is equal to {@code IntegerRange.of(-1)}, returns an empty map.
	 */
	public Map<Integer, C> getIndexedVisibleCells() {
		IntegerRange range = getRowsRange();
		if (IntegerRange.of(-1).equals(range)) return Map.of();
		return getIndexedCells().entrySet().stream()
				.filter(e -> IntegerRange.inRangeOf(e.getKey(), range))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * The paginated grid cannot scroll to a desired row.
	 *
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void scrollToFirstRow() {
		throw new UnsupportedOperationException("The paginated grid cannot scroll to a desired row");
	}

	/**
	 * The paginated grid cannot scroll to a desired row.
	 *
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void scrollToLastRow() {
		throw new UnsupportedOperationException("The paginated grid cannot scroll to a desired row");
	}

	@Override
	protected void onCellSizeChanged() {
		GridHelper helper = getGridHelper();
		if (helper != null) {
			helper.computeEstimatedSize();
		}

		if (getWidth() != 0.0 && getHeight() != 0.0) { // TODO test with w and h = 0 initially
			if (!getViewportManager().init()) {
				requestViewportLayout();
			} else {
				goToPage(1);
				scrollToColumn(0);
			}
		}
	}

	@Override
	protected Skin<?> createDefaultSkin() {
		return new PaginatedVirtualGridSkin<>(this);
	}

	@Override
	protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
		return getClassCssMetaData();
	}

	//================================================================================
	// Styleable Properties
	//================================================================================
	private final StyleableIntegerProperty rowsPerPage = new StyleableIntegerProperty(
			PaginatedVirtualGrid.StyleableProperties.ROWS_PER_PAGE,
			this,
			"rowsPerPage",
			5
	) {
		@Override
		protected void invalidated() {
			updateMaxPage();
			changePage(getCurrentPage());
			requestViewportLayout();
		}
	};

	public int getRowsPerPage() {
		return rowsPerPage.get();
	}

	/**
	 * Specifies the number of rows to display per page.
	 * <p></p>
	 * Note that this, combined with {@link #cellSizeProperty()}, determines the height of the virtual grid.
	 */
	public StyleableIntegerProperty rowsPerPageProperty() {
		return rowsPerPage;
	}

	public void setRowsPerPage(int rowsPerPage) {
		this.rowsPerPage.set(rowsPerPage);
	}

	//================================================================================
	// CssMetaData
	//================================================================================
	private static class StyleableProperties {
		private static final StyleablePropertyFactory<PaginatedVirtualGrid<?, ?>> FACTORY = new StyleablePropertyFactory<>(VirtualGrid.getClassCssMetaData());
		private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

		private static final CssMetaData<PaginatedVirtualGrid<?, ?>, Number> ROWS_PER_PAGE =
				FACTORY.createSizeCssMetaData(
						"-fx-rows-per-page",
						PaginatedVirtualGrid::rowsPerPageProperty,
						5
				);

		static {
			cssMetaDataList = StyleUtils.cssMetaDataList(
					VirtualGrid.getClassCssMetaData(),
					ROWS_PER_PAGE
			);
		}
	}

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return StyleableProperties.cssMetaDataList;
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

	public int getCurrentPage() {
		return currentPage.get();
	}

	/**
	 * Specifies the current displayed page.
	 */
	public IntegerProperty currentPageProperty() {
		return currentPage;
	}

	public void setCurrentPage(int currentPage) {
		this.currentPage.set(currentPage);
	}

	public int getMaxPage() {
		return maxPage.get();
	}

	/**
	 * Specifies the maximum number of pages, aka last page number.
	 */
	public ReadOnlyIntegerProperty maxPageProperty() {
		return maxPage.getReadOnlyProperty();
	}

	protected void setMaxPage(int maxPage) {
		this.maxPage.set(maxPage);
	}
}
