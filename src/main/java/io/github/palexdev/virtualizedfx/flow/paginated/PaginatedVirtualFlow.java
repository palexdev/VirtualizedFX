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

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableIntegerProperty;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.flow.OrientationHelper;
import io.github.palexdev.virtualizedfx.flow.ViewportState;
import io.github.palexdev.virtualizedfx.flow.VirtualFlow;
import io.github.palexdev.virtualizedfx.flow.paginated.PaginatedHelper.PaginatedHorizontalHelper;
import io.github.palexdev.virtualizedfx.flow.paginated.PaginatedHelper.PaginatedVerticalHelper;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Orientation;
import javafx.scene.control.Skin;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Extension of {@link VirtualFlow} to offer pagination features.
 * <p></p>
 * In addition to all inherited features this adds:
 * <p> - The current displayed page, {@link #currentPageProperty()}
 * <p> - The max number of pages, {@link #maxPageProperty()}
 * <p> - The number of cells per page, {@link #cellsPerPageProperty()}. Note that this is also
 * settable via CSS
 * <p>
 * Note that pages start from index 1.
 * <p></p>
 * This flow also has its own skin, which is basically the same as {@code VirtualFlow} but adapted to
 * resize the control depending on the {@link #cellsPerPageProperty()}.
 * <p></p>
 * Last but not least a word of <b>warning</b>.
 * <p>
 * Note that this is a naive implementation. Some things/components, like the {@link ViewportState} class, have been
 * adapted to work with this, but if you are not careful you could easily break or mess things up.
 * <p>
 * For example: the correct way to scroll in this flow is to change the current page property, but nothing prevents you
 * from using methods such as {@link #setVPos(double)} and {@link #setHPos(double)}.
 * To be more precise you can and must use them, but not both at the same time. What I mean is: if the orientation
 * is VERTICAL and your cells have variable width then you probably want to adjust the hPos. Same thing applies for
 * HORIZONTAL orientation but for vPos.
 * <p>
 * This flow is intended to use implementations of {@link PaginatedHelper} as utilities for the orientation. Again, nothing
 * prevents you from setting a {@link #orientationHelperFactoryProperty()} that only implements {@link OrientationHelper},
 * don't do that! In such case note also that {@link #goToPage(int)} won't work and will end with an exception.
 */
public class PaginatedVirtualFlow<T, C extends Cell<T>> extends VirtualFlow<T, C> {
	//================================================================================
	// Properties
	//================================================================================
	private final String STYLE_CLASS = "paginated-virtual-flow";

	private final IntegerProperty currentPage = new SimpleIntegerProperty() {
		@Override
		protected void invalidated() {
			int page = get();
			goToPage(page);
		}
	};
	private final ReadOnlyIntegerWrapper maxPage = new ReadOnlyIntegerWrapper() {
		@Override
		protected void invalidated() {
			int max = get();
			int curr = NumberUtils.clamp(getCurrentPage(), 0, max);
			System.out.println(curr);
			setCurrentPage(curr);
		}
	};

	//================================================================================
	// Constructors
	//================================================================================
	public PaginatedVirtualFlow() {
		super();
		initialize();
	}

	public PaginatedVirtualFlow(ObservableList<T> items, Function<T, C> cellFactory) {
		super(items, cellFactory);
		initialize();
	}

	public PaginatedVirtualFlow(ObservableList<T> items, Function<T, C> cellFactory, Orientation orientation) {
		super(items, cellFactory, orientation);
		initialize();
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().add(STYLE_CLASS);
		setOrientationHelperFactory(o ->
				(o == Orientation.HORIZONTAL) ?
						new PaginatedHorizontalHelper(this, getViewportManager()) :
						new PaginatedVerticalHelper(this, getViewportManager()));
	}

	/**
	 * Gets the current {@link PaginatedHelper} and calls {@link PaginatedHelper#goToPage(int)}, but
	 * before doing so it ensures that the max page is correct by calling {@link #updateMaxPage()}.
	 */
	public void goToPage(int page) {
		OrientationHelper helper = getOrientationHelper();
		if (!(helper instanceof PaginatedHelper))
			throw new IllegalStateException("The virtual flow's OrientationHelper is not of type PaginatedHelper!");

		updateMaxPage();
		PaginatedHelper pHelper = (PaginatedHelper) helper;
		pHelper.goToPage(page);
	}

	/**
	 * Responsible for updating {@link #maxPageProperty()} when needed.
	 * <p>
	 * The value is given by {@code Math.ceil(nItems / cellsPerPage)}.
	 */
	public void updateMaxPage() {
		int items = getItems().size();
		int cpp = getCellsPerPage();
		int max = (int) Math.ceil(items / (double) cpp);
		setMaxPage(max);
	}

	/**
	 * Returns the range of displayed items in the current page.
	 * It is preferable to use this instead of {@link ViewportState#getRange()} as this range
	 * doesn't take into account the cells that have been hidden, see {@link ViewportState#computePaginatedPositions()}.
	 * <p></p>
	 * In case the current {@link #orientationHelperProperty()} is null, returns {@code IntegerRange.of(-1)}.
	 */
	public IntegerRange getRange() {
		OrientationHelper helper = getOrientationHelper();
		if (helper == null) return IntegerRange.of(-1);

		int first = helper.firstVisible();
		int last = Math.min(first + helper.maxCells() - 1, getItems().size() - 1);
		return IntegerRange.of(first, last);
	}

	/**
	 * Returns a map containing all the currently visible cells in the page.
	 * It is preferable to use this with {@link PaginatedVirtualFlow} instead of {@link #getIndexedCells()} because
	 * of this {@link ViewportState#computePaginatedPositions()}.
	 * <p></p>
	 * In case the range is equal to {@code IntegerRange.of(-1)}, returns an empty map.
	 */
	public Map<Integer, C> getVisibleCells() {
		IntegerRange range = getRange();
		if (IntegerRange.of(-1).equals(range)) return Map.of();
		return getIndexedCells().entrySet().stream()
				.filter(e -> IntegerRange.inRangeOf(e.getKey(), range))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * Calls {@link #goToPage(int)} with 1 as parameter.
	 */
	@Override
	public void scrollToFirst() {
		goToPage(1);
	}

	/**
	 * Calls {@link #goToPage(int)} with {@link #maxPageProperty()} as parameter.
	 */
	@Override
	public void scrollToLast() {
		goToPage(getMaxPage());
	}

	@Override
	protected Skin<?> createDefaultSkin() {
		return new PaginatedVirtualFlowSkin<>(this);
	}

	@Override
	protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
		return getClassCssMetaData();
	}

	//================================================================================
	// Styleable Properties
	//================================================================================
	private final StyleableIntegerProperty cellsPerPage = new StyleableIntegerProperty(
			StyleableProperties.CELLS_PER_PAGE,
			this,
			"cellsPerPage",
			10
	) {
		@Override
		protected void invalidated() {
			updateMaxPage();
			requestViewportLayout();
		}
	};

	public int getCellsPerPage() {
		return cellsPerPage.get();
	}

	/**
	 * Specifies the number of cells to display per page.
	 * <p></p>
	 * Note that this, combined with {@link #cellSizeProperty()}, determines the size of the virtual flow.
	 */
	public StyleableIntegerProperty cellsPerPageProperty() {
		return cellsPerPage;
	}

	public void setCellsPerPage(int cellsPerPage) {
		this.cellsPerPage.set(cellsPerPage);
	}

	//================================================================================
	// CssMetaData
	//================================================================================
	private static class StyleableProperties {
		private static final StyleablePropertyFactory<PaginatedVirtualFlow<?, ?>> FACTORY = new StyleablePropertyFactory<>(VirtualFlow.getClassCssMetaData());
		private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

		private static final CssMetaData<PaginatedVirtualFlow<?, ?>, Number> CELLS_PER_PAGE =
				FACTORY.createSizeCssMetaData(
						"-fx-cells-per-page",
						PaginatedVirtualFlow::cellsPerPageProperty,
						10
				);

		static {
			cssMetaDataList = StyleUtils.cssMetaDataList(
					VirtualFlow.getClassCssMetaData(),
					CELLS_PER_PAGE
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
