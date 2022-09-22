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

package io.github.palexdev.virtualizedfx.grid;

import io.github.palexdev.mfxcore.base.beans.SizeBean;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.NumberRange;
import io.github.palexdev.mfxcore.base.properties.functional.FunctionProperty;
import io.github.palexdev.mfxcore.base.properties.functional.SupplierProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableDoubleProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableSizeProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableSizeProperty.SizeBeanConverter;
import io.github.palexdev.mfxcore.collections.Grid;
import io.github.palexdev.mfxcore.collections.ObservableGrid;
import io.github.palexdev.mfxcore.enums.GridChangeType;
import io.github.palexdev.mfxcore.utils.fx.PropUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.grid.GridHelper.DefaultGridHelper;
import javafx.beans.property.*;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Orientation;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.shape.Rectangle;
import javafx.util.Pair;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Basic implementation of a virtual grid for virtualized tabular data.
 * <p>
 * Extends {@link Control}, has its own skin implementation {@link VirtualGridSkin} and its own
 * cell system, {@link GridCell}.
 * <p>
 * The data structure used by this control is a custom one that is specifically made to simplify
 * operations on rows and columns, a dynamic 2D array. In reality such structure is backed by a 1D collection,
 * a simple {@link List}, so operations are always linear, see {@link Grid} and {@link ObservableGrid}.
 * <p></p>
 * These are all the features:
 * <p> - The items data structure is managed automatically, for all changes that may occur see {@link GridChangeType}
 * <p> - The function used to generate the cells, also called "cellFactory" can be changed anytime even at runtime
 * <p> - The control also manages the size of all the cells through the {@link #cellSizeProperty()}, which is also settable
 * via CSS, see {@link #cellSizeProperty()}
 * <p> - You can set the position (scroll) through the properties or a series of public methods
 * <p> - It is possible to retrieve the current shown/built cells as well as all the other information regarding the state
 * of the viewport through the {@link #stateProperty()}
 * <p> - It is possible to observe for changes to the estimated length or breadth through the dedicated properties,
 * {@link #estimatedLengthProperty()}, {@link #estimatedBreadthProperty()}
 * <p> - It is possible to programmatically tell the viewport to update its layout with {@link #requestViewportLayout()}
 * <p></p>
 * Last but not least, a few notes about the backing data structure. As mentioned before the {@code VirtualGrid} uses
 * a particular dynamic 2D structure which is {@link ObservableGrid}. About the performance: operations on rows are generally
 * faster since the backing data structure is a linear 1D list; operations on columns on the other hand are quite expensive
 * since it will almost always require a "complete" update of the viewport, to be precise, most of the cells will just need
 * an index update, whereas just a few of them will require both an index and item update. Nonetheless, computing such changes
 * is always a quite complex and expensive operation.
 *
 * @param <T> the type of objects to represent
 * @param <C> the type of {@code GridCell} to use
 */
public class VirtualGrid<T, C extends GridCell<T>> extends Control {
	//================================================================================
	// Properties
	//================================================================================
	private final String STYLE_CLASS = "virtual-grid";
	private final ViewportManager<T, C> viewportManager = new ViewportManager<>(this);

	private final ObjectProperty<ObservableGrid<T>> items = new SimpleObjectProperty<>(new ObservableGrid<>()) {
		@Override
		public void set(ObservableGrid<T> newValue) {
			ObservableGrid<T> val = (newValue != null) ? newValue : new ObservableGrid<>();
			super.set(val);
		}
	};
	private final FunctionProperty<T, C> cellFactory = new FunctionProperty<>();

	private final DoubleProperty vPos = PropUtils.clampedDoubleProperty(
			() -> 0.0,
			() -> (getGridHelper() != null) ? getGridHelper().maxVScroll() : 0.0
	);
	private final DoubleProperty hPos = PropUtils.clampedDoubleProperty(
			() -> 0.0,
			() -> (getGridHelper() != null) ? getGridHelper().maxHScroll() : 0.0
	);

	private final ObjectProperty<GridHelper> gridHelper = new SimpleObjectProperty<>() {
		@Override
		public void set(GridHelper newValue) {
			GridHelper oldValue = get();
			if (oldValue != null) oldValue.dispose();
			super.set(newValue);
		}
	};
	private final SupplierProperty<GridHelper> gridHelperFactory = new SupplierProperty<>(() -> new DefaultGridHelper(this)) {
		@Override
		protected void invalidated() {
			GridHelper helper = get().get();
			setGridHelper(helper);
		}
	};

	private final DoubleProperty estimatedLength = new SimpleDoubleProperty();
	private final DoubleProperty estimatedBreadth = new SimpleDoubleProperty();
	private final ReadOnlyBooleanWrapper needsViewportLayout = new ReadOnlyBooleanWrapper(false);

	//================================================================================
	// Constructors
	//================================================================================
	public VirtualGrid() {
		initialize();
	}

	public VirtualGrid(ObservableGrid<T> items, Function<T, C> cellFactory) {
		setItems(items);
		setCellFactory(cellFactory);
		initialize();
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().add(STYLE_CLASS);
		setGridHelper(getGridHelperFactory().get());
	}

	/**
	 * Basically a setter for {@link #needsViewportLayoutProperty()}.
	 * This sets the property to true causing the virtual grid skin to catch the change through a listener
	 * which then orders the viewport container to re-compute the layout.
	 */
	public void requestViewportLayout() {
		setNeedsViewportLayout(true);
	}

	protected void cellSizeChanged() {
		GridHelper helper = getGridHelper();
		if (helper != null) {
			helper.computeEstimatedLength();
			helper.computeEstimatedBreadth();
		}

		if (getWidth() != 0.0 && getHeight() != 0.0) { // TODO test with w and h = 0 initially
			viewportManager.init();
			scrollToRow(0);
			scrollToColumn(0);
		}
	}

	//================================================================================
	// Delegate Methods
	//================================================================================

	/**
	 * Delegate method for {@link ObservableGrid#init()}
	 */
	public ObservableGrid<T> init() {
		return getItems().init();
	}

	/**
	 * Delegate method for {@link ObservableGrid#init(int, int)}
	 */
	public ObservableGrid<T> init(int rows, int columns) {
		return getItems().init(rows, columns);
	}

	/**
	 * Delegate method for {@link ObservableGrid#init(int, int, Object)}
	 */
	public ObservableGrid<T> init(int rows, int columns, T val) {
		return getItems().init(rows, columns, val);
	}

	/**
	 * Delegate method for {@link ObservableGrid#init(int, int, BiFunction)}
	 */
	public ObservableGrid<T> init(int rows, int columns, BiFunction<Integer, Integer, T> valFunction) {
		return getItems().init(rows, columns, valFunction);
	}

	/**
	 * Delegate method for {@link ObservableGrid#clear()}
	 */
	public void clear() {
		getItems().clear();
	}

	/**
	 * Delegate method for {@link ObservableGrid#totalSize()}
	 */
	public int totalSize() {
		return getItems().totalSize();
	}

	/**
	 * Delegate method for {@link ObservableGrid#size()}
	 */
	public Pair<Integer, Integer> size() {
		return getItems().size();
	}

	/**
	 * Delegate method for {@link ObservableGrid#isEmpty()}
	 */
	public boolean isEmpty() {
		return getItems().isEmpty();
	}

	/**
	 * Delegate method for {@link ObservableGrid#getRowsNum()}
	 */
	public int getRowsNum() {
		return getItems().getRowsNum();
	}

	/**
	 * Delegate method for {@link ObservableGrid#getColumnsNum()}
	 */
	public int getColumnsNum() {
		return getItems().getColumnsNum();
	}

	public double getEstimatedLength() {
		return estimatedLength.get();
	}

	/**
	 * Specifies the total length of all the cells (height).
	 */
	public ReadOnlyDoubleProperty estimatedLengthProperty() {
		return estimatedLength;
	}

	public double getEstimatedBreadth() {
		return estimatedBreadth.get();
	}

	/**
	 * Specifies the total breadth of all the cells (width).
	 */
	public ReadOnlyDoubleProperty estimatedBreadthProperty() {
		return estimatedBreadth;
	}

	public GridState<T, C> getState() {
		return viewportManager.getState();
	}

	/**
	 * Specifies the current {@link GridState} which describes the state of the viewport. This property is useful
	 * to listen to any change happening in the viewport.
	 */
	public ReadOnlyObjectProperty<GridState<T, C>> stateProperty() {
		return viewportManager.stateProperty();
	}

	public NumberRange<Integer> getLastRowsRange() {
		return viewportManager.getLastRowsRange();
	}

	/**
	 * Specifies the last range of displayed rows by the viewport as an {@link IntegerRange}.
	 */
	public ReadOnlyObjectProperty<NumberRange<Integer>> lastRowsRangeProperty() {
		return viewportManager.lastRowsRangeProperty();
	}

	public NumberRange<Integer> getLastColumnsRange() {
		return viewportManager.getLastColumnsRange();
	}

	/**
	 * Specifies the last range of displayed columns by the viewport as an {@link IntegerRange}.
	 */
	public ReadOnlyObjectProperty<NumberRange<Integer>> lastColumnsRangeProperty() {
		return viewportManager.lastColumnsRangeProperty();
	}

	/**
	 * Scrolls to the first row.
	 */
	public void scrollToFirstRow() {
		scrollToRow(0);
	}

	/**
	 * Scrolls to the last row.
	 */
	public void scrollToLastRow() {
		scrollToRow(getItems().getRowsNum() - 1);
	}

	/**
	 * Scrolls to the given row index.
	 */
	public void scrollToRow(int index) {
		getGridHelper().scrollToRow(index);
	}

	/**
	 * Scrolls to the first columns.
	 */
	public void scrollToFirstColumn() {
		scrollToColumn(0);
	}

	/**
	 * Scrolls to the last column.
	 */
	public void scrollToLastColumn() {
		scrollToColumn(getItems().getColumnsNum() - 1);
	}

	/**
	 * Scrolls to the given column index.
	 */
	public void scrollToColumn(int index) {
		getGridHelper().scrollToColumn(index);
	}

	/**
	 * Scrolls by the given amount of pixels in the given direction.
	 *
	 * @param orientation the scroll direction
	 */
	public void scrollBy(double pixels, Orientation orientation) {
		getGridHelper().scrollBy(pixels, orientation);
	}

	/**
	 * Scrolls to the given pixel value in the given direction.
	 *
	 * @param orientation the scroll direction
	 */
	public void scrollTo(double pixel, Orientation orientation) {
		getGridHelper().scrollTo(pixel, orientation);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	protected Skin<?> createDefaultSkin() {
		return new VirtualGridSkin<>(this);
	}

	@Override
	protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
		return getClassCssMetaData();
	}

	//================================================================================
	// Styleable Properties
	//================================================================================
	private final StyleableSizeProperty cellSize = new StyleableSizeProperty(
			StyleableProperties.CELL_SIZE,
			this,
			"cellSize",
			SizeBean.of(100, 100)
	) {
		@Override
		protected void invalidated() {
			cellSizeChanged();
		}
	};

	private final StyleableDoubleProperty clipBorderRadius = new StyleableDoubleProperty(
			StyleableProperties.CLIP_BORDER_RADIUS,
			this,
			"clipBorderRadius",
			0.0
	);

	public SizeBean getCellSize() {
		return cellSize.get();
	}

	/**
	 * Specifies the size of the cells as a {@link SizeBean}.
	 * <p>
	 * It is also possible to set this property via CSS with the {@code "-fx-cell-size"} property,
	 * check {@link StyleableSizeProperty} and {@link SizeBeanConverter} for more info.
	 */
	public StyleableSizeProperty cellSizeProperty() {
		return cellSize;
	}

	public void setCellSize(SizeBean cellSize) {
		this.cellSize.set(cellSize);
	}

	public double getClipBorderRadius() {
		return clipBorderRadius.get();
	}

	/**
	 * Used by the viewport's clip to set its border radius.
	 * This is useful when you want to make a rounded virtual grid, this
	 * prevents the content from going outside the view.
	 * <p></p>
	 * This is mostly useful if not using the grid with {@link VirtualScrollPane}, this is the same as
	 * {@link VirtualScrollPane#clipBorderRadiusProperty()}.
	 * <p></p>
	 * <b>Side note:</b> the clip is a {@link Rectangle}, now for some
	 * fucking reason the rectangle's arcWidth and arcHeight values used to make
	 * it round do not act like the border-radius or background-radius properties,
	 * instead their value is usually 2 / 2.5 times the latter.
	 * So for a border radius of 5 you want this value to be at least 10/13.
	 * <p>
	 * It is also possible to set this property via CSS with the {@code "-fx-clip-border-radius"} property.
	 */
	public StyleableDoubleProperty clipBorderRadiusProperty() {
		return clipBorderRadius;
	}

	public void setClipBorderRadius(double clipBorderRadius) {
		this.clipBorderRadius.set(clipBorderRadius);
	}

	//================================================================================
	// CssMetaData
	//================================================================================
	private static class StyleableProperties {
		private static final StyleablePropertyFactory<VirtualGrid<?, ?>> FACTORY = new StyleablePropertyFactory<>(Control.getClassCssMetaData());
		private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

		private static final CssMetaData<VirtualGrid<?, ?>, SizeBean> CELL_SIZE =
				new CssMetaData<>("-fx-cell-size", SizeBeanConverter.getInstance(), SizeBean.of(100, 100)) {
					@Override
					public boolean isSettable(VirtualGrid<?, ?> styleable) {
						return !styleable.cellSizeProperty().isBound();
					}

					@Override
					public StyleableProperty<SizeBean> getStyleableProperty(VirtualGrid<?, ?> styleable) {
						return styleable.cellSizeProperty();
					}
				};

		private static final CssMetaData<VirtualGrid<?, ?>, Number> CLIP_BORDER_RADIUS =
				FACTORY.createSizeCssMetaData(
						"-fx-clip-border-radius",
						VirtualGrid::clipBorderRadiusProperty,
						0.0
				);

		static {
			cssMetaDataList = StyleUtils.cssMetaDataList(
					Control.getClassCssMetaData(),
					CELL_SIZE, CLIP_BORDER_RADIUS
			);
		}
	}

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return StyleableProperties.cssMetaDataList;
	}

	//================================================================================
	// Getters/Setters
	//================================================================================
	protected ViewportManager<T, C> getViewportManager() {
		return viewportManager;
	}

	public ObservableGrid<T> getItems() {
		return items.get();
	}

	/**
	 * Specifies the {@link ObservableGrid} used to store the items.
	 * <p>
	 * This is an {@link ObjectProperty} so that it can also be bound to other properties.
	 */
	public ObjectProperty<ObservableGrid<T>> itemsProperty() {
		return items;
	}

	public void setItems(ObservableGrid<T> items) {
		this.items.set(items);
	}

	public Function<T, C> getCellFactory() {
		return cellFactory.get();
	}

	/**
	 * Specifies the function used to build the cells.
	 */
	public FunctionProperty<T, C> cellFactoryProperty() {
		return cellFactory;
	}

	public void setCellFactory(Function<T, C> cellFactory) {
		this.cellFactory.set(cellFactory);
	}

	public double getVPos() {
		return vPos.get();
	}

	/**
	 * Specifies the vertical position in the viewport.
	 */
	public DoubleProperty vPosProperty() {
		return vPos;
	}

	public void setVPos(double vPos) {
		this.vPos.set(vPos);
	}

	public double getHPos() {
		return hPos.get();
	}

	/**
	 * Specifies the horizontal position in the viewport.
	 */
	public DoubleProperty hPosProperty() {
		return hPos;
	}

	public void setHPos(double hPos) {
		this.hPos.set(hPos);
	}

	public GridHelper getGridHelper() {
		return gridHelper.get();
	}

	/**
	 * The current built helper for the grid, see {@link GridHelper}.
	 */
	public ObjectProperty<GridHelper> gridHelperProperty() {
		return gridHelper;
	}

	protected void setGridHelper(GridHelper gridHelper) {
		this.gridHelper.set(gridHelper);
	}

	public Supplier<GridHelper> getGridHelperFactory() {
		return gridHelperFactory.get();
	}

	/**
	 * To allow more customization on how the cells are laid out, the virtual grid allows you
	 * to specify a supplier used to build a {@link GridHelper} which is responsible for some core actions
	 * about cells and layout. This way you could implement your own helper and set it through this factory since the
	 * {@link #gridHelperProperty()} is intended to be read-only.
	 */
	public SupplierProperty<GridHelper> gridHelperFactoryProperty() {
		return gridHelperFactory;
	}

	public void setGridHelperFactory(Supplier<GridHelper> gridHelperFactory) {
		this.gridHelperFactory.set(gridHelperFactory);
	}

	public boolean isNeedsViewportLayout() {
		return needsViewportLayout.get();
	}

	/**
	 * Specifies whether the viewport needs to compute the layout of its content.
	 * <p>
	 * Since this is read-only, layout requests must be sent by using {@link #requestViewportLayout()}.
	 */
	public ReadOnlyBooleanProperty needsViewportLayoutProperty() {
		return needsViewportLayout.getReadOnlyProperty();
	}

	protected void setNeedsViewportLayout(boolean needsViewportLayout) {
		this.needsViewportLayout.set(needsViewportLayout);
	}
}
