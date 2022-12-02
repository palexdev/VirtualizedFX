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

package io.github.palexdev.virtualizedfx.flow;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.NumberRange;
import io.github.palexdev.mfxcore.base.properties.functional.FunctionProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableBooleanProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableDoubleProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.mfxcore.utils.fx.PropUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.flow.OrientationHelper.HorizontalHelper;
import io.github.palexdev.virtualizedfx.flow.OrientationHelper.VerticalHelper;
import io.github.palexdev.virtualizedfx.flow.paginated.PaginatedVirtualFlow;
import io.github.palexdev.virtualizedfx.utils.VSPUtils;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Orientation;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Basic implementation of a virtual flow for virtualized list controls.
 * <p>
 * Extends {@link Control}, has its own skin implementation {@link VirtualFlowSkin} and
 * its own cell system, {@link Cell}.
 * <p></p>
 * These are all its features:
 * <p> - The items list is managed automatically (permutations, insertions, removals, updates)
 * <p> - The function used to generate the cells, also called "cellFactory", can be changed anytime even at runtime
 * <p> - The control also manages the size of all the cells through the {@link #cellSizeProperty()}
 * <p> - You can set the position (scroll) through the properties or a series of public methods
 * <p> - It can lay out the cell from the TOP to the BOTTOM or from the LEFT to the RIGHT according to the set
 * orientation, {@link #orientationProperty()}, you can swap the orientation at anytime
 * <p> - It even allows you to specify the {@link OrientationHelper} through the {@link #orientationHelperFactoryProperty()}
 * <p> - It is possible to retrieve the currently shown/built cells as a map in which each cell has
 * the item's index as its key. Just keep in mind that if you want to observe for changes in the cells it is best
 * to observe for changes on the {@link #stateProperty()} since the virtual flow rarely builds new cells and reuses
 * the one already available
 * <p> It is possible to observe for changes o the estimated length and max breadth through {@link #estimatedLengthProperty()}
 * and {@link #maxBreadthProperty()}
 * <p> It is possible to programmatically tell the viewport to update its layout with {@link #requestViewportLayout()}
 * <p></p>
 * Before proceeding with reading the methods documentations note that this new implementation
 * uses the concepts of length and breadth, simply put:
 * <p> Orientation.VERTICAL:   length -> height, breadth -> width
 * <p> Orientation.HORIZONTAL: length -> width, breadth -> height
 *
 * @param <T> the type of objects to represent
 * @param <C> the type of {@code Cell} to use
 */
public class VirtualFlow<T, C extends Cell<T>> extends Control implements VirtualScrollPane.Wrappable {
	//================================================================================
	// Properties
	//================================================================================
	private final String STYLE_CLASS = "virtual-flow";
	private final FlowManager<T, C> manager = new FlowManager<>(this);

	private final ObjectProperty<ObservableList<T>> items = new SimpleObjectProperty<>(FXCollections.observableArrayList()) {
		@Override
		public void set(ObservableList<T> newValue) {
			ObservableList<T> val = (newValue != null) ? newValue : FXCollections.observableArrayList();
			super.set(val);
		}
	};
	private final FunctionProperty<T, C> cellFactory = new FunctionProperty<>();

	private final DoubleProperty vPos = PropUtils.clampedDoubleProperty(
			() -> 0.0,
			() -> (getOrientationHelper() != null) ? getOrientationHelper().maxVScroll() : 0.0
	);
	private final DoubleProperty hPos = PropUtils.clampedDoubleProperty(
			() -> 0.0,
			() -> (getOrientationHelper() != null) ? getOrientationHelper().maxHScroll() : 0.0
	);
	private final ReadOnlyObjectWrapper<OrientationHelper> orientationHelper = new ReadOnlyObjectWrapper<>() {
		@Override
		public void set(OrientationHelper newValue) {
			OrientationHelper oldValue = get();
			if (oldValue != null) oldValue.dispose();
			super.set(newValue);
		}
	};
	private final FunctionProperty<Orientation, OrientationHelper> orientationHelperFactory = new FunctionProperty<>(o ->
			(o == Orientation.VERTICAL) ?
					new VerticalHelper(VirtualFlow.this) :
					new HorizontalHelper(VirtualFlow.this)
	) {
		@Override
		protected void invalidated() {
			Orientation orientation = getOrientation();
			OrientationHelper helper = get().apply(orientation);
			setOrientationHelper(helper);
		}
	};

	private final DoubleProperty estimatedLength = new SimpleDoubleProperty();
	private final DoubleProperty maxBreadth = new SimpleDoubleProperty();
	private final ReadOnlyBooleanWrapper needsViewportLayout = new ReadOnlyBooleanWrapper(false);

	//================================================================================
	// Constructors
	//================================================================================
	public VirtualFlow() {
		setOrientation(Orientation.VERTICAL);
		initialize();
	}

	public VirtualFlow(ObservableList<T> items, Function<T, C> cellFactory) {
		setItems(items);
		setCellFactory(cellFactory);
		setOrientation(Orientation.VERTICAL);
		initialize();
	}

	public VirtualFlow(ObservableList<T> items, Function<T, C> cellFactory, Orientation orientation) {
		setItems(items);
		setCellFactory(cellFactory);
		setOrientation(orientation);
		initialize();
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().add(STYLE_CLASS);
	}

	/**
	 * Basically a setter for {@link #needsViewportLayoutProperty()}.
	 * This sets the property to true causing the virtual flow skin to catch the change through a listener
	 * which then orders the viewport container to re-compute the layout.
	 */
	public void requestViewportLayout() {
		needsViewportLayout.set(true);
	}

	protected void cellSizeChanged() {
		OrientationHelper helper = getOrientationHelper();
		helper.computeEstimatedLength();

		if (getWidth() != 0.0 && getHeight() != 0.0) { // TODO test with w and h = 0 initially
			manager.init();
			scrollToPixel(0);
		}
	}

	//================================================================================
	// Delegate Methods
	//================================================================================

	/**
	 * Tells the current {@link #orientationHelperProperty()} to scroll
	 * by the given amount of pixels.
	 * <p>
	 * Unsupported by {@link PaginatedVirtualFlow}
	 */
	public void scrollBy(double pixels) {
		OrientationHelper helper = getOrientationHelper();
		if (helper != null) helper.scrollBy(pixels);
	}

	/**
	 * Tells the current {@link #orientationHelperProperty()} to scroll
	 * to the given pixel.
	 * <p>
	 * Unsupported by {@link PaginatedVirtualFlow}
	 */
	public void scrollToPixel(double pixel) {
		OrientationHelper helper = getOrientationHelper();
		if (helper != null) helper.scrollToPixel(pixel);
	}

	/**
	 * Tells the current {@link #orientationHelperProperty()} to scroll
	 * to the given cell index.
	 */
	public void scrollToIndex(int index) {
		OrientationHelper helper = getOrientationHelper();
		if (helper != null) helper.scrollToIndex(index);
	}

	/**
	 * Tells the current {@link #orientationHelperProperty()} to scroll
	 * to the first cell.
	 */
	public void scrollToFirst() {
		scrollToIndex(0);
	}

	/**
	 * Tells the current {@link #orientationHelperProperty()} to scroll
	 * to the last cell.
	 */
	public void scrollToLast() {
		scrollToIndex(getItems().size() - 1);
	}

	public double getEstimatedLength() {
		return estimatedLength.get();
	}

	/**
	 * Specifies the total length of all the cells.
	 */
	public ReadOnlyDoubleProperty estimatedLengthProperty() {
		return estimatedLength;
	}

	public double getMaxBreadth() {
		return maxBreadth.get();
	}

	/**
	 * Specifies the max breadth among the currently visible cells.
	 */
	public ReadOnlyDoubleProperty maxBreadthProperty() {
		return maxBreadth;
	}

	public FlowState<T, C> getState() {
		return manager.getState();
	}

	/**
	 * Specifies the current {@link FlowState} which describes the state of the viewport. This property is useful
	 * to listen to any change happening in the viewport.
	 */
	public ReadOnlyObjectProperty<FlowState<T, C>> stateProperty() {
		return manager.stateProperty();
	}

	public NumberRange<Integer> getLastRange() {
		return manager.getLastRange();
	}

	/**
	 * Specifies the last range of displayed items by the viewport as an {@link IntegerRange}.
	 */
	public ReadOnlyObjectProperty<NumberRange<Integer>> lastRangeProperty() {
		return manager.lastRangeProperty();
	}

	/**
	 * Delegate method for {@link FlowState#getCells()}.
	 */
	public Map<Integer, C> getIndexedCells() {
		return manager.getState().getCells();
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	protected Skin<?> createDefaultSkin() {
		return new VirtualFlowSkin<>(this);
	}

	@Override
	protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
		return getClassCssMetaData();
	}

	@Override
	public VirtualScrollPane wrap() {
		return VSPUtils.wrap(this);
	}

	//================================================================================
	// Styleable Properties
	//================================================================================
	private final StyleableBooleanProperty fitToBreadth = new StyleableBooleanProperty(
			StyleableProperties.FIT_TO_BREADTH,
			this,
			"fitToBreadth",
			true
	) {
		@Override
		protected void invalidated() {
			OrientationHelper helper = getOrientationHelper();
			if (helper != null) {
				helper.invalidatePos();
				requestViewportLayout();
			}
		}
	};

	private final StyleableDoubleProperty cellSize = new StyleableDoubleProperty(
			StyleableProperties.CELL_SIZE,
			this,
			"cellSize",
			32.0
	) {
		@Override
		protected void invalidated() {
			cellSizeChanged();
		}
	};

	private final StyleableObjectProperty<Orientation> orientation = new StyleableObjectProperty<>(
			StyleableProperties.ORIENTATION,
			this,
			"orientation"
	) {
		@Override
		protected void invalidated() {
			Orientation orientation = get();
			OrientationHelper helper = getOrientationHelperFactory().apply(orientation);
			setOrientationHelper(helper);
			manager.reset();
		}
	};

	private final StyleableDoubleProperty clipBorderRadius = new StyleableDoubleProperty(
			StyleableProperties.CLIP_BORDER_RADIUS,
			this,
			"clipBorderRadius",
			0.0
	);

	public boolean isFitToBreadth() {
		return fitToBreadth.get();
	}

	/**
	 * Specifies whether cells should be resized to be the same size of the viewport in the opposite
	 * direction of the current {@link #orientationProperty()}.
	 * <p>
	 * It is also possible to set this property via CSS with the {@code "-fx-fit-to-breadth"} property.
	 */
	public StyleableBooleanProperty fitToBreadthProperty() {
		return fitToBreadth;
	}

	public void setFitToBreadth(boolean fitToBreadth) {
		this.fitToBreadth.set(fitToBreadth);
	}

	public double getCellSize() {
		return cellSize.get();
	}

	/**
	 * Specifies the cells' size:
	 * <p> - Orientation.VERTICAL:   size -> height
	 * <p> - Orientation.HORIZONTAL: size -> width
	 * <p>
	 * It is also possible to set this property via CSS with the {@code "-fx-cell-size"} property.
	 */
	public StyleableDoubleProperty cellSizeProperty() {
		return cellSize;
	}

	public void setCellSize(double cellSize) {
		this.cellSize.set(cellSize);
	}

	public Orientation getOrientation() {
		return orientation.get();
	}

	/**
	 * Specifies the orientation of the virtual flow.
	 * <p>
	 * It is also possible to set this property via CSS with the {@code "-fx-orientation"} property.
	 */
	public StyleableObjectProperty<Orientation> orientationProperty() {
		return orientation;
	}

	public void setOrientation(Orientation orientation) {
		this.orientation.set(orientation);
	}

	public double getClipBorderRadius() {
		return clipBorderRadius.get();
	}

	/**
	 * Used by the viewport's clip to set its border radius.
	 * This is useful when you want to make a rounded virtual flow, this
	 * prevents the content from going outside the view.
	 * <p></p>
	 * This is mostly useful if not using the flow with {@link VirtualScrollPane}, this is the same as
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
		private static final StyleablePropertyFactory<VirtualFlow<?, ?>> FACTORY = new StyleablePropertyFactory<>(Control.getClassCssMetaData());
		private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

		private static final CssMetaData<VirtualFlow<?, ?>, Boolean> FIT_TO_BREADTH =
				FACTORY.createBooleanCssMetaData(
						"-fx-fit-to-breadth",
						VirtualFlow::fitToBreadthProperty,
						true
				);

		private static final CssMetaData<VirtualFlow<?, ?>, Number> CELL_SIZE =
				FACTORY.createSizeCssMetaData(
						"-fx-cell-size",
						VirtualFlow::cellSizeProperty,
						32.0
				);

		private static final CssMetaData<VirtualFlow<?, ?>, Orientation> ORIENTATION =
				FACTORY.createEnumCssMetaData(
						Orientation.class,
						"-fx-orientation",
						VirtualFlow::orientationProperty,
						Orientation.VERTICAL
				);

		private static final CssMetaData<VirtualFlow<?, ?>, Number> CLIP_BORDER_RADIUS =
				FACTORY.createSizeCssMetaData(
						"-fx-clip-border-radius",
						VirtualFlow::clipBorderRadiusProperty,
						0.0
				);

		static {
			cssMetaDataList = StyleUtils.cssMetaDataList(
					Control.getClassCssMetaData(),
					FIT_TO_BREADTH, CELL_SIZE, ORIENTATION, CLIP_BORDER_RADIUS
			);
		}
	}

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return StyleableProperties.cssMetaDataList;
	}

	//================================================================================
	// Getters/Setters
	//================================================================================
	protected FlowManager<T, C> getViewportManager() {
		return manager;
	}

	public ObservableList<T> getItems() {
		return items.get();
	}

	/**
	 * Specifies the {@link ObservableList} used to store the items.
	 * <p>
	 * This is an {@link ObjectProperty} so that it can also be bound to other properties.
	 */
	public ObjectProperty<ObservableList<T>> itemsProperty() {
		return items;
	}

	public void setItems(ObservableList<T> items) {
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

	public OrientationHelper getOrientationHelper() {
		return orientationHelper.get();
	}

	/**
	 * The current built helper for the current {@link #orientationProperty()},
	 * see {@link OrientationHelper}.
	 */
	public ReadOnlyObjectProperty<OrientationHelper> orientationHelperProperty() {
		return orientationHelper.getReadOnlyProperty();
	}

	protected void setOrientationHelper(OrientationHelper orientationHelper) {
		this.orientationHelper.set(orientationHelper);
	}

	public Function<Orientation, OrientationHelper> getOrientationHelperFactory() {
		return orientationHelperFactory.get();
	}

	/**
	 * To allow more customization on how the cells are laid out, the virtual flow allows you
	 * to specify a function used to build a {@link OrientationHelper} which is responsible for some core actions
	 * about cells and layout. This way you could implement your own helper and set it through this factory since the
	 * {@link #orientationHelperProperty()} is intended to be read-only.
	 */
	public FunctionProperty<Orientation, OrientationHelper> orientationHelperFactoryProperty() {
		return orientationHelperFactory;
	}

	public void setOrientationHelperFactory(Function<Orientation, OrientationHelper> orientationHelperFactory) {
		this.orientationHelperFactory.set(orientationHelperFactory);
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
