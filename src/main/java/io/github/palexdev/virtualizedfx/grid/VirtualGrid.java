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

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.NumberRange;
import io.github.palexdev.mfxcore.base.properties.functional.FunctionProperty;
import io.github.palexdev.mfxcore.base.properties.functional.SupplierProperty;
import io.github.palexdev.mfxcore.base.properties.range.IntegerRangeProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableDoubleProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableSizeProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableSizeProperty.SizeConverter;
import io.github.palexdev.mfxcore.collections.Grid;
import io.github.palexdev.mfxcore.collections.ObservableGrid;
import io.github.palexdev.mfxcore.enums.GridChangeType;
import io.github.palexdev.mfxcore.utils.NumberUtils;
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
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementation of a virtual grid to virtualize the display of tabular data.
 * <p>
 * Extends {@link Control}, has its own skin implementation {@link VirtualGridSkin} and its own cell system,
 * {@link GridCell}.
 * <p>
 * The data structure used by this control is a custom one that is specifically made to simplify operations on rows and
 * columns, a dynamic 2D array. In reality such structure is backed by a 1D collection, a simple {@link List}, so
 * operations are always linear, see {@link Grid} and {@link ObservableGrid}.
 * <p></p>
 * These are all the features:
 * <p> - The items data structure is managed automatically, for all changes that may occur see {@link GridChangeType}
 * <p> - The function used to generate cells, also called "cellFactory" can be changed anytime, even at runtime
 * <p> - The control also manages the size of all the cells through the {@link #cellSizeProperty()}, which is also
 * settable via CSS
 * <p> - You can programmatically set the position of the viewport through a series of public methods
 * <p> - It is possible to retrieve the current shown/built cells as well as other information regarding the state of
 * the viewport through the {@link #stateProperty()}
 * <p> - It is possible to observe for changes to the estimated size of the viewport through the {@link #estimatedSizeProperty()}
 * <p> - It is possible to programmatically tell the viewport to update its layout with {@link #requestViewportLayout()}
 * <p></p>
 * Last but not least, a few notes about the backing data structure. As mentioned before the {@code VirtualGrid} uses
 * a particular dynamic 2D structure which is {@link ObservableGrid}. About the performance: operations on rows are generally
 * faster since the backing data structure is a linear 1D list; operations on columns on the other hand are quite expensive
 * since it will almost always require a "complete" update of the viewport, to be precise, most of the cells will just
 * need an index update, whereas just a few of them may require both an index and item update. Nonetheless, computing
 * changes like a column addition or removal is always a quite complex and expensive operation.
 *
 * @param <T> the type of objects to represent
 * @param <C> the type of {@code GridCell} to use
 */
public class VirtualGrid<T, C extends GridCell<T>> extends Control {
	//================================================================================
	// Properties
	//================================================================================
	private final String STYLE_CLASS = "virtual-grid";
	private final ViewportManager<T, C> manager = new ViewportManager<>(this);

	private final ObjectProperty<ObservableGrid<T>> items = new SimpleObjectProperty<>(new ObservableGrid<>()) {
		@Override
		public void set(ObservableGrid<T> newValue) {
			super.set(newValue == null ? new ObservableGrid<>() : newValue);
		}
	};
	private final FunctionProperty<T, C> cellFactory = new FunctionProperty<>();

	private final ObjectProperty<Position> position = new SimpleObjectProperty<>(Position.of(0, 0)) {
		@Override
		public void set(Position newValue) {
			if (newValue == null) {
				super.set(Position.of(0, 0));
				return;
			}

			GridHelper helper = getGridHelper();
			if (helper == null) {
				super.set(newValue);
				return;
			}

			double x = NumberUtils.clamp(newValue.getX(), 0, helper.maxHScroll());
			double y = NumberUtils.clamp(newValue.getY(), 0, helper.maxVScroll());
			newValue.setX(x);
			newValue.setY(y);
			super.set(newValue);
		}
	};

	private final ReadOnlyObjectWrapper<GridHelper> gridHelper = new ReadOnlyObjectWrapper<>() {
		@Override
		public void set(GridHelper newValue) {
			GridHelper oldValue = get();
			if (oldValue != null) oldValue.dispose();
			super.set(newValue);
		}
	};
	private final SupplierProperty<GridHelper> gridHelperSupplier = new SupplierProperty<>() {
		@Override
		protected void invalidated() {
			GridHelper helper = get().get();
			setGridHelper(helper);
		}
	};

	private final ObjectProperty<Size> estimatedSize = new SimpleObjectProperty<>(Size.of(0, 0));
	private final ReadOnlyBooleanWrapper needsViewportLayout = new ReadOnlyBooleanWrapper(false);

	//================================================================================
	// Constructors
	//================================================================================
	public VirtualGrid() {
		initialize();
	}

	public VirtualGrid(ObservableGrid<T> items, Function<T, C> cellFactory) {
		initialize();
		setItems(items);
		setCellFactory(cellFactory);
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().add(STYLE_CLASS);
		setGridHelperSupplier(() -> new DefaultGridHelper(this));
		setGridHelper(getGridHelperSupplier().get());
	}

	/**
	 * Basically a setter for {@link #needsViewportLayoutProperty()}.
	 * <p>
	 * This sets the property to true causing the virtual grid skin to catch the change through a listener
	 * which then tells the viewport to recompute the layout.
	 */
	public void requestViewportLayout() {
		setNeedsViewportLayout(true);
	}

	/**
	 * This method is called every time the {@link #cellSizeProperty()} changes, and is responsible
	 * for updating the viewport. Different implementations may take different approaches as how to
	 * update it.
	 */
	protected void onCellSizeChanged() {
		GridHelper helper = getGridHelper();
		helper.computeEstimatedSize();

		if (getWidth() != 0.0 && getHeight() != 0.0) {
			if (!manager.init()) {
				requestViewportLayout();
			} else {
				setPosition(0, 0);
			}
		}
	}

	//================================================================================
	// Delegate Methods
	//================================================================================

	/**
	 * Delegate for {@link ObservableGrid#init()}
	 */
	public ObservableGrid<T> init() {
		return getItems().init();
	}

	/**
	 * Delegate for {@link ObservableGrid#init(int, int)}
	 */
	public ObservableGrid<T> init(int rows, int columns) {
		return getItems().init(rows, columns);
	}

	/**
	 * Delegate for {@link ObservableGrid#init(int, int, Object)}
	 */
	public ObservableGrid<T> init(int rows, int columns, T val) {
		return getItems().init(rows, columns, val);
	}

	/**
	 * Delegate for {@link ObservableGrid#init(int, int, BiFunction)}
	 */
	public ObservableGrid<T> init(int rows, int columns, BiFunction<Integer, Integer, T> valFunction) {
		return getItems().init(rows, columns, valFunction);
	}

	/**
	 * Delegate for {@link ObservableGrid#getRowsNum()}
	 */
	public int getRowsNum() {
		return getItems().getRowsNum();
	}

	/**
	 * Delegate for {@link ObservableGrid#getColumnsNum()}
	 */
	public int getColumnsNum() {
		return getItems().getColumnsNum();
	}

	/**
	 * Delegate for {@link ObservableGrid#clear()}
	 */
	public void clear() {
		getItems().clear();
	}

	/**
	 * Delegate for {@link ObservableGrid#totalSize()}
	 */
	public int totalSize() {
		return getItems().totalSize();
	}

	/**
	 * Delegate for {@link ObservableGrid#size()}
	 */
	public Pair<Integer, Integer> size() {
		return getItems().size();
	}

	/**
	 * Delegate for {@link ObservableGrid#isEmpty()}
	 */
	public boolean isEmpty() {
		return getItems().isEmpty();
	}

	public Size getEstimatedSize() {
		return estimatedSize.get();
	}

	/**
	 * Specifies the total virtual size of the viewport as a {@link Size} object.
	 */
	public ReadOnlyObjectProperty<Size> estimatedSizeProperty() {
		return estimatedSize;
	}

	public GridState<T, C> getState() {
		return manager.getState();
	}

	/**
	 * Carries the {@link GridState} object which represents the current state of the viewport.
	 * This property is useful to catch any change happening in the viewport, and carries valuable information.
	 */
	public ReadOnlyObjectProperty<GridState<T, C>> stateProperty() {
		return manager.stateProperty().getReadOnlyProperty();
	}

	public NumberRange<Integer> getLastRowsRange() {
		return manager.getLastRowsRange();
	}

	/**
	 * Specifies the last range of displayed rows by the viewport as an {@link IntegerRange}.
	 */
	public IntegerRangeProperty lastRowsRangeProperty() {
		return manager.lastRowsRangeProperty();
	}

	public NumberRange<Integer> getLastColumnsRange() {
		return manager.getLastColumnsRange();
	}

	/**
	 * Specifies the last range of displayed columns by the viewport as an {@link IntegerRange}.
	 */
	public IntegerRangeProperty lastColumnsRangeProperty() {
		return manager.lastColumnsRangeProperty();
	}

	/**
	 * Delegate for {@link GridState#getIndexedCells()}.
	 */
	public Map<Integer, C> getIndexedCells() {
		return getState().getIndexedCells();
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
			Size.of(100, 100)
	) {
		@Override
		protected void invalidated() {
			onCellSizeChanged();
		}
	};

	private final StyleableDoubleProperty clipBorderRadius = new StyleableDoubleProperty(
			StyleableProperties.CLIP_BORDER_RADIUS,
			this,
			"clipBorderRadius",
			0.0
	);


	public Size getCellSize() {
		return cellSize.get();
	}

	/**
	 * Specifies the size of the cells as a {@link Size}.
	 * <p>
	 * It is also possible to set this property via CSS with the {@code "-fx-cell-size"} property,
	 * check {@link StyleableSizeProperty} and {@link SizeConverter} for more info.
	 */
	public StyleableSizeProperty cellSizeProperty() {
		return cellSize;
	}

	public void setCellSize(Size cellSize) {
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

		private static final CssMetaData<VirtualGrid<?, ?>, Size> CELL_SIZE =
				new CssMetaData<>("-fx-cell-size", SizeConverter.getInstance(), Size.of(100, 100)) {
					@Override
					public boolean isSettable(VirtualGrid<?, ?> styleable) {
						return !styleable.cellSizeProperty().isBound();
					}

					@Override
					public StyleableProperty<Size> getStyleableProperty(VirtualGrid<?, ?> styleable) {
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

	/**
	 * @return the {@link ViewportManager} instance for this {@code VirtualGrid}
	 */
	protected ViewportManager<T, C> getViewportManager() {
		return manager;
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

	/**
	 * Shortcut for {@link #setPosition(double, double)}, which uses the current hPos as the x value for the new
	 * {@link Position} object.
	 */
	public void setVPos(double vPos) {
		setPosition(getHPos(), vPos);
	}

	/**
	 * @return the vertical position of the viewport
	 */
	public double getVPos() {
		return getPosition().getY();
	}

	/**
	 * Shortcut for {@link #setPosition(double, double)}, which uses the current vPos as the y value for the new
	 * {@link Position} object.
	 */
	public void setHPos(double hPos) {
		setPosition(hPos, getVPos());
	}

	/**
	 * @return the horizontal position of the viewport
	 */
	public double getHPos() {
		return getPosition().getX();
	}

	public Position getPosition() {
		return position.get();
	}

	/**
	 * Specifies the current position of the viewport as a {@link Position} object which has
	 * both the x and y positions.
	 */
	public ObjectProperty<Position> positionProperty() {
		return position;
	}

	/**
	 * Convenience method, alternative to {@link #setPosition(Position)}.
	 */
	public void setPosition(double x, double y) {
		setPosition(Position.of(x, y));
	}

	public void setPosition(Position position) {
		this.position.set(position);
	}

	public GridHelper getGridHelper() {
		return gridHelper.get();
	}

	/**
	 * The current built helper for the grid, see {@link GridHelper}.
	 */
	public ReadOnlyObjectProperty<GridHelper> gridHelperProperty() {
		return gridHelper.getReadOnlyProperty();
	}

	protected void setGridHelper(GridHelper helper) {
		this.gridHelper.set(helper);
	}

	/**
	 * To allow more customization on how the cells are laid out, the virtual grid allows you
	 * to specify a supplier used to build a {@link GridHelper} which is responsible for some core actions
	 * about cells and layout. This way you could implement your own helper and set it through this factory since the
	 * {@link #gridHelperProperty()} is intended to be read-only.
	 */
	public Supplier<GridHelper> getGridHelperSupplier() {
		return gridHelperSupplier.get();
	}

	public SupplierProperty<GridHelper> gridHelperSupplierProperty() {
		return gridHelperSupplier;
	}

	public void setGridHelperSupplier(Supplier<GridHelper> gridHelperSupplier) {
		this.gridHelperSupplier.set(gridHelperSupplier);
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
